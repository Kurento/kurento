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

#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <OpenCVPluginSampleImplFactory.hpp>
#include "OpenCVPluginSampleImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include "MediaPipelineImpl.hpp"

#define GST_CAT_DEFAULT kurento_opencv_plugin_sample_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoOpenCVPluginSampleImpl"

namespace kurento
{
namespace module
{
namespace opencvpluginsample
{

OpenCVPluginSampleImpl::OpenCVPluginSampleImpl (const boost::property_tree::ptree &config, std::shared_ptr<MediaPipeline> mediaPipeline) : OpenCVFilterImpl (config, std::dynamic_pointer_cast<MediaPipelineImpl> (mediaPipeline))

{
}

void OpenCVPluginSampleImpl::setFilterType (int filterType)
{
  OpenCVPluginSampleOpenCVImpl::setFilterType (filterType);
}

void OpenCVPluginSampleImpl::setEdgeThreshold (int edgeValue)
{
  OpenCVPluginSampleOpenCVImpl::setEdgeThreshold (edgeValue);
}

MediaObjectImpl *
OpenCVPluginSampleImplFactory::createObject (const boost::property_tree::ptree &config, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new OpenCVPluginSampleImpl (config, mediaPipeline);
}

OpenCVPluginSampleImpl::StaticConstructor OpenCVPluginSampleImpl::staticConstructor;

OpenCVPluginSampleImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* opencvpluginsample */
} /* module */
} /* kurento */
