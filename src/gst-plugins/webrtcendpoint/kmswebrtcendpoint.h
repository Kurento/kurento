/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#ifndef __KMS_WEBRTC_ENDPOINT_H__
#define __KMS_WEBRTC_ENDPOINT_H__

#include <commons/kmsbasertpendpoint.h>
#include "kmsicecandidate.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_ENDPOINT \
  (kms_webrtc_endpoint_get_type())
#define KMS_WEBRTC_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_ENDPOINT,KmsWebrtcEndpoint))
#define KMS_WEBRTC_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_ENDPOINT,KmsWebrtcEndpointClass))
#define KMS_IS_WEBRTC_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_ENDPOINT))
#define KMS_IS_WEBRTC_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_ENDPOINT))
#define KMS_WEBRTC_ENDPOINT_CAST(obj) ((KmsWebrtcEndpoint*)(obj))

typedef struct _KmsWebrtcEndpointPrivate KmsWebrtcEndpointPrivate;
typedef struct _KmsWebrtcEndpoint KmsWebrtcEndpoint;
typedef struct _KmsWebrtcEndpointClass KmsWebrtcEndpointClass;

struct _KmsWebrtcEndpoint
{
  KmsBaseRtpEndpoint parent;

  KmsWebrtcEndpointPrivate *priv;
};

struct _KmsWebrtcEndpointClass
{
  KmsBaseRtpEndpointClass parent_class;

  gboolean (*gather_candidates) (KmsWebrtcEndpoint * self, const gchar *sess_id);
  gboolean (*add_ice_candidate) (KmsWebrtcEndpoint * self, const gchar * sess_id,
      KmsIceCandidate * candidate);

  gint (*create_data_channel) (KmsWebrtcEndpoint *self, const gchar *sess_id, gboolean ordered, gint max_packet_life_time, gint max_retransmits, const gchar * label, const gchar * protocol);
  void (*destroy_data_channel) (KmsWebrtcEndpoint *self, const gchar *sess_id, gint stream_id);
  gboolean (*get_data_channel_supported) (KmsWebrtcEndpoint * self, const gchar * sess_id);

  /* Signals */
  void (*on_ice_candidate) (KmsWebrtcEndpoint * self, const gchar *sess_id,
      KmsIceCandidate * candidate);
  void (*on_ice_gathering_done) (KmsWebrtcEndpoint * self, const gchar *sess_id);
  void (*data_session_established) (KmsWebrtcEndpoint *self, const gchar *sess_id, gboolean connected);
  void (*data_channel_opened) (KmsWebrtcEndpoint *self, const gchar *sess_id, guint stream_id);
  void (*data_channel_closed) (KmsWebrtcEndpoint *self, const gchar *sess_id, guint stream_id);
};

GType kms_webrtc_endpoint_get_type (void);

gboolean kms_webrtc_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_WEBRTC_ENDPOINT_H__ */
