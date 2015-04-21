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

using namespace kurento;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;
std::once_flag init_flag;

static void
init_internal()
{
  gst_init (NULL, NULL);

  moduleManager.loadModulesFromDirectories ("../../src/server");

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.sdpPattern", "sdp_pattern.txt");
  config.add ("modules.kurento.SdpEndpoint.configPath", "../../../tests");

  mediaPipelineId = moduleManager.getFactory ("MediaPipeline")->createObject (
                      config, "",
                      Json::Value() )->getId();
}

static void
init()
{
  std::call_once (init_flag, init_internal);
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

BOOST_AUTO_TEST_CASE (gathering_done)
{
  init ();

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
  init ();

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

  init ();

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
