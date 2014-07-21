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

void
testHttpGetEndPoint (kurento::ModuleManager &moduleManager, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP(mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object = moduleManager.getFactory ("HttpGetEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

void
testHttpPostEndPoint (kurento::ModuleManager &moduleManager, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP(mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object = moduleManager.getFactory("HttpPostEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

void
testPlayerEndPoint (kurento::ModuleManager &moduleManager, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);
  std::string uri ("http://kurento.org");

  w.SerializeNVP(mediaPipeline);
  w.SerializeNVP(uri);

  std::shared_ptr <kurento::MediaObjectImpl >  object = moduleManager.getFactory("PlayerEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

void
testRecorderEndPoint (kurento::ModuleManager &moduleManager, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);
  std::string uri ("http://kurento.org");

  w.SerializeNVP(mediaPipeline);
  w.SerializeNVP(uri);

  std::shared_ptr <kurento::MediaObjectImpl >  object = moduleManager.getFactory("RecorderEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

void
testRtpEndpoint (kurento::ModuleManager &moduleManager, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP(mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object = moduleManager.getFactory("RtpEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

int
main (int argc, char **argv)
{
  std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline;
  std::shared_ptr <kurento::Factory> factory;

  gst_init (&argc, &argv);

  kurento::ModuleManager moduleManager;

  std::string coreModuleName = KURENTO_MODULES_SO_DIR "/libkmscoremodule.so";

  moduleManager.loadModule (coreModuleName);
  mediaPipeline = moduleManager.getFactory ("MediaPipeline")->createObject("", Json::Value());

  moduleManager.loadModule ("../../src/server/libkmselementsmodule.so");

  testHttpGetEndPoint(moduleManager, mediaPipeline);
  testHttpPostEndPoint(moduleManager, mediaPipeline);
  testPlayerEndPoint(moduleManager, mediaPipeline);
  testRecorderEndPoint(moduleManager, mediaPipeline);
  testRtpEndpoint(moduleManager, mediaPipeline);

  /*
  Factory: WebRtcEndpoint
  */
  kurento::MediaSet::getMediaSet()->release(mediaPipeline);

  kurento::MediaSet::destroyMediaSet();

  return 0;
}
