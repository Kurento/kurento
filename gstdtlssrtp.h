/*
 * GStreamer
 *
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */


#ifndef __GST_DTLS_SRTP_H__
#define __GST_DTLS_SRTP_H__

G_BEGIN_DECLS

typedef enum
{
  GST_DTLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80 = (1 << 1),
  GST_DTLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32 = (1 << 2),
  GST_DTLS_SRTP_PROFILE_NULL_HMAC_SHA1_80 = (1 << 3),
  GST_DTLS_SRTP_PROFILE_NULL_HMAC_SHA1_32 = (1 << 4),
} GstDtlsSrtpProfile;

G_END_DECLS
#endif /* __GST_DTLS_SRTP_H__ */
