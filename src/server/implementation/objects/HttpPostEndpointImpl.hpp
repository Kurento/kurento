#ifndef __HTTP_POST_ENDPOINT_IMPL_HPP__
#define __HTTP_POST_ENDPOINT_IMPL_HPP__

#include "HttpEndpointImpl.hpp"
#include "HttpPostEndpoint.hpp"
#include <EventHandler.hpp>
#include <functional>

namespace kurento
{

class MediaPipeline;
class HttpPostEndpointImpl;

void Serialize (std::shared_ptr<HttpPostEndpointImpl> &object, JsonSerializer &serializer);

class HttpPostEndpointImpl : public HttpEndpointImpl, public virtual HttpPostEndpoint
{

public:

  HttpPostEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, int disconnectionTimeout, bool useEncodedMedia);

  virtual ~HttpPostEndpointImpl () {};

  virtual void setHttpServerConfig(MediaServerConfig& config);
  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType, std::shared_ptr<EventHandler> handler);

  sigc::signal<void, EndOfStream> signalEndOfStream;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:
  std::function<void() > eosLambda;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __HTTP_POST_ENDPOINT_IMPL_HPP__ */
