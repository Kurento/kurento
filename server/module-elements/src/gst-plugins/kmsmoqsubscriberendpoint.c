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
#include <commons/kmsagnosticcaps.h>

#include "kmsmoqsubscriberendpoint.h"

#define PLUGIN_NAME "moqsubscriberendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_moq_subscriber_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_moq_subscriber_endpoint_debug_category

#define KMS_MOQ_SUBSCRIBER_ENDPOINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                        \
    (obj),                                             \
    KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT,                  \
    KmsMoqSubscriberEndpointPrivate                    \
  )                                                    \
)

enum
{
  PROP_0,
  PROP_URL,
  PROP_BROADCAST,
  N_PROPERTIES
};

enum
{
  SIGNAL_EOS,
  LAST_SIGNAL
};

static guint kms_moq_subscriber_endpoint_signals[LAST_SIGNAL] = { 0 };

struct _KmsMoqSubscriberEndpointPrivate
{
  GstElement *pipeline;   /* internal pipeline: moqsrc ! decodebin3 */
  GstElement *moqsrc;
  KmsLoop    *loop;

  gchar *url;
  gchar *broadcast;
};

/* Declare the type */
G_DEFINE_TYPE_WITH_CODE (KmsMoqSubscriberEndpoint,
    kms_moq_subscriber_endpoint, KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_moq_subscriber_endpoint_debug_category,
        PLUGIN_NAME, 0, "debug category for MoqSubscriberEndpoint element"));

/* -------------------------------------------------------------------------
 * Internal pipeline → Kurento pipeline bridge (appsink → appsrc)
 * ---------------------------------------------------------------------- */

static GstFlowReturn
appsink_new_sample_cb (GstAppSink *appsink, gpointer user_data)
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
appsink_eos_cb (GstAppSink *appsink, gpointer user_data)
{
  GstAppSrc *appsrc = GST_APP_SRC (user_data);

  GST_DEBUG_OBJECT (appsink, "EOS in internal pipeline, forwarding to appsrc");
  gst_app_src_end_of_stream (appsrc);
}

static GstAppSinkCallbacks appsink_cbs = {
  .eos         = appsink_eos_cb,
  .new_preroll = NULL,
  .new_sample  = appsink_new_sample_cb,
};

static void
connect_internal_pad (KmsMoqSubscriberEndpoint *self, GstPad *internal_pad)
{
  GstCaps *caps;
  GstElement *agnosticbin = NULL;
  GstElement *appsink = NULL;
  GstElement *appsrc  = NULL;
  GstPad *sinkpad;
  GstPadLinkReturn link_ret;

  caps = gst_pad_query_caps (internal_pad, NULL);
  if (caps == NULL) {
    GST_WARNING_OBJECT (self, "Could not get caps from pad %" GST_PTR_FORMAT,
        internal_pad);
    return;
  }

  if (kms_utils_caps_is_audio (caps)) {
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  } else if (kms_utils_caps_is_video (caps)) {
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  }

  gst_caps_unref (caps);

  if (agnosticbin == NULL) {
    GST_WARNING_OBJECT (self, "Could not find agnosticbin for pad %" GST_PTR_FORMAT,
        internal_pad);
    return;
  }

  /* Create appsrc in the outer (KmsElement) pipeline */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc),
      "is-live", TRUE,
      "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0),
      "format", GST_FORMAT_TIME,
      "emit-signals", FALSE,
      NULL);

  gst_bin_add (GST_BIN (self), appsrc);
  gst_element_link (appsrc, agnosticbin);
  gst_element_sync_state_with_parent (appsrc);

  /* Create appsink in the internal pipeline */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (G_OBJECT (appsink),
      "emit-signals", FALSE,
      "sync", FALSE,
      "async", FALSE,
      NULL);

  gst_app_sink_set_callbacks (GST_APP_SINK (appsink), &appsink_cbs,
      g_object_ref (appsrc), g_object_unref);

  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);
  gst_element_sync_state_with_parent (appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  link_ret = gst_pad_link (internal_pad, sinkpad);

  if (link_ret != GST_PAD_LINK_OK) {
    GST_ERROR_OBJECT (self, "Failed to link decodebin pad to appsink: %s",
        gst_pad_link_get_name (link_ret));
  }

  g_object_unref (sinkpad);
}

static void
decodebin_pad_added_cb (GstElement *decodebin, GstPad *pad, gpointer user_data)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (user_data);

  if (GST_PAD_IS_SRC (pad)) {
    connect_internal_pad (self, pad);
  }
}

/* -------------------------------------------------------------------------
 * Bus handler for the internal pipeline
 * ---------------------------------------------------------------------- */

static gboolean
kms_moq_subscriber_emit_eos (gpointer data)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (data);

  g_signal_emit (G_OBJECT (self),
      kms_moq_subscriber_endpoint_signals[SIGNAL_EOS], 0);

  return G_SOURCE_REMOVE;
}

static GstBusSyncReply
internal_bus_sync_handler (GstBus *bus, GstMessage *msg, gpointer user_data)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (user_data);

  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_EOS:
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_moq_subscriber_emit_eos, g_object_ref (self), g_object_unref);
      break;

    case GST_MESSAGE_ERROR:{
      GError *err = NULL;
      gchar *dbg = NULL;

      gst_message_parse_error (msg, &err, &dbg);
      GST_ERROR_OBJECT (self, "Internal pipeline error: %s (%s)",
          err ? err->message : "(null)", dbg ? dbg : "(null)");

      /* Post the error on the outer element so Kurento can propagate it */
      gst_element_post_message (GST_ELEMENT (self),
          gst_message_new_error (GST_OBJECT (self), err, dbg));

      g_clear_error (&err);
      g_free (dbg);
      break;
    }

    default:
      break;
  }

  return GST_BUS_PASS;
}

/* -------------------------------------------------------------------------
 * GObject property get / set
 * ---------------------------------------------------------------------- */

static void
kms_moq_subscriber_endpoint_set_property (GObject *object, guint prop_id,
    const GValue *value, GParamSpec *pspec)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (object);

  switch (prop_id) {
    case PROP_URL:
      g_free (self->priv->url);
      self->priv->url = g_value_dup_string (value);
      if (self->priv->moqsrc) {
        g_object_set (self->priv->moqsrc, "url", self->priv->url, NULL);
      }
      break;
    case PROP_BROADCAST:
      g_free (self->priv->broadcast);
      self->priv->broadcast = g_value_dup_string (value);
      if (self->priv->moqsrc) {
        g_object_set (self->priv->moqsrc, "broadcast", self->priv->broadcast, NULL);
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_moq_subscriber_endpoint_get_property (GObject *object, guint prop_id,
    GValue *value, GParamSpec *pspec)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (object);

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
kms_moq_subscriber_endpoint_dispose (GObject *object)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (object);

  g_clear_object (&self->priv->loop);

  if (self->priv->pipeline != NULL) {
    GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));

    gst_bus_set_sync_handler (bus, NULL, NULL, NULL);
    gst_bus_remove_watch (bus);
    g_object_unref (bus);

    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    g_clear_object (&self->priv->pipeline);
  }

  G_OBJECT_CLASS (kms_moq_subscriber_endpoint_parent_class)->dispose (object);
}

static void
kms_moq_subscriber_endpoint_finalize (GObject *object)
{
  KmsMoqSubscriberEndpoint *self = KMS_MOQ_SUBSCRIBER_ENDPOINT (object);

  g_free (self->priv->url);
  g_free (self->priv->broadcast);

  G_OBJECT_CLASS (kms_moq_subscriber_endpoint_parent_class)->finalize (object);
}

static void
kms_moq_subscriber_endpoint_init (KmsMoqSubscriberEndpoint *self)
{
  GstElement *decodebin;
  GstBus *bus;

  self->priv = KMS_MOQ_SUBSCRIBER_ENDPOINT_GET_PRIVATE (self);

  self->priv->loop = kms_loop_new ();

  /* Build internal pipeline: moqsrc ! decodebin3 */
  self->priv->pipeline = gst_pipeline_new ("moqsubscriber-internal");

  self->priv->moqsrc = gst_element_factory_make ("moqsrc", "moqsrc");
  if (self->priv->moqsrc == NULL) {
    GST_ERROR_OBJECT (self,
        "Could not create 'moqsrc' element. "
        "Is the gstreamer1.0-moq plugin installed?");
    return;
  }

  decodebin = gst_element_factory_make ("decodebin3", "decodebin3");
  if (decodebin == NULL) {
    GST_ERROR_OBJECT (self, "Could not create 'decodebin3' element.");
    return;
  }

  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (decodebin_pad_added_cb), self);

  gst_bin_add_many (GST_BIN (self->priv->pipeline),
      self->priv->moqsrc, decodebin, NULL);
  gst_element_link (self->priv->moqsrc, decodebin);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_set_sync_handler (bus, internal_bus_sync_handler, self, NULL);
  g_object_unref (bus);
}

/* -------------------------------------------------------------------------
 * Class initialisation
 * ---------------------------------------------------------------------- */

static void
kms_moq_subscriber_endpoint_class_init (KmsMoqSubscriberEndpointClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "MoqSubscriberEndpoint", "Sink/Network",
      "Kurento Media over QUIC subscriber endpoint",
      "Kurento <kurento@openvidu.io>");

  gobject_class->dispose      = kms_moq_subscriber_endpoint_dispose;
  gobject_class->finalize     = kms_moq_subscriber_endpoint_finalize;
  gobject_class->set_property = kms_moq_subscriber_endpoint_set_property;
  gobject_class->get_property = kms_moq_subscriber_endpoint_get_property;

  g_object_class_install_property (gobject_class, PROP_URL,
      g_param_spec_string ("url", "MoQ relay URL",
          "URL of the MoQ relay to connect to",
          "", G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_BROADCAST,
      g_param_spec_string ("broadcast", "Broadcast name",
          "Name of the MoQ broadcast to subscribe to",
          "", G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  kms_moq_subscriber_endpoint_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
          G_TYPE_FROM_CLASS (klass),
          G_SIGNAL_RUN_LAST,
          G_STRUCT_OFFSET (KmsMoqSubscriberEndpointClass, eos_signal),
          NULL, NULL,
          g_cclosure_marshal_VOID__VOID,
          G_TYPE_NONE, 0);

  g_type_class_add_private (klass, sizeof (KmsMoqSubscriberEndpointPrivate));
}

/* -------------------------------------------------------------------------
 * Public API: subscribe / unsubscribe
 * ---------------------------------------------------------------------- */

void
kms_moq_subscriber_endpoint_subscribe (KmsMoqSubscriberEndpoint *self)
{
  g_return_if_fail (KMS_IS_MOQ_SUBSCRIBER_ENDPOINT (self));

  if (self->priv->pipeline == NULL) {
    GST_ERROR_OBJECT (self, "Internal pipeline was not created (missing moqsrc?)");
    return;
  }

  if (self->priv->url != NULL) {
    g_object_set (self->priv->moqsrc, "url", self->priv->url, NULL);
  }
  if (self->priv->broadcast != NULL) {
    g_object_set (self->priv->moqsrc, "broadcast", self->priv->broadcast, NULL);
  }

  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);
}

void
kms_moq_subscriber_endpoint_unsubscribe (KmsMoqSubscriberEndpoint *self)
{
  g_return_if_fail (KMS_IS_MOQ_SUBSCRIBER_ENDPOINT (self));

  if (self->priv->pipeline != NULL) {
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  }
}

/* -------------------------------------------------------------------------
 * Plugin registration
 * ---------------------------------------------------------------------- */

gboolean
kms_moq_subscriber_endpoint_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MOQ_SUBSCRIBER_ENDPOINT);
}
