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
#include <objects/PlayerEndpointImpl.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>

#include <cstdio>
#include <iostream>

using namespace kurento;
using namespace boost::unit_test;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

#define EXPECTED_LEN 1.0
#define PLAYED_FILE "http://files.openvidu.io/video/format/small.webm"
#define TIME (15 * G_TIME_SPAN_SECOND)

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

  mediaPipelineId = moduleManager.getFactory ("MediaPipeline")->createObject (
                      config, "",
                      Json::Value() )->getId();
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

static std::shared_ptr <PlayerEndpointImpl>
createPlayerEndpoint ()
{
  std::shared_ptr <kurento::MediaObjectImpl> playerEndpoint;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["uri"] = PLAYED_FILE;

  playerEndpoint = moduleManager.getFactory ("PlayerEndpoint")->createObject (
                     config, "",
                     constructorParams );

  return std::dynamic_pointer_cast <PlayerEndpointImpl> (playerEndpoint);
}

static void
releasePlayerEndpoint (std::shared_ptr<PlayerEndpointImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}

static std::shared_ptr<MediaElementImpl> createTestSink() {
  std::shared_ptr <MediaElementImpl> src = std::dynamic_pointer_cast
      <MediaElementImpl> (MediaSet::getMediaSet()->ref (new  MediaElementImpl (
                            boost::property_tree::ptree(),
                            MediaSet::getMediaSet()->getMediaObject (mediaPipelineId),
                            "dummysink") ) );

  g_object_set (src->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);

  return std::dynamic_pointer_cast <MediaElementImpl> (src);
}

static void
releaseTestSink (std::shared_ptr<MediaElementImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}

static void
eos_received ()
{
  std::shared_ptr <PlayerEndpointImpl> player = createPlayerEndpoint ();
  std::shared_ptr <MediaElementImpl> sink = createTestSink();
  GCond cond{};
  GMutex mutex{};
  bool eos = false;
  gint64 end_time;

  g_mutex_init (&mutex);
  g_cond_init (&cond);

  player->signalEndOfStream.connect ([&] (EndOfStream event) {
    std::cout << "EOS received" << std::endl;
    g_mutex_lock (&mutex);
    eos = true;
    g_cond_signal (&cond);
    g_mutex_unlock (&mutex);
  });

  std::dynamic_pointer_cast <MediaElementImpl> (player)->connect (sink);

  player->play ();
  end_time = g_get_monotonic_time () + TIME;

  g_mutex_lock (&mutex);

  while (!eos) {
    if (!g_cond_wait_until (&cond, &mutex, end_time) ) {
      g_mutex_unlock (&mutex);
      std::cout << "EOS NOT received" << std::endl;
      BOOST_CHECK (false);
      return;
    }
  }

  g_mutex_unlock (&mutex);

  releaseTestSink (sink);
  releasePlayerEndpoint (player);
  g_mutex_clear (&mutex);
  g_cond_clear (&cond);
}

static void
eos_received_with_no_accept_eos_sink ()
{
  std::shared_ptr <PlayerEndpointImpl> player = createPlayerEndpoint ();
  std::shared_ptr <MediaElementImpl> sink = createTestSink();
  GCond cond{};
  GMutex mutex{};
  bool eos = false;
  gint64 end_time;

  g_mutex_init (&mutex);
  g_cond_init (&cond);

  g_object_set (sink->getGstreamerElement(), "accept-eos", FALSE, NULL);

  player->signalEndOfStream.connect ([&] (EndOfStream event) {
    std::cout << "EOS received" << std::endl;
    g_mutex_lock (&mutex);
    eos = true;
    g_cond_signal (&cond);
    g_mutex_unlock (&mutex);
  });

  std::dynamic_pointer_cast <MediaElementImpl> (player)->connect (sink);

  player->play ();
  end_time = g_get_monotonic_time () + TIME;

  g_mutex_lock (&mutex);

  while (!eos) {
    if (!g_cond_wait_until (&cond, &mutex, end_time) ) {
      g_mutex_unlock (&mutex);
      std::cout << "EOS NOT received" << std::endl;
      BOOST_CHECK (false);
      return;
    }
  }

  g_mutex_unlock (&mutex);

  releaseTestSink (sink);
  releasePlayerEndpoint (player);
  g_mutex_clear (&mutex);
  g_cond_clear (&cond);
}

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "PlayerEndpoint" );

  test->add (BOOST_TEST_CASE ( &eos_received ), 0, /* timeout */ 20);
  test->add (BOOST_TEST_CASE ( &eos_received_with_no_accept_eos_sink), 0,
             /* timeout */ 20);

  return test;
}
