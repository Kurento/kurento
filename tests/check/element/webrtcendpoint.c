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
#include <webrtcendpoint/kmsicecandidate.h>

#include <commons/kmselementpadtype.h>

#include <nice/address.h>
#include <nice/interfaces.h>

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

#define AUDIO_SINK "audio-sink"
G_DEFINE_QUARK (AUDIO_SINK, audio_sink);

#define VIDEO_SINK "video-sink"
G_DEFINE_QUARK (VIDEO_SINK, video_sink);

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#define SINK_VIDEO_STREAM "sink_video_default"
#define SINK_AUDIO_STREAM "sink_audio_default"

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

typedef struct HandOffData
{
  GQuark type;
  GMainLoop *loop;
  GstStaticCaps expected_caps;

  /* Check KmsBaseRtpEp::request_local_key_frame begin */
  gboolean check_request_local_key_frame;
  guint count;
  GstElement *webrtcep;
  /* Check KmsBaseRtpEp::request_local_key_frame end */
} HandOffData;

typedef struct OnIceCandidateData
{
  GstElement *peer;
  gchar *peer_sess_id;
} OnIceCandidateData;

static void
on_ice_candidate (GstElement * self, gchar * sess_id,
    KmsIceCandidate * candidate, OnIceCandidateData * data)
{
  gboolean ret;

  g_signal_emit_by_name (data->peer, "add-ice-candidate", data->peer_sess_id,
      candidate, &ret);
  fail_unless (ret);
}

static gboolean
check_caps (GstPad * pad, HandOffData * hod)
{
  GstCaps *caps, *expected_caps;
  gboolean is_subset = FALSE;

  caps = gst_pad_get_current_caps (pad);

  if (caps == NULL) {
    return FALSE;
  }

  expected_caps = gst_static_caps_get (&hod->expected_caps);

  is_subset = gst_caps_is_subset (caps, expected_caps);
  GST_DEBUG ("expected caps: %" GST_PTR_FORMAT ", caps: %" GST_PTR_FORMAT
      ", is subset: %d", expected_caps, caps, is_subset);
  gst_caps_unref (expected_caps);
  gst_caps_unref (caps);

  fail_unless (is_subset);

  return TRUE;
}

static void
fakesink_hand_off (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  HandOffData *hod = (HandOffData *) data;

  if (GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DELTA_UNIT)) {
    GST_DEBUG ("It is not a keyframe");
    return;
  } else {
    GST_DEBUG ("Received keyframe");
  }

  if (!check_caps (pad, hod)) {
    return;
  }

  if (!hod->check_request_local_key_frame) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
    return;
  }

  /* Check KmsBaseRtpEp::request_local_key_frame */
  if (!GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DELTA_UNIT)) {
    if (hod->count >= 5) {
      g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
      g_idle_add (quit_main_loop_idle, hod->loop);
    } else {
      gboolean ret;

      g_signal_emit_by_name (hod->webrtcep, "request-local-key-frame", &ret);
      fail_unless (ret);
      hod->count++;
    }
  }
}

#define OFFERER_RECEIVES "offerer-receives"
G_DEFINE_QUARK (OFFERER_RECEIVES, offerer_receives);

#define ANSWERER_RECEIVES "answerer-receives"
G_DEFINE_QUARK (ANSWERER_RECEIVES, answerer_receives);

G_LOCK_DEFINE_STATIC (check_receive_lock);

static void
receiver_1_fakesink_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer data)
{
  HandOffData *hod = (HandOffData *) data;
  GstElement *pipeline;

  if (GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DELTA_UNIT)) {
    GST_DEBUG ("It is not a keyframe");
    return;
  } else {
    GST_DEBUG ("Received keyframe");
  }

  if (!check_caps (pad, hod)) {
    return;
  }

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);

  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              answerer_receives_quark ()))) {
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), offerer_receives_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
receiver_2_fakesink_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer data)
{
  HandOffData *hod = (HandOffData *) data;
  GstElement *pipeline;

  if (GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DELTA_UNIT)) {
    GST_DEBUG ("It is not a keyframe");
    return;
  } else {
    GST_DEBUG ("Received keyframe");
  }

  if (!check_caps (pad, hod)) {
    return;
  }

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);

  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              offerer_receives_quark ()))) {
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), answerer_receives_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
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
  KmsConnectData *data = g_slice_new0 (KmsConnectData);

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

static void
test_video_sendonly (const gchar * video_enc_name, GstStaticCaps expected_caps,
    gchar * codec, gboolean bundle,
    gboolean check_request_local_key_frame, gboolean gather_asap,
    const gchar * pem)
{
  GArray *codecs_array;
  gchar *codecs[] = { codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *sender_sess_id, *receiver_sess_id;
  OnIceCandidateData sender_cand_data, receiver_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc = gst_element_factory_make (video_enc_name, NULL);
  GstElement *sender = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *receiver = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *outputfakesink = gst_element_factory_make ("fakesink", NULL);
  gchar *sdp_str = NULL;
  gboolean ret;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  if (pem != NULL) {
    g_object_set (sender, "pem-certificate", pem, NULL);
    g_object_set (receiver, "pem-certificate", pem, NULL);
  }

  codecs_array = create_codecs_array (codecs);
  g_object_set (sender, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), "bundle", bundle, NULL);
  g_object_set (receiver, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), NULL);
  g_array_unref (codecs_array);

  /* Session creation */
  g_signal_emit_by_name (sender, "create-session", &sender_sess_id);
  GST_DEBUG_OBJECT (sender, "Created session with id '%s'", sender_sess_id);
  g_signal_emit_by_name (receiver, "create-session", &receiver_sess_id);
  GST_DEBUG_OBJECT (receiver, "Created session with id '%s'", receiver_sess_id);

  /* Trickle ICE management */
  sender_cand_data.peer = receiver;
  sender_cand_data.peer_sess_id = receiver_sess_id;
  g_signal_connect (G_OBJECT (sender), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &sender_cand_data);

  receiver_cand_data.peer = sender;
  receiver_cand_data.peer_sess_id = sender_sess_id;
  g_signal_connect (G_OBJECT (receiver), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &receiver_cand_data);

  hod = g_slice_new0 (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  if (check_request_local_key_frame) {
    GST_INFO ("Check request_local_key_frame");

    g_object_set (video_enc, "keyframe-max-dist", 10000, NULL);
    hod->check_request_local_key_frame = TRUE;
    hod->webrtcep = sender;
  }

  g_object_set (G_OBJECT (outputfakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (outputfakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), hod);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), sender);

  connect_sink_async (sender, videotestsrc, video_enc, NULL, pipeline,
      SINK_VIDEO_STREAM);

  gst_bin_add (GST_BIN (pipeline), receiver);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  if (check_request_local_key_frame) {
    gboolean ret;

    /* WebRtcEp should not be configured yet */
    g_signal_emit_by_name (hod->webrtcep, "request-local-key-frame", &ret);
    fail_unless (!ret);
  }

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (sender, "generate-offer", sender_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  if (gather_asap) {
    g_signal_emit_by_name (sender, "gather-candidates", sender_sess_id, &ret);
    fail_unless (ret);
  }

  mark_point ();
  g_signal_emit_by_name (receiver, "process-offer", receiver_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  if (check_request_local_key_frame) {
    gboolean ret;

    /* Request should not be handled */
    g_signal_emit_by_name (hod->webrtcep, "request-local-key-frame", &ret);
    fail_unless (!ret);
  }

  /* FIXME: not working */
//  if (gather_asap) {
//    g_signal_emit_by_name (receiver, "gather-candidates", &ret);
//    fail_unless (ret);
//  }

  mark_point ();
  g_signal_emit_by_name (sender, "process-answer", sender_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  if (!gather_asap) {
    g_signal_emit_by_name (sender, "gather-candidates", sender_sess_id, &ret);
    fail_unless (ret);
    /* FIXME: not working */
//    g_signal_emit_by_name (receiver, "gather-candidates", &ret);
//    fail_unless (ret);
  }

  g_signal_emit_by_name (receiver, "gather-candidates", receiver_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add (GST_BIN (pipeline), outputfakesink);
  g_object_set_qdata (G_OBJECT (receiver), video_sink_quark (), outputfakesink);
  g_signal_connect (receiver, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (receiver,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendonly_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendonly_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
  g_free (sender_sess_id);
  g_free (receiver_sess_id);
}

static void
test_video_sendrecv (const gchar * video_enc_name,
    GstStaticCaps expected_caps, gchar * codec, gboolean bundle,
    gboolean rtcp_mux)
{
  GArray *codecs_array;
  gchar *codecs[] = { codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *offerer_sess_id, *answerer_sess_id;
  OnIceCandidateData offerer_cand_data, answerer_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *videotestsrc_offerer =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *videotestsrc_answerer =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc_offerer =
      gst_element_factory_make (video_enc_name, NULL);
  GstElement *video_enc_answerer =
      gst_element_factory_make (video_enc_name, NULL);
  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *fakesink_offerer = gst_element_factory_make ("fakesink", NULL);
  GstElement *fakesink_answerer = gst_element_factory_make ("fakesink", NULL);
  gchar *sdp_str = NULL;
  gboolean ret;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  codecs_array = create_codecs_array (codecs);
  g_object_set (offerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), "bundle", bundle, "rtcp-mux", rtcp_mux, NULL);
  g_object_set (answerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), NULL);
  g_array_unref (codecs_array);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  /* Trickle ICE management */
  offerer_cand_data.peer = answerer;
  offerer_cand_data.peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &offerer_cand_data);

  answerer_cand_data.peer = offerer;
  answerer_cand_data.peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &answerer_cand_data);

  hod = g_slice_new0 (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (fakesink_offerer), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_offerer), "handoff",
      G_CALLBACK (receiver_1_fakesink_hand_off), hod);
  g_object_set (G_OBJECT (fakesink_answerer), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_answerer), "handoff",
      G_CALLBACK (receiver_2_fakesink_hand_off), hod);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), offerer);
  connect_sink_async (offerer, videotestsrc_offerer, video_enc_offerer, NULL,
      pipeline, SINK_VIDEO_STREAM);

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, videotestsrc_answerer, video_enc_answerer, NULL,
      pipeline, SINK_VIDEO_STREAM);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", answerer_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

  g_object_set_qdata (G_OBJECT (offerer), video_sink_quark (),
      fakesink_offerer);
  g_signal_connect (offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  g_object_set_qdata (G_OBJECT (answerer), video_sink_quark (),
      fakesink_answerer);
  g_signal_connect (answerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
  g_free (offerer_sess_id);
  g_free (answerer_sess_id);
}

static void
test_audio_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps expected_caps, gchar * codec, gboolean bundle)
{
  GArray *codecs_array;
  gchar *codecs[] = { codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *offerer_sess_id, *answerer_sess_id;
  OnIceCandidateData offerer_cand_data, answerer_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *audiotestsrc_offerer =
      gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *audiotestsrc_answerer =
      gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *capsfilter_offerer =
      gst_element_factory_make ("capsfilter", NULL);
  GstElement *capsfilter_answerer =
      gst_element_factory_make ("capsfilter", NULL);
  GstElement *audio_enc_offerer =
      gst_element_factory_make (audio_enc_name, NULL);
  GstElement *audio_enc_answerer =
      gst_element_factory_make (audio_enc_name, NULL);
  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *fakesink_offerer = gst_element_factory_make ("fakesink", NULL);
  GstElement *fakesink_answerer = gst_element_factory_make ("fakesink", NULL);
  GstCaps *caps;
  gchar *sdp_str = NULL;
  gboolean ret;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  codecs_array = create_codecs_array (codecs);
  g_object_set (offerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (codecs_array), "bundle", bundle, NULL);
  g_object_set (answerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (codecs_array), NULL);
  g_array_unref (codecs_array);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  /* Trickle ICE management */
  offerer_cand_data.peer = answerer;
  offerer_cand_data.peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &offerer_cand_data);

  answerer_cand_data.peer = offerer;
  answerer_cand_data.peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &answerer_cand_data);

  hod = g_slice_new0 (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (fakesink_offerer), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_offerer), "handoff",
      G_CALLBACK (receiver_1_fakesink_hand_off), hod);
  g_object_set (G_OBJECT (fakesink_answerer), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_answerer), "handoff",
      G_CALLBACK (receiver_2_fakesink_hand_off), hod);

  g_object_set (G_OBJECT (audiotestsrc_offerer), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (audiotestsrc_answerer), "is-live", TRUE, NULL);

  caps = gst_caps_new_simple ("audio/x-raw", "rate", G_TYPE_INT, 8000, NULL);
  g_object_set (capsfilter_offerer, "caps", caps, NULL);
  g_object_set (capsfilter_answerer, "caps", caps, NULL);
  gst_caps_unref (caps);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), offerer);
  connect_sink_async (offerer, audiotestsrc_offerer, audio_enc_offerer,
      capsfilter_offerer, pipeline, SINK_AUDIO_STREAM);

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, audiotestsrc_answerer, audio_enc_answerer,
      capsfilter_answerer, pipeline, SINK_AUDIO_STREAM);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", answerer_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

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
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
  g_free (offerer_sess_id);
  g_free (answerer_sess_id);
}

#define OFFERER_RECEIVES_AUDIO "offerer-receives-audio"
G_DEFINE_QUARK (OFFERER_RECEIVES_AUDIO, offerer_receives_audio);

#define OFFERER_RECEIVES_VIDEO "offerer-receives-video"
G_DEFINE_QUARK (OFFERER_RECEIVES_VIDEO, offerer_receives_video);

#define ANSWERER_RECEIVES_AUDIO "answerer-receives-audio"
G_DEFINE_QUARK (ANSWERER_RECEIVES_AUDIO, answerer_receives_audio);

#define ANSWERER_RECEIVES_VIDEO "answerer-receives-video"
G_DEFINE_QUARK (ANSWERER_RECEIVES_VIDEO, answerer_receives_video);

static gboolean
check_offerer_and_answerer_receive_audio_and_video (gpointer pipeline)
{
  return GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
          offerer_receives_audio_quark ())) &&
      GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
          offerer_receives_video_quark ())) &&
      GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
          answerer_receives_audio_quark ())) &&
      GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
          answerer_receives_video_quark ()));
}

static void
sendrecv_fakesink_hand_off (GstElement * fakesink,
    GstBuffer * buf, GstPad * pad, gpointer data)
{
  HandOffData *hod = (HandOffData *) data;
  GstElement *pipeline;

  if (!check_caps (pad, hod)) {
    return;
  }

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  g_object_set_qdata (G_OBJECT (pipeline), hod->type, GINT_TO_POINTER (TRUE));
  g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);

  if (check_offerer_and_answerer_receive_audio_and_video (pipeline)) {
    g_idle_add (quit_main_loop_idle, hod->loop);
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
test_audio_video_sendonly_recvonly (const gchar * audio_enc_name,
    GstStaticCaps audio_expected_caps, gchar * audio_codec,
    const gchar * video_enc_name, GstStaticCaps video_expected_caps,
    gchar * video_codec, gboolean bundle)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { audio_codec, NULL };
  gchar *video_codecs[] = { video_codec, NULL };
  HandOffData *hod_audio, *hod_video;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *sender_sess_id, *receiver_sess_id;
  OnIceCandidateData *sender_cand_data, *receiver_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);

  GstElement *audiotestsrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *capsfilter = gst_element_factory_make ("capsfilter", NULL);
  GstElement *audio_enc = gst_element_factory_make (audio_enc_name, NULL);

  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc = gst_element_factory_make (video_enc_name, NULL);

  GstElement *sender = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *receiver = gst_element_factory_make ("webrtcendpoint", NULL);

  GstElement *audio_fakesink = gst_element_factory_make ("fakesink", NULL);
  GstElement *video_fakesink = gst_element_factory_make ("fakesink", NULL);

  GstCaps *caps;
  gchar *sdp_str = NULL;
  gboolean ret;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (sender, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), "bundle", bundle, NULL);
  g_object_set (receiver, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);
  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  /* Session creation */
  g_signal_emit_by_name (sender, "create-session", &sender_sess_id);
  GST_DEBUG_OBJECT (sender, "Created session with id '%s'", sender_sess_id);
  g_signal_emit_by_name (receiver, "create-session", &receiver_sess_id);
  GST_DEBUG_OBJECT (receiver, "Created session with id '%s'", receiver_sess_id);

  /* Trickle ICE management */
  sender_cand_data = g_slice_new0 (OnIceCandidateData);
  sender_cand_data->peer = receiver;
  sender_cand_data->peer_sess_id = receiver_sess_id;
  g_signal_connect (G_OBJECT (sender), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), sender_cand_data);

  receiver_cand_data = g_slice_new0 (OnIceCandidateData);
  receiver_cand_data->peer = sender;
  receiver_cand_data->peer_sess_id = sender_sess_id;
  g_signal_connect (G_OBJECT (receiver), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), receiver_cand_data);

  /* Hack to avoid audio and video reception in sender(offerer) */
  g_object_set_qdata (G_OBJECT (pipeline), offerer_receives_audio_quark (),
      GINT_TO_POINTER (TRUE));
  g_object_set_qdata (G_OBJECT (pipeline), offerer_receives_video_quark (),
      GINT_TO_POINTER (TRUE));

  hod_audio = g_slice_new0 (HandOffData);
  hod_audio->type = answerer_receives_audio_quark ();
  hod_audio->expected_caps = audio_expected_caps;
  hod_audio->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (audio_fakesink), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio);

  hod_video = g_slice_new0 (HandOffData);
  hod_video->type = answerer_receives_video_quark ();
  hod_video->expected_caps = video_expected_caps;
  hod_video->loop = loop;
  g_object_set (G_OBJECT (video_fakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (video_fakesink), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_video);

  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (videotestsrc), "is-live", TRUE, NULL);

  caps = gst_caps_new_simple ("audio/x-raw", "rate", G_TYPE_INT, 8000, NULL);
  g_object_set (capsfilter, "caps", caps, NULL);
  gst_caps_unref (caps);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), sender);
  connect_sink_async (sender, audiotestsrc, audio_enc, capsfilter, pipeline,
      SINK_AUDIO_STREAM);
  connect_sink_async (sender, videotestsrc, video_enc, NULL, pipeline,
      SINK_VIDEO_STREAM);

  gst_bin_add (GST_BIN (pipeline), receiver);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (sender, "generate-offer", sender_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (receiver, "process-offer", receiver_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (sender, "process-answer", sender_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (sender, "gather-candidates", sender_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (receiver, "gather-candidates", receiver_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add (GST_BIN (pipeline), audio_fakesink);
  g_object_set_qdata (G_OBJECT (receiver), audio_sink_quark (), audio_fakesink);
  g_signal_connect (receiver, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (receiver,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  gst_bin_add (GST_BIN (pipeline), video_fakesink);
  g_object_set_qdata (G_OBJECT (receiver), video_sink_quark (), video_fakesink);
  fail_unless (kms_element_request_srcpad (receiver,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL,
      "test_audio_video_sendonly_recvonly_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_video_sendonly_recvonly_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod_audio);
  g_slice_free (HandOffData, hod_video);
  g_free (sender_sess_id);
  g_free (receiver_sess_id);
  g_slice_free (OnIceCandidateData, sender_cand_data);
  g_slice_free (OnIceCandidateData, receiver_cand_data);
}

static void
test_audio_video_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps audio_expected_caps, gchar * audio_codec,
    const gchar * video_enc_name, GstStaticCaps video_expected_caps,
    gchar * video_codec, gboolean bundle)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { audio_codec, NULL };
  gchar *video_codecs[] = { video_codec, NULL };
  HandOffData *hod_audio_offerer, *hod_video_offerer, *hod_audio_answerer,
      *hod_video_answerer;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *offerer_sess_id, *answerer_sess_id;
  OnIceCandidateData offerer_cand_data, answerer_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);

  GstElement *audiotestsrc_offerer =
      gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *audiotestsrc_answerer =
      gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *capsfilter_offerer =
      gst_element_factory_make ("capsfilter", NULL);
  GstElement *capsfilter_answerer =
      gst_element_factory_make ("capsfilter", NULL);
  GstElement *audio_enc_offerer =
      gst_element_factory_make (audio_enc_name, NULL);
  GstElement *audio_enc_answerer =
      gst_element_factory_make (audio_enc_name, NULL);

  GstElement *videotestsrc_offerer =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *videotestsrc_answerer =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc_offerer =
      gst_element_factory_make (video_enc_name, NULL);
  GstElement *video_enc_answerer =
      gst_element_factory_make (video_enc_name, NULL);

  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("webrtcendpoint", NULL);

  GstElement *audio_fakesink_offerer =
      gst_element_factory_make ("fakesink", NULL);
  GstElement *audio_fakesink_answerer =
      gst_element_factory_make ("fakesink", NULL);
  GstElement *video_fakesink_offerer =
      gst_element_factory_make ("fakesink", NULL);
  GstElement *video_fakesink_answerer =
      gst_element_factory_make ("fakesink", NULL);

  GstCaps *caps;
  gchar *sdp_str = NULL;
  gboolean ret;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (offerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), "bundle", bundle, NULL);
  g_object_set (answerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);
  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  /* Trickle ICE management */
  offerer_cand_data.peer = answerer;
  offerer_cand_data.peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &offerer_cand_data);

  answerer_cand_data.peer = offerer;
  answerer_cand_data.peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &answerer_cand_data);

  hod_audio_offerer = g_slice_new0 (HandOffData);
  hod_audio_offerer->type = offerer_receives_audio_quark ();
  hod_audio_offerer->expected_caps = audio_expected_caps;
  hod_audio_offerer->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (audio_fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio_offerer);

  hod_video_offerer = g_slice_new0 (HandOffData);
  hod_video_offerer->type = offerer_receives_video_quark ();
  hod_video_offerer->expected_caps = video_expected_caps;
  hod_video_offerer->loop = loop;
  g_object_set (G_OBJECT (video_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (video_fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_video_offerer);

  hod_audio_answerer = g_slice_new0 (HandOffData);
  hod_audio_answerer->type = answerer_receives_audio_quark ();
  hod_audio_answerer->expected_caps = audio_expected_caps;
  hod_audio_answerer->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink_answerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (audio_fakesink_answerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio_answerer);

  hod_video_answerer = g_slice_new0 (HandOffData);
  hod_video_answerer->type = answerer_receives_video_quark ();
  hod_video_answerer->expected_caps = video_expected_caps;
  hod_video_answerer->loop = loop;
  g_object_set (G_OBJECT (video_fakesink_answerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (video_fakesink_answerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_video_answerer);

  g_object_set (G_OBJECT (audiotestsrc_offerer), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (audiotestsrc_answerer), "is-live", TRUE, NULL);

  caps = gst_caps_new_simple ("audio/x-raw", "rate", G_TYPE_INT, 8000, NULL);
  g_object_set (capsfilter_offerer, "caps", caps, NULL);
  g_object_set (capsfilter_answerer, "caps", caps, NULL);
  gst_caps_unref (caps);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), offerer);
  connect_sink_async (offerer, audiotestsrc_offerer, audio_enc_offerer,
      capsfilter_offerer, pipeline, SINK_AUDIO_STREAM);
  connect_sink_async (offerer, videotestsrc_offerer, video_enc_offerer, NULL,
      pipeline, SINK_VIDEO_STREAM);

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, audiotestsrc_answerer, audio_enc_answerer,
      capsfilter_answerer, pipeline, SINK_AUDIO_STREAM);
  connect_sink_async (answerer, videotestsrc_answerer, video_enc_answerer, NULL,
      pipeline, SINK_VIDEO_STREAM);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", answerer_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add_many (GST_BIN (pipeline), audio_fakesink_offerer,
      audio_fakesink_answerer, NULL);

  g_signal_connect (offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  g_signal_connect (answerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);

  g_object_set_qdata (G_OBJECT (offerer), audio_sink_quark (),
      audio_fakesink_offerer);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));
  g_object_set_qdata (G_OBJECT (answerer), audio_sink_quark (),
      audio_fakesink_answerer);
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  gst_bin_add_many (GST_BIN (pipeline), video_fakesink_offerer,
      video_fakesink_answerer, NULL);

  g_object_set_qdata (G_OBJECT (offerer), video_sink_quark (),
      video_fakesink_offerer);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));
  g_object_set_qdata (G_OBJECT (answerer), video_sink_quark (),
      video_fakesink_answerer);
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod_audio_offerer);
  g_slice_free (HandOffData, hod_video_offerer);
  g_slice_free (HandOffData, hod_audio_answerer);
  g_slice_free (HandOffData, hod_video_answerer);
  g_free (offerer_sess_id);
  g_free (answerer_sess_id);
}

static void
test_offerer_audio_video_answerer_video_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps audio_expected_caps, gchar * audio_codec,
    const gchar * video_enc_name, GstStaticCaps video_expected_caps,
    gchar * video_codec, gboolean bundle)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { audio_codec, NULL };
  gchar *video_codecs[] = { video_codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *offerer_sess_id, *answerer_sess_id;
  OnIceCandidateData offerer_cand_data, answerer_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);

  GstElement *videotestsrc_offerer =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *videotestsrc_answerer =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc_offerer =
      gst_element_factory_make (video_enc_name, NULL);
  GstElement *video_enc_answerer =
      gst_element_factory_make (video_enc_name, NULL);

  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("webrtcendpoint", NULL);

  GstElement *video_fakesink_offerer =
      gst_element_factory_make ("fakesink", NULL);
  GstElement *video_fakesink_answerer =
      gst_element_factory_make ("fakesink", NULL);

  gchar *sdp_str = NULL;
  gboolean ret;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);

  g_object_set (offerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), "bundle", bundle,
      "min-port", 50000, "max-port", 55000, NULL);

  // Answerer only supports video
  g_object_set (answerer, "num-audio-medias", 0, "num-video-medias", 1,
      "video-codecs", g_array_ref (video_codecs_array), "bundle", bundle,
      "min-port", 50000, "max-port", 55000, NULL);

  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  // Session creation
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  // Trickle ICE management
  offerer_cand_data.peer = answerer;
  offerer_cand_data.peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &offerer_cand_data);

  answerer_cand_data.peer = offerer;
  answerer_cand_data.peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), &answerer_cand_data);

  hod = g_slice_new0 (HandOffData);
  hod->expected_caps = video_expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (video_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (video_fakesink_offerer), "handoff",
      G_CALLBACK (receiver_1_fakesink_hand_off), hod);
  g_object_set (G_OBJECT (video_fakesink_answerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (video_fakesink_answerer), "handoff",
      G_CALLBACK (receiver_2_fakesink_hand_off), hod);

  // Add elements
  gst_bin_add (GST_BIN (pipeline), offerer);
  connect_sink_async (offerer, videotestsrc_offerer, video_enc_offerer, NULL,
      pipeline, SINK_VIDEO_STREAM);

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, videotestsrc_answerer, video_enc_answerer, NULL,
      pipeline, SINK_VIDEO_STREAM);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  // SDP negotiation
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG_OBJECT (offerer, "Offer:\n%s", (sdp_str =
          gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", answerer_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG_OBJECT (answerer, "Answer:\n%s", (sdp_str =
          gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  GST_DEBUG_OBJECT (offerer, "============ Offerer gather candidates BEGIN");
  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  GST_DEBUG_OBJECT (offerer, "============ Offerer gather candidates END");
  fail_unless (ret);

  GST_DEBUG_OBJECT (answerer, "============ Answerer gather candidates BEGIN");
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
  GST_DEBUG_OBJECT (answerer, "============ Answerer gather candidates END");
  fail_unless (ret);

  g_signal_connect (offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  g_signal_connect (answerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);

  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  gst_bin_add_many (GST_BIN (pipeline), video_fakesink_offerer,
      video_fakesink_answerer, NULL);

  g_object_set_qdata (G_OBJECT (offerer), video_sink_quark (),
      video_fakesink_offerer);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));
  g_object_set_qdata (G_OBJECT (answerer), video_sink_quark (),
      video_fakesink_answerer);
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_before_entering_loop");

  mark_point ();
  GST_INFO ("============ Main loop BEGIN");
  g_main_loop_run (loop);
  GST_INFO ("============ Main loop END");
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
  g_free (offerer_sess_id);
  g_free (answerer_sess_id);
}

#define TEST_MESSAGE "Hello world!"

static void
feed_data_channel (GstElement * appsrc, guint unused_size, gpointer data)
{
  GstFlowReturn ret;
  GstBuffer *buff;
  gchar *msg;

  msg = g_strdup (TEST_MESSAGE);
  buff = gst_buffer_new_wrapped (msg, strlen (msg));

  g_signal_emit_by_name (appsrc, "push-buffer", buff, &ret);
  gst_buffer_unref (buff);
}

static void
webrtc_sender_pad_added (GstElement * element, GstPad * new_pad,
    gpointer user_data)
{
  GstElement *appsrc, *pipeline = GST_ELEMENT (user_data);
  GstPad *srcpad;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);

  fail_unless (GST_PAD_IS_SINK (new_pad));

  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "min-latency",
      G_GINT64_CONSTANT (0), "max-bytes", 0, "emit-signals", TRUE, NULL);

  g_signal_connect (appsrc, "need-data", G_CALLBACK (feed_data_channel), NULL);

  gst_bin_add (GST_BIN (pipeline), appsrc);

  srcpad = gst_element_get_static_pad (appsrc, "src");
  fail_if (gst_pad_link (srcpad, new_pad) != GST_PAD_LINK_OK);
  g_object_unref (srcpad);

  gst_element_sync_state_with_parent (appsrc);
}

static void
fakesink_handoff (GstElement * fakesink, GstBuffer * buff, GstPad * pad,
    gpointer user_data)
{
  GMainLoop *loop = user_data;
  GstMapInfo info;
  gchar *data;

  if (!gst_buffer_map (buff, &info, GST_MAP_READ)) {
    GST_WARNING ("Can no map buffer");
    return;
  }

  data = g_strndup ((const gchar *) info.data, info.size);
  GST_DEBUG ("Data buffer: '%s'", data);
  g_free (data);

  gst_buffer_unmap (buff, &info);

  g_idle_add (quit_main_loop_idle, loop);

  g_signal_handlers_disconnect_by_data (fakesink, user_data);
}

typedef struct _TmpCallbackData
{
  GstElement *pipeline;
  GMainLoop *loop;
} TmpCallbackData;

static void
webrtc_receiver_pad_added (GstElement * element, GstPad * new_pad,
    gpointer user_data)
{
  TmpCallbackData *tmp = user_data;
  GstElement *fakesink, *pipeline = tmp->pipeline;
  GstPad *sink_pad;

  if (GST_PAD_IS_SINK (new_pad)) {
    /* Do not connect anything here */
    return;
  }

  fakesink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (fakesink), "sync", FALSE, "async", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (fakesink, "handoff", G_CALLBACK (fakesink_handoff),
      tmp->loop);

  gst_bin_add (GST_BIN (pipeline), fakesink);

  sink_pad = gst_element_get_static_pad (fakesink, "sink");
  fail_unless (gst_pad_link (new_pad, sink_pad) == GST_PAD_LINK_OK);
  g_object_unref (sink_pad);

  gst_element_sync_state_with_parent (fakesink);
}

static void
data_session_established_cb (GstElement * self, const gchar * sess_id,
    gboolean connected, gpointer data)
{
  GST_DEBUG_OBJECT (self, "Data session %s",
      (connected) ? "established" : "finished");

  if (connected) {
    gint stream_id;

    g_signal_emit_by_name (self, "create-data-channel", sess_id, TRUE, -1, -1,
        "TestChannel", "webrtc-datachannel", &stream_id);

    fail_if (stream_id < 0);

    GST_DEBUG ("Requested data channel id %u", stream_id);
  }
}

static void
test_data_channels (gboolean bundle)
{
  gchar *sender_sess_id, *receiver_sess_id;
  OnIceCandidateData *sender_cand_data, *receiver_cand_data;
  GstElement *sender = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *receiver = gst_element_factory_make ("webrtcendpoint", NULL);
  GstSDPMessage *offer = NULL, *answer = NULL;
  GstSDPMessage *offerer_local_sdp = NULL, *offerer_remote_sdp = NULL;
  gchar *offerer_local_sdp_str, *offerer_remote_sdp_str;
  GstSDPMessage *answerer_local_sdp = NULL, *answerer_remote_sdp = NULL;
  gchar *answerer_local_sdp_str, *answerer_remote_sdp_str;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (NULL);
  gchar *sdp_str = NULL;
  gchar *padname = NULL;
  gboolean ret;
  TmpCallbackData tmp;
  gboolean answer_ok;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_signal_connect (sender, "data-session-established",
      G_CALLBACK (data_session_established_cb), NULL);

  /* Session creation */
  g_signal_emit_by_name (sender, "create-session", &sender_sess_id);
  GST_DEBUG_OBJECT (sender, "Created session with id '%s'", sender_sess_id);
  g_signal_emit_by_name (receiver, "create-session", &receiver_sess_id);
  GST_DEBUG_OBJECT (receiver, "Created session with id '%s'", receiver_sess_id);

  /* Trickle ICE management */
  sender_cand_data = g_slice_new0 (OnIceCandidateData);
  sender_cand_data->peer = receiver;
  sender_cand_data->peer_sess_id = receiver_sess_id;
  g_signal_connect (G_OBJECT (sender), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), sender_cand_data);

  receiver_cand_data = g_slice_new0 (OnIceCandidateData);
  receiver_cand_data->peer = sender;
  receiver_cand_data->peer_sess_id = sender_sess_id;
  g_signal_connect (G_OBJECT (receiver), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), receiver_cand_data);

  g_object_set (sender, "use-data-channels", TRUE, NULL);
  g_object_set (receiver, "use-data-channels", TRUE, NULL);

  gst_bin_add_many (GST_BIN (pipeline), sender, receiver, NULL);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_object_set (sender, "max-video-recv-bandwidth", 0, NULL);
  g_object_set (receiver, "max-video-recv-bandwidth", 0, NULL);

  g_object_set (sender, "bundle", bundle, "num-audio-medias", 0,
      "num-video-medias", 0, NULL);
  g_object_set (receiver, "num-audio-medias", 0, "num-video-medias", 0, NULL);

  g_signal_connect (sender, "pad-added", G_CALLBACK (webrtc_sender_pad_added),
      pipeline);

  tmp.pipeline = pipeline;
  tmp.loop = loop;

  g_signal_connect (receiver, "pad-added",
      G_CALLBACK (webrtc_receiver_pad_added), &tmp);

  g_signal_emit_by_name (receiver, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad name %s", padname);
  g_free (padname);

  /* SDP Negotiation */
  g_signal_emit_by_name (sender, "generate-offer", sender_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (receiver, "process-offer", receiver_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (sender, "process-answer", sender_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);

  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (sender, "get-local-sdp", sender_sess_id,
      &offerer_local_sdp);
  fail_unless (offerer_local_sdp != NULL);
  g_signal_emit_by_name (sender, "get-remote-sdp", sender_sess_id,
      &offerer_remote_sdp);
  fail_unless (offerer_remote_sdp != NULL);

  g_signal_emit_by_name (receiver, "get-local-sdp", receiver_sess_id,
      &answerer_local_sdp);
  fail_unless (answerer_local_sdp != NULL);
  g_signal_emit_by_name (receiver, "get-remote-sdp", receiver_sess_id,
      &answerer_remote_sdp);
  fail_unless (answerer_remote_sdp != NULL);

  offerer_local_sdp_str = gst_sdp_message_as_text (offerer_local_sdp);
  offerer_remote_sdp_str = gst_sdp_message_as_text (offerer_remote_sdp);

  answerer_local_sdp_str = gst_sdp_message_as_text (answerer_local_sdp);
  answerer_remote_sdp_str = gst_sdp_message_as_text (answerer_remote_sdp);

  GST_DEBUG ("Offerer local SDP\n%s", offerer_local_sdp_str);
  GST_DEBUG ("Offerer remote SDP\n%s", offerer_remote_sdp_str);
  GST_DEBUG ("Answerer local SDP\n%s", answerer_local_sdp_str);
  GST_DEBUG ("Answerer remote SDP\n%s", answerer_remote_sdp_str);

  fail_unless (g_strcmp0 (offerer_local_sdp_str, answerer_remote_sdp_str) == 0);
  fail_unless (g_strcmp0 (offerer_remote_sdp_str, answerer_local_sdp_str) == 0);

  g_free (offerer_local_sdp_str);
  g_free (offerer_remote_sdp_str);
  g_free (answerer_local_sdp_str);
  g_free (answerer_remote_sdp_str);

  gst_sdp_message_free (offerer_local_sdp);
  gst_sdp_message_free (offerer_remote_sdp);
  gst_sdp_message_free (answerer_local_sdp);
  gst_sdp_message_free (answerer_remote_sdp);

  g_signal_emit_by_name (sender, "gather-candidates", sender_sess_id, &ret);
  fail_unless (ret);

  g_signal_emit_by_name (receiver, "gather-candidates", receiver_sess_id, &ret);
  fail_unless (ret);

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_data_channels_end");

  GST_WARNING ("Finishing test");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_free (sender_sess_id);
  g_free (receiver_sess_id);
  g_slice_free (OnIceCandidateData, sender_cand_data);
  g_slice_free (OnIceCandidateData, receiver_cand_data);
}

GST_START_TEST (test_webrtc_data_channel)
{
  /* Check data channels in a bundle connection */
  test_data_channels (TRUE);

  /* Check data channels in a dedicated connection */
  test_data_channels (FALSE);
}
GST_END_TEST

/* Video tests */
static GstStaticCaps vp8_expected_caps = GST_STATIC_CAPS ("video/x-vp8");

GST_START_TEST (test_vp8_sendonly_recvonly)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE,
      FALSE, NULL);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, FALSE,
      FALSE, NULL);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, TRUE,
      FALSE, NULL);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, FALSE,
      TRUE, NULL);
}
GST_END_TEST

const gchar *rsa_pem = "-----BEGIN PRIVATE KEY-----\r\n"
    "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCb+LTrbVIUWgpo\r\n"
    "2P1xIONodNWBZrjKoSiuDFgmwHvRtbsHu3/wVHBw8aCgMfSAkx5fr/qE3V2u9Ufc\r\n"
    "OF2Sm2+n6FpSl4n3Y0Pj06GkoZ3G2Q68Pce53jOpud1TJbFT9cPl4zUnz+36fczB\r\n"
    "US9nNsHHEvkPXu1NbNWsf2/cRB3nSUlENz0lJYMDnNQwmE1IALFvxccY3cCUUsku\r\n"
    "3hUJiK9SSSHvGOT41xW55IpfjL03HEyU+eBzo+KqCgZ7GPtaYdbMtg6AOIktRIiK\r\n"
    "NjoMU5Kmv5XY/wdOZFX42pSit3SyBZXIdlJ2/cilRzHdTpX0+FJaQOsE0cmADqUu\r\n"
    "Z9xQVxYVAgMBAAECggEAeTfDtC0UW4jXCkGKR3/d/XK/9H/XInQ533rsj8GM+aEZ\r\n"
    "qJrKhge6E61WvunUMpEkTA3Cz7wTcURkAv0zjBuNnZtxhzsLGN9nBguWVxLcQoyz\r\n"
    "bQ0+ym+tCz3Oiy6CMNSS2XnJ+BUbyVA38A6ensLpu0Q9mPqXx1LMhfHNCA1QiE0w\r\n"
    "KtmgLaCH55x7fhWJXCkQmsD2Ir8JrS6y3UYQ9fEoFT/qxPyIt77f6cYvsd2zdtWv\r\n"
    "LvsKG8YeLtbJAgmO0dSDuTKOEIjvjfOio3geVGNXcAa12i7aSsaHElk8OEmTTTDI\r\n"
    "s8KkqHtuK+/vzj09dvPtT/VFF6NwY+Chjw1aOwGLSQKBgQDKOuPv8zJt3uIiYnoD\r\n"
    "uSFdReK4daGa/lLrmQ0eEUsRT8hcMV/oZ4nxPh8iNAPVwxtsXGyZ7XTguXrG+Zxq\r\n"
    "me+9y0A5Nzy+iNMDhQltJP5tnaIVy/GoS2SmPYqz4QcgU//77YZgnKsm3B6vIjJa\r\n"
    "cs8P0gmiU/1ps/5HwJxZjil33wKBgQDFcSfmotKJLPM2cP/XiYhSqrAy0SrvbvmQ\r\n"
    "Bbupsr0wnAAfu+/SrbYCUA2x8//Qs8dtZBjTNgQYR+26zWDG2xuHuOwKfG0CYzVk\r\n"
    "8CHDmlBpM/Jv5H993SXlTj5sp+LA6tplprq0v+9sXINk5R+SP4SaZi+t3zKk0bK2\r\n"
    "EiymJo0AiwKBgGX5cizx7lD23gLGs44jEU7uSZgIQPheHEQPBk1OHRNarsbGYv1X\r\n"
    "EHjkgWCG6BQncTGgHWc1FQWood+pyJT8kKac0gLH9sqBRh51PD6cM+vkW/Ivx+i8\r\n"
    "M8GcMM/pveUwVlmb+XHILonG33YigU+Yqw7oM9F2FsfxalyWJIEILaLzAoGAQWGV\r\n"
    "OvSUD0TJTS/iKLesYuOO8WT+eMcg8SZU3H8J0zz1dYzAf91yNhXYUyNfhPbjhT/u\r\n"
    "UJLEjF3VRVSZRYBs/2anE1ncpzu/BKvYInPJSO0gzRi3dzByShq85TI7DnM22w55\r\n"
    "KT2dxR5ljFWrPLy35oEMLOGKXbXrHguqqWJ+sr0CgYAihBSmBp5KUtR+3ywB4XVF\r\n"
    "V56MRokU2vrvYO071L5VGfw2aQgj71Mnyqou82RhEpSfO6INsbh++KkcXvgamyK9\r\n"
    "ecXXDlHpfaQqW9uKDMSrSjIS/stw1vPuaQ5aUYt+zSPkEZtQitDo3mtoYd75gznz\r\n"
    "oCwhRa+5PS9/8qiujp3WXw==\r\n"
    "-----END PRIVATE KEY-----\r\n"
    "-----BEGIN CERTIFICATE-----\r\n"
    "MIICtzCCAZ+gAwIBAgIBADANBgkqhkiG9w0BAQsFADAfMQswCQYDVQQGEwJTRTEQ\r\n"
    "MA4GA1UEAwwHS3VyZW50bzAeFw0xNjA2MzAxMDUxMzhaFw0xNzA2MzAxMDUxMzha\r\n"
    "MB8xCzAJBgNVBAYTAlNFMRAwDgYDVQQDDAdLdXJlbnRvMIIBIjANBgkqhkiG9w0B\r\n"
    "AQEFAAOCAQ8AMIIBCgKCAQEAm/i0621SFFoKaNj9cSDjaHTVgWa4yqEorgxYJsB7\r\n"
    "0bW7B7t/8FRwcPGgoDH0gJMeX6/6hN1drvVH3Dhdkptvp+haUpeJ92ND49OhpKGd\r\n"
    "xtkOvD3Hud4zqbndUyWxU/XD5eM1J8/t+n3MwVEvZzbBxxL5D17tTWzVrH9v3EQd\r\n"
    "50lJRDc9JSWDA5zUMJhNSACxb8XHGN3AlFLJLt4VCYivUkkh7xjk+NcVueSKX4y9\r\n"
    "NxxMlPngc6PiqgoGexj7WmHWzLYOgDiJLUSIijY6DFOSpr+V2P8HTmRV+NqUord0\r\n"
    "sgWVyHZSdv3IpUcx3U6V9PhSWkDrBNHJgA6lLmfcUFcWFQIDAQABMA0GCSqGSIb3\r\n"
    "DQEBCwUAA4IBAQCNE5tE/ohbaWOTossq2PmYypJjitHUoHfheR9dT5vYm2Izla+z\r\n"
    "AHZVODp9r/25EG3VjKXshL0rSV3ERC5P0wqGBaKCDRJ4pycfA4Fz93byH4r8/6xL\r\n"
    "EcRsG6F8vsMht1yTjq1zFCNN+OeWJtQmXCKKFKLY4+lMsnGyJJGXlW1yJe7D9x9Q\r\n"
    "32DO9KIiRiju+ATHMtrwPwTMgg5Gqd+HmVKhTwcb5RbGwP/xCcK44NLGBdxD8eNi\r\n"
    "bqedNYytvSmEQGEuwlwtA0fNAetr5x7Qegfl4vTWTKogna1xm7SSYqNeOYJZYauV\r\n"
    "1E1yY33Cjjz/BFBW6lqcl6ryeqzTwg/GXXGW\r\n" "-----END CERTIFICATE-----";

GST_START_TEST (test_vp8_sendonly_recvonly_rsa)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE,
      FALSE, rsa_pem);
}
GST_END_TEST

const gchar *ecdsa_pem = "-----BEGIN EC PARAMETERS-----\r\n"
    "BggqhkjOPQMBBw==\r\n"
    "-----END EC PARAMETERS-----\r\n"
    "-----BEGIN EC PRIVATE KEY-----\r\n"
    "MHcCAQEEIMIn2bIr1dCmHepxf8r/NINPMR2rj1v43jPxS3a+HTvIoAoGCCqGSM49\r\n"
    "AwEHoUQDQgAECXHjHX4dtJbSo+9f713PN4rxfcb37XW1G2pDepeI78Fl5oPAKPBI\r\n"
    "Ws4tJWkrPB1pRX0FKpsZyl79i6w3AS/s+Q==\r\n"
    "-----END EC PRIVATE KEY-----\r\n"
    "-----BEGIN CERTIFICATE-----\r\n"
    "MIIBKzCB0aADAgECAgEAMAoGCCqGSM49BAMCMB8xCzAJBgNVBAYTAlNFMRAwDgYD\r\n"
    "VQQDDAdLdXJlbnRvMB4XDTE2MDcxODExMDEyM1oXDTE3MDcxODExMDEyM1owHzEL\r\n"
    "MAkGA1UEBhMCU0UxEDAOBgNVBAMMB0t1cmVudG8wWTATBgcqhkjOPQIBBggqhkjO\r\n"
    "PQMBBwNCAAQJceMdfh20ltKj71/vXc83ivF9xvftdbUbakN6l4jvwWXmg8Ao8Eha\r\n"
    "zi0laSs8HWlFfQUqmxnKXv2LrDcBL+z5MAoGCCqGSM49BAMCA0kAMEYCIQC+mC/s\r\n"
    "6oZzJ6SPfJfJXi5PrOdDDQxhR/aKoxzDbY2SRQIhAL78PAvG56DmpXU2cLTaDlIp\r\n"
    "zjhIHfiZIzPxTHr129TE\r\n" "-----END CERTIFICATE-----";

GST_START_TEST (test_vp8_sendonly_recvonly_ecdsa)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE,
      FALSE, ecdsa_pem);
}
GST_END_TEST

GST_START_TEST (test_vp8_sendrecv)
{
  test_video_sendrecv ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE);
  test_video_sendrecv ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, TRUE);
  test_video_sendrecv ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, TRUE);
}
GST_END_TEST

GST_START_TEST (test_vp8_sendrecv_but_sendonly)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, FALSE,
      FALSE, NULL);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE,
      FALSE, NULL);
}
GST_END_TEST

/* Audio tests */
static GstStaticCaps pcmu_expected_caps = GST_STATIC_CAPS ("audio/x-mulaw");

GST_START_TEST (test_pcmu_sendrecv)
{
  test_audio_sendrecv ("mulawenc", pcmu_expected_caps, "PCMU/8000", FALSE);
  test_audio_sendrecv ("mulawenc", pcmu_expected_caps, "PCMU/8000", TRUE);
}
GST_END_TEST

/* Audio and video tests */
GST_START_TEST (test_pcmu_vp8_sendonly_recvonly)
{
  test_audio_video_sendonly_recvonly ("mulawenc", pcmu_expected_caps,
      "PCMU/8000", "vp8enc", vp8_expected_caps, "VP8/90000", FALSE);
  test_audio_video_sendonly_recvonly ("mulawenc", pcmu_expected_caps,
      "PCMU/8000", "vp8enc", vp8_expected_caps, "VP8/90000", TRUE);
}
GST_END_TEST

GST_START_TEST (test_pcmu_vp8_sendrecv)
{
  test_audio_video_sendrecv ("mulawenc", pcmu_expected_caps, "PCMU/8000",
      "vp8enc", vp8_expected_caps, "VP8/90000", FALSE);
  test_audio_video_sendrecv ("mulawenc", pcmu_expected_caps, "PCMU/8000",
      "vp8enc", vp8_expected_caps, "VP8/90000", TRUE);
}
GST_END_TEST

GST_START_TEST (test_offerer_pcmu_vp8_answerer_vp8_sendrecv)
{
  test_offerer_audio_video_answerer_video_sendrecv ("mulawenc",
      pcmu_expected_caps, "PCMU/8000", "vp8enc", vp8_expected_caps, "VP8/90000",
      FALSE);
  test_offerer_audio_video_answerer_video_sendrecv ("mulawenc",
      pcmu_expected_caps, "PCMU/8000", "vp8enc", vp8_expected_caps, "VP8/90000",
      TRUE);
}
GST_END_TEST

GST_START_TEST (test_remb_params)
{
  GArray *codecs_array;
  gchar *codecs[] = { "VP8/90000", NULL };
  gchar *offerer_sess_id, *answerer_sess_id;
  GstSDPMessage *offer, *answer;
  gchar *sdp_str = NULL;
  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstStructure *remb_params_in = gst_structure_new_empty ("remb-params");
  GstStructure *remb_params_out;
  guint v1, v2;
  gboolean answer_ok;

  codecs_array = create_codecs_array (codecs);
  g_object_set (offerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), NULL);
  g_object_set (answerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), NULL);
  g_array_unref (codecs_array);

  gst_structure_set (remb_params_in, "lineal-factor-min", G_TYPE_INT, 200,
      "remb-on-connect", G_TYPE_INT, 500, NULL);
  g_object_set (offerer, "remb-params", remb_params_in, NULL);
  gst_structure_free (remb_params_in);

  g_object_get (offerer, "remb-params", &remb_params_out, NULL);
  v1 = v2 = 0;
  gst_structure_get (remb_params_out, "lineal-factor-min", G_TYPE_INT, &v1,
      "remb-on-connect", G_TYPE_INT, &v2, NULL);
  gst_structure_free (remb_params_out);
  fail_if (v1 != 200);
  fail_if (v2 != 500);

  /* Check twice to verify that the getter does a copy of the structure */
  g_object_get (offerer, "remb-params", &remb_params_out, NULL);
  v1 = v2 = 0;
  gst_structure_get (remb_params_out, "lineal-factor-min", G_TYPE_INT, &v1,
      "remb-on-connect", G_TYPE_INT, &v2, NULL);
  gst_structure_free (remb_params_out);
  fail_if (v1 != 200);
  fail_if (v2 != 500);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", answerer_sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer,
      &answer_ok);
  fail_unless (answer_ok);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  /* Check that RembLocal has the previous value */
  g_object_get (offerer, "remb-params", &remb_params_out, NULL);
  v1 = v2 = 0;
  gst_structure_get (remb_params_out, "lineal-factor-min", G_TYPE_INT, &v1,
      "remb-on-connect", G_TYPE_INT, &v2, NULL);
  gst_structure_free (remb_params_out);
  fail_if (v1 != 200);
  fail_if (v2 != 500);

  /* This should set RembLocal params instead the aux structure */
  remb_params_in = gst_structure_new_empty ("remb-params");
  gst_structure_set (remb_params_in, "lineal-factor-min", G_TYPE_INT, 300,
      "remb-on-connect", G_TYPE_INT, 600, NULL);
  g_object_set (offerer, "remb-params", remb_params_in, NULL);
  gst_structure_free (remb_params_in);

  g_object_get (offerer, "remb-params", &remb_params_out, NULL);
  v1 = v2 = 0;
  gst_structure_get (remb_params_out, "lineal-factor-min", G_TYPE_INT, &v1,
      "remb-on-connect", G_TYPE_INT, &v2, NULL);
  gst_structure_free (remb_params_out);
  fail_if (v1 != 300);
  fail_if (v2 != 600);

  g_object_unref (offerer);
  g_object_unref (answerer);
  g_free (offerer_sess_id);
  g_free (answerer_sess_id);
}
GST_END_TEST

GST_START_TEST (test_session_creation)
{
  gchar *sess_id;
  gboolean ret;
  GstElement *webrtcendpoint =
      gst_element_factory_make ("webrtcendpoint", NULL);

  g_signal_emit_by_name (webrtcendpoint, "create-session", &sess_id);
  GST_DEBUG_OBJECT (webrtcendpoint, "Created session with id '%s'", sess_id);
  fail_unless (sess_id != NULL);
  g_signal_emit_by_name (webrtcendpoint, "release-session", sess_id, &ret);
  fail_unless (ret);
  g_free (sess_id);

  g_signal_emit_by_name (webrtcendpoint, "create-session", &sess_id);
  GST_DEBUG_OBJECT (webrtcendpoint, "Created session with id '%s'", sess_id);
  fail_unless (sess_id == NULL);

  g_object_unref (webrtcendpoint);
}
GST_END_TEST

typedef struct _CandidateRangeData
{
  guint min_port;
  guint max_port;
} CandidateRangeData;

static void
port_range_on_ice_candidate (GstElement * self, gchar * sess_id,
    KmsIceCandidate * candidate, CandidateRangeData * data)
{
  const guint port = kms_ice_candidate_get_port (candidate);

  GST_DEBUG ("Candidate: '%s'", kms_ice_candidate_get_candidate (candidate));
  GST_DEBUG ("Port: %u, min_port: %u, max_port: %u",
      port, data->min_port, data->max_port);

  // Acording to https://tools.ietf.org/html/rfc6544#section-4.5
  // port == 9 should be discarded
  if (port != 9) {
    fail_if (port > data->max_port);
    fail_if (port < data->min_port);
  }
}

GST_START_TEST (test_port_range)
{
  GArray *codecs_array;
  gchar *codecs[] = { "VP8/90000", NULL };
  gchar *offerer_sess_id;
  GstSDPMessage *offer;
  gchar *sdp_str = NULL;
  gboolean ret = FALSE;
  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  CandidateRangeData offerer_cand_data;

  offerer_cand_data.min_port = 50000;
  offerer_cand_data.max_port = 55000;

  codecs_array = create_codecs_array (codecs);
  g_object_set (offerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), "min-port", offerer_cand_data.min_port,
      "max-port", offerer_cand_data.max_port, NULL);
  g_array_unref (codecs_array);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);

  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (port_range_on_ice_candidate), &offerer_cand_data);

  /* SDP negotiation */
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);

  gst_sdp_message_free (offer);

  g_object_unref (offerer);
  g_free (offerer_sess_id);
}
GST_END_TEST

// ----------------------------------------------------------------------------

// not_enough_ports
// ----------------

typedef struct {
  GHashTable *tcp;
  GHashTable *udp;
} GatheringData;

static void
not_enough_ports_on_ice_candidate (GstElement *self, gchar *sess_id,
    KmsIceCandidate *candidate, GatheringData *gatheringData)
{
  const KmsIceTcpCandidateType tcp_type =
      kms_ice_candidate_get_candidate_tcp_type (candidate);
  const KmsIceProtocol proto = kms_ice_candidate_get_protocol (candidate);

  GST_DEBUG ("SessionId: '%s', candidate: '%s'", sess_id,
      kms_ice_candidate_get_candidate (candidate));

  if (tcp_type == KMS_ICE_TCP_CANDIDATE_TYPE_ACTIVE) {
    return;
  }

  // Check that this candidate doesn't contain a repeated address
  NiceAddress *address = nice_address_new ();
  gboolean ok = nice_address_set_from_string (
      address, kms_ice_candidate_get_address (candidate));
  fail_unless (ok);
  nice_address_set_port (address, kms_ice_candidate_get_port (candidate));

  if (proto == KMS_ICE_PROTOCOL_TCP) {
    fail_if (g_hash_table_contains (gatheringData->tcp, address));
    g_hash_table_add (gatheringData->tcp, address);
  } else {
    fail_if (g_hash_table_contains (gatheringData->udp, address));
    g_hash_table_add (gatheringData->udp, address);
  }
}

GST_START_TEST (test_not_enough_ports)
{
  GArray *codecs_array;
  gchar *codecs[] = {"VP8/90000", NULL};
  gchar *offerer_sess_id, *second_offerer_sess_id;
  GstSDPMessage *offer, *second_offer;
  gchar *sdp_str = NULL;
  gboolean ret = FALSE;
  GstElement *offerer = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *second_offerer =
      gst_element_factory_make ("webrtcendpoint", NULL);

  CandidateRangeData offerer_cand_data;
  offerer_cand_data.min_port = 55000;
  offerer_cand_data.max_port = 55002;

  GatheringData gatheringData = {
      .tcp = g_hash_table_new_full (NULL, (GEqualFunc) &nice_address_equal,
          (GDestroyNotify) &nice_address_free, NULL),
      .udp = g_hash_table_new_full (NULL, (GEqualFunc) &nice_address_equal,
          (GDestroyNotify) &nice_address_free, NULL),
  };

  codecs_array = create_codecs_array (codecs);
  g_object_set (offerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), "min-port", offerer_cand_data.min_port,
      "max-port", offerer_cand_data.max_port, NULL);
  g_object_set (second_offerer, "num-video-medias", 1, "video-codecs",
      g_array_ref (codecs_array), "min-port", offerer_cand_data.min_port,
      "max-port", offerer_cand_data.max_port, NULL);
  g_array_unref (codecs_array);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);

  g_signal_emit_by_name (
      second_offerer, "create-session", &second_offerer_sess_id);
  GST_DEBUG_OBJECT (
      second_offerer, "Created session with id '%s'", second_offerer_sess_id);

  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (not_enough_ports_on_ice_candidate), &gatheringData);

  g_signal_connect (G_OBJECT (second_offerer), "on-ice-candidate",
      G_CALLBACK (not_enough_ports_on_ice_candidate), &gatheringData);

  /* SDP negotiation */
  g_signal_emit_by_name (offerer, "generate-offer", offerer_sess_id, &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (
      second_offerer, "generate-offer", second_offerer_sess_id, &second_offer);
  fail_unless (second_offer != NULL);
  GST_DEBUG (
      "Second offer:\n%s", (sdp_str = gst_sdp_message_as_text (second_offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);

  /* libnice 0.1.13 should fail here because the second offerer cannot get
   * two UDP ports for its two components.
   *
   * libnice 0.1.14 was improved in this regard, and shouldn't fail because
   * even if it doesn't find UDP candidates, it should be able to find
   * TCP-ACTIVE type ones for the second component of the second offerer.
   *
   * Example list of candidate gathering (from libnice-0.1.14 debug log):
   * Component 1:
   * UDP local candidate : [192.168.56.5]:55000 for s1/c1
   * TCP-ACT local candidate : [192.168.56.5]:0 for s1/c1
   * TCP-PASS local candidate : [192.168.56.5]:55000 for s1/c1
   * UDP local candidate : [192.168.1.2]:55000 for s1/c1
   * TCP-ACT local candidate : [192.168.1.2]:0 for s1/c1
   * TCP-PASS local candidate : [192.168.1.2]:55002 for s1/c1
   * Component 2:
   * TCP-ACT local candidate : [192.168.56.5]:0 for s1/c2
   * TCP-ACT local candidate : [192.168.1.2]:0 for s1/c2
   */

  g_signal_emit_by_name (
      second_offerer, "gather-candidates", second_offerer_sess_id, &ret);
  fail_unless (ret);

  gst_sdp_message_free (offer);
  gst_sdp_message_free (second_offer);

  g_object_unref (offerer);
  g_object_unref (second_offerer);
  g_free (offerer_sess_id);
  g_free (second_offerer_sess_id);

  g_hash_table_unref (gatheringData.tcp);
  g_hash_table_unref (gatheringData.udp);
}
GST_END_TEST

// ----------------------------------------------------------------------------

static void
on_ice_candidate_check_mid (GstElement * self, gchar * sess_id,
    KmsIceCandidate * candidate, const gchar * expected_mid)
{
  const gchar *mid;

  mid = kms_ice_candidate_get_sdp_mid (candidate);
  fail_unless (g_strcmp0 (mid, expected_mid) == 0);
}

GST_START_TEST (process_mid_no_bundle_offer)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { "opus/48000/1", NULL };
  gchar *video_codecs[] = { "VP8/90000", NULL };
  GstElement *webrtcendpoint =
      gst_element_factory_make ("webrtcendpoint", NULL);
  gchar *sess_id;
  GstSDPMessage *offer = NULL, *answer = NULL;
  gchar *aux = NULL;
  const GstSDPMedia *media;
  const gchar *mid;
  gboolean ret;

  static const gchar *offer_str = "v=0\r\n"
      "o=mozilla...THIS_IS_SDPARTA-43.0 4115481872190049086 0 IN IP4 0.0.0.0\r\n"
      "s=-\r\n"
      "t=0 0\r\n"
      "a=fingerprint:sha-256 34:05:1B:DC:3E:50:C7:45:15:D4:B7:42:31:1C:D9:11:5B:4D:61:CF:DB:47:B7:EC:E0:76:8E:E7:3D:EB:72:92\r\n"
      "a=ice-options:trickle\r\n"
      "a=msid-semantic:WMS *\r\n"
      "m=video 9 UDP/TLS/RTP/SAVPF 120\r\n"
      "c=IN IP4 0.0.0.0\r\n"
      "a=sendrecv\r\n"
      "a=ice-pwd:ba52db4f140d7f0272f0b5329ef95aa2\r\n"
      "a=ice-ufrag:66d7677a\r\n"
      "a=mid:sdparta_0\r\n"
      "a=msid:{4ab114f1-f994-456c-ba62-272b79006b5d} {a45061ff-7fb6-4208-930c-4c637b839815}\r\n"
      "a=rtcp-fb:120 nack\r\n"
      "a=rtcp-fb:120 nack pli\r\n"
      "a=rtcp-fb:120 ccm fir\r\n"
      "a=rtcp-mux\r\n"
      "a=rtpmap:120 VP8/90000\r\n"
      "a=setup:actpass\r\n"
      "a=ssrc:1841010112 cname:{9e639331-ad7e-4a2e-9a26-9623e3e68ea2}\r\n";

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);

  g_object_set (webrtcendpoint, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);

  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  g_signal_connect (G_OBJECT (webrtcendpoint), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate_check_mid), "sdparta_0");

  fail_unless (gst_sdp_message_new (&offer) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          offer_str, -1, offer) == GST_SDP_OK);

  GST_DEBUG ("Offer:\n%s", (aux = gst_sdp_message_as_text (offer)));
  g_free (aux);
  aux = NULL;

  g_signal_emit_by_name (webrtcendpoint, "create-session", &sess_id);
  GST_DEBUG_OBJECT (webrtcendpoint, "Created session with id '%s'", sess_id);
  g_signal_emit_by_name (webrtcendpoint, "process-offer", sess_id, offer,
      &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (aux = gst_sdp_message_as_text (answer)));
  g_free (aux);
  aux = NULL;

  fail_if (gst_sdp_message_get_attribute_val (answer, "group") != NULL);

  media = gst_sdp_message_get_media ((const GstSDPMessage *) answer, 0);
  mid = gst_sdp_media_get_attribute_val (media, "mid");
  fail_if (g_strcmp0 (mid, "sdparta_0") != 0);

  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (webrtcendpoint, "gather-candidates", sess_id, &ret);
  fail_unless (ret);

  g_object_unref (webrtcendpoint);
  g_free (sess_id);
}
GST_END_TEST

/**
 * "on-ice-candidate" event handler for testing ICE candidate IP.
 * Checks assertion:
 *   ICE Candidate IP has expected value.
 *
 * @param self         webrtcendpoint instance.
 * @param sess_id      webrtcsession ID.
 * @param candidate    ICE Candidate.
 * @param expected_ip  expected IP address.
 */
static void
on_ice_candidate_check_ip (GstElement * self, gchar * sess_id,
    KmsIceCandidate * candidate, const gchar * expected_ip)
{
  gchar *candidate_ip = kms_ice_candidate_get_address (candidate);
  assert_equals_string (candidate_ip, expected_ip);
  g_free (candidate_ip);
}

/**
 * Test setting local network interface to limit ICE candidate gathering.
 */
GST_START_TEST (set_network_interfaces_test)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { "opus/48000/1", NULL };
  gchar *video_codecs[] = { "VP8/90000", NULL };
  GstElement *webrtcendpoint =
      gst_element_factory_make ("webrtcendpoint", NULL);
  gchar *sess_id;
  GstSDPMessage *offer = NULL, *answer = NULL;
  gboolean ret;

  // Check that candidates only include the localhost IP
  g_object_set (webrtcendpoint, "network-interfaces", "lo", NULL);

  static const gchar *offer_str = "v=0\r\n"
      "o=mozilla...THIS_IS_SDPARTA-43.0 4115481872190049086 0 IN IP4 0.0.0.0\r\n"
      "a=ice-options:trickle\r\n"
      "a=msid-semantic:WMS *\r\n"
      "m=video 9 UDP/TLS/RTP/SAVPF 120\r\n"
      "c=IN IP4 0.0.0.0\r\n"
      "a=sendrecv\r\n"
      "a=mid:sdparta_0\r\n"
      "a=rtpmap:120 VP8/90000\r\n";

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (webrtcendpoint, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);

  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  gchar *lo_ip = nice_interfaces_get_ip_for_interface ("lo");
  g_signal_connect (G_OBJECT (webrtcendpoint), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate_check_ip), lo_ip);

  fail_unless (gst_sdp_message_new (&offer) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          offer_str, -1, offer) == GST_SDP_OK);
  g_signal_emit_by_name (webrtcendpoint, "create-session", &sess_id);
  g_signal_emit_by_name (webrtcendpoint, "process-offer", sess_id, offer,
      &answer);
  g_signal_emit_by_name (webrtcendpoint, "gather-candidates", sess_id, &ret);
  fail_unless (ret);
  g_object_unref (webrtcendpoint);
  g_free (sess_id);
  g_free (lo_ip);
}
GST_END_TEST

/**
 * Test setting local network interface to limit ICE candidate gathering.
 */
GST_START_TEST (set_external_address_test)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { "opus/48000/1", NULL };
  gchar *video_codecs[] = { "VP8/90000", NULL };
  GstElement *webrtcendpoint =
      gst_element_factory_make ("webrtcendpoint", NULL);
  gchar *sess_id;
  GstSDPMessage *offer = NULL, *answer = NULL;
  gboolean ret;

  // Check that candidates only include the localhost IP
  g_object_set (webrtcendpoint, "external-address", "10.20.30.40", NULL);

  static const gchar *offer_str = "v=0\r\n"
      "o=mozilla...THIS_IS_SDPARTA-43.0 4115481872190049086 0 IN IP4 0.0.0.0\r\n"
      "a=ice-options:trickle\r\n"
      "a=msid-semantic:WMS *\r\n"
      "m=video 9 UDP/TLS/RTP/SAVPF 120\r\n"
      "c=IN IP4 0.0.0.0\r\n"
      "a=sendrecv\r\n"
      "a=mid:sdparta_0\r\n"
      "a=rtpmap:120 VP8/90000\r\n";

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (webrtcendpoint, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);

  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  g_signal_connect (G_OBJECT (webrtcendpoint), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate_check_ip), "10.20.30.40");

  fail_unless (gst_sdp_message_new (&offer) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          offer_str, -1, offer) == GST_SDP_OK);
  g_signal_emit_by_name (webrtcendpoint, "create-session", &sess_id);
  g_signal_emit_by_name (webrtcendpoint, "process-offer", sess_id, offer,
      &answer);
  g_signal_emit_by_name (webrtcendpoint, "gather-candidates", sess_id, &ret);
  fail_unless (ret);
  g_object_unref (webrtcendpoint);
  g_free (sess_id);
}
GST_END_TEST

/*
 * End of test cases
 */
static Suite *
webrtcendpoint_test_suite (void)
{
  Suite *s = suite_create ("webrtcendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, test_pcmu_sendrecv);
  tcase_add_test (tc_chain, test_vp8_sendrecv_but_sendonly);
  tcase_add_test (tc_chain, test_vp8_sendonly_recvonly);
  tcase_add_test (tc_chain, test_vp8_sendonly_recvonly_rsa);
  tcase_add_test (tc_chain, test_vp8_sendonly_recvonly_ecdsa);
  tcase_add_test (tc_chain, test_vp8_sendrecv);
  tcase_add_test (tc_chain, test_offerer_pcmu_vp8_answerer_vp8_sendrecv);
  tcase_add_test (tc_chain, test_pcmu_vp8_sendrecv);
  tcase_add_test (tc_chain, test_pcmu_vp8_sendonly_recvonly);

  tcase_add_test (tc_chain, test_remb_params);
  tcase_add_test (tc_chain, test_session_creation);
  tcase_add_test (tc_chain, test_port_range);
  tcase_add_test (tc_chain, test_not_enough_ports);

  tcase_add_test (tc_chain, test_webrtc_data_channel);

  tcase_add_test (tc_chain, process_mid_no_bundle_offer);
  tcase_add_test (tc_chain, set_network_interfaces_test);
  tcase_add_test (tc_chain, set_external_address_test);

  return s;
}

GST_CHECK_MAIN (webrtcendpoint_test)
