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

#ifndef __VIDEO_SAMPLER_IMPL_HPP__
#define __VIDEO_SAMPLER_IMPL_HPP__

#include "MediaElementImpl.hpp"
#include "VideoSampler.hpp"
#include <EventHandler.hpp>
#include <ImageEncoding.hpp>
#include <ImageDelivery.hpp>
#include <SampleImageDelivered.hpp>


namespace kurento
{
namespace module
{
namespace videosampler
{
class VideoSamplerImpl;
} /* videosampler */
} /* module */
} /* kurento */

namespace kurento
{
void Serialize (std::shared_ptr<kurento::module::videosampler::VideoSamplerImpl>
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
namespace videosampler
{

class VideoSamplerImpl : public MediaElementImpl, public virtual VideoSampler
{

public:

  VideoSamplerImpl (const boost::property_tree::ptree &config,
                    std::shared_ptr<MediaPipeline> mediaPipeline,
                    int framePeriod,
                    std::shared_ptr<ImageDelivery> imageDeliveryMethod,
                    int height,
                    int width,
                    std::shared_ptr<ImageEncoding> imageEncoding,
                    std::string endpointUrl,
                    std::string metdata);

  sigc::signal<void, SampleImageDelivered> signalSampleImageDelivered;

  std::string getEncodingStr () { return encoding->getString(); }
  std::string getEndpointUrl () { return endpointUrl; }
  std::string getMetadata () { return metadata; }
  

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);
  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  void imageDelivered ();

  std::shared_ptr<ImageEncoding> encoding;
  std::shared_ptr<ImageDelivery> delivery;
  std::string endpointUrl;
  int width;
  int height;
  int msFramePeriod;
  std::string metadata;


private:

  gulong handlerImageDelivered = 0;


  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* videosampler */
} /* module */
} /* kurento */

#endif /*  __VIDEO_SAMPLER_IMPL_HPP__ */
