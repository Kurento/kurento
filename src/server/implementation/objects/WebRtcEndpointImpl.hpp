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
                      std::shared_ptr<MediaPipeline> mediaPipeline,
                      bool useDataChannels);

  virtual ~WebRtcEndpointImpl ();

  std::string getStunServerAddress ();
  void setStunServerAddress (const std::string &stunServerAddress);
  int getStunServerPort ();
  void setStunServerPort (int stunServerPort);

  std::string getTurnUrl ();
  void setTurnUrl (const std::string &turnUrl);

  void gatherCandidates ();
  void addIceCandidate (std::shared_ptr<IceCandidate> candidate);

  void createDataChannel ();
  void createDataChannel (const std::string &label);
  void createDataChannel (const std::string &label, bool ordered);
  void createDataChannel (const std::string &label, bool ordered,
                          int maxPacketLifeTime);
  void createDataChannel (const std::string &label, bool ordered,
                          int maxPacketLifeTime, int maxRetransmits);
  void createDataChannel (const std::string &label, bool ordered,
                          int maxPacketLifeTime, int maxRetransmits, const std::string &protocol);
  void closeDataChannel (int channelId);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, OnIceCandidate> signalOnIceCandidate;
  sigc::signal<void, OnIceGatheringDone> signalOnIceGatheringDone;
  sigc::signal<void, OnIceComponentStateChanged> signalOnIceComponentStateChanged;

  sigc::signal<void, OnDataChannelOpened> signalOnDataChannelOpened;
  sigc::signal<void, OnDataChannelClosed> signalOnDataChannelClosed;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  virtual void postConstructor ();
  virtual void fillStatsReport (std::map <std::string, std::shared_ptr<Stats>>
                                &report, const GstStructure *stats, double timestamp);

private:

  gulong handlerOnIceCandidate = 0;
  gulong handlerOnIceGatheringDone = 0;
  gulong handlerOnIceComponentStateChanged = 0;
  gulong handlerOnDataChannelOpened = 0;
  gulong handlerOnDataChannelClosed = 0;

  void onIceCandidate (gchar *sessId, KmsIceCandidate *candidate);
  void onIceGatheringDone (gchar *sessId);
  void onIceComponentStateChanged (gchar *sessId, const gchar *streamId,
                                   guint componentId, guint state);
  void onDataChannelOpened (gchar *sessId, guint stream_id);
  void onDataChannelClosed (gchar *sessId, guint stream_id);

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
