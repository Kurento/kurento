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
#ifndef _KMS_SDP_SDES_EXT_H_
#define _KMS_SDP_SDES_EXT_H_

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_SDP_SDES_EXT \
  (kms_sdp_sdes_ext_get_type())

#define KMS_SDP_SDES_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (  \
    (obj),                      \
    KMS_TYPE_SDP_SDES_EXT,      \
    KmsSdpSdesExt               \
  )                             \
)
#define KMS_SDP_SDES_EXT_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_SDP_SDES_EXT,              \
    KmsSdpSdesExtClass                  \
  )                                     \
)
#define KMS_IS_SDP_SDES_EXT(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (     \
    (obj),                         \
    KMS_TYPE_SDP_SDES_EXT          \
  )                                \
)
#define KMS_IS_SDP_SDES_EXT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_SDES_EXT))
#define KMS_SDP_SDES_EXT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_SDP_SDES_EXT,                 \
    KmsSdpSdesExtClass                     \
  )                                        \
)

typedef struct _KmsSdpSdesExt KmsSdpSdesExt;
typedef struct _KmsSdpSdesExtClass KmsSdpSdesExtClass;
typedef struct _KmsSdpSdesExtPrivate KmsSdpSdesExtPrivate;

struct _KmsSdpSdesExt
{
  GObject parent;

  /*< private > */
  KmsSdpSdesExtPrivate *priv;
};

struct _KmsSdpSdesExtClass
{
  GObjectClass parent_class;

  /* signals */
  GArray * (*on_offer_keys) (KmsSdpSdesExt * ext);
  gboolean (*on_answer_keys) (KmsSdpSdesExt * ext, const GArray * keys, GValue *key);
  void (*on_selected_key) (KmsSdpSdesExt * ext, const GValue key);
};

GType kms_sdp_sdes_ext_get_type ();

KmsSdpSdesExt * kms_sdp_sdes_ext_new ();

#define KMS_SDES_TAG_FIELD "tag"
#define KMS_SDES_KEY_FIELD "key"
#define KMS_SDES_CRYPTO "crypto"
#define KMS_SDES_LIFETIME "lifetime"
#define KMS_SDES_MKI "mki"
#define KMS_SDES_LENGTH "length"

typedef enum {
  KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_32, /* from rfc4568 */
  KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_80, /* from rfc4568 */
  KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_32, /* from rfc6188 */
  KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_80  /* from rfc6188 */
} SrtpCryptoSuite;

#define kms_sdp_sdes_ext_create_key(tag, key, crypto, val) \
  kms_sdp_sdes_ext_create_key_detailed (tag, key, crypto, NULL, NULL, NULL, val, NULL)

gboolean kms_sdp_sdes_ext_create_key_detailed (guint tag, const gchar *key,
  SrtpCryptoSuite crypto, const gchar *lifetime, const guint *mki,
  const guint *length, GValue *val, GError **error);

gboolean kms_sdp_sdes_ext_get_parameters_from_key (const GValue *key,
  const char *first_param, ...);

G_END_DECLS

#endif /* _KMS_SDP_SDES_EXT_H_ */
