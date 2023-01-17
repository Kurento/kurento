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

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

static GMainLoop *loop;
GstElement *pipeline;
guint finish;

typedef void (*CheckOnData) (GstElement *, GstBuffer *, GstPad *, gpointer);

typedef struct _CallbackData
{
  GstElement *element;
  gboolean *checkpoint;
  CheckOnData checkFunc;
} CallbackData;

struct callback_counter
{
  guint count;
  gpointer data;
  GSourceFunc func;
  GMutex mutex;
};

struct callback_counter *
create_callback_counter (gpointer data, GSourceFunc func)
{
  struct callback_counter *counter;

  counter = g_slice_new (struct callback_counter);

  g_mutex_init (&counter->mutex);
  counter->func = func;
  counter->data = data;
  counter->count = 1;

  return counter;
}

static void
callback_count_inc (struct callback_counter *counter)
{
  g_mutex_lock (&counter->mutex);
  counter->count++;
  g_mutex_unlock (&counter->mutex);
}

static void
callback_count_dec (struct callback_counter *counter)
{
  g_mutex_lock (&counter->mutex);
  counter->count--;
  if (counter->count > 0) {
    g_mutex_unlock (&counter->mutex);
    return;
  }

  g_mutex_unlock (&counter->mutex);
  g_mutex_clear (&counter->mutex);

  if (counter->func) {
    g_idle_add (counter->func, counter->data);
  }

  g_slice_free (struct callback_counter, counter);
}

static gboolean
quit_main_loop_idle (gpointer data)
{
  g_main_loop_quit (loop);
  return FALSE;
}

static gboolean
print_timedout_pipeline (gpointer data)
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

static void
handoff_checking_callback (GstElement * object, GstBuffer * buff, GstPad * pad,
    CallbackData * cb_data)
{
  *cb_data->checkpoint = TRUE;

  if (cb_data->checkFunc != NULL) {
    cb_data->checkFunc (object, buff, pad, cb_data);
  }
  /* We have received a buffer, finish test */
  g_idle_add (quit_main_loop_idle, NULL);
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
    default:
      break;
  }
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GST_DEBUG_OBJECT (pad, "Removing probe %lu", GST_PAD_PROBE_INFO_ID (info));
  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  return GST_PAD_PROBE_OK;
}

static void
call_on_negotiated (GstElement * agnosticbin, GSourceFunc func, gpointer data)
{
  struct callback_counter *counter;
  GValue val = G_VALUE_INIT;
  GstIterator *it;
  gboolean done = FALSE;

  counter = create_callback_counter (data, func);

  it = gst_element_iterate_sink_pads (GST_ELEMENT (agnosticbin));
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad;

        sinkpad = g_value_get_object (&val);
        /* install new probe for event CAPS */
        callback_count_inc (counter);
        GST_INFO ("Setting callback %" GST_PTR_FORMAT, sinkpad);
        gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_BUFFER,
            event_probe_cb, counter, (GDestroyNotify) callback_count_dec);

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR_OBJECT (agnosticbin, "Error iterating over sink pads");
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  callback_count_dec (counter);
  gst_iterator_free (it);
}

GST_START_TEST (create_sink_pad_test)
{
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);

  pipeline = gst_pipeline_new (__FUNCTION__);

  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, agnosticbin, NULL);

  if (!gst_element_link_pads (audiotestsrc, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link elements");
    return;
  }

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
}

GST_END_TEST
GST_START_TEST (create_src_pad_test)
{
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);

  pipeline = gst_pipeline_new (__FUNCTION__);

  gst_bin_add_many (GST_BIN (pipeline), sink, agnosticbin, NULL);

  if (!gst_element_link_pads (agnosticbin, "src_%u", sink, "sink")) {
    fail ("Could not link elements");
    return;
  }

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
}

GST_END_TEST static gboolean
caps_request (GstElement * object, GstCaps * caps, gboolean * success)
{
  GST_DEBUG ("Signal catched %" GST_PTR_FORMAT, caps);
  *success = TRUE;

  if (!g_source_remove (finish)) {
    GST_ERROR ("Can not remove source");
  }

  g_idle_add (quit_main_loop_idle, NULL);

  /* No caps supported */
  return FALSE;
}

GST_START_TEST (create_src_pad_test_with_caps)
{
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  GstElement *sink = gst_element_factory_make ("avenc_msmpeg4", NULL);
  GstCaps *caps = gst_caps_from_string ("video/x-raw");
  GstPadTemplate *templ;
  GstPad *srcpad, *sinkpad;
  gboolean success = FALSE;
  GstBus *bus;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  finish = g_timeout_add_seconds (2, quit_main_loop_idle, NULL);
  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request), &success);

  loop = g_main_loop_new (NULL, TRUE);

  templ =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (agnosticbin),
      "src_%u");

  gst_bin_add_many (GST_BIN (pipeline), sink, agnosticbin, NULL);

  srcpad = gst_element_request_pad (agnosticbin, templ, NULL, caps);
  sinkpad = gst_element_get_static_pad (sink, "sink");

  if (gst_pad_link (srcpad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link elements");
  }

  g_object_unref (srcpad);
  g_object_unref (sinkpad);
  g_object_unref (templ);
  gst_caps_unref (caps);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (success, "No signal catched");
}

GST_END_TEST static gboolean
connect_source_without_caps (GstElement * agnosticbin)
{
  GstElement *src = gst_element_factory_make ("videotestsrc", NULL);

  g_object_set (G_OBJECT (src), "is-live", TRUE, NULL);

  gst_bin_add (GST_BIN (pipeline), src);

  GST_DEBUG ("Connecting %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, src,
      agnosticbin);

  if (!gst_element_link_pads (src, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link elements");
  }

  gst_element_sync_state_with_parent (src);

  return G_SOURCE_REMOVE;
}

static void
handoff_callback (GstElement * object, GstBuffer * buff, GstPad * pad,
    gboolean * success)
{
  *success = TRUE;

  /* We have received a buffer, finish test */
  g_idle_add (quit_main_loop_idle, NULL);
}

GST_START_TEST (connect_source_pause_sink_test)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *conv = gst_element_factory_make ("videoconvert", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  gboolean success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_callback), &success);

  loop = g_main_loop_new (NULL, TRUE);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  gst_bin_add_many (GST_BIN (pipeline), conv, sink, agnosticbin, NULL);

  if (!gst_element_link (conv, sink)) {
    fail ("Could not link converter to videosink");
  }

  gst_element_sync_state_with_parent (agnosticbin);
  gst_element_sync_state_with_parent (conv);
  gst_element_sync_state_with_parent (sink);

  if (!gst_element_link_pads (agnosticbin, "src_%u", conv, "sink")) {
    fail ("Could not link agnosticbin to converter");
  }

  g_idle_add ((GSourceFunc) connect_source_without_caps, agnosticbin);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (success, "No buffer received");
}

GST_END_TEST
GST_START_TEST (connect_source_pause_sink_transcoding_test)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *dec = gst_element_factory_make ("avdec_msmpeg4", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  gboolean success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_callback), &success);

  loop = g_main_loop_new (NULL, TRUE);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  gst_bin_add_many (GST_BIN (pipeline), dec, sink, agnosticbin, NULL);

  if (!gst_element_link_many (dec, sink, NULL)) {
    fail ("Could not link elements");
  }

  gst_element_sync_state_with_parent (agnosticbin);
  gst_element_sync_state_with_parent (dec);
  gst_element_sync_state_with_parent (sink);

  if (!gst_element_link_pads (agnosticbin, "src_%u", dec, "sink")) {
    fail ("Could not link elements");
  }

  g_idle_add ((GSourceFunc) connect_source_without_caps, agnosticbin);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (success, "No buffer received");
}

GST_END_TEST static gboolean
caps_request_triggered (GstElement * object, GstCaps * caps,
    gboolean * triggered)
{
  GST_DEBUG ("Signal catched %" GST_PTR_FORMAT, caps);
  *triggered = TRUE;

  /* No caps supported */
  return FALSE;
}

GST_START_TEST (connect_source_configured_pause_sink_test)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  GstBus *bus;
  GstCaps *caps = gst_caps_from_string ("video/x-raw");
  GstPadTemplate *templ;
  GstPad *srcpad, *sinkpad;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_callback), &success);

  loop = g_main_loop_new (NULL, TRUE);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  gst_bin_add_many (GST_BIN (pipeline), sink, agnosticbin, NULL);
  gst_element_sync_state_with_parent (agnosticbin);
  gst_element_sync_state_with_parent (sink);

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  templ =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (agnosticbin),
      "src_%u");
  srcpad = gst_element_request_pad (agnosticbin, templ, NULL, caps);
  sinkpad = gst_element_get_static_pad (sink, "sink");

  if (gst_pad_link (srcpad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link elements");
  }

  g_object_unref (srcpad);
  g_object_unref (sinkpad);
  g_object_unref (templ);
  gst_caps_unref (caps);

  g_idle_add ((GSourceFunc) connect_source_without_caps, agnosticbin);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (triggered, "No caps signal triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST
GST_START_TEST (connect_source_configured_pause_sink_transcoding_test)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *dec = gst_element_factory_make ("avdec_msmpeg4", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  GstBus *bus;
  GstCaps *caps = gst_caps_from_string ("video/x-msmpeg");
  GstPadTemplate *templ;
  GstPad *srcpad, *sinkpad;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_callback), &success);

  loop = g_main_loop_new (NULL, TRUE);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  gst_bin_add_many (GST_BIN (pipeline), dec, sink, agnosticbin, NULL);
  gst_element_sync_state_with_parent (agnosticbin);
  gst_element_sync_state_with_parent (dec);
  gst_element_sync_state_with_parent (sink);

  if (!gst_element_link (dec, sink)) {
    fail ("Could not decoder to sink");
  }

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  templ =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (agnosticbin),
      "src_%u");
  srcpad = gst_element_request_pad (agnosticbin, templ, NULL, caps);
  sinkpad = gst_element_get_static_pad (dec, "sink");

  if (gst_pad_link (srcpad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link agnosticbin to decoder");
  }

  g_object_unref (srcpad);
  g_object_unref (sinkpad);
  g_object_unref (templ);
  gst_caps_unref (caps);

  g_idle_add ((GSourceFunc) connect_source_without_caps, agnosticbin);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (triggered, "No caps signal triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST static gboolean
connect_sink_without_caps (CallbackData * cb_data)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = cb_data->element;

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_checking_callback),
      cb_data);

  gst_bin_add (GST_BIN (pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  if (!gst_element_link_pads (agnosticbin, "src_%u", sink, "sink")) {
    fail ("Could not link agnosticbin to videosink");
  }

  return G_SOURCE_REMOVE;
}

GST_START_TEST (connect_sinkpad_pause_srcpad_test)
{
  GstElement *source = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  CallbackData cb_data;
  GstBus *bus;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source), "is-live", TRUE, NULL);

  loop = g_main_loop_new (NULL, TRUE);

  cb_data.element = agnosticbin;
  cb_data.checkpoint = &success;
  cb_data.checkFunc = NULL;

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source, agnosticbin, NULL);

  if (!gst_element_link_pads (source, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link videotestsrc to agnosticbin");
  }

  /* Connect sink element once caps are negotiated */
  call_on_negotiated (agnosticbin, (GSourceFunc) connect_sink_without_caps,
      &cb_data);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (!triggered, "Caps signal did not have to be triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST static gboolean
connect_sink_with_raw_caps (CallbackData * cb_data)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *conv = gst_element_factory_make ("videoconvert", NULL);
  GstElement *agnosticbin = cb_data->element;

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_checking_callback),
      cb_data);

  gst_bin_add_many (GST_BIN (pipeline), conv, sink, NULL);

  /* We use a videoconvert to force video-raw format */
  if (!gst_element_link (conv, sink)) {
    fail ("Could not link videoconverter to videosink");
  }

  gst_element_sync_state_with_parent (conv);
  gst_element_sync_state_with_parent (sink);

  if (!gst_element_link_pads (agnosticbin, "src_%u", conv, "sink")) {
    fail ("Could not link agnosticbin to converter");
  }

  return G_SOURCE_REMOVE;
}

GST_START_TEST (connect_sinkpad_pause_srcpad_transcoded_test)
{
  GstElement *source = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *enc = gst_element_factory_make ("vp8enc", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  CallbackData cb_data;
  GstBus *bus;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source), "is-live", TRUE, NULL);

  loop = g_main_loop_new (NULL, TRUE);

  cb_data.element = agnosticbin;
  cb_data.checkpoint = &success;
  cb_data.checkFunc = NULL;

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source, enc, agnosticbin, NULL);

  if (!gst_element_link (source, enc)) {
    fail ("Could not link videotestsrc to agnosticbin");
  }

  if (!gst_element_link_pads (enc, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link encoder to agnosticbin");
  }

  /* Connect sink element once caps are negotiated */
  call_on_negotiated (agnosticbin, (GSourceFunc) connect_sink_with_raw_caps,
      &cb_data);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (triggered, "Caps signal triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST static gboolean
connect_sink_with_raw_caps_templ (CallbackData * cb_data)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = cb_data->element;
  GstCaps *caps = gst_caps_from_string ("video/x-raw");
  GstPadTemplate *templ;
  GstPad *srcpad, *sinkpad;

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_checking_callback),
      cb_data);

  templ =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (agnosticbin),
      "src_%u");

  gst_bin_add (GST_BIN (pipeline), sink);

  srcpad = gst_element_request_pad (agnosticbin, templ, NULL, caps);
  sinkpad = gst_element_get_static_pad (sink, "sink");

  if (gst_pad_link (srcpad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link agnosticbin to sink");
  }

  g_object_unref (srcpad);
  g_object_unref (sinkpad);
  g_object_unref (templ);
  gst_caps_unref (caps);

  gst_element_sync_state_with_parent (sink);

  return G_SOURCE_REMOVE;
}

GST_START_TEST (connect_sinkpad_pause_srcpad_with_caps_test)
{
  GstElement *source = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  CallbackData cb_data;
  GstBus *bus;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source), "is-live", TRUE, NULL);

  loop = g_main_loop_new (NULL, TRUE);

  cb_data.element = agnosticbin;
  cb_data.checkpoint = &success;
  cb_data.checkFunc = NULL;

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source, agnosticbin, NULL);

  if (!gst_element_link_pads (source, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link videotestsrc to agnosticbin");
  }

  /* Connect sink element once caps are negotiated */
  call_on_negotiated (agnosticbin,
      (GSourceFunc) connect_sink_with_raw_caps_templ, &cb_data);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (!triggered, "Caps signal did not have to be triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST static gboolean
connect_sink_with_mpeg_caps_templ (CallbackData * cb_data)
{
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = cb_data->element;
  GstCaps *caps = gst_caps_from_string ("video/x-msmpeg");
  GstPadTemplate *templ;
  GstPad *srcpad, *sinkpad;

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", G_CALLBACK (handoff_checking_callback),
      cb_data);

  templ =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (agnosticbin),
      "src_%u");

  gst_bin_add (GST_BIN (pipeline), sink);

  srcpad = gst_element_request_pad (agnosticbin, templ, NULL, caps);
  sinkpad = gst_element_get_static_pad (sink, "sink");

  if (gst_pad_link (srcpad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link agnosticbin to sink");
  }

  g_object_unref (srcpad);
  g_object_unref (sinkpad);
  g_object_unref (templ);
  gst_caps_unref (caps);

  gst_element_sync_state_with_parent (sink);

  return G_SOURCE_REMOVE;
}

GST_START_TEST (connect_sinkpad_pause_srcpad_with_caps_transcoding_test)
{
  GstElement *source = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  CallbackData cb_data;
  GstBus *bus;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source), "is-live", TRUE, NULL);

  loop = g_main_loop_new (NULL, TRUE);

  cb_data.element = agnosticbin;
  cb_data.checkpoint = &success;
  cb_data.checkFunc = NULL;

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source, agnosticbin, NULL);

  if (!gst_element_link_pads (source, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link videotestsrc to agnosticbin");
  }

  /* Connect sink element once caps are negotiated */
  call_on_negotiated (agnosticbin,
      (GSourceFunc) connect_sink_with_mpeg_caps_templ, &cb_data);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (triggered, "Caps signal was not triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST static void
exit_on_handoff (GstElement * object, GstBuffer * buff, GstPad * pad,
    gpointer data)
{
  /* We have received a buffer in the second sink => finish test */
  g_idle_add (quit_main_loop_idle, NULL);
}

static gboolean
add_sink3 (GstElement * agnosticbin)
{
  GstElement *dec = gst_element_factory_make ("vp8dec", NULL);
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (sink, "handoff", G_CALLBACK (exit_on_handoff), NULL);

  gst_bin_add_many (GST_BIN (pipeline), dec, sink, NULL);

  if (!gst_element_link (dec, sink)) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, dec, sink);
    return G_SOURCE_REMOVE;
  }

  if (!gst_element_link_pads (agnosticbin, "src_%u", dec, "sink")) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, agnosticbin,
        dec);
    return G_SOURCE_REMOVE;
  }

  gst_element_sync_state_with_parent (sink);
  gst_element_sync_state_with_parent (dec);

  return G_SOURCE_REMOVE;
}

G_LOCK_DEFINE (sink2_mutex);
static gboolean added_sink2 = FALSE;

static void
sink2_handoff_cb (GstElement * object, GstBuffer * buff, GstPad * pad,
    GstElement * agnosticbin)
{
  G_LOCK (sink2_mutex);
  if (!added_sink2) {
    g_idle_add ((GSourceFunc) add_sink3, agnosticbin);
    added_sink2 = TRUE;
  }
  G_UNLOCK (sink2_mutex);
}

static gboolean
add_sink2 (GstElement * agnosticbin)
{
  GstElement *dec = gst_element_factory_make ("avdec_msmpeg4", NULL);
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);

  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (sink, "handoff", G_CALLBACK (sink2_handoff_cb),
      agnosticbin);

  gst_bin_add_many (GST_BIN (pipeline), dec, sink, NULL);

  if (!gst_element_link (dec, sink)) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, dec, sink);
    return G_SOURCE_REMOVE;
  }

  if (!gst_element_link_pads (agnosticbin, "src_%u", dec, "sink")) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, agnosticbin,
        dec);
    return G_SOURCE_REMOVE;
  }

  gst_element_sync_state_with_parent (dec);
  gst_element_sync_state_with_parent (sink);

  return G_SOURCE_REMOVE;
}

G_LOCK_DEFINE (event_mutex);
static gboolean negotiated = FALSE;

static GstPadProbeReturn
event_caps_cb (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_OK;
  }

  G_LOCK (event_mutex);
  if (!negotiated) {
    g_idle_add ((GSourceFunc) add_sink2, data);
    negotiated = TRUE;
  }
  G_UNLOCK (event_mutex);

  return GST_PAD_PROBE_REMOVE;
}

static gboolean
add_source2 (GstElement * agnosticbin)
{
  GstElement *source = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *enc = gst_element_factory_make ("avenc_msmpeg4", NULL);
  GstPad *pad;

  pad = gst_element_get_static_pad (enc, "sink");
  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      event_caps_cb, agnosticbin, NULL);
  g_object_unref (pad);

  g_object_set (G_OBJECT (source), "is-live", TRUE, NULL);

  gst_bin_add_many (GST_BIN (pipeline), source, enc, NULL);

  if (!gst_element_link (source, enc)) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, enc,
        source);
    return G_SOURCE_REMOVE;
  }

  if (!gst_element_link_pads (enc, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, enc,
        agnosticbin);
    return G_SOURCE_REMOVE;
  }

  gst_element_sync_state_with_parent (enc);
  gst_element_sync_state_with_parent (source);

  return G_SOURCE_REMOVE;
}

G_LOCK_DEFINE (sink1_mutex);
static gboolean added_source2 = FALSE;

static void
sink_handoff_cb (GstElement * object, GstBuffer * buff, GstPad * pad,
    GstElement * agnosticbin)
{
  G_LOCK (sink1_mutex);
  if (!added_source2) {
    g_idle_add ((GSourceFunc) add_source2, agnosticbin);
    added_source2 = TRUE;
  }
  G_UNLOCK (sink1_mutex);
}

static gboolean
are_same_capabilities (GstElement * agnosticbin)
{
  GstCaps *assigned_caps = NULL;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE, same = TRUE;
  GstIterator *it;

  it = gst_element_iterate_src_pads (agnosticbin);

  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *srcpad;
        GstCaps *current_caps;

        srcpad = g_value_get_object (&val);
        current_caps = gst_pad_get_current_caps (srcpad);

        if (assigned_caps == NULL) {
          assigned_caps = gst_caps_copy (current_caps);
        } else {
          if (!gst_caps_is_equal (assigned_caps, current_caps)) {
            /* Stop checking so test has failed */
            done = TRUE;
            same = FALSE;
          }
        }

        gst_caps_unref (current_caps);
        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (agnosticbin));
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  } while (!done);

  g_value_unset (&val);
  gst_iterator_free (it);

  if (assigned_caps != NULL) {
    gst_caps_unref (assigned_caps);
  }

  return same;
}

GST_START_TEST (connect_two_sources_three_sinks)
{
  GstElement *source = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *dec = gst_element_factory_make ("vp8dec", NULL);
  GstElement *sink = gst_element_factory_make ("fakesink", NULL);
  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  gboolean triggered = FALSE;
  GstBus *bus;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (sink, "handoff", G_CALLBACK (sink_handoff_cb), agnosticbin);

  loop = g_main_loop_new (NULL, TRUE);

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source, agnosticbin, dec, sink, NULL);

  if (!gst_element_link (dec, sink)) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, dec, sink);
    return;
  }

  if (!gst_element_link_pads (agnosticbin, "src_%u", dec, "sink")) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, agnosticbin,
        dec);
    return;
  }

  if (!gst_element_link_pads (source, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, source,
        agnosticbin);
  }

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  if (FALSE) {
    fail_if (!are_same_capabilities (agnosticbin),
        "Transcoding should not have happened");
  }

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST static void
check_src_capabilities (GstElement * element, GstBuffer * buff, GstPad * pad,
    gpointer data)
{
  GstElement *agnosticbin;
  GstProxyPad *proxypad;
  GstPad *peerpad;
  GstPad *sinkpad, *sink;
  GstCaps *sinkcaps, *srccaps;

  peerpad = gst_pad_get_peer (pad);
  fail_if (peerpad == NULL, "No peer pad");

  proxypad = gst_proxy_pad_get_internal (GST_PROXY_PAD (peerpad));
  fail_if (proxypad == NULL, "No proxypad");

  sinkpad = gst_pad_get_peer (GST_PAD (proxypad));
  fail_if (sinkpad == NULL, "No sinkpad got from proxypad");

  agnosticbin = gst_pad_get_parent_element (sinkpad);
  fail_if (sinkpad == NULL, "No agnosticbin connected");

  sink = gst_element_get_static_pad (agnosticbin, "sink");
  sinkcaps = gst_pad_get_current_caps (sink);
  srccaps = gst_pad_get_current_caps (pad);

  if (!gst_caps_is_always_compatible (sinkcaps, srccaps)) {
    GST_DEBUG_OBJECT (sink, "%" GST_PTR_FORMAT, sinkcaps);
    GST_DEBUG_OBJECT (pad, "%" GST_PTR_FORMAT, srccaps);
    fail ("Incompatible caps");
  }

  gst_caps_unref (sinkcaps);
  gst_caps_unref (srccaps);

  g_object_unref (sink);
  g_object_unref (sinkpad);
  g_object_unref (proxypad);
  g_object_unref (peerpad);
  g_object_unref (agnosticbin);
}

GST_START_TEST (two_sinks_one_src_test)
{
  GstElement *source1 = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *source2 = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *enc1 = gst_element_factory_make ("vp8enc", NULL);
  GstElement *enc2 = gst_element_factory_make ("avenc_msmpeg4", NULL);

  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  CallbackData cb_data;
  GstBus *bus;
  gboolean triggered = FALSE, success = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source1), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (source2), "is-live", TRUE, NULL);

  loop = g_main_loop_new (NULL, TRUE);

  cb_data.element = agnosticbin;
  cb_data.checkpoint = &success;
  cb_data.checkFunc = check_src_capabilities;

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source1, source2, enc1, enc2,
      agnosticbin, NULL);

  if (!gst_element_link (source1, enc1)) {
    fail ("Could not link videotestsrc1 to encoder1");
  }

  if (!gst_element_link (source2, enc2)) {
    fail ("Could not link videotestsrc2 to encoder2");
  }

  if (!gst_element_link_pads (enc1, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link encoder1 to agnosticbin");
  }

  if (!gst_element_link_pads (enc2, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link encoder2 to agnosticbin");
  }

  /* Connect sink element once caps are negotiated */
  call_on_negotiated (agnosticbin,
      (GSourceFunc) connect_sink_with_mpeg_caps_templ, &cb_data);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  fail_unless (!triggered, "Unexpected caps signal was triggered");
  fail_unless (success, "No buffer received");
}

GST_END_TEST typedef struct _ReceivedPads
{
  GMutex mutex;
  GstPad *pad1;
  GstPad *pad2;
} ReceivedPads;

static void
check_finish_main_loop (GstElement * object, GstBuffer * buff, GstPad * pad,
    ReceivedPads * pads)
{
  g_mutex_lock (&pads->mutex);

  if (pads->pad1 == NULL) {
    pads->pad1 = pad;
    goto end;
  }

  if (pads->pad2 == NULL && pads->pad1 != pad) {
    pads->pad2 = pad;
  }

end:
  if (pads->pad1 != NULL && pads->pad2 != NULL) {
    /* Both pads have received buffers */
    g_idle_add (quit_main_loop_idle, NULL);
  }

  g_mutex_unlock (&pads->mutex);
}

static gboolean
transcode_done_on (GstElement * sink)
{
  GstPad *filtersink, *sinkpad, *peerpad, *agnosticsrcpad, *agnosticsinkpad,
      *proxysink;
  GstElement *capsfilter, *transcoder;
  GstCaps *incaps, *outcaps;
  GstProxyPad *proxypad;
  gboolean transcoded = FALSE;

  sinkpad = gst_element_get_static_pad (sink, "sink");
  peerpad = gst_pad_get_peer (sinkpad);
  capsfilter = gst_pad_get_parent_element (peerpad);
  filtersink = gst_element_get_static_pad (capsfilter, "sink");
  agnosticsrcpad = gst_pad_get_peer (filtersink);
  proxypad = gst_proxy_pad_get_internal (GST_PROXY_PAD (agnosticsrcpad));

  proxysink = gst_pad_get_peer (GST_PAD (proxypad));
  transcoder = gst_pad_get_parent_element (proxysink);
  agnosticsinkpad = gst_element_get_static_pad (transcoder, "sink");

  incaps = gst_pad_get_allowed_caps (agnosticsinkpad);
  outcaps = gst_pad_get_allowed_caps (sinkpad);

  if (!gst_caps_can_intersect (incaps, outcaps)) {
    GST_ERROR_OBJECT (sink, "Transcoded done.");
    GST_WARNING_OBJECT (agnosticsinkpad, "%" GST_PTR_FORMAT, incaps);
    GST_WARNING_OBJECT (sinkpad, "%" GST_PTR_FORMAT, outcaps);
    transcoded = TRUE;
  }

  gst_caps_unref (incaps);
  gst_caps_unref (outcaps);

  g_object_unref (sinkpad);
  g_object_unref (peerpad);
  g_object_unref (agnosticsrcpad);
  g_object_unref (proxypad);
  g_object_unref (proxysink);
  g_object_unref (agnosticsinkpad);

  g_object_unref (filtersink);
  g_object_unref (capsfilter);
  g_object_unref (transcoder);

  return transcoded;
}

GST_START_TEST (two_sinks_two_srcs_test)
{
  GstElement *source1 = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *source2 = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *enc1 = gst_element_factory_make ("vp8enc", NULL);
  GstElement *enc2 = gst_element_factory_make ("avenc_msmpeg4", NULL);
  GstElement *sink1 = gst_element_factory_make ("fakesink", NULL);
  GstElement *sink2 = gst_element_factory_make ("fakesink", NULL);
  GstCaps *filter1 = gst_caps_from_string ("video/x-vp8");
  GstCaps *filter2 = gst_caps_from_string ("video/x-msmpeg");
  ReceivedPads pads;

  GstElement *agnosticbin = gst_element_factory_make ("agnosticbin3", NULL);
  GstBus *bus;
  gboolean triggered = FALSE;

  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (source1), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (source2), "is-live", TRUE, NULL);

  g_object_set (G_OBJECT (sink1), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_object_set (G_OBJECT (sink2), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  pads.pad1 = NULL;
  pads.pad2 = NULL;
  g_mutex_init (&pads.mutex);

  g_signal_connect (sink1, "handoff", G_CALLBACK (check_finish_main_loop),
      &pads);
  g_signal_connect (sink2, "handoff", G_CALLBACK (check_finish_main_loop),
      &pads);

  g_object_set (G_OBJECT (sink1), "async", FALSE, "sync", FALSE, NULL);
  g_object_set (G_OBJECT (sink2), "async", FALSE, "sync", FALSE, NULL);

  loop = g_main_loop_new (NULL, TRUE);

  g_signal_connect (agnosticbin, "caps", G_CALLBACK (caps_request_triggered),
      &triggered);

  gst_bin_add_many (GST_BIN (pipeline), source1, source2, enc1, enc2,
      agnosticbin, sink1, sink2, NULL);

  if (!gst_element_link (source1, enc1)) {
    fail ("Could not link videotestsrc1 to encoder1");
  }

  if (!gst_element_link (source2, enc2)) {
    fail ("Could not link videotestsrc2 to encoder2");
  }

  if (!gst_element_link_pads (enc1, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link encoder1 to agnosticbin");
  }

  if (!gst_element_link_pads (enc2, "src", agnosticbin, "sink_%u")) {
    fail ("Could not link encoder2 to agnosticbin");
  }

  if (!gst_element_link_pads_filtered (agnosticbin, "src_%u", sink1, "sink",
          filter1)) {
    fail ("Could not link agnosticbin to sink1");
  }

  if (!gst_element_link_pads_filtered (agnosticbin, "src_%u", sink2, "sink",
          filter2)) {
    fail ("Could not link agnosticbin to sink2");
  }

  gst_caps_unref (filter1);
  gst_caps_unref (filter2);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  g_main_loop_run (loop);

  fail_if (transcode_done_on (sink1), "Sink1 should not transcode");
  fail_if (transcode_done_on (sink2), "Sink2 should not transcode");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  g_mutex_clear (&pads.mutex);

  fail_if (triggered, "Caps signal should not be triggered");
}

GST_END_TEST
/*
 * End of test cases
 */
static Suite *
agnosticbin3_suite (void)
{
  Suite *s = suite_create ("agnosticbin3");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  /* basic use cases */
  tcase_add_test (tc_chain, create_sink_pad_test);
  tcase_add_test (tc_chain, create_src_pad_test_with_caps);
  tcase_add_test (tc_chain, create_src_pad_test);
  tcase_add_test (tc_chain, connect_source_pause_sink_test);
  tcase_add_test (tc_chain, connect_source_pause_sink_transcoding_test);
  tcase_add_test (tc_chain, connect_source_configured_pause_sink_test);
  tcase_add_test (tc_chain,
      connect_source_configured_pause_sink_transcoding_test);
  tcase_add_test (tc_chain, connect_sinkpad_pause_srcpad_test);
  tcase_add_test (tc_chain, connect_sinkpad_pause_srcpad_transcoded_test);
  tcase_add_test (tc_chain, connect_sinkpad_pause_srcpad_with_caps_test);
  tcase_add_test (tc_chain,
      connect_sinkpad_pause_srcpad_with_caps_transcoding_test);

  /* complex use cases */
  tcase_add_test (tc_chain, two_sinks_one_src_test);
  tcase_add_test (tc_chain, two_sinks_two_srcs_test);
  tcase_add_test (tc_chain, connect_two_sources_three_sinks);

  return s;
}

GST_CHECK_MAIN (agnosticbin3);
