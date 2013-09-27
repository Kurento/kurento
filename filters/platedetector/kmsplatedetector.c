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
static GstFlowReturn kms_plate_detector_transform_frame (GstVideoFilter *
    filter, GstVideoFrame * inframe, GstVideoFrame * outframe);
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
  video_filter_class->transform_frame =
      GST_DEBUG_FUNCPTR (kms_plate_detector_transform_frame);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_plate_detector_transform_frame_ip);

}

static void
kms_plate_detector_init (KmsPlateDetector * platedetector)
{
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

  /* clean up object here */

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

/* transform */
static GstFlowReturn
kms_plate_detector_transform_frame (GstVideoFilter * filter,
    GstVideoFrame * inframe, GstVideoFrame * outframe)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (filter);

  GST_DEBUG_OBJECT (platedetector, "transform_frame");

  return GST_FLOW_OK;
}

static GstFlowReturn
kms_plate_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPlateDetector *platedetector = KMS_PLATE_DETECTOR (filter);

  GST_DEBUG_OBJECT (platedetector, "transform_frame_ip");

  return GST_FLOW_OK;
}

gboolean
kms_plate_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLATE_DETECTOR);
}
