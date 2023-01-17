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

#include "kmsparsetreebin.h"
#include "kmsutils.h"

#define GST_DEFAULT_NAME "parsetreebin"
#define GST_CAT_DEFAULT kms_parse_tree_bin_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_parse_tree_bin_parent_class parent_class
G_DEFINE_TYPE (KmsParseTreeBin, kms_parse_tree_bin, KMS_TYPE_TREE_BIN);

#define KMS_PARSE_TREE_BIN_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PARSE_TREE_BIN,                    \
    KmsParseTreeBinPrivate                      \
  )                                             \
)

#define BITRATE_THRESHOLD 0.07

struct _KmsParseTreeBinPrivate
{
  GstElement *parser;

  /* Bitrate calculation */
  GstClockTime last_buffer_pts;
  GstClockTime last_buffer_dts;
  guint bitrate_mean;
  guint last_pushed_bitrate;
};

static GstElement *
create_parser_for_caps (const GstCaps * caps)
{
  GList *parser_list, *filtered_list, *l;
  GstElementFactory *parser_factory = NULL;
  GstElement *parser = NULL;

  parser_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_PARSER,
      GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (parser_list, caps, GST_PAD_SINK, FALSE);

  for (l = filtered_list; l != NULL && parser_factory == NULL; l = l->next) {
    parser_factory = GST_ELEMENT_FACTORY (l->data);
    if (gst_element_factory_get_num_pad_templates (parser_factory) != 2)
      parser_factory = NULL;
  }

  if ((parser_factory != NULL) &&
    (g_strcmp0 (gst_plugin_feature_get_name(parser_factory),
      "rawaudioparse") != 0) &&
    (g_strcmp0 (gst_plugin_feature_get_name(parser_factory),
      "rawvideoparse") != 0)) {
    parser = gst_element_factory_create (parser_factory, NULL);
  } else {
    parser = kms_utils_element_factory_make ("capsfilter", "parsetreebin");
  }

  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (parser_list);

  return parser;
}

static gboolean
difference_over_threshold (guint a, guint b, float th)
{
  return (a > b ? (a - b) > (a * th) : (b - a) > (b * th));
}

static GstPadProbeReturn
bitrate_calculation_probe (GstPad * pad, GstPadProbeInfo * info,
    KmsParseTreeBin * self)
{
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BUFFER) {
    GstBuffer *buffer = gst_pad_probe_info_get_buffer (info);
    GstClockTime timediff = GST_CLOCK_TIME_NONE;
    guint bitrate;

    if (GST_CLOCK_TIME_IS_VALID (buffer->dts)
        && GST_CLOCK_TIME_IS_VALID (self->priv->last_buffer_dts)) {
      timediff = buffer->dts - self->priv->last_buffer_dts;
    } else if (GST_CLOCK_TIME_IS_VALID (buffer->pts)
        && GST_CLOCK_TIME_IS_VALID (self->priv->last_buffer_pts)) {
      timediff = buffer->pts - self->priv->last_buffer_pts;
    }

    if (timediff > 0) {
      bitrate = (gst_buffer_get_size (buffer) * GST_SECOND * 8) / timediff;

      self->priv->bitrate_mean = (self->priv->bitrate_mean * 7 + bitrate) / 8;

      if (self->priv->last_pushed_bitrate == 0
          || difference_over_threshold (self->priv->bitrate_mean,
              self->priv->last_pushed_bitrate, BITRATE_THRESHOLD)) {
        GstTagList *taglist = NULL;
        GstEvent *previous_tag_event;

        GST_TRACE_OBJECT (self, "Bitrate: %u", bitrate);
        GST_TRACE_OBJECT (self, "Bitrate_mean:\t\t%u",
            self->priv->bitrate_mean);

        previous_tag_event = gst_pad_get_sticky_event (pad, GST_EVENT_TAG, 0);

        if (previous_tag_event) {
          GST_TRACE_OBJECT (self, "Previous tag event: %" GST_PTR_FORMAT,
              previous_tag_event);
          gst_event_parse_tag (previous_tag_event, &taglist);

          taglist = gst_tag_list_copy (taglist);
          gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, "bitrate",
              self->priv->bitrate_mean, NULL);

          gst_event_unref (previous_tag_event);
        }

        if (!taglist) {
          taglist =
              gst_tag_list_new ("bitrate", self->priv->bitrate_mean, NULL);
        }

        gst_pad_send_event (pad, gst_event_new_tag (taglist));
        self->priv->last_pushed_bitrate = self->priv->bitrate_mean;
      }
    }

    self->priv->last_buffer_pts = buffer->pts;
    self->priv->last_buffer_dts = buffer->dts;
  }
  else if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BUFFER_LIST) {
    GST_WARNING_OBJECT (self,
        "Bufferlist is not supported yet for bitrate calculation");
  }

  return GST_PAD_PROBE_OK;
}

static void
kms_parse_tree_bin_configure (KmsParseTreeBin * self, const GstCaps * caps)
{
  KmsTreeBin *tree_bin = KMS_TREE_BIN (self);
  GstElement *output_tee;

  self->priv->parser = create_parser_for_caps (caps);

  gst_bin_add (GST_BIN (self), self->priv->parser);
  gst_element_sync_state_with_parent (self->priv->parser);

  kms_tree_bin_set_input_element (tree_bin, self->priv->parser);
  output_tee = kms_tree_bin_get_output_tee (tree_bin);
  if (!kms_utils_caps_is_raw (caps) && kms_utils_caps_is_video (caps)) {
    GstPad *sink = gst_element_get_static_pad (output_tee, "sink");

    gst_pad_add_probe (sink,
        GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,
        (GstPadProbeCallback) bitrate_calculation_probe, self, NULL);

    g_object_unref (sink);
  }
  gst_element_link_many (self->priv->parser, output_tee, NULL);
}

KmsParseTreeBin *
kms_parse_tree_bin_new (const GstCaps * caps)
{
  GObject *parse;

  parse = g_object_new (KMS_TYPE_PARSE_TREE_BIN, NULL);
  kms_parse_tree_bin_configure (KMS_PARSE_TREE_BIN (parse), caps);

  return KMS_PARSE_TREE_BIN (parse);
}

GstElement *
kms_parse_tree_bin_get_parser (KmsParseTreeBin * self)
{
  return self->priv->parser;
}

static void
kms_parse_tree_bin_init (KmsParseTreeBin * self)
{
  self->priv = KMS_PARSE_TREE_BIN_GET_PRIVATE (self);

  self->priv->last_buffer_dts = GST_CLOCK_TIME_NONE;
  self->priv->last_buffer_pts = GST_CLOCK_TIME_NONE;
}

static void
kms_parse_tree_bin_class_init (KmsParseTreeBinClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_details_simple (gstelement_class,
      "ParseTreeBin",
      "Generic",
      "Bin to parse and distribute media.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  g_type_class_add_private (klass, sizeof (KmsParseTreeBinPrivate));
}
