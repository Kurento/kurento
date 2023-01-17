/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

static void
start_on_handoff (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  static guint count = 0;

  GST_DEBUG ("Handoff");

  if (count > 20) {
    g_object_set (fakesink, "signal-handoffs", FALSE, NULL);
    quit_main_loop_idle (loop);
  }

  count++;
}

static gboolean
push_sample (gpointer data)
{
  static gint width = 640;
  static gint height = 480;
  static gint framerate = 15;
  static GstClockTime time = 0;
  static GstSegment segment;
  static gboolean first = TRUE;
  GstFlowReturn ret;
  GstElement *appsrc = data;
  GstBuffer *buffer;
  GstSample *sample;
  gsize size;
  GstCaps *caps =
      gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "RGB",
      "framerate", GST_TYPE_FRACTION, framerate, 1, "width", G_TYPE_INT, width,
      "height", G_TYPE_INT, height, "pixel-aspect-ratio", GST_TYPE_FRACTION, 1,
      1, NULL);

  if (first) {
    gst_segment_init (&segment, GST_FORMAT_TIME);
    first = FALSE;
  }

  size = (gsize) width *(gsize) height *3;

  buffer = gst_buffer_new_allocate (NULL, size, NULL);
  sample = gst_sample_new (buffer, caps, &segment, NULL);

  GST_BUFFER_PTS (buffer) = time;

  g_object_set (appsrc, "caps", caps, NULL);
  g_signal_emit_by_name (appsrc, "push-sample", sample, &ret);

  fail_if (ret != GST_FLOW_OK, "Error flow");

  gst_sample_unref (sample);
  gst_buffer_unref (buffer);
  gst_caps_unref (caps);

  time += GST_SECOND / framerate;
  width += 10;
  if (width > 1640) {
    width = 640;
  }
  height += 20;
  if (height > 1480) {
    height = 480;
  }

  return G_SOURCE_CONTINUE;
}

GST_START_TEST (negotiation_performance)
{
  GstElement *fakesink, *appsrc;
  GstElement *pipeline =
      gst_parse_launch
      ("appsrc name=src do-timestamp=true format=time ! agnosticbin ! agnosticbin ! agnosticbin ! fakesink async=false sync=false name=sink signal-handoffs=true",
      NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  loop = g_main_loop_new (NULL, TRUE);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fakesink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");
  appsrc = gst_bin_get_by_name (GST_BIN (pipeline), "src");

  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (start_on_handoff), loop);

  g_object_unref (fakesink);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_full (G_PRIORITY_DEFAULT, 1000 / 15, push_sample, appsrc,
      g_object_unref);

  g_timeout_add_seconds (2, timeout_check, pipeline);

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
/*
 * End of test cases
 */
static Suite *
agnostic2_suite (void)
{
  Suite *s = suite_create ("agnosticbin_negotiation");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, negotiation_performance);

  return s;
}

GST_CHECK_MAIN (agnostic2);
