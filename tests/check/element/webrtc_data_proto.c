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
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>

#include <webrtcendpoint/kmswebrtcdataproto.h>
#include <webrtcendpoint/kmswebrtcdatasessionbin.h>

#define TEST_MESSAGE "Hello world!"

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

static GstFlowReturn
data_channel_buffer_received_cb (GObject * obj, GstBuffer * buffer,
    gpointer user_data)
{
  KmsWebRtcDataChannel *channel = KMS_WEBRTC_DATA_CHANNEL (obj);
  GstMapInfo info = GST_MAP_INFO_INIT;
  gchar *msg;

  GST_INFO_OBJECT (channel, "Buffer received");

  if (!gst_buffer_map (buffer, &info, GST_MAP_READ)) {
    fail ("Can not read buffer");
    return GST_FLOW_ERROR;
  }

  fail_if (strlen (TEST_MESSAGE) != info.size);
  msg = g_strndup ((const gchar *) info.data, info.size);

  gst_buffer_unmap (buffer, &info);

  GST_INFO ("Received message: \"%s\"", msg);

  fail_unless (g_strcmp0 (TEST_MESSAGE, msg) == 0);

  g_free (msg);

  g_idle_add (quit_main_loop_idle, user_data);

  return GST_FLOW_OK;
}

static void
data_channel_opened_cb (KmsWebRtcDataSessionBin * self, guint stream_id,
    gpointer user_data)
{
  KmsWebRtcDataChannel *channel;
  gboolean is_client;
  GstBuffer *buff;
  gchar *msg;

  g_signal_emit_by_name (self, "get-data-channel", stream_id, &channel);
  GST_INFO_OBJECT (channel, "Data channel opened with stream id %u -- %"
      GST_PTR_FORMAT, stream_id, self);

  g_object_get (self, "dtls-client-mode", &is_client, NULL);

  kms_webrtc_data_channel_set_new_buffer_callback (channel,
      data_channel_buffer_received_cb, user_data, NULL);

  if (!is_client) {
    return;
  }

  msg = g_strdup (TEST_MESSAGE);
  buff = gst_buffer_new_wrapped (msg, strlen (msg));
  kms_webrtc_data_channel_push_buffer (channel, buff, FALSE);
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
      G_CALLBACK (data_channel_opened_cb), loop);

  udpsink2 = gst_element_factory_make ("udpsink", NULL);
  udpsrc2 = gst_element_factory_make ("udpsrc", NULL);
  session2 = GST_ELEMENT (kms_webrtc_data_session_bin_new (FALSE));
  id2 = g_signal_connect (session2, "data-channel-opened",
      G_CALLBACK (data_channel_opened_cb), loop);

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

  g_signal_emit_by_name (session1, "create-data-channel", TRUE, -1, -1,
      "TestChannel", "webrtc-datachannel", &stream_id);

  GST_DEBUG ("Creating data channel with stream id %d", stream_id);

  g_main_loop_run (loop);

  GST_DEBUG ("Finished test");

  g_signal_handler_disconnect (session1, id1);
  g_signal_handler_disconnect (session2, id2);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_main_loop_unref (loop);
}

GST_END_TEST static void
data_session_established_cb (KmsWebRtcDataSessionBin * self, gboolean connected,
    gpointer user_data)
{
  GST_DEBUG_OBJECT (self, "Data session %s",
      (connected) ? "established" : "finished");

  if (connected) {
    g_idle_add (quit_main_loop_idle, user_data);
  }
}

GST_START_TEST (data_session_established)
{
  GstElement *session1, *session2, *udpsrc1, *udpsink1, *udpsrc2, *udpsink2;
  GstElement *pipeline;
  GMainLoop *loop;
  gulong id1;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline");

  udpsink1 = gst_element_factory_make ("udpsink", NULL);
  udpsrc1 = gst_element_factory_make ("udpsrc", NULL);
  session1 = GST_ELEMENT (kms_webrtc_data_session_bin_new (TRUE));

  id1 = g_signal_connect (session1, "data-session-established",
      G_CALLBACK (data_session_established_cb), loop);

  udpsink2 = gst_element_factory_make ("udpsink", NULL);
  udpsrc2 = gst_element_factory_make ("udpsrc", NULL);
  session2 = GST_ELEMENT (kms_webrtc_data_session_bin_new (FALSE));

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

  g_main_loop_run (loop);

  GST_DEBUG ("Finished test");

  g_signal_handler_disconnect (session1, id1);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_main_loop_unref (loop);
}

GST_END_TEST typedef struct _ChannelSession
{
  guint stream_id;
  GstElement *session;
} ChannelSession;

typedef struct _ExitTest
{
  gint count;
  GMainLoop *loop;
} ExitTest;

static gboolean
destroy_data_channel (ChannelSession * session)
{
  GST_DEBUG_OBJECT (session->session, "Destroy channel %u", session->stream_id);
  g_signal_emit_by_name (session->session, "destroy-data-channel",
      session->stream_id);

  return G_SOURCE_REMOVE;
}

static void
destroy_data_channel_opened_cb (KmsWebRtcDataSessionBin * self, guint stream_id,
    ChannelSession * session)
{
  GST_DEBUG_OBJECT (self, "Created data channel %u", stream_id);
  session->stream_id = stream_id;

  g_idle_add ((GSourceFunc) destroy_data_channel, session);
}

static void
data_channel_closed_cb (KmsWebRtcDataSessionBin * self, guint stream_id,
    ExitTest * exit)
{
  GST_DEBUG_OBJECT (self, "Closed data channel %u", stream_id);

  if (g_atomic_int_dec_and_test (&exit->count)) {
    g_idle_add (quit_main_loop_idle, exit->loop);
  }
}

GST_START_TEST (destroy_channels)
{
  GstElement *session1, *session2, *udpsrc1, *udpsink1, *udpsrc2, *udpsink2;
  ChannelSession session;
  GstElement *pipeline;
  gint stream_id;
  GMainLoop *loop;
  gulong id1, id2, id3;
  ExitTest exit;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline");

  exit.count = 2;
  exit.loop = loop;

  udpsink1 = gst_element_factory_make ("udpsink", NULL);
  udpsrc1 = gst_element_factory_make ("udpsrc", NULL);
  session1 = GST_ELEMENT (kms_webrtc_data_session_bin_new (TRUE));

  session.session = session1;
  id1 = g_signal_connect (session1, "data-channel-opened",
      G_CALLBACK (destroy_data_channel_opened_cb), &session);
  id2 = g_signal_connect (session1, "data-channel-closed",
      G_CALLBACK (data_channel_closed_cb), &exit);

  udpsink2 = gst_element_factory_make ("udpsink", NULL);
  udpsrc2 = gst_element_factory_make ("udpsrc", NULL);
  session2 = GST_ELEMENT (kms_webrtc_data_session_bin_new (FALSE));
  id3 = g_signal_connect (session2, "data-channel-closed",
      G_CALLBACK (data_channel_closed_cb), &exit);

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

  g_signal_emit_by_name (session1, "create-data-channel", TRUE, -1, -1,
      "TestChannel", "webrtc-datachannel", &stream_id);

  GST_DEBUG ("Creating data channel with stream id %d", stream_id);

  g_main_loop_run (loop);

  GST_DEBUG ("Finished test");

  g_signal_handler_disconnect (session1, id1);
  g_signal_handler_disconnect (session1, id2);
  g_signal_handler_disconnect (session2, id3);

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

  tcase_add_test (tc_chain, data_session_established);
  tcase_add_test (tc_chain, connection);
  tcase_add_test (tc_chain, destroy_channels);

  return s;
}

GST_CHECK_MAIN (webrtc_data_protocol);
