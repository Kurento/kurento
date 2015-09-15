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
