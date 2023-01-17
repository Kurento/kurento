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
#define BOOST_TEST_MODULE ComplexType
#include <boost/test/unit_test.hpp>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <Fraction.hpp>
#include <gst/gst.h>
#include <MediaSet.hpp>
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


BOOST_AUTO_TEST_CASE (complex_type)
{
  std::shared_ptr <ModuleManager> moduleManager (new ModuleManager() );
  std::shared_ptr<kurento::Factory> mediaPipelineFactory;
  std::shared_ptr<kurento::MediaObject> mediaPipeline;

  gst_init(nullptr, nullptr);

  std::string moduleName = "../../src/server/libkmscoremodule.so";

  moduleManager->loadModule (moduleName);

  mediaPipelineFactory = moduleManager->getFactory ("MediaPipeline");

  mediaPipeline = mediaPipelineFactory->createObject (
                    boost::property_tree::ptree(), "", Json::Value() );

  try {
    kurento::JsonSerializer writer (true);
    kurento::JsonSerializer writer2 (true);
    kurento::JsonSerializer reader (false);

    kurento::Fraction complexType (15, 1);
    std::shared_ptr<kurento::Fraction > complexTypeReturned;

    complexType.Serialize (writer);

    reader.JsonValue = writer.JsonValue;
    Serialize (complexTypeReturned , reader);
    Serialize (complexTypeReturned , writer2);

    if (writer.JsonValue.toStyledString() != writer2.JsonValue.toStyledString() ) {
      BOOST_ERROR ("Serialization does not match");
    }

  } catch (kurento::KurentoException &e) {
    std::cerr << "Unexpected exception: " << e.what() << std::endl;
    BOOST_ERROR ("Unexpected error");
  }

  kurento::MediaSet::getMediaSet()->release (std::dynamic_pointer_cast
      <MediaObjectImpl> (mediaPipeline) );
}
