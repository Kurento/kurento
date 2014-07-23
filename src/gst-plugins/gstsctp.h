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
#ifndef __GST_SCTP_H__
#define __GST_SCTP_H__

#define STREAM_ID_HIHEST G_MAXUINT16
#define STREAM_ID_LOWEST 0
#define DEFAULT_STREAM_ID STREAM_ID_LOWEST

#define SCTP_DEFAULT_PORT 8000
#define SCTP_DEFAULT_HOST "localhost"
#define SCTP_DEFAULT_STREAM 0

gssize
sctp_socket_send (GSocket *socket, guint streamid, guint32 timetolive,
  const gchar *buffer, gsize size, GCancellable *cancellable, GError **error);

gssize
sctp_socket_receive (GSocket *socket, gchar *buffer, gsize size,
  GCancellable *cancellable, guint* streamid, GError **error);

#endif /* __GST_SCTP_BASE_SINK_H__ */