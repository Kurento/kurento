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

#include <stdlib.h>
#include <string.h>

#include "kmssdpagent.h"
#include "sdp_utils.h"
#include "kmssdprtpavpmediahandler.h"

#define OBJECT_NAME "sdprtpavpmediahandler"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_rtp_avp_media_handler_debug_category);
#define GST_CAT_DEFAULT kms_sdp_rtp_avp_media_handler_debug_category

#define parent_class kms_sdp_rtp_avp_media_handler_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpRtpAvpMediaHandler,
    kms_sdp_rtp_avp_media_handler, KMS_TYPE_SDP_RTP_MEDIA_HANDLER,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_rtp_avp_media_handler_debug_category,
        OBJECT_NAME, 0, "debug category for sdp rtp avp media_handler"));

#define KMS_SDP_RTP_AVP_MEDIA_HANDLER_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                           \
    (obj),                                                \
    KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER,                   \
    KmsSdpRtpAvpMediaHandlerPrivate                       \
  )                                                       \
)

struct _KmsSdpRtpAvpMediaHandlerPrivate
{
  GHashTable *extmaps;
  KmsISdpPayloadManager *ptmanager;
  GSList *audio_fmts;
  GSList *video_fmts;
};

#define SDP_AUDIO_MEDIA "audio"
#define SDP_VIDEO_MEDIA "video"

#define DEFAULT_RTP_AUDIO_BASE_PAYLOAD 0
#define DEFAULT_RTP_VIDEO_BASE_PAYLOAD 24

/* Table extracted from rfc3551 [6] */
static gchar *rtpmaps[] = {
  /* Payload types (PT) for audio encodings */
  "PCMU/8000",
  NULL,                         /* reserved */
  NULL,                         /* reserved */
  "GSM/8000",
  "G723/8000",
  "DVI4/8000",
  "DVI4/16000",
  "LPC/8000",
  "PCMA/8000",
  "G722/8000",
  "L16/44100/2",
  "L16/44100",
  "QCELP/8000",
  "CN/8000",
  "MPA/90000",
  "G728/8000",
  "DVI4/11025",
  "DVI4/22050",
  "G729/8000",
  NULL,                         /* reserved */
  NULL,                         /* unasigned */
  NULL,                         /* unasigned */
  NULL,                         /* unasigned */
  NULL,                         /* unasigned */

  /* Payload types (PT) for video encodings */
  NULL,                         /* unasigned */
  "CelB/90000",
  "JPEG/90000",
  NULL,                         /* unasigned */
  "nv/90000",
  NULL,                         /* unasigned */
  NULL,                         /* unasigned */
  "H261/90000",
  "MPV/90000",
  "MP2T/90000",
  "H263/90000",
};

typedef struct _KmsSdpRtpMap KmsSdpRtpMap;
struct _KmsSdpRtpMap
{
  guint payload;
  gchar *name;
  GSList *fmtps;                /* list of GstSDPAttributes */
};

static KmsSdpRtpMap *
kms_sdp_rtp_map_new (guint payload, const gchar * name)
{
  KmsSdpRtpMap *rtpmap;

  rtpmap = g_slice_new0 (KmsSdpRtpMap);
  rtpmap->payload = payload;
  rtpmap->name = g_strdup (name);

  return rtpmap;
}

static void
kms_sdp_attribute_destroy (GstSDPAttribute * attr)
{
  gst_sdp_attribute_clear (attr);

  g_slice_free (GstSDPAttribute, attr);
}

static void
kms_sdp_rtp_map_destroy (KmsSdpRtpMap * rtpmap)
{
  g_free (rtpmap->name);
  g_slist_free_full (rtpmap->fmtps, (GDestroyNotify) kms_sdp_attribute_destroy);

  g_slice_free (KmsSdpRtpMap, rtpmap);
}

static void
kms_sdp_rtp_map_destroy_pointer (gpointer rtpmap)
{
  kms_sdp_rtp_map_destroy ((KmsSdpRtpMap *) rtpmap);
}

static gboolean
cmp_static_payload (const gchar * enc, const gchar * static_pt)
{
  const gchar *cmp;
  gchar *substr = NULL;
  gboolean ret;

  if (static_pt == NULL || !g_str_has_prefix (enc, static_pt)) {
    return FALSE;
  }

  /* For audio streams, <encoding parameters> indicates the number */
  /* of audio channels.  This parameter is OPTIONAL and may be     */
  /* omitted if the number of channels is one, provided that no    */
  /* additional parameters are needed. [rfc4566] section 6.        */

  if (g_str_has_suffix (enc, "/1")) {
    substr = g_strndup (enc, strlen (enc) - 2);
    cmp = substr;
  } else {
    cmp = enc;
  }

  ret = g_strcmp0 (cmp, static_pt) == 0;
  g_free (substr);

  return ret;
}

static gint
get_static_payload_for_codec_name (const gchar * name)
{
  guint i;

  for (i = 0; i < G_N_ELEMENTS (rtpmaps); i++) {
    if (cmp_static_payload (name, rtpmaps[i])) {
      return i;
    }
  }

  return -1;
}

static KmsSdpRtpMap *
kms_sdp_rtp_map_create_for_codec (KmsSdpRtpAvpMediaHandler * self,
    const gchar * name, GError ** error)
{
  KmsSdpRtpMap *rtpmap = NULL;
  gint payload;

  payload = get_static_payload_for_codec_name (name);

  if (payload >= 0) {
    return kms_sdp_rtp_map_new (payload, name);
  }

  if (self->priv->ptmanager == NULL) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR,
        "Media handler not configured to assign dynamic payload types");
    return NULL;
  }

  payload = kms_i_sdp_payload_manager_get_dynamic_pt (self->priv->ptmanager,
      name, error);

  if (payload >= 0) {
    rtpmap = kms_sdp_rtp_map_new (payload, name);
  }

  return rtpmap;
}

static GObject *
kms_sdp_rtp_avp_media_handler_constructor (GType gtype, guint n_properties,
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
        g_value_set_string (property->value, SDP_MEDIA_RTP_AVP_PROTO);
      }
    }
  }

  object =
      G_OBJECT_CLASS (parent_class)->constructor (gtype, n_properties,
      properties);

  return object;
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_supported_fmts (KmsSdpRtpAvpMediaHandler *
    self, GstSDPMedia * media, GError ** error)
{
  GSList *item = NULL;
  gboolean is_audio;

  if (g_strcmp0 (gst_sdp_media_get_media (media), SDP_AUDIO_MEDIA) == 0) {
    item = self->priv->audio_fmts;
    is_audio = TRUE;
  } else if (g_strcmp0 (gst_sdp_media_get_media (media), SDP_VIDEO_MEDIA) == 0) {
    item = self->priv->video_fmts;
    is_audio = FALSE;
  } else {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Unsuported media '%s'", gst_sdp_media_get_media (media));
    return FALSE;
  }

  while (item != NULL) {
    KmsSdpRtpMap *rtpmap = item->data;
    gchar *fmt;

    /* Make some checks for default PTs */
    if (rtpmap->payload >= DEFAULT_RTP_AUDIO_BASE_PAYLOAD &&
        rtpmap->payload <= G_N_ELEMENTS (rtpmaps)) {
      if (rtpmaps[rtpmap->payload] == NULL) {
        g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
            "Trying to use an invalid PT (%d)", rtpmap->payload);
      } else if (is_audio && rtpmap->payload >= DEFAULT_RTP_VIDEO_BASE_PAYLOAD) {
        g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
            "Trying to use a reserved video payload type for audio (%d)",
            rtpmap->payload);
        return FALSE;
      } else if (!is_audio && rtpmap->payload < DEFAULT_RTP_VIDEO_BASE_PAYLOAD) {
        g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
            "Trying to use a reserved audio payload type for video (%d)",
            rtpmap->payload);
        return FALSE;
      } else {
        gchar **codec;
        gboolean ret;

        codec = g_strsplit (rtpmap->name, "/", 0);

        ret = g_str_has_prefix (rtpmaps[rtpmap->payload], codec[0]);
        g_strfreev (codec);

        if (!ret) {
          g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
              "Trying to use a reserved payload (%d) for '%s'",
              rtpmap->payload, rtpmap->name);
          return FALSE;
        }
      }
    }

    fmt = g_strdup_printf ("%u", rtpmap->payload);

    if (gst_sdp_media_add_format (media, fmt) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Can not set format (%u)", rtpmap->payload);
      g_free (fmt);
      return FALSE;
    }

    g_free (fmt);
    item = g_slist_next (item);
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_extmaps (KmsSdpRtpAvpMediaHandler *
    self, GstSDPMedia * media, GError ** error)
{
  GHashTableIter iter;
  gpointer key, value;

  g_hash_table_iter_init (&iter, self->priv->extmaps);
  while (g_hash_table_iter_next (&iter, &key, &value)) {
    guint8 id = GPOINTER_TO_UINT (key);
    const gchar *uri = (const gchar *) value;
    gchar *attr;

    attr = g_strdup_printf ("%" G_GUINT32_FORMAT " %s", id, uri);
    if (gst_sdp_media_add_attribute (media, "extmap", attr) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Can not to set attribute 'rtpmap:%s'", attr);
      g_free (attr);
      return FALSE;
    }
    g_free (attr);
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_fmtp_attrs (KmsSdpRtpAvpMediaHandler * self,
    const KmsSdpRtpMap * rtpmap, GstSDPMedia * media, GError ** error)
{
  GSList *item = NULL;

  for (item = rtpmap->fmtps; item != NULL; item = g_slist_next (item)) {
    GstSDPAttribute *fmtp = item->data;

    if (gst_sdp_media_add_attribute (media, fmtp->key,
            fmtp->value) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Can not to set attribute '%s:%s'", fmtp->key, fmtp->value);
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_rtpmap_attrs (KmsSdpRtpAvpMediaHandler * self,
    GstSDPMedia * media, GError ** error)
{
  GSList *fmts = NULL;
  gboolean omit;
  guint i;

  if (g_strcmp0 (gst_sdp_media_get_media (media), SDP_AUDIO_MEDIA) == 0) {
    fmts = self->priv->audio_fmts;
  } else if (g_strcmp0 (gst_sdp_media_get_media (media), SDP_VIDEO_MEDIA) == 0) {
    fmts = self->priv->video_fmts;
  } else {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Unsuported media '%s'", gst_sdp_media_get_media (media));
    return FALSE;
  }

  for (i = 0; i < media->fmts->len; i++) {
    gchar *payload;
    GSList *item = NULL;
    guint pt;

    payload = g_array_index (media->fmts, gchar *, i);
    pt = atoi (payload);

    /* [rfc4566] rtpmap attribute can be omitted for static payload type  */
    /* numbers so it is completely defined in the RTP Audio/Video profile */
    omit = pt >= DEFAULT_RTP_AUDIO_BASE_PAYLOAD && pt <= G_N_ELEMENTS (rtpmaps);

    for (item = fmts; item != NULL; item = g_slist_next (item)) {
      KmsSdpRtpMap *rtpmap = item->data;

      if (pt != rtpmap->payload) {
        continue;
      }

      if (!omit) {
        gchar *attr;

        attr = g_strdup_printf ("%u %s", rtpmap->payload, rtpmap->name);

        if (gst_sdp_media_add_attribute (media, "rtpmap", attr) != GST_SDP_OK) {
          /* Add rtpmap attribute */
          g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
              "Can not to set attribute 'rtpmap:%s'", attr);
          g_free (attr);
          return FALSE;
        }

        g_free (attr);
      }

      /* Add fmtp attributes */
      if (!kms_sdp_rtp_avp_media_handler_add_fmtp_attrs (self, rtpmap, media,
              error)) {
        return FALSE;
      }
    }
  }

  return TRUE;
}

static GstSDPMedia *
kms_sdp_rtp_avp_media_handler_create_offer (KmsSdpMediaHandler * handler,
    const gchar * media, const GstSDPMedia * prev_offer, GError ** error)
{
  GstSDPMedia *m;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not create '%s' media", media);
    goto error;
  }

  /* Create m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->init_offer (handler, media, m,
          prev_offer, error)) {
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
kms_sdp_rtp_avp_media_handler_encoding_supported (KmsSdpRtpAvpMediaHandler *
    self, const GstSDPMedia * media, const gchar * enc, gint pt)
{
  GSList *item = NULL;

  if (g_strcmp0 (gst_sdp_media_get_media (media), SDP_AUDIO_MEDIA) == 0) {
    item = self->priv->audio_fmts;
  } else if (g_strcmp0 (gst_sdp_media_get_media (media), SDP_VIDEO_MEDIA) == 0) {
    item = self->priv->video_fmts;
  } else {
    return FALSE;
  }

  while (item != NULL) {
    KmsSdpRtpMap *rtpmap = item->data;
    gboolean supported = FALSE;

    if (rtpmap->payload >= DEFAULT_RTP_AUDIO_BASE_PAYLOAD &&
        rtpmap->payload <= G_N_ELEMENTS (rtpmaps)) {
      /* Check static payload type */
      supported = cmp_static_payload (enc, rtpmaps[rtpmap->payload]);
    } else {
      /* Check dynamic pt */
      supported = g_ascii_strcasecmp (rtpmap->name, enc) == 0;
      if (supported) {
        kms_i_sdp_payload_manager_register_dynamic_payload (self->priv->
            ptmanager, pt, rtpmap->name, NULL);
      }
    }

    if (supported) {
      return TRUE;
    } else {
      item = g_slist_next (item);
    }
  }

  return FALSE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_format_supported (KmsSdpRtpAvpMediaHandler * self,
    const GstSDPMedia * media, const gchar * fmt)
{
  const gchar *val;
  gchar **attrs;
  gboolean ret;
  gint pt;

  val = sdp_utils_get_attr_map_value (media, "rtpmap", fmt);
  pt = atoi (fmt);

  if (val == NULL) {
    /* Check if this is a static payload type so they do not need to be */
    /* set in an rtpmap attribute */

    if (pt >= 0 && pt <= G_N_ELEMENTS (rtpmaps) && rtpmaps[pt] != NULL) {
      return kms_sdp_rtp_avp_media_handler_encoding_supported (self, media,
          rtpmaps[pt], pt);
    } else {
      return FALSE;
    }
  }

  attrs = g_strsplit (val, " ", 0);
  ret =
      kms_sdp_rtp_avp_media_handler_encoding_supported (self, media,
      attrs[1] /* encoding */ , pt);
  g_strfreev (attrs);

  return ret;
}

static gboolean
    kms_sdp_rtp_avp_media_handler_add_supported_extmaps
    (KmsSdpRtpAvpMediaHandler * self, const GstSDPMedia * offer,
    GstSDPMedia * answer, GError ** error)
{
  guint a;

  for (a = 0;; a++) {
    const gchar *attr;
    GHashTableIter iter;
    gpointer key, value;
    gchar **tokens;
    const gchar *offer_uri;

    attr = gst_sdp_media_get_attribute_val_n (offer, "extmap", a);
    if (attr == NULL) {
      return TRUE;
    }

    tokens = g_strsplit (attr, " ", 0);
    offer_uri = tokens[1];
    if (offer_uri == NULL) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Offer with wrong extmap '%s'", attr);
      g_strfreev (tokens);
      return FALSE;
    }

    g_hash_table_iter_init (&iter, self->priv->extmaps);
    while (g_hash_table_iter_next (&iter, &key, &value)) {
      const gchar *uri = (const gchar *) value;

      if (g_strcmp0 (offer_uri, uri) != 0) {
        continue;
      }

      if (gst_sdp_media_add_attribute (answer, "extmap", attr) != GST_SDP_OK) {
        g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
            "Can not to set attribute 'rtpmap:%s'", attr);
        g_strfreev (tokens);
        return FALSE;
      }
    }

    g_strfreev (tokens);
  }
}

static gboolean
    kms_sdp_rtp_avp_media_handler_add_supported_rtpmap_attrs
    (KmsSdpRtpAvpMediaHandler * self, const GstSDPMedia * offer,
    GstSDPMedia * answer, GError ** error)
{
  guint i, len;

  len = gst_sdp_media_formats_len (answer);

  for (i = 0; i < len; i++) {
    const gchar *fmt, *val;

    if (gst_sdp_media_get_port (answer) == 0) {
      // No need to include rtpmap attributes for rejected media.
      continue;
    }

    fmt = gst_sdp_media_get_format (answer, i);
    val = sdp_utils_get_attr_map_value (offer, "rtpmap", fmt);

    if (val == NULL) {
      gint pt;

      /* Check if this is a static payload type so they do not need to be */
      /* set in an rtpmap attribute */

      pt = atoi (fmt);
      if (pt >= 0 && pt <= G_N_ELEMENTS (rtpmaps) && rtpmaps[pt] != NULL) {
        if (kms_sdp_rtp_avp_media_handler_encoding_supported (self, offer,
                rtpmaps[pt], pt)) {
          /* Static payload do not need to be set as rtpmap attribute */
          continue;
        } else {
          GST_DEBUG ("No static payload '%s' supported", fmt);
          return FALSE;
        }
      } else {
        GST_DEBUG ("Not 'rtpmap:%s' attribute found in offer", fmt);
        return FALSE;
      }
    }

    if (gst_sdp_media_add_attribute (answer, "rtpmap", val) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Can not add attribute 'rtpmap:%s'", val);
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_can_insert_attribute (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  if (g_strcmp0 (attr->key, "rtpmap") == 0 ||
      g_strcmp0 (attr->key, "extmap") == 0) {
    /* ignore */
    return FALSE;
  }

  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->can_insert_attribute
      (handler, offer, attr, answer, msg)) {
    return FALSE;
  }

  return TRUE;
}

GstSDPMedia *
kms_sdp_rtp_avp_media_handler_create_answer (KmsSdpMediaHandler * handler,
    const GstSDPMessage * msg, const GstSDPMedia * offer, GError ** error)
{
  GstSDPMedia *m = NULL;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not create '%s' media answer", gst_sdp_media_get_media (offer));
    goto error;
  }

  /* Create m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->init_answer (handler, offer,
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

struct intersect_data
{
  KmsSdpMediaHandler *handler;
  const GstSDPMedia *offer;
  GstSDPMedia *answer;
  const GstSDPMessage *msg;
};

static gboolean
instersect_rtp_avp_media_attr (const GstSDPAttribute * attr, gpointer user_data)
{
  struct intersect_data *data = (struct intersect_data *) user_data;

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
kms_sdp_rtp_avp_media_handler_intersect_sdp_medias (KmsSdpMediaHandler *
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
          instersect_rtp_avp_media_attr, &data)) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR, "Can not intersect media attributes");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_is_valid_media (const gchar * media)
{
  return g_strcmp0 (media, SDP_AUDIO_MEDIA) == 0 ||
      g_strcmp0 (media, SDP_VIDEO_MEDIA) == 0;
}

static gboolean
kms_sdp_rtp_avp_media_handler_init_new_offer (KmsSdpMediaHandler * handler,
    const gchar * media, GstSDPMedia * offer, GError ** error)
{
  gchar *proto = NULL;
  gboolean ret = TRUE;

  if (!kms_sdp_rtp_avp_media_handler_is_valid_media (media)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_MEDIA,
        "Unsupported '%s' media", media);
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_set_media (offer, media) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set '%s' media", media);
    ret = FALSE;
    goto end;
  }

  g_object_get (handler, "proto", &proto, NULL);

  if (gst_sdp_media_set_proto (offer, proto) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set '%s' protocol", SDP_MEDIA_RTP_AVP_PROTO);
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_set_port_info (offer, 1, 1) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set port");
    ret = FALSE;
    goto end;
  }

  // RFC 5763 says:
  // > The endpoint that is the offerer MUST use the setup attribute
  // > value of setup:actpass
  if (gst_sdp_media_add_attribute (offer, "setup", "actpass") != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set attribute 'setup:actpass'");
    ret = FALSE;
    goto end;
  }

end:
  g_free (proto);

  return ret;
}

static gboolean
kms_sdp_rtp_avp_media_handler_init_renegotiated_offer (KmsSdpMediaHandler *
    handler, const gchar * media, GstSDPMedia * offer,
    const GstSDPMedia * prev_offer, GError ** error)
{
  const gchar *m = gst_sdp_media_get_media (prev_offer);
  const gchar *proto;
  guint port, num_ports;

  if (g_strcmp0 (media, m) != 0) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_MEDIA,
        "Previous offer has different media");
    return FALSE;
  }

  if (!kms_sdp_rtp_avp_media_handler_is_valid_media (m)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_MEDIA,
        "Unsupported '%s' media", media);
    return FALSE;
  }

  proto = gst_sdp_media_get_proto (prev_offer);

  if (!kms_sdp_media_handler_manage_protocol (handler, proto)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PROTOCOL,
        "Unexpected media protocol '%s'", gst_sdp_media_get_proto (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_media (offer, m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set '%s' media", media);
    return FALSE;
  }

  if (gst_sdp_media_set_proto (offer, proto) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set '%s' protocol", SDP_MEDIA_RTP_AVP_PROTO);
    return FALSE;
  }

  port = gst_sdp_media_get_port (prev_offer);
  num_ports = gst_sdp_media_get_num_ports (prev_offer);

  if (gst_sdp_media_set_port_info (offer, port, num_ports) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not set port");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_init_offer (KmsSdpMediaHandler * handler,
    const gchar * media, GstSDPMedia * offer, const GstSDPMedia * prev_offer,
    GError ** error)
{
  if (prev_offer == NULL) {
    return kms_sdp_rtp_avp_media_handler_init_new_offer (handler, media, offer,
        error);
  } else {
    return kms_sdp_rtp_avp_media_handler_init_renegotiated_offer (handler,
        media, offer, prev_offer, error);
  }
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_new_offer_attributes (KmsSdpRtpAvpMediaHandler
    * self, GstSDPMedia * offer, GError ** error)
{
  if (!kms_sdp_rtp_avp_media_handler_add_supported_fmts (self, offer, error)) {
    return FALSE;
  }

  if (!kms_sdp_rtp_avp_media_handler_add_extmaps (self, offer, error)) {
    return FALSE;
  }

  if (!kms_sdp_rtp_avp_media_handler_add_rtpmap_attrs (self, offer, error)) {
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_set_supported_fmts (KmsSdpRtpAvpMediaHandler *
    self, const GstSDPMedia * origin, GstSDPMedia * target, GError ** error)
{
  guint i, len;

  len = gst_sdp_media_formats_len (origin);

  /* Set only supported media formats in target */
  for (i = 0; i < len; i++) {
    const gchar *fmt;

    fmt = gst_sdp_media_get_format (origin, i);

    if (!kms_sdp_rtp_avp_media_handler_format_supported (self, origin, fmt)) {
      continue;
    }

    if (gst_sdp_media_add_format (target, fmt) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Can add format '%s'", fmt);
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_supported_fmtp (KmsSdpRtpAvpMediaHandler *
    self, const GstSDPMedia * prev_offer, GstSDPMedia * offer, GError ** error)
{
  guint i, len;

  len = gst_sdp_media_formats_len (offer);

  for (i = 0; i < len; i++) {
    const gchar *payload;
    const gchar *fmtp;

    payload = gst_sdp_media_get_format (prev_offer, i);

    if (payload == NULL) {
      g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
          SDP_AGENT_UNEXPECTED_ERROR, "Can not add payloads to the offer");
      return FALSE;
    }

    fmtp = sdp_utils_sdp_media_get_fmtp (prev_offer, payload);

    if (fmtp == NULL) {
      continue;
    }

    if (gst_sdp_media_add_attribute (offer, "fmtp", fmtp) != GST_SDP_OK) {
      g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
          SDP_AGENT_UNEXPECTED_ERROR, "Can not add fmtp attribute to offer");
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
    kms_sdp_rtp_avp_media_handler_add_negotiated_offer_attributes
    (KmsSdpRtpAvpMediaHandler * self, GstSDPMedia * offer,
    const GstSDPMedia * prev_offer, GError ** error)
{
  guint port, num_ports;

  if (!kms_sdp_rtp_avp_media_handler_set_supported_fmts (self, prev_offer,
          offer, error)) {
    return FALSE;
  }

  if (gst_sdp_media_formats_len (offer) > 0) {
    port = gst_sdp_media_get_port (prev_offer);
    num_ports = gst_sdp_media_get_num_ports (prev_offer);
  } else {
    GST_WARNING_OBJECT (self, "Rejecting media: '%s'",
        gst_sdp_media_get_media (offer));
    /* Disable media */
    port = 0;
    num_ports = 1;
  }

  if (gst_sdp_media_set_port_info (offer, port, num_ports) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Can not set port attribute");
    return FALSE;
  }

  if (!kms_sdp_rtp_avp_media_handler_add_supported_extmaps (self, prev_offer,
          offer, error)) {
    return FALSE;
  }

  if (!kms_sdp_rtp_avp_media_handler_add_supported_rtpmap_attrs (self,
          prev_offer, offer, error)) {
    return FALSE;
  }

  return kms_sdp_rtp_avp_media_handler_add_supported_fmtp (self, prev_offer,
      offer, error);
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_offer_attributes (KmsSdpMediaHandler *
    handler, GstSDPMedia * offer, const GstSDPMedia * prev_offer,
    GError ** error)
{
  KmsSdpRtpAvpMediaHandler *self = KMS_SDP_RTP_AVP_MEDIA_HANDLER (handler);

  if (prev_offer == NULL) {
    if (!kms_sdp_rtp_avp_media_handler_add_new_offer_attributes (self, offer,
            error)) {
      return FALSE;
    }
  } else {
    if (!kms_sdp_rtp_avp_media_handler_add_negotiated_offer_attributes (self,
            offer, prev_offer, error)) {
      return FALSE;
    }
  }

  /* Chain up */
  return
      KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_offer_attributes (handler,
      offer, prev_offer, error);
}

static gboolean
kms_sdp_rtp_avp_media_handler_init_answer (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  const gchar *proto;

  if (g_strcmp0 (gst_sdp_media_get_media (offer), SDP_AUDIO_MEDIA) != 0
      && g_strcmp0 (gst_sdp_media_get_media (offer), SDP_VIDEO_MEDIA) != 0) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_MEDIA,
        "Unsupported '%s' media", gst_sdp_media_get_media (offer));
    return FALSE;
  }

  proto = gst_sdp_media_get_proto (offer);

  if (!kms_sdp_media_handler_manage_protocol (handler, proto)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PROTOCOL,
        "Unexpected media protocol '%s'", gst_sdp_media_get_proto (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_media (answer,
          gst_sdp_media_get_media (offer)) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Can not set '%s' media attribute", gst_sdp_media_get_media (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_proto (answer, proto) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Can not set proto '%s' attribute", proto);
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_rtp_avp_media_handler_add_answer_attributes_impl (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  KmsSdpRtpAvpMediaHandler *self = KMS_SDP_RTP_AVP_MEDIA_HANDLER (handler);
  guint i, len, port;

  len = gst_sdp_media_formats_len (offer);

  /* Set only supported media formats in answer */
  for (i = 0; i < len; i++) {
    const gchar *fmt;

    fmt = gst_sdp_media_get_format (offer, i);

    if (!kms_sdp_rtp_avp_media_handler_format_supported (self, offer, fmt)) {
      GST_DEBUG_OBJECT (self, "Unsupported media format '%s'", fmt);
      continue;
    }

    if (gst_sdp_media_add_format (answer, fmt) != GST_SDP_OK) {
      g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
          "Error adding media format '%s'", fmt);
      return FALSE;
    }
  }

  if (gst_sdp_media_formats_len (answer) > 0) {
    port = 1;
  } else {
    // Reject media.
    GST_WARNING_OBJECT (self,
        "Setting SDP Answer port to 0 (reject media) because SDP Offer didn't contain any supported media formats");
    port = 0;

    // Add the first format from the offer, to conform with the spec:
    // https://datatracker.ietf.org/doc/html/rfc3264#section-6
    // > To reject an offered stream, the port number in the corresponding stream in
    // > the answer MUST be set to zero. At least one MUST be present, as specified
    // > by SDP.
    const gchar *fmt = gst_sdp_media_get_format (offer, 0);
    if (fmt != NULL) {
      // Add the first format, if the SDP Offer had one to start with.
      if (gst_sdp_media_add_format (answer, fmt) != GST_SDP_OK) {
        g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
            "Error adding media format '%s'", fmt);
        return FALSE;
      }
    }
  }

  if (gst_sdp_media_set_port_info (answer, port, 1) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Can not set port attribute");
    return FALSE;
  }

  if (!kms_sdp_rtp_avp_media_handler_add_supported_extmaps (self, offer,
          answer, error)) {
    return FALSE;
  }

  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_answer_attributes
      (handler, offer, answer, error)) {
    return FALSE;
  }

  return kms_sdp_rtp_avp_media_handler_add_supported_rtpmap_attrs (self, offer,
      answer, error);
}

static void
kms_sdp_rtp_avp_media_handler_finalize (GObject * object)
{
  KmsSdpRtpAvpMediaHandler *self = KMS_SDP_RTP_AVP_MEDIA_HANDLER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_hash_table_unref (self->priv->extmaps);

  g_clear_object (&self->priv->ptmanager);

  g_slist_free_full (self->priv->audio_fmts, kms_sdp_rtp_map_destroy_pointer);
  g_slist_free_full (self->priv->video_fmts, kms_sdp_rtp_map_destroy_pointer);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_sdp_rtp_avp_media_handler_class_init (KmsSdpRtpAvpMediaHandlerClass * klass)
{
  GObjectClass *gobject_class;
  KmsSdpMediaHandlerClass *handler_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->constructor = kms_sdp_rtp_avp_media_handler_constructor;
  gobject_class->finalize = kms_sdp_rtp_avp_media_handler_finalize;

  handler_class = KMS_SDP_MEDIA_HANDLER_CLASS (klass);
  handler_class->create_offer = kms_sdp_rtp_avp_media_handler_create_offer;
  handler_class->create_answer = kms_sdp_rtp_avp_media_handler_create_answer;

  handler_class->can_insert_attribute =
      kms_sdp_rtp_avp_media_handler_can_insert_attribute;
  handler_class->intersect_sdp_medias =
      kms_sdp_rtp_avp_media_handler_intersect_sdp_medias;

  handler_class->init_offer = kms_sdp_rtp_avp_media_handler_init_offer;
  handler_class->add_offer_attributes =
      kms_sdp_rtp_avp_media_handler_add_offer_attributes;

  handler_class->init_answer = kms_sdp_rtp_avp_media_handler_init_answer;
  handler_class->add_answer_attributes =
      kms_sdp_rtp_avp_media_handler_add_answer_attributes_impl;

  g_type_class_add_private (klass, sizeof (KmsSdpRtpAvpMediaHandlerPrivate));
}

static void
kms_sdp_rtp_avp_media_handler_init (KmsSdpRtpAvpMediaHandler * self)
{
  self->priv = KMS_SDP_RTP_AVP_MEDIA_HANDLER_GET_PRIVATE (self);

  self->priv->extmaps =
      g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL, g_free);
}

KmsSdpRtpAvpMediaHandler *
kms_sdp_rtp_avp_media_handler_new ()
{
  KmsSdpRtpAvpMediaHandler *handler;

  handler =
      KMS_SDP_RTP_AVP_MEDIA_HANDLER (g_object_new
      (KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER, NULL));

  return handler;
}

gboolean
kms_sdp_rtp_avp_media_handler_add_extmap (KmsSdpRtpAvpMediaHandler * self,
    guint8 id, const gchar * uri, GError ** error)
{

  if (g_hash_table_contains (self->priv->extmaps, GUINT_TO_POINTER (id))) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Trying to add existing extmap id '%" G_GUINT32_FORMAT "'", id);
    return FALSE;
  }

  g_hash_table_insert (self->priv->extmaps, GUINT_TO_POINTER (id),
      g_strdup (uri));

  return TRUE;
}

gboolean
kms_sdp_rtp_avp_media_handler_use_payload_manager (KmsSdpRtpAvpMediaHandler *
    self, KmsISdpPayloadManager * manager, GError ** error)
{
  if (!KMS_IS_I_SDP_PAYLOAD_MANAGER (manager)) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Object provided is not an payload manager");
    return FALSE;
  }

  g_clear_object (&self->priv->ptmanager);

  /* take ownership */
  self->priv->ptmanager = manager;

  return TRUE;
}

static gboolean
is_codec_used (GSList * rtpmaps, const gchar * name)
{
  GSList *l;

  for (l = rtpmaps; l != NULL; l = l->next) {
    KmsSdpRtpMap *rtpmap = l->data;

    if (g_strcmp0 (rtpmap->name, name) == 0) {
      return TRUE;
    }
  }

  return FALSE;
}

static gint
kms_sdp_rtp_avp_media_handler_add_codec (KmsSdpRtpAvpMediaHandler * self,
    const gchar * media, const gchar * name, GError ** error)
{
  KmsSdpRtpMap *rtpmap;
  GSList **fmts;

  if (g_strcmp0 (media, SDP_AUDIO_MEDIA) == 0) {
    fmts = &self->priv->audio_fmts;
  } else if (g_strcmp0 (media, SDP_VIDEO_MEDIA) == 0) {
    fmts = &self->priv->video_fmts;
  } else {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Unsuported media '%s'", media);
    return -1;
  }

  if (is_codec_used (*fmts, name)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Codec %s is already used", name);
    return -1;
  }

  GST_DEBUG_OBJECT (self, "Add format support, media: %s, codec: %s",
      media, name);

  rtpmap = kms_sdp_rtp_map_create_for_codec (self, name, error);

  if (rtpmap == NULL) {
    return -1;
  }

  *fmts = g_slist_append (*fmts, rtpmap);

  // RFC 6184 - RTP Payload Format for H.264 Video.
  // Add format description to enhace compatibility.
  // Tested: Firefox 80 requires "profile-level-id=42e01f", which means
  //         Constrained Baseline profile, Level 3.1.
  if (strstr (name, "H264") != NULL) {
    kms_sdp_rtp_avp_media_handler_add_fmtp (self, rtpmap->payload,
        "level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f",
        NULL);
  }

  return rtpmap->payload;
}

gint
kms_sdp_rtp_avp_media_handler_add_audio_codec (KmsSdpRtpAvpMediaHandler * self,
    const gchar * name, GError ** error)
{
  return kms_sdp_rtp_avp_media_handler_add_codec (self, SDP_AUDIO_MEDIA, name,
      error);
}

gint
kms_sdp_rtp_avp_media_handler_add_video_codec (KmsSdpRtpAvpMediaHandler * self,
    const gchar * name, GError ** error)
{
  return kms_sdp_rtp_avp_media_handler_add_codec (self, SDP_VIDEO_MEDIA, name,
      error);
}

gint kms_sdp_rtp_avp_media_handler_add_generic_audio_payload
    (KmsSdpRtpAvpMediaHandler * self, const gchar * format, GError ** error)
{
  return kms_sdp_rtp_avp_media_handler_add_codec (self, SDP_AUDIO_MEDIA, format,
      error);
}

gint kms_sdp_rtp_avp_media_handler_add_generic_video_payload
    (KmsSdpRtpAvpMediaHandler * self, const gchar * format, GError ** error)
{
  return kms_sdp_rtp_avp_media_handler_add_codec (self, SDP_VIDEO_MEDIA, format,
      error);
}

static gint
find_rtpmap_payload (KmsSdpRtpMap * rtpmap, guint * payload)
{
  return rtpmap->payload - *payload;
}

gboolean
kms_sdp_rtp_avp_media_handler_add_fmtp (KmsSdpRtpAvpMediaHandler * self,
    guint payload, const gchar * value, GError ** error)
{
  GstSDPAttribute *fmtp;
  KmsSdpRtpMap *rtpmap;
  gchar *attr;
  GSList *l;

  l = g_slist_find_custom (self->priv->audio_fmts, &payload,
      (GCompareFunc) find_rtpmap_payload);

  if (l == NULL) {
    l = g_slist_find_custom (self->priv->video_fmts, &payload,
        (GCompareFunc) find_rtpmap_payload);
  }

  if (l == NULL) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Invalid payload (%d)", payload);
    return FALSE;
  }

  fmtp = g_slice_new0 (GstSDPAttribute);

  attr = g_strdup_printf ("%u %s", payload, value);
  if (gst_sdp_attribute_set (fmtp, "fmtp", attr) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not create fmtp attribute");
    g_slice_free (GstSDPAttribute, fmtp);
    g_free (attr);
    return FALSE;
  }

  g_free (attr);

  rtpmap = l->data;
  rtpmap->fmtps = g_slist_prepend (rtpmap->fmtps, fmtp);

  return TRUE;
}
