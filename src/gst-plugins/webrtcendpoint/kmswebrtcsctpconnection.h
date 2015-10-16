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

#ifndef __KMS_WEBRTC_SCTP_CONNECTION_H__
#define __KMS_WEBRTC_SCTP_CONNECTION_H__

#include "kmswebrtcbaseconnection.h"

G_BEGIN_DECLS

#define KMS_TYPE_WEBRTC_SCTP_CONNECTION \
  (kms_webrtc_sctp_connection_get_type())
#define KMS_WEBRTC_SCTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_SCTP_CONNECTION,KmsWebRtcSctpConnection))
#define KMS_WEBRTC_SCTP_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_SCTP_CONNECTION,KmsWebRtcSctpConnectionClass))
#define KMS_IS_WEBRTC_SCTP_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_SCTP_CONNECTION))
#define KMS_IS_WEBRTC_SCTP_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_SCTP_CONNECTION))
#define KMS_WEBRTC_SCTP_CONNECTION_CAST(obj) ((KmsWebRtcSctpConnection*)(obj))

typedef struct _KmsWebRtcSctpConnectionPrivate KmsWebRtcSctpConnectionPrivate;
typedef struct _KmsWebRtcSctpConnection KmsWebRtcSctpConnection;
typedef struct _KmsWebRtcSctpConnectionClass KmsWebRtcSctpConnectionClass;

struct _KmsWebRtcSctpConnection
{
  KmsWebRtcBaseConnection parent;

  KmsWebRtcSctpConnectionPrivate *priv;
};

struct _KmsWebRtcSctpConnectionClass
{
  KmsWebRtcBaseConnectionClass parent_class;
};

GType kms_webrtc_sctp_connection_get_type (void);

KmsWebRtcSctpConnection *kms_webrtc_sctp_connection_new (KmsIceBaseAgent * agent,
    GMainContext * context, const gchar * name, guint16 min_port, guint16 max_port);

G_END_DECLS
#endif /* __KMS_WEBRTC_SCTP_CONNECTION_H__ */
