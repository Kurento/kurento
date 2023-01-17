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

#ifndef __KMS_SDP_REJECT_MEDIA_HANDLER_H__
#define __KMS_SDP_REJECT_MEDIA_HANDLER_H__

#include "kmssdpmediahandler.h"

G_BEGIN_DECLS

#define KMS_TYPE_SDP_REJECT_MEDIA_HANDLER \
  (kms_sdp_reject_media_handler_get_type())

#define KMS_SDP_REJECT_MEDIA_HANDLER(obj) (  \
  G_TYPE_CHECK_INSTANCE_CAST (               \
    (obj),                                   \
    KMS_TYPE_SDP_REJECT_MEDIA_HANDLER,       \
    KmsSdpRejectMediaHandler                 \
  )                                          \
)
#define KMS_SDP_REJECT_MEDIA_HANDLER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                         \
    (klass),                                        \
    KMS_TYPE_SDP_REJECT_MEDIA_HANDLER,              \
    KmsSdpRejectMediaHandlerClass                   \
  )                                                 \
)
#define KMS_IS_SDP_REJECT_MEDIA_HANDLER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (                 \
    (obj),                                     \
    KMS_TYPE_SDP_REJECT_MEDIA_HANDLER          \
  )                                            \
)
#define KMS_IS_SDP_REJECT_MEDIA_HANDLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_REJECT_MEDIA_HANDLER))
#define KMS_SDP_REJECT_MEDIA_HANDLER_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                         \
    (obj),                                            \
    KMS_TYPE_SDP_REJECT_MEDIA_HANDLER,                \
    KmsSdpRejectMediaHandlerClass                     \
  )                                                   \
)

typedef struct _KmsSdpRejectMediaHandler KmsSdpRejectMediaHandler;
typedef struct _KmsSdpRejectMediaHandlerClass KmsSdpRejectMediaHandlerClass;
typedef struct _KmsSdpRejectMediaHandlerPrivate KmsSdpRejectMediaHandlerPrivate;

struct _KmsSdpRejectMediaHandler
{
  KmsSdpMediaHandler parent;

  /*< private > */
  KmsSdpRejectMediaHandlerPrivate *priv;
};

struct _KmsSdpRejectMediaHandlerClass
{
  KmsSdpMediaHandlerClass parent_class;
};

GType kms_sdp_reject_media_handler_get_type ();

KmsSdpRejectMediaHandler * kms_sdp_reject_media_handler_new ();

G_END_DECLS

#endif /* __KMS_SDP_REJECT_MEDIA_HANDLER_H__ */
