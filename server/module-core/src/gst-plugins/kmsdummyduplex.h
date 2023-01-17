/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#ifndef _KMS_DUMMY_DUPLEX_H_
#define _KMS_DUMMY_DUPLEX_H_

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_DUMMY_DUPLEX       \
  (kms_dummy_duplex_get_type())
#define KMS_DUMMY_DUPLEX(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST(     \
    (obj),                        \
    KMS_TYPE_DUMMY_DUPLEX,        \
    KmsDummyDuplex                \
  )                               \
)
#define KMS_DUMMY_DUPLEX_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_DUMMY_DUPLEX,              \
    KmsDummyDuplexClass                 \
  )                                     \
)
#define KMS_IS_DUMMY_DUPLEX(obj) (  \
  G_TYPE_CHECK_INSTANCE_TYPE (      \
    (obj),                          \
    KMS_TYPE_DUMMY_DUPLEX           \
  )                                 \
)
#define KMS_IS_DUMMY_DUPLEX_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                  \
    (klass),                                \
    KMS_TYPE_DUMMY_DUPLEX                   \
  )                                         \
)
typedef struct _KmsDummyDuplex KmsDummyDuplex;
typedef struct _KmsDummyDuplexClass KmsDummyDuplexClass;
typedef struct _KmsDummyDuplexPrivate KmsDummyDuplexPrivate;

struct _KmsDummyDuplex
{
  KmsElement parent;

  /*< private > */
  KmsDummyDuplexPrivate *priv;
};

struct _KmsDummyDuplexClass
{
  KmsElementClass parent_class;
};

GType kms_dummy_duplex_get_type (void);

gboolean kms_dummy_duplex_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_DUMMY_DUPLEX_H_ */
