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
#include "kmsdummysdp.h"

#define PLUGIN_NAME "dummysdp"

GST_DEBUG_CATEGORY_STATIC (kms_dummy_sdp_debug_category);
#define GST_CAT_DEFAULT kms_dummy_sdp_debug_category

#define kms_dummy_sdp_parent_class parent_class

#define KMS_DUMMY_SDP_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (             \
    (obj),                                  \
    KMS_TYPE_DUMMY_SDP,                  \
    KmsDummySdpPrivate                   \
  )                                         \
)

G_DEFINE_TYPE_WITH_CODE (KmsDummySdp, kms_dummy_sdp,
    KMS_TYPE_BASE_SDP_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_dummy_sdp_debug_category, PLUGIN_NAME,
        0, "debug category for kurento dummy plugin"));

/* Object properties */
enum
{
  PROP_0,
  N_PROPERTIES
};

static void
kms_dummy_sdp_class_init (KmsDummySdpClass * klass)
{
  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "KmsDummySdp",
      "Generic",
      "Dummy sdp element", "David Fernandez <d.fernandezlop@gmail.com>");
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
kms_dummy_sdp_init (KmsDummySdp * self)
{
}

gboolean
kms_dummy_sdp_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_DUMMY_SDP);
}
