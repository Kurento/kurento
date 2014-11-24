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
};

GType kms_http_post_endpoint_get_type (void);

gboolean kms_http_post_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_HTTP_POST_ENDPOINT_H_ */
