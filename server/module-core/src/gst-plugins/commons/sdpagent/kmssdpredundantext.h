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
#ifndef _KMS_SDP_REDUNDANT_EXT_H_
#define _KMS_SDP_REDUNDANT_EXT_H_

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_SDP_REDUNDANT_EXT \
  (kms_sdp_redundant_ext_get_type())

#define KMS_SDP_REDUNDANT_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (       \
    (obj),                           \
    KMS_TYPE_SDP_REDUNDANT_EXT,      \
    KmsSdpRedundantExt               \
  )                                  \
)
#define KMS_SDP_REDUNDANT_EXT_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                  \
    (klass),                                 \
    KMS_TYPE_SDP_REDUNDANT_EXT,              \
    KmsSdpRedundantExtClass                  \
  )                                          \
)
#define KMS_IS_SDP_REDUNDANT_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_SDP_REDUNDANT_EXT          \
  )                                     \
)
#define KMS_IS_SDP_REDUNDANT_EXT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_REDUNDANT_EXT))
#define KMS_SDP_REDUNDANT_EXT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                   \
    (obj),                                      \
    KMS_TYPE_SDP_REDUNDANT_EXT,                 \
    KmsSdpRedundantExtClass                     \
  )                                             \
)

typedef struct _KmsSdpRedundantExt KmsSdpRedundantExt;
typedef struct _KmsSdpRedundantExtClass KmsSdpRedundantExtClass;
typedef struct _KmsSdpRedundantExtPrivate KmsSdpRedundantExtPrivate;

struct _KmsSdpRedundantExt
{
  GObject parent;

  /*< private > */
  KmsSdpRedundantExtPrivate *priv;
};

struct _KmsSdpRedundantExtClass
{
  GObjectClass parent_class;

  /* signals */
  /* TODO: Provide information about primary, secondary, tertiary.. encodings */
  gboolean (*on_offered_redundancy) (KmsSdpRedundantExt * ext, guint pt, guint clock_rate);
};

GType kms_sdp_redundant_ext_get_type ();

KmsSdpRedundantExt * kms_sdp_redundant_ext_new ();

#endif /* _KMS_SDP_REDUNDANT_EXT_H_ */
