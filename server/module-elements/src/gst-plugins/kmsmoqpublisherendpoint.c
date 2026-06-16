/*
 * (C) Copyright 2024 Kurento (http://kurento.org/)
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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include <commons/kmselement.h>
#include <commons/kmsutils.h>
#include <commons/kmsloop.h>

#include "kmsmoqpublisherendpoint.h"

#define PLUGIN_NAME "moqpublisherendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_moq_publisher_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_moq_publisher_endpoint_debug_category

#define KMS_MOQ_PUBLISHER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                        \
    (obj),                                             \
    KMS_TYPE_MOQ_PUBLISHER_ENDPOINT,                   \
    KmsMoqPublisherEndpointPrivate                     \
  )                                                    \
)

enum
{
  PROP_0,
  PROP_URL,
  PROP_BROADCAST,
  N_PROPERTIES
};

/*
 * Internal pipeline layout:
 *
 *   (KmsElement sink pads)
 *       |           |
 *   appsink_a   appsink_v
 *       |           |
 *   appsrc_a    appsrc_v
 *       |           |
 *   aacenc      x264enc
 *       |           |
 *       +----+-------+
 *            |
 *         moqsink  (url=..., broadcast=...)
 */
struct _KmsMoqPublisherEndpointPrivate
{
  GstElement *pipeline;
  GstElement *moqsink;
  GstElement *audio_appsrc;
  GstElement *video_appsrc;
  KmsLoop    *loop;

  /* Appsinks sitting on the KmsElement side (outer pipeline) */
  GstElement *outer_audio_appsink;
  GstElement *outer_video_appsink;

  gchar *url;
  gchar *broadcast;

  gboolean audio_linked;
  gboolean video_linked;
};

G_DEFINE_TYPE_WITH_CODE (KmsMoqPublisherEndpoint,
    kms_moq_publisher_endpoint, KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_moq_publisher_endpoint_debug_category,
        PLUGIN_NAME, 0, "debug category for MoqPublisherEndpoint element"));

/* -------------------------------------------------------------------------
 * Kurento outer appsink → internal appsrc bridge
 * ---------------------------------------------------------------------- */

static GstFlowReturn
outer_appsink_new_sample_cb (GstAppSink *appsink, gpointer user_data)
{
  GstAppSrc *appsrc = GST_APP_SRC (user_data);
  GstSample *sample;
  GstBuffer *buffer;
  GstFlowReturn ret;

  sample = gst_app_sink_pull_sample (appsink);
  if (sample == NULL) {
    return GST_FLOW_ERROR;
  }

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    gst_sample_unref (sample);
    return GST_FLOW_OK;
  }

  buffer = gst_buffer_copy (buffer);
  ret = gst_app_src_push_buffer (appsrc, buffer);

  gst_sample_unref (sample);
  return ret;
}

static void
outer_appsink_eos_cb (GstAppSink *appsink, gpointer user_data)
{
  GstAppSrc *appsrc = GST_APP_SRC (user_data);

  gst_app_src_end_of_stream (appsrc);
}

static GstAppSinkCallbacks outer_appsink_cbs = {
  .eos         = outer_appsink_eos_cb,
  .new_preroll = NULL,
  .new_sample  = outer_appsink_new_sample_cb,
};

/* -------------------------------------------------------------------------
 * Connect KmsElement sink pads to the internal pipeline
 * ---------------------------------------------------------------------- */

/**
 * connect_sink_pad: called once a new pad appears on KmsElement that should
 * be routed to the internal moqsink pipeline.
 *
 * We do NOT create the internal pipeline here (it was built in _init).  We
 * just need to create an appsink in the outer (KmsElement) bin that drains
 * the agnostic output, and bridge it to the matching appsrc in the internal
 * pipeline.
 */
static void
connect_audio_sink (KmsMoqPublisherEndpoint *self)
{
  KmsMoqPublisherEndpointPrivate *priv = self->priv;
  GstElement *agnosticbin;

  if (priv->audio_linked) {
    return;
  }

  agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  if (agnosticbin == NULL) {
    GST_WARNING_OBJECT (self, "No audio agnosticbin available yet");
    return;
  }

  /* Create the outer appsink that drains the encoded audio from agnosticbin */
  priv->outer_audio_appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (G_OBJECT (priv->outer_audio_appsink),
      "emit-signals", FALSE,
      "sync", FALSE,
      "async", FALSE,
      NULL);

  gst_app_sink_set_callbacks (GST_APP_SINK (priv->outer_audio_appsink),
      &outer_appsink_cbs, g_object_ref (priv->audio_appsrc), g_object_unref);

  gst_bin_add (GST_BIN (self), priv->outer_audio_appsink);

  if (!gst_element_link (agnosticbin, priv->outer_audio_appsink)) {
    GST_ERROR_OBJECT (self, "Failed to link agnosticbin to outer audio appsink");
  }

  gst_element_sync_state_with_parent (priv->outer_audio_appsink);

  priv->audio_linked = TRUE;
}

static void
connect_video_sink (KmsMoqPublisherEndpoint *self)
{
  KmsMoqPublisherEndpointPrivate *priv = self->priv;
  GstElement *agnosticbin;

  if (priv->video_linked) {
    return;
  }

  agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  if (agnosticbin == NULL) {
    GST_WARNING_OBJECT (self, "No video agnosticbin available yet");
    return;
  }

  priv->outer_video_appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (G_OBJECT (priv->outer_video_appsink),
      "emit-signals", FALSE,
      "sync", FALSE,
      "async", FALSE,
      NULL);

  gst_app_sink_set_callbacks (GST_APP_SINK (priv->outer_video_appsink),
      &outer_appsink_cbs, g_object_ref (priv->video_appsrc), g_object_unref);

  gst_bin_add (GST_BIN (self), priv->outer_video_appsink);

  if (!gst_element_link (agnosticbin, priv->outer_video_appsink)) {
    GST_ERROR_OBJECT (self, "Failed to link agnosticbin to outer video appsink");
  }

  gst_element_sync_state_with_parent (priv->outer_video_appsink);

  priv->video_linked = TRUE;
}

/* -------------------------------------------------------------------------
 * Internal pipeline bus handler
 * ---------------------------------------------------------------------- */

static GstBusSyncReply
internal_bus_sync_handler (GstBus *bus, GstMessage *msg, gpointer user_data)
{
  KmsMoqPublisherEndpoint *self = KMS_MOQ_PUBLISHER_ENDPOINT (user_data);

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {
    GError *err = NULL;
    gchar *dbg = NULL;

    gst_message_parse_error (msg, &err, &dbg);
    GST_ERROR_OBJECT (self, "Internal pipeline error: %s (%s)",
        err ? err->message : "(null)", dbg ? dbg : "(null)");

    gst_element_post_message (GST_ELEMENT (self),
        gst_message_new_error (GST_OBJECT (self), err, dbg));

    g_clear_error (&err);
    g_free (dbg);
  }

  return GST_BUS_PASS;
}

/* -------------------------------------------------------------------------
 * GObject property get / set
 * ---------------------------------------------------------------------- */

static void
kms_moq_publisher_endpoint_set_property (GObject *object, guint prop_id,
    const GValue *value, GParamSpec *pspec)
{
  KmsMoqPublisherEndpoint *self = KMS_MOQ_PUBLISHER_ENDPOINT (object);

  switch (prop_id) {
    case PROP_URL:
      g_free (self->priv->url);
      self->priv->url = g_value_dup_string (value);
      if (self->priv->moqsink) {
        g_object_set (self->priv->moqsink, "url", self->priv->url, NULL);
      }
      break;
    case PROP_BROADCAST:
      g_free (self->priv->broadcast);
      self->priv->broadcast = g_value_dup_string (value);
      if (self->priv->moqsink) {
        g_object_set (self->priv->moqsink, "broadcast", self->priv->broadcast, NULL);
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_moq_publisher_endpoint_get_property (GObject *object, guint prop_id,
    GValue *value, GParamSpec *pspec)
{
  KmsMoqPublisherEndpoint *self = KMS_MOQ_PUBLISHER_ENDPOINT (object);

  switch (prop_id) {
    case PROP_URL:
      g_value_set_string (value, self->priv->url);
      break;
    case PROP_BROADCAST:
      g_value_set_string (value, self->priv->broadcast);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/* -------------------------------------------------------------------------
 * GObject lifecycle
 * ---------------------------------------------------------------------- */

static void
kms_moq_publisher_endpoint_dispose (GObject *object)
{
  KmsMoqPublisherEndpoint *self = KMS_MOQ_PUBLISHER_ENDPOINT (object);

  g_clear_object (&self->priv->loop);

  if (self->priv->pipeline != NULL) {
    GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));

    gst_bus_set_sync_handler (bus, NULL, NULL, NULL);
    gst_bus_remove_watch (bus);
    g_object_unref (bus);

    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    g_clear_object (&self->priv->pipeline);
  }

  G_OBJECT_CLASS (kms_moq_publisher_endpoint_parent_class)->dispose (object);
}

static void
kms_moq_publisher_endpoint_finalize (GObject *object)
{
  KmsMoqPublisherEndpoint *self = KMS_MOQ_PUBLISHER_ENDPOINT (object);

  g_free (self->priv->url);
  g_free (self->priv->broadcast);

  G_OBJECT_CLASS (kms_moq_publisher_endpoint_parent_class)->finalize (object);
}

static void
kms_moq_publisher_endpoint_init (KmsMoqPublisherEndpoint *self)
{
  GstElement *audio_enc, *video_enc;
  GstBus *bus;

  self->priv = KMS_MOQ_PUBLISHER_ENDPOINT_GET_PRIVATE (self);
  self->priv->loop = kms_loop_new ();

  /*
   * Internal pipeline:
   *
   *   audio_appsrc ! avenc_aac  \
   *                              +---> moqsink
   *   video_appsrc ! x264enc   /
   *
   * moqsink acts as both muxer and transport sink.
   */
  self->priv->pipeline = gst_pipeline_new ("moqpublisher-internal");

  /* --- audio branch --- */
  self->priv->audio_appsrc = gst_element_factory_make ("appsrc", "audio_appsrc");
  audio_enc = gst_element_factory_make ("avenc_aac", "audio_enc");

  /* --- video branch --- */
  self->priv->video_appsrc = gst_element_factory_make ("appsrc", "video_appsrc");
  video_enc = gst_element_factory_make ("x264enc", "video_enc");

  /* --- moqsink --- */
  self->priv->moqsink = gst_element_factory_make ("moqsink", "moqsink");

  if (self->priv->moqsink == NULL) {
    GST_ERROR_OBJECT (self,
        "Could not create 'moqsink' element. "
        "Is the gstreamer1.0-moq plugin installed?");
    return;
  }

  if (self->priv->audio_appsrc == NULL || audio_enc == NULL
      || self->priv->video_appsrc == NULL || video_enc == NULL) {
    GST_ERROR_OBJECT (self,
        "Could not create encoder elements (avenc_aac / x264enc). "
        "Are gstreamer1.0-plugins-bad and gstreamer1.0-plugins-ugly installed?");
    return;
  }

  g_object_set (G_OBJECT (self->priv->audio_appsrc),
      "is-live", TRUE, "format", GST_FORMAT_TIME, "emit-signals", FALSE, NULL);
  g_object_set (G_OBJECT (self->priv->video_appsrc),
      "is-live", TRUE, "format", GST_FORMAT_TIME, "emit-signals", FALSE, NULL);

  /* x264enc: tune for low-latency live streaming */
  g_object_set (G_OBJECT (video_enc),
      "tune", 4 /* zerolatency */, "key-int-max", 60, NULL);

  gst_bin_add_many (GST_BIN (self->priv->pipeline),
      self->priv->audio_appsrc, audio_enc,
      self->priv->video_appsrc, video_enc,
      self->priv->moqsink, NULL);

  /* Link audio: appsrc_a ! avenc_aac ! moqsink.sink_0 */
  if (!gst_element_link (self->priv->audio_appsrc, audio_enc)) {
    GST_ERROR_OBJECT (self, "Failed to link audio_appsrc ! audio_enc");
  }
  if (!gst_element_link_pads (audio_enc, "src", self->priv->moqsink, "sink_0")) {
    GST_WARNING_OBJECT (self,
        "Failed to link audio_enc to moqsink.sink_0; moqsink may use dynamic request pads");
    gst_element_link (audio_enc, self->priv->moqsink);
  }

  /* Link video: appsrc_v ! x264enc ! moqsink.sink_1 */
  if (!gst_element_link (self->priv->video_appsrc, video_enc)) {
    GST_ERROR_OBJECT (self, "Failed to link video_appsrc ! video_enc");
  }
  if (!gst_element_link_pads (video_enc, "src", self->priv->moqsink, "sink_1")) {
    GST_WARNING_OBJECT (self,
        "Failed to link video_enc to moqsink.sink_1; moqsink may use dynamic request pads");
    gst_element_link (video_enc, self->priv->moqsink);
  }

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_set_sync_handler (bus, internal_bus_sync_handler, self, NULL);
  g_object_unref (bus);
}

/* -------------------------------------------------------------------------
 * Class initialisation
 * ---------------------------------------------------------------------- */

static void
kms_moq_publisher_endpoint_class_init (KmsMoqPublisherEndpointClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "MoqPublisherEndpoint", "Source/Network",
      "Kurento Media over QUIC publisher endpoint",
      "Kurento <kurento@openvidu.io>");

  gobject_class->dispose      = kms_moq_publisher_endpoint_dispose;
  gobject_class->finalize     = kms_moq_publisher_endpoint_finalize;
  gobject_class->set_property = kms_moq_publisher_endpoint_set_property;
  gobject_class->get_property = kms_moq_publisher_endpoint_get_property;

  g_object_class_install_property (gobject_class, PROP_URL,
      g_param_spec_string ("url", "MoQ relay URL",
          "URL of the MoQ relay to publish to",
          "", G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_BROADCAST,
      g_param_spec_string ("broadcast", "Broadcast name",
          "Name of the MoQ broadcast to publish",
          "", G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsMoqPublisherEndpointPrivate));
}

/* -------------------------------------------------------------------------
 * Public API: publish / unpublish
 * ---------------------------------------------------------------------- */

void
kms_moq_publisher_endpoint_publish (KmsMoqPublisherEndpoint *self)
{
  g_return_if_fail (KMS_IS_MOQ_PUBLISHER_ENDPOINT (self));

  if (self->priv->pipeline == NULL) {
    GST_ERROR_OBJECT (self, "Internal pipeline was not created (missing moqsink?)");
    return;
  }

  if (self->priv->url != NULL) {
    g_object_set (self->priv->moqsink, "url", self->priv->url, NULL);
  }
  if (self->priv->broadcast != NULL) {
    g_object_set (self->priv->moqsink, "broadcast", self->priv->broadcast, NULL);
  }

  /* Connect KmsElement sink pads to the internal pipeline if not done yet */
  connect_audio_sink (self);
  connect_video_sink (self);

  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);
}

void
kms_moq_publisher_endpoint_unpublish (KmsMoqPublisherEndpoint *self)
{
  g_return_if_fail (KMS_IS_MOQ_PUBLISHER_ENDPOINT (self));

  if (self->priv->pipeline != NULL) {
    /* Send EOS so moqsink can flush cleanly */
    gst_element_send_event (self->priv->pipeline, gst_event_new_eos ());
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  }
}

/* -------------------------------------------------------------------------
 * Plugin registration
 * ---------------------------------------------------------------------- */

gboolean
kms_moq_publisher_endpoint_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MOQ_PUBLISHER_ENDPOINT);
}
