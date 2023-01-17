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
#ifndef _KMS_SDP_ULP_FEC_EXT_H_
#define _KMS_SDP_ULP_FEC_EXT_H_

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_SDP_ULP_FEC_EXT \
  (kms_sdp_ulp_fec_ext_get_type())

#define KMS_SDP_ULP_FEC_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (     \
    (obj),                         \
    KMS_TYPE_SDP_ULP_FEC_EXT,      \
    KmsSdpUlpFecExt                \
  )                                \
)
#define KMS_SDP_ULP_FEC_EXT_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_SDP_ULP_FEC_EXT,              \
    KmsSdpUlpFecExtClass                   \
  )                                        \
)
#define KMS_IS_SDP_ULP_FEC_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (        \
    (obj),                            \
    KMS_TYPE_SDP_ULP_FEC_EXT          \
  )                                   \
)
#define KMS_IS_SDP_ULP_FEC_EXT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_ULP_FEC_EXT))
#define KMS_SDP_ULP_FEC_EXT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                 \
    (obj),                                    \
    KMS_TYPE_SDP_ULP_FEC_EXT,                 \
    KmsSdpUlpFecExtClass                      \
  )                                           \
)

typedef struct _KmsSdpUlpFecExt KmsSdpUlpFecExt;
typedef struct _KmsSdpUlpFecExtClass KmsSdpUlpFecExtClass;
typedef struct _KmsSdpUlpFecExtPrivate KmsSdpUlpFecExtPrivate;

struct _KmsSdpUlpFecExt
{
  GObject parent;

  /*< private > */
  KmsSdpUlpFecExtPrivate *priv;
};

struct _KmsSdpUlpFecExtClass
{
  GObjectClass parent_class;

  /* signals */
  gboolean (*on_offered_ulpfec) (KmsSdpUlpFecExt * ext, guint pt, guint clock_rate);
};

GType kms_sdp_ulp_fec_ext_get_type ();

KmsSdpUlpFecExt * kms_sdp_ulp_fec_ext_new ();

#endif /* _KMS_SDP_ULP_FEC_EXT_H_ */
