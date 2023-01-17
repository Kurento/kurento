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

#ifndef __KMS_WEBRTC_DATA_CHANNEL_BIN_H__
#define __KMS_WEBRTC_DATA_CHANNEL_BIN_H__

#include <gst/gst.h>

#include "kmswebrtcdatachannelutil.h"

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN \
  (kms_webrtc_data_channel_bin_get_type())
#define KMS_WEBRTC_DATA_CHANNEL_BIN(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST(              \
    (obj),                                 \
    KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN,      \
    KmsWebRtcDataChannelBin                \
  )                                        \
)
#define KMS_WEBRTC_DATA_CHANNEL_BIN_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST(                         \
    (klass),                                       \
    KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN,              \
    KmsWebRtcDataChannelBinClass                   \
  )                                                \
)
#define KMS_IS_WEBRTC_DATA_CHANNEL_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN))
#define KMS_IS_WEBRTC_DATA_CHANNEL_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN))
#define KMS_WEBRTC_DATA_CHANNEL_BIN_CAST(obj) ((KmsWebRtcDataChannelBin*)(obj))
#define KMS_WEBRTC_DATA_CHANNEL_BIN_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                        \
    (obj),                                           \
    KMS_TYPE_WEBRTC_DATA_CHANNEL_BIN,                \
    KmsWebRtcDataChannelBinClass                     \
  )                                                  \
)

typedef struct _KmsWebRtcDataChannelBin KmsWebRtcDataChannelBin;
typedef struct _KmsWebRtcDataChannelBinClass KmsWebRtcDataChannelBinClass;
typedef struct _KmsWebRtcDataChannelBinPrivate KmsWebRtcDataChannelBinPrivate;

struct _KmsWebRtcDataChannelBin
{
  GstBin parent;

  /*< private > */
  KmsWebRtcDataChannelBinPrivate *priv;
};

struct _KmsWebRtcDataChannelBinClass
{
  GstBinClass parent_class;

  /* signals */
  void (*negotiated) (KmsWebRtcDataChannelBin *self);

  /* actions */
  void (*request_open) (KmsWebRtcDataChannelBin *self);
  void (*request_close) (KmsWebRtcDataChannelBin *self);
};

GType kms_webrtc_data_channel_bin_get_type (void);

typedef void (*ResetStreamFunc) (KmsWebRtcDataChannelBin *channel, gpointer user_data);

KmsWebRtcDataChannelBin * kms_webrtc_data_channel_bin_new (guint id, gboolean ordered, gint max_packet_life_time, gint max_retransmits, const gchar *label, const gchar *protocol);
GstCaps * kms_webrtc_data_channel_bin_create_caps (KmsWebRtcDataChannelBin *self);
void kms_webrtc_data_channel_bin_set_new_buffer_callback (KmsWebRtcDataChannelBin *self, DataChannelNewBuffer cb, gpointer user_data, GDestroyNotify notify);
void kms_webrtc_data_channel_bin_set_reset_stream_callback (KmsWebRtcDataChannelBin *self, ResetStreamFunc cb, gpointer user_data, GDestroyNotify notify);
GstFlowReturn kms_webrtc_data_channel_bin_push_buffer (KmsWebRtcDataChannelBin *self, GstBuffer *buffer, gboolean is_binary);

G_END_DECLS

#endif /* __KMS_WEBRTC_DATA_CHANNEL_BIN_H__ */
