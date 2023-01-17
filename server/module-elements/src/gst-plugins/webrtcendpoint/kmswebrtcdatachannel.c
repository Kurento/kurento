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
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmswebrtcdatachannel.h"

#define PLUGIN_NAME "kmswebrtcdatachannel"

GST_DEBUG_CATEGORY_STATIC (kms_webrtc_data_channel_debug_category);
#define GST_CAT_DEFAULT kms_webrtc_data_channel_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsWebRtcDataChannel, kms_webrtc_data_channel,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_webrtc_data_channel_debug_category,
        PLUGIN_NAME, 0, "debug category for webrtc_data_channel"));

#define parent_class kms_webrtc_data_channel_parent_class

#define KMS_WEBRTC_DATA_CHANNEL_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                    \
    (obj),                                         \
    KMS_TYPE_WEBRTC_DATA_CHANNEL,                  \
    KmsWebRtcDataChannelPrivate                    \
  )                                                \
)

struct _KmsWebRtcDataChannelPrivate
{
  KmsWebRtcDataChannelBin *channel_bin;
  DataChannelNewBuffer cb;
  gpointer user_data;
  GDestroyNotify notify;
  GRecMutex mutex;
};

#define KMS_WEBRTC_DATA_CHANNEL_LOCK(obj) \
  (g_rec_mutex_lock (&KMS_WEBRTC_DATA_CHANNEL_CAST ((obj))->priv->mutex))
#define KMS_WEBRTC_DATA_CHANNEL_UNLOCK(obj) \
  (g_rec_mutex_unlock (&KMS_WEBRTC_DATA_CHANNEL_CAST ((obj))->priv->mutex))

static void
kms_webrtc_data_channel_bin_finalize (GObject * object)
{
  KmsWebRtcDataChannel *self = KMS_WEBRTC_DATA_CHANNEL (object);

  GST_DEBUG_OBJECT (self, "finalize");

  if (self->priv->notify != NULL) {
    self->priv->notify (self->priv->user_data);
  }

  g_rec_mutex_clear (&self->priv->mutex);

  /* chain up */
  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_webrtc_data_channel_class_init (KmsWebRtcDataChannelClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->finalize = kms_webrtc_data_channel_bin_finalize;

  g_type_class_add_private (klass, sizeof (KmsWebRtcDataChannelPrivate));
}

static GstFlowReturn
kms_webrtc_data_channel_new_buffer (GObject * obj, GstBuffer * buffer,
    gpointer user_data)
{
  KmsWebRtcDataChannel *self = KMS_WEBRTC_DATA_CHANNEL (user_data);
  DataChannelNewBuffer cb;
  gpointer data;

  KMS_WEBRTC_DATA_CHANNEL_LOCK (self);

  cb = self->priv->cb;
  data = self->priv->user_data;

  KMS_WEBRTC_DATA_CHANNEL_UNLOCK (self);

  if (cb != NULL) {
    return cb (G_OBJECT (self), buffer, data);
  }

  return GST_FLOW_OK;
}

static void
kms_webrtc_data_channel_init (KmsWebRtcDataChannel * self)
{
  self->priv = KMS_WEBRTC_DATA_CHANNEL_GET_PRIVATE (self);
  g_rec_mutex_init (&self->priv->mutex);
}

KmsWebRtcDataChannel *
kms_webrtc_data_channel_new (KmsWebRtcDataChannelBin * channel_bin)
{
  KmsWebRtcDataChannel *obj;

  obj = KMS_WEBRTC_DATA_CHANNEL (g_object_new (KMS_TYPE_WEBRTC_DATA_CHANNEL,
          NULL));

  obj->priv->channel_bin = channel_bin;

  kms_webrtc_data_channel_bin_set_new_buffer_callback (channel_bin,
      kms_webrtc_data_channel_new_buffer, obj, NULL);

  return obj;
}

void
kms_webrtc_data_channel_set_new_buffer_callback (KmsWebRtcDataChannel * channel,
    DataChannelNewBuffer cb, gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  KMS_WEBRTC_DATA_CHANNEL_LOCK (channel);

  data = channel->priv->user_data;
  destroy = channel->priv->notify;

  channel->priv->notify = notify;
  channel->priv->user_data = user_data;
  channel->priv->cb = cb;

  KMS_WEBRTC_DATA_CHANNEL_UNLOCK (channel);

  if (destroy != NULL) {
    destroy (data);
  }
}

GstFlowReturn
kms_webrtc_data_channel_push_buffer (KmsWebRtcDataChannel * channel,
    GstBuffer * buff, gboolean is_binary)
{
  GstBuffer *buffer;

  buffer = gst_buffer_ref (buff);

  if (channel == NULL) {
    gst_buffer_unref (buffer);
    g_return_val_if_reached (GST_FLOW_ERROR);
  }

  return kms_webrtc_data_channel_bin_push_buffer (channel->priv->channel_bin,
      buffer, is_binary);
}
