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

#include "gstsctpclientsink.h"

#define PLUGIN_NAME "sctpclientsink"

GST_DEBUG_CATEGORY_STATIC (gst_sctp_client_sink_debug_category);
#define GST_CAT_DEFAULT gst_sctp_client_sink_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPClientSink, gst_sctp_client_sink,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_client_sink_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define GST_SCTP_CLIENT_SINK_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_CLIENT_SINK, GstSCTPClientSinkPrivate))

struct _GstSCTPClientSinkPrivate
{
  gint sockfd;
};

static void
gst_sctp_client_sink_class_init (GstSCTPClientSinkClass * klass)
{
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP client sink", "Sink/Network",
      "Send data as a client over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  g_type_class_add_private (klass, sizeof (GstSCTPClientSinkPrivate));
}

static void
gst_sctp_client_sink_init (GstSCTPClientSink * self)
{
  self->priv = GST_SCTP_CLIENT_SINK_GET_PRIVATE (self);
  self->priv->sockfd = -1;

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}
