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
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/pbutils/encoding-profile.h>

#include "kms-marshal-commons.h"
#include "kmshttpendpoint.h"
#include "kmsagnosticcaps.h"
#include "kmshttpendpointmethod.h"
#include "kmsrecordingprofile.h"
#include "kms-enumtypes-commons.h"
#include "kmsconfcontroller.h"
#include "kmsutils.h"
#include "kmsloop.h"

#define PLUGIN_NAME "httpendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"
#define BASE_TIME_DATA "base_time_data"

#define GET_PIPELINE "get-pipeline"
#define POST_PIPELINE "post-pipeline"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

#define GST_CAT_DEFAULT kms_http_endpoint_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_HTTP_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_HTTP_ENDPOINT,                   \
    KmsHttpEndpointPrivate                    \
  )                                           \
)

typedef void (*KmsActionFunc) (gpointer user_data);

typedef struct _GetData GetData;
typedef struct _PostData PostData;

struct remove_data
{
  KmsHttpEndpoint *httpep;
  GstElement *element;
};

struct _PostData
{
  GstElement *appsrc;
};

struct _GetData
{
  GstElement *appsink;
  KmsConfController *controller;
};

struct _KmsHttpEndpointPrivate
{
  KmsHttpEndpointMethod method;
  GstElement *pipeline;
  gboolean start;
  gboolean use_encoded_media;
  gboolean use_dvr;
  KmsLoop *loop;
  KmsRecordingProfile profile;
  union
  {
    GetData *get;
    PostData *post;
  };
};

/* Object properties */
enum
{
  PROP_0,
  PROP_DVR,
  PROP_METHOD,
  PROP_START,
  PROP_PROFILE,
  PROP_USE_ENCODED_MEDIA,
  N_PROPERTIES
};

#define DEFAULT_HTTP_ENDPOINT_START FALSE
#define DEFAULT_HTTP_ENDPOINT_LIVE TRUE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

struct config_valve
{
  GstElement *valve;
  const gchar *sinkname;
  const gchar *srcname;
  const gchar *destpadname;
};

struct cb_data
{
  KmsHttpEndpoint *self;
  gboolean start;
};

/* Object signals */
enum
{
  /* signals */
  SIGNAL_EOS,
  SIGNAL_NEW_SAMPLE,

  /* actions */
  SIGNAL_PULL_SAMPLE,
  SIGNAL_PUSH_BUFFER,
  SIGNAL_END_OF_STREAM,
  LAST_SIGNAL
};

static guint http_ep_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEndpoint, kms_http_endpoint,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME,
        0, "debug category for httpendpoint element"));

static void kms_change_internal_pipeline_state (KmsHttpEndpoint *, gboolean);

static void
destroy_cb_data (gpointer data)
{
  struct cb_data *cb_data = data;

  g_object_unref (cb_data->self);
  g_slice_free (struct cb_data, data);
}

static GstFlowReturn
new_sample_emit_signal_handler (GstElement * appsink, gpointer user_data)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (user_data);
  GstFlowReturn ret;

  g_signal_emit (G_OBJECT (self), http_ep_signals[SIGNAL_NEW_SAMPLE], 0, &ret);

  return ret;
}

typedef struct _BaseTimeType
{
  GstClockTime pts;
  GstClockTime dts;
} BaseTimeType;

static void
release_base_time_type (gpointer data)
{
  g_slice_free (BaseTimeType, data);
}

static void
release_gst_clock (gpointer data)
{
  g_slice_free (GstClockTime, data);
}

static GstFlowReturn
new_sample_get_handler (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample = NULL;
  GstBuffer *buffer;
  GstCaps *caps;
  BaseTimeType *base_time;
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (GST_OBJECT_PARENT (appsink));

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample == NULL)
    return GST_FLOW_OK;

  g_object_get (G_OBJECT (appsrc), "caps", &caps, NULL);
  if (caps == NULL) {
    /* Appsrc has not yet caps defined */
    GstPad *sink_pad = gst_element_get_static_pad (appsink, "sink");

    if (sink_pad != NULL) {
      caps = gst_pad_get_current_caps (sink_pad);
      g_object_unref (sink_pad);
    }

    if (caps == NULL) {
      GST_ELEMENT_ERROR (appsrc, CORE, CAPS, ("No caps found for %s",
              GST_ELEMENT_NAME (appsrc)), GST_ERROR_SYSTEM);
      ret = GST_FLOW_ERROR;
      goto end;
    }

    g_object_set (appsrc, "caps", caps, NULL);
  }

  gst_caps_unref (caps);

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);
  buffer = gst_buffer_make_writable (buffer);

  KMS_ELEMENT_LOCK (GST_OBJECT_PARENT (appsink));

  base_time = g_object_get_data (G_OBJECT (appsrc), BASE_TIME_DATA);

  if (base_time == NULL) {
    base_time = g_slice_new0 (BaseTimeType);
    base_time->pts = GST_CLOCK_TIME_NONE;
    base_time->dts = GST_CLOCK_TIME_NONE;
    g_object_set_data_full (G_OBJECT (appsrc), BASE_TIME_DATA, base_time,
        release_base_time_type);
  }

  if (!GST_CLOCK_TIME_IS_VALID (base_time->pts)
      && GST_BUFFER_PTS_IS_VALID (buffer)) {
    base_time->pts = buffer->pts;
    GST_DEBUG_OBJECT (appsrc, "Setting pts base time to: %" G_GUINT64_FORMAT,
        base_time->pts);
  }

  if (!GST_CLOCK_TIME_IS_VALID (base_time->dts)
      && GST_BUFFER_DTS_IS_VALID (buffer)) {
    base_time->dts = buffer->dts;
    GST_DEBUG_OBJECT (appsrc, "Setting dts base time to: %" G_GUINT64_FORMAT,
        base_time->dts);
  }

  if (GST_CLOCK_TIME_IS_VALID (base_time->pts)
      && GST_BUFFER_PTS_IS_VALID (buffer)) {
    buffer->pts -= base_time->pts;
    buffer->dts = buffer->pts;
  } else if (GST_CLOCK_TIME_IS_VALID (base_time->dts)
      && GST_BUFFER_DTS_IS_VALID (buffer)) {
    buffer->dts -= base_time->dts;
    buffer->pts = buffer->dts;
  }

  g_object_set (self->priv->get->controller, "has_data", TRUE, NULL);

  KMS_ELEMENT_UNLOCK (GST_OBJECT_PARENT (appsink));

  GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_LIVE);

  if (GST_BUFFER_FLAG_IS_SET (buffer, GST_BUFFER_FLAG_HEADER))
    GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);

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

  KMS_ELEMENT_LOCK (GST_OBJECT_PARENT (appsrc));

  base_time =
      g_object_get_data (G_OBJECT (GST_OBJECT_PARENT (appsrc)), BASE_TIME_DATA);

  if (base_time == NULL) {
    GstClock *clock;

    clock = gst_element_get_clock (appsrc);
    base_time = g_slice_new0 (GstClockTime);

    g_object_set_data_full (G_OBJECT (GST_OBJECT_PARENT (appsrc)),
        BASE_TIME_DATA, base_time, release_gst_clock);
    *base_time =
        gst_clock_get_time (clock) - gst_element_get_base_time (appsrc);
    g_object_unref (clock);
    GST_DEBUG ("Setting base time to: %" G_GUINT64_FORMAT, *base_time);
  }

  if (GST_BUFFER_PTS_IS_VALID (buffer))
    buffer->pts += *base_time;
  if (GST_BUFFER_DTS_IS_VALID (buffer))
    buffer->dts += *base_time;

  KMS_ELEMENT_UNLOCK (GST_OBJECT_PARENT (appsrc));

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

static void
eos_handler (GstElement * appsink, gpointer user_data)
{
  if (KMS_IS_HTTP_ENDPOINT (user_data)) {
    KmsHttpEndpoint *httep = KMS_HTTP_ENDPOINT (user_data);

    GST_DEBUG ("EOS detected on %s", GST_ELEMENT_NAME (httep));
    g_signal_emit (httep, http_ep_signals[SIGNAL_EOS], 0);
  } else {
    GstElement *appsrc = GST_ELEMENT (user_data);
    GstFlowReturn ret;

    GST_DEBUG ("EOS detected on %s", GST_ELEMENT_NAME (appsink));
    g_signal_emit_by_name (appsrc, "end-of-stream", &ret);
    if (ret != GST_FLOW_OK)
      GST_ERROR ("Could not send EOS to %s", GST_ELEMENT_NAME (appsrc));
  }
}

static void
post_decodebin_pad_added_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndpoint * self)
{
  GstElement *appsrc, *agnosticbin, *appsink;
  GstPad *sinkpad;
  GstCaps *audio_caps, *video_caps;
  GstCaps *src_caps;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_INFO ("pad %" GST_PTR_FORMAT " added", pad);

  /* Create and link appsrc--agnosticbin with proper caps */
  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
  src_caps = gst_pad_query_caps (pad, NULL);
  GST_DEBUG ("caps are %" GST_PTR_FORMAT, src_caps);

  if (gst_caps_can_intersect (audio_caps, src_caps))
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  else if (gst_caps_can_intersect (video_caps, src_caps))
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  else {
    GST_ELEMENT_WARNING (self, CORE, CAPS,
        ("Unsupported media received: %" GST_PTR_FORMAT, src_caps),
        ("Unsupported media received: %" GST_PTR_FORMAT, src_caps));
    goto end;
  }

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", FALSE,
      "min-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME,
      "caps", src_caps, NULL);

  gst_bin_add (GST_BIN (self), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  gst_element_link (appsrc, agnosticbin);

  /* Create appsink and link to pad */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (appsink, "sync", TRUE, "enable-last-sample",
      FALSE, "emit-signals", TRUE, "qos", TRUE, "max-buffers", 1,
      "async", FALSE, NULL);
  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);
  gst_element_sync_state_with_parent (appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  gst_pad_link (pad, sinkpad);
  GST_DEBUG_OBJECT (self, "Linked %s---%s", GST_ELEMENT_NAME (decodebin),
      GST_ELEMENT_NAME (appsink));
  g_object_unref (sinkpad);

  /* Connect new-sample signal to callback */
  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_post_handler),
      appsrc);
  g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
  g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);

end:
  if (src_caps != NULL)
    gst_caps_unref (src_caps);

  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);
}

static void
post_decodebin_pad_removed_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndpoint * self)
{
  GstElement *appsink, *appsrc;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_DEBUG ("pad %" GST_PTR_FORMAT " removed", pad);

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);
  appsrc = g_object_steal_data (G_OBJECT (pad), APPSRC_DATA);

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

  if (appsink == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }

  if (!gst_element_set_locked_state (appsink, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (appsink));

  GST_DEBUG ("Removing appsink %s from %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (self->priv->pipeline));

  gst_element_set_state (appsink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), appsink);
}

static void
bus_message (GstBus * bus, GstMessage * msg, KmsHttpEndpoint * self)
{
  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS)
    g_signal_emit (G_OBJECT (self), http_ep_signals[SIGNAL_EOS], 0);
}

static void
kms_http_endpoint_init_post_pipeline (KmsHttpEndpoint * self)
{
  GstElement *decodebin;
  GstBus *bus;
  GstCaps *deco_caps;

  self->priv->method = KMS_HTTP_ENDPOINT_METHOD_POST;
  self->priv->post = g_slice_new0 (PostData);

  self->priv->pipeline = gst_pipeline_new (POST_PIPELINE);
  self->priv->post->appsrc = gst_element_factory_make ("appsrc", NULL);
  decodebin = gst_element_factory_make ("decodebin", NULL);

  /* configure appsrc */
  g_object_set (G_OBJECT (self->priv->post->appsrc), "is-live", TRUE,
      "do-timestamp", TRUE, "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  /* configure decodebin */
  if (self->priv->use_encoded_media) {
    deco_caps = gst_caps_from_string (KMS_AGNOSTIC_CAPS_CAPS);
    g_object_set (G_OBJECT (decodebin), "caps", deco_caps, NULL);
    gst_caps_unref (deco_caps);
  }

  gst_bin_add_many (GST_BIN (self->priv->pipeline), self->priv->post->appsrc,
      decodebin, NULL);

  gst_element_link (self->priv->post->appsrc, decodebin);

  /* Connect decodebin signals */
  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (post_decodebin_pad_added_handler), self);
  g_signal_connect (decodebin, "pad-removed",
      G_CALLBACK (post_decodebin_pad_removed_handler), self);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_add_signal_watch (bus);
  g_signal_connect (G_OBJECT (bus), "message", G_CALLBACK (bus_message), self);
  g_object_unref (bus);

  /* Set pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);
}

static void
matched_elements_cb (KmsConfController * controller, GstElement * appsink,
    GstElement * appsrc, gpointer httpep)
{
  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_get_handler),
      appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (eos_handler), appsrc);
}

static void
sink_required_cb (KmsConfController * controller, gpointer httpep)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (httpep);

  self->priv->get->appsink = gst_element_factory_make ("appsink", NULL);

  g_object_set (self->priv->get->appsink, "emit-signals", TRUE, "qos", TRUE,
      "max-buffers", 1, "async", FALSE, NULL);
  g_signal_connect (self->priv->get->appsink, "new-sample",
      G_CALLBACK (new_sample_emit_signal_handler), self);
  g_signal_connect (self->priv->get->appsink, "eos", G_CALLBACK (eos_handler),
      self);

  g_object_set (self->priv->get->controller, "sink", self->priv->get->appsink,
      NULL);
}

static void
kms_http_endpoint_init_get_pipeline (KmsHttpEndpoint * self)
{
  self->priv->method = KMS_HTTP_ENDPOINT_METHOD_GET;
  self->priv->get = g_slice_new0 (GetData);

  self->priv->pipeline = gst_pipeline_new (GET_PIPELINE);
  g_object_set (self->priv->pipeline, "async-handling", TRUE, NULL);

  self->priv->get->controller =
      kms_conf_controller_new (KMS_CONF_CONTROLLER_KMS_ELEMENT, self,
      KMS_CONF_CONTROLLER_PIPELINE, self->priv->pipeline,
      KMS_CONF_CONTROLLER_PROFILE, self->priv->profile, NULL);
  g_object_set (G_OBJECT (self->priv->get->controller), "live-DVR",
      self->priv->use_dvr, NULL);

  g_signal_connect (self->priv->get->controller, "matched-elements",
      G_CALLBACK (matched_elements_cb), self);
  g_signal_connect (self->priv->get->controller, "sink-required",
      G_CALLBACK (sink_required_cb), self);
}

static GstSample *
kms_http_endpoint_pull_sample_action (KmsHttpEndpoint * self)
{
  GstSample *sample;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->method != KMS_HTTP_ENDPOINT_METHOD_GET) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Trying to get data from a non-GET HttpEndpoint"), GST_ERROR_SYSTEM);
    return NULL;
  }

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->get->appsink, "pull-sample", &sample);

  return sample;
}

static GstFlowReturn
kms_http_endpoint_push_buffer_action (KmsHttpEndpoint * self,
    GstBuffer * buffer)
{
  GstFlowReturn ret;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->method != KMS_HTTP_ENDPOINT_METHOD_UNDEFINED &&
      self->priv->method != KMS_HTTP_ENDPOINT_METHOD_POST) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Trying to push data in a non-POST HttpEndpoint"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }

  if (self->priv->pipeline == NULL)
    kms_http_endpoint_init_post_pipeline (self);

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->post->appsrc, "push-buffer", buffer, &ret);

  return ret;
}

static GstFlowReturn
kms_http_endpoint_end_of_stream_action (KmsHttpEndpoint * self)
{
  GstFlowReturn ret;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->pipeline == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Pipeline is not initialized"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->post->appsrc, "end-of-stream", &ret);
  return ret;
}

static void
kms_recorder_endpoint_valve_added (KmsHttpEndpoint * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  gboolean initialized = FALSE;

  if (self->priv->method != KMS_HTTP_ENDPOINT_METHOD_UNDEFINED &&
      self->priv->method != KMS_HTTP_ENDPOINT_METHOD_GET) {
    GST_ERROR ("Trying to get data from a non-GET HttpEndpoint");
    return;
  }

  if (self->priv->pipeline == NULL) {
    kms_http_endpoint_init_get_pipeline (self);
    initialized = TRUE;
  }

  kms_conf_controller_link_valve (self->priv->get->controller, valve, sinkname,
      srcname, destpadname);

  if (self->priv->start) {
    if (initialized) {
      /* Force pipeline to change to Playing state */
      self->priv->start = FALSE;
      kms_change_internal_pipeline_state (self, TRUE);
    } else {
      GST_DEBUG ("Setting %" GST_PTR_FORMAT " drop to FALSE", valve);
      kms_utils_set_valve_drop (valve, FALSE);
    }
  }

  /* TODO: Improve this logic */
  /* Drop buffers only if it isn't started */
  kms_utils_set_valve_drop (valve, !self->priv->start);
}

static void
kms_http_endpoint_audio_valve_added (KmsElement * self, GstElement * valve)
{
  kms_recorder_endpoint_valve_added (KMS_HTTP_ENDPOINT (self), valve,
      AUDIO_APPSINK, AUDIO_APPSRC, "audio_%u");
}

static void
kms_http_endpoint_audio_valve_removed (KmsElement * self, GstElement * valve)
{
  KmsHttpEndpoint *httpep = KMS_HTTP_ENDPOINT (self);

  if (httpep->priv->method != KMS_HTTP_ENDPOINT_METHOD_GET)
    return;

  GST_INFO ("TODO: Implement this");
}

static void
kms_http_endpoint_video_valve_added (KmsElement * self, GstElement * valve)
{
  kms_recorder_endpoint_valve_added (KMS_HTTP_ENDPOINT (self), valve,
      VIDEO_APPSINK, VIDEO_APPSRC, "video_%u");
}

static void
kms_http_endpoint_video_valve_removed (KmsElement * self, GstElement * valve)
{
  KmsHttpEndpoint *httpep = KMS_HTTP_ENDPOINT (self);

  if (httpep->priv->method != KMS_HTTP_ENDPOINT_METHOD_GET)
    return;

  GST_INFO ("TODO: Implement this");
}

static void
kms_http_endpoint_dispose_GET (KmsHttpEndpoint * self)
{
  g_clear_object (&self->priv->get->controller);

  if (self->priv->pipeline == NULL)
    return;

  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  g_object_unref (self->priv->pipeline);
  self->priv->pipeline = NULL;
}

static void
kms_http_endpoint_dispose_POST (KmsHttpEndpoint * self)
{
  if (self->priv->pipeline == NULL)
    return;

  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (self->priv->pipeline));
  self->priv->pipeline = NULL;
}

static void
kms_http_endpoint_dispose (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  g_clear_object (&self->priv->loop);

  switch (self->priv->method) {
    case KMS_HTTP_ENDPOINT_METHOD_GET:
      kms_http_endpoint_dispose_GET (self);
      break;
    case KMS_HTTP_ENDPOINT_METHOD_POST:
      kms_http_endpoint_dispose_POST (self);
      break;
    default:
      break;
  }

  /* clean up as possible. May be called multiple times */

  G_OBJECT_CLASS (kms_http_endpoint_parent_class)->dispose (object);
}

static void
kms_http_endpoint_finalize (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  switch (self->priv->method) {
    case KMS_HTTP_ENDPOINT_METHOD_GET:
      g_slice_free (GetData, self->priv->get);
      break;
    case KMS_HTTP_ENDPOINT_METHOD_POST:
      g_slice_free (PostData, self->priv->post);
      break;
    default:
      break;
  }

  /* clean up object here */

  G_OBJECT_CLASS (kms_http_endpoint_parent_class)->finalize (object);
}

static void
kms_change_internal_pipeline_state (KmsHttpEndpoint * self, gboolean start)
{
  GstElement *audio_v, *video_v;

  if (self->priv->pipeline == NULL) {
    GST_WARNING ("Element %s is not initialized", GST_ELEMENT_NAME (self));
    self->priv->start = start;
    return;
  }

  audio_v = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (audio_v != NULL)
    kms_utils_set_valve_drop (audio_v, !start);

  video_v = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (video_v != NULL)
    kms_utils_set_valve_drop (video_v, !start);

  if (start) {
    /* Set pipeline to PLAYING */
    GST_DEBUG ("Setting pipeline to PLAYING");
    if (gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to PLAYING will be asynchronous");
  } else {
    GstElement *audio_src;
    GstElement *video_src;

    /* Set pipeline to READY */
    GST_DEBUG ("Setting pipeline to READY.");
    if (gst_element_set_state (self->priv->pipeline, GST_STATE_READY) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to READY will be asynchronous");

    // Reset base time data
    audio_src =
        gst_bin_get_by_name (GST_BIN (self->priv->pipeline), AUDIO_APPSRC);
    video_src =
        gst_bin_get_by_name (GST_BIN (self->priv->pipeline), VIDEO_APPSRC);

    if (audio_src != NULL) {
      g_object_set_data_full (G_OBJECT (audio_src), BASE_TIME_DATA, NULL, NULL);
      g_object_unref (audio_src);
    }

    if (video_src != NULL) {
      g_object_set_data_full (G_OBJECT (video_src), BASE_TIME_DATA, NULL, NULL);
      g_object_unref (video_src);
    }

    g_object_set_data_full (G_OBJECT (self), BASE_TIME_DATA, NULL, NULL);
  }

  self->priv->start = start;
}

static gboolean
change_state_cb (gpointer user_data)
{
  struct cb_data *tmp_data = (struct cb_data *) user_data;

  KMS_ELEMENT_LOCK (tmp_data->self);

  if (tmp_data->self->priv->start != tmp_data->start)
    kms_change_internal_pipeline_state (tmp_data->self, tmp_data->start);

  KMS_ELEMENT_UNLOCK (tmp_data->self);

  return G_SOURCE_REMOVE;
}

static void
kms_http_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DVR:
      self->priv->use_dvr = g_value_get_boolean (value);
      if (self->priv->method == KMS_HTTP_ENDPOINT_METHOD_GET)
        g_object_set (G_OBJECT (self->priv->get->controller), "live-DVR",
            self->priv->use_dvr, NULL);
      break;
    case PROP_START:{
      if (self->priv->start != g_value_get_boolean (value)) {
        struct cb_data *tmp_data;

        tmp_data = g_slice_new0 (struct cb_data);

        tmp_data->self = g_object_ref (self);
        tmp_data->start = g_value_get_boolean (value);

        kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
            change_state_cb, tmp_data, destroy_cb_data);
      }
      break;
    }
    case PROP_PROFILE:
      self->priv->profile = g_value_get_enum (value);
      if (self->priv->method == KMS_HTTP_ENDPOINT_METHOD_GET)
        g_object_set (G_OBJECT (self->priv->get->controller), "profile",
            self->priv->profile, NULL);
      break;
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
kms_http_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DVR:
      g_value_set_boolean (value, self->priv->use_dvr);
      break;
    case PROP_METHOD:
      g_value_set_enum (value, self->priv->method);
      break;
    case PROP_START:
      g_value_set_boolean (value, self->priv->start);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->priv->profile);
      break;
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
kms_http_endpoint_class_init (KmsHttpEndpointClass * klass)
{
  KmsElementClass *kms_element_class = KMS_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "HttpEndpoint", "Generic", "Kurento http end point plugin",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_http_endpoint_set_property;
  gobject_class->get_property = kms_http_endpoint_get_property;
  gobject_class->dispose = kms_http_endpoint_dispose;
  gobject_class->finalize = kms_http_endpoint_finalize;

  kms_element_class->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_http_endpoint_audio_valve_added);
  kms_element_class->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_http_endpoint_video_valve_added);
  kms_element_class->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_http_endpoint_audio_valve_removed);
  kms_element_class->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_http_endpoint_video_valve_removed);

  /* Install properties */
  obj_properties[PROP_DVR] = g_param_spec_boolean ("live-DVR",
      "Live digital video recorder", "Enables or disbles DVR", FALSE,
      G_PARAM_READWRITE);

  obj_properties[PROP_METHOD] = g_param_spec_enum ("http-method",
      "Http method",
      "Http method used in requests",
      KMS_TYPE_HTTP_ENDPOINT_METHOD,
      KMS_HTTP_ENDPOINT_METHOD_UNDEFINED, G_PARAM_READABLE);

  obj_properties[PROP_START] = g_param_spec_boolean ("start",
      "start media stream",
      "start media stream", DEFAULT_HTTP_ENDPOINT_START, G_PARAM_READWRITE);

  obj_properties[PROP_PROFILE] = g_param_spec_enum ("profile",
      "Recording profile",
      "The profile used for encapsulating the media",
      KMS_TYPE_RECORDING_PROFILE, DEFAULT_RECORDING_PROFILE, G_PARAM_READWRITE);

  obj_properties[PROP_USE_ENCODED_MEDIA] = g_param_spec_boolean
      ("use-encoded-media", "use encoded media",
      "The element uses encoded media instead of raw media. This mode "
      "could have an unexpected behaviour if key frames are lost",
      FALSE, G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* set signals */
  http_ep_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, eos_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  http_ep_signals[SIGNAL_NEW_SAMPLE] =
      g_signal_new ("new-sample", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, new_sample),
      NULL, NULL, __kms_marshal_ENUM__VOID, GST_TYPE_FLOW_RETURN, 0,
      G_TYPE_NONE);

  /* set actions */
  http_ep_signals[SIGNAL_PULL_SAMPLE] =
      g_signal_new ("pull-sample", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, pull_sample),
      NULL, NULL, __kms_marshal_BOXED__VOID, GST_TYPE_SAMPLE, 0, G_TYPE_NONE);

  http_ep_signals[SIGNAL_PUSH_BUFFER] =
      g_signal_new ("push-buffer", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, push_buffer),
      NULL, NULL, __kms_marshal_ENUM__BOXED,
      GST_TYPE_FLOW_RETURN, 1, GST_TYPE_BUFFER);

  http_ep_signals[SIGNAL_END_OF_STREAM] =
      g_signal_new ("end-of-stream", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, end_of_stream),
      NULL, NULL, __kms_marshal_ENUM__VOID,
      GST_TYPE_FLOW_RETURN, 0, G_TYPE_NONE);

  klass->pull_sample = kms_http_endpoint_pull_sample_action;
  klass->push_buffer = kms_http_endpoint_push_buffer_action;
  klass->end_of_stream = kms_http_endpoint_end_of_stream_action;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEndpointPrivate));
}

static void
kms_http_endpoint_init (KmsHttpEndpoint * self)
{
  self->priv = KMS_HTTP_ENDPOINT_GET_PRIVATE (self);

  self->priv->loop = kms_loop_new ();
  self->priv->method = KMS_HTTP_ENDPOINT_METHOD_UNDEFINED;
  self->priv->pipeline = NULL;
  self->priv->start = FALSE;
}

gboolean
kms_http_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_ENDPOINT);
}
