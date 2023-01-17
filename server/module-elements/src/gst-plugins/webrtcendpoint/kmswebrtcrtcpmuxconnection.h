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

#ifndef __KMS_WEBRTC_MUX_CONNECTION_H__
#define __KMS_WEBRTC_MUX_CONNECTION_H__

#include "kmswebrtcbaseconnection.h"

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION \
  (kms_webrtc_rtcp_mux_connection_get_type())
#define KMS_WEBRTC_RTCP_MUX_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION,KmsWebRtcRtcpMuxConnection))
#define KMS_WEBRTC_RTCP_MUX_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION,KmsWebRtcRtcpMuxConnectionClass))
#define KMS_IS_WEBRTC_RTCP_MUX_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION))
#define KMS_IS_WEBRTC_RTCP_MUX_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION))
#define KMS_WEBRTC_RTCP_MUX_CONNECTION_CAST(obj) ((KmsWebRtcRtcpMuxConnection*)(obj))

typedef struct _KmsWebRtcRtcpMuxConnectionPrivate
    KmsWebRtcRtcpMuxConnectionPrivate;
typedef struct _KmsWebRtcRtcpMuxConnection KmsWebRtcRtcpMuxConnection;
typedef struct _KmsWebRtcRtcpMuxConnectionClass KmsWebRtcRtcpMuxConnectionClass;

struct _KmsWebRtcRtcpMuxConnection
{
  KmsWebRtcBaseConnection parent;

  KmsWebRtcRtcpMuxConnectionPrivate *priv;
};

struct _KmsWebRtcRtcpMuxConnectionClass
{
  KmsWebRtcBaseConnectionClass parent_class;
};

GType kms_webrtc_rtcp_mux_connection_get_type (void);

KmsWebRtcRtcpMuxConnection *kms_webrtc_rtcp_mux_connection_new (KmsIceBaseAgent *agent, GMainContext * context, const gchar * name, guint16 min_port, guint16 max_port, gchar* pem_certificate);

G_END_DECLS
#endif /* __KMS_WEBRTC_MUX_CONNECTION_H__ */
