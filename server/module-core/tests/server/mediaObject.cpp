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
#define BOOST_TEST_MODULE SdpEndpoint
#include <boost/test/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <MediaElementImpl.hpp>
#include <ElementConnectionData.hpp>
#include <MediaType.hpp>
#include <KurentoException.hpp>
#include <objects/MediaObjectImpl.hpp>
#include <string>
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

BOOST_AUTO_TEST_CASE (add_tag)
{
  std::shared_ptr <MediaObjectImpl> mediaObject ( new  MediaObjectImpl
      (boost::property_tree::ptree() ) );

  mediaObject->removeTag ("1");

  mediaObject->addTag ("1", "test1");
  mediaObject->addTag ("2", "test2");
  mediaObject->addTag ("3", "test3");

  mediaObject->removeTag ("3");
  mediaObject->removeTag ("5");
  mediaObject->removeTag ("3");

  mediaObject.reset ();
}

BOOST_AUTO_TEST_CASE (add_tag_media_element)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::shared_ptr <MediaElementImpl> mediaElement =
    createDummyElement ("dummyduplex", mediaPipelineId);

  mediaElement->removeTag ("1");

  mediaElement->addTag ("1", "test1");
  mediaElement->addTag ("2", "test2");
  mediaElement->addTag ("3", "test3");

  mediaElement->removeTag ("3");
  mediaElement->removeTag ("5");
  mediaElement->removeTag ("3");

  releaseMediaObject (mediaElement->getId() );
  releaseMediaObject (mediaPipelineId);

  mediaElement.reset ();
}

BOOST_AUTO_TEST_CASE (get_tag)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::shared_ptr <MediaElementImpl> mediaElement =
    createDummyElement ("dummyduplex", mediaPipelineId);

  mediaElement->addTag ("1", "test1");
  mediaElement->addTag ("2", "test2");

  try {
    std::string ret = mediaElement->getTag ("1");

    if (ret != "test1") {
      BOOST_ERROR ("Bad response");
    }
  } catch (KurentoException &e) {
    BOOST_ERROR ("Tag not found");
  }

  try {
    std::string ret = mediaElement->getTag ("3");
    BOOST_ERROR ("Tag not found not detected");
  } catch (KurentoException &e) {
    if (e.getCode () != 40110) {
      BOOST_ERROR ("Tag not found not detected");
    }
  }

  releaseMediaObject (mediaElement->getId() );
  releaseMediaObject (mediaPipelineId);

  mediaElement.reset ();
}

BOOST_AUTO_TEST_CASE (get_tags)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::shared_ptr <MediaElementImpl> mediaElement =
    createDummyElement ("dummyduplex", mediaPipelineId);
  std::vector<std::shared_ptr<Tag>> ret ;
  int i = 1;

  mediaElement->addTag ("1", "test1");
  mediaElement->addTag ("2", "test2");
  mediaElement->addTag ("3", "test3");

  ret = mediaElement->getTags ();

  if (ret.size () == 0) {
    BOOST_ERROR ("Tag list is empty");
  }

  for (auto it : ret ) {
    std::string key (std::to_string (i) );
    std::string value ("test" + std::to_string (i) );

    if ( (it->getKey() != key) || (it->getValue() != value) ) {
      BOOST_ERROR ("Tag list is wrong");
    }

    i++;
  }

  releaseMediaObject (mediaElement->getId() );
  releaseMediaObject (mediaPipelineId);

  mediaElement.reset ();
}

BOOST_AUTO_TEST_CASE (creation_time)
{
  std::string mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();
  std::shared_ptr <MediaElementImpl> mediaElement =
    createDummyElement ("dummyduplex", mediaPipelineId);
  std::shared_ptr <MediaPipelineImpl> pipe = std::dynamic_pointer_cast
      <MediaPipelineImpl> (MediaSet::getMediaSet()->getMediaObject (
                             mediaPipelineId) );
  time_t now = time(nullptr);

  BOOST_CHECK (pipe->getCreationTime() <= mediaElement->getCreationTime() );
  BOOST_CHECK (pipe->getCreationTime() <= now);
  BOOST_CHECK (mediaElement->getCreationTime() <= now);

  releaseMediaObject (mediaElement->getId() );
  releaseMediaObject (mediaPipelineId);

  mediaElement.reset ();
  pipe.reset ();
}
