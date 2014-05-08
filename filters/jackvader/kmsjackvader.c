/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsjackvader.h"
#include "classifier.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>

#include <opencv/highgui.h>

#define GREEN CV_RGB (0, 255, 0)

#define PLUGIN_NAME "jackvader"

#define HAAR_CASCADES_DIR_OPENCV_PREFIX "/usr/share/opencv/haarcascades/"

#define FACE_HAAR_FILE "haarcascade_frontalface_default.xml"
#define COSTUME_IMAGES_PATH_DEFAULT DATAROOTDIR "/" PACKAGE "/"
#define JACK_IMAGE_FILE "jack.png"
#define VADER_IMAGE_FILE "vader.png"

#define MIN_FPS 5
#define MIN_TIME (float)(1.0/7.0)

GST_DEBUG_CATEGORY_STATIC (kms_jack_vader_debug_category);
#define GST_CAT_DEFAULT kms_jack_vader_debug_category

enum
{
  PROP_0,
  PROP_IMAGES_PATH,
  PROP_SHOW_DEBUG_INFO,
  PROP_FILTER_VERSION
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsJackVader, kms_jack_vader,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_jack_vader_debug_category, PLUGIN_NAME,
        0, "debug category for jackvader element"));

static void
kms_jack_vader_initialize_classifiers (KmsJackVader * jackvader)
{
  gchar *path;

  GST_DEBUG ("Loading classifier: %s",
      HAAR_CASCADES_DIR_OPENCV_PREFIX FACE_HAAR_FILE);

  if (jackvader->pCascadeFace != NULL)
    cvReleaseHaarClassifierCascade (&jackvader->pCascadeFace);

  jackvader->pCascadeFace = (CvHaarClassifierCascade *)
      cvLoad ((HAAR_CASCADES_DIR_OPENCV_PREFIX FACE_HAAR_FILE), 0, 0, 0);

  path = g_strdup_printf ("%s%s", jackvader->images_path, JACK_IMAGE_FILE);

  GST_DEBUG ("Loading image: %s", path);

  if (jackvader->originalCostume2 != NULL)
    cvReleaseImage (&jackvader->originalCostume2);

  jackvader->originalCostume2 = cvLoadImage (path, CV_LOAD_IMAGE_UNCHANGED);
  if (jackvader->originalCostume2 == NULL) {
    GST_DEBUG ("cant load Jack image from %s,loading synthetic image..", path);
    jackvader->originalCostume2 =
        cvCreateImage (cvSize (5, 5), IPL_DEPTH_8U, 4);
    cvSet (jackvader->originalCostume2, cvScalar (0, 255, 0, 255), 0);
  }
  g_free (path);

  path = g_strdup_printf ("%s%s", jackvader->images_path, VADER_IMAGE_FILE);

  GST_DEBUG ("Loading image: %s", path);
  if (jackvader->originalCostume1 != NULL)
    cvReleaseImage (&jackvader->originalCostume1);

  jackvader->originalCostume1 = cvLoadImage (path, CV_LOAD_IMAGE_UNCHANGED);
  if (jackvader->originalCostume1 == NULL) {
    GST_DEBUG ("cant load Vader image from %s,loading synthetic image..", path);
    jackvader->originalCostume1 =
        cvCreateImage (cvSize (5, 5), IPL_DEPTH_8U, 4);
    cvSet (jackvader->originalCostume1, cvScalar (255, 0, 0, 255), 0);
  }
  g_free (path);
}

void
kms_jack_vader_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsJackVader *jackvader = KMS_JACK_VADER (object);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      jackvader->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_IMAGES_PATH:
      if (jackvader->images_path != NULL)
        g_free ((gpointer) jackvader->images_path);

      jackvader->images_path = g_value_dup_string (value);
      kms_jack_vader_initialize_classifiers (jackvader);
      break;
    case PROP_FILTER_VERSION:
      jackvader->haarDetector = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_jack_vader_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsJackVader *jackvader = KMS_JACK_VADER (object);

  GST_DEBUG_OBJECT (jackvader, "get_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, jackvader->show_debug_info);
      break;
    case PROP_IMAGES_PATH:
      g_value_set_string (value, jackvader->images_path);
      break;
    case PROP_FILTER_VERSION:
      g_value_set_boolean (value, jackvader->haarDetector);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
displayDetectionsOverlayImg (KmsJackVader * jackvader)
{
  int i;

  for (i = 0;
      i < (jackvader->pFaceRectSeq ? jackvader->pFaceRectSeq->total : 0); i++) {
    CvRect *r = (CvRect *) cvGetSeqElem (jackvader->pFaceRectSeq, i);

    IplImage *costumeAux;

    if ((r->x + r->width / 2) < jackvader->cvImage->width / 2) {
      r->x = r->x - r->width / 4;
      r->y = r->y - r->height / 4.5;
      r->height = r->height * 1.9;
      r->width = r->width * 1.5;

      costumeAux = cvCreateImage (cvSize (r->width, r->height),
          jackvader->cvImage->depth, 4);
      cvResize (jackvader->originalCostume1, costumeAux, CV_INTER_LINEAR);
    } else {
      int widthAux = r->width;

      r->width = r->width * 1.6;
      r->x = r->x - ((r->width - widthAux) / 2);
      r->y = r->y - r->height / 4;
      r->height = r->height * 1.5;

      costumeAux = cvCreateImage (cvSize (r->width, r->height),
          jackvader->cvImage->depth, 4);
      cvResize (jackvader->originalCostume2, costumeAux, CV_INTER_LINEAR);
    }

    int i, j;

    for (i = 0; i < costumeAux->width; i++) {
      for (j = 0; j < costumeAux->height; j++) {
        if (((i + r->x) < jackvader->cvImage->width) && ((i + r->x) >= 0)) {
          if (((j + r->y) < jackvader->cvImage->height) && ((j + r->y) >= 0)) {
            if (*(uchar *) (costumeAux->imageData +
                    (j * costumeAux->widthStep) + (i * 4) + 3) == 255) {
              *(uchar *) (jackvader->cvImage->imageData +
                  (j + r->y) * jackvader->cvImage->widthStep +
                  (i + r->x) * 3) = *(uchar *) (costumeAux->imageData +
                  (j) * costumeAux->widthStep + (i * 4));
              *(uchar *) (jackvader->cvImage->imageData +
                  (j + r->y) * jackvader->cvImage->widthStep +
                  (i + r->x) * 3 + 1) = *(uchar *) (costumeAux->imageData +
                  (j) * costumeAux->widthStep + (i * 4) + 1);
              *(uchar *) (jackvader->cvImage->imageData +
                  (j + r->y) * jackvader->cvImage->widthStep +
                  (i + r->x) * 3 + 2) = *(uchar *) (costumeAux->imageData +
                  (j) * costumeAux->widthStep + (i * 4) + 2);
            }
          }
        }
      }
    }

    cvReleaseImage (&costumeAux);
  }
}

static void
kms_jack_vader_initialize_images (KmsJackVader * jackvader,
    GstVideoFrame * frame)
{
  if (jackvader->cvImage == NULL) {
    jackvader->cvImage =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);

  } else if ((jackvader->cvImage->width != frame->info.width)
      || (jackvader->cvImage->height != frame->info.height)) {

    cvReleaseImage (&jackvader->cvImage);
    jackvader->cvImage =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
  }
}

static void
displayFaceRectangle (KmsJackVader * jackvader)
{
  int i;

  for (i = 0;
      i < (jackvader->pFaceRectSeq ? jackvader->pFaceRectSeq->total : 0); i++) {
    CvRect *r = (CvRect *) cvGetSeqElem (jackvader->pFaceRectSeq, i);
    CvPoint pt1 = { r->x, r->y };
    CvPoint pt2 = { r->x + r->width, r->y + r->height };
    cvRectangle (jackvader->cvImage, pt1, pt2, GREEN, 3, 4, 0);
  }
}

static GstFlowReturn
kms_jack_vader_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsJackVader *jackvader = KMS_JACK_VADER (filter);
  GstMapInfo info;

  if (jackvader->haarDetector) {
    if (jackvader->pCascadeFace == NULL)
      return GST_FLOW_OK;
  }

  kms_jack_vader_initialize_images (jackvader, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  jackvader->cvImage->imageData = (char *) info.data;

  if (g_atomic_int_get (&jackvader->qos_control)) {
    GST_DEBUG ("Filter is too slow. Frame dropped\n");
    goto display;
  }

  cvClearSeq (jackvader->pFaceRectSeq);

  if (jackvader->haarDetector) {
    jackvader->pFaceRectSeq = cvHaarDetectObjects (jackvader->cvImage,
        jackvader->pCascadeFace,
        jackvader->pStorageFace,
        1.2, 3,
        CV_HAAR_DO_CANNY_PRUNING,
        cvSize (jackvader->cvImage->width / 20, jackvader->cvImage->width / 20),
        cvSize (jackvader->cvImage->width / 2, jackvader->cvImage->height / 2));

  } else {
    classify_image (jackvader->cvImage, jackvader->pFaceRectSeq);
  }

display:
  if (jackvader->show_debug_info == TRUE) {
    displayFaceRectangle (jackvader);
  }

  displayDetectionsOverlayImg (jackvader);

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

void
kms_jack_vader_dispose (GObject * object)
{
  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_jack_vader_parent_class)->dispose (object);
}

void
kms_jack_vader_finalize (GObject * object)
{
  KmsJackVader *jackvader = KMS_JACK_VADER (object);

  cvReleaseImage (&jackvader->cvImage);
  cvReleaseImage (&jackvader->originalCostume1);
  cvReleaseImage (&jackvader->originalCostume2);
  if (jackvader->pStorageFace != NULL)
    cvClearMemStorage (jackvader->pStorageFace);
  if (jackvader->pFaceRectSeq != NULL)
    cvClearSeq (jackvader->pFaceRectSeq);

  if (jackvader->images_path != NULL)
    g_free ((gpointer) jackvader->images_path);

  cvReleaseMemStorage (&jackvader->pStorageFace);
  cvReleaseHaarClassifierCascade (&jackvader->pCascadeFace);

  G_OBJECT_CLASS (kms_jack_vader_parent_class)->finalize (object);
}

static void
kms_jack_vader_init (KmsJackVader * jackvader)
{
  jackvader->pCascadeFace = 0;
  jackvader->pStorageFace = cvCreateMemStorage (0);
  jackvader->pFaceRectSeq = cvCreateSeq (0, sizeof (CvSeq), sizeof (CvRect),
      jackvader->pStorageFace);
  jackvader->show_debug_info = FALSE;
  jackvader->images_path = g_strdup (COSTUME_IMAGES_PATH_DEFAULT);
  jackvader->haarDetector = TRUE;
}

static gboolean
kms_jack_vader_src_eventfunc (GstBaseTransform * trans, GstEvent * event)
{
  gboolean ret;
  KmsJackVader *jackvader = KMS_JACK_VADER (trans);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_QOS:
    {
      gdouble proportion;
      GstClockTimeDiff diff;
      GstClockTime timestamp;
      GstQOSType type;

      gst_event_parse_qos (event, &type, &proportion, &diff, &timestamp);
      gst_base_transform_update_qos (trans, proportion, diff, timestamp);
      gfloat difference = (((gfloat) (gint) diff) / (gfloat) GST_SECOND);

      g_atomic_int_set (&jackvader->qos_control, FALSE);

      if (difference > MIN_TIME) {
        if (jackvader->throw_frames <= MIN_FPS) {
          g_atomic_int_set (&jackvader->qos_control, TRUE);
          jackvader->throw_frames++;
        } else {
          jackvader->throw_frames = 0;
        }
      }
      break;
    }
    default:
      break;
  }

  ret = gst_pad_push_event (trans->sinkpad, event);
  return ret;
}

static void
kms_jack_vader_class_init (KmsJackVaderClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  GST_DEBUG ("class init");

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Jack Daver element", "Video/Filter",
      "Set Jack Sparrow or Darth Vader mask depending on the positon of the face",
      "Francisco Rivero <fj.riverog@gmail.com>");

  gobject_class->set_property = kms_jack_vader_set_property;
  gobject_class->get_property = kms_jack_vader_get_property;
  gobject_class->dispose = kms_jack_vader_dispose;
  gobject_class->finalize = kms_jack_vader_finalize;

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_jack_vader_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_IMAGES_PATH,
      g_param_spec_string ("costume-images-path", "costume images path",
          "path of folder that contain the costume images",
          COSTUME_IMAGES_PATH_DEFAULT, G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

  g_object_class_install_property (gobject_class, PROP_FILTER_VERSION,
      g_param_spec_boolean ("filter-version", "filter version",
          "True means filter based on haar detector. False filter based on lbp",
          TRUE, G_PARAM_READWRITE));

  klass->base_facedetector_class.parent_class.src_event =
      GST_DEBUG_FUNCPTR (kms_jack_vader_src_eventfunc);
}

gboolean
kms_jack_vader_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_JACK_VADER);
}
