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

#include "sdp_utils.h"
#include <gst/gst.h>
#include <glib.h>
#include <stdlib.h>

#include "constants.h"

#define GST_CAT_DEFAULT sdp_utils
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "sdputils"

static gchar *directions[] =
    { SENDONLY_STR, RECVONLY_STR, SENDRECV_STR, INACTIVE_STR, NULL };

#define EXT_MAP "extmap"
#define RTPMAP "rtpmap"
#define FMTP "fmtp"

static gchar *rtpmaps[] = {
  "PCMU/8000/1",
  NULL,
  NULL,
  "GSM/8000/1",
  "G723/8000/1",
  "DVI4/8000/1",
  "DVI4/16000/1",
  "LPC/8000/1",
  "PCMA/8000/1",
  "G722/8000/1",
  "L16/44100/2",
  "L16/44100/1",
  "QCELP/8000/1",
  "CN/8000/1",
  "MPA/90000",
  "G728/8000/1",
  "DVI4/11025/1",
  "DVI4/22050/1",
  "G729/8000/1",
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  "CelB/90000",
  "JPEG/90000",
  NULL,
  "nv/90000",
  NULL,
  NULL,
  "H261/90000",
  "MPV/90000",
  "MP2T/90000",
  "H263/90000",
};

gboolean
sdp_utils_attribute_is_direction (const GstSDPAttribute * attr,
    GstSDPDirection * direction)
{
  gint i;

  for (i = 0; directions[i] != NULL; i++) {
    if (g_ascii_strcasecmp (attr->key, directions[i]) == 0) {
      if (direction != NULL) {
        *direction = i;
      }
      return TRUE;
    }
  }

  return FALSE;
}

static gchar *
sdp_media_get_ssrc_str (const GstSDPMedia * media)
{
  gchar *ssrc = NULL;
  const gchar *val;
  GRegex *regex;
  GMatchInfo *match_info = NULL;

  val = gst_sdp_media_get_attribute_val (media, "ssrc");
  if (val == NULL) {
    return NULL;
  }

  regex = g_regex_new ("^(?<ssrc>[0-9]+)(.*)?$", 0, 0, NULL);
  g_regex_match (regex, val, 0, &match_info);
  g_regex_unref (regex);

  if (g_match_info_matches (match_info)) {
    ssrc = g_match_info_fetch_named (match_info, "ssrc");
  }
  g_match_info_free (match_info);

  return ssrc;
}

static guint
ssrc_str_to_uint (const gchar * ssrc_str)
{
  gint64 val;
  guint ssrc = 0;

  val = g_ascii_strtoll (ssrc_str, NULL, 10);
  if (val > G_MAXUINT32) {
    GST_ERROR ("SSRC %" G_GINT64_FORMAT " not valid", val);
  } else {
    ssrc = val;
  }

  return ssrc;
}

guint
sdp_utils_media_get_ssrc (const GstSDPMedia * media)
{
  gchar *ssrc_str;
  guint ssrc = 0;

  ssrc_str = sdp_media_get_ssrc_str (media);
  if (ssrc_str == NULL) {
    return SSRC_INVALID;
  }

  ssrc = ssrc_str_to_uint (ssrc_str);
  g_free (ssrc_str);

  return ssrc;
}

static gchar **
sdp_media_get_fid_ssrcs_str (const GstSDPMedia * media)
{
  gchar **ssrcs = NULL;
  gchar *ssrcs_str;
  const gchar *val;
  GRegex *regex;
  GMatchInfo *match_info = NULL;

  val = gst_sdp_media_get_attribute_val (media, "ssrc-group");
  if (val == NULL) {
    return NULL;
  }

  regex = g_regex_new ("^FID (?<ssrcs>[0-9\\ ]+)$", 0, 0, NULL);
  g_regex_match (regex, val, 0, &match_info);
  g_regex_unref (regex);

  if (g_match_info_matches (match_info)) {
    ssrcs_str = g_match_info_fetch_named (match_info, "ssrcs");
    ssrcs = g_strsplit (ssrcs_str, " ", 0);
    g_free (ssrcs_str);
  }
  g_match_info_free (match_info);

  return ssrcs;
}

guint
sdp_utils_media_get_fid_ssrc (const GstSDPMedia * media, guint pos)
{
  gchar **ssrcs;
  guint ssrc = SSRC_INVALID;
  guint len;

  ssrcs = sdp_media_get_fid_ssrcs_str (media);
  if (ssrcs == NULL) {
    return SSRC_INVALID;
  }

  len = g_strv_length (ssrcs);
  if (len <= pos) {
    GST_WARNING ("Pos '%u' greater than len '%u'", pos, len);
  } else {
    ssrc = ssrc_str_to_uint (ssrcs[pos]);
  }

  g_strfreev (ssrcs);

  return ssrc;
}

GstSDPDirection
sdp_utils_media_config_get_direction (const GstSDPMedia * media)
{
  GstSDPDirection dir = GST_SDP_DIRECTION_SENDRECV;

  guint i, len;

  len = gst_sdp_media_attributes_len (media);

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *a;

    a = gst_sdp_media_get_attribute (media, i);

    if (sdp_utils_attribute_is_direction (a, &dir)) {
      break;
    }
  }

  return dir;
}

gboolean
sdp_utils_media_config_set_direction (GstSDPMedia * media,
    GstSDPDirection direction)
{
  const gchar *dir_str;
  guint i, len;

  if (direction == GST_SDP_DIRECTION_RECVONLY) {
    dir_str = RECVONLY_STR;
  } else if (direction == GST_SDP_DIRECTION_SENDONLY) {
    dir_str = SENDONLY_STR;
  } else if (direction == GST_SDP_DIRECTION_SENDRECV) {
    dir_str = SENDRECV_STR;
  } else if (direction == GST_SDP_DIRECTION_INACTIVE) {
    dir_str = INACTIVE_STR;
  } else {
    GST_WARNING ("Invalid attribute direction: %d", direction);
    return FALSE;
  }

  len = gst_sdp_media_attributes_len (media);
  for (i = 0; i < len; i++) {
    const GstSDPAttribute *a;

    a = gst_sdp_media_get_attribute (media, i);
    if (sdp_utils_attribute_is_direction (a, NULL)) {
      if (gst_sdp_media_remove_attribute (media, i) != GST_SDP_OK) {
        gchar *str = NULL;

        GST_WARNING ("Cannot remove attribute '%d' from media:\n%s", i, (str =
                gst_sdp_media_as_text (media)));
        g_free (str);

        return FALSE;
      }
    }
  }

  return gst_sdp_media_add_attribute (media, dir_str, "") == GST_SDP_OK;
}

/**
 * format: A Payload Type number, from the media PT list
 * Returns : a string or NULL if any.
 */
const gchar *
sdp_utils_sdp_media_get_rtpmap (const GstSDPMedia * media, const gchar * format)
{
  guint i, attrs_len;
  gchar *rtpmap = NULL;

  attrs_len = gst_sdp_media_attributes_len (media);

  for (i = 0; i < attrs_len && rtpmap == NULL; i++) {
    const GstSDPAttribute *attr = gst_sdp_media_get_attribute (media, i);

    if (g_ascii_strcasecmp (RTPMAP, attr->key) == 0) {
      if (g_str_has_prefix (attr->value, format)) {
        rtpmap = g_strstr_len (attr->value, -1, " ");
        if (rtpmap != NULL)
          rtpmap = rtpmap + 1;
      }
    }
  }

  if (rtpmap == NULL) {
    gint pt;

    for (i = 0; format[i] != '\0'; i++) {
      if (!g_ascii_isdigit (format[i]))
        return NULL;
    }

    pt = atoi (format);
    if (pt > 34)
      return NULL;

    rtpmap = rtpmaps[pt];
  }

  return rtpmap;
}

/**
 * format: A Payload Type number, from the media PT list
 */
const gchar *
sdp_utils_sdp_media_get_fmtp (const GstSDPMedia * media, const gchar * format)
{
  guint i;

  for (i = 0;; i++) {
    const gchar *attr_val = NULL;
    gchar **attrs;

    attr_val = gst_sdp_media_get_attribute_val_n (media, FMTP, i);

    // Example:
    // format == "102"
    // attr_val == "102 level-asymmetry-allowed=1;packetization-mode=1; profile-level-id=42001f"

    if (attr_val == NULL) {
      return NULL;
    }

    attrs = g_strsplit (attr_val, " ", 0);

    // attrs[0] == "102"
    // attrs[1] == "level-asymmetry-allowed=1;packetization-mode=1;"
    // attrs[2] == "profile-level-id=42001f"
    // attrs[3] == NULL

    if (attrs[0] == NULL) {
      GST_WARNING ("No payload found for fmtp attribute 'a=%s:%s'", FMTP,
          format);
      g_strfreev (attrs);
      continue;
    }

    if (g_strcmp0 (attrs[0], format) == 0) {
      g_strfreev (attrs);

      return attr_val;
    }

    g_strfreev (attrs);
  }

  return NULL;
}

static gboolean
sdp_utils_add_setup_attribute (const GstSDPAttribute * attr,
    GstSDPAttribute * new_attr)
{
  const gchar *setup;

  /* follow rules defined in RFC4145 */

  if (g_strcmp0 (attr->value, "active") == 0) {
    setup = "passive";
  } else if (g_strcmp0 (attr->value, "passive") == 0) {
    setup = "active";
  } else if (g_strcmp0 (attr->value, "actpass") == 0) {
    setup = "active";
  } else {
    setup = "holdconn";
  }

  return gst_sdp_attribute_set (new_attr, attr->key, setup) == GST_SDP_OK;
}

static gboolean
sdp_utils_add_comedia_attribute (const GstSDPAttribute * attr,
    GstSDPAttribute * new_attr)
{
  const gchar *comedia;

  if (g_strcmp0 (attr->value, "active") == 0) {
    comedia = "passive";
  } else {
    comedia = "active";
  }

  return gst_sdp_attribute_set (new_attr, attr->key, comedia) == GST_SDP_OK;
}

static gboolean
sdp_utils_set_direction_answer (const GstSDPAttribute * attr,
    GstSDPAttribute * new_attr)
{
  const gchar *direction;

  /* rfc3264 6.1 */
  if (g_ascii_strcasecmp (attr->key, SENDONLY_STR) == 0) {
    direction = RECVONLY_STR;
  } else if (g_ascii_strcasecmp (attr->key, RECVONLY_STR) == 0) {
    direction = SENDONLY_STR;
  } else if (g_ascii_strcasecmp (attr->key, SENDRECV_STR) == 0) {
    direction = SENDRECV_STR;
  } else if (g_ascii_strcasecmp (attr->key, INACTIVE_STR) == 0) {
    direction = INACTIVE_STR;
  } else {
    GST_WARNING ("Invalid attribute direction: %s", attr->key);
    return FALSE;
  }

  return gst_sdp_attribute_set (new_attr, direction, "") == GST_SDP_OK;
}

static gboolean
intersect_attribute (const GstSDPAttribute * attr,
    GstSDPIntersectMediaFunc func, gpointer user_data)
{
  const GstSDPAttribute *a;
  GstSDPAttribute new_attr;

  if (g_strcmp0 (attr->key, "setup") == 0) {
    /* follow rules defined in RFC4145 */
    if (!sdp_utils_add_setup_attribute (attr, &new_attr)) {
      GST_WARNING ("Cannot set attribute a=%s:%s", attr->key, attr->value);
      return FALSE;
    }
    a = &new_attr;
  }
  else if (g_strcmp0 (attr->key, "direction") == 0) {
    // COMEDIA-based discovery of remote IP+port
    if (!sdp_utils_add_comedia_attribute (attr, &new_attr)) {
      GST_WARNING ("Cannot set attribute a=%s:%s", attr->key, attr->value);
      return FALSE;
    }
    a = &new_attr;
  }
  else if (g_strcmp0 (attr->key, "connection") == 0) {
    /* TODO: Implment a mechanism that allows us to know if a */
    /* new connection is gonna be required or an existing one */
    /* can be used. By default we always create a new one. */
    if (gst_sdp_attribute_set (&new_attr, "connection", "new") != GST_SDP_OK) {
      GST_WARNING ("Cannot add attribute a=connection:new");
      return FALSE;
    }
    a = &new_attr;
  }
  else if (sdp_utils_attribute_is_direction (attr, NULL)) {
    if (!sdp_utils_set_direction_answer (attr, &new_attr)) {
      GST_WARNING ("Cannot set 'direction' attribute");
      return FALSE;
    }

    a = &new_attr;
  } else {
    a = attr;
  }

  /* No common media attribute. Filter using callback */
  if (func != NULL) {
    func (a, user_data);
  }

  if (a == &new_attr) {
    gst_sdp_attribute_clear (&new_attr);
  }

  return TRUE;
}

gboolean
sdp_utils_intersect_session_attributes (const GstSDPMessage * msg,
    GstSDPIntersectMediaFunc func, gpointer user_data)
{
  guint i, len;

  len = gst_sdp_message_attributes_len (msg);

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;

    attr = gst_sdp_message_get_attribute (msg, i);

    if (!intersect_attribute (attr, func, user_data))
      return FALSE;
  }

  return TRUE;
}

gboolean
sdp_utils_intersect_media_attributes (const GstSDPMedia * offer,
    GstSDPIntersectMediaFunc func, gpointer user_data)
{
  guint i, len;

  len = gst_sdp_media_attributes_len (offer);

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;

    attr = gst_sdp_media_get_attribute (offer, i);

    if (!intersect_attribute (attr, func, user_data)) {
      return FALSE;
    }
  }

  return TRUE;
}

const gchar *
sdp_utils_get_attr_map_value (const GstSDPMedia * media, const gchar * name,
    const gchar * fmt)
{
  const gchar *val = NULL;
  guint i;

  for (i = 0;; i++) {
    gchar **attrs;

    val = gst_sdp_media_get_attribute_val_n (media, name, i);

    if (val == NULL) {
      return NULL;
    }

    attrs = g_strsplit (val, " ", 0);

    if (g_strcmp0 (fmt, attrs[0] /* format */ ) == 0) {
      g_strfreev (attrs);
      return val;
    }

    g_strfreev (attrs);
  }

  return NULL;
}

gboolean
sdp_utils_for_each_media (const GstSDPMessage * msg, GstSDPMediaFunc func,
    gpointer user_data)
{
  guint i, len;

  len = gst_sdp_message_medias_len (msg);
  for (i = 0; i < len; i++) {
    const GstSDPMedia *media = gst_sdp_message_get_media (msg, i);

    if (!func (media, user_data)) {
      /* Do not continue iterating */
      return FALSE;
    }
  }

  return TRUE;
}

gboolean
sdp_utils_is_attribute_in_media (const GstSDPMedia * media,
    const GstSDPAttribute * attr)
{
  guint i, len;

  len = gst_sdp_media_attributes_len (media);

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *a;

    a = gst_sdp_media_get_attribute (media, i);

    if (g_strcmp0 (attr->key, a->key) == 0 &&
        g_strcmp0 (attr->value, a->value) == 0) {
      return TRUE;
    }
  }

  return FALSE;
}

gboolean
sdp_utils_media_is_active (const GstSDPMedia * media, gboolean offerer)
{
  const gchar *attr;

  attr = gst_sdp_media_get_attribute_val_n (media, "setup", 0);
  if (attr == NULL) {
    goto _default;
  }

  if (offerer) {
    if (g_strcmp0 (attr, "active") == 0) {
      GST_DEBUG ("Remote is 'active', so we are 'passive'");
      return FALSE;
    } else if (g_strcmp0 (attr, "passive") == 0) {
      GST_DEBUG ("Remote is 'passive', so we are 'active'");
      return TRUE;
    }
  } else {
    if (g_strcmp0 (attr, "active") == 0) {
      GST_DEBUG ("We are 'active'");
      return TRUE;
    } else if (g_strcmp0 (attr, "passive") == 0) {
      GST_DEBUG ("We are 'passive'");
      return FALSE;
    }
  }

_default:
  GST_DEBUG ("Negotiated SDP is '%s'. %s", attr,
      offerer ? "Local offerer, so 'passive'" : "Remote offerer, so 'active'");

  return !offerer;
}

gboolean
sdp_utils_rtcp_fb_attr_check_type (const gchar * attr,
    const gchar * pt, const gchar * type)
{
  gchar *aux;
  gboolean ret;

  aux = g_strconcat (pt, " ", type, NULL);
  ret = g_strcmp0 (attr, aux) == 0;
  g_free (aux);

  return ret;
}

gboolean
sdp_utils_media_has_remb (const GstSDPMedia * media)
{
  const gchar *payload = gst_sdp_media_get_format (media, 0);
  guint a;

  if (payload == NULL) {
    return FALSE;
  }

  for (a = 0;; a++) {
    const gchar *attr;

    attr = gst_sdp_media_get_attribute_val_n (media, SDP_MEDIA_RTCP_FB, a);
    if (attr == NULL) {
      break;
    }

    if (sdp_utils_rtcp_fb_attr_check_type (attr, payload,
        SDP_MEDIA_RTCP_FB_GOOG_REMB)) {
      return TRUE;
    }
  }

  return FALSE;
}

gboolean
sdp_utils_media_has_rtcp_nack (const GstSDPMedia * media)
{
  const gchar *payload = gst_sdp_media_get_format (media, 0);
  guint a;

  if (payload == NULL) {
    return FALSE;
  }

  for (a = 0;; a++) {
    const gchar *attr;

    attr = gst_sdp_media_get_attribute_val_n (media, SDP_MEDIA_RTCP_FB, a);
    if (attr == NULL) {
      break;
    }

    if (sdp_utils_rtcp_fb_attr_check_type (attr, payload,
        SDP_MEDIA_RTCP_FB_NACK)) {
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
sdp_media_contains_attr (const GstSDPMedia * m, const GstSDPAttribute * attr)
{
  guint i;

  for (i = 0;; i++) {
    const gchar *val;

    val = gst_sdp_media_get_attribute_val_n (m, attr->key, i);

    if (val == NULL) {
      /* Attribute is not present */
      return FALSE;
    }

    if (g_strcmp0 (attr->value, val) == 0) {
      /* Attribute found */
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
sdp_media_equal_attributes (const GstSDPMedia * m1, const GstSDPMedia * m2)
{
  guint i, len;

  len = gst_sdp_media_attributes_len (m1);

  if (len != gst_sdp_media_attributes_len (m2)) {
    return FALSE;
  }

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;

    attr = gst_sdp_media_get_attribute (m1, i);
    if (!sdp_media_contains_attr (m2, attr)) {
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
sdp_media_fmts_equal (const GstSDPMedia * m1, const GstSDPMedia * m2)
{
  guint i, len;

  len = gst_sdp_media_formats_len (m1);

  if (len != gst_sdp_media_formats_len (m2)) {
    return FALSE;
  }

  for (i = 0; i < len; i++) {
    const gchar *fmt1, *fmt2;

    fmt1 = gst_sdp_media_get_format (m1, i);
    fmt2 = gst_sdp_media_get_format (m2, i);

    if (g_strcmp0 (fmt1, fmt2) != 0) {
      return FALSE;
    }
  }

  return TRUE;
}

static gboolean
sdp_key_equal (const GstSDPKey * k1, const GstSDPKey * k2)
{
  if ((k1 == NULL && k2 != NULL) || (k1 != NULL && k2 == NULL)) {
    return FALSE;
  }

  if (k1 == NULL && k2 == NULL) {
    return TRUE;
  }

  return g_strcmp0 (k1->type, k2->type) == 0 &&
      g_strcmp0 (k1->data, k2->data) == 0;
}

gboolean
sdp_utils_equal_medias (const GstSDPMedia * m1, const GstSDPMedia * m2)
{
  if (g_strcmp0 (gst_sdp_media_get_media (m1),
          gst_sdp_media_get_media (m2)) != 0) {
    return FALSE;
  }

  if (gst_sdp_media_get_port (m1) != gst_sdp_media_get_port (m2)) {
    return FALSE;
  }

  if (gst_sdp_media_get_num_ports (m1) != gst_sdp_media_get_num_ports (m2)) {
    return FALSE;
  }

  if (g_strcmp0 (gst_sdp_media_get_proto (m1),
          gst_sdp_media_get_proto (m2)) != 0) {
    return FALSE;
  }

  if (g_strcmp0 (gst_sdp_media_get_information (m1),
          gst_sdp_media_get_information (m2)) != 0) {
    return FALSE;
  }

  if (!sdp_key_equal (gst_sdp_media_get_key (m1), gst_sdp_media_get_key (m2))) {
    return FALSE;
  }

  /* TODO: Check connections, bandwidths */

  if (!sdp_media_fmts_equal (m1, m2)) {
    return FALSE;
  }

  return sdp_media_equal_attributes (m1, m2);
}

static gboolean
sdp_message_contains_attr (const GstSDPMessage * m,
    const GstSDPAttribute * attr)
{
  guint i;

  for (i = 0;; i++) {
    const gchar *val;

    val = gst_sdp_message_get_attribute_val_n (m, attr->key, i);

    if (val == NULL) {
      /* Attribute is not present */
      return FALSE;
    }

    if (g_strcmp0 (attr->value, val) == 0) {
      /* Attribute found */
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
sdp_message_equal_attributes (const GstSDPMessage * msg1,
    const GstSDPMessage * msg2)
{
  guint i, len;

  /* TODO: Check more fields of GstSDPMessage */

  len = gst_sdp_message_attributes_len (msg1);

  if (len != gst_sdp_message_attributes_len (msg2)) {
    return FALSE;
  }

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;

    attr = gst_sdp_message_get_attribute (msg1, i);
    if (!sdp_message_contains_attr (msg2, attr)) {
      return FALSE;
    }
  }

  return TRUE;
}

gboolean
sdp_utils_equal_messages (const GstSDPMessage * msg1,
    const GstSDPMessage * msg2)
{
  guint i, len;

  if (!sdp_message_equal_attributes (msg1, msg2)) {
    return FALSE;
  }

  len = gst_sdp_message_medias_len (msg1);

  if (len != gst_sdp_message_medias_len (msg2)) {
    return FALSE;
  }

  for (i = 0; i < len; i++) {
    const GstSDPMedia *m1 = gst_sdp_message_get_media (msg1, i);
    const GstSDPMedia *m2 = gst_sdp_message_get_media (msg2, i);

    if (!sdp_utils_equal_medias (m1, m2)) {
      return FALSE;
    }
  }

  return TRUE;
}

gboolean
sdp_utils_get_data_from_rtpmap (const gchar * rtpmap, gchar ** codec_name,
    gint * clock_rate)
{
  gboolean ret = FALSE;
  gchar **tokens;

  tokens = g_strsplit (rtpmap, "/", 3);

  if (tokens[0] == NULL || tokens[1] == NULL) {
    goto end;
  }

  if (tokens[0] == NULL || tokens[1] == NULL) {
    goto end;
  }

  if (clock_rate) {
    *clock_rate = atoi (tokens[1]);
  }

  if (codec_name) {
    *codec_name = g_strdup (tokens[0]);
  }

  ret = TRUE;
end:

  g_strfreev (tokens);
  return ret;
}

gboolean
sdp_utils_is_pt_in_fmts (const GstSDPMedia * media, gint pt)
{
  guint i, len;

  len = gst_sdp_media_formats_len (media);

  for (i = 0; i < len; i++) {
    gint payload;

    payload = atoi (gst_sdp_media_get_format (media, i));

    if (payload == pt) {
      return TRUE;
    }
  }

  return FALSE;
}

gboolean
sdp_utils_get_data_from_rtpmap_codec (const GstSDPMedia * media,
    const gchar * codec, gint * pt, gint * clock_rate)
{
  gboolean found = FALSE;
  guint8 i;

  for (i = 0;; i++) {
    const gchar *val = NULL;
    gchar **attrs;

    val = gst_sdp_media_get_attribute_val_n (media, "rtpmap", i);

    if (val == NULL) {
      break;
    }

    if (!g_str_match_string (codec, val, TRUE)) {
      continue;
    }

    attrs = g_strsplit (val, " ", 0);

    if (attrs[0] == NULL) {
      /* No pt */
      g_strfreev (attrs);
      continue;
    }

    if (attrs[1] == NULL) {
      /* No codec */
      g_strfreev (attrs);
      continue;
    }

    if (!sdp_utils_is_pt_in_fmts (media, atoi (attrs[0]))) {
      /* pt is not in the offer */
      g_strfreev (attrs);
      continue;
    }

    if (!sdp_utils_get_data_from_rtpmap (attrs[1], NULL, clock_rate)) {
      g_strfreev (attrs);
      continue;
    }

    if (pt != NULL) {
      *pt = atoi (attrs[0]);
    }

    found = TRUE;

    g_strfreev (attrs);

    break;
  }

  return found;
}

gint
sdp_utils_get_pt_for_codec_name (const GstSDPMedia * media,
    const gchar * codec_name)
{
  const gchar *rtpmap;
  guint j, f_len;
  gint pt = -1;

  f_len = gst_sdp_media_formats_len (media);
  for (j = 0; j < f_len; j++) {
    gchar *found_codec_name;
    const gchar *payload = gst_sdp_media_get_format (media, j);

    rtpmap = sdp_utils_sdp_media_get_rtpmap (media, payload);

    if (!sdp_utils_get_data_from_rtpmap (rtpmap, &found_codec_name, NULL)) {
      continue;
    }

    if (g_strcmp0 (found_codec_name, codec_name) == 0) {
      GST_ERROR ("Found codec name pt is: %u", atoi (payload));
      pt = atoi (payload);
    }

    g_free (found_codec_name);

    if (pt != -1) {
      return pt;
    }
  }

  return pt;
}

gint
sdp_utils_get_abs_send_time_id (const GstSDPMedia * media)
{
  guint a;

  for (a = 0;; a++) {
    const gchar *attr;
    gchar **tokens;

    attr = gst_sdp_media_get_attribute_val_n (media, EXT_MAP, a);
    if (attr == NULL) {
      break;
    }

    tokens = g_strsplit (attr, " ", 0);
    if (g_strcmp0 (RTP_HDR_EXT_ABS_SEND_TIME_URI, tokens[1]) == 0) {
      gint ret = atoi (tokens[0]);

      g_strfreev (tokens);
      return ret;
    }

    g_strfreev (tokens);
  }

  return -1;
}

gboolean
sdp_utils_media_is_inactive (const GstSDPMedia * media)
{
  return gst_sdp_media_get_attribute_val (media, "inactive") != NULL
      || gst_sdp_media_get_port (media) == 0;
}

static void init_debug (void) __attribute__ ((constructor));

static void
init_debug (void)
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
