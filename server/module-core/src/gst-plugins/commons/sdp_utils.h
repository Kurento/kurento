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

#ifndef __SDP_H__
#define __SDP_H__

#include <gst/sdp/gstsdpmessage.h>
#include "gstsdpdirection.h"

#include <glib.h> // G_MAXUINT

#define SENDONLY_STR  "sendonly"
#define RECVONLY_STR  "recvonly"
#define SENDRECV_STR  "sendrecv"
#define INACTIVE_STR  "inactive"

#define SSRC_INVALID G_MAXUINT

typedef gboolean (*GstSDPMediaFunc) (const GstSDPMedia *media, gpointer user_data);
typedef gboolean (*GstSDPIntersectMediaFunc) (const GstSDPAttribute *attr, gpointer user_data);

gboolean sdp_utils_is_attribute_in_media (const GstSDPMedia * media, const GstSDPAttribute * attr);
gboolean sdp_utils_attribute_is_direction (const GstSDPAttribute * attr, GstSDPDirection * direction);
guint sdp_utils_media_get_ssrc (const GstSDPMedia * media);
guint sdp_utils_media_get_fid_ssrc (const GstSDPMedia * media, guint pos);
GstSDPDirection sdp_utils_media_config_get_direction (const GstSDPMedia * media);
gboolean sdp_utils_media_config_set_direction (GstSDPMedia * media, GstSDPDirection direction);

const gchar *sdp_utils_sdp_media_get_rtpmap (const GstSDPMedia * media,
    const gchar * format);
const gchar * sdp_utils_sdp_media_get_fmtp (const GstSDPMedia * media, const gchar * format);

gboolean sdp_utils_intersect_session_attributes (const GstSDPMessage * msg, GstSDPIntersectMediaFunc func, gpointer user_data);
gboolean sdp_utils_intersect_media_attributes (const GstSDPMedia * offer, GstSDPIntersectMediaFunc func, gpointer user_data);

const gchar *sdp_utils_get_attr_map_value (const GstSDPMedia * media, const gchar *name, const gchar * fmt);

gboolean sdp_utils_for_each_media (const GstSDPMessage * msg, GstSDPMediaFunc func, gpointer user_data);
gboolean sdp_utils_media_is_active (const GstSDPMedia * media, gboolean offerer);

gboolean sdp_utils_rtcp_fb_attr_check_type (const gchar * attr, const gchar * pt, const gchar * type);
gboolean sdp_utils_media_has_remb (const GstSDPMedia * media);
gboolean sdp_utils_media_has_rtcp_nack (const GstSDPMedia * media);

gboolean sdp_utils_equal_medias (const GstSDPMedia * m1, const GstSDPMedia * m2);
gboolean sdp_utils_equal_messages (const GstSDPMessage * msg1, const GstSDPMessage * msg2);

gboolean sdp_utils_get_data_from_rtpmap (const gchar * rtpmap, gchar ** codec_name, gint * clock_rate);
gboolean sdp_utils_get_data_from_rtpmap_codec (const GstSDPMedia * media, const gchar * codec, gint *pt, gint * clock_rate);
gboolean sdp_utils_is_pt_in_fmts (const GstSDPMedia * media, gint pt);

gint sdp_utils_get_pt_for_codec_name (const GstSDPMedia *media, const gchar *codec_name);

gint sdp_utils_get_abs_send_time_id (const GstSDPMedia * media);
gboolean sdp_utils_media_is_inactive (const GstSDPMedia * media);

#endif /* __SDP_H__ */
