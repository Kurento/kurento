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
#ifndef __KMS_BASE_RTP_END_POINT_H__
#define __KMS_BASE_RTP_END_POINT_H__

#include <gst/gst.h>
#include <kmsbasesdpendpoint.h>
#include "kmsmediatype.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_RTP_END_POINT \
  (kms_base_rtp_end_point_get_type())
#define KMS_BASE_RTP_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_RTP_END_POINT,KmsBaseRtpEndPoint))
#define KMS_BASE_RTP_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_RTP_END_POINT,KmsBaseRtpEndPointClass))
#define KMS_IS_BASE_RTP_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_RTP_END_POINT))
#define KMS_IS_BASE_RTP_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_RTP_END_POINT))
#define KMS_BASE_RTP_END_POINT_CAST(obj) ((KmsBaseRtpEndPoint*)(obj))
typedef struct _KmsBaseRtpEndPoint KmsBaseRtpEndPoint;
typedef struct _KmsBaseRtpEndPointClass KmsBaseRtpEndPointClass;
typedef struct _KmsBaseRtpEndPointPrivate KmsBaseRtpEndPointPrivate;

#define KMS_BASE_RTP_END_POINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_RTP_END_POINT_CAST ((elem))->media_mutex))
#define KMS_BASE_RTP_END_POINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_RTP_END_POINT_CAST ((elem))->media_mutex))

#define AUDIO_RTPBIN_SEND_SINK "send_rtp_sink_0"
#define VIDEO_RTPBIN_SEND_SINK "send_rtp_sink_1"

struct _KmsBaseRtpEndPoint
{
  KmsBaseSdpEndPoint parent;

  GstElement *rtpbin;
  KmsBaseRtpEndPointPrivate *priv;
};

struct _KmsBaseRtpEndPointClass
{
  KmsBaseSdpEndPointClass parent_class;

  void (*media_start) (KmsBaseRtpEndPoint * self, KmsMediaType type,
    gboolean local);
  void (*media_stop) (KmsBaseRtpEndPoint * self, KmsMediaType type,
    gboolean local);
};

GType kms_base_rtp_end_point_get_type (void);

G_END_DECLS
#endif /* __KMS_BASE_RTP_END_POINT_H__ */
