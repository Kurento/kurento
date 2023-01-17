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

#ifndef __KMS_ICE_ICE_CANDIDATE_H__
#define __KMS_ICE_ICE_CANDIDATE_H__

#include <glib.h>
#include <glib-object.h>

#define SDP_ICE_UFRAG_ATTR "ice-ufrag"
#define SDP_ICE_PWD_ATTR "ice-pwd"
#define SDP_CANDIDATE_ATTR "candidate"
#define SDP_CANDIDATE_ATTR_LEN 12

G_BEGIN_DECLS
#define KMS_TYPE_ICE_CANDIDATE            (kms_ice_candidate_get_type())
#define KMS_ICE_CANDIDATE(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), KMS_TYPE_ICE_CANDIDATE, KmsIceCandidate))
#define KMS_ICE_CANDIDATE_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass), KMS_TYPE_ICE_CANDIDATE, KmsIceCandidateClass))
#define KMS_IS_ICE_CANDIDATE(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), KMS_TYPE_ICE_CANDIDATE))
#define KMS_IS_ICE_CANDIDATE_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass), KMS_TYPE_ICE_CANDIDATE))
#define KMS_ICE_CANDIDATE_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj), KMS_TYPE_ICE_CANDIDATE, KmsIceCandidateClass))

typedef struct _KmsIceCandidate KmsIceCandidate;
typedef struct _KmsIceCandidateClass KmsIceCandidateClass;
typedef struct _KmsIceCandidatePrivate KmsIceCandidatePrivate;

struct _KmsIceCandidate
{
  GObject parent_instance;

  KmsIceCandidatePrivate *priv;
};

struct _KmsIceCandidateClass
{
  GObjectClass parent_class;
};

typedef enum
{
  KMS_ICE_COMPONENT_RTP,
  KMS_ICE_COMPONENT_RTCP
} KmsIceComponent;

typedef enum
{
  KMS_ICE_PROTOCOL_UDP,
  KMS_ICE_PROTOCOL_TCP
} KmsIceProtocol;

typedef enum
{
  KMS_ICE_CANDIDATE_TYPE_HOST,
  KMS_ICE_CANDIDATE_TYPE_SRFLX,
  KMS_ICE_CANDIDATE_TYPE_PRFLX,
  KMS_ICE_CANDIDATE_TYPE_RELAY
} KmsIceCandidateType;

typedef enum
{
  KMS_ICE_TCP_CANDIDATE_TYPE_NONE,
  KMS_ICE_TCP_CANDIDATE_TYPE_ACTIVE,
  KMS_ICE_TCP_CANDIDATE_TYPE_PASSIVE,
  KMS_ICE_TCP_CANDIDATE_TYPE_SO
} KmsIceTcpCandidateType;

GType kms_ice_candidate_get_type (void);

KmsIceCandidate *kms_ice_candidate_new (const gchar * candidate,
    const gchar * sdp_mid, guint8 sdp_m_line_index, const gchar *stream_id);

/* TODO: Use GObject getters instead */
const gchar * kms_ice_candidate_get_candidate (KmsIceCandidate * self);
const gchar * kms_ice_candidate_get_sdp_mid (KmsIceCandidate * self);
gchar * kms_ice_candidate_get_address (KmsIceCandidate * self);
const guint kms_ice_candidate_get_port (KmsIceCandidate * self);
int kms_ice_candidate_get_ip_version (KmsIceCandidate * self);
guint8 kms_ice_candidate_get_sdp_m_line_index (KmsIceCandidate * self);
const gchar * kms_ice_candidate_get_stream_id (KmsIceCandidate * self);
gchar * kms_ice_candidate_get_sdp_line (KmsIceCandidate * self);
gchar * kms_ice_candidate_get_foundation (KmsIceCandidate * self);
guint kms_ice_candidate_get_priority (KmsIceCandidate * self);
KmsIceComponent kms_ice_candidate_get_component (KmsIceCandidate * self);
KmsIceProtocol kms_ice_candidate_get_protocol (KmsIceCandidate * self);
KmsIceCandidateType kms_ice_candidate_get_candidate_type (KmsIceCandidate * self);
KmsIceTcpCandidateType kms_ice_candidate_get_candidate_tcp_type (KmsIceCandidate * self);
gchar * kms_ice_candidate_get_related_address (KmsIceCandidate * self);
gint kms_ice_candidate_get_related_port (KmsIceCandidate * self);
gboolean kms_ice_candidate_get_valid (KmsIceCandidate * self);

void kms_ice_candidate_set_address (KmsIceCandidate * self, const gchar * address);

G_END_DECLS
#endif /* __KMS_ICE_ICE_CANDIDATE_H__ */
