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

#include "kmsrtcpdemux.h"

#include <string.h>

#include <gst/gst.h>
#include <gst/base/gstbaseparse.h>
#include <gst/rtp/gstrtcpbuffer.h>
#include <gst/rtp/gstrtpbuffer.h>
#include "kms-marshal.h"

#define PLUGIN_NAME "rtcpdemux"

#define GST_CAT_DEFAULT kms_rtcp_demux_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_rtcp_demux_parent_class parent_class

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

  GHashTable *rr_ssrcs;         /* remote_ssrc - local_ssrc mapping */
};

/* Signals and args */
enum
{
  SIGNAL_GET_REMOTE_SSRC_PAIR,
  LAST_SIGNAL
};

static guint kms_rtcp_demux_signals[LAST_SIGNAL] = { 0 };

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

static guint32
kms_rtcp_demux_get_local_rr_ssrc_pair (KmsRtcpDemux * self, guint32 remote_ssrc)
{
  gpointer val;

  val =
      g_hash_table_lookup (self->priv->rr_ssrcs,
      GUINT_TO_POINTER (remote_ssrc));
  if (val == NULL) {
    return 0;
  }

  return GPOINTER_TO_UINT (val);
}

static gboolean
refresh_rtcp_rr_ssrcs_map (KmsRtcpDemux * rtcpdemux, GstBuffer * buffer)
{
  GstRTCPBuffer rtcp = { NULL, };
  GstRTCPPacket packet;
  GstRTCPType type;
  gboolean ret = TRUE;
  guint32 remote_ssrc, local_ssrc;

  gst_rtcp_buffer_map (buffer, GST_MAP_READ, &rtcp);

  if (!gst_rtcp_buffer_get_first_packet (&rtcp, &packet)) {
    ret = FALSE;
    goto end;
  }

  type = gst_rtcp_packet_get_type (&packet);

  if (type != GST_RTCP_TYPE_RR) {
    ret = TRUE;
    goto end;
  }

  remote_ssrc = gst_rtcp_packet_rr_get_ssrc (&packet);
  ret =
      g_hash_table_contains (rtcpdemux->priv->rr_ssrcs,
      GUINT_TO_POINTER (remote_ssrc));

  if (!ret && (gst_rtcp_packet_get_rb_count (&packet) > 0)) {
    gst_rtcp_packet_get_rb (&packet, 0, &local_ssrc, NULL, NULL, NULL, NULL,
        NULL, NULL);
    GST_DEBUG_OBJECT (rtcpdemux, "remote_ssrc (%u) - local_ssrc(%u)",
        remote_ssrc, local_ssrc);
    g_hash_table_insert (rtcpdemux->priv->rr_ssrcs,
        GUINT_TO_POINTER (remote_ssrc), GUINT_TO_POINTER (local_ssrc));
    ret = TRUE;
  }

end:
  gst_rtcp_buffer_unmap (&rtcp);

  return ret;
}

static gboolean
buffer_is_rtcp (GstBuffer * buffer)
{
  GstRTPBuffer rtp_buffer = { NULL };

  if (!gst_rtp_buffer_map (buffer, GST_MAP_READ, &rtp_buffer)) {
    return TRUE;
  }

  gst_rtp_buffer_unmap (&rtp_buffer);
  return FALSE;
}

static GstFlowReturn
kms_rtcp_demux_chain (GstPad * chain, GstObject * parent, GstBuffer * buffer)
{
  KmsRtcpDemux *self = KMS_RTCP_DEMUX (parent);

  if (!buffer_is_rtcp (buffer)) {
    GST_TRACE_OBJECT (self, "Push RTP buffer");
    gst_pad_push (self->priv->rtp_src, buffer);
    return GST_FLOW_OK;
  }

  if (refresh_rtcp_rr_ssrcs_map (self, buffer)) {
    GST_TRACE_OBJECT (self, "Push RTCP buffer");
    gst_pad_push (self->priv->rtcp_src, buffer);
  } else {
    gst_buffer_unref (buffer);
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

  rtcpdemux->priv->rr_ssrcs = g_hash_table_new (g_direct_hash, g_direct_equal);

  gst_pad_set_chain_function (sink, GST_DEBUG_FUNCPTR (kms_rtcp_demux_chain));
}

static void
kms_rtcp_demux_finalize (GObject * object)
{
  KmsRtcpDemux *self = KMS_RTCP_DEMUX (object);

  g_hash_table_unref (self->priv->rr_ssrcs);

  /* chain up */
  G_OBJECT_CLASS (kms_rtcp_demux_parent_class)->finalize (object);
}

static void
kms_rtcp_demux_class_init (KmsRtcpDemuxClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gst_element_class = GST_ELEMENT_CLASS (klass);

  gobject_class->finalize = kms_rtcp_demux_finalize;

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

  klass->get_local_rr_ssrc_pair = kms_rtcp_demux_get_local_rr_ssrc_pair;

  kms_rtcp_demux_signals[SIGNAL_GET_REMOTE_SSRC_PAIR] =
      g_signal_new ("get-local-rr-ssrc-pair",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsRtcpDemuxClass, get_local_rr_ssrc_pair), NULL, NULL,
      __kms_marshal_UINT__UINT, G_TYPE_UINT, 1, G_TYPE_UINT);

  g_type_class_add_private (klass, sizeof (KmsRtcpDemuxPrivate));
}

gboolean
kms_rtcp_demux_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTCP_DEMUX);
}
