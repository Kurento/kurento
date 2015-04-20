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

void Serialize (std::shared_ptr<HttpPostEndpointImpl> &object,
                JsonSerializer &serializer);

class HttpPostEndpointImpl : public HttpEndpointImpl,
  public virtual HttpPostEndpoint
{

public:

  HttpPostEndpointImpl (const boost::property_tree::ptree &conf,
                        std::shared_ptr<MediaPipeline> mediaPipeline,
                        int disconnectionTimeout, bool useEncodedMedia);

  virtual ~HttpPostEndpointImpl ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, EndOfStream> signalEndOfStream;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  virtual void postConstructor ();

private:
  void eosLambda ();

  int handlerEos = 0;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __HTTP_POST_ENDPOINT_IMPL_HPP__ */
