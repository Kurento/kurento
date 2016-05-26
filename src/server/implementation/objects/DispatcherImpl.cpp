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
#include "HubPortImpl.hpp"
#include <DispatcherImplFactory.hpp>
#include "DispatcherImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_dispatcher_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoDispatcherImpl"

#define FACTORY_NAME "dispatcher"

namespace kurento
{

DispatcherImpl::DispatcherImpl (const boost::property_tree::ptree &conf,
                                std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (conf,
                                      std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
}

void DispatcherImpl::connect (std::shared_ptr<HubPort> source,
                              std::shared_ptr<HubPort> sink)
{
  std::shared_ptr<HubPortImpl> sourcePort =
    std::dynamic_pointer_cast<HubPortImpl> (source);
  std::shared_ptr<HubPortImpl> sinkPort = std::dynamic_pointer_cast<HubPortImpl>
                                          (sink);
  bool connected;

  g_signal_emit_by_name (G_OBJECT (element), "connect",
                         sourcePort->getHandlerId(),
                         sinkPort->getHandlerId(),
                         &connected);

  if (!connected) {
    throw KurentoException (CONNECT_ERROR, "Can not connect ports");
  }
}

MediaObjectImpl *
DispatcherImplFactory::createObject (const boost::property_tree::ptree &conf,
                                     std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new DispatcherImpl (conf, mediaPipeline);
}

DispatcherImpl::StaticConstructor DispatcherImpl::staticConstructor;

DispatcherImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
