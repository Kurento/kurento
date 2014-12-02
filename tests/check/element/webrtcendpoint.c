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
} HandOffData;

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

  g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
  g_idle_add (quit_main_loop_idle, hod->loop);
}

static void
test_video_sendonly (const gchar * video_enc_name, GstStaticCaps expected_caps,
    const gchar * pattern_sdp_sendonly_str,
    const gchar * pattern_sdp_recvonly_str)
{
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (NULL);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *video_enc = gst_element_factory_make (video_enc_name, NULL);
  GstElement *sender = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *receiver = gst_element_factory_make ("webrtcendpoint", NULL);
  GstElement *outputfakesink = gst_element_factory_make ("fakesink", NULL);
  gchar *sdp_str = NULL;

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendonly_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (sender, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_recvonly_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (receiver, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  hod = g_slice_new (HandOffData);
  hod->expected_caps = expected_caps;
  hod->loop = loop;

  g_object_set (G_OBJECT (outputfakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (outputfakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), hod);

  /* Add elements */
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, video_enc, sender, NULL);
  gst_element_link (videotestsrc, video_enc);
  gst_element_link_pads (video_enc, NULL, sender, "video_sink");

  gst_bin_add (GST_BIN (pipeline), receiver);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (sender, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (receiver, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (sender, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add (GST_BIN (pipeline), outputfakesink);
  kms_element_link_pads (receiver, "video_src_%u", outputfakesink, "sink");
  gst_element_sync_state_with_parent (outputfakesink);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendonly_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendonly_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
}

#define OFFERER_RECEIVES "offerer_receives"
#define ANSWERER_RECEIVES "answerer_receives"

G_LOCK_DEFINE_STATIC (check_receive_lock);

static void
sendrecv_offerer_fakesink_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer data)
{
  HandOffData *hod = (HandOffData *) data;
  GstElement *pipeline;

  if (!check_caps (pad, hod)) {
    return;
  }

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
              ANSWERER_RECEIVES))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_data (G_OBJECT (pipeline), OFFERER_RECEIVES,
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

  if (!check_caps (pad, hod)) {
    return;
  }

  pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_data (G_OBJECT (pipeline),
              OFFERER_RECEIVES))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop_idle, hod->loop);
  } else {
    g_object_set_data (G_OBJECT (pipeline), ANSWERER_RECEIVES,
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
test_video_sendrecv (const gchar * video_enc_name,
    GstStaticCaps expected_caps, const gchar * pattern_sdp_sendrcv_str)
{
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
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

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendrcv_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (offerer, "pattern-sdp", pattern_sdp, NULL);
  g_object_set (answerer, "pattern-sdp", pattern_sdp, NULL);
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
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc_offerer, video_enc_offerer,
      offerer, NULL);
  gst_element_link (videotestsrc_offerer, video_enc_offerer);
  gst_element_link_pads (video_enc_offerer, NULL, offerer, "video_sink");

  gst_bin_add_many (GST_BIN (pipeline)
      , videotestsrc_answerer, video_enc_answerer, answerer, NULL);
  gst_element_link (videotestsrc_answerer, video_enc_answerer);
  gst_element_link_pads (video_enc_answerer, NULL, answerer, "video_sink");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

  kms_element_link_pads (offerer, "video_src_%u", fakesink_offerer, "sink");
  kms_element_link_pads (answerer, "video_src_%u", fakesink_answerer, "sink");
  gst_element_sync_state_with_parent (fakesink_offerer);
  gst_element_sync_state_with_parent (fakesink_answerer);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
}

static void
test_audio_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps expected_caps, const gchar * pattern_sdp_sendrcv_str)
{
  HandOffData *hod;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
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

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendrcv_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (offerer, "pattern-sdp", pattern_sdp, NULL);
  g_object_set (answerer, "pattern-sdp", pattern_sdp, NULL);
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

  g_object_set (G_OBJECT (audiotestsrc_offerer), "is-live", TRUE, NULL);
  g_object_set (G_OBJECT (audiotestsrc_answerer), "is-live", TRUE, NULL);

  caps = gst_caps_new_simple ("audio/x-raw", "rate", G_TYPE_INT, 8000, NULL);
  g_object_set (capsfilter_offerer, "caps", caps, NULL);
  g_object_set (capsfilter_answerer, "caps", caps, NULL);
  gst_caps_unref (caps);

  /* Add elements */
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc_offerer, audio_enc_offerer,
      capsfilter_offerer, offerer, NULL);
  gst_element_link_many (audiotestsrc_offerer, capsfilter_offerer,
      audio_enc_offerer, NULL);
  gst_element_link_pads (audio_enc_offerer, NULL, offerer, "audio_sink");

  gst_bin_add_many (GST_BIN (pipeline)
      , audiotestsrc_answerer, audio_enc_answerer, capsfilter_answerer,
      answerer, NULL);
  gst_element_link_many (audiotestsrc_answerer, capsfilter_answerer,
      audio_enc_answerer, NULL);
  gst_element_link_pads (audio_enc_answerer, NULL, answerer, "audio_sink");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add_many (GST_BIN (pipeline), fakesink_offerer, fakesink_answerer,
      NULL);

  kms_element_link_pads (offerer, "audio_src_%u", fakesink_offerer, "sink");
  kms_element_link_pads (answerer, "audio_src_%u", fakesink_answerer, "sink");
  gst_element_sync_state_with_parent (fakesink_offerer);
  gst_element_sync_state_with_parent (fakesink_answerer);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_sendrecv_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod);
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
    GstStaticCaps audio_expected_caps, const gchar * video_enc_name,
    GstStaticCaps video_expected_caps, const gchar * pattern_sdp_sendonly_str,
    const gchar * pattern_sdp_recvonly_str)
{
  HandOffData *hod_audio, *hod_video;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
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

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendonly_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (sender, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_recvonly_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (receiver, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  /* Hack to avoid audio and video reception in sender(offerer) */
  g_object_set_data (G_OBJECT (pipeline), OFFERER_RECEIVES_AUDIO,
      GINT_TO_POINTER (TRUE));
  g_object_set_data (G_OBJECT (pipeline), OFFERER_RECEIVES_VIDEO,
      GINT_TO_POINTER (TRUE));

  hod_audio = g_slice_new (HandOffData);
  hod_audio->type = ANSWERER_RECEIVES_AUDIO;
  hod_audio->expected_caps = audio_expected_caps;
  hod_audio->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (audio_fakesink), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio);

  hod_video = g_slice_new (HandOffData);
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
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc, audio_enc,
      capsfilter, NULL);
  gst_element_link_many (audiotestsrc, capsfilter, audio_enc, NULL);
  gst_element_link_pads (audio_enc, NULL, sender, "audio_sink");

  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, video_enc, NULL);
  gst_element_link (videotestsrc, video_enc);
  gst_element_link_pads (video_enc, NULL, sender, "video_sink");

  gst_bin_add (GST_BIN (pipeline), receiver);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (sender, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (receiver, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (sender, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add (GST_BIN (pipeline), audio_fakesink);
  kms_element_link_pads (receiver, "audio_src_%u", audio_fakesink, "sink");
  gst_element_sync_state_with_parent (audio_fakesink);

  gst_bin_add (GST_BIN (pipeline), video_fakesink);
  kms_element_link_pads (receiver, "video_src_%u", video_fakesink, "sink");
  gst_element_sync_state_with_parent (video_fakesink);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL,
      "test_audio_video_sendonly_recvonly_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_audio_video_sendonly_recvonly_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod_audio);
  g_slice_free (HandOffData, hod_video);
}

static void
test_audio_video_sendrecv (const gchar * audio_enc_name,
    GstStaticCaps audio_expected_caps, const gchar * video_enc_name,
    GstStaticCaps video_expected_caps, const gchar * pattern_sdp_sendrcv_str)
{
  HandOffData *hod_audio_offerer, *hod_video_offerer, *hod_audio_answerer,
      *hod_video_answerer;
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *pattern_sdp, *offer, *answer;
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

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_sdp_sendrcv_str, -1, pattern_sdp) == GST_SDP_OK);
  g_object_set (offerer, "pattern-sdp", pattern_sdp, NULL);
  g_object_set (answerer, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  hod_audio_offerer = g_slice_new (HandOffData);
  hod_audio_offerer->type = OFFERER_RECEIVES_AUDIO;
  hod_audio_offerer->expected_caps = audio_expected_caps;
  hod_audio_offerer->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (audio_fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio_offerer);

  hod_video_offerer = g_slice_new (HandOffData);
  hod_video_offerer->type = OFFERER_RECEIVES_VIDEO;
  hod_video_offerer->expected_caps = video_expected_caps;
  hod_video_offerer->loop = loop;
  g_object_set (G_OBJECT (video_fakesink_offerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (video_fakesink_offerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_video_offerer);

  hod_audio_answerer = g_slice_new (HandOffData);
  hod_audio_answerer->type = ANSWERER_RECEIVES_AUDIO;
  hod_audio_answerer->expected_caps = audio_expected_caps;
  hod_audio_answerer->loop = loop;
  g_object_set (G_OBJECT (audio_fakesink_answerer), "signal-handoffs", TRUE,
      NULL);
  g_signal_connect (G_OBJECT (audio_fakesink_answerer), "handoff",
      G_CALLBACK (sendrecv_fakesink_hand_off), hod_audio_answerer);

  hod_video_answerer = g_slice_new (HandOffData);
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
  gst_bin_add_many (GST_BIN (pipeline), audiotestsrc_offerer, audio_enc_offerer,
      capsfilter_offerer, NULL);
  gst_element_link_many (audiotestsrc_offerer, capsfilter_offerer,
      audio_enc_offerer, NULL);
  gst_element_link_pads (audio_enc_offerer, NULL, offerer, "audio_sink");

  gst_bin_add_many (GST_BIN (pipeline), videotestsrc_offerer, video_enc_offerer,
      NULL);
  gst_element_link (videotestsrc_offerer, video_enc_offerer);
  gst_element_link_pads (video_enc_offerer, NULL, offerer, "video_sink");

  gst_bin_add (GST_BIN (pipeline), answerer);
  gst_bin_add_many (GST_BIN (pipeline)
      , audiotestsrc_answerer, audio_enc_answerer, capsfilter_answerer, NULL);
  gst_element_link_many (audiotestsrc_answerer, capsfilter_answerer,
      audio_enc_answerer, NULL);
  gst_element_link_pads (audio_enc_answerer, NULL, answerer, "audio_sink");

  gst_bin_add_many (GST_BIN (pipeline), videotestsrc_answerer,
      video_enc_answerer, NULL);
  gst_element_link (videotestsrc_answerer, video_enc_answerer);
  gst_element_link_pads (video_enc_answerer, NULL, answerer, "video_sink");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* SDP negotiation */
  mark_point ();
  g_signal_emit_by_name (offerer, "generate-offer", &offer);
  fail_unless (offer != NULL);
  GST_DEBUG ("Offer:\n%s", (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (answerer, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  mark_point ();
  g_signal_emit_by_name (offerer, "process-answer", answer);
  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  gst_bin_add_many (GST_BIN (pipeline), audio_fakesink_offerer,
      audio_fakesink_answerer, NULL);
  kms_element_link_pads (offerer, "audio_src_%u", audio_fakesink_offerer,
      "sink");
  kms_element_link_pads (answerer, "audio_src_%u", audio_fakesink_answerer,
      "sink");
  gst_element_sync_state_with_parent (audio_fakesink_offerer);
  gst_element_sync_state_with_parent (audio_fakesink_answerer);

  gst_bin_add_many (GST_BIN (pipeline), video_fakesink_offerer,
      video_fakesink_answerer, NULL);
  kms_element_link_pads (offerer, "video_src_%u", video_fakesink_offerer,
      "sink");
  kms_element_link_pads (answerer, "video_src_%u", video_fakesink_answerer,
      "sink");
  gst_element_sync_state_with_parent (video_fakesink_offerer);
  gst_element_sync_state_with_parent (video_fakesink_answerer);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_before_entering_loop");

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_sendrecv_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
  g_slice_free (HandOffData, hod_audio_offerer);
  g_slice_free (HandOffData, hod_video_offerer);
  g_slice_free (HandOffData, hod_audio_answerer);
  g_slice_free (HandOffData, hod_video_answerer);
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

GST_START_TEST (negotiation)
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

  g_object_set (offerer, "max-video-recv-bandwidth", 0, NULL);
  g_object_set (answerer, "max-video-recv-bandwidth", 0, NULL);

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

GST_END_TEST
/* Video tests */
static GstStaticCaps vp8_expected_caps = GST_STATIC_CAPS ("video/x-vp8");

static const gchar *pattern_sdp_vp8_sendonly_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=video 0 RTP/AVP 96\r\n" "a=rtpmap:96 VP8/90000\r\n" "a=sendonly\r\n";

static const gchar *pattern_sdp_vp8_recvonly_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=video 0 RTP/AVP 96\r\n" "a=rtpmap:96 VP8/90000\r\n" "a=recvonly\r\n";

static const gchar *pattern_sdp_vp8_sendrecv_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=video 0 RTP/AVP 96\r\n" "a=rtpmap:96 VP8/90000\r\n" "a=sendrecv\r\n";

GST_START_TEST (test_vp8_sendonly_recvonly)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps,
      pattern_sdp_vp8_sendonly_str, pattern_sdp_vp8_recvonly_str);
}

GST_END_TEST
GST_START_TEST (test_vp8_sendrecv)
{
  test_video_sendrecv ("vp8enc", vp8_expected_caps,
      pattern_sdp_vp8_sendrecv_str);
}

GST_END_TEST
GST_START_TEST (test_vp8_sendrecv_but_sendonly)
{
  test_video_sendonly ("vp8enc", vp8_expected_caps,
      pattern_sdp_vp8_sendrecv_str, pattern_sdp_vp8_sendrecv_str);
}

GST_END_TEST
/* Audio tests */
static GstStaticCaps pcmu_expected_caps = GST_STATIC_CAPS ("audio/x-mulaw");

static const gchar *pattern_sdp_pcmu_sendrecv_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n" "c=IN IP4 0.0.0.0\r\n" "t=0 0\r\n"
    "m=audio 0 RTP/AVP 0\r\n" "a=rtpmap:0 PCMU/8000\r\n" "a=sendrecv\r\n";

GST_START_TEST (test_pcmu_sendrecv)
{
  test_audio_sendrecv ("mulawenc", pcmu_expected_caps,
      pattern_sdp_pcmu_sendrecv_str);
}

/* Audio and video tests */
GST_END_TEST
    static const gchar *pattern_sdp_pcmu_vp8_sendonly_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=audio 0 RTP/AVP 0\r\n" "a=rtpmap:0 PCMU/8000\r\n" "a=sendonly\r\n"
    "m=video 0 RTP/AVP 96\r\n" "a=rtpmap:96 VP8/90000\r\n" "a=sendonly\r\n";

static const gchar *pattern_sdp_pcmu_vp8_recvonly_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=audio 0 RTP/AVP 0\r\n" "a=rtpmap:0 PCMU/8000\r\n" "a=recvonly\r\n"
    "m=video 0 RTP/AVP 96\r\n" "a=rtpmap:96 VP8/90000\r\n" "a=recvonly\r\n";

static const gchar *pattern_sdp_pcmu_vp8_sendrecv_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=audio 0 RTP/AVP 0\r\n" "a=rtpmap:0 PCMU/8000\r\n" "a=sendrecv\r\n"
    "m=video 0 RTP/AVP 96\r\n" "a=rtpmap:96 VP8/90000\r\n" "a=sendrecv\r\n";

GST_START_TEST (test_pcmu_vp8_sendonly_recvonly)
{
  test_audio_video_sendonly_recvonly ("mulawenc", pcmu_expected_caps, "vp8enc",
      vp8_expected_caps, pattern_sdp_pcmu_vp8_sendonly_str,
      pattern_sdp_pcmu_vp8_recvonly_str);
}

GST_END_TEST
GST_START_TEST (test_pcmu_vp8_sendrecv)
{
  test_audio_video_sendrecv ("mulawenc", pcmu_expected_caps, "vp8enc",
      vp8_expected_caps, pattern_sdp_pcmu_vp8_sendrecv_str);
}

GST_END_TEST
GST_START_TEST (test_pcmu_vp8_sendrecv_but_sendonly)
{
  test_audio_video_sendonly_recvonly ("mulawenc", pcmu_expected_caps, "vp8enc",
      vp8_expected_caps, pattern_sdp_pcmu_vp8_sendrecv_str,
      pattern_sdp_pcmu_vp8_sendrecv_str);
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
  tcase_add_test (tc_chain, negotiation);

  tcase_add_test (tc_chain, test_pcmu_sendrecv);
  tcase_add_test (tc_chain, test_vp8_sendrecv_but_sendonly);

  tcase_add_test (tc_chain, test_vp8_sendonly_recvonly);
  tcase_add_test (tc_chain, test_vp8_sendrecv);

  tcase_add_test (tc_chain, test_pcmu_vp8_sendrecv);
  tcase_add_test (tc_chain, test_pcmu_vp8_sendonly_recvonly);
  tcase_add_test (tc_chain, test_pcmu_vp8_sendrecv_but_sendonly);

  return s;
}

GST_CHECK_MAIN (webrtcendpoint_test);
