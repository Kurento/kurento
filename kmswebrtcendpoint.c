/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include "kmswebrtcendpoint.h"
#include <nice/nice.h>

#define PLUGIN_NAME "webrtcendpoint"

#define GST_CAT_DEFAULT kms_webrtc_end_point_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_end_point_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcEndPoint, kms_webrtc_end_point,
    KMS_TYPE_BASE_RTP_END_POINT);

#define KMS_WEBRTC_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_END_POINT,                  \
    KmsWebrtcEndPointPrivate                    \
  )                                             \
)

enum
{
  PROP_0,
  PROP_CERTIFICATE_PEM_FILE,
  N_PROPERTIES
};

#define NICE_N_COMPONENTS 2

#define AUDIO_STREAM_NAME "audio"
#define VIDEO_STREAM_NAME "video"

#define SDP_MEDIA_RTP_AVP_PROTO "RTP/AVP"
#define SDP_MEDIA_RTP_SAVPF_PROTO "RTP/SAVPF"
#define SDP_ICE_UFRAG_ATTR "ice-ufrag"
#define SDP_ICE_PWD_ATTR "ice-pwd"
#define SDP_CANDIDATE_ATTR "candidate"
#define SDP_CANDIDATE_ATTR_LEN 12

typedef struct _KmsWebRTCTransport
{
  guint component_id;

  GstElement *dtlssrtpenc;
  GstElement *dtlssrtpdec;
  GstElement *nicesink;
  GstElement *nicesrc;
} KmsWebRTCTransport;

typedef struct _KmsWebRTCConnection
{
  NiceAgent *agent;
  guint stream_id;

  KmsWebRTCTransport *rtp_transport;
  KmsWebRTCTransport *rtcp_transport;
} KmsWebRTCConnection;

struct _KmsWebrtcEndPointPrivate
{
  GMainContext *context;
  GMainLoop *loop;
  GThread *thread;
  gboolean finalized;

  NiceAgent *agent;
  KmsWebRTCConnection *audio_connection;
  gboolean audio_ice_gathering_done;
  KmsWebRTCConnection *video_connection;
  gboolean video_ice_gathering_done;

  GMutex gather_mutex;
  GCond gather_cond;
  gboolean wait_gathering;
  gboolean ice_gathering_done;

  gchar *certificate_pem_file;
};

/* KmsWebRTCTransport */

static void
kms_webrtc_transport_destroy (KmsWebRTCTransport * tr)
{
  if (tr == NULL)
    return;

  if (tr->dtlssrtpenc != NULL) {
    g_object_unref (tr->dtlssrtpenc);
    tr->dtlssrtpenc = NULL;
  }

  if (tr->dtlssrtpdec != NULL) {
    g_object_unref (tr->dtlssrtpdec);
    tr->dtlssrtpdec = NULL;
  }

  if (tr->nicesink != NULL) {
    g_object_unref (tr->nicesink);
    tr->nicesink = NULL;
  }

  if (tr->nicesrc != NULL) {
    g_object_unref (tr->nicesrc);
    tr->nicesrc = NULL;
  }

  g_slice_free (KmsWebRTCTransport, tr);
}

static KmsWebRTCTransport *
kms_webrtc_transport_create (NiceAgent * agent, guint stream_id,
    guint component_id)
{
  KmsWebRTCTransport *tr;
  gchar *str;

  tr = g_slice_new0 (KmsWebRTCTransport);

  /* TODO: improve creating elements when needed */
  tr->component_id = component_id;
  tr->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);
  tr->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);
  tr->nicesink = gst_element_factory_make ("nicesink", NULL);
  tr->nicesrc = gst_element_factory_make ("nicesrc", NULL);

  if (tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL
      || tr->dtlssrtpenc == NULL || tr->dtlssrtpenc == NULL) {
    GST_ERROR ("Cannot create KmsWebRTCTransport");
    kms_webrtc_transport_destroy (tr);
    return NULL;
  }

  str =
      g_strdup_printf ("%s-%s-%" G_GUINT32_FORMAT "-%" G_GUINT32_FORMAT,
      GST_OBJECT_NAME (tr->dtlssrtpenc), GST_OBJECT_NAME (tr->dtlssrtpdec),
      stream_id, component_id);
  g_object_set (G_OBJECT (tr->dtlssrtpenc), "channel-id", str, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "channel-id", str, NULL);
  g_free (str);

  g_object_set (G_OBJECT (tr->nicesink), "agent", agent, "stream", stream_id,
      "component", component_id, NULL);
  g_object_set (G_OBJECT (tr->nicesrc), "agent", agent, "stream", stream_id,
      "component", component_id, NULL);

  return tr;
}

/* KmsWebRTCTransport */

/* WebRTCConnection */

static void
nice_agent_recv (NiceAgent * agent, guint stream_id, guint component_id,
    guint len, gchar * buf, gpointer user_data)
{
  /* Nothing to do, this callback is only for negotiation */
  GST_TRACE ("ICE data received on stream_id: '%" G_GUINT32_FORMAT
      "' component_id: '%" G_GUINT32_FORMAT "'", stream_id, component_id);
}

static void
kms_webrtc_connection_destroy (KmsWebRTCConnection * conn)
{
  if (conn == NULL)
    return;

  kms_webrtc_transport_destroy (conn->rtp_transport);
  kms_webrtc_transport_destroy (conn->rtcp_transport);

  nice_agent_remove_stream (conn->agent, conn->stream_id);

  g_slice_free (KmsWebRTCConnection, conn);
}

static KmsWebRTCConnection *
kms_webrtc_connection_create (NiceAgent * agent, GMainContext * context,
    const gchar * name)
{
  KmsWebRTCConnection *conn;

  conn = g_slice_new0 (KmsWebRTCConnection);

  conn->agent = agent;
  conn->stream_id = nice_agent_add_stream (agent, NICE_N_COMPONENTS);
  if (conn->stream_id == 0) {
    GST_ERROR ("Cannot add nice stream for %s.", name);
    kms_webrtc_connection_destroy (conn);
    return NULL;
  }

  nice_agent_set_stream_name (agent, conn->stream_id, name);
  nice_agent_attach_recv (agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTP, context, nice_agent_recv, NULL);
  nice_agent_attach_recv (agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTCP, context, nice_agent_recv, NULL);

  conn->rtp_transport =
      kms_webrtc_transport_create (agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTP);
  conn->rtcp_transport =
      kms_webrtc_transport_create (agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTCP);

  if (conn->rtp_transport == NULL || conn->rtp_transport == NULL) {
    GST_ERROR ("Cannot create KmsWebRTCConnection.");
    g_slice_free (KmsWebRTCConnection, conn);
    return NULL;
  }

  return conn;
}

/* WebRTCConnection */

static void
update_sdp_media (GstSDPMedia * media, NiceAgent * agent, guint stream_id,
    gboolean use_ipv6)
{
  const gchar *proto_str;
  GSList *candidates;
  GSList *walk;
  NiceCandidate *rtp_default_candidate, *rtcp_default_candidate;
  gchar rtp_addr[NICE_ADDRESS_STRING_LEN + 1];
  gchar rtcp_addr[NICE_ADDRESS_STRING_LEN + 1];
  const gchar *rtp_addr_type, *rtcp_addr_type;
  gboolean rtp_is_ipv6, rtcp_is_ipv6;
  guint rtp_port, rtcp_port;
  gchar *ufrag, *pwd;
  guint conn_len, c;
  gchar *str;

  proto_str = gst_sdp_media_get_proto (media);
  if (g_ascii_strcasecmp (SDP_MEDIA_RTP_AVP_PROTO, proto_str) != 0 &&
      g_ascii_strcasecmp (SDP_MEDIA_RTP_SAVPF_PROTO, proto_str)) {
    GST_WARNING ("Proto \"%s\" not supported", proto_str);
    ((GstSDPMedia *) media)->port = 0;
    return;
  }

  gst_sdp_media_set_proto (media, SDP_MEDIA_RTP_SAVPF_PROTO);

  rtp_default_candidate =
      nice_agent_get_default_local_candidate (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);
  rtcp_default_candidate =
      nice_agent_get_default_local_candidate (agent, stream_id,
      NICE_COMPONENT_TYPE_RTCP);

  nice_address_to_string (&rtp_default_candidate->addr, rtp_addr);
  rtp_port = nice_address_get_port (&rtp_default_candidate->addr);
  rtp_is_ipv6 = nice_address_ip_version (&rtp_default_candidate->addr) == 6;
  nice_candidate_free (rtp_default_candidate);

  nice_address_to_string (&rtcp_default_candidate->addr, rtcp_addr);
  rtcp_port = nice_address_get_port (&rtcp_default_candidate->addr);
  rtcp_is_ipv6 = nice_address_ip_version (&rtcp_default_candidate->addr) == 6;
  nice_candidate_free (rtcp_default_candidate);

  rtp_addr_type = rtp_is_ipv6 ? "IP6" : "IP4";
  rtcp_addr_type = rtcp_is_ipv6 ? "IP6" : "IP4";

  if (use_ipv6 != rtp_is_ipv6) {
    GST_WARNING ("No valid rtp address type: %s", rtp_addr_type);
    return;
  }

  ((GstSDPMedia *) media)->port = rtp_port;
  conn_len = gst_sdp_media_connections_len (media);
  for (c = 0; c < conn_len; c++) {
    gst_sdp_media_remove_connection ((GstSDPMedia *) media, c);
  }
  gst_sdp_media_add_connection ((GstSDPMedia *) media, "IN", rtp_addr_type,
      rtp_addr, 0, 0);

  str = g_strdup_printf ("%d IN %s %s", rtcp_port, rtcp_addr_type, rtcp_addr);
  gst_sdp_media_add_attribute ((GstSDPMedia *) media, "rtcp", str);
  g_free (str);

  /* ICE credentials */
  nice_agent_get_local_credentials (agent, stream_id, &ufrag, &pwd);
  gst_sdp_media_add_attribute ((GstSDPMedia *) media, SDP_ICE_UFRAG_ATTR,
      ufrag);
  g_free (ufrag);
  gst_sdp_media_add_attribute ((GstSDPMedia *) media, SDP_ICE_PWD_ATTR, pwd);
  g_free (pwd);
  /* TODO: add fingerprint */

  /* ICE candidates */
  candidates =
      nice_agent_get_local_candidates (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);
  candidates =
      g_slist_concat (candidates,
      nice_agent_get_local_candidates (agent, stream_id,
          NICE_COMPONENT_TYPE_RTCP));

  for (walk = candidates; walk; walk = walk->next) {
    NiceCandidate *cand = walk->data;

    str = nice_agent_generate_local_candidate_sdp (agent, cand);
    gst_sdp_media_add_attribute ((GstSDPMedia *) media, SDP_CANDIDATE_ATTR,
        str + SDP_CANDIDATE_ATTR_LEN);
    g_free (str);
  }

  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);
}

static gboolean
kms_webrtc_end_point_set_transport_to_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * msg)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (base_sdp_end_point);
  guint len, i;

  /* Wait for ICE candidates */
  g_mutex_lock (&self->priv->gather_mutex);
  self->priv->wait_gathering = TRUE;
  while (!self->priv->finalized && !self->priv->ice_gathering_done)
    g_cond_wait (&self->priv->gather_cond, &self->priv->gather_mutex);
  self->priv->wait_gathering = FALSE;
  g_cond_signal (&self->priv->gather_cond);
  g_mutex_unlock (&self->priv->gather_mutex);

  if (self->priv->finalized) {
    GST_ERROR_OBJECT (self, "WebrtcEndPoint has finalized.");
    return FALSE;
  }

  len = gst_sdp_message_medias_len (msg);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (msg, i);
    const gchar *media_str;
    guint stream_id;

    media_str = gst_sdp_media_get_media (media);
    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      stream_id = self->priv->audio_connection->stream_id;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      stream_id = self->priv->video_connection->stream_id;
    } else {
      GST_WARNING_OBJECT (self, "Media \"%s\" not supported", media_str);
      continue;
    }

    update_sdp_media ((GstSDPMedia *) media, self->priv->agent,
        stream_id, base_sdp_end_point->use_ipv6);
  }

  return TRUE;
}

static void
add_webrtc_transport_src (KmsWebrtcEndPoint * webrtc_end_point,
    KmsWebRTCTransport * tr, gboolean is_client, const gchar * sink_pad_name)
{
  KmsBaseRtpEndPoint *base_rtp_end_point =
      KMS_BASE_RTP_END_POINT (webrtc_end_point);

  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", is_client, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", is_client, NULL);

  gst_bin_add_many (GST_BIN (webrtc_end_point),
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);

  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);
  gst_element_link_pads (tr->dtlssrtpdec, "src",
      base_rtp_end_point->rtpbin, sink_pad_name);

  gst_element_sync_state_with_parent (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent (tr->nicesrc);
}

static void
add_webrtc_connection_src (KmsWebrtcEndPoint * webrtc_end_point,
    KmsWebRTCConnection * conn, gboolean is_client)
{
  const gchar *stream_name;
  const gchar *rtp_sink_name, *rtcp_sink_name;

  /* FIXME: improve this */
  rtp_sink_name = "recv_rtp_sink_0";    /* audio by default */
  rtcp_sink_name = "recv_rtcp_sink_0";  /* audio by default */
  stream_name = nice_agent_get_stream_name (conn->agent, conn->stream_id);
  if (g_strcmp0 (VIDEO_STREAM_NAME, stream_name) == 0) {
    rtp_sink_name = "recv_rtp_sink_1";
    rtcp_sink_name = "recv_rtcp_sink_1";
  }

  add_webrtc_transport_src (webrtc_end_point, conn->rtp_transport, is_client,
      rtp_sink_name);
  add_webrtc_transport_src (webrtc_end_point, conn->rtcp_transport, is_client,
      rtcp_sink_name);
}

static void
process_sdp_media (const GstSDPMedia * media, NiceAgent * agent,
    guint stream_id, const gchar * msg_ufrag, const gchar * msg_pwd)
{
  const gchar *proto_str;
  const gchar *ufrag, *pwd;
  GRegex *regex;
  guint len, i;

  proto_str = gst_sdp_media_get_proto (media);
  if (g_ascii_strcasecmp (SDP_MEDIA_RTP_SAVPF_PROTO, proto_str) != 0) {
    GST_WARNING ("Proto \"%s\" not supported", proto_str);
    return;
  }

  ufrag = gst_sdp_media_get_attribute_val (media, SDP_ICE_UFRAG_ATTR);
  pwd = gst_sdp_media_get_attribute_val (media, SDP_ICE_PWD_ATTR);
  if (!nice_agent_set_remote_credentials (agent, stream_id, ufrag, pwd)) {
    GST_WARNING ("Cannot set remote media credentials.");
    if (!nice_agent_set_remote_credentials (agent, stream_id, msg_ufrag,
            msg_pwd)) {
      GST_WARNING ("Cannot set remote message credentials.");
      return;
    }
  }

  regex = g_regex_new ("^(?<foundation>[0-9]+) (?<cid>[0-9]+)"
      " (udp|UDP) (?<prio>[0-9]+) (?<addr>[0-9.:a-zA-Z]+)"
      " (?<port>[0-9]+) typ (?<type>(host|srflx|prflx|relay))( generation [0-9]+)?$",
      0, 0, NULL);

  len = gst_sdp_media_attributes_len (media);
  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;
    GMatchInfo *match_info = NULL;

    attr = gst_sdp_media_get_attribute (media, i);
    if (g_strcmp0 (SDP_CANDIDATE_ATTR, attr->key) != 0)
      continue;

    g_regex_match (regex, attr->value, 0, &match_info);

    while (g_match_info_matches (match_info)) {
      NiceCandidateType type;
      NiceCandidate *cand = NULL;
      GSList *candidates;

      gchar *foundation = g_match_info_fetch_named (match_info, "foundation");
      gchar *cid_str = g_match_info_fetch_named (match_info, "cid");
      gchar *prio_str = g_match_info_fetch_named (match_info, "prio");
      gchar *addr = g_match_info_fetch_named (match_info, "addr");
      gchar *port_str = g_match_info_fetch_named (match_info, "port");
      gchar *type_str = g_match_info_fetch_named (match_info, "type");

      /* rfc5245-15.1 */
      if (g_strcmp0 ("host", type_str) == 0) {
        type = NICE_CANDIDATE_TYPE_HOST;
      } else if (g_strcmp0 ("srflx", type_str) == 0) {
        type = NICE_CANDIDATE_TYPE_SERVER_REFLEXIVE;
      } else if (g_strcmp0 ("prflx", type_str) == 0) {
        type = NICE_CANDIDATE_TYPE_PEER_REFLEXIVE;
      } else if (g_strcmp0 ("relay", type_str) == 0) {
        type = NICE_CANDIDATE_TYPE_RELAYED;
      } else {
        GST_WARNING ("Candidate type '%s' not supported", type_str);
        goto next;
      }

      cand = nice_candidate_new (type);
      cand->component_id = g_ascii_strtoll (cid_str, NULL, 10);
      cand->priority = g_ascii_strtoll (prio_str, NULL, 10);
      g_strlcpy (cand->foundation, foundation, NICE_CANDIDATE_MAX_FOUNDATION);

      if (!nice_address_set_from_string (&cand->addr, addr)) {
        GST_WARNING ("Cannot set address '%s' to candidate", addr);
        goto next;
      }
      nice_address_set_port (&cand->addr, g_ascii_strtoll (port_str, NULL, 10));

      candidates = g_slist_append (NULL, cand);
      if (nice_agent_set_remote_candidates (agent, stream_id,
              cand->component_id, candidates) < 0) {
        GST_WARNING ("Cannot add candidate: '%s'in stream_id: %d.", attr->value,
            stream_id);
      } else {
        GST_TRACE ("Candidate added: '%s' in stream_id: %d.", attr->value,
            stream_id);
      }
      g_slist_free (candidates);

    next:
      g_free (addr);
      g_free (foundation);
      g_free (cid_str);
      g_free (prio_str);
      g_free (port_str);
      g_free (type_str);

      if (cand != NULL)
        nice_candidate_free (cand);

      g_match_info_next (match_info, NULL);
    }

    g_match_info_free (match_info);
  }

  g_regex_unref (regex);
}

static void
kms_webrtc_end_point_start_transport_send (KmsBaseSdpEndPoint *
    base_rtp_end_point, const GstSDPMessage * offer,
    const GstSDPMessage * answer, gboolean local_offer)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (base_rtp_end_point);
  const GstSDPMessage *sdp;
  const gchar *ufrag, *pwd;
  guint len, i;

  if (gst_sdp_message_medias_len (answer) != gst_sdp_message_medias_len (offer)) {
    GST_WARNING ("Incompatible offer and answer, possible errors in media");
  }

  if (local_offer) {
    sdp = answer;
  } else {
    sdp = offer;
  }

  ufrag = gst_sdp_message_get_attribute_val (sdp, SDP_ICE_UFRAG_ATTR);
  pwd = gst_sdp_message_get_attribute_val (sdp, SDP_ICE_PWD_ATTR);

  len = gst_sdp_message_medias_len (sdp);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (sdp, i);
    const gchar *media_str;
    KmsWebRTCConnection *conn;

    media_str = gst_sdp_media_get_media (media);
    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      conn = self->priv->audio_connection;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      conn = self->priv->video_connection;
    } else {
      GST_WARNING_OBJECT (self, "Media \"%s\" not supported", media_str);
      continue;
    }

    process_sdp_media (media, self->priv->agent, conn->stream_id, ufrag, pwd);
    add_webrtc_connection_src (self, conn, !local_offer);
  }
}

static void
gathering_done (NiceAgent * agent, guint stream_id, KmsWebrtcEndPoint * self)
{
  GST_DEBUG_OBJECT (self, "ICE gathering done for %s stream.",
      nice_agent_get_stream_name (agent, stream_id));

  g_mutex_lock (&self->priv->gather_mutex);

  if (self->priv->audio_connection != NULL
      && stream_id == self->priv->audio_connection->stream_id)
    self->priv->audio_ice_gathering_done = TRUE;
  if (self->priv->video_connection != NULL
      && stream_id == self->priv->video_connection->stream_id)
    self->priv->video_ice_gathering_done = TRUE;

  self->priv->ice_gathering_done = self->priv->audio_ice_gathering_done &&
      self->priv->video_ice_gathering_done;

  g_cond_signal (&self->priv->gather_cond);
  g_mutex_unlock (&self->priv->gather_mutex);
}

static gpointer
loop_thread (gpointer user_data)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (user_data);

  g_main_context_acquire (self->priv->context);
  g_main_loop_run (self->priv->loop);
  g_main_context_release (self->priv->context);

  return NULL;
}

static gboolean
quit_main_loop_idle (gpointer loop)
{
  g_main_loop_quit (loop);

  return G_SOURCE_REMOVE;
}

static void
destroy_main_loop (gpointer loop)
{
  g_main_loop_unref (loop);
}

static void
kms_webrtc_end_point_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (object);

  switch (prop_id) {
    case PROP_CERTIFICATE_PEM_FILE:
      if (self->priv->certificate_pem_file != NULL)
        g_free (self->priv->certificate_pem_file);

      self->priv->certificate_pem_file = g_value_dup_string (value);
      g_object_set_property (G_OBJECT (self->priv->
              audio_connection->rtp_transport->dtlssrtpdec),
          "certificate-pem-file", value);
      g_object_set_property (G_OBJECT (self->priv->
              audio_connection->rtcp_transport->dtlssrtpdec),
          "certificate-pem-file", value);
      g_object_set_property (G_OBJECT (self->priv->
              video_connection->rtp_transport->dtlssrtpdec),
          "certificate-pem-file", value);
      g_object_set_property (G_OBJECT (self->priv->
              video_connection->rtcp_transport->dtlssrtpdec),
          "certificate-pem-file", value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

}

static void
kms_webrtc_end_point_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (object);

  switch (prop_id) {
    case PROP_CERTIFICATE_PEM_FILE:
      g_value_set_string (value, self->priv->certificate_pem_file);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_webrtc_end_point_finalize (GObject * object)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (object);

  if (self->priv->agent != NULL) {
    kms_webrtc_connection_destroy (self->priv->audio_connection);
    kms_webrtc_connection_destroy (self->priv->video_connection);
    g_object_unref (self->priv->agent);
    self->priv->agent = NULL;
  }

  if (self->priv->loop != NULL) {
    GSource *source;

    source = g_idle_source_new ();
    g_source_set_callback (source, quit_main_loop_idle, self->priv->loop,
        destroy_main_loop);
    g_source_attach (source, self->priv->context);
    g_source_unref (source);
    self->priv->loop = NULL;
  }

  if (self->priv->thread != NULL) {
    g_thread_join (self->priv->thread);
    g_thread_unref (self->priv->thread);
  }

  if (self->priv->context != NULL) {
    g_main_context_unref (self->priv->context);
    self->priv->context = NULL;
  }

  g_mutex_lock (&self->priv->gather_mutex);
  self->priv->finalized = TRUE;
  g_cond_signal (&self->priv->gather_cond);
  while (self->priv->wait_gathering)
    g_cond_wait (&self->priv->gather_cond, &self->priv->gather_mutex);
  g_mutex_unlock (&self->priv->gather_mutex);

  g_cond_clear (&self->priv->gather_cond);
  g_mutex_clear (&self->priv->gather_mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_end_point_parent_class)->finalize (object);
}

static void
kms_webrtc_end_point_class_init (KmsWebrtcEndPointClass * klass)
{
  GObjectClass *gobject_class;
  KmsBaseSdpEndPointClass *base_sdp_end_point_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_webrtc_end_point_set_property;
  gobject_class->get_property = kms_webrtc_end_point_get_property;
  gobject_class->finalize = kms_webrtc_end_point_finalize;

  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "WebrtcEndPoint",
      "WEBRTC/Stream/WebrtcEndPoint",
      "WebRTC EndPoint element", "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_end_point_class = KMS_BASE_SDP_END_POINT_CLASS (klass);
  base_sdp_end_point_class->set_transport_to_sdp =
      kms_webrtc_end_point_set_transport_to_sdp;
  base_sdp_end_point_class->start_transport_send =
      kms_webrtc_end_point_start_transport_send;

  g_object_class_install_property (gobject_class, PROP_CERTIFICATE_PEM_FILE,
      g_param_spec_string ("certificate-pem-file",
          "Certificate PEM File",
          "PEM File name containing the certificate and private key",
          NULL,
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndPointPrivate));
}

static void
kms_webrtc_end_point_init (KmsWebrtcEndPoint * self)
{
  self->priv = KMS_WEBRTC_END_POINT_GET_PRIVATE (self);

  g_mutex_init (&self->priv->gather_mutex);
  g_cond_init (&self->priv->gather_cond);
  self->priv->finalized = FALSE;

  self->priv->context = g_main_context_new ();
  if (self->priv->context == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create context.");
    return;
  }

  self->priv->loop = g_main_loop_new (self->priv->context, TRUE);
  if (self->priv->loop == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create main loop.");
    return;
  }

  self->priv->thread =
      g_thread_new (GST_ELEMENT_NAME (self), loop_thread, self);

  self->priv->agent =
      nice_agent_new (self->priv->context, NICE_COMPATIBILITY_RFC5245);
  if (self->priv->agent == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create nice agent.");
    return;
  }

  g_object_set (self->priv->agent, "upnp", FALSE, NULL);
  g_signal_connect (self->priv->agent, "candidate-gathering-done",
      G_CALLBACK (gathering_done), self);

  self->priv->audio_connection =
      kms_webrtc_connection_create (self->priv->agent, self->priv->context,
      AUDIO_STREAM_NAME);
  if (self->priv->audio_connection == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create audio connection.");
    return;
  }

  self->priv->video_connection =
      kms_webrtc_connection_create (self->priv->agent, self->priv->context,
      VIDEO_STREAM_NAME);
  if (self->priv->video_connection == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create video connection.");
    return;
  }

  if (!nice_agent_gather_candidates (self->priv->agent,
          self->priv->audio_connection->stream_id)) {
    GST_ERROR_OBJECT (self, "Failed to start candidate gathering for %s.",
        AUDIO_STREAM_NAME);
  }

  if (!nice_agent_gather_candidates (self->priv->agent,
          self->priv->video_connection->stream_id)) {
    GST_ERROR_OBJECT (self, "Failed to start candidate gathering for %s.",
        VIDEO_STREAM_NAME);
  }
}

gboolean
kms_webrtc_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_END_POINT);
}
