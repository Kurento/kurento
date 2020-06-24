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
#include <gst/gst.h>
#include "kmsplatedetector.h"
#include <locale.h>
#include <opencv/cv.h>
#include <opencv/highgui.h>
#include <tesseract/capi.h>

#include <math.h>
#include <stdlib.h> // setenv(), requires POSIX.1-2001: -D_POSIX_C_SOURCE=200112L

// FIXME: Compatibility between OpenCV 2.x and 3.x
#include <opencv2/core/version.hpp>
#if CV_MAJOR_VERSION > 2
int
cvRound (double value)
{
  return (ceil (value));
}
#endif

#define PLUGIN_NAME "platedetector"
#define GREEN CV_RGB (0, 255, 0)
#define BLUE CV_RGB (0, 0, 255)
#define RED CV_RGB (255, 0, 0)
#define WHITE CV_RGB (255, 255, 255)
#define BLACK CV_RGB (0, 0, 0)
#define PLATE_IDEAL_PROPORTION  ((float) 5.0)
#define CHARACTER_IDEAL_PROPORTION2  ((float) 1.67)
#define MIN_CHARACTER_CONTOUR_AREA ((int) 50)
#define MIN_PLATE_CONTOUR_AREA ((int) 1500)
#define MIN_CHAR_CONTOUR_AREA ((int) 100)
#define MAX_DIF_PLATE_PROPORTIONS ((float) 1.8)
#define MAX_DIF_PLATE_RECTANGLES_AREA ((float) 0.5)
#define CHARACTER_IDEAL_PROPORTION ((float) 1.3)
#define MAX_DIF_CHARACTER_PROPORTIONS ((float) 0.5)
#define RESIZE_FACTOR_1 ((float) 1)
#define EDGE_MARGIN ((int) 40)
#define MIN_NUMBER_CHARACTERS ((int) 6)
#define PLATE_WIDTH_EXPAND_RATE ((float) 0.2)
#define PLATE_HEIGHT_EXPAND_RATE ((float) 0.4)
#define MIN_OCR_CONFIDENCE_RATE ((int) 70)
#define NUM_ACCUMULATED_PLATES ((int) 3)
#define MAX_NUM_DIF_CHARACTERS ((int) 0)
#define PREVIOUS_PLATE_INI "*********"
#define NULL_PLATE "---------"
#define DEFAULT_CHARACTER_PROPORTION ((int) 100)
#define PLATE_NUMBERS "0123456789"
#define PLATE_LETTERS "AEIOUBCDFGHJKLMNPQRSTVWXYZ"
#define MIN_CHARACTERS_AMOUNT ((int) 6)
#define MARGIN_3 ((int) 3)
#define MARGIN_10 ((int) 10)
#define MARGIN_30 ((int) 30)
#define MARGIN_40 ((int) 40)
#define MARGIN_120 ((int) 120)
#define MARGIN_200 ((int) 200)
#define MARGIN_240 ((int) 240)

#define PLATEWINDOWPERCENTAGE ((int) 6)
#define KERNELY ((int) 3)
#define PLATE_HEIGHT_SCALE_RATE ((float) 60)

#define TESSERAC_PREFIX_DEFAULT DATAROOTDIR "/" PACKAGE "/"
#define TESSDATA_PREFIX "TESSDATA_PREFIX"

GST_DEBUG_CATEGORY_STATIC (kms_plate_detector_debug_category);
#define GST_CAT_DEFAULT kms_plate_detector_debug_category

#define KMS_PLATE_DETECTOR_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PLATE_DETECTOR,                    \
    KmsPlateDetectorPrivate                     \
  )                                             \
)
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

typedef struct _CharacterData
{
  int x;
  int y;
  int width;
  int height;
  gboolean exclude;
  gboolean similarProportion;
  gboolean isMostSimilar;
} CharacterData;

struct _KmsPlateDetectorPrivate
{
  IplImage *cvImage, *edges, *edgesDilatedMask, *characterContoursMask;
  KmsPlateDetectorPreprocessingType preprocessingType;
  TessBaseAPI *handle;
  char plateStore[NUM_PLATES_SAMPLES][NUM_PLATE_CHARACTERS + 1];
  int storePosition;
  int plateRepetition;
  gboolean sendPlateEvent;
  char finalPlate[NUM_PLATE_CHARACTERS + 1];
  char previousFinalPlate[NUM_PLATE_CHARACTERS + 1];
  char sendPlate[NUM_PLATE_CHARACTERS + 1];
  float resizeFactor;
  CvFont littleFont;
  CvFont bigFont;
  gboolean show_debug_info;
  float plate_percentage;
  int kernelX;
  int kernelY;
  int frameWidth;
};

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  PROP_PLATE_WIDTH_PERCENTAGE
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

  g_object_class_install_property (gobject_class, PROP_PLATE_WIDTH_PERCENTAGE,
      g_param_spec_float ("plate-width-percentage", "plate width percentage",
          "define width percentage between window size and plate", 0, 1, 0.25,
          G_PARAM_READWRITE));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlateDetectorPrivate));
}

static void
kms_plate_detector_init (KmsPlateDetector * platedetector)
{
  platedetector->priv = KMS_PLATE_DETECTOR_GET_PRIVATE (platedetector);
  double hScaleLittle = 0.6;
  double vScaleLittle = 0.6;
  int lineWidthLittle = 18;
  double hScaleBig = 1.0;
  double vScaleBig = 1.0;
  int lineWidthBig = 18;
  gint ret_value;

  platedetector->priv->cvImage = NULL;
  platedetector->priv->preprocessingType = PREPROCESSING_ONE;
  platedetector->priv->handle = TessBaseAPICreate ();
  setlocale (LC_NUMERIC, "C");
  setenv (TESSDATA_PREFIX, TESSERAC_PREFIX_DEFAULT, FALSE);
  GST_DEBUG (TESSDATA_PREFIX ": %s", getenv (TESSDATA_PREFIX));
  ret_value =
      TessBaseAPIInit3 (platedetector->priv->handle, getenv (TESSDATA_PREFIX),
      "plateLanguage");

  if (ret_value == -1) {
    GST_ERROR_OBJECT (platedetector,
        "Tesseract OCR training data not found in prefix: '%s'",
        getenv (TESSDATA_PREFIX));
    TessBaseAPIDelete (platedetector->priv->handle);
    platedetector->priv->handle = NULL;

    return;
  }
  TessBaseAPISetPageSegMode (platedetector->priv->handle, PSM_SINGLE_LINE);
  kms_plate_detector_plate_store_initialization (platedetector);
  platedetector->priv->storePosition = 0;
  platedetector->priv->plateRepetition = 0;
  platedetector->priv->sendPlateEvent = FALSE;
  platedetector->priv->resizeFactor = RESIZE_FACTOR_1;
  strncpy (platedetector->priv->previousFinalPlate, PREVIOUS_PLATE_INI,
      NUM_PLATE_CHARACTERS);
  strncpy (platedetector->priv->sendPlate, NULL_PLATE, NUM_PLATE_CHARACTERS);
  cvInitFont (&platedetector->priv->littleFont,
      CV_FONT_HERSHEY_SIMPLEX | CV_FONT_ITALIC, hScaleLittle, vScaleLittle, 0,
      1, lineWidthLittle);
  cvInitFont (&platedetector->priv->bigFont,
      CV_FONT_HERSHEY_SIMPLEX | CV_FONT_ITALIC, hScaleBig, vScaleBig, 0, 1,
      lineWidthBig);
  platedetector->priv->plate_percentage = 0.25;
}

void
kms_plate_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "set_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      platedetector->priv->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_PLATE_WIDTH_PERCENTAGE:
    {
      GST_OBJECT_LOCK (platedetector);
      platedetector->priv->plate_percentage = g_value_get_float (value);

      if (platedetector->priv->frameWidth != 0) {
        platedetector->priv->kernelX =
            (platedetector->priv->plate_percentage *
            platedetector->priv->frameWidth / PLATEWINDOWPERCENTAGE);
        if ((platedetector->priv->kernelX % 2) == 0) {
          platedetector->priv->kernelX = platedetector->priv->kernelX + 1;
        }
      }
      GST_OBJECT_UNLOCK (platedetector);
      break;
    }
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
      g_value_set_boolean (value, platedetector->priv->show_debug_info);
      break;
    case PROP_PLATE_WIDTH_PERCENTAGE:
      GST_OBJECT_LOCK (platedetector);
      g_value_set_float (value, platedetector->priv->plate_percentage);
      GST_OBJECT_UNLOCK (platedetector);
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
  cvReleaseImage (&platedetector->priv->cvImage);
  cvReleaseImage (&platedetector->priv->edges);
  cvReleaseImage (&platedetector->priv->edgesDilatedMask);
  cvReleaseImage (&platedetector->priv->characterContoursMask);
}

void
kms_plate_detector_finalize (GObject * object)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (object);

  GST_DEBUG_OBJECT (platedetector, "finalize");

  kms_plate_detector_release_images (platedetector);
  TessBaseAPIDelete (platedetector->priv->handle);

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
    strcpy (platedetector->priv->plateStore[t], nullPlate);
  }
  platedetector->priv->storePosition = 0;
}

static void
kms_plate_detector_create_images (KmsPlateDetector * platedetector,
    GstVideoFrame * frame)
{
  platedetector->priv->cvImage =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 3);
  platedetector->priv->edges =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  platedetector->priv->edgesDilatedMask =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);
  platedetector->priv->characterContoursMask =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 1);

  GST_OBJECT_LOCK (platedetector);
  platedetector->priv->frameWidth = frame->info.width;
  platedetector->priv->kernelX =
      (platedetector->priv->plate_percentage * platedetector->priv->frameWidth /
      PLATEWINDOWPERCENTAGE);
  if ((platedetector->priv->kernelX % 2) == 0) {
    platedetector->priv->kernelX = platedetector->priv->kernelX + 1;
  }
  GST_OBJECT_UNLOCK (platedetector);
}

static void
kms_plate_detector_initialize_images (KmsPlateDetector * platedetector,
    GstVideoFrame * frame)
{
  if (platedetector->priv->cvImage == NULL) {
    kms_plate_detector_create_images (platedetector, frame);
  } else if ((platedetector->priv->cvImage->width != frame->info.width)
      || (platedetector->priv->cvImage->height != frame->info.height)) {

    kms_plate_detector_release_images (platedetector);
    kms_plate_detector_create_images (platedetector, frame);
  }
}

static void
kms_plate_detector_preprocessing_method_one (KmsPlateDetector * platedetector)
{
  IplConvKernel *kernel;

  cvCanny (platedetector->priv->edges, platedetector->priv->edges, 70, 150, 3);
  kernel = cvCreateStructuringElementEx (platedetector->priv->kernelX, KERNELY,
      platedetector->priv->kernelX / 2 + 1, KERNELY / 2 + 1, CV_SHAPE_RECT, 0);
  cvMorphologyEx (platedetector->priv->edges,
      platedetector->priv->edgesDilatedMask, NULL, kernel, CV_MOP_CLOSE, 1);
  cvReleaseStructuringElement (&kernel);
  kernel = cvCreateStructuringElementEx (platedetector->priv->kernelX, KERNELY,
      platedetector->priv->kernelX / 2 + 1, KERNELY / 2 + 1, CV_SHAPE_RECT, 0);
  cvMorphologyEx (platedetector->priv->edgesDilatedMask,
      platedetector->priv->edgesDilatedMask, NULL, kernel, CV_MOP_OPEN, 1);
  cvReleaseStructuringElement (&kernel);
  cvCopy (platedetector->priv->edgesDilatedMask, platedetector->priv->edges, 0);
}

static void
kms_plate_detector_preprocessing_method_two (KmsPlateDetector * platedetector)
{
  IplConvKernel *kernel;
  IplImage *edgesAux;

  cvCanny (platedetector->priv->edges, platedetector->priv->edges, 70, 150, 3);
  cvSobel (platedetector->priv->edges, platedetector->priv->edges, 2, 0, 3);
  kernel = cvCreateStructuringElementEx (platedetector->priv->kernelX, KERNELY,
      platedetector->priv->kernelX / 2 + 1, KERNELY / 2 + 1, CV_SHAPE_RECT, 0);
  cvNot (platedetector->priv->edges, platedetector->priv->edges);
  cvErode (platedetector->priv->edges, platedetector->priv->edges, kernel, 1);
  cvNot (platedetector->priv->edges, platedetector->priv->edges);
  cvReleaseStructuringElement (&kernel);
  edgesAux = cvCreateImage (cvSize (platedetector->priv->cvImage->width,
          platedetector->priv->cvImage->height),
      platedetector->priv->cvImage->depth, 1);

  cvSobel (platedetector->priv->edges, edgesAux, 0, 2, 3);
  kernel = cvCreateStructuringElementEx (platedetector->priv->kernelX, KERNELY,
      platedetector->priv->kernelX / 2 + 1, KERNELY / 2 + 1, CV_SHAPE_RECT, 0);
  cvDilate (edgesAux, edgesAux, kernel, 1);
  cvSub (platedetector->priv->edges, edgesAux, platedetector->priv->edges, 0);
  cvSobel (platedetector->priv->edges, edgesAux, 2, 0, 3);
  cvReleaseStructuringElement (&kernel);
  kernel = cvCreateStructuringElementEx (3, 15, 2, 8, CV_SHAPE_RECT, 0);
  cvDilate (edgesAux, edgesAux, kernel, 1);
  cvSub (platedetector->priv->edges, edgesAux, platedetector->priv->edges, 0);
  cvReleaseStructuringElement (&kernel);
  kernel = cvCreateStructuringElementEx (7, 3, 4, 2, CV_SHAPE_RECT, 0);
  cvDilate (platedetector->priv->edges, platedetector->priv->edges, kernel, 1);
  cvReleaseImage (&edgesAux);
  cvReleaseStructuringElement (&kernel);
}

static void
kms_plate_detector_preprocessing_method_three (KmsPlateDetector * platedetector)
{
  cvCanny (platedetector->priv->edges, platedetector->priv->edges, 70, 150, 3);
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

static void
kms_plate_detector_chop_char (char *s)
{
  s[strcspn (s, "\n")] = '\0';
}

static void
kms_plate_detector_rotate_image (IplImage * image,
    CvBox2D fitRect, CvRect detectedRect)
{
  IplImage *rotatedImage = cvCreateImage (cvSize (image->width, image->height),
      image->depth, image->nChannels);
  CvPoint2D32f center;
  CvMat *mapMatrix;

  center.x = detectedRect.x;
  center.y = detectedRect.y;
  mapMatrix = cvCreateMat (2, 3, CV_32FC1);
  cv2DRotationMatrix (center, fitRect.angle, 1.0, mapMatrix);
  cvWarpAffine (image, rotatedImage, mapMatrix, CV_INTER_LINEAR +
      CV_WARP_FILL_OUTLIERS, cvScalarAll (0));
  cvCopy (rotatedImage, image, 0);
  cvReleaseImage (&rotatedImage);
  cvReleaseMat (&mapMatrix);
}

static int
kms_plate_detector_median (GSList * plateStore)
{
  int len = g_slist_length (plateStore);

  if (len <= 1)
    return 0;
  if (len % 2 != 0)
    return ((len - 1) / 2 + 1);

  return (len / 2);
}

static void
kms_plate_detector_select_best_characters_contours (GSList * plateStore,
    CharacterData * mostSimContPosData)
{
  int difference;
  GSList *iterator = NULL;

  for (iterator = plateStore; iterator; iterator = iterator->next) {
    CharacterData *data2 = iterator->data;

    difference = abs (mostSimContPosData->height - data2->height);

    if (difference < 5) {
      data2->similarProportion = TRUE;
    } else {
      data2->similarProportion = FALSE;
    }
  }
}

static gboolean
check_proportion_like_character (int rectangleArea,
    CvRect rect, CharacterData * mostSimContPosData,
    double heightTolerance, double widthUpTolerance, double widthDownTolerance)
{

  return (((rectangleArea > mostSimContPosData->width *
              mostSimContPosData->height / 3) &&
          (abs (rect.height - mostSimContPosData->height) <
              mostSimContPosData->height * heightTolerance) &&
          (rect.width > mostSimContPosData->width * widthDownTolerance)
          && (rectangleArea > 100))
      && (((rect.width < mostSimContPosData->width * widthUpTolerance)
              || (rect.width > mostSimContPosData->width * 2))
          && (rect.width < mostSimContPosData->width * widthUpTolerance * 2)));
}

static gint
compare_position_x (gconstpointer data1, gconstpointer data2)
{
  const CharacterData *a = data1;
  const CharacterData *b = data2;

  return a->x - b->x;
}

static int
kms_plate_detector_find_charac_cont (IplImage * plateInterpolatedAux1,
    CvRect rect,
    GSList ** finalPlateStore,
    CharacterData * mostSimContPosData,
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

    if (!check_proportion_like_character (rectangleArea, rect,
            mostSimContPosData, heightTolerance, widthUpTolerance,
            widthDownTolerance))
      continue;

    cvRectangle (plateInterpolatedAux1, cvPoint (rect.x, rect.y),
        cvPoint (rect.x + rect.width, rect.y + rect.height), cvScalar (0, 0,
            0, 0), 1, 8, 0);
    CharacterData *contourData = g_slice_new0 (CharacterData);

    contourData->x = rect.x;
    contourData->y = rect.y;
    contourData->width = rect.width;
    contourData->height = rect.height;
    contourData->exclude = (rect.width > mostSimContPosData->width * 2) &&
        (rect.width < mostSimContPosData->width * widthUpTolerance * 2);
    contourData->similarProportion = FALSE;
    contourData->isMostSimilar = FALSE;
    counter++;
    *finalPlateStore = g_slist_insert_sorted (*finalPlateStore, contourData,
        compare_position_x);
  }
  cvClearMemStorage (memCharacters);
  cvReleaseMemStorage (&memCharacters);

  return counter;
}

static void
kms_plate_detector_clear_edges (IplImage * plateInterpolatedAux1,
    CharacterData * mostSimContPosData)
{
  int i, j;
  uint8_t *aux2;
  uint8_t *aux = (uint8_t *) plateInterpolatedAux1->imageData;

  for (j = 0; j < plateInterpolatedAux1->height; j++) {
    aux2 = aux;
    for (i = 0; i < plateInterpolatedAux1->width; i++) {
      if ((j < mostSimContPosData->y - 1) && (j > mostSimContPosData->y +
              mostSimContPosData->height + 1))
        *aux2 = 0;
      aux2++;
    }
    aux += plateInterpolatedAux1->widthStep;
  }
}

static gboolean
kms_plate_detector_check_is_plate (KmsPlateDetector * platedetector,
    int numOfCharacters, GSList * finalPlateStore,
    CharacterData * mostSimContPosData)
{
  int medHeight = 0;
  int counter = 0;
  GSList *iterator = NULL;

  for (iterator = finalPlateStore; iterator; iterator = iterator->next) {
    CharacterData *data3 = iterator->data;

    if (data3->exclude == TRUE)
      continue;
    if (abs (mostSimContPosData->y - data3->y) < 3) {
      medHeight = medHeight + data3->height;
      counter++;
    }
  }

  if (counter > 0) {
    medHeight = medHeight / counter;
  } else {
    medHeight = 0;
  }

  return numOfCharacters > 6;
}

static void
kms_plate_detector_extend_character_rois (IplImage * plateInterpolated,
    GSList * finalPlateStore, int margin)
{
  GSList *iterator = NULL;

  for (iterator = finalPlateStore; iterator; iterator = iterator->next) {
    CharacterData *data = iterator->data;

    if (data->x - margin > 0) {
      data->x = data->x - margin;
    }
    if (data->y - margin > 0) {
      data->y = data->y - margin;
    }
    if (data->x + data->width + margin < plateInterpolated->width) {
      data->width = data->width + margin;
    }
    if (data->y + data->height + margin < plateInterpolated->height) {
      data->height = data->height + margin;
    }
  }
}

static int
kms_plate_detector_extract_plate_space_position (GSList * finalPlateStore,
    int characters)
{
  int d = 0;
  int maxDistance = 0;
  int posMaxDistance = 0;
  GSList *iterator = NULL;

  for (iterator = finalPlateStore; iterator; iterator = iterator->next) {
    d++;
    if ((d > 2) && (d < characters - 2)) {
      CharacterData *data1 = iterator->data;
      CharacterData *data2 = iterator->next->data;
      int distance = abs (data1->x + data1->width - data2->x);

      if (distance > maxDistance) {
        maxDistance = distance;
        posMaxDistance = data1->x + data1->width + abs (data1->x + data1->width
            - data2->x) / 2;
      }
    }
  }

  return posMaxDistance;
}

static void
kms_plate_detector_extract_final_plate (KmsPlateDetector * platedetector)
{
  int f, g, h;
  int longString = NUM_PLATE_CHARACTERS + 1;
  char stabilizedPlate[NUM_PLATE_CHARACTERS + 1];
  int characterMatches = 0;
  int characterMatches2 = 0;
  int r;

  for (f = 0; f < longString; f++) {
    char mostRecurrentCharacter = '-';
    int recurrentCounter = 0;
    char selectedCharacter = '-';
    int matchesCounter = 0;

    for (g = 0; g < NUM_PLATES_SAMPLES; g++) {
      selectedCharacter = platedetector->priv->plateStore[g][f];
      for (h = 0; (h < NUM_PLATES_SAMPLES); h++) {
        char characerToCompare = platedetector->priv->plateStore[h][f];

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
    if ((platedetector->priv->previousFinalPlate[r] != stabilizedPlate[r])) {
      characterMatches++;
    }
  }

  if ((characterMatches > MAX_NUM_DIF_CHARACTERS)) {
    platedetector->priv->plateRepetition = 1;
    platedetector->priv->sendPlateEvent = TRUE;
    strcpy (platedetector->priv->previousFinalPlate,
        platedetector->priv->finalPlate);

  } else if (characterMatches == 0) {
    platedetector->priv->plateRepetition++;
  }
  characterMatches = 0;

  if ((platedetector->priv->plateRepetition > NUM_ACCUMULATED_PLATES)
      && (platedetector->priv->sendPlateEvent == TRUE)) {

    for (r = 0; r < NUM_PLATE_CHARACTERS; r++) {
      if ((platedetector->priv->previousFinalPlate[r] ==
              platedetector->priv->sendPlate[r])) {
        characterMatches2++;
      }
    }
    if (characterMatches2 < NUM_PLATE_CHARACTERS) {
      GstStructure *s;
      GstMessage *m;

      platedetector->priv->sendPlateEvent = FALSE;
      platedetector->priv->plateRepetition = 0;

      for (r = 0; r < NUM_PLATE_CHARACTERS; r++) {
        if (platedetector->priv->finalPlate[r] ==
            platedetector->priv->previousFinalPlate[r]) {
          characterMatches++;
        }
        platedetector->priv->sendPlate[r] =
            platedetector->priv->previousFinalPlate[r];
      }

      GST_DEBUG ("NEW PLATE: %s", platedetector->priv->previousFinalPlate);

      /* post a plate-detected message to bus */
      s = gst_structure_new ("plate-detected",
          "plate", G_TYPE_STRING, platedetector->priv->previousFinalPlate,
          NULL);
      m = gst_message_new_element (GST_OBJECT (platedetector), s);
      gst_element_post_message (GST_ELEMENT (platedetector), m);
    }
  }

  if (platedetector->priv->show_debug_info == TRUE) {
    cvRectangle (platedetector->priv->cvImage,
        cvPoint (platedetector->priv->cvImage->width / 2 - 95,
            platedetector->priv->cvImage->height - 65),
        cvPoint (platedetector->priv->cvImage->width / 2 + 165,
            platedetector->priv->cvImage->height - 35), WHITE, -2, 8, 0);
    cvPutText (platedetector->priv->cvImage, stabilizedPlate,
        cvPoint (platedetector->priv->cvImage->width / 2 - 90,
            platedetector->priv->cvImage->height - 40),
        &platedetector->priv->bigFont, cvScalar (0, 0, 0, 0));
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
    TessBaseAPISetVariable (platedetector->priv->handle,
        "tessedit_char_whitelist", PLATE_LETTERS);
  } else if ((d >= initialPosition) && (*numbersCounter < 4)) {
    TessBaseAPISetVariable (platedetector->priv->handle,
        "tessedit_char_whitelist", PLATE_NUMBERS);
    *numbersCounter = *numbersCounter + 1;
  } else {
    TessBaseAPISetVariable (platedetector->priv->handle,
        "tessedit_char_whitelist", PLATE_LETTERS);
  }
}

static int
kms_plate_detector_first_character_position (GSList * finalPlateStore,
    int spacePositionX)
{
  int d = 0;
  int initialPosition = 0;
  int spaceFound = FALSE;
  GSList *iterator = NULL;

  for (iterator = finalPlateStore; iterator; iterator = iterator->next) {
    CharacterData *data = iterator->data;

    if ((data->x > spacePositionX) && (spaceFound == 0)) {
      initialPosition = d - 4;
      spaceFound = 1;
      if (initialPosition < 0) {
        initialPosition = 0;
      }
    }
    d++;
  }

  return initialPosition;
}

static void
kms_plate_detector_show_original_characters (KmsPlateDetector * platedetector,
    IplImage * imAux, IplImage * imAuxRGB,
    int spacePositionX, int d, CharacterData * data)
{
  if ((data->x + data->width <
          platedetector->priv->cvImage->width) && (data->y + MARGIN_40 +
          data->height < platedetector->priv->cvImage->height)) {
    cvSetImageROI (platedetector->priv->cvImage,
        cvRect (data->x, data->y + MARGIN_40, data->width, data->height));
    cvCvtColor (imAux, imAuxRGB, CV_GRAY2BGR);
    cvCopy (imAuxRGB, platedetector->priv->cvImage, 0);
    cvResetImageROI (platedetector->priv->cvImage);
  }

  if (d == 0) {
    cvRectangle (platedetector->priv->cvImage, cvPoint (spacePositionX,
            data->y + MARGIN_30),
        cvPoint (spacePositionX + MARGIN_3, 2 * data->y + MARGIN_40 +
            data->height), RED, -2, 8, 0);
  }
}

static void
kms_plate_detector_show_proccesed_characters (KmsPlateDetector * platedetector,
    IplImage * imAuxRGB2, int d, CharacterData * data)
{
  if ((data->x + d * EDGE_MARGIN + imAuxRGB2->width <
          platedetector->priv->cvImage->width)
      && (2 * data->y + MARGIN_120 + imAuxRGB2->height <
          platedetector->priv->cvImage->height)) {
    cvSetImageROI (platedetector->priv->cvImage,
        cvRect (data->x + d * EDGE_MARGIN,
            2 * data->y + MARGIN_120, imAuxRGB2->width, imAuxRGB2->height));
    cvCopy (imAuxRGB2, platedetector->priv->cvImage, 0);
    cvResetImageROI (platedetector->priv->cvImage);
  }
}

static void
kms_plate_detector_show_ocr_results (KmsPlateDetector * platedetector,
    int position, char *ocrResultAux1,
    char *ocrResultAux2, char *textConf1, char *textConf2)
{
  cvPutText (platedetector->priv->cvImage, ocrResultAux1,
      cvPoint (MARGIN_10, +platedetector->priv->cvImage->height -
          MARGIN_200 + 20 * position),
      &platedetector->priv->littleFont, cvScalar (0, 0, 255, 0));
  cvPutText (platedetector->priv->cvImage, textConf1,
      cvPoint (MARGIN_40, +platedetector->priv->cvImage->height -
          MARGIN_200 + 20 * position), &platedetector->priv->littleFont,
      cvScalar (0, 0, 255, 0));

  cvPutText (platedetector->priv->cvImage, ocrResultAux2,
      cvPoint (MARGIN_200, +platedetector->priv->cvImage->height -
          MARGIN_200 + 25 * position), &platedetector->priv->littleFont,
      cvScalar (255, 0, 0, 0));
  cvPutText (platedetector->priv->cvImage, textConf2, cvPoint (MARGIN_240,
          +platedetector->priv->cvImage->height - MARGIN_200 + 25 * position),
      &platedetector->priv->littleFont, cvScalar (255, 0, 0, 0));
}

static void
kms_plate_detector_insert_plate_in_store (KmsPlateDetector * platedetector)
{
  int characterMatches = 0;
  int r;

  for (r = 0; r < NUM_PLATE_CHARACTERS; r++) {
    if (platedetector->priv->previousFinalPlate[r] ==
        platedetector->priv->finalPlate[r]) {
      characterMatches++;
    }
  }

  if ((characterMatches < MIN_CHARACTERS_AMOUNT)) {
    kms_plate_detector_plate_store_initialization (platedetector);
    GST_DEBUG ("new plate detected...");
  }

  if (platedetector->priv->storePosition < NUM_PLATES_SAMPLES) {
    strcpy (platedetector->priv->plateStore[platedetector->priv->storePosition],
        platedetector->priv->finalPlate);
    platedetector->priv->storePosition++;
  } else if (platedetector->priv->storePosition == NUM_PLATES_SAMPLES) {
    int t;

    for (t = NUM_PLATES_SAMPLES - 1; t > 0; t--) {
      strcpy (platedetector->priv->plateStore[t],
          platedetector->priv->plateStore[t - 1]);
    }
    strcpy (platedetector->priv->plateStore[0],
        platedetector->priv->finalPlate);
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
      platedetector->priv->finalPlate[r] = finalPlateAux[r];
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
    GSList * plateStore,
    GSList * finalPlateStore, gboolean checkIsPlate, int spacePositionX)
{
  int pos;
  int initialPosition = 0;
  char finalPlateAux[10] = { 0 };
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
  GSList *iterator = NULL;

  strncpy (finalPlateAux, NULL_PLATE, NUM_PLATE_CHARACTERS);
  if (platedetector->priv->show_debug_info == TRUE) {
    cvRectangle (platedetector->priv->cvImage,
        cvPoint (0, platedetector->priv->cvImage->height - 230),
        cvPoint (400, platedetector->priv->cvImage->height), BLACK, -2, 8, 0);
  }

  initialPosition =
      kms_plate_detector_first_character_position (finalPlateStore,
      spacePositionX);

  for (iterator = finalPlateStore; iterator; iterator = iterator->next) {
    CharacterData *data = iterator->data;

    if (data->exclude) {
      continue;
    }

    kms_plate_detector_select_tesseract_whitelist (platedetector, position,
        platedetector->priv->handle, initialPosition, &numbersCounter);
    imAux = cvCreateImage (cvSize (data->width,
            data->height),
        plateInterpolated->depth, plateInterpolated->nChannels);
    imAuxRGB = cvCreateImage (cvSize (data->width,
            data->height),
        platedetector->priv->cvImage->depth,
        platedetector->priv->cvImage->nChannels);

    if ((data->x + data->width <
            plateInterpolated->width) && (data->y +
            data->height < plateInterpolated->height)) {
      cvSetImageROI (plateInterpolated, cvRect (data->x,
              data->y, data->width, data->height));
      cvCopy (plateInterpolated, imAux, 0);
    }

    if (platedetector->priv->show_debug_info == TRUE) {
      kms_plate_detector_show_original_characters (platedetector,
          imAux, imAuxRGB, spacePositionX, position, data);
    }

    if ((data->x + data->width <
            plateInterpolatedAux2->width) && (data->y +
            data->height < plateInterpolatedAux2->height)) {
      cvSetImageROI (plateInterpolatedAux2,
          cvRect (data->x, data->y, data->width, data->height));
      cvCopy (plateInterpolatedAux2, imAux, 0);
      cvResetImageROI (plateInterpolatedAux2);
    }

    imAux2 = cvCreateImage (cvSize (imAux->width + 2 * EDGE_MARGIN,
            imAux->height + EDGE_MARGIN),
        plateInterpolated->depth, plateInterpolated->nChannels);
    imAuxRGB2 =
        cvCreateImage (cvSize (imAux->width + 2 * EDGE_MARGIN,
            imAux->height + EDGE_MARGIN),
        platedetector->priv->cvImage->depth,
        platedetector->priv->cvImage->nChannels);

    cvSetZero (imAux2);
    cvNot (imAux2, imAux2);
    if ((EDGE_MARGIN + imAux->width < imAux2->width) &&
        (EDGE_MARGIN / 2 + imAux->height < imAux2->height)) {
      cvSetImageROI (imAux2, cvRect (EDGE_MARGIN, EDGE_MARGIN / 2,
              imAux->width, imAux->height));
      cvCopy (imAux, imAux2, 0);
      cvResetImageROI (imAux2);
    }

    kms_plate_detector_clean_character (imAux, imAux2);
    cvCvtColor (imAux2, imAuxRGB2, CV_GRAY2BGR);

    if (platedetector->priv->show_debug_info == TRUE) {
      kms_plate_detector_show_proccesed_characters (platedetector,
          imAuxRGB2, position, data);
    }
    TessBaseAPISetImage (platedetector->priv->handle,
        (unsigned char *) imAux->imageData, imAux->width,
        imAux->height, (imAux->depth / 8) / imAux->nChannels, imAux->widthStep);
    ocrResultAux1 = TessBaseAPIGetUTF8Text (platedetector->priv->handle);
    kms_plate_detector_chop_char (ocrResultAux1);
    confidenceRate1 = TessBaseAPIMeanTextConf (platedetector->priv->handle);

    TessBaseAPIClear (platedetector->priv->handle);

    sprintf (textConf1, "Conf:%d", (int) confidenceRate1);

    TessBaseAPISetImage (platedetector->priv->handle,
        (unsigned char *) imAux2->imageData, imAux2->width,
        imAux2->height, (imAux2->depth / 8) / imAux2->nChannels,
        imAux2->widthStep);

    ocrResultAux2 = TessBaseAPIGetUTF8Text (platedetector->priv->handle);

    kms_plate_detector_chop_char (ocrResultAux2);
    confidenceRate2 = TessBaseAPIMeanTextConf (platedetector->priv->handle);

    TessBaseAPIClear (platedetector->priv->handle);

    sprintf (textConf2, "Conf:%d", (int) confidenceRate2);

    if (confidenceRate1 > confidenceRate2) {
      confidenceRate = confidenceRate1;
      textConf[0] = ocrResultAux1[0];
    } else {
      confidenceRate = confidenceRate2;
      textConf[0] = ocrResultAux2[0];
    }

    pos = position + 2 - initialPosition;
    if (pos >= 0 && pos < NUM_PLATE_CHARACTERS) {
      if (confidenceRate > MIN_OCR_CONFIDENCE_RATE) {
        finalPlateAux[position + 2 - initialPosition] = textConf[0];
      } else {
        finalPlateAux[position + 2 - initialPosition] = '-';
      }
    }

    if (platedetector->priv->show_debug_info == TRUE) {
      kms_plate_detector_show_ocr_results (platedetector, position,
          ocrResultAux1, ocrResultAux2, textConf1, textConf2);
    }
    kms_plate_detector_chop_char (finalPlateAux);
    position++;

    TessDeleteText (ocrResultAux1);
    TessDeleteText (ocrResultAux2);

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
    double *contourFitArea, CvRect * detectedRect, float *PlateProportion,
    CvBox2D * fitRect)
{
  *fitRect = cvMinAreaRect2 (contoursPlates, NULL);

  *contourFitArea = cvContourArea (contoursPlates, CV_WHOLE_SEQ, 0);

  if (abs (fitRect->angle) < 45) {
    detectedRect->width = fitRect->size.width;
    detectedRect->height = fitRect->size.height;
  } else {
    detectedRect->width = fitRect->size.height;
    detectedRect->height = fitRect->size.width;
    fitRect->angle = +(fitRect->angle + 90);
  }
  detectedRect->x = fitRect->center.x - detectedRect->width / 2;
  detectedRect->y = fitRect->center.y - detectedRect->height / 2;

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
          platedetector->priv->cvImage->width) &&
      (rect->y + rect->height + rect->height * expandRateHeight <
          platedetector->priv->cvImage->height)) {
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
  if (detectedRect->x - detectedRect->width >
      platedetector->priv->cvImage->width) {
    detectedRect->width = platedetector->priv->cvImage->width - detectedRect->x;
  }
  if (detectedRect->y - detectedRect->height >
      platedetector->priv->cvImage->height) {
    detectedRect->height =
        platedetector->priv->cvImage->height - detectedRect->y;
  }
}

static void
kms_plate_detector_preprocessing_images (IplImage * plateROI,
    IplImage * plateBinRoi,
    IplImage * plateInterpolatedAux1,
    IplImage * plateInterpolatedAux2,
    IplImage * plateInterAux1Color, IplImage * plateInterpolated)
{
  if (plateROI->width > 0 && plateROI->height) {
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
}

static void
kms_plate_detector_select_preprocessing_type (KmsPlateDetector * platedetector)
{
  if (platedetector->priv->preprocessingType == PREPROCESSING_ONE) {
    kms_plate_detector_preprocessing_method_one (platedetector);
  } else if (platedetector->priv->preprocessingType == PREPROCESSING_TWO) {
    kms_plate_detector_preprocessing_method_two (platedetector);
  } else if (platedetector->priv->preprocessingType == PREPROCESSING_THREE) {
    kms_plate_detector_preprocessing_method_three (platedetector);
  }
}

static gint
compare_height (gconstpointer data1, gconstpointer data2)
{
  const CharacterData *a = data1;
  const CharacterData *b = data2;

  return b->height - a->height;
}

static void
kms_plate_detector_extract_potential_characters (KmsPlateDetector *
    platedetector, CvSeq * contoursCharacters, IplImage * plateInterpolatedAux1,
    IplImage * plateInterAux1Color, GSList ** plateStore, int *numContours,
    int plateHeight)
{

  for (; contoursCharacters != 0;
      contoursCharacters = contoursCharacters->h_next) {
    CvRect rect = cvBoundingRect (contoursCharacters, 0);
    float proporcion1 = DEFAULT_CHARACTER_PROPORTION;

    if ((rect.width != 0)) {
      proporcion1 = (float) rect.height / (float) rect.width;
    }

    if ((fabsf (CHARACTER_IDEAL_PROPORTION2 - proporcion1) < 0.35)
        && MIN_CHAR_CONTOUR_AREA < rect.height * rect.width) {
      CharacterData *contourData = g_slice_new0 (CharacterData);

      contourData->x = rect.x;
      contourData->y = rect.y;
      contourData->width = rect.width;
      contourData->height = rect.height;
      contourData->exclude = FALSE;
      contourData->similarProportion = FALSE;
      contourData->isMostSimilar = FALSE;
      *plateStore = g_slist_insert_sorted (*plateStore, contourData,
          compare_height);
      cvDrawContours (plateInterAux1Color, contoursCharacters, WHITE, WHITE,
          -1, 1, 8, cvPoint (0, 0));
    }
  }
}

static void
kms_plate_detector_rotate_images (IplImage * plateInterAux1Color,
    IplImage * plateInterpolatedAux1,
    IplImage * plateInterpolatedAux2,
    IplImage * plateInterpolated,
    CharacterData * mostSimContPosData, CvBox2D fitRect, GSList * plateStore)
{
  CvRect rect = cvRect (mostSimContPosData->x, mostSimContPosData->y,
      plateInterpolatedAux1->width,
      plateInterpolatedAux1->height);

  kms_plate_detector_rotate_image (plateInterAux1Color, fitRect, rect);
  kms_plate_detector_rotate_image (plateInterpolatedAux1, fitRect, rect);
  kms_plate_detector_rotate_image (plateInterpolatedAux2, fitRect, rect);
  kms_plate_detector_rotate_image (plateInterpolated, fitRect, rect);
}

static void
kms_plate_detector_draw_plate_rectang (KmsPlateDetector * platedetector,
    CvRect * rect)
{
  cvRectangle (platedetector->priv->cvImage, cvPoint (rect->x, rect->y),
      cvPoint (rect->x + rect->width, rect->y + rect->height), GREEN, 2, 8, 0);
}

static void
kms_plate_detector_select_character_resize_factor (KmsPlateDetector *
    platedetector, CvRect * rect)
{
  platedetector->priv->resizeFactor =
      PLATE_HEIGHT_SCALE_RATE / (float) rect->height;
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

static gboolean
check_is_contour_into_contour (CharacterData * data1, CharacterData * data2)
{
  return ((data2->x > data1->x) &&
      (data2->x < (data1->x +
              data1->width)) && (data1->x +
          data2->width < data1->x +
          data1->width) && (data2->y >
          data1->y) && (data2->y + data2->height < data1->y + data1->height));
}

static gboolean
check_is_split_character (CharacterData * mostSimContPosData,
    CharacterData * data1, CharacterData * data2)
{
  return ((data1->exclude != TRUE) &&
      (data1->width < mostSimContPosData->width * 0.7) &&
      (data2->width < mostSimContPosData->width * 0.7) &&
      (data1->width + data2->width <
          mostSimContPosData->width * 1.2) &&
      (data2->exclude != TRUE) && (abs (data1->x - data2->x)
          < mostSimContPosData->width * 0.8));
}

static void
kms_plate_detector_simplify_store (IplImage * plateInterpolatedAux2,
    GSList * finalPlateStore, GSList * plateStore,
    CharacterData * mostSimContPosData)
{
  GSList *iterator = NULL;

  for (iterator = finalPlateStore; iterator; iterator = iterator->next) {
    CharacterData *data1 = iterator->data;

    if (iterator->next == NULL)
      continue;
    CharacterData *data2 = iterator->next->data;

    if (check_is_contour_into_contour (data1, data2)) {
      data2->exclude = TRUE;
    }

    if ((data1->exclude == TRUE) || (data1->y <= mostSimContPosData->width * 2))
      continue;

    data2->exclude = TRUE;

    if (!check_is_split_character (mostSimContPosData, data1, data2))
      continue;

    data1->width = abs (data2->x + data2->width - data1->x);
    data2->exclude = TRUE;
    cvErode (plateInterpolatedAux2, plateInterpolatedAux2, 0, 2);
    cvCanny (plateInterpolatedAux2, plateInterpolatedAux2, 210, 120, 3);
    cvDilate (plateInterpolatedAux2, plateInterpolatedAux2, 0, 3);
  }
}

static void
kms_plate_detector_release_aux_images (IplImage * plateROI,
    IplImage * plateBinRoi,
    IplImage * plateColorRoi,
    IplImage * plateContour,
    IplImage * plateInterpolated,
    IplImage * plateInterpolatedAux1,
    IplImage * plateInterpolatedAux2, IplImage * plateInterAux1Color)
{
  cvReleaseImage (&plateROI);
  cvReleaseImage (&plateBinRoi);
  cvReleaseImage (&plateColorRoi);
  cvReleaseImage (&plateContour);
  cvReleaseImage (&plateInterpolated);
  cvReleaseImage (&plateInterpolatedAux1);
  cvReleaseImage (&plateInterpolatedAux2);
  cvReleaseImage (&plateInterAux1Color);
}

static void
kms_plate_detector_character_data_free (gpointer data)
{
  g_slice_free (CharacterData, data);
}

static GstFlowReturn
kms_plate_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (filter);
  GstMapInfo info;
  CvSeq *contoursPlates = 0;
  CvMemStorage *memPlates;
  CharacterData *mostSimContPosData;

  if (platedetector->priv->handle == NULL)
    return GST_FLOW_OK;

  memPlates = cvCreateMemStorage (0);

  kms_plate_detector_initialize_images (platedetector, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  platedetector->priv->cvImage->imageData = (char *) info.data;
  cvCvtColor (platedetector->priv->cvImage, platedetector->priv->edges,
      CV_BGR2GRAY);
  kms_plate_detector_select_preprocessing_type (platedetector);
  cvFindContours (platedetector->priv->edges, memPlates, &contoursPlates,
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
    int numOfCharacters;
    gboolean checkIsPlate;
    int spacePositionX;
    CvMemStorage *memCharacters;
    CvSeq *contoursCharacters;
    GSList *plateStore = NULL;
    GSList *finalPlateStore = NULL;
    CvBox2D fitRect;

    kms_plate_detector_extract_potential_plate (contoursPlates, &contourFitArea,
        &detectedRect, &PlateProportion, &fitRect);
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

    cvSetImageROI (platedetector->priv->cvImage, rect);
    cvCopy (platedetector->priv->cvImage, plateROI, 0);

    plateBinRoi = cvCreateImage (cvGetSize (plateROI), plateROI->depth, 1);
    plateInterpolated =
        cvCreateImage (cvSize (platedetector->priv->resizeFactor *
            plateBinRoi->width,
            platedetector->priv->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 1);
    plateInterpolatedAux1 =
        cvCreateImage (cvSize (platedetector->priv->resizeFactor *
            plateBinRoi->width,
            platedetector->priv->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 1);
    plateInterpolatedAux2 =
        cvCreateImage (cvSize (platedetector->priv->resizeFactor *
            plateBinRoi->width,
            platedetector->priv->resizeFactor * plateBinRoi->height),
        plateBinRoi->depth, 1);
    plateInterAux1Color =
        cvCreateImage (cvSize (platedetector->priv->resizeFactor *
            plateBinRoi->width,
            platedetector->priv->resizeFactor * plateBinRoi->height),
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

    kms_plate_detector_extract_potential_characters (platedetector,
        contoursCharacters, plateInterpolatedAux1, plateInterAux1Color,
        &plateStore, &numContours, rect.height);
    cvClearMemStorage (memCharacters);
    cvReleaseMemStorage (&memCharacters);

    if (plateStore == NULL) {
      numOfCharacters = -1;
      kms_plate_detector_release_aux_images (plateROI, plateBinRoi,
          plateColorRoi, plateContour,
          plateInterpolated,
          plateInterpolatedAux1, plateInterpolatedAux2, plateInterAux1Color);
      cvResetImageROI (platedetector->priv->cvImage);
      continue;
    }

    mostSimContPos = kms_plate_detector_median (plateStore);
    mostSimContPosData = g_slist_nth_data (plateStore, mostSimContPos);
    kms_plate_detector_select_character_resize_factor (platedetector, &rect);

    kms_plate_detector_select_best_characters_contours (plateStore,
        mostSimContPosData);

    kms_plate_detector_rotate_images (plateInterAux1Color,
        plateInterpolatedAux1, plateInterpolatedAux2, plateInterpolated,
        mostSimContPosData, fitRect, plateStore);
    kms_plate_detector_clear_edges (plateInterpolatedAux1, mostSimContPosData);
    numOfCharacters =
        kms_plate_detector_find_charac_cont (plateInterpolatedAux1,
        rect, &finalPlateStore, mostSimContPosData, 0.2, 1.4, 0.25);
    checkIsPlate =
        kms_plate_detector_check_is_plate (platedetector, numOfCharacters,
        finalPlateStore, mostSimContPosData);
    kms_plate_detector_simplify_store (plateInterpolatedAux2, finalPlateStore,
        plateStore, mostSimContPosData);
    cvNot (plateInterpolatedAux1, plateInterpolatedAux1);
    cvThreshold (plateInterpolatedAux1, plateInterpolatedAux1, 254,
        255, CV_THRESH_BINARY);

    spacePositionX =
        kms_plate_detector_extract_plate_space_position (finalPlateStore,
        numOfCharacters);

    kms_plate_detector_extend_character_rois (plateInterpolated,
        finalPlateStore, 2);
    cvResetImageROI (platedetector->priv->cvImage);

    if (numOfCharacters > MIN_NUMBER_CHARACTERS) {
      if (checkIsPlate) {
        kms_plate_detector_read_characters (platedetector, plateColorRoi,
            plateBinRoi, plateInterpolated,
            plateInterpolatedAux2, plateStore,
            finalPlateStore, checkIsPlate, spacePositionX);
        kms_plate_detector_extract_final_plate (platedetector);
        if (platedetector->priv->show_debug_info == TRUE) {
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

    g_slist_free_full (plateStore, kms_plate_detector_character_data_free);
    g_slist_free_full (finalPlateStore, kms_plate_detector_character_data_free);
  }

  if (platedetector->priv->preprocessingType == PREPROCESSING_ONE) {
    platedetector->priv->preprocessingType = PREPROCESSING_TWO;
  } else if (platedetector->priv->preprocessingType == PREPROCESSING_TWO) {
    platedetector->priv->preprocessingType = PREPROCESSING_THREE;
  } else if (platedetector->priv->preprocessingType == PREPROCESSING_THREE) {
    platedetector->priv->preprocessingType = PREPROCESSING_ONE;
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
