/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#ifndef _KMS_LOGO_OVERLAY_H_
#define _KMS_LOGO_OVERLAY_H_

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
