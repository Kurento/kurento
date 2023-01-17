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
#include "WindowParam.hpp"
#include <ChromaFilterImplFactory.hpp>
#include "ChromaFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#define GST_CAT_DEFAULT kurento_chroma_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoChromaFilterImpl"

#define SET_BACKGROUND_URI "image-background"
#define SET_CALIBRATION_AREA "calibration-area"

namespace kurento
{
namespace module
{
namespace chroma
{

ChromaFilterImpl::ChromaFilterImpl (const boost::property_tree::ptree &config,
                                    std::shared_ptr<MediaPipeline> mediaPipeline,
                                    std::shared_ptr<WindowParam> window,
                                    const std::string &backgroundImage)  : FilterImpl (config,
                                          std::dynamic_pointer_cast<MediaPipelineImpl> (mediaPipeline) )
{
  GstStructure *aux;

  g_object_set (element, "filter-factory", "chroma", NULL);

  g_object_get (G_OBJECT (element), "filter", &chroma, NULL);

  if (chroma == NULL) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  aux = gst_structure_new ("calibration_area",
                           "x", G_TYPE_INT, window->getTopRightCornerX(),
                           "y", G_TYPE_INT, window->getTopRightCornerY(),
                           "width", G_TYPE_INT, window->getWidth(),
                           "height", G_TYPE_INT, window->getHeight(),
                           NULL);

  g_object_set (G_OBJECT (chroma), SET_CALIBRATION_AREA, aux, NULL);
  gst_structure_free (aux);

  if (backgroundImage != "") {
    g_object_set (G_OBJECT (this->chroma), SET_BACKGROUND_URI,
                  backgroundImage.c_str(), NULL);
  }

  g_object_unref (chroma);
}

void ChromaFilterImpl::setBackground (const std::string &uri)
{
  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI,
                uri.c_str(), NULL);
}

void ChromaFilterImpl::unsetBackground ()
{
  g_object_set (G_OBJECT (chroma), SET_BACKGROUND_URI, NULL, NULL);
}

MediaObjectImpl *
ChromaFilterImplFactory::createObject (const boost::property_tree::ptree
                                       &config, std::shared_ptr<MediaPipeline> mediaPipeline,
                                       std::shared_ptr<WindowParam> window, const std::string &backgroundImage) const
{
  return new ChromaFilterImpl (config, mediaPipeline, window, backgroundImage);
}

ChromaFilterImpl::StaticConstructor ChromaFilterImpl::staticConstructor;

ChromaFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* chroma */
} /* module */
} /* kurento */
