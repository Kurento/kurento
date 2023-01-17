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
#ifndef _KMS_PASS_THROUGH_H_
#define _KMS_PASS_THROUGH_H_

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_PASS_THROUGH (kms_pass_through_get_type())
#define KMS_PASS_THROUGH(obj) (                 \
  G_TYPE_CHECK_INSTANCE_CAST (                  \
    (obj),                                      \
    KMS_TYPE_PASS_THROUGH,                      \
    KmsPassThrough                              \
  )                                             \
)
#define KMS_PASS_THROUGH_CLASS(klass) (         \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_PASS_THROUGH,                      \
    KmsPassThroughClass                         \
  )                                             \
)
#define KMS_IS_PASS_THROUGH(obj) (              \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_PASS_THROUGH                       \
    )                                           \
)
#define KMS_IS_PASS_THROUGH_CLASS(klass) (      \
  G_TYPE_CHECK_CLASS_TYPE (                     \
  (klass),                                      \
  KMS_TYPE_PASS_THROUGH                         \
  )                                             \
)
typedef struct _KmsPassThrough KmsPassThrough;
typedef struct _KmsPassThroughClass KmsPassThroughClass;

struct _KmsPassThrough
{
  KmsElement parent;
};

struct _KmsPassThroughClass
{
  KmsElementClass parent_class;
};

GType kms_pass_through_get_type (void);

gboolean kms_pass_through_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
