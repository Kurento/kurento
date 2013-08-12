#include <config.h>
#include <gst/gst.h>

#include <kmsagnosticbin.h>
#include <kmsrtpendpoint.h>
#include <kmsautomuxerbin.h>
#include <kmsuriendpoint.h>
#include <kmsrecorderendpoint.h>
#include <kmsfilterelement.h>
#include <kmsplayerendpoint.h>
#include <kmshttpendpoint.h>

static gboolean
kurento_init (GstPlugin * kurento)
{
  if (!kms_agnostic_bin_plugin_init (kurento))
    return FALSE;

  if (!kms_rtp_end_point_plugin_init (kurento))
    return FALSE;

  if (!kms_automuxer_bin_plugin_init (kurento))
    return FALSE;

  if (!kms_uri_end_point_plugin_init (kurento))
    return FALSE;

  if (!kms_recorder_end_point_plugin_init (kurento))
    return FALSE;

  if (!kms_filter_element_plugin_init (kurento))
    return FALSE;

  if (!kms_player_end_point_plugin_init (kurento))
    return FALSE;

  if (!kms_http_end_point_plugin_init (kurento))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kurento,
    "Kurento plugin",
    kurento_init, VERSION, GST_LICENSE_UNKNOWN, "Kurento",
    "http://kurento.com/")
