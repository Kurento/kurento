/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifndef __KMS_BASE_RTP_ENDPOINT_H__
#define __KMS_BASE_RTP_ENDPOINT_H__

#include <gst/gst.h>
#include <kmsbasesdpendpoint.h>
#include "kmsmediatype.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_RTP_ENDPOINT \
  (kms_base_rtp_endpoint_get_type())
#define KMS_BASE_RTP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_RTP_ENDPOINT,KmsBaseRtpEndpoint))
#define KMS_BASE_RTP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_RTP_ENDPOINT,KmsBaseRtpEndpointClass))
#define KMS_IS_BASE_RTP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_RTP_ENDPOINT))
#define KMS_IS_BASE_RTP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_RTP_ENDPOINT))
#define KMS_BASE_RTP_ENDPOINT_CAST(obj) ((KmsBaseRtpEndpoint*)(obj))
typedef struct _KmsBaseRtpEndpoint KmsBaseRtpEndpoint;
typedef struct _KmsBaseRtpEndpointClass KmsBaseRtpEndpointClass;
typedef struct _KmsBaseRtpEndpointPrivate KmsBaseRtpEndpointPrivate;

#define KMS_BASE_RTP_ENDPOINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_RTP_ENDPOINT_CAST ((elem))->media_mutex))
#define KMS_BASE_RTP_ENDPOINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_RTP_ENDPOINT_CAST ((elem))->media_mutex))

/* rtpbin pad names */
#define AUDIO_RTPBIN_RECV_RTP_SINK "recv_rtp_sink_0"
#define AUDIO_RTPBIN_RECV_RTCP_SINK "recv_rtcp_sink_0"
#define AUDIO_RTPBIN_SEND_RTP_SRC "send_rtp_src_0"
#define AUDIO_RTPBIN_SEND_RTCP_SRC "send_rtcp_src_0"
#define AUDIO_RTPBIN_SEND_RTP_SINK "send_rtp_sink_0"
#define VIDEO_RTPBIN_RECV_RTP_SINK "recv_rtp_sink_1"
#define VIDEO_RTPBIN_RECV_RTCP_SINK "recv_rtcp_sink_1"
#define VIDEO_RTPBIN_SEND_RTP_SRC "send_rtp_src_1"
#define VIDEO_RTPBIN_SEND_RTCP_SRC "send_rtcp_src_1"
#define VIDEO_RTPBIN_SEND_RTP_SINK "send_rtp_sink_1"

/* RTP enconding names */
#define OPUS_ENCONDING_NAME "OPUS"
#define VP8_ENCONDING_NAME "VP8"

struct _KmsBaseRtpEndpoint
{
  KmsBaseSdpEndpoint parent;

  KmsBaseRtpEndpointPrivate *priv;
};

struct _KmsBaseRtpEndpointClass
{
  KmsBaseSdpEndpointClass parent_class;

  void (*media_start) (KmsBaseRtpEndpoint * self, KmsMediaType type,
    gboolean local);
  void (*media_stop) (KmsBaseRtpEndpoint * self, KmsMediaType type,
    gboolean local);
};

GType kms_base_rtp_endpoint_get_type (void);

/* rtpbin must be used helding the KmsBaseRtpEndpoint's lock */
GstElement *kms_base_rtp_endpoint_get_rtpbin (KmsBaseRtpEndpoint * self);

G_END_DECLS
#endif /* __KMS_BASE_RTP_ENDPOINT_H__ */
