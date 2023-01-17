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
#ifndef __URI_ENDPOINT_IMPL_HPP__
#define __URI_ENDPOINT_IMPL_HPP__

#include "EndpointImpl.hpp"
#include "UriEndpoint.hpp"
#include <UriEndpointState.hpp>
#include <UriEndpointStateChanged.hpp>
#include <EventHandler.hpp>

namespace kurento
{

class UriEndpointImpl;

void Serialize (std::shared_ptr<UriEndpointImpl> &object,
                JsonSerializer &serializer);

class UriEndpointImpl : public EndpointImpl, public virtual UriEndpoint
{

public:

  UriEndpointImpl (const boost::property_tree::ptree &config,
                   std::shared_ptr< MediaObjectImpl > parent,
                   const std::string &factoryName, const std::string &uri);

  virtual ~UriEndpointImpl ();

  void pause () override;
  void stop () override;

  virtual std::string getUri () override;
  virtual std::shared_ptr<UriEndpointState> getState () override;

  sigc::signal<void, UriEndpointStateChanged> signalUriEndpointStateChanged;

  /* Next methods are automatically implemented by code generator */
  using EndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:

  void start ();
  virtual void postConstructor () override;

private:

  std::string uri;
  std::string absolute_uri;
  gulong stateChangedHandlerId{};
  std::shared_ptr<UriEndpointState> state;

  void checkUri ();
  void stateChanged (guint new_state);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __URI_ENDPOINT_IMPL_HPP__ */
