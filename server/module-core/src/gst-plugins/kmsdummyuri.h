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
#ifndef _KMS_DUMMY_URI_H_
#define _KMS_DUMMY_URI_H_

#include "kmselement.h"
#include "commons/kmsuriendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_DUMMY_URI       \
  (kms_dummy_uri_get_type())
#define KMS_DUMMY_URI(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST(     \
    (obj),                        \
    KMS_TYPE_DUMMY_URI,        \
    KmsDummyUri                \
  )                               \
)

#define KMS_DUMMY_URI_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_DUMMY_URI,              \
    KmsDummyUriClass                 \
  )                                     \
)
#define KMS_IS_DUMMY_URI(obj) (  \
  G_TYPE_CHECK_INSTANCE_TYPE (      \
    (obj),                          \
    KMS_TYPE_DUMMY_URI           \
  )                                 \
)
#define KMS_IS_DUMMY_URI_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(                  \
    (klass),                                \
    KMS_TYPE_DUMMY_URI                   \
  )                                         \
)
typedef struct _KmsDummyUri KmsDummyUri;
typedef struct _KmsDummyUriClass KmsDummyUriClass;
typedef struct _KmsDummyUriPrivate KmsDummyUriPrivate;

struct _KmsDummyUri
{
  KmsUriEndpoint parent;

  /*< private > */
  KmsDummyUriPrivate *priv;
};

struct _KmsDummyUriClass
{
  KmsUriEndpointClass parent_class;
};

GType kms_dummy_uri_get_type (void);

gboolean kms_dummy_uri_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_DUMMY_URI_H_ */
