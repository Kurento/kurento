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
#ifndef _KMS_SCTP_BASE_RPC_H_
#define _KMS_SCTP_BASE_RPC_H_

#include <kurento/gstmarshal/marshal.h>
#include <gio/gio.h>
#include "kmssctpconnection.h"

G_BEGIN_DECLS
#define KMS_TYPE_SCTP_BASE_RPC (kms_sctp_base_rpc_get_type())
#define KMS_SCTP_BASE_RPC(obj) (           \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_SCTP_BASE_RPC,                \
    KmsSCTPBaseRPC                         \
  )                                        \
)
#define KMS_SCTP_BASE_RPC_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_SCTP_BASE_RPC,                \
    KmsSCTPBaseRPCClass                    \
  )                                        \
)
#define KMS_IS_SCTP_BASE_RPC(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_SCTP_BASE_RPC                 \
  )                                        \
)
#define KMS_IS_SCTP_BASE_RPC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), KMS_TYPE_SCTP_BASE_RPC))
#define KMS_SCTP_BASE_RPC_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_SCTP_BASE_RPC,                \
    KmsSCTPBaseRPCClass                    \
  )                                        \
)

#define SCTP_DEFAULT_NUM_OSTREAMS 1
#define SCTP_DEFAULT_MAX_INSTREAMS 1

#define KMS_SCTP_BASE_RPC_RULES "rules"
#define KMS_SCTP_BASE_RPC_BUFFER_SIZE "buffer-size"

typedef struct _KmsSCTPBaseRPC KmsSCTPBaseRPC;
typedef struct _KmsSCTPBaseRPCClass KmsSCTPBaseRPCClass;

#define KMS_SCTP_BASE_RPC_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_SCTP_BASE_RPC ((elem))->rmutex))
#define KMS_SCTP_BASE_RPC_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_SCTP_BASE_RPC ((elem))->rmutex))

typedef void (*KmsQueryFunction) (GstQuery *query, gpointer user_data);

struct _KmsSCTPBaseRPC
{
  GObject parent;

  /* <private> */
  guint32 req_id;

  KmsQueryFunction query;
  gpointer query_data;
  GDestroyNotify query_notify;

  /* < protected > */
  GRecMutex rmutex;
  KurentoMarshalRules rules;
  gsize buffer_size;
  GHashTable *pending_reqs;
  GHashTable *requests;
  KmsSCTPConnection *conn;

  GstTask *task;
  GRecMutex tmutex;
  GCancellable *cancellable;
};

struct _KmsSCTPBaseRPCClass
{
  GObjectClass parent_class;
};

GType kms_sctp_base_rpc_get_type (void);

/* public methods */
void kms_scp_base_rpc_cancel_pending_requests (KmsSCTPBaseRPC *baserpc);

gboolean kms_scp_base_rpc_query (KmsSCTPBaseRPC *baserpc, GstQuery *query,
  GCancellable *cancellable, GstQuery **rsp, GError **err);

void kms_sctp_base_rpc_set_query_function (KmsSCTPBaseRPC *baserpc,
  KmsQueryFunction func, gpointer user_data, GDestroyNotify notify);

/* protected methods */
gboolean kms_sctp_base_rpc_start_task(KmsSCTPBaseRPC *baserpc,
  GstTaskFunction func, gpointer user_data, GDestroyNotify notify);
void kms_sctp_base_rpc_stop_task(KmsSCTPBaseRPC *baserpc);

void kms_sctp_base_rpc_process_message(KmsSCTPBaseRPC *baserpc, const KmsSCTPMessage *msg);

G_END_DECLS
#endif