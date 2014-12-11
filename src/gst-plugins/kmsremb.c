/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include "kmsremb.h"
#include "kmsrtcp.h"

#define GST_CAT_DEFAULT kmsutils
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmsremb"

#define REMB_MIN 30000          /* bps */
#define REMB_MAX 2000000        /* bps */

/* KmsRembLocal begin */

#define KMS_REMB_LOCAL "kms-remb-local"

#define REMB_EXPONENTIAL_FACTOR 0.04
#define REMB_LINEAL_FACTOR_MIN 50       /* bps */
#define REMB_LINEAL_FACTOR_GRADE ((60 * RTCP_MIN_INTERVAL)/ 1000)       /* Reach last top bitrate in 60secs aprox. */
#define REMB_DECREMENT_FACTOR 0.5
#define REMB_THRESHOLD_FACTOR 0.8
#define REMB_UP_LOSSES 12       /* 4% losses */

static gboolean
get_video_recv_info (KmsRembLocal * rl,
    guint64 * bitrate, guint * fraction_lost)
{
  GValueArray *arr;
  GValue *val;
  guint i;
  gboolean ret = FALSE;

  g_object_get (rl->rtpsess, "sources", &arr, NULL);

  for (i = 0; i < arr->n_values; i++) {
    GObject *source;
    guint ssrc;
    GstStructure *s;

    val = g_value_array_get_nth (arr, i);
    source = g_value_get_object (val);
    g_object_get (source, "ssrc", &ssrc, NULL);
    GST_TRACE_OBJECT (source, "source ssrc: %u", ssrc);

    g_object_get (source, "stats", &s, NULL);
    GST_TRACE_OBJECT (rl->rtpsess, "stats: %" GST_PTR_FORMAT, s);

    if (ssrc == rl->remote_ssrc) {
      GstClockTime current_time;
      guint64 octets_received;

      if (!gst_structure_get_uint64 (s, "bitrate", bitrate)) {
        break;
      }
      if (!gst_structure_get_uint64 (s, "octets-received", &octets_received)) {
        break;
      }
      if (!gst_structure_get_uint (s, "sent-rb-fractionlost", fraction_lost)) {
        break;
      }

      current_time = kms_utils_get_time_nsecs ();

      if (rl->last_time != 0) {
        GstClockTime elapsed = current_time - rl->last_time;
        guint64 bytes_handled = octets_received - rl->last_octets_received;

        *bitrate =
            gst_util_uint64_scale (bytes_handled, 8 * GST_SECOND, elapsed);
        GST_TRACE_OBJECT (rl->rtpsess,
            "Elapsed %" G_GUINT64_FORMAT " bytes %" G_GUINT64_FORMAT ", rate %"
            G_GUINT64_FORMAT, elapsed, bytes_handled, *bitrate);
      }

      rl->last_time = current_time;
      rl->last_octets_received = octets_received;

      ret = TRUE;
      break;
    }
  }

  g_value_array_free (arr);

  return ret;
}

static gboolean
kms_remb_local_update (KmsRembLocal * rl)
{
  guint64 bitrate;
  guint fraction_lost;

  if (!get_video_recv_info (rl, &bitrate, &fraction_lost)) {
    return FALSE;
  }

  if (!rl->probed) {
    if (bitrate == 0) {
      return FALSE;
    }

    rl->remb = bitrate;
    rl->probed = TRUE;
  }

  rl->max_br = MAX (rl->max_br, bitrate);

  if (rl->avg_br == 0) {
    rl->avg_br = bitrate;
  } else {
    rl->avg_br = (rl->avg_br * 7 + bitrate) / 8;
  }

  if (fraction_lost == 0) {
    gint remb_base, remb_new;

    remb_base = MIN (rl->remb, rl->max_br);

    if (remb_base < rl->threshold) {
      GST_TRACE_OBJECT (rl->rtpsess, "A.1) Exponential (%f)",
          REMB_EXPONENTIAL_FACTOR);
      remb_new = remb_base * (1 + REMB_EXPONENTIAL_FACTOR);
    } else {
      GST_TRACE_OBJECT (rl->rtpsess, "A.2) Lineal (%" G_GUINT32_FORMAT ")",
          rl->lineal_factor);
      remb_new = remb_base + rl->lineal_factor;
    }

    rl->remb = MAX (rl->remb, remb_new);
  } else if (fraction_lost < REMB_UP_LOSSES) {
    GST_TRACE_OBJECT (rl->rtpsess, "B) Assumable losses");

    rl->remb = MIN (rl->remb, rl->max_br);
    rl->threshold = rl->remb * REMB_THRESHOLD_FACTOR;
  } else {
    gint remb_base, lineal_factor_new;

    GST_TRACE_OBJECT (rl->rtpsess, "C) Too losses");

    remb_base = MAX (rl->remb, rl->avg_br);
    rl->remb = remb_base * REMB_DECREMENT_FACTOR;
    rl->threshold = remb_base * REMB_THRESHOLD_FACTOR;
    lineal_factor_new = (remb_base - rl->threshold) / REMB_LINEAL_FACTOR_GRADE;
    rl->lineal_factor = MAX (REMB_LINEAL_FACTOR_MIN, lineal_factor_new);
    rl->max_br = 0;
    rl->avg_br = 0;
  }

  if (rl->max_bw > 0) {
    rl->remb = MIN (rl->remb, rl->max_bw * 1000);
  }

  GST_TRACE_OBJECT (rl->rtpsess,
      "REMB: %" G_GUINT32_FORMAT ", TH: %" G_GUINT32_FORMAT
      ", fraction_lost: %d, bitrate: %" G_GUINT64_FORMAT "," " max_br: %"
      G_GUINT32_FORMAT ", avg_br: %" G_GUINT32_FORMAT, rl->remb,
      rl->threshold, fraction_lost, bitrate, rl->max_br, rl->avg_br);

  return TRUE;
}

static void
on_sending_rtcp (GObject * sess, GstBuffer * buffer, gboolean is_early,
    gboolean * do_not_supress)
{
  KmsRembLocal *rl = g_object_get_data (sess, KMS_REMB_LOCAL);
  KmsRTCPPSFBAFBREMBPacket remb_packet;
  GstRTCPBuffer rtcp = { NULL, };
  GstRTCPPacket packet;
  guint packet_ssrc;

  if (is_early) {
    return;
  }

  if (!gst_rtcp_buffer_map (buffer, GST_MAP_READWRITE, &rtcp)) {
    GST_WARNING_OBJECT (sess, "Cannot map buffer to RTCP");
    return;
  }

  if (!gst_rtcp_buffer_add_packet (&rtcp, GST_RTCP_TYPE_PSFB, &packet)) {
    GST_WARNING_OBJECT (sess, "Cannot add RTCP packet");
    return;
  }

  if (!kms_remb_local_update (rl)) {
    return;
  }

  remb_packet.bitrate = rl->remb;
  if (rl->event_manager != NULL) {
    guint remb_local_max;

    remb_local_max = kms_utils_remb_event_manager_get_min (rl->event_manager);
    if (remb_local_max > 0) {
      GST_TRACE_OBJECT (sess, "REMB local max: %" G_GUINT32_FORMAT,
          remb_local_max);
      remb_packet.bitrate = MIN (remb_local_max, rl->remb);
    }
  }

  remb_packet.bitrate = MAX (remb_packet.bitrate, REMB_MIN);
  remb_packet.n_ssrcs = 1;
  remb_packet.ssrcs[0] = rl->remote_ssrc;
  g_object_get (sess, "internal-ssrc", &packet_ssrc, NULL);
  if (!kms_rtcp_psfb_afb_remb_marshall_packet (&packet, &remb_packet,
          packet_ssrc)) {
    gst_rtcp_packet_remove (&packet);
  }
  gst_rtcp_buffer_unmap (&rtcp);

  GST_TRACE_OBJECT (sess, "Sending REMB with bitrate: %d", remb_packet.bitrate);
}

void
kms_remb_local_destroy (KmsRembLocal * rl)
{
  if (rl == NULL) {
    return;
  }

  if (rl->event_manager != NULL) {
    kms_utils_remb_event_manager_destroy (rl->event_manager);
  }

  g_object_unref (rl->rtpsess);
  g_slice_free (KmsRembLocal, rl);
}

KmsRembLocal *
kms_remb_local_create (GObject * rtpsess, guint remote_ssrc, guint max_bw)
{
  KmsRembLocal *rl = g_slice_new0 (KmsRembLocal);

  g_object_set_data (rtpsess, KMS_REMB_LOCAL, rl);
  g_signal_connect (rtpsess, "on-sending-rtcp",
      G_CALLBACK (on_sending_rtcp), NULL);
  rl->rtpsess = g_object_ref (rtpsess);
  rl->remote_ssrc = remote_ssrc;
  rl->max_bw = max_bw;

  rl->probed = FALSE;
  rl->remb = REMB_MAX;
  rl->threshold = REMB_MAX;
  rl->lineal_factor = REMB_LINEAL_FACTOR_MIN;

  return rl;
}

/* KmsRembLocal end */

static void init_debug (void) __attribute__ ((constructor));

static void
init_debug (void)
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
