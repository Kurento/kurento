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
#include "kmssctpserverrpc.h"

#define NAME "sctpserverrpc"

#define PARENT_CLASS kms_sctp_server_rpc_parent_class
#define KMS_SCTP_SERVER_RPC_CANCELLABLE "kms-sctp-server-rpc-cancellable"

GST_DEBUG_CATEGORY_STATIC (kms_sctp_server_rpc_debug_category);
#define GST_CAT_DEFAULT kms_sctp_server_rpc_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsSCTPServerRPC, kms_sctp_server_rpc,
    KMS_TYPE_SCTP_BASE_RPC,
    GST_DEBUG_CATEGORY_INIT (kms_sctp_server_rpc_debug_category, NAME,
        0, "debug category for kurento sctp server rpc"));

#define KMS_SCTP_SERVER_RPC_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_SCTP_SERVER_RPC, KmsSCTPServerRPCPrivate))

struct _KmsSCTPServerRPCPrivate
{
  KmsSCTPConnection *server;

  GCond data_cond;
  GMutex data_mutex;
  gboolean err;
  GIOErrorEnum code;
  GstBuffer *buffer;
};

static void
kms_sctp_server_rpc_finalize (GObject * gobject)
{
  KmsSCTPServerRPC *self = KMS_SCTP_SERVER_RPC (gobject);

  if (self->priv->server != NULL)
    kms_sctp_connection_unref (self->priv->server);

  if (self->priv->buffer != NULL) {
    gst_buffer_unref (self->priv->buffer);
  }

  g_mutex_clear (&self->priv->data_mutex);
  g_cond_clear (&self->priv->data_cond);
  G_OBJECT_CLASS (PARENT_CLASS)->finalize (gobject);
}

static void
kms_sctp_server_rpc_buffer (KmsSCTPBaseRPC * baserpc, GstBuffer * buffer)
{
  KmsSCTPServerRPC *self = KMS_SCTP_SERVER_RPC (baserpc);

  /* Wake stream thread up */
  g_mutex_lock (&self->priv->data_mutex);

  while (self->priv->buffer != NULL && !self->priv->err)
    g_cond_wait (&self->priv->data_cond, &self->priv->data_mutex);

  if (self->priv->buffer == NULL) {
    self->priv->buffer = buffer;
  } else {
    /* There was an error. Drop buffer */
    GST_ERROR_OBJECT (self, "Dropping buffer because of an internal error (%d)",
        self->priv->code);
    gst_buffer_unref (buffer);
  }

  g_cond_signal (&self->priv->data_cond);
  g_mutex_unlock (&self->priv->data_mutex);
}

static void
kms_sctp_server_rpc_class_init (KmsSCTPServerRPCClass * klass)
{
  GObjectClass *gobject_class;
  KmsSCTPBaseRPCClass *sctpbaserpc_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_sctp_server_rpc_finalize;

  sctpbaserpc_class = KMS_SCTP_BASE_RPC_CLASS (klass);
  sctpbaserpc_class->buffer = kms_sctp_server_rpc_buffer;

  g_type_class_add_private (klass, sizeof (KmsSCTPServerRPCPrivate));
}

static void
kms_sctp_server_rpc_init (KmsSCTPServerRPC * self)
{
  self->priv = KMS_SCTP_SERVER_RPC_GET_PRIVATE (self);

  g_mutex_init (&self->priv->data_mutex);
  g_cond_init (&self->priv->data_cond);
}

KmsSCTPServerRPC *
kms_sctp_server_rpc_new (const char *optname1, ...)
{
  KmsSCTPServerRPC *self;

  va_list ap;

  va_start (ap, optname1);
  self = KMS_SCTP_SERVER_RPC (g_object_new_valist (KMS_TYPE_SCTP_SERVER_RPC,
          optname1, ap));
  va_end (ap);

  return KMS_SCTP_SERVER_RPC (self);
}

static void
kms_sctp_sever_rpc_thread (KmsSCTPServerRPC * server)
{
  GCancellable *cancellable;
  KmsSCTPResult result;
  GError *err = NULL;
  KmsSCTPMessage msg = { 0 };
  KmsSCTPConnection *conn = NULL;
  GIOErrorEnum code = G_IO_ERROR_FAILED;
  gsize size;

  KMS_SCTP_BASE_RPC_LOCK (server);

  cancellable = g_object_get_data (G_OBJECT (server),
      KMS_SCTP_SERVER_RPC_CANCELLABLE);

  if (KMS_SCTP_BASE_RPC (server)->conn == NULL) {
    GST_DEBUG ("Accept client connections");
    KMS_SCTP_BASE_RPC_UNLOCK (server);

    /* wait on server socket for connections */
    result = kms_sctp_connection_accept (server->priv->server, cancellable,
        &KMS_SCTP_BASE_RPC (server)->conn, &err);

    if (result != KMS_SCTP_OK) {
      if (g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
        code = G_IO_ERROR_CANCELLED;
      }

      g_error_free (err);
      goto error;
    }

    /* TODO: Notify client connection */
    KMS_SCTP_BASE_RPC_LOCK (server);
  }

  conn = kms_sctp_connection_ref (KMS_SCTP_BASE_RPC (server)->conn);
  size = KMS_SCTP_BASE_RPC (server)->buffer_size;

  KMS_SCTP_BASE_RPC_UNLOCK (server);

  INIT_SCTP_MESSAGE (msg, size);

  result = kms_sctp_connection_receive (conn, &msg, cancellable, &err);

  if (result != KMS_SCTP_OK) {
    if (result == KMS_SCTP_EOF) {
      /* Notify EOF */
      code = G_IO_ERROR_CLOSED;
    }
    goto error;
  }

  kms_sctp_base_rpc_process_message (KMS_SCTP_BASE_RPC (server), &msg);

  CLEAR_SCTP_MESSAGE (msg);
  kms_sctp_connection_unref (conn);

  return;

error:
  {
    GST_DEBUG_OBJECT (server, "Pausing task");

    CLEAR_SCTP_MESSAGE (msg);

    if (conn != NULL)
      kms_sctp_connection_unref (conn);

    /* Wake stream thread up */
    g_mutex_lock (&server->priv->data_mutex);
    server->priv->err = TRUE;
    server->priv->code = code;
    g_cond_signal (&server->priv->data_cond);
    g_mutex_unlock (&server->priv->data_mutex);

    /* pause task */
    KMS_SCTP_BASE_RPC_LOCK (server);
    if (KMS_SCTP_BASE_RPC (server)->task != NULL)
      gst_task_pause (KMS_SCTP_BASE_RPC (server)->task);
    KMS_SCTP_BASE_RPC_UNLOCK (server);
  }
}

gboolean
kms_sctp_server_rpc_start (KmsSCTPServerRPC * server, gchar * host,
    gint * port, GCancellable * cancellable, GError ** err)
{
  KmsSCTPConnection *conn = NULL;

  g_return_val_if_fail (server != NULL, FALSE);

  KMS_SCTP_BASE_RPC_LOCK (server);

  if (server->priv->server != NULL) {
    goto create_task;
  }

  conn = kms_sctp_connection_new (host, *port, cancellable, err);

  if (conn == NULL) {
    GST_ERROR_OBJECT (server, "Error creating SCTP server socket");
    goto fail;
  }

  if (!kms_sctp_connection_set_init_config (conn, SCTP_DEFAULT_NUM_OSTREAMS,
          SCTP_DEFAULT_MAX_INSTREAMS, 0, 0, err)) {
    goto fail;
  }

  if (kms_sctp_connection_bind (conn, cancellable, err) != KMS_SCTP_OK) {
    goto fail;
  }

  server->priv->server = conn;

create_task:

  if (kms_sctp_base_rpc_start_task (KMS_SCTP_BASE_RPC (server),
          (GstTaskFunction) kms_sctp_sever_rpc_thread, server, NULL)) {
    g_object_set_data (G_OBJECT (server), KMS_SCTP_SERVER_RPC_CANCELLABLE,
        cancellable);
    *port = kms_sctp_connection_get_bound_port (server->priv->server);
    KMS_SCTP_BASE_RPC_UNLOCK (server);
    return TRUE;
  }

  server->priv->server = NULL;

fail:
  KMS_SCTP_BASE_RPC_UNLOCK (server);

  if (conn != NULL)
    kms_sctp_connection_unref (conn);

  return FALSE;
}

gboolean
kms_sctp_server_rpc_get_buffer (KmsSCTPServerRPC * server,
    GstBuffer ** outbuf, GError ** err)
{
  gboolean ret;

  g_return_val_if_fail (server != NULL, -1);

  g_mutex_lock (&server->priv->data_mutex);

  while (server->priv->buffer == NULL && !server->priv->err)
    g_cond_wait (&server->priv->data_cond, &server->priv->data_mutex);

  if ((ret = server->priv->buffer != NULL)) {
    *outbuf = server->priv->buffer;
    server->priv->buffer = NULL;
  } else {
    *outbuf = NULL;
    switch (server->priv->code) {
      case G_IO_ERROR_CLOSED:
        g_set_error (err, G_IO_ERROR, G_IO_ERROR_CLOSED, "Closed");
        break;
      case G_IO_ERROR_CANCELLED:
        g_set_error (err, G_IO_ERROR, G_IO_ERROR_CANCELLED, "Cancelled");
        break;
      default:
        g_set_error (err, G_IO_ERROR, server->priv->code, "Error");
        break;
    }
  }

  g_cond_signal (&server->priv->data_cond);
  g_mutex_unlock (&server->priv->data_mutex);

  return ret;
}

void
kms_sctp_server_rpc_stop (KmsSCTPServerRPC * server)
{
  KmsSCTPConnection *conn, *srv;

  g_return_if_fail (server != NULL);

  KMS_SCTP_BASE_RPC_LOCK (server);

  srv = server->priv->server;
  conn = KMS_SCTP_BASE_RPC (server)->conn;

  KMS_SCTP_BASE_RPC (server)->conn = NULL;
  server->priv->server = NULL;

  KMS_SCTP_BASE_RPC_UNLOCK (server);

  kms_sctp_base_rpc_stop_task (KMS_SCTP_BASE_RPC (server));

  if (srv != NULL) {
    kms_sctp_connection_close (srv);
    kms_sctp_connection_unref (srv);
  }

  if (conn != NULL) {
    GST_DEBUG ("Closing server socket");
    kms_sctp_connection_close (conn);
    kms_sctp_connection_unref (conn);
  }

  g_cancellable_cancel (KMS_SCTP_BASE_RPC (server)->cancellable);
}
