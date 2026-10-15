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
#include <MoqPublisherEndpointImplFactory.hpp>
#include "MoqPublisherEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include "kmsmoqpublisherendpoint.h"

#define GST_CAT_DEFAULT kurento_moq_publisher_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMoqPublisherEndpointImpl"

#define FACTORY_NAME "moqpublisherendpoint"

namespace kurento
{

MoqPublisherEndpointImpl::MoqPublisherEndpointImpl (
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

MoqPublisherEndpointImpl::~MoqPublisherEndpointImpl ()
{
  kms_moq_publisher_endpoint_unpublish (
      KMS_MOQ_PUBLISHER_ENDPOINT (element));
}

void MoqPublisherEndpointImpl::publish ()
{
  kms_moq_publisher_endpoint_publish (
      KMS_MOQ_PUBLISHER_ENDPOINT (element));
}

void MoqPublisherEndpointImpl::unpublish ()
{
  kms_moq_publisher_endpoint_unpublish (
      KMS_MOQ_PUBLISHER_ENDPOINT (element));
}

std::string MoqPublisherEndpointImpl::getUrl ()
{
  return url;
}

std::string MoqPublisherEndpointImpl::getBroadcast ()
{
  return broadcast;
}

MediaObjectImpl *
MoqPublisherEndpointImplFactory::createObject (
    const boost::property_tree::ptree &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::string &url,
    const std::string &broadcast) const
{
  return new MoqPublisherEndpointImpl (conf, mediaPipeline, url, broadcast);
}

MoqPublisherEndpointImpl::StaticConstructor
    MoqPublisherEndpointImpl::staticConstructor;

MoqPublisherEndpointImpl::StaticConstructor::StaticConstructor ()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}

} /* kurento */
