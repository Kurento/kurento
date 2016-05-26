/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
    char* stream_id, guint component_id, gchar *pem_certificate);
void kms_webrtc_transport_destroy (KmsWebRtcTransport * tr);

void kms_webrtc_transport_enable_latency_notification (KmsWebRtcTransport * tr,
  BufferLatencyCallback cb, gpointer user_data, GDestroyNotify destroy_data);
void kms_webrtc_transport_disable_latency_notification (KmsWebRtcTransport * tr);

#endif /* __KMS_WEBRTC_TRANSPORT_H__ */
