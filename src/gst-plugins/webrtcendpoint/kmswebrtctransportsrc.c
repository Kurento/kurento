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

#define SRTPDEC_NAME "srtp-decoder"

static void
kms_webrtc_transport_src_init (KmsWebrtcTransportSrc * self)
{
  self->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);
}

void
kms_webrtc_transport_src_connect_elements (KmsWebrtcTransportSrc * self)
{
  GstElement *srtpdec;

  gst_bin_add_many (GST_BIN (self), self->src, self->dtlssrtpdec, NULL);
  gst_element_link (self->src, self->dtlssrtpdec);

  srtpdec = gst_bin_get_by_name (GST_BIN (self->dtlssrtpdec), SRTPDEC_NAME);
  if (srtpdec != NULL) {
    g_object_set (srtpdec, "replay-window-size", RTP_RTX_SIZE, NULL);
    g_object_unref (srtpdec);
  } else {
    GST_WARNING ("Cannot get srtpdec with name %s", SRTPDEC_NAME);
  }
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
