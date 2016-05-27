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

#ifndef _KMS_CHROMA_H_
#define _KMS_CHROMA_H_

#include <gst/video/gstvideofilter.h>

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
