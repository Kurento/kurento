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

#ifndef __KMS_WEBRTC_TRANSPORT_H__
#define __KMS_WEBRTC_TRANSPORT_H__

#include <gst/gst.h>
#include <nice/nice.h>

typedef struct _KmsWebRtcTransport
{
  guint component_id;

  GstElement *dtlssrtpenc;
  GstElement *dtlssrtpdec;
  GstElement *nicesink;
  GstElement *nicesrc;
} KmsWebRtcTransport;

KmsWebRtcTransport *kms_webrtc_transport_create (NiceAgent * agent,
    guint stream_id, guint component_id);
void kms_webrtc_transport_destroy (KmsWebRtcTransport * tr);

void kms_webrtc_transport_nice_agent_recv_cb (NiceAgent * agent,
    guint stream_id, guint component_id, guint len, gchar * buf,
    gpointer user_data);

#endif /* __KMS_WEBRTC_TRANSPORT_H__ */
