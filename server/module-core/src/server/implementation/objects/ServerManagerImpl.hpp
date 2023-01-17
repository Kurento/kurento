/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#ifndef __SERVER_MANAGER_IMPL_HPP__
#define __SERVER_MANAGER_IMPL_HPP__

#include "MediaObjectImpl.hpp"
#include "ServerManager.hpp"
#include <EventHandler.hpp>
#include <boost/property_tree/ptree.hpp>
#include <ModuleManager.hpp>

namespace kurento
{
class ServerManagerImpl;
} /* kurento */

namespace kurento
{
void Serialize (std::shared_ptr<kurento::ServerManagerImpl> &object,
                JsonSerializer &serializer);
} /* kurento */

namespace kurento
{
class ServerInfo;
class MediaPipelineImpl;
} /* kurento */

namespace kurento
{

class ServerManagerImpl : public MediaObjectImpl, public virtual ServerManager
{

public:

  ServerManagerImpl (const std::shared_ptr<ServerInfo> info,
                     const boost::property_tree::ptree &config,
                     ModuleManager &moduleManager);

  virtual ~ServerManagerImpl () {};

  std::string getKmd (const std::string &moduleName) override;

  virtual std::shared_ptr<ServerInfo> getInfo () override;

  virtual std::vector<std::shared_ptr<MediaPipeline>> getPipelines () override;

  virtual std::vector<std::string> getSessions () override;

  virtual std::string getId() override
  {
    return "manager_ServerManager";
  }

  virtual std::string getMetadata () override;

  virtual int getCpuCount () override;

  virtual float getUsedCpu (int interval) override;

  // Used memory, in KiB
  virtual int64_t getUsedMemory() override;

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  sigc::signal<void, ObjectCreated> signalObjectCreated;
  sigc::signal<void, ObjectDestroyed> signalObjectDestroyed;
  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

private:

  std::shared_ptr<ServerInfo> info;
  std::string metadata;

  ModuleManager &moduleManager;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;
};

} /* kurento */

#endif /*  __SERVER_MANAGER_IMPL_HPP__ */
