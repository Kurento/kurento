#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <HttpPostEndpointImplFactory.hpp>
#include "HttpPostEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_http_post_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpPostEndpointImpl"

namespace kurento
{

HttpPostEndpointImpl::HttpPostEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, int disconnectionTimeout, bool useEncodedMedia) : HttpEndpointImpl (std::dynamic_pointer_cast< MediaObjectImpl > (parent), disconnectionTimeout)
{
  // FIXME: Implement this
}

MediaObjectImpl *
HttpPostEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline, int disconnectionTimeout, bool useEncodedMedia) const
{
  return new HttpPostEndpointImpl (mediaPipeline, disconnectionTimeout, useEncodedMedia);
}

HttpPostEndpointImpl::StaticConstructor HttpPostEndpointImpl::staticConstructor;

HttpPostEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
