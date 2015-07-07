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
#include <webrtcendpoint/kmsicecandidate.h>

#include <commons/kmselementpadtype.h>

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

#define AUDIO_SINK "audio-sink"
#define VIDEO_SINK "video-sink"

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

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
  const gchar *type;
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

#define RECEIVER_1_OK "offerer_receives"
#define RECEIVER_2_OK "answerer_receives"

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
  if (GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline), RECEIVER_2_OK))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_data (G_OBJECT (pipeline), RECEIVER_1_OK,
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
  if (GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline), RECEIVER_1_OK))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_data (G_OBJECT (pipeline), RECEIVER_2_OK,
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
    sink = g_object_get_data (G_OBJECT (element), AUDIO_SINK);
  } else if (g_str_has_prefix (GST_PAD_NAME (pad), KMS_VIDEO_PREFIX)) {
    GST_DEBUG_OBJECT (pad, "Connecting video stream");
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
    gboolean check_request_local_key_frame, gboolean gather_asap)
{
  GArray *codecs_array;
  gchar *codecs[] = { codec, NULL };
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  gchar *sender_sess_id, *receiver_sess_id;
  OnIceCandidateData *sender_cand_data, *receiver_cand_data;
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc = gst_element_factory_make (video_enc_name, NULL);
  GstElement *sender = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *receiver = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *outputfakesink = gst_element_factory_make ("fakesink", NULL);
  gchar *sdp_str = NULL;
  gboolean ret;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

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
      "sink_video");

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
  g_signal_emit_by_name (sender, "process-answer", sender_sess_id, answer);
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
  g_object_set_data (G_OBJECT (receiver), VIDEO_SINK, outputfakesink);
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
  g_slice_free (OnIceCandidateData, sender_cand_data);
  g_slice_free (OnIceCandidateData, receiver_cand_data);
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
  OnIceCandidateData *offerer_cand_data, *answerer_cand_data;
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
  offerer_cand_data = g_slice_new0 (OnIceCandidateData);
  offerer_cand_data->peer = answerer;
  offerer_cand_data->peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), offerer_cand_data);

  answerer_cand_data = g_slice_new0 (OnIceCandidateData);
  answerer_cand_data->peer = offerer;
  answerer_cand_data->peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), answerer_cand_data);

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
      pipeline, "sink_video");

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, videotestsrc_answerer, video_enc_answerer, NULL,
      pipeline, "sink_video");

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
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

  g_object_set_data (G_OBJECT (offerer), VIDEO_SINK, fakesink_offerer);
  g_signal_connect (offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  g_object_set_data (G_OBJECT (answerer), VIDEO_SINK, fakesink_answerer);
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
  g_slice_free (OnIceCandidateData, offerer_cand_data);
  g_slice_free (OnIceCandidateData, answerer_cand_data);
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
  OnIceCandidateData *offerer_cand_data, *answerer_cand_data;
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
  offerer_cand_data = g_slice_new0 (OnIceCandidateData);
  offerer_cand_data->peer = answerer;
  offerer_cand_data->peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), offerer_cand_data);

  answerer_cand_data = g_slice_new0 (OnIceCandidateData);
  answerer_cand_data->peer = offerer;
  answerer_cand_data->peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), answerer_cand_data);

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
      capsfilter_offerer, pipeline, "sink_audio");

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, audiotestsrc_answerer, audio_enc_answerer,
      capsfilter_answerer, pipeline, "sink_audio");

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
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

  g_object_set_data (G_OBJECT (offerer), AUDIO_SINK, fakesink_offerer);
  g_signal_connect (offerer, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  g_object_set_data (G_OBJECT (answerer), AUDIO_SINK, fakesink_answerer);
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
  g_slice_free (OnIceCandidateData, offerer_cand_data);
  g_slice_free (OnIceCandidateData, answerer_cand_data);
}

#define OFFERER_RECEIVES_AUDIO "offerer_receives_audio"
#define OFFERER_RECEIVES_VIDEO "offerer_receives_video"
#define ANSWERER_RECEIVES_AUDIO "answerer_receives_audio"
#define ANSWERER_RECEIVES_VIDEO "answerer_receives_video"

static gboolean
check_offerer_and_answerer_receive_audio_and_video (gpointer pipeline)
{
  return GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
          OFFERER_RECEIVES_AUDIO)) &&
      GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
          OFFERER_RECEIVES_VIDEO)) &&
      GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
          ANSWERER_RECEIVES_AUDIO)) &&
      GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
          ANSWERER_RECEIVES_VIDEO));
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
  g_object_set_data (G_OBJECT (pipeline), hod->type, GINT_TO_POINTER (TRUE));
  if (check_offerer_and_answerer_receive_audio_and_video (pipeline)) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
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
  g_object_set_data (G_OBJECT (pipeline), OFFERER_RECEIVES_AUDIO,
      GINT_TO_POINTER (TRUE));
  g_object_set_data (G_OBJECT (pipeline), OFFERER_RECEIVES_VIDEO,
      GINT_TO_POINTER (TRUE));

  hod_audio = g_slice_new0 (HandOffData);
  hod_audio->type = ANSWERER_RECEIVES_AUDIO;
  hod_audio->expected_caps = audio_expected_caps;
  hod_audio->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (audio_fakesink), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio);

  hod_video = g_slice_new0 (HandOffData);
  hod_video->type = ANSWERER_RECEIVES_VIDEO;
  hod_video->expected_caps = video_expected_caps;
  hod_video->loop = loop;
  g_object_set (G_OBJECT (video_fakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (video_fakesink), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_video);

  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (audiotestsrc), "is-live", TRUE, NULL);

  caps = gst_caps_new_simple ("audio/x-raw", "rate", G_TYPE_INT, 8000, NULL);
  g_object_set (capsfilter, "caps", caps, NULL);
  gst_caps_unref (caps);

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), sender);
  connect_sink_async (sender, audiotestsrc, audio_enc, capsfilter, pipeline,
      "sink_audio");
  connect_sink_async (sender, videotestsrc, video_enc, NULL, pipeline,
      "sink_video");

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
  g_signal_emit_by_name (sender, "process-answer", sender_sess_id, answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (sender, "gather-candidates", sender_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (receiver, "gather-candidates", receiver_sess_id, &ret);
  fail_unless (ret);

  gst_bin_add (GST_BIN (pipeline), audio_fakesink);
  g_object_set_data (G_OBJECT (receiver), AUDIO_SINK, audio_fakesink);
  g_signal_connect (receiver, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (receiver,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  gst_bin_add (GST_BIN (pipeline), video_fakesink);
  g_object_set_data (G_OBJECT (receiver), VIDEO_SINK, video_fakesink);
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
  OnIceCandidateData *offerer_cand_data, *answerer_cand_data;
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
  offerer_cand_data = g_slice_new0 (OnIceCandidateData);
  offerer_cand_data->peer = answerer;
  offerer_cand_data->peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), offerer_cand_data);

  answerer_cand_data = g_slice_new0 (OnIceCandidateData);
  answerer_cand_data->peer = offerer;
  answerer_cand_data->peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), answerer_cand_data);

  hod_audio_offerer = g_slice_new0 (HandOffData);
  hod_audio_offerer->type = OFFERER_RECEIVES_AUDIO;
  hod_audio_offerer->expected_caps = audio_expected_caps;
  hod_audio_offerer->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (audio_fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio_offerer);

  hod_video_offerer = g_slice_new0 (HandOffData);
  hod_video_offerer->type = OFFERER_RECEIVES_VIDEO;
  hod_video_offerer->expected_caps = video_expected_caps;
  hod_video_offerer->loop = loop;
  g_object_set (G_OBJECT (video_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (video_fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_video_offerer);

  hod_audio_answerer = g_slice_new0 (HandOffData);
  hod_audio_answerer->type = ANSWERER_RECEIVES_AUDIO;
  hod_audio_answerer->expected_caps = audio_expected_caps;
  hod_audio_answerer->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink_answerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (audio_fakesink_answerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio_answerer);

  hod_video_answerer = g_slice_new0 (HandOffData);
  hod_video_answerer->type = ANSWERER_RECEIVES_VIDEO;
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
      capsfilter_offerer, pipeline, "sink_audio");
  connect_sink_async (offerer, videotestsrc_offerer, video_enc_offerer, NULL,
      pipeline, "sink_video");

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, audiotestsrc_answerer, audio_enc_answerer,
      capsfilter_answerer, pipeline, "sink_audio");
  connect_sink_async (answerer, videotestsrc_answerer, video_enc_answerer, NULL,
      pipeline, "sink_video");

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
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer);
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

  g_object_set_data (G_OBJECT (offerer), AUDIO_SINK, audio_fakesink_offerer);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));
  g_object_set_data (G_OBJECT (answerer), AUDIO_SINK, audio_fakesink_answerer);
  fail_unless (kms_element_request_srcpad (answerer,
          KMS_ELEMENT_PAD_TYPE_AUDIO));

  gst_bin_add_many (GST_BIN (pipeline), video_fakesink_offerer,
      video_fakesink_answerer, NULL);

  g_object_set_data (G_OBJECT (offerer), VIDEO_SINK, video_fakesink_offerer);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));
  g_object_set_data (G_OBJECT (answerer), VIDEO_SINK, video_fakesink_answerer);
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
  g_slice_free (OnIceCandidateData, offerer_cand_data);
  g_slice_free (OnIceCandidateData, answerer_cand_data);
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
  OnIceCandidateData *offerer_cand_data, *answerer_cand_data;
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

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (offerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), "bundle", bundle, NULL);

  /* Answerer only support video */
  g_object_set (answerer, "num-audio-medias", 0, "num-video-medias", 1,
      "video-codecs", g_array_ref (video_codecs_array), NULL);
  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

  /* Session creation */
  g_signal_emit_by_name (offerer, "create-session", &offerer_sess_id);
  GST_DEBUG_OBJECT (offerer, "Created session with id '%s'", offerer_sess_id);
  g_signal_emit_by_name (answerer, "create-session", &answerer_sess_id);
  GST_DEBUG_OBJECT (answerer, "Created session with id '%s'", answerer_sess_id);

  /* Trickle ICE management */
  offerer_cand_data = g_slice_new0 (OnIceCandidateData);
  offerer_cand_data->peer = answerer;
  offerer_cand_data->peer_sess_id = answerer_sess_id;
  g_signal_connect (G_OBJECT (offerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), offerer_cand_data);

  answerer_cand_data = g_slice_new0 (OnIceCandidateData);
  answerer_cand_data->peer = offerer;
  answerer_cand_data->peer_sess_id = offerer_sess_id;
  g_signal_connect (G_OBJECT (answerer), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), answerer_cand_data);

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

  /* Add elements */
  gst_bin_add (GST_BIN (pipeline), offerer);
  connect_sink_async (offerer, videotestsrc_offerer, video_enc_offerer, NULL,
      pipeline, "sink_video");

  gst_bin_add (GST_BIN (pipeline), answerer);
  connect_sink_async (answerer, videotestsrc_answerer, video_enc_answerer, NULL,
      pipeline, "sink_video");

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
  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_signal_emit_by_name (offerer, "gather-candidates", offerer_sess_id, &ret);
  fail_unless (ret);
  g_signal_emit_by_name (answerer, "gather-candidates", answerer_sess_id, &ret);
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

  g_object_set_data (G_OBJECT (offerer), VIDEO_SINK, video_fakesink_offerer);
  fail_unless (kms_element_request_srcpad (offerer,
          KMS_ELEMENT_PAD_TYPE_VIDEO));
  g_object_set_data (G_OBJECT (answerer), VIDEO_SINK, video_fakesink_answerer);
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
  g_slice_free (OnIceCandidateData, offerer_cand_data);
  g_slice_free (OnIceCandidateData, answerer_cand_data);
}

#ifdef ENABLE_DEBUGGING_TESTS
static void
dummysrc_pad_added (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  GstPad *sink_pad = GST_PAD (user_data);

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);

  fail_unless (GST_PAD_IS_SRC (new_pad));

  GST_DEBUG ("Linking %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, new_pad,
      sink_pad);
  fail_unless (gst_pad_link (new_pad, sink_pad) == GST_PAD_LINK_OK);
}

static void
webrtc_sender_pad_added (GstElement * element, GstPad * new_pad,
    gpointer user_data)
{
  GstElement *dummysrc, *pipeline = GST_ELEMENT (user_data);
  gchar *padname = NULL;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);

  fail_unless (GST_PAD_IS_SINK (new_pad));

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  gst_bin_add (GST_BIN (pipeline), dummysrc);

  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (dummysrc_pad_added),
      new_pad);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-srcpad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "data", TRUE, NULL);

  g_free (padname);

  gst_element_sync_state_with_parent_target_state (dummysrc);
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
  g_object_set (G_OBJECT (fakesink), "sync", FALSE, "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (fakesink, "handoff", G_CALLBACK (fakesink_handoff),
      tmp->loop);

  gst_bin_add (GST_BIN (pipeline), fakesink);

  sink_pad = gst_element_get_static_pad (fakesink, "sink");
  fail_unless (gst_pad_link (new_pad, sink_pad) == GST_PAD_LINK_OK);
  g_object_unref (sink_pad);

  gst_element_sync_state_with_parent_target_state (fakesink);
}

static void
test_data_channels (gboolean bundle)
{
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

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (sender, "use-data-channels", TRUE, NULL);
  g_object_set (receiver, "use-data-channels", TRUE, NULL);

  gst_bin_add_many (GST_BIN (pipeline), sender, receiver, NULL);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_object_set (sender, "max-video-recv-bandwidth", 0, NULL);
  g_object_set (receiver, "max-video-recv-bandwidth", 0, NULL);

  g_object_set (sender, "bundle", bundle, "num-audio-medias", 0,
      "num-video-medias", 0, NULL);
  g_object_set (receiver, "num-audio-medias", "num-video-medias", 0, NULL);

  g_signal_connect (sender, "pad-added", G_CALLBACK (webrtc_sender_pad_added),
      pipeline);

  tmp.pipeline = pipeline;
  tmp.loop = loop;

  g_signal_connect (receiver, "pad-added",
      G_CALLBACK (webrtc_receiver_pad_added), &tmp);

  g_signal_emit_by_name (receiver, "request-new-srcpad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad name %s", padname);
  g_free (padname);

  /* Trickle ICE management */
  g_signal_connect (G_OBJECT (sender), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), receiver);
  g_signal_connect (G_OBJECT (receiver), "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), sender);

  /* SDP Negotiation */
  g_signal_emit_by_name (sender, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (receiver, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  g_signal_emit_by_name (sender, "process-answer", answer);

  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_object_get (sender, "local-sdp", &offerer_local_sdp, NULL);
  fail_unless (offerer_local_sdp != NULL);
  g_object_get (sender, "remote-sdp", &offerer_remote_sdp, NULL);
  fail_unless (offerer_remote_sdp != NULL);

  g_object_get (receiver, "local-sdp", &answerer_local_sdp, NULL);
  fail_unless (answerer_local_sdp != NULL);
  g_object_get (receiver, "remote-sdp", &answerer_remote_sdp, NULL);
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

  g_signal_emit_by_name (sender, "gather-candidates", &ret);
  fail_unless (ret);

  g_signal_emit_by_name (receiver, "gather-candidates", &ret);
  fail_unless (ret);

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendonly_end");

  GST_WARNING ("Finishing test");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_START_TEST (test_webrtc_data_channel)
{
  /* Check data channels in a bundle connection */
  test_data_channels (TRUE);

  /* Check data channels in a dedicated connection */
  test_data_channels (FALSE);
}

GST_END_TEST
#endif
/* Video tests */
static GstStaticCaps vp8_expected_caps = GST_STATIC_CAPS ("video/x-vp8");

GST_START_TEST (test_vp8_sendonly_recvonly)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE,
      FALSE);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, FALSE,
      FALSE);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, TRUE,
      FALSE);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", TRUE, FALSE,
      TRUE);
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
      FALSE);
  test_video_sendonly ("vp8enc", vp8_expected_caps, "VP8/90000", FALSE, FALSE,
      FALSE);
}

GST_END_TEST
/* Audio tests */
static GstStaticCaps pcmu_expected_caps = GST_STATIC_CAPS ("audio/x-mulaw");

GST_START_TEST (test_pcmu_sendrecv)
{
  test_audio_sendrecv ("mulawenc", pcmu_expected_caps, "PCMU/8000", FALSE);
  test_audio_sendrecv ("mulawenc", pcmu_expected_caps, "PCMU/8000", TRUE);
}

/* Audio and video tests */
GST_END_TEST
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

  g_signal_emit_by_name (offerer, "process-answer", offerer_sess_id, answer);
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
  tcase_add_test (tc_chain, test_vp8_sendrecv);
  tcase_add_test (tc_chain, test_offerer_pcmu_vp8_answerer_vp8_sendrecv);

  tcase_add_test (tc_chain, test_pcmu_vp8_sendrecv);
  tcase_add_test (tc_chain, test_pcmu_vp8_sendonly_recvonly);

  tcase_add_test (tc_chain, test_remb_params);

#ifdef ENABLE_DEBUGGING_TESTS
  tcase_add_test (tc_chain, test_webrtc_data_channel);
#endif

  return s;
}

GST_CHECK_MAIN (webrtcendpoint_test);
