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
#include <gst/sdp/gstsdpmessage.h>

#include <kmstestutils.h>

#include <commons/kmselementpadtype.h>

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

#define SINK_AUDIO_STREAM "sink_audio_default"

#define AUDIO_SINK "audio-sink"
G_DEFINE_QUARK (AUDIO_SINK, audio_sink);

#define OFFERER_RECEIVES_AUDIO "offerer-receives-audio"
G_DEFINE_QUARK (OFFERER_RECEIVES_AUDIO, offerer_receives_audio);

#define ANSWERER_RECEIVES_AUDIO "answerer-receives-audio"
G_DEFINE_QUARK (ANSWERER_RECEIVES_AUDIO, answerer_receives_audio);

#define KMS_RTP_SDES_CRYPTO_SUITE_AES_128_CM_HMAC_SHA1_32 0
#define KMS_RTP_SDES_CRYPTO_SUITE_AES_128_CM_HMAC_SHA1_80 1
#define KMS_RTP_SDES_CRYPTO_SUITE_AES_256_CM_HMAC_SHA1_32 2
#define KMS_RTP_SDES_CRYPTO_SUITE_AES_256_CM_HMAC_SHA1_80 3
#define KMS_RTP_SDES_CRYPTO_SUITE_NONE 4

#define SDES_30_BYTES_KEY "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5"
#define SDES_46_BYTES_KEY "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NQ=="

static gboolean
print_timedout_pipeline (gpointer data)
{
  GstElement *pipeline = GST_ELEMENT (data);
  gchar *name;
  gchar *pipeline_name;

  GST_WARNING_OBJECT (pipeline, "Timed out test");
  pipeline_name = gst_element_get_name (pipeline);
  name = g_strdup_printf ("%s_timedout", pipeline_name);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, name);

  g_free (name);
  g_free (pipeline_name);

  return FALSE;
}

static GArray *
create_codecs_array (gchar * codecs[])
{
  GArray *a = g_array_new (FALSE, TRUE, sizeof (GValue));
  int i;

  for (i = 0; i < g_strv_length (codecs); i++) {
    GValue v = G_VALUE_INIT;
    GstStructure *s;

    g_value_init (&v, GST_TYPE_STRUCTURE);
    s = gst_structure_new (codecs[i], NULL, NULL);
    gst_value_set_structure (&v, s);
    gst_structure_free (s);
    g_array_append_val (a, v);
  }

  return a;
}

static gboolean
quit_main_loop_idle (gpointer data)
{
  GMainLoop *loop = data;

  g_main_loop_quit (loop);
  return FALSE;
}

// TODO: create a generic bus_msg
static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, "error");
      fail ("Error received on bus");
      break;
    }
    case GST_MESSAGE_WARNING:{
      GST_WARNING ("Warning: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, "warning");
      break;
    }
    default:
      break;
  }
}

typedef struct _KmsConnectData
{
  GstElement *src;
  GstElement *enc;
  GstElement *caps;
  GstBin *pipe;
  const gchar *pad_prefix;
  gulong id;
} KmsConnectData;

static void
connect_sink (GstElement * element, GstPad * pad, gpointer user_data)
{
  KmsConnectData *data = user_data;

  GST_DEBUG_OBJECT (pad, "New pad %" GST_PTR_FORMAT, element);

  if (!g_str_has_prefix (GST_OBJECT_NAME (pad), data->pad_prefix)) {
    return;
  }

  gst_bin_add_many (GST_BIN (data->pipe), data->src, data->enc, NULL);

  if (data->caps != NULL) {
    gst_bin_add (GST_BIN (data->pipe), data->caps);
    gst_element_link_many (data->src, data->caps, data->enc, NULL);
  } else {
    gst_element_link_many (data->src, data->enc, NULL);
  }

  gst_element_link_pads (data->enc, NULL, element, GST_OBJECT_NAME (pad));
  gst_element_sync_state_with_parent (data->enc);

  if (data->caps != NULL) {
    gst_element_sync_state_with_parent (data->caps);
  }

  gst_element_sync_state_with_parent (data->src);

  GST_INFO_OBJECT (pad, "Linking %s", data->pad_prefix);

  if (data->id != 0) {
    g_signal_handler_disconnect (element, data->id);
  }
}

static void
kms_connect_data_destroy (gpointer data)
{
  g_slice_free (KmsConnectData, data);
}

static void
connect_sink_on_srcpad_added (GstElement * element, GstPad * pad,
    gpointer user_data)
{
  GstElement *sink;
  GstPad *sinkpad;

  if (g_str_has_prefix (GST_PAD_NAME (pad), KMS_AUDIO_PREFIX)) {
    GST_DEBUG_OBJECT (pad, "Connecting audio stream");
    sink = g_object_get_qdata (G_OBJECT (element), audio_sink_quark ());
  } else if (g_str_has_prefix (GST_PAD_NAME (pad), KMS_VIDEO_PREFIX)) {
    GST_ERROR_OBJECT (pad, "Not connecting video stream, it is not expected");
    return;
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

  g_signal_emit_by_name (src, "request-new-pad", pad_type, NULL, GST_PAD_SRC,
      &padname);
  if (padname == NULL) {
    return FALSE;
  }

  GST_DEBUG_OBJECT (src, "Requested pad %s", padname);
  g_free (padname);

  return TRUE;
}

static void
connect_sink_async (GstElement * webrtcendpoint, GstElement * src,
    GstElement * enc, GstElement * caps, GstElement * pipe,
    const gchar * pad_prefix)
{
  KmsConnectData *data = g_slice_new (KmsConnectData);

  data->src = src;
  data->enc = enc;
  data->caps = caps;
  data->pipe = GST_BIN (pipe);
  data->pad_prefix = pad_prefix;

  data->id =
      g_signal_connect_data (webrtcendpoint, "pad-added",
      G_CALLBACK (connect_sink), data,
      (GClosureNotify) kms_connect_data_destroy, 0);
}

typedef struct HandOffData
{
  GMainLoop *loop;
  GstStaticCaps expected_caps;
} HandOffData;

static void
check_caps (GstPad * pad, HandOffData * hod)
{
  GstCaps *caps, *expected_caps;
  gboolean is_subset = FALSE;

  caps = gst_pad_get_current_caps (pad);

  if (caps == NULL) {
    return;
  }

  expected_caps = gst_static_caps_get (&hod->expected_caps);

  is_subset = gst_caps_is_subset (caps, expected_caps);
  GST_DEBUG ("expected caps: %" GST_PTR_FORMAT ", caps: %" GST_PTR_FORMAT
      ", is subset: %d", expected_caps, caps, is_subset);
  gst_caps_unref (expected_caps);
  gst_caps_unref (caps);

  fail_unless (is_subset);
}

static void
fakesink_hand_off (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  HandOffData *hod = (HandOffData *) data;

  check_caps (pad, hod);
  g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
  g_idle_add (quit_main_loop_idle, hod->loop);
}

static void
test_audio_sendonly (const gchar * audio_enc_name, GstStaticCaps expected_caps,
    gchar * codec, gboolean play_after_negotiation)
{
  GArray *codecs_array;
  gchar *codecs[] = { codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *sender_sess_id, *receiver_sess_id;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *audio_enc = gst_element_factory_make (audio_enc_name, NULL);
  GstElement *rtpendpointsender =
      gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *rtpendpointreceiver =
      gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *outputfakesink = gst_element_factory_make ("fakesink", NULL);
  gboolean answer_ok;
  guint id;

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  mark_point ();
  codecs_array = create_codecs_array (codecs);
  g_object_set (rtpendpointsender, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (codecs_array), NULL);
  g_object_set (rtpendpointreceiver, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (codecs_array), NULL);
  g_array_unref (codecs_array);

  mark_point ();
  hod = g_slice_new (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (outputfakesink), "signal-handoffs", TRUE,
      "sync", FALSE, "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (outputfakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), hod);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), rtpendpointsender);
  connect_sink_async (rtpendpointsender, audiotestsrc, audio_enc,
      NULL, pipeline, SINK_AUDIO_STREAM);

  gst_bin_add (GST_BIN (pipeline), rtpendpointreceiver);

  if (!play_after_negotiation)
    gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* Session creation */
  g_signal_emit_by_name (rtpendpointsender, "create-session", &sender_sess_id);
  GST_DEBUG_OBJECT (rtpendpointsender, "Created session with id '%s'",
      sender_sess_id);
  g_signal_emit_by_name (rtpendpointreceiver, "create-session",
      &receiver_sess_id);
  GST_DEBUG_OBJECT (rtpendpointreceiver, "Created session with id '%s'",
      receiver_sess_id);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (rtpendpointsender, "generate-offer", sender_sess_id,
      &offer);
  fail_unless (offer != NULL);

  mark_point ();
  g_signal_emit_by_name (rtpendpointreceiver, "process-offer", receiver_sess_id,
      offer, &answer);
  fail_unless (answer != NULL);

  mark_point ();
  g_signal_emit_by_name (rtpendpointsender, "process-answer", sender_sess_id,
      answer, &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add (GST_BIN (pipeline), outputfakesink);
  g_object_set_qdata (G_OBJECT (rtpendpointreceiver), audio_sink_quark (),
      outputfakesink);
  g_signal_connect (rtpendpointreceiver, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (rtpendpointreceiver,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  if (play_after_negotiation)
    gst_element_set_state (pipeline, GST_STATE_PLAYING);

  id = g_timeout_add_seconds (3, print_timedout_pipeline, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (id);

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
  g_slice_free (HandOffData, hod);
  g_free (sender_sess_id);
  g_free (receiver_sess_id);
}

G_LOCK_DEFINE_STATIC (check_receive_lock);

static void
sendrecv_offerer_fakesink_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer data)
{
  HandOffData *hod = (HandOffData *) data;
  GstElement *pipeline;

  check_caps (pad, hod);

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              answerer_receives_audio_quark ()))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), offerer_receives_audio_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
sendrecv_answerer_fakesink_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer data)
{
  HandOffData *hod = (HandOffData *) data;
  GstElement *pipeline;

  check_caps (pad, hod);

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              offerer_receives_audio_quark ()))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), answerer_receives_audio_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
test_audio_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps expected_caps, gchar * codec, guint crypto, const gchar * key,
    gboolean use_ipv6)
{
  GArray *codecs_array;
  gchar *codecs[] = { codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *offerer_sess_id, *answerer_sess_id;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *audiotestsrc_offerer =
      gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *audiotestsrc_answerer =
      gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *audio_enc_offerer =
      gst_element_factory_make (audio_enc_name, NULL);
  GstElement *audio_enc_answerer =
      gst_element_factory_make (audio_enc_name, NULL);
  GstElement *offerer = gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *fakesink_offerer = gst_element_factory_make ("fakesink", NULL);
  GstElement *fakesink_answerer = gst_element_factory_make ("fakesink", NULL);
  gboolean answer_ok;
  guint id;

  if (crypto != KMS_RTP_SDES_CRYPTO_SUITE_NONE) {
    /* Use random key */
    g_object_set (offerer, "crypto-suite", crypto, NULL);
    g_object_set (answerer, "crypto-suite", crypto, NULL);
  }

  if (key != NULL) {
    g_object_set (offerer, "master-key", key, NULL);
    g_object_set (answerer, "master-key", key, NULL);
  }

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  codecs_array = create_codecs_array (codecs);
  g_object_set (offerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (codecs_array), "use-ipv6", use_ipv6, NULL);
  g_object_set (answerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (codecs_array), "use-ipv6", use_ipv6, NULL);
  g_array_unref (codecs_array);

  hod = g_slice_new (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (audiotestsrc_offerer), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (audiotestsrc_answerer), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (fakesink_offerer), "signal-handoffs", TRUE,
      "sync", FALSE, "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_offerer_fakesink_hand_off), hod);
  g_object_set (G_OBJECT (fakesink_answerer), "signal-handoffs", TRUE,
      "sync", FALSE, "async", FALSE, NULL);
  g_signal_connect (G_OBJECT (fakesink_answerer), "handoff",
      G_CALLBACK (sendrecv_answerer_fakesink_hand_off), hod);

  g_object_set_qdata (G_OBJECT (offerer), audio_sink_quark (),
      fakesink_offerer);
  g_signal_connect (offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  g_object_set_qdata (G_OBJECT (answerer), audio_sink_quark (),
      fakesink_answerer);
  g_signal_connect (answerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  connect_sink_async (offerer, audiotestsrc_offerer,
      audio_enc_offerer, NULL, pipeline, SINK_AUDIO_STREAM);

  connect_sink_async (answerer, audiotestsrc_answerer,
      audio_enc_answerer, NULL, pipeline, SINK_AUDIO_STREAM);

  /* Add elements */
  gst_bin_add_many (GST_BIN (pipeline), offerer, answerer, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", answerer_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

  id = g_timeout_add_seconds (3, print_timedout_pipeline, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  g_source_remove (id);

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
  g_slice_free (HandOffData, hod);
  g_free (offerer_sess_id);
  g_free (answerer_sess_id);
}

/* OPUS tests */

static GstStaticCaps opus_expected_caps = GST_STATIC_CAPS ("audio/x-opus");

static void
test_opus_sendonly (gboolean play_after_negotiation)
{
  test_audio_sendonly ("opusenc", opus_expected_caps, "OPUS/48000/1",
      play_after_negotiation);
}

GST_START_TEST (test_opus_sendonly_play_before_negotiation)
{
  test_opus_sendonly (FALSE);
}

GST_END_TEST
GST_START_TEST (test_opus_sendonly_play_after_negotiation)
{
  test_opus_sendonly (TRUE);
}

GST_END_TEST;

void
do_opus_tests (gboolean use_ipv6)
{
  test_audio_sendrecv ("opusenc", opus_expected_caps, "OPUS/48000/1",
      KMS_RTP_SDES_CRYPTO_SUITE_NONE, NULL, use_ipv6);
  test_audio_sendrecv ("opusenc", opus_expected_caps, "OPUS/48000/1",
      KMS_RTP_SDES_CRYPTO_SUITE_AES_128_CM_HMAC_SHA1_32, SDES_30_BYTES_KEY,
      use_ipv6);
  test_audio_sendrecv ("opusenc", opus_expected_caps, "OPUS/48000/1",
      KMS_RTP_SDES_CRYPTO_SUITE_AES_128_CM_HMAC_SHA1_80, SDES_30_BYTES_KEY,
      use_ipv6);
  test_audio_sendrecv ("opusenc", opus_expected_caps, "OPUS/48000/1",
      KMS_RTP_SDES_CRYPTO_SUITE_AES_256_CM_HMAC_SHA1_32, SDES_46_BYTES_KEY,
      use_ipv6);
  test_audio_sendrecv ("opusenc", opus_expected_caps, "OPUS/48000/1",
      KMS_RTP_SDES_CRYPTO_SUITE_AES_256_CM_HMAC_SHA1_80, SDES_46_BYTES_KEY,
      use_ipv6);
}

GST_START_TEST (test_opus_sendrecv)
{
  do_opus_tests (FALSE);
}

GST_END_TEST;
GST_START_TEST (test_opus_sendrecv_ipv6)
{
  do_opus_tests (TRUE);
}

GST_END_TEST;
/*
 * End of test cases
 */
static Suite *
rtpendpoint_audio_test_suite (void)
{
  Suite *s = suite_create ("rtpendpoint_audio");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, test_opus_sendonly_play_before_negotiation);
  tcase_add_test (tc_chain, test_opus_sendonly_play_after_negotiation);
  tcase_add_test (tc_chain, test_opus_sendrecv);
  tcase_add_test (tc_chain, test_opus_sendrecv_ipv6);

  return s;
}

GST_CHECK_MAIN (rtpendpoint_audio_test);
