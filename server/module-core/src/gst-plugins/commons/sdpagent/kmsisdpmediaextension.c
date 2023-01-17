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

#include "kmsisdpmediaextension.h"

G_DEFINE_INTERFACE (KmsISdpMediaExtension, kms_i_sdp_media_extension, 0);

static void
kms_i_sdp_media_extension_default_init (KmsISdpMediaExtensionInterface * iface)
{
  /* Nothing to do */
}

gboolean
kms_i_sdp_media_extension_add_offer_attributes (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_MEDIA_EXTENSION (ext), FALSE);

  return
      KMS_I_SDP_MEDIA_EXTENSION_GET_INTERFACE (ext)->add_offer_attributes (ext,
      offer, error);
}

gboolean
kms_i_sdp_media_extension_add_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_MEDIA_EXTENSION (ext), FALSE);

  return
      KMS_I_SDP_MEDIA_EXTENSION_GET_INTERFACE (ext)->add_answer_attributes (ext,
      offer, answer, error);
}

gboolean
kms_i_sdp_media_extension_can_insert_attribute (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  g_return_val_if_fail (KMS_IS_I_SDP_MEDIA_EXTENSION (ext), FALSE);

  return
      KMS_I_SDP_MEDIA_EXTENSION_GET_INTERFACE (ext)->can_insert_attribute (ext,
      offer, attr, answer, msg);
}

gboolean
kms_i_sdp_media_extension_process_answer_attributes (KmsISdpMediaExtension *
    ext, const GstSDPMedia * answer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_MEDIA_EXTENSION (ext), FALSE);

  if (KMS_I_SDP_MEDIA_EXTENSION_GET_INTERFACE (ext)->process_answer_attributes
      == NULL) {
    return TRUE;
  }

  /* This extension requires to do something with the response attributes */
  return
      KMS_I_SDP_MEDIA_EXTENSION_GET_INTERFACE (ext)->process_answer_attributes
      (ext, answer, error);

}
