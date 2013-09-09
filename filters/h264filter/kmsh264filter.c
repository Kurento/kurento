#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsh264filter.h"

#include <gst/gst.h>
#include <gst/base/gstbasetransform.h>

#define PLUGIN_NAME "h264filter"

#define GST_CAT_DEFAULT kms_h264_filter_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

enum
{
  PROP_0
};

/* pad templates */

#define VIDEO_SRC_CAPS "video/x-h264"

#define VIDEO_SINK_CAPS "video/x-h264"

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsH264Filter, kms_h264_filter,
    GST_TYPE_BASE_TRANSFORM,
    GST_DEBUG_CATEGORY_INIT (kms_h264_filter_debug_category, PLUGIN_NAME,
        0, "debug category for h264filter element"));

void
kms_h264_filter_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  switch (property_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_h264_filter_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  switch (property_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static GstFlowReturn
kms_h264_filter_transform_frame_ip (GstBaseTransform * base_transform,
    GstBuffer * buffer)
{
  GstMapInfo info;

  gst_buffer_map (buffer, &info, GST_MAP_READ);
  // This is the point where the media processing is done
  //    (probably, calling to external libraries):
  //
  // info.data contains the buffer information in a char array
  //
  GST_DEBUG ("Buffer len: %d", info.size);
  gst_buffer_unmap (buffer, &info);

  return GST_FLOW_OK;
}

void
kms_h264_filter_dispose (GObject * object)
{
  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_h264_filter_parent_class)->dispose (object);
}

void
kms_h264_filter_finalize (GObject * object)
{
  G_OBJECT_CLASS (kms_h264_filter_parent_class)->finalize (object);
}

static void
kms_h264_filter_init (KmsH264Filter * h264filter)
{
}

static void
kms_h264_filter_class_init (KmsH264FilterClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstBaseTransformClass *base_transform_class =
      GST_BASE_TRANSFORM_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  GST_DEBUG ("class init");

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "H264 filter element", "Video/Filter",
      "Process h264 video streams",
      "José Antonio Santos <santoscadenas@kurento.com>");

  gobject_class->set_property = kms_h264_filter_set_property;
  gobject_class->get_property = kms_h264_filter_get_property;
  gobject_class->dispose = kms_h264_filter_dispose;
  gobject_class->finalize = kms_h264_filter_finalize;

  base_transform_class->transform_ip =
      GST_DEBUG_FUNCPTR (kms_h264_filter_transform_frame_ip);

  /* Properties initialization */
}

gboolean
kms_h264_filter_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_H264_FILTER);
}
