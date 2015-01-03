/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include <nice/interfaces.h>
#include <gst/rtp/gstrtcpbuffer.h>

#include "kmsrtpendpoint.h"
#include "kmsrtpconnection.h"
#include <commons/sdp_utils.h>
#include <commons/kmsloop.h>

#define PLUGIN_NAME "rtpendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_rtp_endpoint_debug);
#define GST_CAT_DEFAULT kms_rtp_endpoint_debug

#define kms_rtp_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsRtpEndpoint, kms_rtp_endpoint, KMS_TYPE_BASE_RTP_ENDPOINT);

#define MAX_RETRIES 4

#define KMS_RTP_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_RTP_ENDPOINT,                   \
    KmsRtpEndpointPrivate                    \
  )                                          \
)

struct _KmsRtpEndpointPrivate
{
  GHashTable *conns;
};

/* Signals and args */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0
};

/* Connection management begin */
static KmsIRtpConnection *
kms_rtp_endpoint_get_connection (KmsBaseRtpEndpoint * base_rtp_endpoint,
    const gchar * name)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_rtp_endpoint);
  gpointer *conn;

  KMS_ELEMENT_LOCK (self);
  conn = g_hash_table_lookup (self->priv->conns, name);
  KMS_ELEMENT_UNLOCK (self);

  if (conn == NULL) {
    return NULL;
  }

  return KMS_I_RTP_CONNECTION (conn);
}

static KmsIRtpConnection *
kms_rtp_endpoint_create_connection (KmsBaseRtpEndpoint * base_rtp_endpoint,
    const gchar * name)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_rtp_endpoint);
  KmsRtpConnection *conn;

  KMS_ELEMENT_LOCK (self);
  conn = g_hash_table_lookup (self->priv->conns, name);
  if (conn == NULL) {
    conn = kms_rtp_connection_new ();
    g_hash_table_insert (self->priv->conns, g_strdup (name), conn);
  } else {
    GST_WARNING_OBJECT (self, "Connection '%s' already created", name);
  }
  KMS_ELEMENT_UNLOCK (self);

  return KMS_I_RTP_CONNECTION (conn);
}

static KmsIBundleConnection *
kms_rtp_endpoint_create_bundle_connection (KmsBaseRtpEndpoint *
    base_rtp_endpoint, const gchar * name)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_rtp_endpoint);

  GST_WARNING_OBJECT (self, "Not implemented");

  return NULL;
}

static KmsRtpBaseConnection *
kms_rtp_endpoint_media_get_connection (KmsRtpEndpoint * self,
    const GstSDPMedia * media, gboolean bundle)
{
  KmsBaseRtpEndpoint *base_rtp = KMS_BASE_RTP_ENDPOINT (self);
  const gchar *conn_name;
  KmsIRtpConnection *conn;

  if (bundle) {
    conn_name = BUNDLE_STREAM_NAME;
  } else {
    conn_name = gst_sdp_media_get_media (media);
  }

  conn = kms_base_rtp_endpoint_get_connection (base_rtp, conn_name);
  if (conn == NULL) {
    GST_WARNING_OBJECT (self, "Connection '%s' not found", conn_name);
    return NULL;
  }

  return KMS_RTP_BASE_CONNECTION (conn);
}

/* Connection management end */

/* Set Transport begin */
static guint64
get_ntp_time ()
{
  return time (NULL) + G_GUINT64_CONSTANT (2208988800);
}

static void
gst_udp_set_connection (KmsBaseSdpEndpoint * base_sdp_endpoint,
    GstSDPMessage * msg)
{
  GList *ips, *l;
  gboolean done = FALSE;

  ips = nice_interfaces_get_local_ips (FALSE);
  for (l = ips; l != NULL && !done; l = l->next) {
    GInetAddress *addr;
    gboolean is_ipv6 = FALSE;

    addr = g_inet_address_new_from_string (l->data);
    switch (g_inet_address_get_family (addr)) {
      case G_SOCKET_FAMILY_INVALID:
      case G_SOCKET_FAMILY_UNIX:
        /* Ignore this addresses */
        break;
      case G_SOCKET_FAMILY_IPV6:
        is_ipv6 = TRUE;
      case G_SOCKET_FAMILY_IPV4:
      {
        gchar *addr_str;
        gboolean use_ipv6;

        g_object_get (base_sdp_endpoint, "use-ipv6", &use_ipv6, NULL);
        if (is_ipv6 != use_ipv6) {
          GST_DEBUG ("No valid address type: %d", is_ipv6);
          break;
        }

        addr_str = g_inet_address_to_string (addr);
        if (addr_str != NULL) {
          const gchar *addr_type = is_ipv6 ? "IP6" : "IP4";
          gchar *ntp = g_strdup_printf ("%" G_GUINT64_FORMAT, get_ntp_time ());

          gst_sdp_message_set_connection (msg, "IN", addr_type, l->data, 0, 0);
          gst_sdp_message_set_origin (msg, "-", ntp, ntp, "IN",
              addr_type, addr_str);
          g_free (ntp);
          g_free (addr_str);
          done = TRUE;
        }
        break;
      }
    }
    g_object_unref (addr);
  }

  g_list_free_full (ips, g_free);
}

static gboolean
kms_rtp_endpoint_set_transport_to_sdp (KmsBaseSdpEndpoint * base_sdp_endpoint,
    GstSDPMessage * msg)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp_endpoint);
  gboolean ret;
  guint len, i;

  /* Chain up */
  ret =
      KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->set_transport_to_sdp (base_sdp_endpoint,
      msg);

  if (ret == FALSE) {
    return FALSE;
  }

  gst_udp_set_connection (base_sdp_endpoint, msg);

  len = gst_sdp_message_medias_len (msg);
  for (i = 0; i < len; i++) {
    GstSDPMedia *media = (GstSDPMedia *) gst_sdp_message_get_media (msg, i);
    guint conn_len, c;
    guint attr_len, a;
    KmsRtpBaseConnection *conn;

    if (g_ascii_strcasecmp ("RTP/AVP", gst_sdp_media_get_proto (media)) != 0) {
      ((GstSDPMedia *) media)->port = 0;
      continue;
    }

    conn_len = gst_sdp_media_connections_len (media);
    for (c = 0; c < conn_len; c++) {
      gst_sdp_media_remove_connection (media, c);
    }

    /* TODO: bundle support */
    conn = kms_rtp_endpoint_media_get_connection (self, media, FALSE);
    if (conn == NULL) {
      continue;
    }

    media->port = kms_rtp_base_connection_get_rtp_port (conn);

    attr_len = gst_sdp_media_attributes_len (media);
    for (a = 0; a < attr_len; a++) {
      const GstSDPAttribute *attr = gst_sdp_media_get_attribute (media, a);

      if (g_strcmp0 (attr->key, "rtcp") == 0) {
        gst_sdp_media_remove_attribute (media, a);
        /* TODO: complete rtcp attr with addr and rtcp port */
      }
    }
  }

  return TRUE;
}

/* Set Transport end */

static void
kms_rtp_endpoint_start_transport_send (KmsBaseSdpEndpoint * base_rtp_endpoint,
    const GstSDPMessage * offer, const GstSDPMessage * answer,
    gboolean local_offer)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_rtp_endpoint);
  const GstSDPConnection *msg_conn;
  const GstSDPMessage *sdp;
  guint len, i;

  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->start_transport_send
      (base_rtp_endpoint, answer, offer, local_offer);

  if (local_offer) {
    sdp = answer;
  } else {
    sdp = offer;
  }

  msg_conn = gst_sdp_message_get_connection (sdp);

  len = gst_sdp_message_medias_len (sdp);
  for (i = 0; i < len; i++) {
    const GstSDPConnection *media_con;
    const GstSDPMedia *offer_media = gst_sdp_message_get_media (offer, i);
    const GstSDPMedia *answer_media = gst_sdp_message_get_media (answer, i);
    const GstSDPMedia *media;
    const gchar *media_str;
    KmsRtpBaseConnection *conn;
    guint port;

    if (offer_media == NULL || answer_media == NULL)
      continue;

    if (g_ascii_strcasecmp ("RTP/AVP",
            gst_sdp_media_get_proto (answer_media)) != 0) {
      ((GstSDPMedia *) answer_media)->port = 0;
      continue;
    }

    if (answer_media->port == 0) {
      continue;
    }

    if (local_offer) {
      media = answer_media;
    } else {
      media = offer_media;
    }
    media_str = gst_sdp_media_get_media (media);

    if (gst_sdp_media_connections_len (media) != 0) {
      media_con = gst_sdp_media_get_connection (media, 0);
    } else {
      media_con = msg_conn;
    }

    if (media_con == NULL || media_con->address == NULL
        || media_con->address[0] == '\0') {
      GST_WARNING_OBJECT (self, "Missing connection information for '%s'",
          media_str);
      continue;
    }

    /* TODO: bundle support */
    conn = kms_rtp_endpoint_media_get_connection (self, media, FALSE);
    if (conn == NULL) {
      continue;
    }

    port = gst_sdp_media_get_port (media);
    kms_rtp_base_connection_set_remote_info (conn,
        media_con->address, port, port + 1);
  }
}

static void
kms_rtp_endpoint_finalize (GObject * object)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_hash_table_destroy (self->priv->conns);

  G_OBJECT_CLASS (kms_rtp_endpoint_parent_class)->finalize (object);
}

static void
kms_rtp_endpoint_class_init (KmsRtpEndpointClass * klass)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class;
  KmsBaseRtpEndpointClass *base_rtp_endpoint_class;
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "RtpEndpoint",
      "RTP/Stream/RtpEndpoint",
      "Rtp Endpoint element",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_rtp_endpoint_finalize;

  base_sdp_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);
  base_sdp_endpoint_class->set_transport_to_sdp =
      kms_rtp_endpoint_set_transport_to_sdp;
  base_sdp_endpoint_class->start_transport_send =
      kms_rtp_endpoint_start_transport_send;

  base_rtp_endpoint_class = KMS_BASE_RTP_ENDPOINT_CLASS (klass);
  /* Connection management */
  base_rtp_endpoint_class->get_connection = kms_rtp_endpoint_get_connection;
  base_rtp_endpoint_class->create_connection =
      kms_rtp_endpoint_create_connection;
  base_rtp_endpoint_class->create_bundle_connection =
      kms_rtp_endpoint_create_bundle_connection;

  g_type_class_add_private (klass, sizeof (KmsRtpEndpointPrivate));
}

static void
kms_rtp_endpoint_init (KmsRtpEndpoint * self)
{
  self->priv = KMS_RTP_ENDPOINT_GET_PRIVATE (self);

  g_object_set (G_OBJECT (self), "proto", SDP_MEDIA_RTP_AVP_PROTO, "bundle",
      FALSE, "rtcp-fir", FALSE, "rtcp-nack", FALSE, "rtcp-pli", FALSE,
      "rtcp-remb", FALSE, "max-video-recv-bandwidth", 0, NULL);
  /* FIXME: remove max-video-recv-bandwidth when it b=AS:X is in the SDP offer */

  self->priv->conns =
      g_hash_table_new_full (g_str_hash, g_str_equal, g_free, g_object_unref);
}

gboolean
kms_rtp_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTP_ENDPOINT);
}
