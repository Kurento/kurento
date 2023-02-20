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

#include "kmsmovementdetector.hpp"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>

#include <opencv2/imgproc.hpp> // cvtColor

#include <vector>

#define PLUGIN_NAME "kmsmovementdetector"
#define GST_CAT_DEFAULT kms_movement_detector_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

/* pad templates */

#define VIDEO_SRC_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

#define VIDEO_SINK_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsMovementDetector,
    kms_movement_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT,
        PLUGIN_NAME,
        0,
        "GStreamer debug category for the '" PLUGIN_NAME "' element"));

void
kms_movement_detector_dispose (GObject *object)
{
  KmsMovementDetector *self = KMS_MOVEMENT_DETECTOR (object);

  GST_DEBUG_OBJECT (self, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_movement_detector_parent_class)->dispose (object);
}

void
kms_movement_detector_finalize (GObject *object)
{
  KmsMovementDetector *self = KMS_MOVEMENT_DETECTOR (object);

  GST_DEBUG_OBJECT (self, "finalize");

  /* clean up object here */
  if (!self->img.empty ()) {
    self->img.release ();
  }
  if (!self->imgOldBW.empty ()) {
    self->imgOldBW.release ();
  }

  G_OBJECT_CLASS (kms_movement_detector_parent_class)->finalize (object);
}

static gboolean
kms_movement_detector_start (GstBaseTransform *trans)
{
  KmsMovementDetector *self = KMS_MOVEMENT_DETECTOR (trans);

  GST_DEBUG_OBJECT (self, "start");

  return TRUE;
}

static gboolean
kms_movement_detector_stop (GstBaseTransform *trans)
{
  KmsMovementDetector *self = KMS_MOVEMENT_DETECTOR (trans);

  GST_DEBUG_OBJECT (self, "stop");

  return TRUE;
}

static gboolean
kms_movement_detector_set_info (GstVideoFilter *filter,
    GstCaps *incaps,
    GstVideoInfo *in_info,
    GstCaps *outcaps,
    GstVideoInfo *out_info)
{
  KmsMovementDetector *self = KMS_MOVEMENT_DETECTOR (filter);

  GST_DEBUG_OBJECT (self, "set_info");

  return TRUE;
}

static gboolean
kms_movement_detector_initialize_images (KmsMovementDetector *self,
    GstVideoFrame *frame)
{
  const int width = GST_VIDEO_FRAME_WIDTH (frame);
  const int height = GST_VIDEO_FRAME_HEIGHT (frame);
  const void *data = GST_VIDEO_FRAME_PLANE_DATA (frame, 0);
  const size_t step = GST_VIDEO_FRAME_PLANE_STRIDE (frame, 0);

  self->img = cv::Mat (cv::Size (width, height), CV_8UC3, (void *)data, step);

  if (self->imgOldBW.empty ()
      || self->imgOldBW.size () != cv::Size (width, height)) {
    return TRUE;
  }

  return FALSE;
}

static GstFlowReturn
kms_movement_detector_transform_frame_ip (GstVideoFilter *filter,
    GstVideoFrame *frame)
{
  KmsMovementDetector *self = KMS_MOVEMENT_DETECTOR (filter);
  cv::Mat imgBW, imgDiff;
  gboolean imagesChanged;
  std::vector<std::vector<cv::Point>> contours;

  //checking image sizes
  imagesChanged = kms_movement_detector_initialize_images (self, frame);

  imgBW = cv::Mat (self->img.size (), CV_8UC1);

  cv::cvtColor (self->img, imgBW, cv::COLOR_BGR2GRAY);

  if (imagesChanged) {
    imgBW.copyTo (self->imgOldBW);
    goto end;
  }

  //image difference
  imgDiff = cv::Mat (self->img.size (), CV_8UC1);

  cv::subtract (self->imgOldBW, imgBW, imgDiff);
  cv::threshold (imgDiff, imgDiff, 125, 255, cv::THRESH_OTSU);
  cv::erode (imgDiff, imgDiff, cv::Mat ());
  cv::dilate (imgDiff, imgDiff, cv::Mat ());

  cv::findContours (imgDiff, contours, cv::RETR_CCOMP, cv::CHAIN_APPROX_NONE);

  for (const auto &contour : contours) {
    cv::Rect rect = cv::boundingRect (contour);
    cv::rectangle (self->img, cv::Point (rect.x, rect.y),
        cv::Point (rect.x + rect.width, rect.y + rect.width),
        cv::Scalar (255, 0, 0, 0), 2);
  }

  self->imgOldBW.release ();
  self->imgOldBW = imgBW;

  imgDiff.release ();

end:
  return GST_FLOW_OK;
}

static void
kms_movement_detector_class_init (KmsMovementDetectorClass *klass)
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
kms_movement_detector_init (KmsMovementDetector *self)
{
}

gboolean
kms_movement_detector_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MOVEMENT_DETECTOR);
}
