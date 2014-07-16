#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaProfileSpecType.hpp"
#include <RecorderEndpointImplFactory.hpp>
#include "RecorderEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_recorder_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoRecorderEndpointImpl"

#define FACTORY_NAME "recorderendpoint"

namespace kurento
{

RecorderEndpointImpl::RecorderEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri, std::shared_ptr<MediaProfileSpecType> mediaProfile, bool stopOnEndOfStream) : UriEndpointImpl (std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
  // FIXME: Implement this
}

void RecorderEndpointImpl::record ()
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "RecorderEndpointImpl::record: Not implemented");
}

MediaObjectImpl *
RecorderEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri, std::shared_ptr<MediaProfileSpecType> mediaProfile, bool stopOnEndOfStream) const
{
  return new RecorderEndpointImpl (mediaPipeline, uri, mediaProfile, stopOnEndOfStream);
}

RecorderEndpointImpl::StaticConstructor RecorderEndpointImpl::staticConstructor;

RecorderEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
