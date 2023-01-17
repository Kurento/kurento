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
#ifndef __SDP_ENDPOINT_IMPL_HPP__
#define __SDP_ENDPOINT_IMPL_HPP__

#include "SessionEndpointImpl.hpp"
#include "SdpEndpoint.hpp"
#include <OfferOptions.hpp>
#include <EventHandler.hpp>
#include <atomic>

namespace kurento
{

class SdpEndpointImpl;

void Serialize (std::shared_ptr<SdpEndpointImpl> &object,
                JsonSerializer &serializer);

class SdpEndpointImpl : public SessionEndpointImpl, public virtual SdpEndpoint
{

public:

  SdpEndpointImpl (const boost::property_tree::ptree &config,
                   std::shared_ptr< MediaObjectImpl > parent,
                   const std::string &factoryName, bool useIpv6 = false);

  virtual ~SdpEndpointImpl () {};

  int getMaxVideoRecvBandwidth () override;
  void setMaxVideoRecvBandwidth (int maxVideoRecvBandwidth) override;
  int getMaxAudioRecvBandwidth () override;
  void setMaxAudioRecvBandwidth (int maxAudioRecvBandwidth) override;

  std::string generateOffer () override;
  std::string generateOffer (std::shared_ptr<OfferOptions> options) override;
  std::string processOffer (const std::string &offer) override;
  std::string processAnswer (const std::string &answer) override;
  std::string getLocalSessionDescriptor () override;
  std::string getRemoteSessionDescriptor () override;

  /* Next methods are automatically implemented by code generator */
  using SessionEndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:

  virtual void postConstructor () override;
  std::string sessId;

private:

  static std::mutex sdpMutex;
  std::atomic_bool offerInProcess{};
  std::atomic_bool waitingAnswer{};
  std::atomic_bool answerProcessed{};

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __SDP_ENDPOINT_IMPL_HPP__ */
