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

#include "kmsfacedetectormetadata.hpp"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>

#include <glib/gprintf.h>

#include <opencv2/core.hpp> // Mat
#include <opencv2/imgproc.hpp> // resize
#include <opencv2/objdetect.hpp> // CascadeClassifier

#include <commons/kmsserializablemeta.h>

#define FACE_HAAR_FILE \
  "/usr/share/opencv4/haarcascades/haarcascade_frontalface_default.xml"

#define MIN_FPS 5
#define MIN_TIME ((float)(1.0 / 7.0))

#define MAX_WIDTH 320

GST_DEBUG_CATEGORY_STATIC (kms_face_detector_metadata_debug_category);
#define GST_CAT_DEFAULT kms_face_detector_metadata_debug_category
#define PLUGIN_NAME "facedetectormetadata"

#define KMS_FACE_DETECTOR_METADATA_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_FACE_DETECTOR_METADATA, \
      KmsFaceDetectorMetadataPrivate))

struct _KmsFaceDetectorMetadataPrivate {
  cv::Mat cvImage;
  cv::Mat cvResizedImage;
  gdouble resize_factor;

  gboolean show_debug_info;
  const char *images_path;
  gint throw_frames;
  gboolean qos_control;
  gboolean haar_detector;
  GMutex mutex;

  cv::CascadeClassifier face_cascade;
  std::vector<cv::Rect> face_rects;
};

enum { PROP_0, PROP_SHOW_DEBUG_INFO, PROP_FILTER_VERSION };

/* pad templates */

#define VIDEO_SRC_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

#define VIDEO_SINK_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsFaceDetectorMetadata,
    kms_face_detector_metadata,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_face_detector_metadata_debug_category,
        PLUGIN_NAME,
        0,
        "debug category for facedetector element"));

static void
kms_face_detector_metadata_initialize_classifiers (
    KmsFaceDetectorMetadata *facedetector)
{
  GST_INFO ("Loading classifier: %s", FACE_HAAR_FILE);
  facedetector->priv->face_cascade.load (FACE_HAAR_FILE);
}

static void
kms_face_detector_metadata_set_property (GObject *object,
    guint property_id,
    const GValue *value,
    GParamSpec *pspec)
{
  KmsFaceDetectorMetadata *facedetector = KMS_FACE_DETECTOR_METADATA (object);

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
kms_face_detector_metadata_get_property (GObject *object,
    guint property_id,
    GValue *value,
    GParamSpec *pspec)
{
  KmsFaceDetectorMetadata *facedetector = KMS_FACE_DETECTOR_METADATA (object);

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
kms_face_detector_metadata_initialize_images (
    KmsFaceDetectorMetadata *facedetector,
    GstVideoFrame *frame)
{
  const int width = GST_VIDEO_FRAME_WIDTH (frame);
  const int height = GST_VIDEO_FRAME_HEIGHT (frame);
  const void *data = GST_VIDEO_FRAME_PLANE_DATA (frame, 0);
  const size_t step = GST_VIDEO_FRAME_PLANE_STRIDE (frame, 0);

  if (facedetector->priv->cvImage.empty ()
      || facedetector->priv->cvImage.size () != cv::Size (width, height)) {
    int target_width = width <= MAX_WIDTH ? width : MAX_WIDTH;

    facedetector->priv->resize_factor = width / target_width;

    facedetector->priv->cvResizedImage = cv::Mat (
        cv::Size (target_width, height / facedetector->priv->resize_factor),
        CV_8UC3);
  }

  facedetector->priv->cvImage =
      cv::Mat (cv::Size (width, height), CV_8UC3, (void *)data, step);

  cv::resize (facedetector->priv->cvImage, facedetector->priv->cvResizedImage,
      facedetector->priv->cvResizedImage.size ());
}

static void
kms_face_detector_metadata_send_metadata (KmsFaceDetectorMetadata *facedetector,
    GstVideoFrame *frame)
{
  GstStructure *faces = gst_structure_new_empty ("faces");

  // clang-format off
  GstStructure *timestamp = gst_structure_new ("time",
      "pts", G_TYPE_UINT64, GST_BUFFER_PTS (frame->buffer),
      "dts", G_TYPE_UINT64, GST_BUFFER_DTS (frame->buffer),
      NULL);
  // clang-format on

  gst_structure_set (faces, "timestamp", GST_TYPE_STRUCTURE, timestamp, NULL);
  gst_structure_free (timestamp);

  guint id = 0;
  for (const auto &face_rect : facedetector->priv->face_rects) {
    // clang-format off
    GstStructure *face = gst_structure_new ("face",
        "x", G_TYPE_UINT, (guint)(face_rect.x * facedetector->priv->resize_factor),
        "y", G_TYPE_UINT, (guint)(face_rect.y * facedetector->priv->resize_factor),
        "width", G_TYPE_UINT, (guint)(face_rect.width * facedetector->priv->resize_factor),
        "height", G_TYPE_UINT, (guint)(face_rect.height * facedetector->priv->resize_factor),
        NULL);
    // clang-format on

    gchar *id_str = g_strdup_printf ("%d", id);
    gst_structure_set (faces, id_str, GST_TYPE_STRUCTURE, face, NULL);
    gst_structure_free (face);
    g_free (id_str);

    id++;
  }

  /* send faces detected as metadata */
  kms_buffer_add_serializable_meta (frame->buffer, faces);
}

static GstFlowReturn
kms_face_detector_metadata_transform_frame_ip (GstVideoFilter *filter,
    GstVideoFrame *frame)
{
  KmsFaceDetectorMetadata *facedetector = KMS_FACE_DETECTOR_METADATA (filter);

  if ((facedetector->priv->haar_detector)
      && (facedetector->priv->face_cascade.empty ())) {
    return GST_FLOW_OK;
  }

  kms_face_detector_metadata_initialize_images (facedetector, frame);

  g_mutex_lock (&facedetector->priv->mutex);

  if (facedetector->priv->qos_control) {
    facedetector->priv->throw_frames++;
    GST_DEBUG ("Filter is too slow. Frame dropped %d",
        facedetector->priv->throw_frames);
    g_mutex_unlock (&facedetector->priv->mutex);
    goto send;
  }

  g_mutex_unlock (&facedetector->priv->mutex);

  facedetector->priv->face_rects.clear ();

  if (facedetector->priv->haar_detector) {
    facedetector->priv->face_cascade.detectMultiScale (
        facedetector->priv->cvResizedImage, facedetector->priv->face_rects, 1.2,
        3, cv::CASCADE_DO_CANNY_PRUNING,
        cv::Size (facedetector->priv->cvResizedImage.cols / 20,
            facedetector->priv->cvResizedImage.rows / 20),
        cv::Size (facedetector->priv->cvResizedImage.cols / 2,
            facedetector->priv->cvResizedImage.rows / 2));
  }

send:
  if (facedetector->priv->face_rects.size () > 0) {
    kms_face_detector_metadata_send_metadata (facedetector, frame);
  }

  return GST_FLOW_OK;
}

static void
kms_face_detector_metadata_finalize (GObject *object)
{
  KmsFaceDetectorMetadata *facedetector = KMS_FACE_DETECTOR_METADATA (object);

  facedetector->priv->cvImage.release ();
  facedetector->priv->cvResizedImage.release ();

  facedetector->priv->face_rects.clear ();

  g_mutex_clear (&facedetector->priv->mutex);

  G_OBJECT_CLASS (kms_face_detector_metadata_parent_class)->finalize (object);
}

static void
kms_face_detector_metadata_init (KmsFaceDetectorMetadata *facedetector)
{
  facedetector->priv = KMS_FACE_DETECTOR_METADATA_GET_PRIVATE (facedetector);

  facedetector->priv->show_debug_info = FALSE;
  facedetector->priv->qos_control = FALSE;
  facedetector->priv->throw_frames = 0;
  facedetector->priv->haar_detector = TRUE;
  g_mutex_init (&facedetector->priv->mutex);

  kms_face_detector_metadata_initialize_classifiers (facedetector);
}

static gboolean
kms_face_detector_metadata_src_eventfunc (GstBaseTransform *trans,
    GstEvent *event)
{
  KmsFaceDetectorMetadata *facedetector = KMS_FACE_DETECTOR_METADATA (trans);

  switch (GST_EVENT_TYPE (event)) {
  case GST_EVENT_QOS: {
    gdouble proportion;
    GstClockTimeDiff diff;
    GstClockTime timestamp;
    GstQOSType type;
    gfloat difference;

    gst_event_parse_qos (event, &type, &proportion, &diff, &timestamp);
    gst_base_transform_update_qos (trans, proportion, diff, timestamp);
    difference = (((gfloat)(gint)diff) / (gfloat)GST_SECOND);

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
kms_face_detector_metadata_class_init (KmsFaceDetectorMetadataClass *klass)
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
      "Face Detector element", "Video/Filter", "Detect faces in an image",
      "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_face_detector_metadata_set_property;
  gobject_class->get_property = kms_face_detector_metadata_get_property;
  gobject_class->finalize = kms_face_detector_metadata_finalize;

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_face_detector_metadata_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_FILTER_VERSION,
      g_param_spec_boolean ("filter-version", "filter version",
          "True means filter based on haar detector. False filter based on lbp",
          TRUE, G_PARAM_READWRITE));

  klass->base_facedetector_class.parent_class.src_event =
      GST_DEBUG_FUNCPTR (kms_face_detector_metadata_src_eventfunc);

  g_type_class_add_private (klass, sizeof (KmsFaceDetectorMetadataPrivate));
}

gboolean
kms_face_detector_metadata_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_FACE_DETECTOR_METADATA);
}
