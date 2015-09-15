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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmswebrtctransportsink.h"
#include <commons/constants.h>

#define GST_DEFAULT_NAME "webrtctransportsink"
#define GST_CAT_DEFAULT kms_webrtc_transport_sink_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_sink_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcTransportSink, kms_webrtc_transport_sink, GST_TYPE_BIN);

#define FUNNEL_NAME "funnel"
#define SRTPENC_NAME "srtp-encoder"

static void
kms_webrtc_transport_sink_init (KmsWebrtcTransportSink * self)
{
  self->dtlssrtpenc = gst_element_factory_make ("dtlssrtpenc", NULL);
}

void
kms_webrtc_transport_sink_connect_elements (KmsWebrtcTransportSink * self)
{
  GstElement *funnel, *srtpenc;

  gst_bin_add_many (GST_BIN (self), self->dtlssrtpenc, self->sink, NULL);
  gst_element_link (self->dtlssrtpenc, self->sink);

  funnel = gst_bin_get_by_name (GST_BIN (self->dtlssrtpenc), FUNNEL_NAME);
  if (funnel != NULL) {
    g_object_set (funnel, "forward-sticky-events", FALSE, NULL);
    g_object_unref (funnel);
  } else {
    GST_WARNING ("Cannot get funnel with name %s", FUNNEL_NAME);
  }

  srtpenc = gst_bin_get_by_name (GST_BIN (self->dtlssrtpenc), SRTPENC_NAME);
  if (srtpenc != NULL) {
    g_object_set (srtpenc, "allow-repeat-tx", TRUE, "replay-window-size",
        RTP_RTX_SIZE, NULL);
    g_object_unref (srtpenc);
  } else {
    GST_WARNING ("Cannot get srtpenc with name %s", SRTPENC_NAME);
  }
}

static void
kms_webrtc_transport_sink_class_init (KmsWebrtcTransportSinkClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSink",
      "Generic",
      "Base bin to manage WebRTC transport sink elements.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}

KmsWebrtcTransportSink *
kms_webrtc_transport_sink_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SINK, NULL);

  return KMS_WEBRTC_TRANSPORT_SINK (obj);
}
