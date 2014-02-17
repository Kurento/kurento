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
#include "gstsctpserversrc.h"

#define SCTP_BACKLOG 1          /* client connection queue */

#define PLUGIN_NAME "sctpserversrc"

GST_DEBUG_CATEGORY_STATIC (gst_sctp_server_src_debug_category);
#define GST_CAT_DEFAULT gst_sctp_server_src_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPServerSrc, gst_sctp_server_src,
    GST_TYPE_PUSH_SRC,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_server_src_debug_category, PLUGIN_NAME,
        0, "debug category for sctp server source"));

#define GST_SCTP_SERVER_SRC_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_SERVER_SRC, GstSCTPServerSrcPrivate))

struct _GstSCTPServerSrcPrivate
{
  /* socket */
  GSocket *server_socket;
  GSocket *client_socket;
  GCancellable *cancellable;

  /* server information */
  int current_port;             /* currently bound-to port, or 0 *//* ATOMIC */
  int server_port;              /* port property */
  gchar *host;
};

enum
{
  PROP_0,
  PROP_HOST,
  PROP_PORT,
  PROP_CURRENT_PORT
};

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void
gst_sctp_server_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstSCTPServerSrc *self;

  g_return_if_fail (GST_IS_SCTP_SERVER_SRC (object));
  self = GST_SCTP_SERVER_SRC (object);

  switch (prop_id) {
    case PROP_HOST:
      if (!g_value_get_string (value)) {
        GST_WARNING ("host property cannot be NULL");
        break;
      }
      g_free (self->priv->host);
      self->priv->host = g_strdup (g_value_get_string (value));
      break;
    case PROP_PORT:
      self->priv->server_port = g_value_get_int (value);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_server_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPServerSrc *self;

  g_return_if_fail (GST_IS_SCTP_SERVER_SRC (object));
  self = GST_SCTP_SERVER_SRC (object);

  switch (prop_id) {
    case PROP_HOST:
      g_value_set_string (value, self->priv->host);
      break;
    case PROP_PORT:
      g_value_set_int (value, self->priv->server_port);
      break;
    case PROP_CURRENT_PORT:
      g_value_set_int (value, g_atomic_int_get (&self->priv->current_port));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_server_src_dispose (GObject * gobject)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (gobject);

  g_clear_object (&self->priv->cancellable);
}

static void
gst_sctp_server_src_finalize (GObject * gobject)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (gobject);

  g_free (self->priv->host);
}

static gboolean
gst_sctp_server_src_stop (GstBaseSrc * bsrc)
{
  /* TODO */
  return FALSE;
}

/* set up server */
static gboolean
gst_sctp_server_src_start (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);
  GError *err = NULL;
  GInetAddress *addr;
  GSocketAddress *saddr;
  GResolver *resolver;
  gint bound_port = 0;

  /* look up name if we need to */
  addr = g_inet_address_new_from_string (self->priv->host);
  if (!addr) {
    GList *results;

    resolver = g_resolver_get_default ();

    results =
        g_resolver_lookup_by_name (resolver, self->priv->host,
        self->priv->cancellable, &err);

    if (!results)
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

  saddr = g_inet_socket_address_new (addr, self->priv->server_port);
  g_object_unref (addr);

  /* create the server listener socket */
  self->priv->server_socket =
      g_socket_new (g_socket_address_get_family (saddr), G_SOCKET_TYPE_STREAM,
      G_SOCKET_PROTOCOL_SCTP, &err);

  if (!self->priv->server_socket)
    goto no_socket;

  GST_DEBUG_OBJECT (self, "opened receiving server socket");

  /* TODO: Set SCTP options */

  /* bind it */
  GST_DEBUG_OBJECT (self, "binding server socket to address");

  if (!g_socket_bind (self->priv->server_socket, saddr, TRUE, &err))
    goto bind_failed;

  g_object_unref (saddr);

  GST_DEBUG_OBJECT (self, "listening on server socket");

  g_socket_set_listen_backlog (self->priv->server_socket, SCTP_BACKLOG);

  if (!g_socket_listen (self->priv->server_socket, &err))
    goto listen_failed;

  if (self->priv->server_port == 0) {
    saddr = g_socket_get_local_address (self->priv->server_socket, NULL);
    bound_port = g_inet_socket_address_get_port ((GInetSocketAddress *) saddr);
    g_object_unref (saddr);
  } else {
    bound_port = self->priv->server_port;
  }

  GST_DEBUG_OBJECT (self, "listening on port %d", bound_port);

  g_atomic_int_set (&self->priv->current_port, bound_port);
  g_object_notify (G_OBJECT (self), "current-port");

  return TRUE;

  /* ERRORS */
no_socket:
  {
    GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
        ("Failed to create socket: %s", err->message));
    g_clear_error (&err);
    g_object_unref (saddr);
    return FALSE;
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
    return FALSE;
  }
bind_failed:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled binding");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to bind on host '%s:%d': %s", self->priv->host,
              self->priv->server_port, err->message));
    }
    g_clear_error (&err);
    g_object_unref (saddr);
    gst_sctp_server_src_stop (GST_BASE_SRC (self));
    return FALSE;
  }
listen_failed:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled listening");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to listen on host '%s:%d': %s", self->priv->host,
              self->priv->server_port, err->message));
    }
    g_clear_error (&err);
    gst_sctp_server_src_stop (GST_BASE_SRC (self));
    return FALSE;
  };
}

/* will be called only between calls to start() and stop() */
static gboolean
gst_sctp_server_src_unlock (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);

  g_cancellable_cancel (self->priv->cancellable);

  return TRUE;
}

static gboolean
gst_sctp_server_src_unlock_stop (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);

  g_cancellable_reset (self->priv->cancellable);

  return TRUE;
}

static GstFlowReturn
gst_sctp_server_src_create (GstPushSrc * psrc, GstBuffer ** outbuf)
{
  /* TODO */
  return GST_FLOW_ERROR;
}

static void
gst_sctp_server_src_class_init (GstSCTPServerSrcClass * klass)
{
  GstPushSrcClass *gstpush_src_class;
  GstElementClass *gstelement_class;
  GstBaseSrcClass *gstbasesrc_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_sctp_server_src_set_property;
  gobject_class->get_property = gst_sctp_server_src_get_property;
  gobject_class->finalize = gst_sctp_server_src_finalize;
  gobject_class->dispose = gst_sctp_server_src_dispose;

  g_object_class_install_property (gobject_class, PROP_HOST,
      g_param_spec_string ("bind-address", "Bind Address",
          "The address to bind the socket to",
          SCTP_DEFAULT_HOST,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_PORT,
      g_param_spec_int ("port", "Port",
          "The port to listen to (0=random available port)",
          0, G_MAXUINT16, SCTP_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_CURRENT_PORT,
      g_param_spec_int ("current-port", "current-port",
          "The port number the socket is currently bound to", 0,
          G_MAXUINT16, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP server source", "Source/Network",
      "Receive data as a server over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));

  gstbasesrc_class = GST_BASE_SRC_CLASS (klass);
  gstbasesrc_class->start = gst_sctp_server_src_start;
  gstbasesrc_class->stop = gst_sctp_server_src_stop;
  gstbasesrc_class->unlock = gst_sctp_server_src_unlock;
  gstbasesrc_class->unlock_stop = gst_sctp_server_src_unlock_stop;

  gstpush_src_class = GST_PUSH_SRC_CLASS (klass);
  gstpush_src_class->create = gst_sctp_server_src_create;

  g_type_class_add_private (klass, sizeof (GstSCTPServerSrcPrivate));
}

static void
gst_sctp_server_src_init (GstSCTPServerSrc * self)
{
  self->priv = GST_SCTP_SERVER_SRC_GET_PRIVATE (self);
  self->priv->cancellable = g_cancellable_new ();
}

gboolean
gst_sctp_server_src_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_SERVER_SRC);
}
