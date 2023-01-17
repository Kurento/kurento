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

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MODULE RtpEndpoint
#include <boost/test/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <MediaElementImpl.hpp>
#include <ElementConnectionData.hpp>
#include <MediaType.hpp>
#include <KurentoException.hpp>
#include <objects/BaseRtpEndpointImpl.hpp>
#include <MediaSet.hpp>
#include <ModuleManager.hpp>

#define MIN_PORT 50000
#define MAX_PORT 50020

using namespace kurento;

std::string mediaPipelineId;
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
  moduleManager.loadModulesFromDirectories ("../../src/server:../../..");
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

static void
releaseMediaObject (const std::string &id)
{
  MediaSet::getMediaSet ()->release (id);
}

BOOST_AUTO_TEST_CASE (port_range)
{
  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  guint min_port, max_port;

  config.add ("modules.kurento.BaseRtpEndpoint.minPort", MIN_PORT);
  config.add ("modules.kurento.BaseRtpEndpoint.maxPort", MAX_PORT);

  std::shared_ptr <MediaObjectImpl> pipe =
    MediaSet::getMediaSet()->getMediaObject (
      mediaPipelineId);

  auto mediaObject = MediaSet::getMediaSet()->ref (new  BaseRtpEndpointImpl (
                       config, pipe, "dummyrtp") );
  std::shared_ptr <BaseRtpEndpointImpl> rtpEndpoint = std::dynamic_pointer_cast
      <BaseRtpEndpointImpl> (mediaObject);
  MediaSet::getMediaSet()->ref ("", mediaObject);

  g_object_get (rtpEndpoint->getGstreamerElement (), "min-port", &min_port,
                "max-port", &max_port, NULL);

  BOOST_CHECK_EQUAL (MIN_PORT, min_port);
  BOOST_CHECK_EQUAL (MAX_PORT, max_port);

  releaseMediaObject (rtpEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  rtpEndpoint.reset ();
  pipe.reset();
}
