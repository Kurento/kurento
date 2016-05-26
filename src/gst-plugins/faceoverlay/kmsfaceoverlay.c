/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsfaceoverlay.h"

#include <gst/gst.h>

#define PLUGIN_NAME "faceoverlay"

GST_DEBUG_CATEGORY_STATIC (kms_face_overlay_debug_category);
#define GST_CAT_DEFAULT kms_face_overlay_debug_category

#define KMS_FACE_OVERLAY_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (             \
    (obj),                                  \
    KMS_TYPE_FACE_OVERLAY,                  \
    KmsFaceOverlayPrivate                   \
  )                                         \
)

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  PROP_IMAGE_TO_OVERLAY
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (VIDEO_SRC_CAPS)
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (VIDEO_SRC_CAPS)
    );

struct _KmsFaceOverlayPrivate
{
  GstElement *face_detector;
  GstElement *image_overlay;
  GstPad *src, *sink;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsFaceOverlay, kms_face_overlay,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_face_overlay_debug_category, PLUGIN_NAME,
        0, "debug category for faceoverlay element"));

static void
kms_face_overlay_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsFaceOverlay *faceoverlay = KMS_FACE_OVERLAY (object);

  faceoverlay->priv = KMS_FACE_OVERLAY_GET_PRIVATE (faceoverlay);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      faceoverlay->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_IMAGE_TO_OVERLAY:
      g_object_set (faceoverlay->priv->image_overlay, "image-to-overlay",
          g_value_get_boxed (value), NULL);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_face_overlay_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsFaceOverlay *faceoverlay = KMS_FACE_OVERLAY (object);

  faceoverlay->priv = KMS_FACE_OVERLAY_GET_PRIVATE (faceoverlay);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, faceoverlay->show_debug_info);
      break;
    case PROP_IMAGE_TO_OVERLAY:
    {
      GstStructure *aux;

      g_object_get (G_OBJECT (faceoverlay->priv->image_overlay),
          "image-to-overlay", &aux, NULL);
      g_value_set_boxed (value, aux);
      gst_structure_free (aux);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_face_overlay_init (KmsFaceOverlay * faceoverlay)
{
  GstPadTemplate *templ;
  GstPad *target;

  faceoverlay->priv = KMS_FACE_OVERLAY_GET_PRIVATE (faceoverlay);

  faceoverlay->priv->face_detector =
      gst_element_factory_make ("facedetector", NULL);
  gst_bin_add (GST_BIN (faceoverlay), faceoverlay->priv->face_detector);

  faceoverlay->priv->image_overlay =
      gst_element_factory_make ("imageoverlay", NULL);
  gst_bin_add (GST_BIN (faceoverlay), faceoverlay->priv->image_overlay);

  target =
      gst_element_get_static_pad (faceoverlay->priv->face_detector, "sink");
  templ = gst_static_pad_template_get (&sink_factory);
  faceoverlay->priv->sink =
      gst_ghost_pad_new_from_template ("sink", target, templ);
  g_object_unref (templ);
  g_object_unref (target);
  gst_element_add_pad (GST_ELEMENT (faceoverlay), faceoverlay->priv->sink);

  target = gst_element_get_static_pad (faceoverlay->priv->image_overlay, "src");
  templ = gst_static_pad_template_get (&src_factory);
  faceoverlay->priv->src =
      gst_ghost_pad_new_from_template ("src", target, templ);
  g_object_unref (templ);
  g_object_unref (target);
  gst_element_add_pad (GST_ELEMENT (faceoverlay), faceoverlay->priv->src);

  gst_element_link (faceoverlay->priv->face_detector,
      faceoverlay->priv->image_overlay);
}

static void
kms_face_overlay_class_init (KmsFaceOverlayClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Face Overlay element", "Video/Filter",
      "Detect faces in an image", "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_face_overlay_set_property;
  gobject_class->get_property = kms_face_overlay_get_property;

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_IMAGE_TO_OVERLAY,
      g_param_spec_boxed ("image-to-overlay", "image to overlay",
          "set the url of the image to overlay the faces",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsFaceOverlayPrivate));
}

gboolean
kms_face_overlay_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_FACE_OVERLAY);
}
