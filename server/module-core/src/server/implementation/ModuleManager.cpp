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
#include <config.h>

#include "ModuleManager.hpp"

#include <gst/gst.h>
#include <KurentoException.hpp>
#include <memory>
#include <sstream>
#include <boost/filesystem.hpp>

#define GST_CAT_DEFAULT kurento_media_set
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoModuleManager"

namespace kurento
{

typedef const char * (*GetVersionFunc) ();
typedef const char * (*GetNameFunc) ();
typedef const char * (*GetDescFunc) ();
typedef const char * (*GetGenerationTimeFunc) ();

int
ModuleManager::loadModule (std::string modulePath)
{
  const kurento::FactoryRegistrar *registrar;
  void *registrarFactory, *getVersion = nullptr, *getName = nullptr,
                          *getDescriptor = nullptr,
                          *getGenerationTime = nullptr;
  std::string moduleFileName;
  std::string moduleName;
  std::string moduleVersion;
  std::string generationTime;
  const char *moduleDescriptor = nullptr;

  boost::filesystem::path path (modulePath);

  moduleFileName = path.filename().string();

  if (loadedModules.find (moduleFileName) != loadedModules.end() ) {
    GST_DEBUG ("Module named %s already loaded", moduleFileName.c_str() );
    return -1;
  }

  try {
    Glib::Module module (modulePath);

    if (!module) {
      GST_WARNING ("Module %s cannot be loaded: %s", modulePath.c_str(),
                  Glib::Module::get_last_error().c_str() );
      return -1;
    }

    if (!module.get_symbol ("getFactoryRegistrar", registrarFactory) ) {
      GST_DEBUG ("Symbol 'getFactoryRegistrar' not found in library %s",
                  moduleFileName.c_str() );
      return -1;
    }

    registrar = ( (RegistrarFactoryFunc) registrarFactory) ();
    const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories =
      registrar->getFactories();

    for (auto it : factories) {
      if (loadedFactories.find (it.first) != loadedFactories.end() ) {
        GST_DEBUG ("Factory %s is already registered, skiping module %s",
                  it.first.c_str(), module.get_name().c_str() );
        return -1;
      }
    }

    module.make_resident();

    GST_INFO ("Load file: %s, module name: %s", modulePath.c_str(),
              module.get_name().c_str() );

    loadedFactories.insert (factories.begin(), factories.end() );

    if (!module.get_symbol ("getModuleVersion", getVersion) ) {
      GST_WARNING ("Cannot get module version");
    } else {
      moduleVersion = ( (GetNameFunc) getVersion) ();
    }

    if (!module.get_symbol ("getModuleName", getName) ) {
      GST_WARNING ("Cannot get module name");
    } else {
      std::string finalModuleName;

      moduleName = ( (GetVersionFunc) getName) ();

      // Factories are also registered using the module name as a prefix
      // Modules core, elements and filters use kurento as prefix
      if (moduleName == "core" || moduleName == "elements"
          || moduleName == "filters")  {
        finalModuleName = "kurento";
      } else {
        finalModuleName = moduleName;
      }

      for (auto it : factories) {
        loadedFactories [finalModuleName + "." + it.first] = it.second;
      }
    }

    if (!module.get_symbol ("getModuleDescriptor", getDescriptor) ) {
      GST_WARNING ("Cannot get module descriptor");
    } else {
      moduleDescriptor = ( (GetDescFunc) getDescriptor) ();
    }

    if (!module.get_symbol ("getGenerationTime", getGenerationTime) ) {
      GST_WARNING ("Cannot get module generationTime");
    } else {
      generationTime = ( (GetGenerationTimeFunc) getGenerationTime) ();
    }

    loadedModules[moduleFileName] = std::make_shared<ModuleData>(
        moduleName, moduleVersion, generationTime, moduleDescriptor, factories);

    GST_INFO ("Loaded module: %s, version: %s, date: %s", moduleName.c_str(),
              moduleVersion.c_str(), generationTime.c_str() );
  } catch (...) {
    GST_WARNING ("Module %s cannot be loaded due to exception: %s", modulePath.c_str(),
                 Glib::Module::get_last_error().c_str() );
    return -1;
  }

  return 0;
}

std::list<std::string> split (const std::string &s, char delim)
{
  std::list<std::string> elems;
  std::stringstream ss (s);
  std::string item;

  while (std::getline (ss, item, delim) ) {
    elems.push_back (item);
  }

  return elems;
}

void
ModuleManager::loadModules (std::string dir)
{
  boost::filesystem::path path (dir);

  if (!boost::filesystem::exists (path) ) {
    GST_INFO ("Skip non-existent path: %s", path.c_str() );
    return;
  }

  path = boost::filesystem::canonical(path);

  if (!boost::filesystem::is_directory (path) ) {
    GST_INFO ("Skip non-directory path: %s", path.c_str() );
    return;
  }

  GST_DEBUG ("Looking for modules in path: %s", path.c_str() );

  boost::filesystem::directory_iterator end_itr;

  for ( boost::filesystem::directory_iterator itr ( path ); itr != end_itr;
        ++itr ) {
    if (boost::filesystem::is_regular_file (*itr) ) {
      boost::filesystem::path extension = itr->path().extension();

      if (extension.string() == ".so") {
        GST_DEBUG ("Found file: %s", itr->path().string().c_str() );
        loadModule (itr->path().string() );
      }
    } else if (boost::filesystem::is_directory (*itr) ) {
      this->loadModules (itr->path().string() );
    }
  }
}

void
ModuleManager::loadModulesFromDirectories (std::string colonSepDirs)
{
  std::list <std::string> dirs;

  dirs = split (colonSepDirs, ':');

  for (std::string dir : dirs) {
    this->loadModules (dir);
  }

  //try to load modules from the default path
  this->loadModules (KURENTO_MODULES_DIR);

  return;
}

const std::map <std::string, std::shared_ptr <kurento::Factory > >
ModuleManager::getLoadedFactories ()
{
  return loadedFactories;
}

std::shared_ptr<kurento::Factory>
ModuleManager::getFactory (std::string factoryName)
{
  try {
    return loadedFactories.at (factoryName);
  } catch (std::exception &e) {
    GST_ERROR ("Factory %s not found: %s", factoryName.c_str(), e.what() );
    throw kurento::KurentoException (MEDIA_OBJECT_NOT_AVAILABLE, "Factory '" +
                                     factoryName + "' not found");
  }
}

ModuleManager::StaticConstructor ModuleManager::staticConstructor;

ModuleManager::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} // kurento
