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
#include <WebRtcEndpointImplFactory.hpp>
#include "WebRtcEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <boost/filesystem.hpp>
#include <IceCandidate.hpp>
#include "IceCandidatePair.hpp"
#include "IceConnection.hpp"
#include "CertificateKeyType.hpp"
#include <webrtcendpoint/kmsicecandidate.h>
#include <IceComponentState.hpp>
#include <DtlsConnectionState.hpp>
#include <DtlsConnection.hpp>
#include <SignalHandler.hpp>
#include <webrtcendpoint/kmsicebaseagent.h>

#include <StatsType.hpp>
#include <RTCDataChannelState.hpp>
#include <RTCDataChannelStats.hpp>
#include <RTCPeerConnectionStats.hpp>
#include <commons/kmsstats.h>
#include <commons/kmsutils.h>
#include <commons/gstsdpdirection.h>

#include "webrtcendpoint/kmswebrtcdatachannelstate.h"
#include "webrtcendpoint/kmswebrtcdtlsconnectionstate.h"
#include <boost/algorithm/string.hpp>

#include <CertificateManager.hpp>

#include <DSCPValue.hpp>

#define GST_CAT_DEFAULT kurento_web_rtc_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoWebRtcEndpointImpl"

#define FACTORY_NAME "webrtcendpoint"

#define CONFIG_PATH "configPath"
#define DEFAULT_PATH "/etc/kurento"

#define PARAM_EXTERNAL_IPV4 "externalIPv4"
#define PARAM_EXTERNAL_IPV6 "externalIPv6"
#define PARAM_NETWORK_INTERFACES "networkInterfaces"
#define PARAM_ICE_TCP "iceTcp"

#define PROP_EXTERNAL_IPV4 "external-ipv4"
#define PROP_EXTERNAL_IPV6 "external-ipv6"
#define PROP_NETWORK_INTERFACES "network-interfaces"
#define PROP_ICE_TCP "ice-tcp"

#define PARAM_QOS_DSCP "qos-dscp"

#define PARAM_ENABLE_H265 "enable-h265"

namespace kurento
{

static const uint DEFAULT_STUN_PORT = 3478;

static std::once_flag check_openh264, certificates_flag;
static std::string defaultCertificateRSA, defaultCertificateECDSA;

// "H264" gets added at runtime by check_support_for_h264()
static std::vector<std::string> supported_codecs = { "AV1", "VP9", "VP8", "opus", "PCMU", "PCMA" };

static void
remove_not_supported_codecs_from_array (GstElement *element, GArray *codecs)
{
  guint i;

  if (codecs == nullptr) {
    return;
  }

  for (i = 0; i < codecs->len; i++) {
    GValue *v = &g_array_index (codecs, GValue, i);
    const GstStructure *s;
    const gchar *codec_name;
    gboolean supported = FALSE;

    if (!GST_VALUE_HOLDS_STRUCTURE (v) ) {
      GST_WARNING_OBJECT (element, "Value into array is not a GstStructure");
      continue;
    }

    s = gst_value_get_structure (v);
    codec_name = gst_structure_get_name (s);

    for (auto &supported_codec : supported_codecs) {

      if (boost::istarts_with(codec_name, supported_codec)) {
        supported = TRUE;
        break;
      }
    }

    if (!supported) {
      GST_INFO_OBJECT (element, "Removing not supported codec '%s'", codec_name);
      g_array_remove_index (codecs, i);
      i--;
    }
  }
}

static void
remove_not_supported_codecs (GstElement *element)
{
  GArray *codecs;

  g_object_get (element, "audio-codecs", &codecs, NULL);
  remove_not_supported_codecs_from_array (element, codecs);
  g_array_unref (codecs);

  g_object_get (element, "video-codecs", &codecs, NULL);
  remove_not_supported_codecs_from_array (element, codecs);
  g_array_unref (codecs);
}

static void
add_support_for_h265 ()
{
  supported_codecs.emplace_back("H265");
}

static void
check_support_for_h264 ()
{
  GstPlugin *plugin;

  plugin = gst_plugin_load_by_name ("openh264");

  if (plugin == nullptr) {
    GST_WARNING ("H264 is NOT supported: Plugin 'openh264' not found");
    return;
  }

  supported_codecs.emplace_back("H264");
  gst_object_unref (plugin);
}

void
WebRtcEndpointImpl::generateDefaultCertificates ()
{
  defaultCertificateECDSA = "";
  defaultCertificateRSA = "";

  std::string pemUriRSA;
  if (getConfigValue <std::string, WebRtcEndpoint> (&pemUriRSA,
      "pemCertificateRSA")) {
    defaultCertificateRSA = getCerficateFromFile (pemUriRSA);
  } else {
    GST_INFO ("Unable to load the RSA certificate from file. Using the default certificate.");
    defaultCertificateRSA = CertificateManager::generateRSACertificate ();
  }

  std::string pemUriECDSA;
  if (getConfigValue <std::string, WebRtcEndpoint> (&pemUriECDSA,
      "pemCertificateECDSA")) {
    defaultCertificateECDSA = getCerficateFromFile (pemUriECDSA);
  } else {
    GST_INFO ("Unable to load the ECDSA certificate from file. Using the default certificate.");
    defaultCertificateECDSA = CertificateManager::generateECDSACertificate ();
  }
}

void WebRtcEndpointImpl::checkUri (std::string &uri)
{
  //Check if uri is an absolute or relative path.
  if (! boost::starts_with (uri, "/") ) {
    std::string path;
    if (!getConfigValue <std::string, WebRtcEndpoint> (&path, CONFIG_PATH)) {
      GST_DEBUG ("WebRtcEndpoint config file doesn't contain a default path");
      getConfigValue <std::string> (&path, CONFIG_PATH, DEFAULT_PATH);
    }

    uri = path + "/" + uri;
  }
}

void WebRtcEndpointImpl::onIceCandidate (gchar *sessId,
    KmsIceCandidate *candidate)
{
  std::string cand_str (kms_ice_candidate_get_candidate (candidate) );
  std::string mid_str (kms_ice_candidate_get_sdp_mid (candidate) );
  int sdp_m_line_index = kms_ice_candidate_get_sdp_m_line_index (candidate);
  std::shared_ptr <IceCandidate> cand ( new  IceCandidate
                                        (cand_str, mid_str, sdp_m_line_index) );

  try {
    IceCandidateFound event (shared_from_this(),
        IceCandidateFound::getName (), cand);
    sigcSignalEmit(signalIceCandidateFound, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", IceCandidateFound::getName ().c_str (),
        e.what ());
  }
}

void WebRtcEndpointImpl::onIceGatheringDone (gchar *sessId)
{
  try {
    IceGatheringDone event (shared_from_this (), IceGatheringDone::getName ());
    sigcSignalEmit(signalIceGatheringDone, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", IceGatheringDone::getName ().c_str (),
        e.what ());
  }
}

void WebRtcEndpointImpl::onIceComponentStateChanged (gchar *sessId,
    const gchar *streamId,
    guint componentId, guint state)
{
  IceComponentState::type type;
  std::shared_ptr<IceConnection> connectionState;
  std::map < std::string, std::shared_ptr<IceConnection>>::iterator it;
  std::string key;

  switch (state) {
  case ICE_STATE_DISCONNECTED:
    type = IceComponentState::DISCONNECTED;
    break;

  case ICE_STATE_GATHERING:
    type = IceComponentState::GATHERING;
    break;

  case ICE_STATE_CONNECTING:
    type = IceComponentState::CONNECTING;
    break;

  case ICE_STATE_CONNECTED:
    type = IceComponentState::CONNECTED;
    break;

  case ICE_STATE_READY:
    type = IceComponentState::READY;
    break;

  case ICE_STATE_FAILED:
    type = IceComponentState::FAILED;
    break;

  default:
    type = IceComponentState::FAILED;
    break;
  }

  IceComponentState *componentState_event = new IceComponentState (type);
  IceComponentState *componentState_property = new IceComponentState (type);

  connectionState = std::make_shared< IceConnection> (streamId, componentId,
                    std::shared_ptr<IceComponentState> (componentState_property) );
  key = std::string (streamId) + '_' + std::to_string (componentId);

  std::unique_lock<std::mutex> mutex (mut);
  it = iceConnectionState.find (key);
  iceConnectionState[key] = connectionState;
  iceConnectionState.insert (std::pair
                             <std::string, std::shared_ptr <IceConnection>> (key, connectionState) );

  try {
    IceComponentStateChanged event (shared_from_this (),
        IceComponentStateChanged::getName (), atoi (streamId), componentId,
        std::shared_ptr<IceComponentState> (componentState_event));
    sigcSignalEmit(signalIceComponentStateChanged, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s",
        IceComponentStateChanged::getName ().c_str (), e.what ());
  }
}

void WebRtcEndpointImpl::onDtlsConnectionStateChanged (gchar *sessId,
    const gchar *streamId,
    gchar *componentId, gchar *connection_id, guint state)
{
  DtlsConnectionState::type type;
  std::shared_ptr<DtlsConnection> connectionState;
  std::map < std::string, std::shared_ptr<DtlsConnection>>::iterator it;
  std::string key;

  switch (state) {
  case DTLS_CONNECTION_STATE_NEW:
    type = DtlsConnectionState::NEW;
    break;

  case DTLS_CONNECTION_STATE_CONNECTING:
    type = DtlsConnectionState::CONNECTING;
    break;

  case DTLS_CONNECTION_STATE_CONNECTED:
    type = DtlsConnectionState::CONNECTED;
    break;

  case DTLS_CONNECTION_STATE_FAILED:
    type = DtlsConnectionState::FAILED;
    break;

  case DTLS_CONNECTION_STATE_CLOSED:
    type = DtlsConnectionState::CLOSED;
    break;

  default:
    type = DtlsConnectionState::FAILED;
    break;
  }

  DtlsConnectionState *newComponentState_event = new DtlsConnectionState (type);
  DtlsConnectionState *componentState_property = new DtlsConnectionState (type);

  connectionState = std::make_shared<DtlsConnection> (std::string(streamId), std::string(componentId), std::string(connection_id),
                    std::shared_ptr<DtlsConnectionState> (componentState_property) );

  key = std::string (streamId) + '_' + std::string (componentId);

  std::unique_lock<std::mutex> mutex (mut);
  it = dtlsConnectionState.find (key);
  dtlsConnectionState[key] = connectionState;
  dtlsConnectionState.insert (std::pair
                             <std::string, std::shared_ptr <DtlsConnection>> (key, connectionState) );

  try {
    DtlsConnectionStateChange event (shared_from_this (),
        DtlsConnectionStateChange::getName (), streamId, componentId, connection_id,
        std::shared_ptr<DtlsConnectionState> (newComponentState_event));
    GST_DEBUG_OBJECT(element,"DTLS connection state change sesId: %s, connId: %s, comp: %s, source: %s, stream: %s, state: %s", 
              sessId, connection_id,  event.getComponentId().c_str(), event.getSource()->getId().c_str(), event.getStreamId().c_str(), event.getState()->getString().c_str());
    sigcSignalEmit(signalDtlsConnectionStateChange, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s",
        DtlsConnectionStateChange::getName ().c_str (), e.what ());
  }
}

void WebRtcEndpointImpl::newSelectedPairFull (gchar *sessId,
    const gchar *streamId,
    guint componentId, KmsIceCandidate *localCandidate,
    KmsIceCandidate *remoteCandidate)
{
  std::shared_ptr<IceCandidatePair > candidatePair;
  std::string key;
  std::map<std::string, std::shared_ptr <IceCandidatePair>>::iterator it;

  GST_DEBUG_OBJECT (element,
      "New candidate pair selected, local: '%s', remote: '%s'"
      ", stream_id: '%s', component_id: %d",
      kms_ice_candidate_get_candidate (localCandidate),
      kms_ice_candidate_get_candidate (remoteCandidate),
      streamId, componentId);

  candidatePair = std::make_shared< IceCandidatePair > (streamId, componentId,
                  kms_ice_candidate_get_candidate (localCandidate),
                  kms_ice_candidate_get_candidate (remoteCandidate) );
  key = std::string (streamId) + "_" + std::to_string (componentId);

  it = candidatePairs.find (key);

  if (it != candidatePairs.end() ) {
    candidatePairs.erase (it);
  }

  candidatePairs.insert (std::pair
                         <std::string, std::shared_ptr <IceCandidatePair>> (key, candidatePair) );

  try {
    NewCandidatePairSelected event (shared_from_this (),
        NewCandidatePairSelected::getName (), candidatePair);
    sigcSignalEmit(signalNewCandidatePairSelected, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s",
        NewCandidatePairSelected::getName ().c_str (), e.what ());
  }
}

void
WebRtcEndpointImpl::onDataChannelOpened (gchar *sessId, guint stream_id)
{
  try {
    DataChannelOpened event (shared_from_this (), DataChannelOpened::getName (),
        stream_id);
    sigcSignalEmit(signalDataChannelOpened, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", DataChannelOpened::getName ().c_str (),
        e.what ());
  }

  try {
    DataChannelOpened event (shared_from_this (), DataChannelOpened::getName (),
        stream_id);
    sigcSignalEmit(signalDataChannelOpened, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", DataChannelOpened::getName ().c_str (),
        e.what ());
  }
}

void
WebRtcEndpointImpl::onDataChannelClosed (gchar *sessId, guint stream_id)
{
  try {
    DataChannelClosed event (shared_from_this (), DataChannelClosed::getName (),
        stream_id);
    sigcSignalEmit(signalDataChannelClosed, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", DataChannelClosed::getName ().c_str (),
        e.what ());
  }

  try {
    DataChannelClosed event (shared_from_this (), DataChannelClosed::getName (),
        stream_id);
    sigcSignalEmit(signalDataChannelClosed, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s", DataChannelClosed::getName ().c_str (),
        e.what ());
  }
}

void WebRtcEndpointImpl::postConstructor ()
{
  BaseRtpEndpointImpl::postConstructor ();

  handlerOnIceCandidate = register_signal_handler (G_OBJECT (element),
                          "on-ice-candidate",
                          std::function <void (GstElement *, gchar *, KmsIceCandidate *) >
                          (std::bind (&WebRtcEndpointImpl::onIceCandidate, this,
                                      std::placeholders::_2, std::placeholders::_3) ),
                          std::dynamic_pointer_cast<WebRtcEndpointImpl>
                          (shared_from_this() ) );

  handlerOnIceGatheringDone = register_signal_handler (G_OBJECT (element),
                              "on-ice-gathering-done",
                              std::function <void (GstElement *, gchar *) >
                              (std::bind (&WebRtcEndpointImpl::onIceGatheringDone, this,
                                          std::placeholders::_2) ),
                              std::dynamic_pointer_cast<WebRtcEndpointImpl>
                              (shared_from_this() ) );

  handlerOnIceComponentStateChanged = register_signal_handler (G_OBJECT (element),
                                      "on-ice-component-state-changed",
                                      std::function <void (GstElement *, gchar *, gchar *, guint, guint) >
                                      (std::bind (&WebRtcEndpointImpl::onIceComponentStateChanged, this,
                                          std::placeholders::_2, std::placeholders::_3, std::placeholders::_4,
                                          std::placeholders::_5) ),
                                      std::dynamic_pointer_cast<WebRtcEndpointImpl>
                                      (shared_from_this() ) );

  handlerOnDtlsConnectionStateChanged = register_signal_handler (G_OBJECT (element),
                                      "on-dtls-connection-state-changed",
                                      std::function <void (GstElement *, gchar *, gchar *, gchar *, char *,guint) >
                                      (std::bind (&WebRtcEndpointImpl::onDtlsConnectionStateChanged, this,
                                          std::placeholders::_2, std::placeholders::_3, std::placeholders::_4,
                                          std::placeholders::_5, std::placeholders::_6) ),
                                      std::dynamic_pointer_cast<WebRtcEndpointImpl>
                                      (shared_from_this() ) );

  handlerNewSelectedPairFull = register_signal_handler (G_OBJECT (element),
                               "new-selected-pair-full",
                               std::function
                               <void (GstElement *, gchar *, gchar *, guint, KmsIceCandidate *, KmsIceCandidate *) >
                               (std::bind (&WebRtcEndpointImpl::newSelectedPairFull, this,
                                   std::placeholders::_2, std::placeholders::_3, std::placeholders::_4,
                                   std::placeholders::_5, std::placeholders::_6) ),
                               std::dynamic_pointer_cast<WebRtcEndpointImpl>
                               (shared_from_this() ) );

  handlerOnDataChannelOpened = register_signal_handler (G_OBJECT (element),
                               "data-channel-opened",
                               std::function <void (GstElement *, gchar *, guint) >
                               (std::bind (&WebRtcEndpointImpl::onDataChannelOpened, this,
                                   std::placeholders::_2, std::placeholders::_3) ),
                               std::dynamic_pointer_cast<WebRtcEndpointImpl>
                               (shared_from_this() ) );

  handlerOnDataChannelClosed = register_signal_handler (G_OBJECT (element),
                               "data-channel-closed",
                               std::function <void (GstElement *, gchar *, guint) >
                               (std::bind (&WebRtcEndpointImpl::onDataChannelClosed, this,
                                   std::placeholders::_2, std::placeholders::_3) ),
                               std::dynamic_pointer_cast<WebRtcEndpointImpl>
                               (shared_from_this() ) );
}

std::string
WebRtcEndpointImpl::getCerficateFromFile (std::string &path)
{
  std::ifstream inFile;
  std::stringstream strStream;
  std::string certificate;

  //check if the uri is absolute or relative
  checkUri (path);
  GST_INFO ("pemCertificate in: %s\n", path.c_str() );

  inFile.open (path);
  strStream << inFile.rdbuf();
  certificate = strStream.str();

  if (!CertificateManager::isCertificateValid (certificate) ) {
    GST_ERROR ("Certificate in file %s is not valid", path.c_str () );
    return "";
  }

  return certificate;
}

static gint
get_dscp_value (std::shared_ptr<DSCPValue> qosDscp)
{
  switch (qosDscp->getValue ())
  {
  case DSCPValue::CS0:
    return 0;
  case DSCPValue::CS1:
    return 8;
  case DSCPValue::CS2:
    return 16;
  case DSCPValue::CS3:
    return 24;
  case DSCPValue::CS4:
    return 32;
  case DSCPValue::CS5:
    return 40;
  case DSCPValue::CS6:
    return 48;
  case DSCPValue::CS7:
    return 56;
  case DSCPValue::AF11:
    return 10;
  case DSCPValue::AF12:
    return 12;
  case DSCPValue::AF13:
    return 14;
  case DSCPValue::AF21:
    return 18;
  case DSCPValue::AF22:
    return 20;
  case DSCPValue::AF23:
    return 22;
  case DSCPValue::AF31:
    return 26;
  case DSCPValue::AF32:
    return 28;
  case DSCPValue::AF33:
    return 30;
  case DSCPValue::AF41:
    return 34;
  case DSCPValue::AF42:
    return 36;
  case DSCPValue::AF43:
    return 38;
  case DSCPValue::EF:
    return 46;
  case DSCPValue::VOICEADMIT:
    return 44;
  case DSCPValue::LE:
    return 1;
  case DSCPValue::AUDIO_VERYLOW:
    return 1;
  case DSCPValue::AUDIO_LOW:
    return 0;
  case DSCPValue::AUDIO_MEDIUM:
    return 46;
  case DSCPValue::AUDIO_HIGH:
    return 46;
  case DSCPValue::VIDEO_VERYLOW:
    return 1;
  case DSCPValue::VIDEO_LOW:
    return 0;
  case DSCPValue::VIDEO_MEDIUM:
    return 36;
  case DSCPValue::VIDEO_MEDIUM_THROUGHPUT:
    return 38;
  case DSCPValue::VIDEO_HIGH:
    return 36;
  case DSCPValue::VIDEO_HIGH_THROUGHPUT:
    return 34;
  case DSCPValue::DATA_VERYLOW:
    return 1;
  case DSCPValue::DATA_LOW:
    return 0;
  case DSCPValue::DATA_MEDIUM:
    return 10;
  case DSCPValue::DATA_HIGH:
    return 18;
  case DSCPValue::CHROME_HIGH:
    return 56;
  case DSCPValue::CHROME_MEDIUM:
    return 56;
  case DSCPValue::CHROME_LOW:
    return 0;
  case DSCPValue::CHROME_VERYLOW:
    return 8;
  default:
    return -1;
  }
}


WebRtcEndpointImpl::WebRtcEndpointImpl (const boost::property_tree::ptree &conf,
                                        std::shared_ptr<MediaPipeline>
                                        mediaPipeline, bool recvonly,
                                        bool sendonly, bool useDataChannels,
                                        std::shared_ptr<CertificateKeyType> certificateKeyType, 
                                        std::shared_ptr<DSCPValue> qosDscp) :
  BaseRtpEndpointImpl (conf,
                       std::dynamic_pointer_cast<MediaObjectImpl>
                       (mediaPipeline), FACTORY_NAME)
{
  std::call_once (check_openh264, check_support_for_h264);
  std::call_once (certificates_flag,
                  std::bind (&WebRtcEndpointImpl::generateDefaultCertificates, this) );

  std::string enableH265;

  if (getConfigValue<std::string,WebRtcEndpoint>(&enableH265, PARAM_ENABLE_H265)) {
    GST_INFO ("ENABLE-H265 configured value is %s", enableH265.c_str());
    if (enableH265 == "true") {
      add_support_for_h265 ();
    }
  }

  this->qosDscp = qosDscp;
  if (qosDscp->getValue () == DSCPValue::NO_VALUE) {
    std::string cfg_dscp_value;
    if (getConfigValue<std::string,WebRtcEndpoint>(&cfg_dscp_value, PARAM_QOS_DSCP)) {
      GST_INFO ("QOS-DSCP default configured value is %s", cfg_dscp_value.c_str());
      qosDscp = std::make_shared<DSCPValue> (cfg_dscp_value);
    }
  }

  if ((qosDscp->getValue () != DSCPValue::NO_VALUE) && (qosDscp->getValue() != DSCPValue::NO_DSCP)) {
    GST_INFO ("Setting QOS-DSCP value to %s", qosDscp->getString().c_str());
    g_object_set (element, "qos-dscp", get_dscp_value (qosDscp), NULL);
  } else {
    GST_INFO ("No QOS-DSCP value set");
  }


  if (recvonly) {
    g_object_set (element, "offer-dir", GST_SDP_DIRECTION_RECVONLY, NULL);
  }

  if (sendonly) {
    g_object_set (element, "offer-dir", GST_SDP_DIRECTION_SENDONLY, NULL);
  }

  if (useDataChannels) {
    g_object_set (element, "use-data-channels", TRUE, NULL);
  }

  remove_not_supported_codecs (element);

  //set properties

  std::string externalIPv4;
  if (getConfigValue <std::string, WebRtcEndpoint> (&externalIPv4,
      PARAM_EXTERNAL_IPV4)) {
    GST_INFO ("Predefined external IPv4 address: %s", externalIPv4.c_str());
    g_object_set (G_OBJECT (element), PROP_EXTERNAL_IPV4,
        externalIPv4.c_str(), NULL);
  } else {
    GST_DEBUG ("No predefined external IPv4 address found in config;"
               " you can set one or default to STUN automatic discovery");
  }

  std::string externalIPv6;
  if (getConfigValue <std::string, WebRtcEndpoint> (&externalIPv6,
      PARAM_EXTERNAL_IPV6)) {
    GST_INFO ("Predefined external IPv6 address: %s", externalIPv6.c_str());
    g_object_set (G_OBJECT (element), PROP_EXTERNAL_IPV6,
        externalIPv6.c_str(), NULL);
  } else {
    GST_DEBUG ("No predefined external IPv6 address found in config;"
               " you can set one or default to STUN automatic discovery");
  }

  std::string networkInterfaces;
  if (getConfigValue <std::string, WebRtcEndpoint> (&networkInterfaces,
      PARAM_NETWORK_INTERFACES)) {
    GST_INFO ("Predefined network interfaces: %s", networkInterfaces.c_str());
    g_object_set (G_OBJECT (element), PROP_NETWORK_INTERFACES,
        networkInterfaces.c_str(), NULL);
  } else {
    GST_DEBUG ("No predefined network interfaces found in config;"
               " you can set one or default to ICE automatic discovery");
  }

  gboolean iceTcp;
  if (getConfigValue<gboolean, WebRtcEndpoint> (&iceTcp, PARAM_ICE_TCP)) {
    GST_INFO ("ICE-TCP candidate gathering is %s",
        iceTcp ? "ENABLED" : "DISABLED");
    g_object_set (G_OBJECT (element), PROP_ICE_TCP, iceTcp, NULL);
  } else {
    GST_DEBUG ("ICE-TCP option not found in config;"
               " you can set it or default to 1 (TRUE)");
  }

  uint stunPort = 0;
  if (!getConfigValue <uint, WebRtcEndpoint> (&stunPort, "stunServerPort",
      DEFAULT_STUN_PORT) ) {
    GST_DEBUG ("STUN port not found in config;"
               " using default value: %d", DEFAULT_STUN_PORT);
  }

  std::string stunAddress;
  if (getConfigValue<std::string, WebRtcEndpoint> (&stunAddress,
      "stunServerAddress")) {
    GST_INFO ("Predefined STUN server: %s:%d", stunAddress.c_str (), stunPort);

    g_object_set (G_OBJECT (element), "stun-server-port", stunPort, NULL);
    g_object_set (G_OBJECT (element), "stun-server", stunAddress.c_str (),
        NULL);
  } else {
    GST_DEBUG ("STUN server not found in config");
  }

  std::string turnURL;
  if (getConfigValue <std::string, WebRtcEndpoint> (&turnURL, "turnURL")) {
    std::string safeURL = "<user:password>";
    size_t separatorPos = turnURL.find_last_of('@');
    if (separatorPos == std::string::npos) {
      safeURL.append("@").append(turnURL);
    } else {
      safeURL.append(turnURL.substr(separatorPos));
    }
    GST_INFO ("Predefined TURN relay server: %s", safeURL.c_str());

    g_object_set (G_OBJECT (element), "turn-url", turnURL.c_str(), NULL);
  } else {
    GST_DEBUG ("TURN relay server not found in config");
  }

  switch (certificateKeyType->getValue () ) {
  case CertificateKeyType::RSA: {
    if (defaultCertificateRSA != "") {
      g_object_set ( G_OBJECT (element), "pem-certificate",
                     defaultCertificateRSA.c_str(),
                     NULL);
    }

    break;
  }

  case CertificateKeyType::ECDSA: {
    if (defaultCertificateECDSA != "") {
      g_object_set ( G_OBJECT (element), "pem-certificate",
                     defaultCertificateECDSA.c_str(),
                     NULL);
    }

    break;
  }

  default:
    GST_ERROR ("Certificate key not supported");
  }
}

WebRtcEndpointImpl::~WebRtcEndpointImpl()
{
  if (handlerOnIceCandidate > 0) {
    unregister_signal_handler (element, handlerOnIceCandidate);
  }

  if (handlerOnIceGatheringDone > 0) {
    unregister_signal_handler (element, handlerOnIceGatheringDone);
  }

  if (handlerOnIceComponentStateChanged > 0) {
    unregister_signal_handler (element, handlerOnIceComponentStateChanged);
  }

  if (handlerOnDataChannelOpened > 0) {
    unregister_signal_handler (element, handlerOnDataChannelOpened);
  }

  if (handlerOnDataChannelClosed > 0) {
    unregister_signal_handler (element, handlerOnDataChannelClosed);
  }

  if (handlerNewSelectedPairFull > 0) {
    unregister_signal_handler (element, handlerNewSelectedPairFull);
  }
}

std::string
WebRtcEndpointImpl::getExternalIPv4 ()
{
  std::string externalIPv4;
  gchar *ret;

  g_object_get (G_OBJECT (element), PROP_EXTERNAL_IPV4, &ret, NULL);

  if (ret != nullptr) {
    externalIPv4 = std::string (ret);
    g_free (ret);
  }

  return externalIPv4;
}

void
WebRtcEndpointImpl::setExternalIPv4 (const std::string &externalIPv4)
{
  GST_INFO ("Set external IPv4 address: %s", externalIPv4.c_str());
  g_object_set (G_OBJECT (element), PROP_EXTERNAL_IPV4,
      externalIPv4.c_str(), NULL);
}

std::string
WebRtcEndpointImpl::getExternalIPv6 ()
{
  std::string externalIPv6;
  gchar *ret;

  g_object_get (G_OBJECT (element), PROP_EXTERNAL_IPV6, &ret, NULL);

  if (ret != nullptr) {
    externalIPv6 = std::string (ret);
    g_free (ret);
  }

  return externalIPv6;
}

void
WebRtcEndpointImpl::setExternalIPv6 (const std::string &externalIPv6)
{
  GST_INFO ("Set external IPv6 address: %s", externalIPv6.c_str());
  g_object_set (G_OBJECT (element), PROP_EXTERNAL_IPV6,
      externalIPv6.c_str(), NULL);
}

std::string
WebRtcEndpointImpl::getNetworkInterfaces ()
{
  std::string networkInterfaces;
  gchar *ret;

  g_object_get (G_OBJECT (element), PROP_NETWORK_INTERFACES, &ret, NULL);

  if (ret != nullptr) {
    networkInterfaces = std::string (ret);
    g_free (ret);
  }

  return networkInterfaces;
}

void
WebRtcEndpointImpl::setNetworkInterfaces (const std::string &networkInterfaces)
{
  GST_INFO ("Set network interfaces: %s", networkInterfaces.c_str());
  g_object_set (G_OBJECT (element), PROP_NETWORK_INTERFACES,
      networkInterfaces.c_str(), NULL);
}

bool
WebRtcEndpointImpl::getIceTcp ()
{
  bool ret;

  g_object_get (G_OBJECT (element), "ice-tcp", &ret, NULL);

  return ret;
}

void
WebRtcEndpointImpl::setIceTcp (bool iceTcp)
{
  g_object_set (G_OBJECT (element), "ice-tcp", iceTcp, NULL);
}

std::string
WebRtcEndpointImpl::getStunServerAddress ()
{
  std::string stunServerAddress;
  gchar *ret;

  g_object_get ( G_OBJECT (element), "stun-server", &ret, NULL);

  if (ret != nullptr) {
    stunServerAddress = std::string (ret);
    g_free (ret);
  }

  return stunServerAddress;
}

void
WebRtcEndpointImpl::setStunServerAddress (const std::string &stunServerAddress)
{
  g_object_set ( G_OBJECT (element), "stun-server",
                 stunServerAddress.c_str(),
                 NULL);
}

int
WebRtcEndpointImpl::getStunServerPort ()
{
  int ret;

  g_object_get ( G_OBJECT (element), "stun-server-port", &ret, NULL);

  return ret;
}

void
WebRtcEndpointImpl::setStunServerPort (int stunServerPort)
{
  g_object_set ( G_OBJECT (element), "stun-server-port",
                 stunServerPort, NULL);
}

std::string
WebRtcEndpointImpl::getTurnUrl ()
{
  std::string turnUrl;
  gchar *ret;

  g_object_get ( G_OBJECT (element), "turn-url", &ret, NULL);

  if (ret != nullptr) {
    turnUrl = std::string (ret);
    g_free (ret);
  }

  return turnUrl;
}

void
WebRtcEndpointImpl::setTurnUrl (const std::string &turnUrl)
{
  g_object_set ( G_OBJECT (element), "turn-url",
                 turnUrl.c_str(),
                 NULL);
}

std::vector<std::shared_ptr<IceCandidatePair>>
    WebRtcEndpointImpl::getICECandidatePairs ()
{
  std::vector<std::shared_ptr<IceCandidatePair>> candidates;
  std::map<std::string, std::shared_ptr <IceCandidatePair>>::iterator it;

  for (it = candidatePairs.begin(); it != candidatePairs.end(); it++) {
    candidates.push_back ( (*it).second);
  }

  return candidates;
}

std::vector<std::shared_ptr<IceConnection>>
    WebRtcEndpointImpl::getIceConnectionState ()
{
  std::vector<std::shared_ptr<IceConnection>> connections;
  std::map<std::string, std::shared_ptr <IceConnection>>::iterator it;
  std::unique_lock<std::mutex> mutex (mut);

  for (it = iceConnectionState.begin(); it != iceConnectionState.end(); it++) {
    connections.push_back ( (*it).second);
  }

  return connections;
}

std::vector<std::shared_ptr<DtlsConnection>>
    WebRtcEndpointImpl::getDtlsConnectionState ()
{
  std::vector<std::shared_ptr<DtlsConnection>> connections;
  std::map<std::string, std::shared_ptr <DtlsConnection>>::iterator it;
  std::unique_lock<std::mutex> mutex (mut);

  for (it = dtlsConnectionState.begin(); it != dtlsConnectionState.end(); it++) {
    connections.push_back ( (*it).second);
  }

  return connections;
}

void
WebRtcEndpointImpl::gatherCandidates ()
{
  gboolean ret;

  g_signal_emit_by_name (element, "gather-candidates", this->sessId.c_str (),
                         &ret);

  if (!ret) {
    throw KurentoException (ICE_GATHER_CANDIDATES_ERROR,
                            "Error gathering candidates");
  }
}

void
WebRtcEndpointImpl::addIceCandidate (std::shared_ptr<IceCandidate> candidate)
{
  gboolean ret = FALSE;
  std::string cand_str = candidate->getCandidate();
  std::string mid_str = candidate->getSdpMid ();
  guint8 sdp_m_line_index = candidate->getSdpMLineIndex ();

  if (cand_str.empty()) {
    // This is an end-of-candidates notification, part of Trickle ICE.
    // Just ignore it.
    return;
  }

  KmsIceCandidate *cand = kms_ice_candidate_new(
      cand_str.c_str(), mid_str.c_str(), sdp_m_line_index, nullptr);

  if (cand) {
    g_signal_emit_by_name (element, "add-ice-candidate", this->sessId.c_str (),
                           cand, &ret);
    g_object_unref (cand);

    if (!ret) {
      throw KurentoException (ICE_ADD_CANDIDATE_ERROR, "Error adding candidate");
    }
  }
}

void
WebRtcEndpointImpl::createDataChannel ()
{
  createDataChannel ("", true, -1, -1, "");
}

void
WebRtcEndpointImpl::createDataChannel (const std::string &label)
{
  createDataChannel (label, true, -1, -1, "");
}

void
WebRtcEndpointImpl::createDataChannel (const std::string &label, bool ordered)
{
  createDataChannel (label, ordered, -1, -1, "");
}

void
WebRtcEndpointImpl::createDataChannel (const std::string &label, bool ordered,
                                       int maxPacketLifeTime)
{
  createDataChannel (label, ordered, maxPacketLifeTime, -1, "");
}

void
WebRtcEndpointImpl::createDataChannel (const std::string &label, bool ordered,
                                       int maxPacketLifeTime, int maxRetransmits)
{
  createDataChannel (label, ordered, maxPacketLifeTime, maxRetransmits, "");
}

void
WebRtcEndpointImpl::createDataChannel (const std::string &label, bool ordered,
                                       int maxPacketLifeTime, int maxRetransmits, const std::string &protocol)
{
  gint lifeTime, retransmits, stream_id;
  gboolean supported;

  g_signal_emit_by_name (element, "get-data-channel-supported",
                         this->sessId.c_str (), &supported);

  if (!supported) {
    throw KurentoException (MEDIA_OBJECT_OPERATION_NOT_SUPPORTED,
                            "Data channels are not supported");
  }

  /* Less than one values mean that parameters are disabled */
  if (maxPacketLifeTime < 0) {
    maxPacketLifeTime = -1;
  }

  if (maxRetransmits < 0) {
    maxRetransmits = -1;
  }

  if (maxPacketLifeTime != -1 && maxRetransmits != -1) {
    /* Both values are incompatible.                                     */
    /* http://w3c.github.io/webrtc-pc/#dom-datachannel-maxpacketlifetime */
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR, "Syntax error");
  }

  if (maxPacketLifeTime > G_MAXUSHORT) {
    GST_WARNING ("maxPacketLifeTime can not be bigger than %u. Setting it to that value",
                 G_MAXUSHORT);
    lifeTime = G_MAXUSHORT;
  } else {
    lifeTime = maxPacketLifeTime;
  }

  if (maxRetransmits > G_MAXUSHORT) {
    GST_WARNING ("maxRetransmits can not be bigger than %u. Setting it to that value",
                 G_MAXUSHORT);
    retransmits = G_MAXUSHORT;
  } else {
    retransmits = maxRetransmits;
  }

  /* Create the data channel */
  g_signal_emit_by_name (element, "create-data-channel", this->sessId.c_str (),
                         ordered, lifeTime,
                         retransmits, label.c_str(), protocol.c_str(),
                         &stream_id);

  if (stream_id < 0) {
    throw KurentoException (UNEXPECTED_ERROR, "Can not create data channel");
  }

  GST_DEBUG ("Creating data channel with stream id %d", stream_id);
}

void
WebRtcEndpointImpl::closeDataChannel (int channelId)
{
  gboolean supported;

  g_signal_emit_by_name (element, "get-data-channel-supported",
                         this->sessId.c_str (), &supported);

  if (!supported) {
    throw KurentoException (MEDIA_OBJECT_OPERATION_NOT_SUPPORTED,
                            "Data channels are not supported");
  }

  /* Destroy the data channel */
  g_signal_emit_by_name (element, "destroy-data-channel", this->sessId.c_str (),
                         channelId);
}

static std::shared_ptr<RTCDataChannelState>
getRTCDataChannelState (KmsWebRtcDataChannelState state)
{
  std::shared_ptr<RTCDataChannelState> rtcDataChannelState;

  switch (state) {
  case KMS_WEB_RTC_DATA_CHANNEL_STATE_CONNECTING:
    rtcDataChannelState =  std::make_shared <RTCDataChannelState>
                           (RTCDataChannelState::connecting);
    break;

  case KMS_WEB_RTC_DATA_CHANNEL_STATE_OPEN:
    rtcDataChannelState = std::make_shared <RTCDataChannelState>
                          (RTCDataChannelState::open);
    break;

  case KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSING:
    rtcDataChannelState = std::make_shared <RTCDataChannelState>
                          (RTCDataChannelState::closing);
    break;

  case KMS_WEB_RTC_DATA_CHANNEL_STATE_CLOSED:
    rtcDataChannelState = std::make_shared <RTCDataChannelState>
                          (RTCDataChannelState::closed);
    break;
  }

  return rtcDataChannelState;
}

static std::shared_ptr<RTCDataChannelStats>
createtRTCDataChannelStats (const GstStructure *stats)
{
  KmsWebRtcDataChannelState state;
  guint64 messages_sent, message_recv, bytes_sent, bytes_recv;
  gchar *id, *label, *protocol;
  guint channelid;

  gst_structure_get (stats, "channel-id", G_TYPE_UINT, &channelid, "label",
                     G_TYPE_STRING, &label, "protocol", G_TYPE_STRING, &protocol, "id",
                     G_TYPE_STRING, &id, "state", G_TYPE_UINT, &state, "bytes-sent",
                     G_TYPE_UINT64, &bytes_sent, "bytes-recv", G_TYPE_UINT64,
                     &bytes_recv, "messages-sent", G_TYPE_UINT64, &messages_sent,
                     "messages-recv", G_TYPE_UINT64, &message_recv, NULL);

  std::shared_ptr<RTCDataChannelStats> rtcDataStats =
    std::make_shared <RTCDataChannelStats> (id,
        std::make_shared <StatsType> (StatsType::datachannel), 0, label,
        protocol, channelid, getRTCDataChannelState (state), messages_sent,
        bytes_sent, message_recv, bytes_recv);

  g_free (protocol);
  g_free (label);
  g_free (id);

  return rtcDataStats;
}

static std::shared_ptr<RTCPeerConnectionStats>
createtRTCPeerConnectionStats (const GstStructure *stats)
{
  guint opened, closed;
  gchar *id;

  gst_structure_get (stats, "data-channels-opened", G_TYPE_UINT, &opened,
                     "data-channels-closed", G_TYPE_UINT, &closed,
                     "id", G_TYPE_STRING, &id, NULL);

  std::shared_ptr<RTCPeerConnectionStats> peerConnStats =
    std::make_shared <RTCPeerConnectionStats> (id,
        std::make_shared <StatsType> (StatsType::session), 0, opened, closed);
  g_free (id);

  return peerConnStats;
}

static void
collectRTCDataChannelStats (std::map <std::string, std::shared_ptr<Stats>>
                            &statsReport, int64_t timestampMillis,
                            const GstStructure *stats)
{
  gint i, n;

  n = gst_structure_n_fields (stats);

  for (i = 0; i < n; i++) {
    std::shared_ptr<RTCDataChannelStats> rtcDataStats;
    const GValue *value;
    const gchar *name;

    name = gst_structure_nth_field_name (stats, i);

    if (!g_str_has_prefix (name, "data-channel-") ) {
      continue;
    }

    value = gst_structure_get_value (stats, name);

    if (!GST_VALUE_HOLDS_STRUCTURE (value) ) {
      gchar *str_val;

      str_val = g_strdup_value_contents (value);
      GST_WARNING ("Unexpected field type (%s) = %s", name, str_val);
      g_free (str_val);

      continue;
    }

    rtcDataStats = createtRTCDataChannelStats (gst_value_get_structure (value) );
    rtcDataStats->setTimestampMillis (timestampMillis);
    statsReport[rtcDataStats->getId ()] = rtcDataStats;
  }

  std::shared_ptr<RTCPeerConnectionStats> peerConnStats =
    createtRTCPeerConnectionStats (stats);
  peerConnStats->setTimestampMillis (timestampMillis);
  statsReport[peerConnStats->getId ()] = peerConnStats;
}

void
WebRtcEndpointImpl::fillStatsReport (std::map
                                     <std::string, std::shared_ptr<Stats>>
                                     &report, const GstStructure *stats,
                                     int64_t timestampMillis)
{
  const GstStructure *data_stats = nullptr;

  BaseRtpEndpointImpl::fillStatsReport (report, stats, timestampMillis);

  data_stats = kms_utils_get_structure_by_name (stats,
               KMS_DATA_SESSION_STATISTICS_FIELD);

  if (data_stats != nullptr) {
    return collectRTCDataChannelStats (report, timestampMillis, data_stats);
  }
}

MediaObjectImpl *
WebRtcEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, bool recvonly, bool sendonly, bool useDataChannels,
    std::shared_ptr<CertificateKeyType> certificateKeyType, 
    std::shared_ptr<DSCPValue> qosDscp) const
{
  return new WebRtcEndpointImpl (conf, mediaPipeline, recvonly,
                                 sendonly, useDataChannels,
                                 certificateKeyType, qosDscp);
}

WebRtcEndpointImpl::StaticConstructor WebRtcEndpointImpl::staticConstructor;

WebRtcEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
