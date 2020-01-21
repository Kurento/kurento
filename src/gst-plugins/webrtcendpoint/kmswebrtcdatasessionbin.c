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
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <stdio.h>
#include <commons/kmsstats.h>
#include <commons/kmsutils.h>

#include "kmswebrtcdatasessionbin.h"
#include "kmswebrtcdatachannelbin.h"
#include "kms-webrtc-data-marshal.h"
#include "kmswebrtcdatachannelstate.h"

#define PLUGIN_NAME "kmswebrtcdatasessionbin"

GST_DEBUG_CATEGORY_STATIC (kms_webrtc_data_session_bin_debug_category);
#define GST_CAT_DEFAULT kms_webrtc_data_session_bin_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcDataSessionBin, kms_webrtc_data_session_bin,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_webrtc_data_session_bin_debug_category,
        PLUGIN_NAME, 0, "debug category for webrtc_data_session_bin"));

#define parent_class kms_webrtc_data_session_bin_parent_class

#define DEFAULT_DTLS_CLIENT_MODE FALSE
#define DEFAULT_SCTP_LOCAL_PORT 0
#define DEFAULT_SCTP_REMOTE_PORT 0

#define SCTP_PORT_MIN 0
#define SCTP_PORT_MAX 65534

#define IS_EVEN(stream_id) (!((stream_id) & 0x01))

#define KMS_WEBRTC_DATA_SESSION_BIN_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                        \
    (obj),                                             \
    KMS_TYPE_WEBRTC_DATA_SESSION_BIN,                  \
    KmsWebRtcDataSessionBinPrivate                     \
  )                                                    \
 )

struct _KmsWebRtcDataSessionBinPrivate
{
  guint16 assoc_id;
  gboolean dtls_client_mode;
  gboolean session_established;
  guint16 local_sctp_port;
  guint16 remote_sctp_port;
  GRecMutex mutex;

  GstElement *sctpdec;
  GstElement *sctpenc;

  GHashTable *data_channels;
  GHashTable *channels;

  guint even_id;
  guint odd_id;

  GSList *pending;

  GThreadPool *pool;

  guint opened;
  guint closed;
};

#define KMS_WEBRTC_DATA_SESSION_BIN_LOCK(obj) \
  (g_rec_mutex_lock (&KMS_WEBRTC_DATA_SESSION_BIN_CAST ((obj))->priv->mutex))
#define KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK(obj) \
  (g_rec_mutex_unlock (&KMS_WEBRTC_DATA_SESSION_BIN_CAST ((obj))->priv->mutex))

enum
{
  PROP_0,

  PROP_DTLS_CLIENT_MODE,
  PROP_SCTP_LOCAL_PORT,
  PROP_SCTP_REMOTE_PORT,

  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

enum
{
  DATA_CHANNEL_OPENED,
  DATA_CHANNEL_CLOSED,
  DATA_SESSION_ESTABLISHED,

  GET_DATA_CHANNEL_ACTION,
  CREATE_DATA_CHANNEL_ACTION,
  DESTROY_DATA_CHANNEL_ACTION,
  STATS_ACTION,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-sctp"));

static GstStaticPadTemplate src_template = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-sctp"));

static gint
kms_webrtc_data_session_bin_create_data_channel_action (KmsWebRtcDataSessionBin
    *, gboolean, gint, gint, const gchar *, const gchar *);

static guint
get_sctp_association_id ()
{
  static guint assoc_id = 0;

  return g_atomic_int_add (&assoc_id, 1);
}

static gchar *
get_decoder_name (guint assoc_id)
{
  return g_strdup_printf ("sctpdec_%u", assoc_id);
}

static gchar *
get_encoder_name (guint assoc_id)
{
  return g_strdup_printf ("sctpenc_%u", assoc_id);
}

static void
kms_webrtc_data_session_bin_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebRtcDataSessionBin *self = KMS_WEBRTC_DATA_SESSION_BIN (object);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  switch (property_id) {
    case PROP_DTLS_CLIENT_MODE:
      self->priv->dtls_client_mode = g_value_get_boolean (value);
      break;
    case PROP_SCTP_LOCAL_PORT:
      self->priv->local_sctp_port = g_value_get_uint (value);
      break;
    case PROP_SCTP_REMOTE_PORT:
      self->priv->remote_sctp_port = g_value_get_uint (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);
}

static void
kms_webrtc_data_session_bin_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsWebRtcDataSessionBin *self = KMS_WEBRTC_DATA_SESSION_BIN (object);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  switch (property_id) {
    case PROP_DTLS_CLIENT_MODE:
      g_value_set_boolean (value, self->priv->dtls_client_mode);
      break;
    case PROP_SCTP_LOCAL_PORT:
      g_value_set_uint (value, self->priv->local_sctp_port);
      break;
    case PROP_SCTP_REMOTE_PORT:
      g_value_set_uint (value, self->priv->remote_sctp_port);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);
}

static void
kms_webrtc_data_session_bin_finalize (GObject * object)
{
  KmsWebRtcDataSessionBin *self = KMS_WEBRTC_DATA_SESSION_BIN (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);
  g_hash_table_unref (self->priv->channels);
  g_hash_table_unref (self->priv->data_channels);
  g_slist_free_full (self->priv->pending, g_object_unref);

  g_thread_pool_free (self->priv->pool, FALSE, FALSE);

  /* chain up */
  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static KmsWebRtcDataChannel *
kms_webrtc_data_session_bin_get_data_channel_action (KmsWebRtcDataSessionBin *
    self, guint stream_id)
{
  KmsWebRtcDataChannel *obj;

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  obj = (KmsWebRtcDataChannel *) g_hash_table_lookup (self->priv->channels,
      GUINT_TO_POINTER (stream_id));

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

  return obj;
}

static void
kms_webrtc_data_session_bin_destroy_data_channel_action (KmsWebRtcDataSessionBin
    * self, gint stream_id)
{
  GstElement *channel;

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  channel = g_hash_table_lookup (self->priv->data_channels,
      GUINT_TO_POINTER (stream_id));

  if (channel == NULL) {
    GST_WARNING_OBJECT (self, "No data channel for stream id %u", stream_id);
  } else {
    g_signal_emit_by_name (channel, "request-close", NULL);
  }

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);
}

static void
collect_data_channel_stats (GstElement * channel, GstStructure * stats)
{
  guint64 messages_sent, message_recv, bytes_sent, bytes_recv;
  KmsWebRtcDataChannelState state;
  gchar *label, *protocol, *name;
  GstStructure *channel_stats;
  const gchar *id;
  guint chann_id;

  id = kms_utils_get_uuid (G_OBJECT (channel));

  g_object_get (channel, "id", &chann_id, "label", &label, "protocol",
      &protocol, "state", &state, "bytes-sent", &bytes_sent, "bytes_recv",
      &bytes_recv, "messages-sent", &messages_sent, "messages-recv",
      &message_recv, NULL);

  channel_stats = gst_structure_new ("data-channel-statistics", "id",
      G_TYPE_STRING, id, "channel-id", G_TYPE_UINT, chann_id, "label",
      G_TYPE_STRING, label, "protocol", G_TYPE_STRING, protocol, "state",
      G_TYPE_UINT, state, "bytes-sent", G_TYPE_UINT64, bytes_sent,
      "bytes-recv", G_TYPE_UINT64, bytes_recv, "messages-sent", G_TYPE_UINT64,
      messages_sent, "messages-recv", G_TYPE_UINT64, message_recv, NULL);

  name = g_strdup_printf ("data-channel-%u", chann_id);

  gst_structure_set (stats, name, GST_TYPE_STRUCTURE, channel_stats, NULL);

  gst_structure_free (channel_stats);

  g_free (name);
  g_free (label);
  g_free (protocol);
}

static void
collect_data_channel_stats_cb (gpointer key, gpointer channel, gpointer stats)
{
  collect_data_channel_stats (channel, stats);
}

static GstStructure *
kms_webrtc_data_session_bin_stats_action (KmsWebRtcDataSessionBin * self)
{
  GstStructure *stats;

  stats = gst_structure_new (KMS_DATA_SESSION_STRUCT_NAME,
      "data-channels-opened", G_TYPE_UINT,
      g_atomic_int_get (&self->priv->opened), "data-channels-closed",
      G_TYPE_UINT, g_atomic_int_get (&self->priv->closed), NULL);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  g_slist_foreach (self->priv->pending, (GFunc) collect_data_channel_stats,
      stats);
  g_hash_table_foreach (self->priv->data_channels,
      collect_data_channel_stats_cb, stats);

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

  return stats;
}

static void
kms_webrtc_data_session_bin_class_init (KmsWebRtcDataSessionBinClass * klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->set_property = kms_webrtc_data_session_bin_set_property;
  gobject_class->get_property = kms_webrtc_data_session_bin_get_property;
  gobject_class->finalize = kms_webrtc_data_session_bin_finalize;

  gst_element_class_set_details_simple (element_class,
      "SCTP data session management",
      "SCTP/Bin/Connector",
      "Automatically manages WebRTC data establishment protocol",
      "Santiago Carot-Nemesio <sancane_dot_kurento_at_gmail_dot_com>");

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_template));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&src_template));

  obj_properties[PROP_DTLS_CLIENT_MODE] =
      g_param_spec_boolean ("dtls-client-mode", "DTLS client mode",
      "Indicates wheter the DTLS role is client or server.",
      DEFAULT_DTLS_CLIENT_MODE,
      G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY);

  obj_properties[PROP_SCTP_LOCAL_PORT] =
      g_param_spec_uint ("sctp-local-port", "SCTP local port",
      "The SCTP port to receive messages", SCTP_PORT_MIN, SCTP_PORT_MAX,
      DEFAULT_SCTP_LOCAL_PORT,
      G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE | G_PARAM_CONSTRUCT);

  obj_properties[PROP_SCTP_REMOTE_PORT] =
      g_param_spec_uint ("sctp-remote-port", "SCTP remote port",
      "The SCTP destination port", SCTP_PORT_MIN, SCTP_PORT_MAX,
      DEFAULT_SCTP_REMOTE_PORT,
      G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE | G_PARAM_CONSTRUCT);

  g_object_class_install_properties (gobject_class, N_PROPERTIES,
      obj_properties);

  obj_signals[DATA_CHANNEL_OPENED] =
      g_signal_new ("data-channel-opened",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, data_channel_opened),
      NULL, NULL, g_cclosure_marshal_VOID__UINT, G_TYPE_NONE, 1, G_TYPE_UINT);

  obj_signals[DATA_CHANNEL_CLOSED] =
      g_signal_new ("data-channel-closed",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, data_channel_closed),
      NULL, NULL, g_cclosure_marshal_VOID__UINT, G_TYPE_NONE, 1, G_TYPE_UINT);

  obj_signals[DATA_SESSION_ESTABLISHED] =
      g_signal_new ("data-session-established",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, data_session_established),
      NULL, NULL, g_cclosure_marshal_VOID__BOOLEAN, G_TYPE_NONE, 1,
      G_TYPE_BOOLEAN);

  obj_signals[CREATE_DATA_CHANNEL_ACTION] =
      g_signal_new ("create-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, create_data_channel),
      NULL, NULL, __kms_webrtc_data_marshal_INT__BOOLEAN_INT_INT_STRING_STRING,
      G_TYPE_INT, 5, G_TYPE_BOOLEAN, G_TYPE_INT, G_TYPE_INT, G_TYPE_STRING,
      G_TYPE_STRING);

  obj_signals[DESTROY_DATA_CHANNEL_ACTION] =
      g_signal_new ("destroy-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, destroy_data_channel),
      NULL, NULL, g_cclosure_marshal_VOID__INT, G_TYPE_NONE, 1, G_TYPE_INT);

  obj_signals[GET_DATA_CHANNEL_ACTION] =
      g_signal_new ("get-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, get_data_channel),
      NULL, NULL, __kms_webrtc_data_marshal_OBJECT__UINT,
      KMS_TYPE_WEBRTC_DATA_CHANNEL, 1, G_TYPE_UINT);

  obj_signals[STATS_ACTION] =
      g_signal_new ("stats", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebRtcDataSessionBinClass, stats),
      NULL, NULL, __kms_webrtc_data_marshal_BOXED__VOID, GST_TYPE_STRUCTURE, 0);

  klass->create_data_channel =
      kms_webrtc_data_session_bin_create_data_channel_action;
  klass->destroy_data_channel =
      kms_webrtc_data_session_bin_destroy_data_channel_action;
  klass->get_data_channel = kms_webrtc_data_session_bin_get_data_channel_action;
  klass->stats = kms_webrtc_data_session_bin_stats_action;

  g_type_class_add_private (klass, sizeof (KmsWebRtcDataSessionBinPrivate));
}

static gboolean
kms_webrtc_data_session_bin_link_data_channel_sink (KmsWebRtcDataSessionBin *
    self, GstElement * channel, GstPad * srcpad)
{
  gboolean ret = FALSE;
  GstPad *sinkpad;

  sinkpad = gst_element_get_static_pad (channel, "sink");
  ret = gst_pad_link (srcpad, sinkpad) == GST_PAD_LINK_OK;
  g_object_unref (sinkpad);

  return ret;
}

static gboolean
kms_webrtc_data_session_bin_link_data_channel_src (KmsWebRtcDataSessionBin *
    self, GstElement * channel)
{
  GstPadTemplate *pad_template;
  GstPad *srcpad, *sinkpad;
  guint sctp_stream_id;
  gboolean ret = FALSE;
  GstCaps *caps;
  gchar *name;

  g_object_get (G_OBJECT (channel), "id", &sctp_stream_id, NULL);

  caps =
      kms_webrtc_data_channel_bin_create_caps (KMS_WEBRTC_DATA_CHANNEL_BIN
      (channel));

  if (caps == NULL) {
    return FALSE;
  }

  pad_template =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (self->
          priv->sctpenc), "sink_%u");
  name = g_strdup_printf ("sink_%u", sctp_stream_id);
  sinkpad = gst_element_request_pad (self->priv->sctpenc, pad_template, name,
      caps);
  g_free (name);

  srcpad = gst_element_get_static_pad (channel, "src");

  ret = gst_pad_link (srcpad, sinkpad) == GST_PAD_LINK_OK;

  g_object_unref (srcpad);
  g_object_unref (sinkpad);
  gst_caps_unref (caps);

  return ret;
}

static gboolean
kms_webrtc_data_session_bin_is_valid_sctp_stream_id (KmsWebRtcDataSessionBin *
    self, guint16 sctp_stream_id)
{
  if ((sctp_stream_id == 65535) ||
      (IS_EVEN (sctp_stream_id) && self->priv->dtls_client_mode) ||
      (!IS_EVEN (sctp_stream_id) && !self->priv->dtls_client_mode)) {
    return FALSE;
  } else {
    return TRUE;
  }
}

static void
data_channel_negotiated_cb (KmsWebRtcDataChannelBin * channel_bin,
    KmsWebRtcDataSessionBin * self)
{
  KmsWebRtcDataChannel *data_channel;
  guint sctp_stream_id;

  g_object_get (channel_bin, "id", &sctp_stream_id, NULL);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  data_channel =
      (KmsWebRtcDataChannel *) g_hash_table_lookup (self->priv->channels,
      GUINT_TO_POINTER (sctp_stream_id));

  if (data_channel == NULL) {
    data_channel = kms_webrtc_data_channel_new (channel_bin);
    g_hash_table_insert (self->priv->channels,
        GUINT_TO_POINTER (sctp_stream_id), data_channel);
  }

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

  g_atomic_int_inc (&self->priv->opened);

  g_signal_emit (self, obj_signals[DATA_CHANNEL_OPENED], 0, sctp_stream_id);
}

static void
kms_webrtc_data_session_bin_reset_channel (KmsWebRtcDataChannelBin * channel,
    KmsWebRtcDataSessionBin * session)
{
  /* reset sctp dec asynchronously */
  g_thread_pool_push (session->priv->pool, channel, NULL);
}

static GstElement *
kms_webrtc_data_session_bin_create_data_channel (KmsWebRtcDataSessionBin
    * self, gboolean ordered, guint sctp_stream_id, gint max_packet_life_time,
    gint max_retransmits, const gchar * label, const gchar * protocol)
{
  GstElement *channel;

  channel = GST_ELEMENT (kms_webrtc_data_channel_bin_new (sctp_stream_id,
          ordered, max_packet_life_time, max_retransmits, label, protocol));
  kms_utils_set_uuid (G_OBJECT (channel));

  g_signal_connect (channel, "negotiated",
      G_CALLBACK (data_channel_negotiated_cb), self);
  kms_webrtc_data_channel_bin_set_reset_stream_callback
      (KMS_WEBRTC_DATA_CHANNEL_BIN (channel),
      (ResetStreamFunc) kms_webrtc_data_session_bin_reset_channel, self, NULL);

  return channel;
}

static GstElement *
kms_webrtc_data_session_bin_create_remote_data_channel (KmsWebRtcDataSessionBin
    * self, guint sctp_stream_id)
{
  GstElement *channel;

  if (!kms_webrtc_data_session_bin_is_valid_sctp_stream_id (self,
          sctp_stream_id)) {
    GST_WARNING_OBJECT (self, "Invalid data channel requested %u",
        sctp_stream_id);
    return NULL;
  }

  GST_DEBUG_OBJECT (self, "Opened stream id (%u)", sctp_stream_id);

  channel = kms_webrtc_data_session_bin_create_data_channel (self, TRUE,
      sctp_stream_id, -1, -1, NULL, NULL);
  g_hash_table_insert (self->priv->data_channels,
      GUINT_TO_POINTER (sctp_stream_id), channel);

  return channel;
}

static void
kms_webrtc_data_session_bin_add_remote_data_channel (KmsWebRtcDataSessionBin *
    self, GstElement * channel, GstPad * sctpdec_srcpad)
{
  guint sctp_stream_id;

  g_object_get (channel, "id", &sctp_stream_id, NULL);

  gst_bin_add (GST_BIN (self), channel);

  if (!kms_webrtc_data_session_bin_link_data_channel_src (self, channel) ||
      !kms_webrtc_data_session_bin_link_data_channel_sink (self, channel,
          sctpdec_srcpad)) {
    GST_ERROR_OBJECT (self, "Can not link data channel (%u)", sctp_stream_id);
    return;
  }

  gst_element_sync_state_with_parent (channel);
}

static void
kms_webrtc_data_session_bin_pad_added (GstElement * sctpdec, GstPad * pad,
    KmsWebRtcDataSessionBin * self)
{
  gboolean is_remote = FALSE;
  guint sctp_stream_id;
  GstElement *channel;
  gchar *name;

  name = gst_pad_get_name (pad);
  sscanf (name, "src_%u", &sctp_stream_id);
  g_free (name);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  channel =
      (GstElement *) g_hash_table_lookup (self->priv->data_channels,
      GUINT_TO_POINTER (sctp_stream_id));

  is_remote = channel == NULL;

  if (is_remote) {
    channel = kms_webrtc_data_session_bin_create_remote_data_channel (self,
        sctp_stream_id);
  }

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

  g_return_if_fail (channel != NULL);

  if (is_remote) {
    kms_webrtc_data_session_bin_add_remote_data_channel (self, channel, pad);
    return;
  }

  /* this is a local channel, connect the sink pad to the sctpdec */
  if (!kms_webrtc_data_session_bin_link_data_channel_sink (self, channel, pad)) {
    GST_ERROR_OBJECT (self, "Can not link data channel (%u)", sctp_stream_id);
  }
}

static void
kms_webrtc_data_session_bin_pad_removed (GstElement * sctpdec, GstPad * srcpad,
    KmsWebRtcDataSessionBin * self)
{
  GstPad *chann_srcpad, *sctpenc_sinkpad;
  gboolean emit_signal = FALSE;
  GstElement *channel;
  guint sctp_stream_id;
  gchar *name;

  name = gst_pad_get_name (srcpad);

  GST_DEBUG_OBJECT (self, "Pad removed %" GST_PTR_FORMAT, srcpad);

  if (!sscanf (name, "src_%u", &sctp_stream_id)) {
    g_free (name);
    return;
  }

  g_free (name);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  channel = (GstElement *) g_hash_table_lookup (self->priv->data_channels,
      GUINT_TO_POINTER (sctp_stream_id));
  g_hash_table_remove (self->priv->data_channels,
      GUINT_TO_POINTER (sctp_stream_id));
  g_hash_table_remove (self->priv->channels, GUINT_TO_POINTER (sctp_stream_id));

  if (channel == NULL) {
    GST_WARNING_OBJECT (self, "No data channel (%d) for pad %" GST_PTR_FORMAT,
        sctp_stream_id, srcpad);
    goto end;
  }

  gst_element_set_state (channel, GST_STATE_NULL);

  chann_srcpad = gst_element_get_static_pad (channel, "src");
  sctpenc_sinkpad = gst_pad_get_peer (chann_srcpad);

  if (sctpenc_sinkpad != NULL) {
    gst_pad_unlink (chann_srcpad, sctpenc_sinkpad);
    gst_element_release_request_pad (self->priv->sctpenc, sctpenc_sinkpad);
    g_object_unref (sctpenc_sinkpad);
  }

  g_object_unref (chann_srcpad);

  gst_bin_remove (GST_BIN (self), channel);

  emit_signal = TRUE;

end:
  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

  if (emit_signal) {
    g_atomic_int_inc (&self->priv->closed);
    g_signal_emit (self, obj_signals[DATA_CHANNEL_CLOSED], 0, sctp_stream_id);
  }
}

static guint
kms_webrtc_data_session_bin_pick_stream_id (KmsWebRtcDataSessionBin * self)
{
  guint *id;

  if (self->priv->dtls_client_mode) {
    id = &self->priv->even_id;
  } else {
    id = &self->priv->odd_id;
  }

  while (g_hash_table_contains (self->priv->data_channels,
          GUINT_TO_POINTER (*id))) {
    *id += 2;
  }

  return *id;
}

static gint
kms_webrtc_data_session_bin_create_data_channel_action (KmsWebRtcDataSessionBin
    * self, gboolean ordered, gint max_packet_life_time, gint max_retransmits,
    const gchar * label, const gchar * protocol)
{
  guint sctp_stream_id;
  GstElement *channel;

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  sctp_stream_id = kms_webrtc_data_session_bin_pick_stream_id (self);
  channel = kms_webrtc_data_session_bin_create_data_channel (self, ordered,
      sctp_stream_id, max_packet_life_time, max_retransmits, label, protocol);

  if (!self->priv->session_established) {
    self->priv->pending = g_slist_prepend (self->priv->pending, channel);
    KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

    return sctp_stream_id;
  }

  gst_bin_add (GST_BIN (self), channel);

  if (!kms_webrtc_data_session_bin_link_data_channel_src (self, channel)) {
    gst_bin_remove (GST_BIN (self), channel);
    GST_ERROR_OBJECT (self, "Can not create data channel for stream id %u",
        sctp_stream_id);
    KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);
    sctp_stream_id = -1;
  } else {
    g_hash_table_insert (self->priv->data_channels,
        GUINT_TO_POINTER (sctp_stream_id), channel);
    KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);
    gst_element_sync_state_with_parent (channel);
    g_signal_emit_by_name (channel, "request-open", NULL);
  }

  return sctp_stream_id;
}

static void
create_pending_data_channel_cb (GstElement * channel,
    KmsWebRtcDataSessionBin * self)
{
  guint sctp_stream_id;

  g_object_get (G_OBJECT (channel), "id", &sctp_stream_id, NULL);

  GST_DEBUG_OBJECT (self, "Creating posponed data channel for stream id %d",
      sctp_stream_id);

  gst_bin_add (GST_BIN (self), channel);

  if (!kms_webrtc_data_session_bin_link_data_channel_src (self, channel)) {
    gst_bin_remove (GST_BIN (self), channel);
    GST_ERROR_OBJECT (self, "Can not create data channel for stream id %u",
        sctp_stream_id);
  } else {
    g_hash_table_insert (self->priv->data_channels,
        GUINT_TO_POINTER (sctp_stream_id), channel);
    gst_element_sync_state_with_parent (channel);
    g_signal_emit_by_name (channel, "request-open", NULL);
  }
}

static void
kms_webrtc_data_session_bin_association_established (GstElement * sctpenc,
    gboolean connected, KmsWebRtcDataSessionBin * self)
{
  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  self->priv->session_established = connected;

  GST_DEBUG_OBJECT (self, "SCTP association %s",
      (connected) ? "established" : "finished");

  if (connected) {
    g_slist_foreach (self->priv->pending,
        (GFunc) create_pending_data_channel_cb, self);
    g_slist_free (self->priv->pending);
    self->priv->pending = NULL;
  }

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);

  g_signal_emit (self, obj_signals[DATA_SESSION_ESTABLISHED], 0, connected);
}

static void
reset_stream_async (gpointer data, gpointer session)
{
  KmsWebRtcDataSessionBin *self = KMS_WEBRTC_DATA_SESSION_BIN (session);
  KmsWebRtcDataChannelBin *channel = KMS_WEBRTC_DATA_CHANNEL_BIN (data);
  guint stream_id;

  g_object_get (channel, "id", &stream_id, NULL);

  KMS_WEBRTC_DATA_SESSION_BIN_LOCK (self);

  GST_DEBUG_OBJECT (self, "reseting stream id %u", stream_id);
  g_signal_emit_by_name (self->priv->sctpdec, "reset-stream", stream_id);

  KMS_WEBRTC_DATA_SESSION_BIN_UNLOCK (self);
}

static void
kms_webrtc_data_session_bin_init (KmsWebRtcDataSessionBin * self)
{
  GstPadTemplate *pad_template;
  GstPad *pad, *target;
  gchar *name;

  self->priv = KMS_WEBRTC_DATA_SESSION_BIN_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->opened = 0;
  self->priv->closed = 0;
  self->priv->data_channels = g_hash_table_new (g_direct_hash, g_direct_equal);
  self->priv->channels =
      g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL,
      g_object_unref);
  self->priv->assoc_id = get_sctp_association_id ();
  self->priv->session_established = FALSE;
  self->priv->even_id = 0;
  self->priv->odd_id = 1;
  self->priv->pool =
      g_thread_pool_new (reset_stream_async, self, -1, FALSE, NULL);

  name = get_decoder_name (self->priv->assoc_id);
  self->priv->sctpdec = gst_element_factory_make ("sctpdec", name);
  g_free (name);

  name = get_encoder_name (self->priv->assoc_id);
  self->priv->sctpenc = gst_element_factory_make ("sctpenc", name);
  g_free (name);

  g_object_set (self->priv->sctpdec, "sctp-association-id",
      self->priv->assoc_id, NULL);
  g_object_set (self->priv->sctpenc, "sctp-association-id",
      self->priv->assoc_id, "use-sock-stream", TRUE, NULL);

  g_object_bind_property (self, "sctp-local-port", self->priv->sctpdec,
      "local-sctp-port", G_BINDING_SYNC_CREATE);

  g_object_bind_property (self, "sctp-remote-port", self->priv->sctpenc,
      "remote-sctp-port", G_BINDING_SYNC_CREATE);

  g_signal_connect (self->priv->sctpdec, "pad-added",
      G_CALLBACK (kms_webrtc_data_session_bin_pad_added), self);
  g_signal_connect (self->priv->sctpdec, "pad-removed",
      G_CALLBACK (kms_webrtc_data_session_bin_pad_removed), self);
  g_signal_connect (self->priv->sctpenc, "sctp-association-established",
      G_CALLBACK (kms_webrtc_data_session_bin_association_established), self);

  gst_bin_add_many (GST_BIN (self), self->priv->sctpdec, self->priv->sctpenc,
      NULL);

  target = gst_element_get_static_pad (self->priv->sctpdec, "sink");
  pad_template = gst_static_pad_template_get (&sink_template);
  pad = gst_ghost_pad_new_from_template ("sink", target, pad_template);
  g_object_unref (pad_template);
  g_object_unref (target);

  gst_element_add_pad (GST_ELEMENT (self), pad);

  target = gst_element_get_static_pad (self->priv->sctpenc, "src");
  pad_template = gst_static_pad_template_get (&src_template);
  pad = gst_ghost_pad_new_from_template ("src", target, pad_template);
  g_object_unref (pad_template);
  g_object_unref (target);

  gst_element_add_pad (GST_ELEMENT (self), pad);

  gst_element_sync_state_with_parent (self->priv->sctpdec);
  gst_element_sync_state_with_parent (self->priv->sctpenc);
}

KmsWebRtcDataSessionBin *
kms_webrtc_data_session_bin_new (gboolean dtls_client_mode)
{
  gpointer *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_DATA_SESSION_BIN, "dtls-client-mode",
      dtls_client_mode, NULL);

  return KMS_WEBRTC_DATA_SESSION_BIN (obj);
}
