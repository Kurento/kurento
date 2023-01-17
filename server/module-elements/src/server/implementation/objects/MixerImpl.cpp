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
#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaType.hpp"
#include "HubPortImpl.hpp"
#include <MixerImplFactory.hpp>
#include "MixerImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_mixer_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMixerImpl"

#define FACTORY_NAME "selectablemixer"

namespace kurento
{

MixerImpl::MixerImpl (const boost::property_tree::ptree &conf,
                      std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (conf,
                            std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
}

void MixerImpl::connect (std::shared_ptr<MediaType> media,
                         std::shared_ptr<HubPort> source, std::shared_ptr<HubPort> sink)
{
  std::shared_ptr<HubPortImpl> sourcePort =
    std::dynamic_pointer_cast<HubPortImpl> (source);
  std::shared_ptr<HubPortImpl> sinkPort =
    std::dynamic_pointer_cast<HubPortImpl> (sink);
  std::string  action;
  bool connected;

  switch (media->getValue() ) {
  case MediaType::AUDIO:
    action = "connect-audio";
    break;

  case MediaType::VIDEO:
    action = "connect-video";
    break;

  default:
    throw KurentoException (UNSUPPORTED_MEDIA_TYPE, "Invalid media type");
  };

  g_signal_emit_by_name (G_OBJECT (element), action.c_str(),
                         sourcePort->getHandlerId(),
                         sinkPort->getHandlerId(),
                         &connected);

  if (!connected) {
    throw KurentoException (CONNECT_ERROR, "Can not connect video ports");
  }
}

void MixerImpl::disconnect (std::shared_ptr<MediaType> media,
                            std::shared_ptr<HubPort> source, std::shared_ptr<HubPort> sink)
{
  std::shared_ptr<HubPortImpl> sourcePort =
    std::dynamic_pointer_cast<HubPortImpl> (source);
  std::shared_ptr<HubPortImpl> sinkPort =
    std::dynamic_pointer_cast<HubPortImpl> (sink);
  std::string  action;
  bool connected;

  switch (media->getValue() ) {
  case MediaType::AUDIO:
    action = "disconnect-audio";
    break;

  case MediaType::VIDEO:
    throw KurentoException (UNSUPPORTED_MEDIA_TYPE,
                            "Video disconnection is not implemented yet");

  default:
    /* Only audio is suppported so far */
    throw KurentoException (UNSUPPORTED_MEDIA_TYPE, "Invalid media type");
  };

  g_signal_emit_by_name (G_OBJECT (element), action.c_str(),
                         sourcePort->getHandlerId(),
                         sinkPort->getHandlerId(),
                         &connected);

  if (!connected) {
    throw KurentoException (CONNECT_ERROR, "Can not connect video ports");
  }
}

MediaObjectImpl *
MixerImplFactory::createObject (const boost::property_tree::ptree &conf,
                                std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new MixerImpl (conf, mediaPipeline);
}

MixerImpl::StaticConstructor MixerImpl::staticConstructor;

MixerImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
