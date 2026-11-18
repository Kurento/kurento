/*
 * (C) Copyright 2024 Kurento (http://kurento.org/)
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
#include <MoqSubscriberEndpointImplFactory.hpp>
#include "MoqSubscriberEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include "SignalHandler.hpp"
#include "kmsmoqsubscriberendpoint.h"

#define GST_CAT_DEFAULT kurento_moq_subscriber_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMoqSubscriberEndpointImpl"

#define FACTORY_NAME "moqsubscriberendpoint"

namespace kurento
{

void MoqSubscriberEndpointImpl::eosHandler ()
{
  try {
    EndOfStream event (shared_from_this (), EndOfStream::getName ());
    sigcSignalEmit (signalEndOfStream, event);
  } catch (const std::bad_weak_ptr &e) {
    GST_ERROR ("BUG creating %s: %s", EndOfStream::getName ().c_str (),
        e.what ());
  }
}

void MoqSubscriberEndpointImpl::postConstructor ()
{
  EndpointImpl::postConstructor ();

  signalEOS = register_signal_handler (G_OBJECT (element), "eos",
      std::function<void (GstElement *)>
      (std::bind (&MoqSubscriberEndpointImpl::eosHandler, this)),
      std::dynamic_pointer_cast<MoqSubscriberEndpointImpl>
      (shared_from_this ()));
}

MoqSubscriberEndpointImpl::MoqSubscriberEndpointImpl (
    const boost::property_tree::ptree &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::string &url,
    const std::string &broadcast)
  : EndpointImpl (conf,
        std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline),
        FACTORY_NAME),
    url (url),
    broadcast (broadcast)
{
  GstElement *elem = getGstreamerElement ();

  g_object_set (G_OBJECT (elem),
      "url",       url.c_str (),
      "broadcast", broadcast.c_str (),
      NULL);
}

MoqSubscriberEndpointImpl::~MoqSubscriberEndpointImpl ()
{
  if (signalEOS > 0) {
    unregister_signal_handler (element, signalEOS);
  }

  /* Stop the internal moqsrc pipeline */
  kms_moq_subscriber_endpoint_unsubscribe (
      KMS_MOQ_SUBSCRIBER_ENDPOINT (element));
}

void MoqSubscriberEndpointImpl::subscribe ()
{
  kms_moq_subscriber_endpoint_subscribe (
      KMS_MOQ_SUBSCRIBER_ENDPOINT (element));
}

void MoqSubscriberEndpointImpl::unsubscribe ()
{
  kms_moq_subscriber_endpoint_unsubscribe (
      KMS_MOQ_SUBSCRIBER_ENDPOINT (element));
}

std::string MoqSubscriberEndpointImpl::getUrl ()
{
  return url;
}

std::string MoqSubscriberEndpointImpl::getBroadcast ()
{
  return broadcast;
}

MediaObjectImpl *
MoqSubscriberEndpointImplFactory::createObject (
    const boost::property_tree::ptree &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::string &url,
    const std::string &broadcast) const
{
  return new MoqSubscriberEndpointImpl (conf, mediaPipeline, url, broadcast);
}

MoqSubscriberEndpointImpl::StaticConstructor
    MoqSubscriberEndpointImpl::staticConstructor;

MoqSubscriberEndpointImpl::StaticConstructor::StaticConstructor ()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}

} /* kurento */
