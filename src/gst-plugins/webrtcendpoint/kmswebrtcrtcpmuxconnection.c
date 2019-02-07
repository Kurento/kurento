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

#include "kmswebrtcrtcpmuxconnection.h"
#include "kmswebrtctransport.h"
#include <commons/kmsutils.h>

#define GST_CAT_DEFAULT kmswebrtcmuxconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtcmuxconnection"

#define KMS_WEBRTC_RTCP_MUX_CONNECTION_GET_PRIVATE(obj) (       \
  G_TYPE_INSTANCE_GET_PRIVATE (                                 \
    (obj),                                                      \
    KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION,                        \
    KmsWebRtcRtcpMuxConnectionPrivate                           \
  )                                                             \
)

enum
{
  PROP_0,
  PROP_ADDED,
  PROP_CONNECTED,
  PROP_IS_CLIENT,
  PROP_MIN_PORT,
  PROP_MAX_PORT,
  PROP_TRANSPORT
};

struct _KmsWebRtcRtcpMuxConnectionPrivate
{
  KmsWebRtcTransport *tr;

  gboolean added;
  gboolean connected;
};

static void
kms_webrtc_rtcp_mux_rtp_connection_interface_init (KmsIRtpConnectionInterface *
    iface);

static void
    kms_webrtc_rtcp_mux_connection_interface_init
    (KmsIRtcpMuxConnectionInterface * iface);

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcRtcpMuxConnection,
    kms_webrtc_rtcp_mux_connection, KMS_TYPE_WEBRTC_BASE_CONNECTION,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTP_CONNECTION,
        kms_webrtc_rtcp_mux_rtp_connection_interface_init)
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTCP_MUX_CONNECTION,
        kms_webrtc_rtcp_mux_connection_interface_init));

static gchar *
kms_webrtc_rtcp_mux_connection_get_certificate_pem_file (KmsWebRtcBaseConnection
    * base_conn)
{
  KmsWebRtcRtcpMuxConnection *self = KMS_WEBRTC_RTCP_MUX_CONNECTION (base_conn);
  gchar *pem;

  g_object_get (G_OBJECT (self->priv->tr->src->dtlssrtpdec), "pem", &pem, NULL);

  return pem;
}

static void
kms_webrtc_rtcp_mux_connection_add (KmsIRtpConnection * base_rtp_conn,
    GstBin * bin, gboolean active)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);
  KmsWebRtcRtcpMuxConnectionPrivate *priv = self->priv;
  KmsWebRtcTransport *tr = priv->tr;

  /* srcs */
  g_object_set (G_OBJECT (tr->sink->dtlssrtpenc), "is-client", active, NULL);

  gst_bin_add (bin, GST_ELEMENT (g_object_ref (tr->src)));
  gst_bin_add (bin, GST_ELEMENT (g_object_ref (tr->sink)));
}

static void
kms_webrtc_rtcp_mux_connection_src_sync_state_with_parent (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);
  GstElement *element = GST_ELEMENT (self->priv->tr->src);

  gst_element_sync_state_with_parent_target_state (element);
}

static void
kms_webrtc_rtcp_mux_connection_sink_sync_state_with_parent (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);
  GstElement *element = GST_ELEMENT (self->priv->tr->sink);

  gst_element_sync_state_with_parent_target_state (element);
}

static GstPad *
kms_webrtc_rtcp_mux_connection_request_rtp_sink (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);
  GstPad *pad;
  gchar *str;

  str = g_strdup_printf ("rtp_sink_%d",
      g_atomic_int_add (&self->priv->tr->rtp_id, 1));

  pad = gst_element_get_request_pad (self->priv->tr->sink->dtlssrtpenc, str);
  g_free (str);

  return pad;
}

static GstPad *
kms_webrtc_rtcp_mux_connection_request_rtp_src (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->tr->src->dtlssrtpdec,
      "rtp_src");
}

static GstPad *
kms_webrtc_rtcp_mux_connection_request_rtcp_sink (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);

  GstPad *pad;
  gchar *str;

  str = g_strdup_printf ("rtcp_sink_%d",
      g_atomic_int_add (&self->priv->tr->rtcp_id, 1));

  pad = gst_element_get_request_pad (self->priv->tr->sink->dtlssrtpenc, str);
  g_free (str);

  return pad;
}

static GstPad *
kms_webrtc_rtcp_mux_connection_request_rtcp_src (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcRtcpMuxConnection *self =
      KMS_WEBRTC_RTCP_MUX_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->tr->src->dtlssrtpdec,
      "rtcp_src");
}

static void
kms_webrtc_rtcp_mux_connection_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebRtcRtcpMuxConnection *self = KMS_WEBRTC_RTCP_MUX_CONNECTION (object);

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
kms_webrtc_rtcp_mux_connection_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  KmsWebRtcRtcpMuxConnection *self = KMS_WEBRTC_RTCP_MUX_CONNECTION (object);

  switch (prop_id) {
    case PROP_ADDED:
      g_value_set_boolean (value, self->priv->added);
      break;
    case PROP_CONNECTED:
      g_value_set_boolean (value, self->priv->connected);
      break;
    case PROP_IS_CLIENT:{
      gboolean is_client;

      g_object_get (G_OBJECT (self->priv->tr->sink->dtlssrtpenc), "is-client",
          &is_client, NULL);
      g_value_set_boolean (value, is_client);
      break;
    }
    case PROP_MIN_PORT:
      g_value_set_uint (value, self->parent.min_port);
      break;
    case PROP_MAX_PORT:
      g_value_set_uint (value, self->parent.max_port);
      break;
    case PROP_TRANSPORT:
      g_value_set_object (value, self->priv->tr);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
connected_cb (GstElement * dtlssrtpenc, gpointer self)
{
  kms_i_rtp_connection_connected_signal (self);
}

KmsWebRtcRtcpMuxConnection *
kms_webrtc_rtcp_mux_connection_new (KmsIceBaseAgent * agent,
    GMainContext * context, const gchar * name, guint16 min_port,
    guint16 max_port, gchar * pem_certificate)
{
  GObject *obj;
  KmsWebRtcBaseConnection *base_conn;
  KmsWebRtcRtcpMuxConnection *conn;
  KmsWebRtcRtcpMuxConnectionPrivate *priv;

  obj =
      g_object_new (KMS_TYPE_WEBRTC_RTCP_MUX_CONNECTION, "max-port", max_port,
      "min-port", min_port, NULL);
  base_conn = KMS_WEBRTC_BASE_CONNECTION (obj);
  conn = KMS_WEBRTC_RTCP_MUX_CONNECTION (obj);
  priv = conn->priv;

  if (!kms_webrtc_base_connection_configure (base_conn, agent, name)) {
    g_object_unref (obj);
    return NULL;
  }

  priv->tr =
      kms_webrtc_transport_new (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTP, pem_certificate);

  if (priv->tr == NULL) {
    GST_ERROR_OBJECT (conn, "Cannot create connection");
    g_object_unref (obj);
    return NULL;
  }

  g_signal_connect (priv->tr->sink->dtlssrtpenc, "on-key-set",
      G_CALLBACK (connected_cb), conn);

  return conn;
}

static void
kms_webrtc_rtcp_mux_connection_finalize (GObject * object)
{
  KmsWebRtcRtcpMuxConnection *self = KMS_WEBRTC_RTCP_MUX_CONNECTION (object);
  KmsWebRtcRtcpMuxConnectionPrivate *priv = self->priv;

  GST_DEBUG_OBJECT (self, "finalize");

  g_clear_object (&priv->tr);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_rtcp_mux_connection_parent_class)->finalize
      (object);
}

static void
kms_webrtc_rtcp_mux_connection_init (KmsWebRtcRtcpMuxConnection * self)
{
  self->priv = KMS_WEBRTC_RTCP_MUX_CONNECTION_GET_PRIVATE (self);
  self->priv->connected = FALSE;
}

static void
kms_webrtc_rtcp_mux_connection_class_init (KmsWebRtcRtcpMuxConnectionClass *
    klass)
{
  GObjectClass *gobject_class;
  KmsWebRtcBaseConnectionClass *base_conn_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_rtcp_mux_connection_finalize;
  gobject_class->set_property = kms_webrtc_rtcp_mux_connection_set_property;
  gobject_class->get_property = kms_webrtc_rtcp_mux_connection_get_property;

  base_conn_class = KMS_WEBRTC_BASE_CONNECTION_CLASS (klass);
  base_conn_class->get_certificate_pem =
      kms_webrtc_rtcp_mux_connection_get_certificate_pem_file;

  g_type_class_add_private (klass, sizeof (KmsWebRtcRtcpMuxConnectionPrivate));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  g_object_class_override_property (gobject_class, PROP_ADDED, "added");
  g_object_class_override_property (gobject_class, PROP_CONNECTED, "connected");
  g_object_class_override_property (gobject_class, PROP_IS_CLIENT, "is-client");
  g_object_class_override_property (gobject_class, PROP_MAX_PORT, "max-port");
  g_object_class_override_property (gobject_class, PROP_MIN_PORT, "min-port");

  g_object_class_install_property (gobject_class, PROP_TRANSPORT,
      g_param_spec_object ("transport", "Transport",
          "The transport used to send and receive RTP and RTCP packets.",
          KMS_TYPE_WEBRTC_TRANSPORT,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
}

static void
kms_webrtc_rtcp_mux_connection_collect_latency_stats (KmsIRtpConnection * obj,
    gboolean enable)
{
  KmsWebRtcRtcpMuxConnection *self = KMS_WEBRTC_RTCP_MUX_CONNECTION (obj);
  KmsWebRtcBaseConnection *base = KMS_WEBRTC_BASE_CONNECTION (obj);

  KMS_WEBRTC_BASE_CONNECTION_LOCK (base);

  if (enable) {
    kms_webrtc_transport_enable_latency_notification (self->priv->tr,
        base->cb, base->user_data, NULL);
  } else {
    kms_webrtc_transport_disable_latency_notification (self->priv->tr);
  }

  kms_webrtc_base_connection_collect_latency_stats (obj, enable);

  KMS_WEBRTC_BASE_CONNECTION_UNLOCK (base);
}

static void
kms_webrtc_rtcp_mux_rtp_connection_interface_init (KmsIRtpConnectionInterface *
    iface)
{
  iface->add = kms_webrtc_rtcp_mux_connection_add;
  iface->src_sync_state_with_parent =
      kms_webrtc_rtcp_mux_connection_src_sync_state_with_parent;
  iface->sink_sync_state_with_parent =
      kms_webrtc_rtcp_mux_connection_sink_sync_state_with_parent;
  iface->request_rtp_sink = kms_webrtc_rtcp_mux_connection_request_rtp_sink;
  iface->request_rtp_src = kms_webrtc_rtcp_mux_connection_request_rtp_src;
  iface->request_rtcp_sink = kms_webrtc_rtcp_mux_connection_request_rtcp_sink;
  iface->request_rtcp_src = kms_webrtc_rtcp_mux_connection_request_rtcp_src;

  iface->set_latency_callback = kms_webrtc_base_connection_set_latency_callback;
  iface->collect_latency_stats =
      kms_webrtc_rtcp_mux_connection_collect_latency_stats;
}

static void
    kms_webrtc_rtcp_mux_connection_interface_init
    (KmsIRtcpMuxConnectionInterface * iface)
{
  /* Nothing to do */
}
