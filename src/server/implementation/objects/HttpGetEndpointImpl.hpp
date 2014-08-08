#ifndef __HTTP_GET_ENDPOINT_IMPL_HPP__
#define __HTTP_GET_ENDPOINT_IMPL_HPP__

#include "HttpEndpointImpl.hpp"
#include "HttpGetEndpoint.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class MediaProfileSpecType;
class HttpGetEndpointImpl;

void Serialize (std::shared_ptr<HttpGetEndpointImpl> &object,
                JsonSerializer &serializer);

class HttpGetEndpointImpl : public HttpEndpointImpl,
  public virtual HttpGetEndpoint
{

public:

  HttpGetEndpointImpl (const boost::property_tree::ptree &conf,
                       std::shared_ptr<MediaPipeline> mediaPipeline,
                       bool terminateOnEOS, std::shared_ptr<MediaProfileSpecType> mediaProfile,
                       int disconnectionTimeout);

  virtual ~HttpGetEndpointImpl () {};

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

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

#endif /*  __HTTP_GET_ENDPOINT_IMPL_HPP__ */
