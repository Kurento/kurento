#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <WebRtcEndpointImplFactory.hpp>
#include "WebRtcEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_web_rtc_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoWebRtcEndpointImpl"

#define FACTORY_NAME "webrtcendpoint"

namespace kurento
{

WebRtcEndpointImpl::WebRtcEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline) : SdpEndpointImpl (std::dynamic_pointer_cast<MediaObjectImpl>(mediaPipeline), FACTORY_NAME)
{
  // FIXME: Implement this
}

MediaObjectImpl *
WebRtcEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new WebRtcEndpointImpl (mediaPipeline);
}

WebRtcEndpointImpl::StaticConstructor WebRtcEndpointImpl::staticConstructor;

WebRtcEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
