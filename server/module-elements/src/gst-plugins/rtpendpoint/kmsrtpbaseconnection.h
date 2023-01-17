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

#ifndef __KMS_RTP_BASE_CONNECTION_H__
#define __KMS_RTP_BASE_CONNECTION_H__

#include <commons/kmsirtpconnection.h>

G_BEGIN_DECLS

#define KMS_TYPE_RTP_BASE_CONNECTION \
  (kms_rtp_base_connection_get_type())
#define KMS_RTP_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTP_BASE_CONNECTION,KmsRtpBaseConnection))
#define KMS_RTP_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTP_BASE_CONNECTION,KmsRtpBaseConnectionClass))
#define KMS_IS_RTP_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTP_BASE_CONNECTION))
#define KMS_IS_RTP_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTP_BASE_CONNECTION))
#define KMS_RTP_BASE_CONNECTION_CAST(obj) ((KmsRtpBaseConnection*)(obj))
typedef struct _KmsRtpBaseConnection KmsRtpBaseConnection;
typedef struct _KmsRtpBaseConnectionClass KmsRtpBaseConnectionClass;

#define KMS_RTP_BASE_CONNECTION_LOCK(conn) \
  (g_rec_mutex_lock (&KMS_RTP_BASE_CONNECTION_CAST ((conn))->mutex))
#define KMS_RTP_BASE_CONNECTION_UNLOCK(conn) \
  (g_rec_mutex_unlock (&KMS_RTP_BASE_CONNECTION_CAST ((conn))->mutex))

struct _KmsRtpBaseConnection
{
  GObject parent;

  GRecMutex mutex;

  guint min_port;
  guint max_port;

  BufferLatencyCallback cb;
  gpointer user_data;

  gboolean stats_enabled;

  gulong src_probe;
  gulong sink_probe;
};

struct _KmsRtpBaseConnectionClass
{
  GObjectClass parent_class;

    guint (*get_rtp_port) (KmsRtpBaseConnection * self);
    guint (*get_rtcp_port) (KmsRtpBaseConnection * self);
  void (*set_remote_info) (KmsRtpBaseConnection * self,
      const gchar * host, gint rtp_port, gint rtcp_port);
  void (*set_latency_callback) (KmsIRtpConnection *self, BufferLatencyCallback cb, gpointer user_data);
  void (*collect_latency_stats) (KmsIRtpConnection *self, gboolean enable);
};

GType kms_rtp_base_connection_get_type (void);

guint kms_rtp_base_connection_get_rtp_port (KmsRtpBaseConnection * self);
guint kms_rtp_base_connection_get_rtcp_port (KmsRtpBaseConnection * self);
void kms_rtp_base_connection_set_remote_info (KmsRtpBaseConnection * self,
    const gchar * host, gint rtp_port, gint rtcp_port);

void kms_rtp_base_connection_set_latency_callback (KmsIRtpConnection *self, BufferLatencyCallback cb, gpointer user_data);
void kms_rtp_base_connection_collect_latency_stats (KmsIRtpConnection *self, gboolean enable);
void kms_rtp_base_connection_remove_probe (KmsRtpBaseConnection * self, GstElement * e, const gchar * pad_name, gulong id);
G_END_DECLS
#endif /* __KMS_RTP_BASE_CONNECTION_H__ */
