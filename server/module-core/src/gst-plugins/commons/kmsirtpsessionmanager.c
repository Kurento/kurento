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

#include "kmsirtpsessionmanager.h"

G_DEFINE_INTERFACE (KmsIRtpSessionManager, kms_i_rtp_session_manager, 0);

static void
kms_i_rtp_session_manager_default_init (KmsIRtpSessionManagerInterface * iface)
{
  /* do nothing */
}

GstPad *
kms_i_rtp_session_manager_request_rtp_sink (KmsIRtpSessionManager * self,
    KmsBaseRtpSession * sess, const GstSDPMedia * media)
{
  g_return_val_if_fail (KMS_IS_I_RTP_SESSION_MANAGER (self), NULL);

  return KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE (self)->request_rtp_sink (self,
      sess, media);
}

GstPad *
kms_i_rtp_session_manager_request_rtp_src (KmsIRtpSessionManager * self,
    KmsBaseRtpSession * sess, const GstSDPMedia * media)
{
  g_return_val_if_fail (KMS_IS_I_RTP_SESSION_MANAGER (self), NULL);

  return KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE (self)->request_rtp_src (self,
      sess, media);
}

GstPad *
kms_i_rtp_session_manager_request_rtcp_sink (KmsIRtpSessionManager * self,
    KmsBaseRtpSession * sess, const GstSDPMedia * media)
{
  g_return_val_if_fail (KMS_IS_I_RTP_SESSION_MANAGER (self), NULL);

  return
      KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE (self)->request_rtcp_sink (self,
      sess, media);
}

GstPad *
kms_i_rtp_session_manager_request_rtcp_src (KmsIRtpSessionManager * self,
    KmsBaseRtpSession * sess, const GstSDPMedia * media)
{
  g_return_val_if_fail (KMS_IS_I_RTP_SESSION_MANAGER (self), NULL);

  return KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE (self)->request_rtcp_src (self,
      sess, media);
}

gboolean
kms_i_rtp_session_manager_custom_ssrc_management (KmsIRtpSessionManager * self,
    KmsBaseRtpSession * sess, GstElement * ssrcdemux, guint ssrc, GstPad * pad)
{
  g_return_val_if_fail (KMS_IS_I_RTP_SESSION_MANAGER (self), FALSE);

  if (!KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE (self)->custom_ssrc_management) {
    // No custom SSRC handler has been installed. Reject the SSRC handling.
    return FALSE;
  }

  return
      KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE (self)->custom_ssrc_management
      (self, sess, ssrcdemux, ssrc, pad);
}
