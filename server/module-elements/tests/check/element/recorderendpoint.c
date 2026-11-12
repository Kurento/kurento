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
#include <glib/gstdio.h>
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

  src = gst_element_request_pad_simple (agnosticbin, "src_%u");
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

/*****************************************************************************/
/* Codec-aware recording (video-codec property)                              */
/*****************************************************************************/

#define CODEC_TEST_SRC_CAPS \
    "videotestsrc is-live=true pattern=18 ! " \
    "video/x-raw,format=I420,width=320,height=240,framerate=15/1"
#define CODEC_TEST_SRC_RAW CODEC_TEST_SRC_CAPS " ! queue"
#define CODEC_TEST_SRC_H264 \
    CODEC_TEST_SRC_CAPS " ! x264enc tune=zerolatency key-int-max=15 ! " \
    "h264parse ! video/x-h264,stream-format=avc,alignment=au ! queue"
#define CODEC_TEST_SRC_H265 \
    CODEC_TEST_SRC_CAPS " ! x265enc tune=zerolatency key-int-max=15 ! " \
    "h265parse ! video/x-h265,stream-format=hvc1,alignment=au ! queue"
#define CODEC_TEST_SRC_H265_BYTESTREAM \
    CODEC_TEST_SRC_CAPS " ! x265enc tune=zerolatency key-int-max=15 ! " \
    "h265parse ! video/x-h265,stream-format=byte-stream,alignment=au ! queue"
#define CODEC_TEST_SRC_VP8 \
    CODEC_TEST_SRC_CAPS " ! vp8enc deadline=1 keyframe-max-dist=15"

typedef struct _CodecRecording
{
  GMainLoop *loop;
  GstElement *pipeline;
  GstElement *recorder;
  GstElement *agnosticbin;
  gboolean link_late;
  guint record_ms;
  gboolean transcoding_signaled;
  gboolean error;
} CodecRecording;

static void
codec_media_transcoding_cb (GstElement * agnosticbin, gboolean is_transcoding,
    gint type, gpointer user_data)
{
  CodecRecording *rec = user_data;

  GST_INFO ("media-transcoding: %d (type %d)", is_transcoding, type);

  if (is_transcoding) {
    rec->transcoding_signaled = TRUE;
  }
}

static gboolean
codec_bus_cb (GstBus * bus, GstMessage * msg, gpointer user_data)
{
  CodecRecording *rec = user_data;

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {
    GError *err = NULL;
    gchar *dbg = NULL;

    gst_message_parse_error (msg, &err, &dbg);
    GST_ERROR ("Pipeline error: %s (%s)", err->message, dbg);
    g_error_free (err);
    g_free (dbg);

    rec->error = TRUE;
    g_main_loop_quit (rec->loop);
  }

  return TRUE;
}

static void
codec_link_agnosticbin_to_recorder (CodecRecording * rec)
{
  GstPad *sink;

  connect_sink_async (rec->recorder, rec->agnosticbin, SINK_VIDEO_STREAM);

  sink = gst_element_get_static_pad (rec->recorder, SINK_VIDEO_STREAM);
  if (sink != NULL) {
    connect_pads_and_remove_on_unlinked (rec->agnosticbin, rec->recorder,
        SINK_VIDEO_STREAM);
    g_object_unref (sink);
  }
}

static gboolean
codec_late_link_cb (gpointer data)
{
  codec_link_agnosticbin_to_recorder ((CodecRecording *) data);
  return FALSE;
}

static gboolean
codec_stop_cb (gpointer data)
{
  CodecRecording *rec = data;

  g_object_set (rec->recorder, "state", KMS_URI_ENDPOINT_STATE_STOP, NULL);
  return FALSE;
}

static void
codec_state_changed_cb (GstElement * recorder, KmsUriEndpointState state,
    gpointer user_data)
{
  CodecRecording *rec = user_data;

  if (state == KMS_URI_ENDPOINT_STATE_START) {
    g_timeout_add (rec->record_ms, codec_stop_cb, rec);
  } else if (state == KMS_URI_ENDPOINT_STATE_STOP) {
    g_idle_add (quit_main_loop_idle, rec->loop);
  }
}

/* TRUE if there is any encoder element inside the agnosticbin */
static gboolean
codec_has_encoder (GstElement * agnosticbin)
{
  GstIterator *it = gst_bin_iterate_recurse (GST_BIN (agnosticbin));
  GValue item = G_VALUE_INIT;
  gboolean found = FALSE, done = FALSE;

  while (!done) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:{
        GstElement *elem = g_value_get_object (&item);
        GstElementFactory *factory = gst_element_get_factory (elem);

        if (factory != NULL) {
          const gchar *klass = gst_element_factory_get_klass (factory);

          if (g_strrstr (klass, "Encoder") != NULL) {
            GST_INFO ("Encoder found in agnosticbin: %s",
                GST_OBJECT_NAME (elem));
            found = TRUE;
          }
        }
        g_value_reset (&item);
        break;
      }
      case GST_ITERATOR_RESYNC:
        found = FALSE;
        gst_iterator_resync (it);
        break;
      default:
        done = TRUE;
        break;
    }
  }

  g_value_unset (&item);
  gst_iterator_free (it);

  return found;
}

/* Records ~record_ms of the source into uri. Returns TRUE on success. */
static gboolean
codec_record (const gchar * src_desc, KmsRecordingProfile profile,
    KmsRecordingVideoCodec codec, const gchar * path, gboolean link_late,
    gboolean * transcoding, gboolean * encoder_in_agnosticbin)
{
  CodecRecording rec = { 0, };
  GstElement *src;
  GstBus *bus;
  GError *err = NULL;
  gchar *uri;
  guint watch_id;

  rec.loop = g_main_loop_new (NULL, FALSE);
  rec.link_late = link_late;
  rec.record_ms = RUNNING_ON_VALGRIND ? 15000 : 4000;

  rec.pipeline = gst_pipeline_new ("recorderendpoint-codec-test");
  src = gst_parse_bin_from_description (src_desc, TRUE, &err);
  fail_unless (src != NULL, "Cannot create source: %s",
      err ? err->message : "?");
  rec.agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  fail_unless (rec.agnosticbin != NULL);
  rec.recorder = gst_element_factory_make ("recorderendpoint", NULL);
  fail_unless (rec.recorder != NULL);

  g_unlink (path);
  uri = g_strdup_printf ("file://%s", path);
  g_object_set (rec.recorder, "uri", uri, "video-codec", codec, "profile",
      profile, NULL);
  g_free (uri);

  g_signal_connect (rec.agnosticbin, "media-transcoding",
      G_CALLBACK (codec_media_transcoding_cb), &rec);
  g_signal_connect (rec.recorder, "state-changed",
      G_CALLBACK (codec_state_changed_cb), &rec);

  bus = gst_pipeline_get_bus (GST_PIPELINE (rec.pipeline));
  watch_id = gst_bus_add_watch (bus, codec_bus_cb, &rec);
  g_object_unref (bus);

  gst_bin_add_many (GST_BIN (rec.pipeline), src, rec.agnosticbin,
      rec.recorder, NULL);
  fail_unless (gst_element_link (src, rec.agnosticbin));

  if (link_late) {
    /* The source is already flowing when the recorder gets connected */
    g_timeout_add (1500, codec_late_link_cb, &rec);
  } else {
    codec_link_agnosticbin_to_recorder (&rec);
  }

  g_object_set (rec.recorder, "state", KMS_URI_ENDPOINT_STATE_START, NULL);
  gst_element_set_state (rec.pipeline, GST_STATE_PLAYING);

  g_main_loop_run (rec.loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (rec.pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "codec_recording_end");

  *transcoding = rec.transcoding_signaled;
  *encoder_in_agnosticbin = codec_has_encoder (rec.agnosticbin);

  gst_element_set_state (rec.pipeline, GST_STATE_NULL);
  gst_object_unref (rec.pipeline);
  g_source_remove (watch_id);
  g_main_loop_unref (rec.loop);

  return !rec.error;
}

static void
codec_count_probe_free (gpointer data)
{
  g_slice_free (guint, data);
}

static GstPadProbeReturn
codec_count_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  (*(guint *) data)++;
  return GST_PAD_PROBE_OK;
}

/* Demuxes the recorded file and returns the caps of the video track
 * (caller owns the result) plus the number of video frames. */
static GstStructure *
codec_inspect_file (const gchar * path, const gchar * demuxer, guint * frames)
{
  GstElement *pipeline, *demux, *sink;
  GstPad *pad;
  GstStructure *st = NULL;
  GstCaps *caps;
  GstMessage *msg;
  GstBus *bus;
  gchar *desc;
  guint *count = g_slice_new0 (guint);

  desc = g_strdup_printf ("filesrc location=%s ! %s name=d "
      "d.video_0 ! fakesink name=sink sync=false", path, demuxer);
  pipeline = gst_parse_launch (desc, NULL);
  g_free (desc);
  fail_unless (pipeline != NULL);

  demux = gst_bin_get_by_name (GST_BIN (pipeline), "d");
  sink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");
  pad = gst_element_get_static_pad (sink, "sink");
  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BUFFER, codec_count_probe, count,
      codec_count_probe_free);

  bus = gst_element_get_bus (pipeline);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  msg = gst_bus_timed_pop_filtered (bus, 20 * GST_SECOND,
      GST_MESSAGE_EOS | GST_MESSAGE_ERROR);
  fail_unless (msg != NULL, "Timeout reading %s", path);
  fail_unless (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS,
      "Error reading %s", path);
  gst_message_unref (msg);

  caps = gst_pad_get_current_caps (pad);
  if (caps != NULL) {
    st = gst_structure_copy (gst_caps_get_structure (caps, 0));
    gst_caps_unref (caps);
  }
  *frames = *count;

  gst_element_set_state (pipeline, GST_STATE_NULL);
  /* the probe data is released with the pad */
  gst_object_unref (pad);
  gst_object_unref (sink);
  gst_object_unref (demux);
  gst_object_unref (bus);
  gst_object_unref (pipeline);

  return st;
}

static void
codec_check_recording (const gchar * src_desc, KmsRecordingProfile profile,
    KmsRecordingVideoCodec codec, const gchar * path, const gchar * demuxer,
    gboolean link_late, gboolean expect_transcoding,
    const gchar * expected_media_type, const gchar * expected_stream_format)
{
  gboolean transcoding = FALSE, encoder = FALSE;
  GstStructure *st;
  guint frames = 0;

  fail_unless (codec_record (src_desc, profile, codec, path, link_late,
          &transcoding, &encoder), "Recording failed");

  fail_unless_equals_int (transcoding, expect_transcoding);
  fail_unless_equals_int (encoder, expect_transcoding);

  st = codec_inspect_file (path, demuxer, &frames);
  fail_unless (st != NULL, "No caps in recorded video track");
  GST_INFO ("Recorded video track: %" GST_PTR_FORMAT, st);

  fail_unless_equals_string (gst_structure_get_name (st), expected_media_type);
  if (expected_stream_format != NULL) {
    fail_unless_equals_string (gst_structure_get_string (st, "stream-format"),
        expected_stream_format);
  }
  fail_unless (frames >= 15, "Too few frames recorded: %u", frames);

  if (g_strcmp0 (expected_media_type, "video/x-h265") == 0) {
    /* main profile (4:2:0 8 bit), not main-444 */
    const gchar *profile_name = gst_structure_get_string (st, "profile");

    fail_unless (profile_name == NULL ||
        g_strcmp0 (profile_name, "main") == 0,
        "Unexpected H265 profile: %s", profile_name);
  }

  gst_structure_free (st);
}

/* (i) H265 source -> MP4 H265 without transcoding */
GST_START_TEST (check_h265_passthrough_mp4)
{
  codec_check_recording (CODEC_TEST_SRC_H265, KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_h265_passthrough.mp4",
      "qtdemux", FALSE, FALSE, "video/x-h265", "hvc1");
}

GST_END_TEST;

/* (i-bis) same, with the recorder connected while the source is flowing */
GST_START_TEST (check_h265_passthrough_mp4_late_link)
{
  codec_check_recording (CODEC_TEST_SRC_H265, KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_h265_passthrough_late.mp4",
      "qtdemux", TRUE, FALSE, "video/x-h265", "hvc1");
}

GST_END_TEST;

/* (ii) H265 source -> MKV H265 without transcoding */
GST_START_TEST (check_h265_passthrough_mkv)
{
  codec_check_recording (CODEC_TEST_SRC_H265, KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_h265_passthrough.mkv",
      "matroskademux", FALSE, FALSE, "video/x-h265", "hvc1");
}

GST_END_TEST;

GST_START_TEST (check_h265_passthrough_mkv_late_link)
{
  codec_check_recording (CODEC_TEST_SRC_H265, KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_h265_passthrough_late.mkv",
      "matroskademux", TRUE, FALSE, "video/x-h265", "hvc1");
}

GST_END_TEST;

/* H265 sources that are not hvc1 (TS, filters, other pipelines) are recorded
 * without transcoding: the recorder repackages them with h265parse */
GST_START_TEST (check_h265_bytestream_passthrough_mp4)
{
  codec_check_recording (CODEC_TEST_SRC_H265_BYTESTREAM,
      KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY, KMS_RECORDING_VIDEO_CODEC_H265,
      "/tmp/check_h265_bytestream_passthrough.mp4", "qtdemux", FALSE, FALSE,
      "video/x-h265", "hvc1");
}

GST_END_TEST;

GST_START_TEST (check_h265_bytestream_passthrough_mkv)
{
  codec_check_recording (CODEC_TEST_SRC_H265_BYTESTREAM,
      KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY, KMS_RECORDING_VIDEO_CODEC_H265,
      "/tmp/check_h265_bytestream_passthrough.mkv", "matroskademux", FALSE,
      FALSE, "video/x-h265", "hvc1");
}

GST_END_TEST;

GST_START_TEST (check_h265_bytestream_passthrough_mp4_late_link)
{
  codec_check_recording (CODEC_TEST_SRC_H265_BYTESTREAM,
      KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY, KMS_RECORDING_VIDEO_CODEC_H265,
      "/tmp/check_h265_bytestream_passthrough_late.mp4", "qtdemux", TRUE,
      FALSE, "video/x-h265", "hvc1");
}

GST_END_TEST;

/* (iii) raw source -> H265 (transcoding with x265enc, 4:2:0) */
GST_START_TEST (check_raw_to_h265_mp4)
{
  codec_check_recording (CODEC_TEST_SRC_RAW, KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_raw_to_h265.mp4",
      "qtdemux", FALSE, TRUE, "video/x-h265", "hvc1");
}

GST_END_TEST;

GST_START_TEST (check_raw_to_h265_mkv)
{
  codec_check_recording (CODEC_TEST_SRC_RAW, KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_raw_to_h265.mkv",
      "matroskademux", FALSE, TRUE, "video/x-h265", "hvc1");
}

GST_END_TEST;

/* Raw formats that make videoconvert prefer 4:4:4 must still yield main */
GST_START_TEST (check_raw_444_to_h265_mp4)
{
  codec_check_recording ("videotestsrc is-live=true pattern=18 ! "
      "video/x-raw,format=Y444,width=320,height=240,framerate=15/1 ! queue",
      KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY, KMS_RECORDING_VIDEO_CODEC_H265,
      "/tmp/check_raw_444_to_h265.mp4", "qtdemux", FALSE, TRUE,
      "video/x-h265", "hvc1");
}

GST_END_TEST;

/* (iv) VP8 source -> H265 */
GST_START_TEST (check_vp8_to_h265_mp4)
{
  codec_check_recording (CODEC_TEST_SRC_VP8, KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_H265, "/tmp/check_vp8_to_h265.mp4",
      "qtdemux", FALSE, TRUE, "video/x-h265", "hvc1");
}

GST_END_TEST;

/* (v) no regression: defaults are unchanged (NONE) */
GST_START_TEST (check_default_mp4_is_h264)
{
  codec_check_recording (CODEC_TEST_SRC_H264, KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_NONE, "/tmp/check_default.mp4",
      "qtdemux", FALSE, FALSE, "video/x-h264", "avc");
}

GST_END_TEST;

GST_START_TEST (check_default_mp4_from_h265_transcodes_to_h264)
{
  codec_check_recording (CODEC_TEST_SRC_H265, KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_NONE, "/tmp/check_default_h265src.mp4",
      "qtdemux", FALSE, TRUE, "video/x-h264", "avc");
}

GST_END_TEST;

GST_START_TEST (check_default_webm_is_vp8)
{
  codec_check_recording (CODEC_TEST_SRC_RAW, KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_NONE, "/tmp/check_default.webm",
      "matroskademux", FALSE, TRUE, "video/x-vp8", NULL);
}

GST_END_TEST;

GST_START_TEST (check_default_mkv_is_h264)
{
  codec_check_recording (CODEC_TEST_SRC_H264, KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY,
      KMS_RECORDING_VIDEO_CODEC_NONE, "/tmp/check_default.mkv",
      "matroskademux", FALSE, FALSE, "video/x-h264", "avc");
}

GST_END_TEST;

/* (vi) container x codec matrix and property validation */
GST_START_TEST (check_video_codec_matrix)
{
  /* NONE is always valid (even for containers without codec choice) */
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_FLV, KMS_RECORDING_VIDEO_CODEC_NONE));
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_KSR, KMS_RECORDING_VIDEO_CODEC_NONE));

  /* H265 only in MP4 and MKV */
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_MP4, KMS_RECORDING_VIDEO_CODEC_H265));
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_MKV, KMS_RECORDING_VIDEO_CODEC_H265));
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY, KMS_RECORDING_VIDEO_CODEC_H265));
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_WEBM, KMS_RECORDING_VIDEO_CODEC_H265));
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_FLV, KMS_RECORDING_VIDEO_CODEC_H265));
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_KSR, KMS_RECORDING_VIDEO_CODEC_H265));
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_JPEG_VIDEO_ONLY, KMS_RECORDING_VIDEO_CODEC_H265));

  /* Other combinations */
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_MP4, KMS_RECORDING_VIDEO_CODEC_VP8));
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_WEBM, KMS_RECORDING_VIDEO_CODEC_H264));
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_MKV, KMS_RECORDING_VIDEO_CODEC_VP8));
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_WEBM, KMS_RECORDING_VIDEO_CODEC_VP9));
  fail_unless (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_MP4, KMS_RECORDING_VIDEO_CODEC_AV1));
  fail_if (kms_recording_profile_supports_video_codec
      (KMS_RECORDING_PROFILE_FLV, KMS_RECORDING_VIDEO_CODEC_H264));
}

GST_END_TEST;

static GstCaps *
codec_profile_video_caps (KmsRecordingProfile profile,
    KmsRecordingVideoCodec codec)
{
  GstEncodingContainerProfile *cprof;
  const GList *l;
  GstCaps *caps = NULL;

  cprof = kms_recording_profile_create_profile (profile, codec, FALSE, TRUE);
  if (cprof == NULL) {
    return NULL;
  }

  for (l = gst_encoding_container_profile_get_profiles (cprof); l != NULL;
      l = l->next) {
    if (GST_IS_ENCODING_VIDEO_PROFILE (l->data)) {
      caps = gst_encoding_profile_get_input_caps (l->data);
    }
  }
  gst_encoding_profile_unref (cprof);

  return caps;
}

GST_START_TEST (check_profile_caps_per_codec)
{
  GstCaps *caps, *expected;

  /* Defaults are unchanged */
  caps = codec_profile_video_caps (KMS_RECORDING_PROFILE_MP4,
      KMS_RECORDING_VIDEO_CODEC_NONE);
  expected = gst_caps_from_string ("video/x-h264, stream-format=avc, "
      "alignment=au");
  fail_unless (gst_caps_is_equal (caps, expected), "%" GST_PTR_FORMAT, caps);
  gst_caps_unref (caps);
  gst_caps_unref (expected);

  caps = codec_profile_video_caps (KMS_RECORDING_PROFILE_WEBM,
      KMS_RECORDING_VIDEO_CODEC_NONE);
  expected = gst_caps_from_string ("video/x-vp8");
  fail_unless (gst_caps_is_equal (caps, expected), "%" GST_PTR_FORMAT, caps);
  gst_caps_unref (caps);
  gst_caps_unref (expected);

  /* H265 */
  expected = gst_caps_from_string ("video/x-h265, stream-format=hvc1, "
      "alignment=au");
  caps = codec_profile_video_caps (KMS_RECORDING_PROFILE_MP4,
      KMS_RECORDING_VIDEO_CODEC_H265);
  fail_unless (gst_caps_is_equal (caps, expected), "%" GST_PTR_FORMAT, caps);
  gst_caps_unref (caps);
  caps = codec_profile_video_caps (KMS_RECORDING_PROFILE_MKV,
      KMS_RECORDING_VIDEO_CODEC_H265);
  fail_unless (gst_caps_is_equal (caps, expected), "%" GST_PTR_FORMAT, caps);
  gst_caps_unref (caps);
  gst_caps_unref (expected);

  /* Invalid combinations do not produce a profile */
  fail_unless (codec_profile_video_caps (KMS_RECORDING_PROFILE_WEBM,
          KMS_RECORDING_VIDEO_CODEC_H265) == NULL);
  fail_unless (codec_profile_video_caps (KMS_RECORDING_PROFILE_MP4,
          KMS_RECORDING_VIDEO_CODEC_VP8) == NULL);
}

GST_END_TEST;

GST_START_TEST (check_video_codec_property)
{
  GstElement *rec;
  gint codec;

  /* Default */
  rec = gst_element_factory_make ("recorderendpoint", NULL);
  fail_unless (rec != NULL);
  g_object_get (rec, "video-codec", &codec, NULL);
  fail_unless_equals_int (codec, KMS_RECORDING_VIDEO_CODEC_NONE);
  gst_object_unref (rec);

  /* Valid, codec before profile */
  rec = gst_element_factory_make ("recorderendpoint", NULL);
  g_object_set (rec, "uri", "file:///tmp/check_prop1.mp4", "video-codec",
      KMS_RECORDING_VIDEO_CODEC_H265, "profile", KMS_RECORDING_PROFILE_MP4,
      NULL);
  g_object_get (rec, "video-codec", &codec, NULL);
  fail_unless_equals_int (codec, KMS_RECORDING_VIDEO_CODEC_H265);
  gst_object_unref (rec);

  /* Valid, codec after profile */
  rec = gst_element_factory_make ("recorderendpoint", NULL);
  g_object_set (rec, "uri", "file:///tmp/check_prop2.mkv", "profile",
      KMS_RECORDING_PROFILE_MKV, "video-codec", KMS_RECORDING_VIDEO_CODEC_H265,
      NULL);
  g_object_get (rec, "video-codec", &codec, NULL);
  fail_unless_equals_int (codec, KMS_RECORDING_VIDEO_CODEC_H265);
  gst_object_unref (rec);

  /* Invalid (H265 in WebM), codec before profile: rejected, back to default */
  rec = gst_element_factory_make ("recorderendpoint", NULL);
  g_object_set (rec, "uri", "file:///tmp/check_prop3.webm", "video-codec",
      KMS_RECORDING_VIDEO_CODEC_H265, "profile", KMS_RECORDING_PROFILE_WEBM,
      NULL);
  g_object_get (rec, "video-codec", &codec, NULL);
  fail_unless_equals_int (codec, KMS_RECORDING_VIDEO_CODEC_NONE);
  gst_object_unref (rec);

  /* Invalid (VP8 in MP4), codec after profile: rejected */
  rec = gst_element_factory_make ("recorderendpoint", NULL);
  g_object_set (rec, "uri", "file:///tmp/check_prop4.mp4", "profile",
      KMS_RECORDING_PROFILE_MP4, "video-codec", KMS_RECORDING_VIDEO_CODEC_VP8,
      NULL);
  g_object_get (rec, "video-codec", &codec, NULL);
  fail_unless_equals_int (codec, KMS_RECORDING_VIDEO_CODEC_NONE);
  gst_object_unref (rec);

  /* Write once */
  rec = gst_element_factory_make ("recorderendpoint", NULL);
  g_object_set (rec, "uri", "file:///tmp/check_prop5.mkv", "video-codec",
      KMS_RECORDING_VIDEO_CODEC_H265, NULL);
  g_object_set (rec, "video-codec", KMS_RECORDING_VIDEO_CODEC_VP8, NULL);
  g_object_get (rec, "video-codec", &codec, NULL);
  fail_unless_equals_int (codec, KMS_RECORDING_VIDEO_CODEC_H265);
  gst_object_unref (rec);
}

GST_END_TEST;

static gboolean
codec_have_elements (const gchar * first, ...)
{
  va_list args;
  const gchar *name;
  gboolean ok = TRUE;

  va_start (args, first);
  for (name = first; name != NULL; name = va_arg (args, const gchar *)) {
    GstElementFactory *f = gst_element_factory_find (name);

    if (f == NULL) {
      GST_WARNING ("Element %s not available", name);
      ok = FALSE;
    } else {
      gst_object_unref (f);
    }
  }
  va_end (args);

  return ok;
}

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

  /* Codec-aware recording */
  tcase_add_test (tc_chain, check_video_codec_matrix);
  tcase_add_test (tc_chain, check_profile_caps_per_codec);
  tcase_add_test (tc_chain, check_video_codec_property);

  if (codec_have_elements ("x265enc", "h265parse", "x264enc", "vp8enc",
          "qtdemux", "matroskademux", "mp4mux", "matroskamux", NULL)) {
    tcase_add_test (tc_chain, check_h265_passthrough_mp4);
    tcase_add_test (tc_chain, check_h265_passthrough_mp4_late_link);
    tcase_add_test (tc_chain, check_h265_passthrough_mkv);
    tcase_add_test (tc_chain, check_h265_passthrough_mkv_late_link);
    tcase_add_test (tc_chain, check_h265_bytestream_passthrough_mp4);
    tcase_add_test (tc_chain, check_h265_bytestream_passthrough_mkv);
    tcase_add_test (tc_chain, check_h265_bytestream_passthrough_mp4_late_link);
    tcase_add_test (tc_chain, check_raw_to_h265_mp4);
    tcase_add_test (tc_chain, check_raw_to_h265_mkv);
    tcase_add_test (tc_chain, check_raw_444_to_h265_mp4);
    tcase_add_test (tc_chain, check_vp8_to_h265_mp4);
    tcase_add_test (tc_chain, check_default_mp4_is_h264);
    tcase_add_test (tc_chain, check_default_mp4_from_h265_transcodes_to_h264);
    tcase_add_test (tc_chain, check_default_webm_is_vp8);
    tcase_add_test (tc_chain, check_default_mkv_is_h264);
  } else {
    GST_WARNING ("Missing codecs/muxers. H265 recording tests skipped");
  }

  if (check_support_for_ksr ()) {
    tcase_add_test (tc_chain, check_ksm_sink_request);
  } else {
    GST_WARNING ("No ksr profile supported. Test skipped");
  }

  return s;
}

GST_CHECK_MAIN (recorderendpoint);
