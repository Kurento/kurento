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
#include <HttpPostEndpointImplFactory.hpp>
#include "HttpPostEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <SignalHandler.hpp>

#define USE_ENCODED_MEDIA "use-encoded-media"

#define GST_CAT_DEFAULT kurento_http_post_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpPostEndpointImpl"

#define FACTORY_NAME "httppostendpoint"

namespace kurento
{

void HttpPostEndpointImpl::eosLambda ()
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

void HttpPostEndpointImpl::postConstructor ()
{
  HttpEndpointImpl::postConstructor ();

  handlerEos = register_signal_handler (G_OBJECT (element), "eos",
                                        std::function <void (GstElement *) >
                                        (std::bind (&HttpPostEndpointImpl::eosLambda, this) ),
                                        std::dynamic_pointer_cast<HttpPostEndpointImpl>
                                        (shared_from_this() ) );
}

HttpPostEndpointImpl::HttpPostEndpointImpl (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, int disconnectionTimeout,
    bool useEncodedMedia) : HttpEndpointImpl (conf,
          std::dynamic_pointer_cast< MediaObjectImpl > (mediaPipeline),
          disconnectionTimeout, FACTORY_NAME)
{
  g_object_set (G_OBJECT (element), USE_ENCODED_MEDIA, useEncodedMedia, NULL);

  /* Do not accept EOS */
  g_object_set ( G_OBJECT (element), "accept-eos", false, NULL);

  register_end_point();

  if (!is_registered() ) {
    throw KurentoException (HTTP_END_POINT_REGISTRATION_ERROR,
                            "Cannot register HttpPostEndPoint");
  }
}

HttpPostEndpointImpl::~HttpPostEndpointImpl ()
{
  if (handlerEos > 0) {
    unregister_signal_handler (element, handlerEos);
  }
}

MediaObjectImpl *
HttpPostEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, int disconnectionTimeout, bool useEncodedMedia) const
{
  return new HttpPostEndpointImpl (conf, mediaPipeline, disconnectionTimeout,
                                   useEncodedMedia);
}

HttpPostEndpointImpl::StaticConstructor HttpPostEndpointImpl::staticConstructor;

HttpPostEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
