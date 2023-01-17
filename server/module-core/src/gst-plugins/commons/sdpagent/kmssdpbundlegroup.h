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

#ifndef __KMS_SDP_BUNDLE_GROUP_H__
#define __KMS_SDP_BUNDLE_GROUP_H__

#include "kmssdpbasegroup.h"

G_BEGIN_DECLS

#define KMS_TYPE_SDP_BUNDLE_GROUP \
  (kms_sdp_bundle_group_get_type())

#define KMS_SDP_BUNDLE_GROUP(obj) (    \
  G_TYPE_CHECK_INSTANCE_CAST (         \
    (obj),                             \
    KMS_TYPE_SDP_BUNDLE_GROUP,         \
    KmsSdpBundleGroup                  \
  )                                    \
)
#define KMS_SDP_BUNDLE_GROUP_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                  \
    (klass),                                 \
    KMS_TYPE_SDP_BUNDLE_GROUP,               \
    KmsSdpBundleGroupClass                   \
  )                                          \
)
#define KMS_IS_SDP_BUNDLE_GROUP(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (         \
    (obj),                             \
    KMS_TYPE_SDP_BUNDLE_GROUP          \
  )                                    \
)
#define KMS_IS_SDP_BUNDLE_GROUP_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_BUNDLE_GROUP))
#define KMS_SDP_BUNDLE_GROUP_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                  \
    (obj),                                     \
    KMS_TYPE_SDP_BUNDLE_GROUP,                 \
    KmsSdpBundleGroupClass                     \
  )                                            \
)

typedef struct _KmsSdpBundleGroup KmsSdpBundleGroup;
typedef struct _KmsSdpBundleGroupClass KmsSdpBundleGroupClass;
typedef struct _KmsSdpBundleGroupPrivate KmsSdpBundleGroupPrivate;

struct _KmsSdpBundleGroup
{
  KmsSdpBaseGroup parent;

  /*< private > */
  KmsSdpBundleGroupPrivate *priv;
};

struct _KmsSdpBundleGroupClass
{
  KmsSdpBaseGroupClass parent_class;
};

GType kms_sdp_bundle_group_get_type ();

KmsSdpBundleGroup * kms_sdp_bundle_group_new ();

G_END_DECLS

#endif /* __KMS_SDP_BUNDLE_GROUP_H__ */
