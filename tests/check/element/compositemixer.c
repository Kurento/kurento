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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>

#define KMS_ELEMENT_PAD_TYPE_VIDEO 2
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1

#define SINK_VIDEO_STREAM "sink_video_default"
#define SINK_AUDIO_STREAM "sink_audio_default"

GstElement *pipeline;
GMainLoop *loop;
GstElement *hubport1, *hubport2, *hubport3;

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
  g_idle_add (quit_main_loop_idle, user_data);
}

static void
srcpad_added (GstElement * hubport, GstPad * new_pad, gpointer user_data)
{
  gchar *padname, *expected_name;
  GstPad *sinkpad;
  GstElement *fakesink;
  GstElement *videosrc;
  GstElement *audiosrc;

  GST_INFO_OBJECT (hubport, "Pad added %" GST_PTR_FORMAT, new_pad);

  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  if (g_strcmp0 (padname, SINK_VIDEO_STREAM) == 0) {
    videosrc = gst_element_factory_make ("videotestsrc", NULL);
    sinkpad = gst_element_get_static_pad (videosrc, "src");

    g_object_set (G_OBJECT (videosrc), "is-live", TRUE, NULL);

    gst_bin_add (GST_BIN (pipeline), videosrc);

    fail_if (gst_pad_link (sinkpad, new_pad) != GST_PAD_LINK_OK);
    gst_element_sync_state_with_parent (videosrc);
    g_object_unref (sinkpad);
    goto end;
  }

  if (g_strcmp0 (padname, SINK_AUDIO_STREAM) == 0) {
    audiosrc = gst_element_factory_make ("audiotestsrc", NULL);
    sinkpad = gst_element_get_static_pad (audiosrc, "src");

    g_object_set (G_OBJECT (audiosrc), "is-live", TRUE, NULL);

    gst_bin_add (GST_BIN (pipeline), audiosrc);

    fail_if (gst_pad_link (sinkpad, new_pad) != GST_PAD_LINK_OK);
    gst_element_sync_state_with_parent (audiosrc);
    g_object_unref (sinkpad);
    goto end;
  }

  expected_name = *(gchar **) user_data;
  if (g_strcmp0 (padname, expected_name) != 0) {
    goto end;
  }

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
  gint handlerId1, handlerId2, handlerId3;
  gint signalId1, signalId2, signalId3;
  gchar *padname1, *padname2, *padname3;
  gchar *padname1_, *padname2_, *padname3_;
  GstElement *mixer = gst_element_factory_make ("compositemixer", NULL);

  hubport1 = gst_element_factory_make ("hubport", NULL);
  hubport2 = gst_element_factory_make ("hubport", NULL);
  hubport3 = gst_element_factory_make ("hubport", NULL);
  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline");

  gst_bin_add_many (GST_BIN (pipeline), hubport1,
      hubport2, hubport3, mixer, NULL);

  signalId1 =
      g_signal_connect (hubport1, "pad-added", G_CALLBACK (srcpad_added),
      &padname1);
  signalId2 =
      g_signal_connect (hubport2, "pad-added", G_CALLBACK (srcpad_added),
      &padname2);
  signalId3 =
      g_signal_connect (hubport3, "pad-added", G_CALLBACK (srcpad_added),
      &padname3);

  g_signal_emit_by_name (hubport1, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname1);
  fail_if (padname1 == NULL);

  g_signal_emit_by_name (hubport1, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname1_);
  fail_if (padname1_ == NULL);

  g_signal_emit_by_name (hubport2, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname2);
  fail_if (padname2 == NULL);

  g_signal_emit_by_name (hubport2, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname2_);
  fail_if (padname2_ == NULL);

  g_signal_emit_by_name (hubport3, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname3);
  fail_if (padname3 == NULL);

  g_signal_emit_by_name (hubport3, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname3_);
  fail_if (padname3_ == NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_signal_emit_by_name (mixer, "handle-port", hubport1, &handlerId1);
  g_signal_emit_by_name (mixer, "handle-port", hubport2, &handlerId2);
  g_signal_emit_by_name (mixer, "handle-port", hubport3, &handlerId3);

  gst_debug_bin_to_dot_file_with_ts (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "composite");

  g_main_loop_run (loop);

  g_signal_emit_by_name (mixer, "unhandle-port", handlerId1);
  g_signal_emit_by_name (mixer, "unhandle-port", handlerId2);
  g_signal_emit_by_name (mixer, "unhandle-port", handlerId3);

  g_signal_handler_disconnect (hubport1, signalId1);
  g_signal_handler_disconnect (hubport2, signalId2);
  g_signal_handler_disconnect (hubport3, signalId3);

  g_free (padname1);
  g_free (padname2);
  g_free (padname3);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_main_loop_unref (loop);
}

GST_END_TEST
/*
 * End of test cases
 */
static Suite *
composite_mixer_suite (void)
{
  Suite *s = suite_create ("compositemixer");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, connection);

  return s;
}

GST_CHECK_MAIN (composite_mixer);
