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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <kmscheck.h>
#include "kmsuriendpointstate.h"

#define ITERATIONS 1
#define VIDEO_PATH BINARY_LOCATION "/video/small.webm"

static int iterations = ITERATIONS;

static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  g_main_loop_quit (loop);
  return FALSE;
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
playerendpoint_eos (GstElement * player, GMainLoop * loop)
{
  GST_DEBUG ("EOS received");
  g_idle_add (quit_main_loop_idle, loop);
}

static void
start_playerendpoint (void)
{
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *playerendpoint =
      gst_element_factory_make ("playerendpoint", NULL);
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_signal_connect (G_OBJECT (playerendpoint), "eos",
      G_CALLBACK (playerendpoint_eos), loop);
  g_object_set (G_OBJECT (playerendpoint), "uri", VIDEO_PATH, NULL);

  gst_bin_add (GST_BIN (pipeline), playerendpoint);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_object_set (G_OBJECT (playerendpoint), "state",
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

GST_START_TEST (test_create_playerendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("playerendpoint");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_playerendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("playerendpoint");
  }
}

KMS_END_TEST
GST_START_TEST (test_start_playerendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    start_playerendpoint ();
  }
}

KMS_END_TEST
/*
 * End of test cases
 */
static Suite *
playerendpoint_suite (void)
{
  char *it_str;

  it_str = getenv ("ITERATIONS");
  if (it_str != NULL) {
    iterations = atoi (it_str);
    if (iterations <= 0)
      iterations = ITERATIONS;
  }

  Suite *s = suite_create ("playerendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, test_create_playerendpoint);
  tcase_add_test (tc_chain, test_play_playerendpoint);
  tcase_add_test (tc_chain, test_start_playerendpoint);

  return s;
}

KMS_CHECK_MAIN (playerendpoint);
