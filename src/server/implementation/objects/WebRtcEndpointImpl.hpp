#ifndef __WEB_RTC_ENDPOINT_IMPL_HPP__
#define __WEB_RTC_ENDPOINT_IMPL_HPP__

#include "BaseRtpEndpointImpl.hpp"
#include "WebRtcEndpoint.hpp"
#include <EventHandler.hpp>

typedef struct _KmsIceCandidate KmsIceCandidate;

namespace kurento
{

class MediaPipeline;
class WebRtcEndpointImpl;

void Serialize (std::shared_ptr<WebRtcEndpointImpl> &object,
                JsonSerializer &serializer);

class WebRtcEndpointImpl : public BaseRtpEndpointImpl,
  public virtual WebRtcEndpoint
{

public:

  WebRtcEndpointImpl (const boost::property_tree::ptree &conf,
                      std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~WebRtcEndpointImpl ();

  std::string getStunServerAddress ();
  void setStunServerAddress (const std::string &stunServerAddress);
  int getStunServerPort ();
  void setStunServerPort (int stunServerPort);

  void gatherCandidates ();
  void addIceCandidate (std::shared_ptr<IceCandidate> candidate);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, OnIceCandidate> signalOnIceCandidate;
  sigc::signal<void, OnIceGatheringDone> signalOnIceGatheringDone;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  int handlerOnIceCandidate;
  int handlerOnIceGatheringDone;

  std::function<void (KmsIceCandidate *) > onIceCandidateLambda;
  std::function<void() > onIceGatheringDoneLambda;

  std::shared_ptr<std::string> getPemCertificate ();
  static std::mutex certificateMutex;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __WEB_RTC_ENDPOINT_IMPL_HPP__ */
