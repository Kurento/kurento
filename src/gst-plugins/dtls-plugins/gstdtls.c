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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdtlsconnection.h"
#include "gstdtlssrtpdemux.h"

#include "gstdtlssrtpenc.h"
#include "gstdtlssrtpdec.h"

static gboolean
kms_dtls_init (GstPlugin * plugin)
{
  if (!gst_element_register (plugin, "dtlsenc", GST_RANK_NONE,
          GST_TYPE_DTLS_ENC)) {
    return FALSE;
  }

  if (!gst_element_register (plugin, "dtlsdec", GST_RANK_NONE,
          GST_TYPE_DTLS_DEC)) {
    return FALSE;
  }

  if (!gst_element_register (plugin, "dtlssrtpdemux", GST_RANK_NONE,
          GST_TYPE_DTLS_SRTP_DEMUX)) {
    return FALSE;
  }

  if (!gst_element_register (plugin, "dtlssrtpenc", GST_RANK_NONE,
          GST_TYPE_DTLS_SRTP_ENC)) {
    return FALSE;
  }

  if (!gst_element_register (plugin, "dtlssrtpdec", GST_RANK_NONE,
          GST_TYPE_DTLS_SRTP_DEC)) {
    return FALSE;
  }

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsdtls,
    "Kurento Datagram TLS",
    kms_dtls_init, VERSION, "LGPL", "Kurento",
    "http://kurento.com/")
