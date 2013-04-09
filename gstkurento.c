
#include <config.h>
#include <gst/gst.h>

#include <gstagnosticbin.h>
#include <gstudpstream.h>

static gboolean
kurento_init (GstPlugin * kurento)
{
  if (!gst_agnostic_bin_plugin_init (kurento))
    return FALSE;

  if (!gst_udp_stream_plugin_init (kurento))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kurento,
    "Kurento plugin",
    kurento_init, VERSION, GST_LICENSE_UNKNOWN, "Kurento",
    "http://kurento.com/")
