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
#define DTLS_ENCODER_FACTORY_NAME "dtlsenc"



// {{{{ FIXME: This can be deleted when we start using GStreamer >=1.18 for Kurento.
// Code sourced from GStreamer/gstbin.c >=1.18
//
// https://gitlab.freedesktop.org/gstreamer/gstreamer/-/blob/10f72da5040b74678c8f81723971127ee8bee04f/subprojects/gstreamer/gst/gstbin.c#L4526-4537
static gint
compare_factory_names (const GValue *velement, GValue *factory_name_val)
{
  GstElement *element = g_value_get_object (velement);
  GstElementFactory *factory = gst_element_get_factory (element);
  const gchar *factory_name = g_value_get_string (factory_name_val);

  if (factory == NULL)
    return -1;

  return g_strcmp0 (GST_OBJECT_NAME (factory), factory_name);
}
//
// https://gitlab.freedesktop.org/gstreamer/gstreamer/-/blob/10f72da5040b74678c8f81723971127ee8bee04f/subprojects/gstreamer/gst/gstbin.c#L4553-4574
static GstIterator *
gst_bin_iterate_all_by_element_factory_name (GstBin *bin,
    const gchar *factory_name)
{
  GstIterator *children;
  GstIterator *result;
  GValue factory_name_val = G_VALUE_INIT;

  g_return_val_if_fail (GST_IS_BIN (bin), NULL);
  g_return_val_if_fail (factory_name && *factory_name, NULL);

  g_value_init (&factory_name_val, G_TYPE_STRING);
  g_value_set_string (&factory_name_val, factory_name);

  children = gst_bin_iterate_recurse (bin);
  result = gst_iterator_filter (children, (GCompareFunc)compare_factory_names,
      &factory_name_val);

  g_value_unset (&factory_name_val);

  return result;
}
// }}}}

static GstElement *
kms_webrtc_transport_sink_get_element_in_dtlssrtpenc (
    KmsWebrtcTransportSink *self,
    const gchar *factory_name)
{
  GstElement *element = NULL;
  GValue velement = G_VALUE_INIT;

  GstIterator *iter = gst_bin_iterate_all_by_element_factory_name (
      GST_BIN (self->dtlssrtpenc), factory_name);

  if (gst_iterator_next (iter, &velement) == GST_ITERATOR_OK) {
    // Assume only one element of the given type. This is the case in dtlssrtpenc.
    element = g_value_dup_object (&velement);
    g_value_reset (&velement);
  }

  if (gst_iterator_next (iter, &velement) != GST_ITERATOR_DONE) {
    GST_WARNING_OBJECT (self,
        "BUG: Several elements '%s' found in dtlssrtpenc; code assumes only one",
        factory_name);
  }

  if (element == NULL) {
    GST_WARNING_OBJECT (self, "BUG: Element '%s' not found in dtlssrtpenc",
        factory_name);
  }

  g_value_unset (&velement);
  gst_iterator_free (iter);

  return element;
}

static void
kms_webrtc_transport_sink_init (KmsWebrtcTransportSink * self)
{
  GstElement *dtls_encoder;

  self->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);

  dtls_encoder = kms_webrtc_transport_sink_get_element_in_dtlssrtpenc (self,
      DTLS_ENCODER_FACTORY_NAME);

  if (dtls_encoder != NULL) {
    gst_element_set_locked_state (dtls_encoder, TRUE);

    g_object_unref (dtls_encoder);
  }
}

void
kms_webrtc_transport_sink_connect_elements (KmsWebrtcTransportSink *self)
{
  GstElement *funnel, *srtpenc;

  gst_bin_add_many (GST_BIN (self), self->dtlssrtpenc, self->sink, NULL);
  gst_element_link (self->dtlssrtpenc, self->sink);

  funnel = kms_webrtc_transport_sink_get_element_in_dtlssrtpenc (self,
      FUNNEL_FACTORY_NAME);

  if (funnel != NULL) {
    g_object_set (funnel, "forward-sticky-events-mode", 0 /* never */, NULL);
    g_object_unref (funnel);
  }

  srtpenc = kms_webrtc_transport_sink_get_element_in_dtlssrtpenc (self,
      SRTPENC_FACTORY_NAME);

  if (srtpenc != NULL) {
    g_object_set (srtpenc, "allow-repeat-tx", TRUE, "replay-window-size",
        RTP_RTX_SIZE, NULL);
    g_object_unref (srtpenc);
  }
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
kms_webrtc_transport_sink_set_dtls_is_client_default (KmsWebrtcTransportSink * self,
    gboolean is_client)
{
  g_object_set (G_OBJECT (self->dtlssrtpenc), "is-client", is_client, NULL);
  if (is_client) {
    GST_DEBUG_OBJECT(self, "Set as DTLS client (handshake initiator)");
  } else {
    GST_DEBUG_OBJECT(self, "Set as DTLS server (wait for handshake)");
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

void
kms_webrtc_transport_sink_set_dtls_is_client (KmsWebrtcTransportSink * self,
    gboolean is_client)
{
  KmsWebrtcTransportSinkClass *klass =
      KMS_WEBRTC_TRANSPORT_SINK_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_dtls_is_client (self, is_client);
}

static void
kms_webrtc_transport_sink_class_init (KmsWebrtcTransportSinkClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  klass->configure = kms_webrtc_transport_sink_configure_default;
  klass->set_dtls_is_client = kms_webrtc_transport_sink_set_dtls_is_client_default;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSink",
      "Generic",
      "Base bin to manage WebRTC transport sink elements.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}

void
kms_webrtc_transport_sink_start_dtls (KmsWebrtcTransportSink * self)
{
  GstElement *dtls_encoder;

  dtls_encoder = kms_webrtc_transport_sink_get_element_in_dtlssrtpenc (self,
      DTLS_ENCODER_FACTORY_NAME);

  if (dtls_encoder != NULL) {
    gst_element_set_locked_state (dtls_encoder, FALSE);
    gst_element_sync_state_with_parent (dtls_encoder);
    GST_DEBUG_OBJECT (self, "Starting DTLS");

    g_object_unref (dtls_encoder);
  }
}

KmsWebrtcTransportSink *
kms_webrtc_transport_sink_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SINK, NULL);

  return KMS_WEBRTC_TRANSPORT_SINK (obj);
}
