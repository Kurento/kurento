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

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_TRANSPORT \
  (kms_webrtc_transport_get_type())

#define KMS_WEBRTC_TRANSPORT(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (      \
    (obj),                          \
    KMS_TYPE_WEBRTC_TRANSPORT,      \
    KmsWebRtcTransport              \
  )                                 \
)
#define KMS_WEBRTC_TRANSPORT_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                 \
    (klass),                                \
    KMS_TYPE_WEBRTC_TRANSPORT,              \
    KmsWebRtcTransportClass                 \
  )                                         \
)
#define KMS_IS_WEBRTC_TRANSPORT(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (         \
    (obj),                             \
    KMS_TYPE_WEBRTC_TRANSPORT          \
  )                                    \
)
#define KMS_IS_WEBRTC_TRANSPORT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_TRANSPORT))
#define KMS_WEBRTC_TRANSPORT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                  \
    (obj),                                     \
    KMS_TYPE_WEBRTC_TRANSPORT,                 \
    KmsWebRtcTransportClass                    \
  )                                            \
)

typedef struct _KmsWebRtcTransport KmsWebRtcTransport;
typedef struct _KmsWebRtcTransportClass KmsWebRtcTransportClass;

typedef struct _KmsWebRtcTransport
{
  GObject parent;

  guint component_id;

  KmsWebrtcTransportSrc *src;
  KmsWebrtcTransportSink *sink;

  guint rtp_id; /* atomic */
  guint rtcp_id; /* atomic */

  gulong src_probe;
  gulong sink_probe;
} KmsWebRtcTransport;

struct _KmsWebRtcTransportClass
{
  GObjectClass parent_class;
};

GType kms_webrtc_transport_get_type ();

KmsWebRtcTransport *kms_webrtc_transport_new (KmsIceBaseAgent * agent,
    char* stream_id, guint component_id, gchar *pem_certificate);

void kms_webrtc_transport_enable_latency_notification (KmsWebRtcTransport * tr,
  BufferLatencyCallback cb, gpointer user_data, GDestroyNotify destroy_data);
void kms_webrtc_transport_disable_latency_notification (KmsWebRtcTransport * tr);

G_END_DECLS

#endif /* __KMS_WEBRTC_TRANSPORT_H__ */
