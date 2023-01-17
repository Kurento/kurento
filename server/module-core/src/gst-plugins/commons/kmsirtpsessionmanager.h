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

#ifndef __KMS_I_RTP_SESSION_MANAGER_H__
#define __KMS_I_RTP_SESSION_MANAGER_H__

#include <gst/gst.h>
#include "kmsbasertpsession.h"

G_BEGIN_DECLS

typedef struct _KmsBaseRtpSession KmsBaseRtpSession;

/* KmsIRtpSessionManager begin */
#define KMS_TYPE_I_RTP_SESSION_MANAGER \
  (kms_i_rtp_session_manager_get_type())
#define KMS_I_RTP_SESSION_MANAGER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_I_RTP_SESSION_MANAGER,KmsIRtpSessionManager))
#define KMS_IS_I_RTP_SESSION_MANAGER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_I_RTP_SESSION_MANAGER))
#define KMS_I_RTP_SESSION_MANAGER_CAST(obj) ((KmsIRtpSessionManager*)(obj))
#define KMS_I_RTP_SESSION_MANAGER_GET_INTERFACE(obj) \
  (G_TYPE_INSTANCE_GET_INTERFACE((obj),KMS_TYPE_I_RTP_SESSION_MANAGER,KmsIRtpSessionManagerInterface))
#define KMS_I_RTP_SESSION_MANAGER_INTERFACE(iface) ((KmsIRtpSessionManagerInterface *) iface)

typedef struct _KmsIRtpSessionManager KmsIRtpSessionManager;
typedef struct _KmsIRtpSessionManagerInterface KmsIRtpSessionManagerInterface;

struct _KmsIRtpSessionManagerInterface
{
  GTypeInterface parent;

  /* virtual methods */
  GstPad * (*request_rtp_sink) (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
  GstPad * (*request_rtp_src) (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
  GstPad * (*request_rtcp_sink) (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
  GstPad * (*request_rtcp_src) (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);

  gboolean (*custom_ssrc_management) (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, GstElement * ssrcdemux, guint ssrc, GstPad * pad);
};

GType kms_i_rtp_session_manager_get_type (void);

GstPad * kms_i_rtp_session_manager_request_rtp_sink (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
GstPad * kms_i_rtp_session_manager_request_rtp_src (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
GstPad * kms_i_rtp_session_manager_request_rtcp_sink (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
GstPad * kms_i_rtp_session_manager_request_rtcp_src (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, const GstSDPMedia * media);
gboolean kms_i_rtp_session_manager_custom_ssrc_management (KmsIRtpSessionManager *self, KmsBaseRtpSession *sess, GstElement * ssrcdemux, guint ssrc, GstPad * pad);

/* KmsIRtpSessionManager end */

G_END_DECLS
#endif /* __KMS_I_RTP_SESSION_MANAGER_H__ */
