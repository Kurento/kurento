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
#ifndef __KMS_BASE_RTP_ENDPOINT_H__
#define __KMS_BASE_RTP_ENDPOINT_H__

#include "kmsbasesdpendpoint.h"
#include "kmsirtpconnection.h"
#include "kmsmediatype.h"
#include "kmsmediastate.h"
#include "kmsconnectionstate.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_RTP_ENDPOINT \
  (kms_base_rtp_endpoint_get_type())
#define KMS_BASE_RTP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_RTP_ENDPOINT,KmsBaseRtpEndpoint))
#define KMS_BASE_RTP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_RTP_ENDPOINT,KmsBaseRtpEndpointClass))
#define KMS_IS_BASE_RTP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_RTP_ENDPOINT))
#define KMS_IS_BASE_RTP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_RTP_ENDPOINT))
#define KMS_BASE_RTP_ENDPOINT_CAST(obj) ((KmsBaseRtpEndpoint*)(obj))

typedef struct _KmsBaseRtpEndpoint KmsBaseRtpEndpoint;
typedef struct _KmsBaseRtpEndpointClass KmsBaseRtpEndpointClass;
typedef struct _KmsBaseRtpEndpointPrivate KmsBaseRtpEndpointPrivate;

#define KMS_BASE_RTP_ENDPOINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_RTP_ENDPOINT_CAST ((elem))->media_mutex))
#define KMS_BASE_RTP_ENDPOINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_RTP_ENDPOINT_CAST ((elem))->media_mutex))


struct _KmsBaseRtpEndpoint
{
  KmsBaseSdpEndpoint parent;

  KmsBaseRtpEndpointPrivate *priv;
};

struct _KmsBaseRtpEndpointClass
{
  KmsBaseSdpEndpointClass parent_class;

  void (*media_start) (KmsBaseRtpEndpoint * self, KmsMediaType type,
    gboolean local);
  void (*media_stop) (KmsBaseRtpEndpoint * self, KmsMediaType type,
    gboolean local);
  void (*media_state_changed) (KmsBaseRtpEndpoint * self, KmsMediaState new_state);

  KmsConnectionState (*get_connection_state) (KmsBaseRtpEndpoint * self, const gchar * sess_id);
  void (*connection_state_changed) (KmsBaseRtpEndpoint * self, KmsConnectionState new_state);

  gboolean (*request_local_key_frame) (KmsBaseRtpEndpoint * self);
};

GType kms_base_rtp_endpoint_get_type (void);
GObject *kms_base_rtp_endpoint_get_internal_session (KmsBaseRtpEndpoint *self, guint session_id);

G_END_DECLS
#endif /* __KMS_BASE_RTP_ENDPOINT_H__ */
