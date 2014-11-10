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

#include "kmsrtcp.h"
#include <string.h>
#include <arpa/inet.h>

gboolean
kms_rtcp_psfb_afb_buffer_map (GstBuffer * buffer, GstMapFlags flags,
    KmsRTCPPSFBAFBBuffer * rtcp_psfb_afb)
{
  g_return_val_if_fail (rtcp_psfb_afb != NULL, FALSE);
  g_return_val_if_fail (rtcp_psfb_afb->buffer == NULL, FALSE);
  g_return_val_if_fail (GST_IS_BUFFER (buffer), FALSE);
  g_return_val_if_fail (flags & GST_MAP_READ, FALSE);

  rtcp_psfb_afb->buffer = buffer;
  gst_buffer_map (buffer, &rtcp_psfb_afb->map, flags);

  return TRUE;
}

gboolean
kms_rtcp_psfb_afb_buffer_unmap (KmsRTCPPSFBAFBBuffer * rtcp_psfb_afb)
{
  g_return_val_if_fail (rtcp_psfb_afb != NULL, FALSE);
  g_return_val_if_fail (GST_IS_BUFFER (rtcp_psfb_afb->buffer), FALSE);

  if (rtcp_psfb_afb->map.flags & GST_MAP_WRITE) {
    /* shrink size */
    gst_buffer_resize (rtcp_psfb_afb->buffer, 0, rtcp_psfb_afb->map.size);
  }

  gst_buffer_unmap (rtcp_psfb_afb->buffer, &rtcp_psfb_afb->map);
  rtcp_psfb_afb->buffer = NULL;

  return TRUE;
}

/* REMB begin */

// Receiver Estimated Max Bitrate (REMB) (draft-alvestrand-rmcat-remb).
//
//    0                   1                   2                   3
//    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |V=2|P| FMT=15  |   PT=206      |             length            |
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |                  SSRC of packet sender                        |
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |                  SSRC of media source                         |
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |  Unique identifier 'R' 'E' 'M' 'B'                            |
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |  Num SSRC     | BR Exp    |  BR Mantissa                      |
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |   SSRC feedback                                               |
//   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   |  ...                                                          |

/* Inspired in The WebRTC project */
static gboolean
is_remb (KmsRTCPPSFBAFBPacket * packet)
{
  GstMapInfo map = packet->rtcp_psfb_afb->map;
  guint8 *fci = map.data;

  if (map.size < 4) {
    return FALSE;
  }

  return (memcmp (fci, "REMB", 4) == 0);
}

static gboolean
read_packet_type (KmsRTCPPSFBAFBPacket * packet)
{
  if (is_remb (packet)) {
    packet->type = KMS_RTCP_PSFB_AFB_TYPE_REMB;
    return TRUE;
  }

  return FALSE;
}

gboolean
kms_rtcp_psfb_afb_get_packet (KmsRTCPPSFBAFBBuffer * rtcp_psfb_afb,
    KmsRTCPPSFBAFBPacket * packet)
{
  g_return_val_if_fail (rtcp_psfb_afb != NULL, FALSE);
  g_return_val_if_fail (GST_IS_BUFFER (rtcp_psfb_afb->buffer), FALSE);
  g_return_val_if_fail (rtcp_psfb_afb->map.flags & GST_MAP_READ, FALSE);
  g_return_val_if_fail (packet != NULL, FALSE);

  packet->rtcp_psfb_afb = rtcp_psfb_afb;
  packet->type = KMS_RTCP_PSFB_AFB_TYPE_INVALID;

  return read_packet_type (packet);
}

KmsRTCPPSFBAFBType
kms_rtcp_psfb_afb_packet_get_type (KmsRTCPPSFBAFBPacket * packet)
{
  return packet->type;
}

/* Inspired in The WebRTC project */
gboolean
kms_rtcp_psfb_afb_remb_get_packet (KmsRTCPPSFBAFBPacket * afb_packet,
    KmsRTCPPSFBAFBREMBPacket * remb_packet)
{
  GstMapInfo map;
  guint length;
  guint8 *fci, *fci_end, br_exp;
  guint32 br_mantissa;
  int i;

  g_return_val_if_fail (afb_packet != NULL, FALSE);
  g_return_val_if_fail (afb_packet->type == KMS_RTCP_PSFB_AFB_TYPE_REMB, FALSE);
  g_return_val_if_fail (GST_IS_BUFFER (afb_packet->rtcp_psfb_afb->buffer),
      FALSE);
  g_return_val_if_fail (afb_packet->rtcp_psfb_afb->map.flags & GST_MAP_READ,
      FALSE);
  g_return_val_if_fail (remb_packet != NULL, FALSE);

  map = afb_packet->rtcp_psfb_afb->map;
  fci = map.data;
  length = map.size;
  fci_end = map.data + length;

  if (!is_remb (afb_packet)) {
    GST_ERROR ("This is not a REMB packet");
    return FALSE;
  }
  fci += 4;                     /* Previously consumed by is_remb */

  length = fci_end - fci;
  if (length < 4) {
    GST_ERROR ("Inconsistent REMB packet length)");
    return FALSE;
  }

  remb_packet->n_ssrcs = *fci++;

  br_exp = (fci[0] >> 2) & 0x3F;
  br_mantissa = (fci[0] & 0x03) << 16;
  br_mantissa += (fci[1] << 8);
  br_mantissa += (fci[2]);
  remb_packet->bitrate = br_mantissa << br_exp;
  fci += 3;

  length = fci_end - fci;
  if (length < 4 * remb_packet->n_ssrcs) {
    GST_ERROR ("Inconsistent REMB packet (n_ssrcs)");
    return FALSE;
  }

  for (i = 0; i < remb_packet->n_ssrcs; i++) {
    guint32 ssrc;

    ssrc = *fci++ << 24;
    ssrc += *fci++ << 16;
    ssrc += *fci++ << 8;
    ssrc += *fci++;

    remb_packet->ssrcs[i] = ssrc;
  }

  return TRUE;
}

/* Inspired in The WebRTC project */
static gboolean
compute_mantissa_and_6_bit_base_2_expoonent (guint32 input_base10,
    guint8 bits_mantissa, guint32 * mantissa, guint8 * exp)
{
  guint32 mantissa_max;
  guint8 exponent = 0;
  gint i;

  /* input_base10 = mantissa * 2^exp */
  if (bits_mantissa > 32) {
    GST_ERROR ("bits_mantissa must be <= 32");
    return FALSE;
  }

  mantissa_max = (1 << bits_mantissa) - 1;

  for (i = 0; i < 64; ++i) {
    if (input_base10 <= (mantissa_max << i)) {
      exponent = i;
      break;
    }
  }

  *exp = exponent;
  *mantissa = input_base10 >> exponent;

  return TRUE;
}

gboolean
kms_rtcp_psfb_afb_remb_marshall_packet (GstRTCPPacket * rtcp_packet,
    KmsRTCPPSFBAFBREMBPacket * remb_packet, guint32 sender_ssrc)
{
  guint8 *fci_data;
  guint16 len;
  guint32 mantissa = 0;
  guint8 exp = 0;
  int i;

  if (!compute_mantissa_and_6_bit_base_2_expoonent (remb_packet->bitrate, 18,
          &mantissa, &exp)) {
    GST_ERROR ("Cannot calculate mantissa and exp)");
    return FALSE;
  }

  gst_rtcp_packet_fb_set_type (rtcp_packet, GST_RTCP_PSFB_TYPE_AFB);
  gst_rtcp_packet_fb_set_sender_ssrc (rtcp_packet, sender_ssrc);
  gst_rtcp_packet_fb_set_media_ssrc (rtcp_packet, 0);

  len = gst_rtcp_packet_fb_get_fci_length (rtcp_packet);
  len += 2 + remb_packet->n_ssrcs;
  if (!gst_rtcp_packet_fb_set_fci_length (rtcp_packet, len)) {
    GST_ERROR ("Cannot increase FCI length (%d)", len);
    return FALSE;
  }

  fci_data = gst_rtcp_packet_fb_get_fci (rtcp_packet);
  memmove (fci_data, "REMB", 4);
  fci_data[4] = remb_packet->n_ssrcs;

  fci_data[5] = (exp << 2) + ((mantissa >> 16) & 0x03);
  fci_data[6] = mantissa >> 8;
  fci_data[7] = mantissa;
  fci_data += 8;

  for (i = 0; i < remb_packet->n_ssrcs; i++) {
    *fci_data = remb_packet->ssrcs[i];
    fci_data += 4;
  }

  return TRUE;
}

/* REMB end */
