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

#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#define PLUGIN_NAME "playerendpoint"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSRC "video_appsrc"
#define RTSPSRC "rtspsrc"

#define APPSRC_KEY "appsrc-key"
G_DEFINE_QUARK (APPSRC_KEY, appsrc);

#define APPSINK_KEY "appsink-key"
G_DEFINE_QUARK (APPSINK_KEY, appsink);

#define PTS_KEY "pts-key"
G_DEFINE_QUARK (PTS_KEY, pts);

#define NETWORK_CACHE_DEFAULT 2000
#define PORT_RANGE_DEFAULT "0-0"
#define IS_PREROLL TRUE

GST_DEBUG_CATEGORY_STATIC (kms_player_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_player_endpoint_debug_category

#define KMS_PLAYER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PLAYER_ENDPOINT,                   \
    KmsPlayerEndpointPrivate                    \
  )                                             \
)

#define BASE_TIME_LOCK(obj) (                                           \
  g_mutex_lock (&KMS_PLAYER_ENDPOINT(obj)->priv->base_time_mutex)       \
)

#define BASE_TIME_UNLOCK(obj) (                                         \
  g_mutex_unlock (&KMS_PLAYER_ENDPOINT(obj)->priv->base_time_mutex)     \
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
  gint network_cache;
  gchar *port_range;

  GMutex base_time_mutex;
  gboolean reset;
  GstClockTime base_time;
  GstClockTime base_time_preroll;

  KmsPlayerStats stats;
};

enum
{
  PROP_0,
  PROP_USE_ENCODED_MEDIA,
  PROP_VIDEO_DATA,
  PROP_POSITION,
  PROP_NETWORK_CACHE,
  PROP_PORT_RANGE,
  PROP_PIPELINE,
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

G_DEFINE_TYPE_WITH_CODE (KmsPlayerEndpoint, kms_player_endpoint,
    KMS_TYPE_URI_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_player_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for playerendpoint element"));

typedef struct _KmsPtsData
{
  GstClockTime base_time;
  GstClockTime offset_time;

  GstClockTime base_time_preroll;

  GstClockTime last_pts;
  GstClockTime last_pts_orig;
  gboolean pts_handled;
} KmsPtsData;

static void
kms_pts_data_destroy (gpointer data)
{
  g_slice_free (KmsPtsData, data);
}

static KmsPtsData *
kms_pts_data_new ()
{
  KmsPtsData *data;

  data = g_slice_new0 (KmsPtsData);

  data->base_time = GST_CLOCK_TIME_NONE;
  data->offset_time = GST_CLOCK_TIME_NONE;
  data->last_pts = GST_CLOCK_TIME_NONE;
  data->last_pts_orig = GST_CLOCK_TIME_NONE;
  data->pts_handled = FALSE;

  return data;
}

static void
kms_pts_data_reset (KmsPtsData * data)
{
  data->base_time = GST_CLOCK_TIME_NONE;
  data->last_pts_orig = GST_CLOCK_TIME_NONE;
}

static void
kms_player_endpoint_disable_decoding (KmsPlayerEndpoint * self)
{
  /* By setting the caps of the uridecodebin element, with all formats
   * except 'application/x-rtp', what we achieve is that all incoming formats
   * will be passed directly to the media pipeline (as is expected of the
   * 'useEncodedMedia' mode), but incoming RTP streams will still be depayloaded.
   * Passing RTP packets directly to the pipeline, without depayloading,
   * is not supported. */

  GstCaps *deco_caps;

  deco_caps = gst_caps_from_string (KMS_AGNOSTIC_NO_RTP_CAPS);
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
        kms_player_endpoint_disable_decoding (playerendpoint);
      }
      break;
    }
    case PROP_NETWORK_CACHE:
      playerendpoint->priv->network_cache = g_value_get_int (value);
      break;
    case PROP_PORT_RANGE:
      g_free (playerendpoint->priv->port_range);
      playerendpoint->priv->port_range = g_value_dup_string (value);
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
    case PROP_PIPELINE:
      g_value_set_object (value, playerendpoint->priv->pipeline);
      break;
    case PROP_NETWORK_CACHE:
      g_value_set_int (value, playerendpoint->priv->network_cache);
      break;
    case PROP_PORT_RANGE:
      g_value_set_string (value, playerendpoint->priv->port_range);
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
    gst_bus_remove_watch(bus);
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

  g_mutex_clear (&self->priv->base_time_mutex);
  g_clear_object (&self->priv->stats.src);
  kms_list_unref (self->priv->stats.probes);

  g_free (self->priv->port_range);
  self->priv->port_range = NULL;

  G_OBJECT_CLASS (kms_player_endpoint_parent_class)->finalize (object);
}

static GstClockTime
kms_player_endpoint_generate_base_time (KmsPlayerEndpoint * self)
{
  GstClock *clock;
  GstClockTime base_time;

  clock = gst_element_get_clock (GST_ELEMENT (self));
  base_time =
      gst_clock_get_time (clock) -
      gst_element_get_base_time (GST_ELEMENT (self));
  g_object_unref (clock);

  return base_time;
}

static GstClockTime
kms_player_endpoint_get_or_generate_base_time (KmsPlayerEndpoint * self,
    GstClockTime * base_time_in, gboolean is_preroll)
{
  GstClockTime base_time;

  BASE_TIME_LOCK (self);

  if (*base_time_in != GST_CLOCK_TIME_NONE) {
    base_time = *base_time_in;
  } else {
    base_time = kms_player_endpoint_generate_base_time (self);
    *base_time_in = base_time;

    GST_DEBUG_OBJECT (self,
        "Setting base time to: %" GST_TIME_FORMAT ", is preroll: %d",
        GST_TIME_ARGS (base_time), is_preroll);
  }

  BASE_TIME_UNLOCK (self);

  return base_time;
}

static void
kms_player_endpoint_reset_base_time (KmsPlayerEndpoint * self)
{
  BASE_TIME_LOCK (self);
  if (self->priv->reset) {
    self->priv->base_time_preroll = GST_CLOCK_TIME_NONE;
    self->priv->base_time = GST_CLOCK_TIME_NONE;
    self->priv->reset = FALSE;
  }
  BASE_TIME_UNLOCK (self);
}

static void
kms_player_endpoint_mark_reset_base_time (KmsPlayerEndpoint * self)
{
  BASE_TIME_LOCK (self);
  self->priv->reset = TRUE;
  BASE_TIME_UNLOCK (self);
}

static GstStateChangeReturn
kms_player_endpoint_mark_reset_base_time_and_set_state (KmsPlayerEndpoint *
    self, GstState state)
{
  kms_player_endpoint_mark_reset_base_time (self);

  return gst_element_set_state (self->priv->pipeline, state);
}

static GstFlowReturn
process_sample (GstAppSink * appsink, GstAppSrc * appsrc, GstSample * sample,
    gboolean is_preroll)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (GST_ELEMENT_PARENT (appsrc));
  KmsPtsData *pts_data;
  GstBuffer *buffer = NULL;
  GstPad *src, *sink;
  GstClockTime pts_orig, base_time, offset_time;
  GstFlowReturn ret = GST_FLOW_OK;
  gint64 diff;

  if (sample == NULL) {
    GST_ERROR_OBJECT (appsink, "Cannot get sample");
    return GST_FLOW_OK;
  }

  if (gst_sample_get_buffer_list (sample) != NULL) {
    GST_ERROR_OBJECT (appsink, "BufferList not supported");
    g_warning ("BufferList not supported");
  }

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    GST_ERROR_OBJECT (appsink, "Cannot get buffer");
    goto end;
  }

  gst_buffer_ref (buffer);
  buffer = gst_buffer_make_writable (buffer);

  pts_data =
      (KmsPtsData *) g_object_get_qdata (G_OBJECT (appsink), pts_quark ());

  if (!GST_BUFFER_PTS_IS_VALID (buffer) && !GST_BUFFER_DTS_IS_VALID (buffer)) {
    if (pts_data->pts_handled) {
      GST_ERROR_OBJECT (appsink,
          "PTS and DTS are not valid and a previous buffer was handled.");
      ret = GST_FLOW_OK;
      goto end;
    }

    goto push;
  } else if (!GST_BUFFER_PTS_IS_VALID (buffer)) {
    GST_BUFFER_PTS (buffer) = GST_BUFFER_DTS (buffer);
  } else if (!GST_BUFFER_DTS_IS_VALID (buffer)) {
    GST_BUFFER_DTS (buffer) = GST_BUFFER_PTS (buffer);
  }

  pts_data->pts_handled = TRUE;
  pts_orig = GST_BUFFER_PTS (buffer);

  if (is_preroll) {
    GST_DEBUG_OBJECT (appsink, "Preroll: reset base time");

    kms_player_endpoint_reset_base_time (self);
    kms_pts_data_reset (pts_data);
    base_time =
        kms_player_endpoint_get_or_generate_base_time (self,
        &self->priv->base_time_preroll, IS_PREROLL);
    if (pts_data->last_pts != GST_CLOCK_TIME_NONE) {
      /* Ensure that base_time is always greater than the last_pts
       * to avoid setting the same or less PTS for different buffers */
      base_time = MAX (base_time, pts_data->last_pts + GST_MSECOND);
    }

    offset_time = GST_BUFFER_PTS (buffer);
  } else {
    base_time = pts_data->base_time;
    offset_time = pts_data->offset_time;

    if (base_time == GST_CLOCK_TIME_NONE) {
      base_time =
          kms_player_endpoint_get_or_generate_base_time (self,
          &self->priv->base_time, !IS_PREROLL);
      if (pts_data->last_pts != GST_CLOCK_TIME_NONE) {
        /* Ensure that base_time is always greater than the last_pts
         * to avoid setting the same or less PTS for different buffers */
        base_time = MAX (base_time, pts_data->last_pts + GST_MSECOND);
      }

      pts_data->base_time = base_time;
      pts_data->offset_time = offset_time = GST_BUFFER_PTS (buffer);
    }
  }

  if (pts_data->last_pts_orig != GST_CLOCK_TIME_NONE) {
    if (pts_orig < pts_data->last_pts_orig) {
      GST_ERROR_OBJECT (appsink,
          "Non incremental original PTS (last original PTS: %"
          GST_TIME_FORMAT ", original PTS: %" GST_TIME_FORMAT
          ", is preroll: %d). Not pushing",
          GST_TIME_ARGS (pts_data->last_pts_orig), GST_TIME_ARGS (pts_orig),
          is_preroll);
      goto end;
    } else if (pts_orig == pts_data->last_pts_orig) {
      GST_DEBUG_OBJECT (appsink,
          "Original PTS equals last PTS (original PTS: %" GST_TIME_FORMAT
          ", is preroll: %d). Seems to be already pushed.",
          GST_TIME_ARGS (pts_orig), is_preroll);
      goto end;
    }
  }

  diff = base_time - offset_time;
  GST_BUFFER_DTS (buffer) += diff;
  GST_BUFFER_PTS (buffer) += diff;

  // HACK: Change duration 1 to -1 to avoid segmentation fault
  //problems in seeks with some formats
  if (GST_BUFFER_DURATION (buffer) == 1) {
    GST_BUFFER_DURATION (buffer) = GST_CLOCK_TIME_NONE;
  }

  GST_LOG_OBJECT (appsink,
      "Is preroll: %d, buffer: %" GST_PTR_FORMAT ", original pts %"
      GST_TIME_FORMAT, is_preroll, buffer, GST_TIME_ARGS (pts_orig));

  if (pts_data->last_pts != GST_CLOCK_TIME_NONE &&
      GST_BUFFER_PTS (buffer) <= pts_data->last_pts) {
    GST_ERROR_OBJECT (appsink,
        "Non incremental PTS assignment (last PTS: %"
        GST_TIME_FORMAT ", PTS: %" GST_TIME_FORMAT
        ", is preroll: %d). Not pushing", GST_TIME_ARGS (pts_data->last_pts),
        GST_TIME_ARGS (GST_BUFFER_PTS (buffer)), is_preroll);
    goto end;
  }

  pts_data->last_pts = GST_BUFFER_PTS (buffer);
  pts_data->last_pts_orig = pts_orig;

push:
  src = gst_element_get_static_pad (GST_ELEMENT (appsrc), "src");
  sink = gst_pad_get_peer (src);
  g_object_unref (src);

  if (sink != NULL) {
    if (GST_OBJECT_FLAG_IS_SET (sink, GST_PAD_FLAG_EOS)) {
      GST_INFO_OBJECT (sink, "Sending flush events");
      gst_pad_send_event (sink, gst_event_new_flush_start ());
      gst_pad_send_event (sink, gst_event_new_flush_stop (FALSE));
    }
    g_object_unref (sink);
  }

  ret = gst_app_src_push_buffer (appsrc, buffer);
  buffer = NULL;
  if (ret != GST_FLOW_OK) {
    GST_ERROR_OBJECT (appsink,
        "Could not send buffer to '%s'. Cause: %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
  }

end:
  if (buffer != NULL) {
    gst_buffer_unref (buffer);
  }

  if (sample != NULL) {
    gst_sample_unref (sample);
  }

  return ret;
}

static GstFlowReturn
appsink_new_preroll_cb (GstAppSink * appsink, gpointer user_data)
{
  GstSample *sample;

  sample = gst_app_sink_pull_preroll (appsink);

  return process_sample (appsink, GST_APP_SRC (user_data), sample, IS_PREROLL);
}

static GstFlowReturn
appsink_new_sample_cb (GstAppSink * appsink, gpointer user_data)
{
  GstSample *sample;

  sample = gst_app_sink_pull_sample (appsink);

  return process_sample (appsink, GST_APP_SRC (user_data), sample, !IS_PREROLL);
}

static void
appsink_eos_cb (GstAppSink * appsink, gpointer user_data)
{
  GstAppSrc *appsrc = GST_APP_SRC (user_data);
  GstFlowReturn ret;
  GstPad *pad;

  GST_DEBUG_OBJECT (appsink, "Send EOS event to main pipeline (via %s)",
      GST_ELEMENT_NAME (appsrc));
  ret = gst_app_src_end_of_stream (appsrc);
  GST_DEBUG_OBJECT (appsink, "Send EOS return: %s", gst_flow_get_name (ret));

  pad = gst_element_get_static_pad (GST_ELEMENT (appsrc), "src");
  if (pad != NULL) {
    GST_INFO_OBJECT (pad, "Send flush events");

    gst_pad_send_event (pad, gst_event_new_flush_start ());
    gst_pad_send_event (pad, gst_event_new_flush_stop (FALSE));

    g_object_unref (pad);
  }
}

static GstPadProbeReturn
appsrc_query_probe (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstQuery *query = gst_pad_probe_info_get_query (info);
  GstQueryType type = GST_QUERY_TYPE (query);
  GstElement *appsink = GST_ELEMENT (element);

  if (type == GST_QUERY_CAPS || type == GST_QUERY_ACCEPT_CAPS) {
    query = gst_query_make_writable (query);
    // Send query upstream to the uridecodebin
    gst_element_query (appsink, query);
    GST_PAD_PROBE_INFO_DATA (info) = query;
  }

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
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME,
      "emit-signals", FALSE, NULL);

  srcpad = gst_element_get_static_pad (appsrc, "src");
  gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_QUERY_UPSTREAM,
      appsrc_query_probe, appsink, NULL);
  g_object_unref (srcpad);

  gst_bin_add (GST_BIN (self), appsrc);

  if (!gst_element_link (appsrc, agnosticbin)) {
    GST_ERROR ("Cannot link elements: %s to %s", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (agnosticbin));
  }

  gst_element_sync_state_with_parent (appsrc);

  return appsrc;
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
  GstCaps *caps;
  GstElement *agnosticbin = NULL;

  caps = gst_pad_query_caps (pad, NULL);
  if (caps == NULL) {
    return NULL;
  }

  GST_DEBUG_OBJECT (pad, "Prepare for input caps: %" GST_PTR_FORMAT, caps);

  /* TODO: Update latency probe to set valid and media type */
  if (kms_utils_caps_is_audio (caps)) {
    GST_DEBUG_OBJECT (pad, "Detected audio caps");
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
    kms_player_end_point_add_stat_probe (self, pad, KMS_MEDIA_TYPE_AUDIO);
  } else if (kms_utils_caps_is_video (caps)) {
    GST_DEBUG_OBJECT (pad, "Detected video caps");
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
    kms_player_end_point_add_stat_probe (self, pad, KMS_MEDIA_TYPE_VIDEO);
  }

  gst_caps_unref (caps);

  return agnosticbin;
}

static GstPadProbeReturn
appsink_probe_set_appsrc_caps (GstPad * pad, GstPadProbeInfo * info,
    gpointer element)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);
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

  GST_DEBUG_OBJECT (appsrc, "Set new caps: %" GST_PTR_FORMAT, caps);
  g_object_set (G_OBJECT (appsrc), "caps", caps, NULL);

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
appsink_probe_query_appsrc_caps (GstPad * pad, GstPadProbeInfo * info,
    gpointer element)
{
  GstQuery *query = gst_pad_probe_info_get_query (info);
  GstQueryType type = GST_QUERY_TYPE (query);
  GstElement *appsrc = GST_ELEMENT (element);

  if (type == GST_QUERY_CAPS || type == GST_QUERY_ACCEPT_CAPS) {
    query = gst_query_make_writable (query);
    // Send query downstream to the agnosticbin
    gst_element_query (appsrc, query);
    GST_PAD_PROBE_INFO_DATA (info) = query;
  }

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
appsink_event_query_probe (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstPadProbeType type = GST_PAD_PROBE_INFO_TYPE (info);

  if (type & GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM) {
    return appsink_probe_set_appsrc_caps (pad, info, element);
  }
  else if (type & GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM) {
    return appsink_probe_query_appsrc_caps (pad, info, element);
  }

  GST_WARNING_OBJECT (pad, "Probe does nothing");
  return GST_PAD_PROBE_OK;
}

static void
kms_player_endpoint_uridecodebin_pad_added (GstElement * element, GstPad * pad,
    KmsPlayerEndpoint * self)
{
  GstElement *appsink, *appsrc;
  GstElement *agnosticbin;
  GstPad *sinkpad;
  GstPadLinkReturn link_ret;

  GST_DEBUG_OBJECT (pad, "Pad added");

  agnosticbin = kms_player_end_point_get_agnostic_for_pad (self, pad);

  if (agnosticbin != NULL) {
    GstAppSinkCallbacks callbacks;

    /* Create appsink */
    appsink = gst_element_factory_make ("appsink", NULL);
    appsrc = kms_player_end_point_add_appsrc (self, agnosticbin, appsink);

    g_object_set (appsink, "enable-last-sample", FALSE, "emit-signals", FALSE,
        "qos", FALSE, "max-buffers", 1, NULL);

    callbacks.eos = appsink_eos_cb;
    callbacks.new_preroll = appsink_new_preroll_cb;
    callbacks.new_sample = appsink_new_sample_cb;
    gst_app_sink_set_callbacks (GST_APP_SINK (appsink), &callbacks, appsrc,
        NULL);

    g_object_set_qdata_full (G_OBJECT (appsink), pts_quark (),
        kms_pts_data_new (), kms_pts_data_destroy);

    g_object_set_qdata (G_OBJECT (pad), appsink_quark (), appsink);
    g_object_set_qdata (G_OBJECT (pad), appsrc_quark (), appsrc);
  } else {
    GST_WARNING_OBJECT (self, "No supported pad: %" GST_PTR_FORMAT
        ". Connecting it to a fakesink", pad);
    appsink = gst_element_factory_make ("fakesink", NULL);
  }

  g_object_set (appsink, "sync", TRUE, "async", TRUE, NULL);

  sinkpad = gst_element_get_static_pad (appsink, "sink");

  if (agnosticbin != NULL) {
    gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM
        | GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM, appsink_event_query_probe,
        appsrc, NULL);
  }

  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);

  link_ret = gst_pad_link (pad, sinkpad);

  if (GST_PAD_LINK_FAILED (link_ret)) {
    GST_ERROR ("Cannot link elements: %s to %s: %s",
        GST_ELEMENT_NAME (GST_PAD_PARENT (pad)),
        GST_ELEMENT_NAME (GST_PAD_PARENT (sinkpad)),
        gst_pad_link_get_name (link_ret));
  }

  g_object_unref (sinkpad);

  gst_element_sync_state_with_parent (appsink);
}

static void
kms_player_endpoint_uridecodebin_pad_removed (GstElement * element,
    GstPad * pad, KmsPlayerEndpoint * self)
{
  GstElement *appsink, *appsrc;

  GST_DEBUG_OBJECT (pad, "Pad removed");

  if (GST_PAD_IS_SINK (pad))
    return;

  kms_player_end_point_remove_stat_probe (self, pad);

  appsink = g_object_steal_qdata (G_OBJECT (pad), appsink_quark ());
  appsrc = g_object_steal_qdata (G_OBJECT (pad), appsrc_quark ());

  // remove appsrc before appsink to avoid segment fault
  // caused by invalid appsink in appsrc_query_probe
  if (appsrc != NULL) {
    kms_utils_bin_remove (GST_BIN (self), appsrc);
  }

  if (appsink != NULL) {
    kms_utils_bin_remove (GST_BIN (self->priv->pipeline), appsink);
  }
}

static gboolean
kms_player_endpoint_stopped (KmsUriEndpoint * obj, GError ** error)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  GST_DEBUG_OBJECT (self, "Pipeline stopped");

  // Set internal pipeline to NULL state
  kms_player_endpoint_mark_reset_base_time_and_set_state (self, GST_STATE_NULL);

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_STOP);

  return TRUE;
}

static gboolean
kms_player_endpoint_started (KmsUriEndpoint * obj, GError ** error)
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

  return TRUE;
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

  kms_player_endpoint_mark_reset_base_time (self);

  if (!gst_element_send_event (self->priv->pipeline, seek)) {
    GST_WARNING_OBJECT (self, "Seek failed");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_player_endpoint_paused (KmsUriEndpoint * obj, GError ** error)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);
  GstStateChangeReturn ret;

  GST_DEBUG_OBJECT (self, "Pipeline paused");

  /* Set internal pipeline to paused */
  ret =
      kms_player_endpoint_mark_reset_base_time_and_set_state (self,
      GST_STATE_PAUSED);

  // HACK: Get the return and perform a seek if the return is success
  //in order to get async state changes. That hack only should be necessary
  //the first time that paused is called.

  if (ret == GST_STATE_CHANGE_SUCCESS) {
    gint64 position = -1;

    gst_element_query_position (self->priv->pipeline,
        GST_FORMAT_TIME, &position);
    kms_player_endpoint_set_position (self, position);
    kms_player_endpoint_mark_reset_base_time_and_set_state (self,
        GST_STATE_PAUSED);
  }

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_PAUSE);

  return TRUE;
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
          "could have an unexpected behaviour if keyframes are lost",
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
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY));

  g_object_class_install_property (gobject_class, PROP_PORT_RANGE,
      g_param_spec_string ("port-range", "UDP port range for RTSP client",
          "Range of ports that can be allocated when acting as RTSP client, "
          "eg. '3000-3005' ('0-0' = no restrictions)", PORT_RANGE_DEFAULT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PIPELINE,
      g_param_spec_object ("pipeline", "Internal pipeline",
          "PlayerEndpoint's private pipeline",
          GST_TYPE_ELEMENT, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

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
  GST_DEBUG ("Emit 'EOS' signal and stop endpoint");
  kms_player_endpoint_stopped (KMS_URI_ENDPOINT (data), NULL);
  g_signal_emit (G_OBJECT (data), kms_player_endpoint_signals[SIGNAL_EOS], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_emit_invalid_uri_signal (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_DEBUG ("Emit 'Invalid URI' signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_endpoint_signals[SIGNAL_INVALID_URI], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_emit_invalid_media_signal (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_DEBUG ("Emit 'Invalid Media' signal");
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
  data->message = gst_message_new_custom (GST_MESSAGE_TYPE (message),
      (GST_OBJECT (self)),
      gst_structure_copy (gst_message_get_structure (message)));

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

  gboolean ok = gst_element_post_message (GST_ELEMENT (data->self),
      gst_message_ref (data->message));

  if (!ok) {
    GST_ERROR ("gst_element_post_message() FAILED");
    gst_message_unref (data->message);
  }

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

      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_post_media_error, data, delete_error_data);
    }
  }
  return GST_BUS_PASS;
}

static void
kms_player_endpoint_uridecodebin_source_setup (GstElement * uridecodebin,
    GstElement * source, KmsPlayerEndpoint * self)
{
  GstPad *srcpad;

  srcpad = gst_element_get_static_pad (source, "src");

  if (srcpad == NULL) {
    GST_WARNING_OBJECT (self, "Skip setting latency probe, no src pad in %"
        GST_PTR_FORMAT " (%s)", uridecodebin, G_OBJECT_TYPE_NAME (source));

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
kms_player_endpoint_uridecodebin_element_added (GstBin * bin,
    GstElement * element, gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  if (g_strcmp0 (gst_plugin_feature_get_name (GST_PLUGIN_FEATURE
              (gst_element_get_factory (element))), RTSPSRC) == 0) {
    g_object_set (G_OBJECT (element),
        "latency", self->priv->network_cache,
        "drop-on-latency", TRUE,
        "port-range", self->priv->port_range,
        NULL);
  }
}

static gboolean
process_bus_message (GstBus * bus, GstMessage * msg, KmsPlayerEndpoint * self)
{
  GstDebugLevel log_level = GST_LEVEL_NONE;
  GError *err = NULL;
  gchar *dbg_info = NULL;

  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:
      log_level = GST_LEVEL_ERROR;
      gst_message_parse_error (msg, &err, &dbg_info);
      break;
    case GST_MESSAGE_WARNING:
      log_level = GST_LEVEL_WARNING;
      gst_message_parse_warning (msg, &err, &dbg_info);
      break;
    default:
      return TRUE;
      break;
  }

  GstElement *parent = self->priv->pipeline;
  gint err_code = 0;
  gchar *err_msg = NULL;

  if (err != NULL) {
    err_code = err->code;
    err_msg = err->message;
  }

  GST_CAT_LEVEL_LOG (GST_CAT_DEFAULT, log_level, self,
      "Error code %d: '%s', element: %s, parent: %s", err_code,
      GST_STR_NULL (err_msg), GST_MESSAGE_SRC_NAME (msg),
      GST_ELEMENT_NAME (parent));

  GST_CAT_LEVEL_LOG (GST_CAT_DEFAULT, log_level, self, "Debugging info: %s",
      GST_STR_NULL (dbg_info));

  gchar *dot_name = g_strdup_printf ("%s_bus_%d", GST_OBJECT_NAME (self),
      err_code);
  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (parent), GST_DEBUG_GRAPH_SHOW_ALL,
      dot_name);
  g_free (dot_name);

  g_error_free (err);
  g_free (dbg_info);

  return TRUE;
}

static void
kms_player_endpoint_init (KmsPlayerEndpoint * self)
{
  GstBus *bus;

  self->priv = KMS_PLAYER_ENDPOINT_GET_PRIVATE (self);

  g_mutex_init (&self->priv->base_time_mutex);
  self->priv->base_time = GST_CLOCK_TIME_NONE;
  self->priv->base_time_preroll = GST_CLOCK_TIME_NONE;

  self->priv->loop = kms_loop_new ();
  self->priv->pipeline = gst_pipeline_new ("internalpipeline");
  self->priv->uridecodebin =
      gst_element_factory_make ("uridecodebin", NULL);
  self->priv->network_cache = NETWORK_CACHE_DEFAULT;
  self->priv->port_range = g_strdup (PORT_RANGE_DEFAULT);

  self->priv->stats.probes = kms_list_new_full (g_direct_equal, g_object_unref,
      (GDestroyNotify) kms_stats_probe_destroy);

  /* Connect to signals */
  g_signal_connect (self->priv->uridecodebin, "pad-added",
      G_CALLBACK (kms_player_endpoint_uridecodebin_pad_added), self);
  g_signal_connect (self->priv->uridecodebin, "pad-removed",
      G_CALLBACK (kms_player_endpoint_uridecodebin_pad_removed), self);
  g_signal_connect (self->priv->uridecodebin, "source-setup",
      G_CALLBACK (kms_player_endpoint_uridecodebin_source_setup), self);
  g_signal_connect (self->priv->uridecodebin, "element-added",
      G_CALLBACK (kms_player_endpoint_uridecodebin_element_added), self);

  /* Eat all async messages such as buffering messages */
  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_add_watch (bus, (GstBusFunc) process_bus_message, self);

  g_object_set (self->priv->uridecodebin, "download", TRUE, NULL);

  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->uridecodebin);

  gst_bus_set_sync_handler (bus, bus_sync_signal_handler, self, NULL);
  g_object_unref (bus);
}

gboolean
kms_player_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLAYER_ENDPOINT);
}
