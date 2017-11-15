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

#include "kmsrtpconnection.h"
#include "kmssocketutils.h"

#define GST_CAT_DEFAULT kmsrtpconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define GST_DEFAULT_NAME "kmsrtpconnection"

#define KMS_RTP_CONNECTION_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_RTP_CONNECTION,                    \
    KmsRtpConnectionPrivate                     \
  )                                             \
)

enum
{
  PROP_0,
  PROP_ADDED,
  PROP_CONNECTED,
  PROP_IS_CLIENT,
  PROP_MIN_PORT,
  PROP_MAX_PORT
};

struct _KmsRtpConnectionPrivate
{
  GSocket *rtp_socket;
  GstElement *rtp_udpsink;
  GstElement *rtp_udpsrc;

  GSocket *rtcp_socket;
  GstElement *rtcp_udpsink;
  GstElement *rtcp_udpsrc;

  gboolean added;
  gboolean connected;
  gboolean is_client;
};

static void
kms_rtp_connection_interface_init (KmsIRtpConnectionInterface * iface);

G_DEFINE_TYPE_WITH_CODE (KmsRtpConnection, kms_rtp_connection,
    KMS_TYPE_RTP_BASE_CONNECTION,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTP_CONNECTION,
        kms_rtp_connection_interface_init));

static guint
kms_rtp_connection_get_rtp_port (KmsRtpBaseConnection * base_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_conn);

  return kms_socket_get_port (self->priv->rtp_socket);
}

static guint
kms_rtp_connection_get_rtcp_port (KmsRtpBaseConnection * base_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_conn);

  return kms_socket_get_port (self->priv->rtcp_socket);
}

static void
kms_rtp_connection_set_remote_info (KmsRtpBaseConnection * base_conn,
    const gchar * host, gint rtp_port, gint rtcp_port)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_conn);
  KmsRtpConnectionPrivate *priv = self->priv;

  GST_INFO_OBJECT (self, "Set remote host: %s, RTP: %d, RTCP: %d",
      host, rtp_port, rtcp_port);

  g_signal_emit_by_name (priv->rtp_udpsink, "add", host, rtp_port, NULL);
  g_signal_emit_by_name (priv->rtcp_udpsink, "add", host, rtcp_port, NULL);
}

static void
kms_rtp_connection_add (KmsIRtpConnection * base_rtp_conn, GstBin * bin,
    gboolean active)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);
  KmsRtpConnectionPrivate *priv = self->priv;

  self->priv->is_client = active;

  gst_bin_add_many (bin, g_object_ref (priv->rtp_udpsink),
      g_object_ref (priv->rtp_udpsrc),
      g_object_ref (priv->rtcp_udpsink),
      g_object_ref (priv->rtcp_udpsrc), NULL);
}

static void
kms_rtp_connection_src_sync_state_with_parent (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);
  KmsRtpConnectionPrivate *priv = self->priv;

  gst_element_sync_state_with_parent (priv->rtp_udpsrc);
  gst_element_sync_state_with_parent (priv->rtcp_udpsrc);
}

static void
kms_rtp_connection_sink_sync_state_with_parent (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);
  KmsRtpConnectionPrivate *priv = self->priv;

  gst_element_sync_state_with_parent (priv->rtp_udpsink);
  gst_element_sync_state_with_parent (priv->rtcp_udpsink);
}

static GstPad *
kms_rtp_connection_request_rtp_sink (KmsIRtpConnection * base_rtp_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtp_udpsink, "sink");
}

static GstPad *
kms_rtp_connection_request_rtp_src (KmsIRtpConnection * base_rtp_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtp_udpsrc, "src");
}

static GstPad *
kms_rtp_connection_request_rtcp_sink (KmsIRtpConnection * base_rtp_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtcp_udpsink, "sink");
}

static GstPad *
kms_rtp_connection_request_rtcp_src (KmsIRtpConnection * base_rtp_conn)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtcp_udpsrc, "src");
}

static void
kms_rtp_connection_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (object);

  switch (prop_id) {
    case PROP_ADDED:
      self->priv->added = g_value_get_boolean (value);
      break;
    case PROP_CONNECTED:
      self->priv->connected = g_value_get_boolean (value);
      break;
    case PROP_MIN_PORT:
      self->parent.min_port = g_value_get_uint (value);
      break;
    case PROP_MAX_PORT:
      self->parent.max_port = g_value_get_uint (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_rtp_connection_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (object);

  switch (prop_id) {
    case PROP_ADDED:
      g_value_set_boolean (value, self->priv->added);
      break;
    case PROP_CONNECTED:
      g_value_set_boolean (value, self->priv->connected);
      break;
    case PROP_IS_CLIENT:
      g_value_set_boolean (value, self->priv->is_client);
      break;
    case PROP_MIN_PORT:
      g_value_set_uint (value, self->parent.min_port);
      break;
    case PROP_MAX_PORT:
      g_value_set_uint (value, self->parent.max_port);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

KmsRtpConnection *
kms_rtp_connection_new (guint16 min_port, guint16 max_port, gboolean use_ipv6)
{
  GObject *obj;
  KmsRtpConnection *conn;
  KmsRtpConnectionPrivate *priv;
  GSocketFamily socket_family;

  obj = g_object_new (KMS_TYPE_RTP_CONNECTION, NULL);
  conn = KMS_RTP_CONNECTION (obj);
  priv = conn->priv;

  if (use_ipv6) {
    socket_family = G_SOCKET_FAMILY_IPV6;
  } else {
    socket_family = G_SOCKET_FAMILY_IPV4;
  }

  if (!kms_rtp_connection_get_rtp_rtcp_sockets
      (&priv->rtp_socket, &priv->rtcp_socket, min_port, max_port,
          socket_family)) {
    GST_ERROR_OBJECT (obj, "Cannot get ports");
    g_object_unref (obj);
    return NULL;
  }

  priv->rtp_udpsink = gst_element_factory_make ("multiudpsink", NULL);
  priv->rtp_udpsrc = gst_element_factory_make ("udpsrc", NULL);
  g_object_set (priv->rtp_udpsink, "socket", priv->rtp_socket,
      "sync", FALSE, "async", FALSE, NULL);
  g_object_set (priv->rtp_udpsrc, "socket", priv->rtp_socket, "auto-multicast",
      FALSE, NULL);

  priv->rtcp_udpsink = gst_element_factory_make ("multiudpsink", NULL);
  priv->rtcp_udpsrc = gst_element_factory_make ("udpsrc", NULL);
  g_object_set (priv->rtcp_udpsink, "socket", priv->rtcp_socket,
      "sync", FALSE, "async", FALSE, NULL);
  g_object_set (priv->rtcp_udpsrc, "socket", priv->rtcp_socket,
      "auto-multicast", FALSE, NULL);

  kms_i_rtp_connection_connected_signal (KMS_I_RTP_CONNECTION (conn));

  return conn;
}

static void
kms_rtp_connection_enable_latency_stats (KmsRtpBaseConnection * base)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base);
  GstPad *pad;

  kms_rtp_base_connection_remove_probe (base, self->priv->rtp_udpsrc, "src",
      base->src_probe);
  pad = gst_element_get_static_pad (self->priv->rtp_udpsrc, "src");
  base->src_probe = kms_stats_add_buffer_latency_meta_probe (pad, FALSE,
      0 /* No matter type at this point */ );
  g_object_unref (pad);

  kms_rtp_base_connection_remove_probe (base, self->priv->rtp_udpsink, "sink",
      base->sink_probe);
  pad = gst_element_get_static_pad (self->priv->rtp_udpsink, "sink");
  base->sink_probe = kms_stats_add_buffer_latency_notification_probe (pad,
      base->cb, TRUE /* Lock the data */ , base->user_data, NULL);
  g_object_unref (pad);
}

void
kms_rtp_transport_disable_latency_notification (KmsRtpBaseConnection * base)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base);

  kms_rtp_base_connection_remove_probe (base, self->priv->rtp_udpsrc, "src",
      base->src_probe);
  base->src_probe = 0UL;

  kms_rtp_base_connection_remove_probe (base, self->priv->rtp_udpsink, "sink",
      base->sink_probe);
  base->sink_probe = 0UL;
}

static void
kms_rtp_connection_collect_latency_stats (KmsIRtpConnection * obj,
    gboolean enable)
{
  KmsRtpBaseConnection *base = KMS_RTP_BASE_CONNECTION (obj);

  KMS_RTP_BASE_CONNECTION_LOCK (base);

  if (enable) {
    kms_rtp_connection_enable_latency_stats (base);
  } else {
    kms_rtp_transport_disable_latency_notification (base);
  }

  kms_rtp_base_connection_collect_latency_stats (obj, enable);

  KMS_RTP_BASE_CONNECTION_UNLOCK (base);
}

static void
kms_rtp_connection_finalize (GObject * object)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (object);
  KmsRtpConnectionPrivate *priv = self->priv;

  GST_DEBUG_OBJECT (self, "finalize");

  kms_rtp_transport_disable_latency_notification (KMS_RTP_BASE_CONNECTION
      (self));

  g_clear_object (&priv->rtp_udpsink);
  g_clear_object (&priv->rtp_udpsrc);
  kms_socket_finalize (&self->priv->rtp_socket);

  g_clear_object (&priv->rtcp_udpsink);
  g_clear_object (&priv->rtcp_udpsrc);
  kms_socket_finalize (&self->priv->rtcp_socket);

  /* chain up */
  G_OBJECT_CLASS (kms_rtp_connection_parent_class)->finalize (object);
}

static void
kms_rtp_connection_init (KmsRtpConnection * self)
{
  self->priv = KMS_RTP_CONNECTION_GET_PRIVATE (self);
  self->priv->connected = FALSE;
}

static void
kms_rtp_connection_class_init (KmsRtpConnectionClass * klass)
{
  GObjectClass *gobject_class;
  KmsRtpBaseConnectionClass *base_conn_class;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_rtp_connection_finalize;
  gobject_class->get_property = kms_rtp_connection_get_property;
  gobject_class->set_property = kms_rtp_connection_set_property;

  base_conn_class = KMS_RTP_BASE_CONNECTION_CLASS (klass);
  base_conn_class->get_rtp_port = kms_rtp_connection_get_rtp_port;
  base_conn_class->get_rtcp_port = kms_rtp_connection_get_rtcp_port;
  base_conn_class->set_remote_info = kms_rtp_connection_set_remote_info;

  g_type_class_add_private (klass, sizeof (KmsRtpConnectionPrivate));

  g_object_class_override_property (gobject_class, PROP_ADDED, "added");
  g_object_class_override_property (gobject_class, PROP_CONNECTED, "connected");
  g_object_class_override_property (gobject_class, PROP_IS_CLIENT, "is-client");
  g_object_class_override_property (gobject_class, PROP_MAX_PORT, "max-port");
  g_object_class_override_property (gobject_class, PROP_MIN_PORT, "min-port");
}

static void
kms_rtp_connection_interface_init (KmsIRtpConnectionInterface * iface)
{
  iface->add = kms_rtp_connection_add;
  iface->src_sync_state_with_parent =
      kms_rtp_connection_src_sync_state_with_parent;
  iface->sink_sync_state_with_parent =
      kms_rtp_connection_sink_sync_state_with_parent;
  iface->request_rtp_sink = kms_rtp_connection_request_rtp_sink;
  iface->request_rtp_src = kms_rtp_connection_request_rtp_src;
  iface->request_rtcp_sink = kms_rtp_connection_request_rtcp_sink;
  iface->request_rtcp_src = kms_rtp_connection_request_rtcp_src;
  iface->set_latency_callback = kms_rtp_base_connection_set_latency_callback;
  iface->collect_latency_stats = kms_rtp_connection_collect_latency_stats;
}
