#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <stdlib.h>
#include <gst/gst.h>

#include "kmsbasertpendpoint.h"
#include "sdp_utils.h"

#define PLUGIN_NAME "base_rtp_endpoint"

GST_DEBUG_CATEGORY_STATIC (kms_base_rtp_end_point_debug);
#define GST_CAT_DEFAULT kms_base_rtp_end_point_debug

#define kms_base_rtp_end_point_parent_class parent_class
G_DEFINE_TYPE (KmsBaseRtpEndPoint, kms_base_rtp_end_point,
    KMS_TYPE_BASE_SDP_END_POINT);

#define RTPBIN "rtpbin"

/* Signals and args */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0
};

static const gchar *
get_caps_codec_name (const gchar * codec_name)
{
  // TODO: Add more special cases here

  if (g_ascii_strcasecmp ("OPUS", codec_name) == 0)
    return "X-GST-OPUS-DRAFT-SPITTKA-00";
  else if (g_ascii_strcasecmp ("VP8", codec_name) == 0)
    return "VP8-DRAFT-IETF-01";
  else
    return codec_name;
}

static GstCaps *
kms_base_rtp_end_point_get_caps_from_rtpmap (const gchar * media,
    const gchar * pt, const gchar * rtpmap)
{
  GstCaps *caps = NULL;
  gchar **tokens;

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
kms_base_rtp_end_point_connect_input_elements (KmsBaseSdpEndPoint *
    base_end_point, const GstSDPMessage * answer)
{
  guint i, len;

  KMS_BASE_SDP_END_POINT_CLASS
      (kms_base_rtp_end_point_parent_class)->connect_input_elements
      (base_end_point, answer);
  GST_DEBUG ("connect_input_elements");

  len = gst_sdp_message_medias_len (answer);

  for (i = 0; i < len; i++) {
    GstElement *payloader;
    GstCaps *caps = NULL;
    const gchar *rtpmap;
    const GstSDPMedia *media = gst_sdp_message_get_media (answer, i);
    guint j, f_len;

    // TODO: Change constant RTP/AVP by a paremeter
    if (g_ascii_strcasecmp ("RTP/AVP", gst_sdp_media_get_proto (media)) != 0)
      continue;

    f_len = gst_sdp_media_formats_len (media);

    for (j = 0; j < f_len && caps == NULL; j++) {
      const gchar *pt = gst_sdp_media_get_format (media, j);

      rtpmap = sdp_utils_sdp_media_get_rtpmap (media, pt);
      caps =
          kms_base_rtp_end_point_get_caps_from_rtpmap (media->media, pt,
          rtpmap);
    }

    if (caps == NULL)
      continue;

    GST_DEBUG ("Found caps: %P", caps);

    payloader = gst_base_rtp_get_payloader_for_caps (caps);
    if (payloader != NULL) {
      KmsElement *joinable = KMS_ELEMENT (base_end_point);
      KmsBaseRtpEndPoint *rtp_end_point =
          KMS_BASE_RTP_END_POINT (base_end_point);
      const gchar *rtpbin_pad_name;
      GstElement *valve = NULL;

      GST_DEBUG ("Found depayloader %P", payloader);
      gst_bin_add (GST_BIN (base_end_point), payloader);
      gst_element_sync_state_with_parent (payloader);

      if (g_strcmp0 ("audio", gst_sdp_media_get_media (media)) == 0) {
        valve = joinable->audio_valve;
        rtpbin_pad_name = "send_rtp_sink_0";
      } else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0) {
        valve = joinable->video_valve;
        rtpbin_pad_name = "send_rtp_sink_1";
      } else {
        gst_bin_remove (GST_BIN (base_end_point), payloader);
      }

      if (valve != NULL) {
        gst_element_link (valve, payloader);
        gst_element_link_pads (payloader, "src", rtp_end_point->rtpbin,
            rtpbin_pad_name);
        g_object_set (valve, "drop", FALSE, NULL);
      }
    }

    gst_caps_unref (caps);
  }
}

static GstCaps *
kms_base_rtp_end_point_get_caps_for_pt (KmsBaseRtpEndPoint * base_rtp_end_point,
    guint pt)
{
  KmsBaseSdpEndPoint *base_end_point =
      KMS_BASE_SDP_END_POINT (base_rtp_end_point);
  GstSDPMessage *answer;
  guint i, len;

  answer = base_end_point->local_answer_sdp;
  if (answer == NULL)
    answer = base_end_point->remote_answer_sdp;

  if (answer == NULL)
    return NULL;

  len = gst_sdp_message_medias_len (answer);

  for (i = 0; i < len; i++) {
    const gchar *rtpmap;
    const GstSDPMedia *media = gst_sdp_message_get_media (answer, i);
    guint j, f_len;

    // TODO: Change constant RTP/AVP by a paremeter
    if (g_ascii_strcasecmp ("RTP/AVP", gst_sdp_media_get_proto (media)) != 0)
      continue;

    f_len = gst_sdp_media_formats_len (media);

    for (j = 0; j < f_len; j++) {
      GstCaps *caps;
      const gchar *payload = gst_sdp_media_get_format (media, j);

      if (atoi (payload) != pt)
        continue;

      rtpmap = sdp_utils_sdp_media_get_rtpmap (media, payload);

      caps =
          kms_base_rtp_end_point_get_caps_from_rtpmap (media->media, payload,
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
kms_base_rtp_end_point_request_pt_map (GstElement * rtpbin, guint session,
    guint pt, KmsBaseRtpEndPoint * base_rtp_end_point)
{
  GstCaps *caps;

  GST_DEBUG ("Caps request for pt: %d", pt);

  caps = kms_base_rtp_end_point_get_caps_for_pt (base_rtp_end_point, pt);

  if (caps != NULL)
    return caps;

  return gst_caps_new_simple ("application/x-rtp", "payload", G_TYPE_INT, pt,
      NULL);
}

static void
kms_base_rtp_end_point_rtpbin_pad_added (GstElement * rtpbin, GstPad * pad,
    KmsBaseRtpEndPoint * rtp_end_point)
{
  GstElement *agnostic, *depayloader;
  GstCaps *caps;

  GST_PAD_STREAM_LOCK (pad);

  if (g_str_has_prefix (GST_OBJECT_NAME (pad), "recv_rtp_src_0_"))
    agnostic = KMS_ELEMENT (rtp_end_point)->audio_agnosticbin;
  else if (g_str_has_prefix (GST_OBJECT_NAME (pad), "recv_rtp_src_1_"))
    agnostic = KMS_ELEMENT (rtp_end_point)->video_agnosticbin;
  else
    goto end;

  caps = gst_pad_query_caps (pad, NULL);
  GST_DEBUG ("New pad: %P for linking to %P with caps %P", pad, agnostic, caps);

  depayloader = gst_base_rtp_get_depayloader_for_caps (caps);

  if (caps != NULL)
    gst_caps_unref (caps);

  if (depayloader != NULL) {
    gst_bin_add (GST_BIN (rtp_end_point), depayloader);
    gst_element_sync_state_with_parent (depayloader);

    gst_element_link_pads (depayloader, "src", agnostic, "sink");
    gst_element_link_pads (rtpbin, GST_OBJECT_NAME (pad), depayloader, "sink");
  } else {
    GstElement *fake = gst_element_factory_make ("fakesink", NULL);

    gst_bin_add (GST_BIN (rtp_end_point), fake);
    gst_element_sync_state_with_parent (fake);
    gst_element_link_pads (rtpbin, GST_OBJECT_NAME (pad), fake, "sink");
  }

end:
  GST_PAD_STREAM_UNLOCK (pad);
}

static void
kms_base_rtp_end_point_class_init (KmsBaseRtpEndPointClass * klass)
{
  KmsBaseSdpEndPointClass *base_end_point_class;
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseRtpEndPoint",
      "Base/Bin/BaseRtpEndPoints",
      "Base class for RtpEndPoints",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_end_point_class = KMS_BASE_SDP_END_POINT_CLASS (klass);

  base_end_point_class->connect_input_elements =
      kms_base_rtp_end_point_connect_input_elements;
}

static void
kms_base_rtp_end_point_init (KmsBaseRtpEndPoint * base_rtp_end_point)
{
  base_rtp_end_point->rtpbin = gst_element_factory_make ("rtpbin", RTPBIN);

  g_signal_connect (base_rtp_end_point->rtpbin, "request-pt-map",
      G_CALLBACK (kms_base_rtp_end_point_request_pt_map), base_rtp_end_point);

  g_signal_connect (base_rtp_end_point->rtpbin, "pad-added",
      G_CALLBACK (kms_base_rtp_end_point_rtpbin_pad_added), base_rtp_end_point);

  g_object_set (base_rtp_end_point->rtpbin, "do-lost", TRUE, NULL);

  gst_bin_add (GST_BIN (base_rtp_end_point), base_rtp_end_point->rtpbin);
}
