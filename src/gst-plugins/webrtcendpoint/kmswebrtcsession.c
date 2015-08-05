/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#include "kmswebrtcsession.h"

#define GST_DEFAULT_NAME "kmswebrtcsession"
#define GST_CAT_DEFAULT kms_webrtc_session_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_session_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcSession, kms_webrtc_session, KMS_TYPE_BASE_RTP_SESSION);

#define BUNDLE_CONN_ADDED "bundle-conn-added"
#define RTCP_DEMUX_PEER "rtcp-demux-peer"

KmsWebrtcSession *
kms_webrtc_session_new (KmsBaseSdpEndpoint * ep, guint id,
    KmsIRtpSessionManager * manager, GMainContext * context)
{
  GObject *obj;
  KmsWebrtcSession *self;

  obj = g_object_new (KMS_TYPE_WEBRTC_SESSION, NULL);
  self = KMS_WEBRTC_SESSION (obj);
  KMS_WEBRTC_SESSION_CLASS (G_OBJECT_GET_CLASS (self))->post_constructor
      (self, ep, id, manager, context);

  return self;
}

static void
kms_webrtc_session_finalize (GObject * object)
{
  KmsWebrtcSession *self = KMS_WEBRTC_SESSION (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_clear_object (&self->agent);
  g_slist_free_full (self->remote_candidates, g_object_unref);

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_session_parent_class)->finalize (object);
}

static void
kms_webrtc_session_post_constructor (KmsWebrtcSession * self,
    KmsBaseSdpEndpoint * ep, guint id, KmsIRtpSessionManager * manager,
    GMainContext * context)
{
  KmsBaseRtpSession *base_rtp_session = KMS_BASE_RTP_SESSION (self);

  self->agent = nice_agent_new (context, NICE_COMPATIBILITY_RFC5245);

  KMS_BASE_RTP_SESSION_CLASS
      (kms_webrtc_session_parent_class)->post_constructor (base_rtp_session, ep,
      id, manager);
}

static void
kms_webrtc_session_init (KmsWebrtcSession * self)
{
  /* nothing to do */
}

static void
kms_webrtc_session_class_init (KmsWebrtcSessionClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gobject_class->finalize = kms_webrtc_session_finalize;

  klass->post_constructor = kms_webrtc_session_post_constructor;

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcSession",
      "Generic",
      "Base bin to manage elements related with a WebRTC session.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}
