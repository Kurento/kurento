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

#include "kmsstats.h"
#include "kmsutils.h"
#include "kmsbufferlacentymeta.h"

struct _KmsStatsProbe
{
  GstPad *pad;
  KmsMediaType type;
  gulong probe_id;
};

typedef struct _BufferLatencyValues
{
  gboolean valid;
  KmsMediaType type;
} BufferLatencyValues;

typedef struct _ProbeData ProbeData;
typedef void (*BufferCb) (GstBuffer * buffer, ProbeData * pdata);

typedef struct _ProbeData
{
  BufferCb invoke_cb;
  gpointer invoke_data;
  GDestroyNotify destroy_invoke;

  GCallback cb;
  gpointer user_data;
  GDestroyNotify destroy_data;

  gboolean locked;
} ProbeData;

static BufferLatencyValues *
buffer_latency_values_new (gboolean is_valid, KmsMediaType type)
{
  BufferLatencyValues *blv;

  blv = g_slice_new (BufferLatencyValues);

  blv->valid = is_valid;
  blv->type = type;

  return blv;
}

static void
buffer_latency_values_destroy (BufferLatencyValues * blv)
{
  g_slice_free (BufferLatencyValues, blv);
}

static ProbeData *
probe_data_new (BufferCb invoke_cb, gpointer invoke_data,
    GDestroyNotify destroy_invoke, GCallback cb, gboolean locked,
    gpointer user_data, GDestroyNotify destroy_data)
{
  ProbeData *pdata;

  pdata = g_slice_new (ProbeData);

  pdata->invoke_cb = invoke_cb;
  pdata->invoke_data = invoke_data;
  pdata->destroy_invoke = destroy_invoke;

  pdata->cb = cb;
  pdata->user_data = user_data;
  pdata->destroy_data = destroy_data;

  pdata->locked = locked;

  return pdata;
}

static void
probe_data_destroy (ProbeData * pdata)
{
  if (pdata->user_data != NULL && pdata->destroy_data != NULL) {
    pdata->destroy_data (pdata->user_data);
  }

  if (pdata->invoke_data != NULL && pdata->destroy_invoke != NULL) {
    pdata->destroy_invoke (pdata->invoke_data);
  }

  g_slice_free (ProbeData, pdata);
}

static void
process_buffer (GstBuffer * buffer, gpointer user_data)
{
  ProbeData *pdata = user_data;

  if (pdata->invoke_cb != NULL) {
    pdata->invoke_cb (buffer, pdata);
  }
}

static gboolean
process_buffer_list_cb (GstBuffer ** buffer, guint idx, gpointer user_data)
{
  process_buffer (*buffer, user_data);

  return TRUE;
}

static GstPadProbeReturn
process_buffer_probe_cb (GstPad * pad, GstPadProbeInfo * info,
    gpointer user_data)
{
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BUFFER) {
    GstBuffer *buffer = gst_pad_probe_info_get_buffer (info);

    process_buffer (buffer, user_data);
  } else if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BUFFER_LIST) {
    GstBufferList *list = gst_pad_probe_info_get_buffer_list (info);

    gst_buffer_list_foreach (list, process_buffer_list_cb, user_data);
  }

  return GST_PAD_PROBE_OK;
}

GstStructure *
kms_stats_get_element_stats (GstStructure * stats)
{
  GstStructure *element_stats;
  const GValue *value;

  if (!gst_structure_has_field (stats, KMS_MEDIA_ELEMENT_FIELD)) {
    return NULL;
  }

  value = gst_structure_get_value (stats, KMS_MEDIA_ELEMENT_FIELD);

  if (!GST_VALUE_HOLDS_STRUCTURE (value)) {
    return NULL;
  }

  element_stats = (GstStructure *) gst_value_get_structure (value);

  if (g_strcmp0 (KMS_ELEMENT_STATS_STRUCT_NAME,
          gst_structure_get_name (element_stats)) != 0) {
    return NULL;
  }

  return element_stats;
}

static void
buffer_latency_probe_cb (GstBuffer * buffer, ProbeData * pdata)
{
  BufferLatencyValues *blv = (BufferLatencyValues *) pdata->invoke_data;
  GstClockTime time;

  time = kms_utils_get_time_nsecs ();

  kms_buffer_add_buffer_latency_meta (buffer, time, blv->valid, blv->type);
}

gulong
kms_stats_add_buffer_latency_meta_probe (GstPad * pad, gboolean is_valid,
    KmsMediaType type)
{
  ProbeData *pdata;

  BufferLatencyValues *blv;

  blv = buffer_latency_values_new (is_valid, type);

  pdata = probe_data_new (buffer_latency_probe_cb, blv,
      (GDestroyNotify) buffer_latency_values_destroy, NULL, FALSE, NULL, NULL);

  return gst_pad_add_probe (pad,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,
      process_buffer_probe_cb, pdata, (GDestroyNotify) probe_data_destroy);
}

static gboolean
buffer_for_each_meta_update_data_cb (GstBuffer * buffer, GstMeta ** meta,
    ProbeData * pdata)
{
  BufferLatencyValues *blv = (BufferLatencyValues *) pdata->invoke_data;
  KmsBufferLatencyMeta *blmeta;

  if ((*meta)->info->api != KMS_BUFFER_LATENCY_META_API_TYPE) {
    /* continue iterating */
    return TRUE;
  }

  blmeta = (KmsBufferLatencyMeta *) * meta;

  blmeta->type = blv->type;
  blmeta->valid = blv->valid;

  return TRUE;
}

static void
buffer_update_latency_probe_cb (GstBuffer * buffer, ProbeData * pdata)
{
  gst_buffer_foreach_meta (buffer,
      (GstBufferForeachMetaFunc) buffer_for_each_meta_update_data_cb, pdata);
}

gulong
kms_stats_add_buffer_update_latency_meta_probe (GstPad * pad, gboolean is_valid,
    KmsMediaType type)
{
  ProbeData *pdata;

  BufferLatencyValues *blv;

  blv = buffer_latency_values_new (is_valid, type);

  pdata = probe_data_new (buffer_update_latency_probe_cb, blv,
      (GDestroyNotify) buffer_latency_values_destroy, NULL, FALSE, NULL, NULL);

  return gst_pad_add_probe (pad,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,
      process_buffer_probe_cb, pdata, (GDestroyNotify) probe_data_destroy);
}

static gboolean
buffer_for_each_meta_cb (GstBuffer * buffer, GstMeta ** meta, ProbeData * pdata)
{
  BufferLatencyCallback func = (BufferLatencyCallback) pdata->cb;
  GstPad *pad = GST_PAD (pdata->invoke_data);
  KmsBufferLatencyMeta *blmeta;
  GstClockTimeDiff diff;
  GstClockTime now;

  if ((*meta)->info->api != KMS_BUFFER_LATENCY_META_API_TYPE) {
    /* continue iterating */
    return TRUE;
  }

  blmeta = (KmsBufferLatencyMeta *) * meta;

  if (!blmeta->valid) {
    /* Ignore this meta */
    return TRUE;
  }

  if (func == NULL) {
    return TRUE;
  }

  now = kms_utils_get_time_nsecs ();
  diff = GST_CLOCK_DIFF (blmeta->ts, now);

  if (pdata->locked) {
    KMS_BUFFER_LATENCY_DATA_LOCK (blmeta);
  }

  func (pad, blmeta->type, diff, blmeta->data, pdata->user_data);

  if (pdata->locked) {
    KMS_BUFFER_LATENCY_DATA_UNLOCK (blmeta);
  }

  return TRUE;
}

static void
buffer_latency_calculation_cb (GstBuffer * buffer, ProbeData * pdata)
{
  gst_buffer_foreach_meta (buffer,
      (GstBufferForeachMetaFunc) buffer_for_each_meta_cb, pdata);
}

gulong
kms_stats_add_buffer_latency_notification_probe (GstPad * pad,
    BufferLatencyCallback cb, gboolean locked, gpointer user_data,
    GDestroyNotify destroy_data)
{
  ProbeData *pdata;

  pdata = probe_data_new (buffer_latency_calculation_cb, pad, NULL,
      G_CALLBACK (cb), locked, user_data, destroy_data);

  return gst_pad_add_probe (pad,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,
      process_buffer_probe_cb, pdata, (GDestroyNotify) probe_data_destroy);
}

KmsStatsProbe *
kms_stats_probe_new (GstPad * pad, KmsMediaType type)
{
  KmsStatsProbe *probe;

  probe = g_slice_new0 (KmsStatsProbe);
  probe->pad = GST_PAD (g_object_ref (pad));
  probe->type = type;

  return probe;
}

void
kms_stats_probe_destroy (KmsStatsProbe * probe)
{
  kms_stats_probe_remove (probe);

  g_object_unref (probe->pad);

  g_slice_free (KmsStatsProbe, probe);
}

void
kms_stats_probe_add_latency (KmsStatsProbe * probe,
    BufferLatencyCallback callback, gboolean locked, gpointer user_data,
    GDestroyNotify destroy_data)
{
  kms_stats_probe_remove (probe);

  probe->probe_id = kms_stats_add_buffer_latency_notification_probe (probe->pad,
      callback, locked, user_data, destroy_data);
}

void
kms_stats_probe_latency_meta_set_valid (KmsStatsProbe * probe,
    gboolean is_valid)
{
  kms_stats_probe_remove (probe);

  probe->probe_id = kms_stats_add_buffer_update_latency_meta_probe (probe->pad,
      is_valid, probe->type);
}

void
kms_stats_probe_remove (KmsStatsProbe * probe)
{
  if (probe->probe_id != 0UL) {
    gst_pad_remove_probe (probe->pad, probe->probe_id);
    probe->probe_id = 0UL;
  }
}

gboolean
kms_stats_probe_watches (KmsStatsProbe * probe, GstPad * pad)
{
  return pad == probe->pad;
}

gchar *
kms_stats_create_id_for_pad (GstElement * obj, GstPad * pad)
{
  gchar *id, *padname, *objname;

  padname = gst_pad_get_name (pad);
  objname = gst_element_get_name (obj);

  id = g_strdup_printf ("%s_%s", objname, padname);

  g_free (padname);
  g_free (objname);

  return id;
}

static void
kms_stats_stream_e2e_avg_stat_destroy (StreamE2EAvgStat * stat)
{
  g_slice_free (StreamE2EAvgStat, stat);
}

StreamE2EAvgStat *
kms_stats_stream_e2e_avg_stat_new (KmsMediaType type)
{
  StreamE2EAvgStat *stat;

  stat = g_slice_new0 (StreamE2EAvgStat);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (stat),
      (GDestroyNotify) kms_stats_stream_e2e_avg_stat_destroy);
  stat->type = type;

  return stat;
}
