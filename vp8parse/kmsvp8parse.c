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
#include "config.h"
#endif

#include "kmsvp8parse.h"

#include <gst/gst.h>
#include <gst/base/gstbaseparse.h>

#define PLUGIN_NAME "vp8parse"

#define GST_CAT_DEFAULT kms_vp8_parse_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_VP8_PARSE_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_VP8_PARSE,                         \
    KmsVp8ParsePrivate                          \
  )                                             \
)

struct _KmsVp8ParsePrivate
{
  gboolean started;
};

/* pad templates */

#define VIDEO_SRC_CAPS "video/x-vp8"

#define VIDEO_SINK_CAPS "video/x-vp8"

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsVp8Parse, kms_vp8_parse,
    GST_TYPE_BASE_PARSE,
    GST_DEBUG_CATEGORY_INIT (kms_vp8_parse_debug_category, PLUGIN_NAME,
        0, "debug category for vp8parse element"));

static gboolean
kms_vp8_parse_start (GstBaseParse * parse)
{
  KmsVp8Parse *self = KMS_VP8_PARSE (parse);

  self->priv->started = FALSE;
  return TRUE;
}

static GstFlowReturn
kms_vp8_parse_handle_frame (GstBaseParse * parse, GstBaseParseFrame * frame,
    gint * skipsize)
{
  return GST_FLOW_OK;
}

void
kms_vp8_parse_dispose (GObject * object)
{
  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_vp8_parse_parent_class)->dispose (object);
}

void
kms_vp8_parse_finalize (GObject * object)
{
  G_OBJECT_CLASS (kms_vp8_parse_parent_class)->finalize (object);
}

static void
kms_vp8_parse_init (KmsVp8Parse * vp8parse)
{
  vp8parse->priv = KMS_VP8_PARSE_GET_PRIVATE (vp8parse);
}

static void
kms_vp8_parse_class_init (KmsVp8ParseClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstBaseParseClass *base_parse_class = GST_BASE_PARSE_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  GST_DEBUG ("class init");

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Vp8 parse element", "Codec/Parser/Converter/Video",
      "Parses vp8 video streams",
      "José Antonio Santos <santoscadenas@kurento.com>");

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_vp8_parse_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_vp8_parse_finalize);

  base_parse_class->start = GST_DEBUG_FUNCPTR (kms_vp8_parse_start);
  base_parse_class->handle_frame =
      GST_DEBUG_FUNCPTR (kms_vp8_parse_handle_frame);
  /* Properties initialization */

  g_type_class_add_private (klass, sizeof (KmsVp8ParsePrivate));
}

gboolean
kms_vp8_parse_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_VP8_PARSE);
}
