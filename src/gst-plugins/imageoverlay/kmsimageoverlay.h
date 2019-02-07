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

#ifndef _KMS_IMAGE_OVERLAY_H_
#define _KMS_IMAGE_OVERLAY_H_

#include "opencv2/core/version.hpp"
#if CV_MAJOR_VERSION == 3
#include <opencv2/core/fast_math.hpp>
#endif
#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS

#define KMS_TYPE_IMAGE_OVERLAY   (kms_image_overlay_get_type())
#define KMS_IMAGE_OVERLAY(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_IMAGE_OVERLAY,KmsImageOverlay))
#define KMS_IMAGE_OVERLAY_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_IMAGE_OVERLAY,KmsImageOverlayClass))
#define KMS_IS_IMAGE_OVERLAY(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_IMAGE_OVERLAY))
#define KMS_IS_IMAGE_OVERLAY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_IMAGE_OVERLAY))

typedef struct _KmsImageOverlay KmsImageOverlay;
typedef struct _KmsImageOverlayClass KmsImageOverlayClass;
typedef struct _KmsImageOverlayPrivate KmsImageOverlayPrivate;

struct _KmsImageOverlay
{
  GstVideoFilter base;
  KmsImageOverlayPrivate *priv;
};

struct _KmsImageOverlayClass
{
  GstVideoFilterClass base_facedetector_class;
};

GType kms_image_overlay_get_type (void);

gboolean kms_image_overlay_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_IMAGE_OVERLAY_H_ */
