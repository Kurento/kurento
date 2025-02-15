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
#include <gst/video/video-event.h>
#include <gst/gst.h>
#include <glib.h>

#define AGNOSTIC_KEY "agnostic"
G_DEFINE_QUARK (AGNOSTIC_KEY, agnostic_key);

#define DECODER_KEY "decoder"
G_DEFINE_QUARK (DECODER_KEY, decoder_key);

#define FAKESINK_KEY "fakesink"
G_DEFINE_QUARK (FAKESINK_KEY, fakesink_key);

#define VALVE_KEY "valve"
G_DEFINE_QUARK (VALVE_KEY, valve_key);

#define COUNT_KEY "count"
G_DEFINE_QUARK (COUNT_KEY, count_key);

/*
 * By now we only enable one output, otherwise the test will probably fail
 * once the bug is solved this value should be incremented
 */
#define N_ITERS 200

typedef struct _ElementsData
{
  GstElement *agnosticbin;
  GstElement *fakesink;
} ElementsData;

static GMainLoop *loop;

static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  g_main_loop_quit (loop);
  return FALSE;
}

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      gchar *error_file = g_strdup_printf ("error-%s", GST_OBJECT_NAME (pipe));

      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, error_file);
      g_free (error_file);
      fail ("Error received on bus");
      break;
    }
    case GST_MESSAGE_WARNING:{
      gchar *warn_file = g_strdup_printf ("warning-%s", GST_OBJECT_NAME (pipe));

      GST_WARNING ("Warning: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, warn_file);
      g_free (warn_file);
      break;
    }
    case GST_MESSAGE_EOS:{
      quit_main_loop_idle (loop);
      break;
    }
    default:
      break;
  }
}

static void
fakesink_hand_off (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  static int count = 0;
  GMainLoop *loop = (GMainLoop *) data;

  if (count++ > 40) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, loop);
  }
}

static gboolean
reconnect_elements (gpointer data)
{
  ElementsData *elements = data;
  GstPad *pad = gst_element_request_pad_simple (elements->agnosticbin, "src_%u");

  gst_element_link_pads (elements->agnosticbin, GST_OBJECT_NAME (pad),
      elements->fakesink, NULL);
  g_object_unref (pad);

  return FALSE;
}

static void
free_elements (gpointer data)
{
  ElementsData *elements = data;

  g_object_unref (elements->agnosticbin);
  g_object_unref (elements->fakesink);

  g_slice_free (ElementsData, elements);
}

static void
fakesink_hand_off_simple (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  static int count = 0;
  GMainLoop *loop = (GMainLoop *) data;

  count++;

  if (count == 40) {
    GstPad *peer = gst_pad_get_peer (pad);
    ElementsData *elements = g_slice_new (ElementsData);

    elements->agnosticbin = gst_pad_get_parent_element (peer);
    elements->fakesink = g_object_ref (fakesink);

    gst_pad_unlink (peer, pad);
    g_object_unref (peer);

    g_idle_add_full (G_PRIORITY_DEFAULT, reconnect_elements, elements,
        free_elements);
  } else if (count > 100) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, loop);
  }
}

static gboolean
link_again (gpointer data)
{
  GstElement *decoder = (GstElement *) data;
  GstElement *agnostic =
      g_object_get_qdata (G_OBJECT (data), agnostic_key_quark ());
  GstElement *fakesink =
      g_object_get_qdata (G_OBJECT (decoder), fakesink_key_quark ());

  GST_DEBUG ("Linking again %" GST_PTR_FORMAT ", %" GST_PTR_FORMAT, agnostic,
      decoder);

  g_object_set (G_OBJECT (fakesink), "signal-handoffs", TRUE, NULL);
  gst_element_link (agnostic, decoder);

  return FALSE;
}

static gboolean
idle_unlink (gpointer data)
{
  GstPad *sink, *src;

  GstElement *decoder = (GstElement *) data;
  GstElement *agnostic =
      g_object_get_qdata (G_OBJECT (decoder), agnostic_key_quark ());

  sink = gst_element_get_static_pad (decoder, "sink");
  src = gst_pad_get_peer (sink);

  gst_pad_unlink (src, sink);

  gst_element_release_request_pad (agnostic, src);
  g_object_unref (src);
  g_object_unref (sink);

  g_timeout_add (200, link_again, decoder);

  return FALSE;
}

static void
fakesink_hand_off2 (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  static int count = 0;
  static int cycles = 0;
  GMainLoop *loop = (GMainLoop *) data;

  if (count++ > 10) {
    count = 0;
    if (cycles++ > 10) {
      g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
      GST_DEBUG ("Quit loop");
      g_idle_add (quit_main_loop_idle, loop);
    } else {
      GstElement *decoder =
          g_object_get_qdata (G_OBJECT (fakesink), decoder_key_quark ());

      g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
      mark_point ();
      g_idle_add (idle_unlink, decoder);
    }
  }
}

static gboolean
timeout_check (gpointer pipeline)
{
  if (GST_IS_BIN (pipeline)) {
    gchar *timeout_file =
        g_strdup_printf ("timeout-%s", GST_OBJECT_NAME (pipeline));

    GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
        GST_DEBUG_GRAPH_SHOW_ALL, timeout_file);
    g_free (timeout_file);
  }

  return FALSE;
}

static gpointer
toggle_thread (gpointer data)
{
  GstElement *pipeline = GST_ELEMENT (data);
  GstElement *valve =
      g_object_get_qdata (G_OBJECT (pipeline), valve_key_quark ());
  GstPad *sink = gst_element_get_static_pad (valve, "sink");
  gint i;

  g_usleep (800000);

  for (i = 0; i < 15; i++) {
    if (g_main_loop_is_running (loop)) {
      g_object_set (valve, "drop", i % 2, NULL);
      g_usleep (20000);
    } else {
      GST_DEBUG ("Main loop stopped");
      break;
    }
  }

  g_usleep (100000);

  g_object_set (valve, "drop", FALSE, NULL);

  g_object_unref (sink);

  GST_DEBUG ("Toggle thread finished");
  {
    GstElement *fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "fakesink");

    GST_DEBUG_OBJECT (fakesink, "Found");
    g_signal_connect (G_OBJECT (fakesink), "handoff",
        G_CALLBACK (fakesink_hand_off), loop);
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", TRUE, NULL);
    g_object_unref (fakesink);
  }

  return NULL;
}

static gboolean
link_source (gpointer data)
{
  GstElement *pipeline = data;
  GstElement *agnosticbin =
      gst_bin_get_by_name (GST_BIN (pipeline), "agnosticbin");
  GstElement *videosrc = gst_element_factory_make ("videotestsrc", NULL);

  g_object_set (G_OBJECT (videosrc), "is-live", TRUE, NULL);
  gst_bin_add_many (GST_BIN (pipeline), videosrc, NULL);
  gst_element_link (videosrc, agnosticbin);
  gst_element_sync_state_with_parent (videosrc);

  g_object_unref (agnosticbin);

  return FALSE;
}

static void
type_found (GstElement * typefind, guint prob, GstCaps * caps,
    GstElement * pipeline)
{
  GstElement *fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "fakesink");
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);

  gst_bin_add (GST_BIN (pipeline), agnosticbin);
  gst_element_sync_state_with_parent (agnosticbin);

  gst_element_link (agnosticbin, fakesink);
  gst_element_link (typefind, agnosticbin);

  g_object_unref (fakesink);
}

static void
change_input (gpointer pipeline)
{
  GstBin *pipe = GST_BIN (pipeline);
  GstElement *agnosticbin;
  GstPad *peer, *sink;
  GstElement *agnosticbin2 = gst_bin_get_by_name (pipe, "agnosticbin_2");
  GstElement *enc = gst_element_factory_make ("vp8enc", NULL);
  GstElement *fakesink = gst_bin_get_by_name (pipe, "fakesink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);
  g_object_unref (fakesink);

  gst_bin_add (pipe, enc);
  gst_element_sync_state_with_parent (enc);

  sink = gst_element_get_static_pad (agnosticbin2, "sink");
  peer = gst_pad_get_peer (sink);
  agnosticbin = gst_pad_get_parent_element (peer);
  gst_pad_unlink (peer, sink);

  GST_INFO ("Got peer: %" GST_PTR_FORMAT, peer);
  gst_element_release_request_pad (agnosticbin, peer);
  gst_element_link (enc, agnosticbin2);
  gst_element_link (agnosticbin, enc);

  g_object_unref (agnosticbin);
  g_object_unref (agnosticbin2);
  g_object_unref (sink);
  g_object_unref (peer);
}

static GstPadProbeReturn
block_agnostic_sink (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  static gboolean configuring = FALSE;

  /* HACK: Ignore caps event and stream start event that causes negotiation
   * failures.This is a workaround that should be removed
   */
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM) {
    GstEvent *event = GST_PAD_PROBE_INFO_EVENT (info);

    if (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START
        || GST_EVENT_TYPE (event) == GST_EVENT_CAPS) {
      return GST_PAD_PROBE_PASS;
    }
  }

  /* HACK: Ignore query accept caps that causes negotiation errors.
   * This is a workaround that should be removed
   */
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM) {
    GstQuery *query = GST_PAD_PROBE_INFO_QUERY (info);

    if (GST_QUERY_TYPE (query) == GST_QUERY_ACCEPT_CAPS) {
      return GST_PAD_PROBE_PASS;
    }
  }

  if (g_atomic_int_compare_and_exchange (&configuring, FALSE, TRUE)) {
    change_input (data);
    g_atomic_int_set (&configuring, FALSE);
    return GST_PAD_PROBE_REMOVE;
  }

  return GST_PAD_PROBE_PASS;
}

static gboolean
change_input_cb (gpointer pipeline)
{
  GstBin *pipe = GST_BIN (pipeline);
  GstPad *sink;
  GstElement *agnosticbin2 = gst_bin_get_by_name (pipe, "agnosticbin_2");

  sink = gst_element_get_static_pad (agnosticbin2, "sink");
  gst_pad_add_probe (sink, GST_PAD_PROBE_TYPE_BLOCK, block_agnostic_sink,
      pipeline, NULL);
  // HACK: Sending a dummy event to ensure the block probe is called
  gst_pad_send_event (sink,
      gst_event_new_custom (GST_EVENT_CUSTOM_DOWNSTREAM,
          gst_structure_new_from_string ("dummy")));

  g_object_unref (agnosticbin2);
  g_object_unref (sink);
  return FALSE;
}

static gboolean
check_pipeline_termination (gpointer data)
{
  GstElement *pipeline = GST_ELEMENT (data);
  int *count = g_object_get_qdata (G_OBJECT (pipeline), count_key_quark ());

  if (count != NULL && g_atomic_int_dec_and_test (count)) {
    GST_DEBUG ("Terminating main loop");
    quit_main_loop_idle (loop);
  }

  return FALSE;
}

static GstFlowReturn
appsink_handle_many (GstElement * appsink, gpointer data)
{
  int *count = g_object_get_qdata (G_OBJECT (appsink), count_key_quark ());
  GstSample *sample;

  if (count == NULL) {
    count = g_malloc0 (sizeof (int));
    g_object_set_qdata_full (G_OBJECT (appsink), count_key_quark (), count,
        g_free);
  }

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  gst_sample_unref (sample);

  if (g_atomic_int_add (count, 1) == 40) {
    GST_DEBUG_OBJECT (appsink, "Terminatig");
    g_idle_add (check_pipeline_termination, GST_OBJECT_PARENT (appsink));
  }

  return GST_FLOW_OK;
}

static gboolean
connect_output (gpointer pipeline)
{
  GstElement *agnosticbin =
      gst_bin_get_by_name (GST_BIN (pipeline), "agnosticbin");
  GstElement *appsink = gst_element_factory_make ("appsink", NULL);
  GstCaps *caps = gst_caps_from_string ("audio/x-vorbis");

  g_object_set (G_OBJECT (appsink), "emit-signals", TRUE, "caps", caps, "sync",
      TRUE, "async", FALSE, NULL);
  gst_caps_unref (caps);

  g_signal_connect (G_OBJECT (appsink), "new-sample",
      G_CALLBACK (appsink_handle_many), NULL);

  gst_bin_add (GST_BIN (pipeline), appsink);
  gst_element_sync_state_with_parent (appsink);

  if (!gst_element_link (agnosticbin, appsink)) {
    GST_ERROR ("Error linking elements");
  }

  g_object_unref (agnosticbin);

  return FALSE;
}

GST_START_TEST (input_reconfiguration)
{
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videosrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *agnosticbin =
      gst_element_factory_make ("agnosticbin", "agnosticbin_1");
  GstElement *agnosticbin2 =
      gst_element_factory_make ("agnosticbin", "agnosticbin_2");
  GstElement *fakesink = gst_element_factory_make ("fakesink", "fakesink");

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  int event_source_tag;

  loop = g_main_loop_new (NULL, TRUE);

  g_object_set (G_OBJECT (videosrc), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  gst_bin_add_many (GST_BIN (pipeline), videosrc, agnosticbin, agnosticbin2,
      fakesink, NULL);
  gst_element_link_many (videosrc, agnosticbin, agnosticbin2, fakesink, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (1, change_input_cb, pipeline);

  event_source_tag = g_timeout_add_seconds (6, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (pipeline);
  g_object_unref (bus);
  g_main_loop_unref (loop);
}

GST_END_TEST;

static gboolean
change_input_caps_cb (gpointer pipeline)
{
  GstBin *pipe = GST_BIN (pipeline);
  GstElement *fakesink = gst_bin_get_by_name (pipe, "fakesink");
  GstCaps *caps = gst_caps_from_string ("video/x-raw,width=321,height=241");
  GstElement *capsfilter = gst_bin_get_by_name (pipe, "input_caps");

  g_object_set (capsfilter, "caps", caps, NULL);
  gst_caps_unref (caps);
  g_object_unref (capsfilter);

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);
  g_object_unref (fakesink);

  return FALSE;
}

GST_START_TEST (input_caps_reconfiguration)
{
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! capsfilter name=input_caps caps=video/x-raw,width=800,height=600"
       "  ! agnosticbin ! video/x-vp8"
       "  ! agnosticbin ! video/x-raw"
       "  ! fakesink sync=false async=false signal-handoffs=true name=fakesink",
      NULL);

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  int event_source_tag;

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (1, change_input_caps_cb, pipeline);

  event_source_tag =g_timeout_add_seconds (6, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (pipeline);
  g_object_unref (bus);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (add_later)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videosrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *typefind = gst_element_factory_make ("typefind", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", "fakesink");
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  int event_source_tag;

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_object_set (G_OBJECT (videosrc), "is-live", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_signal_connect (G_OBJECT (typefind), "have-type", G_CALLBACK (type_found),
      pipeline);

  gst_bin_add_many (GST_BIN (pipeline), videosrc, typefind, fakesink, NULL);
  gst_element_link (videosrc, typefind);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  event_source_tag =g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (pipeline);
  g_object_unref (bus);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (delay_stream)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *agnosticbin =
      gst_element_factory_make ("agnosticbin", "agnosticbin");
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  int event_source_tag;

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "async", FALSE, "sync", TRUE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  gst_bin_add_many (GST_BIN (pipeline), agnosticbin, fakesink, NULL);
  gst_element_link (agnosticbin, fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (1, link_source, pipeline);
  event_source_tag =g_timeout_add_seconds (11, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (pipeline);
  g_object_unref (bus);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (valve_test)
{
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *valve = gst_element_factory_make ("valve", NULL);
  GstElement *decoder = gst_element_factory_make ("vp8dec", NULL);
  GstElement *fakesink2 = gst_element_factory_make ("fakesink", "fakesink");
  gboolean ret;
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GThread *thread;
  int event_source_tag;

  g_object_set_qdata (G_OBJECT (pipeline), valve_key_quark (), valve);

  loop = g_main_loop_new (NULL, TRUE);
  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set_qdata (G_OBJECT (fakesink2), decoder_key_quark (), decoder);
  g_object_set_qdata (G_OBJECT (decoder), agnostic_key_quark (), agnosticbin);

  g_object_set (G_OBJECT (fakesink2), "sync", TRUE, "async", FALSE, NULL);
  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "async", FALSE, NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin, fakesink,
      valve, decoder, fakesink2, NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, agnosticbin, fakesink, NULL);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_many (agnosticbin, valve, decoder, fakesink2, NULL);
  fail_unless (ret);
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  thread = g_thread_new ("toggle", toggle_thread, pipeline);
  g_thread_unref (thread);

  g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (pipeline);
  g_object_unref (bus);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (reconnect_test)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *decoder = gst_element_factory_make ("vp8dec", NULL);
  GstElement *fakesink2 = gst_element_factory_make ("fakesink", NULL);
  gboolean ret;
  int event_source_tag;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink2), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink2), "handoff",
      G_CALLBACK (fakesink_hand_off2), loop);
  g_object_set_qdata (G_OBJECT (fakesink2), decoder_key_quark (), decoder);
  g_object_set_qdata (G_OBJECT (decoder), agnostic_key_quark (), agnosticbin);
  g_object_set_qdata (G_OBJECT (decoder), fakesink_key_quark (), fakesink2);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin, fakesink,
      decoder, fakesink2, NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, agnosticbin, fakesink, NULL);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_many (agnosticbin, decoder, fakesink2, NULL);
  fail_unless (ret);
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  event_source_tag = g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (static_link)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstElement *decoder = gst_element_factory_make ("vp8dec", NULL);
  GstElement *fakesink2 = gst_element_factory_make ("fakesink", NULL);
  gboolean ret;
  int event_source_tag;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink2), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink2), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin, fakesink,
      decoder, fakesink2, NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, agnosticbin, fakesink, NULL);
  fail_unless (ret);
  mark_point ();
  ret = gst_element_link_many (agnosticbin, decoder, fakesink2, NULL);
  fail_unless (ret);
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  event_source_tag = g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (encoded_input_n_encoded_output)
{
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *encoder = gst_element_factory_make ("alawenc", NULL);
  GstElement *agnosticbin =
      gst_element_factory_make ("agnosticbin", "agnosticbin");
  gboolean ret;
  int *count, i;
  int event_source_tag;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, encoder, agnosticbin,
      NULL);
  mark_point ();
  ret = gst_element_link_many (audiotestsrc, encoder, agnosticbin, NULL);
  fail_unless (ret);
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();

  count = g_malloc0 (sizeof (int));
  *count = N_ITERS;
  g_object_set_qdata_full (G_OBJECT (pipeline), count_key_quark (), count,
      g_free);
  GST_INFO ("Connecting %d outputs", N_ITERS);

  event_source_tag = g_timeout_add_seconds (6, timeout_check, pipeline);
  for (i = 0; i < N_ITERS; i++) {
    g_timeout_add (700, connect_output, pipeline);
  }

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (encoded_input_link)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *encoder = gst_element_factory_make ("vp8enc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *filter = gst_element_factory_make ("capsfilter", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstCaps *caps;
  gboolean ret;
  int event_source_tag;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  caps = gst_caps_from_string ("video/x-raw");
  g_object_set (G_OBJECT (filter), "caps", caps, NULL);
  gst_caps_unref (caps);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, encoder, agnosticbin,
      filter, fakesink, NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, encoder, agnosticbin, filter,
      fakesink, NULL);
  fail_unless (ret);
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  event_source_tag = g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (simple_link)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  gboolean ret;
  int event_source_tag;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off_simple), loop);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin, fakesink,
      NULL);
  mark_point ();
  ret = gst_element_link_many (videotestsrc, agnosticbin, fakesink, NULL);
  fail_unless (ret);
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  event_source_tag = g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (create_test)
{
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  GstPad *pad;

  pad = gst_element_request_pad_simple (agnosticbin, "src_%u");

  GST_DEBUG_OBJECT (pad, "Pad created");

  gst_element_release_request_pad (agnosticbin, pad);
  gst_object_unref (pad);

  g_object_unref (agnosticbin);
}

GST_END_TEST
GST_START_TEST (h264_encoding_odd_dimension)
{
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true num-buffers=30"
       "  ! video/x-raw,format=(string)I420,width=(int)319,height=(int)239,"
       "    pixel-aspect-ratio=(fraction)1/1,interlace-mode=(string)progressive,"
       "    colorimetry=(string)bt601,framerate=(fraction)14/1"
       "  ! agnosticbin ! video/x-h264 ! fakesink async=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

static GstPadProbeReturn
vp8_event_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);

  if (GST_EVENT_TYPE (event) == GST_EVENT_CUSTOM_UPSTREAM) {
    fail_if (gst_video_event_is_force_key_unit (event));
  }

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
sink_probe (GstPad * pad, GstPadProbeInfo * info, gpointer pipeline)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);
  static gboolean first = TRUE;

  if (GST_EVENT_TYPE (event) != GST_EVENT_TAG) {
    const GstStructure *st = gst_event_get_structure (event);
    GST_DEBUG_OBJECT (pad, "New event (%p), type: %s, name: %s", event,
        GST_EVENT_TYPE_NAME (event), gst_structure_get_name (st));
  }

  if (GST_EVENT_TYPE (event) == GST_EVENT_CAPS) {
    GstCaps *caps = NULL;
    gst_event_parse_caps (event, &caps);

    if (first) {
      GST_DEBUG_OBJECT (pad,
          "First caps event -> Ignore, caps: %" GST_PTR_FORMAT, caps);
      first = FALSE;
    } else {
      GST_DEBUG_OBJECT (pad,
          "Second caps event -> Finish test, caps: %" GST_PTR_FORMAT, caps);
      g_idle_add (quit_main_loop_idle, loop);
    }
  }

  return GST_PAD_PROBE_OK;
}

static void
fakesink_on_handoff (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  static guint count = 0;
  ++count;

  if (count == 20) {
    GST_DEBUG_OBJECT (pad, "20 buffers handled: change caps resolution");

    // Change dimension
    {
      GstElement *pipeline = GST_ELEMENT_PARENT (fakesink);
      GstCaps *new_caps = gst_caps_from_string (
            "video/x-raw,format=(string)I420,width=(int)640,height=(int)480");
      GstElement *capsfilter;
      GstElement *vp8caps;

      // Fail if a new keyframe is requested from downstream elements.
      vp8caps = gst_bin_get_by_name (GST_BIN (pipeline), "vp8caps");
      if (vp8caps) {
        GstPad *sink = gst_element_get_static_pad (vp8caps, "sink");
        gst_pad_add_probe (sink, GST_PAD_PROBE_TYPE_EVENT_BOTH, vp8_event_probe, NULL, NULL);
        g_object_unref (sink);
        g_object_unref (vp8caps);
      }

      // This triggers a downstream GST_EVENT_CAPS.
      capsfilter = gst_bin_get_by_name (GST_BIN (pipeline), "capsfilter");
      if (capsfilter) {
        g_object_set (capsfilter, "caps", new_caps, NULL);
        g_object_unref (capsfilter);
      }

      gst_caps_unref (new_caps);
    }
  } else if (count == 40) {
    GST_DEBUG_OBJECT (pad, "40 buffers handled: check caps resolution");

    GstPad *pad = gst_element_get_static_pad (fakesink, "sink");
    GstCaps *caps = gst_pad_get_current_caps (pad);
    const GstStructure *st = gst_caps_get_structure (caps, 0);

    gint width, height;
    fail_if (!gst_structure_get_int (st, "width", &width));
    fail_if (!gst_structure_get_int (st, "height", &height));
    fail_if (width != 320 || height != 240);

    g_idle_add (quit_main_loop_idle, loop);

    gst_caps_unref (caps);
    gst_object_unref (pad);
  }
}

GST_START_TEST (video_dimension_change)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! capsfilter name=capsfilter caps=video/x-raw,format=(string)I420,width=(int)320,height=(int)240"
       "  ! agnosticbin"
       "  ! capsfilter name=vp8caps caps=video/x-vp8"
       "  ! agnosticbin"
       "  ! fakesink name=sink async=true sync=true signal-handoffs=true",
       NULL);
  gst_element_set_name(pipeline, __FUNCTION__);
  int event_source_tag;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  {
    GstPad *sink = gst_element_get_static_pad (fakesink, "sink");
    gst_pad_add_probe (sink, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM, sink_probe,
        pipeline, NULL);
    g_object_unref (sink);
  }

  // The "handoff" handler will accept 20 buffers, and then change
  // the resolution at the input caps to 640x480.
  // This should generate a downstream GST_EVENT_CAPS,
  // handled by sink_probe() at "fakesink".
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_on_handoff), NULL);
  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  event_source_tag =g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (video_dimension_change_force_output)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! capsfilter name=capsfilter caps=video/x-raw,format=(string)I420,width=(int)320,height=(int)240"
       "  ! agnosticbin"
       "  ! capsfilter name=vp8caps caps=video/x-vp8"
       "  ! agnosticbin"
       "  ! video/x-vp8,width=(int)320,height=(int)240"
       "  ! fakesink name=sink async=true sync=true signal-handoffs=true",
      NULL);
  gst_element_set_name(pipeline, __FUNCTION__);

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  int event_source_tag;

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  // The "handoff" handler will accept 20 buffers, and then change
  // the resolution at the input caps to 640x480.
  // This should generate a downstream GST_EVENT_CAPS,
  // and the second "agnosticbin" should reject it because it is configured
  // with explicit resolution of 320x240.
  // Later, sink_probe() will check that the fakesink resolution didn't change.
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_on_handoff), NULL);
  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  event_source_tag =g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (event_source_tag);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)video,"
       "    encoding-name=(string)VP8,clock-rate=(int)90000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_vp9)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)video,"
       "    encoding-name=(string)VP9,clock-rate=(int)90000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_h264)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)video,"
       "    encoding-name=(string)H264,clock-rate=(int)90000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_h265)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)video,"
       "    encoding-name=(string)H265,clock-rate=(int)90000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_av1)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("videotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)video,"
       "    encoding-name=(string)AV1,clock-rate=(int)90000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_opus)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("audiotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)audio,"
       "    encoding-name=(string)OPUS,clock-rate=(int)48000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_pcmu)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("audiotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)audio,"
       "    encoding-name=(string)PCMU,clock-rate=(int)8000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_pcma)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("audiotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)audio,"
       "    encoding-name=(string)PCMA,clock-rate=(int)8000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_raw_to_rtp_amr)
{
  GstElement *fakesink;
  GstElement *pipeline =
      gst_parse_launch
      ("audiotestsrc is-live=true"
       "  ! agnosticbin ! application/x-rtp,media=(string)audio,"
       "    encoding-name=(string)AMR-WB,clock-rate=(int)16000"
       "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

GST_START_TEST (test_codec_to_rtp)
{
  GstElement *fakesink;
  GstElement *pipeline = gst_parse_launch (
      "videotestsrc is-live=true"
      "  ! agnosticbin ! video/x-vp8"
      "  ! agnosticbin ! application/x-rtp,media=(string)video,"
      "                  encoding-name=(string)VP8,clock-rate=(int)90000"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST;

static void
check_properties (GstElement * encoder, const GstStructure * config)
{
  guint n_props = 0, i;
  GParamSpec **props;

  props =
      g_object_class_list_properties (G_OBJECT_GET_CLASS (encoder), &n_props);
  for (i = 0; i < n_props; i++) {
    const gchar *name = g_param_spec_get_name (props[i]);

    if (gst_structure_has_field (config, name)) {
      GValue final_value = { 0, };
      gchar *st_value;
      const GValue *val;
      gchar *serialized;

      val = gst_structure_get_value (config, name);
      st_value = gst_value_serialize (val);

      if (G_TYPE_IS_ENUM (props[i]->value_type)
          || G_TYPE_IS_FLAGS (props[i]->value_type)) {
        GValue second_serialized = { 0, };

        g_value_init (&second_serialized, props[i]->value_type);
        gst_value_deserialize (&second_serialized, st_value);

        g_free (st_value);
        st_value = gst_value_serialize (&second_serialized);
        g_value_reset (&second_serialized);

        GST_TRACE ("Value type is %s, serialized again to: %s",
            G_TYPE_IS_ENUM (props[i]->value_type) ? "enum" : "flags", st_value);
      }

      GST_INFO ("Processing property: %s -> %s", name, st_value);
      g_value_init (&final_value, props[i]->value_type);

      g_object_get_property (G_OBJECT (encoder), name, &final_value);

      serialized = gst_value_serialize (&final_value);

      fail_unless (serialized);
      GST_INFO ("Got value from object: %s", serialized);
      fail_if (g_strcmp0 (st_value, serialized));
      g_free (serialized);
      g_free (st_value);

      g_value_reset (&final_value);
    }
  }
  g_free (props);
}

static gboolean
check_encoder (GQuark field_id, const GValue * value, gpointer obj)
{
  if (G_VALUE_HOLDS (value, GST_TYPE_STRUCTURE)) {
    if (g_str_has_prefix (GST_OBJECT_NAME (obj), g_quark_to_string (field_id))) {
      GST_DEBUG ("%" GST_PTR_FORMAT " has matching name", obj);
      check_properties (obj, GST_STRUCTURE (g_value_get_boxed (value)));
      return FALSE;
    }
  }

  return TRUE;
}

static void
check_element_properties (const GValue * item, gpointer codec_config)
{
  GstElement *obj = g_value_get_object (item);

  if (GST_IS_BIN (obj)) {
    GstIterator *it = gst_bin_iterate_elements (GST_BIN (obj));

    gst_iterator_foreach (it, check_element_properties, codec_config);
    gst_iterator_free (it);
  } else if (GST_IS_ELEMENT (obj)) {
    gst_structure_foreach (GST_STRUCTURE (codec_config), check_encoder, obj);
  }
}

static void
test_codec_config (const gchar * pipeline_str, const gchar * config_str,
    const gchar * codec_name, const gchar * agnostic_name)
{
  GstElement *fakesink, *agnostic;
  GstStructure *codec_configs, *vp8config;
  GstElement *pipeline = gst_parse_launch (pipeline_str, NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstIterator *it;

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  agnostic = gst_bin_get_by_name (GST_BIN (pipeline), agnostic_name);
  fail_unless (agnostic);
  vp8config = gst_structure_new_from_string (config_str);
  codec_configs =
      gst_structure_new ("codec-config", codec_name, GST_TYPE_STRUCTURE,
      vp8config, NULL);
  gst_structure_free (vp8config);

  g_object_set (agnostic, "codec-config", codec_configs, NULL);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  it = gst_bin_iterate_elements (GST_BIN (agnostic));
  gst_iterator_foreach (it, check_element_properties, codec_configs);
  gst_iterator_free (it);

  g_object_unref (agnostic);

  gst_structure_free (codec_configs);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_START_TEST (test_codec_config_vp9)
{
  const gchar *pipeline_str =
      "videotestsrc is-live=true"
      "  ! agnosticbin name=ag ! capsfilter caps=video/x-vp9"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true";
  const gchar *config_str =
      "vp8,deadline=(string)10,threads=(string)1,cpu-used=16,keyframe-mode=auto";
  const gchar *codec_name = "vp9";
  const gchar *agnostic_name = "ag";

  test_codec_config (pipeline_str, config_str, codec_name, agnostic_name);
}

GST_END_TEST;

GST_START_TEST (test_codec_config_av1)
{
  const gchar *pipeline_str =
      "videotestsrc is-live=true"
      "  ! agnosticbin name=ag ! capsfilter caps=video/x-av1"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true";
  const gchar *config_str =
      "av1,threads=(string)1,cpu-used=10,keyframe-mode=auto,usage-profile=1";
  const gchar *codec_name = "av1";
  const gchar *agnostic_name = "ag";

  test_codec_config (pipeline_str, config_str, codec_name, agnostic_name);
}

GST_END_TEST;

GST_START_TEST (test_codec_config_x265)
{
  const gchar *pipeline_str =
      "videotestsrc is-live=true"
      "  ! agnosticbin name=ag ! capsfilter caps=video/x-h265"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true";
  const gchar *config_str =
      "x265,speed-preset=faster,tune=zerolatency";
  const gchar *codec_name = "x264";
  const gchar *agnostic_name = "ag";

  test_codec_config (pipeline_str, config_str, codec_name, agnostic_name);
}

GST_END_TEST;

GST_START_TEST (test_codec_config_vp8)
{
  const gchar *pipeline_str =
      "videotestsrc is-live=true"
      "  ! agnosticbin name=ag ! capsfilter caps=video/x-vp8"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true";
  const gchar *config_str =
      "vp8,deadline=(string)10,threads=(string)1,cpu-used=16,keyframe-mode=auto";
  const gchar *codec_name = "vp8";
  const gchar *agnostic_name = "ag";

  test_codec_config (pipeline_str, config_str, codec_name, agnostic_name);
}

GST_END_TEST;

GST_START_TEST (test_codec_config_x264)
{
  const gchar *pipeline_str =
      "videotestsrc is-live=true"
      "  ! agnosticbin name=ag ! capsfilter caps=video/x-h264"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true";
  const gchar *config_str =
      "x264,pass=qual,quantizer=30,speed-preset=faster,tune=zerolatency+stillimage";
  const gchar *codec_name = "x264";
  const gchar *agnostic_name = "ag";

  test_codec_config (pipeline_str, config_str, codec_name, agnostic_name);
}

GST_END_TEST;

GST_START_TEST (test_codec_config_openh264)
{
  const gchar *pipeline_str =
      "videotestsrc is-live=true ! agnosticbin name=ag"
      "  ! capsfilter caps=video/x-h264"
      "  ! fakesink async=true sync=true name=sink signal-handoffs=true";
  const gchar *config_str =
      "openh264,deblocking=off,complexity=low,gop-size=60";
  const gchar *codec_name = "openh264";
  const gchar *agnostic_name = "ag";

  test_codec_config (pipeline_str, config_str, codec_name, agnostic_name);
}

GST_END_TEST;
/*
 * End of test cases
 */
static Suite *
agnostic2_suite (void)
{
  Suite *s = suite_create ("agnosticbin");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, create_test);
  tcase_add_test (tc_chain, simple_link);
  tcase_add_test (tc_chain, encoded_input_link);
  tcase_add_test (tc_chain, static_link);
  tcase_add_test (tc_chain, reconnect_test);
  if (FALSE) {
    // FIXME: Disabled until fixed, there is a race condition in tests
    // more accusated when running with valgrind
    tcase_add_test (tc_chain, valve_test);
  }
  tcase_add_test (tc_chain, delay_stream);
  tcase_add_test (tc_chain, add_later);
  tcase_add_test (tc_chain, input_reconfiguration);
  tcase_add_test (tc_chain, input_caps_reconfiguration);
  tcase_add_test (tc_chain, encoded_input_n_encoded_output);
  tcase_add_test (tc_chain, h264_encoding_odd_dimension);
  tcase_add_test (tc_chain, video_dimension_change);
  tcase_add_test (tc_chain, video_dimension_change_force_output);

  tcase_add_test (tc_chain, test_codec_config_vp8);
  tcase_add_test (tc_chain, test_codec_config_x264);
  tcase_add_test (tc_chain, test_codec_config_openh264);
  tcase_add_test (tc_chain, test_codec_config_vp9);
  tcase_add_test (tc_chain, test_codec_config_av1);
  tcase_add_test (tc_chain, test_codec_config_x265);
  
  tcase_add_test (tc_chain, test_raw_to_rtp);
  tcase_add_test (tc_chain, test_raw_to_rtp_vp9);
  tcase_add_test (tc_chain, test_raw_to_rtp_h264);
  tcase_add_test (tc_chain, test_raw_to_rtp_h265);
  tcase_add_test (tc_chain, test_raw_to_rtp_av1);
  tcase_add_test (tc_chain, test_raw_to_rtp_opus);
  tcase_add_test (tc_chain, test_raw_to_rtp_pcmu);
  tcase_add_test (tc_chain, test_raw_to_rtp_pcma);
  tcase_add_test (tc_chain, test_raw_to_rtp_amr);
  tcase_add_test (tc_chain, test_codec_to_rtp);

  return s;
}

GST_CHECK_MAIN (agnostic2);
