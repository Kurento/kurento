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
#include <commons/kmsloop.h>
#include <commons/kmsutils.h>
#include <commons/sdp_utils.h>

#include <nice/nice.h>
#include <gio/gio.h>
#include <stdlib.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

#include <gst/rtp/gstrtcpbuffer.h>

#define PLUGIN_NAME "webrtcendpoint"

#define GST_CAT_DEFAULT kms_webrtc_endpoint_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcEndpoint, kms_webrtc_endpoint,
    KMS_TYPE_BASE_RTP_ENDPOINT);

#define KMS_WEBRTC_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_ENDPOINT,                   \
    KmsWebrtcEndpointPrivate                    \
  )                                             \
)

enum
{
  PROP_0,
  PROP_CERTIFICATE_PEM_FILE,
  PROP_STUN_SERVER_IP,
  PROP_STUN_SERVER_PORT,
  PROP_TURN_URL,                /* user:password@address:port?transport=[udp|tcp|tls] */
  N_PROPERTIES
};

#define RTCP_DEMUX_PEER "rtcp-demux-peer"

#define IPV4 4
#define IPV6 6

#define KMS_NICE_N_COMPONENTS 2

#define SDP_ICE_UFRAG_ATTR "ice-ufrag"
#define SDP_ICE_PWD_ATTR "ice-pwd"
#define SDP_CANDIDATE_ATTR "candidate"
#define SDP_CANDIDATE_ATTR_LEN 12

#define FINGERPRINT_CHECKSUM G_CHECKSUM_SHA256

#define FILE_PERMISIONS (S_IRWXU)
#define TMP_DIR_TEMPLATE "/tmp/kms_webrtc_endpoint_XXXXXX"
#define CERTTOOL_TEMPLATE "certtool.tmpl"
#define CERT_KEY_PEM_FILE "certkey.pem"

#define WEBRTC_ENDPOINT "webrtc-endpoint"

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

typedef struct _GatherContext
{
  GMutex gather_mutex;
  GCond gather_cond;
  gboolean wait_gathering;
  gboolean ice_gathering_done;
  gboolean finalized;
} GatherContext;

struct _KmsWebrtcEndpointPrivate
{
  GatherContext ctx;
  gchar *tmp_dir;

  KmsLoop *loop;

  NiceAgent *agent;

  KmsWebRTCConnection *bundle_connection;       /* Uses audio_connection */
  GstElement *bundle_rtp_funnel;
  GstElement *bundle_rtcp_funnel;
  gboolean bundle_funnels_added;

  guint remote_audio_ssrc;
  KmsWebRTCConnection *audio_connection;
  gboolean audio_ice_gathering_done;

  guint remote_video_ssrc;
  KmsWebRTCConnection *video_connection;
  gboolean video_ice_gathering_done;

  gchar *certificate_pem_file;

  gchar *turn_url;
  gchar *turn_user;
  gchar *turn_password;
  gchar *turn_address;
  guint turn_port;
  NiceRelayType turn_transport;
};

/* KmsWebRTCTransport */

static void
kms_webrtc_transport_destroy (KmsWebRTCTransport * tr)
{
  if (tr == NULL)
    return;

  g_clear_object (&tr->dtlssrtpenc);
  g_clear_object (&tr->dtlssrtpdec);
  g_clear_object (&tr->nicesink);
  g_clear_object (&tr->nicesrc);

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
      "component", component_id, "sync", FALSE, "async", FALSE, NULL);
  g_object_set (G_OBJECT (tr->nicesrc), "agent", agent, "stream", stream_id,
      "component", component_id, NULL);

  return tr;
}

/* KmsWebRTCTransport */

/* WebRTCConnection */

static void
kms_webrtc_connection_add_remote_candidates (KmsWebRTCConnection * conn,
    const GstSDPMedia * media, const gchar * msg_ufrag, const gchar * msg_pwd)
{
  NiceAgent *agent = conn->agent;
  guint stream_id = conn->stream_id;
  const gchar *ufrag, *pwd;
  GRegex *regex;
  guint len, i;

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
    if (g_strcmp0 (SDP_CANDIDATE_ATTR, attr->key) != 0) {
      continue;
    }

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

      if (cand != NULL) {
        nice_candidate_free (cand);
      }

      g_match_info_next (match_info, NULL);
    }

    g_match_info_free (match_info);
  }

  g_regex_unref (regex);
}

static void
kms_nice_agent_recv_cb (NiceAgent * agent, guint stream_id, guint component_id,
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

  if (conn->agent != NULL) {
    nice_agent_remove_stream (conn->agent, conn->stream_id);
    g_clear_object (&conn->agent);
  }

  g_slice_free (KmsWebRTCConnection, conn);
}

static void
kms_webrtc_connection_set_relay_info (KmsWebRTCConnection * conn,
    KmsWebrtcEndpoint * we)
{
  if (we->priv->turn_address == NULL) {
    return;
  }

  nice_agent_set_relay_info (we->priv->agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTP, we->priv->turn_address, we->priv->turn_port,
      we->priv->turn_user, we->priv->turn_password, we->priv->turn_transport);
  nice_agent_set_relay_info (we->priv->agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTCP, we->priv->turn_address, we->priv->turn_port,
      we->priv->turn_user, we->priv->turn_password, we->priv->turn_transport);
}

static KmsWebRTCConnection *
kms_webrtc_connection_create (NiceAgent * agent, GMainContext * context,
    const gchar * name)
{
  KmsWebRTCConnection *conn;

  conn = g_slice_new0 (KmsWebRTCConnection);

  conn->agent = g_object_ref (agent);
  conn->stream_id = nice_agent_add_stream (agent, KMS_NICE_N_COMPONENTS);
  if (conn->stream_id == 0) {
    GST_ERROR ("Cannot add nice stream for %s.", name);
    kms_webrtc_connection_destroy (conn);
    return NULL;
  }

  nice_agent_set_stream_name (agent, conn->stream_id, name);
  nice_agent_attach_recv (agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTP, context, kms_nice_agent_recv_cb, NULL);
  nice_agent_attach_recv (agent, conn->stream_id,
      NICE_COMPONENT_TYPE_RTCP, context, kms_nice_agent_recv_cb, NULL);

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
generate_fingerprint (KmsWebrtcEndpoint * webrtc_endpoint)
{
  GTlsCertificate *cert;
  GError *error = NULL;
  GByteArray *ba;
  gssize length;
  int size;
  gchar *fingerprint;
  gchar *fingerprint_colon = NULL;
  int i, j;

  cert =
      g_tls_certificate_new_from_file (webrtc_endpoint->
      priv->certificate_pem_file, &error);
  if (cert == NULL) {
    if (error != NULL) {
      GST_ELEMENT_ERROR (webrtc_endpoint, RESOURCE, OPEN_READ,
          ("Cannot get certificate (%s)", error->message),
          ("Cannot get certificate (%s)", error->message));
    } else {
      GST_ELEMENT_ERROR (webrtc_endpoint, RESOURCE, OPEN_READ,
          ("Cannot get certificate"), ("Cannot get certificate"));
    }

    goto end;
  }

  g_object_get (cert, "certificate", &ba, NULL);
  fingerprint =
      g_compute_checksum_for_data (FINGERPRINT_CHECKSUM, ba->data, ba->len);
  g_object_unref (cert);
  g_byte_array_unref (ba);

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

end:
  g_clear_error (&error);

  return fingerprint_colon;
}

static gchar *
kms_webrtc_endpoint_generate_fingerprint_sdp_attr (KmsWebrtcEndpoint * self)
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

  fp = generate_fingerprint (self);
  if (fp == NULL) {
    return NULL;
  }

  ret = g_strconcat ("sha-256 ", fp, NULL);
  g_free (fp);

  return ret;
}

static void
add_bundle_funnels (KmsWebrtcEndpoint * webrtc_endpoint)
{
  if (webrtc_endpoint->priv->bundle_funnels_added) {
    return;
  }

  webrtc_endpoint->priv->bundle_rtp_funnel =
      gst_element_factory_make ("funnel", NULL);
  webrtc_endpoint->priv->bundle_rtcp_funnel =
      gst_element_factory_make ("funnel", NULL);

  webrtc_endpoint->priv->bundle_funnels_added = TRUE;

  gst_bin_add_many (GST_BIN (webrtc_endpoint),
      webrtc_endpoint->priv->bundle_rtp_funnel,
      webrtc_endpoint->priv->bundle_rtcp_funnel, NULL);
  gst_element_sync_state_with_parent_target_state (webrtc_endpoint->priv->
      bundle_rtp_funnel);
  gst_element_sync_state_with_parent_target_state (webrtc_endpoint->priv->
      bundle_rtcp_funnel);
}

/* Set Transport begin */
static gboolean
sdp_media_set_ice_info (KmsWebrtcEndpoint * webrtc_endpoint,
    GstSDPMedia * media, gboolean bundle, const gchar * fingerprint)
{
  NiceAgent *agent = webrtc_endpoint->priv->agent;
  guint stream_id;
  gchar *ufrag, *pwd;

  if (bundle) {
    stream_id = webrtc_endpoint->priv->bundle_connection->stream_id;
  } else {
    const gchar *media_str = gst_sdp_media_get_media (media);

    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      stream_id = webrtc_endpoint->priv->audio_connection->stream_id;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      stream_id = webrtc_endpoint->priv->video_connection->stream_id;
    } else {
      GST_WARNING_OBJECT (webrtc_endpoint, "Media \"%s\" not supported",
          media_str);
      return FALSE;
    }
  }

  /* ICE credentials */
  nice_agent_get_local_credentials (agent, stream_id, &ufrag, &pwd);
  gst_sdp_media_add_attribute (media, SDP_ICE_UFRAG_ATTR, ufrag);
  g_free (ufrag);
  gst_sdp_media_add_attribute (media, SDP_ICE_PWD_ATTR, pwd);
  g_free (pwd);

  /* Crypto info */
  return gst_sdp_media_add_attribute (media, "fingerprint",
      fingerprint) == GST_SDP_OK;
}

static gboolean
kms_webrtc_endpoint_set_ice_info (KmsWebrtcEndpoint * self, GstSDPMessage * msg,
    gboolean bundle, const char *fingerprint)
{
  KmsBaseSdpEndpoint *base_sdp_endpoint = KMS_BASE_SDP_ENDPOINT (self);
  guint len, i;

  len = gst_sdp_message_medias_len (msg);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (msg, i);
    gboolean use_ipv6;

    g_object_get (base_sdp_endpoint, "use-ipv6", &use_ipv6, NULL);
    if (!sdp_media_set_ice_info (self, (GstSDPMedia *) media, bundle,
            fingerprint)) {
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
kms_webrtc_endpoint_sdp_media_set_ice_candidates (KmsWebrtcEndpoint * self,
    GstSDPMedia * media, gboolean bundle, gboolean use_ipv6)
{
  NiceAgent *agent = self->priv->agent;
  guint stream_id;
  GSList *candidates;
  GSList *walk;
  NiceCandidate *rtp_default_candidate, *rtcp_default_candidate;
  gchar rtp_addr[NICE_ADDRESS_STRING_LEN + 1];
  gchar rtcp_addr[NICE_ADDRESS_STRING_LEN + 1];
  const gchar *rtp_addr_type, *rtcp_addr_type;
  gboolean rtp_is_ipv6, rtcp_is_ipv6;
  guint rtp_port, rtcp_port;
  guint conn_len, c;
  gchar *str;
  guint attr_len, i;

  if (bundle) {
    stream_id = self->priv->bundle_connection->stream_id;
  } else {
    const gchar *media_str = gst_sdp_media_get_media (media);

    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      stream_id = self->priv->audio_connection->stream_id;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      stream_id = self->priv->video_connection->stream_id;
    } else {
      GST_WARNING_OBJECT (self, "Media \"%s\" not supported", media_str);
      return FALSE;
    }
  }

  rtp_default_candidate =
      nice_agent_get_default_local_candidate (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (bundle) {
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
      gst_sdp_attribute_set ((GstSDPAttribute *) attr, "rtcp", str);
      g_free (str);
    }
  }

  /* ICE candidates */
  candidates =
      nice_agent_get_local_candidates (agent, stream_id,
      NICE_COMPONENT_TYPE_RTP);

  if (!bundle) {
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
    gst_sdp_media_add_attribute (media, SDP_CANDIDATE_ATTR,
        str + SDP_CANDIDATE_ATTR_LEN);
    g_free (str);
  }

  g_slist_free_full (candidates, (GDestroyNotify) nice_candidate_free);

  return TRUE;
}

static gboolean
kms_webrtc_endpoint_set_ice_candidates (KmsWebrtcEndpoint * self,
    GstSDPMessage * msg, gboolean bundle)
{
  KmsBaseSdpEndpoint *base_sdp_endpoint = KMS_BASE_SDP_ENDPOINT (self);
  guint len, i;

  kms_webrtc_connection_set_relay_info (self->priv->audio_connection, self);
  if (!nice_agent_gather_candidates (self->priv->agent,
          self->priv->audio_connection->stream_id)) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ERROR_OBJECT (self, "Failed to start candidate gathering for %s.",
        AUDIO_STREAM_NAME);
    return FALSE;
  }

  kms_webrtc_connection_set_relay_info (self->priv->video_connection, self);
  if (!nice_agent_gather_candidates (self->priv->agent,
          self->priv->video_connection->stream_id)) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ERROR_OBJECT (self, "Failed to start candidate gathering for %s.",
        VIDEO_STREAM_NAME);
    return FALSE;
  }

  /* Wait for ICE candidates begin */
  g_mutex_lock (&self->priv->ctx.gather_mutex);
  self->priv->ctx.wait_gathering = TRUE;
  while (!self->priv->ctx.finalized && !self->priv->ctx.ice_gathering_done)
    g_cond_wait (&self->priv->ctx.gather_cond, &self->priv->ctx.gather_mutex);
  self->priv->ctx.wait_gathering = FALSE;
  g_cond_broadcast (&self->priv->ctx.gather_cond);

  if (self->priv->ctx.finalized) {
    GST_ERROR_OBJECT (self, "WebrtcEndpoint has finalized.");
    g_mutex_unlock (&self->priv->ctx.gather_mutex);
    return FALSE;
  }
  g_mutex_unlock (&self->priv->ctx.gather_mutex);
  /* Wait for ICE candidates end */

  len = gst_sdp_message_medias_len (msg);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (msg, i);
    gboolean use_ipv6;

    g_object_get (base_sdp_endpoint, "use-ipv6", &use_ipv6, NULL);
    if (!kms_webrtc_endpoint_sdp_media_set_ice_candidates (self,
            (GstSDPMedia *) media, bundle, use_ipv6)) {
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
kms_webrtc_endpoint_set_transport_to_sdp (KmsBaseSdpEndpoint *
    base_sdp_endpoint, GstSDPMessage * msg)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (base_sdp_endpoint);
  gchar *fingerprint = NULL;
  gboolean bundle;
  gboolean ret = TRUE;

  KMS_ELEMENT_LOCK (self);

  /* Chain up */
  ret = KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->set_transport_to_sdp
      (base_sdp_endpoint, msg);

  if (ret == FALSE) {
    goto end;
  }

  fingerprint = kms_webrtc_endpoint_generate_fingerprint_sdp_attr (self);
  if (fingerprint == NULL) {
    ret = FALSE;
    goto end;
  }

  g_object_get (self, "bundle", &bundle, NULL);
  if (bundle) {
    self->priv->bundle_connection = self->priv->audio_connection;
  }

  ret = kms_webrtc_endpoint_set_ice_info (self, msg, bundle, fingerprint);
  if (ret == FALSE) {
    goto end;
  }

  ret = kms_webrtc_endpoint_set_ice_candidates (self, msg, bundle);

end:
  KMS_ELEMENT_UNLOCK (self);

  g_free (fingerprint);

  return ret;
}

/* Set Transport end */

/* Start Transport begin */

static void
add_webrtc_transport_src (KmsWebrtcEndpoint * webrtc_endpoint,
    KmsWebRTCTransport * tr, gboolean is_client, const gchar * sink_pad_name)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint =
      KMS_BASE_RTP_ENDPOINT (webrtc_endpoint);

  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", is_client, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", is_client, NULL);

  gst_bin_add_many (GST_BIN (webrtc_endpoint),
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);

  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);
  gst_element_link_pads (tr->dtlssrtpdec, "src",
      kms_base_rtp_endpoint_get_rtpbin (base_rtp_endpoint), sink_pad_name);

  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent_target_state (tr->nicesrc);
}

static void
add_webrtc_connection_src (KmsWebrtcEndpoint * webrtc_endpoint,
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

  add_webrtc_transport_src (webrtc_endpoint, conn->rtp_transport, is_client,
      rtp_sink_name);
  add_webrtc_transport_src (webrtc_endpoint, conn->rtcp_transport, is_client,
      rtcp_sink_name);
}

static gboolean
ssrcs_are_mapped (KmsWebrtcEndpoint * webrtc_endpoint, GstElement * ssrcdemux,
    guint32 local_ssrc, guint32 remote_ssrc)
{
  GstElement *rtcpdemux =
      g_object_get_data (G_OBJECT (ssrcdemux), RTCP_DEMUX_PEER);
  guint local_ssrc_pair;

  g_signal_emit_by_name (rtcpdemux, "get-local-rr-ssrc-pair", remote_ssrc,
      &local_ssrc_pair);

  return ((local_ssrc != 0) && (local_ssrc_pair == local_ssrc));
}

static void
rtp_ssrc_demux_new_ssrc_pad (GstElement * ssrcdemux, guint ssrc, GstPad * pad,
    KmsWebrtcEndpoint * webrtc_endpoint)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint =
      KMS_BASE_RTP_ENDPOINT (webrtc_endpoint);
  gchar *rtcp_pad_name;
  GstElement *rtpbin;

  GST_DEBUG ("pad: %" GST_PTR_FORMAT " ssrc: %" G_GUINT32_FORMAT, pad, ssrc);
  rtcp_pad_name = g_strconcat ("rtcp_", GST_OBJECT_NAME (pad), NULL);

  KMS_ELEMENT_LOCK (base_rtp_endpoint);
  rtpbin = kms_base_rtp_endpoint_get_rtpbin (base_rtp_endpoint);
  if ((webrtc_endpoint->priv->remote_audio_ssrc == ssrc) ||
      ssrcs_are_mapped (webrtc_endpoint, ssrcdemux,
          kms_base_rtp_endpoint_get_local_audio_ssrc (base_rtp_endpoint),
          ssrc)) {
    gst_element_link_pads (ssrcdemux, GST_OBJECT_NAME (pad), rtpbin,
        AUDIO_RTPBIN_RECV_RTP_SINK);
    gst_element_link_pads (ssrcdemux, rtcp_pad_name, rtpbin,
        AUDIO_RTPBIN_RECV_RTCP_SINK);
  } else if (webrtc_endpoint->priv->remote_video_ssrc == ssrc
      || ssrcs_are_mapped (webrtc_endpoint, ssrcdemux,
          kms_base_rtp_endpoint_get_local_video_ssrc (base_rtp_endpoint),
          ssrc)) {
    gst_element_link_pads (ssrcdemux, GST_OBJECT_NAME (pad), rtpbin,
        VIDEO_RTPBIN_RECV_RTP_SINK);
    gst_element_link_pads (ssrcdemux, rtcp_pad_name, rtpbin,
        VIDEO_RTPBIN_RECV_RTCP_SINK);
  }

  KMS_ELEMENT_UNLOCK (base_rtp_endpoint);

  g_free (rtcp_pad_name);
}

static void
add_webrtc_transport_sink (KmsWebrtcEndpoint * webrtc_endpoint,
    KmsWebRTCTransport * tr)
{
  gst_bin_add_many (GST_BIN (webrtc_endpoint),
      g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);

  gst_element_link (tr->dtlssrtpenc, tr->nicesink);
  gst_element_sync_state_with_parent_target_state (tr->nicesink);
  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpenc);
}

static void
add_webrtc_connection_sink (KmsWebrtcEndpoint * webrtc_endpoint,
    KmsWebRTCConnection * conn)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint =
      KMS_BASE_RTP_ENDPOINT (webrtc_endpoint);
  const gchar *stream_name, *rtcp_src_pad_name;

  /* FIXME: improve this */
  rtcp_src_pad_name = AUDIO_RTPBIN_SEND_RTCP_SRC;       /* audio by default */
  stream_name = nice_agent_get_stream_name (conn->agent, conn->stream_id);
  if (g_strcmp0 (VIDEO_STREAM_NAME, stream_name) == 0) {
    rtcp_src_pad_name = VIDEO_RTPBIN_SEND_RTCP_SRC;
  }
  add_webrtc_transport_sink (webrtc_endpoint, conn->rtp_transport);
  add_webrtc_transport_sink (webrtc_endpoint, conn->rtcp_transport);

  gst_element_link_pads (kms_base_rtp_endpoint_get_rtpbin (base_rtp_endpoint),
      rtcp_src_pad_name, conn->rtcp_transport->dtlssrtpenc, "rtcp_sink");
}

static void
add_webrtc_bundle_connection_src (KmsWebrtcEndpoint * webrtc_endpoint,
    gboolean is_client)
{
  KmsWebRTCTransport *tr =
      webrtc_endpoint->priv->bundle_connection->rtp_transport;
  GstElement *ssrcdemux = gst_element_factory_make ("rtpssrcdemux", NULL);
  GstElement *rtcpdemux = gst_element_factory_make ("rtcpdemux", NULL);

  g_object_set_data_full (G_OBJECT (ssrcdemux), RTCP_DEMUX_PEER,
      g_object_ref (rtcpdemux), g_object_unref);

  g_signal_connect (ssrcdemux, "new-ssrc-pad",
      G_CALLBACK (rtp_ssrc_demux_new_ssrc_pad), webrtc_endpoint);
  gst_bin_add_many (GST_BIN (webrtc_endpoint), ssrcdemux, rtcpdemux, NULL);

  g_object_set (G_OBJECT (tr->dtlssrtpenc), "is-client", is_client, NULL);
  g_object_set (G_OBJECT (tr->dtlssrtpdec), "is-client", is_client, NULL);
  gst_bin_add_many (GST_BIN (webrtc_endpoint),
      g_object_ref (tr->nicesrc), g_object_ref (tr->dtlssrtpdec), NULL);
  gst_element_link (tr->nicesrc, tr->dtlssrtpdec);

  gst_element_link_pads (tr->dtlssrtpdec, "src", rtcpdemux, "sink");
  gst_element_link_pads (rtcpdemux, "rtp_src", ssrcdemux, "sink");
  gst_element_link_pads (rtcpdemux, "rtcp_src", ssrcdemux, "rtcp_sink");

  gst_element_sync_state_with_parent_target_state (ssrcdemux);
  gst_element_sync_state_with_parent_target_state (rtcpdemux);
  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpdec);
  gst_element_sync_state_with_parent_target_state (tr->nicesrc);
}

static void
add_webrtc_bundle_connection_sink (KmsWebrtcEndpoint * webrtc_endpoint)
{
  KmsWebRTCTransport *tr =
      webrtc_endpoint->priv->bundle_connection->rtp_transport;

  gst_bin_add_many (GST_BIN (webrtc_endpoint),
      g_object_ref (tr->dtlssrtpenc), g_object_ref (tr->nicesink), NULL);
  gst_element_link (tr->dtlssrtpenc, tr->nicesink);
  gst_element_sync_state_with_parent_target_state (tr->nicesink);
  gst_element_sync_state_with_parent_target_state (tr->dtlssrtpenc);

  add_bundle_funnels (webrtc_endpoint);

  gst_element_link_pads (webrtc_endpoint->priv->bundle_rtp_funnel, NULL,
      tr->dtlssrtpenc, "rtp_sink");
  gst_element_link_pads (webrtc_endpoint->priv->bundle_rtcp_funnel, NULL,
      tr->dtlssrtpenc, "rtcp_sink");
}

static void
connect_bundle_rtcp_funel (KmsWebrtcEndpoint * webrtc_endpoint,
    const gchar * media_str)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint =
      KMS_BASE_RTP_ENDPOINT (webrtc_endpoint);
  const gchar *rtcp_src_pad_name;

  /* FIXME: improve this */
  rtcp_src_pad_name = AUDIO_RTPBIN_SEND_RTCP_SRC;       /* audio by default */
  if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
    rtcp_src_pad_name = VIDEO_RTPBIN_SEND_RTCP_SRC;
  }

  gst_element_link_pads (kms_base_rtp_endpoint_get_rtpbin (base_rtp_endpoint),
      rtcp_src_pad_name, webrtc_endpoint->priv->bundle_rtcp_funnel, "sink_%u");
}

static void
kms_webrtc_endpoint_start_transport_send (KmsBaseSdpEndpoint *
    base_sdp_endpoint, const GstSDPMessage * offer,
    const GstSDPMessage * answer, gboolean local_offer)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (base_sdp_endpoint);
  KmsBaseRtpEndpoint *base_rtp = KMS_BASE_RTP_ENDPOINT (self);
  const GstSDPMessage *sdp;
  const gchar *ufrag, *pwd;
  gboolean bundle;
  guint len, i;
  GstElement *rtpbin;

  KMS_ELEMENT_LOCK (self);

  rtpbin = kms_base_rtp_endpoint_get_rtpbin (base_rtp);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->start_transport_send
      (base_sdp_endpoint, offer, answer, local_offer);

  if (local_offer) {
    sdp = answer;
  } else {
    sdp = offer;
  }

  ufrag = gst_sdp_message_get_attribute_val (sdp, SDP_ICE_UFRAG_ATTR);
  pwd = gst_sdp_message_get_attribute_val (sdp, SDP_ICE_PWD_ATTR);

  g_object_get (self, "bundle", &bundle, NULL);
  if (bundle) {
    add_webrtc_bundle_connection_src (self, !local_offer);
    add_webrtc_bundle_connection_sink (self);
  }

  len = gst_sdp_message_medias_len (sdp);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (sdp, i);
    const gchar *media_str;
    KmsWebRTCConnection *conn;
    guint ssrc;
    const gchar *rtp_src_pad_name = NULL;

    ssrc = sdp_utils_media_get_ssrc (media);
    media_str = gst_sdp_media_get_media (media);

    if (g_strcmp0 (AUDIO_STREAM_NAME, media_str) == 0) {
      conn = self->priv->audio_connection;
      self->priv->remote_audio_ssrc = ssrc;
      rtp_src_pad_name = AUDIO_RTPBIN_SEND_RTP_SRC;
    } else if (g_strcmp0 (VIDEO_STREAM_NAME, media_str) == 0) {
      conn = self->priv->video_connection;
      self->priv->remote_video_ssrc = ssrc;
      rtp_src_pad_name = VIDEO_RTPBIN_SEND_RTP_SRC;
    } else {
      GST_WARNING_OBJECT (self, "Media \"%s\" not supported", media_str);
      continue;
    }

    if (bundle) {
      kms_webrtc_connection_add_remote_candidates (self->
          priv->bundle_connection, media, ufrag, pwd);
      connect_bundle_rtcp_funel (self, media_str);

      add_bundle_funnels (self);
      gst_element_link_pads (rtpbin, rtp_src_pad_name,
          self->priv->bundle_rtp_funnel, "sink_%u");
    } else {
      add_webrtc_connection_src (self, conn, !local_offer);
      add_webrtc_connection_sink (self, conn);
      kms_webrtc_connection_add_remote_candidates (conn, media, ufrag, pwd);

      gst_element_link_pads (rtpbin,
          rtp_src_pad_name, conn->rtp_transport->dtlssrtpenc, "rtp_sink");
    }
  }

  KMS_ELEMENT_UNLOCK (self);
}

/* Start Transport end */

static void
gathering_done (NiceAgent * agent, guint stream_id, KmsWebrtcEndpoint * self)
{
  gboolean done;

  GST_DEBUG_OBJECT (self, "ICE gathering done for %s stream.",
      nice_agent_get_stream_name (agent, stream_id));

  KMS_ELEMENT_LOCK (self);

  if (self->priv->audio_connection != NULL
      && stream_id == self->priv->audio_connection->stream_id)
    self->priv->audio_ice_gathering_done = TRUE;

  if (self->priv->video_connection != NULL
      && stream_id == self->priv->video_connection->stream_id)
    self->priv->video_ice_gathering_done = TRUE;

  done = self->priv->audio_ice_gathering_done &&
      self->priv->video_ice_gathering_done;

  KMS_ELEMENT_UNLOCK (self);

  g_mutex_lock (&self->priv->ctx.gather_mutex);

  self->priv->ctx.ice_gathering_done = done;

  g_cond_broadcast (&self->priv->ctx.gather_cond);
  g_mutex_unlock (&self->priv->ctx.gather_mutex);
}

static void
kms_webrtc_endpoint_parse_turn_url (KmsWebrtcEndpoint * self)
{
  GRegex *regex;
  GMatchInfo *match_info = NULL;

  g_free (self->priv->turn_user);
  self->priv->turn_user = NULL;
  g_free (self->priv->turn_password);
  self->priv->turn_password = NULL;
  g_free (self->priv->turn_address);
  self->priv->turn_address = NULL;

  if ((self->priv->turn_url == NULL)
      || (g_strcmp0 ("", self->priv->turn_url) == 0)) {
    GST_INFO_OBJECT (self, "TURN server info cleared");
    return;
  }

  regex =
      g_regex_new
      ("^(?<user>.+):(?<password>.+)@(?<address>[0-9.]+):(?<port>[0-9]+)(\\?transport=(?<transport>(udp|tcp|tls)))?$",
      0, 0, NULL);
  g_regex_match (regex, self->priv->turn_url, 0, &match_info);
  g_regex_unref (regex);

  if (g_match_info_matches (match_info)) {
    gchar *port_str;
    gchar *turn_transport;

    self->priv->turn_user = g_match_info_fetch_named (match_info, "user");
    self->priv->turn_password =
        g_match_info_fetch_named (match_info, "password");
    self->priv->turn_address = g_match_info_fetch_named (match_info, "address");

    port_str = g_match_info_fetch_named (match_info, "port");
    self->priv->turn_port = g_ascii_strtoll (port_str, NULL, 10);
    g_free (port_str);

    self->priv->turn_transport = NICE_RELAY_TYPE_TURN_UDP;      /* default */
    turn_transport = g_match_info_fetch_named (match_info, "transport");
    if (turn_transport != NULL) {
      if (g_strcmp0 ("tcp", turn_transport) == 0) {
        self->priv->turn_transport = NICE_RELAY_TYPE_TURN_TCP;
      } else if (g_strcmp0 ("tls", turn_transport) == 0) {
        self->priv->turn_transport = NICE_RELAY_TYPE_TURN_TLS;
      }
      g_free (turn_transport);
    }

    GST_INFO_OBJECT (self, "TURN server info set (%s)", self->priv->turn_url);
  } else {
    GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS,
        ("URL '%s' not allowed. It must have this format: 'user:password@address:port(?transport=[udp|tcp|tls])'",
            self->priv->turn_url),
        ("URL '%s' not allowed. It must have this format: 'user:password@address:port(?transport=[udp|tcp|tls])'",
            self->priv->turn_url));
  }

  g_match_info_free (match_info);
}

static void
kms_webrtc_endpoint_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_CERTIFICATE_PEM_FILE:
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
    case PROP_TURN_URL:
      g_free (self->priv->turn_url);
      self->priv->turn_url = g_value_dup_string (value);
      kms_webrtc_endpoint_parse_turn_url (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_webrtc_endpoint_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

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
    case PROP_TURN_URL:
      g_value_set_string (value, self->priv->turn_url);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_webrtc_endpoint_dispose (GObject * object)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_ELEMENT_LOCK (self);

  g_clear_object (&self->priv->agent);
  g_clear_object (&self->priv->loop);

  KMS_ELEMENT_UNLOCK (self);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_endpoint_parent_class)->dispose (object);
}

static void
kms_webrtc_endpoint_finalize (GObject * object)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  kms_webrtc_connection_destroy (self->priv->audio_connection);
  kms_webrtc_connection_destroy (self->priv->video_connection);

  g_mutex_lock (&self->priv->ctx.gather_mutex);
  self->priv->ctx.finalized = TRUE;
  g_cond_broadcast (&self->priv->ctx.gather_cond);
  while (self->priv->ctx.wait_gathering)
    g_cond_wait (&self->priv->ctx.gather_cond, &self->priv->ctx.gather_mutex);
  g_mutex_unlock (&self->priv->ctx.gather_mutex);

  g_cond_clear (&self->priv->ctx.gather_cond);
  g_mutex_clear (&self->priv->ctx.gather_mutex);

  if (self->priv->tmp_dir != NULL) {
    remove_recursive (self->priv->tmp_dir);
    g_free (self->priv->tmp_dir);
  }

  g_free (self->priv->certificate_pem_file);
  g_free (self->priv->turn_url);
  g_free (self->priv->turn_user);
  g_free (self->priv->turn_password);
  g_free (self->priv->turn_address);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_endpoint_parent_class)->finalize (object);
}

static void
kms_webrtc_endpoint_class_init (KmsWebrtcEndpointClass * klass)
{
  GObjectClass *gobject_class;
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_webrtc_endpoint_set_property;
  gobject_class->get_property = kms_webrtc_endpoint_get_property;
  gobject_class->dispose = kms_webrtc_endpoint_dispose;
  gobject_class->finalize = kms_webrtc_endpoint_finalize;

  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "WebrtcEndpoint",
      "WEBRTC/Stream/WebrtcEndpoint",
      "WebRTC Endpoint element", "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);
  base_sdp_endpoint_class->set_transport_to_sdp =
      kms_webrtc_endpoint_set_transport_to_sdp;
  base_sdp_endpoint_class->start_transport_send =
      kms_webrtc_endpoint_start_transport_send;

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

  g_object_class_install_property (gobject_class, PROP_TURN_URL,
      g_param_spec_string ("turn-url",
          "TurnUrl",
          "TURN server URL with this format: 'user:password@address:port(?transport=[udp|tcp|tls])'."
          "'address' must be an IP (not a domain)."
          "'transport' is optional (UDP by default).",
          NULL, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndpointPrivate));
}

static void
kms_webrtc_endpoint_init (KmsWebrtcEndpoint * self)
{
  GMainContext *context;
  gchar t[] = TMP_DIR_TEMPLATE;

  g_object_set (G_OBJECT (self), "proto", SDP_MEDIA_RTP_SAVPF_PROTO, NULL);

  self->priv = KMS_WEBRTC_ENDPOINT_GET_PRIVATE (self);

  self->priv->tmp_dir = g_strdup (g_mkdtemp_full (t, FILE_PERMISIONS));

  g_mutex_init (&self->priv->ctx.gather_mutex);
  g_cond_init (&self->priv->ctx.gather_cond);
  self->priv->ctx.finalized = FALSE;

  self->priv->loop = kms_loop_new ();

  g_object_get (self->priv->loop, "context", &context, NULL);

  self->priv->agent = nice_agent_new (context, NICE_COMPATIBILITY_RFC5245);
  if (self->priv->agent == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create nice agent.");
    return;
  }

  g_object_set (self->priv->agent, "controlling-mode", FALSE, "upnp", FALSE,
      NULL);
  g_signal_connect (self->priv->agent, "candidate-gathering-done",
      G_CALLBACK (gathering_done), self);

  self->priv->audio_connection =
      kms_webrtc_connection_create (self->priv->agent, context,
      AUDIO_STREAM_NAME);
  if (self->priv->audio_connection == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create audio connection.");
    return;
  }

  self->priv->video_connection =
      kms_webrtc_connection_create (self->priv->agent, context,
      VIDEO_STREAM_NAME);
  if (self->priv->video_connection == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create video connection.");
    return;
  }

  g_main_context_unref (context);
}

gboolean
kms_webrtc_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_ENDPOINT);
}
