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
#define MAX_READ_SIZE (4 * 1024)

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
  guint16 num_ostreams;
  guint16 max_istreams;
};

enum
{
  PROP_0,
  PROP_HOST,
  PROP_PORT,
  PROP_CURRENT_PORT,
  PROP_NUM_OSTREAMS,
  PROP_MAX_INSTREAMS
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
    case PROP_NUM_OSTREAMS:
      self->priv->num_ostreams = g_value_get_int (value);
      break;
    case PROP_MAX_INSTREAMS:
      self->priv->max_istreams = g_value_get_int (value);
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
    case PROP_NUM_OSTREAMS:
      g_value_set_int (value, self->priv->num_ostreams);
      break;
    case PROP_MAX_INSTREAMS:
      g_value_set_int (value, self->priv->max_istreams);
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
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);
  GError *err = NULL;

  if (self->priv->client_socket != NULL) {
    GST_DEBUG_OBJECT (self, "closing client socket");

    if (!g_socket_close (self->priv->client_socket, &err)) {
      GST_ERROR_OBJECT (self, "Failed to close socket: %s", err->message);
      g_clear_error (&err);
    }
    g_object_unref (self->priv->client_socket);
    self->priv->client_socket = NULL;
  }

  if (self->priv->server_socket != NULL) {
    GST_DEBUG_OBJECT (self, "closing server socket");

    if (!g_socket_close (self->priv->server_socket, &err)) {
      GST_ERROR_OBJECT (self, "Failed to close socket: %s", err->message);
      g_clear_error (&err);
    }
    g_object_unref (self->priv->server_socket);
    self->priv->server_socket = NULL;

    g_atomic_int_set (&self->priv->current_port, 0);
    g_object_notify (G_OBJECT (self), "current-port");
  }

  return TRUE;
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

  /* TODO: Add support for SCTP Multi-Homing */

#if defined (SCTP_INITMSG)
  {
    struct sctp_initmsg initmsg;

    memset (&initmsg, 0, sizeof (initmsg));
    initmsg.sinit_num_ostreams = self->priv->num_ostreams;
    initmsg.sinit_max_instreams = self->priv->max_istreams;

    if (setsockopt (g_socket_get_fd (self->priv->server_socket), IPPROTO_SCTP,
            SCTP_INITMSG, &initmsg, sizeof (initmsg)) < 0)
      GST_ELEMENT_WARNING (self, RESOURCE, SETTINGS, (NULL),
          ("Could not configure SCTP socket: %s (%d)", g_strerror (errno),
              errno));
  }
#else
  GST_WARNING_OBJECT (self, "don't know how to configure the SCTP initiation "
      "parameters on this OS.");
#endif

  GST_DEBUG_OBJECT (self, "opened receiving server socket");

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
  GstFlowReturn ret = GST_FLOW_OK;
  GIOCondition condition;
  GstSCTPServerSrc *self;
  GError *err = NULL;
  guint streamid;
  GstMapInfo map;
  gssize rret;

  self = GST_SCTP_SERVER_SRC (psrc);

  if (self->priv->server_socket == NULL)
    goto wrong_state;

  if (self->priv->client_socket == NULL) {
    /* wait on server socket for connections */
    self->priv->client_socket =
        g_socket_accept (self->priv->server_socket, self->priv->cancellable,
        &err);
    if (self->priv->client_socket == NULL)
      goto accept_error;
#if defined (SCTP_EVENTS)
    {
      struct sctp_event_subscribe events;

      memset (&events, 0, sizeof (events));
      events.sctp_data_io_event = 1;
      setsockopt (g_socket_get_fd (self->priv->client_socket), SOL_SCTP,
          SCTP_EVENTS, &events, sizeof (events));
    }
#else
    GST_WARNING_OBJECT (self, "don't know how to configure SCTP events "
        "on this OS.");
#endif
    /* now read from the socket. */
  }

  /* if we have a client, wait for reading */
  GST_LOG_OBJECT (self, "asked for a buffer");

  if (!g_socket_condition_wait (self->priv->client_socket,
          G_IO_IN | G_IO_PRI | G_IO_ERR | G_IO_HUP, self->priv->cancellable,
          &err))
    goto select_error;

  condition = g_socket_condition_check (self->priv->client_socket,
      G_IO_IN | G_IO_PRI | G_IO_ERR | G_IO_HUP);

  if ((condition & G_IO_ERR)) {
    GST_ELEMENT_ERROR (self, RESOURCE, READ, (NULL), ("Socket in error state"));
    *outbuf = NULL;
    ret = GST_FLOW_ERROR;
    goto done;
  } else if ((condition & G_IO_HUP)) {
    GST_DEBUG_OBJECT (self, "Connection closed");
    *outbuf = NULL;
    ret = GST_FLOW_EOS;
    goto done;
  }

  *outbuf = gst_buffer_new_and_alloc (MAX_READ_SIZE);
  gst_buffer_map (*outbuf, &map, GST_MAP_READWRITE);

  rret = sctp_socket_receive (self->priv->client_socket, (gchar *) map.data,
      MAX_READ_SIZE, self->priv->cancellable, &streamid, &err);

  if (rret == 0) {
    GST_DEBUG_OBJECT (self, "Connection closed");
    ret = GST_FLOW_EOS;
    if (*outbuf != NULL) {
      gst_buffer_unmap (*outbuf, &map);
      gst_buffer_unref (*outbuf);
    }
    *outbuf = NULL;
  } else if (rret < 0) {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      ret = GST_FLOW_FLUSHING;
      GST_DEBUG_OBJECT (self, "Cancelled reading from socket");
    } else {
      ret = GST_FLOW_ERROR;
      GST_ELEMENT_ERROR (self, RESOURCE, READ, (NULL),
          ("Failed to read from socket: %s", err->message));
    }
    gst_buffer_unmap (*outbuf, &map);
    gst_buffer_unref (*outbuf);
    *outbuf = NULL;
  } else {
    ret = GST_FLOW_OK;
    gst_buffer_unmap (*outbuf, &map);
    gst_buffer_resize (*outbuf, 0, rret);

    GST_DEBUG_OBJECT (self, "Got buffer %" GST_PTR_FORMAT
        " on stream %u", *outbuf, streamid);
  }
  g_clear_error (&err);

done:
  return ret;

wrong_state:
  {
    GST_DEBUG_OBJECT (self, "connection to closed, cannot read data");
    return GST_FLOW_FLUSHING;
  }
accept_error:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled accepting of client");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to accept client: %s", err->message));
    }
    g_clear_error (&err);
    return GST_FLOW_ERROR;
  }
select_error:
  {
    GST_ELEMENT_ERROR (self, RESOURCE, READ, (NULL),
        ("Select failed: %s", err->message));
    g_clear_error (&err);
    return GST_FLOW_ERROR;
  }
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
  g_object_class_install_property (gobject_class, PROP_NUM_OSTREAMS,
      g_param_spec_int ("num-ostreams", "Output streams",
          "This is the number of streams that the application wishes to be "
          "able to send to", 0, G_MAXUINT16, 1,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MAX_INSTREAMS,
      g_param_spec_int ("max-instreams", "Inputput streams",
          "This value represents the maximum number of inbound streams the "
          "application is prepared to support", 0, G_MAXUINT16, 1,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

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
