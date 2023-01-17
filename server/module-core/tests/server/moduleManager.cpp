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
#define BOOST_TEST_MODULE ModuleManager
#include <boost/test/unit_test.hpp>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <Error.hpp>
#include <gst/gst.h>
#include <MediaSet.hpp>

#include <config.h>
#include <iostream>

using namespace kurento;

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE (GF);

GF::GF() = default;

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

BOOST_AUTO_TEST_CASE (load_modules)
{
  std::shared_ptr <ModuleManager> moduleManager (new ModuleManager() );
  std::shared_ptr<kurento::Factory> mediaPipelineFactory;
  std::shared_ptr<kurento::MediaObject> mediaPipeline;

  gst_init(nullptr, nullptr);

  std::string moduleName = "../../src/server/libkmscoremodule.so";

  moduleManager->loadModule (moduleName);

  // Test if factory can be get with our without module preffix
  mediaPipelineFactory = moduleManager->getFactory ("kurento.MediaPipeline");

  mediaPipelineFactory = moduleManager->getFactory ("MediaPipeline");

  mediaPipeline = mediaPipelineFactory->createObject (
                    boost::property_tree::ptree(), "", Json::Value() );

  try {
    kurento::JsonSerializer writer (true);
    kurento::JsonSerializer writer2 (true);
    kurento::JsonSerializer reader (false);
    kurento::Error e (mediaPipeline, "Error description", 10, "ERROR_TYPE");
    std::shared_ptr<kurento::Error> e2;

    writer.Serialize ("error", e);

    reader.JsonValue = writer.JsonValue;
    reader.Serialize ("error", e2);

    writer2.Serialize ("error", e2);

    if (writer.JsonValue.toStyledString() != writer2.JsonValue.toStyledString() ) {
      std::cerr << "Serialization does not match" << std::endl;
      BOOST_ERROR ("Serialization does not match");
    }

  } catch (kurento::KurentoException &e) {
    std::cerr << "Unexpected exception: " << e.what() << std::endl;
    BOOST_ERROR ("Unexpected error");
  }

  kurento::MediaSet::getMediaSet()->release (std::dynamic_pointer_cast
      <MediaObjectImpl> (mediaPipeline) );

  auto data = moduleManager->getModules().at ("libkmscoremodule.so");

  if (data->getName() != "core") {
    std::cerr << "Module name should be core, but is " << data->getName() <<
              std::endl;
    BOOST_ERROR ("Wrong module name");
  }

  if (data->getVersion() != VERSION) {
    std::cerr << "Module version should be " << VERSION << ", but is " <<
              data->getVersion() << std::endl;
    BOOST_ERROR ("Wrong module version");
  }

  BOOST_CHECK (! data->getGenerationTime().empty() );
}
