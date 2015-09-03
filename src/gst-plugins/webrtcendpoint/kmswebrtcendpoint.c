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

#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include "kmswebrtcendpoint.h"
#include "kmswebrtcsession.h"
#include <commons/kmsloop.h>
#include <commons/kmsutils.h>
#include <commons/sdp_utils.h>
#include <commons/kmsrefstruct.h>
#include <commons/sdpagent/kmssdprtpsavpfmediahandler.h>
#include <commons/sdpagent/kmssdpsctpmediahandler.h>
#include "kms-webrtc-marshal.h"
#include <glib/gstdio.h>
#include "kmswebrtcdatasessionbin.h"
#include "kms-webrtc-data-marshal.h"

#define KMS_WEBRTC_DATA_CHANNEL_PPID_STRING 51
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

#define DEFAULT_STUN_SERVER_IP NULL
#define DEFAULT_STUN_SERVER_PORT 3478
#define DEFAULT_STUN_TURN_URL NULL

#define MAX_DATA_CHANNELS 1

enum
{
  PROP_0,
  PROP_STUN_SERVER_IP,
  PROP_STUN_SERVER_PORT,
  PROP_TURN_URL,                /* user:password@address:port?transport=[udp|tcp|tls] */
  N_PROPERTIES
};

enum
{
  SIGNAL_ON_ICE_CANDIDATE,
  SIGNAL_ON_ICE_GATHERING_DONE,
  SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED,
  SIGNAL_GATHER_CANDIDATES,
  SIGNAL_ADD_ICE_CANDIDATE,
  SIGNAL_DATA_SESSION_ESTABLISHED,
  ACTION_CREATE_DATA_CHANNEL,
  ACTION_DESTROY_DATA_CHANNEL,
  LAST_SIGNAL
};

static guint kms_webrtc_endpoint_signals[LAST_SIGNAL] = { 0 };

typedef struct _DataChannel
{
  KmsRefStruct ref;
  KmsWebRtcDataChannel *chann;
  GstElement *appsink;
  GstElement *appsrc;
} DataChannel;

struct _KmsWebrtcEndpointPrivate
{
  KmsLoop *loop;
  GMainContext *context;

  GstElement *data_session;
  GHashTable *data_channels;

  gchar *stun_server_ip;
  guint stun_server_port;
  gchar *turn_url;
};

/* ConnectSCTPData begin */

typedef struct _ConnectSCTPData
{
  KmsRefStruct ref;
  KmsWebrtcEndpoint *self;
  KmsIRtpConnection *conn;
  GstSDPMedia *media;
  gboolean connected;
} ConnectSCTPData;

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
connect_sctp_data_new (KmsWebrtcEndpoint * self, GstSDPMedia * media,
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

/* Internal session management begin */

static void
on_ice_candidate (KmsWebrtcSession * sess, KmsIceCandidate * candidate,
    KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_CANDIDATE], 0,
      sdp_sess->id_str, candidate);
}

static void
on_ice_gathering_done (KmsWebrtcSession * sess, KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_GATHERING_DONE], 0,
      sdp_sess->id_str);
}

static void
on_ice_component_state_change (KmsWebrtcSession * sess, guint stream_id,
    guint component_id, NiceComponentState state, KmsWebrtcEndpoint * self)
{
  KmsSdpSession *sdp_sess = KMS_SDP_SESSION (sess);

  g_signal_emit (G_OBJECT (self),
      kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED], 0,
      sdp_sess->id_str, stream_id, component_id, state);
}

static void
kms_webrtc_endpoint_create_session_internal (KmsBaseSdpEndpoint * base_sdp,
    gint id, KmsSdpSession ** sess)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (base_sdp);
  KmsIRtpSessionManager *manager = KMS_I_RTP_SESSION_MANAGER (self);
  KmsWebrtcSession *webrtc_sess;

  webrtc_sess =
      kms_webrtc_session_new (base_sdp, id, manager, self->priv->context);

  g_object_bind_property (self, "stun-server",
      webrtc_sess, "stun-server", G_BINDING_DEFAULT);
  g_object_bind_property (self, "stun-server-port",
      webrtc_sess, "stun-server-port", G_BINDING_DEFAULT);
  g_object_bind_property (self, "turn-url",
      webrtc_sess, "turn-url", G_BINDING_DEFAULT);

  g_object_set (webrtc_sess, "stun-server", self->priv->stun_server_ip,
      "stun-server-port", self->priv->stun_server_port,
      "turn-url", self->priv->turn_url, NULL);

  g_signal_connect (webrtc_sess, "on-ice-candidate",
      G_CALLBACK (on_ice_candidate), self);
  g_signal_connect (webrtc_sess, "on-ice-gathering-done",
      G_CALLBACK (on_ice_gathering_done), self);
  g_signal_connect (webrtc_sess, "on-ice-component-state-changed",
      G_CALLBACK (on_ice_component_state_change), self);

  *sess = KMS_SDP_SESSION (webrtc_sess);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->create_session_internal (base_sdp, id,
      sess);
}

/* Internal session management end */

/* Media handler management begin */
static void
kms_webrtc_endpoint_create_media_handler (KmsBaseSdpEndpoint * base_sdp,
    const gchar * media, KmsSdpMediaHandler ** handler)
{
  if (g_strcmp0 (media, "audio") == 0 || g_strcmp0 (media, "video") == 0) {
    *handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_rtp_savpf_media_handler_new ());
  } else if (g_strcmp0 (media, "application") == 0) {
    *handler = KMS_SDP_MEDIA_HANDLER (kms_sdp_sctp_media_handler_new ());
  }

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->create_media_handler (base_sdp, media,
      handler);
}

/* Media handler management end */

/* Configure media SDP begin */

static gboolean
kms_webrtc_endpoint_configure_media (KmsBaseSdpEndpoint *
    base_sdp_endpoint, KmsSdpSession * sess, SdpMediaConfig * mconf)
{
  KmsWebrtcSession *webrtc_sess = KMS_WEBRTC_SESSION (sess);
  gboolean ret;

  /* Chain up */
  ret = KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->configure_media (base_sdp_endpoint,
      sess, mconf);
  if (ret == FALSE) {
    return FALSE;
  }

  if (!kms_webrtc_session_set_ice_credentials (webrtc_sess, mconf)) {
    return FALSE;
  }

  return kms_webrtc_session_set_crypto_info (webrtc_sess, mconf);
}

/* Configure media SDP end */

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
kms_webrtc_endpoint_data_session_established_cb (KmsWebRtcDataSessionBin *
    session, gboolean connected, KmsWebrtcEndpoint * self)
{
  GST_DEBUG_OBJECT (self, "Data session %" GST_PTR_FORMAT " %s",
      session, (connected) ? "established" : "finished");

  g_signal_emit (self,
      kms_webrtc_endpoint_signals[SIGNAL_DATA_SESSION_ESTABLISHED], 0,
      connected);
}

static void
kms_webrtc_endpoint_add_src_data_pad (KmsWebrtcEndpoint * self,
    DataChannel * channel)
{
  GstElement *data_tee;

  data_tee = kms_element_get_data_tee (KMS_ELEMENT (self));
  gst_element_link (channel->appsrc, data_tee);
}

static void
kms_webrtc_endpoint_add_sink_data_pad (KmsWebrtcEndpoint * self,
    DataChannel * channel)
{
  GstPad *sinkpad;

  sinkpad = gst_element_get_static_pad (channel->appsink, "sink");
  kms_element_connect_sink_target (KMS_ELEMENT (self), sinkpad,
      KMS_ELEMENT_PAD_TYPE_DATA);
  g_object_unref (sinkpad);
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

  /* FIXME: We asume that this is a binary buffer. Infer this information */
  /* from metadata that sctp elements set in each buffer. */

  ret = kms_webrtc_data_channel_push_buffer (channel->chann, buffer, TRUE);
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
kms_webrtc_endpoint_data_channel_opened_cb (KmsWebRtcDataSessionBin * session,
    guint stream_id, KmsWebrtcEndpoint * self)
{
  GstAppSinkCallbacks callbacks;
  KmsWebRtcDataChannel *chann;
  DataChannel *channel;

  GST_DEBUG_OBJECT (self, "Data channel with stream_id %u opened", stream_id);

  KMS_ELEMENT_LOCK (self);

  if (g_hash_table_size (self->priv->data_channels) >= MAX_DATA_CHANNELS) {
    GST_WARNING_OBJECT (self, "No more than %u data channel are allowed",
        MAX_DATA_CHANNELS);
    KMS_ELEMENT_UNLOCK (self);
    g_signal_emit_by_name (session, "destroy-data-channel", stream_id, NULL);

    return;
  }

  g_signal_emit_by_name (session, "get-data-channel", stream_id, &chann);

  if (chann == NULL) {
    GST_ERROR_OBJECT (self, "Can not get data channel with id %u", stream_id);
    KMS_ELEMENT_UNLOCK (self);

    return;
  }

  channel = data_channel_new (stream_id, chann);

  g_hash_table_insert (self->priv->data_channels, GUINT_TO_POINTER (stream_id),
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

  kms_webrtc_endpoint_add_src_data_pad (self, channel);
  kms_webrtc_endpoint_add_sink_data_pad (self, channel);

  gst_element_sync_state_with_parent (channel->appsrc);
  gst_element_sync_state_with_parent (channel->appsink);

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_webrtc_endpoint_data_channel_closed_cb (KmsWebRtcDataSessionBin * session,
    guint stream_id, KmsWebrtcEndpoint * self)
{
  GST_DEBUG_OBJECT (self, "Data channel with stream_id %u closed", stream_id);
}

static gboolean
configure_data_session (KmsWebrtcEndpoint * self, GstSDPMedia * media)
{
  const gchar *sctpmap_attr = NULL;
  gint port = -1;
  guint i, len;

  len = gst_sdp_media_formats_len (media);
  if (len < 0) {
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

  g_object_set (self->priv->data_session, "sctp-local-port", port,
      "sctp-remote-port", port, NULL);

  return TRUE;
}

static void
kms_webrtc_endpoint_link_pads (GstPad * src, GstPad * sink)
{
  if (gst_pad_link_full (src, sink, GST_PAD_LINK_CHECK_CAPS) != GST_PAD_LINK_OK) {
    GST_ERROR ("Error linking pads (src: %" GST_PTR_FORMAT ", sink: %"
        GST_PTR_FORMAT ")", src, sink);
  }
}

static void
kms_webrtc_endpoint_connect_data_session (KmsWebrtcEndpoint * self,
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

  tmppad = gst_element_get_static_pad (self->priv->data_session, "sink");
  kms_webrtc_endpoint_link_pads (srcpad, tmppad);
  g_object_unref (tmppad);

  tmppad = gst_element_get_static_pad (self->priv->data_session, "src");
  kms_webrtc_endpoint_link_pads (tmppad, sinkpad);
  g_object_unref (tmppad);

  gst_element_sync_state_with_parent_target_state (self->priv->data_session);

error:

  g_clear_object (&sinkpad);
  g_clear_object (&srcpad);
}

static void
kms_webrtc_endpoint_create_data_session (KmsWebrtcEndpoint * self,
    GstSDPMedia * media, KmsIRtpConnection * conn)
{
  gboolean is_client;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->data_session != NULL) {
    GST_WARNING_OBJECT (self, "Data session already initialized");
    goto end;
  }

  g_object_get (conn, "is-client", &is_client, NULL);

  self->priv->data_session =
      GST_ELEMENT (kms_webrtc_data_session_bin_new (is_client));
  g_signal_connect (self->priv->data_session, "data-session-established",
      G_CALLBACK (kms_webrtc_endpoint_data_session_established_cb), self);
  g_signal_connect (self->priv->data_session, "data-channel-opened",
      G_CALLBACK (kms_webrtc_endpoint_data_channel_opened_cb), self);
  g_signal_connect (self->priv->data_session, "data-channel-closed",
      G_CALLBACK (kms_webrtc_endpoint_data_channel_closed_cb), self);

  gst_bin_add (GST_BIN (self), self->priv->data_session);
  kms_webrtc_endpoint_connect_data_session (self, media, conn);

end:

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_webrtc_endpoint_create_data_session_cb (KmsIRtpConnection * conn,
    ConnectSCTPData * data)
{
  if (g_atomic_int_compare_and_exchange (&data->connected, FALSE, TRUE)) {
    kms_webrtc_endpoint_create_data_session (data->self, data->media,
        data->conn);
  } else {
    GST_WARNING_OBJECT (data->self, "SCTP elements already configured");
  }

  /* DTLS is already connected, so we do not need to be attached to this */
  /* signal any more. We can free the tmp data without waiting for the   */
  /* object to be realeased. (Early release) */
  g_signal_handlers_disconnect_by_data (data->conn, data);
}

static void
kms_webrtc_endpoint_support_sctp_stream (KmsWebrtcEndpoint * self,
    KmsSdpSession * sess, SdpMediaConfig * mconf)
{
  KmsBaseRtpSession *base_rtp_sess = KMS_BASE_RTP_SESSION (sess);
  gboolean connected = FALSE;
  KmsIRtpConnection *conn;
  ConnectSCTPData *data;
  GstSDPMedia *media;
  gulong handler_id = 0;

  conn = kms_base_rtp_session_get_connection (base_rtp_sess, mconf);
  if (conn == NULL) {
    return;
  }

  gst_sdp_media_copy (kms_sdp_media_config_get_sdp_media (mconf), &media);
  data = connect_sctp_data_new (self, media, conn);

  handler_id = g_signal_connect_data (conn, "connected",
      G_CALLBACK (kms_webrtc_endpoint_create_data_session_cb),
      kms_ref_struct_ref (KMS_REF_STRUCT_CAST (data)),
      (GClosureNotify) kms_ref_struct_unref, 0);

  g_object_get (conn, "connected", &connected, NULL);
  if (connected && g_atomic_int_compare_and_exchange (&data->connected, FALSE,
          TRUE)) {
    if (handler_id) {
      g_signal_handler_disconnect (conn, handler_id);
    }
    kms_webrtc_endpoint_create_data_session (self,
        kms_sdp_media_config_get_sdp_media (mconf), conn);
  } else {
    GST_DEBUG_OBJECT (self, "SCTP: waiting for DTLS layer to be established");
  }

  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (data));
}

static void
kms_webrtc_endpoint_connect_input_elements (KmsBaseSdpEndpoint *
    base_sdp_endpoint, KmsSdpSession * sess)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (base_sdp_endpoint);
  const GSList *item = kms_sdp_message_context_get_medias (sess->neg_sdp_ctx);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->connect_input_elements
      (base_sdp_endpoint, sess);

  for (; item != NULL; item = g_slist_next (item)) {
    SdpMediaConfig *mconf = item->data;
    GstSDPMedia *media = kms_sdp_media_config_get_sdp_media (mconf);
    const gchar *media_str = gst_sdp_media_get_media (media);

    if (gst_sdp_media_get_port (media) == 0) {
      /* Media not supported */
      GST_DEBUG_OBJECT (base_sdp_endpoint, "Media not supported: %s",
          media_str);
      continue;
    }

    if (g_strcmp0 (media_str, "application") == 0 &&
        g_strcmp0 (gst_sdp_media_get_proto (media), "DTLS/SCTP") == 0) {
      kms_webrtc_endpoint_support_sctp_stream (self, sess, mconf);
    }
  }
}

static void
kms_webrtc_endpoint_start_transport_send (KmsBaseSdpEndpoint *
    base_sdp_endpoint, KmsSdpSession * sess, gboolean offerer)
{
  KmsWebrtcSession *webrtc_sess = KMS_WEBRTC_SESSION (sess);

  /* Chain up */
  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_webrtc_endpoint_parent_class)->start_transport_send
      (base_sdp_endpoint, sess, offerer);

  kms_webrtc_session_start_transport_send (webrtc_sess, offerer);
}

/* ICE candidates management begin */

static gboolean
kms_webrtc_endpoint_gather_candidates (KmsWebrtcEndpoint * self,
    const gchar * sess_id)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;
  gboolean ret = TRUE;

  GST_DEBUG_OBJECT (self, "Gather candidates for session '%s'", sess_id);

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "There is not session '%s'", sess_id);
    return FALSE;
  }

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_signal_emit_by_name (webrtc_sess, "gather-candidates", &ret);

  return ret;
}

static gboolean
kms_webrtc_endpoint_add_ice_candidate (KmsWebrtcEndpoint * self,
    const gchar * sess_id, KmsIceCandidate * candidate)
{
  KmsBaseSdpEndpoint *base_sdp_ep = KMS_BASE_SDP_ENDPOINT (self);
  KmsSdpSession *sess;
  KmsWebrtcSession *webrtc_sess;
  gboolean ret;

  GST_DEBUG_OBJECT (self, "Add ICE candidate '%s' for session '%s'",
      kms_ice_candidate_get_candidate (candidate), sess_id);

  sess = kms_base_sdp_endpoint_get_session (base_sdp_ep, sess_id);
  if (sess == NULL) {
    GST_ERROR_OBJECT (self, "There is not session '%s'", sess_id);
    return FALSE;
  }

  webrtc_sess = KMS_WEBRTC_SESSION (sess);
  g_signal_emit_by_name (webrtc_sess, "add-ice-candidate", candidate, &ret);

  return ret;
}

/* ICE candidates management end */

static void
kms_webrtc_endpoint_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_STUN_SERVER_IP:
      g_free (self->priv->stun_server_ip);
      self->priv->stun_server_ip = g_value_dup_string (value);
      break;
    case PROP_STUN_SERVER_PORT:
      self->priv->stun_server_port = g_value_get_uint (value);
      break;
    case PROP_TURN_URL:
      g_free (self->priv->turn_url);
      self->priv->turn_url = g_value_dup_string (value);
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
    case PROP_STUN_SERVER_IP:
      g_value_set_string (value, self->priv->stun_server_ip);
      break;
    case PROP_STUN_SERVER_PORT:
      g_value_set_uint (value, self->priv->stun_server_port);
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

  g_clear_object (&self->priv->loop);
  g_hash_table_unref (self->priv->data_channels);

  KMS_ELEMENT_UNLOCK (self);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_endpoint_parent_class)->dispose (object);
}

static void
kms_webrtc_endpoint_finalize (GObject * object)
{
  KmsWebrtcEndpoint *self = KMS_WEBRTC_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_free (self->priv->stun_server_ip);
  g_free (self->priv->turn_url);

  g_main_context_unref (self->priv->context);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_endpoint_parent_class)->finalize (object);
}

static gint
kms_webrtc_endpoint_create_data_channel (KmsWebrtcEndpoint * self,
    gint max_packet_life_time, gint max_retransmits, const gchar * label,
    const gchar * protocol)
{
  gint stream_id = -1;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->data_session == NULL) {
    GST_WARNING_OBJECT (self, "Data session is not yet established");
  } else {
    g_signal_emit_by_name (self->priv->data_session, "create-data-channel",
        max_packet_life_time, max_retransmits, label, protocol, &stream_id);
  }

  KMS_ELEMENT_UNLOCK (self);

  return stream_id;
}

static void
kms_webrtc_endpoint_destroy_data_channel (KmsWebrtcEndpoint * self,
    gint stream_id)
{
  GST_DEBUG_OBJECT (self, "Destroy channel %u", stream_id);
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
  base_sdp_endpoint_class->create_session_internal =
      kms_webrtc_endpoint_create_session_internal;
  base_sdp_endpoint_class->start_transport_send =
      kms_webrtc_endpoint_start_transport_send;

  /* Media handler management */
  base_sdp_endpoint_class->create_media_handler =
      kms_webrtc_endpoint_create_media_handler;

  base_sdp_endpoint_class->configure_media =
      kms_webrtc_endpoint_configure_media;
  base_sdp_endpoint_class->connect_input_elements =
      kms_webrtc_endpoint_connect_input_elements;

  klass->gather_candidates = kms_webrtc_endpoint_gather_candidates;
  klass->add_ice_candidate = kms_webrtc_endpoint_add_ice_candidate;
  klass->create_data_channel = kms_webrtc_endpoint_create_data_channel;
  klass->destroy_data_channel = kms_webrtc_endpoint_destroy_data_channel;

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

  /**
  * KmsWebrtcEndpoint::on-ice-candidate:
  * @self: the object which received the signal
  * @sess_id: id of the related WebRTC session
  * @candidate: the local candidate gathered
  *
  * Notify of a new gathered local candidate for a #KmsWebrtcEndpoint.
  */
  kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_CANDIDATE] =
      g_signal_new ("on-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, on_ice_candidate), NULL,
      NULL, __kms_webrtc_marshal_VOID__STRING_OBJECT, G_TYPE_NONE, 2,
      G_TYPE_STRING, KMS_TYPE_ICE_CANDIDATE);

  /**
  * KmsWebrtcEndpoint::on-candidate-gathering-done:
  * @self: the object which received the signal
  * @sess_id: id of the related WebRTC session
  * @stream_id: The ID of the stream
  *
  * Notify that all candidates have been gathered for a #KmsWebrtcEndpoint
  */
  kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_GATHERING_DONE] =
      g_signal_new ("on-ice-gathering-done",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, on_ice_gathering_done), NULL,
      NULL, g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 1, G_TYPE_STRING);

  /**
   * KmsWebrtcEndpoint::on-component-state-changed
   * @self: the object which received the signal
   * @sess_id: id of the related WebRTC session
   * @stream_id: The ID of the stream
   * @component_id: The ID of the component
   * @state: The #NiceComponentState of the component
   *
   * This signal is fired whenever a component's state changes
   */
  kms_webrtc_endpoint_signals[SIGNAL_ON_ICE_COMPONENT_STATE_CHANGED] =
      g_signal_new ("on-ice-component-state-changed",
      G_OBJECT_CLASS_TYPE (klass), G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL,
      G_TYPE_NONE, 4, G_TYPE_STRING, G_TYPE_UINT, G_TYPE_UINT, G_TYPE_UINT,
      G_TYPE_INVALID);

  kms_webrtc_endpoint_signals[SIGNAL_ADD_ICE_CANDIDATE] =
      g_signal_new ("add-ice-candidate",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, add_ice_candidate), NULL, NULL,
      __kms_webrtc_marshal_BOOLEAN__STRING_OBJECT, G_TYPE_BOOLEAN, 2,
      G_TYPE_STRING, KMS_TYPE_ICE_CANDIDATE);

  kms_webrtc_endpoint_signals[SIGNAL_GATHER_CANDIDATES] =
      g_signal_new ("gather-candidates",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, gather_candidates), NULL, NULL,
      __kms_webrtc_marshal_BOOLEAN__STRING, G_TYPE_BOOLEAN, 1, G_TYPE_STRING);

  kms_webrtc_endpoint_signals[SIGNAL_DATA_SESSION_ESTABLISHED] =
      g_signal_new ("data-session-established",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, data_session_established),
      NULL, NULL, g_cclosure_marshal_VOID__BOOLEAN, G_TYPE_NONE, 1,
      G_TYPE_BOOLEAN);

  kms_webrtc_endpoint_signals[ACTION_CREATE_DATA_CHANNEL] =
      g_signal_new ("create-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, create_data_channel),
      NULL, NULL, __kms_webrtc_data_marshal_INT__INT_INT_STRING_STRING,
      G_TYPE_INT, 4, G_TYPE_INT, G_TYPE_INT, G_TYPE_STRING, G_TYPE_STRING);

  kms_webrtc_endpoint_signals[ACTION_DESTROY_DATA_CHANNEL] =
      g_signal_new ("destroy-data-channel",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsWebrtcEndpointClass, destroy_data_channel),
      NULL, NULL, g_cclosure_marshal_VOID__INT, G_TYPE_NONE, 1, G_TYPE_INT);

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndpointPrivate));
}

static void
kms_webrtc_endpoint_init (KmsWebrtcEndpoint * self)
{
  /* TODO: check which prop should be moved to session */
  g_object_set (G_OBJECT (self), "bundle", TRUE, "rtcp-mux", TRUE, "rtcp-nack",
      TRUE, "rtcp-remb", TRUE, NULL);

  self->priv = KMS_WEBRTC_ENDPOINT_GET_PRIVATE (self);
  self->priv->stun_server_ip = DEFAULT_STUN_SERVER_IP;
  self->priv->stun_server_port = DEFAULT_STUN_SERVER_PORT;
  self->priv->turn_url = DEFAULT_STUN_TURN_URL;

  self->priv->data_channels = g_hash_table_new_full (g_direct_hash,
      g_direct_equal, NULL, (GDestroyNotify) kms_ref_struct_unref);

  self->priv->loop = kms_loop_new ();
  g_object_get (self->priv->loop, "context", &self->priv->context, NULL);
}

gboolean
kms_webrtc_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_ENDPOINT);
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmswebrtcendpoint,
    "Kurento webrtc endpoint",
    kms_webrtc_endpoint_plugin_init, VERSION, "LGPL",
    "Kurento Elements", "http://kurento.com/")
