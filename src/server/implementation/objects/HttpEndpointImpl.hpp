#ifndef __HTTP_ENDPOINT_IMPL_HPP__
#define __HTTP_ENDPOINT_IMPL_HPP__

#include "SessionEndpointImpl.hpp"
#include "HttpEndpoint.hpp"
#include <EventHandler.hpp>
#include "HttpServer/HttpEndPointServer.hpp"

namespace kurento
{

class HttpEndpointImpl;

void Serialize (std::shared_ptr<HttpEndpointImpl> &object,
                JsonSerializer &serializer);

class HttpEndpointImpl : public SessionEndpointImpl, public virtual HttpEndpoint
{

public:

  HttpEndpointImpl (const boost::property_tree::ptree &conf,
                    std::shared_ptr< MediaObjectImpl > parent,
                    int disconnectionTimeout, const std::string &factoryName);

  virtual ~HttpEndpointImpl ();

  virtual std::string getUrl ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  void unregister_end_point ();
  void register_end_point ();
  bool is_registered();

private:
  std::shared_ptr<HttpEndPointServer> server;

  std::string url;
  bool urlSet = false;
  guint disconnectionTimeout;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

  gulong actionRequestedHandlerId;
  gulong urlRemovedHandlerId;
  gulong urlExpiredHandlerId;
  gint sessionStarted = 0;

  std::function<void (const gchar *uri, KmsHttpEndPointAction action) >
  actionRequestedLambda;
  std::function<void (const gchar *uri) > sessionTerminatedLambda;

};

} /* kurento */

#endif /*  __HTTP_ENDPOINT_IMPL_HPP__ */
