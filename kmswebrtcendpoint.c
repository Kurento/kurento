/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include "kmswebrtcendpoint.h"
#include <nice/nice.h>

#define PLUGIN_NAME "webrtcendpoint"

#define GST_CAT_DEFAULT kms_webrtc_end_point_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_end_point_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcEndPoint, kms_webrtc_end_point,
    KMS_TYPE_BASE_RTP_END_POINT);

#define KMS_WEBRTC_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_END_POINT,                  \
    KmsWebrtcEndPointPrivate                    \
  )                                             \
)

struct _KmsWebrtcEndPointPrivate
{
  NiceAgent *agent;
};

static gboolean
kms_webrtc_end_point_set_transport_to_sdp (KmsBaseSdpEndPoint *
    base_sdp_endpoint, GstSDPMessage * msg)
{
  GST_WARNING ("TODO: complete");

  return TRUE;
}

static void
kms_webrtc_end_point_start_transport_send (KmsBaseSdpEndPoint *
    base_rtp_end_point, const GstSDPMessage * offer,
    const GstSDPMessage * answer, gboolean local_offer)
{
  GST_WARNING ("TODO: complete");
}

static void
kms_webrtc_end_point_class_init (KmsWebrtcEndPointClass * klass)
{
  KmsBaseSdpEndPointClass *base_sdp_end_point_class;

  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "WebrtcEndPoint",
      "WEBRTC/Stream/WebrtcEndPoint",
      "WebRTC EndPoint element", "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_end_point_class = KMS_BASE_SDP_END_POINT_CLASS (klass);
  base_sdp_end_point_class->set_transport_to_sdp =
      kms_webrtc_end_point_set_transport_to_sdp;
  base_sdp_end_point_class->start_transport_send =
      kms_webrtc_end_point_start_transport_send;

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndPointPrivate));
}

static void
kms_webrtc_end_point_init (KmsWebrtcEndPoint * self)
{
  self->priv = KMS_WEBRTC_END_POINT_GET_PRIVATE (self);
}

gboolean
kms_webrtc_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_END_POINT);
}
