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

#include <gst/check/gstcheck.h>
#include <gst/sdp/gstsdpmessage.h>

#include <kmstestutils.h>

#include <commons/kmselementpadtype.h>

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

#define AUDIO_SINK "audio-sink"
#define VIDEO_SINK "video-sink"

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
    GST_DEBUG_OBJECT (pad, "Connecting video stream");
    sink = g_object_get_data (G_OBJECT (element), AUDIO_SINK);
  } else if (g_str_has_prefix (GST_PAD_NAME (pad), KMS_VIDEO_PREFIX)) {
    GST_DEBUG_OBJECT (pad, "Connecting audio stream");
    sink = g_object_get_data (G_OBJECT (element), VIDEO_SINK);
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

  g_signal_emit_by_name (src, "request-new-srcpad", pad_type, NULL, &padname);
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
    const gchar * pattern_sdp_sendonly_str,
    const gchar * pattern_sdp_recvonly_str, gboolean play_after_negotiation)
{
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *audio_enc = gst_element_factory_make (audio_enc_name, NULL);
  GstElement *rtpendpointsender =
      gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *rtpendpointreceiver =
      gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *outputfakesink = gst_element_factory_make ("fakesink", NULL);

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  mark_point ();
  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendonly_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (rtpendpointsender, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  mark_point ();
  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_recvonly_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (rtpendpointreceiver, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  mark_point ();
  hod = g_slice_new (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (outputfakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (outputfakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), hod);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), rtpendpointsender);
  connect_sink_async (rtpendpointsender, audiotestsrc, audio_enc,
      NULL, pipeline, "sink_audio");

  gst_bin_add (GST_BIN (pipeline), rtpendpointreceiver);

  if (!play_after_negotiation)
    gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (rtpendpointsender, "generate-offer", &offer);
  fail_unless (offer != NULL);

  mark_point ();
  g_signal_emit_by_name (rtpendpointreceiver, "process-offer", offer, &answer);
  fail_unless (answer != NULL);

  mark_point ();
  g_signal_emit_by_name (rtpendpointsender, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add (GST_BIN (pipeline), outputfakesink);
  g_object_set_data (G_OBJECT (rtpendpointreceiver), AUDIO_SINK,
      outputfakesink);
  g_signal_connect (rtpendpointreceiver, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (rtpendpointreceiver,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  if (play_after_negotiation)
    gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_sendonly_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_sendonly_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
  g_slice_free (HandOffData, hod);
}

#define OFFERER_RECEIVES_AUDIO "offerer_receives_audio"
#define ANSWERER_RECEIVES_AUDIO "answerer_receives_audio"

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
  if (GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
              ANSWERER_RECEIVES_AUDIO))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_data (G_OBJECT (pipeline), OFFERER_RECEIVES_AUDIO,
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
  if (GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
              OFFERER_RECEIVES_AUDIO))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_data (G_OBJECT (pipeline), ANSWERER_RECEIVES_AUDIO,
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
test_audio_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps expected_caps, const gchar * pattern_sdp_sendrcv_str)
{
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
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
  GstElement *rtpendpoint_offerer =
      gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *rtpendpoint_answerer =
      gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *fakesink_offerer = gst_element_factory_make ("fakesink", NULL);
  GstElement *fakesink_answerer = gst_element_factory_make ("fakesink", NULL);

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendrcv_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (rtpendpoint_offerer, "pattern-sdp", pattern_sdp, NULL);
  g_object_set (rtpendpoint_answerer, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  hod = g_slice_new (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (fakesink_offerer), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_offerer_fakesink_hand_off), hod);
  g_object_set (G_OBJECT (fakesink_answerer), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_answerer), "handoff",
      G_CALLBACK (sendrecv_answerer_fakesink_hand_off), hod);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), rtpendpoint_offerer);
  connect_sink_async (rtpendpoint_offerer, audiotestsrc_offerer,
      audio_enc_offerer, NULL, pipeline, "sink_audio");

  gst_bin_add (GST_BIN (pipeline), rtpendpoint_answerer);
  connect_sink_async (rtpendpoint_answerer, audiotestsrc_answerer,
      audio_enc_answerer, NULL, pipeline, "sink_audio");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (rtpendpoint_offerer, "generate-offer", &offer);
  fail_unless (offer != NULL);

  mark_point ();
  g_signal_emit_by_name (rtpendpoint_answerer, "process-offer", offer, &answer);
  fail_unless (answer != NULL);

  mark_point ();
  g_signal_emit_by_name (rtpendpoint_offerer, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add (GST_BIN (pipeline), fakesink_offerer);
  g_object_set_data (G_OBJECT (rtpendpoint_offerer), AUDIO_SINK,
      fakesink_offerer);
  g_signal_connect (rtpendpoint_offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (rtpendpoint_offerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  gst_bin_add (GST_BIN (pipeline), fakesink_answerer);
  g_object_set_data (G_OBJECT (rtpendpoint_answerer), AUDIO_SINK,
      fakesink_answerer);
  g_signal_connect (rtpendpoint_answerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (rtpendpoint_answerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_sendrecv_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);

  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_main_loop_unref (loop);
  g_object_unref (pipeline);
  g_slice_free (HandOffData, hod);
}

/* OPUS tests */

static GstStaticCaps opus_expected_caps = GST_STATIC_CAPS ("audio/x-opus");

static const gchar *pattern_sdp_opus_sendonly_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=audio 0 RTP/AVP 96\r\n" "a=rtpmap:96 OPUS/48000/1\r\n" "a=sendonly\r\n";

static const gchar *pattern_sdp_opus_recvonly_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=audio 0 RTP/AVP 96\r\n" "a=rtpmap:96 OPUS/48000/1\r\n" "a=recvonly\r\n";

static const gchar *pattern_sdp_opus_sendrecv_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=audio 0 RTP/AVP 96\r\n" "a=rtpmap:96 OPUS/48000/1\r\n" "a=sendrecv\r\n";

static void
test_opus_sendonly (gboolean play_after_negotiation)
{
  test_audio_sendonly ("opusenc", opus_expected_caps,
      pattern_sdp_opus_sendonly_str, pattern_sdp_opus_recvonly_str,
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

GST_END_TEST
GST_START_TEST (test_opus_sendrecv)
{
  test_audio_sendrecv ("opusenc", opus_expected_caps,
      pattern_sdp_opus_sendrecv_str);
}

GST_END_TEST
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

  return s;
}

GST_CHECK_MAIN (rtpendpoint_audio_test);
