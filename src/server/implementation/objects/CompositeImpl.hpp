#ifndef __COMPOSITE_IMPL_HPP__
#define __COMPOSITE_IMPL_HPP__

#include "HubImpl.hpp"
#include "Composite.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class CompositeImpl;

void Serialize (std::shared_ptr<CompositeImpl> &object,
                JsonSerializer &serializer);

class CompositeImpl : public HubImpl, public virtual Composite
{

public:

  CompositeImpl (const boost::property_tree::ptree &conf,
                 std::shared_ptr<MediaPipeline> mediaPipeline);

  virtual ~CompositeImpl () {};

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

#endif /*  __COMPOSITE_IMPL_HPP__ */
