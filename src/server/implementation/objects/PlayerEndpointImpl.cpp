#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <PlayerEndpointImplFactory.hpp>
#include "PlayerEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_player_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPlayerEndpointImpl"

#define FACTORY_NAME "playerendpoint"

namespace kurento
{
static void
adaptor_function (GstElement *player, gpointer data)
{
  auto handler = reinterpret_cast<std::function<void() >*> (data);

  (*handler) ();
}


PlayerEndpointImpl::PlayerEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri, bool useEncodedMedia) : UriEndpointImpl (std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
 GstElement *element = getGstreamerElement();

  g_object_set (G_OBJECT (element), "use-encoded-media", useEncodedMedia, NULL);

  eosLambda = [&] () {
    try {
      EndOfStream event (shared_from_this(), EndOfStream::getName() );

      signalEndOfStream (event);
    } catch (std::bad_weak_ptr &e) {
    }
  };

  invalidUriLambda = [&] () {
    try {
      /* TODO: Define error codes and types*/
      Error error (shared_from_this(), "Invalid Uri", 0, "INVALID_URI");

      signalError (error);
    } catch (std::bad_weak_ptr &e) {
    }
  };

  invalidMediaLambda = [&] () {
    try {
      /* TODO: Define error codes and types*/
      Error error (shared_from_this(), "Invalid Media", 0, "INVALID_MEDIA");

      signalError (error);
    } catch (std::bad_weak_ptr &e) {
    }
  };

  signalEOS = g_signal_connect (element, "eos", G_CALLBACK (adaptor_function),
                                &eosLambda);
  signalInvalidURI = g_signal_connect (element, "invalid-uri",
                                       G_CALLBACK (adaptor_function),
                                       &invalidUriLambda);
  signalInvalidMedia = g_signal_connect (element, "invalid-media",
                                         G_CALLBACK (adaptor_function),
                                         &invalidMediaLambda);
}

PlayerEndpointImpl::~PlayerEndpointImpl()
{
  g_signal_handler_disconnect (element, signalEOS);
  g_signal_handler_disconnect (element, signalInvalidMedia);
  g_signal_handler_disconnect (element, signalInvalidURI);
  stop();
}

void PlayerEndpointImpl::play ()
{
  start();
}

MediaObjectImpl *
PlayerEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri, bool useEncodedMedia) const
{
  return new PlayerEndpointImpl (mediaPipeline, uri, useEncodedMedia);
}

PlayerEndpointImpl::StaticConstructor PlayerEndpointImpl::staticConstructor;

PlayerEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
