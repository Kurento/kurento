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

#define ROIS_PARAM "rois"
#define VIDEO_PATH BINARY_LOCATION "/video/filter/crowd.mp4"
#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

GMainLoop *loop;
GstElement *player, *pipeline, *filter, *fakesink_audio, *fakesink_video;

GstStructure *
get_roi_structure (const gchar * id)
{
  int pointCount = 0;
  GstStructure *roiStructure, *configRoiSt;

  roiStructure = gst_structure_new_empty (id);
  for (pointCount = 0; pointCount < 4; pointCount++) {
    GstStructure *pointSt;
    gchar *name;

    name = g_strdup_printf ("point%d", pointCount);
    pointSt = gst_structure_new (name,
        "x", G_TYPE_FLOAT, 0.1 + ((float) pointCount / 100.0),
        "y", G_TYPE_FLOAT, 0.1 + ((float) pointCount / 100.0), NULL);
    gst_structure_set (roiStructure, name, GST_TYPE_STRUCTURE, pointSt, NULL);
    gst_structure_free (pointSt);
    g_free (name);
  }
  configRoiSt = gst_structure_new ("config",
      "id", G_TYPE_STRING, id,
      "occupancy_level_min", G_TYPE_INT, 10,
      "occupancy_level_med", G_TYPE_INT, 35,
      "occupancy_level_max", G_TYPE_INT, 65,
      "occupancy_num_frames_to_event", G_TYPE_INT,
      5,
      "fluidity_level_min", G_TYPE_INT, 10,
      "fluidity_level_med", G_TYPE_INT, 35,
      "fluidity_level_max", G_TYPE_INT, 65,
      "fluidity_num_frames_to_event", G_TYPE_INT, 5,
      "send_optical_flow_event", G_TYPE_BOOLEAN, FALSE,
      "optical_flow_num_frames_to_event", G_TYPE_INT,
      3,
      "optical_flow_num_frames_to_reset", G_TYPE_INT,
      3, "optical_flow_angle_offset", G_TYPE_INT, 0, NULL);
  gst_structure_set (roiStructure, "config", GST_TYPE_STRUCTURE, configRoiSt,
      NULL);
  gst_structure_free (configRoiSt);
  return roiStructure;
}

GST_START_TEST (set_properties)
{
  GstElement *crowddetector;
  GstStructure *roisStructure;
  GstStructure *roiStructureAux;

  crowddetector = gst_element_factory_make ("crowddetector", NULL);

  roisStructure = gst_structure_new_empty ("Rois");
  roiStructureAux = get_roi_structure ("roi1");
  gst_structure_set (roisStructure,
      "roi1", GST_TYPE_STRUCTURE, roiStructureAux, NULL);
  gst_structure_free (roiStructureAux);

  roiStructureAux = get_roi_structure ("roi2");
  gst_structure_set (roisStructure,
      "roi2", GST_TYPE_STRUCTURE, roiStructureAux, NULL);
  gst_structure_free (roiStructureAux);

  g_object_set (G_OBJECT (crowddetector), ROIS_PARAM, roisStructure, NULL);

  g_object_set (G_OBJECT (crowddetector), ROIS_PARAM, roisStructure, NULL);

  g_object_set (G_OBJECT (crowddetector), ROIS_PARAM, roisStructure, NULL);

  gst_structure_free (roisStructure);

  g_object_unref (crowddetector);
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
  GstStructure *roisStructure;
  GstStructure *roiStructureAux;
  gchar *padname;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline_live_stream");
  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  player = gst_element_factory_make ("playerendpoint", NULL);
  filter = gst_element_factory_make ("crowddetector", NULL);
  fakesink_audio = gst_element_factory_make ("fakesink", NULL);
  fakesink_video = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);

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

  roisStructure = gst_structure_new_empty ("Rois");
  roiStructureAux = get_roi_structure ("roi1");
  gst_structure_set (roisStructure,
      "roi1", GST_TYPE_STRUCTURE, roiStructureAux, NULL);
  gst_structure_free (roiStructureAux);
  g_object_set (G_OBJECT (filter), ROIS_PARAM, roisStructure, NULL);

  gst_structure_free (roisStructure);

  /* Set player to start state */
  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST static Suite *
crowddetector_suite (void)
{
  Suite *s = suite_create ("crowddetector");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, set_properties);
  tcase_add_test (tc_chain, player_with_filter);

  return s;
}

GST_CHECK_MAIN (crowddetector);
