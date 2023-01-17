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

#include "kmsrtpbaseconnection.h"
#include <gio/gio.h>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kmsrtpbaseconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define GST_DEFAULT_NAME "kmsrtpbaseconnection"

G_DEFINE_TYPE (KmsRtpBaseConnection, kms_rtp_base_connection, G_TYPE_OBJECT);

void
kms_rtp_base_connection_remove_probe (KmsRtpBaseConnection * self,
    GstElement * e, const gchar * pad_name, gulong id)
{
  GstPad *pad;

  if (id == 0UL) {
    return;
  }

  pad = gst_element_get_static_pad (e, pad_name);
  gst_pad_remove_probe (pad, id);
  g_object_unref (pad);
}

static guint
kms_rtp_base_connection_get_rtp_port_default (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_rtp_port == kms_rtp_base_connection_get_rtp_port_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_rtp_port'", G_OBJECT_CLASS_NAME (klass));
  }

  return 0;
}

static guint
kms_rtp_base_connection_get_rtcp_port_default (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_rtcp_port == kms_rtp_base_connection_get_rtcp_port_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_rtcp_port'", G_OBJECT_CLASS_NAME (klass));
  }

  return 0;
}

static void
kms_rtp_base_connection_set_remote_info_default (KmsRtpBaseConnection * self,
    const gchar * host, gint rtp_port, gint rtcp_port)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->set_remote_info == kms_rtp_base_connection_set_remote_info_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'set_remote_info'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_rtp_base_connection_init (KmsRtpBaseConnection * self)
{
  g_rec_mutex_init (&self->mutex);
  self->stats_enabled = FALSE;
}

static void
kms_rtp_base_connection_set_latency_callback_default (KmsIRtpConnection *
    obj, BufferLatencyCallback cb, gpointer user_data)
{
  KmsRtpBaseConnection *self = KMS_RTP_BASE_CONNECTION (obj);

  self->cb = cb;
  self->user_data = user_data;
}

static void
kms_rtp_base_connection_collect_latency_stats_default (KmsIRtpConnection *
    obj, gboolean enable)
{
  KmsRtpBaseConnection *self = KMS_RTP_BASE_CONNECTION (obj);

  self->stats_enabled = enable;
}

static void
kms_rtp_base_connection_finalize (GObject * object)
{
  KmsRtpBaseConnection *self = KMS_RTP_BASE_CONNECTION (object);

  g_rec_mutex_clear (&self->mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_rtp_base_connection_parent_class)->finalize (object);
}

static void
kms_rtp_base_connection_class_init (KmsRtpBaseConnectionClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_rtp_base_connection_finalize;

  klass->get_rtp_port = kms_rtp_base_connection_get_rtp_port_default;
  klass->get_rtcp_port = kms_rtp_base_connection_get_rtcp_port_default;
  klass->set_remote_info = kms_rtp_base_connection_set_remote_info_default;

  klass->set_latency_callback =
      kms_rtp_base_connection_set_latency_callback_default;
  klass->collect_latency_stats =
      kms_rtp_base_connection_collect_latency_stats_default;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}

guint
kms_rtp_base_connection_get_rtp_port (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_rtp_port (self);
}

guint
kms_rtp_base_connection_get_rtcp_port (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_rtcp_port (self);
}

void
kms_rtp_base_connection_set_remote_info (KmsRtpBaseConnection * self,
    const gchar * host, gint rtp_port, gint rtcp_port)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_remote_info (self, host, rtp_port, rtcp_port);
}

void
kms_rtp_base_connection_set_latency_callback (KmsIRtpConnection * self,
    BufferLatencyCallback cb, gpointer user_data)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_latency_callback (self, cb, user_data);
}

void
kms_rtp_base_connection_collect_latency_stats (KmsIRtpConnection * self,
    gboolean enable)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->collect_latency_stats (self, enable);
}
