#ifndef __HTTP_ENDPOINT_IMPL_HPP__
#define __HTTP_ENDPOINT_IMPL_HPP__

#include "SessionEndpointImpl.hpp"
#include "HttpEndpoint.hpp"
#include <EventHandler.hpp>
#include "HttpServer/KmsHttpEPServer.h"
#include "MediaServerConfig.hpp"

namespace kurento
{

class HttpEndpointImpl;

void Serialize (std::shared_ptr<HttpEndpointImpl> &object,
                JsonSerializer &serializer);

class HttpEndpointImpl : public SessionEndpointImpl, public virtual HttpEndpoint
{

public:

  HttpEndpointImpl (std::shared_ptr< MediaObjectImpl > parent,
                    int disconnectionTimeout);

  virtual ~HttpEndpointImpl ();

  virtual std::string getUrl ();

  virtual void setConfig (const MediaServerConfig &config);
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
