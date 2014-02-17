/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gio/gio.h>

#include "gstsctp.h"
#include "gstsctpclientsink.h"

#define PLUGIN_NAME "sctpclientsink"

GST_DEBUG_CATEGORY_STATIC (gst_sctp_client_sink_debug_category);
#define GST_CAT_DEFAULT gst_sctp_client_sink_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPClientSink, gst_sctp_client_sink,
    GST_TYPE_BASE_SINK,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_client_sink_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define GST_SCTP_CLIENT_SINK_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_CLIENT_SINK, GstSCTPClientSinkPrivate))

struct _GstSCTPClientSinkPrivate
{
  /* socket */
  GSocket *socket;
  guint streamid;
  gboolean owner;
  gboolean close_socket;
  GCancellable *cancellable;

  /* server information */
  gint port;
  gchar *host;
};

enum
{
  PROP_0,
  PROP_CLOSE_SOCKET,
  PROP_HOST,
  PROP_PORT,
  PROP_SOCKET,
  PROP_STREAM_ID
};

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void
gst_sctp_client_sink_destroy_socket (GstSCTPClientSink * self)
{
  if (self->priv->socket == NULL)
    return;

  if (self->priv->owner || self->priv->close_socket) {
    GError *err = NULL;

    if (g_socket_is_closed (self->priv->socket)) {
      GST_DEBUG_OBJECT (self, "Socket is already closed");
      goto end;
    }

    GST_DEBUG_OBJECT (self, "Closing socket");

    if (!g_socket_close (self->priv->socket, &err)) {
      GST_ERROR ("Failed to close socket %p: %s", self->priv->socket,
          err->message);
      g_clear_error (&err);
    }
  }

end:
  g_clear_object (&self->priv->socket);
}

static void
gst_sctp_base_sink_create_socket (GstSCTPClientSink * self)
{
  GSocketAddress *saddr;
  GResolver *resolver;
  GInetAddress *addr;
  GError *err = NULL;

  /* look up name if we need to */
  addr = g_inet_address_new_from_string (self->priv->host);
  if (addr == NULL) {
    GList *results;

    resolver = g_resolver_get_default ();
    results = g_resolver_lookup_by_name (resolver, self->priv->host,
        self->priv->cancellable, &err);

    if (results == NULL)
      goto name_resolve;

    addr = G_INET_ADDRESS (g_object_ref (results->data));

    g_resolver_free_addresses (results);
    g_object_unref (resolver);
  }

  if (G_UNLIKELY (GST_LEVEL_DEBUG <= _gst_debug_min)) {
    gchar *ip = g_inet_address_to_string (addr);

    GST_DEBUG_OBJECT (self, "IP address for host %s is %s", self->priv->host,
        ip);
    g_free (ip);
  }

  saddr = g_inet_socket_address_new (addr, self->priv->port);
  g_object_unref (addr);

  /* create sending client socket */
  GST_DEBUG_OBJECT (self, "opening sending client socket to %s:%d",
      self->priv->host, self->priv->port);

  self->priv->socket = g_socket_new (g_socket_address_get_family (saddr),
      G_SOCKET_TYPE_STREAM, G_SOCKET_PROTOCOL_SCTP, &err);
  if (self->priv->socket == NULL)
    goto no_socket;

  GST_DEBUG_OBJECT (self, "opened sending client socket");

  /* connect to server */
  if (!g_socket_connect (self->priv->socket, saddr, self->priv->cancellable,
          &err))
    goto connect_failed;

  g_object_unref (saddr);

  GST_DEBUG ("Created sctp socket");

  return;

no_socket:
  {
    GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
        ("Failed to create socket: %s", err->message));
    g_clear_error (&err);
    g_object_unref (saddr);

    return;
  }
name_resolve:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled name resolval");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to resolve host '%s': %s", self->priv->host, err->message));
    }
    g_clear_error (&err);
    g_object_unref (resolver);

    return;
  }
connect_failed:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled connecting");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to connect to host '%s:%d': %s", self->priv->host,
              self->priv->port, err->message));
    }
    g_clear_error (&err);
    g_object_unref (saddr);
    gst_sctp_client_sink_destroy_socket (self);

    return;
  }
}

static void
gst_sctp_client_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstSCTPClientSink *self;

  g_return_if_fail (GST_IS_SCTP_CLIENT_SINK (object));
  self = GST_SCTP_CLIENT_SINK (object);

  switch (prop_id) {
    case PROP_CLOSE_SOCKET:
      self->priv->close_socket = g_value_get_boolean (value);
      break;
    case PROP_HOST:
      if (g_value_get_string (value) == NULL) {
        GST_WARNING ("host property cannot be NULL");
        break;
      }
      g_free (self->priv->host);
      self->priv->host = g_strdup (g_value_get_string (value));
      break;
    case PROP_PORT:
      self->priv->port = g_value_get_int (value);
      break;
    case PROP_SOCKET:
      gst_sctp_client_sink_destroy_socket (self);
      self->priv->socket = g_value_dup_object (value);
      GST_DEBUG ("setting socket to %p", self->priv->socket);
      break;
    case PROP_STREAM_ID:
      self->priv->streamid = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_client_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPClientSink *self;

  g_return_if_fail (GST_IS_SCTP_CLIENT_SINK (object));
  self = GST_SCTP_CLIENT_SINK (object);

  switch (prop_id) {
    case PROP_CLOSE_SOCKET:
      g_value_set_boolean (value, self->priv->close_socket);
      break;
    case PROP_HOST:
      g_value_set_string (value, self->priv->host);
      break;
    case PROP_PORT:
      g_value_set_int (value, self->priv->port);
      break;
    case PROP_SOCKET:
      g_value_set_object (value, self->priv->socket);
      break;
    case PROP_STREAM_ID:
      g_value_set_int (value, self->priv->streamid);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_base_sink_dispose (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  g_clear_object (&self->priv->cancellable);
  gst_sctp_client_sink_destroy_socket (self);
}

static void
gst_sctp_base_sink_finalize (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  g_free (self->priv->host);
}

static gboolean
gst_sctp_client_sink_start (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  if (self->priv->socket == NULL) {
    gst_sctp_base_sink_create_socket (self);
    g_return_val_if_fail (self->priv->socket != NULL, FALSE);
    self->priv->owner = TRUE;
  }

  return g_socket_is_connected (self->priv->socket);
}

static gboolean
gst_sctp_client_sink_stop (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  gst_sctp_client_sink_destroy_socket (self);
  return TRUE;
}

/* will be called only between calls to start() and stop() */
static gboolean
gst_sctp_client_sink_unlock (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  GST_DEBUG_OBJECT (self, "set to flushing");
  g_cancellable_cancel (self->priv->cancellable);
  return TRUE;
}

static gboolean
gst_sctp_client_sink_unlock_stop (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  GST_DEBUG_OBJECT (self, "unset flushing");
  g_cancellable_reset (self->priv->cancellable);
  return TRUE;
}

static GstFlowReturn
gst_sctp_client_sink_render (GstBaseSink * bsink, GstBuffer * buf)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);
  GstMapInfo map;
  gsize written = 0;
  gssize rret;
  GError *err = NULL;

  g_return_val_if_fail (g_socket_is_connected (self->priv->socket),
      GST_FLOW_FLUSHING);

  gst_buffer_map (buf, &map, GST_MAP_READ);
  GST_LOG_OBJECT (self, "writing %" G_GSIZE_FORMAT " bytes for buffer data",
      map.size);

  /* write buffer data */
  while (written < map.size) {
    rret =
        sctp_socket_send (self->priv->socket, self->priv->streamid,
        (gchar *) map.data + written, map.size - written,
        self->priv->cancellable, &err);

    if (rret < 0)
      goto write_error;

    written += rret;
  }

  gst_buffer_unmap (buf, &map);

  return GST_FLOW_OK;

  /* ERRORS */
write_error:
  {
    GstFlowReturn ret;

    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      ret = GST_FLOW_FLUSHING;
      GST_DEBUG_OBJECT (self, "Cancelled reading from socket");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, WRITE,
          (("Error while sending data to \"%s:%d\"."), self->priv->host,
              self->priv->port), ("Only %" G_GSIZE_FORMAT " of %" G_GSIZE_FORMAT
              " bytes written: %s", written, map.size, err->message));
      ret = GST_FLOW_ERROR;
    }
    gst_buffer_unmap (buf, &map);
    g_clear_error (&err);
    return ret;
  }
}

static void
gst_sctp_client_sink_class_init (GstSCTPClientSinkClass * klass)
{
  GstBaseSinkClass *gstbasesink_class;
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_sctp_client_sink_set_property;
  gobject_class->get_property = gst_sctp_client_sink_get_property;
  gobject_class->finalize = gst_sctp_base_sink_finalize;
  gobject_class->dispose = gst_sctp_base_sink_dispose;

  gstelement_class = GST_ELEMENT_CLASS (klass);

  g_object_class_install_property (gobject_class, PROP_CLOSE_SOCKET,
      g_param_spec_boolean ("close-socket", "Close socket",
          "Close socket if passed as property on state change",
          SCTP_DEFAULT_CLOSE_SOCKET,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_HOST,
      g_param_spec_string ("host", "Host", "The host/IP to send the packets to",
          SCTP_DEFAULT_HOST, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_PORT,
      g_param_spec_int ("port", "Port", "The port to send the packets to",
          0, G_MAXUINT16, SCTP_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_SOCKET,
      g_param_spec_object ("socket", "Socket",
          "Socket to use for SCTP connection. (NULL == allocate)",
          G_TYPE_SOCKET, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_STREAM_ID,
      g_param_spec_int ("stream-id", "Stream identifier",
          "Identifies the stream to which the following user data belongs",
          STREAM_ID_LOWEST, STREAM_ID_HIHEST, DEFAULT_STREAM_ID,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP client sink", "Sink/Network",
      "Provides data associated to a stream id to be sent over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));

  gstbasesink_class = GST_BASE_SINK_CLASS (klass);

  gstbasesink_class->start = gst_sctp_client_sink_start;
  gstbasesink_class->stop = gst_sctp_client_sink_stop;
  gstbasesink_class->render = gst_sctp_client_sink_render;
  gstbasesink_class->unlock = gst_sctp_client_sink_unlock;
  gstbasesink_class->unlock_stop = gst_sctp_client_sink_unlock_stop;

  g_type_class_add_private (klass, sizeof (GstSCTPClientSinkPrivate));
}

static void
gst_sctp_client_sink_init (GstSCTPClientSink * self)
{
  self->priv = GST_SCTP_CLIENT_SINK_GET_PRIVATE (self);
  self->priv->cancellable = g_cancellable_new ();
  self->priv->host = g_strdup (SCTP_DEFAULT_HOST);
  self->priv->port = SCTP_DEFAULT_PORT;
  self->priv->close_socket = SCTP_DEFAULT_CLOSE_SOCKET;
  self->priv->owner = FALSE;
}

gboolean
gst_sctp_client_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_CLIENT_SINK);
}
