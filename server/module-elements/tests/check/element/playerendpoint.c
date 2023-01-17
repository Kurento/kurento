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

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <commons/kmsuriendpointstate.h>

#include <kmstestutils.h>

#define VIDEO_PATH BINARY_LOCATION "/video/filter/fiwarecut.webm"
#define VIDEO_LENGTH 30

#define VIDEO_PATH2 BINARY_LOCATION "/video/format/sintel.webm"
#define VIDEO_LENGTH2 52

#define VIDEO_PATH3 BINARY_LOCATION "/video/format/small.webm"
#define VIDEO_LENGTH3 6

#define KMS_ELEMENT_PAD_TYPE_DATA 0
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

static GMainLoop *loop = NULL;
static GstElement *player = NULL;
static GstElement *fakesink = NULL;
static GstElement *pipeline = NULL;

static guint state = 0;

G_LOCK_DEFINE (handoff_lock);
static gboolean start_buffer = FALSE;

static const KmsUriEndpointState trasnsitions[] = {
  KMS_URI_ENDPOINT_STATE_START,
  KMS_URI_ENDPOINT_STATE_PAUSE,
  KMS_URI_ENDPOINT_STATE_START,
  KMS_URI_ENDPOINT_STATE_STOP
};

static guint
g_idle_add_loop (GMainLoop *loop, GSourceFunc function, gpointer user_data)
{
  GSource *source = g_idle_source_new ();
  g_source_set_callback (source, function, user_data, NULL);
  guint id = g_source_attach (source, g_main_loop_get_context (loop));
  g_source_unref (source);

  return id;
}

static gchar *
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
change_state (KmsUriEndpointState state)
{
  GST_DEBUG ("Setting player to state %s", state2string (state));
  g_object_set (G_OBJECT (player), "state", state, NULL);
}

static gboolean
print_timedout_pipeline (gpointer data)
{
  gchar *pipeline_name;
  gchar *name;

  GST_ERROR ("Test timed out; generating DOT file...");

  pipeline_name = gst_element_get_name (pipeline);
  name = g_strdup_printf ("%s_timedout", pipeline_name);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, name);

  g_free (name);
  g_free (pipeline_name);

  return FALSE;
}

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

      break;
    }
    default:
      break;
  }
}

static gboolean transite_cb (gpointer);

static void
transite (gpointer user_data)
{
  if (state < G_N_ELEMENTS (trasnsitions)) {
    change_state (trasnsitions[state]);
  } else {
    GMainLoop *loop = (GMainLoop *) user_data;

    GST_DEBUG ("All transitions done. Finishing player check states suit");
    g_main_loop_quit (loop);
  }
}

static gboolean
transite_cb (gpointer user_data)
{
  state++;
  transite (user_data);
  return FALSE;
}

static GstPadProbeReturn
data_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GST_DEBUG_OBJECT (pad, "buffer received");

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  g_idle_add_loop (loop, transite_cb, NULL);

  return GST_PAD_PROBE_OK;
}

static void
state_changed_cb (GstElement * player, KmsUriEndpointState newState,
    gpointer user_data)
{
  GST_DEBUG_OBJECT (player, "State changed %s.", state2string (newState));

  switch (trasnsitions[state]) {
    case KMS_URI_ENDPOINT_STATE_START:{
      gchar *padname = *(gchar **) user_data;
      GstPad *srcpad;

      srcpad = gst_element_get_static_pad (player, padname);
      if (srcpad == NULL) {
        /* Source pad is not yet created */
        return;
      }

      gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BUFFER,
          (GstPadProbeCallback) data_probe_cb, NULL, NULL);
      g_object_unref (srcpad);
      break;
    }
    case KMS_URI_ENDPOINT_STATE_PAUSE:
    case KMS_URI_ENDPOINT_STATE_STOP:
      g_idle_add_loop (loop, transite_cb, loop);
      break;
  }
}

static void
handoff (GstElement * object, GstBuffer * arg0,
    GstPad * arg1, gpointer user_data)
{
  G_LOCK (handoff_lock);

  if (!start_buffer) {
    start_buffer = TRUE;
    /* First buffer received, start transitions */
    g_idle_add_loop (loop, transite_cb, NULL);
  }

  G_UNLOCK (handoff_lock);
}

static void
srcpad_added (GstElement * playerep, GstPad * new_pad, gpointer user_data)
{
  gchar *padname, *expected_name;
  GstPad *sinkpad;

  GST_INFO_OBJECT (playerep, "Pad added %" GST_PTR_FORMAT, new_pad);
  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  expected_name = *(gchar **) user_data;
  fail_if (g_strcmp0 (padname, expected_name) != 0);

  sinkpad = gst_element_get_static_pad (fakesink, "sink");

  fail_if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK);

  g_object_unref (sinkpad);
  g_free (padname);
}

GST_START_TEST (check_states)
{
  guint bus_watch_id;
  gchar *padname;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new (__FUNCTION__);
  player = gst_element_factory_make ("playerendpoint", NULL);
  fakesink = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);
  g_object_set (G_OBJECT (fakesink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (fakesink, "handoff", G_CALLBACK (handoff), loop);
  g_signal_connect (player, "pad-added", G_CALLBACK (srcpad_added), &padname);

  g_signal_connect (player, "state-changed", G_CALLBACK (state_changed_cb),
      &padname);

  gst_bin_add_many (GST_BIN (pipeline), player, fakesink, NULL);

  /* request src pad using action */
  g_signal_emit_by_name (player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad %s", padname);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  transite (loop);

  g_timeout_add_seconds (VIDEO_LENGTH + 3, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  fail_unless (start_buffer == TRUE);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
  g_free (padname);
}

GST_END_TEST
/* check_live_stream */
static gboolean buffer_audio = FALSE;
static gboolean buffer_video = FALSE;

static gboolean
check_handoff_audio (gpointer user_data)
{
  buffer_audio = TRUE;

  if (buffer_audio && buffer_video) {
    GMainLoop *loop = (GMainLoop *) user_data;

    g_main_loop_quit (loop);
  }

  return FALSE;
}

static gboolean
check_handoff_video (gpointer user_data)
{
  buffer_video = TRUE;

  if (buffer_audio && buffer_video) {
    GMainLoop *loop = (GMainLoop *) user_data;

    g_main_loop_quit (loop);
  }

  return FALSE;
}

static void
handoff_audio (GstElement * object, GstBuffer * arg0,
    GstPad * arg1, gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;

  GST_TRACE ("handoff_audio");
  g_idle_add_loop (loop, (GSourceFunc) check_handoff_audio, loop);
}

static void
handoff_video (GstElement * object, GstBuffer * arg0,
    GstPad * arg1, gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;

  GST_TRACE ("handoff_video");
  g_idle_add_loop (loop, (GSourceFunc) check_handoff_video, loop);
}

static void
check_live_stream_on_pad_added (GstElement * playerep, GstPad * new_pad,
    gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;
  GstElement *sink;
  gchar *padname;
  GCallback func;
  GstPad *sinkpad;

  GST_INFO_OBJECT (playerep, "Pad added %" GST_PTR_FORMAT, new_pad);
  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  if (g_str_has_prefix (padname, KMS_VIDEO_PREFIX)) {
    GST_DEBUG_OBJECT (playerep, "Connecting video stream");
    func = G_CALLBACK (handoff_video);
  } else if (g_str_has_prefix (padname, KMS_AUDIO_PREFIX)) {
    GST_DEBUG_OBJECT (playerep, "Connecting audio stream");
    func = G_CALLBACK (handoff_audio);
  } else {
    GST_ERROR_OBJECT (playerep, "Not supported pad type");
    return;
  }

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", func, loop);

  gst_bin_add (GST_BIN (pipeline), sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  fail_if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK);

  gst_element_sync_state_with_parent (sink);

  g_object_unref (sinkpad);
  g_free (padname);
}

GST_START_TEST (check_live_stream)
{
  guint bus_watch_id;
  gchar *padname;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new (__FUNCTION__);
  player = gst_element_factory_make ("playerendpoint", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH2, NULL);

  g_signal_connect (player, "pad-added",
      G_CALLBACK (check_live_stream_on_pad_added), loop);

  gst_bin_add (GST_BIN (pipeline), player);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

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

  /* Set player to start state */
  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  g_timeout_add_seconds (VIDEO_LENGTH2 + 3, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
}
GST_END_TEST

/* check_eos */
static gboolean
quit_main_loop_idle (gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;

  GST_DEBUG ("Test finished; exiting main loop");
  g_main_loop_quit (loop);
  return FALSE;
}

static void
player_eos (GstElement * player, GMainLoop * loop)
{
  GST_DEBUG ("EOS received");
  g_idle_add_loop (loop, quit_main_loop_idle, loop);
}

/* EOS test */
GST_START_TEST (check_eos)
{
  guint bus_watch_id;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new (__FUNCTION__);
  player = gst_element_factory_make ("playerendpoint", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH3, NULL);

  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  gst_bin_add (GST_BIN (pipeline), player);

  g_signal_connect (G_OBJECT (player), "eos", G_CALLBACK (player_eos), loop);

  /* Set player to start state */
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (VIDEO_LENGTH3 + 3, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
}

GST_END_TEST

#ifdef ENABLE_EXPERIMENTAL_TESTS

GST_START_TEST (check_set_encoded_media)
{
  GstElement *player, *pipeline;
  guint bus_watch_id;
  GMainLoop *loop;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new ("pipeline_live_stream");
  player = gst_element_factory_make ("playerendpoint", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH3, NULL);

  g_object_set (G_OBJECT (player), "use-encoded-media", TRUE, NULL);

  gst_bin_add (GST_BIN (pipeline), player);

  g_signal_connect (G_OBJECT (player), "eos", G_CALLBACK (player_eos), loop);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* Set player to start state */
  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "before_entering_main_loop_live_stream");

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_entering_main_loop_live_stream");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
}

GST_END_TEST;

#endif // ENABLE_EXPERIMENTAL_TESTS

/* Define test suite */
static Suite *
playerendpoint_suite (void)
{
  Suite *s = suite_create ("playerendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, check_states);
  tcase_add_test (tc_chain, check_live_stream);
  tcase_add_test (tc_chain, check_eos);
#ifdef ENABLE_EXPERIMENTAL_TESTS
  tcase_add_test (tc_chain, check_set_encoded_media);
#endif

  return s;
}

GST_CHECK_MAIN (playerendpoint);
