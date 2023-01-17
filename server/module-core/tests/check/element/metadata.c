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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <time.h>

#include "kmsbufferlacentymeta.h"

#define KMS_FACTORY_MAKE_IF_AVAILABLE(factory_name) ({      \
  GstElement *_element;                                     \
  _element = gst_element_factory_make (factory_name, NULL); \
  if (_element == NULL) {                                   \
    GST_WARNING ("Can not create element %s. "              \
      "Test will be canceled", factory_name);               \
    goto test_canceled;                                     \
  }                                                         \
  _element;                                                 \
})

typedef enum
{
  MEDIA_TYPE_AUDIO,
  MEDIA_TYPE_VIDEO,
} MediaType;

typedef struct _MetaUnitTest
{
  MediaType type;
  gchar *enc;
  gchar *pay;
  gchar *depay;
  gchar *parser;
  gchar *dec;
} MetaUnitTest;

static MetaUnitTest test_cases[] = {
  {
        MEDIA_TYPE_VIDEO,
        "avenc_mpeg4",
        NULL,                   /*"rtpmp4vpay", */
        NULL,                   /*"rtpmp4vdepay", */
        "mpeg4videoparse",
      "avdec_mpeg4"},
  {
        MEDIA_TYPE_VIDEO,
        "x264enc",
        "rtph264pay",
        "rtph264depay",
        "h264parse",
      "avdec_h264"},
  {
        MEDIA_TYPE_VIDEO,
        "vp8enc",
        "rtpvp8pay",
        "rtpvp8depay",
        "vp8parse",
      "vp8dec"},
  {
        MEDIA_TYPE_VIDEO,
        "x264enc",
        "rtph264pay",
        "rtph264depay",
        "h264parse",
      "avdec_h264"},
  {
        MEDIA_TYPE_VIDEO,
        "x264enc",
        "rtph264pay",
        "rtph264depay",
        "h264parse",
      "openh264dec"},
  {
        MEDIA_TYPE_VIDEO,
        "openh264enc",
        "rtph264pay",
        "rtph264depay",
        "h264parse",
      "openh264dec"},
  {
        MEDIA_TYPE_VIDEO,
        "openh264enc",
        "rtph264pay",
        "rtph264depay",
        "h264parse",
      "avdec_h264"},
  {
        MEDIA_TYPE_AUDIO,
        "opusenc",
        "rtpopuspay",
        "rtpopusdepay",
        "opusparse",
      "opusdec"},
  {
        MEDIA_TYPE_AUDIO,
        "mulawenc",
        NULL,
        NULL,
        NULL,
      "mulawdec"},
  {
        MEDIA_TYPE_AUDIO,
        "mulawenc",
        "rtppcmupay",
        "rtppcmudepay",
        NULL,
      "mulawdec"},
  {
        MEDIA_TYPE_AUDIO,
        "amrnbenc",
        "rtpamrpay",
        "rtpamrdepay",
        NULL,
      "amrnbdec"},
  {
        MEDIA_TYPE_AUDIO,
        "voamrwbenc",
        NULL,
        NULL,
        NULL,
      "amrwbdec"},
  {
        MEDIA_TYPE_AUDIO,
        "voamrwbenc",
        NULL,
        NULL,
        NULL,
      "avdec_amrwb"}
};

static gboolean
quit_main_loop (gpointer data)
{
  g_main_loop_quit ((GMainLoop *) data);

  return G_SOURCE_REMOVE;
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
    case GST_MESSAGE_STATE_CHANGED:{
      GST_TRACE ("Event: %" GST_PTR_FORMAT, msg);
      break;
    }
    default:
      break;
  }
}

static GstPadProbeReturn
add_metadata_data (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstClockTime time;
  GstBuffer *buffer;

  buffer = GST_PAD_PROBE_INFO_BUFFER (info);

  time = g_get_monotonic_time () * GST_USECOND;

  kms_buffer_add_buffer_latency_meta (buffer, time, TRUE,
      0 /*No matter media type */ );

  return GST_PAD_PROBE_OK;
}

static void
debug_latency_meta (GstPad * pad, GstBuffer * buffer)
{
  KmsBufferLatencyMeta *meta;

  meta = kms_buffer_get_buffer_latency_meta (buffer);

  fail_if (meta == NULL);
  fail_if (!GST_CLOCK_TIME_IS_VALID (meta->ts));

  GST_LOG_OBJECT (pad, "Meta %" GST_TIME_FORMAT, GST_TIME_ARGS (meta->ts));
}

static void
calculate_latency (GstPad * pad, GstBuffer * buffer)
{
  KmsBufferLatencyMeta *meta;
  GstClockTime now;

  meta = kms_buffer_get_buffer_latency_meta (buffer);

  fail_if (meta == NULL);
  fail_if (!GST_CLOCK_TIME_IS_VALID (meta->ts));

  now = g_get_monotonic_time () * GST_USECOND;

  GST_INFO_OBJECT (pad, "got meta %" GST_TIME_FORMAT " at %" GST_TIME_FORMAT
      " (diff: %" GST_STIME_FORMAT ")",
      GST_TIME_ARGS (GST_TIME_AS_USECONDS (meta->ts)),
      GST_TIME_ARGS (GST_TIME_AS_USECONDS (now)),
      GST_STIME_ARGS (GST_CLOCK_DIFF (meta->ts, now)));
}

static GstPadProbeReturn
show_metadata_data (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstBuffer *buffer;

  buffer = GST_PAD_PROBE_INFO_BUFFER (info);

  debug_latency_meta (pad, buffer);

  return GST_PAD_PROBE_OK;
}

static void
handoff_cb (GstElement * fakesink, GstBuffer * buff, GstPad * pad,
    gpointer user_data)
{

  calculate_latency (pad, buff);

  g_idle_add ((GSourceFunc) quit_main_loop, user_data);
  g_signal_handlers_disconnect_by_data (fakesink, user_data);
}

static void
set_probe_on_pad (GstElement * e, const gchar * pad_name,
    GstPadProbeCallback callback)
{
  GstPad *pad;

  pad = gst_element_get_static_pad (e, pad_name);
  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BUFFER, callback, NULL, NULL);
  g_object_unref (pad);
}

static void
check_buffer_latency (MetaUnitTest * test)
{
  GstElement *pipeline, *src, *convert, *tee, *queue, *enc, *pay, *depay,
      *parse, *dec, *fakesink, *tmp;
  guint bus_watch_id;
  GMainLoop *loop;
  GstBus *bus;
  gboolean use_parse, use_payloads;

  src = convert = tee = queue = enc = pay = depay = parse = dec = fakesink =
      NULL;

  tee = KMS_FACTORY_MAKE_IF_AVAILABLE ("tee");
  queue = KMS_FACTORY_MAKE_IF_AVAILABLE ("queue");

  switch (test->type) {
    case MEDIA_TYPE_AUDIO:
      src = KMS_FACTORY_MAKE_IF_AVAILABLE ("audiotestsrc");
      convert = KMS_FACTORY_MAKE_IF_AVAILABLE ("audioconvert");
      break;
    case MEDIA_TYPE_VIDEO:
      src = KMS_FACTORY_MAKE_IF_AVAILABLE ("videotestsrc");
      convert = KMS_FACTORY_MAKE_IF_AVAILABLE ("videoconvert");
      break;
    default:
      fail ("Media type (%d) can not be tested", test->type);
      return;
  }

  use_parse = (test->parser != NULL);
  use_payloads = (test->pay != NULL && test->depay != NULL);

  fail_if (!(use_payloads) && (test->pay != NULL || test->depay != NULL));

  enc = KMS_FACTORY_MAKE_IF_AVAILABLE (test->enc);

  if (use_parse) {
    parse = KMS_FACTORY_MAKE_IF_AVAILABLE (test->parser);
  }

  if (use_payloads) {
    pay = KMS_FACTORY_MAKE_IF_AVAILABLE (test->pay);
    depay = KMS_FACTORY_MAKE_IF_AVAILABLE (test->depay);
  }

  dec = KMS_FACTORY_MAKE_IF_AVAILABLE (test->dec);
  fakesink = KMS_FACTORY_MAKE_IF_AVAILABLE ("fakesink");

  g_object_set (G_OBJECT (src), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (fakesink), "sync", FALSE, "async", FALSE,
      "signal-handoffs", TRUE, NULL);

  set_probe_on_pad (src, "src", (GstPadProbeCallback) add_metadata_data);
  set_probe_on_pad (convert, "sink", (GstPadProbeCallback) show_metadata_data);
  set_probe_on_pad (tee, "sink", (GstPadProbeCallback) show_metadata_data);
  set_probe_on_pad (queue, "sink", (GstPadProbeCallback) show_metadata_data);
  set_probe_on_pad (enc, "sink", (GstPadProbeCallback) show_metadata_data);
  set_probe_on_pad (dec, "sink", (GstPadProbeCallback) show_metadata_data);

  if (use_parse) {
    set_probe_on_pad (parse, "sink", (GstPadProbeCallback) show_metadata_data);
  }

  if (use_payloads) {
    set_probe_on_pad (pay, "sink", (GstPadProbeCallback) show_metadata_data);
    set_probe_on_pad (depay, "sink", (GstPadProbeCallback) show_metadata_data);
  }

  loop = g_main_loop_new (NULL, FALSE);

  pipeline = gst_pipeline_new ("metadata-test");
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  g_signal_connect (fakesink, "handoff", G_CALLBACK (handoff_cb), loop);

  gst_bin_add_many (GST_BIN (pipeline), src, convert, tee, queue,
      enc, dec, fakesink, NULL);

  if (use_payloads) {
    gst_bin_add_many (GST_BIN (pipeline), pay, depay, NULL);
  }

  if (use_parse) {
    gst_bin_add (GST_BIN (pipeline), parse);
  }

  gst_element_link_many (src, convert, tee, queue, enc, NULL);
  tmp = enc;

  if (use_payloads) {
    gst_element_link_many (tmp, pay, depay, NULL);
    tmp = depay;
  }

  if (use_parse) {
    gst_element_link (tmp, parse);
    tmp = parse;
  }

  gst_element_link_many (tmp, dec, fakesink, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG ("Test running");

  g_main_loop_run (loop);

  GST_DEBUG ("Stop executed");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  GST_DEBUG ("Pipe released");

  g_source_remove (bus_watch_id);
  g_main_loop_unref (loop);

  return;

test_canceled:
  /* Some test object is missing */
  g_clear_object (&src);
  g_clear_object (&convert);
  g_clear_object (&tee);
  g_clear_object (&queue);
  g_clear_object (&enc);
  g_clear_object (&pay);
  g_clear_object (&depay);
  g_clear_object (&parse);
  g_clear_object (&dec);
  g_clear_object (&fakesink);
}

GST_START_TEST (check_metadata_enc)
{
  gint i;

  for (i = 0; i < G_N_ELEMENTS (test_cases); i++) {
    check_buffer_latency (&test_cases[i]);
  }
}

GST_END_TEST
/******************************/
/* metadata test suite        */
/******************************/
static Suite *
metadata_suite (void)
{
  Suite *s = suite_create ("metadata");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, check_metadata_enc);

  return s;
}

GST_CHECK_MAIN (metadata);
