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
#ifndef _KMS_HTTP_GET_ENDPOINT_H_
#define _KMS_HTTP_GET_ENDPOINT_H_

#include "kmshttpendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_HTTP_GET_ENDPOINT \
  (kms_http_get_endpoint_get_type())
#define KMS_HTTP_GET_ENDPOINT(obj) (       \
  G_TYPE_CHECK_INSTANCE_CAST(              \
    (obj),                                 \
    KMS_TYPE_HTTP_GET_ENDPOINT,            \
    KmsHttpGetEndpoint                     \
  )                                        \
)
#define KMS_HTTP_GET_ENDPOINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                    \
    (klass),                                   \
    KMS_TYPE_HTTP_GET_ENDPOINT,                \
    KmsHttpGetEndpointClass                    \
  )                                            \
)
#define KMS_IS_HTTP_GET_ENDPOINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (                 \
    (obj),                                     \
    KMS_TYPE_HTTP_GET_ENDPOINT                 \
  )                                            \
)
#define KMS_IS_HTTP_GET_ENDPOINT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                       \
    (klass),                                     \
    KMS_TYPE_HTTP_GET_ENDPOINT                   \
  )                                              \
)
typedef struct _KmsHttpGetEndpoint KmsHttpGetEndpoint;
typedef struct _KmsHttpGetEndpointClass KmsHttpGetEndpointClass;
typedef struct _KmsHttpGetEndpointPrivate KmsHttpGetEndpointPrivate;

struct _KmsHttpGetEndpoint
{
  KmsHttpEndpoint parent;

  /*< private > */
  KmsHttpGetEndpointPrivate *priv;
};

struct _KmsHttpGetEndpointClass
{
  KmsHttpEndpointClass parent_class;
};

GType kms_http_get_endpoint_get_type (void);

gboolean kms_http_get_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_HTTP_GET_ENDPOINT_H */
