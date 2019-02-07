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

#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/RtpEndpointImpl.hpp>
#include <IceCandidate.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <MediaSet.hpp>
#include <MediaElementImpl.hpp>
#include <ConnectionState.hpp>
#include <MediaState.hpp>

using namespace kurento;
using namespace boost::unit_test;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE (GF);

GF::GF()
{
  boost::property_tree::ptree ac, audioCodecs, vc, videoCodecs;
  gst_init(nullptr, nullptr);

  moduleManager.loadModulesFromDirectories ("../../src/server:../../..");

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

static std::shared_ptr <RtpEndpointImpl>
createRtpEndpoint (bool useIpv6)
{
  std::shared_ptr <kurento::MediaObjectImpl> rtpEndpoint;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["useIpv6"] = useIpv6;

  rtpEndpoint = moduleManager.getFactory ("RtpEndpoint")->createObject (
                  config, "",
                  constructorParams );

  return std::dynamic_pointer_cast <RtpEndpointImpl> (rtpEndpoint);
}

static void
releaseRtpEndpoint (std::shared_ptr<RtpEndpointImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}

static std::shared_ptr<MediaElementImpl> createTestSrc() {
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
media_state_changes_impl (bool useIpv6)
{
  std::atomic<bool> media_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  std::shared_ptr <RtpEndpointImpl> rtpEpOfferer = createRtpEndpoint (useIpv6);
  std::shared_ptr <RtpEndpointImpl> rtpEpAnswerer = createRtpEndpoint (useIpv6);
  std::shared_ptr <MediaElementImpl> src = createTestSrc();

  src->connect (rtpEpOfferer);

  rtpEpAnswerer->signalMediaStateChanged.connect ([&] (
  MediaStateChanged event) {
    std::shared_ptr <MediaState> state = event.getNewState();
    BOOST_CHECK (state->getValue() == MediaState::CONNECTED);
    media_state_changed = true;
    cv.notify_one();
  });

  std::string offer = rtpEpOfferer->generateOffer ();
  BOOST_TEST_MESSAGE ("offer: " + offer);

  std::string answer = rtpEpAnswerer->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  rtpEpOfferer->processAnswer (answer);

  cv.wait (lck, [&] () {
    return media_state_changed.load();
  });

  if (!media_state_changed) {
    BOOST_ERROR ("Not media state chagned");
  }

  releaseTestSrc (src);
  releaseRtpEndpoint (rtpEpOfferer);
  releaseRtpEndpoint (rtpEpAnswerer);
}

static void
media_state_changes ()
{
  BOOST_TEST_MESSAGE ("Start test: media_state_changes");
  media_state_changes_impl (false);
}

static void
media_state_changes_ipv6 ()
{
  BOOST_TEST_MESSAGE ("Start test: media_state_changes_ipv6");
  media_state_changes_impl (true);
}

static void
connection_state_changes_impl (bool useIpv6)
{
  std::shared_ptr <RtpEndpointImpl> rtpEpOfferer = createRtpEndpoint (useIpv6);
  std::shared_ptr <RtpEndpointImpl> rtpEpAnswerer = createRtpEndpoint (useIpv6);
  std::atomic<bool> conn_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  rtpEpAnswerer->signalConnectionStateChanged.connect ([&] (
  ConnectionStateChanged event) {
    conn_state_changed = true;
    cv.notify_one();
  });

  std::string offer = rtpEpOfferer->generateOffer ();
  BOOST_TEST_MESSAGE ("offer: " + offer);

  if (rtpEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::DISCONNECTED) {
    BOOST_ERROR ("Connection must be disconnected");
  }

  std::string answer = rtpEpAnswerer->processOffer (offer);
  BOOST_TEST_MESSAGE ("answer: " + answer);

  if (rtpEpOfferer->getConnectionState ()->getValue () !=
      ConnectionState::DISCONNECTED) {
    BOOST_ERROR ("Connection must be disconnected");
  }

  rtpEpOfferer->processAnswer (answer);

  cv.wait (lck, [&] () {
    return conn_state_changed.load();
  });

  if (!conn_state_changed) {
    BOOST_ERROR ("Not conn state chagned");
  }

  if (rtpEpAnswerer->getConnectionState ()->getValue () !=
      ConnectionState::CONNECTED) {
    BOOST_ERROR ("Connection must be connected");
  }

  if (rtpEpOfferer->getConnectionState ()->getValue () !=
      ConnectionState::CONNECTED) {
    BOOST_ERROR ("Connection must be connected");
  }

  releaseRtpEndpoint (rtpEpOfferer);
  releaseRtpEndpoint (rtpEpAnswerer);
}

static void
connection_state_changes ()
{
  BOOST_TEST_MESSAGE ("Start test: connection_state_changes");
  connection_state_changes_impl (false);
}

static void
connection_state_changes_ipv6 ()
{
  BOOST_TEST_MESSAGE ("Start test: connection_state_changes_ipv6");
  connection_state_changes_impl (true);
}

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "WebRtcEndpoint" );

  test->add (BOOST_TEST_CASE ( &media_state_changes ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &connection_state_changes ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &media_state_changes_ipv6 ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &connection_state_changes_ipv6 ),
             0, /* timeout */ 15);

  return test;
}
