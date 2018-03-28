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
#include <ImageOverlayFilterImplFactory.hpp>
#include "ImageOverlayFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#define GST_CAT_DEFAULT kurento_image_overlay_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoImageOverlayFilterImpl"

#define IMAGES_TO_OVERLAY "images-to-overlay"

namespace kurento
{

ImageOverlayFilterImpl::ImageOverlayFilterImpl (const
    boost::property_tree::ptree &config,
    std::shared_ptr<MediaPipeline> mediaPipeline)  :
  FilterImpl (config, std::dynamic_pointer_cast<MediaObjectImpl>
              ( mediaPipeline) )
{
  g_object_set (element, "filter-factory", "logooverlay", NULL);

  g_object_get (G_OBJECT (element), "filter", &imageOverlay, NULL);

  if (imageOverlay == nullptr) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  gst_object_unref (imageOverlay);
}

void ImageOverlayFilterImpl::removeImage (const std::string &id)
{
  GstStructure *imagesLayout;
  gint len;

  /* The function obtains the actual window list */
  g_object_get (G_OBJECT (imageOverlay), IMAGES_TO_OVERLAY, &imagesLayout,
                NULL);
  len = gst_structure_n_fields (imagesLayout);

  if (len == 0) {
    GST_WARNING ("There are no images in the layout");
    return;
  }

  for (int i = 0; i < len; i++) {
    const gchar *name;
    name = gst_structure_nth_field_name (imagesLayout, i);

    if (strcmp (id.c_str (), name) == 0) {
      /* this image will be removed */
      gst_structure_remove_field (imagesLayout, name);
      break;
    }
  }

  /* Set the buttons layout list without the window with id = id */
  g_object_set (G_OBJECT (imageOverlay), IMAGES_TO_OVERLAY, imagesLayout, NULL);

  gst_structure_free (imagesLayout);
}

void ImageOverlayFilterImpl::addImage (const std::string &id,
                                       const std::string &uri, float offsetXPercent, float offsetYPercent,
                                       float widthPercent, float heightPercent,
                                       bool keepAspectRatio, bool center)
{
  GstStructure *imagesLayout, *imageSt;

  imageSt = gst_structure_new ("image_position",
                               "id", G_TYPE_STRING, id.c_str (),
                               "uri", G_TYPE_STRING, uri.c_str (),
                               "offsetXPercent", G_TYPE_FLOAT, float (offsetXPercent),
                               "offsetYPercent", G_TYPE_FLOAT, float (offsetYPercent),
                               "widthPercent", G_TYPE_FLOAT, float (widthPercent),
                               "heightPercent", G_TYPE_FLOAT, float (heightPercent),
                               "keepAspectRatio", G_TYPE_BOOLEAN, keepAspectRatio,
                               "center", G_TYPE_BOOLEAN, center,
                               NULL);

  /* The function obtains the actual window list */
  g_object_get (G_OBJECT (imageOverlay), IMAGES_TO_OVERLAY, &imagesLayout,
                NULL);
  gst_structure_set (imagesLayout,
                     id.c_str(), GST_TYPE_STRUCTURE,
                     imageSt, NULL);

  g_object_set (G_OBJECT (imageOverlay), IMAGES_TO_OVERLAY, imagesLayout, NULL);

  gst_structure_free (imagesLayout);
  gst_structure_free (imageSt);
}

MediaObjectImpl *
ImageOverlayFilterImplFactory::createObject (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new ImageOverlayFilterImpl (config, mediaPipeline);
}

ImageOverlayFilterImpl::StaticConstructor
ImageOverlayFilterImpl::staticConstructor;

ImageOverlayFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
