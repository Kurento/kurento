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

#ifndef __KMS_WEBRTC_CONNECTION_H__
#define __KMS_WEBRTC_CONNECTION_H__

#include <nice/nice.h>
#include <gst/gst.h>
#include <commons/kmsirtpconnection.h>

G_BEGIN_DECLS
#define SDP_ICE_UFRAG_ATTR "ice-ufrag"
#define SDP_ICE_PWD_ATTR "ice-pwd"
#define SDP_CANDIDATE_ATTR "candidate"
#define SDP_CANDIDATE_ATTR_LEN 12

/* KmsWebRtcBaseConnection begin */
#define KMS_TYPE_WEBRTC_BASE_CONNECTION \
  (kms_webrtc_base_connection_get_type())
#define KMS_WEBRTC_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_BASE_CONNECTION,KmsWebRtcBaseConnection))
#define KMS_WEBRTC_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_BASE_CONNECTION,KmsWebRtcBaseConnectionClass))
#define KMS_IS_WEBRTC_BASE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_BASE_CONNECTION))
#define KMS_IS_WEBRTC_BASE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_BASE_CONNECTION))
#define KMS_WEBRTC_BASE_CONNECTION_CAST(obj) ((KmsWebRtcBaseConnection*)(obj))

typedef struct _KmsWebRtcBaseConnection KmsWebRtcBaseConnection;
typedef struct _KmsWebRtcBaseConnectionClass KmsWebRtcBaseConnectionClass;

struct _KmsWebRtcBaseConnection
{
  GObject parent;

  NiceAgent *agent;
  gboolean ice_gathering_done;
  guint stream_id;
  gchar *name;
};

struct _KmsWebRtcBaseConnectionClass
{
  GObjectClass parent_class;

  void (*set_certificate_pem_file) (KmsWebRtcBaseConnection * self,
      const gchar * pem);
};

GType kms_webrtc_base_connection_get_type (void);

void
kms_webrtc_base_connection_set_certificate_pem_file (KmsWebRtcBaseConnection *
    self, const gchar * pem);
void kms_webrtc_base_connection_set_relay_info (KmsWebRtcBaseConnection * self,
    const gchar * server_ip, guint server_port, const gchar * username,
    const gchar * password, NiceRelayType type);

/* KmsWebRtcBaseConnection end */

/* KmsWebRtcConnection begin */
#define KMS_TYPE_WEBRTC_CONNECTION \
  (kms_webrtc_connection_get_type())
#define KMS_WEBRTC_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_CONNECTION,KmsWebRtcConnection))
#define KMS_WEBRTC_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_CONNECTION,KmsWebRtcConnectionClass))
#define KMS_IS_WEBRTC_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_CONNECTION))
#define KMS_IS_WEBRTC_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_CONNECTION))
#define KMS_WEBRTC_CONNECTION_CAST(obj) ((KmsWebRtcConnection*)(obj))

typedef struct _KmsWebRtcConnectionPrivate KmsWebRtcConnectionPrivate;
typedef struct _KmsWebRtcConnection KmsWebRtcConnection;
typedef struct _KmsWebRtcConnectionClass KmsWebRtcConnectionClass;

struct _KmsWebRtcConnection
{
  KmsWebRtcBaseConnection parent;

  KmsWebRtcConnectionPrivate *priv;
};

struct _KmsWebRtcConnectionClass
{
  KmsWebRtcBaseConnectionClass parent_class;
};

GType kms_webrtc_connection_get_type (void);

KmsWebRtcConnection *kms_webrtc_connection_new (NiceAgent * agent,
    GMainContext * context, const gchar * name);

/* KmsWebRtcConnection end */

/* KmsWebRtcBundleConnection begin */
#define KMS_TYPE_WEBRTC_BUNDLE_CONNECTION \
  (kms_webrtc_bundle_connection_get_type())
#define KMS_WEBRTC_BUNDLE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_WEBRTC_BUNDLE_CONNECTION,KmsWebRtcBundleConnection))
#define KMS_WEBRTC_BUNDLE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_WEBRTC_BUNDLE_CONNECTION,KmsWebRtcBundleConnectionClass))
#define KMS_IS_WEBRTC_BUNDLE_CONNECTION(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_WEBRTC_BUNDLE_CONNECTION))
#define KMS_IS_WEBRTC_BUNDLE_CONNECTION_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_BUNDLE_CONNECTION))
#define KMS_WEBRTC_BUNDLE_CONNECTION_CAST(obj) ((KmsWebRtcBundleConnection*)(obj))

typedef struct _KmsWebRtcBundleConnectionPrivate
    KmsWebRtcBundleConnectionPrivate;
typedef struct _KmsWebRtcBundleConnection KmsWebRtcBundleConnection;
typedef struct _KmsWebRtcBundleConnectionClass KmsWebRtcBundleConnectionClass;

struct _KmsWebRtcBundleConnection
{
  KmsWebRtcBaseConnection parent;

  KmsWebRtcBundleConnectionPrivate *priv;
};

struct _KmsWebRtcBundleConnectionClass
{
  KmsWebRtcBaseConnectionClass parent_class;
};

GType kms_webrtc_bundle_connection_get_type (void);

KmsWebRtcBundleConnection *kms_webrtc_bundle_connection_new (NiceAgent * agent,
    GMainContext * context, const gchar * name);

/* KmsWebRtcBundleConnection end */

G_END_DECLS
#endif /* __KMS_WEBRTC_CONNECTION_H__ */
