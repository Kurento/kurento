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

#define PLUGIN_NAME "webrtcendpoint"

#define GST_CAT_DEFAULT kms_webrtc_end_point_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_end_point_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcEndPoint, kms_webrtc_end_point,
    KMS_TYPE_BASE_RTP_END_POINT);

static void
kms_webrtc_end_point_class_init (KmsWebrtcEndPointClass * klass)
{
  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "WebrtcEndPoint",
      "WEBRTC/Stream/WebrtcEndPoint",
      "WebRTC EndPoint element", "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
kms_webrtc_end_point_init (KmsWebrtcEndPoint * element)
{
}

gboolean
kms_webrtc_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_END_POINT);
}
