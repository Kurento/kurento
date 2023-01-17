/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include <gst/check/gstcheck.h>
#include <gst/gst.h>

#define KMS_ELEMENT_PAD_TYPE_VIDEO 2
#define NUM_CONNEXIONS 2

#define SINK_VIDEO_STREAM "sink_video_default"

GstElement *pipeline;
GMainLoop *loop;
GstElement *hubport1, *hubport2, *hubport3, *hubport4, *hubport5, *mixer;
int num_ports;
gboolean handoff_3 = FALSE;
gboolean handoff_4 = FALSE;
gboolean handoff_5 = FALSE;
gint handlerId1, handlerId2, handlerId3, handlerId4, handlerId5;
gchar *padname3, *padname4, *padname5;
GMutex mutex;
int connected = 0;

static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  g_main_loop_quit (loop);
  return FALSE;
}

static void
handoff_cb (GstElement * object, GstBuffer * arg0, GstPad * arg1,
    gpointer user_data)
{
  GstPad *pad, *peer_pad;
  GstElement *hubport;

  pad = gst_element_get_static_pad (object, "sink");
  peer_pad = gst_pad_get_peer (pad);
  hubport = gst_pad_get_parent_element (peer_pad);

  if (hubport == hubport3) {
    GST_INFO_OBJECT (object, "Handoff 3");
    g_object_set (G_OBJECT (object), "signal-handoffs", FALSE, NULL);
    handoff_3 = TRUE;
  }
  if (hubport == hubport4) {
    GST_INFO_OBJECT (object, "Handoff 4");
    g_object_set (G_OBJECT (object), "signal-handoffs", FALSE, NULL);
    handoff_4 = TRUE;
  }

  if (hubport == hubport5) {
    GST_INFO_OBJECT (object, "Handoff 5");
    g_object_set (G_OBJECT (object), "signal-handoffs", FALSE, NULL);
    handoff_5 = TRUE;
  }

  g_object_unref (pad);
  g_object_unref (peer_pad);
  g_object_unref (hubport);

  if (handoff_3 && handoff_4 && handoff_5) {
    g_idle_add (quit_main_loop_idle, user_data);
  }
}

void
check_connected ()
{
  if (connected == NUM_CONNEXIONS) {
    gst_element_set_state (pipeline, GST_STATE_PLAYING);

    g_signal_emit_by_name (hubport3, "request-new-pad",
        KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname3);
    fail_if (padname3 == NULL);

    g_signal_emit_by_name (hubport4, "request-new-pad",
        KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname4);
    fail_if (padname4 == NULL);

    g_signal_emit_by_name (hubport5, "request-new-pad",
        KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname5);
    fail_if (padname5 == NULL);

    g_signal_emit_by_name (mixer, "handle-port", hubport3, &handlerId3);
    g_signal_emit_by_name (mixer, "handle-port", hubport4, &handlerId4);
    g_signal_emit_by_name (mixer, "handle-port", hubport5, &handlerId5);

    g_object_set (mixer, "main", handlerId1, NULL);
  }
}

static void
srcpad_added (GstElement * hubport, GstPad * new_pad, gpointer user_data)
{
  gchar *padname, *expected_name;
  GstPad *sinkpad;
  GstElement *fakesink;
  GstElement *videosrc;

  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  if ((g_strcmp0 (padname, SINK_VIDEO_STREAM) == 0) &&
      ((hubport == hubport1) || (hubport == hubport2))) {
    //connect videosrc
    GST_INFO_OBJECT (hubport, "Pad added %" GST_PTR_FORMAT, new_pad);
    videosrc = gst_element_factory_make ("videotestsrc", NULL);
    sinkpad = gst_element_get_static_pad (videosrc, "src");

    g_object_set (G_OBJECT (videosrc), "is-live", TRUE, NULL);

    gst_bin_add (GST_BIN (pipeline), videosrc);

    fail_if (gst_pad_link (sinkpad, new_pad) != GST_PAD_LINK_OK);
    gst_element_sync_state_with_parent (videosrc);
    g_object_unref (sinkpad);

    g_mutex_lock (&mutex);
    connected++;
    check_connected ();
    g_mutex_unlock (&mutex);

    goto end;
  }

  if ((hubport != hubport3) && (hubport != hubport4)
      && (hubport != hubport5)) {
    goto end;
  }

  expected_name = *(gchar **) user_data;
  if (g_strcmp0 (padname, expected_name) != 0) {
    goto end;
  }

  GST_INFO_OBJECT (hubport, "Pad added %" GST_PTR_FORMAT, new_pad);

  fakesink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (fakesink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (fakesink, "handoff", G_CALLBACK (handoff_cb), loop);

  gst_bin_add (GST_BIN (pipeline), fakesink);

  sinkpad = gst_element_get_static_pad (fakesink, "sink");
  fail_if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK);

  gst_element_sync_state_with_parent (fakesink);

  g_object_unref (sinkpad);

end:
  g_free (padname);
}

GST_START_TEST (connection)
{
  gint signalId1, signalId2, signalId3, signalId4, signalId5;

  mixer = gst_element_factory_make ("dispatcheronetomany", NULL);
  g_mutex_init (&mutex);

  hubport1 = gst_element_factory_make ("hubport", NULL);
  hubport2 = gst_element_factory_make ("hubport", NULL);
  hubport3 = gst_element_factory_make ("hubport", NULL);
  hubport4 = gst_element_factory_make ("hubport", NULL);
  hubport5 = gst_element_factory_make ("hubport", NULL);
  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline");

  gst_bin_add_many (GST_BIN (pipeline), hubport1,
      hubport2, hubport3, hubport4, hubport5, mixer, NULL);

  num_ports = 3;

  signalId1 =
      g_signal_connect (hubport1, "pad-added", G_CALLBACK (srcpad_added), NULL);
  signalId2 =
      g_signal_connect (hubport2, "pad-added", G_CALLBACK (srcpad_added), NULL);
  signalId3 =
      g_signal_connect (hubport3, "pad-added", G_CALLBACK (srcpad_added),
      &padname3);
  signalId4 =
      g_signal_connect (hubport4, "pad-added", G_CALLBACK (srcpad_added),
      &padname4);
  signalId5 =
      g_signal_connect (hubport5, "pad-added", G_CALLBACK (srcpad_added),
      &padname5);

  g_signal_emit_by_name (mixer, "handle-port", hubport1, &handlerId1);
  g_signal_emit_by_name (mixer, "handle-port", hubport2, &handlerId2);

  g_object_set (mixer, "main", handlerId1, NULL);

  g_main_loop_run (loop);

  g_signal_emit_by_name (mixer, "unhandle-port", handlerId1);
  g_signal_emit_by_name (mixer, "unhandle-port", handlerId2);
  g_signal_emit_by_name (mixer, "unhandle-port", handlerId3);
  g_signal_emit_by_name (mixer, "unhandle-port", handlerId4);
  g_signal_emit_by_name (mixer, "unhandle-port", handlerId5);

  g_signal_handler_disconnect (hubport1, signalId1);
  g_signal_handler_disconnect (hubport2, signalId2);
  g_signal_handler_disconnect (hubport3, signalId3);
  g_signal_handler_disconnect (hubport4, signalId4);
  g_signal_handler_disconnect (hubport5, signalId5);

  g_free (padname3);
  g_free (padname4);
  g_free (padname5);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_main_loop_unref (loop);
  g_mutex_clear (&mutex);
}

GST_END_TEST
/*
 * End of test cases
 */
static Suite *
dispatcher_one_to_many_suite (void)
{
  Suite *s = suite_create ("dispatcheronetomany");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, connection);

  return s;
}

GST_CHECK_MAIN (dispatcher_one_to_many);
