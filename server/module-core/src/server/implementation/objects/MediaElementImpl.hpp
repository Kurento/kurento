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
#ifndef __MEDIA_ELEMENT_IMPL_HPP__
#define __MEDIA_ELEMENT_IMPL_HPP__

#include "MediaObjectImpl.hpp"
#include "MediaElement.hpp"
#include "MediaType.hpp"
#include "MediaLatencyStat.hpp"
#include <EventHandler.hpp>
#include <gst/gst.h>
#include <mutex>
#include <set>
#include "MediaFlowOutStateChanged.hpp"
#include "MediaFlowInStateChanged.hpp"
#include "MediaTranscodingStateChanged.hpp"
#include "MediaFlowState.hpp"
#include "MediaTranscodingState.hpp"
#include "commons/kmselement.h"
#include "commons/kmselementpadtype.h"

namespace kurento
{

class MediaType;
class MediaElementImpl;
class AudioCodec;
class VideoCodec;

struct MediaTypeCmp {
  bool operator() (const std::shared_ptr<MediaType> &a,
                   const std::shared_ptr<MediaType> &b) const
  {
    return a->getValue () < b->getValue ();
  }
};

void Serialize (std::shared_ptr<MediaElementImpl> &object,
                JsonSerializer &serializer);

class ElementConnectionDataInternal;

class MediaElementImpl : public MediaObjectImpl, public virtual MediaElement
{

public:

  MediaElementImpl (const boost::property_tree::ptree &config,
                    std::shared_ptr<MediaObjectImpl> parent,
                    const std::string &factoryName);

  virtual ~MediaElementImpl ();

  GstElement *getGstreamerElement()
  {
    return element;
  }

  virtual std::map <std::string, std::shared_ptr<Stats>> getStats () override;
  virtual std::map <std::string, std::shared_ptr<Stats>> getStats (
        std::shared_ptr<MediaType> mediaType) override;

  virtual std::vector<std::shared_ptr<ElementConnectionData>>
      getSourceConnections () override;
  virtual std::vector<std::shared_ptr<ElementConnectionData>>
      getSourceConnections (
        std::shared_ptr<MediaType> mediaType) override;
  virtual std::vector<std::shared_ptr<ElementConnectionData>>
      getSourceConnections (
        std::shared_ptr<MediaType> mediaType, const std::string &description) override;
  virtual std::vector<std::shared_ptr<ElementConnectionData>>
      getSinkConnections () override;
  virtual std::vector<std::shared_ptr<ElementConnectionData>> getSinkConnections (
        std::shared_ptr<MediaType> mediaType) override;
  virtual std::vector<std::shared_ptr<ElementConnectionData>> getSinkConnections (
        std::shared_ptr<MediaType> mediaType, const std::string &description) override;
  virtual void connect (std::shared_ptr<MediaElement> sink) override;
  virtual void connect (std::shared_ptr<MediaElement> sink,
                        std::shared_ptr<MediaType> mediaType) override;
  virtual void connect (std::shared_ptr<MediaElement> sink,
                        std::shared_ptr<MediaType> mediaType,
                        const std::string &sourceMediaDescription) override;
  virtual void connect (std::shared_ptr<MediaElement> sink,
                        std::shared_ptr<MediaType> mediaType,
                        const std::string &sourceMediaDescription,
                        const std::string &sinkMediaDescription) override;
  virtual void disconnect (std::shared_ptr<MediaElement> sink) override;
  virtual void disconnect (std::shared_ptr<MediaElement> sink,
                           std::shared_ptr<MediaType> mediaType) override;
  virtual void disconnect (std::shared_ptr<MediaElement> sink,
                           std::shared_ptr<MediaType> mediaType,
                           const std::string &sourceMediaDescription) override;
  virtual void disconnect (std::shared_ptr<MediaElement> sink,
                           std::shared_ptr<MediaType> mediaType,
                           const std::string &sourceMediaDescription,
                           const std::string &sinkMediaDescription) override;
  void setAudioFormat (std::shared_ptr<AudioCaps> caps) override;
  void setVideoFormat (std::shared_ptr<VideoCaps> caps) override;

  virtual void release () override;

  virtual std::string getGstreamerDot () override;
  virtual std::string getGstreamerDot (std::shared_ptr<GstreamerDotDetails>
                                       details) override;

  virtual void dumpGstreamerDot ();
  virtual void dumpGstreamerDot (std::shared_ptr<GstreamerDotDetails>
                                       details);

  bool isMediaFlowingIn (std::shared_ptr<MediaType> mediaType) override;
  bool isMediaFlowingIn (std::shared_ptr<MediaType> mediaType,
                         const std::string &sinkMediaDescription) override;
  bool isMediaFlowingOut (std::shared_ptr<MediaType> mediaType) override;
  bool isMediaFlowingOut (std::shared_ptr<MediaType> mediaType,
                          const std::string &sourceMediaDescription) override;
  bool isMediaTranscoding (std::shared_ptr<MediaType> mediaType) override;
  bool isMediaTranscoding (std::shared_ptr<MediaType> mediaType,
                           const std::string &binName) override;

  virtual int getEncoderBitrate () override;
  virtual void setEncoderBitrate (int encoderBitrate) override;

  virtual int getMinEncoderBitrate () override;
  virtual void setMinEncoderBitrate (int minEncoderBitrate) override;

  virtual int getMaxEncoderBitrate () override;
  virtual void setMaxEncoderBitrate (int maxEncoderBitrate) override;

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler) override;

  sigc::signal<void, ElementConnected> signalElementConnected;
  sigc::signal<void, ElementDisconnected> signalElementDisconnected;
  sigc::signal<void, MediaFlowOutStateChanged> signalMediaFlowOutStateChanged;
  sigc::signal<void, MediaFlowInStateChanged> signalMediaFlowInStateChanged;
  sigc::signal<void, MediaTranscodingStateChanged> signalMediaTranscodingStateChanged;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response) override;

  virtual void Serialize (JsonSerializer &serializer) override;

protected:
  GstElement *element;
  std::map <std::string, std::shared_ptr <MediaFlowState>> mediaFlowInStates;
  std::map <std::string, std::shared_ptr <MediaFlowState>> mediaFlowOutStates;
  std::map <std::string, std::shared_ptr <MediaTranscodingState>> mediaTranscodingStates;

  virtual void postConstructor () override;
  void collectLatencyStats (std::vector<std::shared_ptr<MediaLatencyStat>>
                            &latencyStats, const GstStructure *stats);
  virtual void fillStatsReport (std::map <std::string, std::shared_ptr<Stats>>
                                &report, const GstStructure *stats,
                                int64_t timestampMillis);

  virtual void prepareSinkConnection (std::shared_ptr<MediaElement> src,
                                      std::shared_ptr<MediaType> mediaType,
                                      const std::string &sourceMediaDescription,
                                      const std::string &sinkMediaDescription);

private:
  std::recursive_timed_mutex sourcesMutex;
  std::recursive_timed_mutex sinksMutex;

  std::map < std::shared_ptr <MediaType>, std::map < std::string,
      std::shared_ptr<ElementConnectionDataInternal >> , MediaTypeCmp > sources;
  std::map < std::shared_ptr <MediaType>, std::map < std::string,
      std::set<std::shared_ptr<ElementConnectionDataInternal> >> , MediaTypeCmp >
      sinks;

  gulong padAddedHandlerId = 0;
  gulong mediaFlowOutHandler = 0;
  gulong mediaFlowInHandler = 0;
  gulong mediaTranscodingHandler = 0;
  gulong busMessageHandler = 0;

  void disconnectAll();
  void performConnection (std::shared_ptr <ElementConnectionDataInternal> data);
  std::map <std::string, std::shared_ptr<Stats>> generateStats (
        const gchar *selector);
  void mediaFlowOutStateChanged (gboolean isFlowing, gchar *padName,
                                 KmsElementPadType type);
  void mediaFlowInStateChanged (gboolean isFlowing, gchar *padName,
                               KmsElementPadType type);
  void onMediaTranscodingStateChanged (gboolean isTranscoding, gchar *binName,
                                      KmsElementPadType type);
  void processBusMessage (GstMessage *msg);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

  friend void _media_element_pad_added (GstElement *elem, GstPad *pad,
                                        gpointer data);
};

} /* kurento */

#endif /*  __MEDIA_ELEMENT_IMPL_HPP__ */
