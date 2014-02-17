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


#ifndef __GST_SCTP_SERVER_SRC_H__
#define __GST_SCTP_SERVER_SRC_H__

#include <gst/base/gstpushsrc.h>

G_END_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_SCTP_SERVER_SRC \
  (gst_sctp_server_src_get_type())
#define GST_SCTP_SERVER_SRC(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_SCTP_SERVER_SRC,GstSCTPServerSrc))
#define GST_SCTP_SERVER_SRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_SCTP_SERVER_SRC,GstSCTPServerSrcClass))
#define GST_IS_SCTP_SERVER_SRC(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_SCTP_SERVER_SRC))
#define GST_IS_SCTP_SERVER_SRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_SCTP_SERVER_SRC))
#define GST_SCTP_SERVER_SRC_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                \
    (obj),                                   \
    GST_TYPE_SCTP_SERVER_SRC,                \
    GstSCTPServerSrcClass                    \
  )                                          \
)

typedef struct _GstSCTPServerSrc GstSCTPServerSrc;
typedef struct _GstSCTPServerSrcClass GstSCTPServerSrcClass;
typedef struct _GstSCTPServerSrcPrivate GstSCTPServerSrcPrivate;

struct _GstSCTPServerSrc {
  GstPushSrc element;

  /*< private > */
  GstSCTPServerSrcPrivate *priv;
};

struct _GstSCTPServerSrcClass {
  GstPushSrcClass parent_class;
};

GType gst_sctp_server_src_get_type (void);
gboolean gst_sctp_server_src_plugin_init (GstPlugin * plugin);

G_BEGIN_DECLS

#endif /* __GST_SCTP_SERVER_SRC_H__ */