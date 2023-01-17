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
#include "MediaPipelineImpl.hpp"
#include "HubPort.hpp"
#include "HubPortImpl.hpp"
#include <AlphaBlendingImplFactory.hpp>
#include "AlphaBlendingImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_alpha_blending_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoAlphaBlendingImpl"

#define FACTORY_NAME "alphablending"
#define MASTER_PORT "set-master"
#define SET_PORT_PROPERTIES "set-port-properties"

namespace kurento
{

AlphaBlendingImpl::AlphaBlendingImpl (const boost::property_tree::ptree &conf,
                                      std::shared_ptr<MediaPipeline>
                                      mediaPipeline) : HubImpl (conf, std::dynamic_pointer_cast<MediaPipelineImpl>
                                            (mediaPipeline), FACTORY_NAME)
{
}

void AlphaBlendingImpl::setMaster (std::shared_ptr<HubPort> source, int zOrder)
{
  GstStructure *data;
  std::shared_ptr<HubPortImpl> mixerPort =
    std::dynamic_pointer_cast<HubPortImpl> (source);

  GST_DEBUG ("set master");
  data = gst_structure_new ("data",
                            "port", G_TYPE_INT, mixerPort->getHandlerId(),
                            "z_order", G_TYPE_INT, zOrder,
                            NULL);

  g_object_set (G_OBJECT (element), MASTER_PORT, data, NULL);
}

void AlphaBlendingImpl::setPortProperties (float relativeX, float relativeY,
    int zOrder, float relativeWidth, float relativeHeight,
    std::shared_ptr<HubPort> port)
{
  GstStructure *data;
  std::shared_ptr<HubPortImpl> mixerPort =
    std::dynamic_pointer_cast<HubPortImpl> (port);

  GST_DEBUG ("set properties");
  data = gst_structure_new ("data",
                            "port", G_TYPE_INT, mixerPort->getHandlerId(),
                            "relative_x", G_TYPE_FLOAT, relativeX,
                            "relative_y", G_TYPE_FLOAT, relativeY,
                            "relative_width", G_TYPE_FLOAT, relativeWidth,
                            "relative_height", G_TYPE_FLOAT, relativeHeight,
                            "z_order", G_TYPE_INT, zOrder,
                            NULL);

  g_signal_emit_by_name (element, SET_PORT_PROPERTIES, data);
  gst_structure_free (data);
}

MediaObjectImpl *
AlphaBlendingImplFactory::createObject (const boost::property_tree::ptree &conf,
                                        std::shared_ptr<MediaPipeline>
                                        mediaPipeline) const
{
  return new AlphaBlendingImpl (conf, mediaPipeline);
}

AlphaBlendingImpl::StaticConstructor AlphaBlendingImpl::staticConstructor;

AlphaBlendingImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
