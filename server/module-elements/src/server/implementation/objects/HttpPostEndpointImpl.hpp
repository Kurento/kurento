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
#ifndef __HTTP_POST_ENDPOINT_IMPL_HPP__
#define __HTTP_POST_ENDPOINT_IMPL_HPP__

#include "HttpEndpointImpl.hpp"
#include "HttpPostEndpoint.hpp"
#include <EventHandler.hpp>
#include <functional>

namespace kurento
{

class MediaPipeline;
class HttpPostEndpointImpl;

void Serialize (std::shared_ptr<HttpPostEndpointImpl> &object,
                JsonSerializer &serializer);

class HttpPostEndpointImpl : public HttpEndpointImpl,
  public virtual HttpPostEndpoint
{

public:

  HttpPostEndpointImpl (const boost::property_tree::ptree &conf,
                        std::shared_ptr<MediaPipeline> mediaPipeline,
                        int disconnectionTimeout, bool useEncodedMedia);

  virtual ~HttpPostEndpointImpl ();

  /* Next methods are automatically implemented by code generator */
  using HttpEndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  sigc::signal<void, EndOfStream> signalEndOfStream;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:
  virtual void postConstructor () override;

private:
  void eosLambda ();

  int handlerEos = 0;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __HTTP_POST_ENDPOINT_IMPL_HPP__ */
