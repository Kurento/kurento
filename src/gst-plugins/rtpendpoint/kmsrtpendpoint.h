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
#ifndef __KMS_RTP_ENDPOINT_H__
#define __KMS_RTP_ENDPOINT_H__

#include <gio/gio.h>
#include <gst/gst.h>
#include <commons/kmsbasertpendpoint.h>

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

  /* signals */
  void (*key_soft_limit) (KmsRtpEndpoint *obj, gchar *media);
};

GType kms_rtp_endpoint_get_type (void);

gboolean kms_rtp_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_RTP_ENDPOINT_H__ */
