#ifndef __HTTP_ENDPOINT_IMPL_HPP__
#define __HTTP_ENDPOINT_IMPL_HPP__

#include "SessionEndpointImpl.hpp"
#include "HttpEndpoint.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class HttpEndpointImpl;

void Serialize (std::shared_ptr<HttpEndpointImpl> &object, JsonSerializer &serializer);

class HttpEndpointImpl : public SessionEndpointImpl, public virtual HttpEndpoint
{

public:

  HttpEndpointImpl (std::shared_ptr< MediaObjectImpl > parent, int disconnectionTimeout);

  virtual ~HttpEndpointImpl () {};

  std::string getUrl ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType, std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __HTTP_ENDPOINT_IMPL_HPP__ */
