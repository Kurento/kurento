/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifndef __KMS_BUFFER_INJECTOR_H__
#define __KMS_BUFFER_INJECTOR_H__

#include <gst/gst.h>

G_BEGIN_DECLS
#define KMS_TYPE_BUFFER_INJECTOR \
  (kms_buffer_injector_get_type())
#define KMS_BUFFER_INJECTOR(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BUFFER_INJECTOR,KmsBufferInjector))
#define KMS_BUFFER_INJECTOR_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BUFFER_INJECTOR,KmsBufferInjectorClass))
#define KMS_IS_BUFFER_INJECTOR(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BUFFER_INJECTOR))
#define KMS_IS_BUFFER_INJECTOR_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BUFFER_INJECTOR))
#define KMS_BUFFER_INJECTOR_CAST(obj) ((KmsBufferInjector*)(obj))

typedef struct _KmsBufferInjector KmsBufferInjector;
typedef struct _KmsBufferInjectorClass KmsBufferInjectorClass;
typedef struct _KmsBufferInjectorPrivate KmsBufferInjectorPrivate;

struct _KmsBufferInjector
{
  GstElement element;

  KmsBufferInjectorPrivate *priv;
};

struct _KmsBufferInjectorClass
{
  GstElementClass parent_class;
};

GType kms_buffer_injector_get_type (void);

gboolean kms_buffer_injector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_BUFFER_INJECTOR_H__ */
