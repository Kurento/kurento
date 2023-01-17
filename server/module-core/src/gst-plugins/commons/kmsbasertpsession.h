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

#ifndef __KMS_BASE_RTP_SESSION_H__
#define __KMS_BASE_RTP_SESSION_H__

#include <gst/gst.h>
#include "kmssdpsession.h"
#include "kmsirtpsessionmanager.h"
#include "kmsirtpconnection.h"
#include "kmsconnectionstate.h"

G_BEGIN_DECLS

typedef struct _KmsIRtpSessionManager KmsIRtpSessionManager;

/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_RTP_SESSION \
  (kms_base_rtp_session_get_type())
#define KMS_BASE_RTP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_RTP_SESSION,KmsBaseRtpSession))
#define KMS_BASE_RTP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_RTP_SESSION,KmsBaseRtpSessionClass))
#define KMS_IS_BASE_RTP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_RTP_SESSION))
#define KMS_IS_BASE_RTP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_RTP_SESSION))
#define KMS_BASE_RTP_SESSION_CAST(obj) ((KmsBaseRtpSession*)(obj))

typedef struct _KmsBaseRtpSession KmsBaseRtpSession;
typedef struct _KmsBaseRtpSessionClass KmsBaseRtpSessionClass;
typedef struct _KmsBaseRTPSessionStats KmsBaseRTPSessionStats;

struct _KmsBaseRtpSession
{
  KmsSdpSession parent;

  KmsIRtpSessionManager *manager;
  GHashTable *conns;
  KmsConnectionState conn_state;

  GstSDPMedia *audio_neg;
  guint32 local_audio_ssrc;
  guint32 remote_audio_ssrc;

  GstSDPMedia *video_neg;
  guint32 local_video_ssrc;
  guint32 remote_video_ssrc;

  gboolean stats_enabled;
};

struct _KmsBaseRtpSessionClass
{
  KmsSdpSessionClass parent_class;

  /* private */
  /* virtual methods */
  void (*post_constructor) (KmsBaseRtpSession * self, KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager);

  KmsIRtpConnection * (*create_connection) (KmsBaseRtpSession *self, const GstSDPMedia * media, const gchar *name, guint16 min_port, guint16 max_port);
  KmsIRtcpMuxConnection* (*create_rtcp_mux_connection) (KmsBaseRtpSession *self, const gchar *name, guint16 min_port, guint16 max_port);
  KmsIBundleConnection * (*create_bundle_connection) (KmsBaseRtpSession *self, const gchar *name, guint16 min_port, guint16 max_port);

  void (*connection_state_changed) (KmsBaseRtpSession * self, KmsConnectionState new_state);
};

GType kms_base_rtp_session_get_type (void);

KmsBaseRtpSession * kms_base_rtp_session_new (KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager);
KmsIRtpConnection * kms_base_rtp_session_get_connection_by_name (KmsBaseRtpSession * self, const gchar * name);
KmsIRtpConnection * kms_base_rtp_session_get_connection (KmsBaseRtpSession * self, KmsSdpMediaHandler *handler);
KmsIRtpConnection * kms_base_rtp_session_create_connection (KmsBaseRtpSession * self, KmsSdpMediaHandler *handler, GstSDPMedia * media, guint16 min_port, guint16 max_port);

void kms_base_rtp_session_start_transport_send (KmsBaseRtpSession * self, gboolean offerer);

void kms_base_rtp_session_enable_connections_stats (KmsBaseRtpSession * self);
void kms_base_rtp_session_disable_connections_stats (KmsBaseRtpSession * self);

G_END_DECLS
#endif /* __KMS_BASE_RTP_SESSION_H__ */
