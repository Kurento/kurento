/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#include "kmssocketutils.h"

void
kms_socket_finalize (GSocket ** socket)
{
  if (socket == NULL || *socket == NULL) {
    return;
  }

  g_socket_close (*socket, NULL);
  g_clear_object (socket);
}

static GSocket *
kms_socket_open (guint16 port)
{
  GSocket *socket;
  GSocketAddress *bind_saddr;
  GInetAddress *addr;

  socket = g_socket_new (G_SOCKET_FAMILY_IPV4, G_SOCKET_TYPE_DATAGRAM,
      G_SOCKET_PROTOCOL_UDP, NULL);
  if (socket == NULL) {
    return NULL;
  }

  addr = g_inet_address_new_any (G_SOCKET_FAMILY_IPV4);
  bind_saddr = g_inet_socket_address_new (addr, port);
  g_object_unref (addr);
  if (!g_socket_bind (socket, bind_saddr, FALSE, NULL)) {
    g_socket_close (socket, NULL);
    g_object_unref (socket);
    socket = NULL;
  }
  g_object_unref (bind_saddr);

  return socket;
}

guint16
kms_socket_get_port (GSocket * socket)
{
  GInetSocketAddress *addr;
  guint16 port;

  addr = G_INET_SOCKET_ADDRESS (g_socket_get_local_address (socket, NULL));
  if (!addr) {
    return 0;
  }

  port = g_inet_socket_address_get_port (addr);
  g_inet_socket_address_get_address (addr);
  g_object_unref (addr);

  return port;
}

gboolean
kms_rtp_connection_get_rtp_rtcp_sockets (GSocket ** rtp, GSocket ** rtcp,
    guint16 min_port, guint16 max_port)
{
  GSocket *s1, *s2;
  guint16 port1, port2;

  if (rtp == NULL || rtcp == NULL) {
    return FALSE;
  }

  if (min_port != 0 && max_port != 0 && min_port != 1
      && max_port != G_MAXUINT16) {
    port1 = (guint16) g_random_int_range (min_port + 1, max_port);
  } else {
    port1 = 0;
  }

  s1 = kms_socket_open (port1);

  if (s1 == NULL) {
    return FALSE;
  }

  port1 = kms_socket_get_port (s1);

  if (port1 & 0x01) {
    port2 = port1 - 1;
  } else {
    port2 = port1 + 1;
  }

  s2 = kms_socket_open (port2);

  if (s2 == NULL) {
    kms_socket_finalize (&s1);
    return FALSE;
  }

  if (port1 < port2) {
    *rtp = s1;
    *rtcp = s2;
  } else {
    *rtp = s2;
    *rtcp = s1;
  }

  return TRUE;
}
