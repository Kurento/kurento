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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

#include "kmshttpendpointmethod.h"

#define WAIT_TIMEOUT 3
//#define LOCATION "http://ci.kurento.com/downloads/sintel_trailer-480p.webm"
#define VIDEO_PATH BINARY_LOCATION "/video/small.webm"

static GMainLoop *loop = NULL;
static KmsHttpEndpointMethod method;
GstElement *src_pipeline, *souphttpsrc, *appsink, *uridecodebin;
GstElement *test_pipeline, *httpep, *fakesink;

static void
bus_msg_cb (GstBus * bus, GstMessage * msg, gpointer pipeline)
{
  switch (msg->type) {
    case GST_MESSAGE_ERROR:{
      GST_ERROR ("%s bus error: %" GST_PTR_FORMAT, GST_ELEMENT_NAME (pipeline),
          msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "bus_error");
      fail ("Error received on %s bus", GST_ELEMENT_NAME (pipeline));
      break;
    }
    case GST_MESSAGE_WARNING:{
      GST_WARNING ("%s bus: %" GST_PTR_FORMAT, GST_ELEMENT_NAME (pipeline),
          msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "warning");
      break;
    }
    case GST_MESSAGE_STATE_CHANGED:{
      GST_TRACE ("%s bus event: %" GST_PTR_FORMAT, GST_ELEMENT_NAME (pipeline),
          msg);
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
  httpep = gst_element_factory_make ("httpendpoint", NULL);

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
  /* Http end point is not configured yet */
  ck_assert_int_eq (method, KMS_HTTP_ENDPOINT_METHOD_UNDEFINED);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (test_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_entering_main_loop");

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
    static void
get_recv_eos (GstElement * httep, gpointer user_data)
{
  GST_DEBUG ("End of streaming detected. Finishing test");
  g_main_loop_quit (loop);
}

static GstFlowReturn
get_recv_sample (GstElement * appsink, gpointer user_data)
{
  GstSample *sample;
  GstBuffer *buffer;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);

  if (sample == NULL)
    return GST_FLOW_ERROR;

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL)
    GST_WARNING ("No buffer got from sample");
  else
    GST_TRACE ("Got buffer");

  gst_sample_unref (sample);
  return GST_FLOW_OK;
}

GST_START_TEST (check_pull_buffer)
{
  GstElement *videotestsrc, *audiotestsrc, *timeoverlay, *encoder, *aencoder;
  guint bus_watch_id1;
  GstBus *srcbus;

  GST_INFO ("Running test check_pull_buffer");

  loop = g_main_loop_new (NULL, FALSE);

  GST_DEBUG ("Preparing source pipeline");

  /* Create gstreamer elements */
  src_pipeline = gst_pipeline_new ("src-pipeline");
  videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  encoder = gst_element_factory_make ("vp8enc", NULL);
  aencoder = gst_element_factory_make ("vorbisenc", NULL);
  timeoverlay = gst_element_factory_make ("timeoverlay", NULL);
  audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  httpep = gst_element_factory_make ("httpendpoint", NULL);

  GST_DEBUG ("Adding watcher to the pipeline");
  srcbus = gst_pipeline_get_bus (GST_PIPELINE (src_pipeline));

  bus_watch_id1 = gst_bus_add_watch (srcbus, gst_bus_async_signal_func, NULL);
  g_signal_connect (srcbus, "message", G_CALLBACK (bus_msg_cb), src_pipeline);
  g_object_unref (srcbus);

  GST_DEBUG ("Configuring source pipeline");
  gst_bin_add_many (GST_BIN (src_pipeline), videotestsrc, timeoverlay,
      audiotestsrc, encoder, aencoder, httpep, NULL);
  gst_element_link (videotestsrc, timeoverlay);
  gst_element_link (timeoverlay, encoder);
  gst_element_link (audiotestsrc, aencoder);
  gst_element_link_pads (encoder, NULL, httpep, "video_sink");
  gst_element_link_pads (aencoder, NULL, httpep, "audio_sink");

  GST_DEBUG ("Configuring elements");
  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "pattern", 18, "num-buffers", 150, NULL);
  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "num-buffers", 150, NULL);
  g_object_set (G_OBJECT (timeoverlay), "font-desc", "Sans 28", NULL);

  g_signal_connect (httpep, "new-sample", G_CALLBACK (get_recv_sample), NULL);
  g_signal_connect (httpep, "eos", G_CALLBACK (get_recv_eos), NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (src_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "entering_main_loop");

  GST_DEBUG ("Starting pipeline");
  gst_element_set_state (src_pipeline, GST_STATE_PLAYING);

  g_object_get (G_OBJECT (httpep), "http-method", &method, NULL);
  GST_INFO ("Http end point configured as %d", method);
  ck_assert_int_eq (method, KMS_HTTP_ENDPOINT_METHOD_GET);

  /* allow media stream to flow */
  g_object_set (G_OBJECT (httpep), "start", TRUE, NULL);

  g_main_loop_run (loop);

  GST_DEBUG ("Main loop stopped");

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (src_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_main_loop");

  gst_element_set_state (src_pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (src_pipeline));

  g_source_remove (bus_watch_id1);
  g_main_loop_unref (loop);
}

GST_END_TEST
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
  httpep = gst_element_factory_make ("httpendpoint", NULL);
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
  /* Http end point is not configured yet */
  ck_assert_int_eq (method, KMS_HTTP_ENDPOINT_METHOD_UNDEFINED);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (test_pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_entering_main_loop");

  mark_point ();

  g_timeout_add_seconds (WAIT_TIMEOUT, timer_cb, NULL);
  GST_INFO ("Waitig %d second for Agnosticbin to be ready to go to "
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
/******************************/
/* HttpEndpoint test suit */
/******************************/
static Suite *
httpendpoint_suite (void)
{
  Suite *s = suite_create ("httpendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  /* Simulates GET behaviour */
  tcase_add_test (tc_chain, check_pull_buffer);

  /* Simulates POST behaviour */
  tcase_add_test (tc_chain, check_push_buffer);

  /* Simulates POST behaviour with encoded media */
  tcase_add_test (tc_chain, check_emit_encoded_media);

  return s;
}

GST_CHECK_MAIN (httpendpoint);
