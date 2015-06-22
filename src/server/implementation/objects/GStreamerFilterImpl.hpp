/* Autogenerated with kurento-module-creator */

#ifndef __GSTREAMER_FILTER_IMPL_HPP__
#define __GSTREAMER_FILTER_IMPL_HPP__

#include "FilterImpl.hpp"
#include "GStreamerFilter.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class FilterType;
class GStreamerFilterImpl;

void Serialize (std::shared_ptr<GStreamerFilterImpl> &object,
                JsonSerializer &serializer);

class GStreamerFilterImpl : public FilterImpl, public virtual GStreamerFilter
{

public:

  GStreamerFilterImpl (const boost::property_tree::ptree &conf,
                       std::shared_ptr<MediaPipeline> mediaPipeline,
                       const std::string &command, std::shared_ptr<FilterType> filterType);

  virtual ~GStreamerFilterImpl () {};

  virtual std::string getCommand ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:
  GstElement *filter = NULL;
  std::string cmd;

  void setCommandProperties (const std::string &rest_token);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __GSTREAMER_FILTER_IMPL_HPP__ */
