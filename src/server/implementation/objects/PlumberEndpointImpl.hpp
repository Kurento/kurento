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

#ifndef __PLUMBER_ENDPOINT_IMPL_HPP__
#define __PLUMBER_ENDPOINT_IMPL_HPP__

#include "EndpointImpl.hpp"
#include "PlumberEndpoint.hpp"
#include <EventHandler.hpp>
#include <boost/property_tree/ptree.hpp>

namespace kurento
{
class PlumberEndpointImpl;
} /* kurento */

namespace kurento
{
void Serialize (std::shared_ptr<kurento::PlumberEndpointImpl> &object,
                JsonSerializer &serializer);
} /* kurento */

namespace kurento
{
class MediaPipelineImpl;
} /* kurento */

namespace kurento
{

class PlumberEndpointImpl : public EndpointImpl, public virtual PlumberEndpoint
{

public:

  PlumberEndpointImpl (const boost::property_tree::ptree &config,
                       std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~PlumberEndpointImpl () {};

  bool connect (const std::string &address, int port);

  virtual std::string getAddress ();

  virtual int getPort ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);
  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  std::string address;
  uint port;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __PLUMBER_ENDPOINT_IMPL_HPP__ */
