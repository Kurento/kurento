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
#include <locale.h>

#define PLUGIN_NAME "platedetector"
#define GREEN CV_RGB (0, 255, 0)
#define BLUE CV_RGB (0, 0, 255)
#define RED CV_RGB (255, 0, 0)
#define WHITE CV_RGB (255, 255, 255)
#define BLACK CV_RGB (0, 0, 0)
#define PLATE_IDEAL_PROPORTION  ((float) 5.0)
#define CHARACTER_IDEAL_PROPORTION2  ((float) 1.67)
#define MIN_CHARACTER_CONTOUR_AREA ((int) 50)
#define MAX_CHARACTER_CONTOUR_AREA ((int) 500)
#define MIN_PLATE_CONTOUR_AREA ((int) 1500)
#define MAX_DIF_PLATE_PROPORTIONS ((float) 1.8)
#define MAX_DIF_PLATE_RECTANGLES_AREA ((float) 0.5)
#define CHARACTER_IDEAL_PROPORTION ((float) 1.3)
#define MAX_DIF_CHARACTER_PROPORTIONS ((float) 0.5)
#define RESIZE_FACTOR_1 ((float) 1.5)
#define RESIZE_FACTOR_2 ((float) 1.8)
#define RESIZE_FACTOR_3 ((float) 2.2)
#define EDGE_MARGIN ((int) 40)
#define MIN_NUMBER_CHARACTERS ((int) 6)
#define PLATE_WIDTH_EXPAND_RATE ((float) 0.2)
#define PLATE_HEIGHT_EXPAND_RATE ((float) 0.4)
#define RESIZE_HIGH_THRES ((int) 19)
#define RESIZE_LOW_THRES ((int) 13)
#define MIN_OCR_CONFIDENCE_RATE ((int) 70)
#define NUM_ACCUMULATED_PLATES ((int) 4)
#define MAX_NUM_DIF_CHARACTERS ((int) 0)
#define PREVIOUS_PLATE_INI "*********"
#define NULL_PLATE "---------"
#define DEFAULT_CHARACTER_PROPORTION ((int) 100)
#define PLATE_NUMBERS "0123456789"
#define PLATE_LETTERS "BCDFGHJKLMNPQRSTVWXYZ"

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
static void kms_plate_detector_plate_store_initialization (KmsPlateDetector *
    platedetector);

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO
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

  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-info", "show debug info",
          "show characters segmentation and ocr results", FALSE,
          G_PARAM_READWRITE));

}

static void
kms_plate_detector_init (KmsPlateDetector * platedetector)
{
  double hScaleLittle = 0.6;
  double vScaleLittle = 0.6;
  int lineWidthLittle = 18;
  double hScaleBig = 1.0;
  double vScaleBig = 1.0;
  int lineWidthBig = 18;

  platedetector->cvImage = NULL;
  platedetector->preprocessingType = PREPROCESSING_ONE;
  platedetector->handle = TessBaseAPICreate ();
  setlocale (LC_NUMERIC, "C");
  TessBaseAPIInit3 (platedetector->handle, "", "plateLanguage");
  TessBaseAPISetPageSegMode (platedetector->handle, PSM_SINGLE_LINE);
  kms_plate_detector_plate_store_initialization (platedetector);
  platedetector->storePosition = 0;
  platedetector->plateRepetition = 0;
  platedetector->sendPlateEvent = FALSE;
  platedetector->resizeFactor = RESIZE_FACTOR_1;
  strncpy (platedetector->previousFinalPlate, PREVIOUS_PLATE_INI,
      NUM_PLATE_CHARACTERS);
  cvInitFont (&platedetector->littleFont,
      CV_FONT_HERSHEY_SIMPLEX | CV_FONT_ITALIC, hScaleLittle, vScaleLittle, 0,
      1, lineWidthLittle);
  cvInitFont (&platedetector->bigFont, CV_FONT_HERSHEY_SIMPLEX | CV_FONT_ITALIC,
      hScaleBig, vScaleBig, 0, 1, lineWidthBig);
}

void
kms_plate_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "set_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      platedetector->show_debug_info = g_value_get_boolean (value);
      break;
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
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, platedetector->show_debug_info);
      break;
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

static void
kms_plate_detector_release_images (KmsPlateDetector * platedetector)
{
  cvReleaseImage (&platedetector->cvImage);
  cvReleaseImage (&platedetector->edges);
  cvReleaseImage (&platedetector->edgesDilatedMask);
  cvReleaseImage (&platedetector->characterContoursMask);
}

void
kms_plate_detector_finalize (GObject * object)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "finalize");

  kms_plate_detector_release_images (platedetector);
  TessBaseAPIDelete (platedetector->handle);

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
kms_plate_detector_plate_store_initialization (KmsPlateDetector * platedetector)
{
  int t;
  char nullPlate[10] = NULL_PLATE;

  for (t = 0; t < NUM_PLATES_SAMPLES; t++) {
    strcpy (platedetector->plateStore[t], nullPlate);
  }
  platedetector->storePosition = 0;
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
kms_plate_detector_initialize_images (KmsPlateDetector * platedetector,
    GstVideoFrame * frame)
{
  if (platedetector->cvImage == NULL) {
    kms_plate_detector_create_images (platedetector, frame);
  } else if ((platedetector->cvImage->width != frame->info.width)
      || (platedetector->cvImage->height != frame->info.height)) {

    kms_plate_detector_release_images (platedetector);
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
  kernel = cvCreateStructuringElementEx (21, 3, 11, 2, CV_SHAPE_RECT, 0);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_preprocessing_method_two (KmsPlateDetector * platedetector)
{
  IplConvKernel *kernel;
  IplImage *edgesAux;

  cvCanny (platedetector->edges, platedetector->edges, 70, 150, 3);
  cvSobel (platedetector->edges, platedetector->edges, 2, 0, 3);
  kernel = cvCreateStructuringElementEx (11, 3, 6, 2, CV_SHAPE_RECT, 0);
  cvNot (platedetector->edges, platedetector->edges);
  cvErode (platedetector->edges, platedetector->edges, kernel, 1);
  cvNot (platedetector->edges, platedetector->edges);
  cvReleaseStructuringElement (&kernel);
  edgesAux = cvCreateImage (cvSize (platedetector->cvImage->width,
          platedetector->cvImage->height), platedetector->cvImage->depth, 1);

  cvSobel (platedetector->edges, edgesAux, 0, 2, 3);
  kernel = cvCreateStructuringElementEx (15, 3, 8, 2, CV_SHAPE_RECT, 0);
  cvDilate (edgesAux, edgesAux, kernel, 1);
  cvSub (platedetector->edges, edgesAux, platedetector->edges, 0);

  cvSobel (platedetector->edges, edgesAux, 2, 0, 3);
  cvReleaseStructuringElement (&kernel);
  kernel = cvCreateStructuringElementEx (3, 15, 2, 8, CV_SHAPE_RECT, 0);
  cvDilate (edgesAux, edgesAux, kernel, 1);
  cvSub (platedetector->edges, edgesAux, platedetector->edges, 0);
  cvReleaseStructuringElement (&kernel);
  kernel = cvCreateStructuringElementEx (7, 3, 4, 2, CV_SHAPE_RECT, 0);
  cvDilate (platedetector->edges, platedetector->edges, kernel, 1);
  cvReleaseImage (&edgesAux);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_preprocessing_method_three (KmsPlateDetector * platedetector)
{
  cvCanny (platedetector->edges, platedetector->edges, 70, 150, 3);
}

static void
kms_plate_detector_create_little_contour_mask (KmsPlateDetector * platedetector)
{
  CvSeq *contoursCharacters = 0;
  IplConvKernel *kernel;
  CvMemStorage *memCharacters;

  memCharacters = cvCreateMemStorage (0);

  cvSetZero (platedetector->characterContoursMask);
  cvFindContours (platedetector->edges, memCharacters, &contoursCharacters,
      sizeof (CvContour), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contoursCharacters != 0;
      contoursCharacters = contoursCharacters->h_next) {
    CvBox2D fitRect = cvMinAreaRect2 (contoursCharacters, NULL);
    CvRect detectedRect;
    float proportionWidthHeight;
    int contourArea;

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

    if ((contourArea > MIN_CHARACTER_CONTOUR_AREA - 15) &&
        (contourArea < MAX_CHARACTER_CONTOUR_AREA)
        && (abs (proportionWidthHeight - CHARACTER_IDEAL_PROPORTION) <
            MAX_DIF_CHARACTER_PROPORTIONS)) {
      cvDrawContours (platedetector->characterContoursMask, contoursCharacters,
          WHITE, WHITE, -1, 1, 8, cvPoint (0, 0));
    }
  }
  kernel = cvCreateStructuringElementEx (31, 1, 16, 0, CV_SHAPE_RECT, 0);
  cvDilate (platedetector->characterContoursMask,
      platedetector->characterContoursMask, kernel, 1);
  cvClearMemStorage (memCharacters);
  cvReleaseMemStorage (&memCharacters);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_chop_char (char *s)
{
  s[strcspn (s, "\n")] = '\0';
}

static void
kms_plate_detector_rotate_image (IplImage * image,
    float angle, CvRect detectedRect)
{
  IplImage *rotatedImage = cvCreateImage (cvSize (image->width, image->height),
      image->depth, image->nChannels);
  CvPoint2D32f center;
  CvMat *mapMatrix;

  center.x = detectedRect.x;
  center.y = detectedRect.y;
  mapMatrix = cvCreateMat (2, 3, CV_32FC1);
  cv2DRotationMatrix (center, angle, 1.0, mapMatrix);
  cvWarpAffine (image, rotatedImage, mapMatrix, CV_INTER_LINEAR +
      CV_WARP_FILL_OUTLIERS, cvScalarAll (0));
  cvCopy (rotatedImage, image, 0);
  cvReleaseImage (&rotatedImage);
  cvReleaseMat (&mapMatrix);
}

static void
kms_plate_detector_adaptive_threshold (IplImage * src, IplImage * srcAux)
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

static int
kms_plate_detector_median (int plateStore[][7], int platesCounter)
{
  gboolean flag = FALSE;
  int counter = 0;
  int counter2 = 0;
  int counter3 = 0;
  int number = 0;
  int number2 = 0;
  int plateStore2[platesCounter][2];

  for (counter = 0; counter < platesCounter; counter++) {
    if (plateStore[counter][5] == 1) {
      plateStore2[counter3][1] = plateStore[counter][3];
      plateStore2[counter3][0] = counter;
      counter3++;
    }
  }

  for (counter = counter3 - 1; counter >= 0 && flag == FALSE; counter--) {
    flag = TRUE;
    for (counter2 = 0; counter2 < counter; counter2++) {
      if (plateStore2[counter2][1] <= plateStore2[counter2 + 1][1])
        continue;
      number = plateStore2[counter2][1];
      number2 = plateStore2[counter2][0];
      plateStore2[counter2][1] = plateStore2[counter2 + 1][1];
      plateStore2[counter2][0] = plateStore2[counter2 + 1][0];
      plateStore2[counter2 + 1][1] = number;
      plateStore2[counter2 + 1][0] = number2;
      flag = FALSE;
    }
  }

  if (counter3 <= 0)
    return 0;
  if (counter3 % 2 != 0)
    return plateStore2[(counter3 - 1) / 2][0];

  return plateStore2[(counter3) / 2][0];
}

static void
kms_plate_detector_select_best_characters_contours (int plateStore[][7],
    int numContours, int selectedContourPosition)
{
  int k;
  int difference;

  for (k = 0; k < numContours; k++) {
    if (plateStore[k][5] != 1)
      continue;
    difference = abs (plateStore[k][3] -
        plateStore[selectedContourPosition][3]);
    if (difference < 5) {
      plateStore[k][6] = 1;
    } else {
      plateStore[k][6] = 0;
    }
  }
}

static float
kms_plate_detector_angle_median (float angleStore[], int anglesCounter)
{
  gboolean flag = FALSE;
  int counter = 0;
  int counter2 = 0;
  int number = 0;

  for (counter = anglesCounter - 1; counter > 0 && flag == FALSE; counter--) {
    flag = TRUE;
    for (counter2 = 0; counter2 < counter; counter2++) {
      if (angleStore[counter2] <= angleStore[counter2 + 1])
        continue;
      number = angleStore[counter2];
      angleStore[counter2] = angleStore[counter2 + 1];
      angleStore[counter2 + 1] = number;
      flag = FALSE;
    }
  }

  return angleStore[anglesCounter / 2];
}

static float
kms_plate_detector_calc_rotation_angle (int plateStore[][7], int numContours)
{
  int i, j;
  int angleCounter = 0;
  int counter = 0;

  for (i = 0; i < numContours; i++) {
    for (j = 0; j < numContours; j++) {
      if ((j > i) && (plateStore[j][0] - plateStore[i][0] != 0) &&
          (plateStore[i][5] == 1) && (plateStore[j][5] == 1) &&
          (plateStore[i][6] == 1) && (plateStore[j][6] == 1)) {
        counter++;
      }
    }
  }
  float angleStore[counter];

  for (i = 0; i < numContours; i++) {
    for (j = 0; j < numContours; j++) {
      if ((j > i) && (plateStore[j][0] - plateStore[i][0] != 0) &&
          (plateStore[i][5] == 1) && (plateStore[j][5] == 1) &&
          (plateStore[i][6] == 1) && (plateStore[j][6] == 1)) {
        float angle = atan ((float) (plateStore[j][1] - plateStore[i][1]) /
            (float) (plateStore[j][0] - plateStore[i][0])) * 180 / 3.14;
        angleStore[angleCounter] = (int) angle;
        angleCounter++;
      }
    }
  }

  if (angleCounter > 0)
    return kms_plate_detector_angle_median (angleStore, angleCounter);

  return 0;
}

static gboolean
check_proportion_like_character (int rectangleArea,
    CvRect rect,
    int plateStore[][7],
    int mostSimContPos,
    double heightTolerance, double widthUpTolerance, double widthDownTolerance)
{
  return (((rectangleArea > plateStore[mostSimContPos][2] *
              plateStore[mostSimContPos][3] / 3) &&
          (abs (rect.height - plateStore[mostSimContPos][3]) <
              plateStore[mostSimContPos][3] * heightTolerance) &&
          (rect.width > plateStore[mostSimContPos][2] * widthDownTolerance) &&
          (rectangleArea > 100))
      && (((rect.width < plateStore[mostSimContPos][2] * widthUpTolerance)
              || (rect.width > plateStore[mostSimContPos][2] * 2))
          && (rect.width <
              plateStore[mostSimContPos][2] * widthUpTolerance * 2)));
}

static int
kms_plate_detector_find_charac_cont (IplImage * plateInterpolatedAux1,
    CvRect rect,
    int plateStore[][7],
    int finalPlateStore[][7],
    int mostSimContPos,
    double heightTolerance, double widthUpTolerance, double widthDownTolerance)
{

  int counter = 0;
  CvMemStorage *memCharacters;
  CvSeq *contoursCharacters = 0;

  memCharacters = cvCreateMemStorage (0);
  cvFindContours (plateInterpolatedAux1, memCharacters, &contoursCharacters,
      sizeof (CvContour), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contoursCharacters != 0;
      contoursCharacters = contoursCharacters->h_next) {
    float rectangleArea = rect.width * rect.height;
    CvRect rect = cvBoundingRect (contoursCharacters, 0);

    if (!check_proportion_like_character (rectangleArea, rect, plateStore,
            mostSimContPos, heightTolerance, widthUpTolerance,
            widthDownTolerance))
      continue;
    cvRectangle (plateInterpolatedAux1, cvPoint (rect.x, rect.y),
        cvPoint (rect.x + rect.width, rect.y + rect.height), cvScalar (0, 0,
            0, 0), 1, 8, 0);
    finalPlateStore[counter][0] = rect.x;
    finalPlateStore[counter][1] = rect.y;
    finalPlateStore[counter][2] = rect.width;
    finalPlateStore[counter][3] = rect.height;
    if ((rect.width > plateStore[mostSimContPos][2] * 2) &&
        (rect.width < plateStore[mostSimContPos][2] * widthUpTolerance * 2)) {
      finalPlateStore[counter][4] = 1;
    } else {
      finalPlateStore[counter][4] = 0;
    }
    counter++;
  }
  cvClearMemStorage (memCharacters);
  cvReleaseMemStorage (&memCharacters);

  return counter;
}

static void
kms_plate_detector_clear_edges (IplImage * plateInterpolatedAux1,
    CvRect rect, int plateStore[][7], int mostSimContPos)
{
  int i, j;
  uint8_t *aux2;
  uint8_t *aux = (uint8_t *) plateInterpolatedAux1->imageData;

  for (j = 0; j < plateInterpolatedAux1->height; j++) {
    aux2 = aux;
    for (i = 0; i < plateInterpolatedAux1->width; i++) {
      if ((j < plateStore[mostSimContPos][1] - 1) && (j >
              plateStore[mostSimContPos][1] + plateStore[mostSimContPos][3] +
              1))
        *aux2 = 0;
      aux2++;
    }
    aux += plateInterpolatedAux1->widthStep;
  }
}

static void
kms_plate_detector_sort_store (int finalPlateStore[][7], int numContours)
{
  int finalPlateStoreAux[1][7];
  int c1, c2, d;

  for (c1 = 0; c1 < numContours - 1; c1++) {
    for (c2 = 0; c2 < numContours - 1; c2++) {
      if (finalPlateStore[c2][0] <= finalPlateStore[c2 + 1][0])
        continue;
      for (d = 0; d < 7; d++) {
        finalPlateStoreAux[0][d] = finalPlateStore[c2][d];
        finalPlateStore[c2][d] = finalPlateStore[c2 + 1][d];
        finalPlateStore[c2 + 1][d] = finalPlateStoreAux[0][d];
      }
    }
  }
}

static gboolean
check_is_contour_into_contour (int finalPlateStore[][7], int c1)
{
  return ((finalPlateStore[c1 + 1][0] > finalPlateStore[c1][0]) &&
      (finalPlateStore[c1 + 1][0] < (finalPlateStore[c1][0] +
              finalPlateStore[c1][2])) && (finalPlateStore[c1 + 1][0] +
          finalPlateStore[c1 + 1][2] < finalPlateStore[c1][0] +
          finalPlateStore[c1][2]) && (finalPlateStore[c1 + 1][1] >
          finalPlateStore[c1][1]) && (finalPlateStore[c1 + 1][1] +
          finalPlateStore[c1 + 1][3] < finalPlateStore[c1][1] +
          finalPlateStore[c1][3]));
}

static gboolean
check_is_split_character (int plateStore[][7],
    int finalPlateStore[][7], int mostSimContPos, int c1)
{
  return ((finalPlateStore[c1][4] != 1) &&
      (finalPlateStore[c1][2] < plateStore[mostSimContPos][2] * 0.7) &&
      (finalPlateStore[c1 + 1][2] < plateStore[mostSimContPos][2] * 0.7) &&
      (finalPlateStore[c1][2] + finalPlateStore[c1 + 1][2] <
          plateStore[mostSimContPos][2] * 1.2) &&
      (finalPlateStore[c1 + 1][4] != 1) &&
      (abs (finalPlateStore[c1][0] - finalPlateStore[c1 + 1][0])
          < plateStore[mostSimContPos][2] * 0.8));
}

static void
kms_plate_detector_simplify_store (KmsPlateDetector * platedetector,
    IplImage * plateInterpolatedAux1,
    IplImage * plateInterpolatedAux2,
    int finalPlateStore[][7],
    int numContours, int plateStore[][7], int mostSimContPos)
{
  int c1;

  for (c1 = 0; c1 < numContours - 1; c1++) {
    if (check_is_contour_into_contour (finalPlateStore, c1)) {
      finalPlateStore[c1 + 1][4] = 1;
    }
  }

  for (c1 = 0; c1 < numContours - 1; c1++) {
    if ((finalPlateStore[c1][4] == 1) ||
        (finalPlateStore[c1][1] <= plateStore[mostSimContPos][2] * 2))
      continue;
    finalPlateStore[c1 + 1][4] = 1;
  }

  for (c1 = 0; c1 < numContours - 1; c1++) {
    if (!check_is_split_character (plateStore, finalPlateStore,
            mostSimContPos, c1))
      continue;
    finalPlateStore[c1][2] = abs (finalPlateStore[c1 + 1][0] +
        finalPlateStore[c1 + 1][2] - finalPlateStore[c1][0]);
    finalPlateStore[c1 + 1][4] = 1;
    cvErode (plateInterpolatedAux2, plateInterpolatedAux2, 0, 2);
    cvCanny (plateInterpolatedAux2, plateInterpolatedAux2, 210, 120, 3);
    cvDilate (plateInterpolatedAux2, plateInterpolatedAux2, 0, 3);
  }
}

static gboolean
kms_plate_detector_check_is_plate (KmsPlateDetector * platedetector,
    int finalPlateStore[][7],
    int platesCounter2, int plateStore[][7], int mostSimContPos)
{
  int c;
  int medPosY = 0;
  int medHeight = 0;
  int counter = 0;

  medPosY = plateStore[mostSimContPos][1];
  for (c = 0; c < platesCounter2; c++) {
    if (finalPlateStore[c + 1][4] == 1)
      continue;

    if (abs (medPosY - finalPlateStore[c][1]) < 3) {
      medHeight = medHeight + finalPlateStore[c][3];
      counter++;
    }
  }

  if (counter > 0) {
    medHeight = medHeight / counter;
  } else {
    medHeight = 1000;
  }

  return (counter > 5) && (medHeight > 7 * platedetector->resizeFactor);
}

static void
kms_plate_detector_extend_character_rois (IplImage * plateInterpolated,
    int finalPlateStore[][7], int characters, int margin)
{
  int d;

  for (d = 0; d < characters; d++) {
    if (finalPlateStore[d][0] - margin > 0) {
      finalPlateStore[d][0] = finalPlateStore[d][0] - margin;
    }
    if (finalPlateStore[d][1] - margin > 0) {
      finalPlateStore[d][1] = finalPlateStore[d][1] - margin;
    }
    if (finalPlateStore[d][0] + finalPlateStore[d][2] +
        margin < plateInterpolated->width) {
      finalPlateStore[d][2] = finalPlateStore[d][2] + margin;
    }
    if (finalPlateStore[d][1] + finalPlateStore[d][3] +
        margin < plateInterpolated->height) {
      finalPlateStore[d][3] = finalPlateStore[d][3] + margin;
    }
  }
}

static int
kms_plate_detector_extract_plate_space_position (int finalPlateStore[][7],
    int characters, float smallSpaceProportion)
{
  int d;
  int maxDistance = 0;
  int posMaxDistance = 0;

  for (d = 3; d < characters - 3; d++) {
    int distance = abs (finalPlateStore[d][0] +
        finalPlateStore[d][2] - finalPlateStore[d + 1][0]);
    if (distance > maxDistance) {
      maxDistance = distance;
      posMaxDistance = finalPlateStore[d][0] + finalPlateStore[d][2] + 5;
    }
  }

  return posMaxDistance;
}

static void
kms_plate_detector_extract_final_plate (KmsPlateDetector * platedetector)
{
  int f, g, h;
  int longString = strlen (platedetector->finalPlate);
  char stabilizedPlate[longString];
  int characterMatches = 0;
  int r;

  for (f = 0; f < longString; f++) {
    char mostRecurrentCharacter = '-';
    int recurrentCounter = 0;
    char selectedCharacter = '-';
    int matchesCounter = 0;

    for (g = 0; g < NUM_PLATES_SAMPLES; g++) {
      selectedCharacter = platedetector->plateStore[g][f];
      for (h = 0; (h < NUM_PLATES_SAMPLES); h++) {
        char characerToCompare = platedetector->plateStore[h][f];

        if ((selectedCharacter == characerToCompare)) {
          matchesCounter++;
        }
      }

      if ((matchesCounter > recurrentCounter) && (selectedCharacter != '-')) {
        recurrentCounter = matchesCounter;
        mostRecurrentCharacter = selectedCharacter;
      }
      matchesCounter = 0;

    }
    stabilizedPlate[f] = mostRecurrentCharacter;
  }

  for (r = 0; r < NUM_PLATE_CHARACTERS; r++) {
    if ((platedetector->previousFinalPlate[r] != stabilizedPlate[r])) {
      characterMatches++;
    }
  }

  if ((characterMatches > MAX_NUM_DIF_CHARACTERS)) {
    platedetector->plateRepetition = 1;
    platedetector->sendPlateEvent = TRUE;
    strcpy (platedetector->previousFinalPlate, platedetector->finalPlate);
  } else if (characterMatches == 0) {
    platedetector->plateRepetition++;
  }
  characterMatches = 0;

  if ((platedetector->plateRepetition > NUM_ACCUMULATED_PLATES)
      && (platedetector->sendPlateEvent == TRUE)) {
    platedetector->sendPlateEvent = FALSE;
    platedetector->plateRepetition = 0;
    GST_DEBUG ("NEW PLATE: %s", platedetector->previousFinalPlate);
    GST_DEBUG ("TODO: send event to the bus");
  }

  if (platedetector->show_debug_info == TRUE) {
    cvRectangle (platedetector->cvImage,
        cvPoint (platedetector->cvImage->width / 2 - 95,
            platedetector->cvImage->height - 65),
        cvPoint (platedetector->cvImage->width / 2 + 165,
            platedetector->cvImage->height - 35), WHITE, -2, 8, 0);
    cvPutText (platedetector->cvImage, stabilizedPlate,
        cvPoint (platedetector->cvImage->width / 2 - 90,
            platedetector->cvImage->height - 40),
        &platedetector->bigFont, cvScalar (0, 0, 0, 0));
  }
  GST_DEBUG ("STABILIZED PLATE: %s", stabilizedPlate);
}

static void
kms_plate_detector_clean_character (IplImage * imAux1, IplImage * imAux2)
{

  int counter = 0;
  int i, j;
  uint8_t *aux;
  uint8_t *aux2;
  uint8_t *auxContours;
  uint8_t *auxContours2;
  CvMemStorage *memCharacters;
  CvSeq *contoursCharacters = 0;
  IplImage *contours = cvCreateImage (cvSize (imAux2->width,
          imAux2->height),
      imAux2->depth,
      imAux2->nChannels);

  memCharacters = cvCreateMemStorage (0);

  cvCopy (imAux2, contours, 0);
  cvCanny (contours, contours, 70, 150, 3);
  cvRectangle (contours,
      cvPoint (0, 0), cvPoint (imAux2->width, imAux2->height), WHITE, 1, 8, 0);
  cvFindContours (contours, memCharacters, &contoursCharacters,
      sizeof (CvContour), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contoursCharacters != 0;
      contoursCharacters = contoursCharacters->h_next) {

    CvRect rect = cvBoundingRect (contoursCharacters, 0);

    if ((rect.width < imAux1->width * 0.7) ||
        (rect.height < imAux1->height * 0.7))
      continue;

    counter++;
    cvSetZero (contours);
    cvDrawContours (contours, contoursCharacters, WHITE, WHITE,
        0, CV_FILLED, 8, cvPoint (0, 0));
    cvNot (contours, contours);
  }

  aux = (uint8_t *) imAux2->imageData;
  auxContours = (uint8_t *) contours->imageData;

  for (j = 0; j < imAux2->height; j++) {
    aux2 = aux;
    auxContours2 = auxContours;
    for (i = 0; i < imAux2->width; i++) {
      if (*auxContours2 == 255)
        *aux2 = 255;
      aux2++;
      auxContours2++;
    }
    aux += imAux2->widthStep;
    auxContours += contours->widthStep;
  }

  cvReleaseImage (&contours);
  cvClearMemStorage (memCharacters);
  cvReleaseMemStorage (&memCharacters);
}

static void
kms_plate_detector_select_tesseract_whitelist (KmsPlateDetector * platedetector,
    int d, TessBaseAPI * handle, int initialPosition, int *numbersCounter)
{
  if (d < initialPosition) {
    TessBaseAPISetVariable (platedetector->handle,
        "tessedit_char_whitelist", PLATE_LETTERS);
  } else if ((d >= initialPosition) && (*numbersCounter < 4)) {
    TessBaseAPISetVariable (platedetector->handle,
        "tessedit_char_whitelist", PLATE_NUMBERS);
    *numbersCounter = *numbersCounter + 1;
  } else {
    TessBaseAPISetVariable (platedetector->handle,
        "tessedit_char_whitelist", PLATE_LETTERS);
  }
}

static int
kms_plate_detector_first_character_position (int finalPlateStore[][7],
    int *characters, int *spacePositionX, int *spaceFound)
{
  int d;
  int initialPosition = 0;

  for (d = 0; d < *characters; d++) {
    if ((finalPlateStore[d][0] > *spacePositionX) && (*spaceFound == 0)) {
      initialPosition = d - 4;
      *spaceFound = 1;
      if (initialPosition < 0) {
        initialPosition = 0;
      }
    }
  }

  return initialPosition;
}

static void
kms_plate_detector_show_original_characters (KmsPlateDetector * platedetector,
    int finalPlateStore[][7],
    IplImage * imAux, IplImage * imAuxRGB, int spacePositionX, int d)
{
  if ((finalPlateStore[d][0] + finalPlateStore[d][2] <
          platedetector->cvImage->width) && (finalPlateStore[d][1] + 40 +
          finalPlateStore[d][3] < platedetector->cvImage->height)) {
    cvSetImageROI (platedetector->cvImage,
        cvRect (finalPlateStore[d][0],
            finalPlateStore[d][1] + 40,
            finalPlateStore[d][2], finalPlateStore[d][3]));
    cvCvtColor (imAux, imAuxRGB, CV_GRAY2BGR);
    cvCopy (imAuxRGB, platedetector->cvImage, 0);
    cvResetImageROI (platedetector->cvImage);
  }

  if (d == 0) {
    cvRectangle (platedetector->cvImage, cvPoint (spacePositionX,
            finalPlateStore[d][1] + 30),
        cvPoint (spacePositionX + 3, 2 * finalPlateStore[d][1] + 40 +
            finalPlateStore[d][3]), RED, -2, 8, 0);
  }
}

static void
kms_plate_detector_show_proccesed_characters (KmsPlateDetector * platedetector,
    int finalPlateStore[][7], IplImage * imAuxRGB2, int d)
{
  if ((finalPlateStore[d][0] + d * EDGE_MARGIN + imAuxRGB2->width <
          platedetector->cvImage->width) && (2 * finalPlateStore[d][1] + 120
          + imAuxRGB2->height < platedetector->cvImage->height)) {
    cvSetImageROI (platedetector->cvImage,
        cvRect (finalPlateStore[d][0] +
            d * EDGE_MARGIN, 2 * finalPlateStore[d][1] + 120,
            imAuxRGB2->width, imAuxRGB2->height));
    cvCopy (imAuxRGB2, platedetector->cvImage, 0);
    cvResetImageROI (platedetector->cvImage);
  }
}

static void
kms_plate_detector_show_ocr_results (KmsPlateDetector * platedetector,
    int position, char *ocrResultAux1,
    char *ocrResultAux2, char *textConf1, char *textConf2)
{
  cvPutText (platedetector->cvImage, ocrResultAux1,
      cvPoint (10, +platedetector->cvImage->height -
          200 + 20 * position),
      &platedetector->littleFont, cvScalar (0, 0, 255, 0));
  cvPutText (platedetector->cvImage, textConf1,
      cvPoint (40, +platedetector->cvImage->height -
          200 + 20 * position), &platedetector->littleFont, cvScalar (0, 0, 255,
          0));

  cvPutText (platedetector->cvImage, ocrResultAux2,
      cvPoint (200, +platedetector->cvImage->height -
          200 + 25 * position), &platedetector->littleFont, cvScalar (255, 0, 0,
          0));
  cvPutText (platedetector->cvImage, textConf2, cvPoint (240,
          +platedetector->cvImage->height - 200 + 25 * position),
      &platedetector->littleFont, cvScalar (255, 0, 0, 0));
}

static void
kms_plate_detector_insert_plate_in_store (KmsPlateDetector * platedetector)
{
  int characterMatches = 0;
  int r;

  for (r = 0; r < NUM_PLATE_CHARACTERS; r++) {
    if (platedetector->previousFinalPlate[r] == platedetector->finalPlate[r]) {
      characterMatches++;
    }
  }

  if ((characterMatches < 6)) {
    kms_plate_detector_plate_store_initialization (platedetector);
    GST_DEBUG ("new plate detected...");
  }

  if (platedetector->storePosition < NUM_PLATES_SAMPLES) {
    strcpy (platedetector->plateStore[platedetector->storePosition],
        platedetector->finalPlate);
    platedetector->storePosition++;
  } else if (platedetector->storePosition == NUM_PLATES_SAMPLES) {
    int t;

    for (t = NUM_PLATES_SAMPLES - 1; t > 0; t--) {
      strcpy (platedetector->plateStore[t], platedetector->plateStore[t - 1]);
    }
    strcpy (platedetector->plateStore[0], platedetector->finalPlate);
  }
}

static gboolean
kms_plate_detector_format_plate (KmsPlateDetector * platedetector,
    char *finalPlateAux)
{
  gboolean validPlate = FALSE;
  int letter1 = 0;
  int letter2 = 0;
  int letter3 = 0;
  int letter4 = 0;
  int letter5 = 0;
  int r;

  int missingCharactersCounter = 0;

  for (r = 2; r < 7; r++) {
    if ((finalPlateAux[r] == '-') || (finalPlateAux[r] == ' ')
        || (finalPlateAux[r] == 0)) {
      missingCharactersCounter++;
    }
  }

  if (missingCharactersCounter == 0) {
    if (((int) finalPlateAux[0] > 64) && ((int) finalPlateAux[0] < 91)) {
      letter1 = 1;
    }
    if (((int) finalPlateAux[1] > 64) && ((int) finalPlateAux[1] < 91)) {
      letter2 = 1;
    }
    if (((int) finalPlateAux[6] > 64) && ((int) finalPlateAux[6] < 91)) {
      letter3 = 1;
    }
    if (((int) finalPlateAux[7] > 64) && ((int) finalPlateAux[7] < 91)) {
      letter4 = 1;
    }
    if (((int) finalPlateAux[8] > 64) && ((int) finalPlateAux[8] < 91)) {
      letter5 = 1;
    }

    if ((letter3 == 1) && (letter4 == 1) && (letter5 == 1)) {
      finalPlateAux[0] = '-';
      finalPlateAux[1] = '-';
      validPlate = TRUE;
    } else if ((letter3 + letter4 > 1) && (letter1 + letter2 > 0)
        && (letter2 == 1)) {
      validPlate = TRUE;
    } else {
      validPlate = FALSE;
    }
  }

  if (validPlate) {
    for (r = 0; r < NUM_PLATE_CHARACTERS; r++) {
      platedetector->finalPlate[r] = finalPlateAux[r];
    }
  }

  return validPlate;
}

static void
kms_plate_detector_read_characters (KmsPlateDetector * platedetector,
    IplImage * plateColorRoi,
    IplImage * plateBinRoi,
    IplImage * plateInterpolated,
    IplImage * plateInterpolatedAux2,
    int plateStore[][7],
    int finalPlateStore[][7],
    int characters,
    gboolean checkIsPlate, int spacePositionX, int mostSimContPos)
{
  int d;
  int initialPosition = 0;
  int spaceFound = 0;
  char finalPlateAux[10];
  int numbersCounter = 0;
  int position = 0;
  int validPlate = 0;
  int confidenceRate1;
  char *ocrResultAux1;
  int confidenceRate2;
  char *ocrResultAux2;
  int confidenceRate;
  char textConf[9];
  char textConf1[9];
  char textConf2[9];
  IplImage *imAux;
  IplImage *imAuxRGB;
  IplImage *imAux2;
  IplImage *imAuxRGB2;

  strncpy (finalPlateAux, NULL_PLATE, NUM_PLATE_CHARACTERS);

  if (platedetector->show_debug_info == TRUE) {
    cvRectangle (platedetector->cvImage,
        cvPoint (0, platedetector->cvImage->height - 230),
        cvPoint (400, platedetector->cvImage->height), BLACK, -2, 8, 0);
  }

  initialPosition =
      kms_plate_detector_first_character_position (finalPlateStore, &characters,
      &spacePositionX, &spaceFound);
  for (d = 0; d < characters; d++) {
    if (finalPlateStore[d][4] == 1)
      continue;

    kms_plate_detector_select_tesseract_whitelist (platedetector, d,
        platedetector->handle, initialPosition, &numbersCounter);
    imAux = cvCreateImage (cvSize (finalPlateStore[d][2],
            finalPlateStore[d][3]),
        plateInterpolated->depth, plateInterpolated->nChannels);
    imAuxRGB = cvCreateImage (cvSize (finalPlateStore[d][2],
            finalPlateStore[d][3]),
        platedetector->cvImage->depth, platedetector->cvImage->nChannels);

    if ((finalPlateStore[d][0] + finalPlateStore[d][2] <
            plateInterpolated->width) && (finalPlateStore[d][1] +
            finalPlateStore[d][3] < plateInterpolated->height)) {
      cvSetImageROI (plateInterpolated, cvRect (finalPlateStore[d][0],
              finalPlateStore[d][1],
              finalPlateStore[d][2], finalPlateStore[d][3]));
      cvCopy (plateInterpolated, imAux, 0);
    }

    if (platedetector->show_debug_info == TRUE) {
      kms_plate_detector_show_original_characters (platedetector,
          finalPlateStore, imAux, imAuxRGB, spacePositionX, d);
    }

    if ((finalPlateStore[d][0] + finalPlateStore[d][2] <
            plateInterpolatedAux2->width) && (finalPlateStore[d][1] +
            finalPlateStore[d][3] < plateInterpolatedAux2->height)) {
      cvSetImageROI (plateInterpolatedAux2,
          cvRect (finalPlateStore[d][0],
              finalPlateStore[d][1],
              finalPlateStore[d][2], finalPlateStore[d][3]));
      cvCopy (plateInterpolatedAux2, imAux, 0);
      cvResetImageROI (plateInterpolatedAux2);
    }

    imAux2 = cvCreateImage (cvSize (imAux->width + 2 * EDGE_MARGIN,
            imAux->height + EDGE_MARGIN),
        plateInterpolated->depth, plateInterpolated->nChannels);
    imAuxRGB2 =
        cvCreateImage (cvSize (imAux->width + 2 * EDGE_MARGIN,
            imAux->height + EDGE_MARGIN),
        platedetector->cvImage->depth, platedetector->cvImage->nChannels);

    cvSetZero (imAux2);
    cvNot (imAux2, imAux2);
    if ((EDGE_MARGIN + imAux->width < imAux2->width) &&
        (EDGE_MARGIN / 2 + imAux->height < imAux2->height)) {
      cvSetImageROI (imAux2, cvRect (EDGE_MARGIN, EDGE_MARGIN / 2,
              imAux->width, imAux->height));
      cvCopy (imAux, imAux2, 0);
      cvResetImageROI (imAux2);
    }

    if (platedetector->resizeFactor == RESIZE_FACTOR_3) {
      cvDilate (imAux2, imAux2, 0, 1);
      cvSmooth (imAux2, imAux2, CV_MEDIAN, 3, 0, 0, 0);
      cvSmooth (imAux2, imAux2, CV_MEDIAN, 3, 0, 0, 0);
    }

    kms_plate_detector_clean_character (imAux, imAux2);
    cvCvtColor (imAux2, imAuxRGB2, CV_GRAY2BGR);

    if (platedetector->show_debug_info == TRUE) {
      kms_plate_detector_show_proccesed_characters (platedetector,
          finalPlateStore, imAuxRGB2, d);
    }

    TessBaseAPISetImage (platedetector->handle,
        (unsigned char *) imAux->imageData, imAux->width,
        imAux->height, (imAux->depth / 8) / imAux->nChannels, imAux->widthStep);
    ocrResultAux1 = TessBaseAPIGetUTF8Text (platedetector->handle);
    kms_plate_detector_chop_char (ocrResultAux1);
    confidenceRate1 = TessBaseAPIMeanTextConf (platedetector->handle);

    TessBaseAPIClear (platedetector->handle);

    sprintf (textConf1, "Conf:%d", (int) confidenceRate1);

    TessBaseAPISetImage (platedetector->handle,
        (unsigned char *) imAux2->imageData, imAux2->width,
        imAux2->height, (imAux2->depth / 8) / imAux2->nChannels,
        imAux2->widthStep);

    ocrResultAux2 = TessBaseAPIGetUTF8Text (platedetector->handle);

    kms_plate_detector_chop_char (ocrResultAux2);
    confidenceRate2 = TessBaseAPIMeanTextConf (platedetector->handle);

    TessBaseAPIClear (platedetector->handle);

    sprintf (textConf2, "Conf:%d", (int) confidenceRate2);

    if (confidenceRate1 > confidenceRate2) {
      confidenceRate = confidenceRate1;
      textConf[0] = ocrResultAux1[0];
    } else {
      confidenceRate = confidenceRate2;
      textConf[0] = ocrResultAux2[0];
    }

    if (confidenceRate > MIN_OCR_CONFIDENCE_RATE) {
      if (position < NUM_PLATE_CHARACTERS) {
        finalPlateAux[position + 2 - initialPosition] = textConf[0];
      }
    } else {
      finalPlateAux[position + 2 - initialPosition] = '-';
    }

    if (platedetector->show_debug_info == TRUE) {
      kms_plate_detector_show_ocr_results (platedetector, position,
          ocrResultAux1, ocrResultAux2, textConf1, textConf2);
    }
    kms_plate_detector_chop_char (finalPlateAux);
    position++;

    cvReleaseImage (&imAux);
    cvReleaseImage (&imAux2);
    cvReleaseImage (&imAuxRGB);
    cvReleaseImage (&imAuxRGB2);
  }

  validPlate = kms_plate_detector_format_plate (platedetector, finalPlateAux);

  if (validPlate) {
    kms_plate_detector_insert_plate_in_store (platedetector);
  }
}

static void
kms_plate_detector_extract_potential_plate (CvSeq * contoursPlates,
    double *contourFitArea, CvRect * detectedRect, float *PlateProportion)
{
  CvBox2D fitRect = cvMinAreaRect2 (contoursPlates, NULL);

  *contourFitArea = cvContourArea (contoursPlates, CV_WHOLE_SEQ, 0);

  if (abs (fitRect.angle) < 45) {
    detectedRect->width = fitRect.size.width;
    detectedRect->height = fitRect.size.height;
  } else {
    detectedRect->width = fitRect.size.height;
    detectedRect->height = fitRect.size.width;
    fitRect.angle = -(fitRect.angle + 90);
  }
  detectedRect->x = fitRect.center.x - detectedRect->width / 2;
  detectedRect->y = fitRect.center.y - detectedRect->height / 2;

  if ((detectedRect->height > 0) && (detectedRect->width > 0)) {
    *PlateProportion = detectedRect->width / detectedRect->height;
  } else {
    *PlateProportion = 100;
  }
}

static void
kms_plate_detector_expand_potential_plate_rect (KmsPlateDetector *
    platedetector, CvRect * rect, float expandRateWidth, float expandRateHeight)
{
  if ((rect->x - rect->width * expandRateWidth / 2 > 0) &&
      (rect->y - rect->height * expandRateHeight / 2 > 0) &&
      (rect->x + rect->width + rect->width * expandRateWidth <
          platedetector->cvImage->width) &&
      (rect->y + rect->height + rect->height * expandRateHeight <
          platedetector->cvImage->height)) {
    rect->x = rect->x - rect->width * expandRateWidth / 2;
    rect->y = rect->y - rect->height * expandRateHeight / 2;
    rect->width = rect->width + rect->width * expandRateWidth;
    rect->height = rect->height + rect->height * expandRateHeight;
  }
}

static void
kms_plate_detector_check_rect_into_margins (KmsPlateDetector * platedetector,
    CvRect * detectedRect)
{
  if (detectedRect->x - detectedRect->width > platedetector->cvImage->width) {
    detectedRect->width = platedetector->cvImage->width - detectedRect->x;
  }
  if (detectedRect->y - detectedRect->height > platedetector->cvImage->height) {
    detectedRect->height = platedetector->cvImage->height - detectedRect->y;
  }
}

static void
kms_plate_detector_preprocessing_images (IplImage * plateROI,
    IplImage * plateBinRoi,
    IplImage * plateInterpolatedAux1,
    IplImage * plateInterpolatedAux2,
    IplImage * plateInterAux1Color, IplImage * plateInterpolated)
{
  cvCvtColor (plateROI, plateBinRoi, CV_BGR2GRAY);
  cvSetZero (plateInterpolatedAux1);
  cvSetZero (plateInterpolatedAux2);
  cvSetZero (plateInterAux1Color);
  cvResize (plateBinRoi, plateInterpolated, CV_INTER_LANCZOS4);
  kms_plate_detector_adaptive_threshold (plateInterpolated,
      plateInterpolatedAux1);
  cvThreshold (plateInterpolatedAux1, plateInterpolatedAux1,
      70, 255, CV_THRESH_OTSU);
  cvSmooth (plateInterpolatedAux1, plateInterpolatedAux1, CV_MEDIAN,
      3, 0, 0, 0);
  cvCopy (plateInterpolatedAux1, plateInterpolatedAux2, 0);
  cvCanny (plateInterpolatedAux1, plateInterpolatedAux1, 210, 120, 3);
  cvCvtColor (plateInterpolatedAux1, plateInterAux1Color, CV_GRAY2BGR);
}

static void
kms_plate_detector_select_preprocessing_type (KmsPlateDetector * platedetector)
{
  if (platedetector->preprocessingType == PREPROCESSING_ONE) {
    kms_plate_detector_preprocessing_method_one (platedetector);
  } else if (platedetector->preprocessingType == PREPROCESSING_TWO) {
    kms_plate_detector_preprocessing_method_two (platedetector);
  } else if (platedetector->preprocessingType == PREPROCESSING_THREE) {
    IplConvKernel *kernel;

    kms_plate_detector_preprocessing_method_three (platedetector);
    kms_plate_detector_create_little_contour_mask (platedetector);
    kernel = cvCreateStructuringElementEx (5, 5, 3, 3, CV_SHAPE_RECT, 0);
    cvMorphologyEx (platedetector->characterContoursMask,
        platedetector->characterContoursMask, NULL, kernel, CV_MOP_CLOSE, 1);
    cvReleaseStructuringElement (&kernel);
    cvCopy (platedetector->characterContoursMask, platedetector->edges, 0);
  }
}

static void
kms_plate_detector_extract_potential_characters (KmsPlateDetector *
    platedetector, CvSeq * contoursCharacters, IplImage * plateInterpolatedAux1,
    IplImage * plateInterAux1Color, int plateStore[][7],
    int finalPlateStore[][7], int *numContours)
{
  int platesCounter = 0;

  for (; contoursCharacters != 0;
      contoursCharacters = contoursCharacters->h_next) {
    CvRect rect = cvBoundingRect (contoursCharacters, 0);
    float rectangleArea;
    float proporcionPatronCharac = CHARACTER_IDEAL_PROPORTION;
    float proporcion1 = DEFAULT_CHARACTER_PROPORTION;

    rectangleArea = rect.width * rect.height;
    plateStore[platesCounter][0] = rect.x;
    plateStore[platesCounter][1] = rect.y;
    plateStore[platesCounter][2] = rect.width;
    plateStore[platesCounter][3] = rect.height;
    plateStore[platesCounter][4] = rectangleArea;

    if ((rect.width != 0)) {
      proporcion1 = (float) rect.height / (float) rect.width;
    }

    if ((fabsf (proporcionPatronCharac - proporcion1) < 0.25)
        && (rectangleArea > 100)) {
      plateStore[platesCounter][5] = 1;
      cvDrawContours (plateInterAux1Color, contoursCharacters, WHITE, WHITE,
          -1, 1, 8, cvPoint (0, 0));
    }
    platesCounter++;
  }
}

static void
kms_plate_detector_rotate_images (IplImage * plateInterAux1Color,
    IplImage * plateInterpolatedAux1,
    IplImage * plateInterpolatedAux2,
    IplImage * plateInterpolated,
    int mostSimContPos, float angle, int plateStore[][7])
{
  kms_plate_detector_rotate_image (plateInterAux1Color, angle,
      cvRect (plateStore[mostSimContPos][0],
          plateStore[mostSimContPos][1],
          plateInterpolatedAux1->width, plateInterpolatedAux1->height));
  kms_plate_detector_rotate_image (plateInterpolatedAux1, angle,
      cvRect (plateStore[mostSimContPos][0],
          plateStore[mostSimContPos][1],
          plateInterpolatedAux1->width, plateInterpolatedAux1->height));
  kms_plate_detector_rotate_image (plateInterpolatedAux2, angle,
      cvRect (plateStore[mostSimContPos][0],
          plateStore[mostSimContPos][1],
          plateInterpolatedAux1->width, plateInterpolatedAux1->height));
  kms_plate_detector_rotate_image (plateInterpolated, angle,
      cvRect (plateStore[mostSimContPos][0],
          plateStore[mostSimContPos][1],
          plateInterpolated->width, plateInterpolated->height));
}

static void
kms_plate_detector_draw_plate_rectang (KmsPlateDetector * platedetector,
    CvRect * rect)
{
  cvRectangle (platedetector->cvImage, cvPoint (rect->x, rect->y),
      cvPoint (rect->x + rect->width, rect->y + rect->height), GREEN, 2, 8, 0);
}

static void
kms_plate_detector_select_character_resize_factor (KmsPlateDetector *
    platedetector, int plateStore[][7], int mostSimContPos)
{
  if ((plateStore[mostSimContPos][2] >= RESIZE_LOW_THRES) &&
      (plateStore[mostSimContPos][2] < RESIZE_HIGH_THRES)) {
    platedetector->resizeFactor = RESIZE_FACTOR_2;
  } else if (plateStore[mostSimContPos][2] < RESIZE_LOW_THRES) {
    platedetector->resizeFactor = RESIZE_FACTOR_3;
  } else {
    platedetector->resizeFactor = RESIZE_FACTOR_1;
  }
}

static int
check_proportions_like_plate (KmsPlateDetector * platedetector,
    int contourBoundArea, double contourFitArea, float PlateProportion)
{
  return ((contourBoundArea > 0) && (contourBoundArea > MIN_PLATE_CONTOUR_AREA)
      && (abs (PLATE_IDEAL_PROPORTION - PlateProportion) <
          MAX_DIF_PLATE_PROPORTIONS)
      && (contourFitArea / contourBoundArea > MAX_DIF_PLATE_RECTANGLES_AREA));
}

static GstFlowReturn
kms_plate_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (filter);
  GstMapInfo info;
  IplConvKernel *kernel;
  CvSeq *contoursPlates = 0;
  CvMemStorage *memPlates;

  memPlates = cvCreateMemStorage (0);

  kms_plate_detector_initialize_images (platedetector, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  platedetector->cvImage->imageData = (char *) info.data;
  cvCvtColor (platedetector->cvImage, platedetector->edges, CV_BGR2GRAY);
  kms_plate_detector_select_preprocessing_type (platedetector);

  cvFindContours (platedetector->edges, memPlates, &contoursPlates,
      sizeof (CvContour), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contoursPlates != 0; contoursPlates = contoursPlates->h_next) {

    CvRect detectedRect;
    CvRect rect;
    float PlateProportion;
    double contourFitArea;
    int contourBoundArea;
    IplImage *plateROI;
    IplImage *plateBinRoi;
    IplImage *plateInterpolated;
    IplImage *plateInterpolatedAux1;
    IplImage *plateInterAux1Color;
    IplImage *plateInterpolatedAux2;
    IplImage *plateColorRoi;
    IplImage *plateContour;
    int numContours;
    int mostSimContPos;
    float angle;
    int numOfCharacters;
    gboolean checkIsPlate;
    int spacePositionX;
    CvMemStorage *memCharacters;
    CvSeq *contoursCharacters;

    kms_plate_detector_extract_potential_plate (contoursPlates, &contourFitArea,
        &detectedRect, &PlateProportion);
    rect = cvBoundingRect (contoursPlates, 0);
    contourBoundArea = detectedRect.width * detectedRect.height;

    if (!check_proportions_like_plate (platedetector, contourBoundArea,
            contourFitArea, PlateProportion))
      continue;
    kms_plate_detector_expand_potential_plate_rect (platedetector,
        &rect, PLATE_WIDTH_EXPAND_RATE, PLATE_HEIGHT_EXPAND_RATE);
    kms_plate_detector_check_rect_into_margins (platedetector, &detectedRect);

    plateROI = cvCreateImage (cvSize (rect.width, rect.height),
        IPL_DEPTH_8U, 3);

    cvSetImageROI (platedetector->cvImage, rect);
    cvCopy (platedetector->cvImage, plateROI, 0);

    plateBinRoi = cvCreateImage (cvGetSize (plateROI), plateROI->depth, 1);
    plateInterpolated =
        cvCreateImage (cvSize (platedetector->resizeFactor *
            plateBinRoi->width,
            platedetector->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 1);
    plateInterpolatedAux1 =
        cvCreateImage (cvSize (platedetector->resizeFactor *
            plateBinRoi->width,
            platedetector->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 1);
    plateInterpolatedAux2 =
        cvCreateImage (cvSize (platedetector->resizeFactor *
            plateBinRoi->width,
            platedetector->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 1);
    plateInterAux1Color =
        cvCreateImage (cvSize (platedetector->resizeFactor *
            plateBinRoi->width,
            platedetector->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 3);
    plateColorRoi = cvCreateImage (cvGetSize (plateROI), plateROI->depth, 3);
    plateContour = cvCreateImage (cvGetSize (plateROI), plateROI->depth, 1);

    kms_plate_detector_preprocessing_images (plateROI, plateBinRoi,
        plateInterpolatedAux1,
        plateInterpolatedAux2, plateInterAux1Color, plateInterpolated);
    contoursCharacters = 0;
    memCharacters = cvCreateMemStorage (0);
    numContours = cvFindContours (plateInterpolatedAux1, memCharacters,
        &contoursCharacters, sizeof (CvContour),
        CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE, cvPoint (0, 0));
    int plateStore[numContours][7];
    int finalPlateStore[numContours][7];

    kms_plate_detector_extract_potential_characters (platedetector,
        contoursCharacters,
        plateInterpolatedAux1,
        plateInterAux1Color, plateStore, finalPlateStore, &numContours);
    cvClearMemStorage (memCharacters);
    cvReleaseMemStorage (&memCharacters);

    mostSimContPos = kms_plate_detector_median (plateStore, numContours);
    kms_plate_detector_select_character_resize_factor (platedetector,
        plateStore, mostSimContPos);

    kms_plate_detector_select_best_characters_contours (plateStore,
        numContours, mostSimContPos);
    angle = kms_plate_detector_calc_rotation_angle (plateStore, numContours);

    kms_plate_detector_rotate_images (plateInterAux1Color,
        plateInterpolatedAux1, plateInterpolatedAux2, plateInterpolated,
        mostSimContPos, angle, plateStore);

    kms_plate_detector_clear_edges (plateInterpolatedAux1, rect,
        plateStore, mostSimContPos);
    numOfCharacters =
        kms_plate_detector_find_charac_cont (plateInterpolatedAux1,
        rect, plateStore, finalPlateStore, mostSimContPos, 0.2, 1.4, 0.25);
    checkIsPlate =
        kms_plate_detector_check_is_plate (platedetector, finalPlateStore,
        numOfCharacters, plateStore, mostSimContPos);

    kms_plate_detector_sort_store (finalPlateStore, numOfCharacters);
    kms_plate_detector_simplify_store (platedetector, plateInterpolatedAux1,
        plateInterpolatedAux2, finalPlateStore,
        numOfCharacters, plateStore, mostSimContPos);
    cvNot (plateInterpolatedAux1, plateInterpolatedAux1);
    cvThreshold (plateInterpolatedAux1, plateInterpolatedAux1, 254,
        255, CV_THRESH_BINARY);
    spacePositionX =
        kms_plate_detector_extract_plate_space_position (finalPlateStore,
        numOfCharacters, 0.3);

    kms_plate_detector_extend_character_rois (plateInterpolated,
        finalPlateStore, numOfCharacters, 2);
    cvResetImageROI (platedetector->cvImage);

    if (numOfCharacters > MIN_NUMBER_CHARACTERS) {
      if (checkIsPlate) {
        kms_plate_detector_read_characters (platedetector, plateColorRoi,
            plateBinRoi, plateInterpolated,
            plateInterpolatedAux2, plateStore,
            finalPlateStore, numOfCharacters,
            checkIsPlate, spacePositionX, mostSimContPos);

        kms_plate_detector_extract_final_plate (platedetector);
        if (platedetector->show_debug_info == TRUE) {
          kms_plate_detector_draw_plate_rectang (platedetector, &rect);
        }
      }
    }

    numOfCharacters = -1;

    cvReleaseImage (&plateROI);
    cvReleaseImage (&plateBinRoi);
    cvReleaseImage (&plateColorRoi);
    cvReleaseImage (&plateContour);
    cvReleaseImage (&plateInterpolated);
    cvReleaseImage (&plateInterpolatedAux1);
    cvReleaseImage (&plateInterpolatedAux2);
    cvReleaseImage (&plateInterAux1Color);
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
  cvReleaseStructuringElement (&kernel);

  return GST_FLOW_OK;
}

gboolean
kms_plate_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLATE_DETECTOR);
}
