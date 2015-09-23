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
