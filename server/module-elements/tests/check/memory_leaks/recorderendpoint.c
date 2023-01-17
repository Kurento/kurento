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
#include "kmsuriendpointstate.h"

#define ITERATIONS 1

static int iterations = ITERATIONS;

static GMainLoop *loop;

#define RECORDER_NAME "recorder"

static gboolean
quit_main_loop_idle (gpointer data)
{
  g_main_loop_quit (loop);
  return FALSE;
}

static const gchar *
state2string (KmsUriEndpointState state)
{
  switch (state) {
    case KMS_URI_ENDPOINT_STATE_STOP:
      return "STOP";
    case KMS_URI_ENDPOINT_STATE_START:
      return "START";
    case KMS_URI_ENDPOINT_STATE_PAUSE:
      return "PAUSE";
    default:
      return "Invalid state";
  }
}

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
      GstElement *recorderendpoint;

      GST_DEBUG ("EOS received");
      recorderendpoint = gst_bin_get_by_name (GST_BIN (pipe), RECORDER_NAME);
      g_object_set (G_OBJECT (recorderendpoint), "state",
          KMS_URI_ENDPOINT_STATE_STOP, NULL);
      g_object_unref (recorderendpoint);
      break;
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

  gst_element_set_state (pipeline, GST_STATE_READY);
  gst_element_set_state (pipeline, GST_STATE_NULL);

  g_object_unref (pipeline);
}

static void
state_changed_cb (GstElement * recorder, KmsUriEndpointState newState,
    gpointer data)
{
  GST_DEBUG ("State changed %s.", state2string (newState));
  if (newState == KMS_URI_ENDPOINT_STATE_STOP) {
    GST_DEBUG ("Recorder stopped. Exiting the main loop.");
    g_idle_add (quit_main_loop_idle, loop);
  }
}

static void
start_audio_recorderendpoint ()
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *aencoder = gst_element_factory_make ("vorbisenc", NULL);
  GstElement *recorderendpoint =
      gst_element_factory_make ("recorderendpoint", RECORDER_NAME);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (audiotestsrc), "num-buffers", 50, "is-live", TRUE,
      "do-timestamp", TRUE, "wave", 8, NULL);
  g_object_set (G_OBJECT (recorderendpoint), "uri",
      "file:///tmp/audio_recorder.avi", NULL);

  g_signal_connect (recorderendpoint, "state-changed",
      G_CALLBACK (state_changed_cb), loop);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, aencoder,
      recorderendpoint, NULL);
  mark_point ();
  ret = gst_element_link (audiotestsrc, aencoder);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_pads (aencoder, NULL, recorderendpoint, "audio_sink");
  fail_unless (ret);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_object_set (G_OBJECT (recorderendpoint), "state",
      KMS_URI_ENDPOINT_STATE_START, NULL);

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
start_video_recorderendpoint ()
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vencoder = gst_element_factory_make ("vp8enc", NULL);
  GstElement *recorderendpoint =
      gst_element_factory_make ("recorderendpoint", RECORDER_NAME);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (videotestsrc), "num-buffers", 50, "is-live", TRUE,
      "do-timestamp", TRUE, "pattern", 18, NULL);
  g_object_set (G_OBJECT (recorderendpoint), "uri",
      "file:///tmp/video_recorder.avi", NULL);

  g_signal_connect (recorderendpoint, "state-changed",
      G_CALLBACK (state_changed_cb), loop);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, vencoder,
      recorderendpoint, NULL);
  mark_point ();
  ret = gst_element_link (videotestsrc, vencoder);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_pads (vencoder, NULL, recorderendpoint, "video_sink");
  fail_unless (ret);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_object_set (G_OBJECT (recorderendpoint), "state",
      KMS_URI_ENDPOINT_STATE_START, NULL);

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
start_audio_video_recorderendpoint ()
{
  gboolean ret;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *aencoder = gst_element_factory_make ("vorbisenc", NULL);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vencoder = gst_element_factory_make ("vp8enc", NULL);
  GstElement *recorderendpoint =
      gst_element_factory_make ("recorderendpoint", RECORDER_NAME);

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (audiotestsrc), "num-buffers", 50, "is-live", TRUE,
      "do-timestamp", TRUE, "wave", 8, NULL);
  g_object_set (G_OBJECT (videotestsrc), "num-buffers", 50, "is-live", TRUE,
      "do-timestamp", TRUE, "pattern", 18, NULL);
  g_object_set (G_OBJECT (recorderendpoint), "uri",
      "file:///tmp/audio_video_recorder.avi", NULL);

  g_signal_connect (recorderendpoint, "state-changed",
      G_CALLBACK (state_changed_cb), loop);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, aencoder,
      videotestsrc, vencoder, recorderendpoint, NULL);
  mark_point ();

  // Link audio elements
  ret = gst_element_link (audiotestsrc, aencoder);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_pads (aencoder, NULL, recorderendpoint, "audio_sink");
  fail_unless (ret);
  mark_point ();

  // Link video elements
  ret = gst_element_link (videotestsrc, vencoder);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_pads (vencoder, NULL, recorderendpoint, "video_sink");
  fail_unless (ret);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_object_set (G_OBJECT (recorderendpoint), "state",
      KMS_URI_ENDPOINT_STATE_START, NULL);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
}

GST_START_TEST (test_create_recorderendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("recorderendpoint");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_recorderendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("recorderendpoint");
  }
}

KMS_END_TEST
GST_START_TEST (test_start_audio_recorderendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    start_audio_recorderendpoint ();
  }
}

KMS_END_TEST
GST_START_TEST (test_start_video_recorderendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    start_video_recorderendpoint ();
  }
}

KMS_END_TEST
GST_START_TEST (test_start_audio_video_recorderendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    start_audio_video_recorderendpoint ();
  }
}

KMS_END_TEST
/*
 * End of test cases
 */
static Suite *
recorderendpoint_suite (void)
{
  char *it_str;

  it_str = getenv ("ITERATIONS");
  if (it_str != NULL) {
    iterations = atoi (it_str);
    if (iterations <= 0)
      iterations = ITERATIONS;
  }

  Suite *s = suite_create ("recorderendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, test_create_recorderendpoint);
  tcase_add_test (tc_chain, test_play_recorderendpoint);
  tcase_add_test (tc_chain, test_start_audio_recorderendpoint);
  tcase_add_test (tc_chain, test_start_video_recorderendpoint);
  tcase_add_test (tc_chain, test_start_audio_video_recorderendpoint);

  return s;
}

KMS_CHECK_MAIN (recorderendpoint);
