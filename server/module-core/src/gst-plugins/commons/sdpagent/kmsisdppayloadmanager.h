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

#ifndef __KMS_I_SDP_PAYLOAD_MANAGER_H__
#define __KMS_I_SDP_PAYLOAD_MANAGER_H__

#include <gst/gst.h>

#define KMS_TYPE_I_SDP_PAYLOAD_MANAGER \
  (kms_i_sdp_payload_manager_get_type ())
#define KMS_I_SDP_PAYLOAD_MANAGER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST ((obj), KMS_TYPE_I_SDP_PAYLOAD_MANAGER, KmsISdpPayloadManager))
#define KMS_IS_I_SDP_PAYLOAD_MANAGER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), KMS_TYPE_I_SDP_PAYLOAD_MANAGER))
#define KMS_I_SDP_PAYLOAD_MANAGER_GET_INTERFACE(inst) \
  (G_TYPE_INSTANCE_GET_INTERFACE ((inst), KMS_TYPE_I_SDP_PAYLOAD_MANAGER, KmsISdpPayloadManagerInterface))

typedef struct _KmsISdpPayloadManager               KmsISdpPayloadManager;
typedef struct _KmsISdpPayloadManagerInterface      KmsISdpPayloadManagerInterface;

struct _KmsISdpPayloadManagerInterface
{
  GTypeInterface parent;

  gint (*get_dynamic_pt) (KmsISdpPayloadManager *self, const gchar *codec_name, GError **error);
  gboolean (*register_dynamic_payload) (KmsISdpPayloadManager *self, gint pt, const gchar *codec_name, GError **error);
};

GType kms_i_sdp_payload_manager_get_type (void);
gint kms_i_sdp_payload_manager_get_dynamic_pt (KmsISdpPayloadManager *self, const gchar *codec_name, GError **error);
gboolean kms_i_sdp_payload_manager_register_dynamic_payload (KmsISdpPayloadManager *self, gint pt, const gchar *codec_name, GError **error);

#endif /* __KMS_I_SDP_PAYLOAD_MANAGER_H__ */
