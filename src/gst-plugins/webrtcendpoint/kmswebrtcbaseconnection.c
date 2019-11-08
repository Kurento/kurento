/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include "kmswebrtcbaseconnection.h"
#include <commons/kmsstats.h>
#include "kmsiceniceagent.h"

#include <string.h> // strlen()

#define GST_CAT_DEFAULT kmswebrtcbaseconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtcbaseconnection"

G_DEFINE_TYPE (KmsWebRtcBaseConnection, kms_webrtc_base_connection,
    G_TYPE_OBJECT);

enum
{
  PROP_0,
  PROP_ICE_AGENT,
  PROP_STREAM_ID
};

gboolean
kms_webrtc_base_connection_configure (KmsWebRtcBaseConnection * self,
    KmsIceBaseAgent * agent, const gchar * name)
{
  self->agent = g_object_ref (agent);
  self->name = g_strdup (name);

  self->stream_id =
      kms_ice_base_agent_add_stream (agent, self->name, self->min_port,
      self->max_port);

  if (g_strcmp0 (self->stream_id, "0") == 0) {
    GST_ERROR_OBJECT (self, "Cannot add stream for %s.", name);
    return FALSE;
  }

  return TRUE;
}

static gchar *kms_webrtc_base_connection_get_certificate_pem_default
    (KmsWebRtcBaseConnection * self)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_certificate_pem ==
      kms_webrtc_base_connection_get_certificate_pem_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_certificate_pem'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return NULL;
}

static void
kms_webrtc_base_connection_finalize (GObject * object)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (object);

  GST_DEBUG_OBJECT (self, "finalize");

  kms_ice_base_agent_remove_stream (self->agent, self->stream_id);
  g_free (self->name);
  g_free (self->stream_id);
  g_clear_object (&self->agent);
  g_rec_mutex_clear (&self->mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_base_connection_parent_class)->finalize (object);
}

static void
kms_webrtc_base_connection_init (KmsWebRtcBaseConnection * self)
{
  g_rec_mutex_init (&self->mutex);
  self->stats_enabled = FALSE;
}

static void
kms_webrtc_base_connection_set_latency_callback_default (KmsIRtpConnection *
    obj, BufferLatencyCallback cb, gpointer user_data)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (obj);

  self->cb = cb;
  self->user_data = user_data;
}

static void
kms_webrtc_base_connection_collect_latency_stats_default (KmsIRtpConnection *
    obj, gboolean enable)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (obj);

  self->stats_enabled = enable;
}

static void
kms_webrtc_base_connection_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (object);

  KMS_WEBRTC_BASE_CONNECTION_LOCK (self);

  switch (prop_id) {
    case PROP_ICE_AGENT:
      g_value_set_object (value, self->agent);
      break;
    case PROP_STREAM_ID:
      g_value_set_string (value, self->stream_id);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_WEBRTC_BASE_CONNECTION_UNLOCK (self);
}

static void
kms_webrtc_base_connection_class_init (KmsWebRtcBaseConnectionClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_base_connection_finalize;
  gobject_class->get_property = kms_webrtc_base_connection_get_property;

  klass->get_certificate_pem =
      kms_webrtc_base_connection_get_certificate_pem_default;

  klass->set_latency_callback =
      kms_webrtc_base_connection_set_latency_callback_default;
  klass->collect_latency_stats =
      kms_webrtc_base_connection_collect_latency_stats_default;

  g_object_class_install_property (gobject_class, PROP_ICE_AGENT,
      g_param_spec_object ("ice-agent", "Ice agent",
          "The Ice agent.", KMS_TYPE_ICE_BASE_AGENT,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_STREAM_ID,
      g_param_spec_string ("stream-id", "Stream identifier",
          "The stream identifier.", NULL,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}

gchar *
kms_webrtc_base_connection_get_certificate_pem (KmsWebRtcBaseConnection * self)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_certificate_pem (self);
}

/**
 * Split comma-separated string containing IP addresses.
 *
 * @param external_ips Comma-separated string with IP addresses.
 * @return List of strings, each one containing one IP address.
 */
static GSList *
kms_webrtc_base_connection_split_ips (const char * ips_str)
{
  if (ips_str == NULL) {
    return NULL;
  }

  // ips_str == "192.168.1.2, 127.0.0.1,172.17.0.1"

  gchar **ips_arr = g_strsplit_set (ips_str, " ,", -1);

  // ips_arr[0] == "192.168.1.2"
  // ips_arr[1] == ""
  // ips_arr[2] == "127.0.0.1"
  // ips_arr[3] == "172.17.0.1"
  // ips_arr[4] == NULL

  GSList *ips_list = NULL;

  for (int i = 0; ips_arr[i] != NULL; ++i) {
    if (strlen (ips_arr[i]) == 0) {
      // ips_arr[i] == ""
      g_free (ips_arr[i]);
      continue;
    }

    ips_list = g_slist_append (ips_list, ips_arr[i]);
  }

  g_free (ips_arr);

  return ips_list;
}

/**
 * Adds new local IP address to NiceAgent instance.
 *
 * @param addr pointer to the new local IP address.
 * @param agent NiceAgent instance.
 */
static void
kms_webrtc_base_connection_agent_add_local_addr (const char * local_ip,
    NiceAgent * agent)
{
  NiceAddress *nice_address = nice_address_new ();

  nice_address_set_from_string (nice_address, local_ip);
  nice_agent_add_local_address (agent, nice_address);
  nice_address_free (nice_address);
}

void
kms_webrtc_base_connection_set_external_ips_info (KmsWebRtcBaseConnection *
    self, const gchar * external_ips)
{
  if (KMS_IS_ICE_NICE_AGENT (self->agent)) {
    KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self->agent);
    NiceAgent *agent = kms_ice_nice_agent_get_agent (nice_agent);

    GSList *local_ips = kms_webrtc_base_connection_split_ips (external_ips);

    if (local_ips != NULL) {
      g_slist_foreach (local_ips,
          (GFunc) kms_webrtc_base_connection_agent_add_local_addr,
          agent);
    }

    g_slist_free_full (local_ips, g_free);
  }
}

void
kms_webrtc_base_connection_set_stun_server_info (KmsWebRtcBaseConnection * self,
    const gchar * ip, guint port)
{
  // TODO: This code should be independent of the type of ice agent
  if (KMS_IS_ICE_NICE_AGENT (self->agent)) {
    KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self->agent);

    g_object_set (kms_ice_nice_agent_get_agent (nice_agent),
        "stun-server", ip, "stun-server-port", port, NULL);
  }
}

void
kms_webrtc_base_connection_set_relay_info (KmsWebRtcBaseConnection * self,
    const gchar * server_ip,
    guint server_port,
    const gchar * username, const gchar * password, TurnProtocol type)
{
  KmsIceRelayServerInfo info;

  info.server_ip = server_ip;
  info.server_port = server_port;
  info.username = username;
  info.password = password;
  info.type = type;
  info.stream_id = self->stream_id;

  kms_ice_base_agent_add_relay_server (self->agent, info);
}

void
kms_webrtc_base_connection_set_latency_callback (KmsIRtpConnection * self,
    BufferLatencyCallback cb, gpointer user_data)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_latency_callback (self, cb, user_data);
}

void
kms_webrtc_base_connection_collect_latency_stats (KmsIRtpConnection * self,
    gboolean enable)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->collect_latency_stats (self, enable);
}
