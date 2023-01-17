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
#include <RtpEndpointImplFactory.hpp>
#include "RtpEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <CryptoSuite.hpp>
#include <SDES.hpp>
#include <SignalHandler.hpp>
#include <memory>
#include <string>

#define GST_CAT_DEFAULT kurento_rtp_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoRtpEndpointImpl"

#define FACTORY_NAME "rtpendpoint"

/* In theory the Master key can be shorter than the maximum length, but
 * the GStreamer's SRTP plugin enforces using the maximum length possible
 * for the type of cypher used (in file 'gstsrtpenc.c'). So, KMS also expects
 * that the maximum Master key size is used. */
#define KMS_SRTP_CIPHER_AES_CM_128_SIZE  ((gsize)30)
#define KMS_SRTP_CIPHER_AES_CM_256_SIZE  ((gsize)46)

namespace kurento
{

RtpEndpointImpl::RtpEndpointImpl (const boost::property_tree::ptree &conf,
                                  std::shared_ptr<MediaPipeline> mediaPipeline,
                                  std::shared_ptr<SDES> crypto, bool useIpv6)
  : BaseRtpEndpointImpl (conf,
                         std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline),
                         FACTORY_NAME, useIpv6)
{
  if (!crypto->isSetCrypto() ) {
    return;
  }

  if (!crypto->isSetKey() && !crypto->isSetKeyBase64()) {
    /* Use random key */
    g_object_set (element, "crypto-suite", crypto->getCrypto()->getValue(),
                  NULL);
    return;
  }

  gsize expect_size;

  switch (crypto->getCrypto()->getValue() ) {
  case CryptoSuite::AES_128_CM_HMAC_SHA1_32:
  case CryptoSuite::AES_128_CM_HMAC_SHA1_80:
    expect_size = KMS_SRTP_CIPHER_AES_CM_128_SIZE;
    break;
  case CryptoSuite::AES_256_CM_HMAC_SHA1_32:
  case CryptoSuite::AES_256_CM_HMAC_SHA1_80:
    expect_size = KMS_SRTP_CIPHER_AES_CM_256_SIZE;
    break;
  default:
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            "Invalid crypto suite");
  }

  std::string key_b64;
  gsize key_data_size = 0;

  if (crypto->isSetKey()) {
    std::string tmp = crypto->getKey();
    key_data_size = tmp.length();

    gchar *tmp_b64 = g_base64_encode ((const guchar *)tmp.data(), tmp.length());
    key_b64 = std::string (tmp_b64);
    g_free(tmp_b64);
  }
  else if (crypto->isSetKeyBase64()) {
    key_b64 = crypto->getKeyBase64();
    guchar *tmp_b64 = g_base64_decode (key_b64.data(), &key_data_size);
    if (!tmp_b64) {
      GST_ERROR_OBJECT (element, "Master key is not valid Base64");
      throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                              "Master key is not valid Base64");
    }
    g_free (tmp_b64);
  }

  if (key_data_size != expect_size) {
    GST_ERROR_OBJECT (element,
        "Bad Base64-decoded master key size: got %lu, expected %lu",
        key_data_size, expect_size);
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            "Master key size is wrong");
  }

  g_object_set (element, "master-key", key_b64.data(),
                "crypto-suite", crypto->getCrypto()->getValue(), NULL);
}

RtpEndpointImpl::~RtpEndpointImpl()
{
  if (handlerOnKeySoftLimit > 0) {
    unregister_signal_handler (element, handlerOnKeySoftLimit);
  }
}

void
RtpEndpointImpl::postConstructor ()
{
  BaseRtpEndpointImpl::postConstructor ();

  handlerOnKeySoftLimit = register_signal_handler (G_OBJECT (element),
                          "key-soft-limit",
                          std::function <void (GstElement *, gchar *) >
                          (std::bind (&RtpEndpointImpl::onKeySoftLimit, this,
                                      std::placeholders::_2) ),
                          std::dynamic_pointer_cast<RtpEndpointImpl>
                          (shared_from_this() ) );
}

void
RtpEndpointImpl::onKeySoftLimit (gchar *media)
{
  std::shared_ptr<MediaType> type;

  if (g_strcmp0 (media, "audio") == 0) {
    type = std::make_shared<MediaType>(MediaType::AUDIO);
  } else if (g_strcmp0 (media, "video") == 0) {
    type = std::make_shared<MediaType>(MediaType::VIDEO);
  } else if (g_strcmp0 (media, "data") == 0) {
    type = std::make_shared<MediaType>(MediaType::DATA);
  } else {
    GST_ERROR ("Unsupported media %s", media);
    return;
  }

  try {
    OnKeySoftLimit event (shared_from_this (), OnKeySoftLimit::getName (),
        type);
    sigcSignalEmit(signalOnKeySoftLimit, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", OnKeySoftLimit::getName ().c_str (),
        e.what ());
  }
}

MediaObjectImpl *
RtpEndpointImplFactory::createObject (const boost::property_tree::ptree &conf,
                                      std::shared_ptr<MediaPipeline> mediaPipeline,
                                      std::shared_ptr<SDES> crypto, bool useIpv6) const
{
  return new RtpEndpointImpl (conf, mediaPipeline, crypto, useIpv6);
}

RtpEndpointImpl::StaticConstructor RtpEndpointImpl::staticConstructor;

RtpEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
