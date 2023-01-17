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
#ifndef __KMS_STATS_H__
#define __KMS_STATS_H__

#include "gst/gst.h"
#include "kmsmediatype.h"
#include "kmslist.h"
#include "kmsrefstruct.h"

G_BEGIN_DECLS

#define KMS_MEDIA_ELEMENT_FIELD "media-element"
#define KMS_RTC_STATISTICS_FIELD "rtc-statistics"
#define KMS_DATA_SESSION_STATISTICS_FIELD "data-session-statistics"
#define KMS_ELEMENT_STATS_STRUCT_NAME "element-stats"
#define KMS_RTP_STRUCT_NAME "rtp-stats"
#define KMS_SESSIONS_STRUCT_NAME "session-stats"
#define KMS_DATA_SESSION_STRUCT_NAME "data-session-stats"

/* Macros used to calculate latency stats */
#define KMS_STATS_ALPHA 0.25
#define KMS_STATS_CALCULATE_LATENCY_AVG(ti, ax) ({        \
  (ti) * KMS_STATS_ALPHA + (ax) * (1 - KMS_STATS_ALPHA);  \
})

GstStructure * kms_stats_get_element_stats (GstStructure *stats);

/* buffer latency */
typedef void (*BufferLatencyCallback) (GstPad * pad, KmsMediaType type, GstClockTimeDiff t, KmsList *data, gpointer user_data);
gulong kms_stats_add_buffer_latency_meta_probe (GstPad * pad, gboolean is_valid, KmsMediaType type);
gulong kms_stats_add_buffer_update_latency_meta_probe (GstPad * pad, gboolean is_valid, KmsMediaType type);
gulong kms_stats_add_buffer_latency_notification_probe (GstPad * pad, BufferLatencyCallback cb, gboolean locked, gpointer user_data, GDestroyNotify destroy_data);

typedef struct _KmsStatsProbe KmsStatsProbe;

KmsStatsProbe * kms_stats_probe_new (GstPad *pad, KmsMediaType type);
void kms_stats_probe_destroy (KmsStatsProbe *probe);
void kms_stats_probe_add_latency (KmsStatsProbe *probe, BufferLatencyCallback callback,
  gboolean locked, gpointer user_data, GDestroyNotify destroy_data);
void kms_stats_probe_latency_meta_set_valid (KmsStatsProbe *probe, gboolean is_valid);
void kms_stats_probe_remove (KmsStatsProbe *probe);
gboolean kms_stats_probe_watches (KmsStatsProbe *probe, GstPad *pad);

typedef struct _StreamE2EAvgStat
{
  KmsRefStruct ref;
  KmsMediaType type;
  gdouble avg;
} StreamE2EAvgStat;

gchar * kms_stats_create_id_for_pad (GstElement * obj, GstPad * pad);
StreamE2EAvgStat * kms_stats_stream_e2e_avg_stat_new (KmsMediaType type);

#define kms_stats_stream_e2e_avg_stat_ref(obj) \
  (StreamE2EAvgStat *) kms_ref_struct_ref (KMS_REF_STRUCT_CAST (obj))
#define kms_stats_stream_e2e_avg_stat_unref(obj) \
  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (obj))

G_END_DECLS

#endif /* __KMS_STATS_H__ */
