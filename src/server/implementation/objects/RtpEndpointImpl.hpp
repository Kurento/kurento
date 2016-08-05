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
#ifndef __RTP_ENDPOINT_IMPL_HPP__
#define __RTP_ENDPOINT_IMPL_HPP__

#include "BaseRtpEndpointImpl.hpp"
#include "RtpEndpoint.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class RtpEndpointImpl;

void Serialize (std::shared_ptr<RtpEndpointImpl> &object,
                JsonSerializer &serializer);

class RtpEndpointImpl : public BaseRtpEndpointImpl, public virtual RtpEndpoint
{

public:

  RtpEndpointImpl (const boost::property_tree::ptree &conf,
                   std::shared_ptr<MediaPipeline> mediaPipeline,
                   std::shared_ptr<SDES> crypto, bool useIpv6);

  virtual ~RtpEndpointImpl ();

  sigc::signal<void, OnKeySoftLimit> signalOnKeySoftLimit;

  /* Next methods are automatically implemented by code generator */
  using BaseRtpEndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:
  virtual void postConstructor () override;

private:

  gulong handlerOnKeySoftLimit = 0;
  void onKeySoftLimit (gchar *media);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __RTP_ENDPOINT_IMPL_HPP__ */
