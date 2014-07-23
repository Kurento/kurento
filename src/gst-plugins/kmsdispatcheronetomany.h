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
#ifndef _KMS_DISPATCHER_ONE_TO_MANY_H_
#define _KMS_DISPATCHER_ONE_TO_MANY_H_

#include "kmsbasehub.h"

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
