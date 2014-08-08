#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "HubPort.hpp"
#include <DispatcherImplFactory.hpp>
#include "DispatcherImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_dispatcher_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoDispatcherImpl"

#define FACTORY_NAME "dispatcher"

namespace kurento
{

DispatcherImpl::DispatcherImpl (const boost::property_tree::ptree &conf,
                                std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (conf,
                                      std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
  // FIXME: Implement this
}

void DispatcherImpl::connect (std::shared_ptr<HubPort> source,
                              std::shared_ptr<HubPort> sink)
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED,
                          "DispatcherImpl::connect: Not implemented");
}

MediaObjectImpl *
DispatcherImplFactory::createObject (const boost::property_tree::ptree &conf,
                                     std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new DispatcherImpl (conf, mediaPipeline);
}

DispatcherImpl::StaticConstructor DispatcherImpl::staticConstructor;

DispatcherImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
