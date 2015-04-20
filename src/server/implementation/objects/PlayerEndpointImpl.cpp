#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <PlayerEndpointImplFactory.hpp>
#include "PlayerEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include "SignalHandler.hpp"

#define GST_CAT_DEFAULT kurento_player_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPlayerEndpointImpl"

#define FACTORY_NAME "playerendpoint"

namespace kurento
{
void PlayerEndpointImpl::eosHandler ()
{
  try {
    EndOfStream event (shared_from_this(), EndOfStream::getName() );

    signalEndOfStream (event);
  } catch (std::bad_weak_ptr &e) {
  }
}

void PlayerEndpointImpl::invalidUri ()
{
  try {
    /* TODO: Define error codes and types*/
    Error error (shared_from_this(), "Invalid Uri", 0, "INVALID_URI");

    signalError (error);
  } catch (std::bad_weak_ptr &e) {
  }
}

void PlayerEndpointImpl::invalidMedia ()
{
  try {
    /* TODO: Define error codes and types*/
    Error error (shared_from_this(), "Invalid Media", 0, "INVALID_MEDIA");

    signalError (error);
  } catch (std::bad_weak_ptr &e) {
  }
}

void PlayerEndpointImpl::postConstructor()
{
  UriEndpointImpl::postConstructor ();

  signalEOS = register_signal_handler (G_OBJECT (element), "eos",
                                       std::function <void (GstElement *) >
                                       (std::bind (&PlayerEndpointImpl::eosHandler, this) ),
                                       std::dynamic_pointer_cast<PlayerEndpointImpl>
                                       (shared_from_this() ) );

  signalInvalidURI = register_signal_handler (G_OBJECT (element), "invalid-uri",
                     std::function <void (GstElement *) >
                     (std::bind (&PlayerEndpointImpl::invalidUri, this) ),
                     std::dynamic_pointer_cast<PlayerEndpointImpl>
                     (shared_from_this() ) );

  signalInvalidMedia = register_signal_handler (G_OBJECT (element),
                       "invalid-media",
                       std::function <void (GstElement *) >
                       (std::bind (&PlayerEndpointImpl::invalidMedia, this) ),
                       std::dynamic_pointer_cast<PlayerEndpointImpl>
                       (shared_from_this() ) );
}


PlayerEndpointImpl::PlayerEndpointImpl (const boost::property_tree::ptree &conf,
                                        std::shared_ptr<MediaPipeline>
                                        mediaPipeline, const std::string &uri,
                                        bool useEncodedMedia) : UriEndpointImpl (conf,
                                              std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
  GstElement *element = getGstreamerElement();

  g_object_set (G_OBJECT (element), "use-encoded-media", useEncodedMedia, NULL);
}

PlayerEndpointImpl::~PlayerEndpointImpl()
{
  if (signalEOS > 0 ) {
    unregister_signal_handler (element, signalEOS);
  }

  if (signalInvalidMedia > 0 ) {
    unregister_signal_handler (element, signalInvalidMedia);
  }

  if (signalInvalidURI > 0 ) {
    unregister_signal_handler (element, signalInvalidURI);
  }

  stop();
}

void PlayerEndpointImpl::play ()
{
  start();
}

MediaObjectImpl *
PlayerEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri,
    bool useEncodedMedia) const
{
  return new PlayerEndpointImpl (conf, mediaPipeline, uri, useEncodedMedia);
}

PlayerEndpointImpl::StaticConstructor PlayerEndpointImpl::staticConstructor;

PlayerEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
