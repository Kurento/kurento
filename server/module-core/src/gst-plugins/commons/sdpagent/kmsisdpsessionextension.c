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

#include "kmsisdpsessionextension.h"

G_DEFINE_INTERFACE (KmsISdpSessionExtension, kms_i_sdp_session_extension, 0);

#define DEFAULT_PREPROCESSING_MEDIA TRUE

static void
kms_i_sdp_session_extension_default_init (KmsISdpSessionExtensionInterface *
    iface)
{
  g_object_interface_install_property (iface,
      g_param_spec_boolean ("pre-media-processing", "Pre-processing medias",
          "If the extension should be executed after processing medias",
          DEFAULT_PREPROCESSING_MEDIA, G_PARAM_READWRITE));
}

gboolean
kms_i_sdp_session_extension_add_offer_attributes (KmsISdpSessionExtension * ext,
    GstSDPMessage * offer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_SESSION_EXTENSION (ext), FALSE);

  return
      KMS_I_SDP_SESSION_EXTENSION_GET_INTERFACE (ext)->add_offer_attributes
      (ext, offer, error);
}

gboolean
kms_i_sdp_session_extension_add_answer_attributes (KmsISdpSessionExtension *
    ext, const GstSDPMessage * offer, GstSDPMessage * answer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_SESSION_EXTENSION (ext), FALSE);

  return
      KMS_I_SDP_SESSION_EXTENSION_GET_INTERFACE (ext)->add_answer_attributes
      (ext, offer, answer, error);
}

gboolean
kms_i_sdp_session_extension_can_insert_attribute (KmsISdpSessionExtension * ext,
    const GstSDPMessage * offer, const GstSDPAttribute * attr,
    GstSDPMessage * answer)
{
  g_return_val_if_fail (KMS_IS_I_SDP_SESSION_EXTENSION (ext), FALSE);

  return
      KMS_I_SDP_SESSION_EXTENSION_GET_INTERFACE (ext)->can_insert_attribute
      (ext, offer, attr, answer);
}
