/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifndef _KMS_FILTER_ELEMENT_H_
#define _KMS_FILTER_ELEMENT_H_

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_FILTER_ELEMENT (kms_filter_element_get_type())
#define KMS_FILTER_ELEMENT(obj) (               \
  G_TYPE_CHECK_INSTANCE_CAST (                  \
    (obj),                                      \
    KMS_TYPE_FILTER_ELEMENT,                    \
    KmsFilterElement                            \
  )                                             \
)
#define KMS_FILTER_ELEMENT_CLASS(klass) (       \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_FILTER_ELEMENT,                    \
    KmsFilterElementClass                       \
  )                                             \
)
#define KMS_IS_FILTER_ELEMENT(obj) (            \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_FILTER_ELEMENT                     \
    )                                           \
)
#define KMS_IS_FILTER_ELEMENT_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_TYPE (                     \
  (klass),                                      \
  KMS_TYPE_FILTER_ELEMENT                       \
  )                                             \
)
typedef struct _KmsFilterElement KmsFilterElement;
typedef struct _KmsFilterElementClass KmsFilterElementClass;
typedef struct _KmsFilterElementPrivate KmsFilterElementPrivate;

struct _KmsFilterElement
{
  KmsElement parent;

  /*< private > */
  KmsFilterElementPrivate *priv;
};

struct _KmsFilterElementClass
{
  KmsElementClass parent_class;
};

GType kms_filter_element_get_type (void);

gboolean kms_filter_element_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
