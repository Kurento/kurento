#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <stdlib.h>
#include <gst/gst.h>

#include "gstbasertpstream.h"
#include "sdp_utils.h"

#define PLUGIN_NAME "base_rtp_stream"

GST_DEBUG_CATEGORY_STATIC (gst_base_rtp_stream_debug);
#define GST_CAT_DEFAULT gst_base_rtp_stream_debug

#define gst_base_rtp_stream_parent_class parent_class
G_DEFINE_TYPE (GstBaseRtpStream, gst_base_rtp_stream, GST_TYPE_BASE_STREAM);

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
gst_base_rtp_stream_get_caps_from_rtpmap (const gchar * media, const gchar * pt,
    const gchar * rtpmap)
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
gst_base_rtp_stream_connect_input_elements (GstBaseStream * base_stream,
    const GstSDPMessage * answer)
{
  guint i, len;

  GST_BASE_STREAM_CLASS
      (gst_base_rtp_stream_parent_class)->connect_input_elements (base_stream,
      answer);
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
          gst_base_rtp_stream_get_caps_from_rtpmap (media->media, pt, rtpmap);
    }

    if (caps == NULL)
      continue;

    GST_DEBUG ("Found caps: %P", caps);

    payloader = gst_base_rtp_get_payloader_for_caps (caps);
    if (payloader != NULL) {
      KmsElement *joinable = KMS_ELEMENT (base_stream);
      GstBaseRtpStream *rtp_stream = GST_BASE_RTP_STREAM (base_stream);
      const gchar *rtpbin_pad_name;
      GstElement *valve = NULL;

      GST_DEBUG ("Found depayloader %P", payloader);
      gst_bin_add (GST_BIN (base_stream), payloader);
      gst_element_sync_state_with_parent (payloader);

      if (g_strcmp0 ("audio", gst_sdp_media_get_media (media)) == 0) {
        valve = joinable->audio_valve;
        rtpbin_pad_name = "send_rtp_sink_0";
      } else if (g_strcmp0 ("video", gst_sdp_media_get_media (media)) == 0) {
        valve = joinable->video_valve;
        rtpbin_pad_name = "send_rtp_sink_1";
      } else {
        gst_bin_remove (GST_BIN (base_stream), payloader);
      }

      if (valve != NULL) {
        gst_element_link (valve, payloader);
        gst_element_link_pads (payloader, "src", rtp_stream->rtpbin,
            rtpbin_pad_name);
        g_object_set (valve, "drop", FALSE, NULL);
      }
    }

    gst_caps_unref (caps);
  }
}

static GstCaps *
gst_base_rtp_stream_get_caps_for_pt (GstBaseRtpStream * base_rtp_stream,
    guint pt)
{
  GstBaseStream *base_stream = GST_BASE_STREAM (base_rtp_stream);
  GstSDPMessage *answer;
  guint i, len;

  answer = base_stream->local_answer_sdp;
  if (answer == NULL)
    answer = base_stream->remote_answer_sdp;

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
          gst_base_rtp_stream_get_caps_from_rtpmap (media->media, payload,
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
gst_base_rtp_stream_request_pt_map (GstElement * rtpbin, guint session,
    guint pt, GstBaseRtpStream * base_rtp_stream)
{
  GstCaps *caps;

  GST_DEBUG ("Caps request for pt: %d", pt);

  caps = gst_base_rtp_stream_get_caps_for_pt (base_rtp_stream, pt);

  if (caps != NULL)
    return caps;

  return gst_caps_new_simple ("application/x-rtp", "payload", G_TYPE_INT, pt,
      NULL);
}

static void
gst_base_rtp_stream_rtpbin_pad_added (GstElement * rtpbin, GstPad * pad,
    GstBaseRtpStream * rtp_stream)
{
  GstElement *agnostic, *depayloader;
  GstCaps *caps;

  GST_PAD_STREAM_LOCK (pad);

  if (g_str_has_prefix (GST_OBJECT_NAME (pad), "recv_rtp_src_0_"))
    agnostic = KMS_ELEMENT (rtp_stream)->audio_agnosticbin;
  else if (g_str_has_prefix (GST_OBJECT_NAME (pad), "recv_rtp_src_1_"))
    agnostic = KMS_ELEMENT (rtp_stream)->video_agnosticbin;
  else
    goto end;

  caps = gst_pad_query_caps (pad, NULL);
  GST_DEBUG ("New pad: %P for linking to %P with caps %P", pad, agnostic, caps);

  depayloader = gst_base_rtp_get_depayloader_for_caps (caps);

  if (caps != NULL)
    gst_caps_unref (caps);

  if (depayloader != NULL) {
    gst_bin_add (GST_BIN (rtp_stream), depayloader);
    gst_element_sync_state_with_parent (depayloader);

    gst_element_link_pads (depayloader, "src", agnostic, "sink");
    gst_element_link_pads (rtpbin, GST_OBJECT_NAME (pad), depayloader, "sink");
  } else {
    GstElement *fake = gst_element_factory_make ("fakesink", NULL);

    gst_bin_add (GST_BIN (rtp_stream), fake);
    gst_element_sync_state_with_parent (fake);
    gst_element_link_pads (rtpbin, GST_OBJECT_NAME (pad), fake, "sink");
  }

end:
  GST_PAD_STREAM_UNLOCK (pad);
}

static void
gst_base_rtp_stream_class_init (GstBaseRtpStreamClass * klass)
{
  GstBaseStreamClass *base_stream_class;
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseRtpStream",
      "Base/Bin/BaseRtpStream",
      "Base class for streams",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_stream_class = GST_BASE_STREAM_CLASS (klass);

  base_stream_class->connect_input_elements =
      gst_base_rtp_stream_connect_input_elements;
}

static void
gst_base_rtp_stream_init (GstBaseRtpStream * base_rtp_stream)
{
  base_rtp_stream->rtpbin = gst_element_factory_make ("rtpbin", RTPBIN);

  g_signal_connect (base_rtp_stream->rtpbin, "request-pt-map",
      G_CALLBACK (gst_base_rtp_stream_request_pt_map), base_rtp_stream);

  g_signal_connect (base_rtp_stream->rtpbin, "pad-added",
      G_CALLBACK (gst_base_rtp_stream_rtpbin_pad_added), base_rtp_stream);

  g_object_set (base_rtp_stream->rtpbin, "do-lost", TRUE, NULL);

  gst_bin_add (GST_BIN (base_rtp_stream), base_rtp_stream->rtpbin);
}
