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
#include "kmsiceniceagent.h"
#include <stdlib.h>

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

void
kms_webrtc_transport_sink_nice_configure (KmsWebrtcTransportSink * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (agent);
  guint id = atoi (stream_id);

  g_object_set (G_OBJECT (self->sink),
      "agent", kms_ice_nice_agent_get_agent (nice_agent),
      "stream", id, "component", component_id,
      "sync", FALSE, "async", FALSE, NULL);
}

static void
kms_webrtc_transport_sink_nice_class_init (KmsWebrtcTransportSinkNiceClass *
    klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  KmsWebrtcTransportSinkClass *base_class;

  base_class = KMS_WEBRTC_TRANSPORT_SINK_CLASS (klass);
  base_class->configure = kms_webrtc_transport_sink_nice_configure;

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
