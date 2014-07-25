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
}

void WebRtcEndpointImpl::setConfig(const MediaServerConfig& config)
{
  g_object_set (element, "pattern-sdp", config.getSdpPattern(), NULL);

  //set properties
  GST_INFO ("stun port %d\n", config.getStunServerPort());

  if (config.getStunServerPort() != 0) {
    g_object_set ( G_OBJECT (element), "stun-server-port", config.getStunServerPort(), NULL);
  }

  GST_INFO ("stun address %s\n", config.getStunServerAddress().c_str() );
  g_object_set ( G_OBJECT (element), "stun-server", config.getStunServerAddress().c_str(),
                 NULL);

  GST_INFO ("turn info: %s\n", config.getTurnURL().c_str() );
  g_object_set ( G_OBJECT (element), "turn-url", config.getTurnURL().c_str(), NULL);

  if (config.getPemCertificate().compare ("") == 0) {
    GST_INFO ("Using default pemCertificate");
  } else {
    GST_INFO ("PemCertificate %s\n", config.getPemCertificate().c_str() );
    g_object_set ( G_OBJECT (element), "certificate-pem-file",
                   config.getPemCertificate().c_str(), NULL);
  }
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
