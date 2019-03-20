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
#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>
#include <valgrind/valgrind.h>

#include <commons/kmsrecordingprofile.h>
#include <commons/kmsuriendpointstate.h>

#define SINK_VIDEO_STREAM "sink_video_default"
#define SINK_AUDIO_STREAM "sink_audio_default"

#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

gboolean set_state_start (gpointer *);
gboolean set_state_pause (gpointer *);
gboolean set_state_stop (gpointer *);

static GstElement *recorder = NULL;
static guint number_of_transitions;
static gboolean expected_warnings;
static guint test_number;
static guint state;

typedef struct _RequestPadData
{
  gint n;
  gint count;
  gchar **pads;
} RequestPadData;

struct state_controller
{
  KmsUriEndpointState state;
  guint seconds;
};

static const struct state_controller trasnsitions0[] = {
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_PAUSE, 1},
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_PAUSE, 1},
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_STOP, 1}
};

static const struct state_controller trasnsitions1[] = {
  {KMS_URI_ENDPOINT_STATE_START, 2},
  {KMS_URI_ENDPOINT_STATE_PAUSE, 1},
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
  GstElement *testsrc;
  GstElement *testsink;

  GST_DEBUG ("Setting recorder to state %s", state2string (state));
  g_object_set (G_OBJECT (recorder), "state", state, NULL);

  /* Add more element to the pipeline to check that this does not affect
     to the timestamps */
  testsrc = gst_element_factory_make ("videotestsrc", NULL);
  testsink = gst_element_factory_make ("fakesink", NULL);

  g_object_set (testsink, "async", FALSE, "sync", FALSE, NULL);
  g_object_set (testsrc, "is-live", TRUE, NULL);

  GST_DEBUG_OBJECT (recorder, "Adding more elements");
  gst_bin_add_many (GST_BIN (GST_OBJECT_PARENT (recorder)), testsrc, testsink,
      NULL);
  gst_element_link (testsrc, testsink);
  gst_element_sync_state_with_parent (testsink);
  gst_element_sync_state_with_parent (testsrc);
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

      GST_ERROR ("%s", err_str);

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
  if (RUNNING_ON_VALGRIND) {
    g_timeout_add (seconds * 10000, transite_cb, loop);
  } else {
    g_timeout_add (seconds * 1000, transite_cb, loop);
  }
}

static void
remove_on_unlinked (GstPad * pad, GstPad * peer, gpointer data)
{
  GstElement *parent = gst_pad_get_parent_element (pad);

  if (parent != NULL) {
    gst_element_release_request_pad (parent, pad);
    g_object_unref (parent);
  }
}

static void
connect_pads_and_remove_on_unlinked (GstElement * agnosticbin,
    GstElement * elem, const gchar * sink_name)
{
  GstPad *src;

  src = gst_element_get_request_pad (agnosticbin, "src_%u");
  g_assert (src != NULL);
  g_signal_connect (src, "unlinked", G_CALLBACK (remove_on_unlinked), NULL);
  gst_element_link_pads (agnosticbin, GST_OBJECT_NAME (src), elem, sink_name);
  g_object_unref (src);
}

typedef struct _KmsConnectData
{
  GstElement *src;
  const gchar *pad_name;
  gulong id;
} KmsConnectData;

static void
connect_sink (GstElement * element, GstPad * pad, gpointer user_data)
{
  KmsConnectData *data = user_data;

  GST_DEBUG_OBJECT (pad, "New pad %" GST_PTR_FORMAT, element);

  if (g_strcmp0 (GST_OBJECT_NAME (pad), data->pad_name)) {
    return;
  }

  connect_pads_and_remove_on_unlinked (data->src, element, data->pad_name);

  GST_INFO_OBJECT (pad, "Linking %s", data->pad_name);
}

static void
kms_connect_data_destroy (gpointer data)
{
  g_slice_free (KmsConnectData, data);
}

static void
connect_sink_async (GstElement * recorder, GstElement * src,
    const gchar * pad_name)
{
  KmsConnectData *data = g_slice_new (KmsConnectData);

  data->src = src;
  data->pad_name = pad_name;

  data->id =
      g_signal_connect_data (recorder, "pad-added",
      G_CALLBACK (connect_sink), data,
      (GClosureNotify) kms_connect_data_destroy, 0);
}

static void
link_to_recorder (GstElement * recorder, GstElement * src, GstElement * pipe,
    const gchar * pad_name)
{
  GstPad *sink;
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);

  gst_bin_add (GST_BIN (pipe), agnosticbin);
  gst_element_link (src, agnosticbin);
  gst_element_sync_state_with_parent (agnosticbin);

  connect_sink_async (recorder, agnosticbin, pad_name);

  sink = gst_element_get_static_pad (recorder, pad_name);

  if (sink != NULL) {
    connect_pads_and_remove_on_unlinked (agnosticbin, recorder, pad_name);
    g_object_unref (sink);
  }
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
  fail_unless (videotestsrc != NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  fail_unless (vencoder != NULL);
  aencoder = gst_element_factory_make ("vorbisenc", NULL);
  fail_unless (aencoder != NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  fail_unless (timeoverlay != NULL);
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  fail_unless (audiotestsrc != NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);
  fail_unless (recorder != NULL);

  g_object_set (G_OBJECT (recorder), "uri", "file:///tmp/state_recorder.webm",
      "profile", KMS_RECORDING_PROFILE_WEBM, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, videotestsrc, vencoder,
      aencoder, recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);
  gst_element_link (audiotestsrc, aencoder);

  link_to_recorder (recorder, vencoder, pipeline, SINK_VIDEO_STREAM);
  link_to_recorder (recorder, aencoder, pipeline, SINK_AUDIO_STREAM);

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb),
      loop);

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, NULL);
  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "wave", 8, NULL);
  g_object_set (G_OBJECT (timeoverlay), "font-desc", "Sans 28", NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  transite (loop);

  g_main_loop_run (loop);
  GST_DEBUG ("Stop executed");

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
  fail_unless (videotestsrc != NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  fail_unless (vencoder != NULL);
  aencoder = gst_element_factory_make ("vorbisenc", NULL);
  fail_unless (aencoder != NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  fail_unless (timeoverlay != NULL);
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  fail_unless (audiotestsrc != NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);
  fail_unless (recorder != NULL);

  g_object_set (G_OBJECT (recorder), "uri", "file:///tmp/warning_pipeline.webm",
      "profile", KMS_RECORDING_PROFILE_WEBM, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, videotestsrc, vencoder,
      aencoder, recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);
  gst_element_link (audiotestsrc, aencoder);

  link_to_recorder (recorder, vencoder, pipeline, SINK_VIDEO_STREAM);
  link_to_recorder (recorder, aencoder, pipeline, SINK_AUDIO_STREAM);

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

static gboolean
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

  if (newState == KMS_URI_ENDPOINT_STATE_START) {
    if (RUNNING_ON_VALGRIND) {
      g_timeout_add (15000, stop_recorder, NULL);
    } else {
      g_timeout_add (3000, stop_recorder, NULL);
    }
  } else if (newState == KMS_URI_ENDPOINT_STATE_STOP) {
    g_idle_add (quit_main_loop_idle, loop);
  }
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
  fail_unless (videotestsrc != NULL);
  vencoder = gst_element_factory_make ("vp8enc", NULL);
  fail_unless (vencoder != NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  fail_unless (timeoverlay != NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);
  fail_unless (recorder != NULL);

  g_object_set (G_OBJECT (recorder), "uri", "file:///tmp/check_video_only.webm",
      "profile", KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, vencoder,
      recorder, timeoverlay, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, vencoder);

  link_to_recorder (recorder, vencoder, pipeline, SINK_VIDEO_STREAM);

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb3),
      loop);

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, NULL);

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

GST_END_TEST;

GST_START_TEST (check_audio_only)
{
  GstElement *pipeline, *audiotestsrc, *encoder;
  guint bus_watch_id;
  GstBus *bus;

  GMainLoop *loop = g_main_loop_new (NULL, FALSE);

  expected_warnings = FALSE;

  /* Create gstreamer elements */
  pipeline = gst_pipeline_new ("recorderendpoint0-test");
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  encoder = gst_element_factory_make ("vorbisenc", NULL);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);

  g_object_set (G_OBJECT (recorder), "uri", "file:///tmp/check_audio_only.webm",
      "profile", KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY, NULL);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, encoder, recorder, NULL);
  gst_element_link (audiotestsrc, encoder);

  link_to_recorder (recorder, encoder, pipeline, SINK_AUDIO_STREAM);

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_cb3),
      loop);

  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      NULL);

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
check_support_for_ksr ()
{
  GstPlugin *plugin = NULL;
  gboolean supported;

  plugin = gst_plugin_load_by_name ("kmsrecorder");

  supported = plugin != NULL;

  g_clear_object (&plugin);

  return supported;
}

static void
state_changed_ksr (GstElement * recorder, KmsUriEndpointState newState,
    gpointer loop)
{
  GST_DEBUG ("State changed %s.", state2string (newState));

  if (newState == KMS_URI_ENDPOINT_STATE_STOP) {
    g_idle_add (quit_main_loop_idle, loop);
  }
}

static gboolean
is_pad_requested (GstPad * pad, gchar ** pads, gint n)
{
  gboolean found = FALSE;
  gchar *padname;
  gint i;

  padname = gst_pad_get_name (pad);

  for (i = 0; i < n && !found; i++) {
    found = g_strcmp0 (padname, pads[i]) == 0;
  }

  g_free (padname);

  return found;
}

static void
sink_pad_added (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  RequestPadData *data = (RequestPadData *) user_data;

  GST_INFO_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);

  if (!is_pad_requested (new_pad, data->pads, data->n)) {
    return;
  }

  if (g_atomic_int_dec_and_test (&data->count)) {
    GST_DEBUG_OBJECT (element, "All sink pads created");
    g_idle_add (stop_recorder, NULL);
  }
}

GST_START_TEST (check_ksm_sink_request)
{
  GstElement *pipeline;
  guint bus_watch_id;
  RequestPadData data;
  GstBus *bus;
  GMainLoop *loop = g_main_loop_new (NULL, FALSE);
  guint i;

  data.count = data.n = 4;
  data.pads = g_slice_alloc0 (sizeof (guint8) * data.n);

  pipeline = gst_pipeline_new (__FUNCTION__);
  recorder = gst_element_factory_make ("recorderendpoint", NULL);

  g_object_set (G_OBJECT (recorder), "uri", "file:///tmp/output.ksr",
      "profile", KMS_RECORDING_PROFILE_KSR, NULL);

  g_signal_connect (recorder, "state-changed", G_CALLBACK (state_changed_ksr),
      loop);
  g_signal_connect (recorder, "pad-added", G_CALLBACK (sink_pad_added), &data);

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  gst_bin_add (GST_BIN (pipeline), recorder);

  /* request src pad using action */
  for (i = 0; i < data.n; i++) {
    gchar *id = g_strdup_printf ("tag_%u", i);

    g_signal_emit_by_name (recorder, "request-new-pad",
        KMS_ELEMENT_PAD_TYPE_VIDEO, id, GST_PAD_SINK, &data.pads[i]);
    g_free (id);
  }

  g_object_set (G_OBJECT (recorder), "state", KMS_URI_ENDPOINT_STATE_START,
      NULL);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  for (i = 0; i < data.n; i++) {
    g_free (data.pads[i]);
  }

  g_slice_free1 (sizeof (guint8) * data.n, data.pads);

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

/* Enable test when recorder is able to emit dropable buffers for the muxer */
  tcase_add_test (tc_chain, check_video_only);
  tcase_add_test (tc_chain, check_audio_only);
  tcase_add_test (tc_chain, check_states_pipeline);
  tcase_add_test (tc_chain, warning_pipeline);

  if (check_support_for_ksr ()) {
    tcase_add_test (tc_chain, check_ksm_sink_request);
  } else {
    GST_WARNING ("No ksr profile supported. Test skipped");
  }

  return s;
}

GST_CHECK_MAIN (recorderendpoint);
