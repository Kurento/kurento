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
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <string.h>
#include <nice/interfaces.h>

#include "kmsrtpendpoint.h"
#include "kmsrtpsession.h"
#include "kmssrtpsession.h"
#include <commons/constants.h>
#include <commons/kmsbasertpendpoint.h>
#include <commons/sdp_utils.h>
#include <commons/sdpagent/kmssdprtpsavpfmediahandler.h>
#include <commons/sdpagent/kmssdprtpavpfmediahandler.h>
#include <commons/sdpagent/kmssdpsdesext.h>
#include <commons/kmsrefstruct.h>
#include "kms-rtp-enumtypes.h"
#include "kmsrtpsdescryptosuite.h"
#include "kmsrandom.h"

#include <stdlib.h> // atoi()

#define PLUGIN_NAME "rtpendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_rtp_endpoint_debug);
#define GST_CAT_DEFAULT kms_rtp_endpoint_debug

#define kms_rtp_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsRtpEndpoint, kms_rtp_endpoint, KMS_TYPE_BASE_RTP_ENDPOINT);

#define DEFAULT_USE_SDES FALSE
#define DEFAULT_MASTER_KEY NULL
#define DEFAULT_CRYPTO_SUITE KMS_RTP_SDES_CRYPTO_SUITE_NONE
#define DEFAULT_KEY_TAG 1

#define KMS_SRTP_AUTH_HMAC_SHA1_32 1
#define KMS_SRTP_AUTH_HMAC_SHA1_80 2
#define KMS_SRTP_CIPHER_AES_CM_128 1
#define KMS_SRTP_CIPHER_AES_CM_256 2
#define KMS_SRTP_CIPHER_AES_CM_128_SIZE ((gsize)30)
#define KMS_SRTP_CIPHER_AES_CM_256_SIZE ((gsize)46)

#define KMS_RTP_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_RTP_ENDPOINT,                   \
    KmsRtpEndpointPrivate                    \
  )                                          \
)

typedef struct _SdesExtData
{
  KmsRefStruct ref;
  gchar *media;
  KmsRtpEndpoint *rtpep;
} SdesExtData;

typedef struct _SdesKeys
{
  KmsRefStruct ref;
  GValue local;
  GValue remote;
  KmsRtpBaseConnection *conn;
  KmsISdpMediaExtension *ext;
} SdesKeys;

typedef struct _KmsComedia KmsComedia;
struct _KmsComedia
{
  GHashTable *rtp_conns; // GHashTable<RTPSession*, KmsIRtpConnection*>
  GHashTable *signal_ids; // GHashTable<RTPSession*, int>
};

struct _KmsRtpEndpointPrivate
{
  gboolean use_sdes;
  GHashTable *sdes_keys;

  gchar *master_key;  // SRTP Master Key, base64 encoded
  KmsRtpSDESCryptoSuite crypto;

  /* COMEDIA (passive port discovery) */
  KmsComedia comedia;
};

/* Signals and args */
enum
{
  /* signals */
  SIGNAL_KEY_SOFT_LIMIT,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

enum
{
  PROP_0,
  PROP_USE_SDES,
  PROP_MASTER_KEY,
  PROP_CRYPTO_SUITE
};

static void
sdes_ext_data_destroy (SdesExtData * edata)
{
  g_free (edata->media);

  g_slice_free (SdesExtData, edata);
}

static SdesExtData *
sdes_ext_data_new (KmsRtpEndpoint * ep, const gchar * media)
{
  SdesExtData *edata;

  edata = g_slice_new0 (SdesExtData);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (edata),
      (GDestroyNotify) sdes_ext_data_destroy);

  edata->media = g_strdup (media);
  edata->rtpep = ep;

  return edata;
}

static void
sdes_keys_destroy (SdesKeys * keys)
{
  if (G_IS_VALUE (&keys->local)) {
    g_value_unset (&keys->local);
  }

  if (G_IS_VALUE (&keys->remote)) {
    g_value_unset (&keys->remote);
  }

  g_clear_object (&keys->conn);
  g_clear_object (&keys->ext);

  g_slice_free (SdesKeys, keys);
}

static SdesKeys *
sdes_keys_new (KmsISdpMediaExtension * ext)
{
  SdesKeys *keys;

  keys = g_slice_new0 (SdesKeys);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (keys),
      (GDestroyNotify) sdes_keys_destroy);

  keys->ext = g_object_ref (ext);

  return keys;
}

static gboolean
get_auth_cipher_from_crypto (SrtpCryptoSuite crypto, guint * auth,
    guint * cipher)
{
  switch (crypto) {
    case KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_32:
      *auth = KMS_SRTP_AUTH_HMAC_SHA1_32;
      *cipher = KMS_SRTP_CIPHER_AES_CM_128;
      return TRUE;
    case KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_80:
      *auth = KMS_SRTP_AUTH_HMAC_SHA1_80;
      *cipher = KMS_SRTP_CIPHER_AES_CM_128;
      return TRUE;
    case KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_32:
      *auth = KMS_SRTP_AUTH_HMAC_SHA1_32;
      *cipher = KMS_SRTP_CIPHER_AES_CM_256;
      return TRUE;
    case KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_80:
      *auth = KMS_SRTP_AUTH_HMAC_SHA1_80;
      *cipher = KMS_SRTP_CIPHER_AES_CM_256;
      return TRUE;
    default:
      *auth = *cipher = 0;
      return FALSE;
  }
}

static gboolean
kms_rtp_endpoint_set_local_srtp_connection_key (KmsRtpEndpoint * self,
    const gchar * media, SdesKeys * sdes_keys)
{
  SrtpCryptoSuite crypto;
  guint auth, cipher;
  gchar *key;

  if (!G_IS_VALUE (&sdes_keys->local)) {

    return FALSE;
  }

  if (!kms_sdp_sdes_ext_get_parameters_from_key (&sdes_keys->local,
          KMS_SDES_KEY_FIELD, G_TYPE_STRING, &key, KMS_SDES_CRYPTO, G_TYPE_UINT,
          &crypto, NULL)) {

    return FALSE;
  }

  if (!get_auth_cipher_from_crypto (crypto, &auth, &cipher)) {
    g_free (key);

    return FALSE;
  }

  kms_srtp_connection_set_key (KMS_SRTP_CONNECTION (sdes_keys->conn),
      key, auth, cipher, TRUE);
  g_free (key);

  return TRUE;
}

static void
kms_rtp_endpoint_set_remote_srtp_connection_key (KmsRtpEndpoint * self,
    const gchar * media, SdesKeys * sdes_keys)
{
  SrtpCryptoSuite my_crypto, rem_crypto;
  guint my_tag, rem_tag;
  gchar *rem_key = NULL;
  gboolean done = FALSE;
  guint auth, cipher;

  if (!G_IS_VALUE (&sdes_keys->local) || !G_IS_VALUE (&sdes_keys->remote)) {
    GST_DEBUG_OBJECT (self, "Keys are not yet negotiated");
    return;
  }

  if (!kms_sdp_sdes_ext_get_parameters_from_key (&sdes_keys->local,
          KMS_SDES_TAG_FIELD, G_TYPE_UINT, &my_tag, KMS_SDES_CRYPTO,
          G_TYPE_UINT, &my_crypto, NULL)) {
    goto end;
  }

  if (!kms_sdp_sdes_ext_get_parameters_from_key (&sdes_keys->remote,
          KMS_SDES_TAG_FIELD, G_TYPE_UINT, &rem_tag, KMS_SDES_CRYPTO,
          G_TYPE_UINT, &rem_crypto, KMS_SDES_KEY_FIELD, G_TYPE_STRING, &rem_key,
          NULL)) {
    goto end;
  }

  if (my_tag != rem_tag || my_crypto != rem_crypto) {
    goto end;
  }

  if (!get_auth_cipher_from_crypto (rem_crypto, &auth, &cipher)) {
    goto end;
  }

  kms_srtp_connection_set_key (KMS_SRTP_CONNECTION (sdes_keys->conn), rem_key,
      auth, cipher, FALSE);

  done = TRUE;

end:
  if (!done) {
    GST_ERROR_OBJECT (self, "Can not configure remote connection key");
  }

  g_free (rem_key);
}

static void
conn_soft_limit_cb (KmsSrtpConnection * conn, gpointer user_data)
{
  SdesExtData *data = (SdesExtData *) user_data;
  KmsRtpEndpoint *self = data->rtpep;

  g_signal_emit (self, obj_signals[SIGNAL_KEY_SOFT_LIMIT], 0, data->media);
}

static KmsRtpBaseConnection *
kms_rtp_endpoint_get_connection (KmsRtpEndpoint * self, KmsSdpSession * sess,
    KmsSdpMediaHandler * handler, const GstSDPMedia * media)
{
  if (self->priv->use_sdes) {
    KmsRtpBaseConnection *conn;
    SdesExtData *data;

    conn = kms_srtp_session_get_connection (KMS_SRTP_SESSION (sess), handler);

    data = sdes_ext_data_new (self, gst_sdp_media_get_media (media));

    g_signal_connect_data (conn, "key-soft-limit",
        G_CALLBACK (conn_soft_limit_cb), data,
        (GClosureNotify) kms_ref_struct_unref, 0);
    return conn;
  } else {
    return kms_rtp_session_get_connection (KMS_RTP_SESSION (sess), handler);
  }
}

/* Internal session management begin */

static void
kms_rtp_endpoint_set_addr (KmsRtpEndpoint * self)
{
  GList *ips, *l;
  gboolean done = FALSE;

  ips = nice_interfaces_get_local_ips (FALSE);
  for (l = ips; l != NULL && !done; l = l->next) {
    GInetAddress *addr;
    gboolean is_ipv6 = FALSE;

    GST_DEBUG_OBJECT (self, "Check local address: %s", (const gchar*)l->data);
    addr = g_inet_address_new_from_string (l->data);

    if (G_IS_INET_ADDRESS (addr)) {
      switch (g_inet_address_get_family (addr)) {
        case G_SOCKET_FAMILY_INVALID:
        case G_SOCKET_FAMILY_UNIX:
          /* Ignore this addresses */
          break;
        case G_SOCKET_FAMILY_IPV6:
          is_ipv6 = TRUE;
        case G_SOCKET_FAMILY_IPV4:
        {
          gchar *addr_str;
          gboolean use_ipv6;

          g_object_get (self, "use-ipv6", &use_ipv6, NULL);
          if (is_ipv6 != use_ipv6) {
            GST_DEBUG_OBJECT (self, "Skip address (wanted IPv6: %d)", use_ipv6);
            break;
          }

          addr_str = g_inet_address_to_string (addr);
          if (addr_str != NULL) {
            g_object_set (self, "addr", addr_str, NULL);
            g_free (addr_str);
            done = TRUE;
          }
          break;
        }
      }
    }

    if (G_IS_OBJECT (addr)) {
      g_object_unref (addr);
    }
  }

  g_list_free_full (ips, g_free);

  if (!done) {
    GST_WARNING_OBJECT (self, "Addr not set");
  }
}

static void
kms_rtp_endpoint_create_session_internal (KmsBaseSdpEndpoint * base_sdp,
    gint id, KmsSdpSession ** sess)
{
  KmsIRtpSessionManager *manager = KMS_I_RTP_SESSION_MANAGER (base_sdp);
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp);
  gboolean use_ipv6 = FALSE;

  /* Get ip address now that session is bein created */
  kms_rtp_endpoint_set_addr (self);

  g_object_get (self, "use-ipv6", &use_ipv6, NULL);
  if (self->priv->use_sdes) {
    *sess =
        KMS_SDP_SESSION (kms_srtp_session_new (base_sdp, id, manager,
            use_ipv6));
  } else {
    *sess =
        KMS_SDP_SESSION (kms_rtp_session_new (base_sdp, id, manager, use_ipv6));
  }

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->create_session_internal (base_sdp, id,
      sess);
}

/* Internal session management end */

/* Media handler management begin */

static guint
get_max_key_size (SrtpCryptoSuite crypto)
{
  switch (crypto) {
    case KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_32:
    case KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_80:
      return KMS_SRTP_CIPHER_AES_CM_128_SIZE;
    case KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_32:
    case KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_80:
      return KMS_SRTP_CIPHER_AES_CM_256_SIZE;
    default:
      return 0;
  }
}

static void
enhanced_g_value_copy (const GValue * src, GValue * dest)
{
  if (G_IS_VALUE (dest)) {
    g_value_unset (dest);
  }

  g_value_init (dest, G_VALUE_TYPE (src));
  g_value_copy (src, dest);
}

static gboolean
kms_rtp_endpoint_create_new_key (KmsRtpEndpoint * self, guint tag, GValue * key)
{
  if (self->priv->crypto == KMS_RTP_SDES_CRYPTO_SUITE_NONE) {
    return FALSE;
  }

  if (self->priv->master_key == NULL) {
    guint size;

    GST_INFO_OBJECT (self, "Master key unset, generate random one");

    size = get_max_key_size ((SrtpCryptoSuite) self->priv->crypto);
    self->priv->master_key = generate_random_key (size);
  }

  if (self->priv->master_key == NULL) {
    return FALSE;
  }

  return kms_sdp_sdes_ext_create_key_detailed (tag, self->priv->master_key,
      (SrtpCryptoSuite) self->priv->crypto, NULL, NULL, NULL, key, NULL);
}

static GArray *
kms_rtp_endpoint_on_offer_keys_cb (KmsSdpSdesExt * ext, SdesExtData * edata)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (edata->rtpep);
  GValue key = G_VALUE_INIT;
  SdesKeys *sdes_keys;
  GArray *keys;

  KMS_ELEMENT_LOCK (self);

  sdes_keys = g_hash_table_lookup (self->priv->sdes_keys, edata->media);

  if (sdes_keys == NULL) {
    GST_ERROR_OBJECT (self, "No keys configured for media %s", edata->media);
    KMS_ELEMENT_UNLOCK (self);

    return NULL;
  }

  if (!kms_rtp_endpoint_create_new_key (self, DEFAULT_KEY_TAG, &key)) {
    GST_ERROR_OBJECT (self, "Can not generate master key for media %s",
        edata->media);
    KMS_ELEMENT_UNLOCK (self);

    return NULL;
  }

  enhanced_g_value_copy (&key, &sdes_keys->local);

  KMS_ELEMENT_UNLOCK (self);

  keys = g_array_sized_new (FALSE, FALSE, sizeof (GValue), 1);

  /* Sets a function to clear an element of array */
  g_array_set_clear_func (keys, (GDestroyNotify) g_value_unset);

  g_array_append_val (keys, key);

  return keys;
}

static gboolean
kms_rtp_endpoint_is_supported_key (KmsRtpEndpoint * self, GValue * key)
{
  SrtpCryptoSuite crypto;

  if (!kms_sdp_sdes_ext_get_parameters_from_key (key, KMS_SDES_CRYPTO,
          G_TYPE_UINT, &crypto, NULL)) {
    return FALSE;
  }

  switch (crypto) {
    case KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_32:
    case KMS_SDES_EXT_AES_CM_128_HMAC_SHA1_80:
    case KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_32:
    case KMS_SDES_EXT_AES_256_CM_HMAC_SHA1_80:
      return (SrtpCryptoSuite) self->priv->crypto == crypto;
    default:
      return FALSE;
  }
}

static GValue *
kms_rtp_endpoint_get_supported_key (KmsRtpEndpoint * self, const GArray * keys)
{
  guint i;

  for (i = 0; i < keys->len; i++) {
    GValue *key;

    key = &g_array_index (keys, GValue, 0);

    if (key != NULL && kms_rtp_endpoint_is_supported_key (self, key)) {
      return key;
    }
  }

  return NULL;
}

static gboolean
kms_rtp_endpoint_on_answer_keys_cb (KmsSdpSdesExt * ext, const GArray * keys,
    GValue * key, SdesExtData * edata)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (edata->rtpep);
  SdesKeys *sdes_keys;
  GValue *offer_key;
  gboolean ret = FALSE;
  guint tag;

  if (keys->len == 0) {
    GST_ERROR_OBJECT (self, "No key provided in offer");
    return FALSE;
  }

  KMS_ELEMENT_LOCK (self);

  offer_key = kms_rtp_endpoint_get_supported_key (self, keys);

  if (offer_key == NULL) {
    GST_ERROR_OBJECT (self, "No supported keys provided");
    goto end;
  }

  sdes_keys = g_hash_table_lookup (self->priv->sdes_keys, edata->media);

  if (sdes_keys == NULL) {
    GST_ERROR_OBJECT (self, "No key configured for media %s", edata->media);
    goto end;
  }

  if (!kms_sdp_sdes_ext_get_parameters_from_key (offer_key, KMS_SDES_TAG_FIELD,
          G_TYPE_UINT, &tag, NULL)) {
    GST_ERROR_OBJECT (self, "Invalid key offered");
    goto end;
  }

  if (!kms_rtp_endpoint_create_new_key (self, tag, key)) {
    GST_ERROR_OBJECT (self, "Can not generate master key for media %s",
        edata->media);
    goto end;
  }

  enhanced_g_value_copy (key, &sdes_keys->local);
  enhanced_g_value_copy (offer_key, &sdes_keys->remote);

  ret = TRUE;

end:
  KMS_ELEMENT_UNLOCK (self);

  return ret;
}

static void
kms_rtp_endpoint_on_selected_key_cb (KmsSdpSdesExt * ext, const GValue * key,
    SdesExtData * edata)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (edata->rtpep);
  SdesKeys *sdes_keys;

  KMS_ELEMENT_LOCK (self);

  sdes_keys = g_hash_table_lookup (self->priv->sdes_keys, edata->media);

  if (sdes_keys == NULL) {
    GST_ERROR_OBJECT (self, "Can not configure keys for connection");
    goto end;
  }

  enhanced_g_value_copy (key, &sdes_keys->remote);

  kms_rtp_endpoint_set_remote_srtp_connection_key (self, edata->media,
      sdes_keys);

end:
  KMS_ELEMENT_UNLOCK (self);
}

static KmsSdpMediaHandler *
kms_rtp_endpoint_provide_sdes_handler (KmsRtpEndpoint * self,
    const gchar * media)
{
  KmsSdpMediaHandler *handler;
  SdesKeys *sdes_keys;
  SdesExtData *edata;
  KmsSdpSdesExt *ext;

  handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_rtp_savpf_media_handler_new ());

  /* Let's use sdes extension */
  ext = kms_sdp_sdes_ext_new ();
  if (!kms_sdp_media_handler_add_media_extension (handler,
          KMS_I_SDP_MEDIA_EXTENSION (ext))) {
    GST_ERROR_OBJECT (self, "Can not use SDES in handler %" GST_PTR_FORMAT,
        handler);
    goto end;
  }

  edata = sdes_ext_data_new (self, media);

  g_signal_connect_data (ext, "on-offer-keys",
      G_CALLBACK (kms_rtp_endpoint_on_offer_keys_cb), edata,
      (GClosureNotify) kms_ref_struct_unref, 0);
  g_signal_connect_data (ext, "on-answer-keys",
      G_CALLBACK (kms_rtp_endpoint_on_answer_keys_cb),
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (edata)),
      (GClosureNotify) kms_ref_struct_unref, 0);
  g_signal_connect_data (ext, "on-selected-key",
      G_CALLBACK (kms_rtp_endpoint_on_selected_key_cb),
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (edata)),
      (GClosureNotify) kms_ref_struct_unref, 0);

  sdes_keys = sdes_keys_new (KMS_I_SDP_MEDIA_EXTENSION (ext));

  KMS_ELEMENT_LOCK (self);

  g_hash_table_insert (self->priv->sdes_keys, g_strdup (media), sdes_keys);

  KMS_ELEMENT_UNLOCK (self);

end:
  return handler;
}

static KmsSdpMediaHandler *
kms_rtp_endpoint_get_media_handler (KmsRtpEndpoint * self, const gchar * media)
{
  KmsSdpMediaHandler *handler;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->use_sdes) {
    handler = kms_rtp_endpoint_provide_sdes_handler (self, media);
  } else {
    handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_rtp_avpf_media_handler_new ());
  }

  KMS_ELEMENT_UNLOCK (self);

  return handler;
}

static void
kms_rtp_endpoint_create_media_handler (KmsBaseSdpEndpoint * base_sdp,
    const gchar * media, KmsSdpMediaHandler ** handler)
{
  if (g_strcmp0 (media, "audio") == 0 || g_strcmp0 (media, "video") == 0) {
    *handler = kms_rtp_endpoint_get_media_handler (KMS_RTP_ENDPOINT (base_sdp),
        media);
  }

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->create_media_handler (base_sdp, media,
      handler);
}

/* Media handler management end */

static void
kms_rtp_endpoint_configure_connection_keys (KmsRtpEndpoint * self,
    KmsRtpBaseConnection * conn, const gchar * media)
{
  SdesKeys *sdes_keys;

  KMS_ELEMENT_LOCK (self);

  sdes_keys = g_hash_table_lookup (self->priv->sdes_keys, media);

  if (sdes_keys == NULL) {
    GST_ERROR_OBJECT (self, "No keys configured for %s connection", media);
    goto end;
  } else {
    sdes_keys->conn = g_object_ref (conn);
  }

  if (!kms_rtp_endpoint_set_local_srtp_connection_key (self, media, sdes_keys)) {
    GST_ERROR_OBJECT (self, "Can not configure local connection key");
    goto end;
  }

  kms_rtp_endpoint_set_remote_srtp_connection_key (self, media, sdes_keys);

end:
  KMS_ELEMENT_UNLOCK (self);
}

/* Configure media SDP begin */
static gboolean
kms_rtp_endpoint_configure_media (KmsBaseSdpEndpoint * base_sdp_endpoint,
    KmsSdpSession * sess, KmsSdpMediaHandler * handler, GstSDPMedia * media)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp_endpoint);
  guint conn_len, c;
  guint attr_len, a;
  KmsRtpBaseConnection *conn;
  gboolean ret = TRUE;

  /* Chain up */
  ret = KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_rtp_endpoint_parent_class)->configure_media (base_sdp_endpoint, sess,
      handler, media);
  if (ret == FALSE) {
    media->port = 0;
    GST_WARNING_OBJECT (base_sdp_endpoint,
        "Setting port to 0 because connection could not be created");
    return FALSE;
  }

  conn_len = gst_sdp_media_connections_len (media);
  for (c = 0; c < conn_len; c++) {
    gst_sdp_media_remove_connection (media, c);
  }

  conn = kms_rtp_endpoint_get_connection (KMS_RTP_ENDPOINT (base_sdp_endpoint),
      sess, handler, media);

  if (conn == NULL) {
    return TRUE;
  }

  media->port = kms_rtp_base_connection_get_rtp_port (conn);

  attr_len = gst_sdp_media_attributes_len (media);
  for (a = 0; a < attr_len; a++) {
    GstSDPAttribute *attr =
        (GstSDPAttribute *) gst_sdp_media_get_attribute (media, a);

    if (g_strcmp0 (attr->key, "rtcp") == 0) {
      const guint rtcp_port = kms_rtp_base_connection_get_rtcp_port (conn);
      g_free (attr->value);
      attr->value = g_strdup_printf ("%" G_GUINT32_FORMAT, rtcp_port);
    }
  }

  if (self->priv->use_sdes) {
    kms_rtp_endpoint_configure_connection_keys (self, conn,
        gst_sdp_media_get_media (media));
  }

  return TRUE;
}

/* Configure media SDP end */

static void
kms_rtp_endpoint_comedia_on_ssrc_active(GObject *rtpsession,
    GObject *rtpsource, KmsRtpEndpoint *self)
{
  GstStructure* source_stats = NULL;
  gchar *rtp_from = NULL;
  gchar *rtcp_from = NULL;
  GNetworkAddress *rtp_addr = NULL;
  GNetworkAddress *rtcp_addr = NULL;
  gboolean ok;

  guint ssrc;
  gboolean is_validated;
  gboolean is_sender;

  // Each session has a minimum of 2 source SSRCs: sender and receiver.
  // Here we look for stats from the sender which had COMEDIA enabled
  // (by setting "a=direction:active" in the SDP negotiation).

  // Property RTPSource::ssrc, doc: GStreamer/rtpsource.c
  g_object_get (rtpsource,
      "ssrc", &ssrc, "is-validated", &is_validated, "is-sender", &is_sender, NULL);

  if (!is_validated || !is_sender) {
    GST_DEBUG_OBJECT (rtpsession,
        "Ignore uninteresting RTPSource, SSRC: %u", ssrc);
    return;
  }

  GST_INFO_OBJECT (rtpsession, "COMEDIA: Get port info, SSRC: %u", ssrc);

  g_object_get (rtpsource, "stats", &source_stats, NULL);
  if (!source_stats) {
    GST_ERROR_OBJECT (rtpsession, "COMEDIA: RTPSource lacks stats");
    return;
  }

  ok = gst_structure_get (source_stats,
      "rtp-from", G_TYPE_STRING, &rtp_from, NULL);
  if (!ok) {
    GST_WARNING_OBJECT (rtpsession, "COMEDIA: 'rtp-from' not available yet");
    goto end;
  } else {
    GST_INFO_OBJECT (rtpsession, "COMEDIA: 'rtp-from' found: '%s'", rtp_from);
  }

  ok = gst_structure_get (source_stats,
      "rtcp-from", G_TYPE_STRING, &rtcp_from, NULL);
  if (!ok) {
    GST_WARNING_OBJECT (rtpsession, "COMEDIA: 'rtcp-from' not available yet");
    goto end;
  } else {
    GST_INFO_OBJECT (rtpsession, "COMEDIA: 'rtcp-from' found: '%s'", rtcp_from);
  }

  rtp_addr = G_NETWORK_ADDRESS (g_network_address_parse (rtp_from, 5004, NULL));
  if (!rtp_addr) {
    GST_ERROR_OBJECT (rtpsession, "COMEDIA: Cannot parse 'rtp-from'");
    goto end;
  }

  rtcp_addr = G_NETWORK_ADDRESS (
      g_network_address_parse (rtcp_from, 5005, NULL));
  if (!rtcp_addr) {
    GST_ERROR_OBJECT (rtpsession, "COMEDIA: Cannot parse 'rtcp-from'");
    goto end;
  }

  KmsRtpBaseConnection *conn =
    g_hash_table_lookup (self->priv->comedia.rtp_conns, rtpsession);

  kms_rtp_base_connection_set_remote_info(conn,
    g_network_address_get_hostname (rtcp_addr),
    g_network_address_get_port (rtp_addr),
    g_network_address_get_port (rtcp_addr));

  GST_INFO_OBJECT (rtpsession, "COMEDIA: Parsed route: IP: %s, RTP: %u, RTCP: %u",
    g_network_address_get_hostname (rtcp_addr),
    g_network_address_get_port (rtp_addr),
    g_network_address_get_port (rtcp_addr));

  gulong signal_id =
    GPOINTER_TO_UINT(
      g_hash_table_lookup (self->priv->comedia.signal_ids, rtpsession));

  GST_INFO_OBJECT (rtpsession, "COMEDIA: Disconnect from signal 'on_ssrc_active'");
  g_signal_handler_disconnect (rtpsession, signal_id);

end:
  if (rtp_addr) { g_object_unref (rtp_addr); }
  if (rtcp_addr) { g_object_unref (rtcp_addr); }
  if (rtp_from) { g_free(rtp_from); }
  if (rtcp_from) { g_free(rtcp_from); }
  gst_structure_free (source_stats);
}

static void
kms_rtp_endpoint_comedia_manager_create(KmsRtpEndpoint *self,
    const GstSDPMedia *media, KmsRtpBaseConnection *conn)
{
  const gchar *media_str = gst_sdp_media_get_media (media);
  guint session_id;

  /* TODO: think about this when multiple audio/video medias */
  if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
    session_id = AUDIO_RTP_SESSION;
  } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
    session_id = VIDEO_RTP_SESSION;
  } else {
    GST_WARNING_OBJECT (self, "Media '%s' not supported", media_str);
    return;
  }

  GObject *rtpsession = kms_base_rtp_endpoint_get_internal_session (
      KMS_BASE_RTP_ENDPOINT(self), session_id);
  if (!rtpsession) {
    GST_WARNING_OBJECT (self,
        "Abort: No RTP Session with ID %u", session_id);
    return;
  }

  gulong signal_id = g_signal_connect (rtpsession, "on-ssrc-active",
      G_CALLBACK (kms_rtp_endpoint_comedia_on_ssrc_active), self);

  g_hash_table_insert (self->priv->comedia.rtp_conns, g_object_ref (rtpsession),
      conn);
  g_hash_table_insert (self->priv->comedia.signal_ids,
      g_object_ref (rtpsession), GUINT_TO_POINTER(signal_id));

  g_object_unref (rtpsession);
}

static void
kms_rtp_endpoint_start_transport_send (KmsBaseSdpEndpoint *base_sdp_endpoint,
    KmsSdpSession *sess, gboolean offerer)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (base_sdp_endpoint);
  const GstSDPConnection *msg_conn;

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS (parent_class)->start_transport_send
      (base_sdp_endpoint, sess, offerer);

  msg_conn = gst_sdp_message_get_connection (sess->remote_sdp);

  guint len = gst_sdp_message_medias_len (sess->remote_sdp);
  for (guint i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (sess->remote_sdp, i);
    const GstSDPConnection *media_con;
    KmsSdpMediaHandler *handler;
    KmsRtpBaseConnection *conn;

    if (gst_sdp_media_get_port (media) == 0) {
      // RFC 3264 section 5.1:
      // A port number of zero indicates that the media stream is not wanted.
      // We cannot use `sdp_utils_media_is_inactive()` here because
      // medias marked with "a=inactive" still should send RTCP packets.
      GST_INFO_OBJECT (self, "Media is unwanted (id=%u)", i);
      continue;
    }

    if (gst_sdp_media_connections_len (media) != 0) {
      media_con = gst_sdp_media_get_connection (media, 0);
    } else {
      media_con = msg_conn;
    }

    if (media_con == NULL
        || media_con->address == NULL
        || media_con->address[0] == '\0')
    {
      const gchar *media_str = gst_sdp_media_get_media (media);
      GST_WARNING_OBJECT (self, "Missing connection information for '%s'", media_str);
      continue;
    }

    handler =
        kms_sdp_agent_get_handler_by_index (KMS_SDP_SESSION (sess)->agent, i);
    if (handler == NULL) {
      GST_ERROR_OBJECT (self, "No handler for media at index %u", i);
      continue;
    }

    conn = kms_rtp_endpoint_get_connection (self, sess, handler, media);
    g_object_unref (handler);
    if (conn == NULL) {
      continue;
    }

    // Check if connection-oriented mode ("COMEDIA") is used and we are
    // the passive peer, so the remote IP and port will be discovered
    // when packets start arriving from the other end.
    gboolean comedia_enabled =
        g_strcmp0(gst_sdp_media_get_attribute_val(media, "direction"),
                   "active") == 0;
    if (comedia_enabled) {
      const gchar *media_str = gst_sdp_media_get_media (media);
      GST_INFO_OBJECT (self, "COMEDIA: Media '%s' uses COMEDIA", media_str);
      kms_rtp_endpoint_comedia_manager_create(self, media, conn);
    }
    else {
      const gchar *media_str = gst_sdp_media_get_media (media);
      GST_INFO_OBJECT (self, "COMEDIA: Media '%s' doesn't use COMEDIA", media_str);

      guint rtp_port = gst_sdp_media_get_port (media);
      guint rtcp_port = rtp_port + 1;

      const gchar *attr_rtcp_port = gst_sdp_media_get_attribute_val(media,
          "rtcp");
      if (attr_rtcp_port != NULL) {
        rtcp_port = (guint)atoi (attr_rtcp_port);
      }

      kms_rtp_base_connection_set_remote_info (conn,
          media_con->address, (gint)rtp_port, (gint)rtcp_port);
    }
  }
}

static void
kms_rtp_endpoint_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_MASTER_KEY:{
      const gchar *key_b64 = g_value_get_string (value);
      if (key_b64 == NULL) {
        break;
      }

      gsize key_data_size;
      guchar *tmp_b64 = g_base64_decode (key_b64, &key_data_size);
      if (!tmp_b64) {
        GST_ERROR_OBJECT (self, "Master key is not valid Base64");
        break;
      }
      g_free (tmp_b64);

      if (key_data_size != KMS_SRTP_CIPHER_AES_CM_128_SIZE
          && key_data_size != KMS_SRTP_CIPHER_AES_CM_256_SIZE)
      {
        GST_ERROR_OBJECT (self,
            "Bad Base64-decoded master key size: got %lu, expected %lu or %lu",
            key_data_size, KMS_SRTP_CIPHER_AES_CM_128_SIZE,
            KMS_SRTP_CIPHER_AES_CM_256_SIZE);
        break;
      }

      g_free (self->priv->master_key);
      self->priv->master_key = g_value_dup_string (value);
      break;
    }
    case PROP_CRYPTO_SUITE:
      self->priv->crypto = g_value_get_enum (value);
      self->priv->use_sdes =
          self->priv->crypto != KMS_RTP_SDES_CRYPTO_SUITE_NONE;
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_rtp_endpoint_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_USE_SDES:
      g_value_set_boolean (value, self->priv->use_sdes);
      break;
    case PROP_MASTER_KEY:
      g_value_set_string (value, self->priv->master_key);
      break;
    case PROP_CRYPTO_SUITE:
      g_value_set_enum (value, self->priv->crypto);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_rtp_endpoint_finalize (GObject * object)
{
  KmsRtpEndpoint *self = KMS_RTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_free (self->priv->master_key);
  g_hash_table_unref (self->priv->sdes_keys);

  g_hash_table_unref (self->priv->comedia.rtp_conns);
  g_hash_table_unref (self->priv->comedia.signal_ids);

  /* chain up */
  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_rtp_endpoint_class_init (KmsRtpEndpointClass * klass)
{
  GObjectClass *gobject_class;
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class;
  GstElementClass *gstelement_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_rtp_endpoint_set_property;
  gobject_class->get_property = kms_rtp_endpoint_get_property;
  gobject_class->finalize = kms_rtp_endpoint_finalize;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "RtpEndpoint",
      "RTP/Stream/RtpEndpoint",
      "Rtp Endpoint element",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);
  base_sdp_endpoint_class->create_session_internal =
      kms_rtp_endpoint_create_session_internal;
  base_sdp_endpoint_class->start_transport_send =
      kms_rtp_endpoint_start_transport_send;

  /* Media handler management */
  base_sdp_endpoint_class->create_media_handler =
      kms_rtp_endpoint_create_media_handler;

  base_sdp_endpoint_class->configure_media = kms_rtp_endpoint_configure_media;

  g_object_class_install_property (gobject_class, PROP_USE_SDES,
      g_param_spec_boolean ("use-sdes",
          "Use SDES", "Set if Session Description Protocol Decurity"
          " Description (SDES) is used", DEFAULT_USE_SDES,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_MASTER_KEY,
      g_param_spec_string ("master-key",
          "Master key", "Master key (either 30 or 46 bytes, depending on the"
          " crypto-suite used)",
          DEFAULT_MASTER_KEY,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_CRYPTO_SUITE,
      g_param_spec_enum ("crypto-suite",
          "Crypto suite",
          "Describes the encryption and authentication algorithms",
          KMS_TYPE_RTP_SDES_CRYPTO_SUITE, DEFAULT_CRYPTO_SUITE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  obj_signals[SIGNAL_KEY_SOFT_LIMIT] =
      g_signal_new ("key-soft-limit",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsRtpEndpointClass, key_soft_limit), NULL, NULL,
      g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 1, G_TYPE_STRING);

  g_type_class_add_private (klass, sizeof (KmsRtpEndpointPrivate));
}

/* TODO: not add abs-send-time extmap */

static void
kms_rtp_endpoint_init (KmsRtpEndpoint * self)
{
  self->priv = KMS_RTP_ENDPOINT_GET_PRIVATE (self);

  self->priv->sdes_keys = g_hash_table_new_full (g_str_hash, g_str_equal,
      g_free, (GDestroyNotify) kms_ref_struct_unref);

  self->priv->comedia.rtp_conns = g_hash_table_new_full (NULL, NULL,
      g_object_unref, NULL);
  self->priv->comedia.signal_ids = g_hash_table_new_full (NULL, NULL,
      g_object_unref, NULL);

  g_object_set (G_OBJECT (self), "bundle",
      FALSE, "rtcp-mux", FALSE, "rtcp-nack", TRUE, "rtcp-remb", TRUE,
      "max-video-recv-bandwidth", 0, NULL);
  /* FIXME: remove max-video-recv-bandwidth when it b=AS:X is in the SDP offer */
}

gboolean
kms_rtp_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RTP_ENDPOINT);
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsrtpendpoint,
    "Kurento rtp endpoint",
    kms_rtp_endpoint_plugin_init, VERSION, GST_LICENSE_UNKNOWN,
    "Kurento Elements", "http://kurento.com/")
