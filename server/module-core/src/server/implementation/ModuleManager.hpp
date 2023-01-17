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

#ifndef __MODULE_MANAGER_H__
#define __MODULE_MANAGER_H__

#include <glibmm/module.h>
#include <unordered_set>
#include <map>
#include <memory>
#include <string>
#include <set>
#include <FactoryRegistrar.hpp>
#include <MediaObjectImpl.hpp>

namespace kurento
{

class ModuleData
{
public:
  ModuleData (const std::string &name, const std::string &version,
              const std::string &compilationTime,
              const char *descriptor,
              const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories) :
    name (name), version (version), generationTime (compilationTime),
    descriptor (descriptor), factories (factories)
  {
  }

  std::string getName() const
  {
    return name;
  }

  std::string getVersion () const
  {
    return version;
  }

  std::string getGenerationTime () const
  {
    return generationTime;
  }

  std::string getDescriptor () const
  {
    if (descriptor == NULL) {
      return "";
    } else {
      return descriptor;
    }
  }

  const std::map <std::string, std::shared_ptr <kurento::Factory > >
  &getFactories()
  {
    return factories;
  }


private:
  std::string name;
  std::string version;
  std::string generationTime;
  const char *descriptor;
  const std::map <std::string, std::shared_ptr <kurento::Factory > > &factories;
};

class ModuleManager
{
public:
  ModuleManager () {};
  ~ModuleManager () {};

  int loadModule (std::string modulePath);
  void loadModulesFromDirectories (std::string colonSepDirs);
  const std::map <std::string, std::shared_ptr <kurento::Factory > >
  getLoadedFactories ();
  std::shared_ptr<kurento::Factory> getFactory (std::string symbolName);

  const std::map <std::string, std::shared_ptr <ModuleData>> getModules () const
  {
    return loadedModules;
  }

private:

  std::map <std::string, std::shared_ptr <kurento::Factory > > loadedFactories;
  std::map <std::string, std::shared_ptr <ModuleData>> loadedModules;
  void loadModules (std::string paths);


  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;
};

} // kurento

#endif /* __MODULE_MANAGER_H__ */
