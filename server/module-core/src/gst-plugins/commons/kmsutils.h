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
#ifndef __KMS_UTILS_H__
#define __KMS_UTILS_H__

#include "gst/gst.h"
#include "kmsmediatype.h"
#include "kmselementpadtype.h"

G_BEGIN_DECLS

typedef void (*KmsPadIterationAction) (GstPad * pad, gpointer data);
typedef void (*KmsPadCallback) (GstPad * pad, gpointer data);

void kms_element_for_each_src_pad (GstElement * element,
  KmsPadCallback action, gpointer data);
gboolean kms_element_for_each_sink_pad (GstElement * element,
  KmsPadCallback action, gpointer data);

void kms_utils_debug_graph_delay (GstBin * bin, guint interval);

gboolean gst_element_sync_state_with_parent_target_state (GstElement * element);

/* ---- GstBin ---- */

/*
 * Remove a GstElement from a GstBin, with proper state changes.
 */
void kms_utils_bin_remove (GstBin * bin, GstElement * element);

/* ---- GstElement ---- */

/*
 * Make a new GstElement with an unique name as generated by
 * gst_element_factory_make(), but with the given prefix.
 */
GstElement *kms_utils_element_factory_make (const gchar *factoryname,
    const gchar *name_prefix);

/* Caps */
gboolean kms_utils_caps_is_audio (const GstCaps * caps);
gboolean kms_utils_caps_is_video (const GstCaps * caps);
gboolean kms_utils_caps_is_data (const GstCaps * caps);
gboolean kms_utils_caps_is_rtp (const GstCaps * caps);
gboolean kms_utils_caps_is_raw (const GstCaps * caps);

GstElement *kms_utils_create_convert_for_caps (const GstCaps *caps,
    const gchar *name_prefix);
GstElement *kms_utils_create_mediator_element (const GstCaps *caps,
    const gchar *name_prefix);
GstElement *kms_utils_create_rate_for_caps (const GstCaps *caps,
    const gchar *name_prefix);

const gchar * kms_utils_get_caps_codec_name_from_sdp (const gchar * codec_name);

KmsElementPadType kms_utils_convert_media_type (KmsMediaType media_type);
KmsMediaType kms_utils_convert_element_pad_type (KmsElementPadType pad_type);

/* keyframe management */
void kms_utils_drop_until_keyframe (GstPad *pad, gboolean all_headers);
void kms_utils_pad_monitor_gaps (GstPad *pad);
void kms_utils_control_key_frames_request_duplicates (GstPad *pad);

/* Pad blocked action */
void kms_utils_execute_with_pad_blocked (GstPad * pad, gboolean drop, KmsPadCallback func, gpointer userData);

/* REMB event */
GstEvent * kms_utils_remb_event_upstream_new (guint bitrate, guint ssrc);
gboolean kms_utils_is_remb_event_upstream (GstEvent * event);
gboolean kms_utils_remb_event_upstream_parse (GstEvent *event, guint *bitrate, guint *ssrc);

typedef struct _RembEventManager RembEventManager;
typedef void (*RembBitrateUpdatedCallback) (RembEventManager * manager, guint bitrate, gpointer user_data);
RembEventManager * kms_utils_remb_event_manager_create (GstPad *pad);
void kms_utils_remb_event_manager_destroy (RembEventManager * manager);
void kms_utils_remb_event_manager_pointer_destroy (gpointer manager);
guint kms_utils_remb_event_manager_get_min (RembEventManager * manager);
void kms_utils_remb_event_manager_set_callback (RembEventManager * manager, RembBitrateUpdatedCallback cb, gpointer data, GDestroyNotify destroy_notify);
void kms_utils_remb_event_manager_set_clear_interval (RembEventManager * manager, GstClockTime interval);
GstClockTime kms_utils_remb_event_manager_get_clear_interval (RembEventManager * manager);

/* time */
GstClockTime kms_utils_get_time_nsecs ();

gboolean kms_utils_contains_proto (const gchar *search_term, const gchar *proto);
const GstStructure * kms_utils_get_structure_by_name (const GstStructure *str, const gchar *name);

gchar * kms_utils_generate_uuid ();
void kms_utils_set_uuid (GObject *obj);
const gchar * kms_utils_get_uuid (GObject *obj);

const char * kms_utils_media_type_to_str (KmsMediaType type);

gchar * kms_utils_generate_fingerprint_from_pem (const gchar * pem);

/* Set event function for this pad. This function variant allows to keep */
/* previous callbacks enabled if chain callbacks is TRUE                 */
void kms_utils_set_pad_event_function_full (GstPad *pad, GstPadEventFunction event, gpointer user_data, GDestroyNotify notify, gboolean chain_callbacks);

/* Set query function for this pad. This function variant allows to keep */
/* previous callbacks enabled if chain callbacks is TRUE                 */
void kms_utils_set_pad_query_function_full (GstPad *pad, GstPadQueryFunction query, gpointer user_data, GDestroyNotify notify, gboolean chain_callbacks);

void kms_utils_depayloader_monitor_pts_out (GstElement * depayloader);

/* Get wether an IP address is IPv4 or IPv6. */
int kms_utils_get_ip_version (const gchar *ip_address);

/* Type destroying */
#define KMS_UTILS_DESTROY_H(type) void kms_utils_destroy_##type (type * data);
KMS_UTILS_DESTROY_H (guint64)
KMS_UTILS_DESTROY_H (gsize)
KMS_UTILS_DESTROY_H (GstClockTime)
KMS_UTILS_DESTROY_H (gfloat)
KMS_UTILS_DESTROY_H (guint)

G_END_DECLS

#endif /* __KMS_UTILS_H__ */
