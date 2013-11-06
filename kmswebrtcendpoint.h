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

#ifndef __KMS_WEBRTC_END_POINT_H__
#define __KMS_WEBRTC_END_POINT_H__

#include <kmsbasertpendpoint.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_END_POINT \
  (kms_webrtc_end_point_get_type())
#define KMS_WEBRTC_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_END_POINT,KmsWebrtcEndPoint))
#define KMS_WEBRTC_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_END_POINT,KmsWebrtcEndPointClass))
#define KMS_IS_WEBRTC_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_END_POINT))
#define KMS_IS_WEBRTC_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_END_POINT))
#define KMS_WEBRTC_END_POINT_CAST(obj) ((KmsWebrtcEndPoint*)(obj))

typedef struct _KmsWebrtcEndPoint KmsWebrtcEndPoint;
typedef struct _KmsWebrtcEndPointClass KmsWebrtcEndPointClass;

struct _KmsWebrtcEndPoint
{
  KmsBaseRtpEndPoint parent;
};

struct _KmsWebrtcEndPointClass
{
  KmsBaseRtpEndPointClass parent_class;
};

GType kms_webrtc_end_point_get_type (void);

gboolean kms_webrtc_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_WEBRTC_END_POINT_H__ */
