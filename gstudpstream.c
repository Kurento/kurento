#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <nice/interfaces.h>
#include <gst/rtp/gstrtcpbuffer.h>

#include "gstudpstream.h"
#include "gstagnosticbin.h"
#include "sdp_utils.h"

#define PLUGIN_NAME "udpstream"

GST_DEBUG_CATEGORY_STATIC (gst_udp_stream_debug);
#define GST_CAT_DEFAULT gst_udp_stream_debug

#define gst_udp_stream_parent_class parent_class
G_DEFINE_TYPE (GstUdpStream, gst_udp_stream, GST_TYPE_BASE_RTP_STREAM);

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
gst_udp_stream_open_socket (guint16 port)
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
gst_udp_stream_get_socket_port (GSocket * socket)
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
gst_udp_stream_get_rtp_rtcp_sockets (GSocket ** rtp, GSocket ** rtcp)
{
  GSocket *s1, *s2;
  guint16 port1, port2;

  if (rtp == NULL || rtcp == NULL)
    return FALSE;

  s1 = gst_udp_stream_open_socket (0);

  if (s1 == NULL)
    return FALSE;

  port1 = gst_udp_stream_get_socket_port (s1);

  if ((port1 % 2) == 0)
    port2 = port1 + 1;
  else
    port2 = port1 - 1;

  s2 = gst_udp_stream_open_socket (port2);

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
  return time (NULL) + 2208988800;
}

static void
gst_udp_set_connection (KmsBaseSdpEndPoint * base_stream, GstSDPMessage * msg)
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

        if (is_ipv6 != base_stream->use_ipv6) {
          GST_DEBUG ("No valid address type: %d", is_ipv6);
          break;
        }

        name = g_resolver_lookup_by_address (resolver, addr, NULL, NULL);
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
gst_udp_stream_set_transport_to_sdp (KmsBaseSdpEndPoint * base_stream,
    GstSDPMessage * msg)
{
  GstUdpStream *udpstream = GST_UDP_STREAM (base_stream);
  gboolean ret;
  guint len, i;

  ret =
      KMS_BASE_SDP_END_POINT_CLASS
      (gst_udp_stream_parent_class)->set_transport_to_sdp (base_stream, msg);

  if (!ret)
    return FALSE;

  gst_udp_set_connection (base_stream, msg);

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
          gst_udp_stream_get_socket_port (udpstream->audio_rtp_socket);
    else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0)
      ((GstSDPMedia *) media)->port =
          gst_udp_stream_get_socket_port (udpstream->video_rtp_socket);
  }

  return TRUE;
}

static void
gst_udp_stream_start_transport_send (KmsBaseSdpEndPoint * base_stream,
    const GstSDPMessage * answer)
{
  GstUdpStream *udpstream = GST_UDP_STREAM (base_stream);
  const GstSDPConnection *con;
  guint len, i;

  KMS_BASE_SDP_END_POINT_CLASS
      (gst_udp_stream_parent_class)->start_transport_send (base_stream, answer);

  GST_DEBUG ("Start transport send");

  con = gst_sdp_message_get_connection (answer);

  len = gst_sdp_message_medias_len (answer);

  for (i = 0; i < len; i++) {
    const GstSDPConnection *media_con;
    const GstSDPMedia *media = gst_sdp_message_get_media (answer, i);

    if (g_ascii_strcasecmp ("RTP/AVP", gst_sdp_media_get_proto (media)) != 0) {
      ((GstSDPMedia *) media)->port = 0;
      continue;
    }

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
      g_object_set (udpstream->audio_rtp_udpsink, "host", con->address, "port",
          media->port, NULL);
      g_object_set (udpstream->audio_rtcp_udpsink, "host", con->address, "port",
          media->port + 1, NULL);

      gst_element_sync_state_with_parent (udpstream->audio_rtp_udpsink);
      gst_element_sync_state_with_parent (udpstream->audio_rtcp_udpsink);

      GST_DEBUG ("Audio sent to: %s:%d", con->address, media->port);
    } else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0) {
      g_object_set (udpstream->video_rtp_udpsink, "host", con->address, "port",
          gst_sdp_media_get_port (media), NULL);
      g_object_set (udpstream->video_rtcp_udpsink, "host", con->address, "port",
          gst_sdp_media_get_port (media) + 1, NULL);

      gst_element_sync_state_with_parent (udpstream->video_rtp_udpsink);
      gst_element_sync_state_with_parent (udpstream->video_rtcp_udpsink);

      GST_DEBUG ("Video sent to: %s:%d", con->address, media->port);
    }
  }
}

static void
gst_udp_stream_finalize (GObject * object)
{
  GstUdpStream *udp_stream = GST_UDP_STREAM (object);

  finalize_socket (&udp_stream->audio_rtp_socket);
  finalize_socket (&udp_stream->audio_rtcp_socket);
  finalize_socket (&udp_stream->video_rtp_socket);
  finalize_socket (&udp_stream->video_rtcp_socket);

  G_OBJECT_CLASS (gst_udp_stream_parent_class)->finalize (object);
}

static void
gst_udp_stream_class_init (GstUdpStreamClass * klass)
{
  KmsBaseSdpEndPointClass *gst_base_stream_class;
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "UdpStream",
      "RTP/Stream/UdpStream",
      "Udp stream element",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = gst_udp_stream_finalize;

  gst_base_stream_class = KMS_BASE_SDP_END_POINT_CLASS (klass);

  gst_base_stream_class->set_transport_to_sdp =
      gst_udp_stream_set_transport_to_sdp;
  gst_base_stream_class->start_transport_send =
      gst_udp_stream_start_transport_send;
}

static gboolean
gst_udp_stream_connect_video_rtcp (GstUdpStream * udp_stream)
{
  GST_DEBUG ("connect_video_rtcp");
  gst_element_link_pads (GST_BASE_RTP_STREAM (udp_stream)->rtpbin,
      "send_rtcp_src_1", udp_stream->video_rtcp_udpsink, "sink");
  return FALSE;
}

static gboolean
gst_udp_stream_connect_audio_rtcp (GstUdpStream * udp_stream)
{
  GST_DEBUG ("connect_audio_rtcp");
  gst_element_link_pads (GST_BASE_RTP_STREAM (udp_stream)->rtpbin,
      "send_rtcp_src_0", udp_stream->audio_rtcp_udpsink, "sink");
  return FALSE;
}

static void
gst_udp_stream_rtpbin_pad_added (GstElement * rtpbin, GstPad * pad,
    GstUdpStream * udp_stream)
{
  if (g_strcmp0 (GST_OBJECT_NAME (pad), "send_rtp_src_0") == 0) {
    gst_element_link_pads (rtpbin,
        "send_rtp_src_0", udp_stream->audio_rtp_udpsink, "sink");

    g_idle_add_full (G_PRIORITY_DEFAULT,
        (GSourceFunc) (gst_udp_stream_connect_audio_rtcp),
        g_object_ref (udp_stream), g_object_unref);
  } else if (g_strcmp0 (GST_OBJECT_NAME (pad), "send_rtp_src_1") == 0) {
    gst_element_link_pads (GST_BASE_RTP_STREAM (udp_stream)->rtpbin,
        "send_rtp_src_1", udp_stream->video_rtp_udpsink, "sink");

    g_idle_add_full (G_PRIORITY_DEFAULT,
        (GSourceFunc) gst_udp_stream_connect_video_rtcp,
        g_object_ref (udp_stream), g_object_unref);
  }
}

static void
gst_udp_stream_init (GstUdpStream * udp_stream)
{
  GstBaseRtpStream *base_rtp_stream = GST_BASE_RTP_STREAM (udp_stream);
  GstElement *audio_rtp_src, *audio_rtcp_src, *audio_rtp_sink, *audio_rtcp_sink,
      *video_rtp_src, *video_rtcp_src, *video_rtp_sink, *video_rtcp_sink;
  gint retries = 0;

  udp_stream->audio_rtp_socket = NULL;
  udp_stream->audio_rtcp_socket = NULL;

  udp_stream->video_rtp_socket = NULL;
  udp_stream->video_rtcp_socket = NULL;

  while (!gst_udp_stream_get_rtp_rtcp_sockets (&udp_stream->audio_rtp_socket,
          &udp_stream->audio_rtcp_socket) && retries++ < MAX_RETRIES) {
    GST_DEBUG ("Getting ports for audio failed, retring");
  }

  if (udp_stream->audio_rtp_socket == NULL)
    return;

  retries = 0;
  while (!gst_udp_stream_get_rtp_rtcp_sockets (&udp_stream->video_rtp_socket,
          &udp_stream->video_rtcp_socket) && retries++ < MAX_RETRIES) {
    GST_DEBUG ("Getting ports for video failed, retring");
  }

  if (udp_stream->video_rtp_socket == NULL) {
    finalize_socket (&udp_stream->audio_rtp_socket);
    finalize_socket (&udp_stream->audio_rtcp_socket);
    return;
  }

  GST_DEBUG ("Audio Rtp Port: %d",
      gst_udp_stream_get_socket_port (udp_stream->audio_rtp_socket));
  GST_DEBUG ("Audio Rtcp Port: %d",
      gst_udp_stream_get_socket_port (udp_stream->audio_rtcp_socket));

  GST_DEBUG ("Video Rtp Port: %d",
      gst_udp_stream_get_socket_port (udp_stream->video_rtp_socket));
  GST_DEBUG ("Video Rtcp Port: %d",
      gst_udp_stream_get_socket_port (udp_stream->video_rtcp_socket));

  audio_rtp_src = gst_element_factory_make ("udpsrc", "audio_rtp_src");
  audio_rtcp_src = gst_element_factory_make ("udpsrc", "audio_rtcp_src");

  video_rtp_src = gst_element_factory_make ("udpsrc", "video_rtp_src");
  video_rtcp_src = gst_element_factory_make ("udpsrc", "video_rtcp_src");

  audio_rtp_sink = gst_element_factory_make ("udpsink", "audio_rtp_sink");
  audio_rtcp_sink = gst_element_factory_make ("udpsink", "audio_rtcp_sink");

  video_rtp_sink = gst_element_factory_make ("udpsink", "video_rtp_sink");
  video_rtcp_sink = gst_element_factory_make ("udpsink", "video_rtcp_sink");

  udp_stream->audio_rtp_udpsink = audio_rtp_sink;
  udp_stream->audio_rtcp_udpsink = audio_rtcp_sink;

  udp_stream->video_rtp_udpsink = video_rtp_sink;
  udp_stream->video_rtcp_udpsink = video_rtcp_sink;

  g_object_set (audio_rtp_src, "socket", udp_stream->audio_rtp_socket, NULL);
  g_object_set (audio_rtcp_src, "socket", udp_stream->audio_rtcp_socket, NULL);

  g_object_set (video_rtp_src, "socket", udp_stream->video_rtp_socket, NULL);
  g_object_set (video_rtcp_src, "socket", udp_stream->video_rtcp_socket, NULL);

  g_object_set (audio_rtp_sink, "socket", udp_stream->audio_rtp_socket, NULL);
  g_object_set (audio_rtcp_sink, "socket", udp_stream->audio_rtcp_socket, NULL);

  g_object_set (video_rtp_sink, "socket", udp_stream->video_rtp_socket, NULL);
  g_object_set (video_rtcp_sink, "socket", udp_stream->video_rtcp_socket, NULL);

  gst_bin_add_many (GST_BIN (udp_stream), audio_rtp_src, audio_rtcp_src,
      audio_rtp_sink, audio_rtcp_sink, video_rtp_src, video_rtcp_src,
      video_rtp_sink, video_rtcp_sink, NULL);

  gst_element_link_pads (audio_rtp_src, "src", base_rtp_stream->rtpbin,
      "recv_rtp_sink_%u");
  gst_element_link_pads (audio_rtcp_src, "src", base_rtp_stream->rtpbin,
      "recv_rtcp_sink_%u");

  gst_element_link_pads (video_rtp_src, "src", base_rtp_stream->rtpbin,
      "recv_rtp_sink_%u");
  gst_element_link_pads (video_rtcp_src, "src", base_rtp_stream->rtpbin,
      "recv_rtcp_sink_%u");

  g_signal_connect (base_rtp_stream->rtpbin, "pad-added",
      G_CALLBACK (gst_udp_stream_rtpbin_pad_added), udp_stream);
}

gboolean
gst_udp_stream_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_UDP_STREAM);
}
