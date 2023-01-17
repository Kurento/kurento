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

/* Manual test: Compile with -DMANUAL_CHECK=true
 * gst-launch-1.0 filesrc location=[path_to_wav_file] ! wavparse ! autoaudiosink
 */
#ifdef MANUAL_CHECK
G_LOCK_DEFINE (mutex);
static guint id = 0;
#endif

static GMainLoop *loop;

G_LOCK_DEFINE (hash_mutex);
GHashTable *hash;
GHashTable *padhash;
static gint counter;
GSourceFunc recv_callback;
gpointer cb_data;

static gboolean
print_timedout_pipeline (GstElement * pipeline)
{
  gchar *name;
  gchar *pipeline_name;

  pipeline_name = gst_element_get_name (pipeline);
  name = g_strdup_printf ("%s_timedout", pipeline_name);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, name);

  g_free (name);
  g_free (pipeline_name);

  return FALSE;
}

static gboolean
quit_main_loop (gpointer data)
{
  g_main_loop_quit (loop);

  return G_SOURCE_REMOVE;
}

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      GError *err = NULL;
      gchar *dbg_info = NULL;
      gchar *err_str;

      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
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
    default:
      break;
  }
}

static GstPadProbeReturn
buffer_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  gboolean call;

  GST_DEBUG_OBJECT (pad, "Removing probe %lu", GST_PAD_PROBE_INFO_ID (info));

  G_LOCK (hash_mutex);
  if (!g_hash_table_contains (padhash, pad)) {
    GST_DEBUG ("Inserting %" GST_PTR_FORMAT, pad);
    g_hash_table_insert (padhash, GST_OBJECT_NAME (pad), NULL);
    G_UNLOCK (hash_mutex);
    call = g_atomic_int_dec_and_test (&counter);
  } else {
    G_UNLOCK (hash_mutex);
    call = FALSE;
  }

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  if (call) {
    g_idle_add (recv_callback, cb_data);
  }

  return GST_PAD_PROBE_OK;
}

static void
pad_added_cb (GstElement * element, GstPad * pad, gpointer data)
{
  GstElement *pipeline = GST_ELEMENT (data);
  GstElement *wavenc, *sink;
  GstPadLinkReturn ret;
  GstPad *sinkpad;
  gchar *msg;

  if (gst_pad_get_direction (pad) != GST_PAD_SRC)
    return;

  wavenc = gst_element_factory_make ("wavenc", NULL);

#ifdef MANUAL_CHECK
  {
    gchar *filename;

    G_LOCK (mutex);
    filename = g_strdup_printf ("file_%u.wv", id++);
    GST_DEBUG ("Creating file %s", filename);
    G_UNLOCK (mutex);

    sink = gst_element_factory_make ("filesink", NULL);
    g_object_set (G_OBJECT (sink), "location", filename, NULL);
    g_free (filename);
  }
#else
  {
    sink = gst_element_factory_make ("fakesink", NULL);
  }
#endif

  g_object_set (G_OBJECT (sink), "sync", FALSE, "async", FALSE, NULL);

  gst_bin_add_many (GST_BIN (pipeline), wavenc, sink, NULL);
  sinkpad = gst_element_get_static_pad (wavenc, "sink");

  if ((ret = gst_pad_link (pad, sinkpad)) != GST_PAD_LINK_OK) {
    msg = g_strdup_printf ("Can not link pads (%d)", ret);
    gst_object_unref (sinkpad);
    goto failed;
  }

  gst_object_unref (sinkpad);

  if (!gst_element_link (wavenc, sink)) {
    msg = g_strdup_printf ("Can not link elements");
    goto failed;
  }

  sinkpad = gst_element_get_static_pad (sink, "sink");
  gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_BUFFER, buffer_probe_cb, NULL,
      NULL);
  gst_object_unref (sinkpad);

  gst_element_sync_state_with_parent (wavenc);
  gst_element_sync_state_with_parent (sink);

  G_LOCK (hash_mutex);
  g_hash_table_insert (hash, GST_OBJECT_NAME (pad), wavenc);
  G_UNLOCK (hash_mutex);

  return;

failed:

  gst_element_set_state (wavenc, GST_STATE_NULL);
  gst_element_set_state (sink, GST_STATE_NULL);
  gst_bin_remove_many (GST_BIN (pipeline), wavenc, sink, NULL);

  GST_ERROR ("Error %s", msg);
  fail (msg);
  g_free (msg);

  g_idle_add ((GSourceFunc) quit_main_loop, NULL);
}

static void
pad_removed_cb (GstElement * element, GstPad * pad, gpointer data)
{
  GstElement *wavenc;
  GstPad *sinkpad;

  if (gst_pad_get_direction (pad) != GST_PAD_SRC)
    return;

  GST_DEBUG ("Removed pad %" GST_PTR_FORMAT, pad);
  wavenc = g_hash_table_lookup (hash, GST_OBJECT_NAME (pad));

  if (wavenc == NULL)
    return;

  GST_DEBUG ("Send EOS to %s", GST_OBJECT_NAME (wavenc));

  sinkpad = gst_element_get_static_pad (wavenc, "sink");
  gst_pad_send_event (sinkpad, gst_event_new_eos ());
  gst_object_unref (sinkpad);

#ifdef MANUAL_CHECK
  {
    /* Let test last for a few seconds to have a decent output file to debug */
    g_timeout_add_seconds (2, quit_main_loop, NULL);
  }
#else
  {
    g_idle_add (quit_main_loop, NULL);
  }
#endif
}

static gboolean
recv_data_test1 (gpointer data)
{
#ifdef MANUAL_CHECK
  {
    /* Let test last for a few seconds to have a decent output file to debug */
    g_timeout_add_seconds (2, quit_main_loop, NULL);
  }
#else
  {
    g_idle_add (quit_main_loop, NULL);
  }
#endif

  return G_SOURCE_REMOVE;
}

GST_START_TEST (check_audio_connection)
{
  GstElement *pipeline, *audiotestsrc1, *audiotestsrc2, *audiotestsrc3,
      *audiomixer;
  guint bus_watch_id;
  GstBus *bus;
  gulong s1;

  g_atomic_int_set (&counter, 3);

  loop = g_main_loop_new (NULL, FALSE);
  recv_callback = recv_data_test1;
  hash = g_hash_table_new_full (g_str_hash, g_str_equal, NULL, NULL);
  padhash = g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL, NULL);

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("audimixer0-test");
  audiotestsrc1 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc2 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc3 = gst_element_factory_make ("audiotestsrc", NULL);
  audiomixer = gst_element_factory_make ("kmsaudiomixer", NULL);

  g_object_set (G_OBJECT (audiotestsrc1), "is-live", TRUE, "wave", 0, NULL);
  g_object_set (G_OBJECT (audiotestsrc2), "is-live", TRUE, "wave", 8, NULL);
  g_object_set (G_OBJECT (audiotestsrc3), "is-live", TRUE, "wave", 11, NULL);

  s1 = g_signal_connect (audiomixer, "pad-added", G_CALLBACK (pad_added_cb),
      pipeline);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc1, audiotestsrc2,
      audiotestsrc3, audiomixer, NULL);
  gst_element_link (audiotestsrc1, audiomixer);
  gst_element_link (audiotestsrc2, audiomixer);
  gst_element_link (audiotestsrc3, audiomixer);

  g_timeout_add_seconds (4, (GSourceFunc) print_timedout_pipeline, pipeline);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG ("Test running");

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  g_signal_handler_disconnect (audiomixer, s1);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipe released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);

  g_hash_table_unref (hash);
  g_hash_table_unref (padhash);
  hash = NULL;
  padhash = NULL;
}

GST_END_TEST static gboolean
remove_audiotestsrc (GstElement * audiotestsrc)
{
  GstElement *pipeline = GST_ELEMENT (gst_element_get_parent (audiotestsrc));

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
  if (sinkpad == NULL) {
    GST_DEBUG_OBJECT (pad, "No peer pad");
    return GST_PAD_PROBE_DROP;
  }

  audiomixer = gst_pad_get_parent_element (sinkpad);
  if (audiomixer == NULL) {
    GST_DEBUG_OBJECT (pad, "No audiomixer");
    return GST_PAD_PROBE_DROP;
  }

  if (!gst_pad_unlink (pad, sinkpad)) {
    GST_ERROR ("Can not unilnk pads");
  }

  GST_DEBUG ("Releasing %" GST_PTR_FORMAT, sinkpad);
  gst_element_release_request_pad (audiomixer, sinkpad);

  gst_object_unref (sinkpad);
  gst_object_unref (audiomixer);

  g_idle_add ((GSourceFunc) remove_audiotestsrc, audiotestsrc);

  return GST_PAD_PROBE_DROP;
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

static gboolean
recv_data_test2 (gpointer data)
{
#ifdef MANUAL_CHECK
  {
    /* Let test last for a few seconds to have a decent output file to debug */
    g_timeout_add_seconds (2, (GSourceFunc) block_audiotestsrc, data);
  }
#else
  {
    g_idle_add ((GSourceFunc) block_audiotestsrc, data);
  }
#endif

  return G_SOURCE_REMOVE;
}

GST_START_TEST (check_audio_disconnection)
{
  GstElement *pipeline, *audiotestsrc1, *audiotestsrc2, *audiotestsrc3,
      *audiomixer;
  guint bus_watch_id;
  GstBus *bus;
  gulong s1, s2;

  g_atomic_int_set (&counter, 3);
  loop = g_main_loop_new (NULL, FALSE);
  recv_callback = recv_data_test2;
  hash = g_hash_table_new_full (g_str_hash, g_str_equal, NULL, NULL);
  padhash = g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL, NULL);

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("audimixer0-test");
  audiotestsrc1 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc2 = gst_element_factory_make ("audiotestsrc", NULL);
  audiotestsrc3 = gst_element_factory_make ("audiotestsrc", NULL);
  audiomixer = gst_element_factory_make ("kmsaudiomixer", NULL);

  cb_data = audiotestsrc1;

  g_object_set (G_OBJECT (audiotestsrc1), "is-live", TRUE, "wave", 0, NULL);
  g_object_set (G_OBJECT (audiotestsrc2), "is-live", TRUE, "wave", 8, NULL);
  g_object_set (G_OBJECT (audiotestsrc3), "is-live", TRUE, "wave", 11, NULL);

  s1 = g_signal_connect (audiomixer, "pad-added", G_CALLBACK (pad_added_cb),
      pipeline);
  s2 = g_signal_connect (audiomixer, "pad-removed", G_CALLBACK (pad_removed_cb),
      pipeline);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc1, audiotestsrc2,
      audiotestsrc3, audiomixer, NULL);
  gst_element_link (audiotestsrc1, audiomixer);
  gst_element_link (audiotestsrc2, audiomixer);
  gst_element_link (audiotestsrc3, audiomixer);

  g_timeout_add_seconds (4, (GSourceFunc) print_timedout_pipeline, pipeline);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG ("Test running");

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  g_signal_handler_disconnect (audiomixer, s1);
  g_signal_handler_disconnect (audiomixer, s2);

  GST_DEBUG ("Setting pipline to NULL state");
  gst_element_set_state (pipeline, GST_STATE_NULL);
  GST_DEBUG ("Releasing pipeline");
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipeline released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);

  g_hash_table_unref (hash);
  g_hash_table_unref (padhash);
  hash = NULL;
  padhash = NULL;
}

GST_END_TEST
/******************************/
/* audiomixer test suit */
/******************************/
static Suite *
audiomixer_suite (void)
{
  Suite *s = suite_create ("kmsaudiomixer");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, check_audio_connection);
  tcase_add_test (tc_chain, check_audio_disconnection);

  return s;
}

GST_CHECK_MAIN (audiomixer);
