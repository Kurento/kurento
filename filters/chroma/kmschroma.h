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

#ifndef _KMS_CHROMA_H_
#define _KMS_CHROMA_H_

#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>
#include <opencv/cxcore.h>
#include <opencv/highgui.h>

G_BEGIN_DECLS

#define KMS_TYPE_CHROMA   (kms_chroma_get_type())
#define KMS_CHROMA(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_CHROMA,KmsChroma))
#define KMS_CHROMA_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_CHROMA,KmsChromaClass))
#define KMS_IS_CHROMA(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_CHROMA))
#define KMS_IS_CHROMA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_CHROMA))

typedef struct _KmsChroma KmsChroma;
typedef struct _KmsChromaClass KmsChromaClass;
typedef struct _KmsChromaPrivate KmsChromaPrivate;

struct _KmsChroma
{
  GstVideoFilter base;
  KmsChromaPrivate *priv;
};

struct _KmsChromaClass
{
  GstVideoFilterClass base_chroma_class;
};

GType kms_chroma_get_type (void);

gboolean kms_chroma_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_CHROMA_H_ */
