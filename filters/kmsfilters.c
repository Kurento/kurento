#include <config.h>
#include <gst/gst.h>

static gboolean
init (GstPlugin * plugin)
{
  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsfilters,
    "Kurento filters",
    init, VERSION, GST_LICENSE_UNKNOWN, "Kurento", "http://kurento.com/")
