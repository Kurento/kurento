#ifndef __MIXER_IMPL_HPP__
#define __MIXER_IMPL_HPP__

#include "HubImpl.hpp"
#include "Mixer.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class MediaType;
class HubPort;
class MixerImpl;

void Serialize (std::shared_ptr<MixerImpl> &object, JsonSerializer &serializer);

class MixerImpl : public HubImpl, public virtual Mixer
{

public:

  MixerImpl (const boost::property_tree::ptree &conf,
             std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~MixerImpl () {};

  void connect (std::shared_ptr<MediaType> media, std::shared_ptr<HubPort> source,
                std::shared_ptr<HubPort> sink);
  void disconnect (std::shared_ptr<MediaType> media,
                   std::shared_ptr<HubPort> source, std::shared_ptr<HubPort> sink);

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __MIXER_IMPL_HPP__ */
