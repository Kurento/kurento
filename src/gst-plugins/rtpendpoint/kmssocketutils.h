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

#ifndef __KMS_SOCKETUTILS_H__
#define __KMS_SOCKETUTILS_H__

#include <gio/gio.h>

void kms_socket_finalize (GSocket ** socket);
guint16 kms_socket_get_port (GSocket * socket);
gboolean kms_rtp_connection_get_rtp_rtcp_sockets (GSocket ** rtp,
    GSocket ** rtcp, guint16 min_port, guint16 max_port);

#endif /* __KMS_SOCKETUTILS_H__ */
