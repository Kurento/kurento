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

#ifndef __KMS_RTP_BASE_CONNECTION_H__
#define __KMS_RTP_BASE_CONNECTION_H__

#include <commons/kmsirtpconnection.h>

G_BEGIN_DECLS

#define KMS_TYPE_RTP_BASE_CONNECTION \
  (kms_rtp_base_connection_get_type())
#define KMS_RTP_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTP_BASE_CONNECTION,KmsRtpBaseConnection))
#define KMS_RTP_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTP_BASE_CONNECTION,KmsRtpBaseConnectionClass))
#define KMS_IS_RTP_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTP_BASE_CONNECTION))
#define KMS_IS_RTP_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTP_BASE_CONNECTION))
#define KMS_RTP_BASE_CONNECTION_CAST(obj) ((KmsRtpBaseConnection*)(obj))
typedef struct _KmsRtpBaseConnection KmsRtpBaseConnection;
typedef struct _KmsRtpBaseConnectionClass KmsRtpBaseConnectionClass;

struct _KmsRtpBaseConnection
{
  GObject parent;
};

struct _KmsRtpBaseConnectionClass
{
  GObjectClass parent_class;

    guint (*get_rtp_port) (KmsRtpBaseConnection * self);
    guint (*get_rtcp_port) (KmsRtpBaseConnection * self);
  void (*set_remote_info) (KmsRtpBaseConnection * self,
      const gchar * host, gint rtp_port, gint rtcp_port);
};

GType kms_rtp_base_connection_get_type (void);

guint kms_rtp_base_connection_get_rtp_port (KmsRtpBaseConnection * self);
guint kms_rtp_base_connection_get_rtcp_port (KmsRtpBaseConnection * self);
void kms_rtp_base_connection_set_remote_info (KmsRtpBaseConnection * self,
    const gchar * host, gint rtp_port, gint rtcp_port);

G_END_DECLS
#endif /* __KMS_RTP_BASE_CONNECTION_H__ */
