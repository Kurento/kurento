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

/* inclusion guard */
#ifndef __KMS_HTTP_POST_H__
#define __KMS_HTTP_POST_H__

#include <gst/gst.h>
#include <glib-object.h>

/*
 * Type macros.
 */
#define KMS_TYPE_HTTP_POST (            \
  kms_http_post_get_type ()             \
)

#define KMS_HTTP_POST(obj) (            \
  G_TYPE_CHECK_INSTANCE_CAST (          \
    (obj),                              \
    KMS_TYPE_HTTP_POST,                 \
    KmsHttpPost                         \
  )                                     \
)

#define KMS_IS_HTTP_POST(obj) (         \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_HTTP_POST                  \
  )                                     \
)

#define KMS_HTTP_POST_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_HTTP_POST,                 \
    KmsHttpPostClass                    \
  )                                     \
)

#define KMS_IS_HTTP_POST_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE (             \
    (klass),                            \
    KMS_TYPE_HTTP_POST                  \
  )                                     \
)

#define KMS_HTTP_POST_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (           \
    (obj),                              \
    KMS_TYPE_HTTP_POST,                 \
    KmsHttpPostClass)                   \
)

typedef struct _KmsHttpPost KmsHttpPost;
typedef struct _KmsHttpPostClass KmsHttpPostClass;
typedef struct _KmsHttpPostPrivate KmsHttpPostPrivate;

struct _KmsHttpPost
{
  GObject parent_instance;

  /*< private > */
  KmsHttpPostPrivate *priv;
};

struct _KmsHttpPostClass
{
  GObjectClass parent_class;

  /* signal callbacks */
  void (*got_data) (KmsHttpPost * self, SoupBuffer *buffer);
  void (*finished) (KmsHttpPost * self);
};

/* used by KMS_TYPE_HTTP_POST */
GType kms_http_post_get_type (void);

KmsHttpPost * kms_http_post_new ();

#endif /* __KMS_HTTP_POST_H__ */