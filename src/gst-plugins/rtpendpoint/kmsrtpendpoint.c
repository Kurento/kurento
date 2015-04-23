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
#include <commons/sdpagent/kmssdprtpavpfmediahandler.h>

#define PLUGIN_NAME "rtpendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_rtp_endpoint_debug);
#define GST_CAT_DEFAULT kms_rtp_endpoint_debug

#define kms_rtp_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsRtpEndpoint, kms_rtp_endpoint, KMS_TYPE_BASE_RTP_ENDPOINT);

#define MAX_RETRIES 4

/* Signals and args */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0
};

/* Media handler management begin */
static void
kms_rtp_endpoint_create_media_handler (KmsBaseSdpEndpoint * base_sdp,
    KmsSdpMediaHandler ** handler)
{
  *handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_rtp_avpf_media_handler_new ());

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->create_media_handler (base_sdp, handler);
}

/* Media handler management end */

/* Connection management begin */
static KmsIRtpConnection *
kms_rtp_endpoint_create_connection (KmsBaseRtpEndpoint * base_rtp_endpoint,
    const gchar * name)
{
  KmsRtpConnection *conn = kms_rtp_connection_new ();

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
    SdpMediaConfig * mconf)
{
  KmsBaseRtpEndpoint *base_rtp = KMS_BASE_RTP_ENDPOINT (self);
  KmsIRtpConnection *conn;

  conn = kms_base_rtp_endpoint_get_connection (base_rtp, mconf);
  if (conn == NULL) {
    return NULL;
  }

  return KMS_RTP_BASE_CONNECTION (conn);
}

/* Connection management end */

static void
kms_rtp_endpoint_set_addr (KmsRtpEndpoint * self)
{
  GList *ips, *l;
  gboolean done = FALSE;

  ips = nice_interfaces_get_local_ips (FALSE);
  for (l = ips; l != NULL && !done; l = l->next) {
    GInetAddress *addr;
    gboolean is_ipv6 = FALSE;

    addr = g_inet_address_new_from_string (l->data);
    if (G_IS_INET_ADDRESS (addr)) {
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

          g_object_get (self, "use-ipv6", &use_ipv6, NULL);
          if (is_ipv6 != use_ipv6) {
            GST_DEBUG_OBJECT (self, "No valid address type: %d", is_ipv6);
            break;
          }

          addr_str = g_inet_address_to_string (addr);
          if (addr_str != NULL) {
            KmsBaseSdpEndpoint *base_sdp = KMS_BASE_SDP_ENDPOINT (self);
            KmsSdpAgent *agent = kms_base_sdp_endpoint_get_sdp_agent (base_sdp);

            g_object_set (agent, "addr", addr_str, NULL);
            g_free (addr_str);
            done = TRUE;
          }
          break;
        }
      }
    }

    if (G_IS_OBJECT (addr)) {
      g_object_unref (addr);
    }
  }

  g_list_free_full (ips, g_free);

  if (!done) {
    GST_WARNING_OBJECT (self, "Addr not set");
  }
}

/* Configure media SDP begin */
static gboolean
kms_rtp_endpoint_configure_media (KmsBaseSdpEndpoint * base_sdp_endpoint,
    SdpMediaConfig * mconf)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp_endpoint);
  KmsBaseRtpEndpoint *base_rtp = KMS_BASE_RTP_ENDPOINT (self);
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  guint conn_len, c;
  guint attr_len, a;
  KmsRtpBaseConnection *conn;
  gboolean ret = TRUE;

  /* Chain up */
  ret = KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->configure_media (base_sdp_endpoint,
      mconf);
  if (ret == FALSE) {
    return FALSE;
  }

  conn_len = gst_sdp_media_connections_len (media);
  for (c = 0; c < conn_len; c++) {
    gst_sdp_media_remove_connection (media, c);
  }

  conn =
      KMS_RTP_BASE_CONNECTION (kms_base_rtp_endpoint_get_connection (base_rtp,
          mconf));
  if (conn == NULL) {
    return TRUE;
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

  return TRUE;
}

/* Configure media SDP end */

static void
kms_rtp_endpoint_start_transport_send (KmsBaseSdpEndpoint *
    base_sdp_endpoint, gboolean offerer)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp_endpoint);
  SdpMessageContext *remote_ctx =
      kms_base_sdp_endpoint_get_remote_sdp_ctx (base_sdp_endpoint);
  const GstSDPMessage *sdp =
      kms_sdp_message_context_get_sdp_message (remote_ctx);
  const GSList *item = kms_sdp_message_context_get_medias (remote_ctx);
  const GstSDPConnection *msg_conn = gst_sdp_message_get_connection (sdp);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS (parent_class)->start_transport_send
      (base_sdp_endpoint, offerer);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *mconf = item->data;
    GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
    const GstSDPConnection *media_con;
    KmsRtpBaseConnection *conn;
    guint port;

    if (media->port == 0) {
      continue;
    }

    if (gst_sdp_media_connections_len (media) != 0) {
      media_con = gst_sdp_media_get_connection (media, 0);
    } else {
      media_con = msg_conn;
    }

    if (media_con == NULL || media_con->address == NULL
        || media_con->address[0] == '\0') {
      const gchar *media_str = gst_sdp_media_get_media (media);

      GST_WARNING_OBJECT (self, "Missing connection information for '%s'",
          media_str);
      continue;
    }

    conn = kms_rtp_endpoint_media_get_connection (self, mconf);
    if (conn == NULL) {
      continue;
    }

    port = gst_sdp_media_get_port (media);
    kms_rtp_base_connection_set_remote_info (conn,
        media_con->address, port, port + 1);
    /* TODO: get rtcp port from attr if it exists */
  }
}

static void
kms_rtp_endpoint_class_init (KmsRtpEndpointClass * klass)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class;
  KmsBaseRtpEndpointClass *base_rtp_endpoint_class;
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "RtpEndpoint",
      "RTP/Stream/RtpEndpoint",
      "Rtp Endpoint element",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);
  base_sdp_endpoint_class->start_transport_send =
      kms_rtp_endpoint_start_transport_send;

  /* Media handler management */
  base_sdp_endpoint_class->create_media_handler =
      kms_rtp_endpoint_create_media_handler;

  base_sdp_endpoint_class->configure_media = kms_rtp_endpoint_configure_media;

  base_rtp_endpoint_class = KMS_BASE_RTP_ENDPOINT_CLASS (klass);
  /* Connection management */
  base_rtp_endpoint_class->create_connection =
      kms_rtp_endpoint_create_connection;
  base_rtp_endpoint_class->create_bundle_connection =
      kms_rtp_endpoint_create_bundle_connection;
}

/* inmediate-TODO: not add abs-send-time extmap */

static void
kms_rtp_endpoint_init (KmsRtpEndpoint * self)
{
  g_object_set (G_OBJECT (self), "bundle",
      FALSE, "rtcp-mux", FALSE, "rtcp-nack", FALSE, "rtcp-remb", FALSE,
      "max-video-recv-bandwidth", 0, NULL);
  /* FIXME: remove max-video-recv-bandwidth when it b=AS:X is in the SDP offer */

  kms_rtp_endpoint_set_addr (self);
}

gboolean
kms_rtp_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTP_ENDPOINT);
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsrtpendpoint,
    "Kurento rtp endpoint",
    kms_rtp_endpoint_plugin_init, VERSION, "LGPL",
    "Kurento Elements", "http://kurento.com/")
