#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "VideoInfo.hpp"
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
#define VIDEO_DATA "video-data"
#define POSITION "position"
#define SET_POSITION "set-position"
#define NS_TO_MS 1000000

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

std::shared_ptr<VideoInfo> PlayerEndpointImpl::getVideoInfo ()
{
  GstStructure *video_data;
  gboolean isSeekable;
  int64_t seekableInit, seekableEnd, duration;
  std::shared_ptr<VideoInfo> videoInfo;

  g_object_get (G_OBJECT (element), VIDEO_DATA, &video_data, NULL);

  if (video_data == NULL) {
    GST_ERROR ("structure null");
  }

  gst_structure_get (video_data, "isSeekable", G_TYPE_BOOLEAN, &isSeekable,
                     "seekableInit", G_TYPE_INT64, &seekableInit,
                     "seekableEnd", G_TYPE_INT64, &seekableEnd,
                     "duration", G_TYPE_INT64, &duration, NULL);

  videoInfo = std::make_shared<VideoInfo> (VideoInfo (isSeekable,
              seekableInit / NS_TO_MS,
              seekableEnd / NS_TO_MS,
              duration / NS_TO_MS) );
  return videoInfo;
}

int64_t PlayerEndpointImpl::getPosition ()
{
  int64_t position;

  g_object_get (G_OBJECT (element), POSITION, &position, NULL);

  return position / NS_TO_MS;
}

void PlayerEndpointImpl::setPosition (int64_t position)
{
  gboolean ret;

  g_signal_emit_by_name (element, SET_POSITION, position * NS_TO_MS, &ret);

  if (!ret) {
    throw KurentoException (PLAYER_SEEK_FAIL, "Seek fails");
  }
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
