/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#define NUMBER_OF_RECONNECTIONS 5

using namespace kurento;
using namespace boost::unit_test;

static const int TIMEOUT = 5; /* seconds */

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
createWebrtc (bool use_data_channels = false)
{
  std::shared_ptr <kurento::MediaObjectImpl> webrtcEndpoint;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["useDataChannels"] = use_data_channels;

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

  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] () {
  return gathering_done.load();
  }) ) {
    BOOST_ERROR ("Timeout on gathering done");
  }

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

  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] () {
  return ice_state_changed.load();
  }) ) {
    BOOST_ERROR ("Timeout waiting for ICE state change");
  }

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

  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] () {
  return media_state_changed.load();
  }) ) {
    BOOST_ERROR ("Timeout waiting for media state change");
  }

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

  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] () {
  return conn_state_changed.load();
  }) ) {
    BOOST_ERROR ("Error waiting for connection state change");
  }

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

  if (!cv_offer_gathered.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] () {
  return offer_gathered.load();
  }) ) {
    BOOST_ERROR ("Offerer ep does not finished gathering candidates");
  }

  offer = webRtcEpOfferer->getLocalSessionDescriptor();

  std::string answer = webRtcEpAnswerer->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  webRtcEpAnswerer->gatherCandidates ();

  if (!cv_answer_gathered.wait_for (lck, std::chrono::seconds (TIMEOUT),  [&] () {
  return answer_gathered.load();
  }) ) {
    BOOST_ERROR ("Anwerer ep does not finished gathering candidates");
  }

  answer = webRtcEpAnswerer->getLocalSessionDescriptor();

  if (webRtcEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::DISCONNECTED) {
    BOOST_ERROR ("Connection must be disconnected");
  }

  webRtcEpOfferer->processAnswer (answer);

  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] () {
  return conn_state_changed.load();
  }) ) {
    BOOST_ERROR ("Timeout waiting for state change");
  }

  if (!conn_state_changed) {
    BOOST_ERROR ("Not conn state chagned");
  }

  if (webRtcEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::CONNECTED) {
    BOOST_ERROR ("Connection must be connected");
  }
}

static void
check_codec_sdp ()
{
  std::shared_ptr <WebRtcEndpointImpl> webRtcEp = createWebrtc();

  std::string offer ("v=0\r\n"
                     "o=- 5403198809162161286 2 IN IP4 127.0.0.1\r\n"
                     "s=-\r\n"
                     "t=0 0\r\n"
                     "a=group:BUNDLE audio\r\n"
                     "a=msid-semantic: WMS\r\n"
                     "m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 126\r\n"
                     "c=IN IP4 0.0.0.0\r\n"
                     "a=rtcp:9 IN IP4 0.0.0.0\r\n"
                     "a=ice-ufrag:z5Ynp3MoUpUWP2II\r\n"
                     "a=ice-pwd:1dhv/Ia7Vk4yGt/sugyhSz7Q\r\n"
                     "a=fingerprint:sha-256 AF:FE:D2:3C:01:AB:51:65:0D:95:4A:47:1B:CB:68:CE:6A:A8:11:CC:86:00:5F:1C:10:01:42:44:E2:FE:7B:21\r\n"
                     "a=setup:actpass\r\n"
                     "a=mid:audio\r\n"
                     "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n"
                     "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n"
                     "a=recvonly\r\n"
                     "a=rtcp-mux\r\n"
                     "a=rtpmap:111 opus/48000/2\r\n"
                     "a=fmtp:111 minptime=10; useinbandfec=1\r\n"
                     "a=maxptime:60\r\n");

  BOOST_TEST_MESSAGE ("offer: " + offer);

  std::string answer = webRtcEp->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);


  if (answer.find ("opus/48000/2") == std::string::npos) {
    BOOST_ERROR ("Answer doesn't contain opus");
  }

  releaseWebRtc (webRtcEp);
  webRtcEp = createWebrtc();

  offer = ("v=0\r\n"
           "o=- 5403198809162161286 2 IN IP4 127.0.0.1\r\n"
           "s=-\r\n"
           "t=0 0\r\n"
           "a=group:BUNDLE audio\r\n"
           "a=msid-semantic: WMS\r\n"
           "m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 126\r\n"
           "c=IN IP4 0.0.0.0\r\n"
           "a=rtcp:9 IN IP4 0.0.0.0\r\n"
           "a=ice-ufrag:z5Ynp3MoUpUWP2II\r\n"
           "a=ice-pwd:1dhv/Ia7Vk4yGt/sugyhSz7Q\r\n"
           "a=fingerprint:sha-256 AF:FE:D2:3C:01:AB:51:65:0D:95:4A:47:1B:CB:68:CE:6A:A8:11:CC:86:00:5F:1C:10:01:42:44:E2:FE:7B:21\r\n"
           "a=setup:actpass\r\n"
           "a=mid:audio\r\n"
           "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n"
           "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n"
           "a=recvonly\r\n"
           "a=rtcp-mux\r\n"
           "a=rtpmap:111 OPUS/48000/2\r\n"
           "a=fmtp:111 minptime=10; useinbandfec=1\r\n"
           "a=maxptime:60\r\n");

  BOOST_TEST_MESSAGE ("offer: " + offer);

  answer = webRtcEp->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  if (answer.find ("OPUS/48000/2") == std::string::npos) {
    BOOST_ERROR ("Answer doesn't contain opus");
  }

  releaseWebRtc (webRtcEp);
}

static void
check_data_channel ()
{
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpOfferer = createWebrtc (true);
  std::shared_ptr <WebRtcEndpointImpl> webRtcEpAnswerer = createWebrtc (true);
  std::atomic<bool> pass_cond (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);
  int chanId = -1;

  connectWebrtcEndpoints (webRtcEpOfferer, webRtcEpAnswerer, false);

  webRtcEpAnswerer->signalOnDataChannelOpened.connect ([&] (
  OnDataChannelOpened event) {
    BOOST_TEST_MESSAGE ("Data channel " << event.getChannelId() << " opened");

    if (chanId < 0) {
      chanId = event.getChannelId();
    }

    pass_cond = true;
    cv.notify_one();
  });

  webRtcEpOfferer->signalOnDataChannelClosed.connect ([&] (
  OnDataChannelClosed event) {
    BOOST_TEST_MESSAGE ("Data channel " << event.getChannelId() << " closed");

    if (chanId != event.getChannelId() ) {
      BOOST_ERROR ("Unexpected data channel closed");
    }

    pass_cond = true;
    cv.notify_one();
  });

  for (int i = 0; i < NUMBER_OF_RECONNECTIONS; i++) {
    pass_cond = false;
    chanId = -1;

    webRtcEpOfferer->createDataChannel ("TestDataChannel");

    cv.wait (lck, [&] () {
      return pass_cond.load();
    });

    if (!pass_cond) {
      BOOST_ERROR ("No data channel opened");
    }

    pass_cond = false;
    webRtcEpAnswerer->closeDataChannel (chanId);

    cv.wait (lck, [&] () {
      return pass_cond.load();
    });

    if (!pass_cond) {
      BOOST_ERROR ("No data channel closed");
    }
  }

  releaseWebRtc (webRtcEpOfferer);
  releaseWebRtc (webRtcEpAnswerer);
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
  test->add (BOOST_TEST_CASE ( &check_codec_sdp ), 0, /* timeout */ 15);

  test->add (BOOST_TEST_CASE ( &check_data_channel ), 0, /* timeout */ 15);

  return test;
}
