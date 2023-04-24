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

enum
{
  SIGNAL_ON_DTLS_CONNECTION_STATE_CHANGED,
  LAST_SIGNAL
};

static guint kms_webrtc_base_connection_signals[LAST_SIGNAL] = { 0 };


typedef struct _DtlsConnectionSourceData
{
  KmsRefStruct ref;
  KmsWebRtcBaseConnection *self;
  gchar *dtls_component;
} DtlsConnectionSourceData;


gboolean
kms_webrtc_base_connection_configure (KmsWebRtcBaseConnection * self,
    KmsIceBaseAgent * agent, const gchar * name)
{
  self->agent = g_object_ref (agent);
  self->name = g_strdup (name);

  self->stream_id =
      kms_ice_base_agent_add_stream (agent, self->name, self->min_port,
      self->max_port);

  if (self->stream_id == NULL) {
    GST_ERROR_OBJECT (self, "Cannot add stream for %s.", self->name);
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

   /**
   * KmsWebrtcBaseConnection::on-dtls-connection-state-changed
   * @self: the object which received the signal
   * @stream_id: The ID of the stream
   * @dtls_object: The dtls component raising the signal
   * @connection_id: GStreamer connection Id
   * @state: The #DtlsConnectionState of the component
   *
   * This signal is fired whenever a component's DTLS connection state changes
   */
  kms_webrtc_base_connection_signals[SIGNAL_ON_DTLS_CONNECTION_STATE_CHANGED] =
      g_signal_new ("on-dtls-connection-state-changed",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 4, G_TYPE_STRING, G_TYPE_STRING, G_TYPE_STRING, G_TYPE_UINT,
      G_TYPE_INVALID);


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
 * Split comma-separated string.
 */
static GSList *
kms_webrtc_base_connection_split_comma (const gchar * str)
{
  if (str == NULL) {
    return NULL;
  }

  // str == "A, B,C"

  gchar **arr = g_strsplit_set (str, " ,", -1);

  // arr[0] == "A"
  // arr[1] == ""
  // arr[2] == "B"
  // arr[3] == "C"
  // arr[4] == NULL

  GSList *list = NULL;

  for (int i = 0; arr[i] != NULL; ++i) {
    if (strlen (arr[i]) == 0) {
      // arr[i] == ""
      g_free (arr[i]);
      continue;
    }

    list = g_slist_append (list, arr[i]);
  }

  g_free (arr);

  return list;
}

/**
 * Add new local IP address to NiceAgent instance.
 */
static void
kms_webrtc_base_connection_agent_add_net_addr (const gchar * net_name,
    NiceAgent * agent)
{
  NiceAddress *nice_address = nice_address_new ();
  gchar *ip_address = nice_interfaces_get_ip_for_interface ((gchar *)net_name);

  nice_address_set_from_string (nice_address, ip_address);
  nice_agent_add_local_address (agent, nice_address);

  GST_INFO_OBJECT (agent, "Added local address: %s", ip_address);

  nice_address_free (nice_address);
  g_free (ip_address);
}

void
kms_webrtc_base_connection_set_network_ifs_info (KmsWebRtcBaseConnection *
    self, const gchar * net_names)
{
  if (KMS_IS_ICE_NICE_AGENT (self->agent)) {
    KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self->agent);
    NiceAgent *agent = kms_ice_nice_agent_get_agent (nice_agent);

    GSList *net_list = kms_webrtc_base_connection_split_comma (
        net_names);

    if (net_list != NULL) {
      g_slist_foreach (net_list,
          (GFunc) kms_webrtc_base_connection_agent_add_net_addr,
          agent);
    }

    g_slist_free_full (net_list, g_free);
  }
}

void
kms_webrtc_base_connection_set_ice_tcp (KmsWebRtcBaseConnection *self,
    gboolean ice_tcp)
{
  if (KMS_IS_ICE_NICE_AGENT (self->agent)) {
    KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self->agent);
    g_object_set (
        kms_ice_nice_agent_get_agent (nice_agent), "ice-tcp", ice_tcp, NULL);
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


static void
dtls_component_connection_state_cb (GObject * object, GParamSpec * pspec,
    DtlsConnectionSourceData *data)
{
  KmsWebRtcBaseConnection *conn = data->self;
  GValue state_value = { 0 };
  GValue connection_id_value = { 0 };
  GType connection_state_type = g_type_from_name ("GstDtlsConnectionState");
  guint state;
  const gchar *conn_id;

	g_value_init (&state_value, connection_state_type);
	g_value_init (&connection_id_value, G_TYPE_STRING);

  g_object_get_property(object, "connection-state", &state_value);
  g_object_get_property(object, "connection-id", &connection_id_value);
  state = g_value_get_enum (&state_value);
  conn_id = g_value_get_string(&connection_id_value);
  g_signal_emit (G_OBJECT (conn),
        kms_webrtc_base_connection_signals[SIGNAL_ON_DTLS_CONNECTION_STATE_CHANGED], 0,
        conn->stream_id, data->dtls_component, conn_id, state);

  g_value_unset(&state_value);
  g_value_unset(&connection_id_value);
}

static void
dtls_connection_source_data_destroy (DtlsConnectionSourceData * data, GClosure * closure)
{
  g_free(data->dtls_component);

  g_slice_free (DtlsConnectionSourceData, data);
}


static DtlsConnectionSourceData *
dtls_connection_source_data_new (KmsWebRtcBaseConnection * self, const gchar *component, const gchar *direction)
{
  DtlsConnectionSourceData *data;

  data = g_slice_new0 (DtlsConnectionSourceData);


  kms_ref_struct_init (KMS_REF_STRUCT_CAST (data),
      (GDestroyNotify) dtls_connection_source_data_destroy);

  data->self = self;
  data->dtls_component = g_strconcat("[", component, "/", direction, "]", NULL);

  return data;
}


void kms_webrtc_base_connection_add_dtls_component (KmsWebRtcBaseConnection * self,
    GstElement *dtls_component, const gchar *component, const gchar *direction)
{
  DtlsConnectionSourceData* data = dtls_connection_source_data_new(self, component, direction);

  g_signal_connect_data (dtls_component, "notify::connection-state",
      G_CALLBACK (dtls_component_connection_state_cb),
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (data)),
      (GClosureNotify) kms_ref_struct_unref, 0);      
}
