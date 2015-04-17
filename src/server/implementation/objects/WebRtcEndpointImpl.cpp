#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <WebRtcEndpointImplFactory.hpp>
#include "WebRtcEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <boost/filesystem.hpp>

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

class TemporalDirectory
{
public:
  ~TemporalDirectory()
  {
    if (!dir.string ().empty() ) {
      boost::filesystem::remove_all (dir);
    }
  }

  void setDir (boost::filesystem::path &dir)
  {
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

static const std::string supported_codecs[] = {"VP8", "OPUS", "PCMU"};
static const int n_codecs = sizeof (supported_codecs) / sizeof (std::string);

static void
remove_not_supported_codecs_from_pattern (GstElement *element)
{

  GstSDPMessage *old_pattern = NULL, *pattern;
  guint medias_len, i;

  g_object_get (element, "pattern-sdp", &old_pattern, NULL);

  if (old_pattern == NULL) {
    return;
  }

  if (gst_sdp_message_copy (old_pattern, &pattern) != GST_SDP_OK) {
    goto end;
  }

  medias_len = gst_sdp_message_medias_len (pattern);

  for (i = 0; i < medias_len; i ++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (pattern, i);
    guint attributes_len, j, formats_len;

    attributes_len = gst_sdp_media_attributes_len (media);
    std::list <std::string> to_remove_formats;

    for (j = 0; j < attributes_len; j++) {
      const GstSDPAttribute *attribute = gst_sdp_media_get_attribute (media, j);

      if (g_ascii_strcasecmp ("rtpmap", attribute->key) == 0) {
        gchar *rtpmap = g_strstr_len (attribute->value, -1, " ");
        bool supported = false;

        if (rtpmap != NULL) {
          rtpmap = rtpmap + 1;

          for (int k = 0; k < n_codecs; k++) {
            if (g_strstr_len (rtpmap, -1, supported_codecs[k].c_str() ) == rtpmap) {
              supported = true;
              break;
            }
          }
        }

        if (!supported) {
          GST_INFO ("Removing not supported codec from pattern: '%s'", attribute->value);

          * (rtpmap - 1) = '\0';
          to_remove_formats.push_back (std::string (attribute->value) );

          gst_sdp_media_remove_attribute ( (GstSDPMedia *) media, j);
          attributes_len --;
          j--;
        }
      }
    }

    formats_len = gst_sdp_media_formats_len (media);

    for (j = 0; j < formats_len; j++) {
      bool to_remove = false;

      for (std::string format : to_remove_formats) {
        if (format == std::string (gst_sdp_media_get_format (media, j) ) ) {
          to_remove = true;
          break;
        }
      }

      if (to_remove) {
        gst_sdp_media_remove_format ( (GstSDPMedia *) media, j);
        formats_len --;
        j--;
      }
    }
  }

  g_object_set (element, "pattern-sdp", pattern, NULL);

  gst_sdp_message_free (pattern);

end:
  gst_sdp_message_free (old_pattern);
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

  remove_not_supported_codecs_from_pattern (element);

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
