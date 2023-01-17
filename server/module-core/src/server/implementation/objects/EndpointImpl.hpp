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
#ifndef __ENDPOINT_IMPL_HPP__
#define __ENDPOINT_IMPL_HPP__

#include "MediaElementImpl.hpp"
#include "Endpoint.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class EndpointImpl;

void Serialize (std::shared_ptr<EndpointImpl> &object,
                JsonSerializer &serializer);

class EndpointImpl : public MediaElementImpl, public virtual Endpoint
{

public:

  EndpointImpl (const boost::property_tree::ptree &config,
                std::shared_ptr< MediaObjectImpl > parent,
                const std::string &factoryName);

  virtual ~EndpointImpl () {};

  /* Next methods are automatically implemented by code generator */
  using MediaElementImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

private:

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __ENDPOINT_IMPL_HPP__ */
