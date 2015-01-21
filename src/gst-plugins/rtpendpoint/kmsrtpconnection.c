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

#include "kmsrtpconnection.h"
#include <gio/gio.h>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kmsrtpconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define MAX_RETRIES 4

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
  PROP_CONNECTED
};

struct _KmsRtpConnectionPrivate
{
  GSocket *rtp_socket;
  GstElement *rtp_udpsink;
  GstElement *rtp_udpsrc;

  GSocket *rtcp_socket;
  GstElement *rtcp_udpsink;
  GstElement *rtcp_udpsrc;

  gboolean connected;
};

static void
kms_rtp_connection_interface_init (KmsIRtpConnectionInterface * iface);

G_DEFINE_TYPE_WITH_CODE (KmsRtpConnection, kms_rtp_connection,
    KMS_TYPE_RTP_BASE_CONNECTION,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTP_CONNECTION,
        kms_rtp_connection_interface_init));

/* Socket management begin */

static void
kms_socket_finalize (GSocket ** socket)
{
  if (socket == NULL || *socket == NULL) {
    return;
  }

  g_socket_close (*socket, NULL);
  g_clear_object (socket);
}

static GSocket *
kms_socket_open (guint16 port)
{
  GSocket *socket;
  GSocketAddress *bind_saddr;
  GInetAddress *addr;

  socket = g_socket_new (G_SOCKET_FAMILY_IPV4, G_SOCKET_TYPE_DATAGRAM,
      G_SOCKET_PROTOCOL_UDP, NULL);
  if (socket == NULL) {
    return NULL;
  }

  addr = g_inet_address_new_any (G_SOCKET_FAMILY_IPV4);
  bind_saddr = g_inet_socket_address_new (addr, port);
  g_object_unref (addr);
  if (!g_socket_bind (socket, bind_saddr, TRUE, NULL)) {
    g_socket_close (socket, NULL);
    g_object_unref (socket);
    socket = NULL;
  }
  g_object_unref (bind_saddr);

  return socket;
}

static guint16
kms_socket_get_port (GSocket * socket)
{
  GInetSocketAddress *addr;
  guint16 port;

  addr = G_INET_SOCKET_ADDRESS (g_socket_get_local_address (socket, NULL));
  if (!addr) {
    return 0;
  }

  port = g_inet_socket_address_get_port (addr);
  g_inet_socket_address_get_address (addr);
  g_object_unref (addr);

  return port;
}

static gboolean
kms_rtp_connection_get_rtp_rtcp_sockets (GSocket ** rtp, GSocket ** rtcp)
{
  GSocket *s1, *s2;
  guint16 port1, port2;

  if (rtp == NULL || rtcp == NULL) {
    return FALSE;
  }

  s1 = kms_socket_open (0);

  if (s1 == NULL) {
    return FALSE;
  }

  port1 = kms_socket_get_port (s1);

  if (port1 & 0x01) {
    port2 = port1 - 1;
  } else {
    port2 = port1 + 1;
  }

  s2 = kms_socket_open (port2);

  if (s2 == NULL) {
    kms_socket_finalize (&s1);
    return FALSE;
  }

  if (port1 < port2) {
    *rtp = s1;
    *rtcp = s2;
  } else {
    *rtp = s2;
    *rtcp = s1;
  }

  return TRUE;
}

/* Socket management end */

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

  g_object_set (priv->rtp_udpsink, "host", host, "port", rtp_port, NULL);
  g_object_set (priv->rtcp_udpsink, "host", host, "port", rtcp_port, NULL);
}

static void
kms_rtp_connection_add (KmsIRtpConnection * base_rtp_conn, GstBin * bin,
    gboolean local_offer)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (base_rtp_conn);
  KmsRtpConnectionPrivate *priv = self->priv;

  gst_bin_add_many (bin, g_object_ref (priv->rtp_udpsink),
      g_object_ref (priv->rtp_udpsrc),
      g_object_ref (priv->rtcp_udpsink),
      g_object_ref (priv->rtcp_udpsrc), NULL);

  gst_element_sync_state_with_parent (priv->rtp_udpsink);
  gst_element_sync_state_with_parent (priv->rtp_udpsrc);
  gst_element_sync_state_with_parent (priv->rtcp_udpsink);
  gst_element_sync_state_with_parent (priv->rtcp_udpsrc);
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
    case PROP_CONNECTED:
      self->priv->connected = g_value_get_boolean (value);
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
    case PROP_CONNECTED:
      g_value_set_boolean (value, self->priv->connected);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

KmsRtpConnection *
kms_rtp_connection_new ()
{
  GObject *obj;
  KmsRtpConnection *conn;
  KmsRtpConnectionPrivate *priv;
  gint retries = 0;

  obj = g_object_new (KMS_TYPE_RTP_CONNECTION, NULL);
  conn = KMS_RTP_CONNECTION (obj);
  priv = conn->priv;

  while (!kms_rtp_connection_get_rtp_rtcp_sockets
      (&priv->rtp_socket, &priv->rtcp_socket)
      && retries++ < MAX_RETRIES) {
    GST_WARNING_OBJECT (obj, "Getting ports failed, retring");
  }

  if (priv->rtp_socket == NULL) {
    GST_ERROR_OBJECT (obj, "Cannot get ports");
    g_object_unref (obj);
    return NULL;
  }

  priv->rtp_udpsink = gst_element_factory_make ("udpsink", NULL);
  priv->rtp_udpsrc = gst_element_factory_make ("udpsrc", NULL);
  g_object_set (priv->rtp_udpsink, "socket", priv->rtp_socket,
      "sync", FALSE, "async", FALSE, NULL);
  g_object_set (priv->rtp_udpsrc, "socket", priv->rtp_socket, NULL);

  priv->rtcp_udpsink = gst_element_factory_make ("udpsink", NULL);
  priv->rtcp_udpsrc = gst_element_factory_make ("udpsrc", NULL);
  g_object_set (priv->rtcp_udpsink, "socket", priv->rtcp_socket,
      "sync", FALSE, "async", FALSE, NULL);
  g_object_set (priv->rtcp_udpsrc, "socket", priv->rtcp_socket, NULL);

  kms_i_rtp_connection_connected_signal (KMS_I_RTP_CONNECTION (conn));

  return conn;
}

static void
kms_rtp_connection_finalize (GObject * object)
{
  KmsRtpConnection *self = KMS_RTP_CONNECTION (object);
  KmsRtpConnectionPrivate *priv = self->priv;

  GST_DEBUG_OBJECT (self, "finalize");

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

  g_object_class_override_property (gobject_class, PROP_CONNECTED, "connected");
}

static void
kms_rtp_connection_interface_init (KmsIRtpConnectionInterface * iface)
{
  iface->add = kms_rtp_connection_add;
  iface->request_rtp_sink = kms_rtp_connection_request_rtp_sink;
  iface->request_rtp_src = kms_rtp_connection_request_rtp_src;
  iface->request_rtcp_sink = kms_rtp_connection_request_rtcp_sink;
  iface->request_rtcp_src = kms_rtp_connection_request_rtcp_src;
}
