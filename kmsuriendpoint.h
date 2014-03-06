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
#ifndef _KMS_URI_ENDPOINT_H_
#define _KMS_URI_ENDPOINT_H_

#include "kmselement.h"
#include "kmsuriendpointstate.h"

G_BEGIN_DECLS
#define KMS_TYPE_URI_ENDPOINT \
  (kms_uri_endpoint_get_type())
#define KMS_URI_ENDPOINT(obj) (            \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_URI_ENDPOINT,                 \
    KmsUriEndpoint                         \
  )                                        \
)
#define KMS_URI_ENDPOINT_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_URI_ENDPOINT,                 \
    KmsUriEndpointClass                    \
  )                                        \
)
#define KMS_IS_URI_ENDPOINT(obj) (         \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_URI_ENDPOINT                  \
  )                                        \
)
#define KMS_IS_URI_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_URI_ENDPOINT))
#define KMS_URI_ENDPOINT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_URI_ENDPOINT,                 \
    KmsUriEndpointClass                    \
  )                                        \
)
typedef struct _KmsUriEndpoint KmsUriEndpoint;
typedef struct _KmsUriEndpointClass KmsUriEndpointClass;
typedef struct _KmsUriEndpointPrivate KmsUriEndpointPrivate;

struct _KmsUriEndpoint
{
  KmsElement parent;

  /*< private > */
  KmsUriEndpointPrivate *priv;

  /*< protected > */
  gchar *uri;
};

struct _KmsUriEndpointClass
{
  KmsElementClass parent_class;

  /*< protected >*/
  void (*change_state) (KmsUriEndpoint *self, KmsUriEndpointState state);

  /*< protected abstract methods > */
  void (*stopped) (KmsUriEndpoint *self);
  void (*started) (KmsUriEndpoint *self);
  void (*paused) (KmsUriEndpoint *self);

  /*< Signals >*/
  void (*state_changed) (KmsUriEndpoint *self, KmsUriEndpointState state);
};

GType kms_uri_endpoint_get_type (void);

gboolean kms_uri_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
