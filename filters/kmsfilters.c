#include <config.h>
#include <gst/gst.h>

#include "kmspointerdetector.h"

static gboolean
init (GstPlugin * plugin)
{
  if (!kms_pointer_detector_plugin_init (plugin))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsfilters,
    "Kurento filters",
    init, VERSION, GST_LICENSE_UNKNOWN, "Kurento", "http://kurento.com/")
