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

static gboolean
kms_ice_nice_agent_add_ice_candidate (KmsIceBaseAgent * self,
    KmsIceCandidate * candidate, const char *stream_id);

struct _KmsIceNiceAgentPrivate
{
  GMainContext *context;
  NiceAgent *agent;
  GSList *remote_candidates;
};

static char *
kms_ice_nice_agent_get_candidate_sdp_string (NiceAgent * agent,
    NiceCandidate * candidate)
{
  gchar *str = nice_agent_generate_local_candidate_sdp (agent, candidate);
  gchar *cand = g_strconcat (SDP_CANDIDATE_ATTR, ":",
      (str + SDP_CANDIDATE_ATTR_LEN), NULL);

  g_free (str);

  return cand;
}

static KmsIceCandidate *
kms_ice_nice_agent_create_candidate_from_nice (NiceAgent * nice_agent,
    NiceCandidate * nice_cand, const char *stream_id)
{
  gchar *cand_str = kms_ice_nice_agent_get_candidate_sdp_string (nice_agent,
      nice_cand);
  KmsIceCandidate *candidate =
      kms_ice_candidate_new (cand_str, "", 0, stream_id);

  g_free (cand_str);

  return candidate;
}

static void
kms_ice_nice_agent_new_candidate_full (NiceAgent * agent,
    NiceCandidate * candidate, KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  const guint stream_id = candidate->stream_id;
  const guint component_id = candidate->component_id;

  gchar *stream_id_str = g_strdup_printf ("%u", stream_id);
  KmsIceCandidate *kms_candidate =
      kms_ice_nice_agent_create_candidate_from_nice (agent, candidate,
      stream_id_str);
  g_free (stream_id_str);

  GST_LOG_OBJECT (self,
      "[IceCandidateFound] local: '%s', stream_id: %u, component_id: %u",
      kms_ice_candidate_get_candidate (kms_candidate),
      stream_id, component_id);

  g_signal_emit_by_name (parent, "on-ice-candidate", kms_candidate);
  g_object_unref (kms_candidate);
}

static void
kms_ice_nice_agent_new_remote_candidate_full (NiceAgent * agent,
    NiceCandidate * candidate, KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  const guint stream_id = candidate->stream_id;

  gchar *stream_id_str = g_strdup_printf ("%u", stream_id);

  KmsIceCandidate *kms_candidate =
      kms_ice_nice_agent_create_candidate_from_nice (agent, candidate,
      stream_id_str);

  GST_DEBUG_OBJECT (self,
      "[AddIceCandidate] Found peer-reflexive remote: '%s'",
      kms_ice_candidate_get_candidate (kms_candidate));

  kms_ice_nice_agent_add_ice_candidate (parent, kms_candidate, stream_id_str);

  g_free (stream_id_str);
  g_object_unref (kms_candidate);
}

static void
kms_ice_nice_agent_gathering_done (NiceAgent * agent, guint stream_id,
    KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  char buff[33];
  char *ret;

  GST_LOG_OBJECT (self, "[IceGatheringDone] stream_id: %u", stream_id);

  //convert id to char*
  g_snprintf (buff, 32, "%u", stream_id);

  ret = g_strdup (buff);

  g_signal_emit_by_name (parent, "on-ice-gathering-done", ret);

  g_free (ret);
}

static IceState
kms_ice_nice_agent_nice_to_ice_state (NiceComponentState state)
{
  switch (state) {
    case NICE_COMPONENT_STATE_DISCONNECTED:
      return ICE_STATE_DISCONNECTED;
    case NICE_COMPONENT_STATE_GATHERING:
      return ICE_STATE_GATHERING;
    case NICE_COMPONENT_STATE_CONNECTING:
      return ICE_STATE_CONNECTING;
    case NICE_COMPONENT_STATE_CONNECTED:
      return ICE_STATE_CONNECTED;
    case NICE_COMPONENT_STATE_READY:
      return ICE_STATE_READY;
    case NICE_COMPONENT_STATE_FAILED:
      return ICE_STATE_FAILED;
    default:
      return ICE_STATE_FAILED;
  }
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
  g_snprintf (buff, 32, "%u", stream_id);

  ret = g_strdup (buff);
  state_ = kms_ice_nice_agent_nice_to_ice_state (state);

  GST_LOG_OBJECT (self,
      "[IceComponentStateChanged] state: %s, stream_id: %u, component_id: %u",
      nice_component_state_to_string (state), stream_id, component_id);

  g_signal_emit_by_name (parent, "on-ice-component-state-changed", ret,
      component_id, state_);
  g_free (ret);
}

void
kms_ice_nice_agent_new_selected_pair_full (NiceAgent * agent,
    guint stream_id,
    guint component_id,
    NiceCandidate * lcandidate,
    NiceCandidate * rcandidate, KmsIceNiceAgent * self)
{
  KmsIceBaseAgent *parent = KMS_ICE_BASE_AGENT (self);
  gchar *stream_id_str;
  KmsIceCandidate *local_candidate = NULL, *remote_candidate = NULL;

  stream_id_str = g_strdup_printf ("%u", stream_id);

  local_candidate = kms_ice_nice_agent_create_candidate_from_nice (agent,
      lcandidate, stream_id_str);
  if (!local_candidate) {
    gchar *cand_str =
        kms_ice_nice_agent_get_candidate_sdp_string (agent, lcandidate);
    GST_WARNING_OBJECT (self,
        "Invalid local candidate: '%s', stream_id: %u, component_id: %u",
        cand_str, stream_id, component_id);
    g_free (cand_str);
    goto end;
  }

  remote_candidate = kms_ice_nice_agent_create_candidate_from_nice (agent,
      rcandidate, stream_id_str);
  if (!remote_candidate) {
    gchar *cand_str =
        kms_ice_nice_agent_get_candidate_sdp_string (agent, rcandidate);
    GST_WARNING_OBJECT (self,
        "Invalid remote candidate: '%s', stream_id: %u, component_id: %u",
        cand_str, stream_id, component_id);
    g_free (cand_str);
    goto end;
  }

  GST_LOG_OBJECT (self,
      "[NewCandidatePairSelected] local: '%s', remote: '%s'"
      ", stream_id: %u, component_id: %u",
      kms_ice_candidate_get_candidate (local_candidate),
      kms_ice_candidate_get_candidate (remote_candidate),
      stream_id, component_id);

  g_signal_emit_by_name (parent, "new-selected-pair-full", stream_id_str,
      component_id, local_candidate, remote_candidate);

end:
  g_free (stream_id_str);
  if (local_candidate) { g_object_unref (local_candidate); }
  if (remote_candidate) { g_object_unref (remote_candidate); }
}

KmsIceNiceAgent *
kms_ice_nice_agent_new (GMainContext * context)
{
  GObject *obj;
  KmsIceNiceAgent *self;

  obj = g_object_new (KMS_TYPE_ICE_NICE_AGENT, NULL);
  self = KMS_ICE_NICE_AGENT (obj);
  self->priv->context = context;

  GST_DEBUG_OBJECT (self, "Create new instance, compatibility level: RFC5245");
  self->priv->agent =
      nice_agent_new (self->priv->context, NICE_COMPATIBILITY_RFC5245);

  GST_DEBUG_OBJECT (self, "Disable UPNP support");
  g_object_set (self->priv->agent, "upnp", FALSE, NULL);

  g_signal_connect (self->priv->agent, "new-candidate-full",
      G_CALLBACK (kms_ice_nice_agent_new_candidate_full), self);
  g_signal_connect (self->priv->agent, "new-remote-candidate-full",
      G_CALLBACK (kms_ice_nice_agent_new_remote_candidate_full), self);
  g_signal_connect (self->priv->agent, "candidate-gathering-done",
      G_CALLBACK (kms_ice_nice_agent_gathering_done), self);
  g_signal_connect (self->priv->agent, "component-state-changed",
      G_CALLBACK (kms_ice_nice_agent_component_state_change), self);
  g_signal_connect (self->priv->agent, "new-selected-pair-full",
      G_CALLBACK (kms_ice_nice_agent_new_selected_pair_full), self);

  return self;
}

static void
kms_ice_nice_agent_finalize (GObject * object)
{
  KmsIceNiceAgent *self = KMS_ICE_NICE_AGENT (object);

  GST_LOG_OBJECT (self, "finalize");

  // nice_agent_remove_stream(), called from kms_ice_nice_agent_remove_stream(),
  // is an asynchronous function. Run a last iteration of its GMainLoop context
  // in order to allow it run and release all resources and object references.
  g_main_context_wakeup (self->priv->context);
  g_main_context_iteration (self->priv->context, FALSE);

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

// TODO Ask in libnice mail lists if attaching a callback function is really needed
//static void
//kms_ice_nice_agent_recv_cb (NiceAgent *agent, guint stream_id,
//    guint component_id, guint len, gchar *buf, gpointer user_data)
//{
////  ((void)0); // Nothing to do, noop
//  KmsIceBaseAgent *self = user_data;
//  GST_DEBUG_OBJECT (self, "Callback data received");
//}

static char *
kms_ice_nice_agent_add_stream (KmsIceBaseAgent * self, const char *stream_id,
    guint16 min_port, guint16 max_port)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  int i;
  guint id;

  id = nice_agent_add_stream (nice_agent->priv->agent, KMS_NICE_N_COMPONENTS);

  if (id == 0) {
    GST_ERROR_OBJECT (self, "Cannot add data stream, stream_id: %s", stream_id);
    return NULL;
  }

  GST_LOG_OBJECT (self, "Added data stream, ID: %u, stream_id: %s",
      id, stream_id);

  GST_LOG_OBJECT (self, "Set port range: [%u, %u]", min_port, max_port);
  for (i = 1; i <= KMS_NICE_N_COMPONENTS; i++) {
    nice_agent_set_port_range (nice_agent->priv->agent, id, i, min_port,
        max_port);
  }

// TODO Ask in libnice mail lists if attaching a callback function is really needed
//  GST_DEBUG_OBJECT (self, "Attach recv callback to mainloop");
//  for (i = 1; i <= KMS_NICE_N_COMPONENTS; i++) {
//    nice_agent_attach_recv (nice_agent->priv->agent, id, i,
//        nice_agent->priv->context, kms_ice_nice_agent_recv_cb, self);
//  }

  return g_strdup_printf ("%u", id);
}

static void
kms_ice_nice_agent_remove_stream (KmsIceBaseAgent * self, const char *stream_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  GST_LOG_OBJECT (self, "Remove data stream, stream_id: %u", id);

  nice_agent_remove_stream (nice_agent->priv->agent, id);
}

static gboolean
kms_ice_nice_agent_set_remote_credentials (KmsIceBaseAgent * self,
    const char *stream_id, const char *ufrag, const char *pwd)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  GST_LOG_OBJECT (self, "Set remote credentials, stream_id: %u", id);

  return nice_agent_set_remote_credentials (nice_agent->priv->agent,
      id, ufrag, pwd);
}

static void
kms_ice_nice_agent_get_local_credentials (KmsIceBaseAgent * self,
    const char *stream_id, gchar ** ufrag, gchar ** pwd)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);

  GST_LOG_OBJECT (self, "Get local credentials, stream_id: %u", id);

  nice_agent_get_local_credentials (nice_agent->priv->agent, id, ufrag, pwd);
}

static void
kms_ice_nice_agent_set_remote_description (KmsIceBaseAgent * self,
    const char *remote_description)
{
  GST_TRACE_OBJECT (self, "Nothing to do in set_remote_description");
}

static void
kms_ice_nice_agent_set_local_description (KmsIceBaseAgent * self,
    const char *local_description)
{
  GST_TRACE_OBJECT (self, "Nothing to do in set_local_description");
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
      GST_WARNING ("Wrong type of relay transport. Using TCP");
      return NICE_RELAY_TYPE_TURN_TCP;
  }
}

static gchar *
from_turn_protocol_to_string (TurnProtocol transport)
{
  switch (transport) {
    default:
      GST_WARNING ("Wrong type of relay transport. Using TCP");
    case TURN_PROTOCOL_TCP:
      return "tcp";
    case TURN_PROTOCOL_UDP:
      return "udp";
    case TURN_PROTOCOL_TLS:
    case TURN_PROTOCOL_SSLTCP:
      return "tls";
  }
}

static void
kms_ice_nice_agent_add_relay_server (KmsIceBaseAgent * self,
    KmsIceRelayServerInfo server_info)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (server_info.stream_id);
  NiceRelayType type = from_turn_protocol_to_nice_relay (server_info.type);

  GST_DEBUG_OBJECT (self, "Add relay server,"
      " IP: %s, port: %u, type: %s, stream_id: %u",
      server_info.server_ip, server_info.server_port,
      from_turn_protocol_to_string (server_info.type), id);

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

  gboolean ok = nice_agent_gather_candidates (nice_agent->priv->agent, id);

  if (ok) {
    GST_LOG_OBJECT (self, "[IceGatheringStarted] stream_id: %s", stream_id);
  }

  return ok;
}

static gboolean
kms_ice_nice_agent_add_ice_candidate (KmsIceBaseAgent * self,
    KmsIceCandidate * candidate, const char *stream_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  NiceCandidate *nice_cand;
  guint id = (guint) atoi (stream_id);
  gboolean ret;
  GSList *candidates;
  gchar *candidate_str;

  candidate_str =
      g_strdup_printf ("a=%s", kms_ice_candidate_get_candidate (candidate));
  nice_cand =
      nice_agent_parse_remote_candidate_sdp (nice_agent->priv->agent, id,
      candidate_str);
  g_free (candidate_str);

  if (nice_cand == NULL) {
    GST_WARNING_OBJECT (self,
        "[AddIceCandidate] Error in libnice parsing, remote: '%s'",
        kms_ice_candidate_get_candidate (candidate));
    return FALSE;
  }

  // libnice docs say: "You must first call nice_agent_gather_candidates()
  // before calling nice_agent_set_remote_candidates()".
  // This is enforced one level up, by KmsWebrtcSession.

  nice_cand->stream_id = id;
  candidates = g_slist_append (NULL, nice_cand);

  GST_LOG_OBJECT (self,
      "[AddIceCandidate] remote: '%s', stream_id: %u, component_id: %u",
      kms_ice_candidate_get_candidate (candidate),
      nice_cand->stream_id, nice_cand->component_id);

  if (nice_agent_set_remote_candidates (nice_agent->priv->agent,
          nice_cand->stream_id, nice_cand->component_id, candidates) < 0) {
    GST_WARNING_OBJECT (self,
        "[AddIceCandidate] Error in libnice, adding remote: '%s'",
        kms_ice_candidate_get_candidate (candidate));
    ret = FALSE;
  } else {
    ret = TRUE;
  }

  g_slist_free (candidates);
  nice_candidate_free (nice_cand);

  return ret;
}

static KmsIceCandidate *
kms_ice_nice_agent_get_default_local_candidate (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  NiceCandidate *nice_cand;
  guint id = atoi (stream_id);
  KmsIceCandidate *ret;

  nice_cand =
      nice_agent_get_default_local_candidate (nice_agent->priv->agent, id,
      component_id);
  ret =
      kms_ice_nice_agent_create_candidate_from_nice (nice_agent->priv->agent,
      nice_cand, stream_id);
  nice_candidate_free (nice_cand);

  return ret;
}

static GSList *
kms_ice_nice_agent_get_local_candidates (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  GSList *ret = NULL;
  guint id = atoi (stream_id);
  GSList *candidates;
  GSList *walk;

  candidates =
      nice_agent_get_local_candidates (nice_agent->priv->agent, id,
      component_id);

  for (walk = candidates; walk; walk = walk->next) {
    NiceCandidate *nice_cand = walk->data;
    KmsIceCandidate *candidate =
        kms_ice_nice_agent_create_candidate_from_nice (nice_agent->priv->agent,
        nice_cand,
        stream_id);

    if (candidate) {
      ret = g_slist_append (ret, candidate);
    }
  }

  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);

  return ret;
}

static GSList *
kms_ice_nice_agent_get_remote_candidates (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  GSList *ret = NULL;
  guint id = atoi (stream_id);
  GSList *candidates;
  GSList *walk;

  candidates =
      nice_agent_get_remote_candidates (nice_agent->priv->agent, id,
      component_id);

  for (walk = candidates; walk; walk = walk->next) {
    NiceCandidate *nice_cand = walk->data;
    KmsIceCandidate *candidate =
        kms_ice_nice_agent_create_candidate_from_nice (nice_agent->priv->agent,
        nice_cand,
        stream_id);

    if (candidate) {
      ret = g_slist_append (ret, candidate);
    }
  }

  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);

  return ret;
}

static IceState
kms_ice_nice_agent_get_component_state (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  guint id = atoi (stream_id);
  NiceComponentState state;

  state = nice_agent_get_component_state (nice_agent->priv->agent, id,
      component_id);

  return kms_ice_nice_agent_nice_to_ice_state (state);
}

static gboolean
kms_ice_nice_agent_get_controlling_mode (KmsIceBaseAgent * self)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self);
  gboolean controller;

  g_object_get (nice_agent->priv->agent, "controlling-mode", &controller, NULL);

  return controller;
}

static void
kms_ice_nice_agent_run_agent (KmsIceBaseAgent * self)
{
  GST_TRACE_OBJECT (self, "Nothing to do in run_agent");
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
  base_class->run_agent = kms_ice_nice_agent_run_agent;
  base_class->get_default_local_candidate =
      kms_ice_nice_agent_get_default_local_candidate;
  base_class->get_local_candidates = kms_ice_nice_agent_get_local_candidates;
  base_class->get_remote_candidates = kms_ice_nice_agent_get_remote_candidates;
  base_class->get_component_state = kms_ice_nice_agent_get_component_state;
  base_class->get_controlling_mode = kms_ice_nice_agent_get_controlling_mode;
  base_class->remove_stream = kms_ice_nice_agent_remove_stream;

  g_type_class_add_private (klass, sizeof (KmsIceNiceAgentPrivate));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
