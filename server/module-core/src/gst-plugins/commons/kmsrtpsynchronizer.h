/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#ifndef __KMS_RTP_SYNCHRONIZER_H__
#define __KMS_RTP_SYNCHRONIZER_H__

#include <gst/gst.h>
#include <gst/rtp/gstrtpbuffer.h>
#include <gst/rtp/gstrtcpbuffer.h>

G_BEGIN_DECLS

#define KMS_RTP_SYNC_ERROR \
  g_quark_from_static_string("kms-rtp-sync-error-quark")

typedef enum
{
  KMS_RTP_SYNC_INVALID_DATA,
  KMS_RTP_SYNC_UNEXPECTED_ERROR
} KmsRtpSyncError;

#define KMS_TYPE_RTP_SYNCHRONIZER \
  (kms_rtp_synchronizer_get_type())

#define KMS_RTP_SYNCHRONIZER(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (      \
    (obj),                          \
    KMS_TYPE_RTP_SYNCHRONIZER,      \
    KmsRtpSynchronizer              \
  )                                 \
)
#define KMS_RTP_SYNCHRONIZER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                 \
    (klass),                                \
    KMS_TYPE_RTP_SYNCHRONIZER,              \
    KmsRtpSynchronizerClass                 \
  )                                         \
)
#define KMS_IS_RTP_SYNCHRONIZER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (         \
    (obj),                             \
    KMS_TYPE_RTP_SYNCHRONIZER          \
  )                                    \
)
#define KMS_IS_RTP_SYNCHRONIZER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE (                    \
    (klass),                                   \
    KMS_TYPE_RTP_SYNCHRONIZER                  \
  )                                            \

#define KMS_RTP_SYNCHRONIZER_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                 \
    (obj),                                    \
    KMS_TYPE_RTP_SYNCHRONIZER,                \
    KmsRtpSynchronizerClass                   \
  )                                           \
)

#define KMS_RTP_SYNCHRONIZER_CAST(obj) ((KmsRtpSynchronizer*)(obj))

typedef struct _KmsRtpSynchronizer KmsRtpSynchronizer;
typedef struct _KmsRtpSynchronizerClass KmsRtpSynchronizerClass;
typedef struct _KmsRtpSynchronizerPrivate KmsRtpSynchronizerPrivate;

struct _KmsRtpSynchronizer
{
  GObject parent;

  KmsRtpSynchronizerPrivate *priv;
};

struct _KmsRtpSynchronizerClass
{
  GObjectClass parent_class;
};

GType kms_rtp_synchronizer_get_type ();

// 'stats_name': Name of the stats file that will be generated if
// the environment variable "KMS_RTP_SYNC_STATS_PATH" is set. Can be NULL.
KmsRtpSynchronizer * kms_rtp_synchronizer_new (gboolean feeded_ordered,
    const gchar * stats_name);

gboolean kms_rtp_synchronizer_set_pt_clock_rate (KmsRtpSynchronizer * self,
                                                     gint32 pt,
                                                     gint32 clock_rate,
                                                     GError ** error);

// Get timing info from the RTCP Sender Reports
gboolean kms_rtp_synchronizer_process_rtcp_buffer (KmsRtpSynchronizer * self,
                                                   GstBuffer * buffer,
                                                   GError ** error);

// Adjust PTS, using timing info from the RTCP Sender Reports
gboolean kms_rtp_synchronizer_process_rtp_buffer_writable (KmsRtpSynchronizer * self,
                                                  GstBuffer * buffer,
                                                  GError ** error);

G_END_DECLS

#endif /* __KMS_RTP_SYNCHRONIZER_H__ */
