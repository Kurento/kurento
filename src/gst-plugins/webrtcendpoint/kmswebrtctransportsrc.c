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

#include "kmswebrtctransportsrc.h"

#define GST_DEFAULT_NAME "webrtctransportsrc"
#define GST_CAT_DEFAULT kms_webrtc_transport_src_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_src_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcTransportSrc, kms_webrtc_transport_src, GST_TYPE_BIN);

#define SRTPDEC_NAME "srtp-decoder"
#define REPLAY_WINDOW_SIZE 512  /* inmediate-TODO: move to constants file */

static void
kms_webrtc_transport_src_init (KmsWebrtcTransportSrc * self)
{
  GstElement *srtpdec;

  self->nicesrc = gst_element_factory_make ("nicesrc", NULL);
  self->dtlssrtpdec = gst_element_factory_make ("dtlssrtpdec", NULL);

  gst_bin_add_many (GST_BIN (self), self->nicesrc, self->dtlssrtpdec, NULL);
  gst_element_link (self->nicesrc, self->dtlssrtpdec);

  srtpdec = gst_bin_get_by_name (GST_BIN (self->dtlssrtpdec), SRTPDEC_NAME);
  if (srtpdec != NULL) {
    g_object_set (srtpdec, "replay-window-size", REPLAY_WINDOW_SIZE, NULL);
    g_object_unref (srtpdec);
  } else {
    GST_WARNING ("Cannot get srtpdec with name %s", SRTPDEC_NAME);
  }
}

static void
kms_webrtc_transport_src_class_init (KmsWebrtcTransportSrcClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSrc",
      "Generic",
      "Base bin to manage WebRTC transport src elements.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}

KmsWebrtcTransportSrc *
kms_webrtc_transport_src_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SRC, NULL);

  return KMS_WEBRTC_TRANSPORT_SRC (obj);
}
