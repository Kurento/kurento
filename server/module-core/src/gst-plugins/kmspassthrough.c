/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmspassthrough.h"
#include "kmsagnosticcaps.h"
#include "kmsutils.h"
#include "kms-core-enumtypes.h"
#include "kmsfiltertype.h"

#define PLUGIN_NAME "passthrough"

#define DEFAULT_FILTER_TYPE KMS_FILTER_TYPE_AUTODETECT

GST_DEBUG_CATEGORY_STATIC (kms_pass_through_debug_category);
#define GST_CAT_DEFAULT kms_pass_through_debug_category

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPassThrough, kms_pass_through,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_pass_through_debug_category, PLUGIN_NAME,
        0, "debug category for passthrough element"));

static void
kms_pass_through_connect_passthrough (KmsPassThrough * self,
    KmsElementPadType type, GstElement * agnosticbin)
{
  GstPad *target = gst_element_get_static_pad (agnosticbin, "sink");

  kms_element_connect_sink_target (KMS_ELEMENT (self), target, type);
  g_object_unref (target);
}

static void
kms_pass_through_class_init (KmsPassThroughClass * klass)
{
  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PassThrough", "Generic/KmsElement", "Kurento pass_through",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");
}

static void
kms_pass_through_init (KmsPassThrough * self)
{
  kms_pass_through_connect_passthrough (self, KMS_ELEMENT_PAD_TYPE_AUDIO,
      kms_element_get_audio_agnosticbin (KMS_ELEMENT (self)));
  kms_pass_through_connect_passthrough (self, KMS_ELEMENT_PAD_TYPE_VIDEO,
      kms_element_get_video_agnosticbin (KMS_ELEMENT (self)));
}

gboolean
kms_pass_through_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PASS_THROUGH);
}
