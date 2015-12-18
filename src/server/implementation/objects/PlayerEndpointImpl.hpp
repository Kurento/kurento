#ifndef __PLAYER_ENDPOINT_IMPL_HPP__
#define __PLAYER_ENDPOINT_IMPL_HPP__

#include "UriEndpointImpl.hpp"
#include "PlayerEndpoint.hpp"
#include <EventHandler.hpp>
#include <functional>

namespace kurento
{

class MediaPipeline;
class PlayerEndpointImpl;

void Serialize (std::shared_ptr<PlayerEndpointImpl> &object,
                JsonSerializer &serializer);
class VideoInfo;

class PlayerEndpointImpl : public UriEndpointImpl, public virtual PlayerEndpoint
{

public:

  PlayerEndpointImpl (const boost::property_tree::ptree &conf,
                      std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri,
                      bool useEncodedMedia);

  virtual ~PlayerEndpointImpl ();

  void play ();

  virtual std::shared_ptr<VideoInfo> getVideoInfo ();

  virtual int64_t getPosition();
  virtual void setPosition (int64_t position);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, EndOfStream> signalEndOfStream;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  virtual void postConstructor ();

private:

  gulong signalEOS = 0;
  gulong signalInvalidURI = 0;
  gulong signalInvalidMedia = 0;

  void eosHandler ();
  void invalidUri ();
  void invalidMedia ();

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __PLAYER_ENDPOINT_IMPL_HPP__ */
