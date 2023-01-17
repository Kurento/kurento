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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>

#include "kmsisdpmediaextension.h"
#include "kmssdpsdesext.h"
#include "kms-sdp-agent-marshal.h"
#include "kmssdpagent.h"
#include <gobject/gvaluecollector.h>

#define OBJECT_NAME "sdpsdesext"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_sdes_ext_debug_category);
#define GST_CAT_DEFAULT kms_sdp_sdes_ext_debug_category

#define parent_class kms_sdp_sdes_ext_parent_class

#define CRYPTO_ATTR "crypto"
#define KEY_METHOD "inline"

static void kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsSdpSdesExt, kms_sdp_sdes_ext,
    G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_MEDIA_EXTENSION,
        kms_i_sdp_media_extension_init)
    GST_DEBUG_CATEGORY_INIT (kms_sdp_sdes_ext_debug_category, OBJECT_NAME,
        0, "debug category for sdp sdes_ext"));

#define index_of(str,chr) ({      \
  gint __pos = -1;                \
  gchar *__c;                     \
  __c = strchr ((str), (chr));    \
  if (__c != NULL) {              \
    __pos = (gint)(__c - (str));  \
  }                               \
  __pos;                          \
})

#define MAX_CRYPTO_TAG 999999999
#define is_valid_tag(tag) ((tag) <= MAX_CRYPTO_TAG)
enum
{
  SIGNAL_ON_OFFER_KEYS,
  SIGNAL_ON_ANSWER_KEYS,
  SIGNAL_ON_SELECTED_KEY,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static const gchar *crypto_suites[] = {
  "AES_CM_128_HMAC_SHA1_32",
  "AES_CM_128_HMAC_SHA1_80",
  "AES_256_CM_HMAC_SHA1_32",
  "AES_256_CM_HMAC_SHA1_80"
};

static const gchar *
srtp_crypto_suite_to_str (SrtpCryptoSuite crypto)
{
  int suite = crypto;

  if (suite <= G_N_ELEMENTS (crypto_suites)) {
    return crypto_suites[crypto];
  } else {
    return NULL;
  }
}

static gboolean
srtp_crypto_suite_from_str (const gchar * str, SrtpCryptoSuite * crypto)
{
  guint i;

  for (i = 0; i < G_N_ELEMENTS (crypto_suites); i++) {
    if (g_strcmp0 (str, crypto_suites[i]) == 0) {
      *crypto = i;
      return TRUE;
    }
  }

  return FALSE;
}

static const GstStructure *
get_structure_from_value (const GValue * val)
{
  if (!GST_VALUE_HOLDS_STRUCTURE (val)) {
    return NULL;
  }

  return gst_value_get_structure (val);
}

static gboolean
is_expected_param (const gchar * param)
{
  return g_strcmp0 (param, KMS_SDES_TAG_FIELD) == 0 ||
      g_strcmp0 (param, KMS_SDES_KEY_FIELD) == 0 ||
      g_strcmp0 (param, KMS_SDES_CRYPTO) == 0;
}

static gboolean
get_parameters_from_key_structure (const GstStructure * str,
    const char *first_param, va_list args)
{
  GType expected_type = G_TYPE_INVALID;
  const char *field_name;

  field_name = first_param;
  while (field_name) {
    const GValue *val = NULL;
    gboolean expected;
    gchar *desc = NULL;

    expected_type = va_arg (args, GType);

    val = gst_structure_get_value (str, field_name);
    expected = is_expected_param (field_name);

    if (val == NULL) {
      if (expected) {
        return FALSE;
      } else {
        /* optional parameter. Look for another ones */
        va_arg (args, gpointer);
        field_name = va_arg (args, const gchar *);

        continue;
      }
    }

    if (G_VALUE_TYPE (val) != expected_type) {
      /* values dont have the same type */
      return FALSE;
    }

    G_VALUE_LCOPY (val, args, 0, &desc);
    if (desc != NULL) {
      g_free (desc);
      return FALSE;
    }

    field_name = va_arg (args, const gchar *);
  }

  return TRUE;
}

static const GValue *
get_key_by_tag (const GArray * keys, guint tag)
{
  const GValue *key;
  guint i;

  for (i = 0; i < keys->len; i++) {
    guint t;

    key = &g_array_index (keys, GValue, i);

    if (!kms_sdp_sdes_ext_get_parameters_from_key (key, KMS_SDES_TAG_FIELD,
            G_TYPE_UINT, &t, NULL)) {
      continue;
    }

    if (t == tag) {
      return key;
    }
  }

  return NULL;
}

static gboolean
kms_sdp_sdes_ext_add_crypto_attr (KmsISdpMediaExtension * ext,
    GstSDPMedia * media, guint tag, const gchar * key, SrtpCryptoSuite crypto,
    const gchar * lifetime, const guint * mki, const guint * len,
    GError ** error)
{
  const gchar *crypto_str;
  gboolean ret = TRUE;
  gchar *val, *tmp;

  crypto_str = srtp_crypto_suite_to_str (crypto);
  if (crypto_str == NULL) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Invalid crypto suite provided (%u)", crypto);

    return FALSE;
  }

  val = g_strdup_printf ("%u %s %s:%s", tag, crypto_str, KEY_METHOD, key);

  if (lifetime != NULL) {
    tmp = val;
    val = g_strdup_printf ("%s|%s", tmp, lifetime);
    g_free (tmp);
  }

  if (mki != NULL && len != NULL) {
    tmp = val;
    val = g_strdup_printf ("%s|%u:%u", tmp, *mki, *len);
    g_free (tmp);
  }

  if (gst_sdp_media_add_attribute (media, CRYPTO_ATTR, val) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Can not add " CRYPTO_ATTR "  attribute [%u]", tag);

    ret = FALSE;
  }

  g_free (val);

  return ret;
}

static gboolean
kms_sdp_sdes_ext_add_offer_crypto_attrs (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GArray * keys, GError ** error)
{
  gboolean ret = TRUE;
  guint i;

  for (i = 0; i < keys->len; i++) {
    const GstStructure *str;
    SrtpCryptoSuite crypto;
    gchar *key, *lifetime;
    guint tag, *mki, *len;
    GValue *val;

    key = lifetime = NULL;
    mki = len = NULL;

    val = &g_array_index (keys, GValue, i);

    if (!GST_VALUE_HOLDS_STRUCTURE (val)) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
          "Can not add crypto attribute for key %u", i);
      return FALSE;
    }

    str = gst_value_get_structure (val);

    if (!gst_structure_get (str, KMS_SDES_TAG_FIELD, G_TYPE_UINT, &tag,
            KMS_SDES_KEY_FIELD, G_TYPE_STRING, &key, KMS_SDES_CRYPTO,
            G_TYPE_UINT, &crypto, NULL)) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
          "Can not add crypto attribute for key %u", i);
      ret = FALSE;
    }

    if (ret) {
      guint m, l;

      /* get optional parameters */
      gst_structure_get (str, KMS_SDES_LIFETIME, G_TYPE_STRING, &lifetime,
          NULL);

      if (gst_structure_get (str, KMS_SDES_MKI, G_TYPE_UINT, &m,
              KMS_SDES_LENGTH, G_TYPE_UINT, &l, NULL)) {
        mki = &m;
        len = &l;
      }

      ret = kms_sdp_sdes_ext_add_crypto_attr (ext, offer, tag, key, crypto,
          lifetime, mki, len, error);
    }

    g_free (key);
    g_free (lifetime);

    if (!ret) {
      /* Something went wrong */
      break;
    }
  }

  return ret;
}

static gboolean
kms_sdp_sdes_ext_add_offer_attributes (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GError ** error)
{
  gboolean ret = TRUE;
  GArray *keys;

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_OFFER_KEYS], 0, &keys);

  if (keys != NULL) {
    ret = kms_sdp_sdes_ext_add_offer_crypto_attrs (ext, offer, keys, error);
    g_array_unref (keys);
  } else {
    GST_DEBUG_OBJECT (ext, "No keys provided in offer");
  }

  return ret;
}

static gboolean
kms_sdp_sdes_ext_extract_key_params (KmsISdpMediaExtension * ext,
    const gchar * key_params, gchar ** key, gchar ** lifetime, gint64 * mki,
    gint64 * len)
{
  gboolean ret = FALSE;
  gchar **attrs, *str;
  const gchar *tmp;
  gint i, l;

  attrs = g_strsplit (key_params, "|", 0);

  if (attrs[0] == NULL) {
    GST_ERROR_OBJECT (ext, "Noy key provided in crypto attribute");
    goto end;
  }

  *key = g_strdup (attrs[0]);

  ret = TRUE;

  if (attrs[1] == NULL) {
    /* no optional parameters */
    goto end;
  }

  i = index_of (attrs[1], ':');
  if (i < 0) {
    /* this is a lifetime parameter */
    *lifetime = g_strdup (attrs[1]);
    if (attrs[2] == NULL) {
      /* There is neither mki nor length parameters */
      goto end;
    } else {
      tmp = attrs[2];
      i = index_of (tmp, ':');
      if (i < 0) {
        GST_ERROR_OBJECT (ext, "Bad key parameters format: '%s'", tmp);
        ret = FALSE;
        goto end;
      }
    }
  } else {
    tmp = attrs[1];
  }

  /* get mki and length parameters */
  str = g_strndup (tmp, i);
  *mki = g_ascii_strtoll (attrs[0], NULL, 10);
  l = strlen (str);
  g_free (str);

  str = g_strndup (tmp + l + 1, strlen (tmp) - l);
  *len = g_ascii_strtoll (attrs[0], NULL, 10);
  g_free (str);

end:
  g_strfreev (attrs);

  return ret;
}

static gboolean
kms_sdp_sdes_ext_parse_key_attr (KmsISdpMediaExtension * ext,
    const gchar * attr_val, GValue * val)
{
  gchar **attrs, *key_params, *tmp, *key, *lifetime;
  SrtpCryptoSuite crypto;
  gboolean ret = FALSE;
  GError *err = NULL;
  gint64 mki, len;
  guint64 tag;

  key = lifetime = NULL;
  mki = len = G_GINT64_CONSTANT (-1);

  attrs = g_strsplit (attr_val, " ", 0);

  if (attrs[0] == NULL) {
    GST_ERROR_OBJECT (ext, "Bad crypto attribute format");
    goto end;
  }

  tag = g_ascii_strtoull (attrs[0], NULL, 10);

  if (attrs[1] == NULL) {
    GST_ERROR_OBJECT (ext, "No crypto suite provided");
    goto end;
  }

  if (!srtp_crypto_suite_from_str (attrs[1], &crypto)) {
    GST_ERROR_OBJECT (ext, "Unsupported crypto-suite provided: '%s'", attrs[1]);
    goto end;
  }

  if (attrs[2] == NULL) {
    GST_ERROR_OBJECT (ext, "No key parameters provided");
    goto end;
  }

  tmp = attrs[2];
  if (!g_str_has_prefix (tmp, KEY_METHOD ":")) {
    GST_ERROR_OBJECT (ext, "Unsupported key method provided in '%s'", tmp);
    goto end;
  }

  key_params = g_strndup (tmp + strlen (KEY_METHOD) + 1,
      strlen (tmp) - (strlen (KEY_METHOD) + 1));

  if (!kms_sdp_sdes_ext_extract_key_params (ext, key_params, &key, &lifetime,
          &mki, &len)) {
    goto end;
  }

  g_free (key_params);

  ret = kms_sdp_sdes_ext_create_key_detailed (tag, key, crypto, lifetime,
      (mki >= 0) ? (guint *) & mki : NULL, (len >= 0) ? (guint *) & len : NULL,
      val, &err);

  if (!ret) {
    GST_ERROR_OBJECT (ext, "%s", err->message);
    g_error_free (err);
  }

end:
  g_strfreev (attrs);
  g_free (lifetime);
  g_free (key);

  return ret;
}

static gboolean
selected_valid_key (const GArray * keys, const GValue * key)
{
  const GValue *offered;
  SrtpCryptoSuite c1, c2;
  guint tag1, tag2;

  if (!kms_sdp_sdes_ext_get_parameters_from_key (key, KMS_SDES_TAG_FIELD,
          G_TYPE_UINT, &tag1, KMS_SDES_CRYPTO, G_TYPE_UINT, &c1, NULL)) {
    return FALSE;
  }

  offered = get_key_by_tag (keys, tag1);

  if (offered == NULL) {
    return FALSE;
  }

  if (!kms_sdp_sdes_ext_get_parameters_from_key (offered, KMS_SDES_TAG_FIELD,
          G_TYPE_UINT, &tag2, KMS_SDES_CRYPTO, G_TYPE_UINT, &c2, NULL)) {
    return FALSE;
  }

  return tag1 == tag2 && c1 == c2;
}

static gboolean
kms_sdp_sdes_ext_add_answer_key (KmsISdpMediaExtension * ext,
    GstSDPMedia * answer, const GValue * key_val, GError ** error)
{
  gchar *key = NULL, *lifetime = NULL;
  gint mki = -1, len = -1;
  SrtpCryptoSuite crypto;
  gboolean ret;
  guint tag;

  if (!kms_sdp_sdes_ext_get_parameters_from_key (key_val, KMS_SDES_TAG_FIELD,
          G_TYPE_UINT, &tag, KMS_SDES_KEY_FIELD, G_TYPE_STRING, &key,
          KMS_SDES_CRYPTO, G_TYPE_UINT, &crypto, KMS_SDES_LIFETIME,
          G_TYPE_STRING, &lifetime, KMS_SDES_MKI, G_TYPE_UINT, &mki,
          KMS_SDES_LENGTH, G_TYPE_UINT, &len, NULL)) {

    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Invalid key provided");
    ret = FALSE;
  } else {
    ret = kms_sdp_sdes_ext_add_crypto_attr (ext, answer, tag, key, crypto,
        lifetime, (mki >= 0) ? (guint *) & mki : NULL,
        (len >= 0) ? (guint *) & len : NULL, error);
  }

  g_free (lifetime);
  g_free (key);

  return ret;
}

static gboolean
kms_sdp_sdes_ext_add_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  GValue key = G_VALUE_INIT;
  gboolean ret = TRUE, supported;
  GArray *keys;
  guint i;

  if ((gst_sdp_media_get_attribute_val (answer, "keymgt")) != NULL) {
    /* rfc4568 [7.5] */
    /* If the answerer supports both "a=crypto" and "a=keymgt", the answer */
    /* MUST include either "a=crypto" or "a=keymgt", but not both.         */
    return TRUE;
  }

  if (answer->key.type != NULL) {
    /* rfc4568: [7.5] */
    /* If the answerer supports both "a=crypto" and "k=", the answer */
    /* MUST include either "a=crypto" or "k=" but not both.          */
    return TRUE;
  }

  keys = g_array_sized_new (FALSE, FALSE, sizeof (GValue), 3);

  /* Sets a function to clear an element of array */
  g_array_set_clear_func (keys, (GDestroyNotify) g_value_unset);

  for (i = 0;; i++) {
    GValue val = G_VALUE_INIT;
    const gchar *attr_val;

    attr_val = gst_sdp_media_get_attribute_val_n (offer, CRYPTO_ATTR, i);

    if (attr_val == NULL) {
      break;
    }

    if (!kms_sdp_sdes_ext_parse_key_attr (ext, attr_val, &val)) {
      if (G_IS_VALUE (&val)) {
        g_value_unset (&val);
      }
      continue;
    }

    g_array_append_val (keys, val);
  }

  if (!ret || keys->len == 0) {
    goto end;
  }

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_ANSWER_KEYS], 0, keys,
      &key, &supported);

  if (!supported) {
    /* If none of the crypto attributes are valid or none of the valid ones */
    /* are supported, the offered media stream MUST be rejected             */
    GST_WARNING_OBJECT (ext,
        "Rejecting offer because no crypto attributes are supported");
    gst_sdp_media_set_port_info (answer, 0, 1);
    goto end;
  }

  if (!selected_valid_key (keys, &key)) {
    /* Same tag and crypto-suite must be selected in the answer */
    GST_WARNING_OBJECT (ext, "Asnwer key does not match with offered key");
    gst_sdp_media_set_port_info (answer, 0, 1);
  } else {
    ret = kms_sdp_sdes_ext_add_answer_key (ext, answer, &key, error);
  }

  g_value_unset (&key);

end:
  g_array_unref (keys);

  return ret;
}

static gboolean
kms_sdp_sdes_ext_can_insert_attribute (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  GST_DEBUG_OBJECT (ext, "an insert %s:%s ?", attr->key, attr->value);

  return FALSE;
}

static gboolean
kms_sdp_sdes_ext_process_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * answer, GError ** error)
{
  GValue key = G_VALUE_INIT;
  const gchar *attr_val;

  attr_val = gst_sdp_media_get_attribute_val (answer, CRYPTO_ATTR);

  if (attr_val != NULL && kms_sdp_sdes_ext_parse_key_attr (ext, attr_val, &key)) {
    g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_SELECTED_KEY], 0,
        &key);
  }

  g_value_unset (&key);

  return TRUE;
}

static void
kms_sdp_sdes_ext_class_init (KmsSdpSdesExtClass * klass)
{
  obj_signals[SIGNAL_ON_OFFER_KEYS] =
      g_signal_new ("on-offer-keys",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpSdesExtClass, on_offer_keys),
      g_signal_accumulator_first_wins, NULL,
      __kms_sdp_agent_marshal_BOXED__VOID, G_TYPE_ARRAY, 0);

  obj_signals[SIGNAL_ON_ANSWER_KEYS] =
      g_signal_new ("on-answer-keys",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpSdesExtClass, on_answer_keys),
      g_signal_accumulator_true_handled, NULL,
      __kms_sdp_agent_marshal_BOOLEAN__BOXED_POINTER, G_TYPE_BOOLEAN, 2,
      G_TYPE_ARRAY, G_TYPE_POINTER);

  obj_signals[SIGNAL_ON_SELECTED_KEY] =
      g_signal_new ("on-selected-key",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpSdesExtClass, on_selected_key),
      NULL, NULL, g_cclosure_marshal_VOID__POINTER, G_TYPE_NONE, 1,
      G_TYPE_POINTER);
}

static void
kms_sdp_sdes_ext_init (KmsSdpSdesExt * self)
{
  /* Nothing to do */
}

static void
kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface * iface)
{
  iface->add_offer_attributes = kms_sdp_sdes_ext_add_offer_attributes;
  iface->add_answer_attributes = kms_sdp_sdes_ext_add_answer_attributes;
  iface->can_insert_attribute = kms_sdp_sdes_ext_can_insert_attribute;
  iface->process_answer_attributes = kms_sdp_sdes_ext_process_answer_attributes;
}

KmsSdpSdesExt *
kms_sdp_sdes_ext_new ()
{
  gpointer obj;

  obj = g_object_new (KMS_TYPE_SDP_SDES_EXT, NULL);

  return KMS_SDP_SDES_EXT (obj);
}

gboolean
kms_sdp_sdes_ext_create_key_detailed (guint tag, const gchar * key,
    SrtpCryptoSuite crypto, const gchar * lifetime, const guint * mki,
    const guint * length, GValue * val, GError ** error)
{
  const gchar *err_msg;
  GstStructure *str;

  if (!is_valid_tag (tag)) {
    err_msg = "tag can not be greater than 999999999";
    goto error;
  }

  if (key == NULL) {
    err_msg = "key attribute can not be NULL";
    goto error;
  }

  if ((mki != NULL && length == NULL) || (mki == NULL && length != NULL)) {
    err_msg = "MKI and length must be either both NULL or neither of them";
    goto error;
  }

  str = gst_structure_new ("sdp-crypto", KMS_SDES_TAG_FIELD, G_TYPE_UINT, tag,
      KMS_SDES_KEY_FIELD, G_TYPE_STRING, key, KMS_SDES_CRYPTO, G_TYPE_UINT,
      crypto, NULL);

  if (lifetime != NULL) {
    gst_structure_set (str, KMS_SDES_LIFETIME, G_TYPE_STRING, lifetime, NULL);
  }

  if (mki != NULL) {
    gst_structure_set (str, KMS_SDES_MKI, G_TYPE_UINT, *mki,
        KMS_SDES_LENGTH, G_TYPE_UINT, *length, NULL);
  }

  g_value_init (val, GST_TYPE_STRUCTURE);
  gst_value_set_structure (val, str);
  gst_structure_free (str);

  return TRUE;

error:
  g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
      SDP_AGENT_INVALID_PARAMETER, err_msg);

  return FALSE;
}

gboolean
kms_sdp_sdes_ext_get_parameters_from_key (const GValue * key,
    const char *first_param, ...)
{
  const GstStructure *str;
  gboolean ret;
  va_list args;

  str = get_structure_from_value (key);

  if (str == NULL) {
    return FALSE;
  }

  va_start (args, first_param);
  ret = get_parameters_from_key_structure (str, first_param, args);
  va_end (args);

  return ret;
}
