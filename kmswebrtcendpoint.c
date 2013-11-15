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
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmswebrtcendpoint.h"
#include <nice/nice.h>

#define PLUGIN_NAME "webrtcendpoint"

#define GST_CAT_DEFAULT kms_webrtc_end_point_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_end_point_parent_class parent_class
G_DEFINE_TYPE (KmsWebrtcEndPoint, kms_webrtc_end_point,
    KMS_TYPE_BASE_RTP_END_POINT);

#define KMS_WEBRTC_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_WEBRTC_END_POINT,                  \
    KmsWebrtcEndPointPrivate                    \
  )                                             \
)

struct _KmsWebrtcEndPointPrivate
{
  GMainContext *context;
  GMainLoop *loop;
  GThread *thread;

  NiceAgent *agent;
};

static gboolean
kms_webrtc_end_point_set_transport_to_sdp (KmsBaseSdpEndPoint *
    base_sdp_endpoint, GstSDPMessage * msg)
{
  GST_WARNING ("TODO: complete");

  return TRUE;
}

static void
kms_webrtc_end_point_start_transport_send (KmsBaseSdpEndPoint *
    base_rtp_end_point, const GstSDPMessage * offer,
    const GstSDPMessage * answer, gboolean local_offer)
{
  GST_WARNING ("TODO: complete");
}

static void
gathering_done (NiceAgent * agent, guint stream_id, KmsWebrtcEndPoint * self)
{
  GST_WARNING ("TODO: implement");
}

static gpointer
loop_thread (gpointer loop)
{
  GMainContext *context;

  context = g_main_loop_get_context (loop);
  g_main_context_acquire (context);
  g_main_loop_run (loop);
  g_main_context_release (context);

  return NULL;
}

static gboolean
quit_main_loop_idle (gpointer loop)
{
  g_main_loop_quit (loop);

  return G_SOURCE_REMOVE;
}

static void
destroy_main_loop (gpointer loop)
{
  g_main_loop_unref (loop);
}

static void
kms_webrtc_end_point_finalize (GObject * object)
{
  KmsWebrtcEndPoint *self = KMS_WEBRTC_END_POINT (object);

  if (self->priv->agent != NULL) {
    g_object_unref (self->priv->agent);
    self->priv->agent = NULL;
  }

  if (self->priv->loop != NULL) {
    GSource *source;

    source = g_idle_source_new ();
    g_source_set_callback (source, quit_main_loop_idle, self->priv->loop,
        destroy_main_loop);
    g_source_attach (source, self->priv->context);
    g_source_unref (source);
    self->priv->loop = NULL;
  }

  if (self->priv->thread != NULL) {
    g_thread_join (self->priv->thread);
    g_thread_unref (self->priv->thread);
  }

  if (self->priv->context != NULL) {
    g_main_context_unref (self->priv->context);
    self->priv->context = NULL;
  }

  /* chain up */
  G_OBJECT_CLASS (kms_webrtc_end_point_parent_class)->finalize (object);
}

static void
kms_webrtc_end_point_class_init (KmsWebrtcEndPointClass * klass)
{
  GObjectClass *gobject_class;
  KmsBaseSdpEndPointClass *base_sdp_end_point_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_webrtc_end_point_finalize;

  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "WebrtcEndPoint",
      "WEBRTC/Stream/WebrtcEndPoint",
      "WebRTC EndPoint element", "Miguel París Díaz <mparisdiaz@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  base_sdp_end_point_class = KMS_BASE_SDP_END_POINT_CLASS (klass);
  base_sdp_end_point_class->set_transport_to_sdp =
      kms_webrtc_end_point_set_transport_to_sdp;
  base_sdp_end_point_class->start_transport_send =
      kms_webrtc_end_point_start_transport_send;

  g_type_class_add_private (klass, sizeof (KmsWebrtcEndPointPrivate));
}

static void
kms_webrtc_end_point_init (KmsWebrtcEndPoint * self)
{
  self->priv = KMS_WEBRTC_END_POINT_GET_PRIVATE (self);

  self->priv->context = g_main_context_new ();
  if (self->priv->context == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create context.");
    return;
  }

  self->priv->loop = g_main_loop_new (self->priv->context, TRUE);
  if (self->priv->loop == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create main loop.");
    return;
  }

  self->priv->thread =
      g_thread_new (GST_ELEMENT_NAME (self), loop_thread, self->priv->loop);

  self->priv->agent =
      nice_agent_new (self->priv->context, NICE_COMPATIBILITY_RFC5245);
  if (self->priv->agent == NULL) {
    GST_ERROR_OBJECT (self, "Cannot create nice agent.");
    return;
  }

  g_object_set (self->priv->agent, "upnp", FALSE, NULL);
  g_signal_connect (self->priv->agent, "candidate-gathering-done",
      G_CALLBACK (gathering_done), self);
}

gboolean
kms_webrtc_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_END_POINT);
}
