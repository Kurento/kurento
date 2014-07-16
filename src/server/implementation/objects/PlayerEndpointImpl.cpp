#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include <PlayerEndpointImplFactory.hpp>
#include "PlayerEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_player_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPlayerEndpointImpl"

#define FACTORY_NAME "playerendpoint"

namespace kurento
{

PlayerEndpointImpl::PlayerEndpointImpl (std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri, bool useEncodedMedia) : UriEndpointImpl (std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
  // FIXME: Implement this
}

void PlayerEndpointImpl::play ()
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "PlayerEndpointImpl::play: Not implemented");
}

MediaObjectImpl *
PlayerEndpointImplFactory::createObject (std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri, bool useEncodedMedia) const
{
  return new PlayerEndpointImpl (mediaPipeline, uri, useEncodedMedia);
}

PlayerEndpointImpl::StaticConstructor PlayerEndpointImpl::staticConstructor;

PlayerEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
