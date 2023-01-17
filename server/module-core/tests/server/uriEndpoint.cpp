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
#define BOOST_TEST_MODULE UriEndpoint
#include <boost/test/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <MediaElementImpl.hpp>
#include <ElementConnectionData.hpp>
#include <MediaType.hpp>
#include <KurentoException.hpp>
#include <objects/UriEndpointImpl.hpp>
#include <MediaSet.hpp>
#include <ModuleManager.hpp>

#define DEFAULT_PATH_VALUE "file:///var/lib/kurento"

using namespace kurento;

std::string mediaPipelineId;
ModuleManager moduleManager;
boost::property_tree::ptree config;

struct GF {
  GF();
  ~GF();
};

class UriEndpointFactory : public Factory
{
public:
  std::string getName () const override { return "UriEndpointFactory"; }

protected:
  MediaObjectImpl *createObjectPointer (const boost::property_tree::ptree &conf,
      const Json::Value &params) const override
  {
    std::string mediaPipelineId = params["mediaPipeline"].asString ();
    std::string uri = params["uri"].asString ();

    std::shared_ptr <MediaObjectImpl> pipe =
      MediaSet::getMediaSet ()->getMediaObject (mediaPipelineId);

    return new  UriEndpointImpl (config, pipe, "dummyuri", uri);
  }
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

static void
releaseMediaObject (const std::string &id)
{
  MediaSet::getMediaSet ()->release (id);
}


static std::shared_ptr <UriEndpointImpl> generateUriEndpoint (
  const std::string &mediaPipelineId, const std::string &uri)
{
  UriEndpointFactory factory;
  Json::Value params;

  params["mediaPipeline"] = mediaPipelineId;
  params["uri"] = uri;

  return std::dynamic_pointer_cast<UriEndpointImpl> (
           factory.createObject (config, "", params) );
}

BOOST_AUTO_TEST_CASE (check_regular_uri)
{
  std::string uri;
  std::string first_uri = "file:///tmp/file.webm";

  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  config.add ("modules.kurento.UriEndpoint.configPath", "../../../tests" );
  config.add ("modules.kurento.UriEndpoint.defaultPath", DEFAULT_PATH_VALUE);

  std::shared_ptr <UriEndpointImpl> uriEndpoint = generateUriEndpoint (
        mediaPipelineId, first_uri);

  uri = uriEndpoint->getUri ();

  if (uri != first_uri) {
    BOOST_ERROR ("Wrong URI returned by UriEndpoint");
  }

  releaseMediaObject (uriEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  uriEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (check_media_element_uri)
{
  std::string uri;
  std::string first_uri = "file.webm";
  std::string expected_uri = DEFAULT_PATH_VALUE"/file.webm";
  char *uri_value;

  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  config.add ("modules.kurento.UriEndpoint.configPath", "../../../tests" );
  config.add ("modules.kurento.UriEndpoint.defaultPath", DEFAULT_PATH_VALUE"/");

  std::shared_ptr <UriEndpointImpl> uriEndpoint = generateUriEndpoint (
        mediaPipelineId, first_uri);

  uri = uriEndpoint->getUri ();

  if (uri != first_uri) {
    BOOST_ERROR ("Wrong URI returned by UriEndpoint");
  }

  g_object_get (G_OBJECT (uriEndpoint->getGstreamerElement() ), "uri", &uri_value,
                NULL);

  if (g_strcmp0 (uri_value, expected_uri.c_str () ) != 0) {
    BOOST_ERROR ("Wrong URI returned by gstreamer element");
  }

  g_free (uri_value);

  releaseMediaObject (uriEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  uriEndpoint.reset ();
}

BOOST_AUTO_TEST_CASE (check_uri_with_escaped_slashes)
{
  std::string uri;
  std::string first_uri =
    "https://test.com/026cba3020160826100051.mp4?params=host&user=AKIAIHFQODXEKWDTKIRQ%2F20160826%2Ftest%2Fs3%2Floc4_request&pass=db575aaf678085c2595df3bc3bc614";

  mediaPipelineId =
    moduleManager.getFactory ("MediaPipeline")->createObject (
      config, "",
      Json::Value() )->getId();

  config.add ("modules.kurento.UriEndpoint.configPath", "../../../tests" );
  config.add ("modules.kurento.UriEndpoint.defaultPath", DEFAULT_PATH_VALUE"/");

  std::shared_ptr <UriEndpointImpl> uriEndpoint = generateUriEndpoint (
        mediaPipelineId, first_uri);

  releaseMediaObject (uriEndpoint->getId() );
  releaseMediaObject (mediaPipelineId);

  uriEndpoint.reset ();
}
