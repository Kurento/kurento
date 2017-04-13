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
#ifndef __PLAYER_ENDPOINT_IMPL_HPP__
#define __PLAYER_ENDPOINT_IMPL_HPP__

#include "UriEndpointImpl.hpp"
#include "PlayerEndpoint.hpp"
#include <EventHandler.hpp>
#include <functional>

namespace kurento
{

class MediaPipeline;
class PlayerEndpointImpl;

void Serialize (std::shared_ptr<PlayerEndpointImpl> &object,
                JsonSerializer &serializer);
class VideoInfo;

class PlayerEndpointImpl : public UriEndpointImpl, public virtual PlayerEndpoint
{

public:

  PlayerEndpointImpl (const boost::property_tree::ptree &conf,
                      std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri,
                      bool useEncodedMedia, int networkCache);

  virtual ~PlayerEndpointImpl ();

  void play () override;

  virtual std::shared_ptr<VideoInfo> getVideoInfo () override;

  virtual int64_t getPosition() override;
  virtual void setPosition (int64_t position) override;

  virtual std::string getElementGstreamerDot() override;

  /* Next methods are automatically implemented by code generator */
  using UriEndpointImpl::connect;
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

  gulong signalEOS = 0;
  gulong signalInvalidURI = 0;
  gulong signalInvalidMedia = 0;

  void eosHandler ();
  void invalidUri ();
  void invalidMedia ();

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __PLAYER_ENDPOINT_IMPL_HPP__ */
