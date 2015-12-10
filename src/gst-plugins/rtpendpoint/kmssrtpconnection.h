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

KmsSrtpConnection *kms_srtp_connection_new (guint16 min_port, guint16 max_port);
void kms_srtp_connection_set_key (KmsSrtpConnection *conn, const gchar *key, guint auth, guint cipher, gboolean local);

G_END_DECLS
#endif /* __KMS_RTP_CONNECTION_H__ */
