/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <config.h>
#include <gst/gst.h>

#include "kmsagnosticbin.h"
#include "kmsagnosticbin3.h"
#include "kmshubport.h"
#include "kmsfilterelement.h"
#include "kmsaudiomixer.h"
#include "kmsaudiomixerbin.h"
#include "kmsbitratefilter.h"
#include "kmsbufferinjector.h"
#include "kmspassthrough.h"
#include "kmsdummysrc.h"
#include "kmsdummysink.h"
#include "kmsdummyduplex.h"
#include "kmsdummyrtp.h"
#include "kmsdummysdp.h"
#include "kmsdummyuri.h"

static gboolean
kurento_init (GstPlugin * kurento)
{
  if (!kms_agnostic_bin2_plugin_init (kurento))
    return FALSE;

  if (!kms_agnostic_bin3_plugin_init (kurento))
    return FALSE;

  if (!kms_filter_element_plugin_init (kurento))
    return FALSE;

  if (!kms_hub_port_plugin_init (kurento))
    return FALSE;

  if (!kms_audio_mixer_plugin_init (kurento))
    return FALSE;

  if (!kms_audio_mixer_bin_plugin_init (kurento))
    return FALSE;

  if (!kms_bitrate_filter_plugin_init (kurento))
    return FALSE;

  if (!kms_buffer_injector_plugin_init (kurento))
    return FALSE;

  if (!kms_pass_through_plugin_init (kurento))
    return FALSE;

  if (!kms_dummy_src_plugin_init (kurento))
    return FALSE;

  if (!kms_dummy_sink_plugin_init (kurento))
    return FALSE;

  if (!kms_dummy_duplex_plugin_init (kurento))
    return FALSE;

  if (!kms_dummy_sdp_plugin_init (kurento))
    return FALSE;

  if (!kms_dummy_rtp_plugin_init (kurento))
    return FALSE;

  if (!kms_dummy_uri_plugin_init (kurento))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmscore,
    "Kurento core",
    kurento_init, VERSION, GST_LICENSE_UNKNOWN, "Kurento",
    "https://kurento.openvidu.io/")
