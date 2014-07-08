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
#include <string.h>
#include <netinet/sctp.h>

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
  GCancellable *cancellable;

  /* server information */
  gint port;
  gchar *host;
  guint16 num_ostreams;
  guint16 max_istreams;
  guint32 timetolive;

  GstTask *task;
  GRecMutex mutex;
};

enum
{
  PROP_0,
  PROP_HOST,
  PROP_PORT,
  PROP_NUM_OSTREAMS,
  PROP_MAX_INSTREAMS,
  PROP_TIMETOLIVE
};

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void
gst_sctp_client_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstSCTPClientSink *self;

  g_return_if_fail (GST_IS_SCTP_CLIENT_SINK (object));
  self = GST_SCTP_CLIENT_SINK (object);

  GST_OBJECT_LOCK (self);

  switch (prop_id) {
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
    case PROP_NUM_OSTREAMS:
      self->priv->num_ostreams = g_value_get_int (value);
      break;
    case PROP_MAX_INSTREAMS:
      self->priv->max_istreams = g_value_get_int (value);
      break;
    case PROP_TIMETOLIVE:
      self->priv->timetolive = g_value_get_uint (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  GST_OBJECT_UNLOCK (self);
}

static void
gst_sctp_client_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPClientSink *self;

  g_return_if_fail (GST_IS_SCTP_CLIENT_SINK (object));
  self = GST_SCTP_CLIENT_SINK (object);

  GST_OBJECT_LOCK (self);

  switch (prop_id) {
    case PROP_HOST:
      g_value_set_string (value, self->priv->host);
      break;
    case PROP_PORT:
      g_value_set_int (value, self->priv->port);
      break;
    case PROP_NUM_OSTREAMS:
      g_value_set_int (value, self->priv->num_ostreams);
      break;
    case PROP_MAX_INSTREAMS:
      g_value_set_int (value, self->priv->max_istreams);
      break;
    case PROP_TIMETOLIVE:
      g_value_set_uint (value, self->priv->timetolive);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  GST_OBJECT_UNLOCK (self);
}

static gboolean
gst_sctp_client_sink_disconnect (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);
  GSocket *socket = NULL;
  GError *err = NULL;

  GST_OBJECT_LOCK (self);

  if (self->priv->socket == NULL) {
    GST_OBJECT_UNLOCK (self);
    return TRUE;
  }

  socket = self->priv->socket;
  self->priv->socket = NULL;

  GST_OBJECT_UNLOCK (self);

  if (g_socket_is_closed (socket)) {
    GST_DEBUG_OBJECT (self, "Socket is already closed");
    goto end;
  }

  GST_DEBUG_OBJECT (self, "Closing socket");

  if (!g_socket_close (socket, &err)) {
    GST_ERROR ("Failed to close socket %p: %s", socket, err->message);
    g_clear_error (&err);
  }

end:
  g_clear_object (&socket);

  return TRUE;
}

static gboolean
gst_sctp_client_sink_stop (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);
  GstTask *task;

  GST_DEBUG_OBJECT (self, "stopping");

  gst_sctp_client_sink_disconnect (bsink);

  GST_OBJECT_LOCK (self);

  if ((task = self->priv->task)) {
    self->priv->task = NULL;
    GST_OBJECT_UNLOCK (self);

    gst_task_stop (task);

    /* make sure it is not running */
    g_rec_mutex_lock (&self->priv->mutex);
    g_rec_mutex_unlock (&self->priv->mutex);

    /* now wait for the task to finish */
    gst_task_join (task);

    /* and free the task */
    gst_object_unref (GST_OBJECT (task));

  } else {
    GST_OBJECT_UNLOCK (self);
  }

  return TRUE;
}

static gboolean
gst_sctp_client_sink_connect (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);
  GSocket *socket = NULL;
  GSocketAddress *saddr;
  GResolver *resolver;
  GInetAddress *addr;
  GError *err = NULL;
  gchar *host;
  gint port;
  guint16 ostr, istr;

  GST_OBJECT_LOCK (self);

  if (self->priv->socket != NULL) {
    GST_OBJECT_UNLOCK (self);
    return TRUE;
  }

  host = g_strdup (self->priv->host);
  port = self->priv->port;
  ostr = self->priv->num_ostreams;
  istr = self->priv->max_istreams;

  GST_OBJECT_UNLOCK (self);

  /* look up name if we need to */
  addr = g_inet_address_new_from_string (host);
  if (addr == NULL) {
    GList *results;

    resolver = g_resolver_get_default ();
    results = g_resolver_lookup_by_name (resolver, host,
        self->priv->cancellable, &err);

    if (results == NULL)
      goto name_resolve;

    addr = G_INET_ADDRESS (g_object_ref (results->data));

    g_resolver_free_addresses (results);
    g_object_unref (resolver);
  }

  if (G_UNLIKELY (GST_LEVEL_DEBUG <= _gst_debug_min)) {
    gchar *ip = g_inet_address_to_string (addr);

    GST_DEBUG_OBJECT (self, "IP address for host %s is %s", host, ip);
    g_free (ip);
  }

  saddr = g_inet_socket_address_new (addr, port);
  g_object_unref (addr);

  /* create sending client socket */
  GST_DEBUG_OBJECT (self, "opening sending client socket to %s:%d", host, port);

  socket = g_socket_new (g_socket_address_get_family (saddr),
      G_SOCKET_TYPE_STREAM, G_SOCKET_PROTOCOL_SCTP, &err);

  if (socket == NULL)
    goto no_socket;

  /* TODO: Add support for SCTP Multi-Homing */

#if defined (SCTP_INITMSG)
  {
    struct sctp_initmsg initmsg;

    memset (&initmsg, 0, sizeof (initmsg));
    initmsg.sinit_num_ostreams = ostr;
    initmsg.sinit_max_instreams = istr;

    if (setsockopt (g_socket_get_fd (socket), IPPROTO_SCTP,
            SCTP_INITMSG, &initmsg, sizeof (initmsg)) < 0)
      GST_ELEMENT_WARNING (self, RESOURCE, SETTINGS, (NULL),
          ("Could not configure SCTP socket: %s (%d)", g_strerror (errno),
              errno));
  }
#else
  GST_WARNING_OBJECT (self, "don't know how to configure the SCTP initiation "
      "parameters on this OS.");
#endif

  GST_DEBUG_OBJECT (self, "opened sending client socket");

  /* connect to server */
  if (!g_socket_connect (socket, saddr, self->priv->cancellable, &err))
    goto connect_failed;

  if (G_UNLIKELY (GST_LEVEL_DEBUG <= _gst_debug_min)) {
#if defined (SCTP_INITMSG)
    struct sctp_initmsg initmsg;
    socklen_t optlen;

    if (getsockopt (g_socket_get_fd (socket), IPPROTO_SCTP,
            SCTP_INITMSG, &initmsg, &optlen) < 0)
      GST_ELEMENT_WARNING (self, RESOURCE, SETTINGS, (NULL),
          ("Could not get SCTP configuration: %s (%d)", g_strerror (errno),
              errno));
    else
      GST_DEBUG_OBJECT (self, "SCTP client socket: ostreams %u, instreams %u",
          initmsg.sinit_num_ostreams, initmsg.sinit_num_ostreams);
#else
    GST_WARNING_OBJECT (self, "don't know how to get the configuration of the "
        "SCTP initiation structure on this OS.");
#endif
  }

  g_object_unref (saddr);
  g_free (host);

  GST_OBJECT_LOCK (self);

  if (self->priv->socket == NULL) {
    self->priv->socket = socket;
    GST_OBJECT_UNLOCK (self);
    GST_DEBUG ("Created sctp socket");
  } else {
    GST_OBJECT_UNLOCK (self);
    g_socket_close (socket, NULL);
    g_object_unref (socket);
  }

  return TRUE;

no_socket:
  {
    GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
        ("Failed to create socket: %s", err->message));
    g_clear_error (&err);
    g_object_unref (saddr);
    g_free (host);

    return FALSE;
  }
name_resolve:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled name resolval");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to resolve host '%s': %s", host, err->message));
    }

    g_clear_error (&err);
    g_object_unref (resolver);
    g_free (host);

    return FALSE;
  }
connect_failed:
  {
    if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "Cancelled connecting");
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
          ("Failed to connect to host '%s:%d': %s", host, port, err->message));
    }

    g_free (host);
    g_clear_error (&err);
    g_object_unref (saddr);
    g_clear_object (&socket);

    return FALSE;
  }
}

void
gst_sctp_client_sink_thread (GstSCTPClientSink * self)
{
  /* TODO: manage incoming SCTP stuff */
}

static gboolean
gst_sctp_client_sink_start (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  GST_DEBUG_OBJECT (self, "starting");

  if (!gst_sctp_client_sink_connect (bsink))
    return FALSE;

  GST_OBJECT_LOCK (self);

  if (self->priv->task == NULL) {
    self->priv->task =
        gst_task_new ((GstTaskFunction) gst_sctp_client_sink_thread, self,
        NULL);
    if (self->priv->task == NULL)
      goto task_error;

    gst_task_set_lock (self->priv->task, &self->priv->mutex);
  }

  gst_task_start (self->priv->task);

  GST_OBJECT_UNLOCK (self);

  return TRUE;

  /* ERRORS */
task_error:
  {
    GST_OBJECT_UNLOCK (self);
    GST_ERROR_OBJECT (self, "failed to create task");
    gst_sctp_client_sink_stop (bsink);
    return FALSE;
  }
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
    rret = sctp_socket_send (self->priv->socket, SCTP_DEFAULT_STREAM,
        self->priv->timetolive, (gchar *) map.data + written,
        map.size - written, self->priv->cancellable, &err);

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
gst_sctp_client_sink_dispose (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  g_clear_object (&self->priv->cancellable);
  gst_sctp_client_sink_stop (GST_BASE_SINK (self));

  G_OBJECT_CLASS (gst_sctp_client_sink_parent_class)->dispose (gobject);
}

static void
gst_sctp_client_sink_finalize (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  g_free (self->priv->host);
  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (gst_sctp_client_sink_parent_class)->finalize (gobject);
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
  gobject_class->finalize = gst_sctp_client_sink_finalize;
  gobject_class->dispose = gst_sctp_client_sink_dispose;

  gstelement_class = GST_ELEMENT_CLASS (klass);

  g_object_class_install_property (gobject_class, PROP_HOST,
      g_param_spec_string ("host", "Host", "The host/IP to send the packets to",
          SCTP_DEFAULT_HOST,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_PORT,
      g_param_spec_int ("port", "Port", "The port to send the packets to",
          0, G_MAXUINT16, SCTP_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
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
  g_object_class_install_property (gobject_class, PROP_TIMETOLIVE,
      g_param_spec_uint ("timetolive", "Time to live",
          "The message time to live in milliseconds (0 = no timeout)", 0,
          G_MAXUINT32, 0,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

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

  g_rec_mutex_init (&self->priv->mutex);
}

gboolean
gst_sctp_client_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_CLIENT_SINK);
}
