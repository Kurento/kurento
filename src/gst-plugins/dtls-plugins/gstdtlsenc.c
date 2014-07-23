/*
 * GStreamer
 *
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdtlsconnection.h"

GST_DEBUG_CATEGORY_STATIC (dtls_enc_debug);
#define GST_CAT_DEFAULT (dtls_enc_debug)

G_DEFINE_TYPE (GstDtlsEnc, gst_dtls_enc, GST_TYPE_DTLS_BASE);

static GstStaticPadTemplate gst_dtls_enc_sink_template =
GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate gst_dtls_enc_src_template =
GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-dtls"));

static GstFlowReturn gst_dtls_enc_chain (GstDtlsBase * base,
    GstBuffer * buffer);

static GstStateChangeReturn gst_dtls_enc_change_state (GstElement * element,
    GstStateChange transition);

static void
gst_dtls_enc_class_init (GstDtlsEncClass * klass)
{
  GstElementClass *gstelement_class = (GstElementClass *) klass;
  GstDtlsBaseClass *base_class = (GstDtlsBaseClass *) klass;

  GST_DEBUG_CATEGORY_INIT (dtls_enc_debug, "dtlsenc", 0, "DTLS Encrypter");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_enc_src_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_enc_sink_template));
  gst_element_class_set_static_metadata (gstelement_class, "DTLS Encrypter",
      "Generic",
      "Encrypts packets using DTLS",
      "Olivier Crete <olivier.crete@collabora.com>");

  gstelement_class->change_state = gst_dtls_enc_change_state;
  base_class->chain = gst_dtls_enc_chain;
}

static void
gst_dtls_enc_init (GstDtlsEnc * enc)
{
}

static GstFlowReturn
gst_dtls_enc_chain (GstDtlsBase * base, GstBuffer * buffer)
{
  GstDtlsEnc *self = GST_DTLS_ENC (base);
  gssize ret;
  GstMapInfo map;
  GError *error = NULL;

  if (gst_buffer_get_size (buffer) == 0) {
    gst_buffer_unref (buffer);
    return GST_FLOW_OK;
  }

  if (!gst_buffer_map (buffer, &map, GST_MAP_READ)) {
    GST_ELEMENT_ERROR (base, RESOURCE, READ, ("Can't map buffer"),
        ("Can't map buffer"));
    gst_buffer_unref (buffer);
    return GST_FLOW_ERROR;
  }
  GST_OBJECT_LOCK (self);
  self->src_buffer = buffer;
  self->running_thread = g_thread_self ();
  GST_OBJECT_UNLOCK (self);
  ret =
      g_output_stream_write (g_io_stream_get_output_stream (G_IO_STREAM
          (base->conn->conn)), map.data, map.size, NULL, &error);

  g_assert (ret < 0 || ret == map.size);

  if (ret > 0 && ret != map.size)
    ret = -10;

  GST_OBJECT_LOCK (self);
  self->src_buffer = NULL;
  GST_OBJECT_UNLOCK (self);

  gst_buffer_unmap (buffer, &map);
  gst_buffer_unref (buffer);

  if (ret > 0) {
    return GST_FLOW_OK;
  } else {
    if (error) {
      GstFlowReturn flow = GST_FLOW_ERROR;

      if (error->domain == GST_IO_STREAM_FLOW_RETURN) {
        flow = error->code;
      } else {
        GST_ELEMENT_ERROR (base, LIBRARY, FAILED,
            ("DTLS encoding failed: %s", error->message),
            ("DTLS encoding failed: %s", error->message));
      }

      g_clear_error (&error);

      return flow;
    } else {
      GST_ELEMENT_ERROR (base, LIBRARY, FAILED, ("Unknown encoding error"),
          ("Unknown encoding error"));
      return GST_FLOW_ERROR;
    }
  }
}

static GstFlowReturn
gst_dtls_enc_push (GstDtlsEnc * self, GstBuffer * buffer)
{
  GstDtlsBase *base = GST_DTLS_BASE (self);
  GstEvent *segment_event, *caps_event;
  gchar *stream_id;

  stream_id = gst_pad_get_stream_id (base->srcpad);

  if (stream_id == NULL) {
    stream_id = gst_pad_get_stream_id (base->sinkpad);

    if (stream_id == NULL) {
      stream_id = gst_pad_create_stream_id (base->srcpad,
          GST_ELEMENT (base), NULL);
    }

    gst_pad_push_event (base->srcpad, gst_event_new_stream_start (stream_id));
  }

  g_free (stream_id);

  caps_event = gst_pad_get_sticky_event (base->srcpad, GST_EVENT_CAPS, 0);

  if (caps_event == NULL) {
    GstCaps *caps = gst_caps_from_string ("application/x-dtls");

    caps_event = gst_event_new_caps (caps);
    gst_caps_unref (caps);

    gst_pad_push_event (base->srcpad, caps_event);
  } else {
    gst_event_unref (caps_event);
  }

  segment_event = gst_pad_get_sticky_event (base->srcpad, GST_EVENT_SEGMENT, 0);

  if (segment_event == NULL) {
    GstSegment *segment = gst_segment_new ();

    gst_segment_init (segment, GST_FORMAT_BYTES);
    segment_event = gst_event_new_segment (segment);
    gst_segment_free (segment);

    gst_pad_push_event (base->srcpad, segment_event);
  } else {
    gst_event_unref (segment_event);
  }

  GST_OBJECT_LOCK (self);
  if (self->src_buffer && self->running_thread == g_thread_self ()) {
    g_assert (gst_buffer_get_size (buffer) != 181);
    gst_buffer_copy_into (buffer, self->src_buffer, GST_BUFFER_COPY_METADATA, 0,
        -1);
  }
  GST_OBJECT_UNLOCK (self);

  return gst_pad_push (base->srcpad, buffer);
}

static GstStateChangeReturn
gst_dtls_enc_change_state (GstElement * element, GstStateChange transition)
{
  GstDtlsBase *base = GST_DTLS_BASE (element);
  GstStateChangeReturn ret;

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      gst_output_stream_set_push_function (GST_OUTPUT_STREAM
          (g_io_stream_get_output_stream (G_IO_STREAM (base->conn->
                      base_stream))),
          (GstOutputStreamPushFunc) gst_dtls_enc_push, element);
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (gst_dtls_enc_parent_class)->change_state (element,
      transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      gst_output_stream_set_push_function (GST_OUTPUT_STREAM
          (g_io_stream_get_output_stream (G_IO_STREAM (base->conn->
                      base_stream))), NULL, NULL);
      break;
    default:
      break;
  }
  return ret;
}
