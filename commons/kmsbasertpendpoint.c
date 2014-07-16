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

#include <stdlib.h>
#include <gst/gst.h>

#include "kms-enumtypes-commons.h"
#include "kms-marshal-commons.h"
#include "kmsbasertpendpoint.h"
#include "sdp_utils.h"

#define PLUGIN_NAME "base_rtp_endpoint"

GST_DEBUG_CATEGORY_STATIC (kms_base_rtp_endpoint_debug);
#define GST_CAT_DEFAULT kms_base_rtp_endpoint_debug

#define kms_base_rtp_endpoint_parent_class parent_class
G_DEFINE_TYPE (KmsBaseRtpEndpoint, kms_base_rtp_endpoint,
    KMS_TYPE_BASE_SDP_ENDPOINT);

#define RTPBIN "rtpbin"

#define AUDIO_SESSION 0
#define VIDEO_SESSION 1

#define KMS_BASE_RTP_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_BASE_RTP_ENDPOINT,                   \
    KmsBaseRtpEndpointPrivate                     \
  )                                               \
)

struct _KmsBaseRtpEndpointPrivate
{
  GstElement *rtpbin;

  GstElement *audio_payloader;
  GstElement *video_payloader;

  guint audio_ssrc;
  guint video_ssrc;

  gboolean negotiated;
};

/* Signals and args */
enum
{
  MEDIA_START,
  MEDIA_STOP,
  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

enum
{
  PROP_0
};

static const gchar *
get_caps_codec_name (const gchar * codec_name)
{
  // TODO: Add more special cases here

  if (g_ascii_strcasecmp (OPUS_ENCONDING_NAME, codec_name) == 0)
    return "X-GST-OPUS-DRAFT-SPITTKA-00";
  else if (g_ascii_strcasecmp (VP8_ENCONDING_NAME, codec_name) == 0)
    return "VP8-DRAFT-IETF-01";
  else
    return codec_name;
}

static GstCaps *
kms_base_rtp_endpoint_get_caps_from_rtpmap (const gchar * media,
    const gchar * pt, const gchar * rtpmap)
{
  GstCaps *caps = NULL;
  gchar **tokens;

  if (rtpmap == NULL) {
    GST_WARNING ("rtpmap is NULL");
    return NULL;
  }

  tokens = g_strsplit (rtpmap, "/", 3);

  if (tokens[0] == NULL || tokens[1] == NULL)
    goto end;

  caps = gst_caps_new_simple ("application/x-rtp",
      "media", G_TYPE_STRING, media,
      "payload", G_TYPE_INT, atoi (pt),
      "clock-rate", G_TYPE_INT, atoi (tokens[1]),
      "encoding-name", G_TYPE_STRING, get_caps_codec_name (tokens[0]), NULL);

end:
  g_strfreev (tokens);

  return caps;
}

static GstElement *
gst_base_rtp_get_payloader_for_caps (GstCaps * caps)
{
  GstElementFactory *factory;
  GstElement *payloader = NULL;
  GList *payloader_list, *filtered_list;
  GParamSpec *pspec;

  payloader_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_PAYLOADER,
      GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (payloader_list, caps, GST_PAD_SRC,
      FALSE);

  if (filtered_list == NULL)
    goto end;

  factory = GST_ELEMENT_FACTORY (filtered_list->data);
  if (factory == NULL)
    goto end;

  payloader = gst_element_factory_create (factory, NULL);

  pspec = g_object_class_find_property (G_OBJECT_GET_CLASS (payloader), "pt");
  if (pspec != NULL && G_PARAM_SPEC_VALUE_TYPE (pspec) == G_TYPE_UINT) {
    GstStructure *st = gst_caps_get_structure (caps, 0);
    gint payload;

    if (gst_structure_get_int (st, "payload", &payload))
      g_object_set (payloader, "pt", payload, NULL);
  }

  pspec =
      g_object_class_find_property (G_OBJECT_GET_CLASS (payloader),
      "config-interval");
  if (pspec != NULL && G_PARAM_SPEC_VALUE_TYPE (pspec) == G_TYPE_UINT) {
    g_object_set (payloader, "config-interval", 1, NULL);
  }

end:
  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (payloader_list);

  return payloader;
}

static GstElement *
gst_base_rtp_get_depayloader_for_caps (GstCaps * caps)
{
  GstElementFactory *factory;
  GstElement *depayloader = NULL;
  GList *payloader_list, *filtered_list;

  payloader_list =
      gst_element_factory_list_get_elements
      (GST_ELEMENT_FACTORY_TYPE_DEPAYLOADER, GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (payloader_list, caps, GST_PAD_SINK,
      FALSE);

  if (filtered_list == NULL)
    goto end;

  factory = GST_ELEMENT_FACTORY (filtered_list->data);
  if (factory == NULL)
    goto end;

  depayloader = gst_element_factory_create (factory, NULL);

end:
  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (payloader_list);

  return depayloader;
}

static void
kms_base_rtp_endpoint_connect_valve_to_payloader (KmsBaseRtpEndpoint * ep,
    GstElement * valve, GstElement * payloader, const gchar * rtpbin_pad_name)
{
  GstElement *rtprtxqueue = gst_element_factory_make ("rtprtxqueue", NULL);

  g_object_ref (payloader);
  gst_bin_add_many (GST_BIN (ep), payloader, rtprtxqueue, NULL);
  gst_element_sync_state_with_parent (payloader);
  gst_element_sync_state_with_parent (rtprtxqueue);

  gst_element_link_many (valve, payloader, rtprtxqueue, NULL);
  gst_element_link_pads (rtprtxqueue, "src", ep->priv->rtpbin, rtpbin_pad_name);

  g_object_set (valve, "drop", FALSE, NULL);
}

static void
kms_base_rtp_endpoint_connect_input_elements (KmsBaseSdpEndpoint *
    base_endpoint, const GstSDPMessage * answer)
{
  guint i, len;

  KMS_BASE_SDP_ENDPOINT_CLASS
      (kms_base_rtp_endpoint_parent_class)->connect_input_elements
      (base_endpoint, answer);
  GST_DEBUG ("connect_input_elements");

  if (answer == NULL) {
    GST_ERROR ("Asnwer is NULL");
    return;
  }

  len = gst_sdp_message_medias_len (answer);

  KMS_ELEMENT_LOCK (base_endpoint);
  KMS_BASE_RTP_ENDPOINT (base_endpoint)->priv->negotiated = TRUE;

  for (i = 0; i < len; i++) {
    const gchar *proto_str;
    GstElement *payloader;
    GstCaps *caps = NULL;
    const gchar *rtpmap;
    const GstSDPMedia *media = gst_sdp_message_get_media (answer, i);
    guint j, f_len;

    // TODO: Change constant RTP/AVP by a paremeter
    proto_str = gst_sdp_media_get_proto (media);
    if (g_ascii_strcasecmp ("RTP/AVP", proto_str) != 0 &&
        g_ascii_strcasecmp ("RTP/SAVPF", proto_str) != 0) {
      GST_WARNING ("Proto \"%s\" not supported", proto_str);
      continue;
    }

    f_len = gst_sdp_media_formats_len (media);

    for (j = 0; j < f_len && caps == NULL; j++) {
      const gchar *pt = gst_sdp_media_get_format (media, j);

      rtpmap = sdp_utils_sdp_media_get_rtpmap (media, pt);
      caps =
          kms_base_rtp_endpoint_get_caps_from_rtpmap (media->media, pt, rtpmap);
    }

    if (caps == NULL)
      continue;

    GST_DEBUG ("Found caps: %" GST_PTR_FORMAT, caps);

    payloader = gst_base_rtp_get_payloader_for_caps (caps);
    if (payloader != NULL) {
      KmsElement *element = KMS_ELEMENT (base_endpoint);
      KmsBaseRtpEndpoint *rtp_endpoint = KMS_BASE_RTP_ENDPOINT (base_endpoint);
      const gchar *rtpbin_pad_name;
      GstElement *valve = NULL;

      GST_DEBUG ("Found payloader %" GST_PTR_FORMAT, payloader);
      if (g_strcmp0 ("audio", gst_sdp_media_get_media (media)) == 0) {
        rtp_endpoint->priv->audio_payloader = payloader;
        valve = kms_element_get_audio_valve (element);
        rtpbin_pad_name = AUDIO_RTPBIN_SEND_RTP_SINK;
      } else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0) {
        rtp_endpoint->priv->video_payloader = payloader;
        valve = kms_element_get_video_valve (element);
        rtpbin_pad_name = VIDEO_RTPBIN_SEND_RTP_SINK;
      } else {
        g_object_unref (payloader);
      }

      if (valve != NULL) {
        kms_base_rtp_endpoint_connect_valve_to_payloader (rtp_endpoint, valve,
            payloader, rtpbin_pad_name);
      }
    }

    gst_caps_unref (caps);
  }

  KMS_ELEMENT_UNLOCK (base_endpoint);
}

static GstCaps *
kms_base_rtp_endpoint_get_caps_for_pt (KmsBaseRtpEndpoint * base_rtp_endpoint,
    guint pt)
{
  KmsBaseSdpEndpoint *base_endpoint = KMS_BASE_SDP_ENDPOINT (base_rtp_endpoint);
  GstSDPMessage *answer;
  guint i, len;

  answer = base_endpoint->local_answer_sdp;
  if (answer == NULL)
    answer = base_endpoint->remote_answer_sdp;

  if (answer == NULL)
    return NULL;

  len = gst_sdp_message_medias_len (answer);

  for (i = 0; i < len; i++) {
    const gchar *proto_str;
    const gchar *rtpmap;
    const GstSDPMedia *media = gst_sdp_message_get_media (answer, i);
    guint j, f_len;

    // TODO: Change constant RTP/AVP by a paremeter
    proto_str = gst_sdp_media_get_proto (media);
    if (g_ascii_strcasecmp ("RTP/AVP", proto_str) != 0 &&
        g_ascii_strcasecmp ("RTP/SAVPF", proto_str) != 0) {
      GST_WARNING ("Proto \"%s\" not supported", proto_str);
      continue;
    }

    f_len = gst_sdp_media_formats_len (media);

    for (j = 0; j < f_len; j++) {
      GstCaps *caps;
      const gchar *payload = gst_sdp_media_get_format (media, j);

      if (atoi (payload) != pt)
        continue;

      rtpmap = sdp_utils_sdp_media_get_rtpmap (media, payload);
      caps =
          kms_base_rtp_endpoint_get_caps_from_rtpmap (media->media, payload,
          rtpmap);

      if (caps != NULL) {
        gst_caps_set_simple (caps, "rtcp-fb-x-gstreamer-fir-as-repair",
            G_TYPE_BOOLEAN, TRUE, NULL);
        return caps;
      }
    }
  }

  return NULL;
}

static GstCaps *
kms_base_rtp_endpoint_request_pt_map (GstElement * rtpbin, guint session,
    guint pt, KmsBaseRtpEndpoint * base_rtp_endpoint)
{
  GstCaps *caps;

  GST_DEBUG ("Caps request for pt: %d", pt);

  caps = kms_base_rtp_endpoint_get_caps_for_pt (base_rtp_endpoint, pt);

  if (caps != NULL)
    return caps;

  return gst_caps_new_simple ("application/x-rtp", "payload", G_TYPE_INT, pt,
      NULL);
}

static void
kms_base_rtp_endpoint_rtpbin_pad_added (GstElement * rtpbin, GstPad * pad,
    KmsBaseRtpEndpoint * rtp_endpoint)
{
  GstElement *agnostic, *depayloader;
  gboolean added = TRUE;
  KmsMediaType media;
  GstCaps *caps;

  GST_PAD_STREAM_LOCK (pad);

  if (g_str_has_prefix (GST_OBJECT_NAME (pad), "recv_rtp_src_0_")) {
    agnostic = kms_element_get_audio_agnosticbin (KMS_ELEMENT (rtp_endpoint));
    media = KMS_MEDIA_TYPE_AUDIO;
  } else if (g_str_has_prefix (GST_OBJECT_NAME (pad), "recv_rtp_src_1_")) {
    agnostic = kms_element_get_video_agnosticbin (KMS_ELEMENT (rtp_endpoint));
    media = KMS_MEDIA_TYPE_VIDEO;
  } else {
    added = FALSE;
    goto end;
  }

  caps = gst_pad_query_caps (pad, NULL);
  GST_DEBUG ("New pad: %" GST_PTR_FORMAT " for linking to %" GST_PTR_FORMAT
      " with caps %" GST_PTR_FORMAT, pad, agnostic, caps);

  depayloader = gst_base_rtp_get_depayloader_for_caps (caps);

  if (caps != NULL)
    gst_caps_unref (caps);

  if (depayloader != NULL) {
    GST_DEBUG ("Found depayloader %" GST_PTR_FORMAT, depayloader);

    gst_bin_add (GST_BIN (rtp_endpoint), depayloader);
    gst_element_sync_state_with_parent (depayloader);

    gst_element_link_pads (depayloader, "src", agnostic, "sink");
    gst_element_link_pads (rtpbin, GST_OBJECT_NAME (pad), depayloader, "sink");
  } else {
    GstElement *fake = gst_element_factory_make ("fakesink", NULL);

    gst_bin_add (GST_BIN (rtp_endpoint), fake);
    gst_element_sync_state_with_parent (fake);
    gst_element_link_pads (rtpbin, GST_OBJECT_NAME (pad), fake, "sink");
  }

end:
  GST_PAD_STREAM_UNLOCK (pad);

  if (added)
    g_signal_emit (G_OBJECT (rtp_endpoint), obj_signals[MEDIA_START], 0, media,
        TRUE);
}

static void
kms_base_rtp_endpoint_audio_valve_added (KmsElement * self, GstElement * valve)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint = KMS_BASE_RTP_ENDPOINT (self);

  KMS_ELEMENT_LOCK (self);
  if (base_rtp_endpoint->priv->negotiated)
    kms_base_rtp_endpoint_connect_valve_to_payloader (base_rtp_endpoint,
        valve, base_rtp_endpoint->priv->audio_payloader,
        AUDIO_RTPBIN_SEND_RTP_SINK);
  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_base_rtp_endpoint_audio_valve_removed (KmsElement * self,
    GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
}

static void
kms_base_rtp_endpoint_video_valve_added (KmsElement * self, GstElement * valve)
{
  KmsBaseRtpEndpoint *base_rtp_endpoint = KMS_BASE_RTP_ENDPOINT (self);

  KMS_ELEMENT_LOCK (self);
  if (base_rtp_endpoint->priv->negotiated)
    kms_base_rtp_endpoint_connect_valve_to_payloader (base_rtp_endpoint,
        valve, base_rtp_endpoint->priv->video_payloader,
        VIDEO_RTPBIN_SEND_RTP_SINK);
  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_base_rtp_endpoint_video_valve_removed (KmsElement * self,
    GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
}

static void
kms_base_rtp_endpoint_stop_signal (KmsBaseRtpEndpoint * self, guint session,
    guint ssrc)
{
  gboolean local = TRUE;
  KmsMediaType media;

  KMS_ELEMENT_LOCK (self);

  if (ssrc == self->priv->audio_ssrc || ssrc == self->priv->video_ssrc) {
    local = FALSE;

    if (self->priv->audio_ssrc == ssrc)
      self->priv->audio_ssrc = 0;
    else if (self->priv->video_ssrc == ssrc)
      self->priv->video_ssrc = 0;
  }

  KMS_ELEMENT_UNLOCK (self);

  switch (session) {
    case AUDIO_SESSION:
      media = KMS_MEDIA_TYPE_AUDIO;
      break;
    case VIDEO_SESSION:
      media = KMS_MEDIA_TYPE_VIDEO;
      break;
    default:
      GST_WARNING_OBJECT (self, "No media supported for session %u", session);
      return;
  }

  g_signal_emit (G_OBJECT (self), obj_signals[MEDIA_STOP], 0, media, local);
}

static void
kms_base_rtp_endpoint_dispose (GObject * gobject)
{
  KmsBaseRtpEndpoint *self = KMS_BASE_RTP_ENDPOINT (gobject);

  if (self->priv->audio_payloader != NULL) {
    g_object_unref (self->priv->audio_payloader);
    self->priv->audio_payloader = NULL;
  }

  if (self->priv->video_payloader != NULL) {
    g_object_unref (self->priv->video_payloader);
    self->priv->video_payloader = NULL;
  }

  if (self->priv->audio_ssrc != 0) {
    kms_base_rtp_endpoint_stop_signal (self, AUDIO_SESSION,
        self->priv->audio_ssrc);
    g_signal_emit (G_OBJECT (self), obj_signals[MEDIA_STOP], 0,
        KMS_MEDIA_TYPE_AUDIO, TRUE);
  }

  if (self->priv->video_ssrc != 0) {
    kms_base_rtp_endpoint_stop_signal (self, VIDEO_SESSION,
        self->priv->video_ssrc);
    g_signal_emit (G_OBJECT (self), obj_signals[MEDIA_STOP], 0,
        KMS_MEDIA_TYPE_VIDEO, TRUE);
  }

  G_OBJECT_CLASS (kms_base_rtp_endpoint_parent_class)->dispose (gobject);
}

static void
kms_base_rtp_endpoint_class_init (KmsBaseRtpEndpointClass * klass)
{
  KmsBaseSdpEndpointClass *base_endpoint_class;
  KmsElementClass *kms_element_class;
  GstElementClass *gstelement_class;
  GObjectClass *object_class;

  object_class = G_OBJECT_CLASS (klass);
  object_class->dispose = kms_base_rtp_endpoint_dispose;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseRtpEndpoint",
      "Base/Bin/BaseRtpEndpoints",
      "Base class for RtpEndpoints",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_endpoint_class = KMS_BASE_SDP_ENDPOINT_CLASS (klass);

  base_endpoint_class->connect_input_elements =
      kms_base_rtp_endpoint_connect_input_elements;

  kms_element_class = KMS_ELEMENT_CLASS (klass);

  kms_element_class->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_base_rtp_endpoint_audio_valve_added);
  kms_element_class->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_base_rtp_endpoint_video_valve_added);
  kms_element_class->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_base_rtp_endpoint_audio_valve_removed);
  kms_element_class->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_base_rtp_endpoint_video_valve_removed);

  /* set signals */
  obj_signals[MEDIA_START] =
      g_signal_new ("media-start",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseRtpEndpointClass, media_start), NULL, NULL,
      __kms_marshal_VOID__ENUM_BOOLEAN, G_TYPE_NONE, 2, KMS_TYPE_MEDIA_TYPE,
      G_TYPE_BOOLEAN);

  obj_signals[MEDIA_STOP] =
      g_signal_new ("media-stop",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseRtpEndpointClass, media_stop), NULL, NULL,
      __kms_marshal_VOID__ENUM_BOOLEAN, G_TYPE_NONE, 2, KMS_TYPE_MEDIA_TYPE,
      G_TYPE_BOOLEAN);

  g_type_class_add_private (klass, sizeof (KmsBaseRtpEndpointPrivate));
}

static void
kms_base_rtp_endpoint_rtpbin_on_new_ssrc (GstElement * rtpbin, guint session,
    guint ssrc, gpointer user_data)
{
  KmsBaseRtpEndpoint *self = KMS_BASE_RTP_ENDPOINT (user_data);

  KMS_ELEMENT_LOCK (self);

  switch (session) {
    case AUDIO_SESSION:
      if (self->priv->audio_ssrc != 0)
        break;

      self->priv->audio_ssrc = ssrc;
      break;
    case VIDEO_SESSION:
      if (self->priv->video_ssrc != 0)
        break;

      self->priv->video_ssrc = ssrc;
      break;
    default:
      GST_WARNING_OBJECT (self, "No media supported for session %u", session);
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_base_rtp_endpoint_rtpbin_on_bye_ssrc (GstElement * rtpbin, guint session,
    guint ssrc, gpointer user_data)
{
  kms_base_rtp_endpoint_stop_signal (KMS_BASE_RTP_ENDPOINT (user_data),
      session, ssrc);
}

static void
kms_base_rtp_endpoint_rtpbin_on_bye_timeout (GstElement * rtpbin,
    guint session, guint ssrc, gpointer user_data)
{
  kms_base_rtp_endpoint_stop_signal (KMS_BASE_RTP_ENDPOINT (user_data),
      session, ssrc);
}

static void
kms_base_rtp_endpoint_rtpbin_on_ssrc_sdes (GstElement * rtpbin, guint session,
    guint ssrc, gpointer user_data)
{
  KmsBaseRtpEndpoint *self = KMS_BASE_RTP_ENDPOINT (user_data);
  KmsMediaType media;

  KMS_ELEMENT_LOCK (self);

  if (ssrc != self->priv->audio_ssrc && ssrc != self->priv->video_ssrc) {
    GST_WARNING_OBJECT (self, "SSRC %u not valid", ssrc);
    KMS_ELEMENT_UNLOCK (self);
    return;
  }

  KMS_ELEMENT_UNLOCK (self);

  switch (session) {
    case AUDIO_SESSION:
      media = KMS_MEDIA_TYPE_AUDIO;
      break;
    case VIDEO_SESSION:
      media = KMS_MEDIA_TYPE_VIDEO;
      break;
    default:
      GST_WARNING_OBJECT (self, "No media supported for session %u", session);
      return;
  }

  g_signal_emit (G_OBJECT (self), obj_signals[MEDIA_START], 0, media, FALSE);
}

static void
kms_base_rtp_endpoint_rtpbin_on_sender_timeout (GstElement * rtpbin,
    guint session, guint ssrc, gpointer user_data)
{
  kms_base_rtp_endpoint_stop_signal (KMS_BASE_RTP_ENDPOINT (user_data),
      session, ssrc);
}

static void
kms_base_rtp_endpoint_init (KmsBaseRtpEndpoint * base_rtp_endpoint)
{
  base_rtp_endpoint->priv =
      KMS_BASE_RTP_ENDPOINT_GET_PRIVATE (base_rtp_endpoint);
  base_rtp_endpoint->priv->rtpbin = gst_element_factory_make ("rtpbin", RTPBIN);

  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "request-pt-map",
      G_CALLBACK (kms_base_rtp_endpoint_request_pt_map), base_rtp_endpoint);

  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "pad-added",
      G_CALLBACK (kms_base_rtp_endpoint_rtpbin_pad_added), base_rtp_endpoint);

  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "on-new-ssrc",
      G_CALLBACK (kms_base_rtp_endpoint_rtpbin_on_new_ssrc), base_rtp_endpoint);
  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "on-ssrc-sdes",
      G_CALLBACK (kms_base_rtp_endpoint_rtpbin_on_ssrc_sdes),
      base_rtp_endpoint);
  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "on-bye-ssrc",
      G_CALLBACK (kms_base_rtp_endpoint_rtpbin_on_bye_ssrc), base_rtp_endpoint);
  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "on-bye-timeout",
      G_CALLBACK (kms_base_rtp_endpoint_rtpbin_on_bye_timeout),
      base_rtp_endpoint);
  g_signal_connect (base_rtp_endpoint->priv->rtpbin, "on-sender-timeout",
      G_CALLBACK (kms_base_rtp_endpoint_rtpbin_on_sender_timeout),
      base_rtp_endpoint);

  g_object_set (base_rtp_endpoint->priv->rtpbin, "do-lost", TRUE,
      "do-retransmission", TRUE, NULL);
  g_object_set (base_rtp_endpoint, "accept-eos", FALSE, NULL);

  gst_bin_add (GST_BIN (base_rtp_endpoint), base_rtp_endpoint->priv->rtpbin);

  base_rtp_endpoint->priv->audio_payloader = NULL;
  base_rtp_endpoint->priv->video_payloader = NULL;
  base_rtp_endpoint->priv->negotiated = FALSE;
}

GstElement *
kms_base_rtp_endpoint_get_rtpbin (KmsBaseRtpEndpoint * self)
{
  g_return_val_if_fail (KMS_IS_BASE_RTP_ENDPOINT (self), NULL);

  return self->priv->rtpbin;
}
