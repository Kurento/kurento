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

#include "kmsvp8parse.h"

#include <string.h>

#include <gst/gst.h>
#include <gst/base/gstbaseparse.h>

#define PLUGIN_NAME "vp8parse"

#include <vpx/vpx_decoder.h>
#include <vpx/vp8dx.h>

#define GST_CAT_DEFAULT kms_vp8_parse_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_VP8_PARSE_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_VP8_PARSE,                         \
    KmsVp8ParsePrivate                          \
  )                                             \
)

struct _KmsVp8ParsePrivate
{
  gboolean started;

  gint width;
  gint height;

  gint framerate_num;
  gint framerate_denom;

  GstClockTime last_pts;
  GstClockTime last_dts;
};

/* pad templates */

#define VIDEO_SRC_CAPS "video/x-vp8"

#define VIDEO_SINK_CAPS "video/x-vp8"

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsVp8Parse, kms_vp8_parse,
    GST_TYPE_BASE_PARSE,
    GST_DEBUG_CATEGORY_INIT (kms_vp8_parse_debug_category, PLUGIN_NAME,
        0, "debug category for vp8parse element"));

static gboolean
kms_vp8_parse_start (GstBaseParse * parse)
{
  KmsVp8Parse *self = KMS_VP8_PARSE (parse);

  self->priv->started = FALSE;

  self->priv->height = -1;
  self->priv->width = -1;
  self->priv->framerate_denom = -1;
  self->priv->framerate_num = -1;

  self->priv->last_dts = GST_CLOCK_TIME_NONE;
  self->priv->last_pts = GST_CLOCK_TIME_NONE;

  return TRUE;
}

static gboolean
kms_vp8_parse_check_caps_ready (KmsVp8Parse * self)
{
  return (self->priv->framerate_denom != -1) &&
      (self->priv->framerate_num != -1)
      && (self->priv->width != -1) && (self->priv->height != -1);
}

static gboolean
kms_vp8_parse_detect_framerate (KmsVp8Parse * self, GstBaseParseFrame * frame)
{
  GValue value = G_VALUE_INIT;
  GstClockTime duration;
  gint num = 0, denom = 0;
  gboolean update_caps = FALSE;

  if (GST_CLOCK_TIME_IS_VALID (frame->buffer->duration)) {
    GST_INFO_OBJECT (self, "Using buffer duration");
    duration = frame->buffer->duration;
  } else if (GST_CLOCK_TIME_IS_VALID (self->priv->last_pts) &&
      GST_BUFFER_PTS_IS_VALID (frame->buffer)) {
    duration = frame->buffer->pts - self->priv->last_pts;
    GST_INFO_OBJECT (self, "Using pts difference");
  } else if (GST_CLOCK_TIME_IS_VALID (self->priv->last_dts) &&
      GST_BUFFER_PTS_IS_VALID (frame->buffer)) {
    duration = frame->buffer->dts - self->priv->last_dts;
    GST_INFO_OBJECT (self, "Using dts difference");
  } else {
    duration = GST_CLOCK_TIME_NONE;
    GST_INFO_OBJECT (self, "No framerate calculation");
  }

  if (duration == 0 || duration == GST_CLOCK_TIME_NONE) {
    return FALSE;
  }

  g_value_init (&value, GST_TYPE_FRACTION);
  gst_value_set_fraction (&value, (GST_SECOND / duration) * 1000, 1000);
  num = gst_value_get_fraction_numerator (&value);
  denom = gst_value_get_fraction_denominator (&value);

  if (num != 0) {
    if (self->priv->framerate_num != num) {
      GST_INFO_OBJECT (self, "Updating fps num: %d", num);
      self->priv->framerate_num = num;
      update_caps = TRUE;
    }

    if (self->priv->framerate_denom != denom) {
      GST_INFO_OBJECT (self, "Updating fps denom: %d", denom);
      self->priv->framerate_denom = denom;
      update_caps = TRUE;
    }
  }

  g_value_reset (&value);

  return update_caps;
}

static void
kms_vp8_parse_force_key_unit_event (GstBaseParse * self)
{
  GstStructure *s;
  GstEvent *key_unit_event;

  GST_DEBUG_OBJECT (self, "Request key frame");

  s = gst_structure_new ("GstForceKeyUnit",
      "all-headers", G_TYPE_BOOLEAN, TRUE, NULL);
  key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);

  gst_pad_push_event (GST_BASE_PARSE_SINK_PAD (self), key_unit_event);
}

static GstFlowReturn
kms_vp8_parse_handle_frame (GstBaseParse * parse, GstBaseParseFrame * frame,
    gint * skipsize)
{
  vpx_codec_stream_info_t stream_info;
  vpx_codec_err_t status;
  GstMapInfo minfo;
  gboolean update_caps = FALSE;
  KmsVp8Parse *self = KMS_VP8_PARSE (parse);

  if (!gst_buffer_map (frame->buffer, &minfo, GST_MAP_READ)) {
    GST_ERROR_OBJECT (parse, "Failed to map input buffer");
    return GST_FLOW_ERROR;
  }

  if ((GST_CLOCK_TIME_IS_VALID (frame->buffer->duration) ||
          GST_BUFFER_PTS_IS_VALID (frame->buffer) ||
          GST_BUFFER_DTS_IS_VALID (frame->buffer)) && !self->priv->started)
    gst_base_parse_set_has_timing_info (parse, TRUE);

  memset (&stream_info, 0, sizeof (stream_info));
  stream_info.sz = sizeof (stream_info);

  status = vpx_codec_peek_stream_info (&vpx_codec_vp8_dx_algo,
      minfo.data, minfo.size, &stream_info);

  if (status == VPX_CODEC_OK) {
    if (self->priv->height != stream_info.h) {
      self->priv->height = stream_info.h;
      GST_INFO_OBJECT (parse, "Updating height: %d", stream_info.h);
      update_caps = TRUE;
    }

    if (self->priv->width != stream_info.w) {
      self->priv->width = stream_info.w;
      GST_INFO_OBJECT (parse, "Updating width: %d", stream_info.w);
      update_caps = TRUE;
    }

    GST_BUFFER_FLAG_UNSET (frame->buffer, GST_BUFFER_FLAG_DELTA_UNIT);
    GST_BUFFER_FLAG_SET (frame->buffer, GST_BUFFER_FLAG_HEADER);
  } else if (!self->priv->started) {
    kms_vp8_parse_force_key_unit_event (parse);

    GST_BUFFER_FLAG_SET (frame->buffer, GST_BUFFER_FLAG_DELTA_UNIT);
    GST_BUFFER_FLAG_UNSET (frame->buffer, GST_BUFFER_FLAG_HEADER);
  }

  if (!self->priv->started)
    update_caps |= kms_vp8_parse_detect_framerate (self, frame);

  if (update_caps && kms_vp8_parse_check_caps_ready (self)) {
    GstCaps *caps;

    caps = gst_caps_new_simple ("video/x-vp8", "width", G_TYPE_INT,
        self->priv->width, "height", G_TYPE_INT, self->priv->height,
        "framerate", GST_TYPE_FRACTION, self->priv->framerate_num,
        self->priv->framerate_denom, NULL);

    GST_DEBUG_OBJECT (parse, "Caps %" GST_PTR_FORMAT, caps);

    gst_pad_set_caps (GST_BASE_PARSE_SRC_PAD (parse), caps);
    gst_caps_unref (caps);

    if (!self->priv->started)
      gst_base_parse_set_frame_rate (GST_BASE_PARSE (self),
          self->priv->framerate_num, self->priv->framerate_denom, 0, 0);

    if (!self->priv->started) {
      self->priv->started = TRUE;
      frame->flags |= GST_BASE_PARSE_FRAME_FLAG_QUEUE;
    }
  }

  if (!self->priv->started)
    frame->flags |= GST_BASE_PARSE_FRAME_FLAG_QUEUE;

  self->priv->last_dts = frame->buffer->dts;
  self->priv->last_pts = frame->buffer->pts;

  frame->size = minfo.size;

  gst_buffer_unmap (frame->buffer, &minfo);

  return gst_base_parse_finish_frame (parse, frame, frame->size);
}

void
kms_vp8_parse_dispose (GObject * object)
{
  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_vp8_parse_parent_class)->dispose (object);
}

void
kms_vp8_parse_finalize (GObject * object)
{
  G_OBJECT_CLASS (kms_vp8_parse_parent_class)->finalize (object);
}

static void
kms_vp8_parse_init (KmsVp8Parse * vp8parse)
{
  vp8parse->priv = KMS_VP8_PARSE_GET_PRIVATE (vp8parse);
}

static void
kms_vp8_parse_class_init (KmsVp8ParseClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstBaseParseClass *base_parse_class = GST_BASE_PARSE_CLASS (klass);

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
      "Vp8 parse element", "Codec/Parser/Converter/Video",
      "Parses vp8 video streams",
      "José Antonio Santos <santoscadenas@kurento.com>");

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_vp8_parse_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_vp8_parse_finalize);

  base_parse_class->start = GST_DEBUG_FUNCPTR (kms_vp8_parse_start);
  base_parse_class->handle_frame =
      GST_DEBUG_FUNCPTR (kms_vp8_parse_handle_frame);
  /* Properties initialization */

  g_type_class_add_private (klass, sizeof (KmsVp8ParsePrivate));
}

gboolean
kms_vp8_parse_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_MARGINAL,
      KMS_TYPE_VP8_PARSE);
}
