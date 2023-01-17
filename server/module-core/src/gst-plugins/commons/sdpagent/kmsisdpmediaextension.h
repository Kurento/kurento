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
#ifndef _KMS_I_SDP_MEDIA_EXTENSION_H_
#define _KMS_I_SDP_MEDIA_EXTENSION_H_

#include <gst/gst.h>
#include <gst/sdp/gstsdpmessage.h>

G_BEGIN_DECLS

#define KMS_TYPE_I_SDP_MEDIA_EXTENSION \
  (kms_i_sdp_media_extension_get_type())

#define KMS_I_SDP_MEDIA_EXTENSION(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST (           \
    (obj),                               \
    KMS_TYPE_I_SDP_MEDIA_EXTENSION,      \
    KmsISdpMediaExtension                \
  )                                      \
)
#define KMS_IS_I_SDP_MEDIA_EXTENSION(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (              \
    (obj),                                  \
    KMS_TYPE_I_SDP_MEDIA_EXTENSION          \
  )                                         \
)
#define KMS_I_SDP_MEDIA_EXTENSION_CAST(obj) ((KmsISdpMediaExtension*)(obj))
#define KMS_I_SDP_MEDIA_EXTENSION_GET_INTERFACE(obj) \
  (G_TYPE_INSTANCE_GET_INTERFACE(                    \
    (obj),                                           \
    KMS_TYPE_I_SDP_MEDIA_EXTENSION,                  \
    KmsISdpMediaExtensionInterface                   \
  )                                                  \
)

typedef struct _KmsISdpMediaExtension KmsISdpMediaExtension;
typedef struct _KmsISdpMediaExtensionInterface KmsISdpMediaExtensionInterface;

struct _KmsISdpMediaExtensionInterface
{
  GTypeInterface parent_iface;

  gboolean (*add_offer_attributes) (KmsISdpMediaExtension *ext, GstSDPMedia * offer, GError **error);
  gboolean (*add_answer_attributes) (KmsISdpMediaExtension *ext, const GstSDPMedia * offer, GstSDPMedia * answer, GError **error);
  gboolean (*can_insert_attribute) (KmsISdpMediaExtension *ext, const GstSDPMedia * offer, const GstSDPAttribute * attr, GstSDPMedia * answer, const GstSDPMessage *msg);
  gboolean (*process_answer_attributes) (KmsISdpMediaExtension *ext, const GstSDPMedia * answer, GError **error);
};

GType kms_i_sdp_media_extension_get_type (void);

gboolean kms_i_sdp_media_extension_add_offer_attributes (KmsISdpMediaExtension *ext, GstSDPMedia * offer, GError **error);
gboolean kms_i_sdp_media_extension_add_answer_attributes (KmsISdpMediaExtension *ext, const GstSDPMedia * offer, GstSDPMedia * answer, GError **error);
gboolean kms_i_sdp_media_extension_can_insert_attribute (KmsISdpMediaExtension *ext, const GstSDPMedia * offer, const GstSDPAttribute * attr, GstSDPMedia * answer, const GstSDPMessage *msg);
gboolean kms_i_sdp_media_extension_process_answer_attributes (KmsISdpMediaExtension *ext, const GstSDPMedia * answer, GError **error);

G_END_DECLS

#endif /* _KMS_SDP_MEDIA_HANDLER_H_ */
