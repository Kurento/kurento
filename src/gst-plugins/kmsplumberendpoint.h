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
#ifndef _KMS_PLUMBER_ENDPOINT_H_
#define _KMS_PLUMBER_ENDPOINT_H_

#include <commons/kmselement.h>

G_BEGIN_DECLS
#define KMS_TYPE_PLUMBER_ENDPOINT \
  (kms_plumber_endpoint_get_type())
#define KMS_PLUMBER_ENDPOINT(obj) (           \
  G_TYPE_CHECK_INSTANCE_CAST(              \
    (obj),                                 \
    KMS_TYPE_PLUMBER_ENDPOINT,                \
    KmsPlumberEndpoint                        \
  )                                        \
)
#define KMS_PLUMBER_ENDPOINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_PLUMBER_ENDPOINT,                \
    KmsPlumberEndpointClass                   \
  )                                        \
)
#define KMS_IS_PLUMBER_ENDPOINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_PLUMBER_ENDPOINT                 \
  )                                        \
)
#define KMS_IS_PLUMBER_ENDPOINT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                   \
    (klass),                                 \
    KMS_TYPE_PLUMBER_ENDPOINT                   \
  )                                          \
)
typedef struct _KmsPlumberEndpoint KmsPlumberEndpoint;
typedef struct _KmsPlumberEndpointClass KmsPlumberEndpointClass;
typedef struct _KmsPlumberEndpointPrivate KmsPlumberEndpointPrivate;

struct _KmsPlumberEndpoint
{
  KmsElement parent;

  /*< private > */
  KmsPlumberEndpointPrivate *priv;
};

struct _KmsPlumberEndpointClass
{
  KmsElementClass parent_class;

  /* actions */
  gboolean (*accept) (KmsPlumberEndpoint * self);
  gboolean (*connect) (KmsPlumberEndpoint * self, gchar *host, guint port);
};

GType kms_plumber_endpoint_get_type (void);

gboolean kms_plumber_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_PLUMBER_ENDPOINT_H_ */