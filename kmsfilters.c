#include <config.h>
#include <gst/gst.h>

#if HAVE_OPENCV
#include "kmspointerdetector.h"
#endif

static gboolean
init (GstPlugin * plugin)
{
#if HAVE_OPENCV
  if (!kms_pointer_detector_plugin_init (plugin))
    return FALSE;
#endif

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsfilters,
    "Kurento filters",
    init, VERSION, GST_LICENSE_UNKNOWN, "Kurento", "http://kurento.com/")
