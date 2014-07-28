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

#ifndef _KMS_FACE_OVERLAY_H_
#define _KMS_FACE_OVERLAY_H_

#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS

#define KMS_TYPE_FACE_OVERLAY   (kms_face_overlay_get_type())
#define KMS_FACE_OVERLAY(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_FACE_OVERLAY,KmsFaceOverlay))
#define KMS_FACE_OVERLAY_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_FACE_OVERLAY,KmsFaceOverlayClass))
#define KMS_IS_FACE_OVERLAY(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_FACE_OVERLAY))
#define KMS_IS_FACE_OVERLAY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_FACE_OVERLAY))

typedef struct _KmsFaceOverlay KmsFaceOverlay;
typedef struct _KmsFaceOverlayClass KmsFaceOverlayClass;
typedef struct _KmsFaceOverlayPrivate KmsFaceOverlayPrivate;

struct _KmsFaceOverlay
{
  GstBin parent;

   /*< private > */
  KmsFaceOverlayPrivate *priv;

  gboolean show_debug_info;
};

struct _KmsFaceOverlayClass
{
  GstBinClass base_faceoverlay_class;
};

GType kms_face_overlay_get_type (void);

gboolean kms_face_overlay_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_FACE_OVERLAY_H_ */
