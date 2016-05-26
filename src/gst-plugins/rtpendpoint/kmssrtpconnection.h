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

#ifndef __KMS_SRTP_CONNECTION_H__
#define __KMS_SRTP_CONNECTION_H__

#include "kmsrtpbaseconnection.h"

G_BEGIN_DECLS

#define KMS_TYPE_SRTP_CONNECTION \
  (kms_srtp_connection_get_type())
#define KMS_SRTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_SRTP_CONNECTION,KmsSrtpConnection))
#define KMS_SRTP_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_SRTP_CONNECTION,KmsSrtpConnectionClass))
#define KMS_IS_SRTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_SRTP_CONNECTION))
#define KMS_IS_SRTP_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SRTP_CONNECTION))
#define KMS_SRTP_CONNECTION_CAST(obj) ((KmsSrtpConnection*)(obj))

typedef struct _KmsSrtpConnectionPrivate KmsSrtpConnectionPrivate;
typedef struct _KmsSrtpConnection KmsSrtpConnection;
typedef struct _KmsSrtpConnectionClass KmsSrtpConnectionClass;

struct _KmsSrtpConnection
{
  KmsRtpBaseConnection parent;

  KmsSrtpConnectionPrivate *priv;
};

struct _KmsSrtpConnectionClass
{
  KmsRtpBaseConnectionClass parent_class;

  /* signals */
  void (*key_soft_limit) (KmsSrtpConnection *conn);
};

GType kms_srtp_connection_get_type (void);

KmsSrtpConnection *kms_srtp_connection_new (guint16 min_port, guint16 max_port, gboolean use_ipv6);
void kms_srtp_connection_set_key (KmsSrtpConnection *conn, const gchar *key, guint auth, guint cipher, gboolean local);

G_END_DECLS
#endif /* __KMS_RTP_CONNECTION_H__ */
