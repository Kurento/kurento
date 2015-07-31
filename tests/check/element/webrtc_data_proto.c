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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>

#include <webrtcendpoint/kmswebrtcdataproto.h>
#include <webrtcendpoint/kmswebrtcdatasessionbin.h>

static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  g_main_loop_quit (loop);

  return FALSE;
}

static gboolean
print_timedout_pipeline (gpointer data)
{
  GstElement *pipeline = data;
  gchar *pipeline_name;
  gchar *name;

  pipeline_name = gst_element_get_name (pipeline);
  name = g_strdup_printf ("%s_timedout", pipeline_name);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, name);

  g_free (name);
  g_free (pipeline_name);

  return FALSE;
}

static void
data_channel_opened_cb (KmsWebRtcDataSessionBin * self, guint stream_id)
{
  GST_INFO_OBJECT (self, "Data channel opened with stream id %d", stream_id);
}

GST_START_TEST (connection)
{
  GstElement *session1, *session2, *udpsrc1, *udpsink1, *udpsrc2, *udpsink2;
  GstElement *pipeline;
  gint stream_id;
  GMainLoop *loop;
  gulong id1, id2;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline");

  udpsink1 = gst_element_factory_make ("udpsink", NULL);
  udpsrc1 = gst_element_factory_make ("udpsrc", NULL);
  session1 = GST_ELEMENT (kms_webrtc_data_session_bin_new (TRUE));
  id1 = g_signal_connect (session1, "data-channel-opened",
      G_CALLBACK (data_channel_opened_cb), NULL);

  udpsink2 = gst_element_factory_make ("udpsink", NULL);
  udpsrc2 = gst_element_factory_make ("udpsrc", NULL);
  session2 = GST_ELEMENT (kms_webrtc_data_session_bin_new (FALSE));
  id2 = g_signal_connect (session2, "data-channel-opened",
      G_CALLBACK (data_channel_opened_cb), NULL);

  g_object_set (udpsink1, "host", "127.0.0.1", "port", 5555, "sync", FALSE,
      "async", FALSE, NULL);
  g_object_set (udpsrc1, "port", 6666, NULL);
  g_object_set (session1, "sctp-local-port", 9999, "sctp-remote-port", 9999,
      NULL);

  g_object_set (udpsink2, "host", "127.0.0.1", "port", 6666, "sync", FALSE,
      "async", FALSE, NULL);
  g_object_set (udpsrc2, "port", 5555, NULL);
  g_object_set (session2, "sctp-local-port", 9999, "sctp-remote-port", 9999,
      NULL);

  gst_bin_add_many (GST_BIN (pipeline), session1, session2, udpsink1, udpsrc1,
      udpsink2, udpsrc2, NULL);

  gst_element_link_many (udpsrc1, session1, udpsink1, NULL);
  gst_element_link_many (udpsrc2, session2, udpsink2, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (1, print_timedout_pipeline, pipeline);
  g_timeout_add_seconds (1, quit_main_loop_idle, loop);

  g_signal_emit_by_name (session1, "create-data-channel", &stream_id);

  GST_DEBUG ("Creating data channel with stream id %d", stream_id);

  g_main_loop_run (loop);

  GST_DEBUG ("Finished test");

  g_signal_handler_disconnect (session1, id1);
  g_signal_handler_disconnect (session2, id2);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_main_loop_unref (loop);
}

GST_END_TEST static Suite *
webrtc_data_protocol_suite (void)
{
  Suite *s = suite_create ("webrtc_data_protocol");
  TCase *tc_chain = tcase_create ("session_object");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, connection);

  return s;
}

GST_CHECK_MAIN (webrtc_data_protocol);
