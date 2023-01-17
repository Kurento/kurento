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

#include "kmsrefstruct.h"
#include "kmsbufferlacentymeta.h"

GType
kms_buffer_latency_meta_api_get_type (void)
{
  static GType initialization_value = 0;
  if (g_once_init_enter (&initialization_value)) {
    static const gchar *tags[] = {NULL};
    GType type = gst_meta_api_type_register ("KmsBufferLatencyMetaAPI", tags);
    g_once_init_leave (&initialization_value, type);
  }
  return initialization_value;
}

static gboolean
kms_buffer_latency_meta_init (GstMeta * meta, gpointer params,
    GstBuffer * buffer)
{
  KmsBufferLatencyMeta *lmeta = (KmsBufferLatencyMeta *) meta;

  lmeta->ts = GST_CLOCK_TIME_NONE;
  lmeta->valid = FALSE;

  g_rec_mutex_init (&lmeta->datamutex);
  lmeta->data = kms_list_new_full (g_str_equal, g_free,
      (GDestroyNotify) kms_ref_struct_unref);

  return TRUE;
}

static gboolean
kms_buffer_latency_meta_transform (GstBuffer * transbuf, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  KmsBufferLatencyMeta *new_meta, *lmeta;

  /* we always copy no matter what transform */
  if (!GST_META_TRANSFORM_IS_COPY (type)) {
    return TRUE;
  }

  lmeta = (KmsBufferLatencyMeta *) meta;
  new_meta = kms_buffer_add_buffer_latency_meta (transbuf, lmeta->ts,
      lmeta->valid, lmeta->type);

  if (new_meta == NULL) {
    return FALSE;
  }

  if (new_meta->data != NULL) {
    kms_list_unref (new_meta->data);
  }

  KMS_BUFFER_LATENCY_DATA_LOCK (lmeta);
  new_meta->data = kms_list_ref (lmeta->data);
  KMS_BUFFER_LATENCY_DATA_UNLOCK (lmeta);

  return TRUE;
}

static void
kms_buffer_latency_meta_free (GstMeta * meta, GstBuffer * buffer)
{
  KmsBufferLatencyMeta *lmeta = (KmsBufferLatencyMeta *) meta;

  KMS_BUFFER_LATENCY_DATA_LOCK (lmeta);
  kms_list_unref (lmeta->data);
  KMS_BUFFER_LATENCY_DATA_UNLOCK (lmeta);

  g_rec_mutex_clear (&lmeta->datamutex);
}

const GstMetaInfo *
kms_buffer_latency_meta_get_info (void)
{
  static GstMetaInfo *initialization_value = 0;
  if (g_once_init_enter (&initialization_value)) {
    GstMetaInfo *meta_info = (GstMetaInfo *)gst_meta_register (
        KMS_BUFFER_LATENCY_META_API_TYPE, "KmsBufferLatencyMeta",
        sizeof (KmsBufferLatencyMeta), kms_buffer_latency_meta_init,
        kms_buffer_latency_meta_free, kms_buffer_latency_meta_transform);
    g_once_init_leave (&initialization_value, meta_info);
  }
  return initialization_value;
}

KmsBufferLatencyMeta *
kms_buffer_add_buffer_latency_meta (GstBuffer * buffer, GstClockTime ts,
    gboolean valid, KmsMediaType type)
{
  KmsBufferLatencyMeta *meta;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);

  meta = (KmsBufferLatencyMeta *) gst_buffer_add_meta (buffer,
      KMS_BUFFER_LATENCY_META_INFO, NULL);

  meta->ts = ts;
  meta->valid = valid;
  meta->type = type;

  return meta;
}
