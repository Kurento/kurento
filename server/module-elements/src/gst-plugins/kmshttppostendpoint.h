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
#ifndef _KMS_HTTP_POST_ENDPOINT_H_
#define _KMS_HTTP_POST_ENDPOINT_H_

#include "kmshttpendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_HTTP_POST_ENDPOINT \
  (kms_http_post_endpoint_get_type())
#define KMS_HTTP_POST_ENDPOINT(obj) (       \
  G_TYPE_CHECK_INSTANCE_CAST(               \
    (obj),                                  \
    KMS_TYPE_HTTP_POST_ENDPOINT,            \
    KmsHttpPostEndpoint                     \
  )                                         \
)
#define KMS_HTTP_POST_ENDPOINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_HTTP_POST_ENDPOINT,                \
    KmsHttpPostEndpointClass                    \
  )                                             \
)
#define KMS_IS_HTTP_POST_ENDPOINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_HTTP_POST_ENDPOINT                 \
  )                                             \
)
#define KMS_IS_HTTP_POST_ENDPOINT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                        \
    (klass),                                      \
    KMS_TYPE_HTTP_POST_ENDPOINT                   \
  )                                               \
)
typedef struct _KmsHttpPostEndpoint KmsHttpPostEndpoint;
typedef struct _KmsHttpPostEndpointClass KmsHttpPostEndpointClass;
typedef struct _KmsHttpPostEndpointPrivate KmsHttpPostEndpointPrivate;

struct _KmsHttpPostEndpoint
{
  KmsHttpEndpoint parent;

  /*< private > */
  KmsHttpPostEndpointPrivate *priv;
};

struct _KmsHttpPostEndpointClass
{
  KmsHttpEndpointClass parent_class;

  /* actions */
  GstFlowReturn (*push_buffer) (KmsHttpPostEndpoint * self, GstBuffer * buffer);
  GstFlowReturn (*end_of_stream) (KmsHttpPostEndpoint * self);
};

GType kms_http_post_endpoint_get_type (void);

gboolean kms_http_post_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_HTTP_POST_ENDPOINT_H_ */
