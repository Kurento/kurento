
#include <config.h>
#include <gst/gst.h>

#include <kmsagnosticbin.h>
#include <kmsrtpendpoint.h>
#include <kmsautomuxerbin.h>

static gboolean
kurento_init (GstPlugin * kurento)
{
  if (!kms_agnostic_bin_plugin_init (kurento))
    return FALSE;

  if (!kms_rtp_end_point_plugin_init (kurento))
    return FALSE;

  if (!kms_automuxer_bin_plugin_init (kurento))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kurento,
    "Kurento plugin",
    kurento_init, VERSION, GST_LICENSE_UNKNOWN, "Kurento",
    "http://kurento.com/")
