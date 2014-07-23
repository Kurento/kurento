#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "HubPort.hpp"
#include <DispatcherOneToManyImplFactory.hpp>
#include "DispatcherOneToManyImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_dispatcher_one_to_many_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoDispatcherOneToManyImpl"

#define FACTORY_NAME "dispatcheronetomany"

namespace kurento
{

DispatcherOneToManyImpl::DispatcherOneToManyImpl (std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
  // FIXME: Implement this
}

void DispatcherOneToManyImpl::setSource (std::shared_ptr<HubPort> source)
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "DispatcherOneToManyImpl::setSource: Not implemented");
}

void DispatcherOneToManyImpl::removeSource ()
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "DispatcherOneToManyImpl::removeSource: Not implemented");
}

MediaObjectImpl *
DispatcherOneToManyImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new DispatcherOneToManyImpl (mediaPipeline);
}

DispatcherOneToManyImpl::StaticConstructor DispatcherOneToManyImpl::staticConstructor;

DispatcherOneToManyImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
