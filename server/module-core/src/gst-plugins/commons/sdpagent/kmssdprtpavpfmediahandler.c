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

#include "kmssdpagent.h"
#include "sdp_utils.h"
#include "kmssdprtpavpfmediahandler.h"
#include "constants.h"

#define OBJECT_NAME "rtpavpfmediahandler"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_rtp_avpf_media_handler_debug_category);
#define GST_CAT_DEFAULT kms_sdp_rtp_avpf_media_handler_debug_category

#define parent_class kms_sdp_rtp_avpf_media_handler_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpRtpAvpfMediaHandler,
    kms_sdp_rtp_avpf_media_handler, KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_rtp_avpf_media_handler_debug_category,
        OBJECT_NAME, 0, "debug category for sdp rtp avpf media_handler"));

#define SDP_MEDIA_RTP_AVPF_PROTO "RTP/AVPF"

#define DEFAULT_SDP_MEDIA_RTP_AVPF_NACK TRUE
#define DEFAULT_SDP_MEDIA_RTP_GOOG_REMB TRUE

static gchar *video_rtcp_fb_enc[] = {
  "VP8",
  "H264"
};

#define KMS_SDP_RTP_AVPF_MEDIA_HANDLER_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                            \
    (obj),                                                 \
    KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER,                   \
    KmsSdpRtpAvpfMediaHandlerPrivate                       \
  )                                                        \
)

/* Object properties */
enum
{
  PROP_0,
  PROP_NACK,
  PROP_GOOG_REMB,
  N_PROPERTIES
};

struct _KmsSdpRtpAvpfMediaHandlerPrivate
{
  gboolean nack;
  gboolean remb;
};

static GObject *
kms_sdp_rtp_avpf_media_handler_constructor (GType gtype, guint n_properties,
    GObjectConstructParam * properties)
{
  GObjectConstructParam *property;
  gchar const *name;
  GObject *object;
  guint i;

  for (i = 0, property = properties; i < n_properties; ++i, ++property) {
    name = g_param_spec_get_name (property->pspec);
    if (g_strcmp0 (name, "proto") == 0) {
      if (g_value_get_string (property->value) == NULL) {
        /* change G_PARAM_CONSTRUCT_ONLY value */
        g_value_set_string (property->value, SDP_MEDIA_RTP_AVPF_PROTO);
      }
    }
  }

  object =
      G_OBJECT_CLASS (parent_class)->constructor (gtype, n_properties,
      properties);

  return object;
}

static gboolean
is_supported_encoder (const gchar * codec)
{
  guint i, len;

  len = G_N_ELEMENTS (video_rtcp_fb_enc);

  for (i = 0; i < len; i++) {
    if (g_str_has_prefix (codec, video_rtcp_fb_enc[i])) {
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
kms_sdp_rtp_avpf_media_handler_rtcp_fb_attrs (KmsSdpMediaHandler * handler,
    GstSDPMedia * media, const gchar * fmt, const gchar * enc, GError ** error)
{
  KmsSdpRtpAvpfMediaHandler *self = KMS_SDP_RTP_AVPF_MEDIA_HANDLER (handler);
  gchar *attr;

  /* Add rtcp-fb attributes */

  if (!self->priv->nack) {
    goto no_nack;
  }

  attr = g_strdup_printf ("%s %s", fmt, SDP_MEDIA_RTCP_FB_NACK);

  if (gst_sdp_media_add_attribute (media, SDP_MEDIA_RTCP_FB,
          attr) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot add media attribute 'a=%s'", attr);
    g_free (attr);
    return FALSE;
  }

  g_free (attr);
  attr = g_strdup_printf ("%s %s %s", fmt /* format */ , SDP_MEDIA_RTCP_FB_NACK,
      SDP_MEDIA_RTCP_FB_PLI);

  if (gst_sdp_media_add_attribute (media, SDP_MEDIA_RTCP_FB,
          attr) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can add media attribute a=%s", attr);
    g_free (attr);
    return FALSE;
  }

  g_free (attr);

no_nack:
  if (!self->priv->remb) {
    goto no_remb;
  }

  if (g_str_has_prefix (enc, "VP8")) {
    /* Chrome adds goog-remb attribute */
    attr = g_strdup_printf ("%s %s", fmt, SDP_MEDIA_RTCP_FB_GOOG_REMB);

    if (gst_sdp_media_add_attribute (media, SDP_MEDIA_RTCP_FB,
            attr) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Cannot add media attribute 'a=%s'", attr);
      g_free (attr);
      return FALSE;
    }

    g_free (attr);
  }

no_remb:
  attr =
      g_strdup_printf ("%s %s %s", fmt, SDP_MEDIA_RTCP_FB_CCM,
      SDP_MEDIA_RTCP_FB_FIR);
  if (gst_sdp_media_add_attribute (media, SDP_MEDIA_RTCP_FB,
          attr) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot add media attribute 'a=%s'", attr);
    g_free (attr);
    return FALSE;
  }

  g_free (attr);

  return TRUE;
}

static gboolean
kms_sdp_rtp_avpf_media_handler_add_rtcp_fb_attrs (KmsSdpMediaHandler * handler,
    GstSDPMedia * media, GError ** error)
{
  const gchar *media_str;
  guint i;

  media_str = gst_sdp_media_get_media (media);

  if (g_strcmp0 (media_str, "video") != 0) {
    /* Only nack video rtcp_fb attributes are supported */
    /* [rfc4585] 4.2                                    */
    return TRUE;
  }

  for (i = 0;; i++) {
    const gchar *val;
    gchar **codec;

    val = gst_sdp_media_get_attribute_val_n (media, "rtpmap", i);

    if (val == NULL) {
      break;
    }

    codec = g_strsplit (val, " ", 0);
    if (!is_supported_encoder (codec[1] /* encoder */ )) {
      g_strfreev (codec);
      continue;
    }

    if (!kms_sdp_rtp_avpf_media_handler_rtcp_fb_attrs (handler, media,
            codec[0] /* format */ ,
            codec[1] /* encoder */ , error)) {
      g_strfreev (codec);
      return FALSE;
    }

    g_strfreev (codec);
  }

  return TRUE;
}

static GstSDPMedia *
kms_sdp_rtp_avpf_media_handler_create_offer (KmsSdpMediaHandler * handler,
    const gchar * media, const GstSDPMedia * prev_offer, GError ** error)
{
  GstSDPMedia *m;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not create '%s' media", media);
    goto error;
  }

  /* Create m-line */
  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->init_offer (handler, media,
          m, prev_offer, error)) {
    goto error;
  }

  /* Add attributes to m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->add_offer_attributes (handler,
          m, prev_offer, error)) {
    goto error;
  }

  return m;

error:
  if (m != NULL) {
    gst_sdp_media_free (m);
  }

  return NULL;
}

static gboolean
format_supported (const GstSDPMedia * media, const gchar * fmt)
{
  guint i, len;

  len = gst_sdp_media_formats_len (media);

  for (i = 0; i < len; i++) {
    const gchar *format;

    format = gst_sdp_media_get_format (media, i);

    if (g_strcmp0 (format, fmt) == 0) {
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
supported_rtcp_fb_val (const gchar * val)
{
  return g_strcmp0 (val, SDP_MEDIA_RTCP_FB_GOOG_REMB) == 0 ||
      g_strcmp0 (val, SDP_MEDIA_RTCP_FB_NACK) == 0 ||
      g_strcmp0 (val, SDP_MEDIA_RTCP_FB_CCM) == 0;

}

static gboolean
kms_sdp_rtp_avpf_media_handler_filter_rtcp_fb_attrs (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  KmsSdpRtpAvpfMediaHandler *self = KMS_SDP_RTP_AVPF_MEDIA_HANDLER (handler);
  guint i;

  for (i = 0;; i++) {
    const gchar *val;
    gchar **opts;

    val = gst_sdp_media_get_attribute_val_n (offer, SDP_MEDIA_RTCP_FB, i);

    if (val == NULL) {
      return TRUE;
    }

    opts = g_strsplit (val, " ", 0);

    if (!format_supported (answer, opts[0] /* format */ )) {
      /* Ignore rtcp-fb attribute */
      g_strfreev (opts);
      continue;
    }

    if (g_strcmp0 (opts[1] /* rtcp-fb-val */ , SDP_MEDIA_RTCP_FB_NACK) == 0
        && !self->priv->nack) {
      /* ignore rtcp-fb nack attribute */
      g_strfreev (opts);
      continue;
    }

    if (g_strcmp0 (opts[1] /* rtcp-fb-val */ , SDP_MEDIA_RTCP_FB_GOOG_REMB) == 0
        && !self->priv->remb) {
      /* ignore rtcp-fb goog-remb attribute */
      g_strfreev (opts);
      continue;
    }

    if (!supported_rtcp_fb_val (opts[1] /* rtcp-fb-val */ )) {
      /* ignore unsupported rtcp-fb attribute */
      g_strfreev (opts);
      continue;
    }

    if (gst_sdp_media_add_attribute (answer, SDP_MEDIA_RTCP_FB,
            val) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Cannot add media attribute 'a=%s:%s'", SDP_MEDIA_RTCP_FB, val);
      g_strfreev (opts);
      return FALSE;
    }

    g_strfreev (opts);
  }

  return TRUE;
}

GstSDPMedia *
kms_sdp_rtp_avpf_media_handler_create_answer (KmsSdpMediaHandler * handler,
    const GstSDPMessage * msg, const GstSDPMedia * offer, GError ** error)
{
  GstSDPMedia *m;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not create '%s' media answer", gst_sdp_media_get_media (offer));
    goto error;
  }

  /* Create m-line */
  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->init_answer (handler, offer,
          m, error)) {
    goto error;
  }

  /* Add attributes to m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->add_answer_attributes
      (handler, offer, m, error)) {
    goto error;
  }

  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->intersect_sdp_medias (handler,
          offer, m, msg, error)) {
    goto error;
  }

  return m;

error:
  if (m != NULL) {
    gst_sdp_media_free (m);
  }

  return NULL;
}

static gboolean
kms_sdp_rtp_avpf_media_handler_manage_protocol (KmsSdpMediaHandler * handler,
    const gchar * protocol)
{
  return g_strcmp0 (protocol, SDP_MEDIA_RTP_AVPF_PROTO) == 0 ||
      g_strcmp0 (protocol, SDP_MEDIA_RTP_AVP_PROTO) == 0;
}

static gboolean
kms_sdp_rtp_avpf_media_handler_can_insert_attribute (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  if (g_strcmp0 (attr->key, SDP_MEDIA_RTCP_FB) == 0) {
    /* ignore */
    return FALSE;
  }

  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->can_insert_attribute
      (handler, offer, attr, answer, msg)) {
    return FALSE;
  }

  return TRUE;
}

struct intersect_data
{
  KmsSdpMediaHandler *handler;
  const GstSDPMedia *offer;
  GstSDPMedia *answer;
  const GstSDPMessage *msg;
};

static gboolean
instersect_rtp_avpf_media_attr (const GstSDPAttribute * attr,
    gpointer user_data)
{
  struct intersect_data *data = (struct intersect_data *) user_data;

  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (data->handler)->
      can_insert_attribute (data->handler, data->offer, attr, data->answer,
          data->msg)) {
    return FALSE;
  }

  if (gst_sdp_media_add_attribute (data->answer, attr->key,
          attr->value) != GST_SDP_OK) {
    GST_WARNING ("Cannot add attribute '%s'", attr->key);
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avpf_media_handler_intersect_sdp_medias (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, GstSDPMedia * answer,
    const GstSDPMessage * msg, GError ** error)
{
  struct intersect_data data = {
    .handler = handler,
    .offer = offer,
    .answer = answer,
    .msg = msg
  };

  if (!sdp_utils_intersect_media_attributes (offer,
          instersect_rtp_avpf_media_attr, &data)) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR, "Can not intersect media attributes");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avpf_media_handler_add_offer_attributes (KmsSdpMediaHandler *
    handler, GstSDPMedia * offer, const GstSDPMedia * prev_offer,
    GError ** error)
{
  /* We depend of payloads supported by parent class */
  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_offer_attributes
      (handler, offer, prev_offer, error)) {
    return FALSE;
  }

  return kms_sdp_rtp_avpf_media_handler_add_rtcp_fb_attrs (handler, offer,
      error);
}

static gboolean
kms_sdp_rtp_avpf_media_handler_add_answer_attributes_impl (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_answer_attributes
      (handler, offer, answer, error)) {
    return FALSE;
  }

  if (g_strcmp0 (gst_sdp_media_get_proto (offer), SDP_MEDIA_RTP_AVP_PROTO) == 0) {
    /* Do not add specific feedback parameters in response */
    return TRUE;
  }

  return kms_sdp_rtp_avpf_media_handler_filter_rtcp_fb_attrs (handler, offer,
      answer, error);
}

static void
kms_sdp_rtp_avpf_media_handler_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsSdpRtpAvpfMediaHandler *self = KMS_SDP_RTP_AVPF_MEDIA_HANDLER (object);

  switch (prop_id) {
    case PROP_NACK:
      g_value_set_boolean (value, self->priv->nack);
      break;
    case PROP_GOOG_REMB:
      g_value_set_boolean (value, self->priv->remb);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_rtp_avpf_media_handler_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsSdpRtpAvpfMediaHandler *self = KMS_SDP_RTP_AVPF_MEDIA_HANDLER (object);

  switch (prop_id) {
    case PROP_NACK:
      self->priv->nack = g_value_get_boolean (value);
      break;
    case PROP_GOOG_REMB:
      self->priv->remb = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_rtp_avpf_media_handler_class_init (KmsSdpRtpAvpfMediaHandlerClass *
    klass)
{
  GObjectClass *gobject_class;
  KmsSdpMediaHandlerClass *handler_class;

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->constructor = kms_sdp_rtp_avpf_media_handler_constructor;
  gobject_class->get_property = kms_sdp_rtp_avpf_media_handler_get_property;
  gobject_class->set_property = kms_sdp_rtp_avpf_media_handler_set_property;

  handler_class = KMS_SDP_MEDIA_HANDLER_CLASS (klass);

  handler_class->create_offer = kms_sdp_rtp_avpf_media_handler_create_offer;
  handler_class->create_answer = kms_sdp_rtp_avpf_media_handler_create_answer;
  handler_class->manage_protocol =
      kms_sdp_rtp_avpf_media_handler_manage_protocol;

  handler_class->can_insert_attribute =
      kms_sdp_rtp_avpf_media_handler_can_insert_attribute;
  handler_class->intersect_sdp_medias =
      kms_sdp_rtp_avpf_media_handler_intersect_sdp_medias;
  handler_class->add_offer_attributes =
      kms_sdp_rtp_avpf_media_handler_add_offer_attributes;
  handler_class->add_answer_attributes =
      kms_sdp_rtp_avpf_media_handler_add_answer_attributes_impl;

  g_object_class_install_property (gobject_class, PROP_NACK,
      g_param_spec_boolean (SDP_MEDIA_RTCP_FB_NACK, "Nack",
          "Whether rtcp-fb-nack-param if supproted or not",
          DEFAULT_SDP_MEDIA_RTP_AVPF_NACK,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_GOOG_REMB,
      g_param_spec_boolean (SDP_MEDIA_RTCP_FB_GOOG_REMB, "Google-REMB",
          "Whether Google's Receiver Estimated Maximum Bitrate is supported",
          DEFAULT_SDP_MEDIA_RTP_GOOG_REMB,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsSdpRtpAvpfMediaHandlerPrivate));
}

static void
kms_sdp_rtp_avpf_media_handler_init (KmsSdpRtpAvpfMediaHandler * self)
{
  self->priv = KMS_SDP_RTP_AVPF_MEDIA_HANDLER_GET_PRIVATE (self);
}

KmsSdpRtpAvpfMediaHandler *
kms_sdp_rtp_avpf_media_handler_new ()
{
  KmsSdpRtpAvpfMediaHandler *handler;

  handler =
      KMS_SDP_RTP_AVPF_MEDIA_HANDLER (g_object_new
      (KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER, NULL));

  return handler;
}
