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
#ifndef __KMS_BASE_SDP_ENDPOINT_H__
#define __KMS_BASE_SDP_ENDPOINT_H__

#include <gst/gst.h>
#include <gst/sdp/gstsdpmessage.h>
#include "kmselement.h"
#include "kmssdpsession.h"

G_BEGIN_DECLS

typedef struct _KmsSdpSession KmsSdpSession;

/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_SDP_ENDPOINT \
  (kms_base_sdp_endpoint_get_type())
#define KMS_BASE_SDP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_SDP_ENDPOINT,KmsBaseSdpEndpoint))
#define KMS_BASE_SDP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_SDP_ENDPOINT,KmsBaseSdpEndpointClass))
#define KMS_IS_BASE_SDP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_SDP_ENDPOINT))
#define KMS_IS_BASE_SDP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_SDP_ENDPOINT))
#define KMS_BASE_SDP_ENDPOINT_CAST(obj) ((KmsBaseSdpEndpoint*)(obj))

typedef struct _KmsBaseSdpEndpointPrivate KmsBaseSdpEndpointPrivate;
typedef struct _KmsBaseSdpEndpoint KmsBaseSdpEndpoint;
typedef struct _KmsBaseSdpEndpointClass KmsBaseSdpEndpointClass;

#define KMS_BASE_SDP_ENDPOINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_SDP_ENDPOINT_CAST ((elem))->media_mutex))
#define KMS_BASE_SDP_ENDPOINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_SDP_ENDPOINT_CAST ((elem))->media_mutex))

struct _KmsBaseSdpEndpoint
{
  KmsElement parent;

  KmsBaseSdpEndpointPrivate *priv;
};

struct _KmsBaseSdpEndpointClass
{
  KmsElementClass parent_class;

  /* private */
  /* actions */
  const gchar* (*create_session) (KmsBaseSdpEndpoint * self);
  gboolean (*release_session) (KmsBaseSdpEndpoint * self, const gchar *sess_id);

  GstSDPMessage *(*generate_offer) (KmsBaseSdpEndpoint * self, const gchar *sess_id);
  GstSDPMessage *(*process_offer) (KmsBaseSdpEndpoint * self, const gchar *sess_id, GstSDPMessage * offer);
  gboolean (*process_answer) (KmsBaseSdpEndpoint * self, const gchar *sess_id, GstSDPMessage * answer);

  GstSDPMessage *(*get_local_sdp) (KmsBaseSdpEndpoint * self, const gchar *sess_id);
  GstSDPMessage *(*get_remote_sdp) (KmsBaseSdpEndpoint * self, const gchar *sess_id);

  /* virtual methods */
  void (*create_session_internal) (KmsBaseSdpEndpoint * self, gint id, KmsSdpSession **sess);
  void (*release_session_internal) (KmsBaseSdpEndpoint * self, KmsSdpSession *sess);
  void (*start_transport_send) (KmsBaseSdpEndpoint * self, KmsSdpSession *sess, gboolean offerer);
  void (*connect_input_elements) (KmsBaseSdpEndpoint * self, KmsSdpSession *sess);

  gboolean (*configure_media) (KmsBaseSdpEndpoint * self, KmsSdpSession *sess, KmsSdpMediaHandler * handler, GstSDPMedia *media);

  /* Virtual handler factory methods */
  void (*create_media_handler) (KmsBaseSdpEndpoint * self, const gchar *media, KmsSdpMediaHandler **handler);
};

GType kms_base_sdp_endpoint_get_type (void);

GHashTable * kms_base_sdp_endpoint_get_sessions (KmsBaseSdpEndpoint * self);
KmsSdpSession * kms_base_sdp_endpoint_get_session (KmsBaseSdpEndpoint * self, const gchar *sess_id);
const GstSDPMessage * kms_base_sdp_endpoint_get_first_negotiated_sdp (KmsBaseSdpEndpoint * self);

G_END_DECLS
#endif /* __KMS_BASE_SDP_ENDPOINT_H__ */
