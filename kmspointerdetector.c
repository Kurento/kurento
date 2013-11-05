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
#include "kmspointerdetector.h"

#define PLUGIN_NAME "pointerdetector"

GST_DEBUG_CATEGORY_STATIC (kms_pointer_detector_debug_category);
#define GST_CAT_DEFAULT kms_pointer_detector_debug_category

#define FRAMES_TO_RESET  ((int) 250)
#define COMPARE_THRESH_HIST_REF ((double) 0.95)
#define COMPARE_THRESH_SECOND_HIST ((double) 0.95)
#define COMPARE_THRESH_2_RECT ((double) 0.82)
#define GREEN CV_RGB (0, 255, 0)
#define WHITE CV_RGB (255, 255, 255)

/* prototypes */

static void kms_pointer_detector_set_property (GObject * object,
    guint property_id, const GValue * value, GParamSpec * pspec);
static void kms_pointer_detector_get_property (GObject * object,
    guint property_id, GValue * value, GParamSpec * pspec);
static void kms_pointer_detector_dispose (GObject * object);
static void kms_pointer_detector_finalize (GObject * object);

static gboolean kms_pointer_detector_start (GstBaseTransform * trans);
static gboolean kms_pointer_detector_stop (GstBaseTransform * trans);
static gboolean kms_pointer_detector_set_info (GstVideoFilter * filter,
    GstCaps * incaps, GstVideoInfo * in_info, GstCaps * outcaps,
    GstVideoInfo * out_info);
static GstFlowReturn kms_pointer_detector_transform_frame_ip (GstVideoFilter *
    filter, GstVideoFrame * frame);

enum
{
  PROP_0,
  PROP_NUM_REGIONS,
  PROP_WINDOW_SCALE,
  PROP_SHOW_DEBUG_INFO,
  PROP_BUTTONS_STRUCTURE
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPointerDetector, kms_pointer_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_pointer_detector_debug_category, PLUGIN_NAME,
        0, "debug category for pointerdetector element"));

static void
kms_pointer_detector_class_init (KmsPointerDetectorClass * klass)
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
      "Pointer detector element", "Video/Filter",
      "Detects pointer an raises events with its position",
      "Francisco Rivero <fj.riverog@gmail.com>");

  gobject_class->set_property = kms_pointer_detector_set_property;
  gobject_class->get_property = kms_pointer_detector_get_property;
  gobject_class->dispose = kms_pointer_detector_dispose;
  gobject_class->finalize = kms_pointer_detector_finalize;
  base_transform_class->start = GST_DEBUG_FUNCPTR (kms_pointer_detector_start);
  base_transform_class->stop = GST_DEBUG_FUNCPTR (kms_pointer_detector_stop);
  video_filter_class->set_info =
      GST_DEBUG_FUNCPTR (kms_pointer_detector_set_info);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_pointer_detector_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_NUM_REGIONS,
      g_param_spec_int ("num-regions-eval", "num regions eval",
          "Number of regions evaluated when searching for similar regions",
          20, 400, 150, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_WINDOW_SCALE,
      g_param_spec_int ("scale-search-window", "scale search window",
          "Fix the size of the searching window",
          2, 100, 5, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_BUTTONS_STRUCTURE,
      g_param_spec_boxed ("buttons-layout", "buttons layout",
          "supply the positions and dimensions of buttons into the window",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
}

static void
dispose_button_struct (gpointer data)
{
  ButtonStruct *aux = data;

  if (aux->id != NULL)
    g_free (aux->id);
  g_free (aux);
}

static void
kms_pointer_detector_dispose_buttons_layout_list (KmsPointerDetector *
    pointerdetector)
{
  g_slist_free_full (pointerdetector->buttonsLayoutList, dispose_button_struct);
  pointerdetector->buttonsLayoutList = NULL;
}

static void
kms_pointer_detector_load_buttonsLayout (KmsPointerDetector * pointerdetector)
{
  int aux, len;

  if (pointerdetector->buttonsLayoutList != NULL) {
    kms_pointer_detector_dispose_buttons_layout_list (pointerdetector);
  }

  len = gst_structure_n_fields (pointerdetector->buttonsLayout);
  GST_DEBUG ("len: %d", len);

  for (aux = 0; aux < len; aux++) {
    const gchar *name =
        gst_structure_nth_field_name (pointerdetector->buttonsLayout, aux);
    GstStructure *button;
    gboolean ret;

    ret =
        gst_structure_get (pointerdetector->buttonsLayout, name,
        GST_TYPE_STRUCTURE, &button, NULL);
    if (ret) {
      ButtonStruct *structAux = g_malloc0 (sizeof (ButtonStruct));

      gst_structure_get (button, "upRightCornerX", G_TYPE_INT,
          &structAux->cvButtonLayout.x, NULL);
      gst_structure_get (button, "upRightCornerY", G_TYPE_INT,
          &structAux->cvButtonLayout.y, NULL);
      gst_structure_get (button, "width", G_TYPE_INT,
          &structAux->cvButtonLayout.width, NULL);
      gst_structure_get (button, "height", G_TYPE_INT,
          &structAux->cvButtonLayout.height, NULL);
      gst_structure_get (button, "id", G_TYPE_STRING, &structAux->id, NULL);
      GST_DEBUG ("check: %d %d %d %d", structAux->cvButtonLayout.x,
          structAux->cvButtonLayout.y, structAux->cvButtonLayout.width,
          structAux->cvButtonLayout.height);
      pointerdetector->buttonsLayoutList =
          g_slist_append (pointerdetector->buttonsLayoutList, structAux);
      gst_structure_free (button);
    }
  }
}

static void
kms_pointer_detector_init (KmsPointerDetector * pointerdetector)
{
  pointerdetector->cvImage = NULL;
  int h_bins = 30, s_bins = 32;
  int hist_size[] = { h_bins, s_bins };
  float h_ranges[] = { 0, 180 };
  float s_ranges[] = { 0, 255 };
  float *ranges[] = { h_ranges, s_ranges };
  pointerdetector->histModel =
      cvCreateHist (2, hist_size, CV_HIST_ARRAY, ranges, 1);
  pointerdetector->histCompare =
      cvCreateHist (2, hist_size, CV_HIST_ARRAY, ranges, 1);
  pointerdetector->histSetUp1 =
      cvCreateHist (2, hist_size, CV_HIST_ARRAY, ranges, 1);
  pointerdetector->histSetUp2 =
      cvCreateHist (2, hist_size, CV_HIST_ARRAY, ranges, 1);
  pointerdetector->histSetUpRef =
      cvCreateHist (2, hist_size, CV_HIST_ARRAY, ranges, 1);
  pointerdetector->upCornerFinalRect.x = 20;
  pointerdetector->upCornerFinalRect.y = 10;
  pointerdetector->downCornerFinalRect.x = 40;
  pointerdetector->downCornerFinalRect.y = 30;
  pointerdetector->upCornerRect1.x = 20;
  pointerdetector->upCornerRect1.y = 10;
  pointerdetector->downCornerRect1.x = 40;
  pointerdetector->downCornerRect1.y = 30;
  pointerdetector->upCornerRect2.x = 50;
  pointerdetector->upCornerRect2.y = 10;
  pointerdetector->downCornerRect2.x = 70;
  pointerdetector->downCornerRect2.y = 30;
  pointerdetector->upRightButtonCorner.x = 0;
  pointerdetector->upRightButtonCorner.y = 0;
  pointerdetector->downLeftButtonCorner.x = 0;
  pointerdetector->downLeftButtonCorner.y = 0;
  pointerdetector->windowScaleRef = 5;
  pointerdetector->numOfRegions = 150;
  pointerdetector->windowScale = pointerdetector->windowScaleRef;
  pointerdetector->iteration = 0;
  pointerdetector->state = START;
  pointerdetector->histRefCapturesCounter = 0;
  pointerdetector->secondHistCapturesCounter = 0;
  pointerdetector->trackinRectSize.width =
      abs (pointerdetector->upCornerRect1.x -
      pointerdetector->downCornerRect1.x);
  pointerdetector->trackinRectSize.height =
      abs (pointerdetector->upCornerRect1.y -
      pointerdetector->downCornerRect1.y);
  pointerdetector->colorRect1 = WHITE;
  pointerdetector->colorRect2 = WHITE;
  pointerdetector->show_debug_info = FALSE;
  pointerdetector->buttonsLayout = NULL;
  pointerdetector->buttonsLayoutList = NULL;
}

void
kms_pointer_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  GST_DEBUG_OBJECT (pointerdetector, "set_property");

  switch (property_id) {
    case PROP_NUM_REGIONS:
      pointerdetector->numOfRegions = g_value_get_int (value);
      break;
    case PROP_WINDOW_SCALE:
      pointerdetector->windowScaleRef = g_value_get_int (value);
      break;
    case PROP_SHOW_DEBUG_INFO:
      pointerdetector->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_BUTTONS_STRUCTURE:
      if (pointerdetector->buttonsLayout != NULL)
        gst_structure_free (pointerdetector->buttonsLayout);

      pointerdetector->buttonsLayout = g_value_dup_boxed (value);
      kms_pointer_detector_load_buttonsLayout (pointerdetector);
      break;
  }
}

void
kms_pointer_detector_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  GST_DEBUG_OBJECT (pointerdetector, "get_property");

  switch (property_id) {
    case PROP_NUM_REGIONS:
      g_value_set_int (value, pointerdetector->numOfRegions);
      break;
    case PROP_WINDOW_SCALE:
      g_value_set_int (value, pointerdetector->windowScaleRef);
      break;
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, pointerdetector->show_debug_info);
      break;
    case PROP_BUTTONS_STRUCTURE:
      g_value_set_boxed (value, pointerdetector->buttonsLayout);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_pointer_detector_dispose (GObject * object)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  GST_DEBUG_OBJECT (pointerdetector, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_pointer_detector_parent_class)->dispose (object);
}

void
kms_pointer_detector_finalize (GObject * object)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  GST_DEBUG_OBJECT (pointerdetector, "finalize");

  /* clean up object here */

  // TODO: Release window structure and window list

  cvReleaseImageHeader (&pointerdetector->cvImage);
  cvReleaseImageHeader (&pointerdetector->cvImageAux1);
  cvReleaseHist (&pointerdetector->histCompare);
  cvReleaseHist (&pointerdetector->histModel);
  cvReleaseHist (&pointerdetector->histSetUp1);
  cvReleaseHist (&pointerdetector->histSetUp2);
  cvReleaseHist (&pointerdetector->histSetUpRef);

  G_OBJECT_CLASS (kms_pointer_detector_parent_class)->finalize (object);
}

static gboolean
kms_pointer_detector_start (GstBaseTransform * trans)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (trans);

  GST_DEBUG_OBJECT (pointerdetector, "start");

  return TRUE;
}

static gboolean
kms_pointer_detector_stop (GstBaseTransform * trans)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (trans);

  GST_DEBUG_OBJECT (pointerdetector, "stop");

  return TRUE;
}

static gboolean
kms_pointer_detector_set_info (GstVideoFilter * filter, GstCaps * incaps,
    GstVideoInfo * in_info, GstCaps * outcaps, GstVideoInfo * out_info)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (filter);

  GST_DEBUG_OBJECT (pointerdetector, "set_info");

  return TRUE;
}

static void
calc_histogram (IplImage * src, CvHistogram * hist1)
{
// Compute the HSV image, and decompose it into separate planes.
  cvCvtColor (src, src, CV_BGR2HSV);
  IplImage *h_plane = cvCreateImage (cvGetSize (src), 8, 1);
  IplImage *s_plane = cvCreateImage (cvGetSize (src), 8, 1);
  IplImage *v_plane = cvCreateImage (cvGetSize (src), 8, 1);
  IplImage *planes[2];

  planes[0] = h_plane;
  planes[1] = s_plane;
  cvSplit (src, h_plane, s_plane, v_plane, 0);
  cvCalcHist (planes, hist1, 0, 0);     // Compute histogram
  cvNormalizeHist (hist1, 20 * 255);    // Normalize it
  cvReleaseImage (&h_plane);
  cvReleaseImage (&s_plane);
  cvReleaseImage (&v_plane);

}

static void
get_histogram (IplImage * src, CvPoint point, CvSize size,
    CvHistogram * histogram)
{
  IplImage *srcAux =
      cvCreateImage (cvSize (size.width, size.height), IPL_DEPTH_8U, 3);
  cvSetImageROI (src, cvRect (point.x, point.y, size.width, size.height));
  cvCopy (src, srcAux, 0);
  calc_histogram (srcAux, histogram);
  cvReleaseImage (&srcAux);
}

static void
kms_pointer_detector_initialize_images (KmsPointerDetector * pointerdetector,
    GstVideoFrame * frame)
{
  if (pointerdetector->cvImage == NULL) {
    pointerdetector->cvImage =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
    pointerdetector->cvImageAux1 =
        cvCreateImage (cvSize (pointerdetector->trackinRectSize.width,
            pointerdetector->trackinRectSize.height), IPL_DEPTH_8U, 3);
  } else if ((pointerdetector->cvImage->width != frame->info.width)
      || (pointerdetector->cvImage->height != frame->info.height)) {
    cvReleaseImage (&pointerdetector->cvImage);
    cvReleaseImage (&pointerdetector->cvImageAux1);
    pointerdetector->cvImage =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
    pointerdetector->cvImageAux1 =
        cvCreateImage (cvSize (pointerdetector->trackinRectSize.width,
            pointerdetector->trackinRectSize.height), IPL_DEPTH_8U, 3);
  }
}

static gboolean
kms_pointer_detector_check_pointer_into_button (CvPoint * pointer_position,
    ButtonStruct * buttonStruct)
{
  int downLeftCornerX =
      buttonStruct->cvButtonLayout.x + buttonStruct->cvButtonLayout.width;
  int downLeftCornerY =
      buttonStruct->cvButtonLayout.y + buttonStruct->cvButtonLayout.height;
  if (((pointer_position->x - buttonStruct->cvButtonLayout.x) > 0)
      && ((pointer_position->y - buttonStruct->cvButtonLayout.y) > 0)
      && ((pointer_position->x - downLeftCornerX) < 0)
      && ((pointer_position->y - downLeftCornerY) < 0)) {
    return TRUE;
  } else {
    return FALSE;
  }
}

static void
kms_pointer_detector_check_pointer_position (KmsPointerDetector *
    pointerdetector)
{
  ButtonStruct *structAux;
  GSList *l;
  int buttonClickedCounter = 0;
  const gchar *actualButtonClickedId;

  for (l = pointerdetector->buttonsLayoutList; l != NULL; l = l->next) {
    structAux = l->data;

    if (kms_pointer_detector_check_pointer_into_button
        (&pointerdetector->finalPointerPosition, structAux)) {
      CvPoint upRightCorner;
      CvPoint downLeftCorner;

      buttonClickedCounter++;
      upRightCorner.x = structAux->cvButtonLayout.x;
      upRightCorner.y = structAux->cvButtonLayout.y;
      downLeftCorner.x =
          structAux->cvButtonLayout.x + structAux->cvButtonLayout.width;
      downLeftCorner.y =
          structAux->cvButtonLayout.y + structAux->cvButtonLayout.height;
      cvRectangle (pointerdetector->cvImage, upRightCorner, downLeftCorner,
          GREEN, 1, 8, 0);
      actualButtonClickedId = structAux->id;
      GST_DEBUG ("TODO: send event to the bus");
    }
  }

  if (buttonClickedCounter == 0) {
    if (pointerdetector->previousButtonClickedId != NULL) {
      GstStructure *s;
      GstMessage *m;

      /* post a message to bus */
      GST_DEBUG ("exit window: %s", pointerdetector->previousButtonClickedId);
      s = gst_structure_new ("window-out",
          "window", G_TYPE_STRING, pointerdetector->previousButtonClickedId,
          NULL);
      m = gst_message_new_element (GST_OBJECT (pointerdetector), s);
      gst_element_post_message (GST_ELEMENT (pointerdetector), m);
      pointerdetector->previousButtonClickedId = NULL;
    }
  } else {
    if (pointerdetector->previousButtonClickedId != actualButtonClickedId) {
      GstStructure *s;
      GstMessage *m;

      /* post a message to bus */
      GST_DEBUG ("into window: %s", actualButtonClickedId);
      s = gst_structure_new ("window-in",
          "window", G_TYPE_STRING, pointerdetector->previousButtonClickedId,
          NULL);
      m = gst_message_new_element (GST_OBJECT (pointerdetector), s);
      gst_element_post_message (GST_ELEMENT (pointerdetector), m);
      pointerdetector->previousButtonClickedId = actualButtonClickedId;
    }
  }

}

static GstFlowReturn
kms_pointer_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (filter);
  GstMapInfo info;
  double min_Bhattacharyya = 1.0, bhattacharyya = 1, bhattacharyya2 =
      1, bhattacharyya3 = 1;
  int i = 0;

  pointerdetector->frameSize = cvSize (frame->info.width, frame->info.height);
  kms_pointer_detector_initialize_images (pointerdetector, frame);

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  pointerdetector->cvImage->imageData = (char *) info.data;

  if ((pointerdetector->iteration > FRAMES_TO_RESET)
      && (pointerdetector->state != CAPTURING_SECOND_HIST)) {
    get_histogram (pointerdetector->cvImage, pointerdetector->upCornerRect1,
        pointerdetector->trackinRectSize, pointerdetector->histSetUpRef);
    pointerdetector->histRefCapturesCounter = 0;
    pointerdetector->secondHistCapturesCounter = 0;
    pointerdetector->state = CAPTURING_REF_HIST;
    pointerdetector->colorRect1 = WHITE;
    pointerdetector->colorRect2 = WHITE;
    pointerdetector->iteration = 6;
  }
  if (pointerdetector->iteration == 5) {
    get_histogram (pointerdetector->cvImage, pointerdetector->upCornerRect1,
        pointerdetector->trackinRectSize, pointerdetector->histSetUpRef);
    pointerdetector->state = CAPTURING_REF_HIST;
    goto end;
  }

  if (pointerdetector->iteration < 6)
    goto end;

  get_histogram (pointerdetector->cvImage, pointerdetector->upCornerRect1,
      pointerdetector->trackinRectSize, pointerdetector->histSetUp1);
  bhattacharyya2 =
      cvCompareHist (pointerdetector->histSetUp1,
      pointerdetector->histSetUpRef, CV_COMP_BHATTACHARYYA);
  if ((bhattacharyya2 >= COMPARE_THRESH_SECOND_HIST)
      && (pointerdetector->state == CAPTURING_REF_HIST)) {
    pointerdetector->histRefCapturesCounter++;
    if (pointerdetector->histRefCapturesCounter > 20) {
      pointerdetector->histRefCapturesCounter = 0;
      pointerdetector->colorRect1 = CV_RGB (0, 255, 0);
      pointerdetector->state = CAPTURING_SECOND_HIST;
    }
  }
  if (pointerdetector->state == CAPTURING_SECOND_HIST) {
    get_histogram (pointerdetector->cvImage, pointerdetector->upCornerRect2,
        pointerdetector->trackinRectSize, pointerdetector->histSetUp2);
    bhattacharyya3 =
        cvCompareHist (pointerdetector->histSetUp1,
        pointerdetector->histSetUp2, CV_COMP_BHATTACHARYYA);
    if (bhattacharyya3 < COMPARE_THRESH_2_RECT) {
      pointerdetector->secondHistCapturesCounter++;
      if (pointerdetector->secondHistCapturesCounter > 15) {
        pointerdetector->secondHistCapturesCounter = 0;
        pointerdetector->state = BOTH_HIST_SIMILAR;
        pointerdetector->colorRect2 = CV_RGB (0, 255, 0);
        pointerdetector->histModel = pointerdetector->histSetUp2;
        pointerdetector->upCornerFinalRect.x = 10;
        pointerdetector->upCornerFinalRect.y = 10;
        pointerdetector->histRefCapturesCounter = 0;
        pointerdetector->secondHistCapturesCounter = 0;
      }
    }
  }
  for (i = 0; i < pointerdetector->numOfRegions; i++) {
    int horizOffset =
        pointerdetector->upCornerFinalRect.x +
        pointerdetector->windowScale * (rand () %
        pointerdetector->trackinRectSize.width -
        pointerdetector->trackinRectSize.width / 2);
    int vertOffset =
        pointerdetector->upCornerFinalRect.y +
        pointerdetector->windowScale * (rand () %
        pointerdetector->trackinRectSize.height -
        pointerdetector->trackinRectSize.height / 2);
    pointerdetector->trackingPoint1Aux.x = horizOffset;
    pointerdetector->trackingPoint1Aux.y = vertOffset;
    pointerdetector->trackingPoint2Aux.x =
        horizOffset + pointerdetector->trackinRectSize.width;
    pointerdetector->trackingPoint2Aux.y =
        vertOffset + pointerdetector->trackinRectSize.height;
    if ((horizOffset > 0)
        && (pointerdetector->trackingPoint2Aux.x <
            pointerdetector->cvImage->width)
        && (vertOffset > 0)
        && (pointerdetector->trackingPoint2Aux.y <
            pointerdetector->cvImage->height)) {
      if (pointerdetector->show_debug_info)
        cvRectangle (pointerdetector->cvImage,
            pointerdetector->trackingPoint1Aux,
            pointerdetector->trackingPoint2Aux, CV_RGB (0, 255, 0), 1, 8, 0);
      cvSetImageROI (pointerdetector->cvImage,
          cvRect (pointerdetector->trackingPoint1Aux.x,
              pointerdetector->trackingPoint1Aux.y,
              pointerdetector->trackinRectSize.width,
              pointerdetector->trackinRectSize.height));
      cvCopy (pointerdetector->cvImage, pointerdetector->cvImageAux1, 0);
      cvResetImageROI (pointerdetector->cvImage);
      calc_histogram (pointerdetector->cvImageAux1,
          pointerdetector->histCompare);
      bhattacharyya =
          cvCompareHist (pointerdetector->histModel,
          pointerdetector->histCompare, CV_COMP_BHATTACHARYYA);
      if ((bhattacharyya < min_Bhattacharyya)
          && (bhattacharyya < COMPARE_THRESH_HIST_REF)) {
        min_Bhattacharyya = bhattacharyya;
        pointerdetector->trackingPoint1 = pointerdetector->trackingPoint1Aux;
        pointerdetector->trackingPoint2 = pointerdetector->trackingPoint2Aux;
      }
    }
  }
  cvRectangle (pointerdetector->cvImage, pointerdetector->upCornerRect1,
      pointerdetector->downCornerRect1, pointerdetector->colorRect1, 1, 8, 0);
  cvRectangle (pointerdetector->cvImage, pointerdetector->upCornerRect2,
      pointerdetector->downCornerRect2, pointerdetector->colorRect2, 1, 8, 0);
  if (min_Bhattacharyya < 0.95) {
    pointerdetector->windowScale = pointerdetector->windowScaleRef;
  } else {
    pointerdetector->windowScale = pointerdetector->cvImage->width / 8;
  }
  CvPoint finalPointerPositionAux;

  finalPointerPositionAux.x = pointerdetector->upCornerFinalRect.x +
      pointerdetector->trackinRectSize.width / 2;
  finalPointerPositionAux.y = pointerdetector->upCornerFinalRect.y +
      pointerdetector->trackinRectSize.height / 2;
  if (abs (pointerdetector->finalPointerPosition.x -
          finalPointerPositionAux.x) < 55 ||
      abs (pointerdetector->finalPointerPosition.y -
          finalPointerPositionAux.y) < 55) {
    finalPointerPositionAux.x =
        (finalPointerPositionAux.x +
        pointerdetector->finalPointerPosition.x) / 2;
    finalPointerPositionAux.y =
        (finalPointerPositionAux.y +
        pointerdetector->finalPointerPosition.y) / 2;
  }
  pointerdetector->upCornerFinalRect = pointerdetector->trackingPoint1;
  pointerdetector->downCornerFinalRect = pointerdetector->trackingPoint2;

  pointerdetector->finalPointerPosition.x = finalPointerPositionAux.x;
  pointerdetector->finalPointerPosition.y = finalPointerPositionAux.y;

  cvCircle (pointerdetector->cvImage, pointerdetector->finalPointerPosition,
      10.0, WHITE, -1, 8, 0);

  kms_pointer_detector_check_pointer_position (pointerdetector);

end:
  pointerdetector->iteration++;
  gst_buffer_unmap (frame->buffer, &info);
  return GST_FLOW_OK;
}

gboolean
kms_pointer_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_POINTER_DETECTOR);
}
