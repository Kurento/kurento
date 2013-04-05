/*
 * sdp.c - Kurento Media Server
 *
 * Copyright (C) 2013 Kurento
 * Contact: Miguel París Díaz <mparisdiaz@gmail.com>
 * Contact: José Antonio Santos Cadenas <santoscadenas@kurento.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "sdp_utils.h"
#include <gst/gst.h>
#include <glib.h>

#define GST_CAT_DEFAULT sdp_utils
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "sdp_utils"

#define SENDONLY_STR  "sendonly"
#define RECVONLY_STR  "recvonly"
#define SENDRECV_STR  "sendrecv"
#define INACTIVE_STR  "inactive"

static gchar *directions[] =
    { SENDONLY_STR, RECVONLY_STR, SENDRECV_STR, INACTIVE_STR, NULL };

#define RTPMAP "rtpmap"

const gchar *
sdp_utils_get_direction_str (GstSDPDirection direction)
{
  return directions[direction];
}

static GstSDPResult
sdp_message_create_from_src (const GstSDPMessage * src, GstSDPMessage ** msg)
{
  GstSDPResult result;
  const GstSDPConnection *conn;
  const GstSDPOrigin *orig;

  result = gst_sdp_message_new (msg);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp message");
    return result;
  }

  conn = gst_sdp_message_get_connection (src);
  orig = gst_sdp_message_get_origin (src);

  gst_sdp_message_set_version (*msg, gst_sdp_message_get_version (src));
  gst_sdp_message_set_session_name (*msg,
      gst_sdp_message_get_session_name (src));
  gst_sdp_message_set_connection (*msg, conn->nettype, conn->addrtype,
      conn->address, conn->ttl, conn->addr_number);
  gst_sdp_message_set_origin (*msg, orig->username, orig->sess_id,
      orig->sess_version, orig->nettype, orig->addrtype, orig->addr);

  return GST_SDP_OK;
}

static GstSDPResult
sdp_media_create_from_src (const GstSDPMedia * src, GstSDPMedia ** media)
{
  GstSDPResult result;

  result = gst_sdp_media_new (media);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp media");
    return result;
  }

  gst_sdp_media_set_media (*media, gst_sdp_media_get_media (src));
  gst_sdp_media_set_port_info (*media, gst_sdp_media_get_port (src), 1);
  gst_sdp_media_set_proto (*media, gst_sdp_media_get_proto (src));

  return GST_SDP_OK;
}

static gboolean
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

static GstSDPDirection
sdp_media_get_direction (const GstSDPMedia * media)
{
  guint i, attrs_len;
  const GstSDPAttribute *attr;

  attrs_len = gst_sdp_media_attributes_len (media);

  for (i = 0; i < attrs_len; i++) {
    GstSDPDirection direction;

    attr = gst_sdp_media_get_attribute (media, i);

    if (sdp_utils_attribute_is_direction (attr, &direction)) {
      return direction;
    }
  }

  return SENDRECV;
}

static void
sdp_media_set_direction (GstSDPMedia * media, GstSDPDirection direction)
{
  gint i = 0;

  while (i < gst_sdp_media_attributes_len (media)) {
    const GstSDPAttribute *attr = gst_sdp_media_get_attribute (media, i);

    if (sdp_utils_attribute_is_direction (attr, NULL)) {
      g_array_remove_index (media->attributes, i);
      continue;
    }

    i++;
  }

  gst_sdp_media_add_attribute (media, sdp_utils_get_direction_str (direction),
      "");
}

/**
 * Returns : a newly-allocated string or NULL if any.
 * The returned string should be freed with g_free() when no longer needed.
 */
static const gchar *
sdp_media_get_rtpmap (const GstSDPMedia * media, const gchar * format)
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

  return rtpmap;
}

static void
sdp_utils_sdp_media_add_format (GstSDPMedia * media, const gchar * format,
    const gchar * rtpmap)
{
  gst_sdp_media_add_format (media, format);
  if (rtpmap != NULL) {
    gchar *rtpmap_att = g_strconcat (format, " ", rtpmap, NULL);

    gst_sdp_media_add_attribute (media, RTPMAP, rtpmap_att);
    g_free (rtpmap_att);
  }
}

static GstSDPResult
intersect_sdp_medias (const GstSDPMedia * offer,
    const GstSDPMedia * answer, GstSDPMedia ** offer_result,
    GstSDPMedia ** answer_result)
{
  GstSDPResult result;
  guint i;
  guint offer_format_len, answer_format_len;
  const gchar *offer_media_type, *answer_media_type;
  GstSDPDirection offer_dir, answer_dir, offer_result_dir, answer_result_dir;

  offer_media_type = gst_sdp_media_get_media (offer);
  answer_media_type = gst_sdp_media_get_media (answer);
  if (g_ascii_strncasecmp (offer_media_type, answer_media_type,
          g_utf8_strlen (answer_media_type, -1)) != 0) {
    GST_DEBUG ("Media types no compatibles: %s, %s", offer_media_type,
        answer_media_type);
    return GST_SDP_EINVAL;
  }

  offer_dir = sdp_media_get_direction (offer);
  answer_dir = sdp_media_get_direction (answer);

  if ((offer_dir == SENDONLY && answer_dir == SENDONLY) ||
      (offer_dir == RECVONLY && answer_dir == RECVONLY)) {
    return GST_SDP_EINVAL;
  } else if (offer_dir == SENDONLY || answer_dir == RECVONLY) {
    offer_result_dir = SENDONLY;
    answer_result_dir = RECVONLY;
  } else if (offer_dir == RECVONLY || answer_dir == SENDONLY) {
    offer_result_dir = RECVONLY;
    answer_result_dir = SENDONLY;
  } else {
    offer_result_dir = SENDRECV;
    answer_result_dir = SENDRECV;
  }

  result = sdp_media_create_from_src (offer, offer_result);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp media");
    return GST_SDP_EINVAL;
  }

  result = sdp_media_create_from_src (answer, answer_result);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp media");
    return GST_SDP_EINVAL;
  }

  offer_format_len = gst_sdp_media_formats_len (offer);
  answer_format_len = gst_sdp_media_formats_len (answer);

  for (i = 0; i < offer_format_len; i++) {
    gint j;
    const gchar *offer_format = gst_sdp_media_get_format (offer, i);
    const gchar *offer_rtpmap = sdp_media_get_rtpmap (offer, offer_format);

    for (j = 0; j < answer_format_len; j++) {
      const gchar *answer_format = gst_sdp_media_get_format (answer, j);
      const gchar *answer_rtpmap = sdp_media_get_rtpmap (answer, answer_format);

      // TODO: Do more test to check if this conditions are correct
      if ((offer_rtpmap == NULL && answer_rtpmap == NULL)
          && (g_ascii_strncasecmp (offer_format, answer_format,
                  g_utf8_strlen (offer_format, -1)) == 0)) {
        /* static payload */
      } else if (offer_rtpmap == NULL || answer_rtpmap == NULL
          || (g_ascii_strcasecmp (offer_rtpmap, answer_rtpmap) != 0)) {
        continue;
      }
      /* else, dinamyc payload */

      sdp_utils_sdp_media_add_format (*offer_result, offer_format,
          offer_rtpmap);
      sdp_media_set_direction (*offer_result, offer_result_dir);
      sdp_utils_sdp_media_add_format (*answer_result, offer_format,
          offer_rtpmap);
      sdp_media_set_direction (*answer_result, answer_result_dir);
    }
  }

  if (gst_sdp_media_formats_len (*answer_result) == 0
      && gst_sdp_media_formats_len (*offer_result) == 0) {
    gst_sdp_media_free (*answer_result);
    gst_sdp_media_free (*offer_result);
    return GST_SDP_EINVAL;
  }

  return GST_SDP_OK;
}

GstSDPResult
sdp_utils_intersect_sdp_messages (const GstSDPMessage * offer,
    const GstSDPMessage * answer, GstSDPMessage ** offer_result,
    GstSDPMessage ** answer_result)
{
  GstSDPResult result;
  guint i, j, offer_medias_len, answer_medias_len;
  GList *ans_used_media_list = NULL;
  const GstSDPMedia *offer_media, *answer_media;
  GstSDPMedia *offer_media_result, *answer_media_result;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  result = sdp_message_create_from_src (offer, offer_result);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp message");
    goto end;
  }

  result = sdp_message_create_from_src (answer, answer_result);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp message");
    goto end;
  }

  offer_medias_len = gst_sdp_message_medias_len (offer);
  answer_medias_len = gst_sdp_message_medias_len (answer);

  for (i = 0; i < offer_medias_len; i++) {
    result = GST_SDP_EINVAL;
    offer_media = gst_sdp_message_get_media (offer, i);

    for (j = 0; j < answer_medias_len; j++) {
      if (g_list_find (ans_used_media_list, GUINT_TO_POINTER (j)) != NULL)
        continue;

      answer_media = gst_sdp_message_get_media (answer, j);

      result =
          intersect_sdp_medias (offer_media, answer_media,
          &offer_media_result, &answer_media_result);
      if (result == GST_SDP_OK) {
        ans_used_media_list =
            g_list_append (ans_used_media_list, GUINT_TO_POINTER (j));
        gst_sdp_message_add_media (*offer_result, offer_media_result);
        gst_sdp_message_add_media (*answer_result, answer_media_result);
        break;
      }
    }

    if (result != GST_SDP_OK) {
      if (sdp_media_create_from_src (offer_media,
              &offer_media_result) == GST_SDP_EINVAL)
        continue;

      if (sdp_media_create_from_src (offer_media,
              &answer_media_result) == GST_SDP_EINVAL) {
        gst_sdp_media_free (offer_media_result);
        continue;
      }

      if (offer_media->fmts->len > 0) {
        const gchar *offer_format = gst_sdp_media_get_format (offer_media, 0);
        const gchar *offer_rtpmap =
            sdp_media_get_rtpmap (offer_media, offer_format);

        sdp_utils_sdp_media_add_format (offer_media_result, offer_format,
            offer_rtpmap);
        sdp_utils_sdp_media_add_format (answer_media_result, offer_format,
            offer_rtpmap);
      }

      gst_sdp_media_set_port_info (offer_media_result, 0, 0);
      gst_sdp_media_set_port_info (answer_media_result, 0, 0);

      sdp_media_set_direction (offer_media_result, INACTIVE);
      sdp_media_set_direction (answer_media_result, INACTIVE);

      gst_sdp_message_add_media (*offer_result, offer_media_result);
      gst_sdp_message_add_media (*answer_result, answer_media_result);
    }
  }

  result = GST_SDP_OK;

end:
  g_list_free (ans_used_media_list);

  return result;
}
