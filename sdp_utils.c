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

static GstSDPResult
create_sdp_message_from_src (const GstSDPMessage * src, GstSDPMessage ** msg)
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
intersect_sdp_medias (const GstSDPMedia * offer,
    const GstSDPMedia * answer, GstSDPMedia ** offer_result,
    GstSDPMedia ** answer_result)
{
  GST_DEBUG ("intersect_sdp_medias");
  return GST_SDP_OK;
}

GstSDPResult
sdp_utils_intersect_sdp_messages (const GstSDPMessage * offer,
    const GstSDPMessage * answer, GstSDPMessage ** offer_result,
    GstSDPMessage ** answer_result)
{
  GstSDPResult result;
  guint i, j, offer_len, answer_len;
  GList *ans_used_media_list = NULL;
  const GstSDPMedia *offer_media, *answer_media;
  GstSDPMedia *offer_media_result, *answer_media_result;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  result = create_sdp_message_from_src (offer, offer_result);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp message");
    goto end;
  }

  result = create_sdp_message_from_src (answer, answer_result);
  if (result != GST_SDP_OK) {
    GST_ERROR ("Error creating sdp message");
    goto end;
  }

  offer_len = gst_sdp_message_medias_len (offer);
  answer_len = gst_sdp_message_medias_len (answer);

  for (i = 0; i < offer_len; i++) {
    for (j = 0; j < answer_len; j++) {
      if (g_list_find (ans_used_media_list, GUINT_TO_POINTER (j)) != NULL)
        continue;

      offer_media = gst_sdp_message_get_media (offer, i);
      answer_media = gst_sdp_message_get_media (answer, j);

      result =
          intersect_sdp_medias (offer_media, answer_media, &offer_media_result,
          &answer_media_result);
      if (result != GST_SDP_OK) {
        GST_ERROR ("Error creating sdp message");
        gst_sdp_message_free (*offer_result);
        gst_sdp_message_free (*answer_result);
        goto end;
      }

      ans_used_media_list =
          g_list_append (ans_used_media_list, GUINT_TO_POINTER (j));
    }
  }

  result = GST_SDP_OK;

end:
  g_list_free (ans_used_media_list);

  return result;
}
