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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <commons/kmsstats.h>
#include <commons/kmsutils.h>
#include <commons/kmselement.h>
#include <commons/kmsagnosticcaps.h>
#include "kmsplayerendpoint.h"
#include <commons/kmsloop.h>
#include <kms-elements-marshal.h>

#define PLUGIN_NAME "playerendpoint"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSRC "video_appsrc"
#define URIDECODEBIN "uridecodebin"
#define RTSPSRC "rtspsrc"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"

#define NETWORK_CACHE_DEFAULT 2000

GST_DEBUG_CATEGORY_STATIC (kms_player_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_player_endpoint_debug_category

#define KMS_PLAYER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PLAYER_ENDPOINT,                   \
    KmsPlayerEndpointPrivate                    \
  )                                             \
)

typedef void (*KmsActionFunc) (gpointer user_data);

typedef struct _KmsPlayerStats
{
  gboolean enabled;
  GstElement *src;
  gulong meta_id;
  KmsList *probes;              /* <Gstpad, KmsStatsProbe> */
} KmsPlayerStats;

struct _KmsPlayerEndpointPrivate
{
  GstElement *pipeline;
  GstElement *uridecodebin;
  KmsLoop *loop;
  gboolean use_encoded_media;
  GMutex base_time_lock;
  gint network_cache;

  KmsPlayerStats stats;
};

enum
{
  PROP_0,
  PROP_USE_ENCODED_MEDIA,
  PROP_VIDEO_DATA,
  PROP_POSITION,
  PROP_NETWORK_CACHE,
  N_PROPERTIES
};

enum
{
  SIGNAL_EOS,
  SIGNAL_INVALID_URI,
  SIGNAL_INVALID_MEDIA,
  SIGNAL_SET_POSITION,
  LAST_SIGNAL
};

static guint kms_player_endpoint_signals[LAST_SIGNAL] = { 0 };

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlayerEndpoint, kms_player_endpoint,
    KMS_TYPE_URI_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_player_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for playerendpoint element"));

static void
kms_player_endpoint_set_caps (KmsPlayerEndpoint * self)
{
  GstCaps *deco_caps;

  deco_caps = gst_caps_from_string (KMS_AGNOSTIC_CAPS_CAPS);
  g_object_set (G_OBJECT (self->priv->uridecodebin), "caps", deco_caps, NULL);
  gst_caps_unref (deco_caps);
}

void
kms_player_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlayerEndpoint *playerendpoint = KMS_PLAYER_ENDPOINT (object);

  switch (property_id) {
    case PROP_USE_ENCODED_MEDIA:{
      playerendpoint->priv->use_encoded_media = g_value_get_boolean (value);
      if (playerendpoint->priv->use_encoded_media) {
        kms_player_endpoint_set_caps (playerendpoint);
      }
      break;
    }
    case PROP_NETWORK_CACHE:
      playerendpoint->priv->network_cache = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_player_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPlayerEndpoint *playerendpoint = KMS_PLAYER_ENDPOINT (object);

  switch (property_id) {
    case PROP_USE_ENCODED_MEDIA:
      g_value_set_boolean (value, playerendpoint->priv->use_encoded_media);
      break;
    case PROP_VIDEO_DATA:{
      gint64 segment_start = -1;
      gint64 segment_end = -1;
      gint64 duration = -1;
      gboolean seekable = FALSE;
      GstFormat format;
      GstStructure *video_data = NULL;
      GstQuery *query = gst_query_new_seeking (GST_FORMAT_TIME);

      if (gst_element_query (playerendpoint->priv->pipeline, query)) {
        gst_query_parse_seeking (query,
            &format, &seekable, &segment_start, &segment_end);
      } else {
        GST_WARNING_OBJECT (playerendpoint,
            "Impossible to get the file duration");
      }

      gst_query_unref (query);

      if (!gst_element_query_duration (playerendpoint->priv->pipeline,
              GST_FORMAT_TIME, &duration)) {
        GST_WARNING_OBJECT (playerendpoint,
            "Impossible to get the file duration");
      }

      video_data = gst_structure_new ("video_data",
          "isSeekable", G_TYPE_BOOLEAN, seekable,
          "seekableInit", G_TYPE_INT64, segment_start,
          "seekableEnd", G_TYPE_INT64, segment_end,
          "duration", G_TYPE_INT64, duration, NULL);

      g_value_set_boxed (value, video_data);
      break;
    }
    case PROP_POSITION:{
      gint64 position = -1;
      gboolean ret = FALSE;

      if (playerendpoint->priv->pipeline != NULL) {
        ret = gst_element_query_position (playerendpoint->priv->pipeline,
            GST_FORMAT_TIME, &position);
      }

      if (!ret) {
        GST_WARNING_OBJECT (playerendpoint,
            "It is not possible retrieve information about position");
      }

      g_value_set_int64 (value, position);
      break;
    }
    case PROP_NETWORK_CACHE:
      g_value_set_int (value, playerendpoint->priv->network_cache);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

/* This function must be called holding the element mutex */
static void
kms_player_endpoint_disable_latency_probe (KmsPlayerEndpoint * self)
{
  GstPad *pad;

  if (self->priv->stats.src == NULL || self->priv->stats.meta_id == 0UL) {
    return;
  }

  pad = gst_element_get_static_pad (self->priv->stats.src, "src");

  if (pad == NULL) {
    GST_WARNING_OBJECT (self, "No source pad got from %" GST_PTR_FORMAT,
        self->priv->stats.src);
    return;
  }

  gst_pad_remove_probe (pad, self->priv->stats.meta_id);
  self->priv->stats.meta_id = 0UL;

  g_object_unref (pad);
}

/* This function must be called holding the element mutex */
static void
kms_player_endpoint_enable_latency_probe (KmsPlayerEndpoint * self)
{
  GstPad *pad;

  if (self->priv->stats.src == NULL) {
    GST_DEBUG_OBJECT (self, "No source element for stats is yet available");
    return;
  }

  pad = gst_element_get_static_pad (self->priv->stats.src, "src");

  if (pad == NULL) {
    GST_WARNING_OBJECT (self, "No source pad got from %" GST_PTR_FORMAT,
        self->priv->stats.src);
    return;
  }

  if (self->priv->stats.enabled) {
    self->priv->stats.meta_id = kms_stats_add_buffer_latency_meta_probe (pad,
        FALSE, 0);
  }

  g_object_unref (pad);
}

static void
kms_player_endpoint_dispose (GObject * object)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (object);

  g_clear_object (&self->priv->loop);

  if (self->priv->pipeline != NULL) {
    GstBus *bus;

    bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
    gst_bus_set_sync_handler (bus, NULL, NULL, NULL);
    g_object_unref (bus);

    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    gst_object_unref (GST_OBJECT (self->priv->pipeline));
    self->priv->pipeline = NULL;
  }

  /* clean up as possible. May be called multiple times */

  G_OBJECT_CLASS (kms_player_endpoint_parent_class)->dispose (object);
}

static void
kms_player_endpoint_finalize (GObject * object)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_mutex_clear (&self->priv->base_time_lock);
  g_clear_object (&self->priv->stats.src);
  kms_list_unref (self->priv->stats.probes);

  G_OBJECT_CLASS (kms_player_endpoint_parent_class)->finalize (object);
}

static GstFlowReturn
new_preroll_cb (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstPad *src, *sink;

  g_signal_emit_by_name (appsink, "pull-preroll", &sample);

  if (sample == NULL) {
    GST_ERROR_OBJECT (appsink, "Cannot get sample");
    return GST_FLOW_OK;
  }

  buffer = gst_sample_get_buffer (sample);

  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);

  buffer = gst_buffer_make_writable (buffer);

  buffer->pts = GST_CLOCK_TIME_NONE;
  buffer->dts = GST_CLOCK_TIME_NONE;

  // HACK: Change duration 1 to -1 to avoid segmentation fault
  //problems in seeks with some formats
  if (buffer->duration == 1) {
    buffer->duration = GST_CLOCK_TIME_NONE;
  }

  src = gst_element_get_static_pad (appsrc, "src");
  sink = gst_pad_get_peer (src);

  if (sink != NULL) {
    if (GST_OBJECT_FLAG_IS_SET (sink, GST_PAD_FLAG_EOS)) {
      GST_INFO_OBJECT (sink, "Sending flush events");
      gst_pad_send_event (sink, gst_event_new_flush_start ());
      gst_pad_send_event (sink, gst_event_new_flush_stop (FALSE));
    }
    g_object_unref (sink);
  }

  g_object_unref (src);

  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  gst_buffer_unref (buffer);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to appsrc %s. Cause: %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
  }

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static GstFlowReturn
new_sample_cb (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstPad *src, *sink;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);

  if (sample == NULL) {
    GST_ERROR_OBJECT (appsink, "Cannot get sample");
    return GST_FLOW_OK;
  }

  buffer = gst_sample_get_buffer (sample);

  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);

  buffer = gst_buffer_make_writable (buffer);

  buffer->pts = GST_CLOCK_TIME_NONE;
  buffer->dts = GST_CLOCK_TIME_NONE;

  // HACK: Change duration 1 to -1 to avoid segmentation fault
  //problems in seeks with some formats
  if (buffer->duration == 1) {
    buffer->duration = GST_CLOCK_TIME_NONE;
  }

  src = gst_element_get_static_pad (appsrc, "src");
  sink = gst_pad_get_peer (src);

  if (sink != NULL) {
    if (GST_OBJECT_FLAG_IS_SET (sink, GST_PAD_FLAG_EOS)) {
      GST_INFO_OBJECT (sink, "Sending flush events");
      gst_pad_send_event (sink, gst_event_new_flush_start ());
      gst_pad_send_event (sink, gst_event_new_flush_stop (FALSE));
    }
    g_object_unref (sink);
  }

  g_object_unref (src);

  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  gst_buffer_unref (buffer);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to appsrc %s. Cause: %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
  }

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static void
eos_cb (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstPad *pad;

  GST_DEBUG_OBJECT (appsrc, "Sending eos event to main pipeline");

  g_signal_emit_by_name (appsrc, "end-of-stream", &ret);

  pad = gst_element_get_static_pad (appsrc, "src");

  if (pad != NULL) {
    gst_pad_send_event (pad, gst_event_new_flush_start ());
    gst_pad_send_event (pad, gst_event_new_flush_stop (0));
    g_object_unref (pad);
  }

  GST_DEBUG_OBJECT (appsrc, "Returned %s", gst_flow_get_name (ret));
}

static GstPadProbeReturn
main_pipeline_probe (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstQuery *query = GST_PAD_PROBE_INFO_QUERY (info);
  GstElement *appsink = GST_ELEMENT (element);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
      break;
    default:
      return GST_PAD_PROBE_OK;
  }

  query = gst_query_make_writable (query);
  gst_element_query (appsink, query);
  GST_PAD_PROBE_INFO_DATA (info) = query;

  return GST_PAD_PROBE_OK;
}

static GstElement *
kms_player_end_point_add_appsrc (KmsPlayerEndpoint * self,
    GstElement * agnosticbin, GstElement * appsink)
{
  GstElement *appsrc = NULL;
  GstPad *srcpad;

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  srcpad = gst_element_get_static_pad (appsrc, "src");
  gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_QUERY_UPSTREAM,
      main_pipeline_probe, appsink, NULL);
  g_object_unref (srcpad);

  gst_bin_add (GST_BIN (self), appsrc);
  if (!gst_element_link (appsrc, agnosticbin)) {
    GST_ERROR ("Could not link %s to element %s", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (agnosticbin));
  }

  gst_element_sync_state_with_parent (appsrc);

  return appsrc;
}

static GstPadProbeReturn
set_appsrc_caps (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstEvent *event = GST_PAD_PROBE_INFO_EVENT (info);
  GstElement *appsrc = GST_ELEMENT (element);
  GstCaps *caps;

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_OK;
  }

  gst_event_parse_caps (event, &caps);
  if (caps == NULL) {
    GST_ERROR_OBJECT (pad, "Invalid caps received");
    return GST_PAD_PROBE_OK;
  }

  GST_DEBUG_OBJECT (appsrc, "Setting caps %" GST_PTR_FORMAT, caps);

  g_object_set (G_OBJECT (appsrc), "caps", caps, NULL);

  return GST_PAD_PROBE_OK;
}

static void
kms_player_end_point_add_stat_probe (KmsPlayerEndpoint * self, GstPad * pad,
    KmsMediaType type)
{
  KmsStatsProbe *probe;

  probe = kms_stats_probe_new (pad, type);

  KMS_ELEMENT_LOCK (self);

  kms_list_prepend (self->priv->stats.probes, g_object_ref (pad), probe);

  if (self->priv->stats.enabled) {
    kms_stats_probe_latency_meta_set_valid (probe, TRUE);
  }

  KMS_ELEMENT_UNLOCK (self);
}

static void
kms_player_end_point_remove_stat_probe (KmsPlayerEndpoint * self, GstPad * pad)
{
  KMS_ELEMENT_LOCK (self);

  kms_list_remove (self->priv->stats.probes, pad);

  KMS_ELEMENT_UNLOCK (self);
}

static GstElement *
kms_player_end_point_get_agnostic_for_pad (KmsPlayerEndpoint * self,
    GstPad * pad)
{
  GstCaps *caps, *audio_caps = NULL, *video_caps = NULL;
  GstElement *agnosticbin = NULL;

  caps = gst_pad_query_caps (pad, NULL);

  if (caps == NULL) {
    return NULL;
  }

  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);

  /* TODO: Update latency probe to set valid and media type */
  if (gst_caps_can_intersect (audio_caps, caps)) {
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
    kms_player_end_point_add_stat_probe (self, pad, KMS_MEDIA_TYPE_AUDIO);
  } else if (gst_caps_can_intersect (video_caps, caps)) {
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
    kms_player_end_point_add_stat_probe (self, pad, KMS_MEDIA_TYPE_VIDEO);
  }

  gst_caps_unref (caps);
  gst_caps_unref (audio_caps);
  gst_caps_unref (video_caps);

  return agnosticbin;
}

static GstPadProbeReturn
negotiate_appsrc_caps (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstQuery *query = GST_PAD_PROBE_INFO_QUERY (info);
  GstElement *appsrc = GST_ELEMENT (element);
  GstPad *srcpad;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
      break;
    default:
      return GST_PAD_PROBE_OK;
  }

  query = gst_query_make_writable (query);
  srcpad = gst_element_get_static_pad (appsrc, "src");
  /* Send query to the agnosticbin */
  gst_pad_peer_query (srcpad, query);
  g_object_unref (srcpad);
  GST_PAD_PROBE_INFO_DATA (info) = query;

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
internal_pipeline_probe (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM) {
    return set_appsrc_caps (pad, info, element);
  } else if (GST_PAD_PROBE_INFO_TYPE (info) &
      GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM) {
    return negotiate_appsrc_caps (pad, info, element);
  } else {
    GST_WARNING_OBJECT (pad, "Probe does nothing");
    return GST_PAD_PROBE_OK;
  }
}

static void
pad_added (GstElement * element, GstPad * pad, KmsPlayerEndpoint * self)
{
  GstElement *appsink, *appsrc;
  GstElement *agnosticbin;
  GstPad *sinkpad;

  GST_DEBUG_OBJECT (pad, "Pad added");

  agnosticbin = kms_player_end_point_get_agnostic_for_pad (self, pad);

  if (agnosticbin != NULL) {
    /* Create appsink */
    appsink = gst_element_factory_make ("appsink", NULL);
    appsrc = kms_player_end_point_add_appsrc (self, agnosticbin, appsink);

    g_object_set (appsink, "enable-last-sample", FALSE, "emit-signals", TRUE,
        "qos", FALSE, "max-buffers", 1, NULL);

    /* Connect new-sample signal to callback */
    g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_cb),
        appsrc);
    g_signal_connect (appsink, "eos", G_CALLBACK (eos_cb), appsrc);
    g_signal_connect (appsink, "new-preroll", G_CALLBACK (new_preroll_cb),
        appsrc);

    g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);
    g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
  } else {
    GST_WARNING_OBJECT (self, "No supported pad: %" GST_PTR_FORMAT
        ". Connecting it to a fakesink", pad);
    appsink = gst_element_factory_make ("fakesink", NULL);
  }

  g_object_set (appsink, "sync", TRUE, "async", TRUE, NULL);

  sinkpad = gst_element_get_static_pad (appsink, "sink");

  if (agnosticbin != NULL) {
    gst_pad_add_probe (sinkpad,
        (GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM |
            GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM), internal_pipeline_probe,
        appsrc, NULL);
  }

  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);
  gst_pad_link (pad, sinkpad);

  g_object_unref (sinkpad);

  gst_element_sync_state_with_parent (appsink);
}

static void
kms_remove_element_from_bin (GstBin * bin, GstElement * element)
{
  GST_DEBUG ("Removing %" GST_PTR_FORMAT " from %" GST_PTR_FORMAT, element,
      bin);

  if (!gst_element_set_locked_state (element, TRUE)) {
    GST_ERROR ("Could not block element %" GST_PTR_FORMAT, element);
  }

  gst_element_set_state (element, GST_STATE_NULL);
  gst_bin_remove (bin, element);
}

static void
pad_removed (GstElement * element, GstPad * pad, KmsPlayerEndpoint * self)
{
  GstElement *appsink, *appsrc;

  GST_DEBUG_OBJECT (pad, "Pad removed");

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_DEBUG ("pad %" GST_PTR_FORMAT " removed", pad);

  kms_player_end_point_remove_stat_probe (self, pad);

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);
  appsrc = g_object_steal_data (G_OBJECT (pad), APPSRC_DATA);

  if (appsink != NULL) {
    kms_remove_element_from_bin (GST_BIN (self->priv->pipeline), appsink);
  }

  if (appsrc != NULL) {
    kms_remove_element_from_bin (GST_BIN (self), appsrc);
  }
}

static void
kms_player_endpoint_stopped (KmsUriEndpoint * obj)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  GST_DEBUG_OBJECT (self, "Pipeline stopped");

  /* Set internal pipeline to NULL */
  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_STOP);
}

static void
kms_player_endpoint_started (KmsUriEndpoint * obj)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  GST_DEBUG_OBJECT (self, "Pipeline started");

  /* Set uri property in uridecodebin */
  g_object_set (G_OBJECT (self->priv->uridecodebin), "uri",
      KMS_URI_ENDPOINT (self)->uri, NULL);

  /* Set internal pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_START);
}

static gboolean
kms_player_endpoint_set_position (KmsPlayerEndpoint * self, gint64 position)
{
  GstQuery *query;
  GstEvent *seek;
  gboolean seekable = FALSE;

  query = gst_query_new_seeking (GST_FORMAT_TIME);
  if (!gst_element_query (self->priv->pipeline, query)) {
    GST_WARNING_OBJECT (self, "File not seekable in format time");
    gst_query_unref (query);
    return FALSE;
  }

  gst_query_parse_seeking (query, NULL, &seekable, NULL, NULL);
  gst_query_unref (query);

  if (!seekable) {
    GST_WARNING_OBJECT (self, "File not seekable");
    return FALSE;
  }

  seek = gst_event_new_seek (1.0, GST_FORMAT_TIME,
      GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_TRICKMODE | GST_SEEK_FLAG_ACCURATE,
      /* start */ GST_SEEK_TYPE_SET, position,
      /* stop */ GST_SEEK_TYPE_SET, GST_CLOCK_TIME_NONE);

  if (!gst_element_send_event (self->priv->pipeline, seek)) {
    GST_WARNING_OBJECT (self, "Seek failed");
    return FALSE;
  }

  return TRUE;
}

static void
kms_player_endpoint_paused (KmsUriEndpoint * obj)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);
  GstStateChangeReturn ret;

  GST_DEBUG_OBJECT (self, "Pipeline paused");

  /* Set internal pipeline to paused */
  ret = gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);

  // HACK: Get the return and perform a seek if the return is success
  //in order to get async state changes. That hack only should be necessary
  //the first time that paused is called.

  if (ret == GST_STATE_CHANGE_SUCCESS) {
    gint64 position = -1;

    gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);

    gst_element_query_position (self->priv->pipeline,
        GST_FORMAT_TIME, &position);

    kms_player_endpoint_set_position (self, position);

    gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);
  }

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_PAUSE);
}

static void
configure_latency_probes (GstPad * pad, KmsStatsProbe * probe,
    gboolean * enabled)
{
  if (*enabled) {
    kms_stats_probe_latency_meta_set_valid (probe, TRUE);
  } else {
    kms_stats_probe_remove (probe);
  }
}

static void
kms_player_endpoint_update_media_stats (KmsPlayerEndpoint * self)
{
  kms_list_foreach (self->priv->stats.probes, (GHFunc) configure_latency_probes,
      &self->priv->stats.enabled);

  if (self->priv->stats.enabled) {
    kms_player_endpoint_enable_latency_probe (self);
  } else {
    kms_player_endpoint_disable_latency_probe (self);
  }
}

static void
kms_player_endpoint_collect_media_stats (KmsElement * obj, gboolean enable)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  KMS_ELEMENT_LOCK (self);

  self->priv->stats.enabled = enable;
  kms_player_endpoint_update_media_stats (self);

  KMS_ELEMENT_UNLOCK (self);

  KMS_ELEMENT_CLASS
      (kms_player_endpoint_parent_class)->collect_media_stats (obj, enable);
}

static void
kms_player_endpoint_class_init (KmsPlayerEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsElementClass *kms_element_class = KMS_ELEMENT_CLASS (klass);
  KmsUriEndpointClass *urienpoint_class = KMS_URI_ENDPOINT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlayerEndpoint", "Sink/Generic", "Kurento plugin player end point",
      "Joaquin Mengual García <kini.mengual@gmail.com>");

  gobject_class->dispose = kms_player_endpoint_dispose;
  gobject_class->finalize = kms_player_endpoint_finalize;

  gobject_class->set_property = kms_player_endpoint_set_property;
  gobject_class->get_property = kms_player_endpoint_get_property;

  urienpoint_class->stopped = kms_player_endpoint_stopped;
  urienpoint_class->started = kms_player_endpoint_started;
  urienpoint_class->paused = kms_player_endpoint_paused;

  kms_element_class->collect_media_stats =
      GST_DEBUG_FUNCPTR (kms_player_endpoint_collect_media_stats);

  klass->set_position = kms_player_endpoint_set_position;

  g_object_class_install_property (gobject_class, PROP_USE_ENCODED_MEDIA,
      g_param_spec_boolean ("use-encoded-media", "use encoded media",
          "The element uses encoded media instead of raw media. This mode "
          "could have an unexpected behaviour if key frames are lost",
          FALSE, G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY));

  g_object_class_install_property (gobject_class, PROP_VIDEO_DATA,
      g_param_spec_boxed ("video-data", "video data",
          "Get video data from played data",
          GST_TYPE_STRUCTURE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_POSITION,
      g_param_spec_int64 ("position", "position",
          "Playing position in the file as miliseconds",
          0, G_MAXINT64, 0, G_PARAM_READABLE | GST_PARAM_MUTABLE_READY));

  g_object_class_install_property (gobject_class, PROP_NETWORK_CACHE,
      g_param_spec_int ("network-cache", "Network cache",
          "When using rtsp sources, the amount of ms to buffer",
          0, G_MAXINT, NETWORK_CACHE_DEFAULT,
          G_PARAM_READABLE | GST_PARAM_MUTABLE_READY));

  kms_player_endpoint_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, eos_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_endpoint_signals[SIGNAL_INVALID_URI] =
      g_signal_new ("invalid-uri",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, invalid_uri_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_endpoint_signals[SIGNAL_INVALID_MEDIA] =
      g_signal_new ("invalid-media",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, invalid_media_signal), NULL,
      NULL, g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_endpoint_signals[SIGNAL_SET_POSITION] =
      g_signal_new ("set-position",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, set_position), NULL, NULL,
      __kms_elements_marshal_BOOLEAN__INT64, G_TYPE_BOOLEAN, 1, G_TYPE_INT64);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlayerEndpointPrivate));
}

static gboolean
kms_player_endpoint_emit_EOS_signal (gpointer data)
{
  GST_DEBUG ("Emit EOS Signal");
  kms_player_endpoint_stopped (KMS_URI_ENDPOINT (data));
  g_signal_emit (G_OBJECT (data), kms_player_endpoint_signals[SIGNAL_EOS], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_emit_invalid_uri_signal (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_DEBUG ("Emit invalid uri signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_endpoint_signals[SIGNAL_INVALID_URI], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_emit_invalid_media_signal (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_DEBUG ("Emit invalid media signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_endpoint_signals[SIGNAL_INVALID_MEDIA], 0);

  return G_SOURCE_REMOVE;
}

typedef struct _ErrorData
{
  KmsPlayerEndpoint *self;
  GstMessage *message;
} ErrorData;

static ErrorData *
create_error_data (KmsPlayerEndpoint * self, GstMessage * message)
{
  ErrorData *data;

  data = g_slice_new (ErrorData);
  data->self = g_object_ref (self);
  data->message = gst_message_ref (message);

  return data;
}

static void
delete_error_data (gpointer d)
{
  ErrorData *data = d;

  g_object_unref (data->self);
  gst_message_unref (data->message);

  g_slice_free (ErrorData, data);
}

static gboolean
kms_player_endpoint_post_media_error (gpointer d)
{
  ErrorData *data = d;

  gst_element_post_message (GST_ELEMENT (data->self),
      gst_message_ref (data->message));

  return G_SOURCE_REMOVE;
}

static GstBusSyncReply
bus_sync_signal_handler (GstBus * bus, GstMessage * msg, gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS) {
    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
        kms_player_endpoint_emit_EOS_signal, g_object_ref (self),
        g_object_unref);
  } else if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {

    if (g_str_has_prefix (GST_OBJECT_NAME (msg->src), "decodebin")) {
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_emit_invalid_media_signal, g_object_ref (self),
          g_object_unref);
    } else if (g_strcmp0 (GST_OBJECT_NAME (msg->src), "source") == 0) {
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_emit_invalid_uri_signal, g_object_ref (self),
          g_object_unref);
    } else {
      ErrorData *data = create_error_data (self, msg);

      GST_ERROR_OBJECT (self, "Error: %" GST_PTR_FORMAT, msg);
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_post_media_error, data, delete_error_data);
    }
  }
  return GST_BUS_PASS;
}

static void
source_setup_cb (GstElement * uridecodebin, GstElement * source,
    KmsPlayerEndpoint * self)
{
  GstPad *srcpad;

  srcpad = gst_element_get_static_pad (source, "src");

  if (srcpad == NULL) {
    GST_WARNING_OBJECT (self, "Can not set latency probe to %" GST_PTR_FORMAT,
        source);
    return;
  }

  KMS_ELEMENT_LOCK (self);

  kms_player_endpoint_disable_latency_probe (self);

  g_clear_object (&self->priv->stats.src);
  self->priv->stats.src = g_object_ref (source);

  kms_player_endpoint_enable_latency_probe (self);

  KMS_ELEMENT_UNLOCK (self);

  g_object_unref (srcpad);
}

static void
element_added (GstBin * bin, GstElement * element, gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  if (g_strcmp0 (gst_plugin_feature_get_name (GST_PLUGIN_FEATURE
              (gst_element_get_factory (element))), RTSPSRC) == 0) {
    g_object_set (G_OBJECT (element), "latency", self->priv->network_cache,
        NULL);
  }
}

static void
kms_player_endpoint_init (KmsPlayerEndpoint * self)
{
  GstBus *bus;

  self->priv = KMS_PLAYER_ENDPOINT_GET_PRIVATE (self);

  g_mutex_init (&self->priv->base_time_lock);

  self->priv->loop = kms_loop_new ();
  self->priv->pipeline = gst_pipeline_new ("pipeline");
  self->priv->uridecodebin =
      gst_element_factory_make ("uridecodebin", URIDECODEBIN);
  self->priv->network_cache = NETWORK_CACHE_DEFAULT;

  self->priv->stats.probes = kms_list_new_full (g_direct_equal, g_object_unref,
      (GDestroyNotify) kms_stats_probe_destroy);

  /* Connect to signals */
  g_signal_connect (self->priv->uridecodebin, "pad-added",
      G_CALLBACK (pad_added), self);
  g_signal_connect (self->priv->uridecodebin, "pad-removed",
      G_CALLBACK (pad_removed), self);
  g_signal_connect (self->priv->uridecodebin, "source-setup",
      G_CALLBACK (source_setup_cb), self);
  g_signal_connect (self->priv->uridecodebin, "element-added",
      G_CALLBACK (element_added), self);

  g_object_set (self->priv->uridecodebin, "download", TRUE, NULL);

  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->uridecodebin);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_set_sync_handler (bus, bus_sync_signal_handler, self, NULL);
  g_object_unref (bus);
}

gboolean
kms_player_endpoint_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLAYER_ENDPOINT);
}
