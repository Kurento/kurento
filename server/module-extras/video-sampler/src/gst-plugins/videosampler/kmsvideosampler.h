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

#ifndef _KMS_VIDEOSAMPLER_H_
#define _KMS_VIDEOSAMPLER_H_

#include <commons/kmselement.h>

G_BEGIN_DECLS
#define KMS_TYPE_VIDEOSAMPLER   (kms_videosampler_get_type())
#define KMS_VIDEOSAMPLER(obj)   (               \
  G_TYPE_CHECK_INSTANCE_CAST(                   \
    (obj),                                      \
    KMS_TYPE_VIDEOSAMPLER,                      \
    KmsVideoSampler                             \
  )                                             \
)
#define KMS_VIDEOSAMPLER_CLASS(klass)   (       \
  G_TYPE_CHECK_CLASS_CAST(                      \
    (klass),                                    \
    KMS_TYPE_VIDEOSAMPLER,                      \
    KmsVideoSamplerClass                        \
  )                                             \
)
#define KMS_IS_VIDEOSAMPLER(obj)   (            \
  G_TYPE_CHECK_INSTANCE_TYPE(                   \
    (obj),                                      \
    KMS_TYPE_VIDEOSAMPLER                       \
  )                                             \
)
#define KMS_IS_VIDEOSAMPLER_CLASS(klass)   (    \
  G_TYPE_CHECK_CLASS_TYPE(                      \
    (klass),                                    \
    KMS_TYPE_VIDEOSAMPLER                       \
  )                                             \
)

#define KMS_VIDEOSAMPLER_CAST(obj) ((KmsVideoSampler*)(obj))

#define KMS_VIDEOSAMPLER_LOCK(elem) \
  (g_mutex_lock (&KMS_VIDEOSAMPLER_CAST ((elem))->mutex))
#define KMS_VIDEOSAMPLER_UNLOCK(elem) \
  (g_mutex_unlock (&KMS_VIDEOSAMPLER_CAST ((elem))->mutex))



typedef struct _KmsVideoSampler KmsVideoSampler;
typedef struct _KmsVideoSamplerClass KmsVideoSamplerClass;
typedef struct _KmsVideoSamplerPrivate KmsVideoSamplerPrivate;

struct _KmsVideoSampler
{
  KmsElement parent;
  KmsVideoSamplerPrivate *priv;
  GMutex mutex;
};

struct _KmsVideoSamplerClass
{
  KmsElementClass parent_class;
};

GType kms_videosampler_get_type (void);

gboolean kms_videosampler_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_VIDEOSAMPLER_H_ */
