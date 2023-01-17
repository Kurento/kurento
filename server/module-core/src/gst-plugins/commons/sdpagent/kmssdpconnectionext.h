/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
#ifndef _KMS_CONNECTION_EXT_H_
#define _KMS_CONNECTION_EXT_H_

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_CONNECTION_EXT \
  (kms_connection_ext_get_type())

#define KMS_CONNECTION_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (    \
    (obj),                        \
    KMS_TYPE_CONNECTION_EXT,      \
    KmsConnectionExt              \
  )                               \
)
#define KMS_CONNECTION_EXT_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (               \
    (klass),                              \
    KMS_TYPE_CONNECTION_EXT,              \
    KmsConnectionExtClass                 \
  )                                       \
)
#define KMS_IS_CONNECTION_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (       \
    (obj),                           \
    KMS_TYPE_CONNECTION_EXT          \
  )                                  \
)
#define KMS_IS_CONNECTION_EXT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_CONNECTION_EXT))
#define KMS_CONNECTION_EXT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                \
    (obj),                                   \
    KMS_TYPE_CONNECTION_EXT,                 \
    KmsConnectionExtClass                    \
  )                                          \
)

typedef struct _KmsConnectionExt KmsConnectionExt;
typedef struct _KmsConnectionExtClass KmsConnectionExtClass;

struct _KmsConnectionExt
{
  GObject parent;
};

/* Array of Ips:
 * Array [
 *   struct_0 {
 *     nettype: string,   ["IN" for internet]
 *     addrtype : string, ["IP4", "IP4"]
 *     address : string,
 *     ttl: uint,
 *     addrnumber: uint
 *   },
 *   struct_1 {
 *     nettype: string,
 *     addrtype : string,
 *     address : string,
 *     ttl: uint,
 *     addrnumber: uint
 *   },
 *   ...
 *   struct_N {
 *     ...
 *   }
 * ]
 */

struct _KmsConnectionExtClass
{
  GObjectClass parent_class;

  /* Emitted when an offer is processed */
  void (*on_answer_ips) (KmsConnectionExt * ext,
    const GArray * ips_offered, GArray * ips_answered);

  /* Emitted when an offer is going to be created  */
  void (*on_offer_ips) (KmsConnectionExt * ext, GArray * ips);

  /* Emmited as a result of precessing the previous offer generated */
  void (*on_answered_ips) (KmsConnectionExt * ext, const GArray * ips);
};

GType kms_connection_ext_get_type ();

KmsConnectionExt * kms_connection_ext_new ();

G_END_DECLS

#endif /* _KMS_CONNECTION_EXT_H_ */
