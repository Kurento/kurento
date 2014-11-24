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

#include <commons/kms-core-marshal.h>
#include "kmshttpendpoint.h"
#include <commons/kmsagnosticcaps.h>
#include "kms-elements-enumtypes.h"
#include <commons/kms-core-enumtypes.h>
#include "kmsconfcontroller.h"
#include "commons/kmsutils.h"
#include <commons/kmsloop.h>

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

#define BASE_TIME_LOCK(obj) (                                           \
  g_mutex_lock (&KMS_HTTP_ENDPOINT(obj)->base_time_lock)          \
)

#define BASE_TIME_UNLOCK(obj) (                                         \
  g_mutex_unlock (&KMS_HTTP_ENDPOINT(obj)->base_time_lock)        \
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
  gboolean use_encoded_media;
  KmsLoop *loop;
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

  /* actions */
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
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (httpep);
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

  data = g_object_get_data (G_OBJECT (pad), APPSRC_DATA);
  if (data != NULL) {
    g_object_set_data (G_OBJECT (data), "caps", caps);
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

  g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
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
      FALSE, "emit-signals", TRUE, "qos", TRUE, "max-buffers", 1,
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

  g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);

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

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);

  if (appsink == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  appsrc = g_object_get_data (G_OBJECT (sinkpad), APPSRC_DATA);
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

  g_atomic_int_set (&self->method, KMS_HTTP_ENDPOINT_METHOD_POST);
  self->priv->post = g_slice_new0 (PostData);

  self->pipeline = gst_pipeline_new (POST_PIPELINE);
  g_object_set (self->pipeline, "async-handling", TRUE, NULL);
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

  gst_bin_add_many (GST_BIN (self->pipeline), self->priv->post->appsrc,
      decodebin, NULL);

  gst_element_link (self->priv->post->appsrc, decodebin);

  /* Connect decodebin signals */
  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (post_decodebin_pad_added_handler), self);
  g_signal_connect (decodebin, "pad-removed",
      G_CALLBACK (post_decodebin_pad_removed_handler), self);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->pipeline));
  gst_bus_add_signal_watch (bus);
  g_signal_connect (G_OBJECT (bus), "message", G_CALLBACK (bus_message), self);
  g_object_unref (bus);

  /* Set pipeline to playing */
  gst_element_set_state (self->pipeline, GST_STATE_PLAYING);
}

static GstFlowReturn
kms_http_endpoint_push_buffer_action (KmsHttpEndpoint * self,
    GstBuffer * buffer)
{
  GstFlowReturn ret;

  if (g_atomic_int_get (&self->method) !=
      KMS_HTTP_ENDPOINT_METHOD_UNDEFINED
      && g_atomic_int_get (&self->method) != KMS_HTTP_ENDPOINT_METHOD_POST) {
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Trying to push data in a non-POST HttpEndpoint"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }

  KMS_ELEMENT_LOCK (self);

  if (self->pipeline == NULL)
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

  if (self->pipeline == NULL) {
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
kms_http_endpoint_dispose_POST (KmsHttpEndpoint * self)
{
  if (self->pipeline == NULL)
    return;

  gst_element_set_state (self->pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (self->pipeline));
  self->pipeline = NULL;
}

static void
kms_http_endpoint_dispose (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  g_clear_object (&self->priv->loop);

  switch (g_atomic_int_get (&self->method)) {
    case KMS_HTTP_ENDPOINT_METHOD_GET:
//      kms_http_endpoint_dispose_GET (self);
      break;
    case KMS_HTTP_ENDPOINT_METHOD_POST:
      kms_http_endpoint_dispose_POST (self);
      break;
    default:
      break;
  }

  g_mutex_clear (&self->base_time_lock);

  /* clean up as possible. May be called multiple times */

  G_OBJECT_CLASS (kms_http_endpoint_parent_class)->dispose (object);
}

static void
kms_http_endpoint_finalize (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  switch (g_atomic_int_get (&self->method)) {
    case KMS_HTTP_ENDPOINT_METHOD_GET:
//      g_slice_free (GetData, self->priv->get);
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

  if (self->pipeline == NULL) {
    GST_WARNING ("Element %s is not initialized", GST_ELEMENT_NAME (self));
    self->start = start;
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
    if (gst_element_set_state (self->pipeline, GST_STATE_PLAYING) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to PLAYING will be asynchronous");
  } else {
    /* Set pipeline to READY */
    GST_DEBUG ("Setting pipeline to READY.");
    if (gst_element_set_state (self->pipeline, GST_STATE_READY) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to READY will be asynchronous");

    // Reset base time data
    BASE_TIME_LOCK (self);
    g_object_set_data_full (G_OBJECT (self), BASE_TIME_DATA, NULL, NULL);
    BASE_TIME_UNLOCK (self);
  }

  self->start = start;
}

static void
kms_http_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DVR:
      self->use_dvr = g_value_get_boolean (value);
      if (g_atomic_int_get (&self->method) == KMS_HTTP_ENDPOINT_METHOD_GET)
        g_object_set (G_OBJECT (self->priv->get->controller), "live-DVR",
            self->use_dvr, NULL);
      break;
    case PROP_START:{

      if (self->start != g_value_get_boolean (value)) {
//        kms_change_internal_pipeline_state (self, g_value_get_boolean (value));
        KMS_HTTP_ENDPOINT_GET_CLASS (self)->start (self,
            g_value_get_boolean (value));
      }
      break;
    }
    case PROP_PROFILE:
      self->profile = g_value_get_enum (value);
      if (g_atomic_int_get (&self->method) == KMS_HTTP_ENDPOINT_METHOD_GET)
        g_object_set (G_OBJECT (self->priv->get->controller), "profile",
            self->profile, NULL);
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
      g_value_set_boolean (value, self->use_dvr);
      break;
    case PROP_METHOD:
      g_value_set_enum (value, g_atomic_int_get (&self->method));
      break;
    case PROP_START:
      g_value_set_boolean (value, self->start);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->profile);
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
kms_http_endpoint_start (KmsHttpEndpoint * self, gboolean start)
{
  GST_WARNING_OBJECT (self, "Not implemented method");
  kms_change_internal_pipeline_state (self, start);
}

static void
kms_http_endpoint_class_init (KmsHttpEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "HttpEndpoint", "Generic", "Kurento http end point plugin",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_http_endpoint_set_property;
  gobject_class->get_property = kms_http_endpoint_get_property;
  gobject_class->dispose = kms_http_endpoint_dispose;
  gobject_class->finalize = kms_http_endpoint_finalize;

  klass->start = GST_DEBUG_FUNCPTR (kms_http_endpoint_start);

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

  /* set actions */
  http_ep_signals[SIGNAL_PUSH_BUFFER] =
      g_signal_new ("push-buffer", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, push_buffer),
      NULL, NULL, __kms_core_marshal_ENUM__BOXED,
      GST_TYPE_FLOW_RETURN, 1, GST_TYPE_BUFFER);

  http_ep_signals[SIGNAL_END_OF_STREAM] =
      g_signal_new ("end-of-stream", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, end_of_stream),
      NULL, NULL, __kms_core_marshal_ENUM__VOID,
      GST_TYPE_FLOW_RETURN, 0, G_TYPE_NONE);

  klass->push_buffer = kms_http_endpoint_push_buffer_action;
  klass->end_of_stream = kms_http_endpoint_end_of_stream_action;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEndpointPrivate));
}

static void
kms_http_endpoint_init (KmsHttpEndpoint * self)
{
  self->priv = KMS_HTTP_ENDPOINT_GET_PRIVATE (self);

  g_mutex_init (&self->base_time_lock);

  g_object_set (self, "do-synchronization", TRUE, NULL);

  self->priv->loop = kms_loop_new ();
  g_atomic_int_set (&self->method, KMS_HTTP_ENDPOINT_METHOD_UNDEFINED);
  self->pipeline = NULL;
  self->start = FALSE;
}

gboolean
kms_http_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_ENDPOINT);
}
