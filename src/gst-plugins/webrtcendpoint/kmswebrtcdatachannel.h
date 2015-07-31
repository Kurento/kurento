/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#ifndef __KMS_WEBRTC_DATA_CHANNEL_H__
#define __KMS_WEBRTC_DATA_CHANNEL_H__

#include <gst/gst.h>
#include "kmswebrtcdatachannelbin.h"

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_DATA_CHANNEL \
  (kms_webrtc_data_channel_get_type())
#define KMS_WEBRTC_DATA_CHANNEL(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST(          \
    (obj),                             \
    KMS_TYPE_WEBRTC_DATA_CHANNEL,      \
    KmsWebRtcDataChannel               \
  )                                    \
)
#define KMS_WEBRTC_DATA_CHANNEL_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST(                     \
    (klass),                                   \
    KMS_TYPE_WEBRTC_DATA_CHANNEL,              \
    KmsWebRtcDataChannelClass                  \
  )                                            \
)
#define KMS_IS_WEBRTC_DATA_CHANNEL(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_DATA_CHANNEL))
#define KMS_IS_WEBRTC_DATA_CHANNEL_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_DATA_CHANNEL))
#define KMS_WEBRTC_DATA_CHANNEL_CAST(obj) ((KmsWebRtcDataChannel*)(obj))
#define KMS_WEBRTC_DATA_CHANNEL_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                    \
    (obj),                                       \
    KMS_TYPE_WEBRTC_DATA_CHANNEL,                \
    KmsWebRtcDataChannelClass                    \
  )                                              \
)

typedef struct _KmsWebRtcDataChannel KmsWebRtcDataChannel;
typedef struct _KmsWebRtcDataChannelClass KmsWebRtcDataChannelClass;
typedef struct _KmsWebRtcDataChannelPrivate KmsWebRtcDataChannelPrivate;

struct _KmsWebRtcDataChannel
{
  GObject parent;

  /*< private > */
  KmsWebRtcDataChannelPrivate *priv;
};

struct _KmsWebRtcDataChannelClass
{
  GObjectClass parent_class;
};

GType kms_webrtc_data_channel_get_type (void);

KmsWebRtcDataChannel * kms_webrtc_data_channel_new (KmsWebRtcDataChannelBin *channel_bin);

void kms_webrtc_data_channel_set_new_buffer_callback (KmsWebRtcDataChannel *channel, DataChannelNewBuffer cb, gpointer user_data, GDestroyNotify notify);
GstFlowReturn kms_webrtc_data_channel_push_buffer (KmsWebRtcDataChannel *channel, GstBuffer *buffer, gboolean is_binary);

G_END_DECLS

#endif /* __KMS_WEBRTC_DATA_CHANNEL_H__ */
