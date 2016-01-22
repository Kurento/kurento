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

#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/WebRtcEndpointImpl.hpp>
#include <IceCandidate.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <MediaSet.hpp>
#include <MediaElementImpl.hpp>
#include <ConnectionState.hpp>

using namespace kurento;
using namespace boost::unit_test;

static const std::string CAND_PREFIX = "candidate:";

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

static void
exchange_candidate (OnIceCandidate event,
                    std::shared_ptr <WebRtcEndpointImpl> peer, bool useIpv6)
{
  bool isIpv6 = event.getCandidate()->getCandidate().substr (
                  CAND_PREFIX.length() ).find (":") != std::string::npos;

  if (isIpv6 != useIpv6) {
    return;
  }

  BOOST_TEST_MESSAGE ("Offerer: adding candidate " +
                      event.getCandidate()->getCandidate() );
  peer->addIceCandidate (event.getCandidate() );
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


static void
gathering_done ()
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

  cv.wait (lck, [&] () {
    return gathering_done.load();
  });

  if (!gathering_done) {
    BOOST_ERROR ("Gathering not done");
  }

  releaseWebRtc (webRtcEp);
}

static  void
ice_state_changes (bool useIpv6)
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
    exchange_candidate (event, webRtcEpAnswerer, useIpv6);
  });

  webRtcEpAnswerer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    exchange_candidate (event, webRtcEpOfferer, useIpv6);
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

  cv.wait (lck, [&] () {
    return ice_state_changed.load();
  });

  if (!ice_state_changed) {
    BOOST_ERROR ("ICE state not chagned");
  }

  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
}

static void
ice_state_changes_ipv4 ()
{
  ice_state_changes (false);
}

static void
ice_state_changes_ipv6 ()
{
  ice_state_changes (true);
}

static  void
stun_turn_properties ()
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

static void
media_state_changes (bool useIpv6)
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
    exchange_candidate (event, webRtcEpAnswerer, useIpv6);
  });

  webRtcEpAnswerer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    exchange_candidate (event, webRtcEpOfferer, useIpv6);
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

  cv.wait (lck, [&] () {
    return media_state_changed.load();
  });

  if (!media_state_changed) {
    BOOST_ERROR ("Not media state chagned");
  }

  releaseTestSrc (src);
  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
}

static void
media_state_changes_ipv4 ()
{
  media_state_changes (false);
}

static void
media_state_changes_ipv6 ()
{
  media_state_changes (true);
}

static void
connectWebrtcEndpoints (std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer,
                        std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer,
                        bool useIpv6)
{
  std::atomic<bool> conn_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  webRtcEpOfferer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    exchange_candidate (event, webRtcEpAnswerer, useIpv6);
  });

  webRtcEpAnswerer->signalOnIceCandidate.connect ([&] (OnIceCandidate event) {
    exchange_candidate (event, webRtcEpOfferer, useIpv6);
  });

  webRtcEpOfferer->signalOnIceGatheringDone.connect ([&] (
  OnIceGatheringDone event) {
    BOOST_TEST_MESSAGE ("Offerer: Gathering done");
  });

  webRtcEpAnswerer->signalOnIceGatheringDone.connect ([&] (
  OnIceGatheringDone event) {
    BOOST_TEST_MESSAGE ("Answerer: Gathering done");
  });

  webRtcEpAnswerer->signalConnectionStateChanged.connect ([&] (
  ConnectionStateChanged event) {
    conn_state_changed = true;
    cv.notify_one();
  });

  std::string offer = webRtcEpOfferer->generateOffer ();
  BOOST_TEST_MESSAGE ("offer: " + offer);

  std::string answer = webRtcEpAnswerer->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  webRtcEpOfferer->processAnswer (answer);

  if (webRtcEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::DISCONNECTED) {
    BOOST_ERROR ("Connection must be disconnected");
  }

  webRtcEpOfferer->gatherCandidates ();
  webRtcEpAnswerer->gatherCandidates ();

  cv.wait (lck, [&] () {
    return conn_state_changed.load();
  });

  if (!conn_state_changed) {
    BOOST_ERROR ("Not conn state chagned");
  }

  if (webRtcEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::CONNECTED) {
    BOOST_ERROR ("Connection must be connected");
  }
}

static void
connection_state_changes (bool useIpv6)
{
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer = createWebrtc();
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer = createWebrtc();

  connectWebrtcEndpoints (webRtcEpOfferer, webRtcEpAnswerer, useIpv6);

  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
}

static void
connection_state_changes_ipv4 ()
{
  connection_state_changes (false);
}

static void
connection_state_changes_ipv6 ()
{
  connection_state_changes (true);
}

static void
check_webrtc_stats (bool useIpv6)
{
  std::map <std::string, std::shared_ptr<Stats>> stats;
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer = createWebrtc();
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer = createWebrtc();
  std::shared_ptr<MediaPipelineImpl> pipeline;

  connectWebrtcEndpoints (webRtcEpOfferer, webRtcEpAnswerer, useIpv6);

  /* Now webrtcEndPoints are connected it's time to get stats */
  stats = webRtcEpAnswerer->getStats();

  if (stats.find (webRtcEpAnswerer->getId() ) != stats.end() ) {
    BOOST_ERROR ("No latency stats enabled");
  }

  /* Enable latency stats */
  pipeline = std::dynamic_pointer_cast<MediaPipelineImpl>
             (webRtcEpAnswerer->getMediaPipeline () );
  pipeline->setLatencyStats (true);

  stats = webRtcEpAnswerer->getStats();

  if (stats.find (webRtcEpAnswerer->getId() ) == stats.end() ) {
    BOOST_ERROR ("Stats for this element should be in stats report");
  }

  /* Disable latency stats */
  pipeline = std::dynamic_pointer_cast<MediaPipelineImpl>
             (webRtcEpAnswerer->getMediaPipeline () );
  pipeline->setLatencyStats (false);

  stats = webRtcEpAnswerer->getStats();

  if (stats.find (webRtcEpAnswerer->getId() ) != stats.end() ) {
    BOOST_ERROR ("No latency stats enabled");
  }

  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
}

static void
check_webrtc_stats_ipv4 ()
{
  check_webrtc_stats (false);
}

static void
check_webrtc_stats_ipv6 ()
{
  check_webrtc_stats (true);
}

static void
check_exchange_candidates_on_sdp ()
{
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer = createWebrtc();
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer = createWebrtc();
  std::atomic<bool> conn_state_changed (false);
  std::condition_variable cv;
  std::atomic<bool> offer_gathered (false);
  std::condition_variable cv_offer_gathered;
  std::atomic<bool> answer_gathered (false);
  std::condition_variable cv_answer_gathered;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  webRtcEpOfferer->signalOnIceGatheringDone.connect ([&] (
  OnIceGatheringDone event) {
    BOOST_TEST_MESSAGE ("Offerer: Gathering done");
    offer_gathered = true;
    cv_offer_gathered.notify_one();
  });

  webRtcEpAnswerer->signalOnIceGatheringDone.connect ([&] (
  OnIceGatheringDone event) {
    BOOST_TEST_MESSAGE ("Answerer: Gathering done");
    answer_gathered = true;
    cv_answer_gathered.notify_one();
  });

  webRtcEpAnswerer->signalConnectionStateChanged.connect ([&] (
  ConnectionStateChanged event) {
    conn_state_changed = true;
    cv.notify_one();
  });

  std::string offer = webRtcEpOfferer->generateOffer ();
  BOOST_TEST_MESSAGE ("offer: " + offer);

  webRtcEpOfferer->gatherCandidates ();

  cv_offer_gathered.wait (lck, [&] () {
    return offer_gathered.load();
  });

  offer = webRtcEpOfferer->getLocalSessionDescriptor();

  std::string answer = webRtcEpAnswerer->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  webRtcEpAnswerer->gatherCandidates ();

  cv_answer_gathered.wait (lck, [&] () {
    return answer_gathered.load();
  });

  answer = webRtcEpAnswerer->getLocalSessionDescriptor();

  if (webRtcEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::DISCONNECTED) {
    BOOST_ERROR ("Connection must be disconnected");
  }

  webRtcEpOfferer->processAnswer (answer);

  cv.wait (lck, [&] () {
    return conn_state_changed.load();
  });

  if (!conn_state_changed) {
    BOOST_ERROR ("Not conn state chagned");
  }

  if (webRtcEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::CONNECTED) {
    BOOST_ERROR ("Connection must be connected");
  }
}

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "WebRtcEndpoint" );

  test->add (BOOST_TEST_CASE ( &gathering_done ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &ice_state_changes_ipv4 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &ice_state_changes_ipv6 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &stun_turn_properties ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &media_state_changes_ipv4 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &media_state_changes_ipv6 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &connection_state_changes_ipv4 ),
             0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &connection_state_changes_ipv6 ),
             0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &check_webrtc_stats_ipv4 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &check_webrtc_stats_ipv6 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &check_exchange_candidates_on_sdp ),
             0, /* timeout */ 15);

  return test;
}
