/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
};

struct _KmsRtpSessionClass
{
  KmsBaseRtpSessionClass parent_class;

  /* private */
  /* virtual methods */
  void (*post_constructor) (KmsRtpSession * self, KmsBaseSdpEndpoint * ep,
                            guint id, KmsIRtpSessionManager * manager);
};

GType kms_rtp_session_get_type (void);

KmsRtpSession * kms_rtp_session_new (KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager);

KmsRtpBaseConnection * kms_rtp_session_get_connection (KmsRtpSession * self, SdpMediaConfig * mconf);

G_END_DECLS
#endif /* __KMS_RTP_SESSION_H__ */
