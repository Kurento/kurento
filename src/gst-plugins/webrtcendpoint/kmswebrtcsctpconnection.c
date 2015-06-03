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

#include "kmswebrtcsctpconnection.h"
#include "kmswebrtctransport.h"
#include <commons/kmsutils.h>

#define GST_CAT_DEFAULT kmswebrtcsctpconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtcsctpconnection"

#define NICE_COMPONENT_TYPE_SCTP 1

#define KMS_WEBRTC_SCTP_CONNECTION_GET_PRIVATE(obj) (        \
  G_TYPE_INSTANCE_GET_PRIVATE (                         \
    (obj),                                              \
    KMS_TYPE_WEBRTC_SCTP_CONNECTION,                         \
    KmsWebRtcSctpConnectionPrivate                          \
  )                                                     \
)

enum
{
  PROP_0,
  PROP_ADDED,
  PROP_CONNECTED
};

struct _KmsWebRtcSctpConnectionPrivate
{
  KmsWebRtcTransport *tr;

  gboolean added;
  gboolean connected;
};

static void kms_webrtc_rtp_connection_interface_init (KmsIRtpConnectionInterface
    * iface);

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcSctpConnection, kms_webrtc_sctp_connection,
    KMS_TYPE_WEBRTC_BASE_CONNECTION,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_RTP_CONNECTION,
        kms_webrtc_rtp_connection_interface_init));

static void
kms_webrtc_sctp_connection_set_certificate_pem_file (KmsWebRtcBaseConnection
    * base_conn, const gchar * pem)
{
  GST_WARNING_OBJECT (base_conn, "Deprectated (using erdtls)");
}

static gchar *
kms_webrtc_sctp_connection_get_certificate_pem (KmsWebRtcBaseConnection *
    base_conn)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (base_conn);
  gchar *pem;

  g_object_get (G_OBJECT (self->priv->tr->dtlssrtpdec), "pem", &pem, NULL);

  return pem;
}

static void
add_tr (KmsWebRtcTransport * tr, GstBin * bin, gboolean is_client)
{
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", is_client, NULL);

  gst_bin_add_many (bin,
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);
  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);

  gst_bin_add_many (bin,
      g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);
  gst_element_link (tr->dtlssrtpenc, tr->nicesink);
}

static void
kms_webrtc_sctp_connection_add (KmsIRtpConnection * base_conn, GstBin * bin,
    gboolean active)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (base_conn);

  add_tr (self->priv->tr, bin, active);
}

static void
sync_tr_src (KmsWebRtcTransport * tr)
{
  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent_target_state (tr->nicesrc);
}

static void
kms_webrtc_sctp_connection_src_sync_state_with_parent (KmsIRtpConnection *
    base_conn)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (base_conn);

  sync_tr_src (self->priv->tr);
}

static void
sync_tr_sink (KmsWebRtcTransport * tr)
{
  gst_element_sync_state_with_parent_target_state (tr->nicesink);
  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpenc);
}

static void
kms_webrtc_sctp_connection_sink_sync_state_with_parent (KmsIRtpConnection *
    base_conn)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (base_conn);

  sync_tr_sink (self->priv->tr);
}

static GstPad *
kms_webrtc_sctp_connection_request_rtp_sink (KmsIRtpConnection * base_conn)
{
  GST_WARNING_OBJECT (base_conn, "Unsupported operation");

  return NULL;
}

static GstPad *
kms_webrtc_sctp_connection_request_rtp_src (KmsIRtpConnection * base_conn)
{
  GST_WARNING_OBJECT (base_conn, "Unsupported operation");

  return NULL;
}

static GstPad *
kms_webrtc_sctp_connection_request_rtcp_sink (KmsIRtpConnection * base_conn)
{
  GST_WARNING_OBJECT (base_conn, "Unsupported operation");

  return NULL;
}

static GstPad *
kms_webrtc_sctp_connection_request_rtcp_src (KmsIRtpConnection * base_conn)
{
  GST_WARNING_OBJECT (base_conn, "Unsupported operation");

  return NULL;
}

static GstPad *
kms_webrtc_sctp_connection_request_data_src (KmsIRtpConnection * base_conn)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (base_conn);

  return gst_element_get_request_pad (self->priv->tr->dtlssrtpdec, "data_src");
}

static GstPad *
kms_webrtc_sctp_connection_request_data_sink (KmsIRtpConnection * base_conn)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (base_conn);

  return gst_element_get_request_pad (self->priv->tr->dtlssrtpenc, "data_sink");
}

static void
kms_webrtc_sctp_connection_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (object);

  switch (prop_id) {
    case PROP_ADDED:
      self->priv->added = g_value_get_boolean (value);
      break;
    case PROP_CONNECTED:
      self->priv->connected = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_webrtc_sctp_connection_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (object);

  switch (prop_id) {
    case PROP_ADDED:
      g_value_set_boolean (value, self->priv->added);
      break;
    case PROP_CONNECTED:
      g_value_set_boolean (value, self->priv->connected);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
dtls_connected_cb (GstElement * dtlssrtpenc, gpointer data)
{
  KmsWebRtcSctpConnection *self = data;

  kms_i_rtp_connection_connected_signal (KMS_I_RTP_CONNECTION (self));
}

KmsWebRtcSctpConnection *
kms_webrtc_sctp_connection_new (NiceAgent * agent, GMainContext * context,
    const gchar * name)
{
  GObject *obj;
  KmsWebRtcBaseConnection *base_conn;
  KmsWebRtcSctpConnection *conn;
  KmsWebRtcSctpConnectionPrivate *priv;

  obj = g_object_new (KMS_TYPE_WEBRTC_SCTP_CONNECTION, NULL);
  base_conn = KMS_WEBRTC_BASE_CONNECTION (obj);
  conn = KMS_WEBRTC_SCTP_CONNECTION (obj);
  priv = conn->priv;

  if (!kms_webrtc_base_connection_configure (base_conn, agent, name)) {
    g_object_unref (obj);
    return NULL;
  }

  priv->tr =
      kms_webrtc_transport_create (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_SCTP);

  if (priv->tr == NULL) {
    GST_ERROR_OBJECT (obj, "Cannot create KmsWebRtcSctpConnection.");
    g_object_unref (obj);
    return NULL;
  }

  g_signal_connect (priv->tr->dtlssrtpenc, "on-key-set",
      G_CALLBACK (dtls_connected_cb), conn);

  nice_agent_attach_recv (agent, base_conn->stream_id,
      NICE_COMPONENT_TYPE_SCTP, context,
      kms_webrtc_transport_nice_agent_recv_cb, NULL);

  return conn;
}

static void
kms_webrtc_sctp_connection_finalize (GObject * object)
{
  KmsWebRtcSctpConnection *self = KMS_WEBRTC_SCTP_CONNECTION (object);
  KmsWebRtcSctpConnectionPrivate *priv = self->priv;

  GST_DEBUG_OBJECT (self, "finalize");

  kms_webrtc_transport_destroy (priv->tr);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_sctp_connection_parent_class)->finalize (object);
}

static void
kms_webrtc_sctp_connection_init (KmsWebRtcSctpConnection * self)
{
  self->priv = KMS_WEBRTC_SCTP_CONNECTION_GET_PRIVATE (self);
  self->priv->connected = FALSE;
}

static void
kms_webrtc_sctp_connection_class_init (KmsWebRtcSctpConnectionClass * klass)
{
  GObjectClass *gobject_class;
  KmsWebRtcBaseConnectionClass *base_conn_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_sctp_connection_finalize;
  gobject_class->set_property = kms_webrtc_sctp_connection_set_property;
  gobject_class->get_property = kms_webrtc_sctp_connection_get_property;

  base_conn_class = KMS_WEBRTC_BASE_CONNECTION_CLASS (klass);
  base_conn_class->set_certificate_pem_file =
      kms_webrtc_sctp_connection_set_certificate_pem_file;
  base_conn_class->get_certificate_pem =
      kms_webrtc_sctp_connection_get_certificate_pem;

  g_type_class_add_private (klass, sizeof (KmsWebRtcSctpConnectionPrivate));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  g_object_class_override_property (gobject_class, PROP_ADDED, "added");
  g_object_class_override_property (gobject_class, PROP_CONNECTED, "connected");
}

static void
kms_webrtc_rtp_connection_interface_init (KmsIRtpConnectionInterface * iface)
{
  iface->add = kms_webrtc_sctp_connection_add;
  iface->src_sync_state_with_parent =
      kms_webrtc_sctp_connection_src_sync_state_with_parent;
  iface->sink_sync_state_with_parent =
      kms_webrtc_sctp_connection_sink_sync_state_with_parent;
  iface->request_rtp_sink = kms_webrtc_sctp_connection_request_rtp_sink;
  iface->request_rtp_src = kms_webrtc_sctp_connection_request_rtp_src;
  iface->request_rtcp_sink = kms_webrtc_sctp_connection_request_rtcp_sink;
  iface->request_rtcp_src = kms_webrtc_sctp_connection_request_rtcp_src;
  iface->request_data_src = kms_webrtc_sctp_connection_request_data_src;
  iface->request_data_sink = kms_webrtc_sctp_connection_request_data_sink;
}
