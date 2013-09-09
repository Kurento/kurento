#include <config.h>
#include <gst/gst.h>

#include "kmsh264filter.h"

static gboolean
init (GstPlugin * plugin)
{
  if (!kms_h264_filter_plugin_init (plugin))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsh264filter,
    "Kurento h264filter filter",
    init, VERSION, GST_LICENSE_UNKNOWN, "Kurento", "http://kurento.com/")
