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
#include <gst/gst.h>
#include <glib.h>

#include <kmstestutils.h>

#include <commons/kmselementpadtype.h>

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

#define AUDIO_SINK "audio-sink"
#define VIDEO_SINK "video-sink"

static const gchar *pattern_offer_sdp_str = "v=0\r\n"
    "o=- 0 0 IN IP4 0.0.0.0\r\n"
    "s=TestSession\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "t=0 0\r\n"
    "m=video 0 RTP/AVP 96 97\r\n"
    "a=rtpmap:96 H263-1998/90000\r\n"
    "a=rtpmap:97 VP8/90000\r\n"
    "m=audio 0 RTP/AVP 98 99\r\n"
    "a=rtpmap:98 OPUS/48000/1\r\n" "a=rtpmap:99 AMR/8000/1\r\n";

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
quit_main_loop (gpointer data)
{
  g_main_loop_quit (data);
  return FALSE;
}

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{

  switch (msg->type) {
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

static void
fakesink_hand_off (GstElement * fakesink, GstBuffer * buf, GstPad * pad,
    gpointer data)
{
  static int count = 0;
  GMainLoop *loop = (GMainLoop *) data;

  count++;
  GST_DEBUG ("count: %d", count);
  if (count > 40) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop, loop);
  }
}

static gboolean
timeout_check (gpointer pipeline)
{
  gchar *timeout_file =
      g_strdup_printf ("timeout-%s", GST_OBJECT_NAME (pipeline));

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, timeout_file);
  g_free (timeout_file);
  return FALSE;
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

typedef struct _KmsConnectData
{
  GstElement *agnostic;
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

  gst_element_link_pads (data->agnostic, NULL, element, GST_OBJECT_NAME (pad));
  gst_element_sync_state_with_parent (data->agnostic);

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
connect_sink_async (GstElement * rtpendpoint, GstElement * agnostic,
    GstElement * pipe, const gchar * pad_prefix)
{
  KmsConnectData *data = g_slice_new (KmsConnectData);

  data->agnostic = agnostic;
  data->pipe = GST_BIN (pipe);
  data->pad_prefix = pad_prefix;

  data->id =
      g_signal_connect_data (rtpendpoint, "pad-added",
      G_CALLBACK (connect_sink), data,
      (GClosureNotify) kms_connect_data_destroy, 0);
}

GST_START_TEST (loopback)
{
  GArray *video_codecs_array;
  gchar *video_codecs[] = { "VP8/90000", NULL };
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstSDPMessage *offer, *answer;
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *agnosticbin =
      gst_element_factory_make ("agnosticbin", "agnosticbin");
  GstElement *rtpendpointsender =
      gst_element_factory_make ("rtpendpoint", "sender");
  GstElement *rtpendpointreceiver =
      gst_element_factory_make ("rtpendpoint", "receiver");
  GstElement *outputfakesink = gst_element_factory_make ("fakesink", NULL);

  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);

  g_object_set_data (G_OBJECT (pipeline), "loop", loop);

  gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);
  g_object_unref (bus);

  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (rtpendpointsender, "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);
  g_object_set (rtpendpointreceiver, "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);
  g_array_unref (video_codecs_array);

  g_object_set (G_OBJECT (outputfakesink), "signal-handoffs", TRUE, "async",
      FALSE, NULL);
  g_signal_connect (G_OBJECT (outputfakesink), "handoff",
      G_CALLBACK (fakesink_hand_off), loop);

  gst_bin_add_many (GST_BIN (pipeline), videotestsrc, agnosticbin,
      rtpendpointsender, rtpendpointreceiver, outputfakesink, NULL);

  gst_element_link (videotestsrc, agnosticbin);
  connect_sink_async (rtpendpointsender, agnosticbin, pipeline, "sink_video");

  g_object_set_data (G_OBJECT (rtpendpointreceiver), VIDEO_SINK,
      outputfakesink);
  g_signal_connect (rtpendpointreceiver, "pad-added",
      G_CALLBACK (connect_sink_on_srcpad_added), NULL);
  fail_unless (kms_element_request_srcpad (rtpendpointreceiver,
          KMS_ELEMENT_PAD_TYPE_VIDEO));

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

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

  gst_element_set_state (rtpendpointsender, GST_STATE_PLAYING);
  gst_element_set_state (rtpendpointreceiver, GST_STATE_PLAYING);

  g_timeout_add_seconds (10, timeout_check, pipeline);

  mark_point ();
  g_main_loop_run (loop);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, __FUNCTION__);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (negotiation_offerer)
{
  GArray *audio_codecs_array, *video_codecs_array;
  gchar *audio_codecs[] = { "OPUS/48000/1", "AMR/8000/1", NULL };
  gchar *video_codecs[] = { "H263-1998/90000", "VP8/90000", NULL };
  GstElement *offerer = gst_element_factory_make ("rtpendpoint", NULL);
  GstElement *answerer = gst_element_factory_make ("rtpendpoint", NULL);
  GstSDPMessage *offer = NULL, *answer = NULL;
  GstSDPMessage *offerer_local_sdp = NULL, *offerer_remote_sdp = NULL;
  gchar *offerer_local_sdp_str, *offerer_remote_sdp_str;
  GstSDPMessage *answerer_local_sdp = NULL, *answerer_remote_sdp = NULL;
  gchar *answerer_local_sdp_str, *answerer_remote_sdp_str;
  gchar *sdp_str = NULL;

  audio_codecs_array = create_codecs_array (audio_codecs);
  video_codecs_array = create_codecs_array (video_codecs);
  g_object_set (offerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);
  g_object_set (answerer, "num-audio-medias", 1, "audio-codecs",
      g_array_ref (audio_codecs_array), "num-video-medias", 1, "video-codecs",
      g_array_ref (video_codecs_array), NULL);
  g_array_unref (audio_codecs_array);
  g_array_unref (video_codecs_array);

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

  g_object_get (offerer, "local-sdp", &offerer_local_sdp, NULL);
  fail_unless (offerer_local_sdp != NULL);
  g_object_get (offerer, "remote-sdp", &offerer_remote_sdp, NULL);
  fail_unless (offerer_remote_sdp != NULL);

  g_object_get (answerer, "local-sdp", &answerer_local_sdp, NULL);
  fail_unless (answerer_local_sdp != NULL);
  g_object_get (answerer, "remote-sdp", &answerer_remote_sdp, NULL);
  fail_unless (answerer_remote_sdp != NULL);

  offerer_local_sdp_str = gst_sdp_message_as_text (offerer_local_sdp);
  offerer_remote_sdp_str = gst_sdp_message_as_text (offerer_remote_sdp);

  answerer_local_sdp_str = gst_sdp_message_as_text (answerer_local_sdp);
  answerer_remote_sdp_str = gst_sdp_message_as_text (answerer_remote_sdp);

  GST_DEBUG ("Offerer local SDP\n%s", offerer_local_sdp_str);
  GST_DEBUG ("Offerer remote SDPr\n%s", offerer_remote_sdp_str);
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

  g_object_unref (offerer);
  g_object_unref (answerer);
}

GST_END_TEST
GST_START_TEST (process_webrtc_offer)
{
  GstSDPMessage *pattern_sdp;
  GstElement *rtpendpoint = gst_element_factory_make ("rtpendpoint", NULL);
  GstSDPMessage *offer = NULL, *answer = NULL;
  gchar *aux = NULL;

  static const gchar *offer_str = "v=0\r\n"
      "o=- 1783800438437245920 2 IN IP4 127.0.0.1\r\n"
      "s=-\r\n"
      "t=0 0\r\n"
      "a=group:BUNDLE audio video\r\n"
      "a=msid-semantic: WMS MediaStream0\r\n"
      "m=audio 37426 RTP/SAVPF 111 103 9 102 0 8 106 105 13 127 126\r\n"
      "c=IN IP4 5.5.5.5\r\n"
      "a=rtcp:37426 IN IP4 5.5.5.5\r\n"
      "a=candidate:1840965416 1 udp 2113937151 192.168.0.100 37426 typ host generation 0\r\n"
      "a=candidate:1840965416 2 udp 2113937151 192.168.0.100 37426 typ host generation 0\r\n"
      "a=candidate:590945240 1 tcp 1509957375 192.168.0.100 46029 typ host generation 0\r\n"
      "a=candidate:590945240 2 tcp 1509957375 192.168.0.100 46029 typ host generation 0\r\n"
      "a=candidate:3975340444 1 udp 1677729535 5.5.5.5 37426 typ srflx raddr 192.168.0.100 rport 37426 generation 0\r\n"
      "a=candidate:3975340444 2 udp 1677729535 5.5.5.5 37426 typ srflx raddr 192.168.0.100 rport 37426 generation 0\r\n"
      "a=ice-ufrag:RkI7xTFiQgGZu1ww\r\n"
      "a=ice-pwd:6ZTKNoP2vXWYLweywju9Bydv\r\n"
      "a=ice-options:google-ice\r\n"
      "a=mid:audio\r\n"
      "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n"
      "a=sendrecv\r\n"
      "a=rtcp-mux\r\n"
      "a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:vpy+PnhF0bWmwYlAngWT1cc9qppYCvRlwT4aKrYh\r\n"
      "a=rtpmap:111 opus/48000/1\r\n"
      "a=fmtp:111 minptime=10\r\n"
      "a=rtpmap:103 ISAC/16000\r\n"
      "a=rtpmap:9 G722/16000\r\n"
      "a=rtpmap:102 ILBC/8000\r\n"
      "a=rtpmap:0 PCMU/8000\r\n"
      "a=rtpmap:8 PCMA/8000\r\n"
      "a=rtpmap:106 CN/32000\r\n"
      "a=rtpmap:105 CN/16000\r\n"
      "a=rtpmap:13 CN/8000\r\n"
      "a=rtpmap:127 red/8000\r\n"
      "a=rtpmap:126 telephone-event/8000\r\n"
      "a=maxptime:60\r\n"
      "a=ssrc:4210654932 cname:/9kskFtadoxn1x70\r\n"
      "a=ssrc:4210654932 msid:MediaStream0 AudioTrack0\r\n"
      "a=ssrc:4210654932 mslabel:MediaStream0\r\n"
      "a=ssrc:4210654932 label:AudioTrack0\r\n"
      "m=video 37426 RTP/SAVPF 100 116 117\r\n"
      "c=IN IP4 5.5.5.5\r\n"
      "a=rtcp:37426 IN IP4 5.5.5.5\r\n"
      "a=candidate:1840965416 1 udp 2113937151 192.168.0.100 37426 typ host generation 0\r\n"
      "a=candidate:1840965416 2 udp 2113937151 192.168.0.100 37426 typ host generation 0\r\n"
      "a=candidate:590945240 1 tcp 1509957375 192.168.0.100 46029 typ host generation 0\r\n"
      "a=candidate:590945240 2 tcp 1509957375 192.168.0.100 46029 typ host generation 0\r\n"
      "a=candidate:3975340444 1 udp 1677729535 5.5.5.5 37426 typ srflx raddr 192.168.0.100 rport 37426 generation 0\r\n"
      "a=candidate:3975340444 2 udp 1677729535 5.5.5.5 37426 typ srflx raddr 192.168.0.100 rport 37426 generation 0\r\n"
      "a=ice-ufrag:RkI7xTFiQgGZu1ww\r\n"
      "a=ice-pwd:6ZTKNoP2vXWYLweywju9Bydv\r\n"
      "a=ice-options:google-ice\r\n"
      "a=mid:video\r\n"
      "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n"
      "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n"
      "a=sendrecv\r\n"
      "a=rtcp-mux\r\n"
      "a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:vpy+PnhF0bWmwYlAngWT1cc9qppYCvRlwT4aKrYh\r\n"
      "a=rtpmap:100 VP8/90000\r\n"
      "a=rtcp-fb:100 ccm fir\r\n"
      "a=rtcp-fb:100 nack\r\n"
      "a=rtcp-fb:100 nack pli\r\n"
      "a=rtcp-fb:100 goog-remb\r\n"
      "a=rtpmap:116 red/90000\r\n"
      "a=rtpmap:117 ulpfec/90000\r\n"
      "a=ssrc:1686396354 cname:/9kskFtadoxn1x70\r\n"
      "a=ssrc:1686396354 msid:MediaStream0 VideoTrack0\r\n"
      "a=ssrc:1686396354 mslabel:MediaStream0\r\n"
      "a=ssrc:1686396354 label:VideoTrack0\r\n";

  fail_unless (gst_sdp_message_new (&pattern_sdp) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          pattern_offer_sdp_str, -1, pattern_sdp) == GST_SDP_OK);

  g_object_set (rtpendpoint, "pattern-sdp", pattern_sdp, NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);
  g_object_get (rtpendpoint, "pattern-sdp", &pattern_sdp, NULL);
  fail_unless (pattern_sdp != NULL);
  fail_unless (gst_sdp_message_free (pattern_sdp) == GST_SDP_OK);

  fail_unless (gst_sdp_message_new (&offer) == GST_SDP_OK);
  fail_unless (gst_sdp_message_parse_buffer ((const guint8 *)
          offer_str, -1, offer) == GST_SDP_OK);

  GST_DEBUG ("Offer:\n%s", (aux = gst_sdp_message_as_text (offer)));
  g_free (aux);
  aux = NULL;

  g_signal_emit_by_name (rtpendpoint, "process-offer", offer, &answer);
  fail_unless (answer != NULL);
  GST_DEBUG ("Answer:\n%s", (aux = gst_sdp_message_as_text (answer)));
  g_free (aux);
  aux = NULL;

  gst_sdp_message_free (offer);
  gst_sdp_message_free (answer);

  g_object_unref (rtpendpoint);
}

GST_END_TEST
/*
 * End of test cases
 */
static Suite *
sdp_suite (void)
{
  Suite *s = suite_create ("rtpendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, negotiation_offerer);
  tcase_add_test (tc_chain, loopback);

  /* TODO: enable when bundle is available */
  if (FALSE) {
    tcase_add_test (tc_chain, process_webrtc_offer);
  }
  return s;
}

GST_CHECK_MAIN (sdp);
