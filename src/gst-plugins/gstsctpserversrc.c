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
#include "gstsctpserversrc.h"
#include "kmssctpserverrpc.h"

#define SCTP_BACKLOG 1          /* client connection queue */
#define MAX_BUFFER_SIZE (1024 * 16)

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

  KmsSCTPServerRPC *serverrpc;
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

  GST_OBJECT_LOCK (self);

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

  GST_OBJECT_UNLOCK (self);
}

static void
gst_sctp_server_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPServerSrc *self;

  g_return_if_fail (GST_IS_SCTP_SERVER_SRC (object));
  self = GST_SCTP_SERVER_SRC (object);

  GST_OBJECT_LOCK (self);

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

  GST_OBJECT_UNLOCK (self);
}

static void
gst_sctp_server_src_dispose (GObject * gobject)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (gobject);

  g_clear_object (&self->priv->cancellable);
  g_clear_object (&self->priv->serverrpc);

  G_OBJECT_CLASS (gst_sctp_server_src_parent_class)->dispose (gobject);
}

static void
gst_sctp_server_src_finalize (GObject * gobject)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (gobject);

  g_free (self->priv->host);

  G_OBJECT_CLASS (gst_sctp_server_src_parent_class)->finalize (gobject);
}

static gboolean
gst_sctp_server_src_stop (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);

  GST_DEBUG ("stopping");

  g_cancellable_cancel (self->priv->cancellable);
  kms_sctp_server_rpc_stop (self->priv->serverrpc);

  return TRUE;
}

/* set up server */
static gboolean
gst_sctp_server_src_start (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);
  GError *err = NULL;

  GST_DEBUG ("starting");

  if (kms_sctp_server_rpc_start (self->priv->serverrpc, self->priv->host,
          self->priv->server_port, self->priv->cancellable, &err)) {
    return TRUE;
  }

  GST_ELEMENT_ERROR (self, RESOURCE, OPEN_READ, (NULL),
      ("Error: %s", err->message));

  g_error_free (err);

  return FALSE;
}

/* will be called only between calls to start() and stop() */
static gboolean
gst_sctp_server_src_unlock (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);

  GST_DEBUG ("unlock");
  g_cancellable_cancel (self->priv->cancellable);

  return TRUE;
}

static gboolean
gst_sctp_server_src_unlock_stop (GstBaseSrc * bsrc)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (bsrc);

  GST_DEBUG ("unlock_stop");
  g_cancellable_reset (self->priv->cancellable);

  return TRUE;
}

static GstFlowReturn
gst_sctp_server_src_create (GstPushSrc * psrc, GstBuffer ** outbuf)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (psrc);
  GstFlowReturn ret;
  GError *err = NULL;

  if (kms_sctp_server_rpc_get_buffer (self->priv->serverrpc, outbuf, &err)) {
    GST_DEBUG ("Buffer %" GST_PTR_FORMAT, *outbuf);
    return GST_FLOW_OK;
  }

  if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CLOSED)) {
    ret = GST_FLOW_EOS;
    GST_DEBUG_OBJECT (self, "Connection closed");
  } else if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
    ret = GST_FLOW_FLUSHING;
    GST_DEBUG_OBJECT (self, "Cancelled reading from socket");
  } else {
    ret = GST_FLOW_ERROR;
    GST_ELEMENT_ERROR (self, RESOURCE, READ, (NULL),
          ("Failed to read from socket: %s", err->message));
  }

  g_clear_error (&err);

  return ret;
}

static gboolean
gst_sctp_server_sink_query (GstBaseSrc * src, GstQuery * query)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (src);
  GstQuery *rsp_query = NULL;
  GError *err = NULL;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
    case GST_QUERY_URI:
      break;
    default:
      GST_WARNING ("Not propagated query: %" GST_PTR_FORMAT, query);

      return GST_BASE_SRC_CLASS (gst_sctp_server_src_parent_class)->query (src,
          query);
  }

  GST_DEBUG (">> %" GST_PTR_FORMAT, query);

  if (!kms_scp_base_rpc_query (KMS_SCTP_BASE_RPC (self->priv->serverrpc),
      query, self->priv->cancellable, &rsp_query, &err)) {
    GST_WARNING_OBJECT (self, "Error: %s", err->message);
    g_error_free (err);
    return FALSE;
  }

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:{
      GstCaps *caps, *copy;

      gst_query_parse_caps_result (rsp_query, &caps);
      copy = gst_caps_copy (caps);

      gst_query_set_caps_result (query, copy);
      gst_caps_unref (copy);

      break;
    }
    case GST_QUERY_ACCEPT_CAPS:{
      gboolean result;

      gst_query_parse_accept_caps_result (rsp_query, &result);
      gst_query_set_accept_caps_result (query, result);

      break;
    }
    case GST_QUERY_URI:{
      gchar *uri;

      gst_query_parse_uri (rsp_query, &uri);
      gst_query_set_uri (query, uri);
      g_free (uri);

      break;
    }
    default: {
      GST_ERROR("Unexpected response %" GST_PTR_FORMAT, query);
      gst_query_unref (rsp_query);

      return FALSE;
    }
  }

  gst_query_unref (rsp_query);

  GST_DEBUG ("<< %" GST_PTR_FORMAT, query);

  return TRUE;
}

static gboolean
gst_sctp_server_sink_event (GstBaseSrc * src, GstEvent * event)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (src);
  GError *err = NULL;
  gboolean ret, upstream;

  switch (GST_EVENT_TYPE (event)) {
    /* bidirectional events */
    case GST_EVENT_FLUSH_START:
    case GST_EVENT_FLUSH_STOP:

    /* upstream events */
    case GST_EVENT_QOS:
    case GST_EVENT_SEEK:
    case GST_EVENT_NAVIGATION:
      /* A navigation event is generated by a sink element to signal */
      /* the elements of a navigation event such as a mouse movement */
      /* or button click. They are composed by a GstStructure, so if */
      /* they conain any not marshallable field we won't be able to  */
      /* send it upstream. There is not harm in trying it */
    case GST_EVENT_LATENCY:
    case GST_EVENT_STEP:
    case GST_EVENT_RECONFIGURE:
      /* Base class is capable of managing above events for us, but */
      /* we still need to propagate them to the downstream pipeline */
    case GST_EVENT_TOC_SELECT:
    case GST_EVENT_CUSTOM_UPSTREAM: {
      /* Propagation of custom events may result in an error if they */
      /* use a not marshallable value in the internal GstStructure.  */
      upstream = TRUE;
      break;
    }
    default: {
      GST_WARNING ("Not propagated event >> %" GST_PTR_FORMAT, event);
      upstream = FALSE;
      break;
    }
  }

  ret =  GST_BASE_SRC_CLASS (gst_sctp_server_src_parent_class)->event (src,
          event);

  if (!upstream)
    return ret;

  GST_DEBUG (">> %" GST_PTR_FORMAT, event);

  if (!kms_scp_base_rpc_event (KMS_SCTP_BASE_RPC (self->priv->serverrpc),
          event, self->priv->cancellable, &err)) {
    GST_ERROR_OBJECT (self, "Error: %s", err->message);
    g_error_free (err);
  }

  return ret;
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
  gstbasesrc_class->query = gst_sctp_server_sink_query;
  gstbasesrc_class->event = gst_sctp_server_sink_event;

  gstpush_src_class = GST_PUSH_SRC_CLASS (klass);
  gstpush_src_class->create = gst_sctp_server_src_create;

  g_type_class_add_private (klass, sizeof (GstSCTPServerSrcPrivate));
}

static void
gst_sctp_server_src_remote_query (GstQuery * query, GstSCTPServerSrc * self)
{
  GST_DEBUG_OBJECT (self, ">> %" GST_PTR_FORMAT, query);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
    case GST_QUERY_URI:
      gst_pad_peer_query (GST_BASE_SRC_PAD (GST_BASE_SRC (self)), query);
      break;
    default:
      GST_WARNING ("Unsupported query %" GST_PTR_FORMAT, query);
      return;
  }

  GST_DEBUG_OBJECT (self, "<< %" GST_PTR_FORMAT, query);
}

static void
gst_sctp_server_src_remote_event (GstEvent * event, GstSCTPServerSrc * self)
{
  switch (GST_EVENT_TYPE (event)) {
    /* bidirectional events */
    case GST_EVENT_FLUSH_START:
    case GST_EVENT_FLUSH_STOP:
      /* base class will manage above events for us */
      if (!gst_element_send_event (GST_ELEMENT(self), event)) {
        GST_WARNING_OBJECT (self, "Could not manage remote event %"
          GST_PTR_FORMAT, event);
      }

      break;

    /* downstream events */
    case GST_EVENT_STREAM_START:
    case GST_EVENT_CAPS:
    case GST_EVENT_SEGMENT:
    case GST_EVENT_TAG:
    case GST_EVENT_TOC:

    /* non-sticky downstream serialized */
    case GST_EVENT_GAP:
      GST_DEBUG_OBJECT (self, ">> %" GST_PTR_FORMAT, event);

      gst_event_ref (event);
      if (!gst_pad_push_event (GST_BASE_SRC_PAD (GST_BASE_SRC (self)), event)) {
        GST_DEBUG_OBJECT (self, "Downstream elements did not handle %"
          GST_PTR_FORMAT, event);
      }
      break;
    default:
      GST_WARNING ("Unsupported event %" GST_PTR_FORMAT, event);
      return;
  }
}

static void
gst_sctp_server_src_init (GstSCTPServerSrc * self)
{
  self->priv = GST_SCTP_SERVER_SRC_GET_PRIVATE (self);
  self->priv->cancellable = g_cancellable_new ();
  self->priv->serverrpc = kms_sctp_server_rpc_new (KMS_SCTP_BASE_RPC_RULES,
      KURENTO_MARSHALL_BER, KMS_SCTP_BASE_RPC_BUFFER_SIZE, MAX_BUFFER_SIZE,
      NULL);
  kms_sctp_base_rpc_set_query_function (KMS_SCTP_BASE_RPC (self->priv->
          serverrpc), (KmsQueryFunction) gst_sctp_server_src_remote_query, self,
      NULL);
  kms_sctp_base_rpc_set_event_function (KMS_SCTP_BASE_RPC (self->priv->
          serverrpc), (KmsEventFunction) gst_sctp_server_src_remote_event, self,
      NULL);
}

gboolean
gst_sctp_server_src_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_SERVER_SRC);
}
