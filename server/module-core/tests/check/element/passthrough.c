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
#include "../../src/gst-plugins/commons/kmselementpadtype.h"

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

#define VIDEO_SINK "video-sink"
G_DEFINE_QUARK (VIDEO_SINK, video_sink);

#define BITRATE 500000

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
    default:
      break;
  }
}

static void
fakesink_hand_off_check_size (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer data)
{
  static int count = 0;
  static gsize size = 0;
  GMainLoop *loop = (GMainLoop *) data;

  size += gst_buffer_get_size (buf);

  if (count++ >= 60) {
    gsize bitrate = size * 8 / (count / 30);

    // TODO: Count for size
    GST_INFO ("Bitrate is: %" G_GSIZE_FORMAT " bits", bitrate);

    fail_if (abs ((int) ((gssize) bitrate - BITRATE)) > BITRATE * 0.15);
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, loop);
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

typedef struct _KmsConnectData
{
  GstElement *src;
  GstBin *pipe;
  const gchar *pad_prefix;
  gulong id;
} KmsConnectData;

static gboolean
connect_sink_async (GstElement * passthrough, GstElement * src,
    GstElement * pipe, const gchar * pad_prefix)
{
  GstPad *pad = gst_element_get_static_pad (passthrough, pad_prefix);

  if (pad == NULL) {
    return FALSE;
  }

  gst_bin_add (GST_BIN (pipe), src);
  gst_element_link_pads (src, NULL, passthrough, pad_prefix);
  gst_element_sync_state_with_parent (src);
  g_object_unref (pad);

  return TRUE;
}

static void
connect_sink_on_srcpad_added (GstElement * element, GstPad * pad)
{
  GstElement *sink;
  GstPad *sinkpad;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, pad);

  if (g_str_has_prefix (GST_PAD_NAME (pad), KMS_AUDIO_PREFIX)) {
    GST_ERROR_OBJECT (pad, "Not connecting audio stream, it is not expected");
    return;
  } else if (g_str_has_prefix (GST_PAD_NAME (pad), KMS_VIDEO_PREFIX)) {
    GST_DEBUG_OBJECT (pad, "Connecting video stream");
    sink = g_object_get_qdata (G_OBJECT (element), video_sink_quark ());
  } else {
    GST_TRACE_OBJECT (pad, "Not src pad type");
    return;
  }

  sinkpad = gst_element_get_static_pad (sink, "sink");
  gst_pad_link (pad, sinkpad);
  g_object_unref (sinkpad);
  gst_element_sync_state_with_parent (sink);
}

static gboolean
kms_element_request_srcpad (GstElement * src, KmsElementPadType pad_type)
{
  gchar *padname;
  gboolean ret;

  g_signal_emit_by_name (src, "request-new-pad", pad_type, NULL, GST_PAD_SRC,
      &padname);
  ret = padname != NULL;
  g_free (padname);

  return ret;
}

static void
on_pad_added_cb (GstElement * element, GstPad * pad, gpointer user_data)
{
  GST_DEBUG_OBJECT (element, "Pad added %" GST_PTR_FORMAT, pad);

  if (gst_pad_get_direction (pad) == GST_PAD_SRC) {
    connect_sink_on_srcpad_added (element, pad);
  }
}

GST_START_TEST (check_connecion)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *passthrough = gst_element_factory_make ("passthrough", NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  KmsConnectData data;

  data.src = videotestsrc;
  data.pipe = GST_BIN (pipeline);
  data.pad_prefix = "sink_video_default";

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  g_object_set_qdata (G_OBJECT (passthrough), video_sink_quark (), fakesink);
  g_signal_connect (passthrough, "pad-added",
      G_CALLBACK (on_pad_added_cb), &data);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), passthrough, fakesink, NULL);
  mark_point ();
  fail_unless (kms_element_request_srcpad (passthrough,
          KMS_ELEMENT_PAD_TYPE_VIDEO));
  fail_if (!connect_sink_async (passthrough, videotestsrc, pipeline,
          data.pad_prefix));

  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_timeout_add_seconds (10, timeout_check, pipeline);

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

GST_START_TEST (check_bitrate)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *passthrough = gst_element_factory_make ("passthrough", NULL);
  GstElement *capsfilter = gst_element_factory_make ("capsfilter", NULL);
  GstCaps *caps = gst_caps_from_string ("video/x-vp8,framerate=30/1");
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  KmsConnectData data;

  data.src = videotestsrc;
  data.pipe = GST_BIN (pipeline);
  data.pad_prefix = "sink_video_default";

  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "sync", TRUE, "signal-handoffs", TRUE,
      "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "handoff",
      G_CALLBACK (fakesink_hand_off_check_size), loop);

  g_object_set (capsfilter, "caps", caps, NULL);
  gst_caps_unref (caps);

  g_object_set (passthrough, "target-encoder-bitrate", BITRATE,
      "min-encoder-bitrate", BITRATE, "max-encoder-bitrate", BITRATE, NULL);

  g_object_set_qdata (G_OBJECT (passthrough), video_sink_quark (), capsfilter);
  g_signal_connect (passthrough, "pad-added",
      G_CALLBACK (on_pad_added_cb), NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), passthrough, capsfilter, fakesink,
      NULL);
  gst_element_link (capsfilter, fakesink);
  mark_point ();
  fail_if (!connect_sink_async (passthrough, videotestsrc, pipeline,
          data.pad_prefix));
  fail_unless (kms_element_request_srcpad (passthrough,
          KMS_ELEMENT_PAD_TYPE_VIDEO));
  mark_point ();
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  g_timeout_add_seconds (10, timeout_check, pipeline);

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
/* Suite initialization */
static Suite *
passthrough_suite (void)
{
  Suite *s = suite_create ("filterelement");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, check_connecion);
  tcase_add_test (tc_chain, check_bitrate);

  return s;
}

GST_CHECK_MAIN (passthrough);
