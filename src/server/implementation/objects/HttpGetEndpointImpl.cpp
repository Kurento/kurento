#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaProfileSpecType.hpp"
#include <HttpGetEndpointImplFactory.hpp>
#include "HttpGetEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include "commons/kmsrecordingprofile.h"

#define GST_CAT_DEFAULT kurento_http_get_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpGetEndpointImpl"

#define FACTORY_NAME "httpgetendpoint"

namespace kurento
{

HttpGetEndpointImpl::HttpGetEndpointImpl (
  const boost::property_tree::ptree &conf,
  std::shared_ptr<MediaPipeline> mediaPipeline, bool terminateOnEOS,
  std::shared_ptr<MediaProfileSpecType> mediaProfile,
  int disconnectionTimeout) : HttpEndpointImpl (conf,
        std::dynamic_pointer_cast< MediaObjectImpl > (mediaPipeline),
        disconnectionTimeout, FACTORY_NAME)
{
  g_object_set ( G_OBJECT (element), "accept-eos", terminateOnEOS,
                 NULL);

  switch (mediaProfile->getValue() ) {
  case MediaProfileSpecType::WEBM:
    GST_INFO ("Set WEBM profile");
    g_object_set ( G_OBJECT (element), "profile", KMS_RECORDING_PROFILE_WEBM, NULL);
    break;

  case MediaProfileSpecType::MP4:
    GST_INFO ("Set MP4 profile");
    g_object_set ( G_OBJECT (element), "profile", KMS_RECORDING_PROFILE_MP4, NULL);
    break;

  case MediaProfileSpecType::WEBM_VIDEO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY, NULL);
    GST_INFO ("Set WEBM profile");
    break;

  case MediaProfileSpecType::WEBM_AUDIO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY, NULL);
    GST_INFO ("Set WEBM profile");
    break;

  case MediaProfileSpecType::MP4_VIDEO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY, NULL);
    GST_INFO ("Set WEBM profile");
    break;

  case MediaProfileSpecType::MP4_AUDIO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY, NULL);
    GST_INFO ("Set WEBM profile");
    break;
  }

  register_end_point();

  if (!is_registered() ) {
    throw KurentoException (HTTP_END_POINT_REGISTRATION_ERROR,
                            "Cannot register HttpGetEndPoint");
  }
}


MediaObjectImpl *
HttpGetEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, bool terminateOnEOS,
    std::shared_ptr<MediaProfileSpecType> mediaProfile,
    int disconnectionTimeout) const
{
  return new HttpGetEndpointImpl (conf, mediaPipeline, terminateOnEOS,
                                  mediaProfile,
                                  disconnectionTimeout);
}

HttpGetEndpointImpl::StaticConstructor HttpGetEndpointImpl::staticConstructor;

HttpGetEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
