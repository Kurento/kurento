#ifndef __RECORDER_ENDPOINT_IMPL_HPP__
#define __RECORDER_ENDPOINT_IMPL_HPP__

#include "UriEndpointImpl.hpp"
#include "RecorderEndpoint.hpp"
#include <EventHandler.hpp>

namespace kurento
{

class MediaPipeline;
class MediaProfileSpecType;
class RecorderEndpointImpl;

void Serialize (std::shared_ptr<RecorderEndpointImpl> &object,
                JsonSerializer &serializer);

class RecorderEndpointImpl : public UriEndpointImpl,
  public virtual RecorderEndpoint
{

public:

  RecorderEndpointImpl (const boost::property_tree::ptree &conf,
                        std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri,
                        std::shared_ptr<MediaProfileSpecType> mediaProfile, bool stopOnEndOfStream);

  virtual ~RecorderEndpointImpl ();

  void record ();

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

protected:
  virtual void fillStatsReport (std::map <std::string, std::shared_ptr<Stats>>
                                &report, const GstStructure *stats, double timestamp);
private:
  static bool support_ksr;

  void collectEndpointStats (std::map <std::string, std::shared_ptr<Stats>>
                             &statsReport, std::string id, const GstStructure *stats,
                             double timestamp);

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;
};

} /* kurento */

#endif /*  __RECORDER_ENDPOINT_IMPL_HPP__ */
