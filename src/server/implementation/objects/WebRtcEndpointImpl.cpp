#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <WebRtcEndpointImplFactory.hpp>
#include "WebRtcEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <boost/filesystem.hpp>
#include <IceCandidate.hpp>
#include <webrtcendpoint/kmsicecandidate.h>
#include <IceComponentState.hpp>
#include <SignalHandler.hpp>

#define GST_CAT_DEFAULT kurento_web_rtc_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoWebRtcEndpointImpl"

#define FACTORY_NAME "webrtcendpoint"

namespace kurento
{

static const uint DEFAULT_STUN_PORT = 3478;

std::vector<std::string> supported_codecs = { "VP8", "opus", "PCMU" };

static void
remove_not_supported_codecs_from_array (GstElement *element, GArray *codecs)
{
  guint i;

  if (codecs == NULL) {
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

    for (std::vector<std::string>::iterator it = supported_codecs.begin();
         it != supported_codecs.end(); ++it) {
      if (g_str_has_prefix (codec_name, (*it).c_str() ) ) {
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
check_support_for_h264 ()
{
  GstPlugin *plugin;

  plugin = gst_plugin_load_by_name ("openh264");

  if (plugin == NULL) {
    return;
  }

  supported_codecs.push_back ("H264");
  gst_object_unref (plugin);
}

void WebRtcEndpointImpl::onIceCandidate (gchar *sessId,
    KmsIceCandidate *candidate)
{
  try {
    std::string cand_str (kms_ice_candidate_get_candidate (candidate) );
    std::string mid_str (kms_ice_candidate_get_sdp_mid (candidate) );
    int sdp_m_line_index = kms_ice_candidate_get_sdp_m_line_index (candidate);
    std::shared_ptr <IceCandidate> cand ( new  IceCandidate
                                          (cand_str, mid_str, sdp_m_line_index) );
    OnIceCandidate event (shared_from_this(), OnIceCandidate::getName(), cand);

    signalOnIceCandidate (event);
  } catch (std::bad_weak_ptr &e) {
  }
}

void WebRtcEndpointImpl::onIceGatheringDone (gchar *sessId)
{
  try {
    OnIceGatheringDone event (shared_from_this(), OnIceGatheringDone::getName() );

    signalOnIceGatheringDone (event);
  } catch (std::bad_weak_ptr &e) {
  }
}

void WebRtcEndpointImpl::onIceComponentStateChanged (gchar *sessId,
    guint streamId,
    guint componentId, guint state)
{
  try {
    IceComponentState::type type;

    switch (state) {
    case NICE_COMPONENT_STATE_DISCONNECTED:
      type = IceComponentState::DISCONNECTED;
      break;

    case NICE_COMPONENT_STATE_GATHERING:
      type = IceComponentState::GATHERING;
      break;

    case NICE_COMPONENT_STATE_CONNECTING:
      type = IceComponentState::CONNECTING;
      break;

    case NICE_COMPONENT_STATE_CONNECTED:
      type = IceComponentState::CONNECTED;
      break;

    case NICE_COMPONENT_STATE_READY:
      type = IceComponentState::READY;
      break;

    case NICE_COMPONENT_STATE_FAILED:
      type = IceComponentState::FAILED;
      break;

    default:
      type = IceComponentState::FAILED;
      break;
    }

    IceComponentState *componentState = new IceComponentState (type);
    OnIceComponentStateChanged event (shared_from_this(),
                                      OnIceComponentStateChanged::getName(),
                                      streamId, componentId,  std::shared_ptr<IceComponentState> (componentState) );

    signalOnIceComponentStateChanged (event);
  } catch (std::bad_weak_ptr &e) {
  }
}

void
WebRtcEndpointImpl::onDataChannelOpened (guint stream_id)
{
  try {
    OnDataChannelOpened event (shared_from_this(), OnDataChannelOpened::getName(),
                               stream_id);
    signalOnDataChannelOpened (event);
  } catch (std::bad_weak_ptr &e) {
  }
}

void
WebRtcEndpointImpl::onDataChannelClosed (guint stream_id)
{
  try {
    OnDataChannelClosed event (shared_from_this(), OnDataChannelClosed::getName(),
                               stream_id);
    signalOnDataChannelClosed (event);
  } catch (std::bad_weak_ptr &e) {
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
                                      std::function <void (GstElement *, gchar *, guint, guint, guint) >
                                      (std::bind (&WebRtcEndpointImpl::onIceComponentStateChanged, this,
                                          std::placeholders::_2, std::placeholders::_3, std::placeholders::_4,
                                          std::placeholders::_5) ),
                                      std::dynamic_pointer_cast<WebRtcEndpointImpl>
                                      (shared_from_this() ) );

  handlerOnDataChannelOpened = register_signal_handler (G_OBJECT (element),
                               "data-channel-opened",
                               std::function <void (GstElement *, guint) >
                               (std::bind (&WebRtcEndpointImpl::onDataChannelOpened, this,
                                   std::placeholders::_2) ),
                               std::dynamic_pointer_cast<WebRtcEndpointImpl>
                               (shared_from_this() ) );

  handlerOnDataChannelClosed = register_signal_handler (G_OBJECT (element),
                               "data-channel-closed",
                               std::function <void (GstElement *, guint) >
                               (std::bind (&WebRtcEndpointImpl::onDataChannelClosed, this,
                                   std::placeholders::_2) ),
                               std::dynamic_pointer_cast<WebRtcEndpointImpl>
                               (shared_from_this() ) );
}

WebRtcEndpointImpl::WebRtcEndpointImpl (const boost::property_tree::ptree &conf,
                                        std::shared_ptr<MediaPipeline>
                                        mediaPipeline, bool useDataChannels) :
  BaseRtpEndpointImpl (conf,
                       std::dynamic_pointer_cast<MediaObjectImpl>
                       (mediaPipeline), FACTORY_NAME)
{
  uint stunPort;
  std::string stunAddress;
  std::string turnURL;

  if (useDataChannels) {
    g_object_set (element, "use-data-channels", TRUE, NULL);
  }

  remove_not_supported_codecs (element);

  //set properties
  try {
    stunPort = getConfigValue <uint, WebRtcEndpoint> ("stunServerPort");
  } catch (boost::property_tree::ptree_error &e) {
    GST_INFO ("Setting default port %d to stun server",
              DEFAULT_STUN_PORT);
    stunPort = DEFAULT_STUN_PORT;
  }

  if (stunPort != 0) {
    try {
      stunAddress = getConfigValue
                    <std::string, WebRtcEndpoint> ("stunServerAddress");
    } catch (boost::property_tree::ptree_error &e) {
      GST_INFO ("Stun address not found in config, cannot operate behind a NAT" );
    }

    if (!stunAddress.empty() ) {
      GST_INFO ("stun port %d\n", stunPort );
      g_object_set ( G_OBJECT (element), "stun-server-port",
                     stunPort, NULL);

      GST_INFO ("stun address %s\n", stunAddress.c_str() );
      g_object_set ( G_OBJECT (element), "stun-server",
                     stunAddress.c_str(),
                     NULL);
    }
  }

  try {
    turnURL = getConfigValue <std::string, WebRtcEndpoint> ("turnURL");
    GST_INFO ("turn info: %s\n", turnURL.c_str() );
    g_object_set ( G_OBJECT (element), "turn-url", turnURL.c_str(),
                   NULL);
  } catch (boost::property_tree::ptree_error &e) {

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
}

std::string
WebRtcEndpointImpl::getStunServerAddress ()
{
  gchar *ret;

  g_object_get ( G_OBJECT (element), "stun-server", &ret, NULL);

  std::string stunServerAddress (ret);
  g_free (ret);

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
  gchar *ret;

  g_object_get ( G_OBJECT (element), "turn-url", &ret, NULL);

  std::string turnUrl (ret);
  g_free (ret);

  return turnUrl;
}

void
WebRtcEndpointImpl::setTurnUrl (const std::string &turnUrl)
{
  g_object_set ( G_OBJECT (element), "turn-url",
                 turnUrl.c_str(),
                 NULL);
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
  gboolean ret;
  const char *cand_str = candidate->getCandidate().c_str ();
  const char *mid_str = candidate->getSdpMid().c_str ();
  guint8 sdp_m_line_index = candidate->getSdpMLineIndex ();
  KmsIceCandidate *cand = kms_ice_candidate_new (cand_str, mid_str,
                          sdp_m_line_index);

  g_signal_emit_by_name (element, "add-ice-candidate", this->sessId.c_str (),
                         cand, &ret);

  g_object_unref (cand);

  if (!ret) {
    throw KurentoException (ICE_ADD_CANDIDATE_ERROR, "Error adding candidate");
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

  g_object_get (element, "data-channel-supported", &supported, NULL);

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
  g_signal_emit_by_name (element, "create-data-channel", ordered, lifeTime,
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

  g_object_get (element, "data-channel-supported", &supported, NULL);

  if (!supported) {
    throw KurentoException (MEDIA_OBJECT_OPERATION_NOT_SUPPORTED,
                            "Data channels are not supported");
  }

  /* Destroy the data channel */
  g_signal_emit_by_name (element, "destroy-data-channel", channelId);
}

MediaObjectImpl *
WebRtcEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, bool useDataChannels) const
{
  return new WebRtcEndpointImpl (conf, mediaPipeline, useDataChannels);
}

WebRtcEndpointImpl::StaticConstructor WebRtcEndpointImpl::staticConstructor;

WebRtcEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);

  check_support_for_h264 ();
}

} /* kurento */
