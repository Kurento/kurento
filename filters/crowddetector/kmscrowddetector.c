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
#define BACKGROUND_ADD_RATIO ((float) 0.98)
#define TEMPORAL_LBPS_ADD_RATIO ((float) 0.8)
#define EDGES_ADD_RATIO ((float) 0.985)
#define IMAGE_FUSION_ADD_RATIO ((float) 0.55)
#define RESULTS_ADD_RATIO ((float) 0.6)
#define NEIGHBORS ((int) 8)
#define GRAY_THRESHOLD_VALUE ((int) 45)
#define EDGE_THRESHOLD ((int) 45)

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
  IplImage *actualImage, *previousLbp, *frame_previous_gray, *background,
      *acumulated_edges, *acumulated_lbp;
  gboolean show_debug_info;
  int num_rois;
  CvPoint **curves;
  int *n_points;
  GstStructure *rois;
};

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  PROP_ROIS,
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
kms_crowd_detector_release_data (KmsCrowdDetector * crowddetector)
{
  int it;

  if (crowddetector->priv->curves != NULL) {
    for (it = 0; it < crowddetector->priv->num_rois; it++) {
      g_free (crowddetector->priv->curves[it]);
    }
    g_free (crowddetector->priv->curves);
    crowddetector->priv->curves = NULL;
  }

  if (crowddetector->priv->rois != NULL) {
    gst_structure_free (crowddetector->priv->rois);
    crowddetector->priv->rois = NULL;
  }

  if (crowddetector->priv->n_points != NULL) {
    g_free (crowddetector->priv->n_points);
    crowddetector->priv->n_points = NULL;
  }
}

static void
kms_crowd_detector_extract_rois (KmsCrowdDetector * self)
{
  int it = 0, it2;

  self->priv->num_rois = gst_structure_n_fields (self->priv->rois);
  if (self->priv->num_rois != 0) {
    self->priv->curves = g_malloc (sizeof (CvPoint *) * self->priv->num_rois);
    self->priv->n_points = g_malloc (sizeof (int) * self->priv->num_rois);
  }

  while (it < self->priv->num_rois) {
    int len;

    GstStructure *roi;
    gboolean ret2;
    const gchar *nameRoi = gst_structure_nth_field_name (self->priv->rois, it);

    ret2 = gst_structure_get (self->priv->rois, nameRoi,
        GST_TYPE_STRUCTURE, &roi, NULL);
    if (!ret2) {
      continue;
    }
    len = gst_structure_n_fields (roi);
    self->priv->n_points[it] = len;
    if (len == 0) {
      self->priv->num_rois--;
      continue;
    } else {
      self->priv->curves[it] = g_malloc (sizeof (CvPoint) * len);
    }

    for (it2 = 0; it2 < len; it2++) {
      const gchar *name = gst_structure_nth_field_name (roi, it2);
      GstStructure *point;
      gboolean ret;

      ret = gst_structure_get (roi, name, GST_TYPE_STRUCTURE, &point, NULL);

      if (ret) {
        gst_structure_get (point, "x", G_TYPE_INT,
            &self->priv->curves[it][it2].x, NULL);
        gst_structure_get (point, "y", G_TYPE_INT,
            &self->priv->curves[it][it2].y, NULL);
      }
      gst_structure_free (point);
    }
    gst_structure_free (roi);
    it++;
  }
}

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
    case PROP_ROIS:
      kms_crowd_detector_release_data (crowddetector);
      crowddetector->priv->rois = g_value_dup_boxed (value);
      kms_crowd_detector_extract_rois (crowddetector);
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
    case PROP_ROIS:
      if (crowddetector->priv->rois == NULL) {
        crowddetector->priv->rois = gst_structure_new_empty ("rois");
      }
      g_value_set_boxed (value, crowddetector->priv->rois);
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
  cvReleaseImage (&crowddetector->priv->background);
  cvReleaseImage (&crowddetector->priv->acumulated_edges);
  cvReleaseImage (&crowddetector->priv->acumulated_lbp);
}

static void
kms_crowd_detector_finalize (GObject * object)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "finalize");

  kms_crowd_detector_release_images (crowddetector);
  kms_crowd_detector_release_data (crowddetector);

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
  crowddetector->priv->background =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  crowddetector->priv->acumulated_edges =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  crowddetector->priv->acumulated_lbp =
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
kms_crowd_detector_mask_image (IplImage * src, IplImage * mask,
    int thresholdValue)
{
  int w, h;
  uint8_t *maskPointerAux;
  uint8_t *maskPointer = (uint8_t *) mask->imageData;
  uint8_t *srcPointerAux;
  uint8_t *srcPointer = (uint8_t *) src->imageData;

  for (h = 0; h < mask->height; h++) {
    maskPointerAux = maskPointer;
    srcPointerAux = srcPointer;
    for (w = 0; w < mask->width; w++) {
      if (*maskPointerAux == thresholdValue)
        *srcPointerAux = 0;
      maskPointerAux++;
      srcPointerAux++;
    }
    maskPointer += mask->widthStep;
    srcPointer += src->widthStep;
  }
}

static void
kms_crowd_detector_substract_background (IplImage * frame,
    IplImage * background, IplImage * actualImage)
{
  int w, h;
  uint8_t *framePointerAux;
  uint8_t *framePointer = (uint8_t *) frame->imageData;
  uint8_t *backgroundPointerAux;
  uint8_t *backgroundPointer = (uint8_t *) background->imageData;
  uint8_t *actualImagePointerAux;
  uint8_t *actualImagePointer = (uint8_t *) actualImage->imageData;

  for (h = 0; h < frame->height; h++) {
    framePointerAux = framePointer;
    backgroundPointerAux = backgroundPointer;
    actualImagePointerAux = actualImagePointer;
    for (w = 0; w < frame->width; w++) {
      if (abs (*framePointerAux - *backgroundPointerAux) < GRAY_THRESHOLD_VALUE)
        *actualImagePointerAux = 255;
      else
        *actualImagePointerAux = *framePointerAux;
      framePointerAux++;
      backgroundPointerAux++;
      actualImagePointerAux++;
    }
    framePointer += frame->widthStep;
    backgroundPointer += background->widthStep;
    actualImagePointer += actualImage->widthStep;
  }
}

static void
kms_crowd_detector_process_edges_image (KmsCrowdDetector * crowddetector,
    IplImage * speed_map, int windowMargin)
{
  int w, h, w2, h2;
  uint8_t *speedMapPointerAux;
  uint8_t *speedMapPointer = (uint8_t *) speed_map->imageData;

  for (h2 = windowMargin;
      h2 < crowddetector->priv->acumulated_edges->height - windowMargin - 1;
      h2++) {
    speedMapPointerAux = speedMapPointer;
    for (w2 = windowMargin;
        w2 < crowddetector->priv->acumulated_edges->width - windowMargin - 1;
        w2++) {
      int pixelCounter = 0;

      for (h = -windowMargin; h < windowMargin + 1; h++) {
        for (w = -windowMargin; w < windowMargin + 1; w++) {
          if (h != 0 || w != 0) {
            if (*(uchar *) (crowddetector->priv->acumulated_edges->imageData +
                    (h2 +
                        h) * crowddetector->priv->acumulated_edges->widthStep +
                    (w2 + w)) > EDGE_THRESHOLD)
              pixelCounter++;
          }
        }
      }
      if (pixelCounter > pow (windowMargin, 2)) {
        *speedMapPointerAux = 255;
      } else
        *speedMapPointerAux = 0;
      speedMapPointerAux++;
    }
    speedMapPointer += speed_map->widthStep;
  }
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
  IplImage *actualImage_masked =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height), IPL_DEPTH_8U, 1);
  IplImage *substract_background_to_actual =
      cvCreateImage (cvSize (crowddetector->priv->actualImage->width,
          crowddetector->priv->actualImage->height),
      IPL_DEPTH_8U, 1);
  IplImage *low_speed_map =
      cvCreateImage (cvSize (crowddetector->priv->acumulated_edges->width,
          crowddetector->priv->acumulated_edges->height),
      IPL_DEPTH_8U, 1);
  IplImage *high_speed_map =
      cvCreateImage (cvSize (crowddetector->priv->acumulated_edges->width,
          crowddetector->priv->acumulated_edges->height),
      IPL_DEPTH_8U, 1);
  IplImage *actual_motion =
      cvCreateImage (cvSize (crowddetector->priv->acumulated_edges->width,
          crowddetector->priv->acumulated_edges->height),
      IPL_DEPTH_8U, 3);

  uint8_t *lowSpeedPointer;
  uint8_t *lowSpeedPointerAux;
  uint8_t *highSpeedPointer;
  uint8_t *highSpeedPointerAux;
  uint8_t *actualMotionPointer;
  uint8_t *actualMotionPointerAux;

  int w, h;

  cvZero (actualImage_masked);
  if (crowddetector->priv->num_rois != 0) {
    cvFillPoly (actualImage_masked, crowddetector->priv->curves,
        crowddetector->priv->n_points, crowddetector->priv->num_rois,
        cvScalar (255, 255, 255, 0), CV_AA, 0);
  }
  cvCvtColor (crowddetector->priv->actualImage, frame_actual_Gray, CV_BGR2GRAY);
  kms_crowd_detector_mask_image (frame_actual_Gray, actualImage_masked, 0);

  if (crowddetector->priv->background == NULL) {
    cvCopy (frame_actual_Gray, crowddetector->priv->background, 0);
  } else {
    cvAddWeighted (crowddetector->priv->background, BACKGROUND_ADD_RATIO,
        frame_actual_Gray, 1 - BACKGROUND_ADD_RATIO, 0,
        crowddetector->priv->background);
  }

  kms_crowd_detector_compute_temporal_lbp (frame_actual_Gray, actual_lbp,
      actual_lbp, FALSE);
  kms_crowd_detector_compute_temporal_lbp (frame_actual_Gray,
      lbp_temporal_result, crowddetector->priv->frame_previous_gray, TRUE);
  cvAddWeighted (crowddetector->priv->previousLbp, LBPS_ADD_RATIO, actual_lbp,
      (1 - LBPS_ADD_RATIO), 0, add_lbps_result);
  cvSub (crowddetector->priv->previousLbp, actual_lbp, add_lbps_result, 0);
  cvThreshold (add_lbps_result, add_lbps_result, 70, 255, CV_THRESH_OTSU);
  cvNot (add_lbps_result, add_lbps_result);
  cvErode (add_lbps_result, add_lbps_result, 0, 4);
  cvDilate (add_lbps_result, add_lbps_result, 0, 11);
  cvErode (add_lbps_result, add_lbps_result, 0, 3);
  cvCvtColor (add_lbps_result, lbps_alpha_result_rgb, CV_GRAY2BGR);
  cvCopy (actual_lbp, crowddetector->priv->previousLbp, 0);
  cvCopy (frame_actual_Gray, crowddetector->priv->frame_previous_gray, 0);

  if (crowddetector->priv->acumulated_lbp == NULL) {
    cvCopy (add_lbps_result, crowddetector->priv->acumulated_lbp, 0);
  } else {
    cvAddWeighted (crowddetector->priv->acumulated_lbp, TEMPORAL_LBPS_ADD_RATIO,
        add_lbps_result, 1 - TEMPORAL_LBPS_ADD_RATIO, 0,
        crowddetector->priv->acumulated_lbp);
  }

  cvThreshold (crowddetector->priv->acumulated_lbp, high_speed_map, 150, 255,
      CV_THRESH_BINARY);
  cvSmooth (high_speed_map, high_speed_map, CV_MEDIAN, 3, 0, 0, 0);
  kms_crowd_detector_substract_background (frame_actual_Gray,
      crowddetector->priv->background, substract_background_to_actual);
  cvThreshold (substract_background_to_actual, substract_background_to_actual,
      70, 255, CV_THRESH_OTSU);

  cvCanny (substract_background_to_actual,
      substract_background_to_actual, 70, 150, 3);

  if (crowddetector->priv->acumulated_edges == NULL) {
    cvCopy (substract_background_to_actual,
        crowddetector->priv->acumulated_edges, 0);
  } else {
    cvAddWeighted (crowddetector->priv->acumulated_edges, EDGES_ADD_RATIO,
        substract_background_to_actual, 1 - EDGES_ADD_RATIO, 0,
        crowddetector->priv->acumulated_edges);
  }

  cvZero (low_speed_map);
  kms_crowd_detector_process_edges_image (crowddetector, low_speed_map, 3);
  cvErode (low_speed_map, low_speed_map, 0, 1);

  lowSpeedPointer = (uint8_t *) low_speed_map->imageData;
  highSpeedPointer = (uint8_t *) high_speed_map->imageData;
  actualMotionPointer = (uint8_t *) actual_motion->imageData;

  for (h = 0; h < low_speed_map->height; h++) {
    lowSpeedPointerAux = lowSpeedPointer;
    highSpeedPointerAux = highSpeedPointer;
    actualMotionPointerAux = actualMotionPointer;
    for (w = 0; w < low_speed_map->width; w++) {
      if (*highSpeedPointerAux == 0) {
        actualMotionPointerAux[0] = 255;
      }
      if (*lowSpeedPointerAux == 255) {
        *actualMotionPointerAux = 0;
        actualMotionPointerAux[2] = 255;
      }
      lowSpeedPointerAux++;
      highSpeedPointerAux++;
      actualMotionPointerAux = actualMotionPointerAux + 3;
    }
    lowSpeedPointer += low_speed_map->widthStep;
    highSpeedPointer += high_speed_map->widthStep;
    actualMotionPointer += actual_motion->widthStep;
  }

  cvAddWeighted (actual_motion, IMAGE_FUSION_ADD_RATIO,
      crowddetector->priv->actualImage, 1 - IMAGE_FUSION_ADD_RATIO, 0,
      crowddetector->priv->actualImage);
  if (crowddetector->priv->num_rois != 0) {
    cvPolyLine (crowddetector->priv->actualImage, crowddetector->priv->curves,
        crowddetector->priv->n_points, crowddetector->priv->num_rois, 1,
        cvScalar (255, 255, 255, 0), 1, 8, 0);
  }

  cvReleaseImage (&frame_actual_Gray);
  cvReleaseImage (&actual_lbp);
  cvReleaseImage (&lbp_temporal_result);
  cvReleaseImage (&add_lbps_result);
  cvReleaseImage (&lbps_alpha_result_rgb);
  cvReleaseImage (&actualImage_masked);
  cvReleaseImage (&substract_background_to_actual);
  cvReleaseImage (&low_speed_map);
  cvReleaseImage (&high_speed_map);
  cvReleaseImage (&actual_motion);

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

  g_object_class_install_property (gobject_class, PROP_ROIS,
      g_param_spec_boxed ("rois", "rois",
          "set regions of interest to analize",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

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
  crowddetector->priv->num_rois = 0;
  crowddetector->priv->curves = NULL;
  crowddetector->priv->n_points = NULL;
  crowddetector->priv->rois = NULL;
}

gboolean
kms_crowd_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_CROWD_DETECTOR);
}
