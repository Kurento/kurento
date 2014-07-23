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
#ifndef _KMS_SCTP_CLIENT_RPC_H_
#define _KMS_SCTP_CLIENT_RPC_H_

#include "kmssctpbaserpc.h"

G_BEGIN_DECLS
#define KMS_TYPE_SCTP_CLIENT_RPC (kms_sctp_client_rpc_get_type())
#define KMS_SCTP_CLIENT_RPC(obj) (         \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_SCTP_CLIENT_RPC,              \
    KmsSCTPClientRPC                       \
  )                                        \
)
#define KMS_SCTP_CLIENT_RPC_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_SCTP_CLIENT_RPC,              \
    KmsSCTPClientRPCClass                  \
  )                                        \
)
#define KMS_IS_SCTP_CLIENT_RPC(obj) (      \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_SCTP_CLIENT_RPC               \
  )                                        \
)
#define KMS_IS_SCTP_CLIENT_RPC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), KMS_TYPE_SCTP_CLIENT_RPC))
#define KMS_SCTP_CLIENT_RPC_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                \
    (obj),                                   \
    KMS_TYPE_SCTP_CLIENT_RPC,                \
    KmsSCTPClientRPCClass                    \
  )                                          \
)

typedef struct _KmsSCTPClientRPC KmsSCTPClientRPC;
typedef struct _KmsSCTPClientRPCClass KmsSCTPClientRPCClass;
typedef struct _KmsSCTPClientRPCPrivate KmsSCTPClientRPCPrivate;

typedef void (*KmsSocketErrorFunction) (gpointer);

struct _KmsSCTPClientRPC
{
  KmsSCTPBaseRPC parent;

  /*< private > */
  KmsSCTPClientRPCPrivate *priv;
};

struct _KmsSCTPClientRPCClass
{
  KmsSCTPBaseRPCClass parent_class;
};

GType kms_sctp_client_rpc_get_type (void);

KmsSCTPClientRPC *kms_sctp_client_rpc_new (const char *optname1, ...);

gboolean kms_sctp_client_rpc_start (KmsSCTPClientRPC *clientrpc, gchar *host,
  gint port, GCancellable *cancellable, GError **err);

void kms_sctp_client_rpc_stop (KmsSCTPClientRPC *clientrpc);

void kms_sctp_client_rpc_set_error_function_full (KmsSCTPClientRPC *clientrpc,
  KmsSocketErrorFunction func, gpointer user_data, GDestroyNotify notify);

G_END_DECLS
#endif