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
#include <gst/check/gstharness.h>
#include <gst/rtp/gstrtpbuffer.h>
#include <commons/constants.h>

/* Test based on jitterbuffer tests */

#define PCMU_BUF_CLOCK_RATE 8000
#define PCMU_BUF_PT 8
#define PCMU_BUF_SSRC 11223344
#define PCMU_BUF_MS  20
#define PCMU_BUF_DURATION (PCMU_BUF_MS * GST_MSECOND)
#define PCMU_BUF_SIZE (64000 * PCMU_BUF_MS / 1000)
#define PCMU_RTP_TS_DURATION (PCMU_BUF_CLOCK_RATE * PCMU_BUF_MS / 1000)

/* based on rtpjitterbuffer.c */
static GstCaps *
generate_caps (void)
{
  return gst_caps_new_simple ("application/x-rtp",
      "media", G_TYPE_STRING, "audio",
      "clock-rate", G_TYPE_INT, PCMU_BUF_CLOCK_RATE,
      "encoding-name", G_TYPE_STRING, "PCMU",
      "payload", G_TYPE_INT, PCMU_BUF_PT,
      "ssrc", G_TYPE_UINT, PCMU_BUF_SSRC, NULL);
}

/* based on rtpjitterbuffer.c */
static GstBuffer *
generate_test_buffer_full (GstClockTime gst_ts,
    gboolean marker_bit, guint seq_num, guint32 rtp_ts)
{
  GstBuffer *buf;
  guint8 *payload;
  guint i;
  GstRTPBuffer rtp = GST_RTP_BUFFER_INIT;

  buf = gst_rtp_buffer_new_allocate (PCMU_BUF_SIZE, 0, 0);
  GST_BUFFER_DTS (buf) = gst_ts;
  GST_BUFFER_PTS (buf) = gst_ts;

  gst_rtp_buffer_map (buf, GST_MAP_READWRITE, &rtp);

  gst_rtp_buffer_set_payload_type (&rtp, PCMU_BUF_PT);
  gst_rtp_buffer_set_marker (&rtp, marker_bit);
  gst_rtp_buffer_set_seq (&rtp, seq_num);
  gst_rtp_buffer_set_timestamp (&rtp, rtp_ts);
  gst_rtp_buffer_set_ssrc (&rtp, PCMU_BUF_SSRC);

  payload = gst_rtp_buffer_get_payload (&rtp);
  for (i = 0; i < PCMU_BUF_SIZE; i++) {
    payload[i] = (guint8)i;
  }

  gst_rtp_buffer_unmap (&rtp);

  return buf;
}

static GstBuffer *
generate_test_buffer (guint seq_num)
{
  return generate_test_buffer_full (seq_num * PCMU_BUF_DURATION,
      TRUE, seq_num, seq_num * PCMU_RTP_TS_DURATION);
}

static gboolean
compare_test_buffers (GstBuffer *buf1, GstBuffer *buf2)
{
  GstRTPBuffer rtp1 = GST_RTP_BUFFER_INIT;
  GstRTPBuffer rtp2 = GST_RTP_BUFFER_INIT;
  gboolean same = FALSE;

  gst_rtp_buffer_map (buf1, GST_MAP_READ, &rtp1);
  gst_rtp_buffer_map (buf2, GST_MAP_READ, &rtp2);

  same =
      (gst_rtp_buffer_get_payload_type (&rtp1)
          == gst_rtp_buffer_get_payload_type (&rtp2))
      && (gst_rtp_buffer_get_marker (&rtp1)
          == gst_rtp_buffer_get_marker (&rtp2))
      && (gst_rtp_buffer_get_seq (&rtp1)
          == gst_rtp_buffer_get_seq (&rtp2))
      && (gst_rtp_buffer_get_timestamp (&rtp1)
          == gst_rtp_buffer_get_timestamp (&rtp2))
      && (gst_rtp_buffer_get_ssrc (&rtp1)
          == gst_rtp_buffer_get_ssrc (&rtp2))
      && (memcmp (gst_rtp_buffer_get_payload (&rtp1),
          gst_rtp_buffer_get_payload (&rtp2),
          gst_rtp_buffer_get_payload_len (&rtp1)) == 0);

  gst_rtp_buffer_unmap (&rtp1);
  gst_rtp_buffer_unmap (&rtp2);

  return same;
}

/* Check the limits of the replay protection window.
 *
 * When the sequence number is too old and falls outside the replay window,
 * libsrtp should warn with:
 *
 *     Unable to protect buffer (protect failed) code 10
 *
 * in GstSrtpEnc, where "code 10" corresponds to
 *
 *     srtp_err_status_replay_old = 10, replay check failed (index too old)
 *
 * in libsrtp (from libsrtp/include/srtp.h).
 */
GST_START_TEST (test_window_size)
{
  GstElement *srtpenc = gst_element_factory_make ("srtpenc", NULL);
  GstHarness *h;
  GstBuffer *in_buf;
  GstBuffer *out_buf;
  GstFlowReturn ret;

  // Prepare srtp enc
  // libsrtp window size: 32767 (G_MAXINT16)
  g_object_set (srtpenc, "random-key", TRUE, "replay-window-size", 32767, NULL);

  // Prepare test harness
  h = gst_harness_new_with_element (srtpenc, "rtp_sink_0", "rtp_src_0");
  gst_harness_set_src_caps (h, generate_caps ());

  // ===================================================
  // libsrtp minimum sequence number = Last - WindowSize
  // ===================================================

  // Current SRTP window: [0, 32767]
  // RTP sequence number: 32768; this is "in the future", so OK
  in_buf = generate_test_buffer (32768);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  // Current SRTP window: [1, 32768]
  // RTP sequence number: 512; this is inside the window, so OK
  in_buf = generate_test_buffer (512);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  // Current SRTP window: [1, 32768]
  // RTP sequence number: 1; this is inside the window, so OK
  in_buf = generate_test_buffer (1);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  // Current SRTP window: [1, 32768]
  // RTP sequence number: 0; this is before the window, so not OK (too old)
  in_buf = generate_test_buffer (0);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_ERROR);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf == NULL);

  // Current SRTP window: [1, 32768]
  // RTP sequence number: 32770; this is "in the future", so OK
  in_buf = generate_test_buffer (32770);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  // Current SRTP window: [3, 32770]
  // RTP sequence number: 2; this is before the window, so not OK (too old)
  in_buf = generate_test_buffer (2);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_ERROR);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf == NULL);

  // Current SRTP window: [3, 32770]
  // RTP sequence number: 3; this is inside the window, so OK
  in_buf = generate_test_buffer (3);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  gst_harness_teardown (h);
  g_object_unref (srtpenc);
}
GST_END_TEST

/* Force sending repeated packets, which causes this warning:
 *
 *     Unable to protect buffer (protect failed) code 9
 *
 * in GstSrtpEnc, where "code 9" corresponds to
 *
 *     srtp_err_status_replay_fail = 9, replay check failed (bad index)
 *
 * in libsrtp (from libsrtp/include/srtp.h).
 */
GST_START_TEST (test_replay_tx_with_allow_repeat_tx_false)
{
  GstElement *srtpenc = gst_element_factory_make ("srtpenc", NULL);
  GstHarness *h;
  GstBuffer *in_buf;
  GstBuffer *out_buf;
  GstFlowReturn ret;

  // Prepare srtp enc
  g_object_set (srtpenc, "random-key", TRUE, "allow-repeat-tx", FALSE, NULL);

  // Prepare test harness
  h = gst_harness_new_with_element (srtpenc, "rtp_sink_0", "rtp_src_0");
  gst_harness_set_src_caps (h, generate_caps ());

  in_buf = generate_test_buffer (0);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (1);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (0);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_ERROR);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf == NULL);

  in_buf = generate_test_buffer (1);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_ERROR);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf == NULL);

  in_buf = generate_test_buffer (2);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (2);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_ERROR);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf == NULL);

  gst_harness_teardown (h);
  g_object_unref (srtpenc);
}
GST_END_TEST

/* Force sending repeated packets, which causes this warning:
 *
 *     Unable to protect buffer (protect failed) code 9
 *
 * in GstSrtpEnc, where "code 9" corresponds to
 *
 *     srtp_err_status_replay_fail = 9, replay check failed (bad index)
 *
 * in libsrtp (from libsrtp/include/srtp.h).
 *
 * However, when 'allow-repeat-tx = true', this condition shouldn't be
 * forbidden and repeated sequence numbers should be accepted by libsrtp.
 */
GST_START_TEST (test_replay_tx_with_allow_repeat_tx_true)
{
  GstElement *srtpenc = gst_element_factory_make ("srtpenc", NULL);
  GstHarness *h;
  GstBuffer *in_buf;
  GstBuffer *out_buf;
  GstFlowReturn ret;

  // Prepare srtp enc
  g_object_set (srtpenc, "random-key", TRUE, "allow-repeat-tx", TRUE, NULL);

  // Prepare test harness
  h = gst_harness_new_with_element (srtpenc, "rtp_sink_0", "rtp_src_0");
  gst_harness_set_src_caps (h, generate_caps ());

  in_buf = generate_test_buffer (0);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (1);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (0);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (1);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (2);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  in_buf = generate_test_buffer (2);
  ret = gst_harness_push (h, in_buf);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  gst_buffer_unref (out_buf);

  gst_harness_teardown (h);
  g_object_unref (srtpenc);
}
GST_END_TEST

/*
 * SRTP Master Key (30 bytes):
 * - ASCII: `ABCDEFGHIJKLMNOPQRSTUVWXYZ1234`
 * - Hex (gst-launch): `4142434445464748494A4B4C4D4E4F505152535455565758595A31323334`
 */
static char *srtp_key = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234";

static GstCaps *
srtpdec_request_key (void)
{
  GstBuffer *key = gst_buffer_new_wrapped (g_strdup (srtp_key),
      strlen (srtp_key));
  GstCaps *caps = gst_caps_new_simple ("application/x-srtp",
      "payload", G_TYPE_INT, PCMU_BUF_PT,
      "ssrc", G_TYPE_UINT, PCMU_BUF_SSRC,
      "srtp-key", GST_TYPE_BUFFER, key,
      "srtp-cipher", G_TYPE_STRING, "aes-128-icm",
      "srtp-auth", G_TYPE_STRING, "hmac-sha1-80",
      "srtcp-cipher", G_TYPE_STRING, "aes-128-icm",
      "srtcp-auth", G_TYPE_STRING, "hmac-sha1-80",
      NULL);
  gst_buffer_unref (key);
  return caps;
}

/* Force receiving repeated packets, which causes this warning:
 *
 *     Unable to unprotect buffer (unprotect failed code 9)
 *
 * in GstSrtpDec, where "code 9" corresponds to
 *
 *     srtp_err_status_replay_fail = 9, replay check failed (bad index)
 *
 * in libsrtp (from libsrtp/include/srtp.h).
 *
 * However, with Kurento's fork of libsrtp, the srtp_err_status_replay_fail
 * error on the receiver side should be ignored, similarly to how
 * 'allow-repeat-tx = true' allows repeated packets on the transmitter side.
 */
GST_START_TEST (test_replay_rx_with_libsrtp_fork)
{
  GstElement *srtpenc = gst_element_factory_make ("srtpenc", NULL);
  GstElement *srtpdec = gst_element_factory_make ("srtpdec", NULL);
  GstHarness *src_h;
  GstHarness *h;
  GstBuffer *in_buf;
  GstBuffer *out_buf;
  GstFlowReturn ret;

  // Prepare srtp enc and dec
  GstBuffer *key = gst_buffer_new_wrapped (g_strdup (srtp_key),
      strlen (srtp_key));
  g_object_set (srtpenc, "key", key, "allow-repeat-tx", TRUE, NULL);
  gst_buffer_unref (key);
  g_signal_connect (srtpdec, "request-key", G_CALLBACK (srtpdec_request_key),
      NULL);

  // Prepare test harness
  src_h = gst_harness_new_with_element (srtpenc, "rtp_sink_0", "rtp_src_0");
  h = gst_harness_new_with_element (srtpdec, "rtp_sink", "rtp_src");
  gst_harness_set_src_caps (src_h, generate_caps ());
  gst_harness_add_src_harness (h, src_h, FALSE);

  // First run: Send normal packet
  in_buf = generate_test_buffer (15);
  ret = gst_harness_push (src_h, gst_buffer_copy (in_buf));
  fail_unless (ret == GST_FLOW_OK);
  ret = gst_harness_push_from_src (h);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  fail_unless (compare_test_buffers (in_buf, out_buf));
  gst_buffer_unref (in_buf);
  gst_buffer_unref (out_buf);

  // Second run: Repeat sequence number; should raise 'srtp_err_status_replay_fail'
  // but the Kurento fork should allow it to be processed (not dropped)
  in_buf = generate_test_buffer (15);
  ret = gst_harness_push (src_h, gst_buffer_copy (in_buf));
  fail_unless (ret == GST_FLOW_OK);
  ret = gst_harness_push_from_src (h);
  fail_unless (ret == GST_FLOW_OK);
  out_buf = gst_harness_try_pull (h);
  fail_unless (out_buf != NULL);
  fail_unless (compare_test_buffers (in_buf, out_buf));
  gst_buffer_unref (in_buf);
  gst_buffer_unref (out_buf);

  gst_harness_teardown (h);
  g_object_unref (srtpdec);
  g_object_unref (srtpenc);
}
GST_END_TEST

static Suite *
srtp_suite (void)
{
  Suite *s = suite_create ("srtp");
  TCase *tc_chain = tcase_create ("general");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, test_window_size);
  tcase_add_test (tc_chain, test_replay_tx_with_allow_repeat_tx_false);
  tcase_add_test (tc_chain, test_replay_tx_with_allow_repeat_tx_true);
  tcase_add_test (tc_chain, test_replay_rx_with_libsrtp_fork);

  return s;
}

GST_CHECK_MAIN (srtp)
