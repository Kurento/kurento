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

#include <gio/gio.h>
#include <errno.h>
#include <string.h>

#include <netinet/sctp.h>

#include "gstsctp.h"

static gssize
sctp_socket_send_with_blocking (GSocket * socket, guint streamid,
    guint32 timetolive, const gchar * buffer, gsize size, gboolean blocking,
    GCancellable * cancellable, GError ** error)
{
  gssize ret;

  if (g_socket_is_closed (socket)) {
    g_set_error_literal (error, G_IO_ERROR, G_IO_ERROR_CLOSED,
        "Socket is already closed");
    return -1;
  }

  if (g_cancellable_set_error_if_cancelled (cancellable, error))
    return -1;

  while (TRUE) {
    if (blocking &&
        !g_socket_condition_wait (socket, G_IO_OUT, cancellable, error))
      return -1;

    if ((ret = sctp_sendmsg (g_socket_get_fd (socket), buffer, size, NULL, 0,
                0, 0, streamid, timetolive, 0)) < 0) {
      if (errno == EINTR)
        continue;

      if (blocking) {
        if (errno == EWOULDBLOCK || errno == EAGAIN)
          continue;
      }

      g_set_error (error, G_IO_ERROR, errno, "Error sending data: %s",
          strerror (errno));
      return -1;
    }
    break;
  }

  return ret;
}

gssize
sctp_socket_send (GSocket * socket, guint streamid, guint32 timetolive,
    const gchar * buffer, gsize size, GCancellable * cancellable,
    GError ** error)
{
  g_return_val_if_fail (G_IS_SOCKET (socket) && buffer != NULL, -1);

  return sctp_socket_send_with_blocking (socket, streamid, timetolive, buffer,
      size, g_socket_get_blocking (socket), cancellable, error);
}

static gssize
sctp_socket_receive_with_blocking (GSocket * socket, gchar * buffer, gsize size,
    gboolean blocking, GCancellable * cancellable, guint * streamid,
    GError ** error)
{
  gssize ret;

  if (g_socket_is_closed (socket)) {
    g_set_error_literal (error, G_IO_ERROR, G_IO_ERROR_CLOSED,
        "Socket is already closed");
    return -1;
  }

  if (g_cancellable_set_error_if_cancelled (cancellable, error))
    return -1;

  while (TRUE) {
    struct sctp_sndrcvinfo sndrcvinfo;
    int flags = 0;

    if (blocking &&
        !g_socket_condition_wait (socket, G_IO_IN, cancellable, error))
      return -1;

    if ((ret = sctp_recvmsg (g_socket_get_fd (socket), buffer, size, NULL, 0,
                &sndrcvinfo, &flags)) < 0) {
      if (errno == EINTR)
        continue;

      if (blocking) {
        if (errno == EWOULDBLOCK || errno == EAGAIN)
          continue;
      }

      g_set_error (error, G_IO_ERROR, errno, "Error receiving data: %s",
          strerror (errno));

      return -1;
    }

    *streamid = sndrcvinfo.sinfo_stream;
    break;
  }

  return ret;
}

gssize
sctp_socket_receive (GSocket * socket, gchar * buffer, gsize size,
    GCancellable * cancellable, guint * streamid, GError ** error)
{
  g_return_val_if_fail (G_IS_SOCKET (socket) && buffer != NULL, -1);

  return sctp_socket_receive_with_blocking (socket, buffer, size,
      g_socket_get_blocking (socket), cancellable, streamid, error);
}
