/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaPipelineImpl.hpp"
#include <KmsShowFacesImplFactory.hpp>
#include "KmsShowFacesImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#define GST_CAT_DEFAULT kurento_kms_show_faces_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoKmsShowFacesImpl"

namespace kurento
{
namespace module
{
namespace datachannelexample
{

KmsShowFacesImpl::KmsShowFacesImpl (const boost::property_tree::ptree &config,
                                    std::shared_ptr<MediaPipeline> mediaPipeline) :
  FilterImpl (config,
              std::dynamic_pointer_cast<MediaObjectImpl> ( mediaPipeline) )
{
  g_object_set (element, "filter-factory", "imageoverlaymetadata", NULL);

  g_object_get (G_OBJECT (element), "filter", &imageOverlay, NULL);

  if (imageOverlay == NULL) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  g_object_unref (imageOverlay);
}

MediaObjectImpl *
KmsShowFacesImplFactory::createObject (const boost::property_tree::ptree
                                       &config, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new KmsShowFacesImpl (config, mediaPipeline);
}

KmsShowFacesImpl::StaticConstructor KmsShowFacesImpl::staticConstructor;

KmsShowFacesImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* datachannelexample */
} /* module */
} /* kurento */
