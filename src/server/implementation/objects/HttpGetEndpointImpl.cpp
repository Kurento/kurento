#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaProfileSpecType.hpp"
#include <HttpGetEndpointImplFactory.hpp>
#include "HttpGetEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_http_get_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpGetEndpointImpl"

namespace kurento
{

HttpGetEndpointImpl::HttpGetEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, bool terminateOnEOS, std::shared_ptr<MediaProfileSpecType> mediaProfile, int disconnectionTimeout) : HttpEndpointImpl (std::dynamic_pointer_cast< MediaObjectImpl > (parent), disconnectionTimeout)
{
  // FIXME: Implement this
}

MediaObjectImpl *
HttpGetEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline, bool terminateOnEOS, std::shared_ptr<MediaProfileSpecType> mediaProfile, int disconnectionTimeout) const
{
  return new HttpGetEndpointImpl (mediaPipeline, terminateOnEOS, mediaProfile, disconnectionTimeout);
}

HttpGetEndpointImpl::StaticConstructor HttpGetEndpointImpl::staticConstructor;

HttpGetEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
