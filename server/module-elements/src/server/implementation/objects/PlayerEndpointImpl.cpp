/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "VideoInfo.hpp"
#include <PlayerEndpointImplFactory.hpp>
#include "PlayerEndpointImpl.hpp"
#include <DotGraph.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <memory>
#include <gst/gst.h>
#include "SignalHandler.hpp"

#define GST_CAT_DEFAULT kurento_player_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPlayerEndpointImpl"

#define FACTORY_NAME "playerendpoint"
#define VIDEO_DATA "video-data"
#define POSITION "position"
#define PIPELINE "pipeline"
#define SET_POSITION "set-position"
#define NS_TO_MS 1000000
#define RTSP_CLIENT_PORT_RANGE "rtspClientPortRange"

namespace kurento
{
void PlayerEndpointImpl::eosHandler ()
{
  try {
    EndOfStream event (shared_from_this (), EndOfStream::getName ());
    sigcSignalEmit(signalEndOfStream, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", EndOfStream::getName ().c_str (),
        e.what ());
  }
}

void PlayerEndpointImpl::invalidUri ()
{
  /* TODO: Define error codes and types*/
  try {
    Error event (shared_from_this (), "Invalid URI", 0, "INVALID_URI");
    sigcSignalEmit(signalError, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", Error::getName ().c_str (), e.what ());
  }
}

void PlayerEndpointImpl::invalidMedia ()
{
  /* TODO: Define error codes and types*/
  try {
    Error event (shared_from_this (), "Invalid Media", 0, "INVALID_MEDIA");
    sigcSignalEmit(signalError, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", Error::getName ().c_str (), e.what ());
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
                                        bool useEncodedMedia, int networkCache) : UriEndpointImpl (conf,
                                              std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
  GstElement *element = getGstreamerElement();

  g_object_set (G_OBJECT (element), "use-encoded-media", useEncodedMedia,
                "network-cache", networkCache, NULL);

  std::string portRange;
  if (getConfigValue <std::string, PlayerEndpoint> (&portRange,
      RTSP_CLIENT_PORT_RANGE)) {
    g_object_set (G_OBJECT (element), "port-range", portRange.c_str(), NULL);
  }
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

  if (video_data == nullptr) {
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

std::string PlayerEndpointImpl::getElementGstreamerDot ()
{
  GValue *pipeline;
  g_object_get (G_OBJECT (element), PIPELINE, &pipeline, NULL);
  return generateDotGraph(
      GST_BIN(pipeline),
      std::make_shared<GstreamerDotDetails>(GstreamerDotDetails::SHOW_VERBOSE));
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
    bool useEncodedMedia, int networkCache) const
{
  return new PlayerEndpointImpl (conf, mediaPipeline, uri, useEncodedMedia,
                                 networkCache);
}

PlayerEndpointImpl::StaticConstructor PlayerEndpointImpl::staticConstructor;

PlayerEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
