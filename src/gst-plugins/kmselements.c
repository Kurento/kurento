/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include "kmshttpendpoint.h"
#include "kmshttppostendpoint.h"
#include "kmsplayerendpoint.h"
#include "kmsdispatcher.h"
#include "kmsdispatcheronetomany.h"
#include "kmsselectablemixer.h"
#include "kmscompositemixer.h"
#include "kmsalphablending.h"

static gboolean
kurento_init (GstPlugin * kurento)
{
  if (!kms_http_endpoint_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_http_post_endpoint_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_player_endpoint_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_dispatcher_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_dispatcher_one_to_many_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_selectable_mixer_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_composite_mixer_plugin_init (kurento)) {
    return FALSE;
  }

  if (!kms_alpha_blending_plugin_init (kurento))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmselements,
    "Kurento elements",
    kurento_init, VERSION, GST_LICENSE_UNKNOWN, "Kurento Elements",
    "http://kurento.com/")
