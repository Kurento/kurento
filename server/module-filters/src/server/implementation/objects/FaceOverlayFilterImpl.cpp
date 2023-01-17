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
#include <FaceOverlayFilterImplFactory.hpp>
#include "FaceOverlayFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_face_overlay_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoFaceOverlayFilterImpl"

namespace kurento
{

FaceOverlayFilterImpl::FaceOverlayFilterImpl (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline) : FilterImpl ( conf,
                                    std::dynamic_pointer_cast<MediaObjectImpl> ( mediaPipeline) )
{
  g_object_set (element, "filter-factory", "faceoverlay", NULL);

  g_object_get (G_OBJECT (element), "filter", &faceOverlay, NULL);

  if (faceOverlay == nullptr) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  g_object_unref (faceOverlay);
}

void FaceOverlayFilterImpl::unsetOverlayedImage ()
{
  GstStructure *imageSt;
  imageSt = gst_structure_new ("image",
                               "offsetXPercent", G_TYPE_DOUBLE, 0.0,
                               "offsetYPercent", G_TYPE_DOUBLE, 0.0,
                               "widthPercent", G_TYPE_DOUBLE, 0.0,
                               "heightPercent", G_TYPE_DOUBLE, 0.0,
                               "url", G_TYPE_STRING, NULL,
                               NULL);
  g_object_set (G_OBJECT (faceOverlay), "image-to-overlay", imageSt, NULL);
  gst_structure_free (imageSt);
}

void FaceOverlayFilterImpl::setOverlayedImage (const std::string &uri,
    float offsetXPercent, float offsetYPercent, float widthPercent,
    float heightPercent)
{
  GstStructure *imageSt;
  imageSt = gst_structure_new ("image",
                               "offsetXPercent", G_TYPE_DOUBLE, double (offsetXPercent),
                               "offsetYPercent", G_TYPE_DOUBLE, double (offsetYPercent),
                               "widthPercent", G_TYPE_DOUBLE, double (widthPercent),
                               "heightPercent", G_TYPE_DOUBLE, double (heightPercent),
                               "url", G_TYPE_STRING, uri.c_str(),
                               NULL);
  g_object_set (G_OBJECT (faceOverlay), "image-to-overlay", imageSt, NULL);
  gst_structure_free (imageSt);
}

MediaObjectImpl *
FaceOverlayFilterImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline) const
{
  return new FaceOverlayFilterImpl (conf, mediaPipeline);
}

FaceOverlayFilterImpl::StaticConstructor
FaceOverlayFilterImpl::staticConstructor;

FaceOverlayFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
