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

#ifndef __KMS_BUFFER_LATENCY_META_H__
#define __KMS_BUFFER_LATENCY_META_H__

#include <gst/gst.h>

#include "kmsmediatype.h"
#include "kmslist.h"

G_BEGIN_DECLS

typedef struct _KmsBufferLatencyMeta KmsBufferLatencyMeta;

/**
 * KmsBufferLatencyMeta:
 * @meta: the parent type
 * @ts: The time stamp
 *
 * Buffer metadata for measuring buffer latency since the buffer is generated
 * until it is processed by a sink.
 */
struct _KmsBufferLatencyMeta {
  GstMeta       meta;

  GstClockTime ts;
  KmsMediaType type;
  gboolean valid;

  GRecMutex datamutex;
  KmsList *data; /* <string, refstruct> */
};

#define KMS_BUFFER_LATENCY_DATA_LOCK(mdata) \
  (g_rec_mutex_lock (&((KmsBufferLatencyMeta *)mdata)->datamutex))
#define KMS_BUFFER_LATENCY_DATA_UNLOCK(mdata) \
  (g_rec_mutex_unlock (&((KmsBufferLatencyMeta *)mdata)->datamutex))

GType kms_buffer_latency_meta_api_get_type (void);
#define KMS_BUFFER_LATENCY_META_API_TYPE \
  (kms_buffer_latency_meta_api_get_type())

#define kms_buffer_get_buffer_latency_meta(b) \
  ((KmsBufferLatencyMeta*)gst_buffer_get_meta((b), KMS_BUFFER_LATENCY_META_API_TYPE))

/* implementation */
const GstMetaInfo *kms_buffer_latency_meta_get_info (void);
#define KMS_BUFFER_LATENCY_META_INFO (kms_buffer_latency_meta_get_info ())

KmsBufferLatencyMeta * kms_buffer_add_buffer_latency_meta (GstBuffer *buffer,
  GstClockTime ts, gboolean valid, KmsMediaType type);

G_END_DECLS

#endif /* __KMS_BUFFER_LATENCY_META_H__ */
