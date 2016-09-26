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

#include <glibmm/module.h>
#include <FactoryRegistrar.hpp>
#include <ModuleManager.hpp>
#include <MediaObjectImpl.hpp>
#include <KurentoException.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <MediaSet.hpp>
#include <gst/gst.h>
#include <config.h>
#include <FilterType.hpp>
#include <MediaElementImpl.hpp>

boost::property_tree::ptree config;

void
testFaceOverlay (kurento::ModuleManager &moduleManager,
                 std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP (mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object =
    moduleManager.getFactory ("FaceOverlayFilter")->createObject (config, "",
        w.JsonValue);
  kurento::MediaSet::getMediaSet()->release (object);
}

void
testGStreamerFilter (kurento::ModuleManager &moduleManager,
                     std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);
  std::string command ("capsfilter "
                       "caps=video/x-raw,pixel-aspect-ratio=(fraction)1/1,width=(int)640,framerate=(fraction)4/1");
  std::shared_ptr <kurento::FilterType> filter (new kurento::FilterType (
        kurento::FilterType::VIDEO) );

  w.SerializeNVP (mediaPipeline);
  w.SerializeNVP (command);
  w.SerializeNVP (filter);

  std::shared_ptr <kurento::MediaObjectImpl >  object =
    moduleManager.getFactory ("GStreamerFilter")->createObject (config, "",
        w.JsonValue);

  kurento::MediaSet::getMediaSet()->release (object);
}

void
testZBarFilter (kurento::ModuleManager &moduleManager,
                std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP (mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object =
    moduleManager.getFactory ("ZBarFilter")->createObject (config, "", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release (object);
}

int
main (int argc, char **argv)
{
  std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline;
  std::shared_ptr <kurento::Factory> factory;

  gst_init (&argc, &argv);

  kurento::ModuleManager moduleManager;

  moduleManager.loadModulesFromDirectories ("../../src/server:../../..");

  mediaPipeline = moduleManager.getFactory ("MediaPipeline")->createObject (
                    config, "",
                    Json::Value() );

  moduleManager.loadModule ("../../src/server/libkmsfiltersmodule.so");

  testFaceOverlay (moduleManager, mediaPipeline);
  testGStreamerFilter (moduleManager, mediaPipeline);
  testZBarFilter (moduleManager, mediaPipeline);

  kurento::MediaSet::getMediaSet()->release (mediaPipeline);

  return 0;
}
