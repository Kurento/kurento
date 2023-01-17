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
#ifndef _KMS_DISPATCHER_H_
#define _KMS_DISPATCHER_H_

#include <commons/kmsbasehub.h>

G_BEGIN_DECLS
#define KMS_TYPE_DISPATCHER kms_dispatcher_get_type()
#define KMS_DISPATCHER(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST( \
    (obj),                    \
    KMS_TYPE_DISPATCHER,      \
    KmsDispatcher             \
  )                           \
)
#define KMS_DISPATCHER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (           \
    (klass),                          \
    KMS_TYPE_DISPATCHER,              \
    KmsDispatcherClass                \
  )                                   \
)
#define KMS_IS_DISPATCHER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (   \
    (obj),                       \
    KMS_TYPE_DISPATCHER          \
  )                              \
)
#define KMS_IS_DISPATCHER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE((klass),       \
  KMS_TYPE_DISPATCHER)                   \
)

typedef struct _KmsDispatcher KmsDispatcher;
typedef struct _KmsDispatcherClass KmsDispatcherClass;
typedef struct _KmsDispatcherPrivate KmsDispatcherPrivate;

struct _KmsDispatcher
{
  KmsBaseHub parent;

  /*< private > */
  KmsDispatcherPrivate *priv;
};

struct _KmsDispatcherClass
{
  KmsBaseHubClass parent_class;

  /* Actions */
  gboolean (*connect) (KmsDispatcher * self, guint source, guint sink);
};

GType kms_dispatcher_get_type (void);

gboolean kms_dispatcher_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_DISPATCHER_H_ */
