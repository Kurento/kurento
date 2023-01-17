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

#ifndef __KMS_RTP_SESSION_H__
#define __KMS_RTP_SESSION_H__

#include <gst/gst.h>
#include <commons/kmsbasertpsession.h>
#include "kmsrtpconnection.h"

G_BEGIN_DECLS

typedef struct _KmsIRtpSessionManager KmsIRtpSessionManager;

/* #defines don't like whitespacey bits */
#define KMS_TYPE_RTP_SESSION \
  (kms_rtp_session_get_type())
#define KMS_RTP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTP_SESSION,KmsRtpSession))
#define KMS_RTP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTP_SESSION,KmsRtpSessionClass))
#define KMS_IS_RTP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTP_SESSION))
#define KMS_IS_RTP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTP_SESSION))
#define KMS_RTP_SESSION_CAST(obj) ((KmsRtpSession*)(obj))

typedef struct _KmsRtpSession KmsRtpSession;
typedef struct _KmsRtpSessionClass KmsRtpSessionClass;

struct _KmsRtpSession
{
  KmsBaseRtpSession parent;

  gboolean use_ipv6;
};

struct _KmsRtpSessionClass
{
  KmsBaseRtpSessionClass parent_class;

  /* private */
  /* virtual methods */
  void (*post_constructor) (KmsRtpSession * self, KmsBaseSdpEndpoint * ep,
                            guint id, KmsIRtpSessionManager * manager, gboolean use_ipv6);
};

GType kms_rtp_session_get_type (void);

KmsRtpSession * kms_rtp_session_new (KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager, gboolean use_ipv6);

KmsRtpBaseConnection * kms_rtp_session_get_connection (KmsRtpSession * self, KmsSdpMediaHandler * handler);

G_END_DECLS
#endif /* __KMS_RTP_SESSION_H__ */
