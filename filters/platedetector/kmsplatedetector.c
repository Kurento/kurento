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
#include "kmsplatedetector.h"

#define PLUGIN_NAME "platedetector"

#define GREEN CV_RGB (0, 255, 0)
#define BLUE CV_RGB (255, 0, 0)
#define RED CV_RGB (0, 0, 255)
#define WHITE CV_RGB (255, 255, 255)
#define BLACK CV_RGB (0, 0, 0)

#define PLATE_IDEAL_PROPORTION  ((float) 4.7)
#define MIN_CHARACTER_CONTOUR_AREA ((int) 70)
#define MAX_CHARACTER_CONTOUR_AREA ((int) 500)
#define MIN_PLATE_CONTOUR_AREA ((int) 1500)
#define MAX_DIF_PLATE_PROPORTIONS ((float) 1.8)
#define MAX_DIF_PLATE_RECTANGLES_AREA ((float) 0.5)
#define CHARACTER_IDEAL_PROPORTION ((float) 1.3)
#define MAX_DIF_CHARACTER_PROPORTIONS ((float) 0.3)

GST_DEBUG_CATEGORY_STATIC (kms_plate_detector_debug_category);
#define GST_CAT_DEFAULT kms_plate_detector_debug_category

/* prototypes */

static void kms_plate_detector_set_property (GObject * object,
    guint property_id, const GValue * value, GParamSpec * pspec);
static void kms_plate_detector_get_property (GObject * object,
    guint property_id, GValue * value, GParamSpec * pspec);
static void kms_plate_detector_dispose (GObject * object);
static void kms_plate_detector_finalize (GObject * object);

static gboolean kms_plate_detector_start (GstBaseTransform * trans);
static gboolean kms_plate_detector_stop (GstBaseTransform * trans);
static gboolean kms_plate_detector_set_info (GstVideoFilter * filter,
    GstCaps * incaps, GstVideoInfo * in_info, GstCaps * outcaps,
    GstVideoInfo * out_info);
static GstFlowReturn kms_plate_detector_transform_frame_ip (GstVideoFilter *
    filter, GstVideoFrame * frame);

enum
{
  PROP_0
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlateDetector, kms_plate_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_plate_detector_debug_category, PLUGIN_NAME,
        0, "debug category for platedetector element"));

static void
kms_plate_detector_class_init (KmsPlateDetectorClass * klass)
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
      "Plate detector element", "Video/Filter",
      "Detects license plates and raises events with its characters",
      "Francisco Rivero <fj.riverog@gmail.com>");

  gobject_class->set_property = kms_plate_detector_set_property;
  gobject_class->get_property = kms_plate_detector_get_property;
  gobject_class->dispose = kms_plate_detector_dispose;
  gobject_class->finalize = kms_plate_detector_finalize;
  base_transform_class->start = GST_DEBUG_FUNCPTR (kms_plate_detector_start);
  base_transform_class->stop = GST_DEBUG_FUNCPTR (kms_plate_detector_stop);
  video_filter_class->set_info =
      GST_DEBUG_FUNCPTR (kms_plate_detector_set_info);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_plate_detector_transform_frame_ip);

}

static void
kms_plate_detector_init (KmsPlateDetector * platedetector)
{

  platedetector->cvImage = NULL;
  platedetector->edges = NULL;
  platedetector->edgesDilatedMask = NULL;
  platedetector->characterContoursMask = NULL;
  platedetector->preprocessingType = PREPROCESSING_ONE;
}

void
kms_plate_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "set_property");

  switch (property_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_plate_detector_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "get_property");

  switch (property_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_plate_detector_dispose (GObject * object)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_plate_detector_parent_class)->dispose (object);
}

void
kms_plate_detector_finalize (GObject * object)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "finalize");

  cvReleaseImage (&platedetector->cvImage);
  cvReleaseImage (&platedetector->edges);
  cvReleaseImage (&platedetector->edgesDilatedMask);
  cvReleaseImage (&platedetector->characterContoursMask);

  G_OBJECT_CLASS (kms_plate_detector_parent_class)->finalize (object);
}

static gboolean
kms_plate_detector_start (GstBaseTransform * trans)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (trans);

  GST_DEBUG_OBJECT (platedetector, "start");

  return TRUE;
}

static gboolean
kms_plate_detector_stop (GstBaseTransform * trans)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (trans);

  GST_DEBUG_OBJECT (platedetector, "stop");

  return TRUE;
}

static gboolean
kms_plate_detector_set_info (GstVideoFilter * filter, GstCaps * incaps,
    GstVideoInfo * in_info, GstCaps * outcaps, GstVideoInfo * out_info)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (filter);

  GST_DEBUG_OBJECT (platedetector, "set_info");

  return TRUE;
}

static void
kms_plate_detector_create_images (KmsPlateDetector * platedetector,
    GstVideoFrame * frame)
{
  platedetector->cvImage =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 3);
  platedetector->edges =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  platedetector->edgesDilatedMask =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  platedetector->characterContoursMask =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
}

static void
kms_plate_detector_release_images (KmsPlateDetector * platedetector,
    GstVideoFrame * frame)
{
  cvReleaseImage (&platedetector->cvImage);
  cvReleaseImage (&platedetector->edges);
  cvReleaseImage (&platedetector->edgesDilatedMask);
  cvReleaseImage (&platedetector->characterContoursMask);
}

static void
kms_plate_detector_initialize_images (KmsPlateDetector * platedetector,
    GstVideoFrame * frame)
{
  if (platedetector->cvImage == NULL) {
    kms_plate_detector_create_images (platedetector, frame);
  } else if ((platedetector->cvImage->width != frame->info.width)
      || (platedetector->cvImage->height != frame->info.height)) {

    kms_plate_detector_release_images (platedetector, frame);
    kms_plate_detector_create_images (platedetector, frame);
  }
}

static void
kms_plate_detector_preprocessing_method_one (KmsPlateDetector * platedetector)
{
  IplConvKernel *kernel;

  cvCanny (platedetector->edges, platedetector->edges, 70, 150, 3);
  kernel = cvCreateStructuringElementEx (13, 5, 3, 3, CV_SHAPE_RECT, 0);
  cvMorphologyEx (platedetector->edges, platedetector->edgesDilatedMask, NULL,
      kernel, CV_MOP_CLOSE, 1);
  cvReleaseStructuringElement (&kernel);
  kernel = cvCreateStructuringElementEx (9, 5, 5, 3, CV_SHAPE_RECT, 0);
  cvMorphologyEx (platedetector->edgesDilatedMask,
      platedetector->edgesDilatedMask, NULL, kernel, CV_MOP_OPEN, 1);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_preprocessing_method_two (KmsPlateDetector * platedetector)
{
  IplConvKernel *kernel;

  cvSmooth (platedetector->edges, platedetector->edges, CV_MEDIAN, 3, 0, 0, 0);
  cvAdaptiveThreshold (platedetector->edges, platedetector->edges, 255,
      CV_ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY, 3, 5);
  kernel = cvCreateStructuringElementEx (19, 7, 10, 4, CV_SHAPE_RECT, 0);
  cvErode (platedetector->edges, platedetector->edges, kernel, 1);
  cvNot (platedetector->edges, platedetector->edges);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_preprocessing_method_three (KmsPlateDetector * platedetector)
{
  IplConvKernel *kernel;

  cvSmooth (platedetector->edges, platedetector->edges, CV_MEDIAN, 3, 0, 0, 0);
  cvAdaptiveThreshold (platedetector->edges, platedetector->edges, 255,
      CV_ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY, 3, 5);
  cvNot (platedetector->edges, platedetector->edges);
  kernel = cvCreateStructuringElementEx (5, 3, 1, 0, CV_SHAPE_RECT, 0);
  cvDilate (platedetector->edges, platedetector->edges, 0, 1);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_create_little_contour_mask (KmsPlateDetector * platedetector)
{
  CvMemStorage *memCharacters;

  memCharacters = cvCreateMemStorage (0);
  CvSeq *contoursCharacters = 0;
  IplConvKernel *kernel;

  cvSetZero (platedetector->characterContoursMask);
  cvFindContours (platedetector->edges, memCharacters, &contoursCharacters,
      sizeof (CvContour), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contoursCharacters != 0;
      contoursCharacters = contoursCharacters->h_next) {

    CvBox2D fitRect = cvMinAreaRect2 (contoursCharacters, NULL);
    CvRect detectedRect;
    float proportionWidthHeight;
    int contourArea = 0;

    detectedRect.x = fitRect.center.x - fitRect.size.width / 2;
    detectedRect.y = fitRect.center.y - fitRect.size.height / 2;
    detectedRect.width = fitRect.size.width;
    detectedRect.height = fitRect.size.height;

    if (detectedRect.width > 0) {
      proportionWidthHeight = detectedRect.height / detectedRect.width;
    } else {
      proportionWidthHeight = 100;
    }
    contourArea = detectedRect.width * detectedRect.height;

    if ((contourArea > MIN_CHARACTER_CONTOUR_AREA) &&
        (contourArea < MAX_CHARACTER_CONTOUR_AREA)
        && (abs (proportionWidthHeight - CHARACTER_IDEAL_PROPORTION) <
            MAX_DIF_CHARACTER_PROPORTIONS)) {
      cvDrawContours (platedetector->characterContoursMask, contoursCharacters,
          WHITE, WHITE, -1, 1, 8, cvPoint (0, 0));
    }
  }
  kernel = cvCreateStructuringElementEx (21, 11, 1, 0, CV_SHAPE_RECT, 0);
  cvDilate (platedetector->characterContoursMask,
      platedetector->characterContoursMask, kernel, 1);
  cvClearMemStorage (memCharacters);
  cvReleaseMemStorage (&memCharacters);
  cvReleaseStructuringElement (&kernel);
}

static GstFlowReturn
kms_plate_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (filter);
  GstMapInfo info;
  CvSeq *contoursPlates = 0;
  CvMemStorage *memPlates;

  memPlates = cvCreateMemStorage (0);

  kms_plate_detector_initialize_images (platedetector, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  platedetector->cvImage->imageData = (char *) info.data;

  cvCvtColor (platedetector->cvImage, platedetector->edges, CV_BGR2GRAY);

  if (platedetector->preprocessingType == PREPROCESSING_ONE) {
    kms_plate_detector_preprocessing_method_one (platedetector);
  } else if (platedetector->preprocessingType == PREPROCESSING_TWO) {
    kms_plate_detector_preprocessing_method_two (platedetector);
  } else if (platedetector->preprocessingType == PREPROCESSING_THREE) {
    kms_plate_detector_preprocessing_method_three (platedetector);
    kms_plate_detector_create_little_contour_mask (platedetector);
    cvCopy (platedetector->characterContoursMask, platedetector->edges, 0);
  }

  cvFindContours (platedetector->edges, memPlates, &contoursPlates,
      sizeof (CvContour), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contoursPlates != 0; contoursPlates = contoursPlates->h_next) {
    CvBox2D fitRect = cvMinAreaRect2 (contoursPlates, NULL);
    double contourFitArea = cvContourArea (contoursPlates, CV_WHOLE_SEQ, 0);
    CvRect detectedRect;
    float plateProportion;
    int contourBoundArea;
    CvRect rect;

    if (abs (fitRect.angle) < 45) {
      detectedRect.width = fitRect.size.width;
      detectedRect.height = fitRect.size.height;
    } else {
      detectedRect.width = fitRect.size.height;
      detectedRect.height = fitRect.size.width;
    }
    detectedRect.x = fitRect.center.x - detectedRect.width / 2;
    detectedRect.y = fitRect.center.y - detectedRect.height / 2;
    rect = cvBoundingRect (contoursPlates, 0);

    if ((detectedRect.height > 0) && (detectedRect.width > 0)) {
      plateProportion = detectedRect.width / detectedRect.height;
    } else {
      plateProportion = 100;
    }
    contourBoundArea = detectedRect.width * detectedRect.height;

    if ((contourBoundArea > MIN_PLATE_CONTOUR_AREA) &&
        ((abs (PLATE_IDEAL_PROPORTION - plateProportion) <
                MAX_DIF_PLATE_PROPORTIONS))) {
      if ((contourFitArea / contourBoundArea) > MAX_DIF_PLATE_RECTANGLES_AREA) {
        cvRectangle (platedetector->cvImage, cvPoint (rect.x, rect.y),
            cvPoint (rect.x + rect.width, rect.y +
                rect.height), GREEN, 2, 8, 0);
      }
    }
  }

  if (platedetector->preprocessingType == PREPROCESSING_ONE) {
    platedetector->preprocessingType = PREPROCESSING_TWO;
  } else if (platedetector->preprocessingType == PREPROCESSING_TWO) {
    platedetector->preprocessingType = PREPROCESSING_THREE;
  } else if (platedetector->preprocessingType == PREPROCESSING_THREE) {
    platedetector->preprocessingType = PREPROCESSING_ONE;
  }

  cvClearMemStorage (memPlates);
  cvReleaseMemStorage (&memPlates);
  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

gboolean
kms_plate_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLATE_DETECTOR);
}
