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
#ifndef __HTTP_ENDPOINT_IMPL_HPP__
#define __HTTP_ENDPOINT_IMPL_HPP__

#include "SessionEndpointImpl.hpp"
#include "HttpEndpoint.hpp"
#include <EventHandler.hpp>
#include "HttpServer/HttpEndPointServer.hpp"

namespace kurento
{

class HttpEndpointImpl;

void Serialize (std::shared_ptr<HttpEndpointImpl> &object,
                JsonSerializer &serializer);

class HttpEndpointImpl : public SessionEndpointImpl, public virtual HttpEndpoint
{

public:

  HttpEndpointImpl (const boost::property_tree::ptree &conf,
                    std::shared_ptr< MediaObjectImpl > parent,
                    int disconnectionTimeout, const std::string &factoryName);

  virtual ~HttpEndpointImpl ();

  virtual std::string getUrl () override;

  /* Next methods are automatically implemented by code generator */
  using SessionEndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:
  void unregister_end_point ();
  void register_end_point ();
  bool is_registered();

private:
  std::shared_ptr<HttpEndPointServer> server;

  std::string url;
  bool urlSet = false;
  guint disconnectionTimeout;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

  gulong actionRequestedHandlerId;
  gulong urlRemovedHandlerId;
  gulong urlExpiredHandlerId;
  gint sessionStarted = 0;

  std::function<void (const gchar *uri, KmsHttpEndPointAction action) >
  actionRequestedLambda;
  std::function<void (const gchar *uri) > sessionTerminatedLambda;

};

} /* kurento */

#endif /*  __HTTP_ENDPOINT_IMPL_HPP__ */
