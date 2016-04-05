/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#include <commons/kmsstats.h>

#include "kmswebrtctransport.h"
#include <stdlib.h>

#define GST_CAT_DEFAULT kmswebrtctransport
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtctransport"

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

void
kms_webrtc_transport_destroy (KmsWebRtcTransport * tr)
{
  if (tr == NULL) {
    return;
  }

  element_remove_probe (tr->src->src, "src", tr->src_probe);
  element_remove_probe (tr->sink->sink, "sink", tr->sink_probe);

  g_clear_object (&tr->src);
  g_clear_object (&tr->sink);

  g_slice_free (KmsWebRtcTransport, tr);
}

KmsWebRtcTransport *
kms_webrtc_transport_create (KmsIceBaseAgent * agent, char *stream_id,
    guint component_id, gchar * pem_certificate)
{
  KmsWebRtcTransport *tr;
  gchar *str;

  tr = g_slice_new0 (KmsWebRtcTransport);

  if (KMS_IS_ICE_NICE_AGENT (agent)) {
    tr->src = KMS_WEBRTC_TRANSPORT_SRC (kms_webrtc_transport_src_nice_new ());
    tr->sink =
        KMS_WEBRTC_TRANSPORT_SINK (kms_webrtc_transport_sink_nice_new ());
  } else {
    GST_ERROR_OBJECT (tr, "Agent type not found");
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

static void init_debug (void) __attribute__ ((constructor));

static void
init_debug (void)
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
