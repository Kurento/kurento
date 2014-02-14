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

  if (g_socket_is_closed (self->priv->socket)) {
    GST_DEBUG_OBJECT (self, "Socket is already closed");
    goto end;
  }

  if (self->priv->close_socket) {
    GError *err = NULL;

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

static void
gst_sctp_client_sink_class_init (GstSCTPClientSinkClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_sctp_client_sink_set_property;
  gobject_class->get_property = gst_sctp_client_sink_get_property;
  gobject_class->finalize = gst_sctp_base_sink_finalize;
  gobject_class->dispose = gst_sctp_base_sink_dispose;

  gstelement_class = GST_ELEMENT_CLASS (klass);

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

  g_type_class_add_private (klass, sizeof (GstSCTPClientSinkPrivate));
}

static void
gst_sctp_client_sink_init (GstSCTPClientSink * self)
{
  self->priv = GST_SCTP_CLIENT_SINK_GET_PRIVATE (self);
  self->priv->cancellable = g_cancellable_new ();
  self->priv->close_socket = TRUE;
}

gboolean
gst_sctp_client_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_CLIENT_SINK);
}
