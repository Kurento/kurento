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
 */

/*
 * This element demuxes RTCP packets according to type-specific SSRC fields
 * that are not the basic Source SSRC like the "ssrcdemux" element understands.
 *
 * The intention here is to help with identifying the media kind that matches
 * with an RTCP packet, which not always is decided by the Source SSRC.
 *
 * Reference: "Guidelines for Using the Multiplexing Features of RTP to Support Multiple Media Streams"
 * RFC 8872 | https://www.rfc-editor.org/rfc/rfc8872.html
 *
 * For example, in RTCP Receiver Report (RTCP-RR) packets the Source SSRC is
 * meaningless, and the important identifier is the Media SSRC that comes in
 * any of the possible several Report Blocks embedded in the packet. In this
 * case, a copy of the whole RTCP packet should be sent to the appropriate RTCP
 * sink pads of the RtpBin, regardless of the RTCP Source SSRC.
 *
 * In case of RTCP Payload-Specific Feedback (RTCP-PSFB), something similar
 * happens: the Source SSRC is irrelevant, we just care about the Media SSRC
 * which tells us which local media this feedback is about.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsrtcpdemux.h"

#include <commons/sdp_utils.h> // SSRC_INVALID
#include <commons/kmsremb.h>
#include <commons/kmsrtcp.h>

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
  GHashTable *rtcp_src_ssrc; // <Local SSRC, Src GstPad>.

  GHashTable *rr_ssrcs; // <Remote SSRC, Local SSRC>.
};

/* Signals and args */
enum
{
  SIGNAL_GET_REMOTE_SSRC_PAIR,
  SIGNAL_NEW_SSRC_PAD,
  LAST_SIGNAL
};

static guint kms_rtcp_demux_signals[LAST_SIGNAL] = { 0 };

/* pad templates */

#define SINK_CAPS ("application/x-rtp;" "application/x-rtcp")

#define RTP_SRC_CAPS ("application/x-rtp")
#define RTCP_SRC_CAPS ("application/x-rtcp")

static GstStaticPadTemplate sink_template =
GST_STATIC_PAD_TEMPLATE ("sink",
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

static GstStaticPadTemplate rtcp_src_ssrc_template =
GST_STATIC_PAD_TEMPLATE ("rtcp_src_%u",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
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
    GST_DEBUG_OBJECT (self, "Unknown remote SSRC: %u", remote_ssrc);
    return SSRC_INVALID;
  }

  return GPOINTER_TO_UINT (val);
}

static void
handle_rtcp_ssrc (KmsRtcpDemux *self, GstBuffer *buffer, guint32 local_ssrc)
{
  // Get the output pad for this SSRC.
  GstPad *rtcp_pad = NULL;
  if (g_hash_table_contains (self->priv->rtcp_src_ssrc,
          GUINT_TO_POINTER (local_ssrc))) {
    rtcp_pad = g_hash_table_lookup (self->priv->rtcp_src_ssrc,
        GUINT_TO_POINTER (local_ssrc));
  } else {
    // Make a new pad that can be used to push buffers.
    gchar *pad_name = g_strdup_printf ("rtcp_src_%u", local_ssrc);
    // Returns a floating ref (unowned).
    rtcp_pad =
        gst_pad_new_from_static_template (&rtcp_src_ssrc_template, pad_name);
    g_free (pad_name);

    gst_pad_use_fixed_caps (rtcp_pad);
    gst_pad_set_active (rtcp_pad, TRUE);

    // Add the new pad to this element (takes the floating ref).
    // This will emit the "pad-added" signal on the element.
    gst_element_add_pad (GST_ELEMENT (self), rtcp_pad);

    // Store the new pad.
    g_hash_table_insert (self->priv->rtcp_src_ssrc,
        GUINT_TO_POINTER (local_ssrc), gst_object_ref (rtcp_pad));

    GST_DEBUG_OBJECT (self, "New srcpad for local SSRC: %u", local_ssrc);

    g_signal_emit (self, kms_rtcp_demux_signals[SIGNAL_NEW_SSRC_PAD], 0,
        local_ssrc, rtcp_pad);
  }

  if (rtcp_pad != NULL) {
    // Push a copy of the RTCP buffer throught the appropriate src pad.
    gst_pad_push (rtcp_pad, gst_buffer_copy (buffer));
  }
}

static void
handle_rtcp_rr (KmsRtcpDemux *self, GstBuffer *buffer, GstRTCPPacket *packet)
{
  const guint32 remote_ssrc = gst_rtcp_packet_rr_get_ssrc (packet);
  const guint rb_count = gst_rtcp_packet_get_rb_count (packet);

  if (rb_count == 0) {
    // Invalid RTCP. Without Report Blocks in the RTCP packet, we cannot know
    // the local SSRCs to which the RTCP packet refers to.
    return;
  }

  for (guint i = 0; i < rb_count; ++i) {
    guint32 local_ssrc;
    gst_rtcp_packet_get_rb (packet, i, &local_ssrc, NULL, NULL, NULL, NULL,
        NULL, NULL);

    GST_TRACE_OBJECT (self,
        "Got RTCP-RR with remote SSRC: %u, local SSRC: %u, RB count: %u",
        remote_ssrc, local_ssrc, rb_count);

    // Store the match between remote (Source) and local (Media) SSRC.
    //
    // NOTE: This is how the original code in Kurento was able to know which
    // remote SSRC corresponds to our local ones, when the remote SDP message
    // didn't include them in the first place (with "a=ssrc" attributes).
    // Its implementation was half-baked because it assumed just 1 single
    // Report Block per RTCP packet... but I guess when Kurento acts as a
    // receiver that's actually the usual case, so I'm keeping that behavior.
    if (i == 0) {
      if (!g_hash_table_contains (self->priv->rr_ssrcs,
              GUINT_TO_POINTER (remote_ssrc))) {
        g_hash_table_insert (self->priv->rr_ssrcs,
            GUINT_TO_POINTER (remote_ssrc), GUINT_TO_POINTER (local_ssrc));
      }
    }

    handle_rtcp_ssrc (self, buffer, local_ssrc);
  }
}

static void
handle_rtcp_fb (KmsRtcpDemux *self, GstBuffer *buffer, GstRTCPPacket *packet)
{
  const guint32 remote_ssrc = gst_rtcp_packet_fb_get_sender_ssrc (packet);
  const GstRTCPFBType type = gst_rtcp_packet_fb_get_type (packet);

  if (type == GST_RTCP_PSFB_TYPE_FIR) {
    guint8 *fci = gst_rtcp_packet_fb_get_fci (packet);
    guint32 local_ssrc = GST_READ_UINT32_BE (fci);

    GST_TRACE_OBJECT (self,
        "Got RTCP-PSFB-FIR with remote SSRC: %u, local SSRC: %u, type: %d",
        remote_ssrc, local_ssrc, type);

    handle_rtcp_ssrc (self, buffer, local_ssrc);
  } else if (type == GST_RTCP_PSFB_TYPE_AFB) {
    guint8 *fci = gst_rtcp_packet_fb_get_fci (packet);

    // Size in bytes: Length in 32-bit words * 4 bytes per word.
    guint16 fci_size = gst_rtcp_packet_fb_get_fci_length (packet) * 4;

    // Make a new buffer that wraps just the FCI portion of the FB packet. Our
    // code for handling RTCP-PSFB-AFB was written for the signal handler of
    // `RTPSession::on-feedback-rtcp`, which sends a `GstBuffer *fci` that only
    // contains the FCI part, not the whole RTCP packet. So here we do the same,
    // with a non-owning buffer (it won't free or touch the wrapped data).
    GstBuffer *fci_buffer = gst_buffer_new_wrapped_full (
        GST_MEMORY_FLAG_READONLY, fci, fci_size, 0, fci_size, NULL, NULL);

    KmsRTCPPSFBAFBBuffer afb_buffer = {0};
    if (!kms_rtcp_psfb_afb_buffer_map (fci_buffer, GST_MAP_READ, &afb_buffer)) {
      GST_WARNING_OBJECT (self, "RTCP-PSFB-AFB buffer cannot be mapped");
      goto end_rtcp_psfb_afb_unref;
    }

    KmsRTCPPSFBAFBPacket afb_packet = {0};
    if (!kms_rtcp_psfb_afb_get_packet (&afb_buffer, &afb_packet)) {
      GST_WARNING_OBJECT (self, "Cannot get packet from RTCP-PSFB-AFB buffer");
      goto end_rtcp_psfb_afb_unmap;
    }

    KmsRTCPPSFBAFBType type = kms_rtcp_psfb_afb_packet_get_type (&afb_packet);

    if (type == KMS_RTCP_PSFB_AFB_TYPE_REMB) {
      KmsRTCPPSFBAFBREMBPacket remb_packet = {0};
      kms_rtcp_psfb_afb_remb_get_packet (&afb_packet, &remb_packet);

      for (guint i = 0; i < remb_packet.n_ssrcs; ++i) {
        guint32 local_ssrc = remb_packet.ssrcs[i];

        GST_TRACE_OBJECT (self,
            "Got RTCP-PSFB-AFB-REMB with remote SSRC: %u, local SSRC: %u, type: %d",
            remote_ssrc, local_ssrc, type);

        handle_rtcp_ssrc (self, buffer, local_ssrc);
      }
    }

  end_rtcp_psfb_afb_unmap:
    kms_rtcp_psfb_afb_buffer_unmap (&afb_buffer);
  end_rtcp_psfb_afb_unref:
    gst_buffer_unref (fci_buffer);
  } else if (type == GST_RTCP_FB_TYPE_INVALID) {
    // Don't handle invalid RTCP packets.
    return;
  } else {
    guint32 local_ssrc = gst_rtcp_packet_fb_get_media_ssrc (packet);

    GST_TRACE_OBJECT (self,
        "Got RTCP-FB with remote SSRC: %u, local SSRC: %u, type: %d",
        remote_ssrc, local_ssrc, type);

    handle_rtcp_ssrc (self, buffer, local_ssrc);
  }
}

static GstFlowReturn
kms_rtcp_demux_chain (GstPad *pad, GstObject *parent, GstBuffer *buffer)
{
  KmsRtcpDemux *self = KMS_RTCP_DEMUX (parent);

  // This is how GStreamer checks for RTCP packages in ssrcdemux.
  if (!gst_rtcp_buffer_validate_reduced (buffer)) {
    GST_TRACE_OBJECT (self, "Push RTP buffer");
    return gst_pad_push (self->priv->rtp_src, buffer);
  }

  GstRTCPBuffer rtcp = GST_RTCP_BUFFER_INIT;
  if (!gst_rtcp_buffer_map (buffer, GST_MAP_READ, &rtcp)) {
    // Discard invalid RTCP buffer.
    goto end_discard;
  }

  GstRTCPPacket packet;
  if (!gst_rtcp_buffer_get_first_packet (&rtcp, &packet)) {
    // Discard invalid RTCP buffer.
    goto end_discard;
  }

  gboolean do_push = FALSE;

  // Run over all the RTCP packets; there might be more than 1 in case of a
  // Compound RTCP packet, as defined in RFC 3550 (6.1 RTCP Packet Format).
  do {
    const GstRTCPType type = gst_rtcp_packet_get_type (&packet);

    // Dump the RTCP buffer hex representation.
    // DEBUG: Uncomment to enable.
    // gst_util_dump_mem (rtcp.map.data, rtcp.map.size);

    switch (type) {
    case GST_RTCP_TYPE_RR:
      handle_rtcp_rr (self, buffer, &packet);
      break;
    case GST_RTCP_TYPE_RTPFB:
    case GST_RTCP_TYPE_PSFB:
      handle_rtcp_fb (self, buffer, &packet);
      break;
    case GST_RTCP_TYPE_INVALID:
      // Here we are explicit about not pushing invalid RTCP packets.
      do_push = FALSE;
      break;
    default:
      // RTCP type not handled here; pass it through the static pads.
      GST_TRACE_OBJECT (self, "Push unhandled RTCP type: %d", type);
      do_push = TRUE;
      break;
    }
  } while (gst_rtcp_packet_move_to_next (&packet));

  if (do_push) {
    // We found an RTCP Type that is not handled by this element; push it to the
    // static RTCP src pad, letting downstream elements to handle it.
    goto end_push;
  } else {
    // The whole RTCP packet has been handled, and per-ssrc pads have been
    // created as needed. Discard the current buffer, to avoid having downstream
    // elements processing it too.
    goto end_discard;
  }

end_discard:
  gst_rtcp_buffer_unmap (&rtcp);
  gst_buffer_unref (buffer);
  return GST_FLOW_OK;

end_push:
  gst_rtcp_buffer_unmap (&rtcp);
  return gst_pad_push (self->priv->rtcp_src, buffer);
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
  rtcpdemux->priv->rtcp_src_ssrc = g_hash_table_new_full (g_direct_hash,
      g_direct_equal, NULL, gst_object_unref);

  gst_pad_set_chain_function (sink, GST_DEBUG_FUNCPTR (kms_rtcp_demux_chain));
}

static void
kms_rtcp_demux_finalize (GObject * object)
{
  KmsRtcpDemux *self = KMS_RTCP_DEMUX (object);

  g_hash_table_unref (self->priv->rr_ssrcs);
  g_hash_table_unref (self->priv->rtcp_src_ssrc);

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

  /**
   * KmsRtcpDemux::new-ssrc-pad:
   * @demux: the object which received the signal.
   * @ssrc: the SSRC of the new pad.
   * @pad: the new pad.
   *
   * Emitted when a new SSRC pad has been created.
   */
  kms_rtcp_demux_signals[SIGNAL_NEW_SSRC_PAD] =
      g_signal_new ("new-ssrc-pad",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsRtcpDemuxClass, new_ssrc_pad), NULL, NULL,
      NULL, G_TYPE_NONE, 2, G_TYPE_UINT, GST_TYPE_PAD);

  g_type_class_add_private (klass, sizeof (KmsRtcpDemuxPrivate));
}

gboolean
kms_rtcp_demux_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTCP_DEMUX);
}
