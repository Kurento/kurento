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

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MODULE SdpEndpoint
#include <boost/test/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <MediaElementImpl.hpp>
#include <ElementConnectionData.hpp>
#include <MediaType.hpp>
#include <KurentoException.hpp>
#include <objects/SdpEndpointImpl.hpp>
#include <MediaSet.hpp>
#include <ModuleManager.hpp>

using namespace kurento;

std::string mediaPipelineId;
ModuleManager moduleManager;
boost::property_tree::ptree config;

struct GF {
  GF();
  ~GF();
};

class SdpEndpointFactory : public Factory
{
public:
  std::string getName () const override { return "SdpEndpointFactory"; }

protected:
  MediaObjectImpl *createObjectPointer (const boost::property_tree::ptree &conf,
      const Json::Value &params) const override
  {
    std::string mediaPipelineId = params["mediaPipeline"].asString ();
    bool useIpv6 = false;

    if (params.isMember ("useIpv6") ) {
      Json::Value useIpv6Param = params["useIpv6"];

      if (useIpv6Param.isConvertibleTo (Json::ValueType::booleanValue) ) {
        useIpv6 = useIpv6Param.asBool();
      }
    }

    std::shared_ptr <MediaObjectImpl> pipe =
      MediaSet::getMediaSet ()->getMediaObject (mediaPipelineId);

    return new SdpEndpointImpl (config, pipe, "dummysdp", useIpv6);
  }
};

BOOST_GLOBAL_FIXTURE (GF);

GF::GF()
{
  gst_init(nullptr, nullptr);
  moduleManager.loadModulesFromDirectories ("../../src/server");
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

static void
releaseMediaObject (const std::string &id)
{
  MediaSet::getMediaSet ()->release (id);
}


static std::shared_ptr <SdpEndpointImpl> generateSdpEndpoint (
  const std::string &mediaPipelineId, bool useIpv6 = false)
{
  SdpEndpointFactory factory;
  Json::Value params;

  params["mediaPipeline"] = mediaPipelineId;
  params["useIpv6"] = useIpv6;

  return std::dynamic_pointer_cast<SdpEndpointImpl> (
           factory.createObject (config, "", params) );
}

BOOST_AUTO_TEST_CASE (duplicate_offer)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::string offer;

  config.add ("modules.kurento.SdpEndpoint.configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.audioCodecs", "[]");
  config.add ("modules.kurento.SdpEndpoint.videoCodecs", "[]");

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId);

  offer = sdpEndpoint->generateOffer ();

  try {
    offer = sdpEndpoint->generateOffer ();
    BOOST_ERROR ("Duplicate offer not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != 40208) {
      BOOST_ERROR ("Duplicate offer not detected");
    }
  }

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (generate_ipv6_offer)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.audioCodecs", "[]");
  config.add ("modules.kurento.SdpEndpoint.videoCodecs", "[]");

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId, true);

  std::string offer = sdpEndpoint->generateOffer();

  BOOST_CHECK (offer.find ("IP6") != std::string::npos);
  BOOST_CHECK (offer.find ("IP4") == std::string::npos);
  BOOST_CHECK (offer.find ("::") != std::string::npos);

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (generate_ipv6_answer)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.audioCodecs", "[]");
  config.add ("modules.kurento.SdpEndpoint.videoCodecs", "[]");

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId, true);

  std::string offer = "v=0\r\n"
                      "o=- 0 0 IN IP6 ::1\r\n"
                      "s=TestSession\r\n"
                      "c=IN IP6 ::1\r\n"
                      "t=2873397496 2873404696\r\n"
                      "m=audio 9 RTP/AVP 0\r\n"
                      "a=rtpmap:0 PCMU/8000\r\n"
                      "a=sendonly\r\n"
                      "m=video 9 RTP/AVP 96\r\n"
                      "a=rtpmap:96 VP8/90000\r\n"
                      "a=sendonly\r\n";

  std::string answer = sdpEndpoint->processOffer (offer);

  BOOST_CHECK (answer.find ("IP6") != std::string::npos);
  BOOST_CHECK (answer.find ("IP4") == std::string::npos);
  BOOST_CHECK (answer.find ("::") != std::string::npos);

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (process_answer_without_offer)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::string answer =  "v=0\r\n"
                        "o=- 0 0 IN IP4 0.0.0.0\r\n"
                        "s=TestSession\r\n"
                        "c=IN IP4 0.0.0.0\r\n"
                        "t=2873397496 2873404696\r\n"
                        "m=audio 9 RTP/AVP 0\r\n"
                        "a=rtpmap:0 PCMU/8000\r\n"
                        "a=sendonly\r\n"
                        "m=video 9 RTP/AVP 96\r\n"
                        "a=rtpmap:96 VP8/90000\r\n"
                        "a=sendonly\r\n";

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.audioCodecs", "[]");
  config.add ("modules.kurento.SdpEndpoint.videoCodecs", "[]");

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId);

  try {
    sdpEndpoint->processAnswer (answer);
    BOOST_ERROR ("Process answer without generating offer does not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != 40209) {
      BOOST_ERROR ("Process answer without generating offer does not detected");
    }
  }

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (duplicate_answer)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::string offer;

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.audioCodecs", "[]");
  config.add ("modules.kurento.SdpEndpoint.videoCodecs", "[]");

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId);

  offer = sdpEndpoint->generateOffer ();
  sdpEndpoint->processAnswer (offer);

  try {
    sdpEndpoint->processAnswer (offer);
    BOOST_ERROR ("Duplicate answer not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != 40210) {
      BOOST_ERROR ("Duplicate answer not detected");
    }
  }

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (codec_parsing)
{
  boost::property_tree::ptree ac, audioCodecs, vc, videoCodecs;

  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::string offer;

  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 1);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 1);
  config.add ("configPath", "../../../tests" );

  ac.put ("name", "opus/48000/2");
  audioCodecs.push_back (std::make_pair ("", ac) );
  ac.put ("name", "PCMU/8000");
  audioCodecs.push_back (std::make_pair ("", ac) );
  config.add_child ("modules.kurento.SdpEndpoint.audioCodecs", audioCodecs);

  vc.put ("name", "VP8/90000");
  videoCodecs.push_back (std::make_pair ("", vc) );
  vc.put ("name", "H264/90000");
  videoCodecs.push_back (std::make_pair ("", vc) );
  config.add_child ("modules.kurento.SdpEndpoint.videoCodecs", videoCodecs);

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId);

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (invalid_sdp)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 0);
  config.add ("modules.kurento.SdpEndpoint.audioCodecs", "[]");
  config.add ("modules.kurento.SdpEndpoint.videoCodecs", "[]");

  std::shared_ptr <SdpEndpointImpl> sdpEndpoint = generateSdpEndpoint (
        mediaPipelineId);

  try {
    sdpEndpoint->processOffer ("");
    BOOST_ERROR ("Empty offer not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != SDP_PARSE_ERROR) {
      BOOST_ERROR ("Empty offer not detected");
    }
  }

  try {
    sdpEndpoint->processOffer ("bad offer");
    BOOST_ERROR ("Bad offer not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != SDP_PARSE_ERROR) {
      BOOST_ERROR ("Bad offer not detected");
    }
  }

  sdpEndpoint->generateOffer ();

  try {
    sdpEndpoint->processAnswer ("");
    BOOST_ERROR ("Empty answer not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != SDP_PARSE_ERROR) {
      BOOST_ERROR ("Empty answer not detected");
    }
  }

  try {
    sdpEndpoint->processAnswer ("bad answer");
    BOOST_ERROR ("Bad answer not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != SDP_PARSE_ERROR) {
      BOOST_ERROR ("Bad answer not detected");
    }
  }

  releaseMediaObject (sdpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  sdpEndpoint.reset ();
}
