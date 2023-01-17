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
#ifndef _KMS_HTTP_ENDPOINT_H_
#define _KMS_HTTP_ENDPOINT_H_

#include <commons/kmselement.h>
#include "kmshttpendpointmethod.h"

G_BEGIN_DECLS
#define KMS_TYPE_HTTP_ENDPOINT \
  (kms_http_endpoint_get_type())
#define KMS_HTTP_ENDPOINT(obj) (           \
  G_TYPE_CHECK_INSTANCE_CAST(              \
    (obj),                                 \
    KMS_TYPE_HTTP_ENDPOINT,                \
    KmsHttpEndpoint                        \
  )                                        \
)
#define KMS_HTTP_ENDPOINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_HTTP_ENDPOINT,                \
    KmsHttpEndpointClass                   \
  )                                        \
)
#define KMS_IS_HTTP_ENDPOINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_HTTP_ENDPOINT                 \
  )                                        \
)
#define KMS_IS_HTTP_ENDPOINT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                   \
    (klass),                                 \
    KMS_TYPE_HTTP_ENDPOINT                   \
  )                                          \
)

#define KMS_HTTP_ENDPOINT_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_HTTP_ENDPOINT,                \
    KmsHttpEndpointClass                   \
  )                                        \
)

typedef struct _KmsHttpEndpoint KmsHttpEndpoint;
typedef struct _KmsHttpEndpointClass KmsHttpEndpointClass;

#define BASE_TIME_LOCK(obj) (                            \
  g_mutex_lock (&KMS_HTTP_ENDPOINT(obj)->base_time_lock) \
)

#define BASE_TIME_UNLOCK(obj) (                            \
  g_mutex_unlock (&KMS_HTTP_ENDPOINT(obj)->base_time_lock) \
)

struct _KmsHttpEndpoint
{
  KmsElement parent;

  /* <protected> */
  KmsHttpEndpointMethod method;
  GstElement *pipeline;
  gboolean start;
  GMutex base_time_lock;
};

struct _KmsHttpEndpointClass
{
  KmsElementClass parent_class;

  void (*start) (KmsHttpEndpoint *self, gboolean start);

  /* signals */
  void (*eos_signal) (KmsHttpEndpoint * self);
};

GType kms_http_endpoint_get_type (void);

gboolean kms_http_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_HTTP_ENDPOINT_H */
