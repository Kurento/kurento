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

#ifdef MANUAL_CHECK
#define FILE_PREFIX "audiomixerbin_file_"
static guint id = 0;
#endif

static GstElement *pipeline, *audiomixer;
static GMainLoop *loop;

static gboolean
quit_main_loop ()
{
  g_main_loop_quit (loop);

  return G_SOURCE_REMOVE;
}

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer data)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      GError *err = NULL;
      gchar *dbg_info = NULL;
      gchar *err_str;

      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "bus_error");
      gst_message_parse_error (msg, &err, &dbg_info);

      err_str = g_strdup_printf ("Error received on bus: %s: %s", err->message,
          dbg_info);
      g_error_free (err);
      g_free (dbg_info);

      fail (err_str);
      g_free (err_str);

      break;
    }
    case GST_MESSAGE_STATE_CHANGED:{
      GST_TRACE ("Event: %" GST_PTR_FORMAT, msg);
      break;
    }
    case GST_MESSAGE_EOS:{
      GST_DEBUG ("Receive EOS");
      quit_main_loop ();
    }
    default:
      break;
  }
}

static GstElement *
create_sink_element ()
{
  GstElement *sink;

#ifdef MANUAL_CHECK
  {
    gchar *filename;

    filename = g_strdup_printf (FILE_PREFIX "%u.wv", id);

    GST_DEBUG ("Setting location to %s", filename);
    sink = gst_element_factory_make ("filesink", NULL);
    g_object_set (G_OBJECT (sink), "location", filename, NULL);
    g_free (filename);
  }
#else
  {
    sink = gst_element_factory_make ("fakesink", NULL);
  }
#endif

  return sink;
}

#ifdef ENABLE_EXPERIMENTAL_TESTS

GST_START_TEST (check_audio_connection)
{
  GstElement *audiotestsrc1, *audiotestsrc2, *wavenc, *sink;
  guint bus_watch_id;
  GstBus *bus;

  loop = g_main_loop_new (NULL, FALSE);
#ifdef MANUAL_CHECK
  id = 0;
#endif

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("audimixerbin0-test");
  audiotestsrc1 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc2 = gst_element_factory_make ("audiotestsrc", NULL);
  audiomixer = gst_element_factory_make ("audiomixerbin", NULL);
  wavenc = gst_element_factory_make ("wavenc", NULL);
  sink = create_sink_element ();

  g_object_set (G_OBJECT (audiotestsrc1), "wave", 0, "num-buffers", 100, NULL);
  g_object_set (G_OBJECT (audiotestsrc2), "wave", 11, "num-buffers", 100, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc1, audiotestsrc2,
      audiomixer, wavenc, sink, NULL);
  gst_element_link (audiotestsrc1, audiomixer);
  gst_element_link (audiotestsrc2, audiomixer);
  gst_element_link_many (audiomixer, wavenc, sink, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG ("Test running");

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  GST_DEBUG ("Setting pipline to NULL state");
  gst_element_set_state (pipeline, GST_STATE_NULL);
  GST_DEBUG ("Releasing pipeline");
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipeline released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST;

static gboolean
connect_audio_source (gpointer data)
{
  GstElement *audiotestsrc2;

  GST_DEBUG ("Adding audio source 2");
  audiotestsrc2 = gst_element_factory_make ("audiotestsrc", NULL);
  g_object_set (G_OBJECT (audiotestsrc2), "wave", 11, "num-buffers", 100, NULL);

  gst_bin_add (GST_BIN (pipeline), audiotestsrc2);
  gst_element_link (audiotestsrc2, audiomixer);
  gst_element_sync_state_with_parent (audiotestsrc2);

  return G_SOURCE_REMOVE;
}

GST_START_TEST (check_delayed_audio_connection)
{
  GstElement *audiotestsrc1, *wavenc, *sink;
  guint bus_watch_id;
  GstBus *bus;

  loop = g_main_loop_new (NULL, FALSE);
#ifdef MANUAL_CHECK
  id = 1;
#endif

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("audimixerbin1-test");
  audiotestsrc1 = gst_element_factory_make ("audiotestsrc", NULL);
  audiomixer = gst_element_factory_make ("audiomixerbin", NULL);
  wavenc = gst_element_factory_make ("wavenc", NULL);
  sink = create_sink_element ();

  g_object_set (G_OBJECT (audiotestsrc1), "wave", 0, "num-buffers", 100, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc1, audiomixer, wavenc, sink,
      NULL);
  gst_element_link_many (audiotestsrc1, audiomixer, wavenc, sink, NULL);

  g_timeout_add (1000, connect_audio_source, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG ("Test running");

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  GST_DEBUG ("Setting pipline to NULL state");
  gst_element_set_state (pipeline, GST_STATE_NULL);
  GST_DEBUG ("Releasing pipeline");
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipeline released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST;

#endif // ENABLE_EXPERIMENTAL_TESTS

static gboolean
remove_audiotestsrc (GstElement * audiotestsrc)
{
  GstElement *pipeline = GST_ELEMENT (gst_element_get_parent (audiotestsrc));

  GST_DEBUG ("Remove element %" GST_PTR_FORMAT, audiotestsrc);

  gst_object_ref (audiotestsrc);
  gst_element_set_locked_state (audiotestsrc, TRUE);
  gst_element_set_state (audiotestsrc, GST_STATE_NULL);
  if (!gst_bin_remove (GST_BIN (pipeline), audiotestsrc))
    GST_ERROR ("Can not remove %" GST_PTR_FORMAT, audiotestsrc);

  gst_object_unref (pipeline);
  gst_object_unref (audiotestsrc);

  return G_SOURCE_REMOVE;
}

static GstPadProbeReturn
unlink_audiotestsrc (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstElement *audiotestsrc = GST_ELEMENT (user_data);
  GstElement *audiomixer;
  GstPad *sinkpad;

  GST_DEBUG ("Blocked %" GST_PTR_FORMAT, pad);

  sinkpad = gst_pad_get_peer (pad);
  audiomixer = gst_pad_get_parent_element (sinkpad);

  if (!gst_pad_unlink (pad, sinkpad)) {
    GST_ERROR ("Can not unilnk pads");
  }

  GST_INFO ("Releasing pad %" GST_PTR_FORMAT, sinkpad);
  gst_element_release_request_pad (audiomixer, sinkpad);

  gst_object_unref (sinkpad);
  gst_object_unref (audiomixer);

  g_idle_add ((GSourceFunc) remove_audiotestsrc, audiotestsrc);

  return GST_PAD_PROBE_OK;
}

static gboolean
block_audiotestsrc (GstElement * audiotestsrc)
{
  GstPad *srcpad;

  GST_DEBUG ("Blocking %" GST_PTR_FORMAT, audiotestsrc);

  srcpad = gst_element_get_static_pad (audiotestsrc, "src");
  gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM,
      unlink_audiotestsrc, audiotestsrc, NULL);
  gst_object_unref (srcpad);

  return G_SOURCE_REMOVE;
}

GST_START_TEST (check_audio_disconnection)
{
  GstElement *audiotestsrc1, *audiotestsrc2, *audiotestsrc3, *wavenc, *sink;
  guint bus_watch_id;
  GstBus *bus;

  loop = g_main_loop_new (NULL, FALSE);
#ifdef MANUAL_CHECK
  id = 2;
#endif

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("audimixer2-test");
  audiotestsrc1 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc2 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc3 = gst_element_factory_make ("audiotestsrc", NULL);
  audiomixer = gst_element_factory_make ("audiomixerbin", NULL);
  wavenc = gst_element_factory_make ("wavenc", NULL);
  sink = create_sink_element ();

  g_object_set (G_OBJECT (audiotestsrc1), "wave", 0, NULL);
  g_object_set (G_OBJECT (audiotestsrc2), "wave", 8, NULL);
  g_object_set (G_OBJECT (audiotestsrc3), "wave", 11, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc1, audiotestsrc2,
      audiotestsrc3, audiomixer, wavenc, sink, NULL);

  gst_element_link (audiotestsrc1, audiomixer);
  gst_element_link (audiotestsrc2, audiomixer);
  gst_element_link (audiotestsrc3, audiomixer);
  gst_element_link (audiomixer, wavenc);
  gst_element_link (wavenc, sink);

  g_timeout_add (1000, (GSourceFunc) block_audiotestsrc, audiotestsrc1);
  g_timeout_add (2000, (GSourceFunc) block_audiotestsrc, audiotestsrc2);
  g_timeout_add (3000, (GSourceFunc) quit_main_loop, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG ("Test running");

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  GST_DEBUG ("Setting pipline to NULL state");
  gst_element_set_state (pipeline, GST_STATE_NULL);
  GST_DEBUG ("Releasing pipeline");
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipeline released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST
/******************************/
/* audiomixer test suit */
/******************************/
static Suite *
audiomixerbin_suite (void)
{
  Suite *s = suite_create ("audiomixerbin");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
#ifdef ENABLE_EXPERIMENTAL_TESTS
  tcase_add_test (tc_chain, check_audio_connection);
  tcase_add_test (tc_chain, check_delayed_audio_connection);
#endif
  tcase_add_test (tc_chain, check_audio_disconnection);

  return s;
}

GST_CHECK_MAIN (audiomixerbin);
