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

#ifndef __ALPHA_BLENDING_IMPL_HPP__
#define __ALPHA_BLENDING_IMPL_HPP__

#include "HubImpl.hpp"
#include "AlphaBlending.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class HubPort;
class AlphaBlendingImpl;

void Serialize (std::shared_ptr<AlphaBlendingImpl> &object,
                JsonSerializer &serializer);

class AlphaBlendingImpl : public HubImpl, public virtual AlphaBlending
{

public:

  AlphaBlendingImpl (const boost::property_tree::ptree &conf,
                     std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~AlphaBlendingImpl () {};

  void setMaster (std::shared_ptr<HubPort> source, int zOrder);
  void setPortProperties (float relativeX, float relativeY, int zOrder,
                          float relativeWidth, float relativeHeight, std::shared_ptr<HubPort> port);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __ALPHA_BLENDING_IMPL_HPP__ */
