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
#  include <config.h>
#endif

#include "kmswebrtctransportsrc.h"
#include <commons/constants.h>

#define GST_DEFAULT_NAME "webrtctransportsrc"
#define GST_CAT_DEFAULT kms_webrtc_transport_src_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_src_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcTransportSrc, kms_webrtc_transport_src, GST_TYPE_BIN);

#define SRTPDEC_FACTORY_NAME "srtpdec"

static void
kms_webrtc_transport_src_init (KmsWebrtcTransportSrc * self)
{
  self->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);
}

void
kms_webrtc_transport_src_connect_elements (KmsWebrtcTransportSrc * self)
{
  GstElement *element;

  gst_bin_add_many (GST_BIN (self), self->src, self->dtlssrtpdec, NULL);
  gst_element_link (self->src, self->dtlssrtpdec);

  // Iterate over the dtlssrtpdec bin to set the srtpdec
  GstIterator *it = gst_bin_iterate_elements (GST_BIN (self->dtlssrtpdec));
  GValue item = G_VALUE_INIT;
  gboolean done = FALSE;

  while (!done) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:
        element = g_value_get_object (&item);

        if (g_strcmp0 (gst_plugin_feature_get_name (GST_PLUGIN_FEATURE
                (gst_element_get_factory (element))), SRTPDEC_FACTORY_NAME) == 0) {
          g_object_set (element, "replay-window-size", RTP_RTX_SIZE, NULL);
        }

        g_value_reset (&item);
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        done = TRUE;
        break;
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  }

  gst_iterator_free (it);
}

void
kms_webrtc_transport_src_configure_default (KmsWebrtcTransportSrc * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsWebrtcTransportSrcClass *klass =
      KMS_WEBRTC_TRANSPORT_SRC_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->configure == kms_webrtc_transport_src_configure_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'configure'", G_OBJECT_CLASS_NAME (klass));
  }
}

void
kms_webrtc_transport_src_configure (KmsWebrtcTransportSrc * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsWebrtcTransportSrcClass *klass =
      KMS_WEBRTC_TRANSPORT_SRC_CLASS (G_OBJECT_GET_CLASS (self));

  klass->configure (self, agent, stream_id, component_id);
}

static void
kms_webrtc_transport_src_class_init (KmsWebrtcTransportSrcClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  klass->configure = kms_webrtc_transport_src_configure_default;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSrc",
      "Generic",
      "Base bin to manage WebRTC transport src elements.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}

KmsWebrtcTransportSrc *
kms_webrtc_transport_src_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SRC, NULL);

  return KMS_WEBRTC_TRANSPORT_SRC (obj);
}
