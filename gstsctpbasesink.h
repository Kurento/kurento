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
#ifndef __GST_SCTP_BASE_SINK_H__
#define __GST_SCTP_BASE_SINK_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_SCTP_BASE_SINK \
  (gst_sctp_base_sink_get_type())
#define GST_SCTP_BASE_SINK(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_SCTP_BASE_SINK,GstSCTPBaseSink))
#define GST_SCTP_BASE_SINK_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_SCTP_BASE_SINK,GstSCTPBaseSinkClass))
#define GST_IS_SCTP_BASE_SINK(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_SCTP_BASE_SINK))
#define GST_IS_SCTP_BASE_SINK_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_SCTP_BASE_SINK))
#define GST_SCTP_BASE_SINK_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (               \
    (obj),                                  \
    GST_TYPE_SCTP_BASE_SINK,                \
    GstSCTPBaseSinkClass                    \
  )                                         \
)

typedef struct _GstSCTPBaseSink GstSCTPBaseSink;
typedef struct _GstSCTPBaseSinkClass GstSCTPBaseSinkClass;
typedef struct _GstSCTPBaseSinkPrivate GstSCTPBaseSinkPrivate;

struct _GstSCTPBaseSink
{
  GstBin parent;

  /*< private > */
  GstSCTPBaseSinkPrivate *priv;
};

struct _GstSCTPBaseSinkClass
{
  GstBinClass parent_class;
};

GType gst_sctp_base_sink_get_type (void);
gboolean gst_sctp_base_sink_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __GST_SCTP_BASE_SINK_H__ */