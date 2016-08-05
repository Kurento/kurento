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
#  include <config.h>
#endif

#include "kmswebrtcsession.h"
#include "kmswebrtcrtcpmuxconnection.h"
#include "kmswebrtcbundleconnection.h"
#include "kmswebrtcsctpconnection.h"
#include "kmswebrtcdatasessionbin.h"
#include <commons/constants.h>
#include <commons/kmsutils.h>
#include <commons/sdp_utils.h>
#include <commons/kmsrefstruct.h>

#include "kms-webrtc-marshal.h"
#include "kms-webrtc-data-marshal.h"

#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include <string.h>

#include "kmsiceniceagent.h"
#include <stdlib.h>

#define GST_DEFAULT_NAME "kmswebrtcsession"
#define GST_CAT_DEFAULT kms_webrtc_session_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_session_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcSession, kms_webrtc_session, KMS_TYPE_BASE_RTP_SESSION);

#define BUNDLE_CONN_ADDED "bundle-conn-added"
#define RTCP_DEMUX_PEER "rtcp-demux-peer"

#define DEFAULT_STUN_SERVER_IP NULL
#define DEFAULT_STUN_SERVER_PORT 3478
#define DEFAULT_STUN_TURN_URL NULL
#define DEFAULT_DATA_CHANNELS_SUPPORTED FALSE
#define DEFAULT_PEM_CERTIFICATE NULL

#define IP_VERSION_6 6

#define MAX_DATA_CHANNELS 1

enum
{
  SIGNAL_ON_ICE_CANDIDATE,
  SIGNAL_ON_ICE_GATHERING_DONE,
  SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED,
  SIGNAL_GATHER_CANDIDATES,
  SIGNAL_ADD_ICE_CANDIDATE,
  SIGNAL_INIT_ICE_AGENT,
  SIGNAL_DATA_SESSION_ESTABLISHED,
  SIGNAL_DATA_CHANNEL_OPENED,
  SIGNAL_DATA_CHANNEL_CLOSED,
  ACTION_CREATE_DATA_CHANNEL,
  ACTION_DESTROY_DATA_CHANNEL,
  SIGNAL_NEW_SELECTED_PAIR_FULL,
  LAST_SIGNAL
};

static guint kms_webrtc_session_signals[LAST_SIGNAL] = { 0 };

enum
{
  PROP_0,
  PROP_STUN_SERVER_IP,
  PROP_STUN_SERVER_PORT,
  PROP_TURN_URL,                /* user:password@address:port?transport=[udp|tcp|tls] */
  PROP_DATA_CHANNEL_SUPPORTED,
  PROP_PEM_CERTIFICATE,
  N_PROPERTIES
};

/* ConnectSCTPData begin */

typedef struct _ConnectSCTPData
{
  KmsRefStruct ref;
  KmsWebrtcSession *self;
  KmsIRtpConnection *conn;
  GstSDPMedia *media;
  gboolean connected;
} ConnectSCTPData;

typedef struct _DataChannel
{
  KmsRefStruct ref;
  KmsWebRtcDataChannel *chann;
  GstElement *appsink;
  GstElement *appsrc;
} DataChannel;

static void
data_channel_destroy (DataChannel * chann)
{
  g_slice_free (DataChannel, chann);
}

static DataChannel *
data_channel_new (guint stream_id, KmsWebRtcDataChannel * channel)
{
  DataChannel *chann;
  gchar *name;

  chann = g_slice_new (DataChannel);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (chann),
      (GDestroyNotify) data_channel_destroy);

  name = g_strdup_printf ("appsrc_stream_%u", stream_id);
  chann->appsrc = gst_element_factory_make ("appsrc", name);
  g_free (name);

  name = g_strdup_printf ("appsink_stream_%u", stream_id);
  chann->appsink = gst_element_factory_make ("appsink", name);
  g_free (name);

  g_object_set (chann->appsink, "async", FALSE, "sync", FALSE,
      "emit-signals", FALSE, "drop", FALSE, "enable-last-sample", FALSE, NULL);

  g_object_set (chann->appsrc, "is-live", TRUE, "min-latency",
      G_GINT64_CONSTANT (0), "do-timestamp", TRUE, "max-bytes", 0,
      "emit-signals", FALSE, NULL);

  chann->chann = channel;

  return chann;
}

static void
connect_sctp_data_destroy (ConnectSCTPData * data, GClosure * closure)
{
  gst_sdp_media_free (data->media);

  g_slice_free (ConnectSCTPData, data);
}

static ConnectSCTPData *
connect_sctp_data_new (KmsWebrtcSession * self, GstSDPMedia * media,
    KmsIRtpConnection * conn)
{
  ConnectSCTPData *data;

  data = g_slice_new0 (ConnectSCTPData);

  kms_ref_struct_init (KMS_REF_STRUCT_CAST (data),
      (GDestroyNotify) connect_sctp_data_destroy);

  data->self = self;
  data->conn = conn;
  data->media = media;

  return data;
}

/* ConnectSCTPData end */

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
    SdpMediaConfig * mconf, const gchar * name, guint16 min_port,
    guint16 max_port)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (base_rtp_sess);
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  KmsWebRtcBaseConnection *conn;

  self->min_port = min_port;
  self->max_port = max_port;

  if (g_strcmp0 (gst_sdp_media_get_proto (media), "DTLS/SCTP") == 0) {
    GST_DEBUG_OBJECT (self, "Create SCTP connection");
    conn =
        KMS_WEBRTC_BASE_CONNECTION (kms_webrtc_sctp_connection_new
        (self->agent, self->context, name, min_port, max_port,
            self->pem_certificate));
  } else {
    GST_DEBUG_OBJECT (self, "Create RTP connection");
    conn =
        KMS_WEBRTC_BASE_CONNECTION (kms_webrtc_connection_new
        (self->agent, self->context, name, min_port, max_port,
            self->pem_certificate));
  }

  return KMS_I_RTP_CONNECTION (conn);
}

static KmsIRtcpMuxConnection *
kms_webrtc_session_create_rtcp_mux_connection (KmsBaseRtpSession *
    base_rtp_sess, const gchar * name, guint16 min_port, guint16 max_port)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (base_rtp_sess);
  KmsWebRtcRtcpMuxConnection *conn;

  conn =
      kms_webrtc_rtcp_mux_connection_new (self->agent, self->context, name,
      min_port, max_port, self->pem_certificate);

  return KMS_I_RTCP_MUX_CONNECTION (conn);
}

static KmsIBundleConnection *
kms_webrtc_session_create_bundle_connection (KmsBaseRtpSession *
    base_rtp_sess, const gchar * name, guint16 min_port, guint16 max_port)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (base_rtp_sess);
  KmsWebRtcBundleConnection *conn;

  conn =
      kms_webrtc_bundle_connection_new (self->agent, self->context, name,
      min_port, max_port, self->pem_certificate);

  return KMS_I_BUNDLE_CONNECTION (conn);
}

/* Connection management end */

gchar *
kms_webrtc_session_get_stream_id (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  KmsWebRtcBaseConnection *conn;

  conn = kms_webrtc_session_get_connection (self, mconf);
  if (conn == NULL) {
    return NULL;
  }

  return conn->stream_id;
}

static void
sdp_media_add_ice_candidate (GstSDPMedia * media, KmsIceBaseAgent * agent,
    KmsIceCandidate * cand)
{
  gchar *str;

  str = kms_ice_candidate_get_sdp_line (cand);
  if (str != NULL) {
    gst_sdp_media_add_attribute (media, SDP_CANDIDATE_ATTR,
        str + SDP_CANDIDATE_ATTR_LEN);
    g_free (str);
  }
}

void
kms_webrtc_session_remote_sdp_add_ice_candidate (KmsWebrtcSession *
    self, KmsIceCandidate * candidate, guint8 index)
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
        "Media not found in remote SDP for index %u", index);
  } else {
    GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);

    sdp_media_add_ice_candidate (media, self->agent, candidate);
  }
}

gboolean
kms_webrtc_session_set_remote_ice_candidate (KmsWebrtcSession * self,
    KmsIceCandidate * candidate)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (self);
  SdpMessageContext *local_sdp_ctx = sdp_sess->local_sdp_ctx;
  guint8 index;
  GSList *medias;
  SdpMediaConfig *mconf;
  gboolean ret;

  if (local_sdp_ctx == NULL || !self->gather_started) {
    GST_INFO_OBJECT (self,
        "Cannot add candidate until local SDP is generated and gathering candidates starts.");
    return TRUE;                /* We do not know if the candidate is valid until it is set */
  }

  medias = kms_sdp_message_context_get_medias (local_sdp_ctx);
  index = kms_ice_candidate_get_sdp_m_line_index (candidate);
  mconf = g_slist_nth_data (medias, index);
  if (mconf == NULL) {
    GST_WARNING_OBJECT (self,
        "Media not found in local SDP for index %u", index);
    return FALSE;
  } else if (kms_sdp_media_config_is_inactive (mconf)) {
    GST_DEBUG_OBJECT (self, "Media inactive for index %u", index);
    return TRUE;
  } else {
    gchar *stream_id;

    stream_id = kms_webrtc_session_get_stream_id (self, mconf);

    if (stream_id == NULL) {
      return FALSE;
    }

    if (!kms_ice_base_agent_add_ice_candidate (self->agent, candidate,
            stream_id)) {
      GST_WARNING_OBJECT (self, "Cannot add candidate: '%s'in stream_id: %s.",
          kms_ice_candidate_get_candidate (candidate), stream_id);
      ret = FALSE;
    } else {
      GST_TRACE_OBJECT (self, "Candidate added: '%s' in stream_id: %s.",
          kms_ice_candidate_get_candidate (candidate), stream_id);
      ret = TRUE;
    }
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

    if (!kms_webrtc_session_set_remote_ice_candidate (self, candidate)) {
      return;
    }
  }
}

static const gchar *
kms_webrtc_session_sdp_media_add_ice_candidate (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, KmsIceCandidate * cand)
{
  char *media_stream_id;
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  const gchar *mid;

  media_stream_id = kms_webrtc_session_get_stream_id (self, mconf);
  if (media_stream_id == NULL) {
    return NULL;
  }

  if (g_strcmp0 (media_stream_id, kms_ice_candidate_get_stream_id (cand)) != 0) {
    return NULL;
  }

  sdp_media_add_ice_candidate (media, self->agent, cand);

  mid = kms_sdp_media_config_get_mid (mconf);
  if (mid == NULL) {
    return "";
  }

  return mid;
}

static void
kms_webrtc_session_sdp_msg_add_ice_candidate (KmsWebrtcSession * self,
    KmsIceCandidate * cand)
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

    mid = kms_webrtc_session_sdp_media_add_ice_candidate (self, mconf, cand);
    if (mid != NULL) {
      KmsIceCandidate *candidate =
          kms_ice_candidate_new (kms_ice_candidate_get_candidate (cand), mid,
          idx, NULL);

      list = g_list_append (list, candidate);
    }
  }

  KMS_SDP_SESSION_UNLOCK (self);

  for (iterator = list; iterator; iterator = iterator->next) {
    g_signal_emit (G_OBJECT (self),
        kms_webrtc_session_signals[SIGNAL_ON_ICE_CANDIDATE], 0,
        KMS_ICE_CANDIDATE (iterator->data));
  }

  g_list_free_full (list, g_object_unref);
}

static void
kms_webrtc_session_new_candidate (KmsIceBaseAgent * agent,
    KmsIceCandidate * cand, KmsWebrtcSession * self)
{
  kms_webrtc_session_sdp_msg_add_ice_candidate (self, cand);
}

static gboolean
kms_webrtc_session_sdp_media_add_default_info (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, gboolean use_ipv6)
{
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  KmsIceBaseAgent *agent = self->agent;
  char *stream_id;
  KmsIceCandidate *rtp_default_candidate, *rtcp_default_candidate;
  gchar *rtp_addr;
  gchar *rtcp_addr;
  const gchar *rtp_addr_type, *rtcp_addr_type;
  gboolean rtp_is_ipv6, rtcp_is_ipv6;
  guint rtp_port, rtcp_port;
  guint conn_len, c;
  gchar *str;
  guint attr_len, i;

  stream_id = kms_webrtc_session_get_stream_id (self, mconf);
  if (stream_id == NULL) {
    return FALSE;
  }

  rtp_default_candidate =
      kms_ice_base_agent_get_default_local_candidate (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (kms_sdp_media_config_is_rtcp_mux (mconf) ||
      kms_sdp_media_config_get_group (mconf) != NULL) {
    rtcp_default_candidate =
        kms_ice_base_agent_get_default_local_candidate (agent, stream_id,
        NICE_COMPONENT_TYPE_RTP);
  } else {
    rtcp_default_candidate =
        kms_ice_base_agent_get_default_local_candidate (agent, stream_id,
        NICE_COMPONENT_TYPE_RTCP);
  }

  if (rtp_default_candidate == NULL || rtcp_default_candidate == NULL) {
    GST_WARNING_OBJECT (self,
        "Error getting ICE candidates. Network can be unavailable.");
    return FALSE;
  }

  rtp_addr = kms_ice_candidate_get_address (rtp_default_candidate);
  rtp_port = kms_ice_candidate_get_port (rtp_default_candidate);
  rtp_is_ipv6 =
      kms_ice_candidate_get_ip_version (rtp_default_candidate) == IP_VERSION_6;

  rtcp_addr = kms_ice_candidate_get_address (rtcp_default_candidate);
  rtcp_port = kms_ice_candidate_get_port (rtcp_default_candidate);
  rtcp_is_ipv6 =
      kms_ice_candidate_get_ip_version (rtcp_default_candidate) == IP_VERSION_6;

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

  g_free (rtp_addr);
  g_free (rtcp_addr);

  g_object_unref (rtp_default_candidate);
  g_object_unref (rtcp_default_candidate);

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
  use_ipv6 = kms_sdp_session_get_use_ipv6 (sdp_sess);

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
kms_webrtc_session_gathering_done (KmsIceBaseAgent * agent, gchar * stream_id,
    KmsWebrtcSession * self)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (self);
  GHashTableIter iter;
  gpointer key, v;
  gboolean done = TRUE;

  GST_DEBUG_OBJECT (self, "ICE gathering done for '%s' stream.", stream_id);

  KMS_SDP_SESSION_LOCK (self);

  g_hash_table_iter_init (&iter, base_rtp_sess->conns);

  while (g_hash_table_iter_next (&iter, &key, &v)) {
    KmsWebRtcBaseConnection *conn = KMS_WEBRTC_BASE_CONNECTION (v);

    if (g_strcmp0 (stream_id, conn->stream_id) == 0) {
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

static void
kms_webrtc_session_component_state_change (KmsIceBaseAgent * agent,
    char *stream_id, guint component_id, IceState state,
    KmsWebrtcSession * self)
{
  GST_DEBUG_OBJECT (self,
      "stream_id: %s, component_id: %d, state: %s",
      stream_id, component_id, kms_ice_base_agent_state_to_string (state));

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_session_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED], 0,
      stream_id, component_id, state);
}

static void
kms_webrtc_session_set_stun_server_info (KmsWebrtcSession * self,
    KmsWebRtcBaseConnection * conn)
{
  if (self->stun_server_ip == NULL) {
    return;
  }

  kms_webrtc_base_connection_set_stun_server_info (conn, self->stun_server_ip,
      self->stun_server_port);
}

static void
kms_webrtc_session_set_relay_info (KmsWebrtcSession * self,
    KmsWebRtcBaseConnection * conn)
{
  if (self->turn_address == NULL) {
    return;
  }

  kms_webrtc_base_connection_set_relay_info (conn, self->turn_address,
      self->turn_port, self->turn_user, self->turn_password,
      self->turn_transport);
}

static gboolean
kms_webrtc_session_gather_candidates (KmsWebrtcSession * self)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (self);
  GHashTableIter iter;
  gpointer key, v;
  gboolean ret = TRUE;

  GST_DEBUG_OBJECT (self, "Gather candidates");

  KMS_SDP_SESSION_LOCK (self);
  g_hash_table_iter_init (&iter, base_rtp_sess->conns);
  while (g_hash_table_iter_next (&iter, &key, &v)) {
    KmsWebRtcBaseConnection *conn = KMS_WEBRTC_BASE_CONNECTION (v);

    kms_webrtc_session_set_stun_server_info (self, conn);
    kms_webrtc_session_set_relay_info (self, conn);
    if (!kms_ice_base_agent_start_gathering_candidates (conn->agent,
            conn->stream_id)) {
      GST_ERROR_OBJECT (self, "Failed to start candidate gathering for '%s'.",
          conn->name);
      ret = FALSE;
    }
  }

  if (ret) {
    self->gather_started = TRUE;
    kms_webrtc_session_add_stored_ice_candidates (self);
  }

  KMS_SDP_SESSION_UNLOCK (self);

  return ret;
}

static gboolean
kms_webrtc_session_add_ice_candidate (KmsWebrtcSession * self,
    KmsIceCandidate * candidate)
{
  guint8 index;
  gboolean ret;

  GST_DEBUG_OBJECT (self, "Add ICE candidate '%s'",
      kms_ice_candidate_get_candidate (candidate));

  KMS_SDP_SESSION_LOCK (self);
  self->remote_candidates =
      g_slist_append (self->remote_candidates, g_object_ref (candidate));

  ret = kms_webrtc_session_set_remote_ice_candidate (self, candidate);

  index = kms_ice_candidate_get_sdp_m_line_index (candidate);
  kms_webrtc_session_remote_sdp_add_ice_candidate (self, candidate, index);
  KMS_SDP_SESSION_UNLOCK (self);

  return ret;
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

  kms_ice_base_agent_get_local_credentials (self->agent, conn->stream_id,
      &ufrag, &pwd);
  gst_sdp_media_add_attribute (media, SDP_ICE_UFRAG_ATTR, ufrag);
  g_free (ufrag);
  gst_sdp_media_add_attribute (media, SDP_ICE_PWD_ATTR, pwd);
  g_free (pwd);

  return TRUE;
}

gboolean
kms_webrtc_session_set_ice_candidates (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  char *stream_id;
  GSList *candidates;
  GSList *walk;

  stream_id = kms_webrtc_session_get_stream_id (self, mconf);
  if (stream_id == NULL) {
    return FALSE;
  }

  candidates =
      kms_ice_base_agent_get_local_candidates (self->agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);
  for (walk = candidates; walk; walk = walk->next) {
    sdp_media_add_ice_candidate (media, self->agent, walk->data);
  }
  g_slist_free_full (candidates, g_object_unref);

  candidates =
      kms_ice_base_agent_get_local_candidates (self->agent, stream_id,
      NICE_COMPONENT_TYPE_RTCP);
  for (walk = candidates; walk; walk = walk->next) {
    sdp_media_add_ice_candidate (media, self->agent, walk->data);
  }
  g_slist_free_full (candidates, g_object_unref);

  return TRUE;
}

static gchar *
kms_webrtc_session_generate_fingerprint_sdp_attr (KmsWebrtcSession * self,
    SdpMediaConfig * mconf)
{
  gchar *fp, *ret;

  KmsWebRtcBaseConnection *conn =
      kms_webrtc_session_get_connection (self, mconf);
  gchar *pem = kms_webrtc_base_connection_get_certificate_pem (conn);

  fp = kms_utils_generate_fingerprint_from_pem (pem);
  g_free (pem);

  if (fp == NULL) {
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        (("Fingerprint not generated.")), (NULL));
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
gst_media_add_remote_candidates (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, KmsWebRtcBaseConnection * conn,
    const gchar * msg_ufrag, const gchar * msg_pwd)
{
  const GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
  KmsIceBaseAgent *agent = conn->agent;
  gchar *stream_id = conn->stream_id;
  const gchar *ufrag, *pwd;
  guint len, i;

  ufrag = gst_sdp_media_get_attribute_val (media, SDP_ICE_UFRAG_ATTR);
  pwd = gst_sdp_media_get_attribute_val (media, SDP_ICE_PWD_ATTR);

  if (!kms_ice_base_agent_set_remote_credentials (agent, stream_id, ufrag, pwd)) {
    GST_WARNING ("Cannot set remote media credentials (%s, %s).", ufrag, pwd);
    if (!kms_ice_base_agent_set_remote_credentials (agent, stream_id, msg_ufrag,
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
    gchar *candidate_str;
    KmsIceCandidate *candidate;
    gint idx = kms_sdp_media_config_get_id (mconf);
    const gchar *mid = kms_sdp_media_config_get_mid (mconf);

    attr = gst_sdp_media_get_attribute (media, i);
    if (g_strcmp0 (SDP_CANDIDATE_ATTR, attr->key) != 0) {
      continue;
    }

    candidate_str = g_strdup_printf ("%s:%s", SDP_CANDIDATE_ATTR, attr->value);
    candidate = kms_ice_candidate_new (candidate_str, mid, idx, NULL);
    g_free (candidate_str);
    kms_webrtc_session_add_ice_candidate (self, candidate);
    g_object_unref (candidate);
  }
}

static void
kms_webrtc_session_remote_sdp_add_stored_ice_candidates (gpointer data,
    gpointer user_data)
{
  KmsIceCandidate *candidate = data;
  KmsWebrtcSession *webrtc_sess = user_data;
  guint8 index;

  index = kms_ice_candidate_get_sdp_m_line_index (candidate);
  kms_webrtc_session_remote_sdp_add_ice_candidate (webrtc_sess, candidate,
      index);
}

static gboolean
get_port_from_string (const gchar * str, gint * _ret)
{
  gchar *endptr;
  gint64 val;

  errno = 0;                    /* To distinguish success/failure after call */
  val = g_ascii_strtoll (str, &endptr, 0);
  if ((errno == ERANGE && (val == G_MAXINT64 || val == G_MININT64))
      || (errno == EINVAL && val == 0)) {
    return FALSE;
  }

  if (str == endptr) {
    /* Nothing parsed from the string */
    return FALSE;
  }

  if (val > G_MAXINT32 || val < G_MININT32) {
    /* Value ut of int 32 range */
    return FALSE;
  }

  if (_ret != NULL) {
    *_ret = val;
  }

  return TRUE;
}

static void
kms_webrtc_session_data_session_established_cb (KmsWebRtcDataSessionBin *
    session, gboolean connected, KmsWebrtcSession * self)
{
  GST_DEBUG_OBJECT (self, "Data session %" GST_PTR_FORMAT " %s",
      session, (connected) ? "established" : "finished");

  g_signal_emit (self,
      kms_webrtc_session_signals[SIGNAL_DATA_SESSION_ESTABLISHED], 0,
      connected);
}

static GstFlowReturn
new_sample_callback (GstAppSink * appsink, DataChannel * channel)
{
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;

  sample = gst_app_sink_pull_sample (GST_APP_SINK (appsink));
  g_return_val_if_fail (sample, GST_FLOW_ERROR);

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    gst_sample_unref (sample);
    GST_ERROR_OBJECT (appsink, "No buffer got from sample");

    return GST_FLOW_ERROR;
  }

  /* By default all data received in a pipeline is binary unless they are */
  /* sent by other data channel, in such cases, sctpencoders and decoders */
  /* will set the appropriate ppid meta to the buffer */

  ret = kms_webrtc_data_channel_push_buffer (channel->chann, buffer, FALSE);
  gst_sample_unref (sample);

  return ret;
}

static GstFlowReturn
data_channel_buffer_received_cb (GObject * obj, GstBuffer * buffer,
    DataChannel * channel)
{
  /* buffer is tranfser full */
  return gst_app_src_push_buffer (GST_APP_SRC (channel->appsrc),
      gst_buffer_ref (buffer));
}

static void
kms_webrtc_session_data_channel_opened_cb (KmsWebRtcDataSessionBin * session,
    guint stream_id, KmsWebrtcSession * self)
{
  GstAppSinkCallbacks callbacks;
  KmsWebRtcDataChannel *chann;
  DataChannel *channel;
  GstPad *pad;

  GST_DEBUG_OBJECT (self, "Data channel with stream_id %u opened", stream_id);

  KMS_SDP_SESSION_LOCK (self);

  if (g_hash_table_size (self->data_channels) >= MAX_DATA_CHANNELS) {
    GST_WARNING_OBJECT (self, "No more than %u data channel are allowed",
        MAX_DATA_CHANNELS);
    KMS_SDP_SESSION_UNLOCK (self);
    g_signal_emit_by_name (session, "destroy-data-channel", stream_id, NULL);

    return;
  }

  g_signal_emit_by_name (session, "get-data-channel", stream_id, &chann);

  if (chann == NULL) {
    GST_ERROR_OBJECT (self, "Can not get data channel with id %u", stream_id);
    KMS_SDP_SESSION_UNLOCK (self);

    return;
  }

  channel = data_channel_new (stream_id, chann);

  g_hash_table_insert (self->data_channels, GUINT_TO_POINTER (stream_id),
      channel);

  callbacks.eos = NULL;
  callbacks.new_preroll = NULL;
  callbacks.new_sample =
      (GstFlowReturn (*)(GstAppSink *, gpointer)) new_sample_callback;

  gst_app_sink_set_callbacks (GST_APP_SINK (channel->appsink), &callbacks,
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (channel)),
      (GDestroyNotify) kms_ref_struct_unref);

  kms_webrtc_data_channel_set_new_buffer_callback (channel->chann,
      (DataChannelNewBuffer) data_channel_buffer_received_cb,
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (channel)),
      (GDestroyNotify) kms_ref_struct_unref);

  gst_bin_add_many (GST_BIN (self), channel->appsrc, channel->appsink, NULL);

  gst_element_sync_state_with_parent (channel->appsink);
  gst_element_sync_state_with_parent (channel->appsrc);

  pad = gst_element_get_static_pad (channel->appsrc, "src");

  if (self->add_pad_cb != NULL) {
    self->add_pad_cb (self, pad, KMS_ELEMENT_PAD_TYPE_DATA, NULL,
        self->cb_data);
  }

  g_object_unref (pad);

  pad = gst_element_get_static_pad (channel->appsink, "sink");

  if (self->add_pad_cb != NULL) {
    self->add_pad_cb (self, pad, KMS_ELEMENT_PAD_TYPE_DATA, NULL,
        self->cb_data);
  }

  g_object_unref (pad);

  KMS_SDP_SESSION_UNLOCK (self);

  g_signal_emit (self, kms_webrtc_session_signals[SIGNAL_DATA_CHANNEL_OPENED],
      0, stream_id);
}

static void
kms_webrtc_session_remove_data_channel (KmsWebrtcSession * self,
    DataChannel * channel, guint stream_id)
{
  g_object_ref (channel->appsrc);
  g_object_ref (channel->appsink);

  gst_element_set_state (channel->appsrc, GST_STATE_NULL);
  gst_element_set_state (channel->appsink, GST_STATE_NULL);

  gst_bin_remove_many (GST_BIN (self), channel->appsrc, channel->appsink, NULL);

  g_object_unref (channel->appsrc);
  g_object_unref (channel->appsink);
}

static void
kms_webrtc_session_data_channel_closed_cb (KmsWebRtcDataSessionBin * session,
    guint stream_id, KmsWebrtcSession * self)
{
  DataChannel *channel;
  GstPad *pad;

  GST_DEBUG_OBJECT (self, "Data channel with stream_id %u closed", stream_id);

  KMS_SDP_SESSION_LOCK (self);

  channel = (DataChannel *) g_hash_table_lookup (self->data_channels,
      GUINT_TO_POINTER (stream_id));

  if (channel == NULL) {
    KMS_SDP_SESSION_UNLOCK (self);
    return;
  }

  g_hash_table_steal (self->data_channels, GUINT_TO_POINTER (stream_id));

  pad = gst_element_get_static_pad (channel->appsink, "sink");

  if (self->remove_pad_cb != NULL) {
    self->remove_pad_cb (self, pad, KMS_ELEMENT_PAD_TYPE_DATA, NULL,
        self->cb_data);
  }

  g_object_unref (pad);

  KMS_SDP_SESSION_UNLOCK (self);

  kms_webrtc_session_remove_data_channel (self, channel, stream_id);

  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (channel));

  g_signal_emit (self, kms_webrtc_session_signals[SIGNAL_DATA_CHANNEL_CLOSED],
      0, stream_id);
}

static gboolean
configure_data_session (KmsWebrtcSession * self, GstSDPMedia * media)
{
  const gchar *sctpmap_attr = NULL;
  gint port = -1;
  guint i, len;

  len = gst_sdp_media_formats_len (media);
  if (len <= 0) {
    GST_WARNING_OBJECT (self, "No SCTP format");
    return FALSE;
  }

  if (len > 1) {
    GST_WARNING_OBJECT (self,
        "Only one data session is supported over the same DTLS connection");
  }

  for (i = 0; i < len; i++) {
    const gchar *port_str;
    gchar **attrs;

    port_str = gst_sdp_media_get_format (media, 0);
    sctpmap_attr = sdp_utils_get_attr_map_value (media, "sctpmap", port_str);

    attrs = g_strsplit (sctpmap_attr, " ", 0);
    if (g_strcmp0 (attrs[1], "webrtc-datachannel") != 0) {
      g_strfreev (attrs);
      continue;
    }

    if (get_port_from_string (port_str, &port)) {
      g_strfreev (attrs);
      break;
    }

    g_strfreev (attrs);
  }

  if (port < 0) {
    GST_ERROR_OBJECT (self, "Data session can not be configured");
    return FALSE;
  }

  g_object_set (self->data_session, "sctp-local-port", port,
      "sctp-remote-port", port, NULL);

  return TRUE;
}

static void
kms_webrtc_session_link_pads (GstPad * src, GstPad * sink)
{
  if (gst_pad_link_full (src, sink, GST_PAD_LINK_CHECK_CAPS) != GST_PAD_LINK_OK) {
    GST_ERROR ("Error linking pads (src: %" GST_PTR_FORMAT ", sink: %"
        GST_PTR_FORMAT ")", src, sink);
  }
}

static void
kms_webrtc_session_connect_data_session (KmsWebrtcSession * self,
    GstSDPMedia * media, KmsIRtpConnection * conn)
{
  GstPad *srcpad = NULL, *sinkpad = NULL, *tmppad;

  sinkpad = kms_i_rtp_connection_request_data_sink (conn);

  if (sinkpad == NULL) {
    GST_ERROR_OBJECT (self, "Can not get data sink pad");
    return;
  }

  srcpad = kms_i_rtp_connection_request_data_src (conn);
  if (srcpad == NULL) {
    GST_ERROR_OBJECT (self, "Can not get data src pad");
    goto error;
  }

  if (!configure_data_session (self, media)) {
    goto error;
  }

  tmppad = gst_element_get_static_pad (self->data_session, "sink");
  kms_webrtc_session_link_pads (srcpad, tmppad);
  g_object_unref (tmppad);

  tmppad = gst_element_get_static_pad (self->data_session, "src");
  kms_webrtc_session_link_pads (tmppad, sinkpad);
  g_object_unref (tmppad);

  gst_element_sync_state_with_parent_target_state (self->data_session);

error:

  g_clear_object (&sinkpad);
  g_clear_object (&srcpad);
}

static void
kms_webrtc_session_add_data_session (KmsWebrtcSession * self,
    GstSDPMedia * media, KmsIRtpConnection * conn)
{
  KMS_SDP_SESSION_LOCK (self);

  g_signal_connect (self->data_session, "data-session-established",
      G_CALLBACK (kms_webrtc_session_data_session_established_cb), self);
  g_signal_connect (self->data_session, "data-channel-opened",
      G_CALLBACK (kms_webrtc_session_data_channel_opened_cb), self);
  g_signal_connect (self->data_session, "data-channel-closed",
      G_CALLBACK (kms_webrtc_session_data_channel_closed_cb), self);

  g_object_ref (self->data_session);
  gst_bin_add (GST_BIN (self), self->data_session);
  kms_webrtc_session_connect_data_session (self, media, conn);

  KMS_SDP_SESSION_UNLOCK (self);
}

static void
kms_webrtc_session_add_data_session_cb (KmsIRtpConnection * conn,
    ConnectSCTPData * data)
{
  if (g_atomic_int_compare_and_exchange (&data->connected, FALSE, TRUE)) {
    kms_webrtc_session_add_data_session (data->self, data->media, data->conn);
  } else {
    GST_WARNING_OBJECT (data->self, "SCTP elements already configured");
  }

  /* DTLS is already connected, so we do not need to be attached to this */
  /* signal any more. We can free the tmp data without waiting for the   */
  /* object to be realeased. (Early release) */
  g_signal_handlers_disconnect_by_data (data->conn, data);
}

static void
kms_webrtc_session_support_sctp_stream (KmsWebrtcSession * self,
    SdpMediaConfig * mconf, KmsIRtpConnection * conn)
{
  gboolean connected = FALSE;
  ConnectSCTPData *data;
  GstSDPMedia *media;
  gulong handler_id = 0;

  if (self->data_session == NULL) {
    gboolean is_client;

    g_object_get (conn, "is-client", &is_client, NULL);
    self->data_session =
        GST_ELEMENT (kms_webrtc_data_session_bin_new (is_client));
  }

  gst_sdp_media_copy (kms_sdp_media_config_get_sdp_media (mconf), &media);
  data = connect_sctp_data_new (self, media, conn);

  handler_id = g_signal_connect_data (conn, "connected",
      G_CALLBACK (kms_webrtc_session_add_data_session_cb),
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (data)),
      (GClosureNotify) kms_ref_struct_unref, 0);

  g_object_get (conn, "connected", &connected, NULL);
  if (connected && g_atomic_int_compare_and_exchange (&data->connected, FALSE,
          TRUE)) {
    if (handler_id) {
      g_signal_handler_disconnect (conn, handler_id);
    }
    kms_webrtc_session_add_data_session (self,
        kms_sdp_media_config_get_sdp_media (mconf), conn);
  } else {
    GST_DEBUG_OBJECT (self, "SCTP: waiting for DTLS layer to be established");
  }

  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (data));
}

static gboolean
kms_webrtc_session_add_connection (KmsWebrtcSession * self,
    KmsSdpSession * sess, SdpMediaConfig * mconf, gboolean offerer)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (sess);
  gboolean connected;
  KmsIRtpConnection *conn;

  conn = kms_base_rtp_session_get_connection (base_rtp_sess, mconf);
  if (conn == NULL) {
    GST_ERROR_OBJECT (self, "No connection created");
    return FALSE;
  }

  g_object_get (conn, "added", &connected, NULL);
  if (connected) {
    GST_DEBUG_OBJECT (self, "Conn already added");
  } else {
    gboolean active;

    active =
        sdp_utils_media_is_active (kms_sdp_media_config_get_sdp_media (mconf),
        offerer);

    kms_i_rtp_connection_add (conn, GST_BIN (self), active);
    kms_i_rtp_connection_sink_sync_state_with_parent (conn);
    kms_i_rtp_connection_src_sync_state_with_parent (conn);
  }

  kms_webrtc_session_support_sctp_stream (self, mconf, conn);

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
  // TODO: This code should be independent of the ice implementation
  if (KMS_IS_ICE_NICE_AGENT (self->agent)) {
    KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self->agent);

    g_object_set (kms_ice_nice_agent_get_agent (nice_agent), "controlling-mode",
        offerer, NULL);
  }

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

    /* Configure specific webrtc connection such as SCTP if negotiated */
    kms_webrtc_session_configure_connection (self, sdp_sess, neg_mconf,
        remote_mconf, offerer);

    gst_media_add_remote_candidates (self, remote_mconf, conn, ufrag, pwd);
  }

  kms_webrtc_session_add_stored_ice_candidates (self);

  g_slist_foreach (self->remote_candidates,
      kms_webrtc_session_remote_sdp_add_stored_ice_candidates, self);
}

/* Start Transport end */

void
kms_webrtc_session_add_data_channels_stats (KmsWebrtcSession * self,
    GstStructure * stats, const gchar * selector)
{
  GstStructure *data_stats;
  const gchar *id;

  if (self->data_session == NULL || (selector != NULL &&
          g_strcmp0 (selector, DATA_STREAM_NAME) != 0)) {
    return;
  }

  id = kms_utils_get_uuid (G_OBJECT (self->data_session));

  if (id == NULL) {
    kms_utils_set_uuid (G_OBJECT (self->data_session));
    id = kms_utils_get_uuid (G_OBJECT (self->data_session));
  }

  g_signal_emit_by_name (self->data_session, "stats", &data_stats);
  gst_structure_set (data_stats, "id", G_TYPE_STRING, id, NULL);
  gst_structure_set (stats, KMS_DATA_SESSION_STATISTICS_FIELD,
      GST_TYPE_STRUCTURE, data_stats, NULL);
  gst_structure_free (data_stats);
}

static void
kms_webrtc_session_parse_turn_url (KmsWebrtcSession * self)
{
  GRegex *regex;
  GMatchInfo *match_info = NULL;

  g_free (self->turn_user);
  self->turn_user = NULL;
  g_free (self->turn_password);
  self->turn_password = NULL;
  g_free (self->turn_address);
  self->turn_address = NULL;

  if ((self->turn_url == NULL)
      || (g_strcmp0 ("", self->turn_url) == 0)) {
    GST_INFO_OBJECT (self, "TURN server info cleared");
    return;
  }

  regex =
      g_regex_new
      ("^(?<user>.+):(?<password>.+)@(?<address>[0-9.]+):(?<port>[0-9]+)(\\?transport=(?<transport>(udp|tcp|tls)))?$",
      0, 0, NULL);
  g_regex_match (regex, self->turn_url, 0, &match_info);
  g_regex_unref (regex);

  if (g_match_info_matches (match_info)) {
    gchar *port_str;
    gchar *turn_transport;

    self->turn_user = g_match_info_fetch_named (match_info, "user");
    self->turn_password = g_match_info_fetch_named (match_info, "password");
    self->turn_address = g_match_info_fetch_named (match_info, "address");

    port_str = g_match_info_fetch_named (match_info, "port");
    self->turn_port = g_ascii_strtoll (port_str, NULL, 10);
    g_free (port_str);

    self->turn_transport = TURN_PROTOCOL_UDP;   /* default */
    turn_transport = g_match_info_fetch_named (match_info, "transport");
    if (turn_transport != NULL) {
      if (g_strcmp0 ("tcp", turn_transport) == 0) {
        self->turn_transport = TURN_PROTOCOL_TCP;
      } else if (g_strcmp0 ("tls", turn_transport) == 0) {
        self->turn_transport = TURN_PROTOCOL_TLS;
      }
      g_free (turn_transport);
    }

    GST_INFO_OBJECT (self, "TURN server info set (%s)", self->turn_url);
  } else {
    GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS,
        ("URL '%s' not allowed. It must have this format: 'user:password@address:port(?transport=[udp|tcp|tls])'",
            self->turn_url),
        ("URL '%s' not allowed. It must have this format: 'user:password@address:port(?transport=[udp|tcp|tls])'",
            self->turn_url));
  }

  g_match_info_free (match_info);
}

static void
kms_webrtc_session_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (object);

  KMS_SDP_SESSION_LOCK (self);

  switch (prop_id) {
    case PROP_STUN_SERVER_IP:
      g_free (self->stun_server_ip);
      self->stun_server_ip = g_value_dup_string (value);
      break;
    case PROP_STUN_SERVER_PORT:
      self->stun_server_port = g_value_get_uint (value);
      break;
    case PROP_TURN_URL:
      g_free (self->turn_url);
      self->turn_url = g_value_dup_string (value);
      kms_webrtc_session_parse_turn_url (self);
      break;
    case PROP_PEM_CERTIFICATE:
      g_free (self->pem_certificate);
      self->pem_certificate = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_SDP_SESSION_UNLOCK (self);
}

static void
kms_webrtc_session_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (object);

  KMS_SDP_SESSION_LOCK (self);

  switch (prop_id) {
    case PROP_STUN_SERVER_IP:
      g_value_set_string (value, self->stun_server_ip);
      break;
    case PROP_STUN_SERVER_PORT:
      g_value_set_uint (value, self->stun_server_port);
      break;
    case PROP_TURN_URL:
      g_value_set_string (value, self->turn_url);
      break;
    case PROP_DATA_CHANNEL_SUPPORTED:
      g_value_set_boolean (value, self->data_session != NULL);
      break;
    case PROP_PEM_CERTIFICATE:
      g_value_set_string (value, self->pem_certificate);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_SDP_SESSION_UNLOCK (self);
}

static void
kms_webrtc_session_finalize (GObject * object)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_clear_object (&self->agent);
  g_main_context_unref (self->context);
  g_slist_free_full (self->remote_candidates, g_object_unref);

  g_free (self->stun_server_ip);
  g_free (self->turn_url);
  g_free (self->turn_user);
  g_free (self->turn_password);
  g_free (self->turn_address);
  g_free (self->pem_certificate);

  if (self->destroy_data != NULL && self->cb_data != NULL) {
    self->destroy_data (self->cb_data);
  }

  g_clear_object (&self->data_session);
  g_hash_table_unref (self->data_channels);

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

  KMS_BASE_RTP_SESSION_CLASS
      (kms_webrtc_session_parent_class)->post_constructor (base_rtp_session, ep,
      id, manager);
}

static void
kms_webrtc_session_new_selected_pair_full (KmsIceBaseAgent * agent,
    gchar * stream_id,
    guint component_id,
    KmsIceCandidate * lcandidate,
    KmsIceCandidate * rcandidate, KmsWebrtcSession * self)
{
  GST_DEBUG_OBJECT (self,
      "New pair selected stream_id: %s, component_id: %d, local candidate: %s,"
      " remote candidate: %s", stream_id, component_id,
      kms_ice_candidate_get_candidate (lcandidate),
      kms_ice_candidate_get_candidate (rcandidate));

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_session_signals[SIGNAL_NEW_SELECTED_PAIR_FULL], 0, stream_id,
      component_id, lcandidate, rcandidate);
}

static void
kms_webrtc_session_init_ice_agent (KmsWebrtcSession * self)
{
  self->agent = KMS_ICE_BASE_AGENT (kms_ice_nice_agent_new (self->context));

  kms_ice_base_agent_run_agent (self->agent);

  g_signal_connect (self->agent, "on-ice-candidate",
      G_CALLBACK (kms_webrtc_session_new_candidate), self);
  g_signal_connect (self->agent, "on-ice-gathering-done",
      G_CALLBACK (kms_webrtc_session_gathering_done), self);
  g_signal_connect (self->agent, "on-ice-component-state-changed",
      G_CALLBACK (kms_webrtc_session_component_state_change), self);
  g_signal_connect (self->agent, "new-selected-pair-full",
      G_CALLBACK (kms_webrtc_session_new_selected_pair_full), self);
}

static gint
kms_webrtc_session_create_data_channel (KmsWebrtcSession * self,
    gboolean ordered, gint max_packet_life_time, gint max_retransmits,
    const gchar * label, const gchar * protocol)
{
  gint stream_id = -1;

  KMS_SDP_SESSION_LOCK (self);

  if (self->data_session == NULL) {
    GST_WARNING_OBJECT (self, "Data session is not yet established");
  } else {
    g_signal_emit_by_name (self->data_session, "create-data-channel",
        ordered, max_packet_life_time, max_retransmits, label, protocol,
        &stream_id);
  }

  KMS_SDP_SESSION_UNLOCK (self);

  return stream_id;
}

static void
kms_webrtc_session_destroy_data_channel (KmsWebrtcSession * self,
    gint stream_id)
{
  KMS_SDP_SESSION_LOCK (self);

  if (self->data_session == NULL) {
    GST_WARNING_OBJECT (self, "Data session is not yet established");
  } else {
    g_signal_emit_by_name (self->data_session, "destroy-data-channel",
        stream_id);
  }

  KMS_SDP_SESSION_UNLOCK (self);
}

static void
kms_webrtc_session_init (KmsWebrtcSession * self)
{
  self->stun_server_ip = DEFAULT_STUN_SERVER_IP;
  self->stun_server_port = DEFAULT_STUN_SERVER_PORT;
  self->turn_url = DEFAULT_STUN_TURN_URL;
  self->gather_started = FALSE;

  self->data_channels = g_hash_table_new_full (g_direct_hash,
      g_direct_equal, NULL, (GDestroyNotify) kms_ref_struct_unref);
}

void
kms_webrtc_session_set_callbacks (KmsWebrtcSession * self,
    KmsWebrtcSessionCallbacks * cb, gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  KMS_SDP_SESSION_LOCK (self);

  destroy = self->destroy_data;
  data = self->cb_data;

  self->cb_data = user_data;
  self->destroy_data = notify;
  self->add_pad_cb = cb->add_pad_cb;
  self->remove_pad_cb = cb->remove_pad_cb;

  KMS_SDP_SESSION_UNLOCK (self);

  if (destroy != NULL && data != NULL) {
    destroy (data);
  }
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
  gobject_class->set_property = kms_webrtc_session_set_property;
  gobject_class->get_property = kms_webrtc_session_get_property;

  klass->post_constructor = kms_webrtc_session_post_constructor;
  klass->gather_candidates = kms_webrtc_session_gather_candidates;
  klass->add_ice_candidate = kms_webrtc_session_add_ice_candidate;
  klass->init_ice_agent = kms_webrtc_session_init_ice_agent;
  klass->create_data_channel = kms_webrtc_session_create_data_channel;
  klass->destroy_data_channel = kms_webrtc_session_destroy_data_channel;

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

  g_object_class_install_property (gobject_class, PROP_STUN_SERVER_IP,
      g_param_spec_string ("stun-server",
          "StunServer",
          "Stun Server IP Address",
          DEFAULT_STUN_SERVER_IP, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_STUN_SERVER_PORT,
      g_param_spec_uint ("stun-server-port",
          "StunServerPort",
          "Stun Server Port",
          1, G_MAXUINT16, DEFAULT_STUN_SERVER_PORT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_TURN_URL,
      g_param_spec_string ("turn-url",
          "TurnUrl",
          "TURN server URL with this format: 'user:password@address:port(?transport=[udp|tcp|tls])'."
          "'address' must be an IP (not a domain)."
          "'transport' is optional (UDP by default).",
          DEFAULT_STUN_TURN_URL, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PEM_CERTIFICATE,
      g_param_spec_string ("pem-certificate",
          "PemCertificate",
          "Pem certificate to be used in dtls",
          DEFAULT_PEM_CERTIFICATE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_DATA_CHANNEL_SUPPORTED,
      g_param_spec_boolean ("data-channel-supported",
          "Data channel supported",
          "True if data channels are negotiated and supported",
          DEFAULT_DATA_CHANNELS_SUPPORTED,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

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
      NULL, NULL, G_TYPE_NONE, 0);

  /**
   * KmsWebrtcSession::on-component-state-changed
   * @self: the object which received the signal
   * @stream_id: The ID of the stream
   * @component_id: The ID of the component
   * @state: The #NiceComponentState of the component
   *
   * This signal is fired whenever a component's state changes
   */
  kms_webrtc_session_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED] =
      g_signal_new ("on-ice-component-state-changed",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 3, G_TYPE_STRING, G_TYPE_UINT, G_TYPE_UINT, G_TYPE_INVALID);

  kms_webrtc_session_signals[SIGNAL_GATHER_CANDIDATES] =
      g_signal_new ("gather-candidates",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, gather_candidates), NULL, NULL,
      __kms_webrtc_marshal_BOOLEAN__VOID, G_TYPE_BOOLEAN, 0);

  kms_webrtc_session_signals[SIGNAL_NEW_SELECTED_PAIR_FULL] =
      g_signal_new ("new-selected-pair-full",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 4, G_TYPE_STRING, G_TYPE_UINT, KMS_TYPE_ICE_CANDIDATE,
      KMS_TYPE_ICE_CANDIDATE);

  kms_webrtc_session_signals[SIGNAL_ADD_ICE_CANDIDATE] =
      g_signal_new ("add-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, add_ice_candidate), NULL, NULL,
      __kms_webrtc_marshal_BOOLEAN__OBJECT, G_TYPE_BOOLEAN, 1,
      KMS_TYPE_ICE_CANDIDATE);

  /* TODO: look for a better way of doing this */
  kms_webrtc_session_signals[SIGNAL_INIT_ICE_AGENT] =
      g_signal_new ("init-ice-agent",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, init_ice_agent), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_webrtc_session_signals[SIGNAL_DATA_SESSION_ESTABLISHED] =
      g_signal_new ("data-session-established",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, data_session_established),
      NULL, NULL, g_cclosure_marshal_VOID__BOOLEAN, G_TYPE_NONE, 1,
      G_TYPE_BOOLEAN);

  kms_webrtc_session_signals[SIGNAL_DATA_CHANNEL_OPENED] =
      g_signal_new ("data-channel-opened",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, data_channel_opened),
      NULL, NULL, g_cclosure_marshal_VOID__UINT, G_TYPE_NONE, 1, G_TYPE_UINT);

  kms_webrtc_session_signals[SIGNAL_DATA_CHANNEL_CLOSED] =
      g_signal_new ("data-channel-closed",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, data_channel_closed),
      NULL, NULL, g_cclosure_marshal_VOID__UINT, G_TYPE_NONE, 1, G_TYPE_UINT);

  kms_webrtc_session_signals[ACTION_CREATE_DATA_CHANNEL] =
      g_signal_new ("create-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, create_data_channel),
      NULL, NULL, __kms_webrtc_data_marshal_INT__BOOLEAN_INT_INT_STRING_STRING,
      G_TYPE_INT, 5, G_TYPE_BOOLEAN, G_TYPE_INT, G_TYPE_INT, G_TYPE_STRING,
      G_TYPE_STRING);

  kms_webrtc_session_signals[ACTION_DESTROY_DATA_CHANNEL] =
      g_signal_new ("destroy-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcSessionClass, destroy_data_channel),
      NULL, NULL, g_cclosure_marshal_VOID__INT, G_TYPE_NONE, 1, G_TYPE_INT);
}
