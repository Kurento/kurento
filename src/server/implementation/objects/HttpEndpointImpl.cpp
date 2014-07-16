#include <gst/gst.h>
#include "HttpEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_http_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpEndpointImpl"

#define FACTORY_NAME "httpendpoint"

namespace kurento
{

HttpEndpointImpl::HttpEndpointImpl (std::shared_ptr< MediaObjectImpl > parent, int disconnectionTimeout) : SessionEndpointImpl(parent, FACTORY_NAME)
{
  // FIXME: Implement this
}

std::string HttpEndpointImpl::getUrl ()
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "HttpEndpointImpl::getUrl: Not implemented");
}

HttpEndpointImpl::StaticConstructor HttpEndpointImpl::staticConstructor;

HttpEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
