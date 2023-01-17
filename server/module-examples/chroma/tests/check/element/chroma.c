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
#include <commons/kmsuriendpointstate.h>

#include <kmstestutils.h>

#define SET_BACKGROUND_URI "image-background"
#define SET_CALIBRATION_AREA "calibration-area"

#define IMG_PATH BINARY_LOCATION "/img/mario.jpg"
#define VIDEO_PATH BINARY_LOCATION "/video/format/small.webm"

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

GMainLoop *loop;
GstElement *player, *pipeline, *filter, *fakesink_audio, *fakesink_video;

GST_START_TEST (set_properties)
{
  GstElement *chroma;
  GstStructure *calibrationArea;

  chroma = gst_element_factory_make ("chroma", NULL);
  calibrationArea = gst_structure_new ("calibration_area",
      "x", G_TYPE_INT, 20,
      "y", G_TYPE_INT, 20,
      "width", G_TYPE_INT, 50, "height", G_TYPE_INT, 50, NULL);

  g_object_set (G_OBJECT (chroma), SET_CALIBRATION_AREA, calibrationArea, NULL);
  gst_structure_free (calibrationArea);

  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, IMG_PATH, NULL);
  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, NULL, NULL);

  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, IMG_PATH, NULL);
  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, NULL, NULL);

  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, IMG_PATH, NULL);
  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, NULL, NULL);

  g_object_unref (chroma);
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

GST_START_TEST (player_with_filter)
{
  guint bus_watch_id;
  GstBus *bus;
  GstStructure *calibrationArea;
  gchar *padname;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline_live_stream");
  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  player = gst_element_factory_make ("playerendpoint", NULL);
  filter = gst_element_factory_make ("chroma", NULL);
  fakesink_audio = gst_element_factory_make ("fakesink", NULL);
  fakesink_video = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);

  calibrationArea = gst_structure_new ("calibration_area",
      "x", G_TYPE_INT, 20,
      "y", G_TYPE_INT, 20,
      "width", G_TYPE_INT, 50, "height", G_TYPE_INT, 50, NULL);

  g_object_set (G_OBJECT (filter), SET_CALIBRATION_AREA, calibrationArea, NULL);
  gst_structure_free (calibrationArea);

  g_object_set (G_OBJECT (filter), SET_BACKGROUND_URI, IMG_PATH, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  gst_bin_add_many (GST_BIN (pipeline), filter, fakesink_audio, fakesink_video,
      player, NULL);
  gst_element_set_state (filter, GST_STATE_PLAYING);
  gst_element_set_state (fakesink_audio, GST_STATE_PLAYING);
  gst_element_set_state (fakesink_video, GST_STATE_PLAYING);

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

GST_END_TEST static Suite *
chroma_suite (void)
{
  Suite *s = suite_create ("chroma");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, set_properties);
  tcase_add_test (tc_chain, player_with_filter);

  return s;
}

GST_CHECK_MAIN (chroma);
