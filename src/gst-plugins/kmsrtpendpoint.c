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
  KmsLoop *loop;

  GSocket *audio_rtp_socket;
  GSocket *audio_rtcp_socket;

  GSocket *video_rtp_socket;
  GSocket *video_rtcp_socket;

  GstElement *audio_rtp_udpsink;
  GstElement *audio_rtcp_udpsink;

  GstElement *video_rtp_udpsink;
  GstElement *video_rtcp_udpsink;
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

static void
finalize_socket (GSocket ** socket)
{
  if (socket == NULL || *socket == NULL)
    return;

  g_socket_close (*socket, NULL);
  g_object_unref (*socket);
  *socket = NULL;
}

static GSocket *
kms_rtp_endpoint_open_socket (guint16 port)
{
  GSocket *socket;
  GSocketAddress *bind_saddr;
  GInetAddress *addr;

  socket = g_socket_new (G_SOCKET_FAMILY_IPV4, G_SOCKET_TYPE_DATAGRAM,
      G_SOCKET_PROTOCOL_UDP, NULL);
  if (socket == NULL)
    return NULL;

  addr = g_inet_address_new_any (G_SOCKET_FAMILY_IPV4);
  bind_saddr = g_inet_socket_address_new (addr, port);
  g_object_unref (addr);
  if (!g_socket_bind (socket, bind_saddr, TRUE, NULL)) {
    g_socket_close (socket, NULL);
    g_object_unref (socket);
    socket = NULL;
  }
  g_object_unref (bind_saddr);

  return socket;
}

static guint16
kms_rtp_endpoint_get_socket_port (GSocket * socket)
{
  GInetSocketAddress *addr;
  guint16 port;

  addr = G_INET_SOCKET_ADDRESS (g_socket_get_local_address (socket, NULL));
  if (!addr)
    return 0;

  port = g_inet_socket_address_get_port (addr);
  g_inet_socket_address_get_address (addr);
  g_object_unref (addr);

  return port;
}

static gboolean
kms_rtp_endpoint_get_rtp_rtcp_sockets (GSocket ** rtp, GSocket ** rtcp)
{
  GSocket *s1, *s2;
  guint16 port1, port2;

  if (rtp == NULL || rtcp == NULL)
    return FALSE;

  s1 = kms_rtp_endpoint_open_socket (0);

  if (s1 == NULL)
    return FALSE;

  port1 = kms_rtp_endpoint_get_socket_port (s1);

  if (port1 & 0x01)
    port2 = port1 - 1;
  else
    port2 = port1 + 1;

  s2 = kms_rtp_endpoint_open_socket (port2);

  if (s2 == NULL) {
    finalize_socket (&s1);
    return FALSE;
  }

  if (port1 < port2) {
    *rtp = s1;
    *rtcp = s2;
  } else {
    *rtp = s2;
    *rtcp = s1;
  }

  return TRUE;
}

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
  GResolver *resolver;
  gboolean done = FALSE;

  ips = nice_interfaces_get_local_ips (FALSE);

  resolver = g_resolver_get_default ();
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
        gchar *name;
        gboolean use_ipv6;

        g_object_get (base_sdp_endpoint, "use-ipv6", &use_ipv6, NULL);
        if (is_ipv6 != use_ipv6) {
          GST_DEBUG ("No valid address type: %d", is_ipv6);
          break;
        }
        // TODO: Un comment this once lookup does not leak memory
//         name = g_resolver_lookup_by_address (resolver, addr, NULL, NULL);
        name = NULL;

        if (name == NULL) {
          GST_WARNING_OBJECT (base_sdp_endpoint,
              "Cannot resolve name, using IP as name");
          name = g_strdup (l->data);
        }

        if (name != NULL) {
          const gchar *addr_type = is_ipv6 ? "IP6" : "IP4";
          gchar *ntp = g_strdup_printf ("%" G_GUINT64_FORMAT, get_ntp_time ());

          // GET for public address?
          gst_sdp_message_set_connection (msg, "IN", addr_type, l->data, 0, 0);
          gst_sdp_message_set_origin (msg, "-", ntp, ntp, "IN",
              addr_type, name);
          g_free (ntp);
          g_free (name);
          done = TRUE;
        }
        break;
      }
    }
    g_object_unref (addr);
  }
  g_object_unref (resolver);

  g_list_free_full (ips, g_free);
}

static gboolean
kms_rtp_endpoint_set_transport_to_sdp (KmsBaseSdpEndpoint * base_sdp_endpoint,
    GstSDPMessage * msg)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp_endpoint);
  gboolean ret;
  guint len, i;

  g_return_val_if_fail (msg != NULL, FALSE);

  ret =
      KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->set_transport_to_sdp (base_sdp_endpoint,
      msg);

  if (!ret)
    return FALSE;

  gst_udp_set_connection (base_sdp_endpoint, msg);

  len = gst_sdp_message_medias_len (msg);

  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (msg, i);

    if (g_ascii_strcasecmp ("RTP/AVP", gst_sdp_media_get_proto (media)) != 0) {
      ((GstSDPMedia *) media)->port = 0;
      continue;
    }

    if (gst_sdp_media_connections_len (media) != 0) {
      // TODO: If a remove api is added to gstreamer, remove all c= lines
      g_warning ("Pattern should not have connection lines");
    }

    if (g_strcmp0 ("audio", gst_sdp_media_get_media (media)) == 0)
      ((GstSDPMedia *) media)->port =
          kms_rtp_endpoint_get_socket_port (self->priv->audio_rtp_socket);
    else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0)
      ((GstSDPMedia *) media)->port =
          kms_rtp_endpoint_get_socket_port (self->priv->video_rtp_socket);
  }

  return TRUE;
}

static void
kms_rtp_endpoint_start_transport_send (KmsBaseSdpEndpoint * base_rtp_endpoint,
    const GstSDPMessage * offer, const GstSDPMessage * answer,
    gboolean local_offer)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_rtp_endpoint);
  const GstSDPConnection *con;
  const GstSDPMessage *sdp;
  guint len, i;

  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->start_transport_send
      (base_rtp_endpoint, answer, offer, local_offer);

  GST_DEBUG_OBJECT (self, "Start transport send");

  if (gst_sdp_message_medias_len (answer) != gst_sdp_message_medias_len (offer))
    GST_WARNING_OBJECT (self,
        "Incompatible offer and answer, possible errors in media");

  if (local_offer)
    sdp = answer;
  else
    sdp = offer;

  con = gst_sdp_message_get_connection (sdp);

  len = gst_sdp_message_medias_len (sdp);

  for (i = 0; i < len; i++) {
    const GstSDPConnection *media_con;
    const GstSDPMedia *offer_media = gst_sdp_message_get_media (offer, i);
    const GstSDPMedia *answer_media = gst_sdp_message_get_media (answer, i);
    const GstSDPMedia *media;

    if (offer_media == NULL || answer_media == NULL)
      continue;

    if (g_ascii_strcasecmp ("RTP/AVP",
            gst_sdp_media_get_proto (answer_media)) != 0) {
      ((GstSDPMedia *) answer_media)->port = 0;
      continue;
    }

    if (answer_media->port == 0)
      continue;

    if (local_offer)
      media = answer_media;
    else
      media = offer_media;

    if (gst_sdp_media_connections_len (media) != 0)
      media_con = gst_sdp_media_get_connection (media, 0);
    else
      media_con = con;

    if (media_con == NULL || media_con->address == NULL
        || media_con->address[0] == '\0') {
      g_warning ("Missing connection information for %s",
          gst_sdp_media_get_media (media));
      continue;
    }

    if (g_strcmp0 ("audio", gst_sdp_media_get_media (media)) == 0) {
      GstSDPDirection direction = sdp_utils_media_get_direction (media);

      // TODO: create a function to reduce code replication
      if (direction == SENDRECV || direction == RECVONLY) {
        KMS_ELEMENT_LOCK (self);
        self->priv->audio_rtp_udpsink =
            gst_element_factory_make ("udpsink", "audio_rtp_sink");
        g_object_set (self->priv->audio_rtp_udpsink, "socket",
            self->priv->audio_rtp_socket, "sync", FALSE, "async", FALSE, NULL);

        self->priv->audio_rtcp_udpsink =
            gst_element_factory_make ("udpsink", "audio_rtcp_sink");
        g_object_set (self->priv->audio_rtcp_udpsink, "socket",
            self->priv->audio_rtcp_socket, "sync", FALSE, "async", FALSE, NULL);

        gst_bin_add_many (GST_BIN (self),
            self->priv->audio_rtp_udpsink,
            self->priv->audio_rtcp_udpsink, NULL);

        g_object_set (self->priv->audio_rtp_udpsink, "host",
            media_con->address, "port", gst_sdp_media_get_port (media), NULL);
        g_object_set (self->priv->audio_rtcp_udpsink, "host",
            media_con->address, "port", gst_sdp_media_get_port (media) + 1,
            NULL);

        gst_element_sync_state_with_parent (self->priv->audio_rtp_udpsink);
        gst_element_sync_state_with_parent (self->priv->audio_rtcp_udpsink);
        KMS_ELEMENT_UNLOCK (self);

        GST_DEBUG_OBJECT (base_rtp_endpoint, "Audio sent to: %s:%d",
            media_con->address, media->port);
      }
    } else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0) {
      GstSDPDirection direction = sdp_utils_media_get_direction (media);

      if (direction == SENDRECV || direction == RECVONLY) {
        KMS_ELEMENT_LOCK (self);
        self->priv->video_rtp_udpsink =
            gst_element_factory_make ("udpsink", "video_rtp_sink");
        g_object_set (self->priv->video_rtp_udpsink, "socket",
            self->priv->video_rtp_socket, "sync", FALSE, "async", FALSE, NULL);

        self->priv->video_rtcp_udpsink =
            gst_element_factory_make ("udpsink", "video_rtcp_sink");
        g_object_set (self->priv->video_rtcp_udpsink, "socket",
            self->priv->video_rtcp_socket, "sync", FALSE, "async", FALSE, NULL);

        gst_bin_add_many (GST_BIN (self),
            self->priv->video_rtp_udpsink,
            self->priv->video_rtcp_udpsink, NULL);

        g_object_set (self->priv->video_rtp_udpsink, "host",
            media_con->address, "port", gst_sdp_media_get_port (media), NULL);
        g_object_set (self->priv->video_rtcp_udpsink, "host",
            media_con->address, "port", gst_sdp_media_get_port (media) + 1,
            NULL);

        gst_element_sync_state_with_parent (self->priv->video_rtp_udpsink);
        gst_element_sync_state_with_parent (self->priv->video_rtcp_udpsink);
        KMS_ELEMENT_UNLOCK (self);

        GST_DEBUG_OBJECT (base_rtp_endpoint, "Video sent to: %s:%d",
            media_con->address, media->port);
      }
    }
  }
}

static void
kms_rtp_endpoint_dispose (GObject * object)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_ELEMENT_LOCK (self);
  g_clear_object (&self->priv->loop);
  KMS_ELEMENT_UNLOCK (self);

  G_OBJECT_CLASS (kms_rtp_endpoint_parent_class)->dispose (object);
}

static void
kms_rtp_endpoint_finalize (GObject * object)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  finalize_socket (&self->priv->audio_rtp_socket);
  finalize_socket (&self->priv->audio_rtcp_socket);
  finalize_socket (&self->priv->video_rtp_socket);
  finalize_socket (&self->priv->video_rtcp_socket);

  G_OBJECT_CLASS (kms_rtp_endpoint_parent_class)->finalize (object);
}

static void
kms_rtp_endpoint_class_init (KmsRtpEndpointClass * klass)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class;
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
  gobject_class->dispose = kms_rtp_endpoint_dispose;
  gobject_class->finalize = kms_rtp_endpoint_finalize;

  base_sdp_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);

  base_sdp_endpoint_class->set_transport_to_sdp =
      kms_rtp_endpoint_set_transport_to_sdp;
  base_sdp_endpoint_class->start_transport_send =
      kms_rtp_endpoint_start_transport_send;

  g_type_class_add_private (klass, sizeof (KmsRtpEndpointPrivate));
}

static gboolean
kms_rtp_endpoint_connect_video_rtcp (KmsRtpEndpoint * self)
{
  GST_DEBUG_OBJECT (self, "connect_video_rtcp");
  gst_element_link_pads (kms_base_rtp_endpoint_get_rtpbin
      (KMS_BASE_RTP_ENDPOINT (self)), VIDEO_RTPBIN_SEND_RTCP_SRC,
      self->priv->video_rtcp_udpsink, "sink");
  return FALSE;
}

static gboolean
kms_rtp_endpoint_connect_audio_rtcp (KmsRtpEndpoint * self)
{
  GST_DEBUG_OBJECT (self, "connect_audio_rtcp");
  gst_element_link_pads (kms_base_rtp_endpoint_get_rtpbin
      (KMS_BASE_RTP_ENDPOINT (self)), AUDIO_RTPBIN_SEND_RTCP_SRC,
      self->priv->audio_rtcp_udpsink, "sink");
  return FALSE;
}

static void
kms_rtp_endpoint_rtpbin_pad_added (GstElement * rtpbin, GstPad * pad,
    KmsRtpEndpoint * self)
{
  if (g_strcmp0 (GST_OBJECT_NAME (pad), AUDIO_RTPBIN_SEND_RTP_SRC) == 0) {
    KMS_ELEMENT_LOCK (self);
    if (self->priv->audio_rtp_udpsink == NULL) {
      GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

      GST_WARNING_OBJECT (self, "RtpEndpoint not configured to send audio");
      gst_bin_add (GST_BIN (self), fakesink);
      gst_element_sync_state_with_parent (fakesink);
      gst_element_link_pads (rtpbin, AUDIO_RTPBIN_SEND_RTP_SRC, fakesink, NULL);
      KMS_ELEMENT_UNLOCK (self);

      return;
    }

    gst_element_link_pads (rtpbin, AUDIO_RTPBIN_SEND_RTP_SRC,
        self->priv->audio_rtp_udpsink, "sink");

    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_DEFAULT,
        (GSourceFunc) (kms_rtp_endpoint_connect_audio_rtcp),
        g_object_ref (self), g_object_unref);
    KMS_ELEMENT_UNLOCK (self);
  } else if (g_strcmp0 (GST_OBJECT_NAME (pad), VIDEO_RTPBIN_SEND_RTP_SRC) == 0) {
    KMS_ELEMENT_LOCK (self);
    if (self->priv->video_rtp_udpsink == NULL) {
      GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

      GST_WARNING_OBJECT (self, "RtpEndpoint not configured to send video");
      gst_bin_add (GST_BIN (self), fakesink);
      gst_element_sync_state_with_parent (fakesink);
      gst_element_link_pads (rtpbin, VIDEO_RTPBIN_SEND_RTP_SRC, fakesink, NULL);
      KMS_ELEMENT_UNLOCK (self);

      return;
    }

    gst_element_link_pads (rtpbin, VIDEO_RTPBIN_SEND_RTP_SRC,
        self->priv->video_rtp_udpsink, "sink");

    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_DEFAULT,
        (GSourceFunc) kms_rtp_endpoint_connect_video_rtcp,
        g_object_ref (self), g_object_unref);
    KMS_ELEMENT_UNLOCK (self);
  }
}

static void
kms_rtp_endpoint_init (KmsRtpEndpoint * self)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint = KMS_BASE_RTP_ENDPOINT (self);
  GstElement *audio_rtp_src, *audio_rtcp_src, *video_rtp_src, *video_rtcp_src,
      *rtpbin;
  gint retries = 0;

  self->priv = KMS_RTP_ENDPOINT_GET_PRIVATE (self);

  self->priv->loop = kms_loop_new ();

  self->priv->audio_rtp_socket = NULL;
  self->priv->audio_rtcp_socket = NULL;

  self->priv->video_rtp_socket = NULL;
  self->priv->video_rtcp_socket = NULL;

  while (!kms_rtp_endpoint_get_rtp_rtcp_sockets
      (&self->priv->audio_rtp_socket, &self->priv->audio_rtcp_socket)
      && retries++ < MAX_RETRIES) {
    GST_DEBUG ("Getting ports for audio failed, retring");
  }

  if (self->priv->audio_rtp_socket == NULL)
    return;

  retries = 0;
  while (!kms_rtp_endpoint_get_rtp_rtcp_sockets
      (&self->priv->video_rtp_socket, &self->priv->video_rtcp_socket)
      && retries++ < MAX_RETRIES) {
    GST_DEBUG ("Getting ports for video failed, retring");
  }

  if (self->priv->video_rtp_socket == NULL) {
    finalize_socket (&self->priv->audio_rtp_socket);
    finalize_socket (&self->priv->audio_rtcp_socket);
    return;
  }

  GST_DEBUG ("Audio Rtp Port: %d",
      kms_rtp_endpoint_get_socket_port (self->priv->audio_rtp_socket));
  GST_DEBUG ("Audio Rtcp Port: %d",
      kms_rtp_endpoint_get_socket_port (self->priv->audio_rtcp_socket));

  GST_DEBUG ("Video Rtp Port: %d",
      kms_rtp_endpoint_get_socket_port (self->priv->video_rtp_socket));
  GST_DEBUG ("Video Rtcp Port: %d",
      kms_rtp_endpoint_get_socket_port (self->priv->video_rtcp_socket));

  audio_rtp_src = gst_element_factory_make ("udpsrc", "audio_rtp_src");
  audio_rtcp_src = gst_element_factory_make ("udpsrc", "audio_rtcp_src");

  video_rtp_src = gst_element_factory_make ("udpsrc", "video_rtp_src");
  video_rtcp_src = gst_element_factory_make ("udpsrc", "video_rtcp_src");

  g_object_set (audio_rtp_src, "socket", self->priv->audio_rtp_socket, NULL);
  g_object_set (audio_rtcp_src, "socket", self->priv->audio_rtcp_socket, NULL);

  g_object_set (video_rtp_src, "socket", self->priv->video_rtp_socket, NULL);
  g_object_set (video_rtcp_src, "socket", self->priv->video_rtcp_socket, NULL);

  gst_bin_add_many (GST_BIN (self), audio_rtp_src, audio_rtcp_src,
      video_rtp_src, video_rtcp_src, NULL);

  rtpbin = kms_base_rtp_endpoint_get_rtpbin (base_rtp_endpoint);
  gst_element_link_pads (audio_rtp_src, "src", rtpbin, "recv_rtp_sink_%u");
  gst_element_link_pads (audio_rtcp_src, "src", rtpbin, "recv_rtcp_sink_%u");
  gst_element_link_pads (video_rtp_src, "src", rtpbin, "recv_rtp_sink_%u");
  gst_element_link_pads (video_rtcp_src, "src", rtpbin, "recv_rtcp_sink_%u");

  g_signal_connect (rtpbin, "pad-added",
      G_CALLBACK (kms_rtp_endpoint_rtpbin_pad_added), self);
}

gboolean
kms_rtp_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTP_ENDPOINT);
}
