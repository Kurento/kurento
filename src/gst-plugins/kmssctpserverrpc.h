/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#ifndef _KMS_SCTP_SERVER_RPC_H_
#define _KMS_SCTP_SERVER_RPC_H_

#include "kmssctpbaserpc.h"

G_BEGIN_DECLS
#define KMS_TYPE_SCTP_SERVER_RPC (kms_sctp_server_rpc_get_type())
#define KMS_SCTP_SERVER_RPC(obj) (         \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_SCTP_SERVER_RPC,              \
    KmsSCTPServerRPC                       \
  )                                        \
)
#define KMS_SCTP_SERVER_RPC_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_SCTP_SERVER_RPC,              \
    KmsSCTPServerRPCClass                  \
  )                                        \
)
#define KMS_IS_SCTP_SERVER_RPC(obj) (      \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_SCTP_SERVER_RPC               \
  )                                        \
)
#define KMS_IS_SCTP_SERVER_RPC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), KMS_TYPE_SCTP_SERVER_RPC))
#define KMS_SCTP_SERVER_RPC_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                \
    (obj),                                   \
    KMS_TYPE_SCTP_SERVER_RPC,                \
    KmsSCTPServerRPCClass                    \
  )                                          \
)

typedef struct _KmsSCTPServerRPC KmsSCTPServerRPC;
typedef struct _KmsSCTPServerRPCClass KmsSCTPServerRPCClass;
typedef struct _KmsSCTPServerRPCPrivate KmsSCTPServerRPCPrivate;

struct _KmsSCTPServerRPC
{
  KmsSCTPBaseRPC parent;

  /*< private > */
  KmsSCTPServerRPCPrivate *priv;
};

struct _KmsSCTPServerRPCClass
{
  KmsSCTPBaseRPCClass parent_class;
};

GType kms_sctp_server_rpc_get_type (void);

KmsSCTPServerRPC *kms_sctp_server_rpc_new (const char *optname1, ...);

gboolean kms_sctp_server_rpc_start (KmsSCTPServerRPC *server, gchar *host,
  gint port, GCancellable *cancellable, GError **err);

gboolean kms_sctp_server_rpc_get_buffer (KmsSCTPServerRPC *server,
  GstBuffer ** outbuf, GError **err);

void kms_sctp_server_rpc_stop (KmsSCTPServerRPC *server);

G_END_DECLS
#endif