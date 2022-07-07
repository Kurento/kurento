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

#ifndef __KMS_WEBRTC_TRANSPORT_SRC_H__
#define __KMS_WEBRTC_TRANSPORT_SRC_H__

#include <gst/gst.h>
#include "kmsicebaseagent.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_TRANSPORT_SRC \
  (kms_webrtc_transport_src_get_type())
#define KMS_WEBRTC_TRANSPORT_SRC(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_TRANSPORT_SRC,KmsWebrtcTransportSrc))
#define KMS_WEBRTC_TRANSPORT_SRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_TRANSPORT_SRC,KmsWebrtcTransportSrcClass))
#define KMS_IS_WEBRTC_TRANSPORT_SRC(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_TRANSPORT_SRC))
#define KMS_IS_WEBRTC_TRANSPORT_SRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_TRANSPORT_SRC))
#define KMS_WEBRTC_TRANSPORT_SRC_CAST(obj) ((KmsWebrtcTransportSrc*)(obj))

typedef struct _KmsWebrtcTransportSrc KmsWebrtcTransportSrc;
typedef struct _KmsWebrtcTransportSrcClass KmsWebrtcTransportSrcClass;

struct _KmsWebrtcTransportSrc
{
  GstBin parent;

  GstElement *src;
  GstElement *dtlssrtpdec;
  gulong src_probe;
};

struct _KmsWebrtcTransportSrcClass
{
  GstBinClass parent_class;

  /* virtual methods */
  void (*configure) (KmsWebrtcTransportSrc * self,
                         KmsIceBaseAgent *agent,
                         const char *stream_id,
                         guint component_id);
};

GType kms_webrtc_transport_src_get_type (void);

KmsWebrtcTransportSrc * kms_webrtc_transport_src_new ();
void kms_webrtc_transport_src_connect_elements (KmsWebrtcTransportSrc *self);
void kms_webrtc_transport_src_configure (KmsWebrtcTransportSrc * self,
                                             KmsIceBaseAgent *agent,
                                             const char *stream_id,
                                             guint component_id);

G_END_DECLS
#endif /* __KMS_WEBRTC_TRANSPORT_SRC_H__ */
