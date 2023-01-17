/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
#include <gst/gst.h>
#include "SdpEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <boost/filesystem.hpp>
#include <fstream>
#include <CodecConfiguration.hpp>
#include <gst/sdp/gstsdpmessage.h>

#define GST_CAT_DEFAULT kurento_sdp_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoSdpEndpointImpl"

#define PARAM_NUM_AUDIO_MEDIAS "numAudioMedias"
#define PARAM_NUM_VIDEO_MEDIAS "numVideoMedias"
#define PARAM_CODEC_NAME "name"
#define PARAM_AUDIO_CODECS "audioCodecs"
#define PARAM_VIDEO_CODECS "videoCodecs"

namespace kurento
{

static GstSDPMessage *
str_to_sdp (const std::string &sdpStr)
{
  GstSDPResult result;
  GstSDPMessage *sdp = nullptr;

  result = gst_sdp_message_new (&sdp);

  if (result != GST_SDP_OK) {
    throw KurentoException (SDP_CREATE_ERROR, "Error creating SDP message");
  }

  result = gst_sdp_message_parse_buffer ( (const guint8 *) sdpStr.c_str (), -1,
                                          sdp);

  if (result != GST_SDP_OK) {

    gst_sdp_message_free (sdp);
    throw KurentoException (SDP_PARSE_ERROR, "Error parsing SDP");
  }

  if (gst_sdp_message_get_version(sdp) == nullptr) {
    gst_sdp_message_free (sdp);
    throw KurentoException (SDP_PARSE_ERROR, "Invalid SDP");
  }

  return sdp;
}

static void
sdp_to_str (std::string &_return, const GstSDPMessage *sdp)
{
  gchar *sdpGchar;

  sdpGchar = gst_sdp_message_as_text (sdp);
  _return.clear ();
  _return.append (sdpGchar);
  g_free (sdpGchar);
}

static void
append_codec_to_array (GArray *array, const char *codec)
{
  GValue v = G_VALUE_INIT;
  GstStructure *s;

  g_value_init (&v, GST_TYPE_STRUCTURE);

  s = gst_structure_new_empty (codec);
  if (s == NULL) {
    std::string message =
        std::string () + "Invalid codec name in config: '" + codec + "'";
    GST_ERROR ("%s", message.c_str ());
    throw KurentoException (SDP_PARSE_ERROR, message);
  }

  gst_value_set_structure (&v, s);
  gst_structure_free (s);
  g_array_append_val (array, v);
}

void SdpEndpointImpl::postConstructor ()
{
  gchar *sess_id;
  SessionEndpointImpl::postConstructor ();

  g_signal_emit_by_name (element, "create-session", &sess_id);

  if (sess_id == nullptr) {
    throw KurentoException (SDP_END_POINT_CANNOT_CREATE_SESSON,
                            "Cannot create session");
  }

  sessId = std::string (sess_id);
  g_free (sess_id);
}

SdpEndpointImpl::SdpEndpointImpl (const boost::property_tree::ptree &config,
                                  std::shared_ptr< MediaObjectImpl > parent,
                                  const std::string &factoryName, bool useIpv6) :
  SessionEndpointImpl (config, parent, factoryName)
{
  GArray *audio_codecs, *video_codecs;

  audio_codecs = g_array_new (FALSE, TRUE, sizeof (GValue) );
  video_codecs = g_array_new (FALSE, TRUE, sizeof (GValue) );

  //   TODO: Add support for this events
  //   g_signal_connect (element, "media-start", G_CALLBACK (media_start_cb), this);
  //   g_signal_connect (element, "media-stop", G_CALLBACK (media_stop_cb), this);

  guint audio_medias = 0;
  getConfigValue <guint, SdpEndpoint> (&audio_medias, PARAM_NUM_AUDIO_MEDIAS, 1);

  guint video_medias = 0;
  getConfigValue <guint, SdpEndpoint> (&video_medias, PARAM_NUM_VIDEO_MEDIAS, 1);

  std::vector<std::shared_ptr<CodecConfiguration>> acodec_list;
  getConfigValue <std::vector<std::shared_ptr<CodecConfiguration>>, SdpEndpoint>
      (&acodec_list, PARAM_AUDIO_CODECS);

  for (std::shared_ptr<CodecConfiguration> conf : acodec_list) {
    if (!conf->getName().empty()) {
      append_codec_to_array (audio_codecs, conf->getName().c_str() );
    }
  }

  std::vector<std::shared_ptr<CodecConfiguration>> vcodec_list;
  getConfigValue <std::vector<std::shared_ptr<CodecConfiguration>>, SdpEndpoint>
      (&vcodec_list, PARAM_VIDEO_CODECS);

  for (std::shared_ptr<CodecConfiguration> conf : vcodec_list) {
    if (!conf->getName().empty()) {
      append_codec_to_array (video_codecs, conf->getName().c_str() );
    }
  }

  g_object_set (element, "num-audio-medias", audio_medias, "audio-codecs",
                audio_codecs, NULL);
  g_object_set (element, "num-video-medias", video_medias, "video-codecs",
                video_codecs, NULL);
  g_object_set (element, "use-ipv6", useIpv6, NULL);

  offerInProcess = false;
  waitingAnswer = false;
  answerProcessed = false;
}

int SdpEndpointImpl::getMaxVideoRecvBandwidth ()
{
  int maxVideoRecvBandwidth;

  g_object_get (element, "max-video-recv-bandwidth", &maxVideoRecvBandwidth,
                NULL);

  return maxVideoRecvBandwidth;
}

void SdpEndpointImpl::setMaxVideoRecvBandwidth (int maxVideoRecvBandwidth)
{
  g_object_set (element, "max-video-recv-bandwidth", maxVideoRecvBandwidth, NULL);
}

int SdpEndpointImpl::getMaxAudioRecvBandwidth ()
{
  int maxAudioRecvBandwidth;

  g_object_get (element, "max-audio-recv-bandwidth", &maxAudioRecvBandwidth,
                NULL);

  return maxAudioRecvBandwidth;
}

void SdpEndpointImpl::setMaxAudioRecvBandwidth (int maxAudioRecvBandwidth)
{
  g_object_set (element, "max-audio-recv-bandwidth", maxAudioRecvBandwidth, NULL);
}

std::string SdpEndpointImpl::generateOffer ()
{
  std::shared_ptr<OfferOptions> options = std::make_shared <OfferOptions> ();

  options->setOfferToReceiveAudio(true);
  options->setOfferToReceiveVideo(true);

  return generateOffer(options);
}

std::string SdpEndpointImpl::generateOffer (std::shared_ptr<OfferOptions> options)
{
  GstSDPMessage *offer = nullptr;
  std::string offerStr;
  bool expected = false;

  if (!offerInProcess.compare_exchange_strong (expected, true) ) {
    //the endpoint is already negotiated
    throw KurentoException (SDP_END_POINT_ALREADY_NEGOTIATED,
                            "Endpoint already negotiated");
  }

  if (options->isSetOfferToReceiveAudio ()
      && !options->getOfferToReceiveAudio ()) {
    g_object_set (element, "num-audio-medias", 0, NULL);
  }

  if (options->isSetOfferToReceiveVideo ()
      && !options->getOfferToReceiveVideo ()) {
    g_object_set (element, "num-video-medias", 0, NULL);
  }

  g_signal_emit_by_name (element, "generate-offer", sessId.c_str (), &offer);

  if (offer == nullptr) {
    offerInProcess = false;
    throw KurentoException (SDP_END_POINT_GENERATE_OFFER_ERROR,
                            "Error generating offer");
  }

  sdp_to_str (offerStr, offer);
  gst_sdp_message_free (offer);
  waitingAnswer = true;

  return offerStr;
}

std::string SdpEndpointImpl::processOffer (const std::string &offer)
{
  GstSDPMessage *offerSdp = nullptr, *result = nullptr;
  std::string offerSdpStr;
  bool expected = false;

  if (offer.empty () ) {
    throw KurentoException (SDP_PARSE_ERROR, "Empty offer not valid");
  }

  offerSdp = str_to_sdp (offer);

  if (!offerInProcess.compare_exchange_strong (expected, true) ) {
    //the endpoint is already negotiated
    throw KurentoException (SDP_END_POINT_ALREADY_NEGOTIATED,
                            "Endpoint already negotiated");
  }

  g_signal_emit_by_name (element, "process-offer", sessId.c_str (), offerSdp,
                         &result);
  gst_sdp_message_free (offerSdp);

  if (result == nullptr) {
    offerInProcess = false;
    throw KurentoException (SDP_END_POINT_PROCESS_OFFER_ERROR,
                            "Error processing offer");
  }

  sdp_to_str (offerSdpStr, result);
  gst_sdp_message_free (result);

  try {
    MediaSessionStarted event (shared_from_this (),
        MediaSessionStarted::getName ());
    sigcSignalEmit(signalMediaSessionStarted, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", MediaSessionStarted::getName ().c_str (),
        e.what ());
  }

  return offerSdpStr;
}

std::string SdpEndpointImpl::processAnswer (const std::string &answer)
{
  GstSDPMessage *answerSdp;
  bool expected = true;
  bool expected_false = false;
  gboolean result;

  if (answer.empty () ) {
    throw KurentoException (SDP_PARSE_ERROR, "Empty answer not valid");
  }

  if (!waitingAnswer.compare_exchange_strong (expected, true) ) {
    //offer not generated
    throw KurentoException (SDP_END_POINT_NOT_OFFER_GENERATED,
                            "Offer not generated. It is not possible to process an answer.");
  }

  answerSdp = str_to_sdp (answer);

  if (!answerProcessed.compare_exchange_strong (expected_false, true) ) {
    //the endpoint is already negotiated
    gst_sdp_message_free (answerSdp);
    throw KurentoException (SDP_END_POINT_ANSWER_ALREADY_PROCCESED,
                            "Sdp Answer already processed");
  }

  g_signal_emit_by_name (element, "process-answer", sessId.c_str (), answerSdp,
                         &result);
  gst_sdp_message_free (answerSdp);

  if (!result) {
    throw KurentoException (SDP_END_POINT_PROCESS_ANSWER_ERROR,
                            "Error processing answer");
  }

  try {
    MediaSessionStarted event (shared_from_this (),
        MediaSessionStarted::getName());
    sigcSignalEmit(signalMediaSessionStarted, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", MediaSessionStarted::getName ().c_str (),
        e.what ());
  }

  return getLocalSessionDescriptor ();
}

std::string SdpEndpointImpl::getLocalSessionDescriptor ()
{
  GstSDPMessage *localSdp = nullptr;
  std::string localSdpStr;

  g_signal_emit_by_name (element, "get-local-sdp", sessId.c_str (), &localSdp);

  if (localSdp == nullptr) {
    throw KurentoException (SDP_END_POINT_NO_LOCAL_SDP_ERROR, "No local SDP");
  }

  sdp_to_str (localSdpStr, localSdp);
  gst_sdp_message_free (localSdp);

  return localSdpStr;
}

std::string SdpEndpointImpl::getRemoteSessionDescriptor ()
{
  GstSDPMessage *remoteSdp = nullptr;
  std::string remoteSdpStr;

  g_signal_emit_by_name (element, "get-remote-sdp", sessId.c_str (), &remoteSdp);

  if (remoteSdp == nullptr) {
    throw KurentoException (SDP_END_POINT_NO_REMOTE_SDP_ERROR, "No remote SDP");
  }

  sdp_to_str (remoteSdpStr, remoteSdp);;
  gst_sdp_message_free (remoteSdp);

  return remoteSdpStr;
}

SdpEndpointImpl::StaticConstructor SdpEndpointImpl::staticConstructor;

SdpEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
