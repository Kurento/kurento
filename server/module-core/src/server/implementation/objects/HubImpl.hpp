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
#ifndef __HUB_IMPL_HPP__
#define __HUB_IMPL_HPP__

#include "MediaObjectImpl.hpp"
#include "Hub.hpp"
#include <EventHandler.hpp>
#include <gst/gst.h>

namespace kurento
{

class HubImpl;

void Serialize (std::shared_ptr<HubImpl> &object, JsonSerializer &serializer);

class HubImpl : public MediaObjectImpl, public virtual Hub
{

public:

  HubImpl (const boost::property_tree::ptree &config,
           std::shared_ptr<MediaObjectImpl> parent,
           const std::string &factoryName);

  virtual ~HubImpl ();

  GstElement *getGstreamerElement()
  {
    return element;
  }

  virtual std::string getGstreamerDot ();
  virtual std::string getGstreamerDot (std::shared_ptr<GstreamerDotDetails>
                                       details);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  GstElement *element;

private:

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __HUB_IMPL_HPP__ */
