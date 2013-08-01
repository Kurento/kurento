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
  PROP_0
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* FIXME: add/remove formats you can handle */
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

}

static void
kms_pointer_detector_init (KmsPointerDetector * pointerdetector)
{
  pointerdetector->cvImage = NULL;
}

void
kms_pointer_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  GST_DEBUG_OBJECT (pointerdetector, "set_property");

  switch (property_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
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

  cvReleaseImageHeader (&pointerdetector->cvImage);

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

/* transform */
static GstFlowReturn
kms_pointer_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (filter);
  GstMapInfo info;

  if (pointerdetector->cvImage == NULL) {
    pointerdetector->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
  } else if ((pointerdetector->cvImage->width != frame->info.width)
      || (pointerdetector->cvImage->height != frame->info.height)) {
    cvReleaseImageHeader (&pointerdetector->cvImage);
    pointerdetector->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
  }

  /* load Gstreamer buffer into IplImage */
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  pointerdetector->cvImage->imageData = (char *) info.data;

  // TODO: Process image here

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

gboolean
kms_pointer_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_POINTER_DETECTOR);
}
