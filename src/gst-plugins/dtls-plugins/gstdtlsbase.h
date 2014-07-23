/*
 * GStreamer
 *
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */


#ifndef __GST_DTLS_BASE_H__
#define __GST_DTLS_BASE_H__

#include <gst/gst.h>

G_BEGIN_DECLS
#define GST_TYPE_DTLS_BASE            (gst_dtls_base_get_type())
#define GST_DTLS_BASE(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_DTLS_BASE,GstDtlsBase))
#define GST_IS_DTLS_BASE(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_DTLS_BASE))
#define GST_DTLS_BASE_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass) ,GST_TYPE_DTLS_BASE,GstDtlsBaseClass))
#define GST_IS_DTLS_BASE_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass) ,GST_TYPE_DTLS_BASE))
#define GST_DTLS_BASE_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj) ,GST_TYPE_DTLS_BASE,GstDtlsBaseClass))

/**
 * GstDtlsBase:
 *
 * The adder object structure.
 */
struct _GstDtlsBase
{
  /*< private >*/
  GstElement element;

  GstPad *srcpad;
  GstPad *sinkpad;

  gchar *channel_id;
  gboolean is_client;
  gchar *certificate_pem_file;
  GTlsCertificateFlags client_validation_flags;

  GstDtlsConnection *conn;
};

struct _GstDtlsBaseClass
{
  GstElementClass parent_class;

  GstFlowReturn (*chain) (GstDtlsBase * base, GstBuffer * buffer);
};

GType gst_dtls_base_get_type (void);

G_END_DECLS
#endif /* __GST_DTLS_BASE_H__ */
