/* GStreamer
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#ifndef __GST_DTLS_SRTP_DEMUX_H__
#define __GST_DTLS_SRTP_DEMUX_H__

#include <gst/gst.h>

#define GST_TYPE_DTLS_SRTP_DEMUX            (gst_dtls_srtp_demux_get_type())
#define GST_DTLS_SRTP_DEMUX(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_DTLS_SRTP_DEMUX,GstDtlsSrtpDemux))
#define GST_DTLS_SRTP_DEMUX_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_DTLS_SRTP_DEMUX,GstDtlsSrtpDemuxClass))
#define GST_IS_DTLS_SRTP_DEMUX(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_DTLS_SRTP_DEMUX))
#define GST_IS_DTLS_SRTP_DEMUX_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_DTLS_SRTP_DEMUX))

typedef struct _GstDtlsSrtpDemux GstDtlsSrtpDemux;
typedef struct _GstDtlsSrtpDemuxClass GstDtlsSrtpDemuxClass;

struct _GstDtlsSrtpDemux
{
  GstElement parent;

  GstPad *sinkpad;

  GstPad *srtp_srcpad;
  GstPad *dtls_srcpad;
};

struct _GstDtlsSrtpDemuxClass
{
  GstElementClass parent_class;
};

GType gst_dtls_srtp_demux_get_type (void);

#endif /* __GST_DTLS_SRTP_DEMUX_H__ */
