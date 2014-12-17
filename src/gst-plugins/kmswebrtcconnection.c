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

#include "kmswebrtcconnection.h"
#include <commons/kmsutils.h>

#define GST_CAT_DEFAULT kmswebrtcconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtcconnection"

#define KMS_NICE_N_COMPONENTS 2

/* KmsWebRtcTransport begin */

typedef struct _KmsWebRtcTransport
{
  guint component_id;

  GstElement *dtlssrtpenc;
  GstElement *dtlssrtpdec;
  GstElement *nicesink;
  GstElement *nicesrc;
} KmsWebRtcTransport;

static void
kms_webrtc_transport_destroy (KmsWebRtcTransport * tr)
{
  if (tr == NULL) {
    return;
  }

  g_clear_object (&tr->dtlssrtpenc);
  g_clear_object (&tr->dtlssrtpdec);
  g_clear_object (&tr->nicesink);
  g_clear_object (&tr->nicesrc);

  g_slice_free (KmsWebRtcTransport, tr);
}

static KmsWebRtcTransport *
kms_webrtc_transport_create (NiceAgent * agent, guint stream_id,
    guint component_id)
{
  KmsWebRtcTransport *tr;
  gchar *str;

  tr = g_slice_new0 (KmsWebRtcTransport);

  /* TODO: improve creating elements when needed */
  tr->component_id = component_id;
  tr->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);
  tr->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);
  tr->nicesink = gst_element_factory_make ("nicesink", NULL);
  tr->nicesrc = gst_element_factory_make ("nicesrc", NULL);

  if (tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL
      || tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL) {
    GST_ERROR ("Cannot create KmsWebRtcTransport");
    kms_webrtc_transport_destroy (tr);
    return NULL;
  }

  str =
      g_strdup_printf ("%s-%s-%" G_GUINT32_FORMAT "-%" G_GUINT32_FORMAT,
      GST_OBJECT_NAME (tr->dtlssrtpenc), GST_OBJECT_NAME (tr->dtlssrtpdec),
      stream_id, component_id);
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "channel-id", str, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "channel-id", str, NULL);
  g_free (str);

  g_object_set (G_OBJECT (tr->nicesink), "agent", agent, "stream", stream_id,
      "component", component_id, "sync", FALSE, "async", FALSE, NULL);
  g_object_set (G_OBJECT (tr->nicesrc), "agent", agent, "stream", stream_id,
      "component", component_id, NULL);

  return tr;
}

/* KmsWebRtcTransport end */

static void
kms_nice_agent_recv_cb (NiceAgent * agent, guint stream_id, guint component_id,
    guint len, gchar * buf, gpointer user_data)
{
  /* Nothing to do, this callback is only for negotiation */
  GST_TRACE ("ICE data received on stream_id: '%" G_GUINT32_FORMAT
      "' component_id: '%" G_GUINT32_FORMAT "'", stream_id, component_id);
}

/* KmsWebRtcBaseConnection begin */

G_DEFINE_TYPE (KmsWebRtcBaseConnection, kms_webrtc_base_connection,
    G_TYPE_OBJECT);

static gboolean
kms_webrtc_base_connection_configure (KmsWebRtcBaseConnection * self,
    NiceAgent * agent, const gchar * name)
{
  self->agent = g_object_ref (agent);
  self->name = g_strdup (name);

  self->stream_id = nice_agent_add_stream (agent, KMS_NICE_N_COMPONENTS);
  if (self->stream_id == 0) {
    GST_ERROR_OBJECT (self, "Cannot add nice stream for %s.", name);
    return FALSE;
  }

  return TRUE;
}

static void
    kms_webrtc_base_connection_set_certificate_pem_file_default
    (KmsWebRtcBaseConnection * self, const gchar * pem)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->set_certificate_pem_file ==
      kms_webrtc_base_connection_set_certificate_pem_file_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'set_certificate_pem_file'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_webrtc_base_connection_finalize (GObject * object)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (object);

  GST_DEBUG_OBJECT (self, "finalize");

  nice_agent_remove_stream (self->agent, self->stream_id);
  g_free (self->name);
  g_clear_object (&self->agent);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_base_connection_parent_class)->finalize (object);
}

static void
kms_webrtc_base_connection_init (KmsWebRtcBaseConnection * self)
{
  /* Nothing to do */
}

static void
kms_webrtc_base_connection_class_init (KmsWebRtcBaseConnectionClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_base_connection_finalize;

  klass->set_certificate_pem_file =
      kms_webrtc_base_connection_set_certificate_pem_file_default;
}

void
kms_webrtc_base_connection_set_certificate_pem_file (KmsWebRtcBaseConnection *
    self, const gchar * pem)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_certificate_pem_file (self, pem);
}

void
kms_webrtc_base_connection_set_relay_info (KmsWebRtcBaseConnection * self,
    const gchar * server_ip,
    guint server_port,
    const gchar * username, const gchar * password, NiceRelayType type)
{
  nice_agent_set_relay_info (self->agent, self->stream_id,
      NICE_COMPONENT_TYPE_RTP, server_ip, server_port,
      username, password, type);
  nice_agent_set_relay_info (self->agent, self->stream_id,
      NICE_COMPONENT_TYPE_RTCP, server_ip, server_port,
      username, password, type);
}

/* KmsWebRtcBaseConnection end */

/* KmsWebRtcConnection begin */

static void kms_webrtc_rtp_connection_interface_init (KmsIRtpConnectionInterface
    * iface);

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcConnection, kms_webrtc_connection,
    KMS_TYPE_WEBRTC_BASE_CONNECTION,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTP_CONNECTION,
        kms_webrtc_rtp_connection_interface_init))
#define KMS_WEBRTC_CONNECTION_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_CONNECTION,                   \
    KmsWebRtcConnectionPrivate                    \
  )                                             \
)
     struct _KmsWebRtcConnectionPrivate
     {
       KmsWebRtcTransport *rtp_tr;
       KmsWebRtcTransport *rtcp_tr;
     };

     static void
         kms_webrtc_connection_set_certificate_pem_file (KmsWebRtcBaseConnection
    * base_conn, const gchar * pem)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (base_conn);
  KmsWebRtcConnectionPrivate *priv = self->priv;

  g_object_set (G_OBJECT (priv->rtp_tr->dtlssrtpdec),
      "certificate-pem-file", pem, NULL);
  g_object_set (G_OBJECT (priv->rtcp_tr->dtlssrtpdec),
      "certificate-pem-file", pem, NULL);
}

static void
add_tr (KmsWebRtcTransport * tr, GstBin * bin, gboolean is_client)
{
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", is_client, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", is_client, NULL);

  gst_bin_add_many (bin,
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);

  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);

  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent_target_state (tr->nicesrc);

  {
    gst_bin_add_many (bin,
        g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);

    gst_element_link (tr->dtlssrtpenc, tr->nicesink);
    gst_element_sync_state_with_parent_target_state (tr->nicesink);
    gst_element_sync_state_with_parent_target_state (tr->dtlssrtpenc);
  }
}

static void
kms_webrtc_rtp_connection_add (KmsIRtpConnection * base_rtp_conn, GstBin * bin,
    gboolean local_offer)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (base_rtp_conn);
  gboolean is_client = !local_offer;

  add_tr (self->priv->rtp_tr, bin, is_client);
  add_tr (self->priv->rtcp_tr, bin, is_client);
}

static GstPad *
kms_webrtc_rtp_connection_request_rtp_sink (KmsIRtpConnection * base_rtp_conn)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtp_tr->dtlssrtpenc,
      "rtp_sink");
}

static GstPad *
kms_webrtc_rtp_connection_request_rtp_src (KmsIRtpConnection * base_rtp_conn)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtp_tr->dtlssrtpdec, "src");
}

static GstPad *
kms_webrtc_rtp_connection_request_rtcp_sink (KmsIRtpConnection * base_rtp_conn)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtcp_tr->dtlssrtpenc,
      "rtcp_sink");
}

static GstPad *
kms_webrtc_rtp_connection_request_rtcp_src (KmsIRtpConnection * base_rtp_conn)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (base_rtp_conn);

  return gst_element_get_static_pad (self->priv->rtcp_tr->dtlssrtpdec, "src");
}

KmsWebRtcConnection *
kms_webrtc_connection_new (NiceAgent * agent, GMainContext * context,
    const gchar * name)
{
  GObject *obj;
  KmsWebRtcBaseConnection *base_conn;
  KmsWebRtcConnection *conn;
  KmsWebRtcConnectionPrivate *priv;

  obj = g_object_new (KMS_TYPE_WEBRTC_CONNECTION, NULL);
  base_conn = KMS_WEBRTC_BASE_CONNECTION (obj);
  conn = KMS_WEBRTC_CONNECTION (obj);
  priv = conn->priv;

  if (!kms_webrtc_base_connection_configure (base_conn, agent, name)) {
    g_object_unref (obj);
    return NULL;
  }

  priv->rtp_tr =
      kms_webrtc_transport_create (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTP);
  priv->rtcp_tr =
      kms_webrtc_transport_create (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTCP);

  if (priv->rtp_tr == NULL || priv->rtcp_tr == NULL) {
    GST_ERROR ("Cannot create KmsWebRTCConnection.");
    g_object_unref (obj);
    return NULL;
  }

  nice_agent_set_stream_name (agent, base_conn->stream_id, name);
  nice_agent_attach_recv (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTP, context, kms_nice_agent_recv_cb, NULL);
  nice_agent_attach_recv (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_RTCP, context, kms_nice_agent_recv_cb, NULL);

  return conn;
}

static void
kms_webrtc_connection_finalize (GObject * object)
{
  KmsWebRtcConnection *self = KMS_WEBRTC_CONNECTION (object);
  KmsWebRtcConnectionPrivate *priv = self->priv;

  GST_DEBUG_OBJECT (self, "finalize");

  kms_webrtc_transport_destroy (priv->rtp_tr);
  kms_webrtc_transport_destroy (priv->rtcp_tr);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_connection_parent_class)->finalize (object);
}

static void
kms_webrtc_connection_init (KmsWebRtcConnection * self)
{
  self->priv = KMS_WEBRTC_CONNECTION_GET_PRIVATE (self);
}

static void
kms_webrtc_connection_class_init (KmsWebRtcConnectionClass * klass)
{
  GObjectClass *gobject_class;
  KmsWebRtcBaseConnectionClass *base_conn_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_connection_finalize;

  base_conn_class = KMS_WEBRTC_BASE_CONNECTION_CLASS (klass);
  base_conn_class->set_certificate_pem_file =
      kms_webrtc_connection_set_certificate_pem_file;

  g_type_class_add_private (klass, sizeof (KmsWebRtcConnectionPrivate));
}

static void
kms_webrtc_rtp_connection_interface_init (KmsIRtpConnectionInterface * iface)
{
  iface->add = kms_webrtc_rtp_connection_add;
  iface->request_rtp_sink = kms_webrtc_rtp_connection_request_rtp_sink;
  iface->request_rtp_src = kms_webrtc_rtp_connection_request_rtp_src;
  iface->request_rtcp_sink = kms_webrtc_rtp_connection_request_rtcp_sink;
  iface->request_rtcp_src = kms_webrtc_rtp_connection_request_rtcp_src;
}

/* KmsWebRtcConnection end */

/* KmsWebRtcBundleConnection begin */

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
        kms_webrtc_bundle_connection_interface_init))
#define KMS_WEBRTC_BUNDLE_CONNECTION_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_BUNDLE_CONNECTION,                   \
    KmsWebRtcBundleConnectionPrivate                    \
  )                                             \
)
     struct _KmsWebRtcBundleConnectionPrivate
     {
       KmsWebRtcTransport *tr;
       GstElement *rtp_funnel;
       GstElement *rtcp_funnel;
     };

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

  {
    g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", !local_offer, NULL);
    g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", !local_offer, NULL);
    gst_bin_add_many (bin,
        g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);
    gst_element_link (tr->nicesrc, tr->dtlssrtpdec);

    gst_element_sync_state_with_parent_target_state (tr->dtlssrtpdec);
    gst_element_sync_state_with_parent_target_state (tr->nicesrc);
  }

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
      NICE_COMPONENT_TYPE_RTP, context, kms_nice_agent_recv_cb, NULL);

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
}

static void
kms_webrtc_bundle_connection_class_init (KmsWebRtcBundleConnectionClass * klass)
{
  GObjectClass *gobject_class;
  KmsWebRtcBaseConnectionClass *base_conn_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_bundle_connection_finalize;

  base_conn_class = KMS_WEBRTC_BASE_CONNECTION_CLASS (klass);
  base_conn_class->set_certificate_pem_file =
      kms_webrtc_bundle_connection_set_certificate_pem_file;

  g_type_class_add_private (klass, sizeof (KmsWebRtcBundleConnectionPrivate));
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

/* KmsWebRtcBundleConnection end */

static void init_debug (void) __attribute__ ((constructor));

static void
init_debug (void)
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
