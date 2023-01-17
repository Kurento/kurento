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

#include "kmswebrtctransportsinknice.h"
#include <commons/constants.h>
#include "kmsiceniceagent.h"
#include <stdlib.h>

#define GST_DEFAULT_NAME "webrtctransportsinknice"
#define GST_CAT_DEFAULT kms_webrtc_transport_sink_nice_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_sink_nice_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcTransportSinkNice, kms_webrtc_transport_sink_nice,
    KMS_TYPE_WEBRTC_TRANSPORT_SINK);

static void
kms_webrtc_transport_sink_nice_init (KmsWebrtcTransportSinkNice * self)
{
  KmsWebrtcTransportSink *parent = KMS_WEBRTC_TRANSPORT_SINK (self);

  parent->sink = gst_element_factory_make ("nicesink", NULL);

  kms_webrtc_transport_sink_connect_elements (parent);
}

static void
kms_webrtc_transport_sink_nice_component_state_changed (KmsIceBaseAgent * agent,
    char *stream_id, guint component_id, IceState state,
    KmsWebrtcTransportSink * self)
{
  gboolean is_client;

  GST_LOG_OBJECT (self,
      "[IceComponentStateChanged] state: %s, stream_id: %s, component_id: %u",
      kms_ice_base_agent_state_to_string (state), stream_id, component_id);

  g_object_get (G_OBJECT (self->dtlssrtpenc), "is-client",
          &is_client, NULL);

  if ((state == ICE_STATE_CONNECTED) && is_client) {
    kms_webrtc_transport_sink_start_dtls (self);
  }
}

void
kms_webrtc_transport_sink_nice_configure (KmsWebrtcTransportSink * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (agent);
  guint id = atoi (stream_id);

  g_object_set (G_OBJECT (self->sink),
      "agent", kms_ice_nice_agent_get_agent (nice_agent),
      "stream", id, "component", component_id,
      "sync", FALSE, "async", FALSE, NULL);

  g_signal_connect (nice_agent, "on-ice-component-state-changed",
      G_CALLBACK (kms_webrtc_transport_sink_nice_component_state_changed), self);
}

void
kms_webrtc_transport_sink_nice_set_dtls_is_client (KmsWebrtcTransportSink * self,
    gboolean is_client)
{
  KmsWebrtcTransportSinkNiceClass *klass =
      KMS_WEBRTC_TRANSPORT_SINK_NICE_CLASS (G_OBJECT_GET_CLASS (self));
  KmsWebrtcTransportSinkClass *parent_klass =
      KMS_WEBRTC_TRANSPORT_SINK_CLASS  (g_type_class_peek_parent(klass));

  parent_klass->set_dtls_is_client (self, is_client);

  if (!is_client) {
    kms_webrtc_transport_sink_start_dtls (self);
  }
 
}

static void
kms_webrtc_transport_sink_nice_class_init (KmsWebrtcTransportSinkNiceClass *
    klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  KmsWebrtcTransportSinkClass *base_class;

  base_class = KMS_WEBRTC_TRANSPORT_SINK_CLASS (klass);
  base_class->configure = kms_webrtc_transport_sink_nice_configure;
  base_class->set_dtls_is_client = kms_webrtc_transport_sink_nice_set_dtls_is_client;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSinkNice",
      "Generic",
      "WebRTC nice transport sink elements.",
      "David Fernandez Lopez <d.fernandezlop@gmail.com>");
}

KmsWebrtcTransportSinkNice *
kms_webrtc_transport_sink_nice_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE, NULL);

  return KMS_WEBRTC_TRANSPORT_SINK_NICE (obj);
}
