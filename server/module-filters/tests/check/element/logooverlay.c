/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#define IMG_PATH BINARY_LOCATION "/img/mario-wings.png"
#define VIDEO_PATH BINARY_LOCATION "/video/format/small.webm"

#define IMAGES_TO_OVERLAY "images-to-overlay"

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

GMainLoop *loop;
GstElement *player, *pipeline, *filter, *fakesink_audio, *fakesink_video;
GstClockID clock_id;

GstStructure *
generate_logo_structure ()
{
  GstStructure *imageSt;

  imageSt = gst_structure_new ("image_position",
      "id", G_TYPE_STRING, "image",
      "uri", G_TYPE_STRING, IMG_PATH,
      "offsetXPercent", G_TYPE_FLOAT, 0.2,
      "offsetYPercent", G_TYPE_FLOAT, 0.2,
      "widthPercent", G_TYPE_FLOAT, 0.2,
      "heightPercent", G_TYPE_FLOAT, 0.2,
      "keepAspectRatio", G_TYPE_BOOLEAN, TRUE,
      "center", G_TYPE_BOOLEAN, TRUE, NULL);

  return imageSt;
}

GST_START_TEST (set_properties)
{
  GstElement *logooverlay;
  GstStructure *imageSt;

  logooverlay = gst_element_factory_make ("logooverlay", NULL);

  imageSt = generate_logo_structure ();

  //set images
  g_object_set (G_OBJECT (logooverlay), IMAGES_TO_OVERLAY, imageSt, NULL);
  g_object_set (G_OBJECT (logooverlay), IMAGES_TO_OVERLAY, imageSt, NULL);

  //unset images
  g_object_set (G_OBJECT (logooverlay), IMAGES_TO_OVERLAY, NULL, NULL);

  //set images
  g_object_set (G_OBJECT (logooverlay), IMAGES_TO_OVERLAY, imageSt, NULL);

  gst_structure_free (imageSt);
  g_object_unref (logooverlay);
}
GST_END_TEST

static void
bus_msg_cb (GstBus * bus, GstMessage * msg, gpointer pipeline)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR: {
      GError *err = NULL;
      gchar *dbg_info = NULL;

      gst_message_parse_error (msg, &err, &dbg_info);
      GST_ERROR ("Pipeline '%s': Bus error %d: %s", GST_ELEMENT_NAME (pipeline),
          err->code, GST_STR_NULL (err->message));
      GST_ERROR ("Debugging info: %s", GST_STR_NULL (dbg_info));
      g_error_free (err);
      g_free (dbg_info);

      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "bus_error");

      fail ("Pipeline '%s': Bus error", GST_ELEMENT_NAME (pipeline));

      break;
    }
    case GST_MESSAGE_WARNING: {
      GError *err = NULL;
      gchar *dbg_info = NULL;

      gst_message_parse_error (msg, &err, &dbg_info);
      GST_WARNING ("Pipeline '%s': Bus warning %d: %s",
          GST_ELEMENT_NAME (pipeline), err->code, GST_STR_NULL (err->message));
      GST_WARNING ("Debugging info: %s", GST_STR_NULL (dbg_info));
      g_error_free (err);
      g_free (dbg_info);

      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "bus_warning");

      fail ("Pipeline '%s': Bus warning", GST_ELEMENT_NAME (pipeline));

      break;
    }
    case GST_MESSAGE_EOS: {
      GST_DEBUG ("Pipeline '%s': Bus event: EOS (%s)",
          GST_ELEMENT_NAME (pipeline), GST_OBJECT_NAME (msg->src));

      g_main_loop_quit (loop);

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
  GstStructure *imageSt;
  gchar *padname;

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline_live_stream");
  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  player = gst_element_factory_make ("playerendpoint", NULL);
  filter = gst_element_factory_make ("logooverlay", NULL);
  fakesink_audio = gst_element_factory_make ("fakesink", NULL);
  fakesink_video = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);

  gst_element_link (filter, fakesink_video);

  imageSt = generate_logo_structure ();
  g_object_set (G_OBJECT (filter), IMAGES_TO_OVERLAY, imageSt, NULL);
  gst_structure_free (imageSt);

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

GST_END_TEST gboolean
change_structure (GstClock * clock,
    GstClockTime time, GstClockID id, gpointer user_data)
{
  GstStructure *imageSt;

  imageSt = generate_logo_structure ();
  g_object_set (G_OBJECT (filter), IMAGES_TO_OVERLAY, imageSt, NULL);
  gst_structure_free (imageSt);

  return TRUE;
}

static void
player_eos_schedule (GstElement * player, GMainLoop * loop)
{
  GST_DEBUG ("Eos received");
  gst_clock_id_unschedule (clock_id);
  gst_clock_id_unref (clock_id);

  g_idle_add (quit_main_loop_idle, loop);
}

GST_START_TEST (player_with_filter_many_changes)
{
  guint bus_watch_id;
  GstBus *bus;
  GstStructure *imageSt;
  gchar *padname;
  GstClock *clk;
  GstClockTime init_time;

  clk = gst_system_clock_obtain ();
  fail_if (clk == NULL);

  init_time = gst_clock_get_time (clk);
  clock_id = gst_clock_new_periodic_id (clk, init_time, GST_SECOND);
  g_object_unref (clk);

  loop = g_main_loop_new (NULL, FALSE);
  pipeline = gst_pipeline_new ("pipeline_live_stream");
  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  player = gst_element_factory_make ("playerendpoint", NULL);
  filter = gst_element_factory_make ("logooverlay", NULL);
  fakesink_audio = gst_element_factory_make ("fakesink", NULL);
  fakesink_video = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);

  gst_element_link (filter, fakesink_video);

  imageSt = generate_logo_structure ();
  g_object_set (G_OBJECT (filter), IMAGES_TO_OVERLAY, imageSt, NULL);
  gst_structure_free (imageSt);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  gst_bin_add_many (GST_BIN (pipeline), filter, fakesink_audio, fakesink_video,
      player, NULL);
  gst_element_set_state (filter, GST_STATE_PLAYING);
  gst_element_set_state (fakesink_audio, GST_STATE_PLAYING);
  gst_element_set_state (fakesink_video, GST_STATE_PLAYING);

  g_signal_connect (G_OBJECT (player), "eos", G_CALLBACK (player_eos_schedule),
      loop);
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

  gst_clock_id_wait_async (clock_id, change_structure, NULL, NULL);

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
logooverlay_suite (void)
{
  Suite *s = suite_create ("logooverlay");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, set_properties);
  tcase_add_test (tc_chain, player_with_filter);
  tcase_add_test (tc_chain, player_with_filter_many_changes);

  return s;
}

GST_CHECK_MAIN (logooverlay);
