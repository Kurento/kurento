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
