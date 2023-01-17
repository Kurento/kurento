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

#ifndef __ZBAR_FILTER_IMPL_HPP__
#define __ZBAR_FILTER_IMPL_HPP__

#include "FilterImpl.hpp"
#include "ZBarFilter.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class ZBarFilterImpl;

void Serialize (std::shared_ptr<ZBarFilterImpl> &object,
                JsonSerializer &serializer);

class ZBarFilterImpl : public FilterImpl, public virtual ZBarFilter
{

public:

  ZBarFilterImpl (const boost::property_tree::ptree &conf,
                  std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~ZBarFilterImpl ();

  /* Next methods are automatically implemented by code generator */
  using FilterImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, CodeFound> signalCodeFound;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  virtual void postConstructor ();

private:
  GstElement *zbar{};
  gulong bus_handler_id;

  guint64 lastTs = G_GUINT64_CONSTANT (0);
  std::string lastType;
  std::string lastSymbol;

  void busMessage (GstMessage *message);

  void barcodeDetected (guint64 ts, std::string &type, std::string &symbol);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __ZBAR_FILTER_IMPL_HPP__ */
