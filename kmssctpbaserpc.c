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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <kurento/gstmarshal/kmsfragmenter.h>
#include <kurento/gstmarshal/kmsassembler.h>
#include <gst/gst.h>

#include "kmssctpbaserpc.h"
#include "kmsencodingrules.h"
#include "kms-enumtypes.h"

#define NAME "sctpbaserpc"

#define PARENT_CLASS kms_sctp_base_rpc_parent_class

#define DEFAULT_ENCODING_RULES KMS_ENCODING_RULES_BER

GST_DEBUG_CATEGORY_STATIC (kms_sctp_base_rpc_debug_category);
#define GST_CAT_DEFAULT kms_sctp_base_rpc_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsSCTPBaseRPC, kms_sctp_base_rpc,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_sctp_base_rpc_debug_category, NAME,
        0, "debug category for kurento sctp base rpc"));

#define MAX_BUFFER_SIZE (1024 * 16)
#define RESP_TIMEOUT 3          /* seconds */
#define KMS_SCTP_BASE_RPC_ERROR \
  g_quark_from_static_string("kms-sctp-base_rpc-error-quark")

typedef enum
{
  KMS_SCTP_BASE_RPC_CANCELLED,
  KMS_SCTP_BASE_RPC_TIMEOUT,
  KMS_SCTP_BASE_RPC_UNEXPECTED_ERROR
} KmsSCTPBaseRPCError;

enum
{
  PROP_0,
  PROP_ENCODING_RULES,
  PROP_BUFFER_SIZE
};

typedef enum
{
  KMS_RSP_SUCCESS,
  KMS_RSP_TIMEOUT,
  KMS_RSP_CANCELLED
} KmsRspStatus;

typedef struct _KmsPendingReq
{
  GCond cond;
  GMutex mutex;
  KmsAssembler *assembler;
  KmsRspStatus status;
  gboolean done;
} KmsPendingReq;

static void
destroy_pending_req (KmsPendingReq * preq)
{
  if (preq->assembler != NULL)
    kms_assembler_unref (preq->assembler);

  g_mutex_clear (&preq->mutex);
  g_cond_clear (&preq->cond);

  g_slice_free (KmsPendingReq, preq);
}

static KmsPendingReq *
create_pending_req (KurentoMarshalRules rules)
{
  KmsPendingReq *preq;

  preq = g_slice_new0 (KmsPendingReq);

  g_mutex_init (&preq->mutex);
  g_cond_init (&preq->cond);

  preq->assembler = kms_assembler_new (rules);

  return preq;
}

static KurentoMarshalRules
get_kurento_marshal_rules (KmsEncodingRules rules)
{
  switch (rules) {
    case KMS_ENCODING_RULES_BER:
      return KURENTO_MARSHALL_BER;
    case KMS_ENCODING_RULES_XER:
      return KURENTO_MARSHALL_XER;
    default:
      return KURENTO_MARSHALL_PER;
  }
}

static KmsEncodingRules
get_kurento_encoding_rules (KurentoMarshalRules rules)
{
  switch (rules) {
    case KURENTO_MARSHALL_BER:
      return KMS_ENCODING_RULES_BER;
    case KURENTO_MARSHALL_XER:
      return KMS_ENCODING_RULES_XER;
    default:
      return KMS_ENCODING_RULES_PER;
  }
}

static void
kms_sctp_base_rpc_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsSCTPBaseRPC *self;

  g_return_if_fail (KMS_IS_SCTP_BASE_RPC (object));
  self = KMS_SCTP_BASE_RPC (object);

  KMS_SCTP_BASE_RPC_LOCK (self);

  switch (prop_id) {
    case PROP_ENCODING_RULES:
      g_value_set_enum (value, get_kurento_encoding_rules (self->rules));
      break;
    case PROP_BUFFER_SIZE:
      g_value_set_uint (value, self->buffer_size);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_SCTP_BASE_RPC_UNLOCK (self);
}

static void
kms_sctp_base_rpc_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsSCTPBaseRPC *self;

  g_return_if_fail (KMS_IS_SCTP_BASE_RPC (object));
  self = KMS_SCTP_BASE_RPC (object);

  KMS_SCTP_BASE_RPC_LOCK (self);

  switch (prop_id) {
    case PROP_ENCODING_RULES:
      self->rules = get_kurento_marshal_rules (g_value_get_enum (value));
      break;
    case PROP_BUFFER_SIZE:
      self->buffer_size = g_value_get_uint (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  KMS_SCTP_BASE_RPC_UNLOCK (self);
}

static void
cancel_pending_req (gpointer key, gpointer value, gpointer user_data)
{
  guint32 req_id;
  KmsPendingReq *req = value;

  req_id = GPOINTER_TO_INT (key);
  GST_DEBUG ("Cancelling  request (%u)", req_id);

  g_mutex_lock (&req->mutex);
  req->status = KMS_RSP_CANCELLED;
  g_cond_signal (&req->cond);
  g_mutex_unlock (&req->mutex);
}

static void
kms_sctp_base_rpc_finalize (GObject * obj)
{
  KmsSCTPBaseRPC *self = KMS_SCTP_BASE_RPC (obj);

  GST_DEBUG_OBJECT (obj, "Finalize");

  g_hash_table_foreach (self->reqs, cancel_pending_req, NULL);
  g_hash_table_unref (self->reqs);

  if (self->conn != NULL)
    kms_sctp_connection_unref (self->conn);

  g_rec_mutex_clear (&self->rmutex);
  g_rec_mutex_clear (&self->tmutex);

  G_OBJECT_CLASS (PARENT_CLASS)->finalize (obj);
}

static void
kms_sctp_base_rpc_class_init (KmsSCTPBaseRPCClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_sctp_base_rpc_set_property;
  gobject_class->get_property = kms_sctp_base_rpc_get_property;
  gobject_class->finalize = kms_sctp_base_rpc_finalize;

  g_object_class_install_property (gobject_class, PROP_ENCODING_RULES,
      g_param_spec_enum (KMS_SCTP_BASE_RPC_RULES, "Rules",
          "ASN.1 encoding rules",
          KMS_TYPE_ENCODING_RULES,
          DEFAULT_ENCODING_RULES, G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY));
  g_object_class_install_property (gobject_class, PROP_BUFFER_SIZE,
      g_param_spec_uint (KMS_SCTP_BASE_RPC_BUFFER_SIZE, "Buffer size",
          "Size of buffer used for transmissions over SCTP",
          0, G_MAXUINT, MAX_BUFFER_SIZE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY));
}

static void
kms_sctp_base_rpc_init (KmsSCTPBaseRPC * self)
{
  g_rec_mutex_init (&self->rmutex);
  g_rec_mutex_init (&self->tmutex);
  self->reqs =
      g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL, NULL);
}

void
kms_scp_base_rpc_cancel_pending_requests (KmsSCTPBaseRPC * baserpc)
{
  g_return_if_fail (baserpc != NULL);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  g_hash_table_foreach (baserpc->reqs, cancel_pending_req, NULL);
  g_hash_table_remove_all (baserpc->reqs);

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
}

static gboolean
kms_scp_base_rpc_send_fragments (KmsSCTPBaseRPC * baserpc, KmsFragmenter * f,
    GCancellable * cancellable, GError ** err)
{
  guint n, i = 0;

  n = kms_fragmenter_n_messages (f);
  for (i = 0; i < n; i++) {
    KmsSCTPMessage sctpmsg;
    const KmsMessage *msg;
    KmsDataType type;

    msg = kms_fragmenter_nth_message (f, i);
    kms_message_get_data (msg, &type, (const char **) &sctpmsg.buf,
        &sctpmsg.size);
    sctpmsg.used = sctpmsg.size;

    if (kms_sctp_connection_send (baserpc->conn, &sctpmsg, cancellable, err)
        != KMS_SCTP_OK) {
      return FALSE;
    }
  }

  return TRUE;
}

static void
kms_scp_base_rpc_wait_resp (KmsPendingReq * req)
{
  gint64 end_time;

  g_mutex_lock (&req->mutex);

  end_time = g_get_monotonic_time () + RESP_TIMEOUT * G_TIME_SPAN_SECOND;

  while (!req->done) {
    if (!g_cond_wait_until (&req->cond, &req->mutex, end_time))
      req->status = KMS_RSP_TIMEOUT;
    g_mutex_unlock (&req->mutex);
    return;
  }

  g_mutex_unlock (&req->mutex);
}

static KmsAssembler *
kms_scp_base_rpc_send_request (KmsSCTPBaseRPC * baserpc, KmsFragmenter * f,
    guint32 req_id, GCancellable * cancellable, GError ** err)
{
  KmsPendingReq *req;
  KmsAssembler *assembler = NULL;

  req = create_pending_req (baserpc->rules);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  if (!g_hash_table_insert (baserpc->reqs, GUINT_TO_POINTER (req_id), req)) {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    g_set_error (err, KMS_SCTP_BASE_RPC_ERROR,
        KMS_SCTP_BASE_RPC_UNEXPECTED_ERROR, "Can't not send request %u",
        req_id);
    destroy_pending_req (req);
    kms_fragmenter_unref (f);
    return NULL;
  }

  if (!kms_scp_base_rpc_send_fragments (baserpc, f, cancellable, err)) {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    return NULL;
  }

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

  kms_fragmenter_unref (f);

  kms_scp_base_rpc_wait_resp (req);

  switch (req->status) {
    case KMS_RSP_SUCCESS:
      assembler = kms_assembler_ref (req->assembler);
      break;
    case KMS_RSP_TIMEOUT:
      g_set_error (err, KMS_SCTP_BASE_RPC_ERROR, KMS_SCTP_BASE_RPC_TIMEOUT,
          "Request (%u) timeout", req_id);
      break;
    case KMS_RSP_CANCELLED:
      g_set_error (err, KMS_SCTP_BASE_RPC_ERROR, KMS_SCTP_BASE_RPC_CANCELLED,
          "Request (%u) cancelled", req_id);
      goto done;;
  }

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  g_hash_table_remove (baserpc->reqs, GUINT_TO_POINTER (req_id));

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

done:
  destroy_pending_req (req);

  return assembler;
}

static gboolean
unpack_fragmented_query (KmsAssembler * assembler, GstQuery ** query,
    GError ** err)
{
  gchar *buf = NULL;
  gsize size;

  kms_assembler_compose_buffer (assembler, &buf, &size);

  dec_GstQuery (kms_assembler_get_encoding_rules (assembler), buf, size, query,
      err);

  g_free (buf);

  return (*err != NULL);
}

gboolean
kms_scp_base_rpc_query (KmsSCTPBaseRPC * baserpc, GstQuery * query,
    GCancellable * cancellable, GstQuery ** rsp, GError ** err)
{
  KmsFragmenter *f;
  KmsAssembler *a;
  guint32 req_id;
  gboolean ret;

  g_return_val_if_fail (baserpc != NULL, FALSE);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  f = kms_fragmenter_new (baserpc->rules, baserpc->buffer_size);
  req_id = baserpc->req_id++;

  if (!kms_fragmenter_query (f, req_id, query, err)) {
    baserpc->req_id--;
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    kms_fragmenter_unref (f);
    return FALSE;
  }

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

  a = kms_scp_base_rpc_send_request (baserpc, f, req_id, cancellable, err);
  if (a == NULL)
    return FALSE;

  ret = unpack_fragmented_query (a, rsp, err);

  kms_assembler_unref (a);

  return ret;
}

gboolean
kms_sctp_base_rpc_start_task (KmsSCTPBaseRPC * baserpc,
    GstTaskFunction func, gpointer user_data, GDestroyNotify notify)
{
  GstTask *task;

  g_return_val_if_fail (baserpc != NULL, FALSE);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  if (baserpc->task != NULL) {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    return FALSE;
  }

  baserpc->task = gst_task_new (func, user_data, notify);
  if (baserpc->task == NULL) {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    return FALSE;
  }

  gst_task_set_lock (baserpc->task, &baserpc->tmutex);

  if (gst_task_start (baserpc->task)) {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    return TRUE;
  }

  task = baserpc->task;
  baserpc->task = NULL;

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

  /* Task is not started */
  gst_task_join (task);
  gst_object_unref (GST_OBJECT (task));

  return FALSE;
}

void
kms_sctp_base_rpc_stop_task (KmsSCTPBaseRPC * baserpc)
{
  GstTask *task;

  g_return_if_fail (baserpc != NULL);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  if ((task = baserpc->task)) {
    baserpc->task = NULL;

    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

    gst_task_stop (task);

    /* make sure it is not running */
    g_rec_mutex_lock (&baserpc->tmutex);
    g_rec_mutex_unlock (&baserpc->tmutex);

    /* now wait for the task to finish */
    gst_task_join (task);

    /* and free the task */
    gst_object_unref (GST_OBJECT (task));

  } else {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
  }
}
