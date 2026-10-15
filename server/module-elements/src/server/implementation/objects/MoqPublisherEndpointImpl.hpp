/*
 * (C) Copyright 2024 Kurento (http://kurento.org/)
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
#ifndef __MOQ_PUBLISHER_ENDPOINT_IMPL_HPP__
#define __MOQ_PUBLISHER_ENDPOINT_IMPL_HPP__

#include "EndpointImpl.hpp"
#include "MoqPublisherEndpoint.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class MoqPublisherEndpointImpl;

void Serialize (std::shared_ptr<MoqPublisherEndpointImpl> &object,
                JsonSerializer &serializer);

class MoqPublisherEndpointImpl : public EndpointImpl,
  public virtual MoqPublisherEndpoint
{

public:

  MoqPublisherEndpointImpl (const boost::property_tree::ptree &conf,
                             std::shared_ptr<MediaPipeline> mediaPipeline,
                             const std::string &url,
                             const std::string &broadcast);

  virtual ~MoqPublisherEndpointImpl ();

  void publish () override;
  void unpublish () override;

  virtual std::string getUrl () override;
  virtual std::string getBroadcast () override;

  /* Next methods are automatically implemented by code generator */
  using EndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

private:
  std::string url;
  std::string broadcast;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;
};

} /* kurento */

#endif /*  __MOQ_PUBLISHER_ENDPOINT_IMPL_HPP__ */
