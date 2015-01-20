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

#include "kmswebrtcbundleconnection.h"
#include "kmswebrtctransport.h"
#include <commons/kmsutils.h>

#define GST_CAT_DEFAULT kmswebrtcbundleconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtcbundleconnection"

#define KMS_WEBRTC_BUNDLE_CONNECTION_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                         \
    (obj),                                              \
    KMS_TYPE_WEBRTC_BUNDLE_CONNECTION,                  \
    KmsWebRtcBundleConnectionPrivate                    \
  )                                                     \
)

enum
{
  PROP_0,
  PROP_CONNECTED
};

struct _KmsWebRtcBundleConnectionPrivate
{
  KmsWebRtcTransport *tr;
  GstElement *rtp_funnel;
  GstElement *rtcp_funnel;

  gboolean connected;
};

static void
kms_webrtc_bundle_rtp_connection_interface_init (KmsIRtpConnectionInterface *
    iface);

static void
    kms_webrtc_bundle_rtcp_mux_connection_interface_init
    (KmsIRtcpMuxConnectionInterface * iface);

static void
kms_webrtc_bundle_connection_interface_init (KmsIBundleConnectionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcBundleConnection,
    kms_webrtc_bundle_connection, KMS_TYPE_WEBRTC_BASE_CONNECTION,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTP_CONNECTION,
        kms_webrtc_bundle_rtp_connection_interface_init)
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTCP_MUX_CONNECTION,
        kms_webrtc_bundle_rtcp_mux_connection_interface_init)
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_BUNDLE_CONNECTION,
        kms_webrtc_bundle_connection_interface_init));

static void
    kms_webrtc_bundle_connection_set_certificate_pem_file
    (KmsWebRtcBaseConnection * base_conn, const gchar * pem)
{
  KmsWebRtcBundleConnection *self = KMS_WEBRTC_BUNDLE_CONNECTION (base_conn);

  g_object_set (G_OBJECT (self->priv->tr->dtlssrtpdec),
      "certificate-pem-file", pem, NULL);
}

static void
kms_webrtc_bundle_connection_add (KmsIRtpConnection * base_rtp_conn,
    GstBin * bin, gboolean local_offer)
{
  KmsWebRtcBundleConnection *self =
      KMS_WEBRTC_BUNDLE_CONNECTION (base_rtp_conn);
  KmsWebRtcBundleConnectionPrivate *priv = self->priv;
  KmsWebRtcTransport *tr = priv->tr;

  /* srcs */
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", !local_offer, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", !local_offer, NULL);
  gst_bin_add_many (bin,
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);
  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);

  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent_target_state (tr->nicesrc);

  /* sinks */
  gst_bin_add_many (bin, g_object_ref (priv->rtp_funnel),
      g_object_ref (priv->rtcp_funnel),
      g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);

  gst_element_link (tr->dtlssrtpenc, tr->nicesink);
  gst_element_link_pads (priv->rtp_funnel, NULL, tr->dtlssrtpenc, "rtp_sink");
  gst_element_link_pads (priv->rtcp_funnel, NULL, tr->dtlssrtpenc, "rtcp_sink");

  gst_element_sync_state_with_parent_target_state (tr->nicesink);
  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpenc);
  gst_element_sync_state_with_parent_target_state (priv->rtp_funnel);
  gst_element_sync_state_with_parent_target_state (priv->rtcp_funnel);
}

static GstPad *
kms_webrtc_bundle_connection_request_rtp_sink (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcBundleConnection *self =
      KMS_WEBRTC_BUNDLE_CONNECTION (base_rtp_conn);

  return gst_element_get_request_pad (self->priv->rtp_funnel, "sink_%u");
}

static GstPad *
kms_webrtc_bundle_connection_request_rtp_src (KmsIRtpConnection * base_rtp_conn)
{
  KmsWebRtcBundleConnection *self =
      KMS_WEBRTC_BUNDLE_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->tr->dtlssrtpdec, "src");
}

static GstPad *
kms_webrtc_bundle_connection_request_rtcp_sink (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcBundleConnection *self =
      KMS_WEBRTC_BUNDLE_CONNECTION (base_rtp_conn);

  return gst_element_get_request_pad (self->priv->rtcp_funnel, "sink_%u");
}

static GstPad *
kms_webrtc_bundle_connection_request_rtcp_src (KmsIRtpConnection *
    base_rtp_conn)
{
  KmsWebRtcBundleConnection *self =
      KMS_WEBRTC_BUNDLE_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->tr->dtlssrtpdec, "src");
}

static void
kms_webrtc_bundle_connection_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebRtcBundleConnection *self = KMS_WEBRTC_BUNDLE_CONNECTION (object);

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
kms_webrtc_bundle_connection_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  KmsWebRtcBundleConnection *self = KMS_WEBRTC_BUNDLE_CONNECTION (object);

  switch (prop_id) {
    case PROP_CONNECTED:
      g_value_set_boolean (value, self->priv->connected);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

KmsWebRtcBundleConnection *
kms_webrtc_bundle_connection_new (NiceAgent * agent, GMainContext * context,
    const gchar * name)
{
  GObject *obj;
  KmsWebRtcBaseConnection *base_conn;
  KmsWebRtcBundleConnection *conn;
  KmsWebRtcBundleConnectionPrivate *priv;

  obj = g_object_new (KMS_TYPE_WEBRTC_BUNDLE_CONNECTION, NULL);
  base_conn = KMS_WEBRTC_BASE_CONNECTION (obj);
  conn = KMS_WEBRTC_BUNDLE_CONNECTION (obj);
  priv = conn->priv;

  if (!kms_webrtc_base_connection_configure (base_conn, agent, name)) {
    g_object_unref (obj);
    return NULL;
  }

  priv->tr =
      kms_webrtc_transport_create (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (priv->tr == NULL) {
    GST_ERROR_OBJECT (conn, "Cannot create connection");
    g_object_unref (obj);
    return NULL;
  }

  nice_agent_set_stream_name (agent, base_conn->stream_id, name);
  nice_agent_attach_recv (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTP, context, kms_webrtc_transport_nice_agent_recv_cb,
      NULL);

  priv->rtp_funnel = gst_element_factory_make ("funnel", NULL);
  priv->rtcp_funnel = gst_element_factory_make ("funnel", NULL);

  return conn;
}

static void
kms_webrtc_bundle_connection_finalize (GObject * object)
{
  KmsWebRtcBundleConnection *self = KMS_WEBRTC_BUNDLE_CONNECTION (object);
  KmsWebRtcBundleConnectionPrivate *priv = self->priv;

  GST_DEBUG_OBJECT (self, "finalize");

  kms_webrtc_transport_destroy (priv->tr);
  g_clear_object (&priv->rtp_funnel);
  g_clear_object (&priv->rtcp_funnel);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_bundle_connection_parent_class)->finalize (object);
}

static void
kms_webrtc_bundle_connection_init (KmsWebRtcBundleConnection * self)
{
  self->priv = KMS_WEBRTC_BUNDLE_CONNECTION_GET_PRIVATE (self);
  self->priv->connected = FALSE;
}

static void
kms_webrtc_bundle_connection_class_init (KmsWebRtcBundleConnectionClass * klass)
{
  GObjectClass *gobject_class;
  KmsWebRtcBaseConnectionClass *base_conn_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_bundle_connection_finalize;
  gobject_class->set_property = kms_webrtc_bundle_connection_set_property;
  gobject_class->get_property = kms_webrtc_bundle_connection_get_property;

  base_conn_class = KMS_WEBRTC_BASE_CONNECTION_CLASS (klass);
  base_conn_class->set_certificate_pem_file =
      kms_webrtc_bundle_connection_set_certificate_pem_file;

  g_type_class_add_private (klass, sizeof (KmsWebRtcBundleConnectionPrivate));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  g_object_class_override_property (gobject_class, PROP_CONNECTED, "connected");
}

static void
kms_webrtc_bundle_rtp_connection_interface_init (KmsIRtpConnectionInterface *
    iface)
{
  iface->add = kms_webrtc_bundle_connection_add;
  iface->request_rtp_sink = kms_webrtc_bundle_connection_request_rtp_sink;
  iface->request_rtp_src = kms_webrtc_bundle_connection_request_rtp_src;
  iface->request_rtcp_sink = kms_webrtc_bundle_connection_request_rtcp_sink;
  iface->request_rtcp_src = kms_webrtc_bundle_connection_request_rtcp_src;
}

static void
    kms_webrtc_bundle_rtcp_mux_connection_interface_init
    (KmsIRtcpMuxConnectionInterface * iface)
{
  /* Nothing to do */
}

static void
kms_webrtc_bundle_connection_interface_init (KmsIBundleConnectionInterface *
    iface)
{
  /* Nothing to do */
}
