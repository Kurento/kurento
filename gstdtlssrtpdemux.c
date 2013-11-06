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

#include "gstdtlssrtpdemux.h"

GST_DEBUG_CATEGORY_STATIC (dtls_srtp_demux_debug);
#define GST_CAT_DEFAULT (dtls_srtp_demux_debug)

G_DEFINE_TYPE (GstDtlsSrtpDemux, gst_dtls_srtp_demux, GST_TYPE_ELEMENT);


static GstStaticPadTemplate gst_dtls_srtp_demux_sink_template =
GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate gst_dtls_srtp_demux_dtls_src_template =
GST_STATIC_PAD_TEMPLATE ("dtls_src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-dtls"));

static GstStaticPadTemplate gst_dtls_srtp_demux_srtp_src_template =
    GST_STATIC_PAD_TEMPLATE ("srtp_src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-srtp; application/x-rtp;"
        "application/x-srtcp; application/x-rtcp"));

static GstFlowReturn gst_dtls_srtp_demux_chain (GstPad * pad,
    GstObject * parent, GstBuffer * buffer);

static void
gst_dtls_srtp_demux_class_init (GstDtlsSrtpDemuxClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (dtls_srtp_demux_debug, "dtlssrtpdemux", 0,
      "DTLS-SRTP demultiplexer");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_demux_sink_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_demux_dtls_src_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_demux_srtp_src_template));

  gst_element_class_set_static_metadata (gstelement_class,
      "DTLS-SRTP demultiplexer",
      "Demux/Network",
      "Demultiplexes DTLS and RTP/RTCP/SRTP/SRTCP packets",
      "Olivier Crete <olivier.crete@collabora.com>");
}

static void
gst_dtls_srtp_demux_init (GstDtlsSrtpDemux * self)
{

  self->dtls_srcpad =
      gst_pad_new_from_static_template (&gst_dtls_srtp_demux_dtls_src_template,
      "dtls_src");
  gst_element_add_pad (GST_ELEMENT (self), self->dtls_srcpad);

  self->srtp_srcpad =
      gst_pad_new_from_static_template (&gst_dtls_srtp_demux_srtp_src_template,
      "srtp_src");
  gst_element_add_pad (GST_ELEMENT (self), self->srtp_srcpad);

  self->sinkpad =
      gst_pad_new_from_static_template (&gst_dtls_srtp_demux_sink_template,
      "sink");
  gst_pad_set_chain_function (self->sinkpad, gst_dtls_srtp_demux_chain);
  gst_element_add_pad (GST_ELEMENT (self), self->sinkpad);
}


static GstFlowReturn
gst_dtls_srtp_demux_chain (GstPad * pad, GstObject * parent, GstBuffer * buffer)
{
  GstDtlsSrtpDemux *self = GST_DTLS_SRTP_DEMUX (parent);
  GstMapInfo map;
  guint8 first_byte;


  if (!gst_buffer_map_range (buffer, 0, 1, &map, GST_MAP_READ)) {
    gst_buffer_unref (buffer);
    GST_ELEMENT_ERROR (self, STREAM, DEMUX, ("Could not map buffer"),
        ("Unable to map the first memory of buffer %p", buffer));
    return GST_FLOW_ERROR;
  }

  if (map.size < 1) {
    gst_buffer_unmap (buffer, &map);
    GST_WARNING_OBJECT (self, "Buffer %p has a first GstMemory that's less than"
        " one bytes long, ignoring", buffer);
    goto done;
  }

  first_byte = map.data[0];

  gst_buffer_unmap (buffer, &map);

  if (first_byte > 127 && first_byte < 192) {
    return gst_pad_push (self->srtp_srcpad, buffer);
  } else if (first_byte > 19 && first_byte < 64) {
    return gst_pad_push (self->dtls_srcpad, buffer);
  }

  GST_WARNING_OBJECT (self, "Buffer %p has first byte %d which is neither DTLS"
      " nor SRTP/RTP nor SRTCP/RTCP, ignoring it", buffer, first_byte);
done:

  gst_buffer_unref (buffer);
  return GST_FLOW_OK;
}
