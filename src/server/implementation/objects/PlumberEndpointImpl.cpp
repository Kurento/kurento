/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <PlumberEndpointImplFactory.hpp>
#include "PlumberEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#define GST_CAT_DEFAULT kurento_plumber_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPlumberEndpointImpl"

#define FACTORY_NAME "plumberendpoint"

namespace kurento
{

PlumberEndpointImpl::PlumberEndpointImpl (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::string &localAddres, int localPort, const std::string &remoteAddres,
    int remotePort)  : EndpointImpl (config,
                                       std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
  GstElement *element = getGstreamerElement();

  g_object_set (G_OBJECT (element), "local-address", localAddres.c_str(),
                "local-port", localPort, "remote-address", remoteAddres.c_str(),
                "remote-port", remotePort, NULL);
}

MediaObjectImpl *
PlumberEndpointImplFactory::createObject (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::string &localAddres, int localPort, const std::string &remoteAddres,
    int remotePort) const
{
  return new PlumberEndpointImpl (config, mediaPipeline, localAddres, localPort,
                                  remoteAddres, remotePort);
}

PlumberEndpointImpl::StaticConstructor PlumberEndpointImpl::staticConstructor;

PlumberEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
