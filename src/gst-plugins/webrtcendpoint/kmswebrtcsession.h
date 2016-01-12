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

#ifndef __KMS_WEBRTC_SESSION_H__
#define __KMS_WEBRTC_SESSION_H__

#include <gst/gst.h>
#include <commons/kmsbasertpsession.h>
#include "kmsicecandidate.h"
#include "kmsicebaseagent.h"
#include "kmswebrtcconnection.h"

G_BEGIN_DECLS

typedef struct _KmsIRtpSessionManager KmsIRtpSessionManager;
typedef struct _KmsIWebRtcDataChannelManager KmsIWebRtcDataChannelManager;

/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_SESSION \
  (kms_webrtc_session_get_type())
#define KMS_WEBRTC_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_SESSION,KmsWebrtcSession))
#define KMS_WEBRTC_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_SESSION,KmsWebrtcSessionClass))
#define KMS_IS_WEBRTC_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_SESSION))
#define KMS_IS_WEBRTC_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_SESSION))
#define KMS_WEBRTC_SESSION_CAST(obj) ((KmsWebrtcSession*)(obj))

typedef struct _KmsWebrtcSession KmsWebrtcSession;
typedef struct _KmsWebrtcSessionClass KmsWebrtcSessionClass;

struct _KmsWebrtcSession
{
  KmsBaseRtpSession parent;

  GMainContext * context;
  KmsIceBaseAgent *agent;
  GSList *remote_candidates;

  gchar *stun_server_ip;
  guint stun_server_port;
  gchar *turn_url;
  gchar *turn_user;
  gchar *turn_password;
  gchar *turn_address;
  guint turn_port;
  TurnProtocol turn_transport;

  guint16 min_port;
  guint16 max_port;

  gboolean gather_started;

  GstElement *data_session;
  GHashTable *data_channels;
};

struct _KmsWebrtcSessionClass
{
  KmsBaseRtpSessionClass parent_class;

  gboolean (*gather_candidates) (KmsWebrtcSession * self);
  gboolean (*add_ice_candidate) (KmsWebrtcSession * self, KmsIceCandidate * candidate);
  void (*init_ice_agent) (KmsWebrtcSession * self);

  gint (*create_data_channel) (KmsWebrtcSession * self, gboolean ordered, gint max_packet_life_time, gint max_retransmits, const gchar * label, const gchar * protocol);
  void (*destroy_data_channel) (KmsWebrtcSession * self, gint stream_id);

  /* Signals */
  void (*on_ice_candidate) (KmsWebrtcSession * self, KmsIceCandidate * candidate);
  void (*on_ice_gathering_done) (KmsWebrtcSession * self);
  void (*data_session_established) (KmsWebrtcSession * self, gboolean connected);
  void (*data_channel_opened) (KmsWebrtcSession * self, guint stream_id);
  void (*data_channel_closed) (KmsWebrtcSession * self, guint stream_id);

  /* private */
  /* virtual methods */
  void (*post_constructor) (KmsWebrtcSession * self, KmsBaseSdpEndpoint * ep,
			    guint id, KmsIRtpSessionManager * manager,
                            GMainContext * context);
};

GType kms_webrtc_session_get_type (void);

KmsWebrtcSession * kms_webrtc_session_new (KmsBaseSdpEndpoint * ep, guint id,
					   KmsIRtpSessionManager * manager,
                                           GMainContext * context);

KmsWebRtcBaseConnection * kms_webrtc_session_get_connection (KmsWebrtcSession * self, SdpMediaConfig * mconf);
gboolean kms_webrtc_session_set_ice_credentials (KmsWebrtcSession * self, SdpMediaConfig * mconf);
gboolean kms_webrtc_session_set_crypto_info (KmsWebrtcSession * self, SdpMediaConfig * mconf);
void kms_webrtc_session_remote_sdp_add_ice_candidate (KmsWebrtcSession * self, KmsIceCandidate *candidate, guint8 index);
gboolean kms_webrtc_session_set_remote_ice_candidate (KmsWebrtcSession * self, KmsIceCandidate * candidate);
gchar * kms_webrtc_session_get_stream_id (KmsWebrtcSession * self, SdpMediaConfig * mconf);

void kms_webrtc_session_start_transport_send (KmsWebrtcSession * self, gboolean offerer);

void kms_webrtc_session_add_data_channels_stats (KmsWebrtcSession * self, GstStructure * stats, const gchar * selector);

G_END_DECLS
#endif /* __KMS_WEBRTC_SESSION_H__ */
