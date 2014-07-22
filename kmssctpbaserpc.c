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

typedef void (*KmsProcessEnsambledFunction) (KmsSCTPBaseRPC * baserpc,
    guint req_id, KmsAssembler * assembler);

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

  g_hash_table_foreach (self->pending_reqs, cancel_pending_req, NULL);
  g_hash_table_unref (self->pending_reqs);
  g_hash_table_unref (self->requests);
  g_hash_table_unref (self->responses);

  if (self->conn != NULL)
    kms_sctp_connection_unref (self->conn);

  if (self->query_notify != NULL)
    self->query_notify (self->query_data);

  g_rec_mutex_clear (&self->rmutex);
  g_rec_mutex_clear (&self->tmutex);

  g_clear_object (&self->cancellable);

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
  self->cancellable = g_cancellable_new ();
  self->pending_reqs =
      g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL, NULL);
  self->requests = g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL,
      (GDestroyNotify) kms_assembler_unref);
  self->responses = g_hash_table_new_full (g_direct_hash, g_direct_equal, NULL,
      (GDestroyNotify) kms_assembler_unref);
}

void
kms_scp_base_rpc_cancel_pending_requests (KmsSCTPBaseRPC * baserpc)
{
  g_return_if_fail (baserpc != NULL);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  g_hash_table_foreach (baserpc->pending_reqs, cancel_pending_req, NULL);
  g_hash_table_remove_all (baserpc->pending_reqs);

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
    KmsSCTPResult result;

    msg = kms_fragmenter_nth_message (f, i);

    INIT_SCTP_MESSAGE (sctpmsg, baserpc->buffer_size);

    sctpmsg.used = enc_KmsMessage (baserpc->rules, msg, sctpmsg.buf,
        sctpmsg.size, err);

    if (sctpmsg.used < 0) {
      CLEAR_SCTP_MESSAGE (sctpmsg);
      return FALSE;
    }

    result =
        kms_sctp_connection_send (baserpc->conn, &sctpmsg, cancellable, err);
    CLEAR_SCTP_MESSAGE (sctpmsg);

    if (result != KMS_SCTP_OK) {
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

  if (!g_hash_table_insert (baserpc->pending_reqs, GUINT_TO_POINTER (req_id),
          req)) {
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
      goto done;
  }

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  g_hash_table_remove (baserpc->pending_reqs, GUINT_TO_POINTER (req_id));

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

  return (*err == NULL);
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

void
kms_sctp_base_rpc_set_query_function (KmsSCTPBaseRPC * baserpc,
    KmsQueryFunction func, gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  g_return_if_fail (baserpc != NULL);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  destroy = baserpc->query_notify;
  data = baserpc->query_data;

  baserpc->query = func;
  baserpc->query_notify = notify;
  baserpc->query_data = user_data;

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

  if (destroy != NULL)
    destroy (data);
}

static GstQuery *
pack_fragmented_query (KmsAssembler * assembler)
{
  GstQuery *query = NULL;
  GError *err = NULL;
  gchar *buf = NULL;
  gsize size;

  kms_assembler_compose_buffer (assembler, &buf, &size);

  dec_GstQuery (kms_assembler_get_encoding_rules (assembler), buf, size, &query,
      &err);

  if (query != NULL)
    return query;

  GST_ERROR ("%s", err->message);
  g_error_free (err);
  g_free (buf);

  return NULL;
}

static void
kms_scp_base_rpc_query_response (KmsSCTPBaseRPC * baserpc, guint req_id,
    GstQuery * query)
{
  KmsFragmenter *f;
  GError *err = NULL;

  f = kms_fragmenter_new (baserpc->rules, baserpc->buffer_size);
  kms_fragmenter_set_message_type (f, GST_MARSHALL_RESPONSE);
  kms_fragmenter_query (f, req_id, query, &err);
  if (err != NULL) {
    goto done;
  }

  KMS_SCTP_BASE_RPC_LOCK (baserpc);
  kms_scp_base_rpc_send_fragments (baserpc, f, baserpc->cancellable, &err);
  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

done:
  if (err != NULL) {
    GST_ERROR_OBJECT (baserpc, "%s", err->message);
    g_error_free (err);
  }

  kms_fragmenter_unref (f);
}

static void
kms_sctp_base_rpc_process_ensambled_request (KmsSCTPBaseRPC * baserpc,
    guint req_id, KmsAssembler * assembler)
{
  switch (kms_assembler_get_data_type (assembler)) {
    case KMS_DATA_TYPE_QUERY:{
      KmsQueryFunction query_func;
      gpointer query_data;
      GstQuery *query;

      query = pack_fragmented_query (assembler);
      if (query == NULL)
        return;

      KMS_SCTP_BASE_RPC_LOCK (baserpc);
      if (baserpc->query == NULL) {
        KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
        return;
      }

      query_func = baserpc->query;
      query_data = baserpc->query_data;
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

      query_func (query, query_data);
      kms_scp_base_rpc_query_response (baserpc, req_id, query);
      gst_query_unref (query);
      break;
    }
    case KMS_DATA_TYPE_EVENT:{
      GST_DEBUG ("TODO: Unpack events");
      break;
    }
    default:{
      GST_ERROR ("Unknown request received");
      break;
    }
  }
}

static void
kms_sctp_base_rpc_process_ensambled_response (KmsSCTPBaseRPC * baserpc,
    guint req_id, KmsAssembler * assembler)
{
  KmsPendingReq *req;

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  if (!g_hash_table_contains (baserpc->pending_reqs, GUINT_TO_POINTER (req_id))) {
    KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
    return;
  }

  req = g_hash_table_lookup (baserpc->pending_reqs, GUINT_TO_POINTER (req_id));

  g_mutex_lock (&req->mutex);
  req->status = KMS_RSP_SUCCESS;
  req->assembler = kms_assembler_ref (assembler);
  g_cond_signal (&req->cond);
  g_mutex_unlock (&req->mutex);

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
}

static void
kms_sctp_base_rpc_process (KmsSCTPBaseRPC * baserpc, KmsMessage * message,
    KmsProcessEnsambledFunction func)
{
  KmsAssembler *assembler = NULL;
  GHashTable *table;
  guint req_id;

  req_id = kms_message_get_req_id (message);

  KMS_SCTP_BASE_RPC_LOCK (baserpc);

  switch (kms_message_get_message_type (message)) {
    case GST_MARSHALL_REQUEST:
      table = baserpc->requests;
      break;
    case GST_MARSHALL_RESPONSE:
      table = baserpc->responses;
      break;
    default:
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      return;
  }

  if (!g_hash_table_contains (table, GUINT_TO_POINTER (req_id))) {
    guint fragment_id;

    fragment_id = kms_message_get_fragment_id (message);

    if (fragment_id != 0) {
      GST_DEBUG ("Incomplete fragment received (%u/%u). Dropping", req_id,
          fragment_id);
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      return;
    }

    assembler = kms_assembler_new (baserpc->rules);
    if (!kms_assembler_append_message (assembler, kms_message_ref (message))) {
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      GST_ERROR_OBJECT (baserpc, "Error assembling fragments. Dropping");
      kms_message_unref (message);
      goto done;
    }

    if (kms_assembler_is_completed (assembler)) {
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      func (baserpc, req_id, assembler);
      goto done;
    }

    if (!g_hash_table_insert (table, GUINT_TO_POINTER (req_id), assembler)) {
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      goto done;
    }

  } else {
    assembler = kms_assembler_ref (KMS_ASSEMBLER (g_hash_table_lookup (table,
                GUINT_TO_POINTER (req_id))));
    if (!kms_assembler_append_message (assembler, kms_message_ref (message))) {
      GST_ERROR_OBJECT (baserpc, "Error assembling fragments. Dropping");
      g_hash_table_remove (table, GUINT_TO_POINTER (req_id));
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      kms_message_unref (message);
      goto done;
    }

    if (kms_assembler_is_completed (assembler)) {
      kms_assembler_ref (assembler);
      g_hash_table_remove (table, GUINT_TO_POINTER (req_id));
      KMS_SCTP_BASE_RPC_UNLOCK (baserpc);
      func (baserpc, req_id, assembler);
      goto done;
    }
  }

  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

done:
  kms_assembler_unref (assembler);
}

void
kms_sctp_base_rpc_process_message (KmsSCTPBaseRPC * baserpc,
    const KmsSCTPMessage * msg)
{
  GError *err = NULL;
  KurentoMarshalRules rules;
  KmsProcessEnsambledFunction func;
  KmsMessage *message;
  KmsMessageType type;

  KMS_SCTP_BASE_RPC_LOCK (baserpc);
  rules = baserpc->rules;
  KMS_SCTP_BASE_RPC_UNLOCK (baserpc);

  dec_KmsMessage (rules, msg->buf, msg->used, &message, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (baserpc, "%s", err->message);
    g_error_free (err);
    return;
  }

  type = kms_message_get_message_type (message);

  switch (type) {
    case GST_MARSHALL_REQUEST:
      func = kms_sctp_base_rpc_process_ensambled_request;
      break;
    case GST_MARSHALL_RESPONSE:
      func = kms_sctp_base_rpc_process_ensambled_response;
      break;
    default:
      GST_WARNING ("Message type %d not supported", type);
      kms_message_unref (message);
      return;
  }

  kms_sctp_base_rpc_process (baserpc, message, func);
  kms_message_unref (message);

  return;
}
