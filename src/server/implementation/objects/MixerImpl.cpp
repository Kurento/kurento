#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaType.hpp"
#include "HubPort.hpp"
#include <MixerImplFactory.hpp>
#include "MixerImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_mixer_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMixerImpl"

#define FACTORY_NAME "selectablemixer"

namespace kurento
{

MixerImpl::MixerImpl (const boost::property_tree::ptree &conf,
                      std::shared_ptr<MediaPipeline> mediaPipeline) : HubImpl (conf,
                            std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME)
{
  // FIXME: Implement this
}

void MixerImpl::connect (std::shared_ptr<MediaType> media,
                         std::shared_ptr<HubPort> source, std::shared_ptr<HubPort> sink)
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "MixerImpl::connect: Not implemented");
}

void MixerImpl::disconnect (std::shared_ptr<MediaType> media,
                            std::shared_ptr<HubPort> source, std::shared_ptr<HubPort> sink)
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED,
                          "MixerImpl::disconnect: Not implemented");
}

MediaObjectImpl *
MixerImplFactory::createObject (const boost::property_tree::ptree &conf,
                                std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new MixerImpl (conf, mediaPipeline);
}

MixerImpl::StaticConstructor MixerImpl::staticConstructor;

MixerImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
