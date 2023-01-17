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
#ifndef _KMS_SDP_RTP_AVPF_MEDIA_HANDLER_H_
#define _KMS_SDP_RTP_AVPF_MEDIA_HANDLER_H_

#include <gst/gst.h>

#include "kmssdprtpavpmediahandler.h"

G_BEGIN_DECLS

#define KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER \
  (kms_sdp_rtp_avpf_media_handler_get_type())

#define KMS_SDP_RTP_AVPF_MEDIA_HANDLER(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST (                  \
    (obj),                                      \
    KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER,        \
    KmsSdpRtpAvpfMediaHandler                   \
  )                                             \
)
#define KMS_SDP_RTP_AVPF_MEDIA_HANDLER_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                            \
    (klass),                                           \
    KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER,               \
    KmsSdpRtpAvpfMediaHandlerClass                     \
  )                                                    \
)
#define KMS_IS_SDP_RTP_AVPF_MEDIA_HANDLER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (                   \
    (obj),                                       \
    KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER          \
  )                                              \
)
#define KMS_IS_SDP_RTP_AVPF_MEDIA_HANDLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER))
#define KMS_SDP_RTP_AVPF_MEDIA_HANDLER_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                            \
    (obj),                                               \
    KMS_TYPE_SDP_RTP_AVPF_MEDIA_HANDLER,                 \
    KmsSdpRtpAvpfMediaHandlerClass                       \
  )                                                      \
)

typedef struct _KmsSdpRtpAvpfMediaHandler KmsSdpRtpAvpfMediaHandler;
typedef struct _KmsSdpRtpAvpfMediaHandlerClass KmsSdpRtpAvpfMediaHandlerClass;
typedef struct _KmsSdpRtpAvpfMediaHandlerPrivate KmsSdpRtpAvpfMediaHandlerPrivate;

struct _KmsSdpRtpAvpfMediaHandler
{
  KmsSdpRtpAvpMediaHandler parent;

  /*< private > */
  KmsSdpRtpAvpfMediaHandlerPrivate *priv;
};

struct _KmsSdpRtpAvpfMediaHandlerClass
{
  KmsSdpRtpAvpMediaHandlerClass parent_class;
};

GType kms_sdp_rtp_avpf_media_handler_get_type ();

KmsSdpRtpAvpfMediaHandler * kms_sdp_rtp_avpf_media_handler_new ();

G_END_DECLS

#endif /* _KMS_SDP_RTP_AVPF_MEDIA_HANDLER_H_ */
