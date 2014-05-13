/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
#include <config.h>
#include <gst/gst.h>

#include <kmsagnosticbin.h>
#include <kmsrtpendpoint.h>
#include <kmswebrtcendpoint.h>
#include <kmsautomuxerbin.h>
#include <kmsuriendpoint.h>
#include <kmsrecorderendpoint.h>
#include <kmsfilterelement.h>
#include <kmsplayerendpoint.h>
#include <kmshttpendpoint.h>
#include <kmsmixerport.h>
#include <kmsdispatcheronetomany.h>
#include <gstsctpbasesink.h>
#include <gstsctpclientsink.h>
#include <gstsctpserversrc.h>
#include <kmscompositemixer.h>
#include <kmsdispatcher.h>
#include <kmsaudiomixer.h>
#include <kmsselectablemixer.h>

static gboolean
kurento_init (GstPlugin * kurento)
{
  if (!kms_agnostic_bin2_plugin_init (kurento))
    return FALSE;

  if (!kms_rtp_endpoint_plugin_init (kurento))
    return FALSE;

  if (!kms_webrtc_endpoint_plugin_init (kurento))
    return FALSE;

  if (!kms_automuxer_bin_plugin_init (kurento))
    return FALSE;

  if (!kms_uri_endpoint_plugin_init (kurento))
    return FALSE;

  if (!kms_recorder_endpoint_plugin_init (kurento))
    return FALSE;

  if (!kms_filter_element_plugin_init (kurento))
    return FALSE;

  if (!kms_player_endpoint_plugin_init (kurento))
    return FALSE;

  if (!kms_http_endpoint_plugin_init (kurento))
    return FALSE;

  if (!kms_mixer_port_plugin_init (kurento))
    return FALSE;

  if (!kms_dispatcher_one_to_many_plugin_init (kurento))
    return FALSE;

  if (!gst_sctp_base_sink_plugin_init (kurento))
    return FALSE;

  if (!gst_sctp_client_sink_plugin_init (kurento))
    return FALSE;

  if (!gst_sctp_server_src_plugin_init (kurento))
    return FALSE;

  if (!kms_composite_mixer_plugin_init (kurento))
    return FALSE;

  if (!kms_dispatcher_plugin_init (kurento))
    return FALSE;

  if (!kms_audio_mixer_plugin_init (kurento))
    return FALSE;

  if (!kms_selectable_mixer_plugin_init (kurento))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kurento,
    "Kurento plugin",
    kurento_init, VERSION, "LGPL", "Kurento", "http://kurento.com/")
