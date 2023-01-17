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

#ifndef __KMS_I_RTP_CONNECTION_H__
#define __KMS_I_RTP_CONNECTION_H__

#include <gst/gst.h>
#include "kmsstats.h"

G_BEGIN_DECLS

/* KmsIRtpConnection begin */
#define KMS_TYPE_I_RTP_CONNECTION \
  (kms_i_rtp_connection_get_type())
#define KMS_I_RTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_I_RTP_CONNECTION,KmsIRtpConnection))
#define KMS_IS_I_RTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_I_RTP_CONNECTION))
#define KMS_I_RTP_CONNECTION_CAST(obj) ((KmsIRtpConnection*)(obj))
#define KMS_I_RTP_CONNECTION_GET_INTERFACE(obj) \
  (G_TYPE_INSTANCE_GET_INTERFACE((obj),KMS_TYPE_I_RTP_CONNECTION,KmsIRtpConnectionInterface))
#define KMS_I_RTP_CONNECTION_INTERFACE(iface) ((KmsIRtpConnectionInterface *) iface)

typedef struct _KmsIRtpConnection KmsIRtpConnection;
typedef struct _KmsIRtpConnectionInterface KmsIRtpConnectionInterface;

struct _KmsIRtpConnectionInterface
{
  GTypeInterface parent;

  /* virtual methods */
  void (*add) (KmsIRtpConnection *self, GstBin * bin, gboolean active);
  void (*src_sync_state_with_parent) (KmsIRtpConnection *self);
  void (*sink_sync_state_with_parent) (KmsIRtpConnection *self);

  GstPad * (*request_rtp_sink) (KmsIRtpConnection *self);
  GstPad * (*request_rtp_src) (KmsIRtpConnection *self);
  GstPad * (*request_rtcp_sink) (KmsIRtpConnection *self);
  GstPad * (*request_rtcp_src) (KmsIRtpConnection *self);
  GstPad * (*request_data_src) (KmsIRtpConnection *self);
  GstPad * (*request_data_sink) (KmsIRtpConnection *self);

  void (*set_latency_callback) (KmsIRtpConnection *self, BufferLatencyCallback cb, gpointer user_data);
  void (*collect_latency_stats) (KmsIRtpConnection *self, gboolean enable);

  /* Signals */
  void (*connected_signal) (KmsIRtpConnection * self);
};

GType kms_i_rtp_connection_get_type (void);

void kms_i_rtp_connection_add (KmsIRtpConnection *self, GstBin * bin, gboolean active);
void kms_i_rtp_connection_src_sync_state_with_parent (KmsIRtpConnection *self);
void kms_i_rtp_connection_sink_sync_state_with_parent (KmsIRtpConnection *self);

void kms_i_rtp_connection_set_latency_callback (KmsIRtpConnection *self, BufferLatencyCallback cb, gpointer user_data);
void kms_i_rtp_connection_collect_latency_stats (KmsIRtpConnection *self, gboolean enable);

GstPad * kms_i_rtp_connection_request_rtp_sink (KmsIRtpConnection *self);
GstPad * kms_i_rtp_connection_request_rtp_src (KmsIRtpConnection *self);
GstPad * kms_i_rtp_connection_request_rtcp_sink (KmsIRtpConnection *self);
GstPad * kms_i_rtp_connection_request_rtcp_src (KmsIRtpConnection *self);
GstPad * kms_i_rtp_connection_request_data_src (KmsIRtpConnection *self);
GstPad * kms_i_rtp_connection_request_data_sink (KmsIRtpConnection *self);

void kms_i_rtp_connection_connected_signal (KmsIRtpConnection *self);

/* KmsIRtpConnection end */

/* KmsIRtcpMuxConnection begin */
#define KMS_TYPE_I_RTCP_MUX_CONNECTION \
  (kms_i_rtcp_mux_connection_get_type())
#define KMS_I_RTCP_MUX_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_I_RTCP_MUX_CONNECTION,KmsIRtcpMuxConnection))
#define KMS_IS_I_RTCP_MUX_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_I_RTCP_MUX_CONNECTION))
#define KMS_I_RTCP_MUX_CONNECTION_CAST(obj) ((KmsIRtcpMuxConnection*)(obj))
#define KMS_I_RTCP_MUX_CONNECTION_GET_INTERFACE(obj) \
  (G_TYPE_INSTANCE_GET_INTERFACE((obj),KMS_TYPE_I_RTCP_MUX_CONNECTION,KmsIRtcpMuxConnectionInterface))
#define KMS_I_RTCP_MUX_CONNECTION_INTERFACE(iface) ((KmsIRtcpMuxConnectionInterface *) iface)

typedef struct _KmsIRtcpMuxConnection KmsIRtcpMuxConnection;
typedef struct _KmsIRtcpMuxConnectionInterface KmsIRtcpMuxConnectionInterface;

struct _KmsIRtcpMuxConnectionInterface
{
  KmsIRtpConnectionInterface parent;
};

GType kms_i_rtcp_mux_connection_get_type (void);
/* KmsIRtcpMuxConnection end */

/* KmsIBundleConnection begin */
#define KMS_TYPE_I_BUNDLE_CONNECTION \
  (kms_i_bundle_connection_get_type())
#define KMS_I_BUNDLE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_I_BUNDLE_CONNECTION,KmsIBundleConnection))
#define KMS_IS_I_BUNDLE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_I_BUNDLE_CONNECTION))
#define KMS_I_BUNDLE_CONNECTION_CAST(obj) ((KmsIBundleConnection*)(obj))
#define KMS_I_BUNDLE_CONNECTION_GET_INTERFACE(obj) \
  (G_TYPE_INSTANCE_GET_INTERFACE((obj),KMS_TYPE_I_RTCP_MUX_CONNECTION,KmsIBundleConnectionInterface))
#define KMS_I_BUNDLE_CONNECTION_INTERFACE(iface) ((KmsIBundleConnectionInterface *) iface)

typedef struct _KmsIBundleConnection KmsIBundleConnection;
typedef struct _KmsIBundleConnectionInterface KmsIBundleConnectionInterface;

struct _KmsIBundleConnectionInterface
{
  KmsIRtcpMuxConnectionInterface parent;
};

GType kms_i_bundle_connection_get_type (void);
/* KmsIBundleConnection end */

G_END_DECLS
#endif /* __KMS_I_RTP_CONNECTION_H__ */
