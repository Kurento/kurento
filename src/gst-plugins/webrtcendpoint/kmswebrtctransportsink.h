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
