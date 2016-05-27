/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#ifndef _KMS_IMAGE_OVERLAY_METADATA_H_
#define _KMS_IMAGE_OVERLAY_METADATA_H_

#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS

#define KMS_TYPE_IMAGE_OVERLAY_METADATA   (kms_image_overlay_metadata_get_type())
#define KMS_IMAGE_OVERLAY_METADATA(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_IMAGE_OVERLAY_METADATA,KmsImageOverlayMetadata))
#define KMS_IMAGE_OVERLAY_METADATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_IMAGE_OVERLAY_METADATA,KmsImageOverlayMetadataClass))
#define KMS_IS_IMAGE_OVERLAY_METADATA(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_IMAGE_OVERLAY_METADATA))
#define KMS_IS_IMAGE_OVERLAY_METADATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_IMAGE_OVERLAY_METADATA))

typedef struct _KmsImageOverlayMetadata KmsImageOverlayMetadata;
typedef struct _KmsImageOverlayMetadataClass KmsImageOverlayMetadataClass;
typedef struct _KmsImageOverlayMetadataPrivate KmsImageOverlayMetadataPrivate;

struct _KmsImageOverlayMetadata
{
  GstVideoFilter base;
  KmsImageOverlayMetadataPrivate *priv;
};

struct _KmsImageOverlayMetadataClass
{
  GstVideoFilterClass base_facedetector_class;
};

GType kms_image_overlay_metadata_get_type (void);

gboolean kms_image_overlay_metadata_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_IMAGE_OVERLAY_METADATA_H_ */
