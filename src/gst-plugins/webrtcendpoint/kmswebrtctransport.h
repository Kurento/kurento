/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#ifndef __KMS_WEBRTC_TRANSPORT_H__
#define __KMS_WEBRTC_TRANSPORT_H__

#include <gst/gst.h>
#include "kmsiceniceagent.h"
#include "kmswebrtctransportsrcnice.h"
#include "kmswebrtctransportsinknice.h"

typedef struct _KmsWebRtcTransport
{
  guint component_id;

  KmsWebrtcTransportSrc *src;
  KmsWebrtcTransportSink *sink;

  guint rtp_id; /* atomic */
  guint rtcp_id; /* atomic */

  gulong src_probe;
  gulong sink_probe;
} KmsWebRtcTransport;

KmsWebRtcTransport *kms_webrtc_transport_create (KmsIceBaseAgent * agent,
    char* stream_id, guint component_id);
void kms_webrtc_transport_destroy (KmsWebRtcTransport * tr);

void kms_webrtc_transport_enable_latency_notification (KmsWebRtcTransport * tr,
  BufferLatencyCallback cb, gpointer user_data, GDestroyNotify destroy_data);
void kms_webrtc_transport_disable_latency_notification (KmsWebRtcTransport * tr);

#endif /* __KMS_WEBRTC_TRANSPORT_H__ */
