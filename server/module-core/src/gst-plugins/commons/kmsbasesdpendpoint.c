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

#include "kmsbasesdpendpoint.h"
#include "kmsagnosticcaps.h"
#include "kms-core-marshal.h"
#include "sdp_utils.h"
#include "sdpagent/kmssdprtpavpmediahandler.h"
#include "sdpagent/kmssdpbundlegroup.h"

#define PLUGIN_NAME "basesdpendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_base_sdp_endpoint_debug);
#define GST_CAT_DEFAULT kms_base_sdp_endpoint_debug

#define kms_base_sdp_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsBaseSdpEndpoint, kms_base_sdp_endpoint, KMS_TYPE_ELEMENT);

#define KMS_BASE_SDP_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_BASE_SDP_ENDPOINT,                   \
    KmsBaseSdpEndpointPrivate                     \
  )                                               \
)

static gboolean kms_base_sdp_endpoint_init_sdp_handlers (KmsBaseSdpEndpoint *
    self, KmsSdpSession * sess);

#define USE_IPV6_DEFAULT FALSE
#define MAX_VIDEO_RECV_BW_DEFAULT 0
#define MAX_AUDIO_RECV_BW_DEFAULT 0

#ifndef GST_VALUE_HOLDS_STRUCTURE
#define GST_VALUE_HOLDS_STRUCTURE(x) (G_VALUE_HOLDS((x), _gst_structure_type))
#endif

/* Signals and args */
enum
{
  SIGNAL_CREATE_SESSION,
  SIGNAL_RELEASE_SESSION,
  SIGNAL_GENERATE_OFFER,
  SIGNAL_PROCESS_OFFER,
  SIGNAL_PROCESS_ANSWER,
  SIGNAL_GET_LOCAL_SDP,
  SIGNAL_GET_REMOTE_SDP,
  LAST_SIGNAL
};

static guint kms_base_sdp_endpoint_signals[LAST_SIGNAL] = { 0 };

#define DEFAULT_MULTISESSION FALSE
#define DEFAULT_ADDR NULL
#define DEFAULT_BUNDLE    FALSE
#define DEFAULT_NUM_AUDIO_MEDIAS    0
#define DEFAULT_NUM_VIDEO_MEDIAS    0
#define DEFAULT_USE_DATA_CHANNELS FALSE

enum
{
  PROP_0,
  PROP_MULTISESSION,
  PROP_BUNDLE,
  PROP_USE_IPV6,
  PROP_ADDR,
  PROP_NUM_AUDIO_MEDIAS,
  PROP_NUM_VIDEO_MEDIAS,
  PROP_AUDIO_CODECS,
  PROP_VIDEO_CODECS,
  PROP_MAX_VIDEO_RECV_BW,
  PROP_MAX_AUDIO_RECV_BW,
  PROP_USE_DATA_CHANNELS,
  N_PROPERTIES
};

struct _KmsBaseSdpEndpointPrivate
{
  gboolean configured;
  gboolean multisession;
  gint next_session_id;
  GHashTable *sessions;
  GstSDPMessage *first_neg_sdp;

  gboolean bundle;
  gboolean use_ipv6;
  gchar *addr;
  gboolean use_data_channels;

  guint max_video_recv_bw;
  guint max_audio_recv_bw;

  guint num_audio_medias;
  guint num_video_medias;
  GArray *audio_codecs;
  GArray *video_codecs;

  guint audio_handlers;
  guint video_handlers;
  guint data_handlers;
};

/* KmsSdpSession begin */

static gboolean
kms_base_sdp_endpoint_configure_media (KmsSdpAgent * agent,
    KmsSdpMediaHandler * handler, GstSDPMedia * media, gpointer user_data)
{
  KmsSdpSession *sess = KMS_SDP_SESSION (user_data);
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (sess->ep));
  gint id, index;

  g_object_get (handler, "id", &id, "index", &index, NULL);

  GST_LOG ("Handler id: %d, SDP index: %d, Media: %s", id, index,
      gst_sdp_media_get_media (media));

  return base_sdp_endpoint_class->configure_media (sess->ep, sess, handler,
      media);
}

static void
kms_base_sdp_endpoint_create_media_handler (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess, const gchar * media, KmsSdpMediaHandler ** handler);

static KmsSdpMediaHandler *
on_handler_required_cb (KmsSdpAgent * agent, const GstSDPMedia * media,
    gpointer user_data)
{
  KmsSdpSession *session = KMS_SDP_SESSION (user_data);
  KmsBaseSdpEndpoint *self = KMS_BASE_SDP_ENDPOINT (session->ep);
  KmsSdpMediaHandler *handler = NULL;
  const gchar *media_str;
  guint *media_counter = NULL;

  media_str = gst_sdp_media_get_media (media);

  if (g_strcmp0 (media_str, "audio") == 0) {
    if (self->priv->num_audio_medias == 0) {
      GST_DEBUG_OBJECT (self, "No support specified for media '%s'", media_str);
      return NULL;
    }

    if (self->priv->audio_handlers >= self->priv->num_audio_medias) {
      GST_DEBUG_OBJECT (self, "No more '%s' medias supported", media_str);
      return NULL;
    }

    media_counter = &self->priv->audio_handlers;
  } else if (g_strcmp0 (media_str, "video") == 0) {
    if (self->priv->num_video_medias == 0) {
      GST_DEBUG_OBJECT (self, "No support specified for media '%s'", media_str);
      return NULL;
    }

    if (self->priv->video_handlers >= self->priv->num_video_medias) {
      GST_DEBUG_OBJECT (self, "No more '%s' medias supported", media_str);
      return NULL;
    }

    media_counter = &self->priv->video_handlers;
  } else if (g_strcmp0 (media_str, "application") == 0) {
    if (!self->priv->use_data_channels) {
      GST_DEBUG_OBJECT (self, "No support specified for media '%s'", media_str);
      return NULL;
    }

    if (self->priv->data_handlers > 0) {
      GST_DEBUG_OBJECT (self, "No more '%s' medias supported", media_str);
      return NULL;
    }

    media_counter = &self->priv->data_handlers;
  } else {
    GST_ERROR_OBJECT (self, "Media %s not supported", media_str);
    return NULL;
  }

  GST_LOG_OBJECT (self, "Requested media: %s", media_str);

  kms_base_sdp_endpoint_create_media_handler (self, session, media_str,
      &handler);

  if (handler != NULL) {
    *media_counter = *media_counter + 1;
  }

  return handler;
}

static void
on_media_answer_cb (KmsSdpAgent * agent, KmsSdpMediaHandler * handler,
    GstSDPMedia * media, gpointer user_data)
{
  KmsSdpSession *session = KMS_SDP_SESSION (user_data);

  GST_LOG_OBJECT (session, "Answer media");

  kms_base_sdp_endpoint_configure_media (agent, handler, media, user_data);
}

static void
on_media_offer_cb (KmsSdpAgent * agent, KmsSdpMediaHandler * handler,
    GstSDPMedia * media, gpointer user_data)
{
  KmsSdpSession *session = KMS_SDP_SESSION (user_data);

  GST_LOG_OBJECT (session, "Offered media");

  kms_base_sdp_endpoint_configure_media (agent, handler, media, user_data);
}

static const gchar *
kms_base_sdp_endpoint_create_session (KmsBaseSdpEndpoint * self)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));
  KmsSdpAgentCallbacks callbacks;
  gint id;
  gchar *ret = NULL;
  KmsSdpSession *sess = NULL;

  KMS_ELEMENT_LOCK (self);

  if (!self->priv->multisession && self->priv->configured) {
    GST_WARNING_OBJECT (self, "Not multisession: cannot create more sessions.");
    goto end;
  }

  id = g_atomic_int_add (&self->priv->next_session_id, 1);
  base_sdp_endpoint_class->create_session_internal (self, id, &sess);

  if (sess == NULL) {
    goto end;
  }

  kms_sdp_session_set_use_ipv6 (sess, self->priv->use_ipv6);
  if (self->priv->addr != NULL) {
    kms_sdp_session_set_addr (sess, self->priv->addr);
  }

  g_object_ref (sess);
  gst_bin_add (GST_BIN (self), GST_ELEMENT (sess));
  gst_element_sync_state_with_parent (GST_ELEMENT (sess));

  callbacks.on_handler_required = on_handler_required_cb;
  callbacks.on_media_answer = on_media_answer_cb;
  callbacks.on_media_answered = NULL;
  callbacks.on_media_offer = on_media_offer_cb;

  kms_sdp_agent_set_callbacks (sess->agent, &callbacks, sess, NULL);

  g_hash_table_insert (self->priv->sessions, g_strdup (sess->id_str), sess);

  GST_DEBUG_OBJECT (self, "Created session with id '%s'", sess->id_str);
  self->priv->configured = TRUE;
  ret = g_strdup (sess->id_str);

end:
  KMS_ELEMENT_UNLOCK (self);

  return ret;
}

static gboolean
kms_base_sdp_endpoint_release_session (KmsBaseSdpEndpoint * self,
    const gchar * sess_id)
{
  KmsSdpSession *sess;
  gboolean ret;

  KMS_ELEMENT_LOCK (self);

  GST_DEBUG_OBJECT (self, "Release session with id '%s'", sess_id);

  sess = g_hash_table_lookup (self->priv->sessions, sess_id);
  if (sess == NULL) {
    GST_WARNING_OBJECT (self, "There is not session '%s'", sess_id);
    ret = FALSE;
    goto end;
  }

  gst_element_set_state (GST_ELEMENT (sess), GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self), GST_ELEMENT (sess));

  ret = g_hash_table_remove (self->priv->sessions, sess_id);

end:
  KMS_ELEMENT_UNLOCK (self);

  return ret;
}

/* KmsSdpSession end */

/* Media handler management begin */

static void
kms_base_sdp_endpoint_create_media_handler_impl (KmsBaseSdpEndpoint * self,
    const gchar * media, KmsSdpMediaHandler ** handler)
{
  KmsBaseSdpEndpointClass *klass =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->create_media_handler ==
      kms_base_sdp_endpoint_create_media_handler_impl) {
    GST_WARNING_OBJECT (self, "%s does not reimplement 'create_media_handler'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_base_sdp_endpoint_create_media_handler (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess, const gchar * media, KmsSdpMediaHandler ** handler)
{
  KmsBaseSdpEndpointClass *klass =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));
  klass->create_media_handler (self, media, handler);

  if (*handler == NULL) {
    /* No media supported */
    return;
  }

  if (KMS_IS_SDP_RTP_AVP_MEDIA_HANDLER (*handler)) {
    KmsSdpRtpAvpMediaHandler *h = KMS_SDP_RTP_AVP_MEDIA_HANDLER (*handler);
    GError *err = NULL;

    kms_sdp_rtp_avp_media_handler_use_payload_manager (h,
        KMS_I_SDP_PAYLOAD_MANAGER (g_object_ref (sess->ptmanager)), &err);

    if (self->priv->audio_codecs != NULL) {
      int i;

      for (i = 0; i < self->priv->audio_codecs->len; i++) {
        GValue *v = &g_array_index (self->priv->audio_codecs, GValue, i);
        const GstStructure *s;

        if (!GST_VALUE_HOLDS_STRUCTURE (v)) {
          GST_WARNING_OBJECT (self, "Value into array is not a GstStructure");
          continue;
        }

        s = gst_value_get_structure (v);
        kms_sdp_rtp_avp_media_handler_add_audio_codec (h,
            gst_structure_get_name (s), &err);
      }
    }

    if (self->priv->video_codecs != NULL) {
      int i;

      for (i = 0; i < self->priv->video_codecs->len; i++) {
        GValue *v = &g_array_index (self->priv->video_codecs, GValue, i);
        const GstStructure *s;

        if (!GST_VALUE_HOLDS_STRUCTURE (v)) {
          GST_WARNING_OBJECT (self, "Value into array is not a GstStructure");
          continue;
        }

        s = gst_value_get_structure (v);
        kms_sdp_rtp_avp_media_handler_add_video_codec (h,
            gst_structure_get_name (s), &err);
      }
    }
  }
}

static gboolean
kms_base_sdp_endpoint_add_handler (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess, const gchar * media, gint bundle_group_id,
    guint max_recv_bw)
{
  KmsSdpMediaHandler *handler = NULL;
  GError *err = NULL;
  gint hid;

  kms_base_sdp_endpoint_create_media_handler (self, sess, media, &handler);
  if (handler == NULL) {
    GST_ERROR_OBJECT (self,
        "Cannot create media handler for session '%" G_GINT32_FORMAT "'",
        sess->id);
    return FALSE;
  }

  hid = kms_sdp_agent_add_proto_handler (sess->agent, media, handler, &err);
  if (hid < 0) {
    GST_ERROR_OBJECT (self,
        "Error adding handler in session '%" G_GINT32_FORMAT "': %s",
        sess->id, err->message);
    g_object_unref (handler);
    g_error_free (err);
    return FALSE;
  }

  if (bundle_group_id != -1) {
    if (!kms_sdp_agent_group_add (sess->agent, bundle_group_id, hid, &err)) {
      GST_ERROR_OBJECT (self,
          "Error adding handler to bundle group for session '%" G_GINT32_FORMAT
          "': %s", sess->id, err->message);
      g_error_free (err);
      return FALSE;
    }
  }

  if (max_recv_bw > 0) {
    kms_sdp_media_handler_add_bandwidth (handler, "AS", max_recv_bw);
  }

  return TRUE;
}

static gboolean
kms_base_sdp_endpoint_init_sdp_handlers (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess)
{
  GError *err = NULL;
  gint gid;

  gid = -1;
  if (self->priv->bundle) {
    gid = kms_sdp_agent_create_group (sess->agent, KMS_TYPE_SDP_BUNDLE_GROUP,
        &err, NULL);
    if (gid < 0) {
      GST_ERROR_OBJECT (self, "Error: %s.", err->message);
      g_error_free (err);
      return FALSE;
    }
  }

  for (guint i = 0; i < self->priv->num_audio_medias; i++) {
    if (!kms_base_sdp_endpoint_add_handler (self, sess, "audio", gid,
            self->priv->max_audio_recv_bw)) {
      return FALSE;
    }
    self->priv->audio_handlers++;
  }

  for (guint i = 0; i < self->priv->num_video_medias; i++) {
    if (!kms_base_sdp_endpoint_add_handler (self, sess, "video", gid,
            self->priv->max_video_recv_bw)) {
      return FALSE;
    }
    self->priv->video_handlers++;
  }

  if (self->priv->use_data_channels) {
    if (!kms_base_sdp_endpoint_add_handler (self, sess, "application", gid, 0)) {
      return FALSE;
    }
    self->priv->data_handlers++;
  }

  return TRUE;
}

/* Media handler management end */

const GstSDPMessage *
kms_base_sdp_endpoint_get_first_negotiated_sdp (KmsBaseSdpEndpoint * self)
{
  const GstSDPMessage *ret;

  KMS_ELEMENT_LOCK (self);
  ret = self->priv->first_neg_sdp;
  KMS_ELEMENT_UNLOCK (self);

  return ret;
}

static void
kms_base_sdp_endpoint_start_transport_send (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess, gboolean offerer)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));

  /* Default function, do nothing */
  if (base_sdp_endpoint_class->start_transport_send ==
      kms_base_sdp_endpoint_start_transport_send) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement \"start_transport_send\"",
        G_OBJECT_CLASS_NAME (base_sdp_endpoint_class));
  }
}

static void
kms_base_sdp_endpoint_connect_input_elements (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));

  /* Default function, do nothing */
  if (base_sdp_endpoint_class->connect_input_elements ==
      kms_base_sdp_endpoint_connect_input_elements) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement \"connect_input_elements\"",
        G_OBJECT_CLASS_NAME (base_sdp_endpoint_class));
  }
}

static void
kms_base_sdp_endpoint_create_session_internal (KmsBaseSdpEndpoint * self,
    gint id, KmsSdpSession ** sess)
{
  if (*sess == NULL) {
    GST_WARNING_OBJECT (self,
        "Creating default sdp session, should be only done for testing");
    *sess = kms_sdp_session_new (self, id);
  }
}

static void
kms_base_sdp_endpoint_start_media (KmsBaseSdpEndpoint * self,
    KmsSdpSession * sess, gboolean offerer)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));

  GST_DEBUG_OBJECT (self, "Start media");

  base_sdp_endpoint_class->start_transport_send (self, sess, offerer);
  base_sdp_endpoint_class->connect_input_elements (self, sess);
}

static gboolean
kms_base_sdp_endpoint_configure_media_impl (KmsBaseSdpEndpoint *
    self, KmsSdpSession * sess, KmsSdpMediaHandler * handler,
    GstSDPMedia * media)
{
  KmsBaseSdpEndpointClass *base_sdp_endpoint_class =
      KMS_BASE_SDP_ENDPOINT_CLASS (G_OBJECT_GET_CLASS (self));

  if (base_sdp_endpoint_class->configure_media ==
      kms_base_sdp_endpoint_configure_media_impl) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'configure_media'",
        G_OBJECT_CLASS_NAME (base_sdp_endpoint_class));
  }

  /* Default function, do nothing */
  return TRUE;
}

static GstSDPMessage *
kms_base_sdp_endpoint_generate_offer (KmsBaseSdpEndpoint * self,
    const gchar * sess_id)
{
  KmsSdpSession *sess;
  GstSDPMessage *offer = NULL;

  KMS_ELEMENT_LOCK (self);

  GST_DEBUG_OBJECT (self, "Generate offer for session '%s'", sess_id);

  sess = g_hash_table_lookup (self->priv->sessions, sess_id);
  if (sess == NULL) {
    GST_WARNING_OBJECT (self, "There is not session '%s'", sess_id);
    goto end;
  }

  if (!kms_base_sdp_endpoint_init_sdp_handlers (self, sess)) {
    goto end;
  }

  offer = kms_sdp_session_generate_offer (sess);

end:
  KMS_ELEMENT_UNLOCK (self);

  return offer;
}

static GstSDPMessage *
kms_base_sdp_endpoint_process_offer (KmsBaseSdpEndpoint * self,
    const gchar * sess_id, GstSDPMessage * offer)
{
  KmsSdpSession *sess;
  GstSDPMessage *answer = NULL;

  KMS_ELEMENT_LOCK (self);

  GST_DEBUG_OBJECT (self, "Process SDP Offer, session ID: '%s'", sess_id);

  sess = g_hash_table_lookup (self->priv->sessions, sess_id);
  if (sess == NULL) {
    GST_WARNING_OBJECT (self, "Unknown session ID: '%s'", sess_id);
    goto end;
  }

  if (self->priv->bundle) {
    GError *err = NULL;
    gint gid;

    /* Create an empty bundle group so that the agent can add */
    /* handlers in it in case remote peer initiates negotiation */

    gid = kms_sdp_agent_create_group (sess->agent, KMS_TYPE_SDP_BUNDLE_GROUP,
        &err, NULL);

    if (gid < 0) {
      GST_ERROR_OBJECT (self, "Cannot create bundle group: %s.", err->message);
      g_error_free (err);
    }
  }

  answer = kms_sdp_session_process_offer (sess, offer);

  if (!answer) {
    goto end;
  }

  if (self->priv->first_neg_sdp == NULL && sess->neg_sdp) {
    gst_sdp_message_copy (sess->neg_sdp, &self->priv->first_neg_sdp);
  }
  kms_base_sdp_endpoint_start_media (self, sess, FALSE);

end:
  KMS_ELEMENT_UNLOCK (self);

  return answer;
}

static gboolean
kms_base_sdp_endpoint_process_answer (KmsBaseSdpEndpoint * self,
    const gchar * sess_id, GstSDPMessage * answer)
{
  KmsSdpSession *sess;
  gboolean ret = FALSE;

  KMS_ELEMENT_LOCK (self);

  GST_DEBUG_OBJECT (self, "Process answer for session '%s'", sess_id);

  sess = g_hash_table_lookup (self->priv->sessions, sess_id);
  if (sess == NULL) {
    GST_WARNING_OBJECT (self, "There is not session '%s'", sess_id);
    goto end;
  }

  ret = kms_sdp_session_process_answer (sess, answer);

  if (!ret) {
    goto end;
  }

  if (self->priv->first_neg_sdp == NULL && sess->neg_sdp) {
    gst_sdp_message_copy (sess->neg_sdp, &self->priv->first_neg_sdp);
  }
  kms_base_sdp_endpoint_start_media (self, sess, TRUE);

end:
  KMS_ELEMENT_UNLOCK (self);

  return ret;
}

static GstSDPMessage *
kms_base_sdp_endpoint_get_local_sdp (KmsBaseSdpEndpoint * self,
    const gchar * sess_id)
{
  KmsSdpSession *sess;
  GstSDPMessage *sdp = NULL;

  KMS_ELEMENT_LOCK (self);

  GST_DEBUG_OBJECT (self, "Get local SDP for session '%s'", sess_id);

  sess = g_hash_table_lookup (self->priv->sessions, sess_id);
  if (sess == NULL) {
    GST_WARNING_OBJECT (self, "There is not session '%s'", sess_id);
    goto end;
  }

  sdp = kms_sdp_session_get_local_sdp (sess);

end:
  KMS_ELEMENT_UNLOCK (self);

  return sdp;
}

static GstSDPMessage *
kms_base_sdp_endpoint_get_remote_sdp (KmsBaseSdpEndpoint * self,
    const gchar * sess_id)
{
  KmsSdpSession *sess;
  GstSDPMessage *sdp = NULL;

  KMS_ELEMENT_LOCK (self);

  GST_DEBUG_OBJECT (self, "Get remote SDP for session '%s'", sess_id);

  sess = g_hash_table_lookup (self->priv->sessions, sess_id);
  if (sess == NULL) {
    GST_WARNING_OBJECT (self, "There is not session '%s'", sess_id);
    goto end;
  }

  sdp = kms_sdp_session_get_remote_sdp (sess);

end:
  KMS_ELEMENT_UNLOCK (self);

  return sdp;
}

static void
kms_base_sdp_endpoint_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsBaseSdpEndpoint *self = KMS_BASE_SDP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_MULTISESSION:
      if (self->priv->configured) {
        GST_WARNING_OBJECT (self,
            "Element already configured, 'multisession' cannot be set.");
        break;
      }
      self->priv->multisession = g_value_get_boolean (value);
      break;
    case PROP_BUNDLE:
      self->priv->bundle = g_value_get_boolean (value);
      break;
    case PROP_USE_IPV6:{
      self->priv->use_ipv6 = g_value_get_boolean (value);
      break;
    }
    case PROP_ADDR:
      g_free (self->priv->addr);
      self->priv->addr = g_value_dup_string (value);
      break;
    case PROP_MAX_VIDEO_RECV_BW:
      self->priv->max_video_recv_bw = g_value_get_uint (value);
      break;
    case PROP_MAX_AUDIO_RECV_BW:
      self->priv->max_audio_recv_bw = g_value_get_uint (value);
      break;
    case PROP_NUM_AUDIO_MEDIAS:{
      guint n = g_value_get_uint (value);

      if (n > 1) {
        GST_WARNING_OBJECT (self, "Only 1 audio media is supported");
        n = 1;
      }

      self->priv->num_audio_medias = n;
      break;
    }
    case PROP_NUM_VIDEO_MEDIAS:{
      guint n = g_value_get_uint (value);

      if (n > 1) {
        GST_WARNING_OBJECT (self, "Only 1 video media is supported");
        n = 1;
      }

      self->priv->num_video_medias = n;
      break;
    }
    case PROP_AUDIO_CODECS:{
      if (self->priv->audio_codecs != NULL) {
        g_array_free (self->priv->audio_codecs, TRUE);
      }

      self->priv->audio_codecs = g_value_get_boxed (value);
      g_array_set_clear_func (self->priv->audio_codecs,
          (GDestroyNotify) g_value_unset);
      break;
    }
    case PROP_VIDEO_CODECS:{
      if (self->priv->video_codecs != NULL) {
        g_array_free (self->priv->video_codecs, TRUE);
      }

      self->priv->video_codecs = g_value_get_boxed (value);
      g_array_set_clear_func (self->priv->video_codecs,
          (GDestroyNotify) g_value_unset);
      break;
    }
    case PROP_USE_DATA_CHANNELS:
      self->priv->use_data_channels = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_base_sdp_endpoint_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsBaseSdpEndpoint *self = KMS_BASE_SDP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (self);

  switch (prop_id) {
    case PROP_MULTISESSION:
      g_value_set_boolean (value, self->priv->multisession);
      break;
    case PROP_BUNDLE:
      g_value_set_boolean (value, self->priv->bundle);
      break;
    case PROP_USE_IPV6:{
      g_value_set_boolean (value, self->priv->use_ipv6);
      break;
    }
    case PROP_ADDR:
      g_value_set_string (value, self->priv->addr);
      break;
    case PROP_MAX_VIDEO_RECV_BW:
      g_value_set_uint (value, self->priv->max_video_recv_bw);
      break;
    case PROP_MAX_AUDIO_RECV_BW:
      g_value_set_uint (value, self->priv->max_audio_recv_bw);
      break;
    case PROP_NUM_AUDIO_MEDIAS:
      g_value_set_uint (value, self->priv->num_audio_medias);
      break;
    case PROP_NUM_VIDEO_MEDIAS:
      g_value_set_uint (value, self->priv->num_video_medias);
      break;
    case PROP_AUDIO_CODECS:
      g_value_set_boxed (value, self->priv->audio_codecs);
      break;
    case PROP_VIDEO_CODECS:
      g_value_set_boxed (value, self->priv->video_codecs);
      break;
    case PROP_USE_DATA_CHANNELS:
      g_value_set_boolean (value, self->priv->use_data_channels);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_base_sdp_endpoint_finalize (GObject * object)
{
  KmsBaseSdpEndpoint *self = KMS_BASE_SDP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_hash_table_destroy (self->priv->sessions);

  if (self->priv->first_neg_sdp != NULL) {
    gst_sdp_message_free (self->priv->first_neg_sdp);
  }

  if (self->priv->audio_codecs != NULL) {
    g_array_free (self->priv->audio_codecs, TRUE);
  }

  if (self->priv->video_codecs != NULL) {
    g_array_free (self->priv->video_codecs, TRUE);
  }

  g_free (self->priv->addr);

  /* chain up */
  G_OBJECT_CLASS (kms_base_sdp_endpoint_parent_class)->finalize (object);
}

static void
kms_base_sdp_endpoint_class_init (KmsBaseSdpEndpointClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->get_property = kms_base_sdp_endpoint_get_property;
  gobject_class->set_property = kms_base_sdp_endpoint_set_property;
  gobject_class->finalize = kms_base_sdp_endpoint_finalize;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseSdpEndpoint",
      "Base/Bin/BaseSdpEndpoint",
      "Base class for sdpEndpoints",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  /* Session management */
  klass->create_session = kms_base_sdp_endpoint_create_session;
  klass->release_session = kms_base_sdp_endpoint_release_session;

  /* Media handler management */
  klass->create_media_handler = kms_base_sdp_endpoint_create_media_handler_impl;

  klass->create_session_internal =
      kms_base_sdp_endpoint_create_session_internal;
  klass->start_transport_send = kms_base_sdp_endpoint_start_transport_send;
  klass->connect_input_elements = kms_base_sdp_endpoint_connect_input_elements;

  klass->configure_media = kms_base_sdp_endpoint_configure_media_impl;

  klass->generate_offer = kms_base_sdp_endpoint_generate_offer;
  klass->process_offer = kms_base_sdp_endpoint_process_offer;
  klass->process_answer = kms_base_sdp_endpoint_process_answer;
  klass->get_local_sdp = kms_base_sdp_endpoint_get_local_sdp;
  klass->get_remote_sdp = kms_base_sdp_endpoint_get_remote_sdp;

  /* Signals initialization */
  kms_base_sdp_endpoint_signals[SIGNAL_CREATE_SESSION] =
      g_signal_new ("create-session",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, create_session), NULL,
      NULL, __kms_core_marshal_STRING__VOID, G_TYPE_STRING, 0);

  kms_base_sdp_endpoint_signals[SIGNAL_RELEASE_SESSION] =
      g_signal_new ("release-session",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, release_session), NULL,
      NULL, __kms_core_marshal_BOOLEAN__STRING, G_TYPE_BOOLEAN, 1,
      G_TYPE_STRING);

  kms_base_sdp_endpoint_signals[SIGNAL_GENERATE_OFFER] =
      g_signal_new ("generate-offer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, generate_offer), NULL, NULL,
      __kms_core_marshal_BOXED__STRING, GST_TYPE_SDP_MESSAGE, 1, G_TYPE_STRING);

  kms_base_sdp_endpoint_signals[SIGNAL_PROCESS_OFFER] =
      g_signal_new ("process-offer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, process_offer), NULL, NULL,
      __kms_core_marshal_BOXED__STRING_BOXED, GST_TYPE_SDP_MESSAGE, 2,
      G_TYPE_STRING, GST_TYPE_SDP_MESSAGE);

  kms_base_sdp_endpoint_signals[SIGNAL_PROCESS_ANSWER] =
      g_signal_new ("process-answer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, process_answer), NULL, NULL,
      __kms_core_marshal_BOOLEAN__STRING_BOXED, G_TYPE_BOOLEAN, 2,
      G_TYPE_STRING, GST_TYPE_SDP_MESSAGE);

  kms_base_sdp_endpoint_signals[SIGNAL_GET_LOCAL_SDP] =
      g_signal_new ("get-local-sdp",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, get_local_sdp), NULL, NULL,
      __kms_core_marshal_BOXED__STRING, GST_TYPE_SDP_MESSAGE, 1, G_TYPE_STRING);

  kms_base_sdp_endpoint_signals[SIGNAL_GET_REMOTE_SDP] =
      g_signal_new ("get-remote-sdp",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndpointClass, get_remote_sdp), NULL, NULL,
      __kms_core_marshal_BOXED__STRING, GST_TYPE_SDP_MESSAGE, 1, G_TYPE_STRING);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_MULTISESSION,
      g_param_spec_boolean ("multisession", "Multisession",
          "Enable creating multiple sessions.",
          DEFAULT_MULTISESSION, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_BUNDLE,
      g_param_spec_boolean ("bundle", "Bundle media",
          "Bundle media", DEFAULT_BUNDLE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_NUM_AUDIO_MEDIAS,
      g_param_spec_uint ("num-audio-medias", "Number of audio medias",
          "Number of audio medias to be negotiated",
          0, G_MAXUINT32, DEFAULT_NUM_AUDIO_MEDIAS,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_NUM_VIDEO_MEDIAS,
      g_param_spec_uint ("num-video-medias", "Number of video medias",
          "Number of video medias to be negotiated",
          0, G_MAXUINT32, DEFAULT_NUM_VIDEO_MEDIAS,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_AUDIO_CODECS,
      g_param_spec_boxed ("audio-codecs", "Audio codecs", "Audio codecs",
          G_TYPE_ARRAY, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_VIDEO_CODECS,
      g_param_spec_boxed ("video-codecs", "Video codecs", "Video codecs",
          G_TYPE_ARRAY, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_USE_IPV6,
      g_param_spec_boolean ("use-ipv6", "Use ipv6 in SDPs",
          "Use ipv6 addresses in generated sdp offers and answers",
          USE_IPV6_DEFAULT, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_ADDR,
      g_param_spec_string ("addr", "IP address",
          "The IP address used to negotiate SDPs",
          DEFAULT_ADDR, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_MAX_VIDEO_RECV_BW,
      g_param_spec_uint ("max-video-recv-bandwidth",
          "Maximum inbound bitrate requested by this endpoint.",
          "Maximum inbound bitrate requested by this endpoint. Unit: kbps (kilobits per second). Default: 0 (unlimited).",
          0, G_MAXUINT32, MAX_VIDEO_RECV_BW_DEFAULT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_MAX_AUDIO_RECV_BW,
      g_param_spec_uint ("max-audio-recv-bandwidth",
          "Maximum inbound bitrate requested by this endpoint.",
          "Maximum inbound bitrate requested by this endpoint. Unit: kbps (kilobits per second). Default: 0 (unlimited).",
          0, G_MAXUINT32, MAX_AUDIO_RECV_BW_DEFAULT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_USE_DATA_CHANNELS,
      g_param_spec_boolean ("use-data-channels", "Use data channels",
          "Negotiate data channels when this property is true",
          DEFAULT_USE_DATA_CHANNELS,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsBaseSdpEndpointPrivate));
}

static void
kms_base_sdp_endpoint_init (KmsBaseSdpEndpoint * self)
{
  self->priv = KMS_BASE_SDP_ENDPOINT_GET_PRIVATE (self);

  self->priv->multisession = DEFAULT_MULTISESSION;
  self->priv->bundle = DEFAULT_BUNDLE;
  self->priv->use_ipv6 = USE_IPV6_DEFAULT;
  self->priv->addr = DEFAULT_ADDR;
  self->priv->sessions =
      g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      (GDestroyNotify) g_object_unref);

  self->priv->max_video_recv_bw = MAX_VIDEO_RECV_BW_DEFAULT;
  self->priv->max_audio_recv_bw = MAX_AUDIO_RECV_BW_DEFAULT;
}

GHashTable *
kms_base_sdp_endpoint_get_sessions (KmsBaseSdpEndpoint * self)
{
  return self->priv->sessions;
}

KmsSdpSession *
kms_base_sdp_endpoint_get_session (KmsBaseSdpEndpoint * self,
    const gchar * sess_id)
{
  return g_hash_table_lookup (self->priv->sessions, sess_id);
}
