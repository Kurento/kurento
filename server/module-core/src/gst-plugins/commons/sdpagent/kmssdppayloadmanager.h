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
#ifndef _KMS_SDP_PAYLOAD_MANAGER_H_
#define _KMS_SDP_PAYLOAD_MANAGER_H_

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_SDP_PAYLOAD_MANAGER \
  (kms_sdp_payload_manager_get_type())

#define KMS_SDP_PAYLOAD_MANAGER(obj) (  \
  G_TYPE_CHECK_INSTANCE_CAST (          \
    (obj),                              \
    KMS_TYPE_SDP_PAYLOAD_MANAGER,       \
    KmsSdpPayloadManager                \
  )                                     \
)
#define KMS_SDP_PAYLOAD_MANAGER_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_SDP_PAYLOAD_MANAGER,               \
    KmsSdpPayloadManagerClass                   \
  )                                             \
)
#define KMS_IS_SDP_PAYLOAD_MANAGER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (            \
    (obj),                                \
    KMS_TYPE_SDP_PAYLOAD_MANAGER          \
  )                                       \
)
#define KMS_IS_SDP_PAYLOAD_MANAGER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_PAYLOAD_MANAGER))
#define KMS_SDP_PAYLOAD_MANAGER_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                     \
    (obj),                                        \
    KMS_TYPE_SDP_PAYLOAD_MANAGER,                 \
    KmsSdpPayloadManagerClass                     \
  )                                               \
)

typedef struct _KmsSdpPayloadManager KmsSdpPayloadManager;
typedef struct _KmsSdpPayloadManagerClass KmsSdpPayloadManagerClass;
typedef struct _KmsSdpPayloadManagerPrivate KmsSdpPayloadManagerPrivate;

struct _KmsSdpPayloadManager
{
  GObject parent;

  /*< private > */
  KmsSdpPayloadManagerPrivate *priv;
};

struct _KmsSdpPayloadManagerClass
{
  GObjectClass parent_class;
};

GType kms_sdp_payload_manager_get_type ();

KmsSdpPayloadManager * kms_sdp_payload_manager_new ();

KmsSdpPayloadManager * kms_sdp_payload_manager_new_same_codec_shares_pt ();

G_END_DECLS

#endif /* _KMS_SDP_PAYLOAD_MANAGER_H_ */
