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

#define DEFAULT_PLUMBER_ADDRESS "localhost"

#define FACTORY_NAME "plumberendpoint"

namespace kurento
{

PlumberEndpointImpl::PlumberEndpointImpl (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline)  : EndpointImpl (config,
          std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
  GstElement *element = getGstreamerElement();
  bool success;

  /* set properties */
  try {
    address = getConfigValue<std::string, PlumberEndpoint> ("bindAddress");
  } catch (boost::property_tree::ptree_error &e) {
    GST_DEBUG ("Setting default address %s to %" GST_PTR_FORMAT,
               DEFAULT_PLUMBER_ADDRESS, element);
    address = DEFAULT_PLUMBER_ADDRESS;
  }

  g_object_set (G_OBJECT (element), "local-address", address.c_str(),
                "local-port", 0, NULL);
  g_signal_emit_by_name (G_OBJECT (element), "accept", &success);

  if (!success) {
    throw KurentoException (CONNECT_ERROR, "PlumberEndpointImpl binding error");
  }

  try {
    address = getConfigValue <std::string, PlumberEndpoint> ("announcedAddress");
  } catch (boost::property_tree::ptree_error &e) {
    GST_DEBUG ("Announced address is not provided. Using binding address %s",
               address.c_str() );
  }

  /* Get bound port */
  g_object_get (G_OBJECT (element), "bound-port", &port, NULL);
}

std::string PlumberEndpointImpl::getAddress ()
{
  return address;
}

int PlumberEndpointImpl::getPort ()
{
  return port;
}

bool PlumberEndpointImpl::link (const std::string &address, int port)
{
  GstElement *element = getGstreamerElement();
  bool success;

  g_signal_emit_by_name (G_OBJECT (element), "connect", address.c_str(), port,
                         &success);

  return success;
}

MediaObjectImpl *
PlumberEndpointImplFactory::createObject (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new PlumberEndpointImpl (config, mediaPipeline);
}

PlumberEndpointImpl::StaticConstructor PlumberEndpointImpl::staticConstructor;

PlumberEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
