/*
 * Copyright 2022 Kurento
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __GSTREAMER_EXAMPLE_IMPL_HPP__
#define __GSTREAMER_EXAMPLE_IMPL_HPP__

#include "FilterImpl.hpp"
#include "GStreamerExample.hpp"
#include <EventHandler.hpp>
#include <boost/property_tree/ptree.hpp>

namespace kurento
{
namespace module
{
namespace gstreamerexample
{
class GStreamerExampleImpl;
} // namespace gstreamerexample
} // namespace module
} // namespace kurento

namespace kurento
{
void Serialize (
    std::shared_ptr<kurento::module::gstreamerexample::GStreamerExampleImpl>
        &object,
    JsonSerializer &serializer);
} // namespace kurento

namespace kurento
{
class MediaPipelineImpl;
} // namespace kurento

namespace kurento
{
namespace module
{
namespace gstreamerexample
{

class GStreamerExampleImpl : public FilterImpl, public virtual GStreamerExample
{

public:
  GStreamerExampleImpl (const boost::property_tree::ptree &config,
      std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~GStreamerExampleImpl () = default;

  void setFilterType (int filterType);
  void setEdgeThreshold (int edgeValue);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
      std::shared_ptr<EventHandler> handler);
  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
      const std::string &methodName,
      const Json::Value &params,
      Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:
  class StaticConstructor
  {
  public:
    StaticConstructor ();
  };

  static StaticConstructor staticConstructor;
};

} // namespace gstreamerexample
} // namespace module
} // namespace kurento

#endif /*  __GSTREAMER_EXAMPLE_IMPL_HPP__ */
