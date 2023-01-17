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

#ifndef __KMS_RTCP_H__
#define __KMS_RTCP_H__

#include <gst/gst.h>
#include <gst/rtp/gstrtcpbuffer.h>

G_BEGIN_DECLS
/**
 * KmsRTCPPSFBAFBType:
 * @KMS_RTCP_PSFB_AFB_TYPE_INVALID: Invalid type
 * @KMS_RTCP_PSFB_AFB_TYPE_REMB: Receiver Estimated Maximum Bitrate
 *
 * Different types of Payload Specific Application Layer Feedback messages.
 */
    typedef enum
{
  /* generic */
  KMS_RTCP_PSFB_AFB_TYPE_INVALID = 0,
  /* http://tools.ietf.org/html/draft-alvestrand-rmcat-remb */
  KMS_RTCP_PSFB_AFB_TYPE_REMB = 1,
} KmsRTCPPSFBAFBType;

typedef struct _KmsRTCPPSFBAFBBuffer KmsRTCPPSFBAFBBuffer;
typedef struct _KmsRTCPPSFBAFBPacket KmsRTCPPSFBAFBPacket;
typedef struct _KmsRTCPPSFBAFBREMBPacket KmsRTCPPSFBAFBREMBPacket;

struct _KmsRTCPPSFBAFBBuffer
{
  GstBuffer *buffer;
  GstMapInfo map;
};
struct _KmsRTCPPSFBAFBPacket
{
  KmsRTCPPSFBAFBBuffer *rtcp_psfb_afb;

  /*< private > */
  KmsRTCPPSFBAFBType type;
};

#define KMS_RTCP_PSFB_AFB_REMB_MAX_SSRCS_COUNT   255

struct _KmsRTCPPSFBAFBREMBPacket
{
  guint32 bitrate;
  guint8 n_ssrcs;
  guint32 ssrcs[KMS_RTCP_PSFB_AFB_REMB_MAX_SSRCS_COUNT];
};

/* KmsRTCPPSFBAFBBuffer */
gboolean kms_rtcp_psfb_afb_buffer_map (GstBuffer * buffer, GstMapFlags flags,
    KmsRTCPPSFBAFBBuffer * rtcp_psfb_afb);
gboolean kms_rtcp_psfb_afb_buffer_unmap (KmsRTCPPSFBAFBBuffer * rtcp_psfb_afb);

/* KmsRTCPPSFBAFBPacket */
gboolean kms_rtcp_psfb_afb_get_packet (KmsRTCPPSFBAFBBuffer * rtcp_psfb_afb,
    KmsRTCPPSFBAFBPacket * packet);
KmsRTCPPSFBAFBType kms_rtcp_psfb_afb_packet_get_type (KmsRTCPPSFBAFBPacket *
    packet);

/* KmsRTCPPSFBAFBREMBPacket */
gboolean kms_rtcp_psfb_afb_remb_get_packet (KmsRTCPPSFBAFBPacket * afb_packet,
    KmsRTCPPSFBAFBREMBPacket * remb_packet);

gboolean kms_rtcp_psfb_afb_remb_marshall_packet (GstRTCPPacket *rtcp_packet, KmsRTCPPSFBAFBREMBPacket * remb_packet, guint32 sender_ssrc);

G_END_DECLS
#endif /* __KMS_RTCP_H__ */
