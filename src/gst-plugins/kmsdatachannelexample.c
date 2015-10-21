#include <config.h>
#include <gst/gst.h>

#include "kmsshowdata.h"

static gboolean
init (GstPlugin * plugin)
{
  if (!kms_show_data_plugin_init (plugin))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsdatachannelsexample,
    "Filter documentation",
    init, VERSION, GST_LICENSE_UNKNOWN, "PACKAGE_NAME", "origin")
