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

#include <string.h>

#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include <gst/sctp/sctpreceivemeta.h>
#include <gst/sctp/sctpsendmeta.h>

#include "kmswebrtcdatachannelbin.h"
#include "kmswebrtcdataproto.h"
#include "kmswebrtcdatachannelstate.h"
#include "kmswebrtcdatachannelpriority.h"
#include "kms-webrtc-enumtypes.h"

#define PLUGIN_NAME "kmswebrtcdatachannelbin"

GST_DEBUG_CATEGORY_STATIC (kms_webrtc_data_channel_bin_debug_category);
#define GST_CAT_DEFAULT kms_webrtc_data_channel_bin_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcDataChannelBin, kms_webrtc_data_channel_bin,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_webrtc_data_channel_bin_debug_category,
        PLUGIN_NAME, 0, "debug category for webrtc_data_channel_bin"));

#define parent_class kms_webrtc_data_channel_bin_parent_class

#define DEFAULT_ORDERED TRUE
#define DEFAULT_MAX_PACKETS_LIFE_TIME (-1)
#define DEFAULT_MAX_PACKET_RETRANSMITS (-1)
#define DEFAULT_PROTOCOL ""
#define DEFAULT_NEGOTIATED FALSE
#define DEFAULT_ID 0
#define DEFAULT_LABEL ""

#define MAX_PACKETS_LIFE_TIME 65535
#define MAX_PACKET_RETRANSMITS 65535
#define MAX_CHUNK_SIZE G_MAXUSHORT

#define WEBRT_DATA_CAPS "application/webrtc-data"

#define DATA_CHANNEL_OPEN_MIN_SIZE 12   /* bytes */
#define DATA_CHANNEL_ACK_SIZE 1 /* bytes */

#define KMS_WEBRTC_DATA_CHANNEL_BIN_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                        \
    (obj),                                             \
    KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN,                  \
    KmsWebRtcDataChannelBinPrivate                     \
  )                                                    \
 )

struct _KmsWebRtcDataChannelBinPrivate
{
  GstElement *appsrc;
  GstElement *appsink;
  GRecMutex mutex;

  gboolean ordered;
  gint max_packet_life_time;
  gint max_packet_retransmits;
  guint priority;
  gchar *protocol;
  gboolean negotiated;
  guint16 id;
  gchar *label;
  guint64 bytes_sent;
  guint64 bytes_recv;
  guint64 messages_sent;
  guint64 messages_recv;

  KmsWebRtcDataChannelState state;

  guint ctrl_bytes_sent;

  DataChannelNewBuffer cb;
  gpointer user_data;
  GDestroyNotify notify;

  ResetStreamFunc reset_cb;
  gpointer reset_data;
  GDestroyNotify reset_notify;
};

#define KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK(obj) \
  (g_rec_mutex_lock (&KMS_WEBRTC_DATA_CHANNEL_BIN_CAST ((obj))->priv->mutex))
#define KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK(obj) \
  (g_rec_mutex_unlock (&KMS_WEBRTC_DATA_CHANNEL_BIN_CAST ((obj))->priv->mutex))

#define KMS_WEBRTC_DATA_CHANNEL_RESET(obj) ({                  \
  ResetStreamFunc _reset_cb = NULL;                            \
  gpointer _reset_data;                                        \
  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (obj);                      \
  GST_DEBUG_OBJECT ((obj), "Resetting data channel");          \
  (obj)->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSING; \
  _reset_cb = (obj)->priv->reset_cb;                           \
  _reset_data = (obj)->priv->reset_data;                       \
  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (obj);                    \
  if (_reset_cb != NULL) {                                     \
    _reset_cb ((obj), _reset_data);                            \
  }                                                            \
})

enum
{
  PROP_0,

  PROP_ORDERED,
  PROP_MAX_PACKET_LIFE_TIME,
  PROP_MAX_PACKET_RETRANSMITS,
  PROP_PRIORITY,
  PROP_PROTOCOL,
  PROP_NEGOTIATED,
  PROP_ID,
  PROP_LABEL,
  PROP_CHANNEL_STATE,
  PROP_BYTES_SENT,
  PROP_BYTES_RECV,
  PROP_MESSAGES_SENT,
  PROP_MESSAGES_RECV,

  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

enum
{
  /* signals */
  SIGNAL_NEGOTIATED,

  /* actions */
  REQUEST_OPEN,
  REQUEST_CLOSE,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (WEBRT_DATA_CAPS));

static GstStaticPadTemplate src_template = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (WEBRT_DATA_CAPS));

static const gchar *const str_state[] = {
  "connecting",
  "open",
  "closing",
  "closed"
};

static const gchar *
state2str (KmsWebRtcDataChannelState state)
{
  return str_state[state];
}

static gchar *
get_element_name (const gchar * element_name, guint assoc_id)
{
  return g_strdup_printf ("%s_%u", element_name, assoc_id);
}

static guint8 *
create_datachannel_open_request (KmsDataChannelChannelType channel_type,
    guint32 reliability_param, guint16 priority, const gchar * label,
    guint16 label_len, const gchar * protocol, guint16 protocol_len,
    guint32 * buf_size)
{
  guint8 *buf;

  *buf_size = DATA_CHANNEL_OPEN_MIN_SIZE + label_len + protocol_len;
  buf = g_malloc (*buf_size);

  GST_WRITE_UINT8 (buf, KMS_DATA_CHANNEL_MESSAGE_TYPE_OPEN_REQUEST);
  GST_WRITE_UINT8 (buf + 1, channel_type);
  GST_WRITE_UINT16_BE (buf + 2, priority);
  GST_WRITE_UINT32_BE (buf + 4, reliability_param);
  GST_WRITE_UINT16_BE (buf + 8, label_len);
  GST_WRITE_UINT16_BE (buf + 10, protocol_len);

  memcpy (buf + DATA_CHANNEL_OPEN_MIN_SIZE, label, label_len);
  memcpy (buf + DATA_CHANNEL_OPEN_MIN_SIZE + label_len, protocol, protocol_len);

  return buf;
}

static guint8 *
create_datachannel_ack (guint32 * buf_size)
{
  guint8 *buf;

  buf = g_malloc (DATA_CHANNEL_ACK_SIZE);

  GST_WRITE_UINT8 (buf, KMS_DATA_CHANNEL_MESSAGE_TYPE_ACK);
  *buf_size = DATA_CHANNEL_ACK_SIZE;

  return buf;
}

static void
kms_webrtc_data_channel_bin_set_protocol (KmsWebRtcDataChannelBin * self,
    gchar * protocol)
{
  g_free (self->priv->protocol);

  self->priv->protocol = protocol;

  if (self->priv->protocol == NULL) {
    GST_DEBUG_OBJECT (self, "Setting empty protocol");
    self->priv->protocol = g_strdup (DEFAULT_PROTOCOL);
  }
}

static void
kms_webrtc_data_channel_bin_set_label (KmsWebRtcDataChannelBin * self,
    gchar * label)
{
  g_free (self->priv->label);

  self->priv->label = label;

  if (self->priv->label == NULL) {
    GST_DEBUG_OBJECT (self, "Setting empty label");
    self->priv->label = g_strdup (DEFAULT_LABEL);
  }
}

static void
kms_webrtc_data_channel_bin_set_priority (KmsWebRtcDataChannelBin * self,
    KmsWebRtcDataChannelPriority priority)
{
  switch (priority) {
    case KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_IGNORED:
      self->priv->priority = KMS_DATA_CHANNEL_PRIORITY_IGNORED;
      break;
    case KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_BELOW_NORMAL:
      self->priv->priority = KMS_DATA_CHANNEL_PRIORITY_BELOW_NORMAL;
      break;
    case KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_NORMAL:
      self->priv->priority = KMS_DATA_CHANNEL_PRIORITY_NORMAL;
      break;
    case KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_HIGH:
      self->priv->priority = KMS_DATA_CHANNEL_PRIORITY_HIGH;
      break;
    case KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_EXTRA_HIGH:
      self->priv->priority = KMS_DATA_CHANNEL_PRIORITY_EXTRA_HIGH;
      break;
    default:
      GST_WARNING_OBJECT (self, "Trying to set an invalid priority value (%d)."
          " Normal priority will be used", priority);
      self->priv->priority = KMS_DATA_CHANNEL_PRIORITY_NORMAL;
  }
}

static void
kms_webrtc_data_channel_bin_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebRtcDataChannelBin *self = KMS_WEBRTC_DATA_CHANNEL_BIN (object);

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  switch (property_id) {
    case PROP_ID:
      self->priv->id = g_value_get_uint (value);
      break;
    case PROP_ORDERED:
      self->priv->ordered = g_value_get_boolean (value);
      break;
    case PROP_MAX_PACKET_LIFE_TIME:
      self->priv->max_packet_life_time = g_value_get_int (value);
      break;
    case PROP_MAX_PACKET_RETRANSMITS:
      self->priv->max_packet_retransmits = g_value_get_int (value);
      break;
    case PROP_PRIORITY:
      kms_webrtc_data_channel_bin_set_priority (self, g_value_get_enum (value));
      break;
    case PROP_PROTOCOL:
      kms_webrtc_data_channel_bin_set_protocol (self,
          g_value_dup_string (value));
      break;
    case PROP_NEGOTIATED:
      self->priv->negotiated = g_value_get_boolean (value);
      break;
    case PROP_LABEL:
      kms_webrtc_data_channel_bin_set_label (self, g_value_dup_string (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
}

static KmsWebRtcDataChannelPriority
kms_webrtc_data_channel_bin_get_priority (KmsWebRtcDataChannelBin * self)
{
  switch (self->priv->priority) {
    case KMS_DATA_CHANNEL_PRIORITY_IGNORED:
      return KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_IGNORED;
    case KMS_DATA_CHANNEL_PRIORITY_BELOW_NORMAL:
      return KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_BELOW_NORMAL;
    case KMS_DATA_CHANNEL_PRIORITY_NORMAL:
      return KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_NORMAL;
    case KMS_DATA_CHANNEL_PRIORITY_HIGH:
      return KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_HIGH;
    case KMS_DATA_CHANNEL_PRIORITY_EXTRA_HIGH:
      return KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_EXTRA_HIGH;
    default:
      GST_WARNING_OBJECT (self, "Invalid value for property priority (%d)."
          " Using normal priority", self->priv->priority);
      return KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_NORMAL;
  }
}

static void
kms_webrtc_data_channel_bin_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsWebRtcDataChannelBin *self = KMS_WEBRTC_DATA_CHANNEL_BIN (object);

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  switch (property_id) {
    case PROP_ID:
      g_value_set_uint (value, self->priv->id);
      break;
    case PROP_ORDERED:
      g_value_set_boolean (value, self->priv->ordered);
      break;
    case PROP_MAX_PACKET_LIFE_TIME:
      g_value_set_int (value, self->priv->max_packet_life_time);
      break;
    case PROP_MAX_PACKET_RETRANSMITS:
      g_value_set_int (value, self->priv->max_packet_retransmits);
      break;
    case PROP_PRIORITY:
      g_value_set_enum (value, kms_webrtc_data_channel_bin_get_priority (self));
      break;
    case PROP_PROTOCOL:
      g_value_set_string (value, self->priv->protocol);
      break;
    case PROP_NEGOTIATED:
      g_value_set_boolean (value, self->priv->negotiated);
      break;
    case PROP_LABEL:
      g_value_set_string (value, self->priv->label);
      break;
    case PROP_CHANNEL_STATE:
      g_value_set_enum (value, self->priv->state);
      break;
    case PROP_BYTES_SENT:
      g_value_set_uint64 (value, self->priv->bytes_sent);
      break;
    case PROP_BYTES_RECV:
      g_value_set_uint64 (value, self->priv->bytes_recv);
      break;
    case PROP_MESSAGES_SENT:
      g_value_set_uint64 (value, self->priv->messages_sent);
      break;
    case PROP_MESSAGES_RECV:
      g_value_set_uint64 (value, self->priv->messages_recv);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
}

static void
kms_webrtc_data_channel_bin_finalize (GObject * object)
{
  KmsWebRtcDataChannelBin *self = KMS_WEBRTC_DATA_CHANNEL_BIN (object);

  GST_DEBUG_OBJECT (self, "finalize");

  if (self->priv->notify != NULL) {
    self->priv->notify (self->priv->user_data);
  }

  if (self->priv->reset_notify != NULL) {
    self->priv->reset_notify (self->priv->reset_data);
  }

  g_rec_mutex_clear (&self->priv->mutex);
  g_free (self->priv->protocol);
  g_free (self->priv->label);

  /* chain up */
  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_webrtc_data_channel_bin_request_open (KmsWebRtcDataChannelBin * self)
{
  KmsDataChannelChannelType channel_type;
  guint32 reliability_param, buf_size;
  GstFlowReturn flow_ret;
  GstBuffer *gstbuf;
  guint8 *buf;

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  if (self->priv->negotiated ||
      self->priv->state != KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSED) {
    KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
    GST_WARNING_OBJECT (self, "Can not send open request in current state");
    return;
  }

  self->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_CONNECTING;

  channel_type =
      (self->priv->ordered ? KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE :
      KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE_UNORDERED) |
      (self->priv->max_packet_life_time != -1 ?
      KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED : 0) |
      (self->priv->max_packet_retransmits != -1 ?
      KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX : 0);

  reliability_param = 0;

  switch (channel_type) {
    case KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE:
    case KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE_UNORDERED:
      break;
    case KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX:
    case KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX_UNORDERED:
      if (self->priv->max_packet_retransmits != -1) {
        reliability_param = self->priv->max_packet_retransmits;
      }
      break;
    case KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED:
    case KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED_UNORDERED:
      if (self->priv->max_packet_life_time != -1) {
        reliability_param = self->priv->max_packet_life_time;
      }
      break;
    default:
      KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
      GST_ERROR_OBJECT (self, "Unsupported channel type (%hhx)",
          (guchar) channel_type);
      return;
  }

  buf = create_datachannel_open_request (channel_type, reliability_param,
      self->priv->priority, self->priv->label, strlen (self->priv->label),
      self->priv->protocol, strlen (self->priv->protocol), &buf_size);

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  gstbuf = gst_buffer_new_wrapped (buf, buf_size);
  gst_sctp_buffer_add_send_meta (gstbuf, KMS_DATA_CHANNEL_PPID_CONTROL, TRUE,
      GST_SCTP_SEND_META_PARTIAL_RELIABILITY_NONE, 0);

  flow_ret = gst_app_src_push_buffer (GST_APP_SRC (self->priv->appsrc), gstbuf);

  if (flow_ret != GST_FLOW_OK) {
    GST_WARNING_OBJECT (self, "Failed to push data buffer: %s",
        gst_flow_get_name (flow_ret));
  } else {
    KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);
    self->priv->ctrl_bytes_sent += buf_size;
    KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
  }
}

static void
kms_webrtc_data_channel_bin_request_close (KmsWebRtcDataChannelBin * self)
{
  ResetStreamFunc reset_cb = NULL;
  gpointer reset_data;

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  if (self->priv->state >= KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSING) {
    GST_DEBUG_OBJECT (self, "Reset operation already done");
    goto end;
  }

  self->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSING;
  reset_cb = self->priv->reset_cb;
  reset_data = self->priv->reset_data;

end:
  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  if (reset_cb != NULL) {
    reset_cb (self, reset_data);
  }
}

static void
kms_webrtc_data_channel_bin_class_init (KmsWebRtcDataChannelBinClass * klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->set_property = kms_webrtc_data_channel_bin_set_property;
  gobject_class->get_property = kms_webrtc_data_channel_bin_get_property;
  gobject_class->finalize = kms_webrtc_data_channel_bin_finalize;

  gst_element_class_set_details_simple (element_class,
      "SCTP data channel management",
      "SCTP/Bin/Data",
      "Automatically manages WebRTC data establishment protocol",
      "Santiago Carot-Nemesio <sancane_dot_kurento_at_gmail_dot_com>");

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_template));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&src_template));

  obj_properties[PROP_ORDERED] =
      g_param_spec_boolean ("ordered", "Ordered", "Send data ordered",
      DEFAULT_ORDERED, G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY);

  obj_properties[PROP_MAX_PACKET_LIFE_TIME] =
      g_param_spec_int ("max-packet-life-time", "Max packet life time",
      "The maximum time to try to retransmit a packet", -1,
      MAX_PACKETS_LIFE_TIME, DEFAULT_MAX_PACKETS_LIFE_TIME,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_MAX_PACKET_RETRANSMITS] =
      g_param_spec_int ("max-retransmits", "Max retransmits",
      "The maximum number of retransmits for a packet", -1,
      MAX_PACKET_RETRANSMITS, DEFAULT_MAX_PACKET_RETRANSMITS,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_PRIORITY] =
      g_param_spec_enum ("priority", "Channel priority",
      "The priority of this data channel",
      KMS_TYPE_WEB_RTC_DATA_CHANNEL_PRIORITY,
      KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_IGNORED,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_PROTOCOL] =
      g_param_spec_string ("protocol", "DataChannel protocol",
      "Sub-protocol used for this channel", DEFAULT_PROTOCOL,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_NEGOTIATED] =
      g_param_spec_boolean ("negotiated", "Negotiated",
      "Datachannel already negotiated", DEFAULT_NEGOTIATED,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_ID] = g_param_spec_uint ("id", "Id",
      "Channel id. Unless otherwise defined or negotiated, the id are picked based on the DTLS"
      " role; client picks even identifiers and server picks odd. However, the application is "
      "responsible for avoiding conflicts. In case of conflict, the channel should fail.",
      0, G_MAXUSHORT, DEFAULT_ID, G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY |
      G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_LABEL] = g_param_spec_string ("label", "Label",
      "The label of the channel.", DEFAULT_LABEL,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_CHANNEL_STATE] =
      g_param_spec_enum ("state", "Current state",
      "The current state of the data channel",
      KMS_TYPE_WEB_RTC_DATA_CHANNEL_STATE,
      KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSED,
      G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_BYTES_SENT] =
      g_param_spec_uint64 ("bytes-sent", "Bytes sent",
      "The amount of bytes sent on this data channel", 0,
      G_MAXULONG, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_BYTES_RECV] =
      g_param_spec_uint64 ("bytes-recv", "Bytes received",
      "The amount of bytes received on this data channel", 0,
      G_MAXULONG, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_MESSAGES_SENT] =
      g_param_spec_uint64 ("messages-sent", "Messages sent",
      "The number of messages sent on this data channel", 0,
      G_MAXULONG, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);

  obj_properties[PROP_MESSAGES_RECV] =
      g_param_spec_uint64 ("messages-recv", "Messages received",
      "The number of messages received on this data channel", 0,
      G_MAXULONG, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);

  g_object_class_install_properties (gobject_class, N_PROPERTIES,
      obj_properties);

  obj_signals[SIGNAL_NEGOTIATED] =
      g_signal_new ("negotiated",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebRtcDataChannelBinClass, negotiated), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  obj_signals[REQUEST_OPEN] =
      g_signal_new ("request-open",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebRtcDataChannelBinClass, request_open), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  obj_signals[REQUEST_CLOSE] =
      g_signal_new ("request-close",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebRtcDataChannelBinClass, request_close), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  klass->request_open = kms_webrtc_data_channel_bin_request_open;
  klass->request_close = kms_webrtc_data_channel_bin_request_close;

  g_type_class_add_private (klass, sizeof (KmsWebRtcDataChannelBinPrivate));
}

static void
kms_webrtc_data_channel_bin_send_data_channel_ack (KmsWebRtcDataChannelBin *
    self)
{
  GstFlowReturn flow_ret;
  GstBuffer *gstbuf;
  guint32 buf_size;
  guint8 *ackbuf;

  ackbuf = create_datachannel_ack (&buf_size);
  gstbuf = gst_buffer_new_wrapped (ackbuf, buf_size);
  gst_sctp_buffer_add_send_meta (gstbuf, KMS_DATA_CHANNEL_PPID_CONTROL, TRUE,
      GST_SCTP_SEND_META_PARTIAL_RELIABILITY_NONE, 0);

  flow_ret = gst_app_src_push_buffer (GST_APP_SRC (self->priv->appsrc), gstbuf);

  if (flow_ret != GST_FLOW_OK) {
    GST_WARNING_OBJECT (self, "Failed to push data buffer: %s",
        gst_flow_get_name (flow_ret));
    KMS_WEBRTC_DATA_CHANNEL_RESET (self);
  } else {
    KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);
    self->priv->ctrl_bytes_sent += buf_size;
    self->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_OPEN;
    self->priv->negotiated = TRUE;
    KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

    g_signal_emit (self, obj_signals[SIGNAL_NEGOTIATED], 0);
  }
}

static void
kms_webrtc_data_channel_bin_handle_open_request (KmsWebRtcDataChannelBin *
    self, guint8 * data, guint32 size)
{
  guint16 priority, label_len, protocol_len;
  KmsDataChannelChannelType channel_type;
  guint32 reliability_param;
  gchar *label, *protocol;
  guint32 msg_size;

  if (size < DATA_CHANNEL_OPEN_MIN_SIZE) {
    GST_WARNING_OBJECT (self,
        "Invalid size of data channel control message: %u, expected > %u", size,
        DATA_CHANNEL_OPEN_MIN_SIZE);
    KMS_WEBRTC_DATA_CHANNEL_RESET (self);
    return;
  }

  channel_type = GST_READ_UINT8 (data + 1);
  priority = GST_READ_UINT16_BE (data + 2);
  reliability_param = GST_READ_UINT32_BE (data + 4);
  label_len = GST_READ_UINT16_BE (data + 8);
  protocol_len = GST_READ_UINT16_BE (data + 10);

  msg_size = DATA_CHANNEL_OPEN_MIN_SIZE + label_len + protocol_len;

  if (size != msg_size) {
    GST_WARNING_OBJECT (self,
        "Invalid size of data channel control message: %u, expected %u", size,
        msg_size);
    KMS_WEBRTC_DATA_CHANNEL_RESET (self);
    return;
  }

  switch (priority) {
    case KMS_DATA_CHANNEL_PRIORITY_IGNORED:
    case KMS_DATA_CHANNEL_PRIORITY_BELOW_NORMAL:
    case KMS_DATA_CHANNEL_PRIORITY_NORMAL:
    case KMS_DATA_CHANNEL_PRIORITY_HIGH:
    case KMS_DATA_CHANNEL_PRIORITY_EXTRA_HIGH:
      break;
    default:
      GST_WARNING_OBJECT (self, "Invalid priority level negotiated: %d",
          priority);
      KMS_WEBRTC_DATA_CHANNEL_RESET (self);
      return;
  }

  label = g_strndup ((const gchar *) data + 12, label_len);
  protocol = g_strndup ((const gchar *) data + 12 + label_len, protocol_len);

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  if (self->priv->state != KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSED) {
    KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
    GST_WARNING_OBJECT (self, "Data channel open request received in state %s",
        state2str (self->priv->state));
    return;
  }

  self->priv->priority = priority;
  self->priv->ordered =
      !(channel_type & KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE_UNORDERED);
  self->priv->max_packet_life_time = -1;
  self->priv->max_packet_retransmits = -1;
  self->priv->negotiated = FALSE;
  self->priv->messages_recv = G_GUINT64_CONSTANT (0);
  self->priv->messages_sent = G_GUINT64_CONSTANT (0);
  self->priv->bytes_recv = G_GUINT64_CONSTANT (0);
  self->priv->bytes_sent = G_GUINT64_CONSTANT (0);
  self->priv->ctrl_bytes_sent = 0;

  kms_webrtc_data_channel_bin_set_label (self, label);
  kms_webrtc_data_channel_bin_set_protocol (self, protocol);

  if (channel_type == KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX ||
      channel_type ==
      KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX_UNORDERED) {
    self->priv->max_packet_retransmits = reliability_param;
  } else if (channel_type ==
      KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED
      || channel_type ==
      KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED_UNORDERED) {
    self->priv->max_packet_life_time = reliability_param;
  }

  self->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_CONNECTING;

  GST_LOG_OBJECT (self, "Received data channel open request: stream id = %u,"
      " ordered = %u, max_packets_life_time = %d, max_packet_retransmits = %d,"
      " negotiated=%u, protocol = \"%s\", label= \"%s\", state = %s",
      self->priv->id, self->priv->ordered, self->priv->max_packet_life_time,
      self->priv->max_packet_retransmits, self->priv->negotiated,
      self->priv->protocol, self->priv->label, state2str (self->priv->state));

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  kms_webrtc_data_channel_bin_send_data_channel_ack (self);
}

static void
kms_webrtc_data_channel_bin_handle_ack (KmsWebRtcDataChannelBin *
    self, guint8 * data, guint32 size)
{
  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  self->priv->negotiated = TRUE;
  self->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_OPEN;

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  g_signal_emit (self, obj_signals[SIGNAL_NEGOTIATED], 0);
}

static void
kms_webrtc_data_channel_bin_handle_control_message (KmsWebRtcDataChannelBin *
    self, guint8 * data, guint32 size)
{
  KMSDataChannelMessageType message_type;

  if (size == 0) {
    GST_WARNING_OBJECT (self,
        "Invalid size of data channel control message: %u, expected > 0", size);
    KMS_WEBRTC_DATA_CHANNEL_RESET (self);
    return;
  }

  message_type = GST_READ_UINT8 (data);

  if (message_type == KMS_DATA_CHANNEL_MESSAGE_TYPE_OPEN_REQUEST) {
    kms_webrtc_data_channel_bin_handle_open_request (self, data, size);
  } else if (message_type == KMS_DATA_CHANNEL_MESSAGE_TYPE_ACK) {
    kms_webrtc_data_channel_bin_handle_ack (self, data, size);
  } else {
    GST_WARNING_OBJECT (self, "Received invalid data channel control message");
    KMS_WEBRTC_DATA_CHANNEL_RESET (self);
  }
}

static GstFlowReturn
new_data_callback (GstAppSink * appsink, KmsWebRtcDataChannelBin * self)
{
  const GstMetaInfo *meta_info = GST_SCTP_RECEIVE_META_INFO;
  gboolean notify = FALSE, reset = FALSE;
  gpointer state = NULL;
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstMapInfo info;
  guint16 ppid = 0;
  gsize size = 0;
  GstMeta *meta;

  sample = gst_app_sink_pull_sample (GST_APP_SINK (self->priv->appsink));
  g_return_val_if_fail (sample, GST_FLOW_ERROR);

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    gst_sample_unref (sample);
    GST_ERROR_OBJECT (self, "No buffer got from sample");
    return GST_FLOW_ERROR;
  }

  if (!gst_buffer_map (buffer, &info, GST_MAP_READ)) {
    gst_sample_unref (sample);
    GST_ERROR_OBJECT (self, "Can not read buffer");
    return GST_FLOW_ERROR;
  }

  while ((meta = gst_buffer_iterate_meta (buffer, &state))) {
    if (meta->info->api == meta_info->api) {
      GstSctpReceiveMeta *sctp_receive_meta = (GstSctpReceiveMeta *) meta;

      ppid = sctp_receive_meta->ppid;
      break;
    }
  }

  switch (ppid) {
    case KMS_DATA_CHANNEL_PPID_CONTROL:
      kms_webrtc_data_channel_bin_handle_control_message (self, info.data,
          info.size);
      break;
    case KMS_DATA_CHANNEL_PPID_BINARY_PARTIAL:
      GST_WARNING_OBJECT (self,
          "PPID: DATA_CHANNEL_PPID_BINARY_PARTIAL - Deprecated - Not supported");
      reset = TRUE;
      break;
    case KMS_DATA_CHANNEL_PPID_STRING_PARTIAL:
      GST_WARNING_OBJECT (self,
          "PPID: DATA_CHANNEL_PPID_STRING_PARTIAL - Deprecated - Not supported");
      reset = TRUE;
      break;
    case KMS_DATA_CHANNEL_PPID_STRING:
    case KMS_DATA_CHANNEL_PPID_BINARY:
      size = info.size;
    case KMS_DATA_CHANNEL_PPID_STRING_EMPTY:
    case KMS_DATA_CHANNEL_PPID_BINARY_EMPTY:
      KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);
      self->priv->bytes_recv += size;
      self->priv->messages_recv++;
      KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
      notify = TRUE;
      break;
    default:
      GST_WARNING_OBJECT (self, "Unsupported PPID received: %u", ppid);
      reset = TRUE;
      break;
  }

  gst_buffer_unmap (buffer, &info);

  if (reset) {
    KMS_WEBRTC_DATA_CHANNEL_RESET (self);
    ret = GST_FLOW_ERROR;
    goto end;
  }

  if (notify && self->priv->cb != NULL) {
    ret = self->priv->cb (G_OBJECT (self), buffer, self->priv->user_data);
  } else {
    ret = GST_FLOW_OK;
  }

end:
  gst_sample_unref (sample);

  return ret;
}

static void
kms_webrtc_data_channel_bin_init (KmsWebRtcDataChannelBin * self)
{
  GstAppSinkCallbacks callbacks;
  GstPadTemplate *pad_template;
  GstPad *pad, *target;
  gchar *name;

  self->priv = KMS_WEBRTC_DATA_CHANNEL_BIN_GET_PRIVATE (self);

  self->priv->bytes_recv = G_GUINT64_CONSTANT (0);
  self->priv->bytes_sent = G_GUINT64_CONSTANT (0);
  self->priv->messages_recv = G_GUINT64_CONSTANT (0);
  self->priv->messages_sent = G_GUINT64_CONSTANT (0);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->state = KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSED;

  name = get_element_name ("datasrc", self->priv->id);
  self->priv->appsrc = gst_element_factory_make ("appsrc", name);
  g_free (name);

  name = get_element_name ("datasink", self->priv->id);
  self->priv->appsink = gst_element_factory_make ("appsink", name);
  g_free (name);

  callbacks.eos = NULL;
  callbacks.new_preroll = NULL;
  callbacks.new_sample =
      (GstFlowReturn (*)(GstAppSink *, gpointer)) new_data_callback;

  g_object_set (self->priv->appsink, "async", FALSE, "sync", FALSE,
      "emit-signals", FALSE, "drop", FALSE, "enable-last-sample", FALSE, NULL);

  gst_app_sink_set_callbacks (GST_APP_SINK (self->priv->appsink), &callbacks,
      self, NULL);

  g_object_set (self->priv->appsrc, "is-live", TRUE, "min-latency",
      G_GINT64_CONSTANT (0), "do-timestamp", TRUE, "max-bytes", 0,
      "emit-signals", FALSE, NULL);

  gst_bin_add_many (GST_BIN (self), self->priv->appsrc, self->priv->appsink,
      NULL);

  target = gst_element_get_static_pad (self->priv->appsink, "sink");
  pad_template = gst_static_pad_template_get (&sink_template);
  pad = gst_ghost_pad_new_from_template ("sink", target, pad_template);
  g_object_unref (pad_template);
  g_object_unref (target);

  gst_element_add_pad (GST_ELEMENT (self), pad);

  target = gst_element_get_static_pad (self->priv->appsrc, "src");
  pad_template = gst_static_pad_template_get (&src_template);
  pad = gst_ghost_pad_new_from_template ("src", target, pad_template);
  g_object_unref (pad_template);
  g_object_unref (target);

  gst_element_add_pad (GST_ELEMENT (self), pad);

  gst_element_sync_state_with_parent (self->priv->appsrc);
  gst_element_sync_state_with_parent (self->priv->appsink);
}

KmsWebRtcDataChannelBin *
kms_webrtc_data_channel_bin_new (guint id, gboolean ordered,
    gint max_packet_life_time, gint max_retransmits, const gchar * label,
    const gchar * protocol)
{
  KmsWebRtcDataChannelBin *obj;
  GstCaps *caps;

  obj =
      KMS_WEBRTC_DATA_CHANNEL_BIN (g_object_new
      (KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN, "id", id, "ordered", ordered,
          "max-packet-life-time", max_packet_life_time, "max-retransmits",
          max_retransmits, "label", label, "protocol", protocol, NULL));

  caps = kms_webrtc_data_channel_bin_create_caps (obj);
  g_object_set (obj->priv->appsrc, "caps", caps, NULL);
  gst_caps_unref (caps);

  return obj;
}

GstCaps *
kms_webrtc_data_channel_bin_create_caps (KmsWebRtcDataChannelBin * self)
{
  GstCaps *caps = NULL;

  g_return_val_if_fail (self != NULL, NULL);
  g_return_val_if_fail (KMS_IS_WEBRTC_DATA_CHANNEL_BIN (self), NULL);

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  if (self->priv->max_packet_life_time != -1 &&
      self->priv->max_packet_retransmits != -1) {
    GST_WARNING_OBJECT (self, "Invalid parameters for creating caps");
    goto end;
  }

  caps = gst_caps_new_simple (WEBRT_DATA_CAPS, "ordered", G_TYPE_BOOLEAN,
      self->priv->ordered, NULL);

  if (self->priv->max_packet_life_time == -1 &&
      self->priv->max_packet_retransmits == -1) {
    gst_caps_set_simple (caps, "partially-reliability", G_TYPE_STRING, "none",
        "reliability-parameter", G_TYPE_UINT, 0, NULL);
  } else if (self->priv->max_packet_retransmits >= 0) {
    gst_caps_set_simple (caps, "partially-reliability", G_TYPE_STRING, "rtx",
        "reliability-parameter", G_TYPE_UINT,
        self->priv->max_packet_retransmits, NULL);
  } else if (self->priv->max_packet_life_time >= 0) {
    gst_caps_set_simple (caps, "partially-reliability", G_TYPE_STRING, "ttl",
        "reliability-parameter", G_TYPE_UINT,
        self->priv->max_packet_life_time, NULL);
  }

end:
  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  return caps;
}

static gboolean
kms_webrtc_data_channel_bin_get_ppid_from_meta (KmsWebRtcDataChannelBin * self,
    GstSctpReceiveMeta * meta, gboolean is_empty, KmsDataChannelPPID * ppid)
{
  switch (meta->ppid) {
    case KMS_DATA_CHANNEL_PPID_STRING:
      if (is_empty) {
        GST_WARNING_OBJECT (self, "Invalid ppid used for empty string buffers");
        return FALSE;
      }
      break;
    case KMS_DATA_CHANNEL_PPID_BINARY:
      if (is_empty) {
        GST_WARNING_OBJECT (self, "Invalid ppid used for empty binary buffers");
        return FALSE;
      }
      break;
    case KMS_DATA_CHANNEL_PPID_STRING_EMPTY:
      if (!is_empty) {
        GST_WARNING_OBJECT (self, "Invalid empty ppid set for string buffers");
        return FALSE;
      }
      break;
    case KMS_DATA_CHANNEL_PPID_BINARY_EMPTY:
      if (!is_empty) {
        GST_WARNING_OBJECT (self, "Invalid empty ppid set for binary buffers");
      }
      break;
    default:
      GST_WARNING_OBJECT (self, "Can not push buffer with ppid %u", meta->ppid);
      return FALSE;
  }

  *ppid = meta->ppid;

  return TRUE;
}

GstFlowReturn
kms_webrtc_data_channel_bin_push_buffer (KmsWebRtcDataChannelBin * self,
    GstBuffer * buffer, gboolean is_binary)
{
  const GstMetaInfo *meta_info = GST_SCTP_RECEIVE_META_INFO;
  GstSctpReceiveMeta *sctp_receive_meta = NULL;
  GstSctpSendMetaPartiallyReliability pr;
  gboolean is_empty, ordered;
  KmsDataChannelPPID ppid;
  GstBuffer *send_buffer;
  guint64 bytes_sent = 0;
  gpointer state = NULL;
  GstFlowReturn ret;
  guint32 pr_param = 0;
  GstMapInfo info;
  GstBuffer *buff;
  GstMeta *meta;

  if (self == NULL || !KMS_IS_WEBRTC_DATA_CHANNEL_BIN (self)) {
    gst_buffer_unref (buffer);
    g_return_val_if_reached (GST_FLOW_ERROR);
  }

  if (!gst_buffer_map (buffer, &info, GST_MAP_READ)) {
    gst_buffer_unref (buffer);
    GST_ERROR_OBJECT (self, "Can not read buffer");
    return GST_FLOW_ERROR;
  }

  while ((meta = gst_buffer_iterate_meta (buffer, &state))) {
    if (meta->info->api == meta_info->api) {
      sctp_receive_meta = (GstSctpReceiveMeta *) meta;
      break;
    }
  }

  bytes_sent = info.size;
  is_empty = info.size == 0;

  gst_buffer_unmap (buffer, &info);

  if (sctp_receive_meta != NULL &&
      !kms_webrtc_data_channel_bin_get_ppid_from_meta (self,
          sctp_receive_meta, is_empty, &ppid)) {
    gst_buffer_unref (buffer);

    return GST_FLOW_ERROR;
  } else if (is_binary) {
    if (is_empty) {
      ppid = KMS_DATA_CHANNEL_PPID_BINARY_EMPTY;
    } else {
      ppid = KMS_DATA_CHANNEL_PPID_BINARY;
    }
  } else {
    if (is_empty) {
      ppid = KMS_DATA_CHANNEL_PPID_STRING_EMPTY;
    } else {
      ppid = KMS_DATA_CHANNEL_PPID_STRING;
    }
  }

  if (is_empty) {
    guint8 *zero_byte;

    gst_buffer_unref (buffer);
    zero_byte = g_new0 (guint8, 1);
    send_buffer = gst_buffer_new_wrapped (zero_byte, 1);
  } else {
    send_buffer = buffer;
  }

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  ordered = self->priv->ordered;

  switch (self->priv->state) {
    case KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSING:
    case KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSED:
      KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
      return GST_FLOW_NOT_LINKED;
    case KMS_WEB_RTC_DATA_CHANNEL_STATE_CONNECTING:
      /* open request has been sent but no ack is received yet */
      ordered = TRUE;
    case KMS_WEB_RTC_DATA_CHANNEL_STATE_OPEN:
      break;
    default:
      KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);
      GST_ERROR_OBJECT (self, "Channel is in an invalid state %u",
          self->priv->state);
      gst_buffer_unref (send_buffer);
      return GST_FLOW_ERROR;
  }

  if (self->priv->max_packet_life_time == -1
      && self->priv->max_packet_retransmits == -1) {
    pr = GST_SCTP_SEND_META_PARTIAL_RELIABILITY_NONE;
    pr_param = 0;
  } else if (self->priv->max_packet_life_time != -1) {
    pr = GST_SCTP_SEND_META_PARTIAL_RELIABILITY_TTL;
    pr_param = self->priv->max_packet_life_time;
  } else {                      /* if (self->priv->max_packet_retransmits != -1) */

    pr = GST_SCTP_SEND_META_PARTIAL_RELIABILITY_RTX;
    pr_param = self->priv->max_packet_retransmits;
  }

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  /* Buffer must be writable to add meta */
  buff = gst_buffer_make_writable (send_buffer);

  gst_sctp_buffer_add_send_meta (buff, ppid, ordered, pr, pr_param);

  ret = gst_app_src_push_buffer (GST_APP_SRC (self->priv->appsrc), buff);

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  if (bytes_sent > 0 && ret == GST_FLOW_OK) {
    self->priv->bytes_sent += bytes_sent;
  }

  self->priv->messages_sent++;

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  return ret;
}

void
kms_webrtc_data_channel_bin_set_new_buffer_callback (KmsWebRtcDataChannelBin *
    self, DataChannelNewBuffer cb, gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  g_return_if_fail (self != NULL);
  g_return_if_fail (KMS_IS_WEBRTC_DATA_CHANNEL_BIN (self));

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  data = self->priv->user_data;
  destroy = self->priv->notify;

  self->priv->cb = cb;
  self->priv->notify = notify;
  self->priv->user_data = user_data;

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  if (destroy != NULL) {
    destroy (data);
  }
}

void
kms_webrtc_data_channel_bin_set_reset_stream_callback (KmsWebRtcDataChannelBin *
    self, ResetStreamFunc cb, gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  g_return_if_fail (self != NULL);
  g_return_if_fail (KMS_IS_WEBRTC_DATA_CHANNEL_BIN (self));

  KMS_WEBRTC_DATA_CHANNEL_BIN_LOCK (self);

  data = self->priv->reset_data;
  destroy = self->priv->reset_notify;

  self->priv->reset_cb = cb;
  self->priv->reset_notify = notify;
  self->priv->reset_data = user_data;

  KMS_WEBRTC_DATA_CHANNEL_BIN_UNLOCK (self);

  if (destroy != NULL) {
    destroy (data);
  }
}
