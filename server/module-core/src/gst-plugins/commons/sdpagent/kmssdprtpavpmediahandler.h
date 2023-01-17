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
#ifndef _KMS_SDP_RTP_AVP_MEDIA_HANDLER_H_
#define _KMS_SDP_RTP_AVP_MEDIA_HANDLER_H_

#include <gst/gst.h>

#include "kmssdprtpmediahandler.h"
#include "kmsisdppayloadmanager.h"

G_BEGIN_DECLS

#define KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER \
  (kms_sdp_rtp_avp_media_handler_get_type())

#define KMS_SDP_RTP_AVP_MEDIA_HANDLER(obj) (    \
  G_TYPE_CHECK_INSTANCE_CAST (                  \
    (obj),                                      \
    KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER,         \
    KmsSdpRtpAvpMediaHandler                    \
  )                                             \
)
#define KMS_SDP_RTP_AVP_MEDIA_HANDLER_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                           \
    (klass),                                          \
    KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER,               \
    KmsSdpRtpAvpMediaHandlerClass                     \
  )                                                   \
)
#define KMS_IS_SDP_RTP_AVP_MEDIA_HANDLER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER          \
  )                                             \
)
#define KMS_IS_SDP_RTP_AVP_MEDIA_HANDLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER))
#define KMS_SDP_RTP_AVP_MEDIA_HANDLER_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                           \
    (obj),                                              \
    KMS_TYPE_SDP_RTP_AVP_MEDIA_HANDLER,                 \
    KmsSdpRtpAvpMediaHandlerClass                       \
  )                                                     \
)

#define SDP_MEDIA_RTP_AVP_PROTO "RTP/AVP"

typedef struct _KmsSdpRtpAvpMediaHandler KmsSdpRtpAvpMediaHandler;
typedef struct _KmsSdpRtpAvpMediaHandlerClass KmsSdpRtpAvpMediaHandlerClass;
typedef struct _KmsSdpRtpAvpMediaHandlerPrivate KmsSdpRtpAvpMediaHandlerPrivate;

struct _KmsSdpRtpAvpMediaHandler
{
  KmsSdpRtpMediaHandler parent;

  /*< private > */
  KmsSdpRtpAvpMediaHandlerPrivate *priv;
};

struct _KmsSdpRtpAvpMediaHandlerClass
{
  KmsSdpRtpMediaHandlerClass parent_class;
};

GType kms_sdp_rtp_avp_media_handler_get_type ();

KmsSdpRtpAvpMediaHandler * kms_sdp_rtp_avp_media_handler_new ();

gboolean kms_sdp_rtp_avp_media_handler_add_extmap (KmsSdpRtpAvpMediaHandler *self, guint8 id, const gchar *uri, GError **error);
gboolean kms_sdp_rtp_avp_media_handler_use_payload_manager (KmsSdpRtpAvpMediaHandler *self, KmsISdpPayloadManager *manager, GError **error);
gint kms_sdp_rtp_avp_media_handler_add_video_codec (KmsSdpRtpAvpMediaHandler * self, const gchar * name, GError ** error);
gint kms_sdp_rtp_avp_media_handler_add_audio_codec (KmsSdpRtpAvpMediaHandler * self, const gchar * name, GError ** error);
gint kms_sdp_rtp_avp_media_handler_add_generic_audio_payload (KmsSdpRtpAvpMediaHandler * self, const gchar * format, GError ** error);
gint kms_sdp_rtp_avp_media_handler_add_generic_video_payload (KmsSdpRtpAvpMediaHandler * self, const gchar * format, GError ** error);
gboolean kms_sdp_rtp_avp_media_handler_add_fmtp (KmsSdpRtpAvpMediaHandler * self, guint payload, const gchar * format, GError ** error);

G_END_DECLS

#endif /* _KMS_SDP_RTP_AVP_MEDIA_HANDLER_H_ */
