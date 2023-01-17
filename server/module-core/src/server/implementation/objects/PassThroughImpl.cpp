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
#include "MediaPipelineImpl.hpp"
#include <PassThroughImplFactory.hpp>
#include "PassThroughImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#define GST_CAT_DEFAULT kurento_pass_through_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPassThroughImpl"

#define FACTORY_NAME "passthrough"

namespace kurento
{

PassThroughImpl::PassThroughImpl (const boost::property_tree::ptree &config,
                                  std::shared_ptr<MediaPipeline> mediaPipeline)  : MediaElementImpl (config,
                                        std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
}

MediaObjectImpl *
PassThroughImplFactory::createObject (const boost::property_tree::ptree &config,
                                      std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new PassThroughImpl (config, mediaPipeline);
}

PassThroughImpl::StaticConstructor PassThroughImpl::staticConstructor;

PassThroughImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
