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
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include "kmsmovementdetector.h"
#include <opencv/highgui.h>

#define PLUGIN_NAME "movementdetector"

GST_DEBUG_CATEGORY_STATIC (kms_movement_detector_debug_category);
#define GST_CAT_DEFAULT kms_movement_detector_debug_category

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsMovementDetector, kms_movement_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_movement_detector_debug_category, PLUGIN_NAME,
        0, "debug category for movementdetector element"));

void
kms_movement_detector_dispose (GObject * object)
{
  KmsMovementDetector *movementdetector = KMS_MOVEMENT_DETECTOR (object);

  GST_DEBUG_OBJECT (movementdetector, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_movement_detector_parent_class)->dispose (object);
}

void
kms_movement_detector_finalize (GObject * object)
{
  KmsMovementDetector *movementdetector = KMS_MOVEMENT_DETECTOR (object);

  GST_DEBUG_OBJECT (movementdetector, "finalize");

  /* clean up object here */
  if (movementdetector->img != NULL) {
    cvReleaseImage (&movementdetector->img);
    movementdetector->img = NULL;
  }
  if (movementdetector->imgOldBW != NULL) {
    cvReleaseImage (&movementdetector->imgOldBW);
    movementdetector->imgOldBW = NULL;
  }

  G_OBJECT_CLASS (kms_movement_detector_parent_class)->finalize (object);
}

static gboolean
kms_movement_detector_start (GstBaseTransform * trans)
{
  KmsMovementDetector *movementdetector = KMS_MOVEMENT_DETECTOR (trans);

  GST_DEBUG_OBJECT (movementdetector, "start");

  return TRUE;
}

static gboolean
kms_movement_detector_stop (GstBaseTransform * trans)
{
  KmsMovementDetector *movementdetector = KMS_MOVEMENT_DETECTOR (trans);

  GST_DEBUG_OBJECT (movementdetector, "stop");

  return TRUE;
}

static gboolean
kms_movement_detector_set_info (GstVideoFilter * filter, GstCaps * incaps,
    GstVideoInfo * in_info, GstCaps * outcaps, GstVideoInfo * out_info)
{
  KmsMovementDetector *movementdetector = KMS_MOVEMENT_DETECTOR (filter);

  GST_DEBUG_OBJECT (movementdetector, "set_info");

  return TRUE;
}

static gboolean
kms_movement_detector_initialize_images (KmsMovementDetector * movementdetector,
    GstVideoFrame * frame)
{
  if (movementdetector->imgOldBW == NULL) {
    movementdetector->img =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
    return TRUE;
  } else if ((movementdetector->imgOldBW->width != frame->info.width)
      || (movementdetector->imgOldBW->height != frame->info.height)) {
    cvReleaseImage (&movementdetector->imgOldBW);
    movementdetector->img =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
    return TRUE;
  }

  return FALSE;
}

static GstFlowReturn
kms_movement_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsMovementDetector *movementdetector = KMS_MOVEMENT_DETECTOR (filter);
  GstMapInfo info;
  IplImage *imgBW, *imgDiff;
  CvMemStorage *mem;
  CvSeq *contours = 0;
  gboolean imagesChanged;

  //checking image sizes
  imagesChanged =
      kms_movement_detector_initialize_images (movementdetector, frame);

  //get current frame
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  movementdetector->img->imageData = (char *) info.data;
  imgBW = cvCreateImage (cvGetSize (movementdetector->img),
      movementdetector->img->depth, 1);

  cvCvtColor (movementdetector->img, imgBW, CV_BGR2GRAY);
  if (imagesChanged) {
    movementdetector->imgOldBW = imgBW;
    goto end;
  }
  //image difference
  imgDiff = cvCreateImage (cvGetSize (movementdetector->img),
      movementdetector->img->depth, 1);

  cvSub (movementdetector->imgOldBW, imgBW, imgDiff, NULL);
  cvThreshold (imgDiff, imgDiff, 125, 255, CV_THRESH_OTSU);
  cvErode (imgDiff, imgDiff, NULL, 1);
  cvDilate (imgDiff, imgDiff, NULL, 1);

  mem = cvCreateMemStorage (0);
  cvFindContours (imgDiff, mem, &contours, sizeof (CvContour), CV_RETR_CCOMP,
      CV_CHAIN_APPROX_NONE, cvPoint (0, 0));

  for (; contours != 0; contours = contours->h_next) {
    CvRect rect = cvBoundingRect (contours, 0);

    cvRectangle (movementdetector->img, cvPoint (rect.x, rect.y),
        cvPoint (rect.x + rect.width, rect.y + rect.width), cvScalar (255, 0, 0,
            0), 2, 8, 0);

  }

  cvReleaseImage (&movementdetector->imgOldBW);
  movementdetector->imgOldBW = imgBW;

  cvReleaseImage (&imgDiff);
  cvReleaseMemStorage (&mem);

end:
  gst_buffer_unmap (frame->buffer, &info);
  return GST_FLOW_OK;
}

static void
kms_movement_detector_class_init (KmsMovementDetectorClass * klass)
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
      "Movement detector element", "Video/Filter",
      "It detects movement of the objects and it raises events with its position",
      "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->dispose = kms_movement_detector_dispose;
  gobject_class->finalize = kms_movement_detector_finalize;
  base_transform_class->start = GST_DEBUG_FUNCPTR (kms_movement_detector_start);
  base_transform_class->stop = GST_DEBUG_FUNCPTR (kms_movement_detector_stop);
  video_filter_class->set_info =
      GST_DEBUG_FUNCPTR (kms_movement_detector_set_info);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_movement_detector_transform_frame_ip);
}

static void
kms_movement_detector_init (KmsMovementDetector * movementdetector)
{
  movementdetector->imgOldBW = NULL;
  movementdetector->img = NULL;
}

gboolean
kms_movement_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MOVEMENT_DETECTOR);
}
