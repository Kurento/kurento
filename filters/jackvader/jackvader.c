#include <config.h>
#include <gst/gst.h>

#include "kmsjackvader.h"

static gboolean
init (GstPlugin * plugin)
{
  if (!kms_jack_vader_plugin_init (plugin))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsjackvader,
    "Kurento jackvader filter",
    init, VERSION, GST_LICENSE_UNKNOWN, "Kurento", "http://kurento.com/")
