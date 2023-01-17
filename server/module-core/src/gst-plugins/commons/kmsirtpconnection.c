/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include "kmsirtpconnection.h"

enum
{
  SIGNAL_CONNECTED,
  LAST_SIGNAL
};

#define DEFAULT_MIN_PORT 1024
#define DEFAULT_MAX_PORT 65535

static guint kms_i_rtp_connection_signals[LAST_SIGNAL] = { 0 };

/* KmsIRtpConnection begin */
G_DEFINE_INTERFACE (KmsIRtpConnection, kms_i_rtp_connection, 0);

static void
kms_i_rtp_connection_default_init (KmsIRtpConnectionInterface * iface)
{
  kms_i_rtp_connection_signals[SIGNAL_CONNECTED] =
      g_signal_new ("connected",
      G_TYPE_FROM_CLASS (iface),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsIRtpConnectionInterface, connected_signal), NULL,
      NULL, g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  g_object_interface_install_property (iface, g_param_spec_boolean ("connected",
          "Connected", "Indicates if connection is already connected", FALSE,
          G_PARAM_READWRITE));

  g_object_interface_install_property (iface, g_param_spec_boolean ("added",
          "Added", "Indicates if connection is already added", FALSE,
          G_PARAM_READWRITE));

  g_object_interface_install_property (iface, g_param_spec_boolean ("is-client",
          "Is client", "True when connection is client", FALSE,
          G_PARAM_READABLE));

  g_object_interface_install_property (iface, g_param_spec_uint ("min-port",
          "Min port", "Minimum port connection should use", 0, 65535,
          DEFAULT_MIN_PORT, G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY));

  g_object_interface_install_property (iface, g_param_spec_uint ("max-port",
          "Max port", "Maximum port connection should use", 0, 65535,
          DEFAULT_MAX_PORT, G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY));
}

void
kms_i_rtp_connection_add (KmsIRtpConnection * self, GstBin * bin,
    gboolean active)
{
  g_return_if_fail (KMS_IS_I_RTP_CONNECTION (self));

  KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->add (self, bin, active);
  g_object_set (G_OBJECT (self), "added", TRUE, NULL);
}

void
kms_i_rtp_connection_src_sync_state_with_parent (KmsIRtpConnection * self)
{
  g_return_if_fail (KMS_IS_I_RTP_CONNECTION (self));

  KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->src_sync_state_with_parent (self);
}

void
kms_i_rtp_connection_sink_sync_state_with_parent (KmsIRtpConnection * self)
{
  g_return_if_fail (KMS_IS_I_RTP_CONNECTION (self));

  KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->sink_sync_state_with_parent (self);
}

GstPad *
kms_i_rtp_connection_request_rtp_sink (KmsIRtpConnection * self)
{
  g_return_val_if_fail (KMS_IS_I_RTP_CONNECTION (self), NULL);

  return KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_rtp_sink (self);
}

GstPad *
kms_i_rtp_connection_request_rtp_src (KmsIRtpConnection * self)
{
  g_return_val_if_fail (KMS_IS_I_RTP_CONNECTION (self), NULL);

  return KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_rtp_src (self);
}

GstPad *
kms_i_rtp_connection_request_rtcp_sink (KmsIRtpConnection * self)
{
  g_return_val_if_fail (KMS_IS_I_RTP_CONNECTION (self), NULL);

  return KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_rtcp_sink (self);
}

GstPad *
kms_i_rtp_connection_request_rtcp_src (KmsIRtpConnection * self)
{
  g_return_val_if_fail (KMS_IS_I_RTP_CONNECTION (self), NULL);

  return KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_rtcp_src (self);
}

GstPad *
kms_i_rtp_connection_request_data_src (KmsIRtpConnection * self)
{
  g_return_val_if_fail (KMS_IS_I_RTP_CONNECTION (self), NULL);

  if (KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_rtcp_src == NULL) {
    GST_WARNING_OBJECT (self, "Do not support data src pads");
    return NULL;
  }

  return KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_data_src (self);
}

GstPad *
kms_i_rtp_connection_request_data_sink (KmsIRtpConnection * self)
{
  g_return_val_if_fail (KMS_IS_I_RTP_CONNECTION (self), NULL);

  if (KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_rtcp_src == NULL) {
    GST_WARNING_OBJECT (self, "Do not support data sink pads");
    return NULL;
  }

  return KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->request_data_sink (self);
}

void
kms_i_rtp_connection_connected_signal (KmsIRtpConnection * self)
{
  g_object_set (G_OBJECT (self), "connected", TRUE, NULL);
  g_signal_emit (G_OBJECT (self),
      kms_i_rtp_connection_signals[SIGNAL_CONNECTED], 0);
}

void
kms_i_rtp_connection_set_latency_callback (KmsIRtpConnection * self,
    BufferLatencyCallback cb, gpointer user_data)
{
  g_return_if_fail (KMS_IS_I_RTP_CONNECTION (self));

  if (KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->set_latency_callback == NULL) {
    GST_WARNING_OBJECT (self, "Do not support latency management");
    return;
  }

  KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->set_latency_callback (self, cb,
      user_data);
}

void
kms_i_rtp_connection_collect_latency_stats (KmsIRtpConnection * self,
    gboolean enable)
{
  g_return_if_fail (KMS_IS_I_RTP_CONNECTION (self));

  if (KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->collect_latency_stats == NULL) {
    GST_WARNING_OBJECT (self, "Do not support latency management");
    return;
  }

  KMS_I_RTP_CONNECTION_GET_INTERFACE (self)->collect_latency_stats (self,
      enable);
}

/* KmsIRtpConnection end */

/* KmsIRtcpMuxConnection begin */
G_DEFINE_INTERFACE (KmsIRtcpMuxConnection, kms_i_rtcp_mux_connection,
    KMS_TYPE_I_RTP_CONNECTION);

static void
kms_i_rtcp_mux_connection_default_init (KmsIRtcpMuxConnectionInterface * iface)
{
  /* Nothing to do */
}

/* KmsIRtcpMuxConnection end */

/* KmsIBundleConnection begin */
G_DEFINE_INTERFACE (KmsIBundleConnection, kms_i_bundle_connection,
    KMS_TYPE_I_RTCP_MUX_CONNECTION);

static void
kms_i_bundle_connection_default_init (KmsIBundleConnectionInterface * iface)
{
  /* Nothing to do */
}

/* KmsIBundleConnection end */
