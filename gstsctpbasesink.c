/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include "gstsctpbasesink.h"

#define PLUGIN_NAME "sctpbasesink"

GST_DEBUG_CATEGORY_STATIC (gst_sctp_base_sink_debug_category);
#define GST_CAT_DEFAULT gst_sctp_base_sink_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPBaseSink, gst_sctp_base_sink,
    GST_TYPE_BASE_SINK,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_base_sink_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define GST_SCTP_BASE_SINK_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_BASE_SINK, GstSCTPBaseSinkPrivate))

struct _GstSCTPBaseSinkPrivate
{
  guint streamid;
};

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void
gst_sctp_base_sink_class_init (GstSCTPBaseSinkClass * klass)
{
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP base sink", "Sink/Network",
      "Provides data associated to a stream id to be sent over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));

  g_type_class_add_private (klass, sizeof (GstSCTPBaseSinkPrivate));
}

static void
gst_sctp_base_sink_init (GstSCTPBaseSink * self)
{
  self->priv = GST_SCTP_BASE_SINK_GET_PRIVATE (self);
  self->priv->streamid = 0;
}

gboolean
gst_sctp_base_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_BASE_SINK);
}
