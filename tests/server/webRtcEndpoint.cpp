/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MODULE WebRtcEndpoint
#include <boost/test/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/WebRtcEndpointImpl.hpp>
#include <IceCandidate.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <MediaSet.hpp>
#include <MediaElementImpl.hpp>

using namespace kurento;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE (GF)

GF::GF()
{
  boost::property_tree::ptree ac, audioCodecs, vc, videoCodecs;
  gst_init (NULL, NULL);

  moduleManager.loadModulesFromDirectories ("../../src/server");

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 1);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 1);

  ac.put ("name", "opus/48000/2");
  audioCodecs.push_back (std::make_pair ("", ac) );
  config.add_child ("modules.kurento.SdpEndpoint.audioCodecs", audioCodecs);

  vc.put ("name", "VP8/90000");
  videoCodecs.push_back (std::make_pair ("", vc) );
  config.add_child ("modules.kurento.SdpEndpoint.videoCodecs", videoCodecs);

  mediaPipelineId = moduleManager.getFactory ("MediaPipeline")->createObject (
                      config, "",
                      Json::Value() )->getId();
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

static std::shared_ptr <WebRtcEndpointImpl>
createWebrtc (void)
{
  std::shared_ptr <kurento::MediaObjectImpl> webrtcEndpoint;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;

  webrtcEndpoint = moduleManager.getFactory ("WebRtcEndpoint")->createObject (
                     config, "",
                     constructorParams );

  return std::dynamic_pointer_cast <WebRtcEndpointImpl> (webrtcEndpoint);
}

static void
releaseWebRtc (std::shared_ptr<WebRtcEndpointImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}

static std::shared_ptr <MediaElementImpl>
createTestSrc (void)
{
  std::shared_ptr <MediaElementImpl> src = std::dynamic_pointer_cast
      <MediaElementImpl> (MediaSet::getMediaSet()->ref (new  MediaElementImpl (
                            boost::property_tree::ptree(),
                            MediaSet::getMediaSet()->getMediaObject (mediaPipelineId),
                            "dummysrc") ) );

  g_object_set (src->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);

  return std::dynamic_pointer_cast <MediaElementImpl> (src);
}

static void
releaseTestSrc (std::shared_ptr<MediaElementImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}


BOOST_AUTO_TEST_CASE (gathering_done)
{
  std::atomic<bool> gathering_done (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);
  std::shared_ptr <WebRtcEndpointImpl> webRtcEp = createWebrtc();

  webRtcEp->signalOnIceGatheringDone.connect ([&] (OnIceGatheringDone event) {
    gathering_done = true;
    cv.notify_one();
  });

  webRtcEp->generateOffer ();
  webRtcEp->gatherCandidates ();

  cv.wait_for (lck, std::chrono::seconds (5), [&] () {
    return gathering_done.load();
  });

  if (!gathering_done) {
    BOOST_ERROR ("Gathering not done");
  }

  releaseWebRtc (webRtcEp);
}

BOOST_AUTO_TEST_CASE (ice_state_changes)
{
  std::atomic<bool> ice_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer = createWebrtc();
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer = createWebrtc();

  webRtcEpOfferer->setName ("offerer");
  webRtcEpAnswerer->setName ("answerer");

  webRtcEpOfferer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    webRtcEpAnswerer->addIceCandidate (event.getCandidate() );
  });

  webRtcEpAnswerer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    webRtcEpOfferer->addIceCandidate (event.getCandidate() );
  });

  webRtcEpOfferer->signalOnIceComponentStateChanged.connect ([&] (
  OnIceComponentStateChanged event) {
    ice_state_changed = true;
    cv.notify_one();
  });

  std::string offer = webRtcEpOfferer->generateOffer ();
  std::string answer = webRtcEpAnswerer->processOffer (offer);
  webRtcEpOfferer->processAnswer (answer);

  webRtcEpOfferer->gatherCandidates ();
  webRtcEpAnswerer->gatherCandidates ();

  cv.wait_for (lck, std::chrono::seconds (5), [&] () {
    return ice_state_changed.load();
  });

  if (!ice_state_changed) {
    BOOST_ERROR ("ICE state not chagned");
  }

  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
}

BOOST_AUTO_TEST_CASE (stun_turn_properties)
{
  std::string stunServerAddress ("10.0.0.1");
  int stunServerPort = 2345;
  std::string turnUrl ("user0:pass0@10.0.0.2:3456");

  std::shared_ptr <WebRtcEndpointImpl> webRtcEp  = createWebrtc();

  webRtcEp->setStunServerAddress (stunServerAddress);
  std::string stunServerAddressRet = webRtcEp->getStunServerAddress ();
  BOOST_CHECK (stunServerAddressRet == stunServerAddress);

  webRtcEp->setStunServerPort (stunServerPort);
  int stunServerPortRet = webRtcEp->getStunServerPort ();
  BOOST_CHECK (stunServerPortRet == stunServerPort);

  webRtcEp->setTurnUrl (turnUrl);
  std::string turnUrlRet = webRtcEp->getTurnUrl ();
  BOOST_CHECK (turnUrlRet == turnUrl);

  releaseWebRtc (webRtcEp);
}

BOOST_AUTO_TEST_CASE (media_state_changes)
{
  std::atomic<bool> media_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer = createWebrtc();
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer = createWebrtc();
  std::shared_ptr <MediaElementImpl> src = createTestSrc();

  src->connect (webRtcEpOfferer);

  webRtcEpOfferer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    BOOST_TEST_MESSAGE ("Offerer: adding candidate " +
                        event.getCandidate()->getCandidate() );
    webRtcEpAnswerer->addIceCandidate (event.getCandidate() );
  });

  webRtcEpAnswerer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    BOOST_TEST_MESSAGE ("Answerer: adding candidate " +
                        event.getCandidate()->getCandidate() );
    webRtcEpOfferer->addIceCandidate (event.getCandidate() );
  });

  webRtcEpOfferer->signalOnIceGatheringDone.connect ([&] (
  OnIceGatheringDone event) {
    BOOST_TEST_MESSAGE ("Offerer: Gathering done");
  });

  webRtcEpAnswerer->signalOnIceGatheringDone.connect ([&] (
  OnIceGatheringDone event) {
    BOOST_TEST_MESSAGE ("Answerer: Gathering done");
  });

  webRtcEpAnswerer->signalMediaStateChanged.connect ([&] (
  MediaStateChanged event) {
    media_state_changed = true;
    cv.notify_one();
  });

  std::string offer = webRtcEpOfferer->generateOffer ();
  BOOST_TEST_MESSAGE ("offer: " + offer);

  std::string answer = webRtcEpAnswerer->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  webRtcEpOfferer->processAnswer (answer);

  webRtcEpOfferer->gatherCandidates ();
  webRtcEpAnswerer->gatherCandidates ();

  cv.wait_for (lck, std::chrono::seconds (5), [&] () {

    return media_state_changed.load();
  });


  if (!media_state_changed) {
    BOOST_ERROR ("Not media state chagned");
  }

  releaseTestSrc (src);
  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
}
