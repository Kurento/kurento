/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#ifndef __KMS_SDP_MEDIA_DIRECTION_EXT_H__
#define __KMS_SDP_MEDIA_DIRECTION_EXT_H__

#include <gst/gst.h>

#include <sdp_utils.h>

G_BEGIN_DECLS

#define KMS_TYPE_SDP_MEDIA_DIRECTION_EXT    \
  (kms_sdp_media_direction_ext_get_type())

#define KMS_SDP_MEDIA_DIRECTION_EXT(obj) (  \
  G_TYPE_CHECK_INSTANCE_CAST (              \
    (obj),                                  \
    KMS_TYPE_SDP_MEDIA_DIRECTION_EXT,       \
    KmsSdpMediaDirectionExt                 \
  )                                         \
)
#define KMS_SDP_MEDIA_DIRECTION_EXT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                         \
    (klass),                                        \
    KMS_TYPE_SDP_MEDIA_DIRECTION_EXT,               \
    KmsSdpMediaDirectionExtClass                    \
  )                                                 \
)
#define KMS_IS_SDP_MEDIA_DIRECTION_EXT(obj) (  \
  G_TYPE_CHECK_INSTANCE_TYPE (                 \
    (obj),                                     \
    KMS_TYPE_SDP_MEDIA_DIRECTION_EXT           \
  )                                            \
)
#define KMS_IS_SDP_MEDIA_DIRECTION_EXT_CLASS(klass)  \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_MEDIA_DIRECTION_EXT))
#define KMS_SDP_MEDIA_DIRECTION_EXT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                         \
    (obj),                                            \
    KMS_TYPE_SDP_MEDIA_DIRECTION_EXT,                 \
    KmsSdpMediaDirectionExtClass                      \
  )                                                   \
)

typedef struct _KmsSdpMediaDirectionExt KmsSdpMediaDirectionExt;
typedef struct _KmsSdpMediaDirectionExtClass KmsSdpMediaDirectionExtClass;
typedef struct _KmsSdpMediaDirectionExtPrivate KmsSdpMediaDirectionExtPrivate;

struct _KmsSdpMediaDirectionExt
{
  GObject parent;

  /*< private > */
  KmsSdpMediaDirectionExtPrivate *priv;
};

struct _KmsSdpMediaDirectionExtClass
{
  GObjectClass parent_class;

  /* signals */
  GstSDPDirection (*on_offer_media_direction) (KmsSdpMediaDirectionExt * ext);
  GstSDPDirection (*on_answer_media_direction) (KmsSdpMediaDirectionExt * ext, GstSDPDirection dir);
  void (*on_answered_media_direction) (KmsSdpMediaDirectionExt * ext, GstSDPDirection dir);
};

GType kms_sdp_media_direction_ext_get_type ();

KmsSdpMediaDirectionExt * kms_sdp_media_direction_ext_new ();

G_END_DECLS

#endif /* __KMS_SDP_MEDIA_DIRECTION_EXT_H__ */
