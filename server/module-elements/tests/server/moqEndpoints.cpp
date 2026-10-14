/*
 * (C) Copyright 2026 Kurento (http://kurento.org/)
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/MediaElementImpl.hpp>
#include <objects/MoqPublisherEndpointImpl.hpp>
#include <objects/MoqSubscriberEndpointImpl.hpp>
#include <MediaFlowOutStateChanged.hpp>
#include <MediaFlowState.hpp>
#include <MediaType.hpp>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>

#include <condition_variable>
#include <cstdlib>
#include <mutex>
#include <string>

using namespace kurento;
using namespace boost::unit_test;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

static const auto MEDIA_TIMEOUT = std::chrono::seconds(45);

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE(GF);

GF::GF()
{
  gst_init(nullptr, nullptr);
  moduleManager.loadModulesFromDirectories("../../src/server:../../..");

  mediaPipelineId = moduleManager.getFactory("MediaPipeline")->createObject(
      config, "", Json::Value())->getId();
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

static std::shared_ptr<MediaElementImpl>
createTestElement(const char *factoryName)
{
  std::shared_ptr<MediaElementImpl> element =
      std::dynamic_pointer_cast<MediaElementImpl>(
          MediaSet::getMediaSet()->ref(new MediaElementImpl(
              boost::property_tree::ptree(),
              MediaSet::getMediaSet()->getMediaObject(mediaPipelineId),
              factoryName)));

  g_object_set(element->getGstreamerElement(),
      "audio", TRUE, "video", TRUE, NULL);
  return element;
}

static std::shared_ptr<MoqPublisherEndpointImpl>
createPublisher(const std::string &url, const std::string &broadcast)
{
  Json::Value params;
  params["mediaPipeline"] = mediaPipelineId;
  params["url"] = url;
  params["broadcast"] = broadcast;

  return std::dynamic_pointer_cast<MoqPublisherEndpointImpl>(
      moduleManager.getFactory("MoqPublisherEndpoint")->createObject(
          config, "", params));
}

static std::shared_ptr<MoqSubscriberEndpointImpl>
createSubscriber(const std::string &url, const std::string &broadcast)
{
  Json::Value params;
  params["mediaPipeline"] = mediaPipelineId;
  params["url"] = url;
  params["broadcast"] = broadcast;

  return std::dynamic_pointer_cast<MoqSubscriberEndpointImpl>(
      moduleManager.getFactory("MoqSubscriberEndpoint")->createObject(
          config, "", params));
}

static void
releaseElement(std::shared_ptr<MediaElementImpl> &element)
{
  const std::string id = element->getId();
  element.reset();
  MediaSet::getMediaSet()->release(id);
}

static void
releaseEndpoint(std::shared_ptr<MediaObjectImpl> &endpoint)
{
  const std::string id = endpoint->getId();
  endpoint.reset();
  MediaSet::getMediaSet()->release(id);
}

static void
moq_audio_and_video_round_trip()
{
  const char *relayUrl = std::getenv("KURENTO_MOQ_TEST_RELAY_URL");
  BOOST_REQUIRE_MESSAGE(relayUrl != nullptr && relayUrl[0] != '\0',
      "Set KURENTO_MOQ_TEST_RELAY_URL to a reachable MoQ relay to run this "
      "integration test");

  const std::string broadcast = "kurento-moq-test-" +
      std::to_string(g_get_monotonic_time());
  std::shared_ptr<MediaElementImpl> source = createTestElement("dummysrc");
  std::shared_ptr<MediaElementImpl> sink = createTestElement("dummysink");
  std::shared_ptr<MoqPublisherEndpointImpl> publisher =
      createPublisher(relayUrl, broadcast);
  std::shared_ptr<MoqSubscriberEndpointImpl> subscriber =
      createSubscriber(relayUrl, broadcast);
  std::shared_ptr<MediaElementImpl> subscriberElement =
      std::dynamic_pointer_cast<MediaElementImpl>(subscriber);

  std::mutex mutex;
  std::condition_variable condition;
  bool audioFlowing = false;
  bool videoFlowing = false;

  sigc::connection flowConnection =
      subscriberElement->signalMediaFlowOutStateChanged.connect(
          [&](MediaFlowOutStateChanged event) {
            const bool flowing =
                event.getState()->getValue() == MediaFlowState::FLOWING;

            std::lock_guard<std::mutex> lock(mutex);
            if (event.getMediaType()->getValue() == MediaType::AUDIO) {
              audioFlowing = flowing;
            } else if (event.getMediaType()->getValue() == MediaType::VIDEO) {
              videoFlowing = flowing;
            }
            condition.notify_one();
          });

  source->connect(publisher);
  subscriber->connect(sink);
  subscriber->subscribe();
  publisher->publish();

  std::unique_lock<std::mutex> lock(mutex);
  const bool receivedAudioAndVideo = condition.wait_for(
      lock, MEDIA_TIMEOUT, [&]() { return audioFlowing && videoFlowing; });
  lock.unlock();

  flowConnection.disconnect();
  publisher->unpublish();
  subscriber->unsubscribe();

  BOOST_CHECK_MESSAGE(receivedAudioAndVideo,
      "The MoQ subscriber did not receive flowing audio and video before "
      "the timeout");

  releaseElement(source);
  releaseElement(sink);
  releaseEndpoint(std::static_pointer_cast<MediaObjectImpl>(publisher));
  releaseEndpoint(std::static_pointer_cast<MediaObjectImpl>(subscriber));
}

test_suite *
init_unit_test_suite(int, char *[])
{
  test_suite *test = BOOST_TEST_SUITE("MoqEndpoints");
  test->add(BOOST_TEST_CASE(&moq_audio_and_video_round_trip), 0, 50);
  return test;
}
