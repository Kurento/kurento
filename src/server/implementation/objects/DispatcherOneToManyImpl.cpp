#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "HubPort.hpp"
#include "HubPortImpl.hpp"
#include <DispatcherOneToManyImplFactory.hpp>
#include "DispatcherOneToManyImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_dispatcher_one_to_many_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoDispatcherOneToManyImpl"

#define FACTORY_NAME "dispatcheronetomany"
#define MAIN_PORT "main"

namespace kurento
{

DispatcherOneToManyImpl::DispatcherOneToManyImpl (const
    boost::property_tree::ptree &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (conf,
          std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
}

void DispatcherOneToManyImpl::setSource (std::shared_ptr<HubPort> source)
{
  std::shared_ptr<HubPortImpl> sourcePort =
    std::dynamic_pointer_cast<HubPortImpl> (source);

  g_object_set (G_OBJECT (element), MAIN_PORT, sourcePort->getHandlerId (), NULL);
}

void DispatcherOneToManyImpl::removeSource ()
{
  g_object_set (G_OBJECT (element), MAIN_PORT, -1, NULL);
}

MediaObjectImpl *
DispatcherOneToManyImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new DispatcherOneToManyImpl (conf, mediaPipeline);
}

DispatcherOneToManyImpl::StaticConstructor
DispatcherOneToManyImpl::staticConstructor;

DispatcherOneToManyImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
