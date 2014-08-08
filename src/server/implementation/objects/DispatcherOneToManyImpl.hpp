#ifndef __DISPATCHER_ONE_TO_MANY_IMPL_HPP__
#define __DISPATCHER_ONE_TO_MANY_IMPL_HPP__

#include "HubImpl.hpp"
#include "DispatcherOneToMany.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class HubPort;
class DispatcherOneToManyImpl;

void Serialize (std::shared_ptr<DispatcherOneToManyImpl> &object,
                JsonSerializer &serializer);

class DispatcherOneToManyImpl : public HubImpl,
  public virtual DispatcherOneToMany
{

public:

  DispatcherOneToManyImpl (const boost::property_tree::ptree &conf,
                           std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~DispatcherOneToManyImpl () {};

  void setSource (std::shared_ptr<HubPort> source);
  void removeSource ();

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

#endif /*  __DISPATCHER_ONE_TO_MANY_IMPL_HPP__ */
