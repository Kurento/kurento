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

#include "kmsrtcpdemux.h"

#include <string.h>

#include <gst/gst.h>
#include <gst/base/gstbaseparse.h>
#include <gst/rtp/gstrtcpbuffer.h>

#define PLUGIN_NAME "rtcpdemux"

#define GST_CAT_DEFAULT kms_rtcp_demux_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_RTCP_DEMUX_GET_PRIVATE(obj) (       \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_RTCP_DEMUX,                        \
    KmsRtcpDemuxPrivate                         \
  )                                             \
)

struct _KmsRtcpDemuxPrivate
{
  GstPad *rtp_src;
  GstPad *rtcp_src;
};

/* pad templates */

#define RTP_SRC_CAPS "application/x-srtp;application/x-rtp"
#define RTCP_SRC_CAPS "application/x-srtcp;application/x-rtcp"

#define SINK_CAPS "application/x-srtcp;application/x-srtp;"     \
    "application/x-srtcp-mux;"                                  \
    "application/x-rtcp;application/x-rtp;"                     \
    "application/x-rtcp-mux;"

static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (SINK_CAPS)
    );

static GstStaticPadTemplate rtp_src_template =
GST_STATIC_PAD_TEMPLATE ("rtp_src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (RTP_SRC_CAPS)
    );

static GstStaticPadTemplate rtcp_src_template =
GST_STATIC_PAD_TEMPLATE ("rtcp_src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (RTCP_SRC_CAPS)
    );

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRtcpDemux, kms_rtcp_demux,
    GST_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_rtcp_demux_debug_category, PLUGIN_NAME,
        0, "debug category for rtcpdemux element"));

static GstFlowReturn
kms_rtcp_demux_chain (GstPad * chain, GstObject * parent, GstBuffer * buffer)
{
  KmsRtcpDemux *self = KMS_RTCP_DEMUX (parent);

  if (gst_rtcp_buffer_validate (buffer)) {
    GST_TRACE_OBJECT (chain, "Buffer is RTCP");
    gst_pad_push (self->priv->rtcp_src, buffer);
  } else {
    gst_pad_push (self->priv->rtp_src, buffer);
  }

  return GST_FLOW_OK;
}

static void
kms_rtcp_demux_init (KmsRtcpDemux * rtcpdemux)
{
  GstPadTemplate *tmpl;
  GstPad *sink;

  rtcpdemux->priv = KMS_RTCP_DEMUX_GET_PRIVATE (rtcpdemux);

  tmpl = gst_static_pad_template_get (&rtp_src_template);
  rtcpdemux->priv->rtp_src =
      gst_pad_new_from_template (tmpl, tmpl->name_template);
  g_object_unref (tmpl);
  gst_element_add_pad (GST_ELEMENT (rtcpdemux), rtcpdemux->priv->rtp_src);

  tmpl = gst_static_pad_template_get (&rtcp_src_template);
  rtcpdemux->priv->rtcp_src =
      gst_pad_new_from_template (tmpl, tmpl->name_template);
  g_object_unref (tmpl);
  gst_element_add_pad (GST_ELEMENT (rtcpdemux), rtcpdemux->priv->rtcp_src);

  tmpl = gst_static_pad_template_get (&sink_template);
  sink = gst_pad_new_from_template (tmpl, tmpl->name_template);
  g_object_unref (tmpl);
  gst_element_add_pad (GST_ELEMENT (rtcpdemux), sink);

  gst_pad_set_chain_function (sink, GST_DEBUG_FUNCPTR (kms_rtcp_demux_chain));
}

static void
kms_rtcp_demux_class_init (KmsRtcpDemuxClass * klass)
{
  GstElementClass *gst_element_class = GST_ELEMENT_CLASS (klass);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (gst_element_class,
      gst_static_pad_template_get (&rtp_src_template));
  gst_element_class_add_pad_template (gst_element_class,
      gst_static_pad_template_get (&rtcp_src_template));
  gst_element_class_add_pad_template (gst_element_class,
      gst_static_pad_template_get (&sink_template));

  gst_element_class_set_static_metadata (gst_element_class,
      "Rtcp/rtp package demuxer", "Demux/Network/RTP",
      "Demuxes rtp and rtcp flows",
      "José Antonio Santos <santoscadenas@kurento.com>");

  g_type_class_add_private (klass, sizeof (KmsRtcpDemuxPrivate));
}

gboolean
kms_rtcp_demux_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTCP_DEMUX);
}
