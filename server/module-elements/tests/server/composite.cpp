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
#define BOOST_TEST_IGNORE_NON_ZERO_CHILD_CODE // Don't fail if child process fails

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/CompositeImpl.hpp>
#include <objects/HubPortImpl.hpp>
#include <objects/PassThroughImpl.hpp>
#include <MediaFlowInStateChanged.hpp>
#include <MediaFlowState.hpp>
#include <MediaType.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>
#include <GstreamerDotDetails.hpp>


#define DIR_TEMPLATE "/tmp/recoder_test_XXXXXX"

using namespace kurento;
using namespace boost::unit_test;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

#define EXPECTED_LEN 0.2
static const int TIMEOUT = 5; /* seconds */

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

static std::shared_ptr <CompositeImpl>
createComposite ()
{
  std::shared_ptr <kurento::MediaObjectImpl> composite;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;

  composite = moduleManager.getFactory ("Composite")->createObject (
                       config, "",
                       constructorParams );

  return std::dynamic_pointer_cast <CompositeImpl> (composite);
}

static void
releaseComposite (std::shared_ptr<CompositeImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}


static std::shared_ptr <HubPortImpl>
createHubPort (std::shared_ptr<CompositeImpl> composite)
{
  std::shared_ptr <kurento::MediaObjectImpl> port;
  Json::Value constructorParams;

  constructorParams ["hub"] = composite->getId();

  port = moduleManager.getFactory ("HubPort")->createObject (
                       config, "",
                       constructorParams );

  return std::dynamic_pointer_cast <HubPortImpl> (port);
}

static void
releaseHubPort (std::shared_ptr<HubPortImpl> &ep)
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
dumpPipeline (std::shared_ptr<MediaPipeline> pipeline, std::string fileName)
{
  std::string pipelineDot;
  std::shared_ptr<GstreamerDotDetails> details (new GstreamerDotDetails ("SHOW_ALL"));

  pipelineDot = pipeline->getGstreamerDot (details);
  std::ofstream out(fileName);

  out << pipelineDot;
  out.close ();

}

void
dumpPipeline (std::string pipelineId, std::string fileName)
{
  std::shared_ptr<MediaPipeline> pipeline = std::dynamic_pointer_cast<MediaPipeline> (MediaSet::getMediaSet ()->getMediaObject (pipelineId));
  dumpPipeline (pipeline, fileName);

//  MediaSet::getMediaSet ()->release (pipelineId);
}

static std::shared_ptr<MediaElementImpl> getMediaElement (std::shared_ptr<PassThroughImpl> element)
{
	return std::dynamic_pointer_cast<MediaElementImpl> (element);
}


static std::shared_ptr <PassThroughImpl>
createPassThrough (std::string mediaPipelineId)
{
  std::shared_ptr <kurento::MediaObjectImpl> pt;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;

  pt = moduleManager.getFactory ("PassThrough")->createObject (
                  config, "",
                  constructorParams );

  return std::dynamic_pointer_cast <PassThroughImpl> (pt);
}

static void
releasePassTrhough (std::shared_ptr<PassThroughImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}



static void
composite_setup ()
{
  std::atomic<bool> media_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);
  std::shared_ptr<CompositeImpl> composite = createComposite ();
  std::shared_ptr<MediaElementImpl> src1 = createTestSrc();
  std::shared_ptr<MediaElementImpl> src2 = createTestSrc();
  std::shared_ptr<PassThroughImpl> pt = createPassThrough(mediaPipelineId);
  std::shared_ptr<HubPortImpl> port1 = createHubPort (composite);
  std::shared_ptr<HubPortImpl> port2 = createHubPort (composite);

  bool audio_flowing = false;
  bool video_flowing = false;

  sigc::connection conn = getMediaElement(pt)->signalMediaFlowInStateChanged.connect([&] (
		  MediaFlowInStateChanged event) {
	  	  	  std::shared_ptr<MediaFlowState> state = event.getState();
	  	  	  if (state->getValue() == MediaFlowState::FLOWING) {
		  	  	  BOOST_CHECK (state->getValue() == MediaFlowState::FLOWING);
                if (event.getMediaType ()->getValue() == MediaType::AUDIO) {
                    BOOST_TEST_MESSAGE ("Audio flowing");
                    audio_flowing = true;
                } else if (event.getMediaType ()->getValue() == MediaType::VIDEO) {
                    BOOST_TEST_MESSAGE ("Video flowing");
                    video_flowing = true;
                }
      	  	  } else if (state->getValue() == MediaFlowState::NOT_FLOWING) {
                if (event.getMediaType ()->getValue() == MediaType::AUDIO) {
                    BOOST_TEST_MESSAGE ("Audio not flowing");
                    audio_flowing = false;
                } else if (event.getMediaType ()->getValue() == MediaType::VIDEO) {
                    BOOST_TEST_MESSAGE ("Video not flowing");
                    video_flowing = false;
                }
              }
              if (audio_flowing && video_flowing) {
		  	  	  media_state_changed = true; 
		  	  	  cv.notify_one();
              }
          }
  );

  src1->connect (port1);
  src2->connect (port2);
  port1->connect (pt);


  dumpPipeline (mediaPipelineId, "composite_start.dot");
  // First stream
  cv.wait_for (lck, std::chrono::seconds(10), [&] () {
    return media_state_changed.load();
  });
  conn.disconnect ();

  dumpPipeline (mediaPipelineId, "composite_end.dot");

  if (!((getMediaElement(pt)->isMediaFlowingIn (std::make_shared<MediaType>(MediaType::AUDIO))) && ((getMediaElement(pt)->isMediaFlowingIn (std::make_shared<MediaType>(MediaType::VIDEO)))))) {
    BOOST_ERROR ("Media is not flowing out from composite");
  }

 

  releasePassTrhough(pt);
  releaseHubPort(port1);
  releaseHubPort(port2);
  releaseComposite (composite);
  releaseTestSrc(src1);
  releaseTestSrc(src2);

}

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "RecorderEndpoint" );

  test->add (BOOST_TEST_CASE ( &composite_setup), 0, /* timeout */ 100);

  return test;
}
