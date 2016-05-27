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

#ifndef __PLATE_DETECTOR_FILTER_IMPL_HPP__
#define __PLATE_DETECTOR_FILTER_IMPL_HPP__

#include "FilterImpl.hpp"
#include "PlateDetectorFilter.hpp"
#include <EventHandler.hpp>
#include <boost/property_tree/ptree.hpp>

namespace kurento
{
namespace module
{
namespace platedetector
{
class PlateDetectorFilterImpl;
} /* platedetector */
} /* module */
} /* kurento */

namespace kurento
{
void Serialize (
  std::shared_ptr<kurento::module::platedetector::PlateDetectorFilterImpl>
  &object, JsonSerializer &serializer);
} /* kurento */

namespace kurento
{
class MediaPipelineImpl;
} /* kurento */

namespace kurento
{
namespace module
{
namespace platedetector
{

class PlateDetectorFilterImpl : public FilterImpl,
  public virtual PlateDetectorFilter
{

public:

  PlateDetectorFilterImpl (const boost::property_tree::ptree &config,
                           std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~PlateDetectorFilterImpl ();

  void setPlateWidthPercentage (float plateWidthPercentage);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, PlateDetected> signalPlateDetected;
  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  virtual void postConstructor ();

private:

  gulong bus_handler_id;
  GstElement *plateDetector = NULL;
  void busMessage (GstMessage *message);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* platedetector */
} /* module */
} /* kurento */

#endif /*  __PLATE_DETECTOR_FILTER_IMPL_HPP__ */
