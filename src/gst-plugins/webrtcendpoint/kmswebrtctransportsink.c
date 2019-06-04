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

#include "kmswebrtctransportsink.h"
#include <commons/constants.h>

#define GST_DEFAULT_NAME "webrtctransportsink"
#define GST_CAT_DEFAULT kms_webrtc_transport_sink_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_sink_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcTransportSink, kms_webrtc_transport_sink, GST_TYPE_BIN);

#define FUNNEL_FACTORY_NAME "funnel"
#define SRTPENC_FACTORY_NAME "srtpenc"

static void
kms_webrtc_transport_sink_init (KmsWebrtcTransportSink * self)
{
  self->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);
}

void
kms_webrtc_transport_sink_connect_elements (KmsWebrtcTransportSink * self)
{
  GstElement *element;

  gst_bin_add_many (GST_BIN (self), self->dtlssrtpenc, self->sink, NULL);
  gst_element_link (self->dtlssrtpenc, self->sink);

  // Iterate over the dtlssrtpenc bin to set the srtpenc and funnel props
  GstIterator *it = gst_bin_iterate_elements (GST_BIN (self->dtlssrtpenc));
  GValue item = G_VALUE_INIT;
  gboolean done = FALSE;

  while (!done) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:
        element = g_value_get_object (&item);

        if (g_strcmp0 (gst_plugin_feature_get_name (GST_PLUGIN_FEATURE
                (gst_element_get_factory (element))), SRTPENC_FACTORY_NAME) == 0) {
          g_object_set (G_OBJECT (element),
              "allow-repeat-tx", TRUE, "replay-window-size", RTP_RTX_SIZE, NULL);
        } else if (g_strcmp0 (gst_plugin_feature_get_name (GST_PLUGIN_FEATURE
                (gst_element_get_factory (element))), FUNNEL_FACTORY_NAME) == 0) {
          g_object_set (element, "forward-sticky-events", FALSE, NULL);
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
kms_webrtc_transport_sink_configure_default (KmsWebrtcTransportSink * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsWebrtcTransportSinkClass *klass =
      KMS_WEBRTC_TRANSPORT_SINK_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->configure == kms_webrtc_transport_sink_configure_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'configure'", G_OBJECT_CLASS_NAME (klass));
  }
}

void
kms_webrtc_transport_sink_configure (KmsWebrtcTransportSink * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsWebrtcTransportSinkClass *klass =
      KMS_WEBRTC_TRANSPORT_SINK_CLASS (G_OBJECT_GET_CLASS (self));

  klass->configure (self, agent, stream_id, component_id);
}

static void
kms_webrtc_transport_sink_class_init (KmsWebrtcTransportSinkClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  klass->configure = kms_webrtc_transport_sink_configure_default;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSink",
      "Generic",
      "Base bin to manage WebRTC transport sink elements.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}

KmsWebrtcTransportSink *
kms_webrtc_transport_sink_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SINK, NULL);

  return KMS_WEBRTC_TRANSPORT_SINK (obj);
}
