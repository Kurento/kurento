/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

GType kms_ice_candidate_get_type (void);

KmsIceCandidate *kms_ice_candidate_new (const gchar * candidate,
    const gchar * sdp_mid, guint8 sdp_m_line_index, const gchar *stream_id);

const gchar * kms_ice_candidate_get_candidate (KmsIceCandidate * self);
const gchar * kms_ice_candidate_get_sdp_mid (KmsIceCandidate * self);
gchar * kms_ice_candidate_get_address (KmsIceCandidate * self);
const guint kms_ice_candidate_get_port (KmsIceCandidate * self);
int kms_ice_candidate_get_ip_version (KmsIceCandidate * self);
guint8 kms_ice_candidate_get_sdp_m_line_index (KmsIceCandidate * self);
const gchar * kms_ice_candidate_get_stream_id (KmsIceCandidate * self);
gchar * kms_ice_candidate_get_sdp_line (KmsIceCandidate * self);

G_END_DECLS
#endif /* __KMS_ICE_ICE_CANDIDATE_H__ */
