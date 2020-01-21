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

#ifndef __KMS_WEBRTC_BASE_CONNECTION_H__
#define __KMS_WEBRTC_BASE_CONNECTION_H__

#include <gst/gst.h>
#include <commons/kmsirtpconnection.h>
#include "kmsicebaseagent.h"

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_BASE_CONNECTION \
  (kms_webrtc_base_connection_get_type())
#define KMS_WEBRTC_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_BASE_CONNECTION,KmsWebRtcBaseConnection))
#define KMS_WEBRTC_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_BASE_CONNECTION,KmsWebRtcBaseConnectionClass))
#define KMS_IS_WEBRTC_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_BASE_CONNECTION))
#define KMS_IS_WEBRTC_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_BASE_CONNECTION))
#define KMS_WEBRTC_BASE_CONNECTION_CAST(obj) ((KmsWebRtcBaseConnection*)(obj))

#define KMS_WEBRTC_BASE_CONNECTION_LOCK(conn) \
  (g_rec_mutex_lock (&KMS_WEBRTC_BASE_CONNECTION_CAST ((conn))->mutex))
#define KMS_WEBRTC_BASE_CONNECTION_UNLOCK(conn) \
  (g_rec_mutex_unlock (&KMS_WEBRTC_BASE_CONNECTION_CAST ((conn))->mutex))

typedef struct _KmsWebRtcBaseConnection KmsWebRtcBaseConnection;
typedef struct _KmsWebRtcBaseConnectionClass KmsWebRtcBaseConnectionClass;

struct _KmsWebRtcBaseConnection
{
  GObject parent;

  GRecMutex mutex;

  KmsIceBaseAgent *agent;
  gboolean ice_gathering_started;
  gboolean ice_gathering_done;
  gchar* stream_id;
  gchar *name;

  BufferLatencyCallback cb;
  gpointer user_data;

  guint min_port;
  guint max_port;

  gboolean stats_enabled;
};

struct _KmsWebRtcBaseConnectionClass
{
  GObjectClass parent_class;

  gchar *(*get_certificate_pem) (KmsWebRtcBaseConnection * self);

  void (*set_latency_callback) (KmsIRtpConnection *self, BufferLatencyCallback cb, gpointer user_data);
  void (*collect_latency_stats) (KmsIRtpConnection *self, gboolean enable);
};

GType kms_webrtc_base_connection_get_type (void);

gchar *kms_webrtc_base_connection_get_certificate_pem (KmsWebRtcBaseConnection *
    self);
void kms_webrtc_base_connection_set_network_ifs_info (KmsWebRtcBaseConnection *
    self, const gchar * net_names);
void kms_webrtc_base_connection_set_stun_server_info (KmsWebRtcBaseConnection * self,
    const gchar * stun_server_ip, guint stun_server_port);
void kms_webrtc_base_connection_set_relay_info (KmsWebRtcBaseConnection * self,
    const gchar * server_ip, guint server_port, const gchar * username,
    const gchar * password, TurnProtocol type);

gboolean kms_webrtc_base_connection_configure (KmsWebRtcBaseConnection * self,
    KmsIceBaseAgent *agent, const gchar * name);

void kms_webrtc_base_connection_set_latency_callback (KmsIRtpConnection *self, BufferLatencyCallback cb, gpointer user_data);
void kms_webrtc_base_connection_collect_latency_stats (KmsIRtpConnection *self, gboolean enable);

G_END_DECLS
#endif /* __KMS_WEBRTC_BASE_CONNECTION_H__ */
