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

#include <commons/kmsutils.h>

#include "kmswebrtctransport.h"

#define GST_CAT_DEFAULT kmswebrtctransport
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtctransport"

#define FUNNEL_NAME "funnel"
#define SRTPENC_NAME "srtp-encoder"
#define SRTPDEC_NAME "srtp-decoder"
#define REPLAY_WINDOW_SIZE 512

void
kms_webrtc_transport_nice_agent_recv_cb (NiceAgent * agent, guint stream_id,
    guint component_id, guint len, gchar * buf, gpointer user_data)
{
  /* Nothing to do, this callback is only for negotiation */
  GST_TRACE ("ICE data received on stream_id: '%" G_GUINT32_FORMAT
      "' component_id: '%" G_GUINT32_FORMAT "'", stream_id, component_id);
}

//static void
//element_remove_probe (GstElement * e, const gchar * pad_name, gulong id)
//{
//  GstPad *pad;

//  pad = gst_element_get_static_pad (e, pad_name);
//  gst_pad_remove_probe (pad, id);
//  g_object_unref (pad);
//}

void
kms_webrtc_transport_destroy (KmsWebRtcTransport * tr)
{
  if (tr == NULL) {
    return;
  }
//  element_remove_probe (tr->nicesrc, "src", tr->src_probe);
//  element_remove_probe (tr->nicesink, "sink", tr->sink_probe);

  g_clear_object (&tr->dtlssrtpenc);
  g_clear_object (&tr->dtlssrtpdec);
  g_clear_object (&tr->nicesink);
  g_clear_object (&tr->nicesrc);

  g_slice_free (KmsWebRtcTransport, tr);
}

/* inmediate-TODO: same pem for all transports in the same connection */

KmsWebRtcTransport *
kms_webrtc_transport_create (NiceAgent * agent, guint stream_id,
    guint component_id)
{
  KmsWebRtcTransport *tr;
  gchar *str;
  GstElement *funnel, *srtpenc, *srtpdec;

//  GstPad *pad;

  tr = g_slice_new0 (KmsWebRtcTransport);

  /* TODO: improve creating elements when needed */
  tr->component_id = component_id;

  tr->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);
  tr->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);

  tr->nicesink = gst_element_factory_make ("nicesink", NULL);
  tr->nicesrc = gst_element_factory_make ("nicesrc", NULL);

//  pad = gst_element_get_static_pad (tr->nicesrc, "src");
//  tr->src_probe = kms_utils_add_buffer_latency_meta_probe (pad, FALSE,
//      0 /* No matter type at this point */ );
//  g_object_unref (pad);

//  pad = gst_element_get_static_pad (tr->nicesink, "sink");
//  tr->sink_probe = kms_utils_add_buffer_latency_notification_probe (pad, NULL,
//      NULL, NULL);
//  g_object_unref (pad);

  if (tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL
      || tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL) {
    GST_ERROR ("Cannot create KmsWebRtcTransport");
    kms_webrtc_transport_destroy (tr);
    return NULL;
  }

  funnel = gst_bin_get_by_name (GST_BIN (tr->dtlssrtpenc), FUNNEL_NAME);
  if (funnel != NULL) {
    g_object_set (funnel, "forward-sticky-events", FALSE, NULL);
    g_object_unref (funnel);
  } else {
    GST_WARNING ("Cannot get funnel with name %s", FUNNEL_NAME);
  }

  srtpenc = gst_bin_get_by_name (GST_BIN (tr->dtlssrtpenc), SRTPENC_NAME);
  if (srtpenc != NULL) {
    g_object_set (srtpenc, "allow-repeat-tx", TRUE, "replay-window-size",
        REPLAY_WINDOW_SIZE, NULL);
    g_object_unref (srtpenc);
  } else {
    GST_WARNING ("Cannot get srtpenc with name %s", SRTPENC_NAME);
  }

  srtpdec = gst_bin_get_by_name (GST_BIN (tr->dtlssrtpdec), SRTPDEC_NAME);
  if (srtpdec != NULL) {
    g_object_set (srtpdec, "replay-window-size", REPLAY_WINDOW_SIZE, NULL);
    g_object_unref (srtpdec);
  } else {
    GST_WARNING ("Cannot get srtpdec with name %s", SRTPDEC_NAME);
  }

  str =
      g_strdup_printf ("%s-%s-%" G_GUINT32_FORMAT "-%" G_GUINT32_FORMAT,
      GST_OBJECT_NAME (tr->dtlssrtpenc), GST_OBJECT_NAME (tr->dtlssrtpdec),
      stream_id, component_id);
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "connection-id", str, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "connection-id", str, NULL);
  g_free (str);

  g_object_set (G_OBJECT (tr->nicesink), "agent", agent, "stream", stream_id,
      "component", component_id, "sync", FALSE, "async", FALSE, NULL);
  g_object_set (G_OBJECT (tr->nicesrc), "agent", agent, "stream", stream_id,
      "component", component_id, NULL);

  return tr;
}

static void init_debug (void) __attribute__ ((constructor));

static void
init_debug (void)
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
