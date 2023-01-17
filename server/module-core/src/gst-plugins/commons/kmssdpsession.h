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

#ifndef __KMS_SDP_SESSION_H__
#define __KMS_SDP_SESSION_H__

#include <gst/gst.h>
#include "sdpagent/kmssdpagent.h"
#include "sdpagent/kmssdppayloadmanager.h"
#include "kmsbasesdpendpoint.h"

G_BEGIN_DECLS

typedef struct _KmsBaseSdpEndpoint KmsBaseSdpEndpoint;

/* #defines don't like whitespacey bits */
#define KMS_TYPE_SDP_SESSION \
  (kms_sdp_session_get_type())
#define KMS_SDP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_SDP_SESSION,KmsSdpSession))
#define KMS_SDP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_SDP_SESSION,KmsSdpSessionClass))
#define KMS_IS_SDP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_SDP_SESSION))
#define KMS_IS_SDP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_SESSION))
#define KMS_SDP_SESSION_CAST(obj) ((KmsSdpSession*)(obj))

typedef struct _KmsSdpSession KmsSdpSession;
typedef struct _KmsSdpSessionClass KmsSdpSessionClass;

#define KMS_SDP_SESSION_LOCK(sess) \
  (g_rec_mutex_lock (&KMS_SDP_SESSION_CAST ((sess))->mutex))
#define KMS_SDP_SESSION_UNLOCK(sess) \
  (g_rec_mutex_unlock (&KMS_SDP_SESSION_CAST ((sess))->mutex))

struct _KmsSdpSession
{
  GstBin parent;

  GRecMutex mutex;

  guint id;
  gchar *id_str;
  KmsBaseSdpEndpoint * ep;

  /* SDP management */
  KmsSdpAgent *agent;
  KmsSdpPayloadManager *ptmanager;
  GstSDPMessage *local_sdp;
  GstSDPMessage *remote_sdp;
  GstSDPMessage *neg_sdp;
};

struct _KmsSdpSessionClass
{
  GstBinClass parent_class;

  /* private */
  /* virtual methods */
  void (*post_constructor) (KmsSdpSession * self, KmsBaseSdpEndpoint * ep, guint id);
};

GType kms_sdp_session_get_type (void);

KmsSdpSession * kms_sdp_session_new (KmsBaseSdpEndpoint * ep, guint id);
GstSDPMessage * kms_sdp_session_generate_offer (KmsSdpSession * self);
GstSDPMessage * kms_sdp_session_process_offer (KmsSdpSession * self, GstSDPMessage * offer);
gboolean kms_sdp_session_process_answer (KmsSdpSession * self, GstSDPMessage * answer);
GstSDPMessage * kms_sdp_session_get_local_sdp (KmsSdpSession * self);
GstSDPMessage * kms_sdp_session_get_remote_sdp (KmsSdpSession * self);
void kms_sdp_session_set_use_ipv6 (KmsSdpSession * self, gboolean use_ipv6);
gboolean kms_sdp_session_get_use_ipv6 (KmsSdpSession * self);
void kms_sdp_session_set_addr (KmsSdpSession *self, const gchar * addr);

G_END_DECLS
#endif /* __KMS_SDP_SESSION_H__ */
