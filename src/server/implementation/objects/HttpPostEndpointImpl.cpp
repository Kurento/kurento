#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <HttpPostEndpointImplFactory.hpp>
#include "HttpPostEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define USE_ENCODED_MEDIA "use-encoded-media"

#define GST_CAT_DEFAULT kurento_http_post_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpPostEndpointImpl"

namespace kurento
{

static void
adaptor_function (GstElement *player, gpointer data)
{
  auto handler = reinterpret_cast<std::function<void() >*> (data);

  (*handler) ();
}

HttpPostEndpointImpl::HttpPostEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, int disconnectionTimeout, bool useEncodedMedia) : HttpEndpointImpl (std::dynamic_pointer_cast< MediaObjectImpl > (mediaPipeline), disconnectionTimeout)
{
  g_object_set (G_OBJECT (element), USE_ENCODED_MEDIA, useEncodedMedia, NULL);
}

void HttpPostEndpointImpl::setHttpServerConfig(MediaServerConfig& config)
{
  this->setConfig (config);
  eosLambda = [&] () {
    try {
      EndOfStream event (shared_from_this(), EndOfStream::getName() );

      signalEndOfStream (event);
    } catch (std::bad_weak_ptr &e) {
    }
  };

  /* Do not accept EOS */
  g_object_set ( G_OBJECT (element), "accept-eos", false, NULL);
  g_signal_connect (element, "eos", G_CALLBACK (adaptor_function), this);

  register_end_point();

  if (!is_registered() ) {
    throw KurentoException (HTTP_END_POINT_REGISTRATION_ERROR,
                            "Cannot register HttpPostEndPoint");
  }
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
