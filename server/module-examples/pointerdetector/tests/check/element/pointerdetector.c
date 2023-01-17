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
#include <glib.h>
#include <kmstestutils.h>

#include <commons/kmsuriendpointstate.h>

#define IMG_PATH BINARY_LOCATION "/img/mario-wings.png"
#define VIDEO_PATH BINARY_LOCATION "/video/format/small.webm"

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

GMainLoop *loop;
GstElement *player, *pipeline, *filter, *fakesink_audio, *fakesink_video;

static void
configure_structure (GstStructure * buttonsLayout)
{
  GstStructure *buttonsLayoutAux;
  int counter;

  for (counter = 0; counter < 2; counter++) {
    gchar *id;

    id = g_strdup_printf ("id%d", counter);
    buttonsLayoutAux = gst_structure_new (id,
        "upRightCornerX", G_TYPE_INT, 10 + counter,
        "upRightCornerY", G_TYPE_INT, 35 + counter,
        "width", G_TYPE_INT, 25 + counter,
        "height", G_TYPE_INT, 12 + counter,
        "id", G_TYPE_STRING, id,
        "inactive_uri", G_TYPE_STRING, IMG_PATH,
        "transparency", G_TYPE_DOUBLE, (double) 0.3, NULL);
    //"active_uri", G_TYPE_STRING, IMG_PATH, NULL);

    gst_structure_set (buttonsLayout,
        id, GST_TYPE_STRUCTURE, buttonsLayoutAux, NULL);

    gst_structure_free (buttonsLayoutAux);
    g_free (id);
  }
}

GST_START_TEST (set_properties)
{
  GstElement *pointerdetector;
  gboolean debug, message, show;
  GstStructure *buttonsLayout1, *buttonsLayout2;
  GstStructure *calibrationArea;

  pointerdetector = gst_element_factory_make ("pointerdetector", NULL);

  debug = TRUE;
  g_object_set (G_OBJECT (pointerdetector), "show-debug-region", debug, NULL);
  g_object_get (G_OBJECT (pointerdetector), "show-debug-region", &debug, NULL);

  if (debug != TRUE)
    fail ("unexpected attribute value");

  calibrationArea = gst_structure_new ("calibration_area",
      "x", G_TYPE_INT, 1,
      "y", G_TYPE_INT, 2,
      "width", G_TYPE_INT, 3, "height", G_TYPE_INT, 3, NULL);
  g_object_set (G_OBJECT (pointerdetector), "calibration-area",
      calibrationArea, NULL);
  gst_structure_free (calibrationArea);

  buttonsLayout1 = gst_structure_new_empty ("windowsLayout1");
  configure_structure (buttonsLayout1);
  g_object_set (G_OBJECT (pointerdetector), "windows-layout", buttonsLayout1,
      NULL);
  gst_structure_free (buttonsLayout1);

  buttonsLayout2 = gst_structure_new_empty ("windowsLayout1");
  configure_structure (buttonsLayout2);
  g_object_set (G_OBJECT (pointerdetector), "windows-layout", buttonsLayout2,
      NULL);
  gst_structure_free (buttonsLayout2);

  message = FALSE;
  g_object_set (G_OBJECT (pointerdetector), "message", message, NULL);
  g_object_get (G_OBJECT (pointerdetector), "message", &message, NULL);

  if (message != FALSE)
    fail ("unexpected attribute value");

  show = FALSE;
  g_object_set (G_OBJECT (pointerdetector), "message", show, NULL);
  g_object_get (G_OBJECT (pointerdetector), "message", &show, NULL);

  if (show != FALSE)
    fail ("unexpected attribute value");

  g_object_unref (pointerdetector);
}

GST_END_TEST static void
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
    case GST_MESSAGE_EOS:{
      g_main_loop_quit (loop);
      break;
    }
    case GST_MESSAGE_WARNING:{
      GST_ERROR ("Warning: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, "error");
      fail ("Warning received on bus");
      break;
    }
    default:
      break;
  }
}

static void
connect_sink_on_srcpad_added (GstElement * playerep, GstPad * new_pad,
    gpointer user_data)
{
  gchar *padname;
  gboolean ret;

  GST_INFO_OBJECT (playerep, "Pad added %" GST_PTR_FORMAT, new_pad);
  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  if (g_str_has_prefix (padname, KMS_VIDEO_PREFIX)) {
    ret = gst_element_link_pads (playerep, padname, filter, "sink");
    fail_if (ret == FALSE);
    ret = gst_element_link (filter, fakesink_video);
    fail_if (ret == FALSE);
  } else if (g_str_has_prefix (padname, KMS_AUDIO_PREFIX)) {
    ret = gst_element_link_pads (playerep, padname, fakesink_audio, "sink");
    fail_if (ret == FALSE);
  }
  g_free (padname);
}

static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  GST_DEBUG ("Test finished exiting main loop");
  g_main_loop_quit (loop);
  return FALSE;
}

static void
player_eos (GstElement * player, GMainLoop * loop)
{
  GST_DEBUG ("Eos received");
  g_idle_add (quit_main_loop_idle, loop);
}

GST_START_TEST (player_with_pointer)
{
  GstStructure *buttonsLayout, *calibrationArea;
  guint bus_watch_id;
  GstBus *bus;
  gchar *padname;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline_live_stream");
  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  player = gst_element_factory_make ("playerendpoint", NULL);
  filter = gst_element_factory_make ("pointerdetector", NULL);
  fakesink_audio = gst_element_factory_make ("fakesink", NULL);
  fakesink_video = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);

  calibrationArea = gst_structure_new ("calibration_area",
      "x", G_TYPE_INT, 1,
      "y", G_TYPE_INT, 2,
      "width", G_TYPE_INT, 3, "height", G_TYPE_INT, 3, NULL);
  g_object_set (G_OBJECT (filter), "calibration-area", calibrationArea, NULL);
  gst_structure_free (calibrationArea);

  buttonsLayout = gst_structure_new_empty ("windowsLayout");
  configure_structure (buttonsLayout);
  g_object_set (G_OBJECT (filter), "windows-layout", buttonsLayout, NULL);
  gst_structure_free (buttonsLayout);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  gst_bin_add (GST_BIN (pipeline), filter);
  gst_element_set_state (filter, GST_STATE_PLAYING);

  gst_bin_add (GST_BIN (pipeline), fakesink_audio);
  gst_element_set_state (fakesink_audio, GST_STATE_PLAYING);

  gst_bin_add (GST_BIN (pipeline), fakesink_video);
  gst_element_set_state (fakesink_video, GST_STATE_PLAYING);

  gst_bin_add (GST_BIN (pipeline), player);

  g_signal_connect (G_OBJECT (player), "eos", G_CALLBACK (player_eos), loop);
  g_signal_connect (player, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), loop);

  /* request audio src pad using action */
  g_signal_emit_by_name (player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad %s", padname);
  g_free (padname);

  /* request video src pad using action */
  g_signal_emit_by_name (player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad %s", padname);
  g_free (padname);

  gst_element_set_state (player, GST_STATE_PLAYING);
  /* Set player to start state */
  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST
/* Define test suite */
static Suite *
pointerdetector_suite (void)
{
  Suite *s = suite_create ("pointerdetector");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, set_properties);
  tcase_add_test (tc_chain, player_with_pointer);

  return s;
}

GST_CHECK_MAIN (pointerdetector);
