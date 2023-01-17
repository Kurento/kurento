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

#include "kmsisdppayloadmanager.h"

G_DEFINE_INTERFACE (KmsISdpPayloadManager, kms_i_sdp_payload_manager, 0);

static void
kms_i_sdp_payload_manager_default_init (KmsISdpPayloadManagerInterface * iface)
{
  /* Nothing to do */
}

gint
kms_i_sdp_payload_manager_get_dynamic_pt (KmsISdpPayloadManager *
    self, const gchar * codec_name, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_PAYLOAD_MANAGER (self), -1);

  return
      KMS_I_SDP_PAYLOAD_MANAGER_GET_INTERFACE (self)->get_dynamic_pt
      (self, codec_name, error);
}

gboolean
kms_i_sdp_payload_manager_register_dynamic_payload (KmsISdpPayloadManager *
    self, gint pt, const gchar * codec_name, GError ** error)
{
  g_return_val_if_fail (KMS_IS_I_SDP_PAYLOAD_MANAGER (self), FALSE);

  return
      KMS_I_SDP_PAYLOAD_MANAGER_GET_INTERFACE (self)->register_dynamic_payload
      (self, pt, codec_name, error);
}
