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

#include <kmscheck.h>
#include <gst/sdp/gstsdpmessage.h>

#define ITERATIONS 1

static int iterations = ITERATIONS;

static void
create_element (const gchar * element_name)
{
  GstElement *element = gst_element_factory_make (element_name, NULL);

  g_object_unref (element);
}

static void
play_element (const gchar * element_name)
{
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *element = gst_element_factory_make (element_name, NULL);

  gst_bin_add (GST_BIN (pipeline), element);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  gst_element_set_state (pipeline, GST_STATE_NULL);

  g_object_unref (pipeline);
}

static const gchar *pattern_sdp_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=video 0 RTP/AVP 96\r\n"
    "a=rtpmap:96 VP8/90000\r\n"
    "a=sendrecv\r\n"
    "m=audio 0 RTP/AVP 97\r\n" "a=rtpmap:97 OPUS/48000/1\r\n" "a=sendrecv\r\n";

static void
negotiation ()
{
  GstSDPMessage *pattern_sdp;
  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstSDPMessage *offer = NULL, *answer = NULL;
  GstSDPMessage *local_offer = NULL, *local_answer = NULL;
  gchar *local_offer_str, *local_answer_str;
  GstSDPMessage *remote_offer = NULL, *remote_answer = NULL;
  gchar *remote_offer_str, *remote_answer_str;
  gchar *sdp_str = NULL;

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (offerer, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);
  g_object_get (offerer, "pattern-sdp", &pattern_sdp, NULL);
  fail_unless (pattern_sdp != NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (answerer, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);
  g_object_get (answerer, "pattern-sdp", &pattern_sdp, NULL);
  fail_unless (pattern_sdp != NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  g_signal_emit_by_name (offerer, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (answerer, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (offerer, "process-answer", answer);

  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_object_get (offerer, "local-offer-sdp", &local_offer, NULL);
  fail_unless (local_offer != NULL);
  g_object_get (offerer, "remote-answer-sdp", &remote_answer, NULL);
  fail_unless (remote_answer != NULL);

  g_object_get (answerer, "remote-offer-sdp", &remote_offer, NULL);
  fail_unless (remote_offer != NULL);
  g_object_get (answerer, "local-answer-sdp", &local_answer, NULL);
  fail_unless (local_answer != NULL);

  local_offer_str = gst_sdp_message_as_text (local_offer);
  remote_answer_str = gst_sdp_message_as_text (remote_answer);

  remote_offer_str = gst_sdp_message_as_text (remote_offer);
  local_answer_str = gst_sdp_message_as_text (local_answer);

  GST_DEBUG ("Local offer\n%s", local_offer_str);
  GST_DEBUG ("Remote answer\n%s", remote_answer_str);
  GST_DEBUG ("Remote offer\n%s", remote_offer_str);
  GST_DEBUG ("Local answer\n%s", local_answer_str);

  fail_unless (g_strcmp0 (local_offer_str, remote_offer_str) == 0);
  fail_unless (g_strcmp0 (remote_answer_str, local_answer_str) == 0);

  g_free (local_offer_str);
  g_free (remote_answer_str);
  g_free (local_answer_str);
  g_free (remote_offer_str);

  gst_sdp_message_free (local_offer);
  gst_sdp_message_free (remote_answer);
  gst_sdp_message_free (remote_offer);
  gst_sdp_message_free (local_answer);

  g_object_unref (offerer);
  g_object_unref (answerer);
}

GST_START_TEST (test_negotiation)
{
  int i;

  for (i = 0; i < iterations; i++) {
    negotiation ();
  }
}

GST_END_TEST
GST_START_TEST (test_create_rtcpdemux)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("rtcpdemux");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_dtlssrtpdemux)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("dtlssrtpdemux");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_srtpdec)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("srtpdec");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_dtlsdec)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("dtlsdec");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_dtlssrtpdec)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("dtlssrtpdec");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_dtlsenc)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("dtlsenc");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_dtlssrtpenc)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("dtlssrtpenc");
  }
}

KMS_END_TEST
GST_START_TEST (test_create_webrtcendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    create_element ("webrtcendpoint");
  }
}

KMS_END_TEST
GST_START_TEST (test_play_webrtcendpoint)
{
  int i;

  for (i = 0; i < iterations; i++) {
    play_element ("webrtcendpoint");
  }
}

KMS_END_TEST
/*
 * End of test cases
 */
static Suite *
webrtcendpoint_suite (void)
{
  char *it_str;

  it_str = getenv ("ITERATIONS");
  if (it_str != NULL) {
    iterations = atoi (it_str);
    if (iterations <= 0)
      iterations = ITERATIONS;
  }

  Suite *s = suite_create ("webrtcendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, test_create_rtcpdemux);
  tcase_add_test (tc_chain, test_create_dtlssrtpdemux);

  tcase_add_test (tc_chain, test_create_srtpdec);
  tcase_add_test (tc_chain, test_create_dtlsdec);
  tcase_add_test (tc_chain, test_create_dtlssrtpdec);

  tcase_add_test (tc_chain, test_create_dtlsenc);
  tcase_add_test (tc_chain, test_create_dtlssrtpenc);

  tcase_add_test (tc_chain, test_create_webrtcendpoint);
  tcase_add_test (tc_chain, test_play_webrtcendpoint);

  tcase_add_test (tc_chain, test_negotiation);

  return s;
}

KMS_CHECK_MAIN (webrtcendpoint);
