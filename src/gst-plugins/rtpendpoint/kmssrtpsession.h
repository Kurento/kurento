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

#ifndef __KMS_SRTP_SESSION_H__
#define __KMS_SRTP_SESSION_H__

#include <gst/gst.h>
#include <commons/kmsbasertpsession.h>
#include "kmssrtpconnection.h"

G_BEGIN_DECLS

typedef struct _KmsIRtpSessionManager KmsIRtpSessionManager;

/* #defines don't like whitespacey bits */
#define KMS_TYPE_SRTP_SESSION \
  (kms_srtp_session_get_type())
#define KMS_SRTP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_SRTP_SESSION,KmsSrtpSession))
#define KMS_SRTP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_SRTP_SESSION,KmsSrtpSessionClass))
#define KMS_IS_SRTP_SESSION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_SRTP_SESSION))
#define KMS_IS_SRTP_SESSION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SRTP_SESSION))
#define KMS_SRTP_SESSION_CAST(obj) ((KmsSrtpSession*)(obj))

typedef struct _KmsSrtpSession KmsSrtpSession;
typedef struct _KmsSrtpSessionClass KmsSrtpSessionClass;

struct _KmsSrtpSession
{
  KmsBaseRtpSession parent;

  gboolean use_ipv6;
};

struct _KmsSrtpSessionClass
{
  KmsBaseRtpSessionClass parent_class;

  /* private */
  /* virtual methods */
  void (*post_constructor) (KmsSrtpSession * self, KmsBaseSdpEndpoint * ep,
                            guint id, KmsIRtpSessionManager * manager,
                            gboolean use_ipv6);
};

GType kms_srtp_session_get_type (void);

KmsSrtpSession *kms_srtp_session_new (KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager, gboolean use_ipv6);

KmsRtpBaseConnection * kms_srtp_session_get_connection (KmsSrtpSession * self, SdpMediaConfig * mconf);

G_END_DECLS
#endif /* __KMS_SRTP_SESSION_H__ */
