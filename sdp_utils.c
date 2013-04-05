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
  guint i;

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

  for (i = 0; i < gst_sdp_message_times_len (src); i++) {
    const GstSDPTime *time = gst_sdp_message_get_time (src, i);
    gchar **repeat;
    gint j;

    repeat = g_malloc (time->repeat->len) + 1;
    for (j = 0; j < time->repeat->len; j++) {
      repeat[j] = g_array_index (time->repeat, gchar *, j);
    }
    repeat[time->repeat->len] = NULL;

    gst_sdp_message_add_time (*msg, time->start, time->stop,
        (const gchar **) repeat);
    g_free (repeat);
  }

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

/**
 * Returns : a newly-allocated string or NULL if any.
 * The returned string should be freed with g_free() when no longer needed.
 */
static gchar *
sdp_media_get_rtpmap (const GstSDPMedia * media, const gchar * format)
{
  guint i, attrs_len;
  const GstSDPAttribute *attr;
  gchar *rtpmap = NULL;
  gchar **tokens = NULL;

  attrs_len = gst_sdp_media_attributes_len (media);

  for (i = 0; i < attrs_len; i++) {
    attr = gst_sdp_media_get_attribute (media, i);
    if (g_ascii_strncasecmp (RTPMAP, attr->key, g_utf8_strlen (RTPMAP,
                -1)) == 0) {
      if (g_ascii_strncasecmp (format, attr->value, g_utf8_strlen (format,
                  -1)) == 0) {
        tokens = g_strsplit (attr->value, " ", 2);
        rtpmap = g_strdup (tokens[1]);
        break;
      }
    }
  }

  g_strfreev (tokens);

  return rtpmap;
}

static GstSDPResult
intersect_sdp_medias (const GstSDPMedia * offer,
    const GstSDPMedia * answer, GstSDPMedia ** offer_result,
    GstSDPMedia ** answer_result)
{
  GstSDPResult result;
  guint i, j;
  guint offer_format_len, answer_format_len;
  const gchar *offer_format, *answer_format;
  const gchar *offer_media_type, *answer_media_type;
  GstSDPDirection offer_dir, answer_dir, offer_result_dir, answer_result_dir;
  gchar *offer_rtpmap, *answer_rtpmap, *rtpmap_result;

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
    offer_format = gst_sdp_media_get_format (offer, i);
    offer_rtpmap = sdp_media_get_rtpmap (offer, offer_format);

    for (j = 0; j < answer_format_len; j++) {
      answer_format = gst_sdp_media_get_format (answer, j);
      answer_rtpmap = sdp_media_get_rtpmap (answer, answer_format);

      if ((offer_rtpmap == NULL && answer_rtpmap == NULL)
          && (g_ascii_strncasecmp (offer_format, answer_format,
                  g_utf8_strlen (offer_format, -1)) == 0)) {
        /* static payload */
        gst_sdp_media_add_format (*offer_result, offer_format);
        gst_sdp_media_add_attribute (*offer_result,
            sdp_utils_get_direction_str (offer_result_dir), "");
        gst_sdp_media_add_format (*answer_result, offer_format);
        gst_sdp_media_add_attribute (*answer_result,
            sdp_utils_get_direction_str (answer_result_dir), "");
      } else if (offer_rtpmap == NULL || answer_rtpmap == NULL) {
        continue;
      } else if (g_ascii_strncasecmp (offer_rtpmap, answer_rtpmap,
              g_utf8_strlen (offer_rtpmap, -1)) == 0) {
        /* dinamyc payload */
        rtpmap_result = g_strconcat (offer_format, " ", offer_rtpmap, NULL);
        gst_sdp_media_add_format (*offer_result, offer_format);
        gst_sdp_media_add_attribute (*offer_result, RTPMAP, rtpmap_result);
        gst_sdp_media_add_attribute (*offer_result,
            sdp_utils_get_direction_str (offer_result_dir), "");
        gst_sdp_media_add_format (*answer_result, offer_format);
        gst_sdp_media_add_attribute (*answer_result, RTPMAP, rtpmap_result);
        gst_sdp_media_add_attribute (*answer_result,
            sdp_utils_get_direction_str (answer_result_dir), "");
      }
      g_free (answer_rtpmap);
    }
    g_free (offer_rtpmap);
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
    for (j = 0; j < answer_medias_len; j++) {
      if (g_list_find (ans_used_media_list, GUINT_TO_POINTER (j)) != NULL)
        continue;

      offer_media = gst_sdp_message_get_media (offer, i);
      answer_media = gst_sdp_message_get_media (answer, j);

      result =
          intersect_sdp_medias (offer_media, answer_media, &offer_media_result,
          &answer_media_result);
      if (result == GST_SDP_OK) {
        ans_used_media_list =
            g_list_append (ans_used_media_list, GUINT_TO_POINTER (j));
        gst_sdp_message_add_media (*offer_result, offer_media_result);
        gst_sdp_message_add_media (*answer_result, answer_media_result);
      }
    }
  }

  result = GST_SDP_OK;

end:
  g_list_free (ans_used_media_list);

  return result;
}
