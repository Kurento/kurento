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
#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include "kmscrowddetector.h"

#define PLUGIN_NAME "crowddetector"
#define LBPS_ADD_RATIO ((float) 0.4)
#define RESULTS_ADD_RATIO ((float) 0.6)
#define NEIGHBORS ((int) 8)

GST_DEBUG_CATEGORY_STATIC (kms_crowd_detector_debug_category);
#define GST_CAT_DEFAULT kms_crowd_detector_debug_category

#define KMS_CROWD_DETECTOR_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_CROWD_DETECTOR,                    \
    KmsCrowdDetectorPrivate                     \
  )                                             \
)

struct _KmsCrowdDetectorPrivate
{
  IplImage *actualImage, *previousLbp, *frame_previous_gray;
  gboolean show_debug_info;
};

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  N_PROPERTIES
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsCrowdDetector, kms_crowd_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_crowd_detector_debug_category, PLUGIN_NAME,
        0, "debug category for crowddetector element"));

static void
kms_crowd_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "set_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      crowddetector->priv->show_debug_info = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_crowd_detector_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "get_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, crowddetector->priv->show_debug_info);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_crowd_detector_dispose (GObject * object)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_crowd_detector_parent_class)->dispose (object);
}

static void
kms_crowd_detector_release_images (KmsCrowdDetector * crowddetector)
{
  cvReleaseImage (&crowddetector->priv->actualImage);
  cvReleaseImage (&crowddetector->priv->previousLbp);
  cvReleaseImage (&crowddetector->priv->frame_previous_gray);
}

static void
kms_crowd_detector_finalize (GObject * object)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "finalize");

  kms_crowd_detector_release_images (crowddetector);

  G_OBJECT_CLASS (kms_crowd_detector_parent_class)->finalize (object);
}

static gboolean
kms_crowd_detector_start (GstBaseTransform * trans)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (trans);

  GST_DEBUG_OBJECT (crowddetector, "start");

  return TRUE;
}

static gboolean
kms_crowd_detector_stop (GstBaseTransform * trans)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (trans);

  GST_DEBUG_OBJECT (crowddetector, "stop");

  return TRUE;
}

static gboolean
kms_crowd_detector_set_info (GstVideoFilter * filter, GstCaps * incaps,
    GstVideoInfo * in_info, GstCaps * outcaps, GstVideoInfo * out_info)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (filter);

  GST_DEBUG_OBJECT (crowddetector, "set_info");

  return TRUE;
}

static void
kms_crowd_detector_create_images (KmsCrowdDetector * crowddetector,
    GstVideoFrame * frame)
{
  crowddetector->priv->actualImage =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 3);
  crowddetector->priv->previousLbp =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  crowddetector->priv->frame_previous_gray =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
}

static void
kms_crowd_detector_initialize_images (KmsCrowdDetector * crowddetector,
    GstVideoFrame * frame)
{
  if (crowddetector->priv->actualImage == NULL) {
    kms_crowd_detector_create_images (crowddetector, frame);
  } else if ((crowddetector->priv->actualImage->width != frame->info.width)
      || (crowddetector->priv->actualImage->height != frame->info.height)) {

    kms_crowd_detector_release_images (crowddetector);
    kms_crowd_detector_create_images (crowddetector, frame);
  }
}

static void
kms_crowd_detector_compute_temporal_lbp (IplImage * frameGray,
    IplImage * frameResult, IplImage * previousFrameGray, gboolean temporal)
{
  int w, h;
  uint8_t *imagePointer;
  uint8_t *dataAux;
  int refValue = 0;

  if (temporal == TRUE)
    imagePointer = (uint8_t *) previousFrameGray->imageData;
  else
    imagePointer = (uint8_t *) frameGray->imageData;

  for (h = 1; h < frameGray->height - 1; h++) {
    dataAux = imagePointer;
    for (w = 1; w < frameGray->width - 1; w++) {
      refValue = *dataAux;
      dataAux++;
      unsigned int code = 0;

      code |= (*(uchar *) (frameGray->imageData + (h - 1) *
              frameGray->widthStep + (w - 1) * 1) > refValue) << 7;
      code |= (*(uchar *) (frameGray->imageData + (h) *
              frameGray->widthStep + (w - 1) * 1) > refValue) << 6;
      code |= (*(uchar *) (frameGray->imageData + (h + 1) *
              frameGray->widthStep + (w - 1) * 1) > refValue) << 5;
      code |= (*(uchar *) (frameGray->imageData + (h) *
              frameGray->widthStep + (w) * 1) > refValue) << 4;
      code |= (*(uchar *) (frameGray->imageData + (h + 1) *
              frameGray->widthStep + (w + 1) * 1) > refValue) << 3;
      code |= (*(uchar *) (frameGray->imageData + (h) *
              frameGray->widthStep + (w + 1) * 1) > refValue) << 2;
      code |= (*(uchar *) (frameGray->imageData + (h - 1) *
              frameGray->widthStep + (w + 1) * 1) > refValue) << 1;
      code |= (*(uchar *) (frameGray->imageData + (h) *
              frameGray->widthStep + (w + 1) * 1) > refValue) << 0;

      *(uchar *) (frameResult->imageData + h *
          frameResult->widthStep + w * 1) = code;
    }
    imagePointer += previousFrameGray->widthStep;
  }
}

static void
kms_crowd_detector_adaptive_threshold (IplImage * src, IplImage * srcAux)
{
  IplImage *regionAux1 = cvCreateImage (cvSize (src->width, src->height),
      src->depth, 1);
  IplImage *regionAux2 = cvCreateImage (cvSize (src->width, src->height),
      src->depth, 1);
  IplImage *regionAux3 = cvCreateImage (cvSize (src->width, src->height),
      src->depth, 1);
  IplImage *regionAux4 = cvCreateImage (cvSize (src->width, src->height),
      src->depth, 1);
  IplImage *regionAux5 = cvCreateImage (cvSize (src->width, src->height),
      src->depth, 1);
  IplImage *regionAux6 = cvCreateImage (cvSize (src->width, src->height),
      src->depth, 1);

  cvAdaptiveThreshold (src, regionAux1, 42, CV_ADAPTIVE_THRESH_MEAN_C,
      CV_THRESH_BINARY, 3, 5);
  cvAdaptiveThreshold (src, regionAux2, 42, CV_ADAPTIVE_THRESH_MEAN_C,
      CV_THRESH_BINARY, 9, 5);
  cvAdaptiveThreshold (src, regionAux3, 42, CV_ADAPTIVE_THRESH_MEAN_C,
      CV_THRESH_BINARY, 13, 5);
  cvAdaptiveThreshold (src, regionAux4, 42, CV_ADAPTIVE_THRESH_MEAN_C,
      CV_THRESH_BINARY, 17, 5);
  cvAdaptiveThreshold (src, regionAux5, 42, CV_ADAPTIVE_THRESH_MEAN_C,
      CV_THRESH_BINARY, 25, 5);
  cvAdaptiveThreshold (src, regionAux6, 42, CV_ADAPTIVE_THRESH_MEAN_C,
      CV_THRESH_BINARY, 33, 5);

  cvAdd (regionAux1, regionAux2, srcAux, 0);
  cvAdd (srcAux, regionAux3, srcAux, 0);
  cvAdd (srcAux, regionAux4, srcAux, 0);
  cvAdd (srcAux, regionAux5, srcAux, 0);
  cvAdd (srcAux, regionAux6, srcAux, 0);

  cvReleaseImage (&regionAux1);
  cvReleaseImage (&regionAux2);
  cvReleaseImage (&regionAux3);
  cvReleaseImage (&regionAux4);
  cvReleaseImage (&regionAux5);
  cvReleaseImage (&regionAux6);
}

static GstFlowReturn
kms_crowd_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (filter);
  GstMapInfo info;

  kms_crowd_detector_initialize_images (crowddetector, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  crowddetector->priv->actualImage->imageData = (char *) info.data;

  IplImage *frame_actual_Gray =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height),
      IPL_DEPTH_8U, 1);
  IplImage *actual_lbp =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height),
      IPL_DEPTH_8U, 1);
  IplImage *lbp_temporal_result =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height),
      IPL_DEPTH_8U, 1);
  IplImage *add_lbps_result =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height),
      IPL_DEPTH_8U, 1);
  IplImage *lbps_alpha_result_rgb =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height),
      IPL_DEPTH_8U, 3);

  cvCvtColor (crowddetector->priv->actualImage, frame_actual_Gray, CV_BGR2GRAY);
  kms_crowd_detector_adaptive_threshold (frame_actual_Gray, frame_actual_Gray);
  kms_crowd_detector_compute_temporal_lbp (frame_actual_Gray, actual_lbp,
      actual_lbp, FALSE);
  kms_crowd_detector_compute_temporal_lbp (frame_actual_Gray,
      lbp_temporal_result, crowddetector->priv->frame_previous_gray, TRUE);
  cvAddWeighted (crowddetector->priv->previousLbp, LBPS_ADD_RATIO, actual_lbp,
      (1 - LBPS_ADD_RATIO), 0, add_lbps_result);
  cvSub (crowddetector->priv->previousLbp, actual_lbp, add_lbps_result, 0);
  cvThreshold (add_lbps_result, add_lbps_result, 70, 255, CV_THRESH_OTSU);
  cvNot (add_lbps_result, add_lbps_result);
  cvErode (add_lbps_result, add_lbps_result, 0, 4);     // 4
  cvDilate (add_lbps_result, add_lbps_result, 0, 11);   // 11
  cvErode (add_lbps_result, add_lbps_result, 0, 3);     // 4
  cvCvtColor (add_lbps_result, lbps_alpha_result_rgb, CV_GRAY2BGR);
  cvAddWeighted (lbps_alpha_result_rgb, RESULTS_ADD_RATIO,
      crowddetector->priv->actualImage,
      RESULTS_ADD_RATIO, 0, crowddetector->priv->actualImage);
  cvCopy (actual_lbp, crowddetector->priv->previousLbp, 0);
  cvCopy (frame_actual_Gray, crowddetector->priv->frame_previous_gray, 0);

  cvReleaseImage (&frame_actual_Gray);
  cvReleaseImage (&actual_lbp);
  cvReleaseImage (&lbp_temporal_result);
  cvReleaseImage (&add_lbps_result);
  cvReleaseImage (&lbps_alpha_result_rgb);

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
kms_crowd_detector_class_init (KmsCrowdDetectorClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstBaseTransformClass *base_transform_class =
      GST_BASE_TRANSFORM_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Crowd detector element", "Video/Filter",
      "Detects crowded areas based on movement detection",
      "Francisco Rivero <fj.riverog@gmail.com>");

  gobject_class->set_property = kms_crowd_detector_set_property;
  gobject_class->get_property = kms_crowd_detector_get_property;
  gobject_class->dispose = kms_crowd_detector_dispose;
  gobject_class->finalize = kms_crowd_detector_finalize;
  base_transform_class->start = GST_DEBUG_FUNCPTR (kms_crowd_detector_start);
  base_transform_class->stop = GST_DEBUG_FUNCPTR (kms_crowd_detector_stop);
  video_filter_class->set_info =
      GST_DEBUG_FUNCPTR (kms_crowd_detector_set_info);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_crowd_detector_transform_frame_ip);

  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-info", "show debug info",
          "show debug info", FALSE, G_PARAM_READWRITE));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsCrowdDetectorPrivate));
}

static void
kms_crowd_detector_init (KmsCrowdDetector * crowddetector)
{
  crowddetector->priv = KMS_CROWD_DETECTOR_GET_PRIVATE (crowddetector);
  crowddetector->priv->actualImage = NULL;
  crowddetector->priv->previousLbp = NULL;
  crowddetector->priv->frame_previous_gray = NULL;
}

gboolean
kms_crowd_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_CROWD_DETECTOR);
}
