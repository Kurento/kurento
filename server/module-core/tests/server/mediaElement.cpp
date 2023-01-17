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
#define BOOST_TEST_MODULE MediaElement
#include <boost/test/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <MediaElementImpl.hpp>
#include <ElementConnectionData.hpp>
#include <MediaType.hpp>
#include <KurentoException.hpp>
#include <GstreamerDotDetails.hpp>
#include <MediaSet.hpp>
#include <ModuleManager.hpp>

using namespace kurento;

ModuleManager moduleManager;
boost::property_tree::ptree config;

struct GF {
  GF();
  ~GF();
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

static std::shared_ptr <MediaElementImpl>
createDummyElement (const std::string &name, const std::string &mediaPipelineId)
{
  auto mediaObject = MediaSet::getMediaSet()->ref (new  MediaElementImpl (
                       boost::property_tree::ptree(),
                       MediaSet::getMediaSet()->getMediaObject (mediaPipelineId),
                       name) );
  std::shared_ptr <MediaElementImpl> element = std::dynamic_pointer_cast
      <MediaElementImpl> (mediaObject);
  MediaSet::getMediaSet()->ref ("", mediaObject);

  return element;
}

static void
releaseMediaObject (const std::string &id)
{
  MediaSet::getMediaSet ()->release (id);
}

BOOST_AUTO_TEST_CASE (connection_test)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::shared_ptr <MediaElementImpl> sink = createDummyElement ("dummysink",
      mediaPipelineId);
  std::shared_ptr <MediaElementImpl> src = createDummyElement ("dummysrc",
      mediaPipelineId);

  std::shared_ptr <MediaType> VIDEO (new MediaType (MediaType::VIDEO) );
  std::shared_ptr <MediaType> AUDIO (new MediaType (MediaType::AUDIO) );
  std::shared_ptr <MediaType> DATA (new MediaType (MediaType::DATA) );

  src->setName ("SOURCE");
  sink->setName ("SINK");

  src->connect (sink);
  auto connections = sink->getSourceConnections ();

  BOOST_CHECK (connections.size() == 3);

  for (auto it : connections) {
    BOOST_CHECK (it->getSource()->getId() == src->getId() );
  }

  g_object_set (src->getGstreamerElement(), "audio", TRUE, "video", TRUE,
                "data", TRUE, NULL);
  g_object_set (sink->getGstreamerElement(), "audio", TRUE, "video", TRUE,
                "data", TRUE, NULL);

  connections = src->getSinkConnections ();

  BOOST_CHECK (connections.size() == 3);

  for (auto it : connections) {
    BOOST_CHECK (it->getSource()->getId() == src->getId() );
  }

  connections = sink->getSourceConnections (AUDIO);
  BOOST_CHECK (connections.size() == 1);

  connections = sink->getSourceConnections (AUDIO, "default");
  BOOST_CHECK (connections.size() == 1);

  connections = sink->getSourceConnections (AUDIO, "test");
  BOOST_CHECK (connections.size() == 0);

  connections = sink->getSourceConnections (VIDEO);
  BOOST_CHECK (connections.size() == 1);

  connections = sink->getSourceConnections (VIDEO, "default");
  BOOST_CHECK (connections.size() == 1);

  connections = sink->getSourceConnections (VIDEO, "test");
  BOOST_CHECK (connections.size() == 0);

  connections = sink->getSourceConnections (DATA, "default");
  BOOST_CHECK (connections.size() == 1);

  connections = sink->getSourceConnections (DATA, "test");
  BOOST_CHECK (connections.size() == 0);

  src->disconnect (sink);

  connections = sink->getSourceConnections ();

  BOOST_CHECK (connections.size() == 0);

  src->connect (sink, AUDIO);

  connections = sink->getSourceConnections ();
  BOOST_CHECK (connections.size() == 1);

  connections = src->getSinkConnections ();
  BOOST_CHECK (connections.size() == 1);

  connections = sink->getSourceConnections (VIDEO, "default");
  BOOST_CHECK (connections.size() == 0);

  releaseMediaObject (src->getId() );
  releaseMediaObject (sink->getId() );
  releaseMediaObject (mediaPipelineId);

  sink.reset();
  src.reset();
}

BOOST_AUTO_TEST_CASE (release_before_real_connection)
{
  GstElement *srcElement;

  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::shared_ptr <MediaElementImpl> sink = createDummyElement ("dummysink",
      mediaPipelineId);
  std::shared_ptr <MediaElementImpl> src = createDummyElement ("dummysrc",
      mediaPipelineId);

  src->setName ("SOURCE");
  sink->setName ("SINK");

  g_object_set (sink->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);
  srcElement = (GstElement *) g_object_ref (src->getGstreamerElement() );
  g_object_set (srcElement, "audio", TRUE, NULL);

  src->connect (sink);

  src->disconnect (sink);

  g_object_set (srcElement, "audio", TRUE, "video", TRUE, NULL);
  g_object_unref (srcElement);

  releaseMediaObject (src->getId() );
  releaseMediaObject (sink->getId() );
  releaseMediaObject (mediaPipelineId);

  src.reset();
  sink.reset();
}

BOOST_AUTO_TEST_CASE (loopback)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::shared_ptr <MediaElementImpl> duplex = createDummyElement ("dummyduplex",
      mediaPipelineId);

  duplex->setName ("DUPLEX");

  g_object_set (duplex->getGstreamerElement(), "src-audio", TRUE, "src-video",
                TRUE, "sink-audio", TRUE, "sink-video", TRUE, NULL);

  duplex->connect (duplex);

  releaseMediaObject (duplex->getId() );
  releaseMediaObject (mediaPipelineId);

  duplex.reset();
}

BOOST_AUTO_TEST_CASE (no_common_pipeline)
{
  std::string mediaPipelineId1 =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::string mediaPipelineId2 =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::shared_ptr <MediaElementImpl> sink = createDummyElement ("dummysink",
      mediaPipelineId1);
  std::shared_ptr <MediaElementImpl> src = createDummyElement ("dummysrc",
      mediaPipelineId2);


  src->setName ("SOURCE");
  sink->setName ("SINK");

  try {
    src->connect (sink);
    BOOST_FAIL ("Previous operation should raise an exception");
  } catch (const KurentoException &e) {
    BOOST_CHECK (e.getCode () == CONNECT_ERROR);
  }

  releaseMediaObject (sink->getId() );
  releaseMediaObject (src->getId() );
  releaseMediaObject (mediaPipelineId1);
  releaseMediaObject (mediaPipelineId2);

  sink.reset();
  src.reset();
}

BOOST_AUTO_TEST_CASE (dot_test)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  std::shared_ptr <MediaElementImpl> sink = createDummyElement ("dummysink",
      mediaPipelineId);
  std::shared_ptr <MediaElementImpl> src = createDummyElement ("dummysrc",
      mediaPipelineId);
  std::shared_ptr <MediaPipelineImpl> pipe = std::dynamic_pointer_cast
      <MediaPipelineImpl> (MediaSet::getMediaSet()->getMediaObject (
                             mediaPipelineId) );

  g_object_set (src->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);
  g_object_set (sink->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);

  src->connect (sink);

  std::string dot = sink->getGstreamerDot();

  BOOST_CHECK (!dot.empty() );

  dot = src->getGstreamerDot();

  BOOST_CHECK (!dot.empty() );

  dot = pipe->getGstreamerDot();

  BOOST_CHECK (!dot.empty() );

  std::shared_ptr<kurento::GstreamerDotDetails> details (new
      kurento::GstreamerDotDetails (kurento::GstreamerDotDetails::SHOW_STATES) );
  dot = pipe->getGstreamerDot (details);

  BOOST_CHECK (!dot.empty() );

  releaseMediaObject (sink->getId() );
  releaseMediaObject (src->getId() );
  releaseMediaObject (mediaPipelineId);

  sink.reset();
  src.reset();
  pipe.reset();
}
