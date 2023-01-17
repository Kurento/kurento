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

#include "kmsicebaseagent.h"
#include "kmsicecandidate.h"

#define GST_CAT_DEFAULT kmsicebaseagent
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmsicebaseagent"

G_DEFINE_TYPE (KmsIceBaseAgent, kms_ice_base_agent, G_TYPE_OBJECT);

enum
{
  SIGNAL_ON_ICE_CANDIDATE_,
  SIGNAL_ON_ICE_GATHERING_DONE_,
  SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED_,
  SIGNAL_NEW_SELECTED_PAIR_FULL_,
  LAST_SIGNAL_
};

static guint kms_ice_base_agent_signals[LAST_SIGNAL_] = { 0 };

static void
kms_ice_base_agent_finalize (GObject * object)
{
  KmsIceBaseAgent *self = KMS_ICE_BASE_AGENT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  /* chain up */
  G_OBJECT_CLASS (kms_ice_base_agent_parent_class)->finalize (object);
}

const gchar *
kms_ice_base_agent_state_to_string (IceState state)
{
  switch (state) {
    case ICE_STATE_READY:
      return "ready";
    case ICE_STATE_GATHERING:
      return "gathering";
    case ICE_STATE_CONNECTING:
      return "connecting";
    case ICE_STATE_CONNECTED:
      return "connected";
    case ICE_STATE_FAILED:
      return "failed";
    case ICE_STATE_DISCONNECTED:
      return "disconnected";
    default:
      return "";
  }
}

static void
kms_ice_base_agent_init (KmsIceBaseAgent * self)
{
}

static char *
kms_ice_base_agent_add_stream_default (KmsIceBaseAgent * self,
    const char *stream_id, guint16 min_port, guint16 max_port)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->add_stream == kms_ice_base_agent_add_stream_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'add_stream'", G_OBJECT_CLASS_NAME (klass));
  }

  return NULL;
}

static void
kms_ice_base_agent_remove_stream_default (KmsIceBaseAgent * self,
    const char *stream_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->remove_stream == kms_ice_base_agent_remove_stream_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'remove_stream'", G_OBJECT_CLASS_NAME (klass));
  }
}

static gboolean
kms_ice_base_agent_set_remote_credentials_default (KmsIceBaseAgent * self,
    const char *stream_id, const char *ufrag, const char *pwd)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->set_remote_credentials ==
      kms_ice_base_agent_set_remote_credentials_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'set_remote_credentials'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return FALSE;
}

static void
kms_ice_base_agent_get_local_credentials_default (KmsIceBaseAgent * self,
    const char *stream_id, gchar ** ufrag, gchar ** pwd)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_local_credentials ==
      kms_ice_base_agent_get_local_credentials_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_local_credentials'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_ice_base_agent_set_remote_description_default (KmsIceBaseAgent * self,
    const char *remote_description)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->set_remote_description ==
      kms_ice_base_agent_set_remote_description_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'set_remote_description'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_ice_base_agent_set_local_description_default (KmsIceBaseAgent * self,
    const char *local_description)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->set_local_description ==
      kms_ice_base_agent_set_local_description_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'set_local_description'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_ice_base_agent_add_relay_server_default (KmsIceBaseAgent * self,
    KmsIceRelayServerInfo server_info)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->add_relay_server == kms_ice_base_agent_add_relay_server_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'add_relay_server'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static gboolean
kms_ice_base_agent_start_gathering_candidates_default (KmsIceBaseAgent * self,
    const char *stream_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->start_gathering_candidates ==
      kms_ice_base_agent_start_gathering_candidates_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'start_gathering_candidates'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return FALSE;
}

static gboolean
kms_ice_base_agent_add_ice_candidate_default (KmsIceBaseAgent * self,
    KmsIceCandidate * candidate, const char *stream_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->add_ice_candidate == kms_ice_base_agent_add_ice_candidate_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'add_ice_candidate'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return FALSE;
}

static KmsIceCandidate *
kms_ice_base_agent_get_default_local_candidate_default (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_default_local_candidate ==
      kms_ice_base_agent_get_default_local_candidate_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_default_local_candidate'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return NULL;
}

static GSList *
kms_ice_base_agent_get_local_candidates_default (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_local_candidates ==
      kms_ice_base_agent_get_local_candidates_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_local_candidates'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return NULL;
}

static GSList *
kms_ice_base_agent_get_remote_candidates_default (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_remote_candidates ==
      kms_ice_base_agent_get_remote_candidates_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_remote_candidates'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return NULL;
}

static IceState
kms_ice_base_agent_get_component_state_default (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_component_state ==
      kms_ice_base_agent_get_component_state_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_component_state'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return ICE_STATE_DISCONNECTED;
}

static gboolean
kms_ice_base_agent_get_controlling_mode_default (KmsIceBaseAgent * self)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_controlling_mode ==
      kms_ice_base_agent_get_controlling_mode_default) {
    GST_WARNING_OBJECT (self, "%s does not reimplement 'get_controlling_mode'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return FALSE;
}

static void
kms_ice_base_agent_run_agent_default (KmsIceBaseAgent * self)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->run_agent == kms_ice_base_agent_run_agent_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'run_agent'", G_OBJECT_CLASS_NAME (klass));
  }
}

char *
kms_ice_base_agent_add_stream (KmsIceBaseAgent * self, const char *stream_id,
    guint16 min_port, guint16 max_port)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->add_stream (self, stream_id, min_port, max_port);
}

void
kms_ice_base_agent_remove_stream (KmsIceBaseAgent * self, const char *stream_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->remove_stream (self, stream_id);
}

gboolean
kms_ice_base_agent_set_remote_credentials (KmsIceBaseAgent * self,
    const char *stream_id, const char *ufrag, const char *pwd)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->set_remote_credentials (self, stream_id, ufrag, pwd);
}

void
kms_ice_base_agent_get_local_credentials (KmsIceBaseAgent * self,
    const char *stream_id, gchar ** ufrag, gchar ** pwd)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_local_credentials (self, stream_id, ufrag, pwd);
}

void
kms_ice_base_agent_set_remote_description (KmsIceBaseAgent * self,
    const char *remote_description)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->set_remote_description (self, remote_description);
}

void
kms_ice_base_agent_set_local_description (KmsIceBaseAgent * self,
    const char *local_description)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->set_local_description (self, local_description);
}

void
kms_ice_base_agent_add_relay_server (KmsIceBaseAgent * self,
    KmsIceRelayServerInfo server_info)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->add_relay_server (self, server_info);
}

gboolean
kms_ice_base_agent_start_gathering_candidates (KmsIceBaseAgent * self,
    const char *stream_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->start_gathering_candidates (self, stream_id);
}

gboolean
kms_ice_base_agent_add_ice_candidate (KmsIceBaseAgent * self,
    KmsIceCandidate * candidate, const char *stream_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->add_ice_candidate (self, candidate, stream_id);
}

KmsIceCandidate *
kms_ice_base_agent_get_default_local_candidate (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_default_local_candidate (self, stream_id, component_id);
}

GSList *
kms_ice_base_agent_get_local_candidates (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_local_candidates (self, stream_id, component_id);
}

GSList *
kms_ice_base_agent_get_remote_candidates (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_remote_candidates (self, stream_id, component_id);
}

IceState
kms_ice_base_agent_get_component_state (KmsIceBaseAgent * self,
    const char *stream_id, guint component_id)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_component_state (self, stream_id, component_id);
}

gboolean
kms_ice_base_agent_get_controlling_mode (KmsIceBaseAgent * self)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_controlling_mode (self);
}

void
kms_ice_base_agent_run_agent (KmsIceBaseAgent * self)
{
  KmsIceBaseAgentClass *klass =
      KMS_ICE_BASE_AGENT_CLASS (G_OBJECT_GET_CLASS (self));

  klass->run_agent (self);
}

static void
kms_ice_base_agent_class_init (KmsIceBaseAgentClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_ice_base_agent_finalize;

  klass->add_stream = kms_ice_base_agent_add_stream_default;
  klass->remove_stream = kms_ice_base_agent_remove_stream_default;
  klass->set_remote_credentials =
      kms_ice_base_agent_set_remote_credentials_default;
  klass->get_local_credentials =
      kms_ice_base_agent_get_local_credentials_default;
  klass->set_remote_description =
      kms_ice_base_agent_set_remote_description_default;
  klass->set_local_description =
      kms_ice_base_agent_set_local_description_default;
  klass->add_relay_server = kms_ice_base_agent_add_relay_server_default;
  klass->start_gathering_candidates =
      kms_ice_base_agent_start_gathering_candidates_default;
  klass->add_ice_candidate = kms_ice_base_agent_add_ice_candidate_default;
  klass->get_default_local_candidate =
      kms_ice_base_agent_get_default_local_candidate_default;
  klass->get_local_candidates = kms_ice_base_agent_get_local_candidates_default;
  klass->get_remote_candidates =
      kms_ice_base_agent_get_remote_candidates_default;
  klass->get_component_state = kms_ice_base_agent_get_component_state_default;
  klass->get_controlling_mode = kms_ice_base_agent_get_controlling_mode_default;
  klass->run_agent = kms_ice_base_agent_run_agent_default;

  /**
  * KmsIceBaseAgent::on-ice-candidate:
  * @self: the object which received the signal
  * @candidate: the local candidate gathered
  *
  * Notify of a new gathered local candidate for a #KmsIceBaseAgent.
  */
  kms_ice_base_agent_signals[SIGNAL_ON_ICE_CANDIDATE_] =
      g_signal_new ("on-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsIceBaseAgentClass, on_ice_candidate), NULL,
      NULL, g_cclosure_marshal_VOID__OBJECT, G_TYPE_NONE, 1,
      KMS_TYPE_ICE_CANDIDATE);

  /**
  * KmsIceBaseAgent::on-ice-gathering-done:
  * @self: the object which received the signal
  *
  * Notify that all candidates have been gathered for a #KmsIceBaseAgent
  */
  kms_ice_base_agent_signals[SIGNAL_ON_ICE_GATHERING_DONE_] =
      g_signal_new ("on-ice-gathering-done",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsIceBaseAgentClass, on_ice_gathering_done), NULL,
      NULL, g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 1, G_TYPE_STRING);

  /**
   * KmsIceBaseAgent::on-ice-component-state-changed:
   * @self: the object which received the signal
   * @stream_id: The ID of the stream
   * @component_id: The ID of the component
   * @state: The #IceState of the component
   *
   * This signal is fired whenever a component's state changes
   */
  kms_ice_base_agent_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED_] =
      g_signal_new ("on-ice-component-state-changed",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 3, G_TYPE_STRING, G_TYPE_UINT, G_TYPE_UINT, G_TYPE_INVALID);

  kms_ice_base_agent_signals[SIGNAL_NEW_SELECTED_PAIR_FULL_] =
      g_signal_new ("new-selected-pair-full",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 4, G_TYPE_STRING, G_TYPE_UINT, KMS_TYPE_ICE_CANDIDATE,
      KMS_TYPE_ICE_CANDIDATE);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
