/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#  include <config.h>
#endif

#include "kmsdectreebin.h"
#include "kmsutils.h"

#define GST_DEFAULT_NAME "dectreebin"
#define GST_CAT_DEFAULT kms_dec_tree_bin_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_dec_tree_bin_parent_class parent_class
G_DEFINE_TYPE (KmsDecTreeBin, kms_dec_tree_bin, KMS_TYPE_TREE_BIN);

static GstElement *
create_decoder_for_caps (const GstCaps * caps, const GstCaps * raw_caps)
{
  GList *decoder_list, *filtered_list, *aux_list, *l;
  GstElementFactory *decoder_factory = NULL;
  GstElement *decoder = NULL;
  gboolean contains_openh264 = FALSE;

  decoder_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_DECODER,
      GST_RANK_NONE);

  /* HACK: Augment the openh264 rank */
  for (l = decoder_list; l != NULL; l = l->next) {
    decoder_factory = GST_ELEMENT_FACTORY (l->data);

    if (g_str_has_prefix (GST_OBJECT_NAME (decoder_factory), "openh264")) {
      decoder_list = g_list_remove (decoder_list, l->data);
      decoder_list = g_list_prepend (decoder_list, decoder_factory);
      contains_openh264 = TRUE;
      break;
    }
  }
  decoder_factory = NULL;

  /* Remove stream-format from raw_caps to allow select openh264dec */
  if (contains_openh264 &&
      g_str_has_suffix (gst_structure_get_name (gst_caps_get_structure (caps,
                  0)), "h264")) {
    GstCaps *caps_copy;
    GstStructure *structure;

    structure = gst_structure_copy (gst_caps_get_structure (caps, 0));
    gst_structure_remove_field (structure, "stream-format");
    caps_copy = gst_caps_new_full (structure, NULL);
    aux_list =
        gst_element_factory_list_filter (decoder_list, caps_copy, GST_PAD_SINK,
        FALSE);
    gst_caps_unref (caps_copy);
  } else {
    aux_list =
        gst_element_factory_list_filter (decoder_list, caps, GST_PAD_SINK,
        FALSE);
  }

  filtered_list =
      gst_element_factory_list_filter (aux_list, raw_caps, GST_PAD_SRC, FALSE);

  for (l = filtered_list; l != NULL && decoder_factory == NULL; l = l->next) {
    decoder_factory = GST_ELEMENT_FACTORY (l->data);
    if (gst_element_factory_get_num_pad_templates (decoder_factory) != 2) {
      decoder_factory = NULL;
    }

    // Verify that the factory can actually consume all input caps.
    // Introduced for GStreamer 1.20.
    // Avoid selecting vp8alphadecodebin with VP8 streams that don't contain an
    // alpha channel. For some reason, the alpha decoding factories pass the
    // filtering done in gst_element_factory_list_filter() for an input that
    // doesn't contain alpha channel (and later fails decoding for that reason)
    if (!gst_element_factory_can_sink_all_caps (decoder_factory, caps)) {
      decoder_factory = NULL;
    }
  }

  if (decoder_factory != NULL) {
    decoder = gst_element_factory_create (decoder_factory, NULL);
  }

  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (decoder_list);
  gst_plugin_feature_list_free (aux_list);

  return decoder;
}

static gboolean
kms_dec_tree_bin_configure (KmsDecTreeBin * self, const GstCaps * caps,
    const GstCaps * raw_caps)
{
  KmsTreeBin *tree_bin = KMS_TREE_BIN (self);
  GstElement *dec, *output_tee;
  GstPad *pad;
  gchar *name;

  GST_DEBUG_OBJECT (self,
      "Create decoder for caps: %" GST_PTR_FORMAT
      ", raw_caps: %" GST_PTR_FORMAT,
      caps, raw_caps);

  dec = create_decoder_for_caps (caps, raw_caps);
  if (dec == NULL) {
    GST_WARNING_OBJECT (self, "Invalid decoder for caps %" GST_PTR_FORMAT,
        caps);
    return FALSE;
  }
  GST_DEBUG_OBJECT (self, "Decoder found: %" GST_PTR_FORMAT, dec);
  name = gst_element_get_name (dec);

  if (g_str_has_prefix (name, "opusdec")) {
    g_object_set (dec, "plc", TRUE, "use-inband-fec", TRUE, NULL);
  }

  if (g_str_has_prefix (name, "openh264dec")) {
    GstElement *parse = gst_element_factory_make ("h264parse", NULL);

    pad = gst_element_get_static_pad (parse, "sink");
    kms_utils_drop_until_keyframe (pad, TRUE);
    gst_object_unref (pad);

    gst_bin_add_many (GST_BIN (self), parse, dec, NULL);
    gst_element_link (parse, dec);
    gst_element_sync_state_with_parent (parse);
    gst_element_sync_state_with_parent (dec);

    kms_tree_bin_set_input_element (tree_bin, parse);
    output_tee = kms_tree_bin_get_output_tee (tree_bin);
    gst_element_link (dec, output_tee);
  } else {
    pad = gst_element_get_static_pad (dec, "sink");
    kms_utils_drop_until_keyframe (pad, TRUE);
    gst_object_unref (pad);

    gst_bin_add (GST_BIN (self), dec);
    gst_element_sync_state_with_parent (dec);

    kms_tree_bin_set_input_element (tree_bin, dec);
    output_tee = kms_tree_bin_get_output_tee (tree_bin);
    gst_element_link (dec, output_tee);
  }

  g_free (name);

  return TRUE;
}

KmsDecTreeBin *
kms_dec_tree_bin_new (const GstCaps * caps, const GstCaps * raw_caps)
{
  GObject *dec;

  dec = g_object_new (KMS_TYPE_DEC_TREE_BIN, NULL);
  if (!kms_dec_tree_bin_configure (KMS_DEC_TREE_BIN (dec), caps, raw_caps)) {
    g_object_unref (dec);
    return NULL;
  }

  return KMS_DEC_TREE_BIN (dec);
}

static void
kms_dec_tree_bin_init (KmsDecTreeBin * self)
{
  /* Nothing to do */
}

static void
kms_dec_tree_bin_class_init (KmsDecTreeBinClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_details_simple (gstelement_class,
      "DecTreeBin",
      "Generic",
      "Bin to decode and distribute RAW media.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
