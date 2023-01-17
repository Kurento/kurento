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
#include <glib.h>

#include "kmshttpendpointmethod.h"

#define WAIT_TIMEOUT 3
#define VIDEO_PATH BINARY_LOCATION "/video/format/small.webm"

static GMainLoop *loop = NULL;
static KmsHttpEndpointMethod method;
GstElement *src_pipeline, *souphttpsrc, *appsink, *uridecodebin;
GstElement *test_pipeline, *httpep, *fakesink;

typedef struct _KmsConnectData
{
  GstElement *src;
  GstBin *pipe;
  const gchar *pad_prefix;
  gulong id;
} KmsConnectData;

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
    case GST_MESSAGE_STATE_CHANGED: {
      GstState old_state, new_state;

      gst_message_parse_state_changed (msg, &old_state, &new_state, NULL);

      GST_DEBUG ("Pipeline '%s': Bus event: State change (%s) %s -> %s",
          GST_ELEMENT_NAME (pipeline), GST_OBJECT_NAME (msg->src),
          gst_element_state_get_name (old_state),
          gst_element_state_get_name (new_state));

      break;
    }
    default:
      break;
  }
}

static GstFlowReturn
post_recv_sample (GstElement * appsink, gpointer user_data)
{
  GstSample *sample = NULL;
  GstFlowReturn ret;
  GstBuffer *buffer;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample == NULL)
    return GST_FLOW_ERROR;

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  g_signal_emit_by_name (httpep, "push-buffer", buffer, &ret);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to httpep %s. Ret code %d",
        GST_ELEMENT_NAME (httpep), ret);
  }

  g_object_get (G_OBJECT (httpep), "http-method", &method, NULL);
  ck_assert_int_eq (method, KMS_HTTP_ENDPOINT_METHOD_POST);

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static gboolean
timer_cb (gpointer data)
{
  /* Agnostic bin might be now ready to go to Playing state */
  GST_INFO ("Connecting appsink to receive buffers");
  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (test_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "timer_dot");

  fakesink = gst_element_factory_make ("fakesink", NULL);

  gst_bin_add (GST_BIN (test_pipeline), fakesink);

  gst_element_set_state (fakesink, GST_STATE_PLAYING);
  gst_element_set_state (httpep, GST_STATE_PLAYING);

  gst_element_link_pads (httpep, "video_src_%u", fakesink, "sink");

  /* Start getting data from Internet */
  gst_element_set_state (src_pipeline, GST_STATE_PLAYING);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (src_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "src_getting_data");

  return FALSE;
}

static void
appsink_eos_cb (GstElement * appsink, gpointer user_data)
{
  GstFlowReturn ret;

  GST_INFO ("EOS received on %s. Preparing %s to finish the test",
      GST_ELEMENT_NAME (appsink), GST_ELEMENT_NAME (test_pipeline));

  g_signal_emit_by_name (httpep, "end-of-stream", &ret);

  if (ret != GST_FLOW_OK) {
    // something wrong
    GST_ERROR ("Could not send EOS to %s. Ret code %d",
        GST_ELEMENT_NAME (httpep), ret);
    fail ("Can not send buffer to", GST_ELEMENT_NAME (httpep));
  }
}

static void
http_eos_cb (GstElement * appsink, gpointer user_data)
{
  GST_INFO ("EOS received on %s element. Stopping main loop",
      GST_ELEMENT_NAME (httpep));
  g_main_loop_quit (loop);
}

static void
link_pad (GstElement * uridecodebin, GstPad * pad, GstElement * appsink)
{
  gst_element_link_pads (uridecodebin, GST_OBJECT_NAME (pad), appsink, "sink");
}

GST_START_TEST (check_push_buffer)
{
  guint bus_watch_id1, bus_watch_id2;
  GstBus *srcbus, *testbus;
  GstCaps *caps;

  GST_INFO ("Running test check_push_buffer");

  loop = g_main_loop_new (NULL, FALSE);

  /* Create source pipeline */
  src_pipeline = gst_pipeline_new ("src-pipeline");
  uridecodebin = gst_element_factory_make ("uridecodebin", NULL);
  appsink = gst_element_factory_make ("appsink", NULL);

  srcbus = gst_pipeline_get_bus (GST_PIPELINE (src_pipeline));

  bus_watch_id1 = gst_bus_add_watch (srcbus, gst_bus_async_signal_func, NULL);
  g_signal_connect (srcbus, "message", G_CALLBACK (bus_msg_cb), src_pipeline);
  g_object_unref (srcbus);

  gst_bin_add_many (GST_BIN (src_pipeline), uridecodebin, appsink, NULL);

  caps = gst_caps_new_any ();
  g_object_set (G_OBJECT (uridecodebin), "uri", VIDEO_PATH, "caps", caps, NULL);
  gst_caps_unref (caps);

  g_signal_connect (G_OBJECT (uridecodebin), "pad-added", G_CALLBACK (link_pad),
      appsink);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_signal_connect (appsink, "new-sample", G_CALLBACK (post_recv_sample), NULL);
  g_signal_connect (appsink, "eos", G_CALLBACK (appsink_eos_cb), NULL);

  /* Create test pipeline */
  test_pipeline = gst_pipeline_new ("test-pipeline");
  httpep = gst_element_factory_make ("httppostendpoint", NULL);

  testbus = gst_pipeline_get_bus (GST_PIPELINE (test_pipeline));

  bus_watch_id2 = gst_bus_add_watch (testbus, gst_bus_async_signal_func, NULL);
  g_signal_connect (testbus, "message", G_CALLBACK (bus_msg_cb), test_pipeline);
  g_object_unref (testbus);

  gst_bin_add (GST_BIN (test_pipeline), httpep);
  g_signal_connect (G_OBJECT (httpep), "eos", G_CALLBACK (http_eos_cb), NULL);

  /* Set pipeline to start state */
  gst_element_set_state (test_pipeline, GST_STATE_PLAYING);

  g_object_get (G_OBJECT (httpep), "http-method", &method, NULL);
  GST_INFO ("Http end point configured as %d", method);

  mark_point ();

  g_timeout_add_seconds (WAIT_TIMEOUT, timer_cb, NULL);
  GST_INFO ("Waiting %d second for Agnosticbin to be ready to go to "
      "PLAYING state", WAIT_TIMEOUT);

  g_main_loop_run (loop);

  mark_point ();

  GST_DEBUG ("Main loop stopped");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (src_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "src_after_main_loop");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (test_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_after_main_loop");

  gst_element_set_state (src_pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (src_pipeline));

  gst_element_set_state (test_pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (test_pipeline));

  g_source_remove (bus_watch_id1);
  g_source_remove (bus_watch_id2);
  g_main_loop_unref (loop);
}

GST_END_TEST
/* End of test check_push_buffer */
GST_START_TEST (check_emit_encoded_media)
{
  guint bus_watch_id1, bus_watch_id2;
  GstBus *srcbus, *testbus;
  GstCaps *caps;

  GST_INFO ("Running test check_push_buffer");

  loop = g_main_loop_new (NULL, FALSE);

  /* Create source pipeline */
  src_pipeline = gst_pipeline_new ("src-pipeline");
  uridecodebin = gst_element_factory_make ("uridecodebin", NULL);
  appsink = gst_element_factory_make ("appsink", NULL);

  srcbus = gst_pipeline_get_bus (GST_PIPELINE (src_pipeline));

  bus_watch_id1 = gst_bus_add_watch (srcbus, gst_bus_async_signal_func, NULL);
  g_signal_connect (srcbus, "message", G_CALLBACK (bus_msg_cb), src_pipeline);
  g_object_unref (srcbus);

  gst_bin_add_many (GST_BIN (src_pipeline), uridecodebin, appsink, NULL);

  caps = gst_caps_new_any ();
  g_object_set (G_OBJECT (uridecodebin), "uri", VIDEO_PATH, "caps", caps, NULL);
  gst_caps_unref (caps);

  g_signal_connect (G_OBJECT (uridecodebin), "pad-added", G_CALLBACK (link_pad),
      appsink);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_signal_connect (appsink, "new-sample", G_CALLBACK (post_recv_sample), NULL);
  g_signal_connect (appsink, "eos", G_CALLBACK (appsink_eos_cb), NULL);

  /* Create test pipeline */
  test_pipeline = gst_pipeline_new ("test-pipeline");
  httpep = gst_element_factory_make ("httppostendpoint", NULL);
  g_object_set (httpep, "use-encoded-media", TRUE, NULL);

  testbus = gst_pipeline_get_bus (GST_PIPELINE (test_pipeline));

  bus_watch_id2 = gst_bus_add_watch (testbus, gst_bus_async_signal_func, NULL);
  g_signal_connect (testbus, "message", G_CALLBACK (bus_msg_cb), test_pipeline);
  g_object_unref (testbus);

  gst_bin_add (GST_BIN (test_pipeline), httpep);
  g_signal_connect (G_OBJECT (httpep), "eos", G_CALLBACK (http_eos_cb), NULL);

  /* Set pipeline to start state */
  gst_element_set_state (test_pipeline, GST_STATE_PLAYING);

  g_object_get (G_OBJECT (httpep), "http-method", &method, NULL);
  GST_INFO ("Http end point configured as %d", method);

  mark_point ();

  g_timeout_add_seconds (WAIT_TIMEOUT, timer_cb, NULL);

  g_main_loop_run (loop);

  mark_point ();

  GST_DEBUG ("Main loop stopped");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (src_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "src_after_main_loop");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (test_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_after_main_loop");

  gst_element_set_state (src_pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (src_pipeline));

  gst_element_set_state (test_pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (test_pipeline));

  g_source_remove (bus_watch_id1);
  g_source_remove (bus_watch_id2);
  g_main_loop_unref (loop);
}

GST_END_TEST
/******************************/
/* HttpEndpoint test suit */
/******************************/
static Suite *
httpendpoint_suite (void)
{
  Suite *s = suite_create ("httpendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  /* Simulates POST behaviour */
  tcase_add_test (tc_chain, check_push_buffer);

  /* Simulates POST behaviour with encoded media */
  tcase_add_test (tc_chain, check_emit_encoded_media);

  return s;
}

GST_CHECK_MAIN (httpendpoint);
