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
#ifndef _KMS_RECORDER_ENDPOINT_H_
#define _KMS_RECORDER_ENDPOINT_H_

#include "kmsuriendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_RECORDER_ENDPOINT \
  (kms_recorder_endpoint_get_type())
#define KMS_RECORDER_ENDPOINT(obj) (           \
  G_TYPE_CHECK_INSTANCE_CAST(                  \
    (obj),                                     \
    KMS_TYPE_RECORDER_ENDPOINT,                \
    KmsRecorderEndpoint                        \
  )                                            \
)
#define KMS_RECORDER_ENDPOINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                    \
    (klass),                                   \
    KMS_TYPE_RECORDER_ENDPOINT,                \
    KmsRecorderEndpointClass                   \
  )                                            \
)
#define KMS_IS_RECORDER_ENDPOINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (                 \
    (obj),                                     \
    KMS_TYPE_RECORDER_ENDPOINT                 \
  )                                            \
)
#define KMS_IS_RECORDER_ENDPOINT_CLASS(klass) (         \
  G_TYPE_CHECK_CLASS_TYPE((klass),                      \
  KMS_TYPE_RECORDER_ENDPOINT)                           \
)
typedef struct _KmsRecorderEndpoint KmsRecorderEndpoint;
typedef struct _KmsRecorderEndpointClass KmsRecorderEndpointClass;
typedef struct _KmsRecorderEndpointPrivate KmsRecorderEndpointPrivate;

struct _KmsRecorderEndpoint
{
  KmsUriEndpoint parent;

  /*< private > */
  KmsRecorderEndpointPrivate *priv;
};

struct _KmsRecorderEndpointClass
{
  KmsUriEndpointClass parent_class;
};

GType kms_recorder_endpoint_get_type (void);

gboolean kms_recorder_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
