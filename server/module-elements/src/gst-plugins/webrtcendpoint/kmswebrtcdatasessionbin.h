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

#ifndef __KMS_WEBRTC_DATA_SESSION_BIN_H__
#define __KMS_WEBRTC_DATA_SESSION_BIN_H__

#include <gst/gst.h>
#include "kmswebrtcdatachannel.h"

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_DATA_SESSION_BIN \
  (kms_webrtc_data_session_bin_get_type())
#define KMS_WEBRTC_DATA_SESSION_BIN(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST(              \
    (obj),                                 \
    KMS_TYPE_WEBRTC_DATA_SESSION_BIN,      \
    KmsWebRtcDataSessionBin                \
  )                                        \
)
#define KMS_WEBRTC_DATA_SESSION_BIN_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST(                         \
    (klass),                                       \
    KMS_TYPE_WEBRTC_DATA_SESSION_BIN,              \
    KmsWebRtcDataSessionBinClass                   \
  )                                                \
)
#define KMS_IS_WEBRTC_DATA_SESSION_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_DATA_SESSION_BIN))
#define KMS_IS_WEBRTC_DATA_SESSION_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_DATA_SESSION_BIN))
#define KMS_WEBRTC_DATA_SESSION_BIN_CAST(obj) ((KmsWebRtcDataSessionBin*)(obj))
#define KMS_WEBRTC_DATA_SESSION_BIN_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                        \
    (obj),                                           \
    KMS_TYPE_WEBRTC_DATA_SESSION_BIN,                \
    KmsWebRtcDataSessionBinClass                     \
  )                                                  \
)

typedef struct _KmsWebRtcDataSessionBin KmsWebRtcDataSessionBin;
typedef struct _KmsWebRtcDataSessionBinClass KmsWebRtcDataSessionBinClass;
typedef struct _KmsWebRtcDataSessionBinPrivate KmsWebRtcDataSessionBinPrivate;

struct _KmsWebRtcDataSessionBin
{
  GstBin parent;

  /*< private > */
  KmsWebRtcDataSessionBinPrivate *priv;
};

struct _KmsWebRtcDataSessionBinClass
{
  GstBinClass parent_class;

  /* signals */
  void (*data_channel_opened) (KmsWebRtcDataSessionBin *self, guint stream_id);
  void (*data_channel_closed) (KmsWebRtcDataSessionBin *self, guint stream_id);
  void (*data_session_established) (KmsWebRtcDataSessionBin *self, gboolean connected);

  /* actions */
  gint (*create_data_channel) (KmsWebRtcDataSessionBin *self, gboolean ordered, gint max_packet_life_time, gint max_retransmits, const gchar * label, const gchar * protocol);
  void (*destroy_data_channel) (KmsWebRtcDataSessionBin *self, gint stream_id);
  KmsWebRtcDataChannel * (*get_data_channel) (KmsWebRtcDataSessionBin *self, guint stream_id);
  GstStructure * (*stats) (KmsWebRtcDataSessionBin * self);
};

GType kms_webrtc_data_session_bin_get_type (void);

KmsWebRtcDataSessionBin * kms_webrtc_data_session_bin_new (gboolean dtls_client_mode);

G_END_DECLS

#endif /* __KMS_WEBRTC_DATA_SESSION_BIN_H__ */
