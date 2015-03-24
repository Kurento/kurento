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

using namespace kurento;

BOOST_AUTO_TEST_CASE (gathering_done)
{
  gst_init (NULL, NULL);

  std::atomic<bool> gathering_done (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  std::shared_ptr <MediaPipelineImpl> pipe (new MediaPipelineImpl (
        boost::property_tree::ptree() ) );
  boost::property_tree::ptree config;

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.sdpPattern", "sdp_pattern.txt");
  config.add ("modules.kurento.SdpEndpoint.configPath", "../../../tests");

  std::shared_ptr <WebRtcEndpointImpl> webRtcEp ( new  WebRtcEndpointImpl
      (config, pipe) );

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

  webRtcEp.reset ();
}

BOOST_AUTO_TEST_CASE (ice_state_changes)
{
  gst_init (NULL, NULL);

  std::atomic<bool> ice_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  std::shared_ptr <MediaPipelineImpl> pipe (new MediaPipelineImpl (
        boost::property_tree::ptree() ) );
  boost::property_tree::ptree config;

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.sdpPattern", "sdp_pattern.txt");
  config.add ("modules.kurento.SdpEndpoint.configPath", "../../../tests");

  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer ( new  WebRtcEndpointImpl
      (config, pipe) );
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer ( new  WebRtcEndpointImpl
      (config, pipe) );

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

  webRtcEpOfferer.reset ();
}

BOOST_AUTO_TEST_CASE (stun_properties)
{
  gst_init (NULL, NULL);

  std::string stunServerAddress ("10.0.0.1");
  int stunServerPort = 2345;

  std::shared_ptr <MediaPipelineImpl> pipe (new MediaPipelineImpl (
        boost::property_tree::ptree() ) );
  boost::property_tree::ptree config;

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.sdpPattern", "sdp_pattern.txt");
  config.add ("modules.kurento.SdpEndpoint.configPath", "../../../tests");

  std::shared_ptr <WebRtcEndpointImpl> webRtcEp ( new  WebRtcEndpointImpl
      (config, pipe) );

  webRtcEp->setStunServerAddress (stunServerAddress);
  std::string stunServerAddressRet = webRtcEp->getStunServerAddress ();
  BOOST_CHECK (stunServerAddressRet == stunServerAddress);

  webRtcEp->setStunServerPort (stunServerPort);
  int stunServerPortRet = webRtcEp->getStunServerPort ();
  BOOST_CHECK (stunServerPortRet == stunServerPort);
}
