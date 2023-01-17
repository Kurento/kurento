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
#include "kmssdprtpmediahandler.h"

#define OBJECT_NAME "sdprtpmediahandler"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_rtp_media_handler_debug_category);
#define GST_CAT_DEFAULT kms_sdp_rtp_media_handler_debug_category

#define parent_class kms_sdp_rtp_media_handler_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpRtpMediaHandler, kms_sdp_rtp_media_handler,
    KMS_TYPE_SDP_MEDIA_HANDLER,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_rtp_media_handler_debug_category,
        OBJECT_NAME, 0, "debug category for sdp rtp media_handler"));

#define DEFAULT_SDP_MEDIA_RTP_RTCP_MUX TRUE
#define DEFAULT_SDP_MEDIA_RTP_RTCP_ENTRY TRUE

#define DEFAULT_RTCP_ENTRY_PORT 9

#define KMS_SDP_RTP_MEDIA_HANDLER_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                       \
    (obj),                                            \
    KMS_TYPE_SDP_RTP_MEDIA_HANDLER,                   \
    KmsSdpRtpMediaHandlerPrivate                      \
  )                                                   \
)

/* Object properties */
enum
{
  PROP_0,
  PROP_RTCP_MUX,
  PROP_RTCP_ENTRY,
  N_PROPERTIES
};

struct _KmsSdpRtpMediaHandlerPrivate
{
  gboolean rtcp_mux;
  gboolean rtcp_entry;
};

static void
kms_sdp_rtp_media_handler_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsSdpRtpMediaHandler *self = KMS_SDP_RTP_MEDIA_HANDLER (object);

  switch (prop_id) {
    case PROP_RTCP_MUX:
      g_value_set_boolean (value, self->priv->rtcp_mux);
      break;
    case PROP_RTCP_ENTRY:
      g_value_set_boolean (value, self->priv->rtcp_entry);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_rtp_media_handler_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsSdpRtpMediaHandler *self = KMS_SDP_RTP_MEDIA_HANDLER (object);

  switch (prop_id) {
    case PROP_RTCP_MUX:
      self->priv->rtcp_mux = g_value_get_boolean (value);
      break;
    case PROP_RTCP_ENTRY:
      self->priv->rtcp_entry = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static gboolean
kms_sdp_rtp_media_handler_can_insert_attribute (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  KmsSdpRtpMediaHandler *self = KMS_SDP_RTP_MEDIA_HANDLER (handler);

  if (g_strcmp0 (attr->key, "rtcp-mux") != 0) {
    return KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->can_insert_attribute
        (handler, offer, attr, answer, msg);
  }

  if (!self->priv->rtcp_mux) {
    return FALSE;
  }

  if (!self->priv->rtcp_entry) {
    return FALSE;
  }

  return !sdp_utils_is_attribute_in_media (answer, attr);
}

struct IntersectAttrData
{
  KmsSdpMediaHandler *handler;
  const GstSDPMedia *offer;
  GstSDPMedia *answer;
  const GstSDPMessage *msg;
};

static gboolean
instersect_rtp_media_attr (const GstSDPAttribute * attr, gpointer user_data)
{
  struct IntersectAttrData *data = (struct IntersectAttrData *) user_data;

  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (data->
          handler)->can_insert_attribute (data->handler, data->offer, attr,
          data->answer, data->msg)) {
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
kms_sdp_rtp_media_handler_intersect_sdp_medias (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, const GstSDPMessage * msg,
    GError ** error)
{
  struct IntersectAttrData data = {
    .handler = handler,
    .offer = offer,
    .answer = answer,
    .msg = msg
  };

  if (!sdp_utils_intersect_media_attributes (offer,
          instersect_rtp_media_attr, &data)) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR, "Can not intersect media attributes");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_media_handler_add_offer_attributes (KmsSdpMediaHandler * handler,
    GstSDPMedia * offer, const GstSDPMedia * prev_offer, GError ** error)
{
  KmsSdpRtpMediaHandler *self = KMS_SDP_RTP_MEDIA_HANDLER (handler);

  if (self->priv->rtcp_entry) {
    gchar *val, *addr, *addr_type;

    g_object_get (self, "addr", &addr, "addr_type", &addr_type, NULL);

    if (addr != NULL) {
      val = g_strdup_printf ("%d IN %s %s", DEFAULT_RTCP_ENTRY_PORT, addr_type,
          addr);
    } else {
      val = g_strdup_printf ("%d", DEFAULT_RTCP_ENTRY_PORT);
    }

    gst_sdp_media_add_attribute (offer, "rtcp", val);

    g_free (addr_type);
    g_free (addr);
    g_free (val);
  }

  if (self->priv->rtcp_mux) {
    gst_sdp_media_add_attribute (offer, "rtcp-mux", "");
  }

  return
      KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_offer_attributes (handler,
      offer, prev_offer, error);
}

static gboolean
kms_sdp_rtp_media_handler_add_answer_attributes (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  KmsSdpRtpMediaHandler *self = KMS_SDP_RTP_MEDIA_HANDLER (handler);

  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_answer_attributes
      (handler, offer, answer, error)) {
    return FALSE;
  }

  if (self->priv->rtcp_entry) {
    gchar *val, *addr, *addr_type;

    g_object_get (self, "addr", &addr, "addr_type", &addr_type, NULL);

    if (addr != NULL) {
      val = g_strdup_printf ("%d IN %s %s", DEFAULT_RTCP_ENTRY_PORT, addr_type,
          addr);
    } else {
      val = g_strdup_printf ("%d", DEFAULT_RTCP_ENTRY_PORT);
    }

    gst_sdp_media_add_attribute (answer, "rtcp", val);

    g_free (addr_type);
    g_free (addr);
    g_free (val);
  }

  return TRUE;
}

static void
kms_sdp_rtp_media_handler_class_init (KmsSdpRtpMediaHandlerClass * klass)
{
  GObjectClass *gobject_class;
  KmsSdpMediaHandlerClass *handler_class;

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->get_property = kms_sdp_rtp_media_handler_get_property;
  gobject_class->set_property = kms_sdp_rtp_media_handler_set_property;

  handler_class = KMS_SDP_MEDIA_HANDLER_CLASS (klass);

  handler_class->can_insert_attribute =
      kms_sdp_rtp_media_handler_can_insert_attribute;
  handler_class->intersect_sdp_medias =
      kms_sdp_rtp_media_handler_intersect_sdp_medias;
  handler_class->add_offer_attributes =
      kms_sdp_rtp_media_handler_add_offer_attributes;
  handler_class->add_answer_attributes =
      kms_sdp_rtp_media_handler_add_answer_attributes;

  g_object_class_install_property (gobject_class, PROP_RTCP_MUX,
      g_param_spec_boolean ("rtcp-mux", "rtcp-mux",
          "Wheter multiplexing RTP data and control packets on a single port is supported",
          DEFAULT_SDP_MEDIA_RTP_RTCP_MUX,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_RTCP_ENTRY,
      g_param_spec_boolean ("rtcp-entry", "rtcp-entry",
          "When TRUE an rtcp entry [rfc3605] will be added",
          DEFAULT_SDP_MEDIA_RTP_RTCP_ENTRY,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsSdpRtpMediaHandlerPrivate));
}

static void
kms_sdp_rtp_media_handler_init (KmsSdpRtpMediaHandler * self)
{
  self->priv = KMS_SDP_RTP_MEDIA_HANDLER_GET_PRIVATE (self);
}
