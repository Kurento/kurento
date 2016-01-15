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

#define GST_CAT_DEFAULT kurento_rtp_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoRtpEndpointImpl"

#define FACTORY_NAME "rtpendpoint"

#define MIN_KEY_LENGTH 30
#define MAX_KEY_LENGTH 46

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

  if (!crypto->isSetKey() ) {
    /* Use random key */
    g_object_set (element, "crypto-suite", crypto->getCrypto()->getValue(),
                  NULL);
    return;
  }

  std::string key = crypto->getKey();
  uint len;

  switch (crypto->getCrypto()->getValue() ) {
  case CryptoSuite::AES_128_CM_HMAC_SHA1_32:
  case CryptoSuite::AES_128_CM_HMAC_SHA1_80:
    len = MIN_KEY_LENGTH;
    break;

  case CryptoSuite::AES_256_CM_HMAC_SHA1_32:
  case CryptoSuite::AES_256_CM_HMAC_SHA1_80:
    len = MAX_KEY_LENGTH;
    break;

  default:
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            "Invalid crypto suite");
  }

  if (key.length () != len) {
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            "Master key size out of range");
  }

  g_object_set (element, "master-key", crypto->getKey().c_str(),
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
    type = std::shared_ptr<MediaType> (new MediaType (MediaType::AUDIO) );
  } else if (g_strcmp0 (media, "video") == 0) {
    type = std::shared_ptr<MediaType> (new MediaType (MediaType::VIDEO) );
  } else if (g_strcmp0 (media, "data") == 0) {
    type = std::shared_ptr<MediaType> (new MediaType (MediaType::DATA) );
  } else {
    GST_ERROR ("Unsupported media %s", media);
    return;
  }

  try {
    OnKeySoftLimit event (shared_from_this(), OnKeySoftLimit::getName(), type);
    signalOnKeySoftLimit (event);
  } catch (std::bad_weak_ptr &e) {
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
