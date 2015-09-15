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

#ifndef __KMS_WEBRTC_TRANSPORT_SINK_NICE_H__
#define __KMS_WEBRTC_TRANSPORT_SINK_NICE_H__

#include <gst/gst.h>
#include <kmswebrtctransportsink.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE \
  (kms_webrtc_transport_sink_nice_get_type())
#define KMS_WEBRTC_TRANSPORT_SINK_NICE(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE,KmsWebrtcTransportSinkNice))
#define KMS_WEBRTC_TRANSPORT_SINK_NICE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE,KmsWebrtcTransportSinkNiceClass))
#define KMS_IS_WEBRTC_TRANSPORT_SINK_NICE(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE))
#define KMS_IS_WEBRTC_TRANSPORT_SINK_NICE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE))
#define KMS_WEBRTC_TRANSPORT_SINK_NICE_CAST(obj) ((KmsWebrtcTransportSinkNice*)(obj))

typedef struct _KmsWebrtcTransportSinkNice KmsWebrtcTransportSinkNice;
typedef struct _KmsWebrtcTransportSinkNiceClass KmsWebrtcTransportSinkNiceClass;

struct _KmsWebrtcTransportSinkNice
{
  KmsWebrtcTransportSink parent;
};

struct _KmsWebrtcTransportSinkNiceClass
{
  KmsWebrtcTransportSinkClass parent_class;
};

GType kms_webrtc_transport_sink_nice_get_type (void);

KmsWebrtcTransportSinkNice * kms_webrtc_transport_sink_nice_new ();

G_END_DECLS
#endif /* __KMS_WEBRTC_TRANSPORT_SINK_NICE_H__ */
