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

#ifndef _KMS_LOGO_OVERLAY_H_
#define _KMS_LOGO_OVERLAY_H_

#include "opencv2/core/version.hpp"
#if CV_MAJOR_VERSION == 3
#include <opencv2/core/fast_math.hpp>
#endif
#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS

#define KMS_TYPE_LOGO_OVERLAY   (kms_logo_overlay_get_type())
#define KMS_LOGO_OVERLAY(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_LOGO_OVERLAY,KmsLogoOverlay))
#define KMS_LOGO_OVERLAY_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_LOGO_OVERLAY,KmsLogoOverlayClass))
#define KMS_IS_LOGO_OVERLAY(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_LOGO_OVERLAY))
#define KMS_IS_LOGO_OVERLAY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_LOGO_OVERLAY))

typedef struct _KmsLogoOverlay KmsLogoOverlay;
typedef struct _KmsLogoOverlayClass KmsLogoOverlayClass;
typedef struct _KmsLogoOverlayPrivate KmsLogoOverlayPrivate;

struct _KmsLogoOverlay
{
  GstVideoFilter base;
  KmsLogoOverlayPrivate *priv;
};

struct _KmsLogoOverlayClass
{
  GstVideoFilterClass base_facedetector_class;
};

GType kms_logo_overlay_get_type (void);

gboolean kms_logo_overlay_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_LOGO_OVERLAY_H_ */
