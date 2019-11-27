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
class CertificateKeyType;

void Serialize (std::shared_ptr<WebRtcEndpointImpl> &object,
                JsonSerializer &serializer);

class WebRtcEndpointImpl : public BaseRtpEndpointImpl,
  public virtual WebRtcEndpoint
{

public:

  WebRtcEndpointImpl (const boost::property_tree::ptree &conf,
                      std::shared_ptr<MediaPipeline> mediaPipeline,
                      bool recvonly, bool sendonly, bool useDataChannels,
                      std::shared_ptr<CertificateKeyType> certificateKeyType);

  ~WebRtcEndpointImpl () override;

  std::string getExternalAddress () override;
  void setExternalAddress (const std::string &externalAddress) override;

  std::string getNetworkInterfaces () override;
  void setNetworkInterfaces (const std::string &networkInterfaces) override;

  std::string getStunServerAddress () override;
  void setStunServerAddress (const std::string &stunServerAddress) override;

  int getStunServerPort () override;
  void setStunServerPort (int stunServerPort) override;

  std::string getTurnUrl () override;
  void setTurnUrl (const std::string &turnUrl) override;

  std::vector<std::shared_ptr<IceCandidatePair>> getICECandidatePairs () override;

  std::vector<std::shared_ptr<IceConnection>> getIceConnectionState () override;

  void gatherCandidates () override;
  void addIceCandidate (std::shared_ptr<IceCandidate> candidate) override;

  void createDataChannel () override;
  void createDataChannel (const std::string &label) override;
  void createDataChannel (const std::string &label, bool ordered) override;
  void createDataChannel (const std::string &label, bool ordered,
                          int maxPacketLifeTime) override;
  void createDataChannel (const std::string &label, bool ordered,
                          int maxPacketLifeTime, int maxRetransmits) override;
  void createDataChannel (const std::string &label, bool ordered,
                          int maxPacketLifeTime, int maxRetransmits,
                          const std::string &protocol) override;
  void closeDataChannel (int channelId) override;

  /* Next methods are automatically implemented by code generator */
  using BaseRtpEndpointImpl::connect;
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  sigc::signal<void, OnIceCandidate> signalOnIceCandidate;
  sigc::signal<void, IceCandidateFound> signalIceCandidateFound;
  sigc::signal<void, OnIceGatheringDone> signalOnIceGatheringDone;
  sigc::signal<void, IceGatheringDone> signalIceGatheringDone;
  sigc::signal<void, OnIceComponentStateChanged> signalOnIceComponentStateChanged;
  sigc::signal<void, IceComponentStateChange> signalIceComponentStateChange;
  sigc::signal<void, NewCandidatePairSelected> signalNewCandidatePairSelected;

  sigc::signal<void, OnDataChannelOpened> signalOnDataChannelOpened;
  sigc::signal<void, DataChannelOpen> signalDataChannelOpen;
  sigc::signal<void, OnDataChannelClosed> signalOnDataChannelClosed;
  sigc::signal<void, DataChannelClose> signalDataChannelClose;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:
  virtual void postConstructor () override;
  virtual void fillStatsReport (std::map <std::string, std::shared_ptr<Stats>>
                                &report, const GstStructure *stats,
                                double timestamp, int64_t timestampMillis) override;

private:

  gulong handlerOnIceCandidate = 0;
  gulong handlerOnIceGatheringDone = 0;
  gulong handlerOnIceComponentStateChanged = 0;
  gulong handlerOnDataChannelOpened = 0;
  gulong handlerOnDataChannelClosed = 0;
  gulong handlerNewSelectedPairFull = 0;

  void onIceCandidate (gchar *sessId, KmsIceCandidate *candidate);
  void onIceGatheringDone (gchar *sessId);
  void onIceComponentStateChanged (gchar *sessId, const gchar *streamId,
                                   guint componentId, guint state);
  void newSelectedPairFull (gchar *sessId, const gchar *streamId,
                            guint componentId, KmsIceCandidate *localCandidate,
                            KmsIceCandidate *remoteCandidate);
  void onDataChannelOpened (gchar *sessId, guint stream_id);
  void onDataChannelClosed (gchar *sessId, guint stream_id);
  void checkUri (std::string &uri);
  std::string getCerficateFromFile (std::string &path);
  void generateDefaultCertificates ();

  std::map < std::string, std::shared_ptr<IceCandidatePair >> candidatePairs;
  std::map < std::string, std::shared_ptr<IceConnection>> iceConnectionState;

  std::mutex mut;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __WEB_RTC_ENDPOINT_IMPL_HPP__ */
