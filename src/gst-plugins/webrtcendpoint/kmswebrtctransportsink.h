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

#ifndef __KMS_WEBRTC_TRANSPORT_SINK_H__
#define __KMS_WEBRTC_TRANSPORT_SINK_H__

#include <gst/gst.h>
#include "kmsicebaseagent.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_TRANSPORT_SINK \
  (kms_webrtc_transport_sink_get_type())
#define KMS_WEBRTC_TRANSPORT_SINK(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_TRANSPORT_SINK,KmsWebrtcTransportSink))
#define KMS_WEBRTC_TRANSPORT_SINK_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_TRANSPORT_SINK,KmsWebrtcTransportSinkClass))
#define KMS_IS_WEBRTC_TRANSPORT_SINK(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_TRANSPORT_SINK))
#define KMS_IS_WEBRTC_TRANSPORT_SINK_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_TRANSPORT_SINK))
#define KMS_WEBRTC_TRANSPORT_SINK_CAST(obj) ((KmsWebrtcTransportSink*)(obj))

typedef struct _KmsWebrtcTransportSink KmsWebrtcTransportSink;
typedef struct _KmsWebrtcTransportSinkClass KmsWebrtcTransportSinkClass;

struct _KmsWebrtcTransportSink
{
  GstBin parent;

  GstElement *dtlssrtpenc;
  GstElement *sink;
};

struct _KmsWebrtcTransportSinkClass
{
  GstBinClass parent_class;

  /* virtual methods */
  void (*configure) (KmsWebrtcTransportSink * self,
                          KmsIceBaseAgent *agent,
                          const char *stream_id,
                          guint component_id);
};

GType kms_webrtc_transport_sink_get_type (void);

KmsWebrtcTransportSink * kms_webrtc_transport_sink_new ();
void kms_webrtc_transport_sink_connect_elements (KmsWebrtcTransportSink *self);
void kms_webrtc_transport_sink_configure (KmsWebrtcTransportSink * self,
                                              KmsIceBaseAgent *agent,
                                              const char *stream_id,
                                              guint component_id);
G_END_DECLS
#endif /* __KMS_WEBRTC_TRANSPORT_SINK_H__ */
