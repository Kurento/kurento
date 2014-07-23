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
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netinet/sctp.h>
#include <arpa/inet.h>

#include "gstsctp.h"
#include "gstsctpbasesink.h"

#define PLUGIN_NAME "sctpbasesink"

GST_DEBUG_CATEGORY_STATIC (gst_sctp_base_sink_debug_category);
#define GST_CAT_DEFAULT gst_sctp_base_sink_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPBaseSink, gst_sctp_base_sink,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_base_sink_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define GST_SCTP_BASE_SINK_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_BASE_SINK, GstSCTPBaseSinkPrivate))

struct _GstSCTPBaseSinkPrivate
{
  GRecMutex mutex;

  guint16 num_ostreams;
  guint16 max_istreams;

  /* socket */
  GSocket *socket;
  GCancellable *cancellable;
  guint streams;

  /* server information */
  gint port;
  gchar *host;
};

#define GST_SCTP_BASE_SINK_LOCK(elem) \
  (g_rec_mutex_lock (&GST_SCTP_BASE_SINK ((elem))->priv->mutex))
#define GST_SCTP_BASE_SINK_UNLOCK(elem) \
  (g_rec_mutex_unlock (&GST_SCTP_BASE_SINK ((elem))->priv->mutex))

enum
{
  PROP_0,
  PROP_HOST,
  PROP_PORT,
  PROP_NUM_OSTREAMS,
  PROP_MAX_INSTREAMS
};

static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink_%u",
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS_ANY);

typedef enum
{
  GST_SCTP_BASE_SINK_OPEN = (GST_ELEMENT_FLAG_LAST << 0),
  GST_SCTP_BASE_SINK_FLAG_LAST = (GST_ELEMENT_FLAG_LAST << 2),
} GstSCTPBaseSinkFlags;

static gchar *
get_stream_id_from_padname (const gchar * name)
{
  GMatchInfo *match_info = NULL;
  GRegex *regex;
  gchar *id = NULL;

  if (name == NULL)
    return NULL;

  regex = g_regex_new ("^sink_(?<id>\\d+)", 0, 0, NULL);
  g_regex_match (regex, name, 0, &match_info);

  if (g_match_info_matches (match_info))
    id = g_match_info_fetch_named (match_info, "id");

  g_match_info_free (match_info);
  g_regex_unref (regex);

  return id;
}

static void
gst_sctp_base_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstSCTPBaseSink *self;

  g_return_if_fail (GST_IS_SCTP_BASE_SINK (object));
  self = GST_SCTP_BASE_SINK (object);

  GST_SCTP_BASE_SINK_LOCK (self);

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
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  GST_SCTP_BASE_SINK_UNLOCK (self);
}

static void
gst_sctp_base_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPBaseSink *self;

  g_return_if_fail (GST_IS_SCTP_BASE_SINK (object));
  self = GST_SCTP_BASE_SINK (object);

  GST_SCTP_BASE_SINK_LOCK (self);

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
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  GST_SCTP_BASE_SINK_UNLOCK (self);
}

static void
gst_sctp_base_sink_finalize (GObject * gobject)
{
  GstSCTPBaseSink *self = GST_SCTP_BASE_SINK (gobject);

  g_free (self->priv->host);
  self->priv->host = NULL;

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (gst_sctp_base_sink_parent_class)->finalize (gobject);
}

static GstPad *
gst_sctp_base_sink_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstSCTPBaseSink *self = GST_SCTP_BASE_SINK (element);
  GstPad *sinkpad, *ghostpad;
  GstElement *sctpclientsink;
  gchar *padname;
  gchar *pad_id;
  gint64 val;
  guint16 id;

  GST_SCTP_BASE_SINK_LOCK (self);
  if (self->priv->num_ostreams < self->priv->streams) {
    GST_SCTP_BASE_SINK_UNLOCK (self);
    GST_WARNING ("No more available streams");
    return NULL;
  }

  pad_id = get_stream_id_from_padname (name);
  if (pad_id == NULL) {
    GST_SCTP_BASE_SINK_UNLOCK (self);
    GST_WARNING
        ("Link of elements without using pad names is not yet supported");
    return NULL;
  }

  val = g_ascii_strtoll (pad_id, NULL, 10);
  g_free (pad_id);

  if (val > G_MAXUINT32) {
    GST_SCTP_BASE_SINK_UNLOCK (self);
    GST_ERROR ("SCTP stream id %" G_GINT64_FORMAT " is not valid", val);
    return NULL;
  }

  id = val;

  sctpclientsink = gst_element_factory_make ("sctpclientsink", NULL);
  sinkpad = gst_element_get_static_pad (sctpclientsink, "sink");
  if (sinkpad == NULL) {
    GST_SCTP_BASE_SINK_UNLOCK (self);
    GST_ERROR_OBJECT (sctpclientsink, "Can not get sink pad");
    gst_object_unref (sctpclientsink);
    return NULL;
  }

  g_object_set (sctpclientsink, "stream-id", id, "socket", self->priv->socket,
      NULL);

  gst_bin_add (GST_BIN (element), sctpclientsink);
  gst_element_sync_state_with_parent (sctpclientsink);

  padname = g_strdup_printf ("sink_%u", id);
  ghostpad = gst_ghost_pad_new_from_template (padname, sinkpad, templ);

  g_object_unref (sinkpad);
  g_free (padname);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (ghostpad, TRUE);

  gst_element_add_pad (element, ghostpad);

  self->priv->streams++;
  GST_SCTP_BASE_SINK_UNLOCK (self);

  return ghostpad;
}

static void
gst_sctp_base_sink_release_pad (GstElement * element, GstPad * pad)
{
  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  gst_element_remove_pad (element, pad);
}

static void
gst_sctp_base_sink_destroy_socket (GstSCTPBaseSink * self)
{
  GError *err = NULL;

  if (!GST_OBJECT_FLAG_IS_SET (self, GST_SCTP_BASE_SINK_OPEN))
    return;

  if (self->priv->socket != NULL) {
    GST_DEBUG_OBJECT (self, "closing socket");

    if (!g_socket_close (self->priv->socket, &err)) {
      GST_ERROR_OBJECT (self, "Failed to close socket: %s", err->message);
      g_clear_error (&err);
    }
    g_clear_object (&self->priv->socket);
  }

  GST_OBJECT_FLAG_UNSET (self, GST_SCTP_BASE_SINK_OPEN);
}

/* create a socket for sending to remote machine */
static void
gst_sctp_base_sink_create_socket (GstSCTPBaseSink * self)
{
  GSocketAddress *saddr;
  GResolver *resolver;
  GInetAddress *addr;
  GError *err = NULL;

  if (GST_OBJECT_FLAG_IS_SET (self, GST_SCTP_BASE_SINK_OPEN))
    return;

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

#if defined (SCTP_INITMSG)
  {
    struct sctp_initmsg initmsg;

    memset (&initmsg, 0, sizeof (initmsg));
    initmsg.sinit_num_ostreams = self->priv->num_ostreams;
    initmsg.sinit_max_instreams = self->priv->max_istreams;

    if (setsockopt (g_socket_get_fd (self->priv->socket), IPPROTO_SCTP,
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
  if (!g_socket_connect (self->priv->socket, saddr, self->priv->cancellable,
          &err))
    goto connect_failed;

  g_object_unref (saddr);

  GST_OBJECT_FLAG_SET (self, GST_SCTP_BASE_SINK_OPEN);
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
    /* pretend we opened ok for proper cleanup to happen */
    GST_OBJECT_FLAG_SET (self, GST_SCTP_BASE_SINK_OPEN);
    gst_sctp_base_sink_destroy_socket (self);
    return;
  }
}

static void
gst_sctp_base_sink_configure_sinks (GstSCTPBaseSink * self)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  it = gst_bin_iterate_sinks (GST_BIN (self));
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstElement *sctpclientsink;

        sctpclientsink = g_value_get_object (&val);
        g_object_set (sctpclientsink, "socket", self->priv->socket, NULL);

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink elements",
            GST_ELEMENT_NAME (self));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static GstStateChangeReturn
gst_sctp_base_sink_change_state (GstElement * element,
    GstStateChange transition)
{
  GstSCTPBaseSink *self = GST_SCTP_BASE_SINK (element);
  GstStateChangeReturn ret;

  if (transition == GST_STATE_CHANGE_NULL_TO_READY) {
    GST_SCTP_BASE_SINK_LOCK (self);
    gst_sctp_base_sink_create_socket (self);
    if (!GST_OBJECT_FLAG_IS_SET (self, GST_SCTP_BASE_SINK_OPEN)) {
      GST_SCTP_BASE_SINK_UNLOCK (self);
      return GST_STATE_CHANGE_FAILURE;
    }
    gst_sctp_base_sink_configure_sinks (self);
    GST_SCTP_BASE_SINK_UNLOCK (self);
  }

  /* Let parent class manage the state transition. Parent will initialize  */
  /* children when transitioning from NULL state onward or it will release */
  /* children's resources when transitioning from PLAYING state backwards  */
  ret =
      GST_ELEMENT_CLASS (gst_sctp_base_sink_parent_class)->change_state
      (element, transition);

  if (transition == GST_STATE_CHANGE_READY_TO_NULL) {
    GST_SCTP_BASE_SINK_LOCK (self);
    gst_sctp_base_sink_destroy_socket (self);
    if (GST_OBJECT_FLAG_IS_SET (self, GST_SCTP_BASE_SINK_OPEN)) {
      GST_SCTP_BASE_SINK_UNLOCK (self);
      return GST_STATE_CHANGE_FAILURE;
    }
    GST_SCTP_BASE_SINK_UNLOCK (self);
  }

  return ret;
}

static void
gst_sctp_base_sink_dispose (GObject * gobject)
{
  GstSCTPBaseSink *self = GST_SCTP_BASE_SINK (gobject);

  GST_SCTP_BASE_SINK_LOCK (self);

  if (self->priv->cancellable != NULL) {
    g_cancellable_cancel (self->priv->cancellable);
    g_clear_object (&self->priv->cancellable);
  }

  if (self->priv->socket != NULL)
    gst_sctp_base_sink_destroy_socket (self);

  GST_SCTP_BASE_SINK_UNLOCK (self);

  G_OBJECT_CLASS (gst_sctp_base_sink_parent_class)->dispose (gobject);
}

static void
gst_sctp_base_sink_class_init (GstSCTPBaseSinkClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_sctp_base_sink_set_property;
  gobject_class->get_property = gst_sctp_base_sink_get_property;
  gobject_class->finalize = gst_sctp_base_sink_finalize;
  gobject_class->dispose = gst_sctp_base_sink_dispose;

  g_object_class_install_property (gobject_class, PROP_HOST,
      g_param_spec_string ("host", "Host", "The host/IP to send the packets to",
          SCTP_DEFAULT_HOST, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_PORT,
      g_param_spec_int ("port", "Port", "The port to send the packets to",
          0, G_MAXUINT16, SCTP_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_NUM_OSTREAMS,
      g_param_spec_int ("num-ostreams", "Output streams",
          "This is the number of streams that the application wishes to be "
          "able to send to", 0, G_MAXUINT16, 1,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MAX_INSTREAMS,
      g_param_spec_int ("max-instreams", "Inputput streams",
          "This value represents the maximum number of inbound streams the "
          "application is prepared to support", 0, G_MAXUINT16, 0,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP base sink", "Sink/Network",
      "Send data as a client over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (gst_sctp_base_sink_change_state);
  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (gst_sctp_base_sink_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (gst_sctp_base_sink_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  g_type_class_add_private (klass, sizeof (GstSCTPBaseSinkPrivate));
}

static void
gst_sctp_base_sink_init (GstSCTPBaseSink * self)
{
  self->priv = GST_SCTP_BASE_SINK_GET_PRIVATE (self);
  self->priv->host = g_strdup (SCTP_DEFAULT_HOST);
  self->priv->port = SCTP_DEFAULT_PORT;
  self->priv->cancellable = g_cancellable_new ();

  g_rec_mutex_init (&self->priv->mutex);

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
  GST_OBJECT_FLAG_UNSET (self, GST_SCTP_BASE_SINK_OPEN);
}

gboolean
gst_sctp_base_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_BASE_SINK);
}
