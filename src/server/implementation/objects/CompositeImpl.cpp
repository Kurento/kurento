#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <CompositeImplFactory.hpp>
#include "CompositeImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_composite_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoCompositeImpl"

#define FACTORY_NAME "compositemixer"

namespace kurento
{

CompositeImpl::CompositeImpl (std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
  // FIXME: Implement this
}

MediaObjectImpl *
CompositeImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new CompositeImpl (mediaPipeline);
}

CompositeImpl::StaticConstructor CompositeImpl::staticConstructor;

CompositeImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
