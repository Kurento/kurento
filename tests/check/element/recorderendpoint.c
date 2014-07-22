/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

#include <commons/kmsuriendpointstate.h>

gboolean set_state_start (gpointer *);
gboolean set_state_pause (gpointer *);
gboolean set_state_stop (gpointer *);

static GstElement *recorder = NULL;
static guint number_of_transitions;
static gboolean expected_warnings;
static guint test_number;
static guint state;

struct state_controller
{
  KmsUriEndpointState state;
  guint seconds;
};

static const struct state_controller trasnsitions0[] = {
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_STOP, 1},
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_PAUSE, 1},
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_STOP, 1}
};

static const struct state_controller trasnsitions1[] = {
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_STOP, 1},
  {KMS_URI_ENDPOINT_STATE_START, 1}
};

static const struct state_controller *
get_transtions ()
{
  switch (test_number) {
    case 0:
      return trasnsitions0;
    case 1:
      return trasnsitions1;
    default:
      fail ("Undefined transitions for test %d.", test_number);
      return NULL;
  }
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
change_state (KmsUriEndpointState state)
{
  GST_DEBUG ("Setting recorder to state %s", state2string (state));
  g_object_set (G_OBJECT (recorder), "state", state, NULL);
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
    case GST_MESSAGE_WARNING:{
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, "warning");
      if (expected_warnings)
        GST_INFO ("Do not worry. Warning expected");
      else
        fail ("Warnings not expected");
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

static void
transite (gpointer loop)
{
  const struct state_controller *transitions = get_transtions ();

  if (state < number_of_transitions) {
    change_state (transitions[state].state);
  } else {
    GST_DEBUG ("All transitions done. Finishing recorder test suite");
    g_main_loop_quit (loop);
  }
}

static gboolean
transite_cb (gpointer loop)
{
  state++;
  transite (loop);
  return FALSE;
}

static void
state_changed_cb (GstElement * recorder, KmsUriEndpointState newState,
    gpointer loop)
{
  const struct state_controller *transitions = get_transtions ();
  guint seconds = transitions[state].seconds;

  GST_DEBUG ("State changed %s. Time %d seconds.", state2string (newState),
      seconds);
  g_timeout_add (seconds * 1000, transite_cb, loop);
}

GST_START_TEST (check_states_pipeline)
{
  GstElement *pipeline, *videotestsrc, *vencoder, *aencoder, *audiotestsrc,
      *timeoverlay;
  guint bus_watch_id;
  GstBus *bus;

  GMainLoop *loop = g_main_loop_new (NULL, FALSE);

  number_of_transitions = 6;
  expected_warnings = FALSE;
  test_number = 0;
  state = 0;

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("recorderendpoint0-test");
  videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  aencoder = gst_element_factory_make ("vorbisenc", NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);

  g_object_set (G_OBJECT (recorder), "uri",
      "file:///tmp/state_recorder.webm", NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, videotestsrc, vencoder,
      aencoder, recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);
  gst_element_link (audiotestsrc, aencoder);

  gst_element_link_pads (vencoder, NULL, recorder, "video_sink");
  gst_element_link_pads (aencoder, NULL, recorder, "audio_sink");

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb),
      loop);

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, NULL);
  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "wave", 8, NULL);
  g_object_set (G_OBJECT (timeoverlay), "font-desc", "Sans 28", NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  transite (loop);

  g_main_loop_run (loop);
  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipe released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (warning_pipeline)
{
  GstElement *pipeline, *videotestsrc, *vencoder, *aencoder, *audiotestsrc,
      *timeoverlay;
  guint bus_watch_id;
  GstBus *bus;

  GMainLoop *loop = g_main_loop_new (NULL, FALSE);

  number_of_transitions = 3;
  expected_warnings = TRUE;
  test_number = 1;
  state = 0;

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("recorderendpoint0-test");
  videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  aencoder = gst_element_factory_make ("vorbisenc", NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);

  g_object_set (G_OBJECT (recorder), "uri",
      "file:///tmp/warning_pipeline.webm", NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, videotestsrc, vencoder,
      aencoder, recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);
  gst_element_link (audiotestsrc, aencoder);

  gst_element_link_pads (vencoder, NULL, recorder, "video_sink");
  gst_element_link_pads (aencoder, NULL, recorder, "audio_sink");

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb),
      loop);

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, NULL);
  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "wave", 8, NULL);
  g_object_set (G_OBJECT (timeoverlay), "font-desc", "Sans 28", NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  transite (loop);

  g_main_loop_run (loop);
  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipe released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  GST_DEBUG ("Test finished exiting main loop");
  g_main_loop_quit (loop);
  return FALSE;
}

static void
state_changed_cb2 (GstElement * recorder, KmsUriEndpointState newState,
    gpointer loop)
{
  GST_DEBUG ("State changed %s.", state2string (newState));

  if (newState == KMS_URI_ENDPOINT_STATE_STOP)
    g_idle_add (quit_main_loop_idle, loop);
}

GST_START_TEST (finite_video_test)
{
  GstElement *pipeline, *videotestsrc, *vencoder, *aencoder, *audiotestsrc,
      *timeoverlay;
  guint bus_watch_id;
  GstBus *bus;

  GMainLoop *loop = g_main_loop_new (NULL, FALSE);

  expected_warnings = FALSE;

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("recorderendpoint0-test");
  videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  aencoder = gst_element_factory_make ("vorbisenc", NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);

  g_object_set (G_OBJECT (recorder), "uri",
      "file:///tmp/finite_video_test.webm", NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, videotestsrc, vencoder,
      aencoder, recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);
  gst_element_link (audiotestsrc, aencoder);

  gst_element_link_pads (vencoder, NULL, recorder, "video_sink");
  gst_element_link_pads (aencoder, NULL, recorder, "audio_sink");

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb2),
      loop);

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, "num-buffers", 50, NULL);
  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "wave", 8, "num-buffers", 50, NULL);
  g_object_set (G_OBJECT (timeoverlay), "font-desc", "Sans 28", NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  g_object_set (G_OBJECT (recorder), "state",
      KMS_URI_ENDPOINT_STATE_START, NULL);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_main_loop_run (loop);
  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipe released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST static gboolean
stop_recorder (gpointer data)
{
  GST_DEBUG ("Setting recorder to STOP");

  g_object_set (G_OBJECT (recorder), "state", KMS_URI_ENDPOINT_STATE_STOP,
      NULL);
  return FALSE;
}

static void
state_changed_cb3 (GstElement * recorder, KmsUriEndpointState newState,
    gpointer loop)
{
  GST_DEBUG ("State changed %s.", state2string (newState));

  if (newState == KMS_URI_ENDPOINT_STATE_START)
    g_timeout_add (3000, stop_recorder, NULL);
  else if (newState == KMS_URI_ENDPOINT_STATE_STOP)
    g_idle_add (quit_main_loop_idle, loop);
}

GST_START_TEST (check_video_only)
{
  GstElement *pipeline, *videotestsrc, *vencoder, *timeoverlay;
  guint bus_watch_id;
  GstBus *bus;

  GMainLoop *loop = g_main_loop_new (NULL, FALSE);

  expected_warnings = FALSE;

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("recorderendpoint0-test");
  videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);

  g_object_set (G_OBJECT (recorder), "uri",
      "file:///tmp/check_video_only.webm", NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, vencoder,
      recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);

  gst_element_link_pads (vencoder, NULL, recorder, "video_sink");

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb3),
      loop);

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, "num-buffers", 50, NULL);

  g_object_set (G_OBJECT (timeoverlay), "font-desc", "Sans 28", NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  g_object_set (G_OBJECT (recorder), "state",
      KMS_URI_ENDPOINT_STATE_START, NULL);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_main_loop_run (loop);
  GST_DEBUG ("Stop executed");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipe released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);
}

GST_END_TEST
/******************************/
/* RecorderEndpoint test suit */
/******************************/
static Suite *
recorderendpoint_suite (void)
{
  Suite *s = suite_create ("recorderendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, check_video_only);
  tcase_add_test (tc_chain, check_states_pipeline);
  tcase_add_test (tc_chain, warning_pipeline);
  tcase_add_test (tc_chain, finite_video_test);

  return s;
}

GST_CHECK_MAIN (recorderendpoint);
