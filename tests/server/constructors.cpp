/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
  std::string command ("videobox");
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

  moduleManager.loadModulesFromDirectories ("../../src/server");

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
