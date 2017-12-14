/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#include <commons/kmsagnosticcaps.h>
#include <commons/kms-core-marshal.h>

#include "kmshttppostendpoint.h"

#define PLUGIN_NAME "httppostendpoint"

#define APPSRC_DATA "appsrc-data"
G_DEFINE_QUARK (APPSRC_DATA, appsrc_data);

#define APPSINK_DATA "appsink-data"
G_DEFINE_QUARK (APPSINK_DATA, appsink_data);

#define BASE_TIME_DATA "base-time-data"
G_DEFINE_QUARK (BASE_TIME_DATA, base_time_data);

#define POST_PIPELINE "post-pipeline"

GST_DEBUG_CATEGORY_STATIC (kms_http_post_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_http_post_endpoint_debug_category

#define KMS_HTTP_POST_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                    \
    (obj),                                         \
    KMS_TYPE_HTTP_POST_ENDPOINT,                   \
    KmsHttpPostEndpointPrivate                     \
  )                                                \
)

struct _KmsHttpPostEndpointPrivate
{
  GstElement *appsrc;
  gboolean use_encoded_media;
  int handler_id;
  GstBus *bus;
};

/* Object properties */
enum
{
  PROP_0,
  PROP_USE_ENCODED_MEDIA,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* Object signals */
enum
{
  /* actions */
  SIGNAL_PUSH_BUFFER,
  SIGNAL_END_OF_STREAM,
  LAST_SIGNAL
};

static guint http_post_ep_signals[LAST_SIGNAL] = { 0 };

G_DEFINE_TYPE_WITH_CODE (KmsHttpPostEndpoint, kms_http_post_endpoint,
    KMS_TYPE_HTTP_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_http_post_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for http post endpoint plugin"));

static void
bus_message (GstBus * bus, GstMessage * msg, KmsHttpEndpoint * self)
{
  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS)
    g_signal_emit_by_name (G_OBJECT (self), "eos", 0);
}

static void
release_gst_clock (gpointer data)
{
  g_slice_free (GstClockTime, data);
}

static GstFlowReturn
new_sample_post_handler (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstSample *sample = NULL;
  GstBuffer *buffer;
  GstFlowReturn ret;
  GstClockTime *base_time;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample == NULL)
    return GST_FLOW_OK;

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);
  buffer = gst_buffer_make_writable (buffer);

  BASE_TIME_LOCK (GST_OBJECT_PARENT (appsrc));

  base_time =
      g_object_get_qdata (G_OBJECT (GST_OBJECT_PARENT (appsrc)),
      base_time_data_quark ());

  if (base_time == NULL) {
    GstClock *clock;

    clock = gst_element_get_clock (appsrc);
    base_time = g_slice_new0 (GstClockTime);

    g_object_set_qdata_full (G_OBJECT (GST_OBJECT_PARENT (appsrc)),
        base_time_data_quark (), base_time, release_gst_clock);
    *base_time =
        gst_clock_get_time (clock) - gst_element_get_base_time (appsrc);
    g_object_unref (clock);
    GST_DEBUG ("Setting base time to: %" G_GUINT64_FORMAT, *base_time);
  }

  if (GST_BUFFER_PTS_IS_VALID (buffer))
    buffer->pts += *base_time;
  if (GST_BUFFER_DTS_IS_VALID (buffer))
    buffer->dts += *base_time;

  BASE_TIME_UNLOCK (GST_OBJECT_PARENT (appsrc));

  /* Pass the buffer through appsrc element which is */
  /* placed in a different pipeline */
  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  gst_buffer_unref (buffer);

  if (ret != GST_FLOW_OK) {
    /* something went wrong */
    GST_ERROR ("Could not send buffer to appsrc %s. Cause %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
  }

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static GstPadProbeReturn
set_appsrc_caps (GstPad * pad, GstPadProbeInfo * info, gpointer httpep)
{
  KmsHttpPostEndpoint *self = KMS_HTTP_POST_ENDPOINT (httpep);
  GstEvent *event = GST_PAD_PROBE_INFO_EVENT (info);
  GstCaps *audio_caps = NULL, *video_caps = NULL;
  GstElement *appsrc, *appsink, *agnosticbin;
  GstCaps *caps;
  gpointer data;

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_OK;
  }

  gst_event_parse_caps (event, &caps);
  if (caps == NULL) {
    GST_ERROR_OBJECT (pad, "Invalid caps received");
    return GST_PAD_PROBE_OK;
  }

  GST_TRACE ("caps are %" GST_PTR_FORMAT, caps);

  data = g_object_get_qdata (G_OBJECT (pad), appsrc_data_quark ());
  if (data != NULL) {
    goto end;
  }

  /* Get the proper agnosticbin */
  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);

  if (gst_caps_can_intersect (audio_caps, caps))
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  else if (gst_caps_can_intersect (video_caps, caps))
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  else {
    GST_ELEMENT_WARNING (self, CORE, CAPS,
        ("Unsupported media received: %" GST_PTR_FORMAT, caps),
        ("Unsupported media received: %" GST_PTR_FORMAT, caps));
    goto end;
  }

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", FALSE,
      "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME,
      "caps", caps, NULL);

  gst_bin_add (GST_BIN (self), appsrc);
  if (!gst_element_link (appsrc, agnosticbin)) {
    GST_ERROR ("Could not link %s to element %s", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (agnosticbin));
  }

  /* Connect new-sample signal to callback */
  appsink = gst_pad_get_parent_element (pad);
  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_post_handler),
      appsrc);
  g_object_unref (appsink);

  g_object_set_qdata (G_OBJECT (pad), appsrc_data_quark (), appsrc);
  gst_element_sync_state_with_parent (appsrc);

end:
  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);

  return GST_PAD_PROBE_OK;
}

static void
post_decodebin_pad_added_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndpoint * self)
{
  GstElement *appsink;
  GstPad *sinkpad;

  GST_DEBUG_OBJECT (pad, "Pad added");

  /* Create appsink and link to pad */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (appsink, "sync", TRUE, "enable-last-sample",
      FALSE, "emit-signals", TRUE, "qos", FALSE, "max-buffers", 1,
      "async", FALSE, NULL);
  gst_bin_add (GST_BIN (self->pipeline), appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  if (gst_pad_link (pad, sinkpad) != GST_PAD_LINK_OK) {
    GST_ERROR_OBJECT (self, "Can not link %" GST_PTR_FORMAT " to %"
        GST_PTR_FORMAT, decodebin, appsink);
  }

  gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      set_appsrc_caps, self, NULL);

  g_object_unref (sinkpad);

  g_object_set_qdata (G_OBJECT (pad), appsink_data_quark (), appsink);

  gst_element_sync_state_with_parent (appsink);
}

static void
post_decodebin_pad_removed_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndpoint * self)
{
  GstElement *appsink, *appsrc;
  GstPad *sinkpad;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_DEBUG ("pad %" GST_PTR_FORMAT " removed", pad);

  appsink = g_object_steal_qdata (G_OBJECT (pad), appsink_data_quark ());

  if (appsink == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  appsrc = g_object_get_qdata (G_OBJECT (sinkpad), appsrc_data_quark ());
  g_object_unref (sinkpad);

  if (!gst_element_set_locked_state (appsink, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (appsink));

  GST_DEBUG ("Removing appsink %s from %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (self->pipeline));

  gst_element_set_state (appsink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->pipeline), appsink);

  if (appsrc == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }

  if (GST_OBJECT_PARENT (appsrc) != NULL) {
    g_object_ref (appsrc);
    gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (appsrc)), appsrc);
    gst_element_set_state (appsrc, GST_STATE_NULL);
    g_object_unref (appsrc);
  }
}

static void
kms_http_post_endpoint_init_pipeline (KmsHttpPostEndpoint * self)
{
  GstElement *decodebin;
  GstCaps *deco_caps;

  KMS_HTTP_ENDPOINT (self)->pipeline = gst_pipeline_new (POST_PIPELINE);
  self->priv->appsrc = gst_element_factory_make ("appsrc", NULL);
  decodebin = gst_element_factory_make ("decodebin", NULL);

  /* configure appsrc */
  g_object_set (G_OBJECT (self->priv->appsrc), "is-live", TRUE,
      "do-timestamp", TRUE, "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  /* configure decodebin */
  if (self->priv->use_encoded_media) {
    deco_caps = gst_caps_from_string (KMS_AGNOSTIC_NO_RTP_CAPS);
    g_object_set (G_OBJECT (decodebin), "caps", deco_caps, NULL);
    gst_caps_unref (deco_caps);
  }

  gst_bin_add_many (GST_BIN (KMS_HTTP_ENDPOINT (self)->pipeline),
      self->priv->appsrc, decodebin, NULL);

  gst_element_link (self->priv->appsrc, decodebin);

  /* Connect decodebin signals */
  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (post_decodebin_pad_added_handler), self);
  g_signal_connect (decodebin, "pad-removed",
      G_CALLBACK (post_decodebin_pad_removed_handler), self);

  self->priv->bus =
      gst_pipeline_get_bus (GST_PIPELINE (KMS_HTTP_ENDPOINT (self)->pipeline));
  gst_bus_add_signal_watch (self->priv->bus);
  self->priv->handler_id =
      g_signal_connect (G_OBJECT (self->priv->bus), "message",
      G_CALLBACK (bus_message), self);

  /* Set pipeline to playing */
  gst_element_set_state (KMS_HTTP_ENDPOINT (self)->pipeline, GST_STATE_PLAYING);
}

static GstFlowReturn
kms_http_post_endpoint_push_buffer_action (KmsHttpPostEndpoint * self,
    GstBuffer * buffer)
{
  GstFlowReturn ret;

  KMS_ELEMENT_LOCK (self);

  if (KMS_HTTP_ENDPOINT (self)->pipeline == NULL)
    kms_http_post_endpoint_init_pipeline (self);

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->appsrc, "push-buffer", buffer, &ret);

  return ret;
}

static GstFlowReturn
kms_http_post_endpoint_end_of_stream_action (KmsHttpPostEndpoint * self)
{
  GstFlowReturn ret;

  KMS_ELEMENT_LOCK (self);

  if (KMS_HTTP_ENDPOINT (self)->pipeline == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Pipeline is not initialized"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->appsrc, "end-of-stream", &ret);

  return ret;
}

static void
kms_http_post_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsHttpPostEndpoint *self = KMS_HTTP_POST_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_USE_ENCODED_MEDIA:
      self->priv->use_encoded_media = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_http_post_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsHttpPostEndpoint *self = KMS_HTTP_POST_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_USE_ENCODED_MEDIA:
      g_value_set_boolean (value, self->priv->use_encoded_media);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_http_post_endpoint_finalize (GObject * object)
{
  KmsHttpPostEndpoint *self = KMS_HTTP_POST_ENDPOINT (object);

  if (self->priv->bus != NULL) {
    if (self->priv->handler_id > 0) {
      g_signal_handler_disconnect (self->priv->bus, self->priv->handler_id);
    }
    gst_bus_remove_signal_watch (self->priv->bus);
    g_object_unref (self->priv->bus);
  }

  G_OBJECT_CLASS (kms_http_post_endpoint_parent_class)->finalize (object);
}

static void
kms_http_post_endpoint_class_init (KmsHttpPostEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->set_property = kms_http_post_endpoint_set_property;
  gobject_class->get_property = kms_http_post_endpoint_get_property;
  gobject_class->finalize = kms_http_post_endpoint_finalize;

  /* Install properties */
  obj_properties[PROP_USE_ENCODED_MEDIA] = g_param_spec_boolean
      ("use-encoded-media", "use encoded media",
      "The element uses encoded media instead of raw media. This mode "
      "could have an unexpected behaviour if keyframes are lost",
      FALSE, G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* set actions */
  http_post_ep_signals[SIGNAL_PUSH_BUFFER] =
      g_signal_new ("push-buffer", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpPostEndpointClass, push_buffer),
      NULL, NULL, __kms_core_marshal_ENUM__BOXED,
      GST_TYPE_FLOW_RETURN, 1, GST_TYPE_BUFFER);

  http_post_ep_signals[SIGNAL_END_OF_STREAM] =
      g_signal_new ("end-of-stream", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpPostEndpointClass, end_of_stream),
      NULL, NULL, __kms_core_marshal_ENUM__VOID,
      GST_TYPE_FLOW_RETURN, 0, G_TYPE_NONE);

  klass->push_buffer = kms_http_post_endpoint_push_buffer_action;
  klass->end_of_stream = kms_http_post_endpoint_end_of_stream_action;

  g_type_class_add_private (klass, sizeof (KmsHttpPostEndpointPrivate));
}

static void
kms_http_post_endpoint_init (KmsHttpPostEndpoint * self)
{
  self->priv = KMS_HTTP_POST_ENDPOINT_GET_PRIVATE (self);
  KMS_HTTP_ENDPOINT (self)->method = KMS_HTTP_ENDPOINT_METHOD_POST;
}

gboolean
kms_http_post_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_POST_ENDPOINT);
}
