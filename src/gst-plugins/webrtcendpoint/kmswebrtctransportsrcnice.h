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

#ifndef __KMS_WEBRTC_TRANSPORT_SRC_NICE_H__
#define __KMS_WEBRTC_TRANSPORT_SRC_NICE_H__

#include <gst/gst.h>
#include "kmswebrtctransportsrc.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_WEBRTC_TRANSPORT_SRC_NICE \
  (kms_webrtc_transport_src_nice_get_type())
#define KMS_WEBRTC_TRANSPORT_SRC_NICE(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_TRANSPORT_SRC_NICE,KmsWebrtcTransportSrcNice))
#define KMS_WEBRTC_TRANSPORT_SRC_NICE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_TRANSPORT_SRCNICE,KmsWebrtcTransportSrcNiceClass))
#define KMS_IS_WEBRTC_TRANSPORT_SRC_NICE(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_TRANSPORT_SRC_NICE))
#define KMS_IS_WEBRTC_TRANSPORT_SRC_NICE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_TRANSPORT_SRC_NICE))
#define KMS_WEBRTC_TRANSPORT_SRC_NICE_CAST(obj) ((KmsWebrtcTransportSrcNice*)(obj))

typedef struct _KmsWebrtcTransportSrcNice KmsWebrtcTransportSrcNice;
typedef struct _KmsWebrtcTransportSrcNiceClass KmsWebrtcTransportSrcNiceClass;

struct _KmsWebrtcTransportSrcNice
{
  KmsWebrtcTransportSrc parent;
};

struct _KmsWebrtcTransportSrcNiceClass
{
  KmsWebrtcTransportSrcClass parent_class;
};

GType kms_webrtc_transport_src_nice_get_type (void);

KmsWebrtcTransportSrcNice * kms_webrtc_transport_src_nice_new ();

G_END_DECLS
#endif /* __KMS_WEBRTC_TRANSPORT_SRC_NICE_H__ */
