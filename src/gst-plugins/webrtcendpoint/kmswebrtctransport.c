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

#include "kmswebrtctransport.h"

#define GST_CAT_DEFAULT kmswebrtctransport
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtctransport"

void
kms_webrtc_transport_nice_agent_recv_cb (NiceAgent * agent, guint stream_id,
    guint component_id, guint len, gchar * buf, gpointer user_data)
{
  /* Nothing to do, this callback is only for negotiation */
  GST_TRACE ("ICE data received on stream_id: '%" G_GUINT32_FORMAT
      "' component_id: '%" G_GUINT32_FORMAT "'", stream_id, component_id);
}

void
kms_webrtc_transport_destroy (KmsWebRtcTransport * tr)
{
  if (tr == NULL) {
    return;
  }

  g_clear_object (&tr->dtlssrtpenc);
  g_clear_object (&tr->dtlssrtpdec);
  g_clear_object (&tr->nicesink);
  g_clear_object (&tr->nicesrc);

  g_slice_free (KmsWebRtcTransport, tr);
}

KmsWebRtcTransport *
kms_webrtc_transport_create (NiceAgent * agent, guint stream_id,
    guint component_id)
{
  KmsWebRtcTransport *tr;
  gchar *str;

  tr = g_slice_new0 (KmsWebRtcTransport);

  /* TODO: improve creating elements when needed */
  tr->component_id = component_id;
  tr->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);
  tr->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);
  tr->nicesink = gst_element_factory_make ("nicesink", NULL);
  tr->nicesrc = gst_element_factory_make ("nicesrc", NULL);

  if (tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL
      || tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL) {
    GST_ERROR ("Cannot create KmsWebRtcTransport");
    kms_webrtc_transport_destroy (tr);
    return NULL;
  }

  str =
      g_strdup_printf ("%s-%s-%" G_GUINT32_FORMAT "-%" G_GUINT32_FORMAT,
      GST_OBJECT_NAME (tr->dtlssrtpenc), GST_OBJECT_NAME (tr->dtlssrtpdec),
      stream_id, component_id);
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "channel-id", str, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "channel-id", str, NULL);
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
