#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <RtpEndpointImplFactory.hpp>
#include "RtpEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_rtp_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoRtpEndpointImpl"

#define FACTORY_NAME "rtpendpoint"

namespace kurento
{

RtpEndpointImpl::RtpEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline) : SdpEndpointImpl (std::dynamic_pointer_cast<MediaObjectImpl>(mediaPipeline), FACTORY_NAME)
{
  // FIXME: Implement this
}

MediaObjectImpl *
RtpEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new RtpEndpointImpl (mediaPipeline);
}

RtpEndpointImpl::StaticConstructor RtpEndpointImpl::staticConstructor;

RtpEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
