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
#include "MediaElementImpl.hpp"
#include "MediaType.hpp"
#include "MediaPadImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_media_pad_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMediaPadImpl"

namespace kurento
{

MediaPadImpl::MediaPadImpl (const boost::property_tree::ptree &config,
                            std::shared_ptr<MediaObjectImpl> parent,
                            std::shared_ptr<MediaType> mediaType,
                            const std::string &mediaDescription) : MediaObjectImpl (config, parent)
{
  this->mediaElement = std::dynamic_pointer_cast<MediaElement> (parent);
  this->mediaType = mediaType;
  this->mediaDescription = mediaDescription;
}

GstElement *
MediaPadImpl::getGstreamerElement ()
{
  std::shared_ptr<MediaElementImpl> element = std::dynamic_pointer_cast
      <MediaElementImpl> (getParent() );

  return element->getGstreamerElement();
}

MediaPadImpl::StaticConstructor MediaPadImpl::staticConstructor;

MediaPadImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
