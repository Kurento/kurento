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
#include <KmsDetectFacesImplFactory.hpp>
#include "KmsDetectFacesImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#define GST_CAT_DEFAULT kurento_kms_detect_faces_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoKmsDetectFacesImpl"

namespace kurento
{
namespace module
{
namespace datachannelexample
{

KmsDetectFacesImpl::KmsDetectFacesImpl (const boost::property_tree::ptree
                                        &config,
                                        std::shared_ptr<MediaPipeline> mediaPipeline) :
  FilterImpl (config,
              std::dynamic_pointer_cast<MediaObjectImpl> ( mediaPipeline) )
{
  g_object_set (element, "filter-factory", "facedetectormetadata", NULL);

  g_object_get (G_OBJECT (element), "filter", &faceDetector, NULL);

  if (faceDetector == NULL) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  g_object_unref (faceDetector);
}

MediaObjectImpl *
KmsDetectFacesImplFactory::createObject (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new KmsDetectFacesImpl (config, mediaPipeline);
}

KmsDetectFacesImpl::StaticConstructor KmsDetectFacesImpl::staticConstructor;

KmsDetectFacesImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* datachannelexample */
} /* module */
} /* kurento */
