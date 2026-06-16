/*
 * (C) Copyright 2024 Kurento (http://kurento.org/)
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
#ifndef _KMS_MOQ_SUBSCRIBER_ENDPOINT_H_
#define _KMS_MOQ_SUBSCRIBER_ENDPOINT_H_

#include <commons/kmselement.h>

G_BEGIN_DECLS

#define KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT               \
  (kms_moq_subscriber_endpoint_get_type())
#define KMS_MOQ_SUBSCRIBER_ENDPOINT(obj)               \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),                   \
  KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT,                    \
  KmsMoqSubscriberEndpoint))
#define KMS_MOQ_SUBSCRIBER_ENDPOINT_CLASS(klass)       \
  (G_TYPE_CHECK_CLASS_CAST((klass),                    \
  KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT,                    \
  KmsMoqSubscriberEndpointClass))
#define KMS_IS_MOQ_SUBSCRIBER_ENDPOINT(obj)            \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),                   \
  KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT))
#define KMS_IS_MOQ_SUBSCRIBER_ENDPOINT_CLASS(klass)    \
  (G_TYPE_CHECK_CLASS_TYPE((klass),                    \
  KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT))

typedef struct _KmsMoqSubscriberEndpoint        KmsMoqSubscriberEndpoint;
typedef struct _KmsMoqSubscriberEndpointClass   KmsMoqSubscriberEndpointClass;
typedef struct _KmsMoqSubscriberEndpointPrivate KmsMoqSubscriberEndpointPrivate;

struct _KmsMoqSubscriberEndpoint
{
  KmsElement parent;

  /*< private >*/
  KmsMoqSubscriberEndpointPrivate *priv;
};

struct _KmsMoqSubscriberEndpointClass
{
  KmsElementClass parent_class;

  /* Signals */
  void (*eos_signal) (KmsMoqSubscriberEndpoint *self);
};

GType    kms_moq_subscriber_endpoint_get_type (void);
gboolean kms_moq_subscriber_endpoint_plugin_init (GstPlugin *plugin);

G_END_DECLS

#endif /* _KMS_MOQ_SUBSCRIBER_ENDPOINT_H_ */
