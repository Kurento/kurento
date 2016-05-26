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

#ifndef __KMS_RTP_CONNECTION_H__
#define __KMS_RTP_CONNECTION_H__

#include "kmsrtpbaseconnection.h"

G_BEGIN_DECLS
#define KMS_TYPE_RTP_CONNECTION \
  (kms_rtp_connection_get_type())
#define KMS_RTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTP_CONNECTION,KmsRtpConnection))
#define KMS_RTP_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTP_CONNECTION,KmsRtpConnectionClass))
#define KMS_IS_RTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTP_CONNECTION))
#define KMS_IS_RTP_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTP_CONNECTION))
#define KMS_RTP_CONNECTION_CAST(obj) ((KmsRtpConnection*)(obj))
typedef struct _KmsRtpConnectionPrivate KmsRtpConnectionPrivate;
typedef struct _KmsRtpConnection KmsRtpConnection;
typedef struct _KmsRtpConnectionClass KmsRtpConnectionClass;

struct _KmsRtpConnection
{
  KmsRtpBaseConnection parent;

  KmsRtpConnectionPrivate *priv;
};

struct _KmsRtpConnectionClass
{
  KmsRtpBaseConnectionClass parent_class;
};

GType kms_rtp_connection_get_type (void);

KmsRtpConnection *kms_rtp_connection_new (guint16 min_port, guint16 max_port,
    gboolean use_ipv6);

G_END_DECLS
#endif /* __KMS_RTP_CONNECTION_H__ */
