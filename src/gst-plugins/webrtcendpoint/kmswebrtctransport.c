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

#include <commons/kmsstats.h>

#include "kmswebrtctransport.h"
#include <stdlib.h>

#define GST_CAT_DEFAULT kmswebrtctransport
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtctransport"

#define parent_class kms_webrtc_transport_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcTransport, kms_webrtc_transport,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
        GST_DEFAULT_NAME));

static void
element_remove_probe (GstElement * e, const gchar * pad_name, gulong id)
{
  GstPad *pad;

  if (id == 0UL) {
    return;
  }

  pad = gst_element_get_static_pad (e, pad_name);
  gst_pad_remove_probe (pad, id);
  g_object_unref (pad);
}

static void
kms_webrtc_transport_finalize (GObject * object)
{
  KmsWebRtcTransport *self = KMS_WEBRTC_TRANSPORT (object);

  element_remove_probe (self->src->src, "src", self->src_probe);
  element_remove_probe (self->sink->sink, "sink", self->sink_probe);

  g_clear_object (&self->src);
  g_clear_object (&self->sink);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_webrtc_transport_class_init (KmsWebRtcTransportClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_transport_finalize;
}

static void
kms_webrtc_transport_init (KmsWebRtcTransport * self)
{
  self->src = KMS_WEBRTC_TRANSPORT_SRC (kms_webrtc_transport_src_nice_new ());
  self->sink =
      KMS_WEBRTC_TRANSPORT_SINK (kms_webrtc_transport_sink_nice_new ());
}

KmsWebRtcTransport *
kms_webrtc_transport_new (KmsIceBaseAgent * agent,
    char *stream_id, guint component_id, gchar * pem_certificate)
{
  KmsWebRtcTransport *tr;
  gchar *str;

  if (!KMS_IS_ICE_NICE_AGENT (agent)) {
    GST_ERROR ("Agent type not found");
    return NULL;
  }

  tr = KMS_WEBRTC_TRANSPORT (g_object_new (KMS_TYPE_WEBRTC_TRANSPORT, NULL));

  if (tr->sink->dtlssrtpenc == NULL || tr->src->dtlssrtpdec == NULL) {
    GST_ERROR ("SRTP plugin not available: dtlssrtpenc, dtlssrtpdec");
    g_object_unref (tr);
    return NULL;
  }

  if (pem_certificate != NULL) {
    g_object_set (G_OBJECT (tr->src->dtlssrtpdec), "pem", pem_certificate,
        NULL);
  }

  str =
      g_strdup_printf ("%s-%s-%s-%" G_GUINT32_FORMAT,
      GST_OBJECT_NAME (tr->sink->dtlssrtpenc),
      GST_OBJECT_NAME (tr->src->dtlssrtpdec), stream_id, component_id);
  g_object_set (G_OBJECT (tr->sink->dtlssrtpenc), "connection-id", str, NULL);
  g_object_set (G_OBJECT (tr->src->dtlssrtpdec), "connection-id", str, NULL);
  g_free (str);

  kms_webrtc_transport_src_configure (tr->src, agent, stream_id, component_id);
  kms_webrtc_transport_sink_configure (tr->sink, agent, stream_id,
      component_id);

  return tr;
}

void
kms_webrtc_transport_enable_latency_notification (KmsWebRtcTransport * tr,
    BufferLatencyCallback cb, gpointer user_data, GDestroyNotify destroy_data)
{
  GstPad *pad;

  element_remove_probe (tr->src->src, "src", tr->src_probe);
  pad = gst_element_get_static_pad (tr->src->src, "src");
  tr->src_probe = kms_stats_add_buffer_latency_meta_probe (pad, FALSE,
      0 /* No matter type at this point */ );
  g_object_unref (pad);

  element_remove_probe (tr->sink->sink, "sink", tr->sink_probe);
  pad = gst_element_get_static_pad (tr->sink->sink, "sink");

  tr->sink_probe = kms_stats_add_buffer_latency_notification_probe (pad, cb,
      TRUE /* Lock the data */ , user_data, destroy_data);
  g_object_unref (pad);
}

void
kms_webrtc_transport_disable_latency_notification (KmsWebRtcTransport * tr)
{
  element_remove_probe (tr->src->src, "src", tr->src_probe);
  tr->src_probe = 0UL;

  element_remove_probe (tr->sink->sink, "sink", tr->sink_probe);
  tr->sink_probe = 0UL;
}
