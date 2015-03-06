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

#define GST_CAT_DEFAULT kurento_web_rtc_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoWebRtcEndpointImpl"

#define FACTORY_NAME "webrtcendpoint"

namespace kurento
{

static const std::string CERTTOOL_TEMPLATE = "autoCerttool.tmpl";
static const std::string CERT_KEY_PEM_FILE = "autoCertkey.pem";

static const uint DEFAULT_STUN_PORT = 3478;

static std::shared_ptr<std::string> pemCertificate;
std::mutex WebRtcEndpointImpl::certificateMutex;

static void
on_ice_candidate (GstElement *webrtcendpoint, KmsIceCandidate *candidate,
                  gpointer data)
{
  auto handler = reinterpret_cast<std::function<void (KmsIceCandidate *) >*>
                 (data);

  (*handler) (candidate);
}

static void
on_ice_gathering_done (GstElement *webrtcendpoint, gpointer data)
{
  auto handler = reinterpret_cast<std::function<void() >*> (data);

  (*handler) ();
}

static void
on_ice_component_state_changed (NiceAgent *agent, guint stream_id,
                                guint component_id, NiceComponentState state, gpointer data)
{
  auto handler =
    reinterpret_cast<std::function<void (guint streamId, guint componentId, guint state) >*>
    (data);

  (*handler) (stream_id, component_id, state);
}

class TemporalDirectory
{

public:
  ~TemporalDirectory() {
    if (!dir.string ().empty() ) {
      boost::filesystem::remove_all (dir);
    }
  }

  void setDir (boost::filesystem::path &dir) {
    this->dir = dir;
  }
private:
  boost::filesystem::path dir;
};

static TemporalDirectory tmpDir;

static void
create_pem_certificate ()
{
  int ret;
  boost::filesystem::path temporalDirectory = boost::filesystem::unique_path (
        boost::filesystem::temp_directory_path() / "WebRtcEndpoint_%%%%%%%%" );
  boost::filesystem::create_directories (temporalDirectory);
  tmpDir.setDir (temporalDirectory);

  boost::filesystem::path pemFile = temporalDirectory / CERT_KEY_PEM_FILE;
  std::string pemGenerationCommand =
    "/bin/sh -c \"certtool --generate-privkey --outfile " + pemFile .string()  +
    "\"";

  ret = system (pemGenerationCommand.c_str() );

  if (ret == -1) {
    return;
  }

  boost::filesystem::path templateFile = temporalDirectory / CERTTOOL_TEMPLATE;
  std::string certtoolCommand = "/bin/sh -c \"echo 'organization = kurento' > " +
                                templateFile.string() + " && certtool --generate-self-signed --load-privkey " +
                                pemFile.string() + " --template " + templateFile.string() +  " >> " +
                                pemFile.string() + " 2>/dev/null\"";

  ret = system (certtoolCommand.c_str() );

  if (ret == -1) {
    return;
  }

  pemCertificate = std::shared_ptr <std::string> (new std::string (
                     pemFile.string() ) );
}

std::shared_ptr<std::string>
WebRtcEndpointImpl::getPemCertificate ()
{
  std::unique_lock<std::mutex> lock (certificateMutex);

  if (pemCertificate) {
    return pemCertificate;
  }

  try {
    boost::filesystem::path pem_certificate_file_name (
      getConfigValue<std::string, WebRtcEndpoint> ("pemCertificate") );

    if (pem_certificate_file_name.is_relative() ) {
      pem_certificate_file_name = boost::filesystem::path (
                                    config.get<std::string> ("configPath") ) / pem_certificate_file_name;
    }

    pemCertificate = std::shared_ptr <std::string> (new std::string (
                       pem_certificate_file_name.string() ) );

    return pemCertificate;
  } catch (boost::property_tree::ptree_error &e) {

  }

  create_pem_certificate();

  return pemCertificate;
}


WebRtcEndpointImpl::WebRtcEndpointImpl (const boost::property_tree::ptree &conf,
                                        std::shared_ptr<MediaPipeline>
                                        mediaPipeline) : BaseRtpEndpointImpl (conf,
                                              std::dynamic_pointer_cast<MediaObjectImpl>
                                              (mediaPipeline), FACTORY_NAME)
{
  uint stunPort;
  std::string stunAddress;
  std::string turnURL;

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

  g_object_set ( G_OBJECT (element), "certificate-pem-file",
                 getPemCertificate ()->c_str(), NULL);

  onIceCandidateLambda = [&] (KmsIceCandidate * candidate) {
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
  };

  handlerOnIceCandidate = g_signal_connect (element, "on-ice-candidate",
                          G_CALLBACK (on_ice_candidate),
                          &onIceCandidateLambda);

  onIceGatheringDoneLambda = [&] () {
    try {
      OnIceGatheringDone event (shared_from_this(), OnIceGatheringDone::getName() );

      signalOnIceGatheringDone (event);
    } catch (std::bad_weak_ptr &e) {
    }
  };

  handlerOnIceGatheringDone = g_signal_connect (element, "on-ice-gathering-done",
                              G_CALLBACK (on_ice_gathering_done),
                              &onIceGatheringDoneLambda);

  onIceComponentStateChangedLambda = [&] (guint streamId, guint componentId,
  guint state) {
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
  };

  handlerOnIceComponentStateChanged = g_signal_connect (element,
                                      "on-ice-component-state-changed",
                                      G_CALLBACK (on_ice_component_state_changed),
                                      &onIceComponentStateChangedLambda);
}

WebRtcEndpointImpl::~WebRtcEndpointImpl()
{
  g_signal_handler_disconnect (element, handlerOnIceCandidate);
  g_signal_handler_disconnect (element, handlerOnIceGatheringDone);
  g_signal_handler_disconnect (element, handlerOnIceComponentStateChanged);
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

  g_signal_emit_by_name (element, "gather-candidates", &ret);

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

  g_signal_emit_by_name (element, "add-ice-candidate", cand, &ret);

  if (!ret) {
    throw KurentoException (ICE_ADD_CANDIDATE_ERROR, "Error adding candidate");
  }
}

MediaObjectImpl *
WebRtcEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline) const
{
  return new WebRtcEndpointImpl (conf, mediaPipeline);
}

WebRtcEndpointImpl::StaticConstructor WebRtcEndpointImpl::staticConstructor;

WebRtcEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
