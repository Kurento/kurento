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
#define BOOST_TEST_MODULE MediaSet
#include <boost/test/unit_test.hpp>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <MediaSet.hpp>
#include <ServerManagerImpl.hpp>
#include <ServerInfo.hpp>
#include <ModuleInfo.hpp>
#include <ServerType.hpp>
#include <ObjectCreated.hpp>
#include <ObjectDestroyed.hpp>
#include <memory>

#include <config.h>

using namespace kurento;

std::shared_ptr <ModuleManager> moduleManager;

struct InitTests {
  InitTests();
  ~InitTests();
};

BOOST_GLOBAL_FIXTURE (InitTests);

InitTests::InitTests()
{
  gst_init(nullptr, nullptr);

  moduleManager = std::make_shared<ModuleManager>();

  gst_init(nullptr, nullptr);

  std::string moduleName = "../../src/server/libkmscoremodule.so";

  moduleManager->loadModule (moduleName);
}

InitTests::~InitTests()
{
  moduleManager.reset();
  MediaSet::deleteMediaSet();
}


struct F {
  F();
  ~F();

  std::shared_ptr<ServerManagerImpl> serverManager;
};

F::F ()
{
  std::vector<std::shared_ptr<ModuleInfo>> modules;

  for (auto moduleIt : moduleManager->getModules () ) {
    std::vector<std::string> factories;

    for (auto factIt : moduleIt.second->getFactories() ) {
      factories.push_back (factIt.first);
    }

    modules.push_back(std::make_shared<ModuleInfo>(
        moduleIt.second->getVersion(), moduleIt.second->getName(),
        moduleIt.second->getGenerationTime(), factories));
  }

  std::shared_ptr<ServerType> type (new ServerType (ServerType::KMS) );
  std::vector<std::string> capabilities;
  capabilities.emplace_back("transactions");

  std::shared_ptr<ServerInfo> serverInfo =
      std::make_shared<ServerInfo>("", modules, type, capabilities);

  serverManager =  std::dynamic_pointer_cast <ServerManagerImpl>
                   (MediaSet::getMediaSet ()->ref (new ServerManagerImpl (
                         serverInfo, boost::property_tree::ptree (), *moduleManager.get() ) ) );
  MediaSet::getMediaSet ()->setServerManager (std::dynamic_pointer_cast
      <ServerManagerImpl> (serverManager) );
}

F::~F ()
{
  serverManager.reset();
  MediaSet::deleteMediaSet();
}

BOOST_FIXTURE_TEST_CASE (release_elements, F)
{
  std::mutex mtx;
  std::condition_variable cv;
  bool created = false;
  bool destroyed = false;
  std::string watched_object;

  std::shared_ptr<kurento::Factory> mediaPipelineFactory;
  std::shared_ptr<kurento::Factory> passThroughFactory;
  std::string mediaPipelineId;
  std::string passThroughId;
  std::string passThrough2Id;

  sigc::connection createdConn = serverManager->signalObjectCreated.connect ([&] (
  ObjectCreated event) {
    std::unique_lock<std::mutex> lck (mtx);
    created = true;
    cv.notify_one();
  });

  sigc::connection destroyedConn =
  serverManager->signalObjectDestroyed.connect ([&] (ObjectDestroyed event) {
    std::unique_lock<std::mutex> lck (mtx);

    if (watched_object == event.getObjectId() ) {
      destroyed = true;
      cv.notify_one();
    }
  });

  mediaPipelineFactory = moduleManager->getFactory ("MediaPipeline");
  passThroughFactory = moduleManager->getFactory ("PassThrough");

  mediaPipelineId = mediaPipelineFactory->createObject (
                      boost::property_tree::ptree(), "session1", Json::Value() )->getId();

  // Wait for creation event
  std::unique_lock<std::mutex> lck (mtx);

  if (!cv.wait_for (lck, std::chrono::seconds (1), [&created] () {
  return created;
}) ) {
    BOOST_FAIL ("Timeout waiting for creationg event");
  }
  createdConn.disconnect();

  watched_object = mediaPipelineId;
  lck.unlock();

  Json::Value params;
  params["mediaPipeline"] = mediaPipelineId;

  passThroughId = passThroughFactory->createObject (
                    boost::property_tree::ptree(), "session1", params )->getId();

  passThrough2Id = passThroughFactory->createObject (
                     boost::property_tree::ptree(), "session1", params )->getId();

  // Ref by other session
  MediaSet::getMediaSet()->ref ("session2", mediaPipelineId);
  MediaSet::getMediaSet()->ref ("session2", passThroughId);


  lck.lock();
  watched_object = passThrough2Id;
  lck.unlock();

  // This should destroy passThrow2 but no other elements
  kurento::MediaSet::getMediaSet()->unref ("session1", mediaPipelineId);

  lck.lock();

  if (!cv.wait_for (lck, std::chrono::seconds (1), [&destroyed] () {
  return destroyed;
}) ) {
    BOOST_FAIL ("Timeout waiting for " + watched_object + " destruction event");
  }
  watched_object = "";
  destroyed = false;
  lck.unlock();

  // Check that objects in session2 were not destroyed
  MediaSet::getMediaSet()->ref ("session3", mediaPipelineId);
  MediaSet::getMediaSet()->ref ("session3", passThroughId);

  // Check that object not in session2 were destroyed
  try {
    MediaSet::getMediaSet()->ref ("session3", passThrough2Id);
    BOOST_FAIL ("This code should not be reached");
  } catch (const KurentoException &e) {
    BOOST_CHECK (e.getCode() == MEDIA_OBJECT_NOT_FOUND);
  }

  lck.lock();
  watched_object = mediaPipelineId;
  lck.unlock();

  BOOST_CHECK (kurento::MediaSet::getMediaSet()->getPipelines ("session3").size()
               == 1);

  kurento::MediaSet::getMediaSet()->release (mediaPipelineId);

  // Wait for pipeline destruction event
  lck.lock();

  if (!cv.wait_for (lck, std::chrono::seconds (1), [&destroyed] () {
  return destroyed;
}) ) {
    BOOST_FAIL ("Timeout waiting for " + watched_object + " destruction event");
  }
}

BOOST_FIXTURE_TEST_CASE (get_pipelines, F)
{
  std::shared_ptr<kurento::Factory> mediaPipelineFactory;
  std::string mediaPipelineId;

  mediaPipelineFactory = moduleManager->getFactory ("MediaPipeline");

  auto obj = mediaPipelineFactory->createObject (boost::property_tree::ptree(),
             "session1", Json::Value() );
  obj = mediaPipelineFactory->createObject (boost::property_tree::ptree(),
        "session1", Json::Value() );
  obj = mediaPipelineFactory->createObject (boost::property_tree::ptree(),
        "session1", Json::Value() );
  obj.reset();

  auto pipes = kurento::MediaSet::getMediaSet()->getPipelines ("session3");
  BOOST_CHECK (pipes.size() == 3);

  for (auto pipe : pipes) {
    kurento::MediaSet::getMediaSet()->release (pipe->getId() );
  }

  pipes.clear();
}
