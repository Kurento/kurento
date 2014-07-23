/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#ifndef _KMS_RTCP_DEMUX_H_
#define _KMS_RTCP_DEMUX_H_

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_RTCP_DEMUX   (kms_rtcp_demux_get_type())
#define KMS_RTCP_DEMUX(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTCP_DEMUX,KmsRtcpDemux))
#define KMS_RTCP_DEMUX_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTCP_DEMUX,KmsRtcpDemuxClass))
#define KMS_IS_RTCP_DEMUX(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTCP_DEMUX))
#define KMS_IS_RTCP_DEMUX_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTCP_DEMUX))

typedef struct _KmsRtcpDemux KmsRtcpDemux;
typedef struct _KmsRtcpDemuxClass KmsRtcpDemuxClass;
typedef struct _KmsRtcpDemuxPrivate KmsRtcpDemuxPrivate;

struct _KmsRtcpDemux
{
  GstElement element;
  KmsRtcpDemuxPrivate *priv;
};

struct _KmsRtcpDemuxClass
{
  GstElementClass element_class;

  /* private */
  /* actions */
  guint32 (*get_local_rr_ssrc_pair) (KmsRtcpDemux * self, guint32 remote_ssrc);
};

GType kms_rtcp_demux_get_type (void);

gboolean kms_rtcp_demux_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_RTCP_DEMUX_H_ */
