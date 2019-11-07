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

#ifndef __GSTREAMER_FILTER_IMPL_HPP__
#define __GSTREAMER_FILTER_IMPL_HPP__

#include "FilterImpl.hpp"
#include "GStreamerFilter.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class FilterType;
class GStreamerFilterImpl;

void Serialize (std::shared_ptr<GStreamerFilterImpl> &object,
                JsonSerializer &serializer);

class GStreamerFilterImpl : public FilterImpl, public virtual GStreamerFilter
{

public:

  GStreamerFilterImpl (const boost::property_tree::ptree &conf,
                       std::shared_ptr<MediaPipeline> mediaPipeline,
                       const std::string &command, std::shared_ptr<FilterType> filterType);

  virtual ~GStreamerFilterImpl () {};

  virtual std::string getCommand () override;

  virtual void setElementProperty (const std::string &propertyName,
      const std::string &propertyValue) override;

  /* Next methods are automatically implemented by code generator */
  using FilterImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

private:
  GstElement *gstElement = NULL;
  std::string cmd;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __GSTREAMER_FILTER_IMPL_HPP__ */
