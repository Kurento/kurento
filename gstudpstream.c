#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstudpstream.h"
#include "gstagnosticbin.h"

#define PLUGIN_NAME "udpstream"

GST_DEBUG_CATEGORY_STATIC (gst_udp_stream_debug);
#define GST_CAT_DEFAULT gst_udp_stream_debug

#define gst_udp_stream_parent_class parent_class
G_DEFINE_TYPE (GstUdpStream, gst_udp_stream, GST_TYPE_BASE_RTP_STREAM);

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
gst_udp_stream_class_init (GstUdpStreamClass * klass)
{
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "UdpStream",
      "RTP/Stream/UdpStream",
      "Udp stream element",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
gst_udp_stream_init (GstUdpStream * udp_stream)
{
}

gboolean
gst_udp_stream_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_UDP_STREAM);
}
