/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#ifndef _KMS_DISPATCHER_H_
#define _KMS_DISPATCHER_H_

#include "kmsbasehub.h"

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
