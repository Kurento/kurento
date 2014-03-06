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
#ifndef __KMS_RTP_ENDPOINT_H__
#define __KMS_RTP_ENDPOINT_H__

#include <gio/gio.h>
#include <gst/gst.h>
#include <kmsbasertpendpoint.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_RTP_ENDPOINT \
  (kms_rtp_endpoint_get_type())
#define KMS_RTP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTP_ENDPOINT,KmsRtpEndpoint))
#define KMS_RTP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTP_ENDPOINT,KmsRtpEndpointClass))
#define KMS_IS_RTP_ENDPOINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTP_ENDPOINT))
#define KMS_IS_RTP_ENDPOINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTP_ENDPOINT))
#define KMS_RTP_ENDPOINT_CAST(obj) ((KmsRtpEndpoint*)(obj))
typedef struct _KmsRtpEndpoint KmsRtpEndpoint;
typedef struct _KmsRtpEndpointClass KmsRtpEndpointClass;
typedef struct _KmsRtpEndpointPrivate KmsRtpEndpointPrivate;

#define KMS_RTP_ENDPOINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_RTP_ENDPOINT_CAST ((elem))->media_mutex))
#define KMS_RTP_ENDPOINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_RTP_ENDPOINT_CAST ((elem))->media_mutex))

struct _KmsRtpEndpoint
{
  KmsBaseRtpEndpoint parent;

  KmsRtpEndpointPrivate *priv;
};

struct _KmsRtpEndpointClass
{
  KmsBaseRtpEndpointClass parent_class;
};

GType kms_rtp_endpoint_get_type (void);

gboolean kms_rtp_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_RTP_ENDPOINT_H__ */
