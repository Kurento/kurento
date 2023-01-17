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
#include "HubImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <MediaPipelineImpl.hpp>
#include <gst/gst.h>
#include <DotGraph.hpp>
#include <memory>

#define GST_CAT_DEFAULT kurento_hub_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHubImpl"

namespace kurento
{

std::string HubImpl::getGstreamerDot (
  std::shared_ptr<GstreamerDotDetails> details)
{
  return generateDotGraph (GST_BIN (element), details);
}

std::string HubImpl::getGstreamerDot()
{
  return generateDotGraph(
      GST_BIN(element),
      std::make_shared<GstreamerDotDetails>(GstreamerDotDetails::SHOW_VERBOSE));
}

HubImpl::HubImpl (const boost::property_tree::ptree &config,
                  std::shared_ptr<MediaObjectImpl> parent,
                  const std::string &factoryName) : MediaObjectImpl (config, parent)
{
  std::shared_ptr<MediaPipelineImpl> pipe;

  pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );

  element = gst_element_factory_make(factoryName.c_str(), nullptr);

  g_object_ref (element);
  pipe->addElement (element);
}

HubImpl::~HubImpl()
{
  std::shared_ptr<MediaPipelineImpl> pipe;

  pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );

  gst_bin_remove (GST_BIN ( pipe->getPipeline() ), element);
  gst_element_set_state (element, GST_STATE_NULL);
  g_object_unref (element);
}

HubImpl::StaticConstructor HubImpl::staticConstructor;

HubImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
