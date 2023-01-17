/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include <kmscheck.h>

#define ITERATIONS 1

static int iterations = ITERATIONS;
static GMainLoop *loop;

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, "error");
      fail ("Error received on bus");
      break;
    }
    case GST_MESSAGE_WARNING:{
      GST_WARNING ("Warning: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, "warning");
      break;
    }
    case GST_MESSAGE_EOS:{
      GST_INFO ("EOS received");
      g_main_loop_quit (loop);
      break;
    }
    case GST_MESSAGE_STATE_CHANGED:{
      if (g_str_has_prefix (GST_OBJECT_NAME (msg->src), "agnosticbin")) {
        GST_INFO ("Event: %" GST_PTR_FORMAT, msg);
      }
    }
    default:
      break;
  }
}

static void
create_element (const gchar * element_name)
{
  GstElement *element = gst_element_factory_make (element_name, NULL);

  g_object_unref (element);
}

static void
play_element (const gchar * element_name)
{
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *element = gst_element_factory_make (element_name, NULL);

  gst_bin_add (GST_BIN (pipeline), element);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  gst_element_set_state (pipeline, GST_STATE_NULL);

  g_object_unref (pipeline);
}

static void
play_agnosticbin_video_passthrough (void)
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (videotestsrc), "num-buffers", 100, NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin, fakesink,
      NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, agnosticbin, fakesink, NULL);
  fail_unless (ret);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

static void
play_agnosticbin_raw_to_vp8 (void)
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8dec = gst_element_factory_make ("vp8dec", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (videotestsrc), "num-buffers", 100, NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin, vp8dec,
      fakesink, NULL);
  mark_point ();
  ret =
      gst_element_link_many (videotestsrc, agnosticbin, vp8dec, fakesink, NULL);
  fail_unless (ret);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, __FUNCTION__);

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

static void
decodebin_pad_added (GstElement * decodebin, GstPad * pad,
    GstElement * fakesink)
{
  gboolean ret;

  ret = gst_element_link (decodebin, fakesink);
  fail_unless (ret);
}

static void
play_decodebin_vp8_to_raw (void)
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8enc = gst_element_factory_make ("vp8enc", NULL);
  GstElement *decodebin = gst_element_factory_make ("decodebin", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  mark_point ();
  g_object_set (G_OBJECT (videotestsrc), "num-buffers", 100, NULL);
  g_object_set (G_OBJECT (vp8enc), "keyframe-max-dist", 1, NULL);
  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (decodebin_pad_added), fakesink);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, vp8enc, decodebin,
      fakesink, NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, vp8enc, decodebin, NULL);
  fail_unless (ret);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "play_agnosticbin_vp8_to_raw_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

static void
play_agnosticbin_audio_passthrough (void)
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (audiotestsrc), "num-buffers", 100, NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, agnosticbin, fakesink,
      NULL);
  mark_point ();
  ret = gst_element_link_many (audiotestsrc, agnosticbin, fakesink, NULL);
  fail_unless (ret);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

static void
play_agnosticbin_vp8_to_raw (void)
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8enc = gst_element_factory_make ("vp8enc", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *capsfilter = gst_element_factory_make ("capsfilter", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  loop = g_main_loop_new (NULL, TRUE);
  GstCaps *caps;

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  mark_point ();
  g_object_set (G_OBJECT (videotestsrc), "num-buffers", 100, NULL);
  g_object_set (G_OBJECT (vp8enc), "keyframe-max-dist", 1, NULL);

  caps = gst_caps_new_empty_simple ("video/x-raw");
  g_object_set (G_OBJECT (capsfilter), "caps", caps, NULL);
  gst_caps_unref (caps);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, vp8enc, agnosticbin,
      capsfilter, fakesink, NULL);
  mark_point ();
  ret =
      gst_element_link_many (videotestsrc, vp8enc, agnosticbin, capsfilter,
      fakesink, NULL);
  fail_unless (ret);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "play_agnosticbin_vp8_to_raw_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

static gboolean
timeout_check (gpointer pipeline)
{
  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "timeout_test_end");
  GST_DEBUG ("Timeout");
  return FALSE;
}

static void
play_agnosticbin_raw_to_vorbis (void)
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);

  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *vorbisdec = gst_element_factory_make ("vorbisdec", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (audiotestsrc), "num-buffers", 100, "is-live", TRUE,
      NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, agnosticbin, vorbisdec,
      fakesink, NULL);
  mark_point ();
  ret =
      gst_element_link_many (audiotestsrc, agnosticbin, vorbisdec, fakesink,
      NULL);
  fail_unless (ret);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, __FUNCTION__);

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

GST_START_TEST (test_create_valve)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("valve");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_tee)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("tee");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_queue)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("queue");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_decodebin)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("decodebin");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_agnosticbin)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("agnosticbin");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_valve)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("valve");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_tee)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("tee");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_queue)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("queue");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_decodebin)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("decodebin");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_agnosticbin)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("agnosticbin");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_agnosticbin_video_passthrough)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_agnosticbin_video_passthrough ();
  }
}

KMS_END_TEST
GST_START_TEST (test_play_agnosticbin_raw_to_vp8)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_agnosticbin_raw_to_vp8 ();
  }
}

KMS_END_TEST
GST_START_TEST (test_play_decodebin_vp8_to_raw)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_decodebin_vp8_to_raw ();
  }
}

KMS_END_TEST
GST_START_TEST (test_play_agnosticbin_vp8_to_raw)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_agnosticbin_vp8_to_raw ();
  }
}

KMS_END_TEST
GST_START_TEST (test_play_agnosticbin_audio_passthrough)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_agnosticbin_audio_passthrough ();
  }
}

KMS_END_TEST
GST_START_TEST (test_play_agnosticbin_raw_to_vorbis)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_agnosticbin_raw_to_vorbis ();
  }
}

KMS_END_TEST
/*
 * End of test cases
 */
static Suite *
agnosticbin_suite (void)
{
  char *it_str;

  it_str = getenv ("ITERATIONS");
  if (it_str != NULL) {
    iterations = atoi (it_str);
    if (iterations <= 0)
      iterations = ITERATIONS;
  }

  Suite *s = suite_create ("agnosticbin");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, test_create_valve);
  tcase_add_test (tc_chain, test_create_tee);
  tcase_add_test (tc_chain, test_create_queue);
  tcase_add_test (tc_chain, test_create_decodebin);
  tcase_add_test (tc_chain, test_create_agnosticbin);

  tcase_add_test (tc_chain, test_play_valve);
  tcase_add_test (tc_chain, test_play_tee);
  tcase_add_test (tc_chain, test_play_queue);
  tcase_add_test (tc_chain, test_play_decodebin);
  tcase_add_test (tc_chain, test_play_agnosticbin);

  tcase_add_test (tc_chain, test_play_agnosticbin_video_passthrough);
  tcase_add_test (tc_chain, test_play_agnosticbin_raw_to_vp8);

  tcase_add_test (tc_chain, test_play_decodebin_vp8_to_raw);
  tcase_add_test (tc_chain, test_play_agnosticbin_vp8_to_raw);

  tcase_add_test (tc_chain, test_play_agnosticbin_audio_passthrough);
  tcase_add_test (tc_chain, test_play_agnosticbin_raw_to_vorbis);

  return s;
}

KMS_CHECK_MAIN (agnosticbin);
