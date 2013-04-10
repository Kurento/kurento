#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "gstudpstream.h"
#include "gstagnosticbin.h"

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
dispose_socket (GSocket ** socket)
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
    dispose_socket (&s1);
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

static void
gst_udp_stream_dispose (GObject * object)
{
  GstUdpStream *udp_stream = GST_UDP_STREAM (object);

  dispose_socket (&udp_stream->audio_rtp_socket);
  dispose_socket (&udp_stream->audio_rtcp_socket);
  dispose_socket (&udp_stream->video_rtp_socket);
  dispose_socket (&udp_stream->video_rtcp_socket);

  G_OBJECT_CLASS (gst_udp_stream_parent_class)->dispose (object);
}

static void
gst_udp_stream_class_init (GstUdpStreamClass * klass)
{
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
  gobject_class->dispose = gst_udp_stream_dispose;
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
    dispose_socket (&udp_stream->audio_rtp_socket);
    dispose_socket (&udp_stream->audio_rtcp_socket);
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

  // TODO: Connect udp sink using callbacks
}

gboolean
gst_udp_stream_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_UDP_STREAM);
}
