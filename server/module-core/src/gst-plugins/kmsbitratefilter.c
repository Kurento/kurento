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
#  include <config.h>
#endif

#include "kmsbitratefilter.h"
#include "commons/kmsutils.h"

#define PLUGIN_NAME "bitratefilter"

#define GST_CAT_DEFAULT kms_bitrate_filter_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_bitrate_filter_parent_class parent_class
G_DEFINE_TYPE (KmsBitrateFilter, kms_bitrate_filter, GST_TYPE_BASE_TRANSFORM);

#define KMS_BITRATE_FILTER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_BITRATE_FILTER,                  \
    KmsBitrateFilterPrivate                   \
  )                                           \
)

#define BITRATE_CALC_INTERVAL GST_SECOND
#define BITRATE_CALC_THRESHOLD 100000   /* bps */

typedef struct _KmsBitrateCalcData
{
  GQueue /*GstClockTime */  * pts_queue;
  GQueue /*gsize */  * sizes_queue;
  guint64 total_size;
  gint bitrate, last_bitrate;   /* bps */
} KmsBitrateCalcData;

struct _KmsBitrateFilterPrivate
{
  KmsBitrateCalcData bitrate_calc_data;
};

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void
kms_bitrate_calc_data_clear (KmsBitrateCalcData * data)
{
  if (data == NULL) {
    return;
  }

  g_queue_free_full (data->pts_queue,
      (GDestroyNotify) kms_utils_destroy_guint64);
  g_queue_free (data->sizes_queue);
}

static void
kms_bitrate_calc_data_init (KmsBitrateCalcData * data)
{
  data->pts_queue = g_queue_new ();
  data->sizes_queue = g_queue_new ();
}

static void
kms_bitrate_calc_data_update (KmsBitrateCalcData * data, GstBuffer * buffer)
{
  gsize size;
  guint64 *current_pts, *last_pts, diff;

  current_pts = g_slice_new0 (guint64);
  *current_pts = buffer->pts;
  g_queue_push_head (data->pts_queue, current_pts);

  size = gst_buffer_get_size (buffer);
  g_queue_push_head (data->sizes_queue, GSIZE_TO_POINTER (size));
  data->total_size += size;

  /* Remove old buffers */
  last_pts = (guint64 *) g_queue_peek_tail (data->pts_queue);
  diff = *current_pts - *last_pts;
  while (diff > BITRATE_CALC_INTERVAL) {
    gpointer p;

    p = g_queue_pop_tail (data->pts_queue);
    kms_utils_destroy_guint64 (p);

    p = g_queue_pop_tail (data->sizes_queue);
    data->total_size -= GPOINTER_TO_SIZE (p);

    last_pts = (guint64 *) g_queue_peek_tail (data->pts_queue);
    diff = *current_pts - *last_pts;
  }

  if (diff == 0) {
    data->bitrate = 0;
  } else {
    data->bitrate = (8 * GST_SECOND * data->total_size) / diff;
  }
}

static GstFlowReturn
kms_bitrate_filter_transform_ip (GstBaseTransform * base, GstBuffer * buf)
{
  /* No actual work here. It's all done in the prepare output buffer func */
  return GST_FLOW_OK;
}

static void
kms_bitrate_filter_update_src_caps (KmsBitrateFilter * self)
{
  GstBaseTransform *trans = GST_BASE_TRANSFORM (self);
  KmsBitrateCalcData *data = &self->priv->bitrate_calc_data;
  GstCaps *caps;

  if (ABS (data->bitrate - data->last_bitrate) < BITRATE_CALC_THRESHOLD) {
    return;
  }

  caps = gst_pad_get_current_caps (trans->srcpad);
  if (caps == NULL) {
    return;
  }

  data->last_bitrate = data->bitrate;

  GST_DEBUG_OBJECT (trans, "Old caps: %" GST_PTR_FORMAT, caps);

  caps = gst_caps_make_writable (caps);
  gst_caps_set_simple (caps, "bitrate", G_TYPE_INT, data->bitrate, NULL);
  gst_pad_set_caps (trans->srcpad, caps);

  GST_DEBUG_OBJECT (trans, "New caps: %" GST_PTR_FORMAT, caps);

  gst_caps_unref (caps);
}

static GstFlowReturn
kms_bitrate_filter_prepare_buf (GstBaseTransform * trans, GstBuffer * input,
    GstBuffer ** buf)
{
  KmsBitrateFilter *self = KMS_BITRATE_FILTER (trans);
  KmsBitrateCalcData *data = &self->priv->bitrate_calc_data;

  /* always return the input as output buffer */
  *buf = input;
  kms_bitrate_calc_data_update (data, input);
  kms_bitrate_filter_update_src_caps (self);

  GST_TRACE_OBJECT (self, "bitrate: %" G_GINT32_FORMAT " bps", data->bitrate);

  return GST_FLOW_OK;
}

static void
kms_bitrate_filter_dispose (GObject * object)
{
  KmsBitrateFilter *self = KMS_BITRATE_FILTER (object);

  kms_bitrate_calc_data_clear (&self->priv->bitrate_calc_data);

  /* chain up */
  G_OBJECT_CLASS (kms_bitrate_filter_parent_class)->dispose (object);
}

static GstCaps *
kms_bitrate_filter_transform_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps, GstCaps * filter)
{
  KmsBitrateFilter *self = KMS_BITRATE_FILTER (base);
  GstCaps *ret;
  guint caps_size, i;

  ret = gst_caps_make_writable (gst_caps_ref (caps));
  caps_size = gst_caps_get_size (ret);

  for (i = 0; i < caps_size; i++) {
    GstStructure *s = gst_caps_get_structure (ret, i);

    if (GST_PAD_SRC == direction) {
      gst_structure_remove_field (s, "bitrate");
    } else if (GST_PAD_SINK == direction) {
      gst_structure_set (s, "bitrate", GST_TYPE_INT_RANGE, 0, G_MAXINT32, NULL);
    }
  }

  GST_DEBUG_OBJECT (self, "Input caps: %" GST_PTR_FORMAT, caps);
  GST_DEBUG_OBJECT (self, "Filter: %" GST_PTR_FORMAT, filter);
  GST_DEBUG_OBJECT (self, "Old caps: %" GST_PTR_FORMAT, caps);
  GST_DEBUG_OBJECT (self, "Output caps: %" GST_PTR_FORMAT, ret);

  return ret;
}

static void
kms_bitrate_filter_init (KmsBitrateFilter * self)
{
  self->priv = KMS_BITRATE_FILTER_GET_PRIVATE (self);
  kms_bitrate_calc_data_init (&self->priv->bitrate_calc_data);
}

static void
kms_bitrate_filter_class_init (KmsBitrateFilterClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GstBaseTransformClass *trans_class = GST_BASE_TRANSFORM_CLASS (klass);

  gobject_class->dispose = kms_bitrate_filter_dispose;

  gst_element_class_set_details_simple (gstelement_class,
      "BitrateFilter",
      "Generic",
      "Pass data without modification, managing bitrate caps.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));

  trans_class->transform_ip =
      GST_DEBUG_FUNCPTR (kms_bitrate_filter_transform_ip);
  trans_class->prepare_output_buffer =
      GST_DEBUG_FUNCPTR (kms_bitrate_filter_prepare_buf);
  trans_class->transform_caps =
      GST_DEBUG_FUNCPTR (kms_bitrate_filter_transform_caps);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  g_type_class_add_private (klass, sizeof (KmsBitrateFilterPrivate));
}

gboolean
kms_bitrate_filter_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_BITRATE_FILTER);
}
