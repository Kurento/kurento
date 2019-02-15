/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsfacedetector.h"
#include "classifier.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>

#include <glib/gprintf.h>

#define GREEN CV_RGB (0, 255, 0)

#define PLUGIN_NAME "facedetector"

#define HAAR_CASCADES_DIR_OPENCV_PREFIX "/usr/share/opencv/haarcascades/"

#define FACE_HAAR_FILE "haarcascade_frontalface_default.xml"

#define MIN_FPS 5
#define MIN_TIME ((float)(1.0/7.0))

#define MAX_WIDTH 320

GST_DEBUG_CATEGORY_STATIC (kms_face_detector_debug_category);
#define GST_CAT_DEFAULT kms_face_detector_debug_category

#define KMS_FACE_DETECTOR_GET_PRIVATE(obj) (    \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_FACE_DETECTOR,                     \
    KmsFaceDetectorPrivate                      \
  )                                             \
)

struct _KmsFaceDetectorPrivate
{
  IplImage *cvImage;
  IplImage *cvResizedImage;
  gdouble resize_factor;

  gboolean show_debug_info;
  const char *images_path;
  gint throw_frames;
  gboolean qos_control;
  gboolean haar_detector;
  GMutex mutex;

  CvHaarClassifierCascade *pCascadeFace;
  CvMemStorage *pStorageFace;
  CvSeq *pFaceRectSeq;
};

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  PROP_FILTER_VERSION
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsFaceDetector, kms_face_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_face_detector_debug_category, PLUGIN_NAME,
        0, "debug category for facedetector element"));

static void
kms_face_detector_initialize_classifiers (KmsFaceDetector * facedetector)
{
  GST_DEBUG ("Loading classifier: %s",
      HAAR_CASCADES_DIR_OPENCV_PREFIX FACE_HAAR_FILE);

  facedetector->priv->pCascadeFace = (CvHaarClassifierCascade *)
      cvLoad ((HAAR_CASCADES_DIR_OPENCV_PREFIX FACE_HAAR_FILE), 0, 0, 0);

  if (facedetector->priv->pCascadeFace == NULL) {
    GST_ERROR ("Failed loading classifier: %s",
        HAAR_CASCADES_DIR_OPENCV_PREFIX FACE_HAAR_FILE);
  }
}

static void
kms_face_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsFaceDetector *facedetector = KMS_FACE_DETECTOR (object);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      facedetector->priv->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_FILTER_VERSION:
      facedetector->priv->haar_detector = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_face_detector_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsFaceDetector *facedetector = KMS_FACE_DETECTOR (object);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, facedetector->priv->show_debug_info);
      break;
    case PROP_FILTER_VERSION:
      g_value_set_boolean (value, facedetector->priv->haar_detector);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_face_detector_initialize_images (KmsFaceDetector * facedetector,
    GstVideoFrame * frame)
{
  if (facedetector->priv->cvImage == NULL) {
    int target_width =
        frame->info.width <= MAX_WIDTH ? frame->info.width : MAX_WIDTH;

    facedetector->priv->resize_factor = frame->info.width / target_width;

    facedetector->priv->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);

    facedetector->priv->cvResizedImage =
        cvCreateImage (cvSize (target_width,
            frame->info.height / facedetector->priv->resize_factor),
        IPL_DEPTH_8U, 3);
  } else if ((facedetector->priv->cvImage->width != frame->info.width)
      || (facedetector->priv->cvImage->height != frame->info.height)) {
    int target_width =
        frame->info.width <= MAX_WIDTH ? frame->info.width : MAX_WIDTH;

    facedetector->priv->resize_factor = frame->info.width / target_width;

    cvReleaseImageHeader (&facedetector->priv->cvImage);
    cvReleaseImage (&facedetector->priv->cvResizedImage);

    facedetector->priv->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);

    facedetector->priv->cvResizedImage =
        cvCreateImage (cvSize (target_width,
            frame->info.height / facedetector->priv->resize_factor),
        IPL_DEPTH_8U, 3);
  }
}

static void
kms_face_detector_send_event (KmsFaceDetector * facedetector,
    GstVideoFrame * frame)
{
  GstStructure *faces;
  GstStructure *timestamp;
  GstEvent *e;
  gint i;

  faces = gst_structure_new_empty ("faces");

  timestamp = gst_structure_new ("time",
      "pts", G_TYPE_UINT64, GST_BUFFER_PTS (frame->buffer),
      "dts", G_TYPE_UINT64, GST_BUFFER_DTS (frame->buffer), NULL);
  gst_structure_set (faces, "timestamp", GST_TYPE_STRUCTURE, timestamp, NULL);
  gst_structure_free (timestamp);

  for (i = 0;
      i <
      (facedetector->priv->pFaceRectSeq ? facedetector->priv->
          pFaceRectSeq->total : 0); i++) {
    CvRect *r;
    GstStructure *face;
    gchar *id = NULL;

    r = (CvRect *) cvGetSeqElem (facedetector->priv->pFaceRectSeq, i);
    face = gst_structure_new ("face",
        "x", G_TYPE_UINT, (guint) (r->x * facedetector->priv->resize_factor),
        "y", G_TYPE_UINT, (guint) (r->y * facedetector->priv->resize_factor),
        "width", G_TYPE_UINT,
        (guint) (r->width * facedetector->priv->resize_factor), "height",
        G_TYPE_UINT, (guint) (r->height * facedetector->priv->resize_factor),
        NULL);

    id = g_strdup_printf ("%d", i);
    gst_structure_set (faces, id, GST_TYPE_STRUCTURE, face, NULL);
    gst_structure_free (face);
    g_free (id);
  }

  /* post a faces detected event to src pad */
  e = gst_event_new_custom (GST_EVENT_CUSTOM_DOWNSTREAM, faces);
  gst_pad_push_event (facedetector->base.element.srcpad, e);
}

static GstFlowReturn
kms_face_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsFaceDetector *facedetector = KMS_FACE_DETECTOR (filter);
  GstMapInfo info;

  if ((facedetector->priv->haar_detector)
      && (facedetector->priv->pCascadeFace == NULL)) {
    return GST_FLOW_OK;
  }

  kms_face_detector_initialize_images (facedetector, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  facedetector->priv->cvImage->imageData = (char *) info.data;
  cvResize (facedetector->priv->cvImage, facedetector->priv->cvResizedImage,
      CV_INTER_LINEAR);

  g_mutex_lock (&facedetector->priv->mutex);

  if (facedetector->priv->qos_control) {
    facedetector->priv->throw_frames++;
    GST_DEBUG ("Filter is too slow. Frame dropped %d",
        facedetector->priv->throw_frames);
    g_mutex_unlock (&facedetector->priv->mutex);
    goto send;
  }

  g_mutex_unlock (&facedetector->priv->mutex);

  cvClearSeq (facedetector->priv->pFaceRectSeq);
  cvClearMemStorage (facedetector->priv->pStorageFace);
  if (facedetector->priv->haar_detector) {
    facedetector->priv->pFaceRectSeq =
        cvHaarDetectObjects (facedetector->priv->cvResizedImage,
        facedetector->priv->pCascadeFace, facedetector->priv->pStorageFace, 1.2,
        3, CV_HAAR_DO_CANNY_PRUNING,
        cvSize (facedetector->priv->cvResizedImage->width / 20,
            facedetector->priv->cvResizedImage->height / 20),
        cvSize (facedetector->priv->cvResizedImage->width / 2,
            facedetector->priv->cvResizedImage->height / 2));

  } else {
    classify_image (facedetector->priv->cvResizedImage,
        facedetector->priv->pFaceRectSeq);
  }

send:
  if (facedetector->priv->pFaceRectSeq->total != 0) {
    kms_face_detector_send_event (facedetector, frame);
  }

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
kms_face_detector_finalize (GObject * object)
{
  KmsFaceDetector *facedetector = KMS_FACE_DETECTOR (object);

  cvReleaseImageHeader (&facedetector->priv->cvImage);
  cvReleaseImage (&facedetector->priv->cvResizedImage);

  if (facedetector->priv->pStorageFace != NULL)
    cvClearMemStorage (facedetector->priv->pStorageFace);
  if (facedetector->priv->pFaceRectSeq != NULL)
    cvClearSeq (facedetector->priv->pFaceRectSeq);

  cvReleaseMemStorage (&facedetector->priv->pStorageFace);
  cvReleaseHaarClassifierCascade (&facedetector->priv->pCascadeFace);

  g_mutex_clear (&facedetector->priv->mutex);

  G_OBJECT_CLASS (kms_face_detector_parent_class)->finalize (object);
}

static void
kms_face_detector_init (KmsFaceDetector * facedetector)
{
  facedetector->priv = KMS_FACE_DETECTOR_GET_PRIVATE (facedetector);

  facedetector->priv->pCascadeFace = NULL;
  facedetector->priv->pStorageFace = cvCreateMemStorage (0);
  facedetector->priv->pFaceRectSeq =
      cvCreateSeq (0, sizeof (CvSeq), sizeof (CvRect),
      facedetector->priv->pStorageFace);
  facedetector->priv->show_debug_info = FALSE;
  facedetector->priv->qos_control = FALSE;
  facedetector->priv->throw_frames = 0;
  facedetector->priv->haar_detector = TRUE;
  facedetector->priv->cvImage = NULL;
  facedetector->priv->cvResizedImage = NULL;
  g_mutex_init (&facedetector->priv->mutex);

  kms_face_detector_initialize_classifiers (facedetector);
}

static gboolean
kms_face_detector_src_eventfunc (GstBaseTransform * trans, GstEvent * event)
{
  KmsFaceDetector *facedetector = KMS_FACE_DETECTOR (trans);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_QOS:
    {
      gdouble proportion;
      GstClockTimeDiff diff;
      GstClockTime timestamp;
      GstQOSType type;
      gfloat difference;

      gst_event_parse_qos (event, &type, &proportion, &diff, &timestamp);
      gst_base_transform_update_qos (trans, proportion, diff, timestamp);
      difference = (((gfloat) (gint) diff) / (gfloat) GST_SECOND);

      g_mutex_lock (&facedetector->priv->mutex);

      if (difference > MIN_TIME) {
        if (facedetector->priv->throw_frames <= MIN_FPS) {
          facedetector->priv->qos_control = TRUE;
        } else {
          facedetector->priv->qos_control = FALSE;
          facedetector->priv->throw_frames = 0;
        }
      } else {
        facedetector->priv->qos_control = FALSE;
        facedetector->priv->throw_frames = 0;
      }

      g_mutex_unlock (&facedetector->priv->mutex);

      break;
    }
    default:
      break;
  }

  return gst_pad_push_event (trans->sinkpad, event);
}

static void
kms_face_detector_class_init (KmsFaceDetectorClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Face Detector element", "Video/Filter",
      "Detect faces in an image", "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_face_detector_set_property;
  gobject_class->get_property = kms_face_detector_get_property;
  gobject_class->finalize = kms_face_detector_finalize;

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_face_detector_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_FILTER_VERSION,
      g_param_spec_boolean ("filter-version", "filter version",
          "True means filter based on haar detector. False filter based on lbp",
          TRUE, G_PARAM_READWRITE));

  klass->base_facedetector_class.parent_class.src_event =
      GST_DEBUG_FUNCPTR (kms_face_detector_src_eventfunc);

  g_type_class_add_private (klass, sizeof (KmsFaceDetectorPrivate));
}

gboolean
kms_face_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_FACE_DETECTOR);
}
