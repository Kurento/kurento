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
#define _XOPEN_SOURCE 500

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmswebrtcendpoint.h"
#include <nice/nice.h>
#include <gio/gio.h>
#include <stdlib.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

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
  PROP_STUN_SERVER_IP,
  PROP_STUN_SERVER_PORT,
  N_PROPERTIES
};

#define IPV4 4
#define IPV6 6

#define NICE_N_COMPONENTS 2

#define AUDIO_STREAM_NAME "audio"
#define VIDEO_STREAM_NAME "video"

#define SDP_MEDIA_RTP_AVP_PROTO "RTP/AVP"
#define SDP_MEDIA_RTP_SAVPF_PROTO "RTP/SAVPF"
#define SDP_ICE_UFRAG_ATTR "ice-ufrag"
#define SDP_ICE_PWD_ATTR "ice-pwd"
#define SDP_CANDIDATE_ATTR "candidate"
#define SDP_CANDIDATE_ATTR_LEN 12

#define FINGERPRINT_CHECKSUM G_CHECKSUM_SHA256

#define FILE_PERMISIONS (S_IRWXU | S_IRWXG | S_IRWXO)
#define ROOT_TMP_DIR "/tmp/kms_webrtc_end_point"
#define TMP_DIR_TEMPLATE ROOT_TMP_DIR "/XXXXXX"
#define CERTTOOL_TEMPLATE "certtool.tmpl"
#define CERT_KEY_PEM_FILE "certkey.pem"

/* rtpbin pad names */
#define AUDIO_RTPBIN_RECV_RTP_SINK "recv_rtp_sink_0"
#define AUDIO_RTPBIN_RECV_RTCP_SINK "recv_rtcp_sink_0"
#define AUDIO_RTPBIN_SEND_RTP_SINK "send_rtp_src_0"
#define AUDIO_RTPBIN_SEND_RTCP_SINK "send_rtcp_src_0"
#define VIDEO_RTPBIN_RECV_RTP_SINK "recv_rtp_sink_1"
#define VIDEO_RTPBIN_RECV_RTCP_SINK "recv_rtcp_sink_1"
#define VIDEO_RTPBIN_SEND_RTP_SINK "send_rtp_src_1"
#define VIDEO_RTPBIN_SEND_RTCP_SINK "send_rtcp_src_1"

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
  gchar *tmp_dir;

  GMainContext *context;
  GMainLoop *loop;
  GThread *thread;
  gboolean finalized;

  NiceAgent *agent;
  gint is_bundle;               /* Implies rtcp-mux */

  KmsWebRTCConnection *bundle_connection;       /* Uses audio_connection */
  GstElement *bundle_rtp_funnel;
  GstElement *bundle_rtcp_funnel;
  gboolean bundle_funnels_added;

  gchar *remote_audio_ssrc;
  KmsWebRTCConnection *audio_connection;
  gboolean audio_ice_gathering_done;

  gchar *remote_video_ssrc;
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

  if (conn->rtp_transport == NULL || conn->rtcp_transport == NULL) {
    GST_ERROR ("Cannot create KmsWebRTCConnection.");
    g_slice_free (KmsWebRTCConnection, conn);
    return NULL;
  }

  return conn;
}

/* WebRTCConnection */

/* ConnectRtcpData */

typedef struct _ConnectRtcpData
{
  KmsWebrtcEndPoint *webrtc_end_point;
  KmsWebRTCTransport *tr;
  const gchar *src_pad_name;
} ConnectRtcpData;

static void
connect_rtcp_data_destroy (gpointer data)
{
  if (data == NULL)
    return;

  g_slice_free (ConnectRtcpData, data);
}

/* ConnectRtcpData */

static int
delete_file (const char *fpath, const struct stat *sb, int typeflag,
    struct FTW *ftwbuf)
{
  int rv = g_remove (fpath);

  if (rv) {
    GST_WARNING ("Error deleting file: %s. %s", fpath, strerror (errno));
  }

  return rv;
}

static void
remove_recursive (const gchar * path)
{
  nftw (path, delete_file, 64, FTW_DEPTH | FTW_PHYS);
}

static gchar *
generate_certkey_pem_file (const gchar * dir)
{
  gchar *cmd, *template_path, *pem_path;
  int ret;

  if (dir == NULL)
    return NULL;

  pem_path = g_strdup_printf ("%s/%s", dir, CERT_KEY_PEM_FILE);
  cmd =
      g_strconcat ("/bin/sh -c \"certtool --generate-privkey --outfile ",
      pem_path, "\"", NULL);
  ret = system (cmd);
  g_free (cmd);

  if (ret == -1)
    goto err;

  template_path = g_strdup_printf ("%s/%s", dir, CERTTOOL_TEMPLATE);
  cmd =
      g_strconcat
      ("/bin/sh -c \"echo 'organization = kurento' > ", template_path,
      " && certtool --generate-self-signed --load-privkey ", pem_path,
      " --template ", template_path, " >> ", pem_path, " 2>/dev/null\"", NULL);
  g_free (template_path);
  ret = system (cmd);
  g_free (cmd);

  if (ret == -1)
    goto err;

  return pem_path;

err:

  GST_ERROR ("Error while generating certificate file");

  g_free (pem_path);
  return NULL;
}

static gchar *
generate_fingerprint (const gchar * pem_file)
{
  GTlsCertificate *cert;
  GError *error = NULL;
  GByteArray *ba;
  gssize length;
  int size;
  gchar *fingerprint;
  gchar *fingerprint_colon;
  int i, j;

  cert = g_tls_certificate_new_from_file (pem_file, &error);
  g_object_get (cert, "certificate", &ba, NULL);
  fingerprint =
      g_compute_checksum_for_data (FINGERPRINT_CHECKSUM, ba->data, ba->len);
  g_object_unref (cert);

  length = g_checksum_type_get_length (FINGERPRINT_CHECKSUM);
  size = (int) (length * 2 + length) * sizeof (gchar);
  fingerprint_colon = g_malloc0 (size);

  j = 0;
  for (i = 0; i < length * 2; i += 2) {
    fingerprint_colon[j] = g_ascii_toupper (fingerprint[i]);
    fingerprint_colon[++j] = g_ascii_toupper (fingerprint[i + 1]);
    fingerprint_colon[++j] = ':';
    j++;
  };
  fingerprint_colon[size - 1] = '\0';
  g_free (fingerprint);

  return fingerprint_colon;
}

static gchar *
kms_webrtc_endpoint_generate_fingerprint_sdp_attr (KmsWebrtcEndPoint * self)
{
  gchar *fp, *ret;

  if (self->priv->certificate_pem_file == NULL) {
    gchar *autogenerated_pem_file;

    GST_ELEMENT_INFO (self, RESOURCE, SETTINGS,
        ("\"certificate_pem_file\" property not set, autogenerate a certificate"),
        (NULL));

    autogenerated_pem_file = generate_certkey_pem_file (self->priv->tmp_dir);

    if (autogenerated_pem_file == NULL) {
      GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS,
          ("A certicate cannot be autogenerated."), GST_ERROR_SYSTEM);
      g_free (autogenerated_pem_file);

      return NULL;
    }

    g_object_set (self, "certificate-pem-file", autogenerated_pem_file, NULL);
    g_free (autogenerated_pem_file);
  }

  fp = generate_fingerprint (self->priv->certificate_pem_file);
  ret = g_strconcat ("sha-256 ", fp, NULL);
  g_free (fp);

  return ret;
}

static guint
rtpbin_get_ssrc (GstElement * rtpbin, const gchar * rtpbin_pad_name)
{
  guint ssrc = 0;
  GstPad *pad = gst_element_get_static_pad (rtpbin, rtpbin_pad_name);
  GstCaps *caps;
  int i;

  if (pad == NULL) {            /* FIXME: race condition problem */
    pad = gst_element_get_request_pad (rtpbin, rtpbin_pad_name);
  }

  if (pad == NULL) {
    GST_WARNING ("No pad");
    return 0;
  }

  caps = gst_pad_query_caps (pad, NULL);
  g_object_unref (pad);
  if (caps == NULL) {
    GST_WARNING ("No caps");
    return 0;
  }

  GST_DEBUG ("Peer caps: %" GST_PTR_FORMAT, caps);

  for (i = 0; i < gst_caps_get_size (caps); i++) {
    GstStructure *st;

    st = gst_caps_get_structure (caps, 0);
    if (gst_structure_get_uint (st, "ssrc", &ssrc))
      break;
  }

  gst_caps_unref (caps);

  return ssrc;
}

static void
add_bundle_funnels (KmsWebrtcEndPoint * webrtc_end_point)
{
  GST_OBJECT_LOCK (webrtc_end_point);
  if (webrtc_end_point->priv->bundle_funnels_added) {
    GST_OBJECT_UNLOCK (webrtc_end_point);
    return;
  }
  webrtc_end_point->priv->bundle_funnels_added = TRUE;
  GST_OBJECT_UNLOCK (webrtc_end_point);

  gst_bin_add_many (GST_BIN (webrtc_end_point),
      webrtc_end_point->priv->bundle_rtp_funnel,
      webrtc_end_point->priv->bundle_rtcp_funnel, NULL);
  gst_element_sync_state_with_parent (webrtc_end_point->priv->
      bundle_rtp_funnel);
  gst_element_sync_state_with_parent (webrtc_end_point->priv->
      bundle_rtcp_funnel);
}

static const gchar *
update_sdp_media (KmsWebrtcEndPoint * webrtc_end_point, GstSDPMedia * media,
    const gchar * fingerprint, gboolean use_ipv6)
{
  KmsBaseRtpEndPoint *base_rtp_end_point =
      KMS_BASE_RTP_END_POINT (webrtc_end_point);
  gint is_bundle = g_atomic_int_get (&webrtc_end_point->priv->is_bundle);
  const gchar *media_str;
  guint stream_id;
  const gchar *rtpbin_pad_name = NULL;
  NiceAgent *agent = webrtc_end_point->priv->agent;
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

  media_str = gst_sdp_media_get_media (media);

  if (is_bundle) {
    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      rtpbin_pad_name = AUDIO_RTPBIN_SEND_SINK;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      rtpbin_pad_name = VIDEO_RTPBIN_SEND_SINK;
    } else {
      GST_WARNING_OBJECT (webrtc_end_point, "Media \"%s\" not supported",
          media_str);
      return NULL;
    }
    stream_id = webrtc_end_point->priv->bundle_connection->stream_id;
  } else {
    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      stream_id = webrtc_end_point->priv->audio_connection->stream_id;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      stream_id = webrtc_end_point->priv->video_connection->stream_id;
    } else {
      GST_WARNING_OBJECT (webrtc_end_point, "Media \"%s\" not supported",
          media_str);
      return NULL;
    }
  }

  proto_str = gst_sdp_media_get_proto (media);
  if (g_ascii_strcasecmp (SDP_MEDIA_RTP_AVP_PROTO, proto_str) != 0 &&
      g_ascii_strcasecmp (SDP_MEDIA_RTP_SAVPF_PROTO, proto_str)) {
    GST_WARNING ("Proto \"%s\" not supported", proto_str);
    ((GstSDPMedia *) media)->port = 0;
    return NULL;
  }

  gst_sdp_media_set_proto (media, SDP_MEDIA_RTP_SAVPF_PROTO);

  rtp_default_candidate =
      nice_agent_get_default_local_candidate (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (is_bundle) {
    rtcp_default_candidate =
        nice_agent_get_default_local_candidate (agent, stream_id,
        NICE_COMPONENT_TYPE_RTP);
  } else {
    rtcp_default_candidate =
        nice_agent_get_default_local_candidate (agent, stream_id,
        NICE_COMPONENT_TYPE_RTCP);
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
    GST_WARNING ("No valid rtp address type: %s", rtp_addr_type);
    return NULL;
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

  if (fingerprint != NULL)
    gst_sdp_media_add_attribute ((GstSDPMedia *) media, "fingerprint",
        fingerprint);

  /* ICE candidates */
  candidates =
      nice_agent_get_local_candidates (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (is_bundle) {
    gst_sdp_media_add_attribute ((GstSDPMedia *) media, "rtcp-mux", "");
  } else {
    candidates =
        g_slist_concat (candidates,
        nice_agent_get_local_candidates (agent, stream_id,
            NICE_COMPONENT_TYPE_RTCP));
  }

  for (walk = candidates; walk; walk = walk->next) {
    NiceCandidate *cand = walk->data;

    if (nice_address_ip_version (&cand->addr) == IPV6 && !use_ipv6)
      continue;

    str = nice_agent_generate_local_candidate_sdp (agent, cand);
    gst_sdp_media_add_attribute ((GstSDPMedia *) media, SDP_CANDIDATE_ATTR,
        str + SDP_CANDIDATE_ATTR_LEN);
    g_free (str);
  }

  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);

  if (rtpbin_pad_name != NULL) {
    guint ssrc = rtpbin_get_ssrc (base_rtp_end_point->rtpbin, rtpbin_pad_name);

    if (ssrc != 0) {
      gchar *value;
      GstStructure *sdes;

      g_object_get (base_rtp_end_point->rtpbin, "sdes", &sdes, NULL);
      value =
          g_strdup_printf ("%" G_GUINT32_FORMAT " cname:%s", ssrc,
          gst_structure_get_string (sdes, "cname"));
      gst_structure_free (sdes);
      gst_sdp_media_add_attribute (media, "ssrc", value);
      g_free (value);
    }
  }

  return media_str;
}

static gboolean
sdp_message_is_bundle (GstSDPMessage * msg)
{
  gboolean is_bundle = FALSE;
  guint i;

  if (msg == NULL)
    return FALSE;

  for (i = 0;; i++) {
    const gchar *val;
    GRegex *regex;
    GMatchInfo *match_info = NULL;

    val = gst_sdp_message_get_attribute_val_n (msg, "group", i);
    if (val == NULL)
      break;

    regex = g_regex_new ("BUNDLE(?<mids>.*)?", 0, 0, NULL);
    g_regex_match (regex, val, 0, &match_info);
    g_regex_unref (regex);

    if (g_match_info_matches (match_info)) {
      gchar *mids_str = g_match_info_fetch_named (match_info, "mids");
      gchar **mids;

      mids = g_strsplit (mids_str, " ", 0);
      g_free (mids_str);
      is_bundle = g_strv_length (mids) > 0;
      g_strfreev (mids);
      g_match_info_free (match_info);

      break;
    }

    g_match_info_free (match_info);
  }

  return is_bundle;
}

static gboolean
kms_webrtc_end_point_set_transport_to_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * msg)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (base_sdp_end_point);
  gboolean is_bundle = FALSE;
  gchar *fingerprint;
  guint len, i;
  gchar *bundle_mids = NULL;

  if (!nice_agent_gather_candidates (self->priv->agent,
          self->priv->audio_connection->stream_id)) {
    GST_ERROR_OBJECT (self, "Failed to start candidate gathering for %s.",
        AUDIO_STREAM_NAME);
    return FALSE;
  }

  if (!nice_agent_gather_candidates (self->priv->agent,
          self->priv->video_connection->stream_id)) {
    GST_ERROR_OBJECT (self, "Failed to start candidate gathering for %s.",
        VIDEO_STREAM_NAME);
    return FALSE;
  }

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

  is_bundle = sdp_message_is_bundle (base_sdp_end_point->remote_offer_sdp);
  g_atomic_int_set (&self->priv->is_bundle, is_bundle);

  GST_INFO ("BUNDLE: %" G_GUINT32_FORMAT, is_bundle);

  if (is_bundle) {
    self->priv->bundle_connection = self->priv->audio_connection;
    bundle_mids = g_strdup ("BUNDLE");
  }

  fingerprint = kms_webrtc_endpoint_generate_fingerprint_sdp_attr (self);
  len = gst_sdp_message_medias_len (msg);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (msg, i);
    const gchar *media_str;

    media_str = update_sdp_media (self, (GstSDPMedia *) media,
        fingerprint, base_sdp_end_point->use_ipv6);

    if (media_str == NULL)
      continue;

    if (is_bundle) {
      gchar *tmp;

      tmp = g_strconcat (bundle_mids, " ", media_str, NULL);
      g_free (bundle_mids);
      bundle_mids = tmp;
    }
  }

  if (is_bundle) {
    gst_sdp_message_add_attribute (msg, "group", bundle_mids);
    g_free (bundle_mids);
  }

  if (fingerprint != NULL)
    g_free (fingerprint);

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
  rtp_sink_name = AUDIO_RTPBIN_RECV_RTP_SINK;   /* audio by default */
  rtcp_sink_name = AUDIO_RTPBIN_RECV_RTCP_SINK; /* audio by default */
  stream_name = nice_agent_get_stream_name (conn->agent, conn->stream_id);
  if (g_strcmp0 (VIDEO_STREAM_NAME, stream_name) == 0) {
    rtp_sink_name = VIDEO_RTPBIN_RECV_RTP_SINK;
    rtcp_sink_name = VIDEO_RTPBIN_RECV_RTCP_SINK;
  }

  add_webrtc_transport_src (webrtc_end_point, conn->rtp_transport, is_client,
      rtp_sink_name);
  add_webrtc_transport_src (webrtc_end_point, conn->rtcp_transport, is_client,
      rtcp_sink_name);
}

static void
rtp_ssrc_demux_new_ssrc_pad (GstElement * ssrcdemux, guint ssrc, GstPad * pad,
    KmsWebrtcEndPoint * webrtc_end_point)
{
  KmsBaseRtpEndPoint *base_rtp_end_point =
      KMS_BASE_RTP_END_POINT (webrtc_end_point);
  gchar *rtcp_pad_name;

  GST_DEBUG ("pad: %" GST_PTR_FORMAT " ssrc: %" G_GUINT32_FORMAT, pad, ssrc);
  rtcp_pad_name = g_strconcat ("rtcp_", GST_OBJECT_NAME (pad), NULL);

  if (g_str_has_suffix (GST_OBJECT_NAME (pad),
          webrtc_end_point->priv->remote_audio_ssrc)) {
    gst_element_link_pads (ssrcdemux, GST_OBJECT_NAME (pad),
        base_rtp_end_point->rtpbin, AUDIO_RTPBIN_RECV_RTP_SINK);
    gst_element_link_pads (ssrcdemux, rtcp_pad_name,
        base_rtp_end_point->rtpbin, AUDIO_RTPBIN_RECV_RTCP_SINK);
  } else if (g_str_has_suffix (GST_OBJECT_NAME (pad),
          webrtc_end_point->priv->remote_video_ssrc)) {
    gst_element_link_pads (ssrcdemux, GST_OBJECT_NAME (pad),
        base_rtp_end_point->rtpbin, VIDEO_RTPBIN_RECV_RTP_SINK);
    gst_element_link_pads (ssrcdemux, rtcp_pad_name,
        base_rtp_end_point->rtpbin, VIDEO_RTPBIN_RECV_RTCP_SINK);
  }

  g_free (rtcp_pad_name);
}

static void
add_webrtc_bundle_connection_src (KmsWebrtcEndPoint * webrtc_end_point,
    gboolean is_client)
{
  KmsWebRTCTransport *tr =
      webrtc_end_point->priv->bundle_connection->rtp_transport;
  GstElement *ssrcdemux = gst_element_factory_make ("rtpssrcdemux", NULL);

  g_signal_connect (ssrcdemux, "new-ssrc-pad",
      G_CALLBACK (rtp_ssrc_demux_new_ssrc_pad), webrtc_end_point);
  gst_bin_add (GST_BIN (webrtc_end_point), ssrcdemux);
  gst_element_sync_state_with_parent (ssrcdemux);

  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", is_client, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", is_client, NULL);
  gst_bin_add_many (GST_BIN (webrtc_end_point),
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);
  gst_element_sync_state_with_parent (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent (tr->nicesrc);
  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);

  gst_element_link_pads (tr->dtlssrtpdec, "src", ssrcdemux, "sink");
  /* TODO: link rtcp_sink pad when dtlssrtpdec provides rtcp packets */
}

static void
add_webrtc_bundle_connection_sink (KmsWebrtcEndPoint * webrtc_end_point)
{
  KmsWebRTCTransport *tr =
      webrtc_end_point->priv->bundle_connection->rtp_transport;

  gst_bin_add_many (GST_BIN (webrtc_end_point),
      g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);
  gst_element_sync_state_with_parent (tr->dtlssrtpenc);
  gst_element_sync_state_with_parent (tr->nicesink);
  gst_element_link (tr->dtlssrtpenc, tr->nicesink);

  add_bundle_funnels (webrtc_end_point);

  gst_element_link_pads (webrtc_end_point->priv->bundle_rtp_funnel, NULL,
      tr->dtlssrtpenc, "rtp_sink");
  gst_element_link_pads (webrtc_end_point->priv->bundle_rtcp_funnel, NULL,
      tr->dtlssrtpenc, "rtcp_sink");
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
      " (?<port>[0-9]+) typ (?<type>(host|srflx|prflx|relay))"
      "( raddr [0-9.:a-zA-Z]+ rport [0-9]+)?( generation [0-9]+)?$",
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

static gchar *
sdp_media_get_ssrc (const GstSDPMedia * media)
{
  gchar *ssrc = NULL;
  const gchar *val;
  GRegex *regex;
  GMatchInfo *match_info = NULL;

  val = gst_sdp_media_get_attribute_val (media, "ssrc");

  if (val == NULL)
    return NULL;

  regex = g_regex_new ("^(?<ssrc>[0-9]+)(.*)?$", 0, 0, NULL);
  g_regex_match (regex, val, 0, &match_info);
  g_regex_unref (regex);

  if (g_match_info_matches (match_info)) {
    ssrc = g_match_info_fetch_named (match_info, "ssrc");
  }
  g_match_info_free (match_info);

  return ssrc;
}

static void
kms_webrtc_end_point_start_transport_send (KmsBaseSdpEndPoint *
    base_sdp_end_point, const GstSDPMessage * offer,
    const GstSDPMessage * answer, gboolean local_offer)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (base_sdp_end_point);
  gint is_bundle = g_atomic_int_get (&self->priv->is_bundle);
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
    gchar *ssrc;

    ssrc = sdp_media_get_ssrc (media);
    media_str = gst_sdp_media_get_media (media);
    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      conn = self->priv->audio_connection;
      self->priv->remote_audio_ssrc = ssrc;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      conn = self->priv->video_connection;
      self->priv->remote_video_ssrc = ssrc;
    } else {
      GST_WARNING_OBJECT (self, "Media \"%s\" not supported", media_str);
      continue;
    }

    if (is_bundle) {
      process_sdp_media (media, self->priv->agent,
          self->priv->bundle_connection->stream_id, ufrag, pwd);
    } else {
      process_sdp_media (media, self->priv->agent, conn->stream_id, ufrag, pwd);
      add_webrtc_connection_src (self, conn, !local_offer);
    }
  }

  if (is_bundle) {
    add_webrtc_bundle_connection_src (self, !local_offer);
    add_webrtc_bundle_connection_sink (self);
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
add_webrtc_transport_sink (KmsWebrtcEndPoint * webrtc_end_point,
    KmsWebRTCTransport * tr, const gchar * src_pad_name,
    const gchar * sink_pad_name)
{
  KmsBaseRtpEndPoint *base_rtp_end_point =
      KMS_BASE_RTP_END_POINT (webrtc_end_point);

  gst_bin_add_many (GST_BIN (webrtc_end_point),
      g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);

  gst_element_link (tr->dtlssrtpenc, tr->nicesink);
  gst_element_link_pads (base_rtp_end_point->rtpbin,
      src_pad_name, tr->dtlssrtpenc, sink_pad_name);

  gst_element_sync_state_with_parent (tr->dtlssrtpenc);
  gst_element_sync_state_with_parent (tr->nicesink);
}

static gboolean
connect_rtcp (ConnectRtcpData * data)
{
  add_webrtc_transport_sink (data->webrtc_end_point, data->tr,
      data->src_pad_name, "rtcp_sink");

  return FALSE;
}

static void
add_webrtc_connection_sink (KmsWebrtcEndPoint * webrtc_end_point,
    KmsWebRTCConnection * conn)
{
  const gchar *stream_name;
  const gchar *rtp_src_name, *rtcp_src_name;
  ConnectRtcpData *data;

  /* FIXME: improve this */
  rtp_src_name = AUDIO_RTPBIN_SEND_RTP_SINK;    /* audio by default */
  rtcp_src_name = AUDIO_RTPBIN_SEND_RTCP_SINK;  /* audio by default */
  stream_name = nice_agent_get_stream_name (conn->agent, conn->stream_id);
  if (g_strcmp0 (VIDEO_STREAM_NAME, stream_name) == 0) {
    rtp_src_name = VIDEO_RTPBIN_SEND_RTP_SINK;
    rtcp_src_name = VIDEO_RTPBIN_SEND_RTCP_SINK;
  }

  data = g_slice_new0 (ConnectRtcpData);
  data->webrtc_end_point = webrtc_end_point;
  data->tr = conn->rtcp_transport;
  data->src_pad_name = rtcp_src_name;

  add_webrtc_transport_sink (webrtc_end_point, conn->rtp_transport,
      rtp_src_name, "rtp_sink");
  g_idle_add_full (G_PRIORITY_DEFAULT, (GSourceFunc) (connect_rtcp), data,
      connect_rtcp_data_destroy);
}

static gboolean
connect_bundle_rtcp_funnel (KmsWebrtcEndPoint * webrtc_end_point)
{
  KmsBaseRtpEndPoint *base_rtp_end_point =
      KMS_BASE_RTP_END_POINT (webrtc_end_point);

  gst_element_link_pads (base_rtp_end_point->rtpbin,
      "send_rtcp_src_%u", webrtc_end_point->priv->bundle_rtcp_funnel,
      "sink_%u");

  return FALSE;
}

static void
rtpbin_pad_added (GstElement * rtpbin, GstPad * pad,
    KmsWebrtcEndPoint * webrtc_end_point)
{
  if (g_atomic_int_get (&webrtc_end_point->priv->is_bundle)) {
    add_bundle_funnels (webrtc_end_point);

    if (g_strcmp0 (GST_OBJECT_NAME (pad), AUDIO_RTPBIN_SEND_RTP_SINK) == 0) {
      gst_element_link_pads (rtpbin, AUDIO_RTPBIN_SEND_RTP_SINK,
          webrtc_end_point->priv->bundle_rtp_funnel, "sink_%u");
      g_idle_add_full (G_PRIORITY_DEFAULT,
          (GSourceFunc) (connect_bundle_rtcp_funnel),
          g_object_ref (webrtc_end_point), g_object_unref);
    } else if (g_strcmp0 (GST_OBJECT_NAME (pad),
            VIDEO_RTPBIN_SEND_RTP_SINK) == 0) {
      gst_element_link_pads (rtpbin, VIDEO_RTPBIN_SEND_RTP_SINK,
          webrtc_end_point->priv->bundle_rtp_funnel, "sink_%u");
      g_idle_add_full (G_PRIORITY_DEFAULT,
          (GSourceFunc) (connect_bundle_rtcp_funnel),
          g_object_ref (webrtc_end_point), g_object_unref);
    }
  } else {
    KmsWebRTCConnection *conn = NULL;

    if (g_strcmp0 (GST_OBJECT_NAME (pad), AUDIO_RTPBIN_SEND_RTP_SINK) == 0) {
      conn = webrtc_end_point->priv->audio_connection;
    } else if (g_strcmp0 (GST_OBJECT_NAME (pad),
            VIDEO_RTPBIN_SEND_RTP_SINK) == 0) {
      conn = webrtc_end_point->priv->video_connection;
    }

    if (conn != NULL) {
      add_webrtc_connection_sink (webrtc_end_point, conn);
    }
  }
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
      g_object_set_property (G_OBJECT (self->priv->audio_connection->
              rtp_transport->dtlssrtpdec), "certificate-pem-file", value);
      g_object_set_property (G_OBJECT (self->priv->audio_connection->
              rtcp_transport->dtlssrtpdec), "certificate-pem-file", value);
      g_object_set_property (G_OBJECT (self->priv->video_connection->
              rtp_transport->dtlssrtpdec), "certificate-pem-file", value);
      g_object_set_property (G_OBJECT (self->priv->video_connection->
              rtcp_transport->dtlssrtpdec), "certificate-pem-file", value);
      break;
    case PROP_STUN_SERVER_IP:
      if (self->priv->agent == NULL) {
        GST_ERROR_OBJECT (self, "ICE agent not initialized.");
        break;
      }

      g_object_set_property (G_OBJECT (self->priv->agent), "stun-server",
          value);
      break;
    case PROP_STUN_SERVER_PORT:
      if (self->priv->agent == NULL) {
        GST_ERROR_OBJECT (self, "ICE agent not initialized.");
        break;
      }

      g_object_set_property (G_OBJECT (self->priv->agent), "stun-server-port",
          value);
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
    case PROP_STUN_SERVER_IP:
      if (self->priv->agent == NULL) {
        GST_ERROR_OBJECT (self, "ICE agent not initialized.");
        break;
      }

      g_object_get_property (G_OBJECT (self->priv->agent), "stun-server",
          value);
      break;
    case PROP_STUN_SERVER_PORT:
      if (self->priv->agent == NULL) {
        GST_ERROR_OBJECT (self, "ICE agent not initialized.");
        break;
      }

      g_object_get_property (G_OBJECT (self->priv->agent), "stun-server-port",
          value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_webrtc_end_point_dispose (GObject * object)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (object);

  if (self->priv->remote_audio_ssrc != NULL)
    g_free (self->priv->remote_audio_ssrc);
  if (self->priv->remote_video_ssrc != NULL)
    g_free (self->priv->remote_video_ssrc);
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
    if (g_thread_self () != self->priv->thread)
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

  if (self->priv->tmp_dir != NULL) {
    remove_recursive (self->priv->tmp_dir);
    g_free (self->priv->tmp_dir);
  }

  if (self->priv->certificate_pem_file != NULL) {
    g_free (self->priv->certificate_pem_file);
  }

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
  gobject_class->dispose = kms_webrtc_end_point_dispose;
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

  g_object_class_install_property (gobject_class, PROP_STUN_SERVER_IP,
      g_param_spec_string ("stun-server",
          "StunServer",
          "Stun Server IP Address",
          NULL, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_STUN_SERVER_PORT,
      g_param_spec_uint ("stun-server-port",
          "StunServerPort",
          "Stun Server Port",
          1, G_MAXUINT16, 3478, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndPointPrivate));

  g_mkdir_with_parents (ROOT_TMP_DIR, FILE_PERMISIONS);
}

static void
kms_webrtc_end_point_init (KmsWebrtcEndPoint * self)
{
  KmsBaseRtpEndPoint *base_rtp_end_point = KMS_BASE_RTP_END_POINT (self);
  gchar t[] = TMP_DIR_TEMPLATE;

  self->priv = KMS_WEBRTC_END_POINT_GET_PRIVATE (self);

  self->priv->tmp_dir = g_strdup (g_mkdtemp_full (t, FILE_PERMISIONS));

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

  self->priv->bundle_rtp_funnel = gst_element_factory_make ("funnel", NULL);
  self->priv->bundle_rtcp_funnel = gst_element_factory_make ("funnel", NULL);

  g_signal_connect (base_rtp_end_point->rtpbin, "pad-added",
      G_CALLBACK (rtpbin_pad_added), self);
}

gboolean
kms_webrtc_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_END_POINT);
}
