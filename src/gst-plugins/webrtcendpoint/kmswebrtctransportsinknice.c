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

#include "kmswebrtctransportsinknice.h"
#include <commons/constants.h>

#define GST_DEFAULT_NAME "webrtctransportsinknice"
#define GST_CAT_DEFAULT kms_webrtc_transport_sink_nice_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_sink_nice_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcTransportSinkNice, kms_webrtc_transport_sink_nice,
    KMS_TYPE_WEBRTC_TRANSPORT_SINK);

static void
kms_webrtc_transport_sink_nice_init (KmsWebrtcTransportSinkNice * self)
{
  KmsWebrtcTransportSink *parent = KMS_WEBRTC_TRANSPORT_SINK (self);

  parent->sink = gst_element_factory_make ("nicesink", NULL);

  kms_webrtc_transport_sink_connect_elements (parent);
}

static void
kms_webrtc_transport_sink_nice_class_init (KmsWebrtcTransportSinkNiceClass *
    klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSinkNice",
      "Generic",
      "WebRTC nice transport sink elements.",
      "David Fernandez Lopez <d.fernandezlop@gmail.com>");
}

KmsWebrtcTransportSinkNice *
kms_webrtc_transport_sink_nice_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SINK_NICE, NULL);

  return KMS_WEBRTC_TRANSPORT_SINK_NICE (obj);
}
