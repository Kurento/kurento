/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmswebrtcsession.h"
#include "kmswebrtcrtcpmuxconnection.h"
#include "kmswebrtcbundleconnection.h"
#include "kmswebrtcsctpconnection.h"
#include <commons/sdp_utils.h>

#include "kms-webrtc-marshal.h"

#include <string.h>

#define GST_DEFAULT_NAME "kmswebrtcsession"
#define GST_CAT_DEFAULT kms_webrtc_session_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_session_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcSession, kms_webrtc_session, KMS_TYPE_BASE_RTP_SESSION);

#define BUNDLE_CONN_ADDED "bundle-conn-added"
#define RTCP_DEMUX_PEER "rtcp-demux-peer"

enum
{
  SIGNAL_ON_ICE_CANDIDATE,
  SIGNAL_ON_ICE_GATHERING_DONE,
  LAST_SIGNAL
};

static guint kms_webrtc_session_signals[LAST_SIGNAL] = { 0 };

KmsWebrtcSession *
kms_webrtc_session_new (KmsBaseSdpEndpoint * ep, guint id,
    KmsIRtpSessionManager * manager, GMainContext * context)
{
  GObject *obj;
  KmsWebrtcSession *self;

  obj = g_object_new (KMS_TYPE_WEBRTC_SESSION, NULL);
  self = KMS_WEBRTC_SESSION (obj);
  KMS_WEBRTC_SESSION_CLASS (G_OBJECT_GET_CLASS (self))->post_constructor
      (self, ep, id, manager, context);

  return self;
}

/* Connection management begin */

KmsWebRtcBaseConnection *
kms_webrtc_session_get_connection (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (self);
  KmsIRtpConnection *conn;

  conn = kms_base_rtp_session_get_connection (base_rtp_sess, mconf);
  if (conn == NULL) {
    return NULL;
  }

  return KMS_WEBRTC_BASE_CONNECTION (conn);
}

static KmsIRtpConnection *
kms_webrtc_session_create_connection (KmsBaseRtpSession * base_rtp_sess,
    SdpMediaConfig * mconf, const gchar * name)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (base_rtp_sess);
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  KmsWebRtcBaseConnection *conn;

  if (g_strcmp0 (gst_sdp_media_get_proto (media), "DTLS/SCTP") == 0) {
    GST_DEBUG_OBJECT (self, "Create SCTP connection");
    conn =
        KMS_WEBRTC_BASE_CONNECTION (kms_webrtc_sctp_connection_new
        (self->agent, self->context, name));
  } else {
    GST_DEBUG_OBJECT (self, "Create RTP connection");
    conn =
        KMS_WEBRTC_BASE_CONNECTION (kms_webrtc_connection_new
        (self->agent, self->context, name));
  }

  return KMS_I_RTP_CONNECTION (conn);
}

static KmsIRtcpMuxConnection *
kms_webrtc_session_create_rtcp_mux_connection (KmsBaseRtpSession *
    base_rtp_sess, const gchar * name)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (base_rtp_sess);
  KmsWebRtcRtcpMuxConnection *conn;

  conn = kms_webrtc_rtcp_mux_connection_new (self->agent, self->context, name);

  return KMS_I_RTCP_MUX_CONNECTION (conn);
}

static KmsIBundleConnection *
kms_webrtc_session_create_bundle_connection (KmsBaseRtpSession *
    base_rtp_sess, const gchar * name)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (base_rtp_sess);
  KmsWebRtcBundleConnection *conn;

  conn = kms_webrtc_bundle_connection_new (self->agent, self->context, name);

  return KMS_I_BUNDLE_CONNECTION (conn);
}

/* Connection management end */

static guint
kms_webrtc_session_get_stream_id (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  KmsWebRtcBaseConnection *conn;

  conn = kms_webrtc_session_get_connection (self, mconf);
  if (conn == NULL) {
    return -1;
  }

  return conn->stream_id;
}

static void
sdp_media_add_ice_candidate (GstSDPMedia * media, NiceAgent * agent,
    NiceCandidate * cand)
{
  gchar *str;

  str = nice_agent_generate_local_candidate_sdp (agent, cand);
  gst_sdp_media_add_attribute (media, SDP_CANDIDATE_ATTR,
      str + SDP_CANDIDATE_ATTR_LEN);
  g_free (str);
}

static const gchar *
kms_webrtc_session_sdp_media_add_ice_candidate (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, NiceAgent * agent, NiceCandidate * cand)
{
  guint media_stream_id;
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  const gchar *mid;

  media_stream_id = kms_webrtc_session_get_stream_id (self, mconf);
  if (media_stream_id == -1) {
    return NULL;
  }

  if (media_stream_id != cand->stream_id) {
    return NULL;
  }

  sdp_media_add_ice_candidate (media, agent, cand);

  mid = kms_sdp_media_config_get_mid (mconf);
  if (mid == NULL) {
    return "";
  }

  return mid;
}

void
kms_webrtc_session_remote_sdp_add_ice_candidate (KmsWebrtcSession *
    self, NiceCandidate * nice_cand, guint8 index)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  SdpMessageContext *remote_sdp_ctx = sdp_sess->remote_sdp_ctx;
  GSList *medias;
  SdpMediaConfig *mconf;

  if (remote_sdp_ctx == NULL) {
    GST_INFO_OBJECT (self, "Cannot update remote SDP until it is set.");
    return;
  }

  medias = kms_sdp_message_context_get_medias (remote_sdp_ctx);
  mconf = g_slist_nth_data (medias, index);
  if (mconf == NULL) {
    GST_WARNING_OBJECT (self,
        "Media not found in remote SDP for index %" G_GUINT16_FORMAT, index);
  } else {
    GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);

    sdp_media_add_ice_candidate (media, self->agent, nice_cand);
  }
}

gboolean
kms_webrtc_session_set_remote_ice_candidate (KmsWebrtcSession * self,
    KmsIceCandidate * candidate, NiceCandidate * nice_cand)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  SdpMessageContext *local_sdp_ctx = sdp_sess->local_sdp_ctx;
  guint8 index;
  GSList *medias;
  SdpMediaConfig *mconf;
  gboolean ret;

  if (local_sdp_ctx == NULL) {
    GST_INFO_OBJECT (self,
        "Cannot add candidate until local SDP is generated.");
    return TRUE;                /* We do not know if the candidate is valid until it is set */
  }

  medias = kms_sdp_message_context_get_medias (local_sdp_ctx);
  index = kms_ice_candidate_get_sdp_m_line_index (candidate);
  mconf = g_slist_nth_data (medias, index);
  if (mconf == NULL) {
    GST_WARNING_OBJECT (self,
        "Media not found in local SDP for index %" G_GUINT16_FORMAT, index);
    return FALSE;
  } else if (kms_sdp_media_config_is_inactive (mconf)) {
    GST_DEBUG_OBJECT (self, "Media inactive for index %" G_GUINT16_FORMAT,
        index);
    return TRUE;
  } else {
    GSList *candidates;
    const gchar *cand_str;

    nice_cand->stream_id = kms_webrtc_session_get_stream_id (self, mconf);
    if (nice_cand->stream_id == -1) {
      return FALSE;
    }

    cand_str = kms_ice_candidate_get_candidate (candidate);
    candidates = g_slist_append (NULL, nice_cand);

    if (nice_agent_set_remote_candidates (self->agent,
            nice_cand->stream_id, nice_cand->component_id, candidates) < 0) {
      GST_WARNING_OBJECT (self, "Cannot add candidate: '%s'in stream_id: %d.",
          cand_str, nice_cand->stream_id);
      ret = FALSE;
    } else {
      GST_TRACE_OBJECT (self, "Candidate added: '%s' in stream_id: %d.",
          cand_str, nice_cand->stream_id);
      ret = TRUE;
    }

    g_slist_free (candidates);
  }

  return ret;
}

void
kms_webrtc_session_add_stored_ice_candidates (KmsWebrtcSession * self)
{
  guint i;
  guint len = g_slist_length (self->remote_candidates);

  for (i = 0; i < len; i++) {
    KmsIceCandidate *candidate = g_slist_nth_data (self->remote_candidates, i);
    NiceCandidate *nice_cand;

    kms_ice_candidate_create_nice (candidate, &nice_cand);
    if (nice_cand == NULL) {
      return;
    }

    if (!kms_webrtc_session_set_remote_ice_candidate (self, candidate,
            nice_cand)) {
      nice_candidate_free (nice_cand);
      return;
    }
    nice_candidate_free (nice_cand);
  }
}

static void
kms_webrtc_session_sdp_msg_add_ice_candidate (KmsWebrtcSession * self,
    NiceAgent * agent, NiceCandidate * nice_cand)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  SdpMessageContext *local_sdp_ctx = sdp_sess->local_sdp_ctx;
  const GSList *item = kms_sdp_message_context_get_medias (local_sdp_ctx);
  GList *list = NULL, *iterator = NULL;

  KMS_SDP_SESSION_LOCK (self);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *mconf = item->data;
    gint idx = kms_sdp_media_config_get_id (mconf);
    const gchar *mid;

    if (kms_sdp_media_config_is_inactive (mconf)) {
      GST_DEBUG_OBJECT (self, "Media (id=%d) inactive", idx);
      continue;
    }

    mid =
        kms_webrtc_session_sdp_media_add_ice_candidate (self, mconf,
        agent, nice_cand);
    if (mid != NULL) {
      KmsIceCandidate *candidate =
          kms_ice_candidate_new_from_nice (agent, nice_cand, mid, idx);

      list = g_list_append (list, candidate);
    }
  }

  KMS_SDP_SESSION_UNLOCK (self);

  for (iterator = list; iterator; iterator = iterator->next) {
    g_signal_emit (G_OBJECT (self),
        kms_webrtc_session_signals[SIGNAL_ON_ICE_CANDIDATE], 0, iterator->data);
  }

  g_list_free_full (list, g_object_unref);
}

/* TODO: change using "new-candidate-full" of libnice 0.1.8 */
static void
kms_webrtc_session_new_candidate (NiceAgent * agent,
    guint stream_id,
    guint component_id, gchar * foundation, KmsWebrtcSession * self)
{
  GSList *candidates;
  GSList *walk;

  GST_TRACE_OBJECT (self,
      "stream_id: %d, component_id: %d, foundation: %s", stream_id,
      component_id, foundation);

  candidates = nice_agent_get_local_candidates (agent, stream_id, component_id);

  for (walk = candidates; walk; walk = walk->next) {
    NiceCandidate *cand = walk->data;

    if (cand->stream_id == stream_id &&
        cand->component_id == component_id &&
        g_strcmp0 (foundation, cand->foundation) == 0) {
      kms_webrtc_session_sdp_msg_add_ice_candidate (self, agent, cand);
    }
  }
  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);
}

static gboolean
kms_webrtc_session_sdp_media_add_default_info (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, gboolean use_ipv6)
{
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  NiceAgent *agent = self->agent;
  guint stream_id;
  NiceCandidate *rtp_default_candidate, *rtcp_default_candidate;
  gchar rtp_addr[NICE_ADDRESS_STRING_LEN + 1];
  gchar rtcp_addr[NICE_ADDRESS_STRING_LEN + 1];
  const gchar *rtp_addr_type, *rtcp_addr_type;
  gboolean rtp_is_ipv6, rtcp_is_ipv6;
  guint rtp_port, rtcp_port;
  guint conn_len, c;
  gchar *str;
  guint attr_len, i;

  stream_id = kms_webrtc_session_get_stream_id (self, mconf);
  if (stream_id == -1) {
    return FALSE;
  }

  rtp_default_candidate =
      nice_agent_get_default_local_candidate (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (kms_sdp_media_config_is_rtcp_mux (mconf) ||
      kms_sdp_media_config_get_group (mconf) != NULL) {
    rtcp_default_candidate =
        nice_agent_get_default_local_candidate (agent, stream_id,
        NICE_COMPONENT_TYPE_RTP);
  } else {
    rtcp_default_candidate =
        nice_agent_get_default_local_candidate (agent, stream_id,
        NICE_COMPONENT_TYPE_RTCP);
  }

  if (rtcp_default_candidate == NULL || rtcp_default_candidate == NULL) {
    GST_WARNING_OBJECT (self,
        "Error getting ICE candidates. Network can be unavailable.");
    return FALSE;
  }

  nice_address_to_string (&rtp_default_candidate->addr, rtp_addr);
  rtp_port = nice_address_get_port (&rtp_default_candidate->addr);
  rtp_is_ipv6 = nice_address_ip_version (&rtp_default_candidate->addr) == IPV6;
  nice_candidate_free (rtp_default_candidate);

  nice_address_to_string (&rtcp_default_candidate->addr, rtcp_addr);
  rtcp_port = nice_address_get_port (&rtcp_default_candidate->addr);
  rtcp_is_ipv6 =
      nice_address_ip_version (&rtcp_default_candidate->addr) == IPV6;
  nice_candidate_free (rtcp_default_candidate);

  rtp_addr_type = rtp_is_ipv6 ? "IP6" : "IP4";
  rtcp_addr_type = rtcp_is_ipv6 ? "IP6" : "IP4";

  if (use_ipv6 != rtp_is_ipv6) {
    GST_WARNING_OBJECT (self, "No valid rtp address type: %s", rtp_addr_type);
    return FALSE;
  }

  media->port = rtp_port;
  conn_len = gst_sdp_media_connections_len (media);
  for (c = 0; c < conn_len; c++) {
    gst_sdp_media_remove_connection (media, c);
  }
  gst_sdp_media_add_connection (media, "IN", rtp_addr_type, rtp_addr, 0, 0);

  attr_len = gst_sdp_media_attributes_len (media);
  for (i = 0; i < attr_len; i++) {
    const GstSDPAttribute *attr = gst_sdp_media_get_attribute (media, i);

    if (g_strcmp0 (attr->key, "rtcp") == 0) {
      str =
          g_strdup_printf ("%d IN %s %s", rtcp_port, rtcp_addr_type, rtcp_addr);
      gst_sdp_attribute_clear ((GstSDPAttribute *) attr);
      gst_sdp_attribute_set ((GstSDPAttribute *) attr, "rtcp", str);
      g_free (str);
    }
  }

  return TRUE;
}

static gboolean
kms_webrtc_session_local_sdp_add_default_info (KmsWebrtcSession * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  SdpMessageContext *local_sdp_ctx = sdp_sess->local_sdp_ctx;
  const GstSDPMessage *sdp =
      kms_sdp_message_context_get_sdp_message (local_sdp_ctx);
  const GSList *item = kms_sdp_message_context_get_medias (local_sdp_ctx);
  gboolean use_ipv6;
  GstSDPConnection *conn;

  conn = (GstSDPConnection *) gst_sdp_message_get_connection (sdp);
  gst_sdp_connection_clear (conn);
  /* inmediate-TODO: remove this dependency */
  g_object_get (sdp_sess->ep, "use-ipv6", &use_ipv6, NULL);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *mconf = item->data;

    if (kms_sdp_media_config_is_inactive (mconf)) {
      gint mid = kms_sdp_media_config_get_id (mconf);

      GST_DEBUG_OBJECT (self, "Media (id=%d) inactive", mid);
      continue;
    }

    if (!kms_webrtc_session_sdp_media_add_default_info (self, mconf, use_ipv6)) {
      return FALSE;
    }
  }

  return TRUE;
}

static void
kms_webrtc_session_gathering_done (NiceAgent * agent, guint stream_id,
    KmsWebrtcSession * self)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (self);
  GHashTableIter iter;
  gpointer key, v;
  gboolean done = TRUE;

  GST_DEBUG_OBJECT (self, "ICE gathering done for '%s' stream.",
      nice_agent_get_stream_name (agent, stream_id));

  KMS_SDP_SESSION_LOCK (self);

  g_hash_table_iter_init (&iter, base_rtp_sess->conns);
  while (g_hash_table_iter_next (&iter, &key, &v)) {
    KmsWebRtcBaseConnection *conn = KMS_WEBRTC_BASE_CONNECTION (v);

    if (stream_id == conn->stream_id) {
      conn->ice_gathering_done = TRUE;
    }

    if (!conn->ice_gathering_done) {
      done = FALSE;
    }
  }

  if (done) {
    kms_webrtc_session_local_sdp_add_default_info (self);
  }
  KMS_SDP_SESSION_UNLOCK (self);

  if (done) {
    g_signal_emit (G_OBJECT (self),
        kms_webrtc_session_signals[SIGNAL_ON_ICE_GATHERING_DONE], 0);
  }
}

gboolean
kms_webrtc_session_set_ice_credentials (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  KmsWebRtcBaseConnection *conn;
  gchar *ufrag, *pwd;

  conn = kms_webrtc_session_get_connection (self, mconf);
  if (conn == NULL) {
    return FALSE;
  }

  nice_agent_get_local_credentials (conn->agent, conn->stream_id, &ufrag, &pwd);
  gst_sdp_media_add_attribute (media, SDP_ICE_UFRAG_ATTR, ufrag);
  g_free (ufrag);
  gst_sdp_media_add_attribute (media, SDP_ICE_PWD_ATTR, pwd);
  g_free (pwd);

  return TRUE;
}

static gchar *
generate_fingerprint_from_pem (const gchar * pem)
{
  guint i;
  gchar *line;
  guchar *der, *tmp;
  gchar **lines;
  gint state = 0;
  guint save = 0;
  gsize der_length = 0;
  GChecksum *checksum;
  guint8 *digest;
  gsize digest_length;
  GString *fingerprint;
  gchar *ret;

  der = tmp = g_new0 (guchar, (strlen (pem) / 4) * 3 + 3);
  lines = g_strsplit (pem, "\n", 0);
  for (i = 0, line = lines[i]; line; line = lines[++i]) {
    if (line[0] && !g_str_has_prefix (line, "-----"))
      tmp += g_base64_decode_step (line, strlen (line), tmp, &state, &save);
  }
  der_length = tmp - der;
  checksum = g_checksum_new (G_CHECKSUM_SHA256);
  digest_length = g_checksum_type_get_length (G_CHECKSUM_SHA256);
  digest = g_new (guint8, digest_length);
  g_checksum_update (checksum, der, der_length);
  g_checksum_get_digest (checksum, digest, &digest_length);
  fingerprint = g_string_new (NULL);
  for (i = 0; i < digest_length; i++) {
    if (i)
      g_string_append (fingerprint, ":");
    g_string_append_printf (fingerprint, "%02X", digest[i]);
  }
  ret = g_string_free (fingerprint, FALSE);

  g_free (digest);
  g_checksum_free (checksum);
  g_free (der);
  g_strfreev (lines);

  return ret;
}

static gchar *
kms_webrtc_session_generate_fingerprint_sdp_attr (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  gchar *fp, *ret;

  KmsWebRtcBaseConnection *conn =
      kms_webrtc_session_get_connection (self, mconf);
  gchar *pem = kms_webrtc_base_connection_get_certificate_pem (conn);

  fp = generate_fingerprint_from_pem (pem);
  g_free (pem);

  if (fp == NULL) {
    return NULL;
  }

  /* TODO: store fingerprint to reuse it for each media */
  ret = g_strconcat ("sha-256 ", fp, NULL);
  g_free (fp);

  return ret;
}

gboolean
kms_webrtc_session_set_crypto_info (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  gchar *fingerprint;

  /* Crypto info */
  fingerprint = kms_webrtc_session_generate_fingerprint_sdp_attr (self, mconf);
  if (fingerprint == NULL) {
    return FALSE;
  }

  gst_sdp_media_add_attribute (media, "fingerprint", fingerprint);
  g_free (fingerprint);

  return TRUE;
}

/* Start Transport begin */

static void
gst_media_add_remote_candidates (SdpMediaConfig * mconf,
    KmsWebRtcBaseConnection * conn,
    const gchar * msg_ufrag, const gchar * msg_pwd)
{
  const GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  NiceAgent *agent = conn->agent;
  guint stream_id = conn->stream_id;
  const gchar *ufrag, *pwd;
  guint len, i;

  ufrag = gst_sdp_media_get_attribute_val (media, SDP_ICE_UFRAG_ATTR);
  pwd = gst_sdp_media_get_attribute_val (media, SDP_ICE_PWD_ATTR);
  if (!nice_agent_set_remote_credentials (agent, stream_id, ufrag, pwd)) {
    GST_WARNING ("Cannot set remote media credentials (%s, %s).", ufrag, pwd);
    if (!nice_agent_set_remote_credentials (agent, stream_id, msg_ufrag,
            msg_pwd)) {
      GST_WARNING ("Cannot set remote message credentials (%s, %s).", ufrag,
          pwd);
      return;
    } else {
      GST_DEBUG ("Set remote message credentials OK (%s, %s).", ufrag, pwd);
    }
  } else {
    GST_DEBUG ("Set remote media credentials OK (%s, %s).", ufrag, pwd);
  }

  len = gst_sdp_media_attributes_len (media);
  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;
    NiceCandidate *cand;

    attr = gst_sdp_media_get_attribute (media, i);
    if (g_strcmp0 (SDP_CANDIDATE_ATTR, attr->key) != 0) {
      continue;
    }

    kms_ice_candidate_create_nice_from_str (attr->value, &cand);
    if (cand != NULL) {
      GSList *candidates = g_slist_append (NULL, cand);

      if (nice_agent_set_remote_candidates (agent, stream_id,
              cand->component_id, candidates) < 0) {
        GST_WARNING ("Cannot add candidate: '%s'in stream_id: %d.", attr->value,
            stream_id);
      } else {
        GST_TRACE ("Candidate added: '%s' in stream_id: %d.", attr->value,
            stream_id);
      }
      g_slist_free (candidates);
      nice_candidate_free (cand);
    }
  }
}

static void
kms_webrtc_session_remote_sdp_add_stored_ice_candidates (gpointer data,
    gpointer user_data)
{
  KmsIceCandidate *candidate = data;
  KmsWebrtcSession *webrtc_sess = user_data;
  NiceCandidate *nice_cand;
  guint8 index;

  kms_ice_candidate_create_nice (candidate, &nice_cand);
  if (nice_cand == NULL) {
    return;
  }

  index = kms_ice_candidate_get_sdp_m_line_index (candidate);
  kms_webrtc_session_remote_sdp_add_ice_candidate (webrtc_sess, nice_cand,
      index);
  nice_candidate_free (nice_cand);
}

static gboolean
kms_webrtc_session_add_connection (KmsWebrtcSession * self,
    KmsSdpSession * sess, SdpMediaConfig * mconf, gboolean offerer)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (sess);
  gboolean connected, active;
  KmsIRtpConnection *conn;

  conn = kms_base_rtp_session_get_connection (base_rtp_sess, mconf);
  if (conn == NULL) {
    GST_ERROR_OBJECT (self, "No connection created");
    return FALSE;
  }

  g_object_get (conn, "added", &connected, NULL);
  if (connected) {
    GST_DEBUG_OBJECT (self, "Conn already added");
    return TRUE;
  }

  active =
      sdp_utils_media_is_active (kms_sdp_media_config_get_sdp_media (mconf),
      offerer);

  kms_i_rtp_connection_add (conn, GST_BIN (self), active);
  kms_i_rtp_connection_sink_sync_state_with_parent (conn);
  kms_i_rtp_connection_src_sync_state_with_parent (conn);

  return TRUE;
}

static gboolean
kms_webrtc_session_configure_connection (KmsWebrtcSession * self,
    KmsSdpSession * sess, SdpMediaConfig * neg_mconf,
    SdpMediaConfig * remote_mconf, gboolean offerer)
{
  GstSDPMedia *neg_media = kms_sdp_media_config_get_sdp_media (neg_mconf);
  const gchar *neg_proto_str = gst_sdp_media_get_proto (neg_media);
  GstSDPMedia *remote_media = kms_sdp_media_config_get_sdp_media (remote_mconf);
  const gchar *remote_proto_str = gst_sdp_media_get_proto (remote_media);

  if (g_strcmp0 (neg_proto_str, remote_proto_str) != 0) {
    GST_WARNING_OBJECT (self,
        "Negotiated proto ('%s') not matching with remote proto ('%s')",
        neg_proto_str, remote_proto_str);
    return FALSE;
  }

  if (g_strcmp0 (neg_proto_str, "DTLS/SCTP") != 0) {
    return FALSE;
  }

  kms_webrtc_session_add_connection (self, sess, neg_mconf, offerer);

  return TRUE;
}

static void
kms_webrtc_session_configure_connections (KmsWebrtcSession * self,
    KmsSdpSession * sess, gboolean offerer)
{
  GSList *item = kms_sdp_message_context_get_medias (sess->neg_sdp_ctx);
  GSList *remote_media_list =
      kms_sdp_message_context_get_medias (sess->remote_sdp_ctx);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *neg_mconf = item->data;
    gint mid = kms_sdp_media_config_get_id (neg_mconf);
    SdpMediaConfig *remote_mconf;

    if (kms_sdp_media_config_is_inactive (neg_mconf)) {
      GST_DEBUG_OBJECT (self, "Media (id=%d) inactive", mid);
      continue;
    }

    remote_mconf = g_slist_nth_data (remote_media_list, mid);
    if (remote_mconf == NULL) {
      GST_WARNING_OBJECT (self, "Media (id=%d) is not in the remote SDP", mid);
      continue;
    }

    kms_webrtc_session_configure_connection (self, sess, neg_mconf,
        remote_mconf, offerer);
  }
}

void
kms_webrtc_session_start_transport_send (KmsWebrtcSession * self,
    gboolean offerer)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  const GstSDPMessage *sdp =
      kms_sdp_message_context_get_sdp_message (sdp_sess->remote_sdp_ctx);
  const GSList *item =
      kms_sdp_message_context_get_medias (sdp_sess->neg_sdp_ctx);
  GSList *remote_media_list =
      kms_sdp_message_context_get_medias (sdp_sess->remote_sdp_ctx);
  const gchar *ufrag, *pwd;

  /*  [rfc5245#section-5.2]
   *  The agent that generated the offer which
   *  started the ICE processing MUST take the controlling role, and the
   *  other MUST take the controlled role.
   */
  g_object_set (self->agent, "controlling-mode", offerer, NULL);

  /* Configure specific webrtc connection such as SCTP if negotiated */
  kms_webrtc_session_configure_connections (self, sdp_sess, offerer);

  ufrag = gst_sdp_message_get_attribute_val (sdp, SDP_ICE_UFRAG_ATTR);
  pwd = gst_sdp_message_get_attribute_val (sdp, SDP_ICE_PWD_ATTR);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *neg_mconf = item->data;
    gint mid = kms_sdp_media_config_get_id (neg_mconf);
    SdpMediaConfig *remote_mconf;
    KmsWebRtcBaseConnection *conn;

    if (kms_sdp_media_config_is_inactive (neg_mconf)) {
      GST_DEBUG_OBJECT (self, "Media (id=%d) inactive", mid);
      continue;
    }

    conn = kms_webrtc_session_get_connection (self, neg_mconf);
    if (conn == NULL) {
      continue;
    }

    remote_mconf = g_slist_nth_data (remote_media_list, mid);
    if (remote_mconf == NULL) {
      GST_WARNING_OBJECT (self, "Media (id=%d) is not in the remote SDP", mid);
      continue;
    }
    gst_media_add_remote_candidates (remote_mconf, conn, ufrag, pwd);
  }

  kms_webrtc_session_add_stored_ice_candidates (self);

  g_slist_foreach (self->remote_candidates,
      kms_webrtc_session_remote_sdp_add_stored_ice_candidates, self);
}

/* Start Transport end */

static void
kms_webrtc_session_finalize (GObject * object)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_clear_object (&self->agent);
  g_main_context_unref (self->context);
  g_slist_free_full (self->remote_candidates, g_object_unref);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_session_parent_class)->finalize (object);
}

static void
kms_webrtc_session_post_constructor (KmsWebrtcSession * self,
    KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager,
    GMainContext * context)
{
  KmsBaseRtpSession *base_rtp_session = KMS_BASE_RTP_SESSION (self);

  self->context = g_main_context_ref (context);
  self->agent = nice_agent_new (context, NICE_COMPATIBILITY_RFC5245);

  g_object_set (self->agent, "upnp", FALSE, NULL);
  g_signal_connect (self->agent, "new-candidate",
      G_CALLBACK (kms_webrtc_session_new_candidate), self);
  g_signal_connect (self->agent, "candidate-gathering-done",
      G_CALLBACK (kms_webrtc_session_gathering_done), self);

  KMS_BASE_RTP_SESSION_CLASS
      (kms_webrtc_session_parent_class)->post_constructor (base_rtp_session, ep,
      id, manager);
}

static void
kms_webrtc_session_init (KmsWebrtcSession * self)
{
  /* nothing to do */
}

static void
kms_webrtc_session_class_init (KmsWebrtcSessionClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  KmsBaseRtpSessionClass *base_rtp_session_class;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gobject_class->finalize = kms_webrtc_session_finalize;

  klass->post_constructor = kms_webrtc_session_post_constructor;

  base_rtp_session_class = KMS_BASE_RTP_SESSION_CLASS (klass);
  /* Connection management */
  base_rtp_session_class->create_connection =
      kms_webrtc_session_create_connection;
  base_rtp_session_class->create_rtcp_mux_connection =
      kms_webrtc_session_create_rtcp_mux_connection;
  base_rtp_session_class->create_bundle_connection =
      kms_webrtc_session_create_bundle_connection;

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcSession",
      "Generic",
      "Base bin to manage elements related with a WebRTC session.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");

  /**
  * KmsWebrtcSession::on-ice-candidate:
  * @self: the object which received the signal
  * @candidate: the local candidate gathered
  *
  * Notify of a new gathered local candidate for a #KmsWebrtcSession.
  */
  kms_webrtc_session_signals[SIGNAL_ON_ICE_CANDIDATE] =
      g_signal_new ("on-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, on_ice_candidate), NULL,
      NULL, g_cclosure_marshal_VOID__OBJECT, G_TYPE_NONE, 1,
      KMS_TYPE_ICE_CANDIDATE);

  /**
  * KmsWebrtcSession::on-candidate-gathering-done:
  * @self: the object which received the signal
  *
  * Notify that all candidates have been gathered for a #KmsWebrtcSession
  */
  kms_webrtc_session_signals[SIGNAL_ON_ICE_GATHERING_DONE] =
      g_signal_new ("on-ice-gathering-done",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, on_ice_gathering_done), NULL,
      NULL, g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 0);
}
