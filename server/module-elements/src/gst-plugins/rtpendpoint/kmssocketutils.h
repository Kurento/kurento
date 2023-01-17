/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#ifndef __KMS_SOCKETUTILS_H__
#define __KMS_SOCKETUTILS_H__

#include <gio/gio.h>

void kms_socket_finalize (GSocket ** socket);
guint16 kms_socket_get_port (GSocket * socket);
gboolean kms_rtp_connection_get_rtp_rtcp_sockets (GSocket ** rtp,
    GSocket ** rtcp, guint16 min_port, guint16 max_port, GSocketFamily socket_family);

#endif /* __KMS_SOCKETUTILS_H__ */
