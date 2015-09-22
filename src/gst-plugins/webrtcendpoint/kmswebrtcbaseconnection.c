/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include "kmswebrtcbaseconnection.h"
#include <commons/kmsstats.h>
#include <kmsiceniceagent.h>

#define GST_CAT_DEFAULT kmswebrtcbaseconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "kmswebrtcbaseconnection"

G_DEFINE_TYPE (KmsWebRtcBaseConnection, kms_webrtc_base_connection,
    G_TYPE_OBJECT);

gboolean
kms_webrtc_base_connection_configure (KmsWebRtcBaseConnection * self,
    KmsIceBaseAgent * agent, const gchar * name)
{
  self->agent = g_object_ref (agent);
  self->name = g_strdup (name);

  self->stream_id = kms_ice_base_agent_add_stream (agent, self->name);

  if (g_strcmp0 (self->stream_id, "0") == 0) {
    GST_ERROR_OBJECT (self, "Cannot add stream for %s.", name);
    return FALSE;
  }

  return TRUE;
}

static gchar *kms_webrtc_base_connection_get_certificate_pem_default
    (KmsWebRtcBaseConnection * self)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_certificate_pem ==
      kms_webrtc_base_connection_get_certificate_pem_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_certificate_pem'",
        G_OBJECT_CLASS_NAME (klass));
  }

  return NULL;
}

static void
kms_webrtc_base_connection_finalize (GObject * object)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (object);

  GST_DEBUG_OBJECT (self, "finalize");

  kms_ice_base_agent_remove_stream (self->agent, self->stream_id);
  g_free (self->name);
  g_free (self->stream_id);
  g_clear_object (&self->agent);
  g_rec_mutex_clear (&self->mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_base_connection_parent_class)->finalize (object);
}

static void
kms_webrtc_base_connection_init (KmsWebRtcBaseConnection * self)
{
  g_rec_mutex_init (&self->mutex);
  self->stats_enabled = FALSE;
}

static void
kms_webrtc_base_connection_set_latency_callback_default (KmsIRtpConnection *
    obj, BufferLatencyCallback cb, gpointer user_data)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (obj);

  self->cb = cb;
  self->user_data = user_data;
}

static void
kms_webrtc_base_connection_collect_latency_stats_default (KmsIRtpConnection *
    obj, gboolean enable)
{
  KmsWebRtcBaseConnection *self = KMS_WEBRTC_BASE_CONNECTION (obj);

  self->stats_enabled = enable;
}

static void
kms_webrtc_base_connection_class_init (KmsWebRtcBaseConnectionClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_base_connection_finalize;

  klass->get_certificate_pem =
      kms_webrtc_base_connection_get_certificate_pem_default;

  klass->set_latency_callback =
      kms_webrtc_base_connection_set_latency_callback_default;
  klass->collect_latency_stats =
      kms_webrtc_base_connection_collect_latency_stats_default;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}

gchar *
kms_webrtc_base_connection_get_certificate_pem (KmsWebRtcBaseConnection * self)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_certificate_pem (self);
}

void
kms_webrtc_base_connection_set_stun_server_info (KmsWebRtcBaseConnection * self,
    const gchar * ip, guint port)
{
  if (KMS_IS_ICE_NICE_AGENT (self->agent)) {
    KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (self->agent);

    g_object_set (kms_ice_nice_agent_get_agent (nice_agent),
        "stun-server", ip, "stun-server-port", port, NULL);
  }
}

void
kms_webrtc_base_connection_set_relay_info (KmsWebRtcBaseConnection * self,
    const gchar * server_ip,
    guint server_port,
    const gchar * username, const gchar * password, TurnProtocol type)
{
  KmsIceRelayServerInfo info;

  info.server_ip = server_ip;
  info.server_port = server_port;
  info.username = username;
  info.password = password;
  info.type = type;
  info.stream_id = self->stream_id;

  kms_ice_base_agent_add_relay_server (self->agent, info);
}

void
kms_webrtc_base_connection_set_latency_callback (KmsIRtpConnection * self,
    BufferLatencyCallback cb, gpointer user_data)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_latency_callback (self, cb, user_data);
}

void
kms_webrtc_base_connection_collect_latency_stats (KmsIRtpConnection * self,
    gboolean enable)
{
  KmsWebRtcBaseConnectionClass *klass =
      KMS_WEBRTC_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->collect_latency_stats (self, enable);
}
