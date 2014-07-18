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
#include <MediaObjectImpl.hpp>
#include <KurentoException.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <MediaSet.hpp>
#include <gst/gst.h>
#include <config.h>

void
testHttpGetEndPoint (const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP(mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object = factories.at ("HttpGetEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

void
testHttpPostEndPoint (const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories, std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline)
{
  kurento::JsonSerializer w (true);

  w.SerializeNVP(mediaPipeline);

  std::shared_ptr <kurento::MediaObjectImpl >  object = factories.at ("HttpPostEndpoint")->createObject("", w.JsonValue);
  kurento::MediaSet::getMediaSet()->release(object);
}

std::shared_ptr <kurento::MediaObjectImpl>
createMediaPipeline (Glib::Module &coreModule)
{
  const kurento::FactoryRegistrar *registrar;
  void *registrarFactory;

  if (!coreModule.get_symbol ("getFactoryRegistrar", registrarFactory) ) {
    std::cerr << "symbol not found" << std::endl;
    exit(1);
  }

  registrar = ( (RegistrarFactoryFunc) registrarFactory) ();
  const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories = registrar->getFactories();
  return factories.at("MediaPipeline")->createObject("", Json::Value());
}

int
main (int argc, char **argv)
{
  std::shared_ptr <kurento::MediaObjectImpl> mediaPipeline;
  const kurento::FactoryRegistrar *registrar;
  void *registrarFactory;

  gst_init (&argc, &argv);

  std::string coreModuleName = KURENTO_MODULES_SO_DIR "/libkmscoremodule.so";

  Glib::Module coreModule (coreModuleName);

  mediaPipeline = createMediaPipeline(coreModule);

  std::string moduleName = "../../src/server/libkmselementsmodule.so";

  Glib::Module module (moduleName);

  if (!module) {
    std::cerr << "module cannot be loaded: " << Glib::Module::get_last_error() << std::endl;
    return 1;
  }

  if (!module.get_symbol ("getFactoryRegistrar", registrarFactory) ) {
    std::cerr << "symbol not found" << std::endl;
    return 1;
  }

  registrar = ( (RegistrarFactoryFunc) registrarFactory) ();
  const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories = registrar->getFactories();

  testHttpGetEndPoint(factories, mediaPipeline);
  testHttpPostEndPoint(factories, mediaPipeline);

  /*
  Factory: PlayerEndpoint
  Factory: RecorderEndpoint
  Factory: RtpEndpoint
  Factory: WebRtcEndpoint
  */
  kurento::MediaSet::getMediaSet()->release(mediaPipeline);

  kurento::MediaSet::destroyMediaSet();

  return 0;
}
