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

#ifndef __KMS_SDP_MID_EXT_H__
#define __KMS_SDP_MID_EXT_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_SDP_MID_EXT    \
  (kms_sdp_mid_ext_get_type())

#define KMS_SDP_MID_EXT(obj) (  \
  G_TYPE_CHECK_INSTANCE_CAST (  \
    (obj),                      \
    KMS_TYPE_SDP_MID_EXT,       \
    KmsSdpMidExt                \
  )                             \
)
#define KMS_SDP_MID_EXT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_SDP_MID_EXT,               \
    KmsSdpMidExtClass                   \
  )                                     \
)
#define KMS_IS_SDP_MID_EXT(obj) (  \
  G_TYPE_CHECK_INSTANCE_TYPE (     \
    (obj),                         \
    KMS_TYPE_SDP_MID_EXT           \
  )                                \
)
#define KMS_IS_SDP_MID_EXT_CLASS(klass)  \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_MID_EXT))
#define KMS_SDP_MID_EXT_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (             \
    (obj),                                \
    KMS_TYPE_SDP_MID_EXT,                 \
    KmsSdpMidExtClass                     \
  )                                       \
)

typedef struct _KmsSdpMidExt KmsSdpMidExt;
typedef struct _KmsSdpMidExtClass KmsSdpMidExtClass;
typedef struct _KmsSdpMidExtPrivate KmsSdpMidExtPrivate;

struct _KmsSdpMidExt
{
  GObject parent;

  /*< private > */
  KmsSdpMidExtPrivate *priv;
};

struct _KmsSdpMidExtClass
{
  GObjectClass parent_class;

  /* signals */
  gchar * (*on_offer_mid) (KmsSdpMidExt * ext);
  gboolean (*on_answer_mid) (KmsSdpMidExt * ext, gchar *mid);
};

GType kms_sdp_mid_ext_get_type ();

KmsSdpMidExt * kms_sdp_mid_ext_new ();

G_END_DECLS

#endif /* __KMS_SDP_MID_EXT_H__ */
