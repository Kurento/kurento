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

typedef gboolean (*KmsAddPad) (KmsWebrtcSession * self, GstPad *pad, KmsElementPadType type, const gchar *description, gpointer user_data);
typedef gboolean (*KmsRemovePad) (KmsWebrtcSession * self, GstPad *pad, KmsElementPadType type, const gchar *description, gpointer user_data);

typedef struct {
  KmsAddPad add_pad_cb;
  KmsRemovePad remove_pad_cb;
} KmsWebrtcSessionCallbacks;

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
  gchar *pem_certificate;
  gchar *network_interfaces;
  gchar *external_address;

  guint16 min_port;
  guint16 max_port;

  gboolean gather_started;

  GstElement *data_session;
  GHashTable *data_channels;

  KmsAddPad add_pad_cb;
  KmsRemovePad remove_pad_cb;
  gpointer cb_data;
  GDestroyNotify destroy_data;
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

KmsWebRtcBaseConnection * kms_webrtc_session_get_connection (KmsWebrtcSession * self, KmsSdpMediaHandler * handler);
gboolean kms_webrtc_session_set_ice_credentials (KmsWebrtcSession * self, KmsSdpMediaHandler *handler, GstSDPMedia *media);
gboolean kms_webrtc_session_set_ice_candidates (KmsWebrtcSession * self, KmsSdpMediaHandler * handler, GstSDPMedia *media);
gboolean kms_webrtc_session_set_crypto_info (KmsWebrtcSession * self, KmsSdpMediaHandler * handler, GstSDPMedia *media);
gchar * kms_webrtc_session_get_stream_id (KmsWebrtcSession * self, KmsSdpMediaHandler *handler);

void kms_webrtc_session_start_transport_send (KmsWebrtcSession * self, gboolean offerer);

void kms_webrtc_session_add_data_channels_stats (KmsWebrtcSession * self, GstStructure * stats, const gchar * selector);

void kms_webrtc_session_set_callbacks (KmsWebrtcSession * self, KmsWebrtcSessionCallbacks *cb, gpointer user_data, GDestroyNotify notify);

G_END_DECLS
#endif /* __KMS_WEBRTC_SESSION_H__ */
