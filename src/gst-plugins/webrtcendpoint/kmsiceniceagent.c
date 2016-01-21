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

#include "kmsiceniceagent.h"
#include <stdlib.h>

#define GST_CAT_DEFAULT kms_ice_nice_agent_debug
#define GST_DEFAULT_NAME "kmsiceniceagent"
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

G_DEFINE_TYPE (KmsIceNiceAgent, kms_ice_nice_agent, KMS_TYPE_ICE_BASE_AGENT);

#define KMS_ICE_NICE_AGENT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                \
    (obj),                                     \
    KMS_TYPE_ICE_NICE_AGENT,                   \
    KmsIceNiceAgentPrivate                     \
  )                                            \
)

#define KMS_NICE_N_COMPONENTS 2

struct _KmsIceNiceAgentPrivate
{
  GMainContext *context;
  NiceAgent *agent;
  GSList *remote_candidates;

  KmsWebrtcSession *session;
};

static void
sdp_media_add_ice_candidate (GstSDPMedia * media, NiceAgent * agent,
    NiceCandidate * cand)
{
  gchar *str;

  str = nice_agent_generate_local_candidate_sdp (agent, cand);
  gst_sdp_media_add_attribute (media, SDP_CANDIDATE_ATTR,
      str + SDP_CANDIDATE_ATTR_LEN);
  g_free (str);
}

static const gchar *
kms_ice_nice_agent_sdp_media_add_ice_candidate (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, NiceAgent * agent, NiceCandidate * cand)
{
  char *media_stream_id;
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  const gchar *mid;

  media_stream_id = kms_webrtc_session_get_stream_id (self, mconf);
  if (media_stream_id == NULL) {
    return NULL;
  }

  if (atoi (media_stream_id) != cand->stream_id) {
    return NULL;
  }

  sdp_media_add_ice_candidate (media, agent, cand);

  mid = kms_sdp_media_config_get_mid (mconf);
  if (mid == NULL) {
    return "";
  }

  return mid;
}

static void
kms_ice_nice_agent_sdp_msg_add_ice_candidate (KmsWebrtcSession * self,
    NiceAgent * agent, NiceCandidate * nice_cand, KmsIceBaseAgent * parent)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  SdpMessageContext *local_sdp_ctx = sdp_sess->local_sdp_ctx;
  const GSList *item = kms_sdp_message_context_get_medias (local_sdp_ctx);
  GList *list = NULL, *iterator = NULL;

  KMS_SDP_SESSION_LOCK (self);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *mconf = item->data;
    gint idx = kms_sdp_media_config_get_id (mconf);
    const gchar *mid;

    if (kms_sdp_media_config_is_inactive (mconf)) {
      GST_DEBUG_OBJECT (self, "Media (id=%d) inactive", idx);
      continue;
    }

    mid =
        kms_ice_nice_agent_sdp_media_add_ice_candidate (self, mconf,
        agent, nice_cand);
    if (mid != NULL) {
      KmsIceCandidate *candidate =
          kms_ice_candidate_new_from_nice (agent, nice_cand, mid, idx);
      list = g_list_append (list, candidate);
    }
  }

  KMS_SDP_SESSION_UNLOCK (self);

  for (iterator = list; iterator; iterator = iterator->next) {
    g_signal_emit_by_name (parent, "on-ice-candidate",
        KMS_ICE_CANDIDATE (iterator->data));
  }

  g_list_free_full (list, g_object_unref);
}

static void
kms_ice_nice_agent_new_candidate (NiceAgent * agent,
    guint stream_id,
    guint component_id, gchar * foundation, KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  GSList *candidates;
  GSList *walk;

  GST_TRACE_OBJECT (self,
      "stream_id: %d, component_id: %d, foundation: %s", stream_id,
      component_id, foundation);

  candidates = nice_agent_get_local_candidates (agent, stream_id, component_id);

  for (walk = candidates; walk; walk = walk->next) {
    NiceCandidate *cand = walk->data;

    if (cand->stream_id == stream_id &&
        cand->component_id == component_id &&
        g_strcmp0 (foundation, cand->foundation) == 0) {
      kms_ice_nice_agent_sdp_msg_add_ice_candidate (self->priv->session,
          self->priv->agent, cand, parent);
    }
  }
  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);
}

static void
kms_ice_nice_agent_gathering_done (NiceAgent * agent, guint stream_id,
    KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  char buff[33];
  char *ret;

  //convert id to char*
  g_snprintf (buff, 32, "%d", stream_id);

  ret = g_strdup (buff);

  g_signal_emit_by_name (parent, "on-ice-gathering-done", ret);

  g_free (ret);
}

static void
kms_ice_nice_agent_component_state_change (NiceAgent * agent, guint stream_id,
    guint component_id, NiceComponentState state, KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  IceState state_;
  char buff[33];
  char *ret;

  //convert id to char*
  g_snprintf (buff, 32, "%d", stream_id);

  ret = g_strdup (buff);

  switch (state) {
    case NICE_COMPONENT_STATE_DISCONNECTED:
      state_ = ICE_STATE_DISCONNECTED;
      break;
    case NICE_COMPONENT_STATE_GATHERING:
      state_ = ICE_STATE_GATHERING;
      break;
    case NICE_COMPONENT_STATE_CONNECTING:
      state_ = ICE_STATE_CONNECTING;
      break;
    case NICE_COMPONENT_STATE_CONNECTED:
      state_ = ICE_STATE_CONNECTED;
      break;
    case NICE_COMPONENT_STATE_READY:
      state_ = ICE_STATE_READY;
      break;
    case NICE_COMPONENT_STATE_FAILED:
      state_ = ICE_STATE_FAILED;
      break;
    default:
      state_ = ICE_STATE_FAILED;
      break;
  }

  GST_DEBUG_OBJECT (self,
      "stream_id: %d, component_id: %d, state: %s",
      stream_id, component_id, nice_component_state_to_string (state));

  g_signal_emit_by_name (parent, "on-ice-component-state-changed", ret,
      component_id, state_);
  g_free (ret);
}

KmsIceNiceAgent *
kms_ice_nice_agent_new (GMainContext * context, KmsWebrtcSession * session)
{
  GObject *obj;
  KmsIceNiceAgent *agent_object;

  obj = g_object_new (KMS_TYPE_ICE_NICE_AGENT, NULL);
  agent_object = KMS_ICE_NICE_AGENT (obj);
  agent_object->priv->context = context;

  agent_object->priv->agent =
      nice_agent_new (agent_object->priv->context, NICE_COMPATIBILITY_RFC5245);
  agent_object->priv->session = session;

  g_object_set (agent_object->priv->agent, "upnp", FALSE, NULL);

  g_signal_connect (agent_object->priv->agent, "new-candidate",
      G_CALLBACK (kms_ice_nice_agent_new_candidate), agent_object);
  g_signal_connect (agent_object->priv->agent, "candidate-gathering-done",
      G_CALLBACK (kms_ice_nice_agent_gathering_done), agent_object);
  g_signal_connect (agent_object->priv->agent, "component-state-changed",
      G_CALLBACK (kms_ice_nice_agent_component_state_change), agent_object);

  return agent_object;
}

static void
kms_ice_nice_agent_finalize (GObject * object)
{
  KmsIceNiceAgent *self = KMS_ICE_NICE_AGENT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_clear_object (&self->priv->agent);
  g_slist_free_full (self->priv->remote_candidates, g_object_unref);

  /* chain up */
  G_OBJECT_CLASS (kms_ice_nice_agent_parent_class)->finalize (object);
}

static void
kms_ice_nice_agent_init (KmsIceNiceAgent * self)
{
  self->priv = KMS_ICE_NICE_AGENT_GET_PRIVATE (self);
}

static char *
kms_ice_nice_agent_add_stream (KmsIceBaseAgent * self, const char *stream_id,
    guint16 min_port, guint16 max_port)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id =
      nice_agent_add_stream (nice_agent->priv->agent, KMS_NICE_N_COMPONENTS);
  int i;

  if (min_port != 0 && max_port != 0 && min_port != 1
      && max_port != G_MAXUINT16) {
    for (i = 1; i <= KMS_NICE_N_COMPONENTS; i++) {
      nice_agent_set_port_range (nice_agent->priv->agent, id, i, min_port,
          max_port);
    }
  }

  if (id == 0) {
    GST_ERROR_OBJECT (self, "Cannot add nice stream for %s.", stream_id);
    return NULL;
  }

  return g_strdup_printf ("%d", id);
}

static void
kms_ice_nice_agent_remove_stream (KmsIceBaseAgent * self, const char *stream_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  nice_agent_remove_stream (nice_agent->priv->agent, id);
}

static gboolean
kms_ice_nice_agent_set_remote_credentials (KmsIceBaseAgent * self,
    const char *stream_id, const char *ufrag, const char *pwd)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  return nice_agent_set_remote_credentials (nice_agent->priv->agent,
      id, ufrag, pwd);
}

static void
kms_ice_nice_agent_get_local_credentials (KmsIceBaseAgent * self,
    const char *stream_id, gchar ** ufrag, gchar ** pwd)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  nice_agent_get_local_credentials (nice_agent->priv->agent, id, ufrag, pwd);
}

static void
kms_ice_nice_agent_set_remote_description (KmsIceBaseAgent * self,
    const char *remote_description)
{
  GST_DEBUG_OBJECT (self, "Nothing to do in set_remote_description");
}

static void
kms_ice_nice_agent_set_local_description (KmsIceBaseAgent * self,
    const char *local_description)
{
  GST_DEBUG_OBJECT (self, "Nothing to do in set_local_description");
}

static NiceRelayType
from_turn_protocol_to_nice_relay (TurnProtocol transport)
{
  switch (transport) {
    case TURN_PROTOCOL_TCP:
      return NICE_RELAY_TYPE_TURN_TCP;
    case TURN_PROTOCOL_UDP:
      return NICE_RELAY_TYPE_TURN_UDP;
    case TURN_PROTOCOL_TLS:
      return NICE_RELAY_TYPE_TURN_TLS;
    case TURN_PROTOCOL_SSLTCP:
      return NICE_RELAY_TYPE_TURN_TLS;
    default:
      GST_DEBUG ("Wrong type of relay transport. Using TCP");
      return NICE_RELAY_TYPE_TURN_TCP;
  }
}

static void
kms_ice_nice_agent_add_relay_server (KmsIceBaseAgent * self,
    KmsIceRelayServerInfo server_info)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (server_info.stream_id);
  NiceRelayType type = from_turn_protocol_to_nice_relay (server_info.type);

  nice_agent_set_relay_info (nice_agent->priv->agent,
      id,
      NICE_COMPONENT_TYPE_RTP,
      server_info.server_ip,
      server_info.server_port,
      server_info.username, server_info.password, type);
  nice_agent_set_relay_info (nice_agent->priv->agent,
      id,
      NICE_COMPONENT_TYPE_RTCP,
      server_info.server_ip,
      server_info.server_port,
      server_info.username, server_info.password, type);
}

static gboolean
kms_ice_nice_agent_start_gathering_candidates (KmsIceBaseAgent * self,
    const char *stream_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  GST_DEBUG_OBJECT (self, "Start to gathering candidates");

  return nice_agent_gather_candidates (nice_agent->priv->agent, id);
}

static gboolean
kms_ice_nice_agent_add_ice_candidate (KmsIceBaseAgent * self,
    KmsIceCandidate * candidate, const char *stream_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  NiceCandidate *nice_cand;
  guint id = atoi (stream_id);
  gboolean ret;
  GSList *candidates;
  const gchar *cand_str;

  GST_DEBUG_OBJECT (self, "Add ICE candidate '%s'",
      kms_ice_candidate_get_candidate (candidate));

  ret = kms_ice_candidate_create_nice (candidate, &nice_cand);
  if (nice_cand == NULL) {
    return ret;
  }

  nice_cand->stream_id = id;
  cand_str = kms_ice_candidate_get_candidate (candidate);
  candidates = g_slist_append (NULL, nice_cand);

  if (nice_agent_set_remote_candidates (nice_agent->priv->agent,
          nice_cand->stream_id, nice_cand->component_id, candidates) < 0) {
    GST_WARNING_OBJECT (self, "Cannot add candidate: '%s'in stream_id: %d.",
        cand_str, nice_cand->stream_id);
    ret = FALSE;
  } else {
    GST_TRACE_OBJECT (self, "Candidate added: '%s' in stream_id: %d.",
        cand_str, nice_cand->stream_id);
    ret = TRUE;
  }

  g_slist_free (candidates);
  nice_candidate_free (nice_cand);

  return ret;
}

static gchar *
kms_ice_nice_agent_generate_local_candidate_sdp (KmsIceBaseAgent * self,
    KmsIceCandidate * candidate)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  NiceCandidate *nice_cand;
  gchar *ret;

  GST_DEBUG_OBJECT (self, "Add ICE candidate '%s'",
      kms_ice_candidate_get_candidate (candidate));

  kms_ice_candidate_create_nice (candidate, &nice_cand);
  if (nice_cand == NULL) {
    return NULL;
  }

  ret =
      nice_agent_generate_local_candidate_sdp (nice_agent->priv->agent,
      nice_cand);
  nice_candidate_free (nice_cand);

  return ret;
}

static KmsIceCandidate *
kms_ice_nice_agent_get_default_local_candidate (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  NiceCandidate *nice_cand;
  KmsIceCandidate *ret = NULL;
  guint id = atoi (stream_id);
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (nice_agent->priv->session);
  SdpMessageContext *local_sdp_ctx = sdp_sess->local_sdp_ctx;
  const GSList *item = kms_sdp_message_context_get_medias (local_sdp_ctx);

  nice_cand =
      nice_agent_get_default_local_candidate (nice_agent->priv->agent, id,
      component_id);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *mconf = item->data;
    gint idx = kms_sdp_media_config_get_id (mconf);
    const gchar *mid;
    gchar *media_stream_id;

    if (kms_sdp_media_config_is_inactive (mconf)) {
      GST_DEBUG_OBJECT (self, "Media (id=%d) inactive", idx);
      continue;
    }

    media_stream_id =
        kms_webrtc_session_get_stream_id (nice_agent->priv->session, mconf);
    if (media_stream_id == NULL) {
      goto end;
    }

    if (g_strcmp0 (media_stream_id, stream_id) != 0) {
      goto end;
    }

    mid = kms_sdp_media_config_get_mid (mconf);
    if (mid != NULL) {
      ret = kms_ice_candidate_new_from_nice (nice_agent->priv->agent, nice_cand,
          mid, idx);
      goto end;
    }
  }

end:
  nice_candidate_free (nice_cand);

  return ret;
}

static void
kms_ice_nice_agent_run_agent (KmsIceBaseAgent * self)
{
  GST_DEBUG_OBJECT (self, "Nothing to do in run_agent");
}

NiceAgent *
kms_ice_nice_agent_get_agent (KmsIceNiceAgent * agent)
{
  return agent->priv->agent;
}

static void
kms_ice_nice_agent_class_init (KmsIceNiceAgentClass * klass)
{
  GObjectClass *gobject_class;
  KmsIceBaseAgentClass *base_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_ice_nice_agent_finalize;

  base_class = KMS_ICE_BASE_AGENT_CLASS (klass);

  base_class->add_stream = kms_ice_nice_agent_add_stream;
  base_class->set_remote_credentials =
      kms_ice_nice_agent_set_remote_credentials;
  base_class->get_local_credentials = kms_ice_nice_agent_get_local_credentials;
  base_class->set_remote_description =
      kms_ice_nice_agent_set_remote_description;
  base_class->set_local_description = kms_ice_nice_agent_set_local_description;
  base_class->add_relay_server = kms_ice_nice_agent_add_relay_server;
  base_class->start_gathering_candidates =
      kms_ice_nice_agent_start_gathering_candidates;
  base_class->add_ice_candidate = kms_ice_nice_agent_add_ice_candidate;
  base_class->generate_local_candidate_sdp =
      kms_ice_nice_agent_generate_local_candidate_sdp;
  base_class->run_agent = kms_ice_nice_agent_run_agent;
  base_class->get_default_local_candidate =
      kms_ice_nice_agent_get_default_local_candidate;
  base_class->remove_stream = kms_ice_nice_agent_remove_stream;

  g_type_class_add_private (klass, sizeof (KmsIceNiceAgentPrivate));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
