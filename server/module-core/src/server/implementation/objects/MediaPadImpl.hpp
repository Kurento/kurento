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
#ifndef __MEDIA_PAD_IMPL_HPP__
#define __MEDIA_PAD_IMPL_HPP__

#include "MediaObjectImpl.hpp"
#include "MediaPad.hpp"
#include <EventHandler.hpp>
#include <gst/gst.h>

namespace kurento
{

class MediaElementImpl;
class MediaType;
class MediaPadImpl;

void Serialize (std::shared_ptr<MediaPadImpl> &object,
                JsonSerializer &serializer);

class MediaPadImpl : public MediaObjectImpl, public virtual MediaPad
{

public:

  MediaPadImpl (const boost::property_tree::ptree &config,
                std::shared_ptr<MediaObjectImpl> parent,
                std::shared_ptr<MediaType> mediaType,
                const std::string &mediaDescription);

  virtual ~MediaPadImpl () {};

  virtual std::shared_ptr<MediaElement> getMediaElement ()
  {
    return mediaElement;
  }

  virtual std::shared_ptr<MediaType> getMediaType ()
  {
    return mediaType;
  }

  virtual std::string getMediaDescription ()
  {
    return mediaDescription;
  }

  GstElement *getGstreamerElement ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  std::shared_ptr<MediaElement> mediaElement;
  std::shared_ptr<MediaType> mediaType;
  std::string mediaDescription;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __MEDIA_PAD_IMPL_HPP__ */
