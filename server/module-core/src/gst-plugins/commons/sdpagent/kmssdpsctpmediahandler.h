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
#ifndef _KMS_SDP_SCTP_MEDIA_HANDLER_H_
#define _KMS_SDP_SCTP_MEDIA_HANDLER_H_

#include <gst/gst.h>
#include <gst/sdp/gstsdpmessage.h>

#include "kmssdpmediahandler.h"

#define SDP_MEDIA_SCTP_FMT "webrtc-datachannel"
#define SDP_MEDIA_SCTPMAP_ATTR "sctpmap"
#define SDP_MEDIA_SCTP_PORT_ATTR "sctp-port"

G_BEGIN_DECLS

#define KMS_TYPE_SDP_SCTP_MEDIA_HANDLER \
  (kms_sdp_sctp_media_handler_get_type())

#define KMS_SDP_SCTP_MEDIA_HANDLER(obj) (  \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_SDP_SCTP_MEDIA_HANDLER,       \
    KmsSdpSctpMediaHandler                 \
  )                                        \
)
#define KMS_SDP_SCTP_MEDIA_HANDLER_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                        \
    (klass),                                       \
    KMS_TYPE_SDP_SCTP_MEDIA_HANDLER,               \
    KmsSdpSctpMediaHandlerClass                    \
  )                                                \
)
#define KMS_IS_SDP_SCTP_MEDIA_HANDLER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (               \
    (obj),                                   \
    KMS_TYPE_SDP_SCTP_MEDIA_HANDLER          \
  )                                          \
)
#define KMS_IS_SDP_SCTP_MEDIA_HANDLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_SCTP_MEDIA_HANDLER))
#define KMS_SDP_SCTP_MEDIA_HANDLER_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                        \
    (obj),                                           \
    KMS_TYPE_SDP_SCTP_MEDIA_HANDLER,                 \
    KmsSdpSctpMediaHandlerClass                      \
  )                                                  \
)

typedef struct _KmsSdpSctpMediaHandler KmsSdpSctpMediaHandler;
typedef struct _KmsSdpSctpMediaHandlerClass KmsSdpSctpMediaHandlerClass;

struct _KmsSdpSctpMediaHandler
{
  KmsSdpMediaHandler parent;
};

struct _KmsSdpSctpMediaHandlerClass
{
  KmsSdpMediaHandlerClass parent_class;
};

GType kms_sdp_sctp_media_handler_get_type ();

KmsSdpSctpMediaHandler * kms_sdp_sctp_media_handler_new ();

gboolean kms_sdp_sctp_media_handler_manage_protocol (const gchar * protocol);

G_END_DECLS

#endif /* _KMS_SDP_SCTP_MEDIA_HANDLER_H_ */
