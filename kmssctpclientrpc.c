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

#include <gst/gst.h>
#include "kmssctpclientrpc.h"

#define NAME "sctpclientrpc"

#define PARENT_CLASS kms_sctp_client_rpc_parent_class

#define KMS_SCTP_CLIENT_RPC_CANCELLABLE "kms-sctp-client-rpc-cancellable"

GST_DEBUG_CATEGORY_STATIC (kms_sctp_client_rpc_debug_category);
#define GST_CAT_DEFAULT kms_sctp_client_rpc_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsSCTPClientRPC, kms_sctp_client_rpc,
    KMS_TYPE_SCTP_BASE_RPC,
    GST_DEBUG_CATEGORY_INIT (kms_sctp_client_rpc_debug_category, NAME,
        0, "debug category for kurento sctp client rpc"));

#define KMS_SCTP_CLIENT_RPC_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_SCTP_CLIENT_RPC, KmsSCTPClientRPCPrivate))

struct _KmsSCTPClientRPCPrivate
{
  GstTask *task;
  GRecMutex tmutex;

  KmsSocketErrorFunction cb;
  gpointer cb_data;
  GDestroyNotify destroy;
};

static void
kms_sctp_client_rpc_finalize (GObject * gobject)
{
  KmsSCTPClientRPC *self = KMS_SCTP_CLIENT_RPC (gobject);

  if (self->priv->cb_data != NULL && self->priv->destroy != NULL)
    self->priv->destroy (self->priv->cb_data);

  g_rec_mutex_clear (&self->priv->tmutex);

  G_OBJECT_CLASS (PARENT_CLASS)->finalize (gobject);
}

static void
kms_sctp_client_rpc_class_init (KmsSCTPClientRPCClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_sctp_client_rpc_finalize;

  g_type_class_add_private (klass, sizeof (KmsSCTPClientRPCPrivate));
}

static void
kms_sctp_client_rpc_init (KmsSCTPClientRPC * self)
{
  self->priv = KMS_SCTP_CLIENT_RPC_GET_PRIVATE (self);
  g_rec_mutex_init (&self->priv->tmutex);
}

KmsSCTPClientRPC *
kms_sctp_client_rpc_new (const char *optname1, ...)
{
  KmsSCTPClientRPC *self;

  va_list ap;

  va_start (ap, optname1);
  self = KMS_SCTP_CLIENT_RPC (g_object_new_valist (KMS_TYPE_SCTP_CLIENT_RPC,
          optname1, ap));
  va_end (ap);

  return KMS_SCTP_CLIENT_RPC (self);
}

static void
kms_sctp_client_rpc_thread (KmsSCTPClientRPC * clientrpc)
{
  GCancellable *cancellable;
  GError *err = NULL;
  KmsSCTPConnection *conn;
  KmsSCTPResult result;
  KmsSCTPMessage msg;
  gsize size;

  KMS_SCTP_BASE_RPC_LOCK (clientrpc);

  if (KMS_SCTP_BASE_RPC (clientrpc)->conn == NULL) {
    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);
    return;
  }

  cancellable = g_object_get_data (G_OBJECT (clientrpc),
      KMS_SCTP_CLIENT_RPC_CANCELLABLE);
  conn = kms_sctp_connection_ref (KMS_SCTP_BASE_RPC (clientrpc)->conn);
  size = KMS_SCTP_BASE_RPC (clientrpc)->buffer_size;

  KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

  if (g_cancellable_is_cancelled (cancellable))
    goto pause;

  INIT_SCTP_MESSAGE (msg, size);
  result = kms_sctp_connection_receive (conn, &msg, cancellable, &err);

  if (result != KMS_SCTP_OK)
    goto error;

  GST_DEBUG ("Got buffer!");

  CLEAR_SCTP_MESSAGE (msg);
  kms_sctp_connection_unref (conn);

  return;

error:
  {
    KmsSocketErrorFunction cb;
    gpointer cb_data;

    if (err != NULL) {
      GST_ERROR ("Error code (%u): %s", result, err->message);
      g_error_free (err);
    } else {
      GST_ERROR ("Failed reading from socket code (%u)", result);
    }

    KMS_SCTP_BASE_RPC_LOCK (clientrpc);

    cb = clientrpc->priv->cb;
    cb_data = clientrpc->priv->cb_data;

    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

    if (cb != NULL)
      cb (cb_data);
  }

pause:
  {
    CLEAR_SCTP_MESSAGE (msg);
    kms_sctp_connection_unref (conn);

    /* pause task */
    KMS_SCTP_BASE_RPC_LOCK (clientrpc);
    if (clientrpc->priv->task != NULL)
      gst_task_pause (clientrpc->priv->task);
    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);
  }
}

gboolean
kms_sctp_client_rpc_start (KmsSCTPClientRPC * clientrpc, gchar * host,
    gint port, GCancellable * cancellable, GError ** err)
{
  KmsSCTPConnection *conn = NULL;
  KmsSCTPResult result;
  GstTask *task;

  g_return_val_if_fail (clientrpc != NULL, FALSE);

  KMS_SCTP_BASE_RPC_LOCK (clientrpc);

  if (KMS_SCTP_BASE_RPC (clientrpc)->conn != NULL) {
    goto create_task;
  }

  KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

  conn = kms_sctp_connection_new (host, port, cancellable, err);

  if (conn == NULL) {
    GST_ERROR_OBJECT (clientrpc, "Error creating SCTP socket");
    return FALSE;
  }

  if (!kms_sctp_connection_set_init_config (conn, SCTP_DEFAULT_NUM_OSTREAMS,
          SCTP_DEFAULT_MAX_INSTREAMS, 0, 0, err)) {
    kms_sctp_connection_unref (conn);
    return FALSE;
  }

  result = kms_sctp_connection_connect (conn, cancellable, err);
  if (result != KMS_SCTP_OK) {
    GST_ERROR_OBJECT (clientrpc, "Error connecting SCTP socket");
    kms_sctp_connection_unref (conn);
    return FALSE;
  }

  KMS_SCTP_BASE_RPC_LOCK (clientrpc);

create_task:

  if (clientrpc->priv->task != NULL) {
    if (conn != NULL)
      KMS_SCTP_BASE_RPC (clientrpc)->conn = conn;
    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);
    return TRUE;
  }

  task =
      gst_task_new ((GstTaskFunction) kms_sctp_client_rpc_thread, clientrpc,
      NULL);
  if (task == NULL) {
    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);
    goto task_error;
  }

  clientrpc->priv->task = task;

  KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

  gst_task_set_lock (task, &clientrpc->priv->tmutex);

  g_object_set_data (G_OBJECT (clientrpc), KMS_SCTP_CLIENT_RPC_CANCELLABLE,
      cancellable);

  if (!gst_task_start (task)) {
    KMS_SCTP_BASE_RPC_LOCK (clientrpc);
    clientrpc->priv->task = NULL;
    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);
    goto task_error;
  }

  if (conn == NULL)
    return TRUE;

  KMS_SCTP_BASE_RPC_LOCK (clientrpc);

  if (KMS_SCTP_BASE_RPC (clientrpc)->conn == NULL) {
    KMS_SCTP_BASE_RPC (clientrpc)->conn = conn;
  } else {
    kms_sctp_connection_close (conn);
    kms_sctp_connection_unref (conn);
  }

  KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

  return TRUE;

  /* ERRORS */
task_error:
  {
    GST_ERROR ("failed to create task");

    if (conn != NULL) {
      kms_sctp_connection_close (conn);
      kms_sctp_connection_unref (conn);
    }

    gst_object_unref (task);

    g_object_steal_data (G_OBJECT (clientrpc), KMS_SCTP_CLIENT_RPC_CANCELLABLE);

    return FALSE;
  }
}

void
kms_sctp_client_rpc_stop (KmsSCTPClientRPC * clientrpc)
{
  KmsSCTPConnection *conn;
  GstTask *task;

  g_return_if_fail (clientrpc != NULL);

  KMS_SCTP_BASE_RPC_LOCK (clientrpc);

  conn = KMS_SCTP_BASE_RPC (clientrpc)->conn;
  KMS_SCTP_BASE_RPC (clientrpc)->conn = NULL;

  if ((task = clientrpc->priv->task)) {
    clientrpc->priv->task = NULL;

    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

    gst_task_stop (task);

    /* make sure it is not running */
    g_rec_mutex_lock (&clientrpc->priv->tmutex);
    g_rec_mutex_unlock (&clientrpc->priv->tmutex);

    /* now wait for the task to finish */
    gst_task_join (task);

    /* and free the task */
    gst_object_unref (GST_OBJECT (task));

  } else {
    KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);
  }

  if (conn != NULL) {
    kms_sctp_connection_close (conn);
    kms_sctp_connection_unref (conn);
  }

  g_cancellable_cancel (KMS_SCTP_BASE_RPC (clientrpc)->cancellable);
}

void
kms_sctp_client_rpc_set_error_function_full (KmsSCTPClientRPC * clientrpc,
    KmsSocketErrorFunction func, gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  g_return_if_fail (clientrpc != NULL);

  KMS_SCTP_BASE_RPC_LOCK (clientrpc);

  data = clientrpc->priv->cb_data;
  destroy = clientrpc->priv->destroy;

  clientrpc->priv->cb_data = user_data;
  clientrpc->priv->destroy = notify;
  clientrpc->priv->cb = func;

  KMS_SCTP_BASE_RPC_UNLOCK (clientrpc);

  if (data != NULL && destroy != NULL)
    destroy (data);
}
