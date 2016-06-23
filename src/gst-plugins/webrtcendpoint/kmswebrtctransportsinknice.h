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

#ifndef __KMS_WEBRTC_TRANSPORT_SINK_NICE_H__
#define __KMS_WEBRTC_TRANSPORT_SINK_NICE_H__

#include <gst/gst.h>
#include "kmswebrtctransportsink.h"

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
