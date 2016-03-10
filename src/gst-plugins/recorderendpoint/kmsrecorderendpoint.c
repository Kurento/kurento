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

#include <string.h>
#include <gst/gst.h>
#include <gst/pbutils/encoding-profile.h>
#include <sys/stat.h>

#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include <commons/kmsagnosticcaps.h>
#include "kmsrecorderendpoint.h"
#include <commons/kmsuriendpointstate.h>
#include <commons/kmsutils.h>
#include <commons/kmsloop.h>
#include <commons/kmsstats.h>
#include <commons/kmsrecordingprofile.h>
#include <commons/kms-core-enumtypes.h>
#include <commons/constants.h>
#include <commons/kmsrefstruct.h>
#include "kmsbasemediamuxer.h"
#include "kmsavmuxer.h"
#include "kmsksrmuxer.h"

#define PLUGIN_NAME "recorderendpoint"

#define BASE_TIME_DATA "base_time_data"
#define RECORDER_DEFAULT_SUFFIX "_default"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_NONE

#define KMS_PAD_IDENTIFIER_KEY "kms-pad-identifier-key"

GST_DEBUG_CATEGORY_STATIC (kms_recorder_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_recorder_endpoint_debug_category

#define KMS_RECORDER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_RECORDER_ENDPOINT,                   \
    KmsRecorderEndpointPrivate                    \
  )                                               \
)

#define BASE_TIME_LOCK(obj) (                                           \
  g_mutex_lock (&KMS_RECORDER_ENDPOINT(obj)->priv->base_time_lock)      \
)

#define BASE_TIME_UNLOCK(obj) (                                         \
  g_mutex_unlock (&KMS_RECORDER_ENDPOINT(obj)->priv->base_time_lock)    \
)

static void link_sinkpad_cb (GstPad * pad, GstPad * peer, gpointer user_data);
static void unlink_sinkpad_cb (GstPad * pad, GstPad * peer, gpointer user_data);

enum
{
  PROP_0,
  PROP_DVR,
  PROP_PROFILE,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

typedef struct _KmsSinkPadData
{
  KmsElementPadType type;
  gchar *description;
  gchar *name;
  GstPad *sink_target;
  gulong sink_probe;
  gboolean requested;
} KmsSinkPadData;

typedef struct _KmsRecorderStats
{
  gchar *id;
  gboolean enabled;
  /* End-to-end average stream stats */
  GHashTable *avg_e2e;          /* <"pad_name", StreamE2EAvgStat> */
} KmsRecorderStats;

struct _KmsRecorderEndpointPrivate
{
  KmsRecordingProfile profile;
  GstClockTime paused_time;
  GstClockTime paused_start;
  gboolean use_dvr;
  GstTaskPool *pool;
  KmsBaseMediaMuxer *mux;
  GMutex base_time_lock;

  GSList *sink_probes;
  GHashTable *srcs;

  KmsRecorderStats stats;

  KmsUriEndpointState moving_to_state;
  gboolean stopping;
  GSList *pending_pads;

  GHashTable *sink_pad_data;    /* <name, KmsSinkPadData> */
};

typedef struct _MarkBufferProbeData
{
  gchar *id;
  StreamE2EAvgStat *stat;
} MarkBufferProbeData;

typedef struct _DataEvtProbe
{
  GstElement *appsrc;
  KmsRecordingProfile profile;
} DataEvtProbe;

static KmsSinkPadData *
sink_pad_data_new (KmsElementPadType type, const gchar * description,
    const gchar * name, gboolean requested)
{
  KmsSinkPadData *data;

  data = g_slice_new0 (KmsSinkPadData);
  data->type = type;
  data->description = g_strdup (description);
  data->name = g_strdup (name);
  data->requested = requested;

  return data;
}

static void
sink_pad_data_destroy (KmsSinkPadData * data)
{
  g_free (data->name);
  g_free (data->description);

  g_slice_free (KmsSinkPadData, data);
}

static void
data_evt_probe_destroy (DataEvtProbe * data)
{
  g_clear_object (&data->appsrc);
  g_slice_free (DataEvtProbe, data);
}

static DataEvtProbe *
data_evt_probe_new (GstElement * appsrc, KmsRecordingProfile profile)
{
  DataEvtProbe *data;

  data = g_slice_new0 (DataEvtProbe);

  data->appsrc = g_object_ref (appsrc);
  data->profile = profile;

  return data;
}

static MarkBufferProbeData *
mark_buffer_probe_data_new ()
{
  MarkBufferProbeData *data;

  data = g_slice_new0 (MarkBufferProbeData);

  return data;
}

static void
mark_buffer_probe_data_destroy (MarkBufferProbeData * data)
{
  g_free (data->id);
  kms_stats_stream_e2e_avg_stat_unref (data->stat);

  g_slice_free (MarkBufferProbeData, data);
}

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndpoint, kms_recorder_endpoint,
    KMS_TYPE_URI_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

static GstBusSyncReply bus_sync_signal_handler (GstBus * bus, GstMessage * msg,
    gpointer data);

static gchar *
kms_element_get_padname_from_id (KmsRecorderEndpoint * self, const gchar * id)
{
  gchar *objname, *padname = NULL;

  objname = gst_element_get_name (self);

  if (!g_str_has_prefix (id, objname)) {
    goto end;
  }

  padname =
      g_strndup (id + strlen (objname) + 1, strlen (id) - strlen (objname) - 1);

end:
  g_free (objname);

  return padname;
}

static void
send_eos (GstElement * appsrc)
{
  GstFlowReturn ret;

  GST_DEBUG ("Send EOS to %s", GST_ELEMENT_NAME (appsrc));

  ret = gst_app_src_end_of_stream (GST_APP_SRC (appsrc));
  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send EOS to appsrc  %s. Ret code %d",
        GST_ELEMENT_NAME (appsrc), ret);
  }
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

static GstFlowReturn
recv_sample (GstAppSink * appsink, gpointer user_data)
{
  KmsRecorderEndpoint *self =
      KMS_RECORDER_ENDPOINT (GST_OBJECT_PARENT (appsink));
  GstAppSrc *appsrc = GST_APP_SRC (user_data);
  GstFlowReturn ret;
  GstSample *sample;
  GstSegment *segment;
  GstBuffer *buffer;
  BaseTimeType *base_time;
  GstClockTime offset;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample == NULL)
    return GST_FLOW_OK;

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  segment = gst_sample_get_segment (sample);

  KMS_ELEMENT_LOCK (self);
  if (self->priv->moving_to_state != KMS_URI_ENDPOINT_STATE_START) {
    GST_WARNING ("Dropping buffer received in invalid state %" GST_PTR_FORMAT,
        buffer);
    // TODO: Add a flag to discard buffers until keyframe
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);
  buffer = gst_buffer_make_writable (buffer);

  if (GST_BUFFER_PTS_IS_VALID (buffer))
    buffer->pts =
        gst_segment_to_running_time (segment, GST_FORMAT_TIME, buffer->pts);
  if (GST_BUFFER_DTS_IS_VALID (buffer))
    buffer->dts =
        gst_segment_to_running_time (segment, GST_FORMAT_TIME, buffer->dts);

  BASE_TIME_LOCK (self);

  base_time = g_object_get_data (G_OBJECT (self), BASE_TIME_DATA);

  if (base_time == NULL) {
    base_time = g_slice_new0 (BaseTimeType);
    base_time->pts = buffer->pts;
    base_time->dts = buffer->dts;
    GST_DEBUG_OBJECT (appsrc, "Setting pts base time to: %" G_GUINT64_FORMAT,
        base_time->pts);
    g_object_set_data_full (G_OBJECT (self), BASE_TIME_DATA, base_time,
        release_base_time_type);
  }

  if (!GST_CLOCK_TIME_IS_VALID (base_time->pts)
      && GST_BUFFER_PTS_IS_VALID (buffer)) {
    base_time->pts = buffer->pts;
    GST_DEBUG_OBJECT (appsrc, "Setting pts base time to: %" G_GUINT64_FORMAT,
        base_time->pts);
    base_time->dts = buffer->dts;
  }

  if (GST_CLOCK_TIME_IS_VALID (base_time->pts)) {
    if (GST_BUFFER_PTS_IS_VALID (buffer)) {
      offset = base_time->pts + self->priv->paused_time;
      if (buffer->pts > offset) {
        buffer->pts -= offset;
      } else {
        buffer->pts = 0;
      }
    }
  }

  if (GST_CLOCK_TIME_IS_VALID (base_time->dts)) {
    if (GST_BUFFER_DTS_IS_VALID (buffer)) {
      offset = base_time->dts + self->priv->paused_time;
      if (buffer->dts > offset) {
        buffer->dts -= offset;
      } else {
        buffer->dts = 0;
      }
    }
  }

  BASE_TIME_UNLOCK (GST_OBJECT_PARENT (appsink));

  GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_LIVE);

  if (GST_BUFFER_FLAG_IS_SET (buffer, GST_BUFFER_FLAG_HEADER))
    GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);

  ret = gst_app_src_push_buffer (appsrc, buffer);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR_OBJECT (self, "Could not send buffer to appsrc %s. Cause: %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
    ret = GST_FLOW_CUSTOM_SUCCESS;
  }

end:
  KMS_ELEMENT_UNLOCK (self);
  if (sample != NULL) {
    gst_sample_unref (sample);
  }

  return ret;
}

static void
recv_eos (GstAppSink * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);

  send_eos (appsrc);
}

static void
kms_recorder_endpoint_change_state (KmsRecorderEndpoint * self,
    KmsUriEndpointState new_state)
{
  // TODO: check stopping and wait until stopping is set to false
  if (self->priv->stopping) {
    GST_WARNING_OBJECT (self, "Stopping is in progress, waif for it to finish");
  }

  self->priv->moving_to_state = new_state;
}

static void
kms_recorder_endpoint_state_changed (KmsRecorderEndpoint * self,
    KmsUriEndpointState state)
{
  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  if (!self->priv->stopping || state == KMS_URI_ENDPOINT_STATE_STOP) {
    KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
        state);
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
send_eos_cb (gchar * id, GstElement * appsrc, gpointer user_data)
{
  send_eos (appsrc);
}

static void
kms_recorder_endpoint_send_eos_to_appsrcs (KmsRecorderEndpoint * self)
{
  if (g_hash_table_size (self->priv->srcs) == 0) {
    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
    kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_NULL);
    KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
    return;
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
  kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_PLAYING);
  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  g_hash_table_foreach (self->priv->srcs, (GHFunc) send_eos_cb, NULL);
}

static void
kms_recorder_endpoint_dispose (GObject * object)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  if (self->priv->mux != NULL) {
    if (kms_base_media_muxer_get_state (self->priv->mux) != GST_STATE_NULL) {
      GST_ELEMENT_WARNING (self, RESOURCE, BUSY,
          ("Recorder may have buffers to save"),
          ("Disposing recorder when it isn't stopped."));
    }

    kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_NULL);

    if (self->priv->stopping) {
      GST_WARNING_OBJECT (self, "Forcing pending stop operation to finish");
      kms_recorder_endpoint_state_changed (self, KMS_URI_ENDPOINT_STATE_STOP);
      self->priv->stopping = FALSE;
    }
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_mutex_clear (&self->priv->base_time_lock);

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_recorder_endpoint_parent_class)->dispose (object);
}

static void
kms_recorder_endpoint_release_pending_requests (KmsRecorderEndpoint * self)
{
  gst_task_pool_cleanup (self->priv->pool);

  g_clear_object (&self->priv->mux);
  gst_object_unref (self->priv->pool);
}

static void
kms_recorder_endpoint_finalize (GObject * object)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "releasing resources...");

  kms_recorder_endpoint_release_pending_requests (self);
  g_slist_free_full (self->priv->sink_probes,
      (GDestroyNotify) kms_stats_probe_destroy);
  g_hash_table_unref (self->priv->srcs);

  g_hash_table_unref (self->priv->sink_pad_data);
  g_slist_free_full (self->priv->pending_pads, g_free);
  g_hash_table_unref (self->priv->stats.avg_e2e);

  GST_DEBUG_OBJECT (self, "finalized");

  G_OBJECT_CLASS (kms_recorder_endpoint_parent_class)->finalize (object);
}

static void
connect_pad_signals_cb (GstPad * pad, gpointer data)
{
  g_signal_connect (pad, "linked", G_CALLBACK (link_sinkpad_cb), data);
  g_signal_connect (pad, "unlinked", G_CALLBACK (unlink_sinkpad_cb), data);
}

static void
add_mark_data_cb (GstPad * pad, KmsMediaType type, GstClockTimeDiff t,
    KmsList * meta_data, gpointer user_data)
{
  MarkBufferProbeData *data = (MarkBufferProbeData *) user_data;
  StreamE2EAvgStat *stat;

  stat = kms_list_lookup (meta_data, data->id);

  if (stat != NULL) {
    GST_WARNING_OBJECT (pad, "Can not mark buffer for e2e latency. "
        "Already used ID: %s", data->id);
  } else {
    /* add mark data to this meta */
    kms_list_prepend (meta_data, g_strdup (data->id),
        kms_stats_stream_e2e_avg_stat_ref (data->stat));
  }
}

static void
connect_sink_func (const gchar * key, KmsSinkPadData * data,
    KmsRecorderEndpoint * self)
{
  MarkBufferProbeData *markdata;
  StreamE2EAvgStat *stat;
  KmsMediaType type;
  GstPad *sinkpad;
  gchar *id;

  if (gst_pad_is_linked (data->sink_target)) {
    /* Pad was not previously removed */
    return;
  }

  sinkpad = kms_element_connect_sink_target_full (KMS_ELEMENT (self),
      data->sink_target, data->type, data->description, connect_pad_signals_cb,
      self);

  switch (data->type) {
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      type = KMS_MEDIA_TYPE_AUDIO;
      break;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      type = KMS_MEDIA_TYPE_VIDEO;
      break;
    default:
      GST_DEBUG_OBJECT (self, "No e2e stats will be collected for pad type %u",
          data->type);
      return;
  }

  id = kms_stats_create_id_for_pad (GST_ELEMENT (self), sinkpad);

  stat = g_hash_table_lookup (self->priv->stats.avg_e2e, id);

  if (stat == NULL) {
    stat = kms_stats_stream_e2e_avg_stat_new (type);
    g_hash_table_insert (self->priv->stats.avg_e2e, g_strdup (id), stat);
  }

  markdata = mark_buffer_probe_data_new ();
  markdata->id = id;
  markdata->stat = kms_stats_stream_e2e_avg_stat_ref (stat);

  kms_stats_add_buffer_latency_notification_probe (sinkpad, add_mark_data_cb,
      TRUE /* lock the data */ , markdata,
      (GDestroyNotify) mark_buffer_probe_data_destroy);
}

static void
kms_recorder_generate_pads (KmsRecorderEndpoint * self)
{
  g_hash_table_foreach (self->priv->sink_pad_data, (GHFunc) connect_sink_func,
      self);
}

static void
remove_sink_func (const gchar * key, KmsSinkPadData * data,
    KmsRecorderEndpoint * self)
{
  kms_element_remove_sink_by_type_full (KMS_ELEMENT (self), data->type,
      data->description);
}

static void
kms_recorder_endpoint_remove_pads (KmsRecorderEndpoint * self)
{
  g_hash_table_foreach (self->priv->sink_pad_data, (GHFunc) remove_sink_func,
      self);
}

static void
kms_recorder_endpoint_create_parent_directories (KmsRecorderEndpoint * self)
{
  const gchar *uri = KMS_URI_ENDPOINT (self)->uri;
  gchar *protocol = gst_uri_get_protocol (uri);

  if (g_strcmp0 (protocol, "file") == 0) {
    gchar *file = gst_uri_get_location (uri);
    gchar *dir = g_path_get_dirname (file);

    // Try to create directory
    if (g_mkdir_with_parents (dir, ALLPERMS) != 0) {
      GST_WARNING_OBJECT (self, "Directory %s could not be created", dir);
    }

    g_free (file);
    g_free (dir);
  }

  g_free (protocol);
}

static void
kms_recorder_endpoint_stopped (KmsUriEndpoint * obj)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);

  kms_recorder_endpoint_change_state (self, KMS_URI_ENDPOINT_STATE_STOP);

  if (kms_base_media_muxer_get_state (self->priv->mux) >= GST_STATE_PAUSED) {
    self->priv->stopping = TRUE;
    kms_recorder_endpoint_send_eos_to_appsrcs (self);
  }

  kms_recorder_endpoint_remove_pads (self);

  // Reset base time data
  BASE_TIME_LOCK (self);

  g_object_set_data_full (G_OBJECT (self), BASE_TIME_DATA, NULL, NULL);

  self->priv->paused_time = G_GUINT64_CONSTANT (0);
  self->priv->paused_start = GST_CLOCK_TIME_NONE;

  BASE_TIME_UNLOCK (self);

  if (kms_base_media_muxer_get_state (self->priv->mux) < GST_STATE_PAUSED &&
      !self->priv->stopping) {
    KMS_ELEMENT_UNLOCK (self);
    kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_NULL);
    KMS_ELEMENT_LOCK (self);
  }
}

static void
kms_recorder_endpoint_started (KmsUriEndpoint * obj)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);

  kms_recorder_endpoint_create_parent_directories (self);

  kms_recorder_endpoint_change_state (self, KMS_URI_ENDPOINT_STATE_START);

  KMS_ELEMENT_UNLOCK (self);
  /* Set internal pipeline to playing */
  kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_PLAYING);
  KMS_ELEMENT_LOCK (self);

  BASE_TIME_LOCK (self);

  if (GST_CLOCK_TIME_IS_VALID (self->priv->paused_start)) {
    self->priv->paused_time +=
        gst_clock_get_time (kms_base_media_muxer_get_clock (self->priv->mux)) -
        self->priv->paused_start;
    self->priv->paused_start = GST_CLOCK_TIME_NONE;
  }

  BASE_TIME_UNLOCK (self);

  kms_recorder_generate_pads (self);
}

static void
kms_recorder_endpoint_paused (KmsUriEndpoint * obj)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);
  GstClock *clk;

  kms_recorder_endpoint_change_state (self, KMS_URI_ENDPOINT_STATE_PAUSE);

  kms_recorder_endpoint_remove_pads (self);

  KMS_ELEMENT_UNLOCK (self);

  /* Set internal pipeline to GST_STATE_PAUSED */
  kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_PAUSED);

  KMS_ELEMENT_LOCK (self);

  clk = kms_base_media_muxer_get_clock (self->priv->mux);

  if (clk) {
    self->priv->paused_start = gst_clock_get_time (clk);
  }

  kms_recorder_endpoint_state_changed (self, KMS_URI_ENDPOINT_STATE_PAUSE);
}

static void
set_appsrc_caps (GstElement * appsrc, const GstCaps * caps)
{
  GValue framerate = G_VALUE_INIT;
  GstStructure *str;
  GstCaps *srccaps;

  srccaps = gst_caps_copy (caps);

  str = gst_caps_get_structure (srccaps, 0);

  if (str == NULL) {
    GST_ERROR_OBJECT (appsrc,
        "Can not get caps at index 0 from %" GST_PTR_FORMAT, srccaps);
    goto end;
  }

  /* Set variable framerate 0/1 */

  g_value_init (&framerate, GST_TYPE_FRACTION);
  gst_value_set_fraction (&framerate, 0, 1);
  gst_structure_set_value (str, "framerate", &framerate);
  g_value_reset (&framerate);

  GST_DEBUG_OBJECT (appsrc, "Setting source caps %" GST_PTR_FORMAT, srccaps);
  g_object_set (appsrc, "caps", srccaps, NULL);

end:

  gst_caps_unref (srccaps);
}

static void
set_appsink_caps (GstElement * appsink, const GstCaps * caps,
    KmsRecordingProfile profile)
{
  GstStructure *str;
  GstCaps *sinkcaps;

  sinkcaps = gst_caps_copy (caps);

  str = gst_caps_get_structure (sinkcaps, 0);

  if (str == NULL) {
    GST_ERROR_OBJECT (appsink,
        "Can not get caps at index 0 from %" GST_PTR_FORMAT, sinkcaps);
    goto end;
  }

  if (!gst_structure_has_field (str, "framerate")) {
    GST_DEBUG_OBJECT (appsink, "No framerate in caps %" GST_PTR_FORMAT,
        sinkcaps);
  } else {
    GST_DEBUG_OBJECT (appsink, "Removing framerate from caps %" GST_PTR_FORMAT,
        sinkcaps);
    gst_structure_remove_field (str, "framerate");
  }

  switch (profile) {
    case KMS_RECORDING_PROFILE_WEBM:
    case KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY:
      /* Allow renegotiation of width and height because webmmux supports it */
      gst_structure_remove_field (str, "width");
      gst_structure_remove_field (str, "height");
      break;
    default:
      /* No to allow height and width renegotiation */
      break;
  }

  GST_DEBUG_OBJECT (appsink, "Setting sink caps %" GST_PTR_FORMAT, sinkcaps);
  g_object_set (appsink, "caps", sinkcaps, NULL);

end:

  gst_caps_unref (sinkcaps);
}

static GstPadProbeReturn
configure_pipeline_capabilities (GstPad * pad, GstPadProbeInfo * info,
    gpointer user_data)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);
  DataEvtProbe *data = user_data;
  GstElement *appsrc = data->appsrc;
  GstElement *appsink;
  GstCaps *caps;

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS)
    return GST_PAD_PROBE_OK;

  gst_event_parse_caps (event, &caps);

  GST_DEBUG_OBJECT (appsrc, "Processing caps event %" GST_PTR_FORMAT, caps);

  if (gst_caps_get_size (caps) == 0) {
    GST_ERROR_OBJECT (pad, "Invalid event %" GST_PTR_FORMAT, event);

    return GST_PAD_PROBE_OK;
  }

  if (!gst_caps_is_fixed (caps)) {
    GST_WARNING_OBJECT (pad, "Not fixed caps in event %" GST_PTR_FORMAT, event);
  }

  set_appsrc_caps (appsrc, caps);

  appsink = gst_pad_get_parent_element (pad);

  if (appsink) {
    set_appsink_caps (appsink, caps, data->profile);
    g_object_unref (appsink);
  }

  return GST_PAD_PROBE_OK;
}

static void
link_sinkpad_cb (GstPad * pad, GstPad * peer, gpointer user_data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (user_data);
  KmsSinkPadData *sinkdata;
  GstAppSinkCallbacks callbacks;
  GstElement *appsink, *appsrc;
  KmsRecordingProfile profile;
  DataEvtProbe *data;
  KmsMediaType type;
  GstPad *target;
  gchar *id, *key;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));
  if (target == NULL) {
    GST_ERROR_OBJECT (pad, "No target pad set");
    return;
  }

  key = g_object_get_data (G_OBJECT (target), KMS_PAD_IDENTIFIER_KEY);

  if (key == NULL) {
    GST_ERROR_OBJECT (pad, "No identifier assigned");
    g_object_unref (&target);
    return;
  }

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  sinkdata = g_hash_table_lookup (self->priv->sink_pad_data, key);
  if (sinkdata == NULL) {
    GST_ERROR_OBJECT (self, "Invalid pad %" GST_PTR_FORMAT " connected %"
        GST_PTR_FORMAT, pad, peer);
    goto end;
  }

  switch (sinkdata->type) {
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      type = KMS_MEDIA_TYPE_AUDIO;
      break;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      type = KMS_MEDIA_TYPE_VIDEO;
      break;
    default:
      GST_ERROR_OBJECT (self, "Invalid pad %" GST_PTR_FORMAT " connected %"
          GST_PTR_FORMAT, pad, peer);
      goto end;
  }

  profile = self->priv->profile;

  GST_DEBUG_OBJECT (pad, "linked to %" GST_PTR_FORMAT, peer);

  id = gst_pad_get_name (pad);

  appsrc = kms_base_media_muxer_add_src (self->priv->mux, type, id);

  if (appsrc == NULL) {
    GST_ERROR_OBJECT (self, "Can not get appsrc for pad %" GST_PTR_FORMAT, pad);
    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
    g_object_unref (target);
    g_free (id);

    return;
  }

  gst_pad_set_element_private (pad, g_object_ref (appsrc));

  g_hash_table_insert (self->priv->srcs, id, g_object_ref (appsrc));

  if (sinkdata->sink_probe != 0UL) {
    gst_pad_remove_probe (target, sinkdata->sink_probe);
  }

  callbacks.eos = recv_eos;
  callbacks.new_preroll = NULL;
  callbacks.new_sample = recv_sample;

  appsink = gst_pad_get_parent_element (target);
  gst_app_sink_set_callbacks (GST_APP_SINK (appsink), &callbacks, appsrc, NULL);
  g_object_unref (appsink);

  data = data_evt_probe_new (appsrc, profile);
  sinkdata->sink_probe =
      gst_pad_add_probe (target, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      configure_pipeline_capabilities, data,
      (GDestroyNotify) data_evt_probe_destroy);

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_clear_object (&target);
}

static void
unlink_sinkpad_cb (GstPad * pad, GstPad * peer, gpointer user_data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (user_data);
  gchar *id = NULL;
  GstElement *appsrc;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  id = gst_pad_get_name (pad);

  GST_OBJECT_LOCK (pad);
  appsrc = gst_pad_get_element_private (pad);
  gst_pad_set_element_private (pad, NULL);

  if (appsrc) {
    g_object_unref (appsrc);
  }
  GST_OBJECT_UNLOCK (pad);

  if (self->priv->stopping) {
    GST_DEBUG_OBJECT (self, "Stop operation is pending");
    self->priv->pending_pads = g_slist_prepend (self->priv->pending_pads,
        g_strdup (id));
    goto end;
  }

  if (kms_base_media_muxer_remove_src (self->priv->mux, id)) {
    g_hash_table_remove (self->priv->srcs, id);
  }

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_free (id);
}

static void
kms_recorder_endpoint_add_appsink (KmsRecorderEndpoint * self,
    KmsElementPadType type, const gchar * description, const gchar * name,
    gboolean requested)
{
  KmsSinkPadData *data;
  GstElement *appsink;
  GstPad *sinkpad;

  if (g_hash_table_contains (self->priv->sink_pad_data, name)) {
    GST_WARNING_OBJECT (self, "Sink %s already added", name);
    return;
  }

  if (type != KMS_ELEMENT_PAD_TYPE_AUDIO && type != KMS_ELEMENT_PAD_TYPE_VIDEO) {
    GST_WARNING_OBJECT (self, "Unsupported pad type %u", type);
    return;
  }

  appsink = gst_element_factory_make ("appsink", NULL);

  g_object_set (appsink, "emit-signals", FALSE, "async", FALSE,
      "sync", FALSE, "qos", FALSE, NULL);

  gst_bin_add (GST_BIN (self), appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");

  gst_element_sync_state_with_parent (appsink);

  data = sink_pad_data_new (type, description, name, requested);
  data->sink_target = sinkpad;
  g_hash_table_insert (self->priv->sink_pad_data, g_strdup (name), data);
  g_object_set_data_full (G_OBJECT (sinkpad), KMS_PAD_IDENTIFIER_KEY,
      g_strdup (name), g_free);

  g_object_unref (sinkpad);
}

static void
kms_recorder_endpoint_latency_cb (GstPad * pad, KmsMediaType type,
    GstClockTimeDiff t, KmsList * mdata, gpointer user_data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (user_data);
  KmsListIter iter;
  gpointer key, value;
  gchar *name;

  name = gst_element_get_name (self);

  kms_list_iter_init (&iter, mdata);
  while (kms_list_iter_next (&iter, &key, &value)) {
    gchar *id = (gchar *) key;
    StreamE2EAvgStat *stat;

    if (!g_str_has_prefix (id, name)) {
      /* This element did not add this mark to the metada */
      continue;
    }

    stat = (StreamE2EAvgStat *) value;
    stat->avg = KMS_STATS_CALCULATE_LATENCY_AVG (t, stat->avg);
  }
}

static void
kms_recorder_endpoint_enable_media_stats (KmsStatsProbe * sprobe,
    KmsRecorderEndpoint * self)
{
  kms_stats_probe_add_latency (sprobe, kms_recorder_endpoint_latency_cb,
      TRUE /* Lock the data */ , self, NULL);
}

static void
kms_recorder_endpoint_disable_media_stats (KmsStatsProbe * sprobe,
    KmsRecorderEndpoint * self)
{
  kms_stats_probe_remove (sprobe);
}

static void
kms_recorder_endpoint_update_media_stats (KmsRecorderEndpoint * self)
{
  g_slist_foreach (self->priv->sink_probes,
      (GFunc) kms_recorder_endpoint_disable_media_stats, self);

  if (self->priv->stats.enabled) {
    g_slist_foreach (self->priv->sink_probes,
        (GFunc) kms_recorder_endpoint_enable_media_stats, self);
  }
}

static void
kms_recorder_release_pending_pad (gchar * id, KmsRecorderEndpoint * self)
{
  if (kms_base_media_muxer_remove_src (self->priv->mux, id)) {
    g_hash_table_remove (self->priv->srcs, id);
  }
}

static void
kms_recorder_endpoint_on_eos (KmsBaseMediaMuxer * obj, gpointer user_data)
{
  KmsRecorderEndpoint *recorder = KMS_RECORDER_ENDPOINT (user_data);

  GST_DEBUG_OBJECT (recorder,
      "Received EOS in muxing pipeline, setting NULL state");

  kms_base_media_muxer_set_state (recorder->priv->mux, GST_STATE_NULL);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  if (recorder->priv->stopping) {
    GST_WARNING_OBJECT (recorder, "Releasing pending pads");
    g_slist_foreach (recorder->priv->pending_pads,
        (GFunc) kms_recorder_release_pending_pad, recorder);
    g_slist_free_full (recorder->priv->pending_pads, g_free);
    recorder->priv->pending_pads = NULL;
    recorder->priv->stopping = FALSE;
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));
}

static void
kms_recorder_endpoint_on_sink_added (KmsBaseMediaMuxer * obj,
    GstElement * sink, gpointer user_data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (user_data);
  KmsStatsProbe *sprobe;
  GstPad *sinkpad;

  sinkpad = gst_element_get_static_pad (sink, "sink");
  sprobe = kms_stats_probe_new (sinkpad, 0 /* Does not matter media type */ );

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  self->priv->sink_probes = g_slist_append (self->priv->sink_probes, sprobe);

  if (self->priv->stats.enabled) {
    kms_stats_probe_add_latency (sprobe, kms_recorder_endpoint_latency_cb,
        TRUE /* Lock the data */ , self, NULL);
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_recorder_endpoint_create_base_media_muxer (KmsRecorderEndpoint * self)
{
  KmsBaseMediaMuxer *mux;

  if (self->priv->profile == KMS_RECORDING_PROFILE_KSR) {
    mux = KMS_BASE_MEDIA_MUXER (kms_ksr_muxer_new
        (KMS_BASE_MEDIA_MUXER_PROFILE, self->priv->profile,
            KMS_BASE_MEDIA_MUXER_URI, KMS_URI_ENDPOINT (self)->uri, NULL));
  } else {
    mux = KMS_BASE_MEDIA_MUXER (kms_av_muxer_new
        (KMS_BASE_MEDIA_MUXER_PROFILE, self->priv->profile,
            KMS_BASE_MEDIA_MUXER_URI, KMS_URI_ENDPOINT (self)->uri, NULL));
  }

  self->priv->mux = mux;
}

static void
kms_recorder_endpoint_new_media_muxer (KmsRecorderEndpoint * self)
{
  GstBus *bus;

  kms_recorder_endpoint_create_base_media_muxer (self);

  g_signal_connect (self->priv->mux, "on-sink-added",
      G_CALLBACK (kms_recorder_endpoint_on_sink_added), self);

  kms_recorder_endpoint_update_media_stats (self);
  bus = kms_base_media_muxer_get_bus (self->priv->mux);
  gst_bus_set_sync_handler (bus, bus_sync_signal_handler, self, NULL);
  g_object_unref (bus);

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_AUDIO)) {
    kms_recorder_endpoint_add_appsink (self, KMS_ELEMENT_PAD_TYPE_AUDIO, NULL,
        AUDIO_STREAM_NAME RECORDER_DEFAULT_SUFFIX, FALSE);
  }

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_VIDEO)) {
    kms_recorder_endpoint_add_appsink (self, KMS_ELEMENT_PAD_TYPE_VIDEO, NULL,
        VIDEO_STREAM_NAME RECORDER_DEFAULT_SUFFIX, FALSE);
  }
}

static void
kms_recorder_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DVR:
      self->priv->use_dvr = g_value_get_boolean (value);
      break;
    case PROP_PROFILE:{
      if (self->priv->profile == KMS_RECORDING_PROFILE_NONE) {
        self->priv->profile = g_value_get_enum (value);

        if (self->priv->profile != KMS_RECORDING_PROFILE_NONE) {
          kms_recorder_endpoint_new_media_muxer (self);
        }
      } else {
        GST_ERROR_OBJECT (self, "Profile can only be configured once");
      }

      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_recorder_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DVR:
      g_value_set_boolean (value, self->priv->use_dvr);
      break;
    case PROP_PROFILE:{
      g_value_set_enum (value, self->priv->profile);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static GstCaps *
kms_recorder_endpoint_get_caps_from_profile (KmsRecorderEndpoint * self,
    KmsElementPadType type)
{
  GstEncodingContainerProfile *cprof;
  const GList *profiles, *l;
  GstCaps *caps = NULL;

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      cprof =
          kms_recording_profile_create_profile (self->priv->profile, FALSE,
          TRUE);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      cprof =
          kms_recording_profile_create_profile (self->priv->profile, TRUE,
          FALSE);
      break;
    default:
      return NULL;
  }

  profiles = gst_encoding_container_profile_get_profiles (cprof);

  for (l = profiles; l != NULL; l = l->next) {
    GstEncodingProfile *prof = l->data;

    if ((GST_IS_ENCODING_AUDIO_PROFILE (prof) &&
            type == KMS_ELEMENT_PAD_TYPE_AUDIO) ||
        (GST_IS_ENCODING_VIDEO_PROFILE (prof) &&
            type == KMS_ELEMENT_PAD_TYPE_VIDEO)) {
      caps = gst_encoding_profile_get_input_caps (prof);
      break;
    }
  }

  gst_encoding_profile_unref (cprof);
  return caps;
}

static gboolean
kms_recorder_endpoint_query_caps (KmsElement * element, GstPad * pad,
    GstQuery * query)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (element);
  GstCaps *allowed = NULL, *caps = NULL;
  GstCaps *filter, *result, *tcaps;
  GstPad *target;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));
  if (target == NULL) {
    GST_ERROR_OBJECT (pad, "No target pad set");
    return FALSE;
  }

  gst_query_parse_caps (query, &filter);

  switch (kms_element_get_pad_type (element, pad)) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      caps =
          kms_recorder_endpoint_get_caps_from_profile (self,
          KMS_ELEMENT_PAD_TYPE_VIDEO);
      result = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      caps =
          kms_recorder_endpoint_get_caps_from_profile (self,
          KMS_ELEMENT_PAD_TYPE_AUDIO);
      result = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
      break;
    default:
      GST_ERROR_OBJECT (pad, "unknown pad");
      g_object_unref (target);
      return FALSE;
  }

  allowed = gst_pad_get_allowed_caps (target);

  /* make sure we only return results that intersect our padtemplate */
  tcaps = gst_pad_get_pad_template_caps (pad);
  if (tcaps != NULL) {
    /* Update result caps */
    gst_caps_unref (result);

    if (allowed == NULL) {
      result = gst_caps_ref (tcaps);
    } else {
      result = gst_caps_intersect (allowed, tcaps);
    }
    gst_caps_unref (tcaps);
  } else {
    GST_WARNING_OBJECT (pad,
        "Can not get capabilities from pad's template. Using agnostic's' caps");
  }

  if (caps == NULL) {
    GST_ERROR_OBJECT (self, "No caps from profile");
  } else {
    GstElement *appsrc;
    GstPad *srcpad;

    GST_OBJECT_LOCK (pad);
    appsrc = gst_pad_get_element_private (pad);

    if (appsrc == NULL) {
      GstCaps *aux;

      GST_OBJECT_UNLOCK (pad);
      GST_INFO_OBJECT (self, "No appsrc attached to pad %" GST_PTR_FORMAT, pad);

      /* Filter against profile */
      GST_INFO_OBJECT (appsrc, "Using generic profile's caps");
      aux = gst_caps_intersect (caps, result);
      gst_caps_unref (result);
      result = aux;
      goto filter_caps;
    }
    srcpad = gst_element_get_static_pad (appsrc, "src");

    GST_OBJECT_UNLOCK (pad);

    /* Get encodebin's caps filtering by profile */
    tcaps = gst_pad_peer_query_caps (srcpad, caps);
    if (tcaps != NULL) {
      /* Filter against filtered encodebin's caps */
      GstCaps *aux;

      aux = gst_caps_intersect (tcaps, result);
      gst_caps_unref (result);
      gst_caps_unref (tcaps);
      result = aux;
    } else if (caps != NULL) {
      /* Filter against profile */
      GstCaps *aux;

      GST_WARNING_OBJECT (appsrc, "Using generic profile's caps");
      aux = gst_caps_intersect (caps, result);
      gst_caps_unref (result);
      result = aux;
    }

    g_object_unref (srcpad);
  }

filter_caps:

  /* filter against the query filter when needed */
  if (filter != NULL) {
    GstCaps *aux;

    aux = gst_caps_intersect (result, filter);
    gst_caps_unref (result);
    result = aux;
  }

  gst_query_set_caps_result (query, result);
  gst_caps_unref (result);

  if (allowed != NULL)
    gst_caps_unref (allowed);

  if (caps != NULL)
    gst_caps_unref (caps);

  g_object_unref (target);

  return TRUE;
}

static gboolean
kms_recorder_endpoint_query_accept_caps (KmsElement * element, GstPad * pad,
    GstQuery * query)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (element);
  GstCaps *caps, *accept;
  gboolean ret = TRUE;

  switch (kms_element_get_pad_type (element, pad)) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      caps = kms_recorder_endpoint_get_caps_from_profile (self,
          KMS_ELEMENT_PAD_TYPE_VIDEO);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      caps = kms_recorder_endpoint_get_caps_from_profile (self,
          KMS_ELEMENT_PAD_TYPE_AUDIO);
      break;
    default:
      GST_ERROR_OBJECT (pad, "unknown pad");
      return FALSE;
  }

  if (caps == NULL) {
    GST_ERROR_OBJECT (self, "Can not accept caps without profile");
    gst_query_set_accept_caps_result (query, FALSE);
    return TRUE;
  }

  gst_query_parse_accept_caps (query, &accept);

  ret = gst_caps_can_intersect (accept, caps);

  if (ret) {
    GstElement *appsrc;
    GstPad *srcpad;
    gchar *id;

    id = gst_pad_get_name (pad);

    KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

    appsrc = g_hash_table_lookup (self->priv->srcs, id);
    g_free (id);

    if (appsrc == NULL) {
      KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
      GST_DEBUG_OBJECT (self, "No appsrc attached to pad %" GST_PTR_FORMAT,
          pad);
      goto end;
    }
    srcpad = gst_element_get_static_pad (appsrc, "src");

    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

    ret = gst_pad_peer_query_accept_caps (srcpad, accept);
    gst_object_unref (srcpad);
  } else {
    GST_ERROR_OBJECT (self, "Incompatbile caps %" GST_PTR_FORMAT, caps);
  }

end:

  gst_caps_unref (caps);

  gst_query_set_accept_caps_result (query, ret);

  return TRUE;
}

static gboolean
kms_recorder_endpoint_sink_query (KmsElement * self, GstPad * pad,
    GstQuery * query)
{
  gboolean ret;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
      ret = kms_recorder_endpoint_query_caps (self, pad, query);
      break;
    case GST_QUERY_ACCEPT_CAPS:
      ret = kms_recorder_endpoint_query_accept_caps (self, pad, query);
      break;
    default:
      ret =
          KMS_ELEMENT_CLASS (kms_recorder_endpoint_parent_class)->sink_query
          (self, pad, query);
  }

  return ret;
}

static void
kms_recorder_endpoint_collect_media_stats (KmsElement * obj, gboolean enable)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);

  KMS_ELEMENT_LOCK (self);

  self->priv->stats.enabled = enable;
  kms_recorder_endpoint_update_media_stats (self);

  KMS_ELEMENT_UNLOCK (self);

  KMS_ELEMENT_CLASS
      (kms_recorder_endpoint_parent_class)->collect_media_stats (obj, enable);
}

static GstStructure *
kms_element_get_e2e_latency_stats (KmsRecorderEndpoint * self, gchar * selector)
{
  gpointer key, value;
  GHashTableIter iter;
  GstStructure *stats;

  stats = gst_structure_new_empty ("e2e-latencies");

  KMS_ELEMENT_LOCK (self);

  g_hash_table_iter_init (&iter, self->priv->stats.avg_e2e);

  while (g_hash_table_iter_next (&iter, &key, &value)) {
    StreamE2EAvgStat *avg = value;
    GstStructure *pad_latency;
    gchar *padname, *id = key;

    if (selector != NULL && ((g_strcmp0 (selector, AUDIO_STREAM_NAME) == 0 &&
                avg->type != KMS_MEDIA_TYPE_AUDIO) ||
            (g_strcmp0 (selector, VIDEO_STREAM_NAME) == 0 &&
                avg->type != KMS_MEDIA_TYPE_VIDEO))) {
      continue;
    }

    padname = kms_element_get_padname_from_id (self, id);

    if (padname == NULL) {
      GST_WARNING_OBJECT (self, "No pad identified by %s", id);
      continue;
    }

    /* Video and audio latencies are measured in nano seconds. They */
    /* are such an small values so there is no harm in casting them */
    /* to uint64 even we might lose a bit of preccision.            */

    pad_latency = gst_structure_new (padname, "type", G_TYPE_STRING,
        (avg->type ==
            KMS_MEDIA_TYPE_AUDIO) ? AUDIO_STREAM_NAME : VIDEO_STREAM_NAME,
        "avg", G_TYPE_UINT64, (guint64) avg->avg, NULL);

    gst_structure_set (stats, padname, GST_TYPE_STRUCTURE, pad_latency, NULL);
    gst_structure_free (pad_latency);
    g_free (padname);
  }

  KMS_ELEMENT_UNLOCK (self);

  return stats;
}

static GstStructure *
kms_recorder_endpoint_stats (KmsElement * obj, gchar * selector)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);
  GstStructure *stats, *e_stats, *l_stats;

  /* chain up */
  stats =
      KMS_ELEMENT_CLASS (kms_recorder_endpoint_parent_class)->stats (obj,
      selector);

  if (!self->priv->stats.enabled) {
    return stats;
  }

  e_stats = kms_stats_get_element_stats (stats);

  if (e_stats == NULL) {
    return stats;
  }

  l_stats = kms_element_get_e2e_latency_stats (self, selector);

  /* Add end to end latency */
  gst_structure_set (e_stats, "e2e-latencies", GST_TYPE_STRUCTURE, l_stats,
      NULL);
  gst_structure_free (l_stats);

  GST_DEBUG_OBJECT (self, "Stats: %" GST_PTR_FORMAT, stats);

  return stats;
}

static gboolean
kms_recorder_endpoint_request_new_sink_pad (KmsElement * obj,
    KmsElementPadType type, const gchar * description, const gchar * name)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);
  KmsUriEndpointState state;
  KmsSinkPadData *data;
  gboolean ret = FALSE;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  ret = self->priv->profile == KMS_RECORDING_PROFILE_KSR;

  if (!ret) {
    GST_WARNING_OBJECT (self, "KSR profile not configured");
    goto end;
  }

  kms_recorder_endpoint_add_appsink (self, type, description, name, TRUE);
  g_object_get (self, "state", &state, NULL);

  if (state != KMS_URI_ENDPOINT_STATE_START) {
    goto end;
  }

  data = g_hash_table_lookup (self->priv->sink_pad_data, name);
  if (data == NULL) {
    GST_ERROR_OBJECT (self, "Can not create requested pad %s", name);
    goto end;
  }

  connect_sink_func (name, data, self);

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  return ret;
}

static gboolean
kms_recorder_endpoint_release_requested_sink_pad (KmsElement * obj,
    GstPad * pad)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);
  gchar *padname = NULL;
  KmsSinkPadData *data;
  gboolean ret = FALSE;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  ret = self->priv->profile == KMS_RECORDING_PROFILE_KSR;

  if (!ret) {
    goto end;
  }

  padname = gst_pad_get_name (pad);
  data = g_hash_table_lookup (self->priv->sink_pad_data, padname);

  if (data == NULL) {
    GST_ERROR_OBJECT (self, "Can not release requested pad %s", padname);
    goto end;
  }

  if (!data->requested) {
    GST_ERROR_OBJECT (self, "Can not release not requested pad %"
        GST_PTR_FORMAT, pad);
    goto end;
  }

  kms_element_remove_sink_by_type_full (KMS_ELEMENT (self), data->type,
      data->description);
  g_hash_table_remove (self->priv->sink_pad_data, padname);

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_free (padname);

  return ret;
}

static void
kms_recorder_endpoint_class_init (KmsRecorderEndpointClass * klass)
{
  KmsUriEndpointClass *urienpoint_class = KMS_URI_ENDPOINT_CLASS (klass);
  KmsElementClass *kms_element_class;
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "RecorderEndpoint", "Sink/Generic", "Kurento plugin recorder end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->dispose = kms_recorder_endpoint_dispose;
  gobject_class->finalize = kms_recorder_endpoint_finalize;

  urienpoint_class->stopped = kms_recorder_endpoint_stopped;
  urienpoint_class->started = kms_recorder_endpoint_started;
  urienpoint_class->paused = kms_recorder_endpoint_paused;

  kms_element_class = KMS_ELEMENT_CLASS (klass);
  kms_element_class->sink_query =
      GST_DEBUG_FUNCPTR (kms_recorder_endpoint_sink_query);
  kms_element_class->collect_media_stats =
      GST_DEBUG_FUNCPTR (kms_recorder_endpoint_collect_media_stats);
  kms_element_class->stats = GST_DEBUG_FUNCPTR (kms_recorder_endpoint_stats);
  kms_element_class->request_new_sink_pad =
      GST_DEBUG_FUNCPTR (kms_recorder_endpoint_request_new_sink_pad);
  kms_element_class->release_requested_sink_pad =
      GST_DEBUG_FUNCPTR (kms_recorder_endpoint_release_requested_sink_pad);

  gobject_class->set_property =
      GST_DEBUG_FUNCPTR (kms_recorder_endpoint_set_property);
  gobject_class->get_property =
      GST_DEBUG_FUNCPTR (kms_recorder_endpoint_get_property);

  obj_properties[PROP_DVR] = g_param_spec_boolean ("live-DVR",
      "Live digital video recorder", "Enables or disbles DVR", FALSE,
      G_PARAM_READWRITE);

  obj_properties[PROP_PROFILE] = g_param_spec_enum ("profile",
      "Recording profile",
      "The profile used for encapsulating the media",
      KMS_TYPE_RECORDING_PROFILE, DEFAULT_RECORDING_PROFILE, G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsRecorderEndpointPrivate));
}

typedef struct _ErrorData
{
  KmsRecorderEndpoint *self;
  GstMessage *message;
} ErrorData;

static ErrorData *
create_error_data (KmsRecorderEndpoint * self, GstMessage * message)
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

static void
kms_recorder_endpoint_post_error (gpointer d)
{
  ErrorData *data = d;

  gst_element_post_message (GST_ELEMENT (data->self),
      gst_message_ref (data->message));

  delete_error_data (data);
}

static void
kms_recorder_endpoint_on_eos_message (gpointer data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (data);

  kms_recorder_endpoint_on_eos (self->priv->mux, self);
}

static GstBusSyncReply
bus_sync_signal_handler (GstBus * bus, GstMessage * msg, gpointer data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (data);

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {
    ErrorData *data;

    GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (self),
        GST_DEBUG_GRAPH_SHOW_ALL, GST_ELEMENT_NAME (self));
    kms_base_media_muxer_dot_file (self->priv->mux);

    GST_ERROR_OBJECT (self, "Message %" GST_PTR_FORMAT, msg);

    data = create_error_data (self, msg);

    GST_ERROR_OBJECT (self, "Error: %" GST_PTR_FORMAT, msg);

    gst_task_pool_push (self->priv->pool, kms_recorder_endpoint_post_error,
        data, NULL);
  } else if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS) {
    gst_task_pool_push (self->priv->pool, kms_recorder_endpoint_on_eos_message,
        self, NULL);
  } else if ((GST_MESSAGE_TYPE (msg) == GST_MESSAGE_STATE_CHANGED)
      && (GST_OBJECT_CAST (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self->
                  priv->mux)) == GST_MESSAGE_SRC (msg))) {
    GstState new_state, pending;

    gst_message_parse_state_changed (msg, NULL, &new_state, &pending);

    if (pending == GST_STATE_VOID_PENDING || (pending == GST_STATE_NULL
            && new_state == GST_STATE_READY)) {
      GST_DEBUG_OBJECT (self, "Pipeline changed state to %d", new_state);

      switch (new_state) {
        case GST_STATE_PLAYING:
          kms_recorder_endpoint_state_changed (self,
              KMS_URI_ENDPOINT_STATE_START);
          break;
        case GST_STATE_READY:
          kms_recorder_endpoint_state_changed (self,
              KMS_URI_ENDPOINT_STATE_STOP);
          break;
        default:
          GST_DEBUG_OBJECT (self, "Not raising event");
          break;
      }
    }
  }
  return GST_BUS_PASS;
}

static void
kms_recorder_endpoint_init (KmsRecorderEndpoint * self)
{
  GError *err = NULL;

  self->priv = KMS_RECORDER_ENDPOINT_GET_PRIVATE (self);

  g_mutex_init (&self->priv->base_time_lock);

  self->priv->srcs = g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      g_object_unref);

  self->priv->profile = KMS_RECORDING_PROFILE_NONE;

  self->priv->paused_time = G_GUINT64_CONSTANT (0);
  self->priv->paused_start = GST_CLOCK_TIME_NONE;

  self->priv->sink_pad_data = g_hash_table_new_full (g_str_hash, g_str_equal,
      g_free, (GDestroyNotify) sink_pad_data_destroy);

  self->priv->stats.avg_e2e = g_hash_table_new_full (g_str_hash, g_str_equal,
      g_free, (GDestroyNotify) kms_ref_struct_unref);

  self->priv->pool = gst_task_pool_new ();
  gst_task_pool_prepare (self->priv->pool, &err);

  if (G_UNLIKELY (err != NULL)) {
    g_warning ("%s", err->message);
    g_error_free (err);
  }
}

gboolean
kms_recorder_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_ENDPOINT);
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    kmsrecorderendpoint,
    "Kurento recorder endpoint",
    kms_recorder_endpoint_plugin_init, VERSION, "LGPL",
    "Kurento Elements", "http://kurento.com/")
