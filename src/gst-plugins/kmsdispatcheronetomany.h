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
#ifndef _KMS_DISPATCHER_ONE_TO_MANY_H_
#define _KMS_DISPATCHER_ONE_TO_MANY_H_

#include <commons/kmsbasehub.h>

G_BEGIN_DECLS
#define KMS_TYPE_DISPATCHER_ONE_TO_MANY                 \
    kms_dispatcher_one_to_many_get_type()

#define KMS_DISPATCHER_ONE_TO_MANY(obj) (               \
  G_TYPE_CHECK_INSTANCE_CAST(                           \
    (obj),                                              \
    KMS_TYPE_DISPATCHER_ONE_TO_MANY,                    \
    KmsDispatcherOneToMany                              \
  )                                                     \
)

#define KMS_DISPATCHER_ONE_TO_MANY_CLASS(klass) (       \
  G_TYPE_CHECK_CLASS_CAST (                             \
    (klass),                                            \
    KMS_TYPE_DISPATCHER_ONE_TO_MANY,                    \
    KmsDispatcherOneToManyClass                         \
  )                                                     \
)
#define KMS_IS_DISPATCHER_ONE_TO_MANY(obj) (            \
  G_TYPE_CHECK_INSTANCE_TYPE (                          \
    (obj),                                              \
    KMS_TYPE_DISPATCHER_ONE_TO_MANY                     \
  )                                                     \
)
#define KMS_IS_DISPATCHER_ONE_TO_MANY_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_TYPE((klass),                      \
  KMS_TYPE_DISPATCHER_ONE_TO_MANY)                      \
)

typedef struct _KmsDispatcherOneToMany KmsDispatcherOneToMany;
typedef struct _KmsDispatcherOneToManyClass KmsDispatcherOneToManyClass;
typedef struct _KmsDispatcherOneToManyPrivate KmsDispatcherOneToManyPrivate;

struct _KmsDispatcherOneToMany
{
  KmsBaseHub parent;

  /*< private > */
  KmsDispatcherOneToManyPrivate *priv;
};

struct _KmsDispatcherOneToManyClass
{
  KmsBaseHubClass parent_class;
};

GType kms_dispatcher_one_to_many_get_type (void);

gboolean kms_dispatcher_one_to_many_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_DISPATCHER_ONE_TO_MANY_H_ */
