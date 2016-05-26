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
