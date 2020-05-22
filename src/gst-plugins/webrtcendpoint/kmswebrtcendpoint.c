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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmswebrtcendpoint.h"
#include "kmswebrtcsession.h"
#include <commons/constants.h>
#include <commons/kmsloop.h>
#include <commons/kmsutils.h>
#include <commons/sdp_utils.h>
#include <commons/kmsrefstruct.h>
#include <commons/sdpagent/kmssdprtpsavpfmediahandler.h>
#include <commons/sdpagent/kmssdpsctpmediahandler.h>
#include "kms-webrtc-marshal.h"
#include <glib/gstdio.h>
#include "kms-webrtc-data-marshal.h"

#define KMS_WEBRTC_DATA_CHANNEL_PPID_STRING 51
#define PLUGIN_NAME "webrtcendpoint"

#define GST_CAT_DEFAULT kms_webrtc_endpoint_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcEndpoint, kms_webrtc_endpoint,
    KMS_TYPE_BASE_RTP_ENDPOINT);

#define KMS_WEBRTC_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_ENDPOINT,                   \
    KmsWebrtcEndpointPrivate                    \
  )                                             \
)

#define DEFAULT_STUN_SERVER_IP NULL
#define DEFAULT_STUN_SERVER_PORT 3478
#define DEFAULT_STUN_TURN_URL NULL
#define DEFAULT_PEM_CERTIFICATE NULL
#define DEFAULT_NETWORK_INTERFACES NULL
#define DEFAULT_EXTERNAL_ADDRESS NULL

enum
{
  PROP_0,
  PROP_STUN_SERVER_IP,
  PROP_STUN_SERVER_PORT,
  PROP_TURN_URL,                /* user:password@address:port?transport=[udp|tcp|tls] */
  PROP_PEM_CERTIFICATE,
  PROP_NETWORK_INTERFACES,
  PROP_EXTERNAL_ADDRESS,
  N_PROPERTIES
};

enum
{
  SIGNAL_ON_ICE_CANDIDATE,
  SIGNAL_ON_ICE_GATHERING_DONE,
  SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED,
  SIGNAL_GATHER_CANDIDATES,
  SIGNAL_ADD_ICE_CANDIDATE,
  SIGNAL_DATA_SESSION_ESTABLISHED,
  SIGNAL_DATA_CHANNEL_OPENED,
  SIGNAL_DATA_CHANNEL_CLOSED,
  SIGNAL_NEW_SELECTED_PAIR_FULL,
  ACTION_CREATE_DATA_CHANNEL,
  ACTION_DESTROY_DATA_CHANNEL,
  ACTION_GET_DATA_CHANNEL_SUPPORTED,
  LAST_SIGNAL
};

static guint kms_webrtc_endpoint_signals[LAST_SIGNAL] = { 0 };

struct _KmsWebrtcEndpointPrivate
{
  KmsLoop *loop;
  GMainContext *context;

  gchar *stun_server_ip;
  guint stun_server_port;
  gchar *turn_url;
  gchar *pem_certificate;
  gchar *network_interfaces;
  gchar *external_address;
};

/* Internal session management begin */

static void
on_ice_candidate (KmsWebrtcSession * sess, KmsIceCandidate * candidate,
    KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  GST_DEBUG_OBJECT (self,
      "[IceCandidateFound] local: '%s', stream_id: %s, component_id: %d",
      kms_ice_candidate_get_candidate (candidate),
      kms_ice_candidate_get_stream_id (candidate),
      kms_ice_candidate_get_component (candidate));

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_CANDIDATE], 0,
      sdp_sess->id_str, candidate);
}

static void
on_ice_gathering_done (KmsWebrtcSession * sess, KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  GST_DEBUG_OBJECT (self, "[IceGatheringDone] session: '%s'", sdp_sess->id_str);

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_GATHERING_DONE], 0,
      sdp_sess->id_str);
}

static void
on_ice_component_state_change (KmsWebrtcSession * sess, const gchar * stream_id,
    guint component_id, IceState state, KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  GST_LOG_OBJECT (self,
      "[IceComponentStateChanged] state: %s, stream_id: %s, component_id: %u",
      kms_ice_base_agent_state_to_string (state), stream_id, component_id);

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED], 0,
      sdp_sess->id_str, stream_id, component_id, state);
}

static void
on_data_session_established (KmsWebrtcSession * sess, gboolean connected,
    KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  g_signal_emit (self,
      kms_webrtc_endpoint_signals[SIGNAL_DATA_SESSION_ESTABLISHED], 0,
      sdp_sess->id_str, connected);
}

static void
on_data_channel_opened (KmsWebrtcSession * sess, guint stream_id,
    KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  g_signal_emit (self, kms_webrtc_endpoint_signals[SIGNAL_DATA_CHANNEL_OPENED],
      0, sdp_sess->id_str, stream_id);
}

static void
on_data_channel_closed (KmsWebrtcSession * sess, guint stream_id,
    KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  g_signal_emit (self, kms_webrtc_endpoint_signals[SIGNAL_DATA_CHANNEL_CLOSED],
      0, sdp_sess->id_str, stream_id);
}

static void
kms_webrtc_endpoint_link_pads (GstPad * src, GstPad * sink)
{
  if (gst_pad_link_full (src, sink, GST_PAD_LINK_CHECK_CAPS) != GST_PAD_LINK_OK) {
    GST_ERROR ("Error linking pads (src: %" GST_PTR_FORMAT ", sink: %"
        GST_PTR_FORMAT ")", src, sink);
  }
}

static gboolean
kms_webrtc_endpoint_add_data_sink_pad (KmsWebrtcEndpoint * self,
    GstPad * target, const gchar * description)
{
  GstPad *pad;

  pad = kms_element_connect_sink_target_full (KMS_ELEMENT (self), target,
      KMS_ELEMENT_PAD_TYPE_DATA, description, NULL, NULL);

  if (pad == NULL) {
    GST_ERROR_OBJECT (self, "Can not add pad %" GST_PTR_FORMAT, target);
    return FALSE;
  }

  GST_TRACE_OBJECT (self, "Added pad %" GST_PTR_FORMAT, pad);

  return TRUE;
}

static gboolean
kms_webrtc_endpoint_add_data_src_pad (KmsWebrtcEndpoint * self, GstPad * pad,
    const gchar * description)
{
  GstElement *data_tee;
  GstPad *sink;

  data_tee = kms_element_get_data_output_element (KMS_ELEMENT (self),
      description);

  if (data_tee == NULL) {
    GST_ERROR_OBJECT (self, "Can not add pad %" GST_PTR_FORMAT, pad);
    return FALSE;
  }

  sink = gst_element_get_static_pad (data_tee, "sink");
  kms_webrtc_endpoint_link_pads (pad, sink);
  g_object_unref (sink);

  return TRUE;
}

static gboolean
kms_webrtc_endpoint_add_pad (KmsWebrtcSession * session, GstPad * pad,
    KmsElementPadType type, const gchar * description, gpointer user_data)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (user_data);
  gboolean ret = FALSE;

  if (type != KMS_ELEMENT_PAD_TYPE_DATA) {
    GST_WARNING_OBJECT (self, "Unsupported pad type %u", type);
    return FALSE;
  }

  switch (gst_pad_get_direction (pad)) {
    case GST_PAD_SINK:
      ret = kms_webrtc_endpoint_add_data_sink_pad (self, pad, description);
      break;
    case GST_PAD_SRC:
      ret = kms_webrtc_endpoint_add_data_src_pad (self, pad, description);
      break;
    default:
      GST_ERROR_OBJECT (self, "Invalid direction of pad %" GST_PTR_FORMAT, pad);
      return FALSE;
  }

  return ret;
}

static gboolean
kms_webrtc_endpoint_remove_pad (KmsWebrtcSession * session, GstPad * pad,
    KmsElementPadType type, const gchar * description, gpointer user_data)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (user_data);

  if (type != KMS_ELEMENT_PAD_TYPE_DATA) {
    GST_ERROR_OBJECT (self, "Unsupported pad type %u", type);
    return FALSE;
  }

  if (gst_pad_get_direction (pad) != GST_PAD_SINK) {
    GST_ERROR_OBJECT (self, "Failed to remove pad %" GST_PTR_FORMAT
        "Only sink pads can be removed", pad);
    return FALSE;
  }

  GST_TRACE_OBJECT (self, "Remove sink pad %" GST_PTR_FORMAT, pad);

  kms_element_remove_sink_by_type_full (KMS_ELEMENT (self), type, description);

  return TRUE;
}

static void
new_selected_pair_full (KmsWebrtcSession * sess,
    gchar * stream_id,
    guint component_id,
    KmsIceCandidate * lcandidate,
    KmsIceCandidate * rcandidate, KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  GST_DEBUG_OBJECT (self,
      "[NewCandidatePairSelected] local: '%s', remote: '%s'"
      ", stream_id: %s, component_id: %u",
      kms_ice_candidate_get_candidate (lcandidate),
      kms_ice_candidate_get_candidate (rcandidate),
      stream_id, component_id);

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_NEW_SELECTED_PAIR_FULL], 0,
      sdp_sess->id_str, stream_id, component_id, lcandidate, rcandidate);
}

static void
kms_webrtc_endpoint_create_session_internal (KmsBaseSdpEndpoint * base_sdp,
    gint id, KmsSdpSession ** sess)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (base_sdp);
  KmsIRtpSessionManager *manager = KMS_I_RTP_SESSION_MANAGER (self);
  KmsWebrtcSessionCallbacks callbacks;
  KmsWebrtcSession *webrtc_sess;

  webrtc_sess =
      kms_webrtc_session_new (base_sdp, id, manager, self->priv->context);

  callbacks.add_pad_cb = kms_webrtc_endpoint_add_pad;
  callbacks.remove_pad_cb = kms_webrtc_endpoint_remove_pad;

  kms_webrtc_session_set_callbacks (webrtc_sess, &callbacks, self, NULL);

  g_object_bind_property (self, "stun-server",
      webrtc_sess, "stun-server", G_BINDING_DEFAULT);
  g_object_bind_property (self, "stun-server-port",
      webrtc_sess, "stun-server-port", G_BINDING_DEFAULT);
  g_object_bind_property (self, "turn-url",
      webrtc_sess, "turn-url", G_BINDING_DEFAULT);
  g_object_bind_property (self, "pem-certificate",
      webrtc_sess, "pem-certificate", G_BINDING_DEFAULT);
  g_object_bind_property (self, "network-interfaces",
      webrtc_sess, "network-interfaces", G_BINDING_DEFAULT);
  g_object_bind_property (self, "external-address",
      webrtc_sess, "external-address", G_BINDING_DEFAULT);

  g_object_set (webrtc_sess, "stun-server", self->priv->stun_server_ip,
      "stun-server-port", self->priv->stun_server_port,
      "turn-url", self->priv->turn_url,
      "pem-certificate", self->priv->pem_certificate,
      "network-interfaces", self->priv->network_interfaces,
      "external-address", self->priv->external_address, NULL);

  g_signal_connect (webrtc_sess, "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), self);
  g_signal_connect (webrtc_sess, "on-ice-gathering-done",
      G_CALLBACK (on_ice_gathering_done), self);
  g_signal_connect (webrtc_sess, "on-ice-component-state-changed",
      G_CALLBACK (on_ice_component_state_change), self);
  g_signal_connect (webrtc_sess, "new-selected-pair-full",
      G_CALLBACK (new_selected_pair_full), self);

  g_signal_connect (webrtc_sess, "data-session-established",
      G_CALLBACK (on_data_session_established), self);
  g_signal_connect (webrtc_sess, "data-channel-opened",
      G_CALLBACK (on_data_channel_opened), self);
  g_signal_connect (webrtc_sess, "data-channel-closed",
      G_CALLBACK (on_data_channel_closed), self);

  *sess = KMS_SDP_SESSION (webrtc_sess);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->create_session_internal (base_sdp, id,
      sess);

  g_signal_emit_by_name (webrtc_sess, "init-ice-agent");
}

/* Internal session management end */

/* Media handler management begin */
static void
kms_webrtc_endpoint_create_media_handler (KmsBaseSdpEndpoint * base_sdp,
    const gchar * media, KmsSdpMediaHandler ** handler)
{
  if (g_strcmp0 (media, "audio") == 0 || g_strcmp0 (media, "video") == 0) {
    *handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_rtp_savpf_media_handler_new ());
  } else if (g_strcmp0 (media, "application") == 0) {
    *handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_sctp_media_handler_new ());
  }

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->create_media_handler (base_sdp, media,
      handler);
}

/* Media handler management end */

/* Configure media SDP begin */

static gboolean
kms_webrtc_endpoint_configure_media (KmsBaseSdpEndpoint *
    base_sdp_endpoint, KmsSdpSession * sess, KmsSdpMediaHandler * handler,
    GstSDPMedia * media)
{
  KmsWebrtcSession *webrtc_sess = KMS_WEBRTC_SESSION (sess);
  gboolean ret;

  /* Chain up */
  ret = KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->configure_media (base_sdp_endpoint,
      sess, handler, media);
  if (ret == FALSE) {
    return FALSE;
  }

  if (!kms_webrtc_session_set_ice_credentials (webrtc_sess, handler, media)) {
    return FALSE;
  }

  return kms_webrtc_session_set_crypto_info (webrtc_sess, handler, media);
}

/* Configure media SDP end */

static void
kms_webrtc_endpoint_start_transport_send (KmsBaseSdpEndpoint *
    base_sdp_endpoint, KmsSdpSession * sess, gboolean offerer)
{
  KmsWebrtcSession *webrtc_sess = KMS_WEBRTC_SESSION (sess);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->start_transport_send
      (base_sdp_endpoint, sess, offerer);

  kms_webrtc_session_start_transport_send (webrtc_sess, offerer);
}

/* ICE candidates management begin */

static gboolean
kms_webrtc_endpoint_gather_candidates (KmsWebrtcEndpoint * self,
    const gchar * sess_id)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;
  gboolean ret = TRUE;

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "[IceGatheringStarted] No session: '%s'", sess_id);
    return FALSE;
  }

  GST_DEBUG_OBJECT (self, "[IceGatheringStarted] session: '%s'", sess_id);

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_signal_emit_by_name (webrtc_sess, "gather-candidates", &ret);

  return ret;
}

static gboolean
kms_webrtc_endpoint_add_ice_candidate (KmsWebrtcEndpoint * self,
    const gchar * sess_id, KmsIceCandidate * candidate)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;
  gboolean ret;

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "[AddIceCandidate] No session: '%s'", sess_id);
    return FALSE;
  }

  // Remote candidates haven't been assigned a stream_id yet, so don't print it
  GST_DEBUG_OBJECT (self,
      "[AddIceCandidate] remote: '%s', component_id: %d",
      kms_ice_candidate_get_candidate (candidate),
      kms_ice_candidate_get_component (candidate));

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_signal_emit_by_name (webrtc_sess, "add-ice-candidate", candidate, &ret);

  return ret;
}

/* ICE candidates management end */

static void
kms_webrtc_endpoint_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_STUN_SERVER_IP:
      g_free (self->priv->stun_server_ip);
      self->priv->stun_server_ip = g_value_dup_string (value);
      break;
    case PROP_STUN_SERVER_PORT:
      self->priv->stun_server_port = g_value_get_uint (value);
      break;
    case PROP_TURN_URL:
      g_free (self->priv->turn_url);
      self->priv->turn_url = g_value_dup_string (value);
      break;
    case PROP_PEM_CERTIFICATE:
      g_free (self->priv->pem_certificate);
      self->priv->pem_certificate = g_value_dup_string (value);
      break;
    case PROP_NETWORK_INTERFACES:
      g_free (self->priv->network_interfaces);
      self->priv->network_interfaces = g_value_dup_string (value);
      break;
    case PROP_EXTERNAL_ADDRESS:
      g_free (self->priv->external_address);
      self->priv->external_address = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_webrtc_endpoint_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_STUN_SERVER_IP:
      g_value_set_string (value, self->priv->stun_server_ip);
      break;
    case PROP_STUN_SERVER_PORT:
      g_value_set_uint (value, self->priv->stun_server_port);
      break;
    case PROP_TURN_URL:
      g_value_set_string (value, self->priv->turn_url);
      break;
    case PROP_PEM_CERTIFICATE:
      g_value_set_string (value, self->priv->pem_certificate);
      break;
    case PROP_NETWORK_INTERFACES:
      g_value_set_string (value, self->priv->network_interfaces);
      break;
    case PROP_EXTERNAL_ADDRESS:
      g_value_set_string (value, self->priv->external_address);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_webrtc_endpoint_dispose (GObject * object)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  GST_LOG_OBJECT (self, "dispose");

  KMS_ELEMENT_LOCK (self);

  g_clear_object (&self->priv->loop);

  KMS_ELEMENT_UNLOCK (self);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_endpoint_parent_class)->dispose (object);
}

static void
kms_webrtc_endpoint_finalize (GObject * object)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  GST_LOG_OBJECT (self, "finalize");

  g_free (self->priv->stun_server_ip);
  g_free (self->priv->turn_url);
  g_free (self->priv->pem_certificate);
  g_free (self->priv->network_interfaces);
  g_free (self->priv->external_address);

  g_main_context_unref (self->priv->context);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_endpoint_parent_class)->finalize (object);
}

static gint
kms_webrtc_endpoint_create_data_channel (KmsWebrtcEndpoint * self,
    const gchar * sess_id, gboolean ordered, gint max_packet_life_time,
    gint max_retransmits, const gchar * label, const gchar * protocol)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;
  gint stream_id;

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "No session: '%s'", sess_id);
    return -1;
  }

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_signal_emit_by_name (webrtc_sess, "create-data-channel", ordered,
      max_packet_life_time, max_retransmits, label, protocol, &stream_id);

  return stream_id;
}

static void
kms_webrtc_endpoint_destroy_data_channel (KmsWebrtcEndpoint * self,
    const gchar * sess_id, gint stream_id)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "No session: '%s'", sess_id);
    return;
  }

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_signal_emit_by_name (webrtc_sess, "destroy-data-channel", stream_id);
}

static gboolean
kms_webrtc_endpoint_get_data_channel_supported (KmsWebrtcEndpoint * self,
    const gchar * sess_id)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;
  gboolean ret;

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "No session: '%s'", sess_id);
    return FALSE;
  }

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_object_get (webrtc_sess, "data-channel-supported", &ret, NULL);

  return ret;
}

typedef struct _KmsSessStats
{
  GstStructure *stats;
  const gchar *selector;
} KmsSessStats;

static void
kms_base_rtp_endpoint_add_session_stats (gpointer key, gpointer value,
    KmsSessStats * ss)
{
  KmsWebrtcSession *session = KMS_WEBRTC_SESSION (value);

  kms_webrtc_session_add_data_channels_stats (session, ss->stats, ss->selector);
}

static GstStructure *
kms_webrtc_endpoint_stats (KmsElement * obj, gchar * selector)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (obj);
  GstStructure *stats;
  KmsSessStats ss;
  GHashTable *sessions;

  /* chain up */
  stats =
      KMS_ELEMENT_CLASS (kms_webrtc_endpoint_parent_class)->stats (obj,
      selector);
  ss.stats = stats;
  ss.selector = selector;

  sessions = kms_base_sdp_endpoint_get_sessions (KMS_BASE_SDP_ENDPOINT (self));
  g_hash_table_foreach (sessions,
      (GHFunc) kms_base_rtp_endpoint_add_session_stats, &ss);

  return stats;
}

static void
kms_webrtc_endpoint_class_init (KmsWebrtcEndpointClass * klass)
{
  GObjectClass *gobject_class;
  KmsElementClass *kmselement_class;
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_webrtc_endpoint_set_property;
  gobject_class->get_property = kms_webrtc_endpoint_get_property;
  gobject_class->dispose = kms_webrtc_endpoint_dispose;
  gobject_class->finalize = kms_webrtc_endpoint_finalize;

  kmselement_class = KMS_ELEMENT_CLASS (klass);
  kmselement_class->stats = GST_DEBUG_FUNCPTR (kms_webrtc_endpoint_stats);

  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "WebrtcEndpoint",
      "WEBRTC/Stream/WebrtcEndpoint",
      "WebRTC Endpoint element", "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);
  base_sdp_endpoint_class->create_session_internal =
      kms_webrtc_endpoint_create_session_internal;
  base_sdp_endpoint_class->start_transport_send =
      kms_webrtc_endpoint_start_transport_send;

  /* Media handler management */
  base_sdp_endpoint_class->create_media_handler =
      kms_webrtc_endpoint_create_media_handler;

  base_sdp_endpoint_class->configure_media =
      kms_webrtc_endpoint_configure_media;

  klass->gather_candidates = kms_webrtc_endpoint_gather_candidates;
  klass->add_ice_candidate = kms_webrtc_endpoint_add_ice_candidate;
  klass->create_data_channel = kms_webrtc_endpoint_create_data_channel;
  klass->destroy_data_channel = kms_webrtc_endpoint_destroy_data_channel;
  klass->get_data_channel_supported =
      kms_webrtc_endpoint_get_data_channel_supported;

  g_object_class_install_property (gobject_class, PROP_STUN_SERVER_IP,
      g_param_spec_string ("stun-server",
          "StunServer",
          "Stun Server IP Address",
          DEFAULT_STUN_SERVER_IP, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_STUN_SERVER_PORT,
      g_param_spec_uint ("stun-server-port",
          "StunServerPort",
          "Stun Server Port",
          1, G_MAXUINT16, DEFAULT_STUN_SERVER_PORT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_TURN_URL,
      g_param_spec_string ("turn-url",
          "TurnUrl",
          "TURN server URL with this format: 'user:password@address:port(?transport=[udp|tcp|tls])'."
          "'address' must be an IP (not a domain)."
          "'transport' is optional (UDP by default).",
          DEFAULT_STUN_TURN_URL, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PEM_CERTIFICATE,
      g_param_spec_string ("pem-certificate",
          "PemCertificate",
          "Pem certificate to be used in dtls",
          DEFAULT_PEM_CERTIFICATE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_NETWORK_INTERFACES,
      g_param_spec_string ("network-interfaces",
          "networkInterfaces",
          "Local network interfaces used for ICE gathering",
          DEFAULT_NETWORK_INTERFACES, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_EXTERNAL_ADDRESS,
      g_param_spec_string ("external-address",
          "externalAddress",
          "External (public) IP address of the media server",
          DEFAULT_EXTERNAL_ADDRESS, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
  * KmsWebrtcEndpoint::on-ice-candidate:
  * @self: the object which received the signal
  * @sess_id: id of the related WebRTC session
  * @candidate: the local candidate gathered
  *
  * Notify of a new gathered local candidate for a #KmsWebrtcEndpoint.
  */
  kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_CANDIDATE] =
      g_signal_new ("on-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, on_ice_candidate), NULL,
      NULL, __kms_webrtc_marshal_VOID__STRING_OBJECT, G_TYPE_NONE, 2,
      G_TYPE_STRING, KMS_TYPE_ICE_CANDIDATE);

  /**
  * KmsWebrtcEndpoint::on-ice-gathering-done:
  * @self: the object which received the signal
  * @sess_id: id of the related WebRTC session
  * @stream_id: The ID of the stream
  *
  * Notify that all candidates have been gathered for a #KmsWebrtcEndpoint
  */
  kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_GATHERING_DONE] =
      g_signal_new ("on-ice-gathering-done",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, on_ice_gathering_done), NULL,
      NULL, g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 1, G_TYPE_STRING);

  /**
   * KmsWebrtcEndpoint::on-component-state-changed
   * @self: the object which received the signal
   * @sess_id: id of the related WebRTC session
   * @stream_id: The ID of the stream
   * @component_id: The ID of the component
   * @state: The #NiceComponentState of the component
   *
   * This signal is fired whenever a component's state changes
   */
  kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED] =
      g_signal_new ("on-ice-component-state-changed",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 4, G_TYPE_STRING, G_TYPE_STRING, G_TYPE_UINT, G_TYPE_UINT,
      G_TYPE_INVALID);

  kms_webrtc_endpoint_signals[SIGNAL_NEW_SELECTED_PAIR_FULL] =
      g_signal_new ("new-selected-pair-full",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 5, G_TYPE_STRING, G_TYPE_STRING, G_TYPE_UINT,
      KMS_TYPE_ICE_CANDIDATE, KMS_TYPE_ICE_CANDIDATE);

  kms_webrtc_endpoint_signals[SIGNAL_ADD_ICE_CANDIDATE] =
      g_signal_new ("add-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, add_ice_candidate), NULL, NULL,
      __kms_webrtc_marshal_BOOLEAN__STRING_OBJECT, G_TYPE_BOOLEAN, 2,
      G_TYPE_STRING, KMS_TYPE_ICE_CANDIDATE);

  kms_webrtc_endpoint_signals[SIGNAL_GATHER_CANDIDATES] =
      g_signal_new ("gather-candidates",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, gather_candidates), NULL, NULL,
      __kms_webrtc_marshal_BOOLEAN__STRING, G_TYPE_BOOLEAN, 1, G_TYPE_STRING);

  kms_webrtc_endpoint_signals[SIGNAL_DATA_SESSION_ESTABLISHED] =
      g_signal_new ("data-session-established",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, data_session_established),
      NULL, NULL, __kms_webrtc_data_marshal_VOID__STRING_BOOLEAN, G_TYPE_NONE,
      2, G_TYPE_STRING, G_TYPE_BOOLEAN);

  kms_webrtc_endpoint_signals[SIGNAL_DATA_CHANNEL_OPENED] =
      g_signal_new ("data-channel-opened",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, data_channel_opened),
      NULL, NULL, __kms_webrtc_data_marshal_VOID__STRING_UINT, G_TYPE_NONE, 2,
      G_TYPE_STRING, G_TYPE_UINT);

  kms_webrtc_endpoint_signals[SIGNAL_DATA_CHANNEL_CLOSED] =
      g_signal_new ("data-channel-closed",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, data_channel_closed),
      NULL, NULL, __kms_webrtc_data_marshal_VOID__STRING_UINT, G_TYPE_NONE, 2,
      G_TYPE_STRING, G_TYPE_UINT);

  kms_webrtc_endpoint_signals[ACTION_CREATE_DATA_CHANNEL] =
      g_signal_new ("create-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, create_data_channel),
      NULL, NULL,
      __kms_webrtc_data_marshal_INT__STRING_BOOLEAN_INT_INT_STRING_STRING,
      G_TYPE_INT, 6, G_TYPE_STRING, G_TYPE_BOOLEAN, G_TYPE_INT, G_TYPE_INT,
      G_TYPE_STRING, G_TYPE_STRING);

  kms_webrtc_endpoint_signals[ACTION_DESTROY_DATA_CHANNEL] =
      g_signal_new ("destroy-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, destroy_data_channel),
      NULL, NULL, __kms_webrtc_data_marshal_VOID__STRING_INT, G_TYPE_NONE, 2,
      G_TYPE_STRING, G_TYPE_INT);

  kms_webrtc_endpoint_signals[ACTION_GET_DATA_CHANNEL_SUPPORTED] =
      g_signal_new ("get-data-channel-supported",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, get_data_channel_supported),
      NULL, NULL, __kms_webrtc_marshal_BOOLEAN__STRING, G_TYPE_BOOLEAN, 1,
      G_TYPE_STRING);

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndpointPrivate));
}

static void
kms_webrtc_endpoint_init (KmsWebrtcEndpoint * self)
{
  /* TODO: check which prop should be moved to session */
  g_object_set (G_OBJECT (self), "bundle", TRUE, "rtcp-mux", TRUE, "rtcp-nack",
      TRUE, "rtcp-remb", TRUE, NULL);

  self->priv = KMS_WEBRTC_ENDPOINT_GET_PRIVATE (self);
  self->priv->stun_server_ip = DEFAULT_STUN_SERVER_IP;
  self->priv->stun_server_port = DEFAULT_STUN_SERVER_PORT;
  self->priv->turn_url = DEFAULT_STUN_TURN_URL;
  self->priv->pem_certificate = DEFAULT_PEM_CERTIFICATE;
  self->priv->network_interfaces = DEFAULT_NETWORK_INTERFACES;
  self->priv->external_address = DEFAULT_EXTERNAL_ADDRESS;

  self->priv->loop = kms_loop_new ();
  g_object_get (self->priv->loop, "context", &self->priv->context, NULL);
}

gboolean
kms_webrtc_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_ENDPOINT);
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmswebrtcendpoint,
    "Kurento webrtc endpoint",
    kms_webrtc_endpoint_plugin_init, VERSION, GST_LICENSE_UNKNOWN,
    "Kurento Elements", "http://kurento.com/")
