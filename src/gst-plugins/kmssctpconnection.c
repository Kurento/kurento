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

#include <kurento/gstmarshal/kmsfragmenter.h>
#include <netinet/sctp.h>
#include <string.h>
#include <gio/gio.h>

#include "kmssctpconnection.h"
#include "gstsctp.h"

#define KMS_SCTP_CONNECTION_ERROR \
  g_quark_from_static_string("kms-sctp-connection-error-quark")

#define SCTP_BACKLOG 1          /* client connection queue */

typedef enum
{
  KMS_CONNECTION_READ_ERROR
} KmsSCTPConnectionError;

GST_DEBUG_CATEGORY_STATIC (kms_sctp_connection_debug);
#define GST_CAT_DEFAULT kms_sctp_connection_debug

GType _kms_sctp_connection_type = 0;

struct _KmsSCTPConnection
{
  GstMiniObject obj;

  GSocket *socket;
  GSocketAddress *saddr;
};

GST_DEFINE_MINI_OBJECT_TYPE (KmsSCTPConnection, kms_sctp_connection);

static void
_priv_kms_sctp_connection_initialize (void)
{
  _kms_sctp_connection_type = kms_sctp_connection_get_type ();

  GST_DEBUG_CATEGORY_INIT (kms_sctp_connection_debug, "sctpconnection", 0,
      "sctp connection");
}

static void
_kms_sctp_connection_free (KmsSCTPConnection * conn)
{
  g_return_if_fail (conn != NULL);

  GST_DEBUG ("free");

  if (conn->socket != NULL)
    kms_sctp_connection_close (conn);

  g_clear_object (&conn->socket);
  g_clear_object (&conn->saddr);

  g_slice_free1 (sizeof (KmsSCTPConnection), conn);
}

static gboolean
kms_sctp_connection_create_socket (KmsSCTPConnection * conn, gchar * host,
    gint port, GCancellable * cancellable, GError ** err)
{
  GInetAddress *addr;

  /* look up name if we need to */
  addr = g_inet_address_new_from_string (host);
  if (addr == NULL) {
    GResolver *resolver;
    GList *results;

    resolver = g_resolver_get_default ();
    results = g_resolver_lookup_by_name (resolver, host, cancellable, err);

    if (results == NULL) {
      g_object_unref (resolver);
      return FALSE;
    }

    addr = G_INET_ADDRESS (g_object_ref (results->data));

    g_resolver_free_addresses (results);
    g_object_unref (resolver);
  }

  if (G_UNLIKELY (GST_LEVEL_DEBUG <= _gst_debug_min)) {
    gchar *ip = g_inet_address_to_string (addr);

    GST_DEBUG ("IP address for host %s is %s", host, ip);
    g_free (ip);
  }

  conn->saddr = g_inet_socket_address_new (addr, port);
  g_object_unref (addr);

  conn->socket = g_socket_new (g_socket_address_get_family (conn->saddr),
      G_SOCKET_TYPE_STREAM, G_SOCKET_PROTOCOL_SCTP, err);

  if (conn->socket == NULL) {
    g_clear_object (&conn->saddr);
    return FALSE;
  }

  /* create socket */
  GST_DEBUG ("created SCTP socket for %s", host);

  return TRUE;
}

KmsSCTPConnection *
kms_sctp_connection_new (gchar * host, gint port, GCancellable * cancellable,
    GError ** err)
{
  KmsSCTPConnection *conn;

  conn = g_slice_new0 (KmsSCTPConnection);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (conn), 0,
      _kms_sctp_connection_type, NULL, NULL,
      (GstMiniObjectFreeFunction) _kms_sctp_connection_free);

  if (!kms_sctp_connection_create_socket (conn, host, port, cancellable, err)) {
    kms_sctp_connection_unref (conn);
    return NULL;
  }

  return GST_SCTP_CONNECTION (conn);
}

void
kms_sctp_connection_close (KmsSCTPConnection * conn)
{
  GError *err = NULL;

  g_return_if_fail (conn != NULL);

  if (conn->socket == NULL)
    return;

  if (g_socket_is_closed (conn->socket)) {
    GST_DEBUG ("Socket is already closed");
    return;
  }

  if (!g_socket_shutdown (conn->socket, TRUE, TRUE, &err)) {
    GST_DEBUG ("%s", err->message);
    g_clear_error (&err);
  }

  GST_DEBUG ("Closing socket");

  if (!g_socket_close (conn->socket, &err)) {
    GST_ERROR ("Failed to close socket %p: %s", conn->socket, err->message);
    g_clear_error (&err);
  }
}

KmsSCTPResult
kms_sctp_connection_connect (KmsSCTPConnection * conn,
    GCancellable * cancellable, GError ** err)
{
  g_return_val_if_fail (conn != NULL, KMS_SCTP_ERROR);
  g_return_val_if_fail (conn->socket != NULL, KMS_SCTP_ERROR);
  g_return_val_if_fail (conn->saddr != NULL, KMS_SCTP_ERROR);

  if (g_socket_is_connected (conn->socket))
    return KMS_SCTP_OK;

  /* connect to server */
  if (!g_socket_connect (conn->socket, conn->saddr, cancellable, err))
    return KMS_SCTP_ERROR;

  if (G_UNLIKELY (GST_LEVEL_DEBUG <= _gst_debug_min)) {
#if defined (SCTP_INITMSG)
    struct sctp_initmsg initmsg;
    socklen_t optlen;

    if (getsockopt (g_socket_get_fd (conn->socket), IPPROTO_SCTP,
            SCTP_INITMSG, &initmsg, &optlen) < 0)
      GST_WARNING ("Could not get SCTP configuration: %s (%d)",
          g_strerror (errno), errno);
    else
      GST_DEBUG ("SCTP client socket: ostreams %u, instreams %u",
          initmsg.sinit_num_ostreams, initmsg.sinit_num_ostreams);
#else
    GST_WARNING ("don't know how to get the configuration of the "
        "SCTP initiation structure on this OS.");
#endif
  }

  GST_DEBUG ("connected sctp socket");

  return KMS_SCTP_OK;
}

KmsSCTPResult
kms_sctp_connection_bind (KmsSCTPConnection * conn, GCancellable * cancellable,
    GError ** err)
{
  gint bound_port;

  g_return_val_if_fail (conn != NULL, KMS_SCTP_ERROR);
  g_return_val_if_fail (conn->socket != NULL, KMS_SCTP_ERROR);
  g_return_val_if_fail (conn->saddr != NULL, KMS_SCTP_ERROR);

  /* bind it */
  GST_DEBUG ("binding server socket");

  if (!g_socket_bind (conn->socket, conn->saddr, TRUE, err))
    return KMS_SCTP_ERROR;

  g_socket_set_listen_backlog (conn->socket, SCTP_BACKLOG);

  if (!g_socket_listen (conn->socket, err))
    return KMS_SCTP_ERROR;

  bound_port = kms_sctp_connection_get_bound_port (conn);

  if (bound_port > 0) {
    GST_DEBUG ("listening on port %d", bound_port);
  }

  return KMS_SCTP_OK;
}

KmsSCTPResult
kms_sctp_connection_accept (KmsSCTPConnection * conn,
    GCancellable * cancellable, KmsSCTPConnection ** client, GError ** err)
{
  KmsSCTPConnection *ccon;
  GSocket *socket;

  g_return_val_if_fail (conn != NULL, KMS_SCTP_ERROR);
  g_return_val_if_fail (conn->socket != NULL, KMS_SCTP_ERROR);

  socket = g_socket_accept (conn->socket, cancellable, err);
  if (socket == NULL)
    return KMS_SCTP_ERROR;

  ccon = g_slice_new0 (KmsSCTPConnection);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (ccon), 0,
      _kms_sctp_connection_type, NULL, NULL,
      (GstMiniObjectFreeFunction) _kms_sctp_connection_free);

  ccon->socket = socket;

  if (!kms_sctp_connection_set_event_subscribe (ccon, KMS_SCTP_DATA_IO_EVENT,
          err)) {
    kms_sctp_connection_unref (ccon);
    return KMS_SCTP_ERROR;
  }

  if (G_UNLIKELY (GST_LEVEL_DEBUG <= _gst_debug_min))
#if defined (SCTP_INITMSG)
  {
    struct sctp_initmsg initmsg;
    socklen_t optlen;

    if (getsockopt (g_socket_get_fd (socket), IPPROTO_SCTP,
            SCTP_INITMSG, &initmsg, &optlen) < 0) {
      GST_WARNING ("Could not get SCTP configuration: %s (%d)",
          g_strerror (errno), errno);
    } else {
      GST_DEBUG ("SCTP client socket: ostreams %u, instreams %u",
          initmsg.sinit_num_ostreams, initmsg.sinit_num_ostreams);
    }
  }
#else
  {
    GST_WARNING ("don't know how to get the configuration of the "
        "SCTP initiation structure on this OS.");
  }
#endif

  *client = ccon;

  return KMS_SCTP_OK;
}

KmsSCTPResult
kms_sctp_connection_receive (KmsSCTPConnection * conn, KmsSCTPMessage * message,
    GCancellable * cancellable, GError ** err)
{
  GIOCondition condition;
  guint streamid;

  g_return_val_if_fail (conn != NULL, KMS_SCTP_ERROR);
  g_return_val_if_fail (conn->socket != NULL, KMS_SCTP_ERROR);

  if (!g_socket_condition_wait (conn->socket,
          G_IO_IN | G_IO_PRI | G_IO_ERR | G_IO_HUP, cancellable, err)) {
    return KMS_SCTP_ERROR;
  }

  condition = g_socket_condition_check (conn->socket,
      G_IO_IN | G_IO_PRI | G_IO_ERR | G_IO_HUP);

  if ((condition & G_IO_ERR)) {
    g_set_error (err, KMS_SCTP_CONNECTION_ERROR, KMS_CONNECTION_READ_ERROR,
        "Socket in error state");
    return KMS_SCTP_ERROR;
  } else if ((condition & G_IO_HUP)) {
    g_set_error (err, KMS_SCTP_CONNECTION_ERROR, KMS_CONNECTION_READ_ERROR,
        "Connection closed");
    return KMS_SCTP_EOF;
  }

  message->used = sctp_socket_receive (conn->socket, message->buf,
      message->size, cancellable, &streamid, err);

  GST_LOG ("Receive data on stream id %d", streamid);

  if (message->used == 0)
    return KMS_SCTP_EOF;
  else if (message->used < 0)
    return KMS_SCTP_ERROR;
  else
    return KMS_SCTP_OK;
}

KmsSCTPResult
kms_sctp_connection_send (KmsSCTPConnection * conn, guint32 stream_id,
    guint32 timetolive, const KmsSCTPMessage * message,
    GCancellable * cancellable, GError ** err)
{
  gsize written = 0;
  gssize rret;

  g_return_val_if_fail (g_socket_is_connected (conn->socket), KMS_SCTP_EOF);

  /* write buffer data */
  while (written < message->used) {
    rret = sctp_socket_send (conn->socket, stream_id, timetolive,
        message->buf + written, message->used - written, cancellable, err);

    if (rret < 0)
      return KMS_SCTP_ERROR;

    written += rret;
  }

  return KMS_SCTP_OK;
}

gboolean
kms_sctp_connection_set_event_subscribe (KmsSCTPConnection * conn,
    KmsSCTPEventFlags events, GError ** err)
{
  g_return_val_if_fail (conn != NULL, FALSE);

#if defined (SCTP_EVENTS)
  {
    struct sctp_event_subscribe sctp_events;

    memset (&sctp_events, 0, sizeof (sctp_events));
    if (events & KMS_SCTP_DATA_IO_EVENT)
      sctp_events.sctp_data_io_event = 1;

    if (events & KMS_SCTP_ASSOCIATION_EVENT)
      sctp_events.sctp_association_event = 1;

    if (events & KMS_SCTP_ADDRESS_EVENT)
      sctp_events.sctp_address_event = 1;

    if (events & KMS_SCTP_SEND_FAILURE_EVENT)
      sctp_events.sctp_send_failure_event = 1;

    if (events & KMS_SCTP_PEER_ERROR_EVENT)
      sctp_events.sctp_peer_error_event = 1;

    if (events & KMS_SCTP_SHUTDOWN_EVENT)
      sctp_events.sctp_shutdown_event = 1;

    if (events & KMS_SCTP_PARTIAL_DELIVERY_EVENT)
      sctp_events.sctp_partial_delivery_event = 1;

    if (events & KMS_SCTP_ADAPTATION_LAYER_EVENT)
      sctp_events.sctp_adaptation_layer_event = 1;

    if (events & KMS_SCTP_AUTHENTICATION_EVENT)
      sctp_events.sctp_authentication_event = 1;

    if (setsockopt (g_socket_get_fd (conn->socket), IPPROTO_SCTP,
            SCTP_EVENTS, &sctp_events, sizeof (sctp_events)) < 0) {
      GST_ERROR ("Could not configure SCTP socket: %s (%d)",
          g_strerror (errno), errno);
      return FALSE;
    }

    return TRUE;
  }
#else
  {
    GST_WARNING ("don't know how to configure SCTP events " "on this OS.");

    g_set_error (err, KMS_SCTP_CONNECTION_ERROR, KMS_CONNECTION_CONFIG_ERROR,
        "Can not configure SCTP socket");

    return FALSE;
  }
#endif
}

gboolean
kms_sctp_connection_set_init_config (KmsSCTPConnection * conn,
    guint16 num_ostreams, guint16 max_instreams, guint16 max_attempts,
    guint16 max_init_timeo, GError ** err)
{
  g_return_val_if_fail (conn != NULL, FALSE);

#if defined (SCTP_INITMSG)
  {
    struct sctp_initmsg initmsg;

    memset (&initmsg, 0, sizeof (initmsg));
    initmsg.sinit_num_ostreams = num_ostreams;
    initmsg.sinit_max_instreams = max_instreams;
    initmsg.sinit_max_attempts = max_attempts;
    initmsg.sinit_max_init_timeo = max_init_timeo;

    if (setsockopt (g_socket_get_fd (conn->socket), IPPROTO_SCTP,
            SCTP_INITMSG, &initmsg, sizeof (initmsg)) < 0) {
      GST_ERROR ("Could not configure SCTP socket: %s (%d)",
          g_strerror (errno), errno);
      return FALSE;
    }

    return TRUE;
  }
#else
  {
    GST_WARNING ("don't know how to configure the SCTP initiation "
        "parameters on this OS.");

    g_set_error (err, KMS_SCTP_CONNECTION_ERROR, KMS_CONNECTION_CONFIG_ERROR,
        "Can not configure SCTP socket");

    return FALSE;
  }
#endif
}

int
kms_sctp_connection_get_bound_port (KmsSCTPConnection * conn)
{
  GSocketAddress *addr;
  gint bound_port = -1;

  g_return_val_if_fail (conn != NULL, bound_port);

  addr = g_socket_get_local_address (conn->socket, NULL);

  if (addr != NULL) {
    bound_port = g_inet_socket_address_get_port ((GInetSocketAddress *) addr);
    g_object_unref (addr);
  }

  return bound_port;
}

gchar *
kms_sctp_connection_get_remote_address (KmsSCTPConnection * conn)
{
  GSocketAddress *addr;
  GError *err = NULL;
  GInetAddress *iaddr;
  gchar *straddr;

  g_return_val_if_fail (conn != NULL, NULL);

  addr = g_socket_get_remote_address (conn->socket, &err);

  if (addr == NULL) {
    GST_ERROR ("%s", err->message);
    g_error_free (err);
    return NULL;
  }

  iaddr = g_inet_socket_address_get_address ((GInetSocketAddress *) addr);
  straddr = g_inet_address_to_string (iaddr);
  g_object_unref (addr);

  return straddr;
}

static void _priv_kms_sctp_connection_initialize (void)
    __attribute__ ((constructor));
