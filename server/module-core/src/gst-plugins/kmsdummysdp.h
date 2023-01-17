/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
#ifndef _KMS_DUMMY_SDP_H_
#define _KMS_DUMMY_SDP_H_

#include "kmselement.h"
#include "commons/kmsbasesdpendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_DUMMY_SDP       \
  (kms_dummy_sdp_get_type())
#define KMS_DUMMY_SDP(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST(     \
    (obj),                        \
    KMS_TYPE_DUMMY_SDP,        \
    KmsDummySdp                \
  )                               \
)

#define KMS_DUMMY_SDP_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_DUMMY_SDP,              \
    KmsDummySdpClass                 \
  )                                     \
)
#define KMS_IS_DUMMY_SDP(obj) (  \
  G_TYPE_CHECK_INSTANCE_TYPE (      \
    (obj),                          \
    KMS_TYPE_DUMMY_SDP           \
  )                                 \
)
#define KMS_IS_DUMMY_SDP_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                  \
    (klass),                                \
    KMS_TYPE_DUMMY_SDP                   \
  )                                         \
)
typedef struct _KmsDummySdp KmsDummySdp;
typedef struct _KmsDummySdpClass KmsDummySdpClass;
typedef struct _KmsDummySdpPrivate KmsDummySdpPrivate;

struct _KmsDummySdp
{
  KmsBaseSdpEndpoint parent;

  /*< private > */
  KmsDummySdpPrivate *priv;
};

struct _KmsDummySdpClass
{
  KmsBaseSdpEndpointClass parent_class;
};

GType kms_dummy_sdp_get_type (void);

gboolean kms_dummy_sdp_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_DUMMY_SDP_H_ */
