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
#include "kmssctpclientrpc.h"

#define PLUGIN_NAME "sctpclientsink"

#define MAX_BUFFER_SIZE (1024 * 16)

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
  KmsSCTPClientRPC *clientrpc;
  GCancellable *cancellable;

  /* server information */
  gint port;
  gchar *host;
  guint16 num_ostreams;
  guint16 max_istreams;
  guint32 timetolive;

  gboolean connected;
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

/* will be called only between calls to start() and stop() */
static gboolean
gst_sctp_client_sink_unlock (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  GST_DEBUG_OBJECT (self, "set to flushing");

  g_cancellable_cancel (self->priv->cancellable);
  kms_scp_base_rpc_cancel_pending_requests (KMS_SCTP_BASE_RPC (self->priv->
          clientrpc));

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

static gboolean
gst_sctp_client_sink_stop (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);

  g_cancellable_cancel (self->priv->cancellable);
  kms_sctp_client_rpc_stop (self->priv->clientrpc);

  return TRUE;
}

static gboolean
gst_sctp_client_sink_start (GstBaseSink * bsink)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);
  GError *err = NULL;

  if (kms_sctp_client_rpc_start (self->priv->clientrpc, self->priv->host,
          self->priv->port, self->priv->cancellable, &err)) {

    GST_OBJECT_LOCK (self);
    self->priv->connected = TRUE;
    GST_OBJECT_UNLOCK (self);

    return TRUE;
  }

  if (err != NULL) {
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED, (NULL),
        ("Error: %s", err->message));
    g_error_free (err);
  } else {
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED, (NULL),
        ("Error starting RPC subsystem"));
  }

  return FALSE;
}

static GstFlowReturn
gst_sctp_client_sink_render (GstBaseSink * bsink, GstBuffer * buf)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (bsink);
  GError *err = NULL;
  GstFlowReturn ret;

  GST_OBJECT_LOCK (self);

  if (!self->priv->connected) {
    GST_OBJECT_UNLOCK (self);
    return GST_FLOW_NOT_LINKED;
  }

  GST_OBJECT_UNLOCK (self);

  if (kms_scp_base_rpc_buffer (KMS_SCTP_BASE_RPC (self->priv-> clientrpc), buf,
    self->priv->cancellable, &err))
    return GST_FLOW_OK;

  if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_DEBUG_OBJECT (self, "%s", err->message);
      ret = GST_FLOW_FLUSHING;
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, WRITE,
          (("Error while sending data to \"%s:%d\"."), self->priv->host,
              self->priv->port), ("%s", err->message));
      ret = GST_FLOW_ERROR;
    }

  g_clear_error (&err);

  return ret;
}

static void
gst_sctp_client_sink_dispose (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  gst_sctp_client_sink_stop (GST_BASE_SINK (self));

  g_clear_object (&self->priv->clientrpc);

  G_OBJECT_CLASS (gst_sctp_client_sink_parent_class)->dispose (gobject);
}

static void
gst_sctp_client_sink_finalize (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  g_free (self->priv->host);
  g_clear_object (&self->priv->cancellable);

  G_OBJECT_CLASS (gst_sctp_client_sink_parent_class)->finalize (gobject);
}

static void
gst_sctp_client_sink_error_cb (GstSCTPClientSink * self)
{
  GST_OBJECT_LOCK (self);

  self->priv->connected = FALSE;

  GST_OBJECT_UNLOCK (self);

  kms_scp_base_rpc_cancel_pending_requests (KMS_SCTP_BASE_RPC (self->priv->
          clientrpc));
}

static gboolean
gst_sctp_client_sink_query (GstBaseSink * sink, GstQuery * query)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (sink);
  GstQuery *rsp_query = NULL;
  GError *err = NULL;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
    case GST_QUERY_URI:
      break;
    default:
      GST_WARNING ("Not propagated query: %" GST_PTR_FORMAT, query);

      return GST_BASE_SINK_CLASS (gst_sctp_client_sink_parent_class)->query (sink, query);
  }

  GST_DEBUG (">> %" GST_PTR_FORMAT, query);

  if (!kms_scp_base_rpc_query (KMS_SCTP_BASE_RPC (self->priv->clientrpc),
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
gst_sctp_client_sink_event (GstBaseSink * sink, GstEvent * event)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (sink);
  GError *err = NULL;
  gboolean ret, downstream;

  switch (GST_EVENT_TYPE (event)) {
    /* bidirectional events */
    case GST_EVENT_FLUSH_START:
    case GST_EVENT_FLUSH_STOP:

    /* downstream serialized events */
    case GST_EVENT_STREAM_START:
    case GST_EVENT_CAPS:
    case GST_EVENT_SEGMENT:
    case GST_EVENT_TAG:
    case GST_EVENT_TOC:

    /* non-sticky downstream serialized */
    case GST_EVENT_GAP:
      /* Base class is capable of managing above events for us, but */
      /* we still need to propagate them to the downstream pipeline */
      downstream = TRUE;
      break;
    default:
      GST_WARNING ("Not propagated >> %" GST_PTR_FORMAT, event);
      downstream = FALSE;
      break;
  }

  ret = GST_BASE_SINK_CLASS (gst_sctp_client_sink_parent_class)->event (sink,
          event);

  if (!downstream)
    return ret;

  GST_DEBUG (">> %" GST_PTR_FORMAT, event);

  if (!kms_scp_base_rpc_event (KMS_SCTP_BASE_RPC (self->priv->clientrpc),
      event, self->priv->cancellable, &err)) {
    GST_ERROR_OBJECT (self, "Error: %s", err->message);
    g_error_free (err);
  }

  return ret;
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
  gstbasesink_class->query = gst_sctp_client_sink_query;
  gstbasesink_class->event = gst_sctp_client_sink_event;

  g_type_class_add_private (klass, sizeof (GstSCTPClientSinkPrivate));
}

static void
gst_sctp_client_sink_remote_query (GstQuery * query, GstSCTPClientSink * self)
{
  GST_DEBUG_OBJECT (self, ">> %" GST_PTR_FORMAT, query);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
    case GST_QUERY_URI:
      gst_pad_peer_query (GST_BASE_SINK_PAD (GST_BASE_SINK (self)), query);
      break;
    default:
      GST_WARNING ("Unsupported query %" GST_PTR_FORMAT, query);
      return;
  }

  GST_DEBUG_OBJECT (self, "<< %" GST_PTR_FORMAT, query);
}

static void
gst_sctp_client_sink_remote_event (GstEvent * event, GstSCTPClientSink * self)
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

    /* upstream events */
    case GST_EVENT_QOS:
    case GST_EVENT_SEEK:
    case GST_EVENT_NAVIGATION:
    case GST_EVENT_LATENCY:
    case GST_EVENT_STEP:
    case GST_EVENT_RECONFIGURE:
    case GST_EVENT_TOC_SELECT:
    case GST_EVENT_CUSTOM_UPSTREAM:
      GST_DEBUG_OBJECT (self, ">> %" GST_PTR_FORMAT, event);

      gst_event_ref (event);
      if (!gst_pad_push_event (GST_BASE_SINK_PAD (GST_BASE_SINK (self)), event)) {
        GST_DEBUG_OBJECT (self, "Upstream elements did not handle %"
          GST_PTR_FORMAT, event);
      }
      break;
    default:
      GST_WARNING ("Unsupported event %" GST_PTR_FORMAT, event);
      return;
  }
}

static void
gst_sctp_client_sink_init (GstSCTPClientSink * self)
{
  self->priv = GST_SCTP_CLIENT_SINK_GET_PRIVATE (self);
  self->priv->cancellable = g_cancellable_new ();

  self->priv->clientrpc = kms_sctp_client_rpc_new (KMS_SCTP_BASE_RPC_RULES,
      KURENTO_MARSHALL_BER, KMS_SCTP_BASE_RPC_BUFFER_SIZE, MAX_BUFFER_SIZE,
      NULL);
  kms_sctp_client_rpc_set_error_function_full (self->priv->clientrpc,
      (KmsSocketErrorFunction) gst_sctp_client_sink_error_cb, self, NULL);

  /* Manage remote requests */
  kms_sctp_base_rpc_set_query_function (KMS_SCTP_BASE_RPC (self->priv->
          clientrpc), (KmsQueryFunction) gst_sctp_client_sink_remote_query, self,
      NULL);
  kms_sctp_base_rpc_set_event_function (KMS_SCTP_BASE_RPC (self->priv->
          clientrpc), (KmsEventFunction) gst_sctp_client_sink_remote_event, self,
      NULL);
}

gboolean
gst_sctp_client_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_CLIENT_SINK);
}
