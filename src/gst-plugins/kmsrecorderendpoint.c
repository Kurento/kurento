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

#include "kmsbasemediamuxer.h"
#include "kmsavmuxer.h"
#include "kmsksrmuxer.h"

#define PLUGIN_NAME "recorderendpoint"

#define AUDIO_APPSINK "audio_appsink"

#define VIDEO_APPSINK "video_appsink"

#define BASE_TIME_DATA "base_time_data"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_NONE

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

struct state_controller
{
  GCond cond;
  GMutex mutex;
  guint locked;
  gboolean changing;
};

struct _KmsRecorderEndpointPrivate
{
  KmsRecordingProfile profile;
  GstClockTime paused_time;
  GstClockTime paused_start;
  gboolean use_dvr;
  struct state_controller state_manager;
  GstTaskPool *pool;
  KmsBaseMediaMuxer *mux;
  GMutex base_time_lock;

  GstPad *audio_target;
  GstPad *video_target;

  GSList *sink_probes;
  GHashTable *srcs;

  gboolean stats_enabled;
  gdouble vi;
  gdouble ai;

  gulong audio_probe;
  gulong video_probe;

  gboolean stopping;
  GSList *pending_pads;
};

typedef struct _DataEvtProbe
{
  GstElement *appsrc;
  KmsRecordingProfile profile;
} DataEvtProbe;

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

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndpoint, kms_recorder_endpoint,
    KMS_TYPE_URI_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

static GstBusSyncReply bus_sync_signal_handler (GstBus * bus, GstMessage * msg,
    gpointer data);

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
  KmsUriEndpointState state;
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

  g_object_get (G_OBJECT (self), "state", &state, NULL);
  if (state != KMS_URI_ENDPOINT_STATE_START) {
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
    GST_ERROR ("Could not send buffer to appsrc %s. Cause: %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
  }

end:
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
kms_recorder_endpoint_change_state (KmsRecorderEndpoint * self)
{
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_mutex_lock (&self->priv->state_manager.mutex);
  while (self->priv->state_manager.changing) {
    GST_WARNING ("Change of state is taking place");
    self->priv->state_manager.locked++;
    g_cond_wait (&self->priv->state_manager.cond,
        &self->priv->state_manager.mutex);
    self->priv->state_manager.locked--;
  }

  self->priv->state_manager.changing = TRUE;
  g_mutex_unlock (&self->priv->state_manager.mutex);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
}

static void
kms_recorder_endpoint_state_changed (KmsRecorderEndpoint * self,
    KmsUriEndpointState state)
{
  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      state);
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_mutex_lock (&self->priv->state_manager.mutex);
  self->priv->state_manager.changing = FALSE;
  if (self->priv->state_manager.locked > 0)
    g_cond_broadcast (&self->priv->state_manager.cond);
  g_mutex_unlock (&self->priv->state_manager.mutex);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
}

static void
send_eos_cb (gchar * id, GstElement * appsrc, gpointer user_data)
{
  send_eos (appsrc);
}

static void
kms_recorder_endpoint_send_eos_to_appsrcs (KmsRecorderEndpoint * self)
{
  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  if (g_hash_table_size (self->priv->srcs) == 0) {
    kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_NULL);
    kms_recorder_endpoint_state_changed (self, KMS_URI_ENDPOINT_STATE_STOP);
    goto end;
  }

  kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_PLAYING);

  g_hash_table_foreach (self->priv->srcs, (GHFunc) send_eos_cb, NULL);

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
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
  g_mutex_lock (&self->priv->state_manager.mutex);
  while (self->priv->state_manager.changing ||
      self->priv->state_manager.locked > 0) {
    GST_WARNING ("Waiting to all process blocked");
    self->priv->state_manager.locked++;
    g_cond_wait (&self->priv->state_manager.cond,
        &self->priv->state_manager.mutex);
    self->priv->state_manager.locked--;
  }
  g_mutex_unlock (&self->priv->state_manager.mutex);

  gst_task_pool_cleanup (self->priv->pool);

  g_clear_object (&self->priv->mux);
  gst_object_unref (self->priv->pool);

  g_cond_clear (&self->priv->state_manager.cond);
  g_mutex_clear (&self->priv->state_manager.mutex);
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

  g_slist_free_full (self->priv->pending_pads, g_free);

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
kms_recorder_generate_pads (KmsRecorderEndpoint * self)
{
  KmsElement *elem = KMS_ELEMENT (self);

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_AUDIO)) {
    kms_element_connect_sink_target_full (elem, self->priv->audio_target,
        KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, connect_pad_signals_cb, self);
  }

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_VIDEO)) {
    kms_element_connect_sink_target_full (elem, self->priv->video_target,
        KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, connect_pad_signals_cb, self);
  }
}

static void
kms_recorder_endpoint_remove_pads (KmsRecorderEndpoint * self)
{
  KmsElement *elem = KMS_ELEMENT (self);

  kms_element_remove_sink_by_type (elem, KMS_ELEMENT_PAD_TYPE_AUDIO);
  kms_element_remove_sink_by_type (elem, KMS_ELEMENT_PAD_TYPE_VIDEO);
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

  kms_recorder_endpoint_change_state (self);

  if (kms_base_media_muxer_get_state (self->priv->mux) >= GST_STATE_PAUSED) {
    kms_recorder_endpoint_send_eos_to_appsrcs (self);
    self->priv->stopping = TRUE;
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
    kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_NULL);
    kms_recorder_endpoint_state_changed (self, KMS_URI_ENDPOINT_STATE_STOP);
  }
}

static void
kms_recorder_endpoint_started (KmsUriEndpoint * obj)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);

  kms_recorder_endpoint_create_parent_directories (self);

  kms_recorder_endpoint_change_state (self);

  /* Set internal pipeline to playing */
  kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_PLAYING);

  BASE_TIME_LOCK (self);

  if (GST_CLOCK_TIME_IS_VALID (self->priv->paused_start)) {
    self->priv->paused_time +=
        gst_clock_get_time (kms_base_media_muxer_get_clock (self->priv->mux)) -
        self->priv->paused_start;
    self->priv->paused_start = GST_CLOCK_TIME_NONE;
  }

  BASE_TIME_UNLOCK (self);

  kms_recorder_generate_pads (self);

  kms_recorder_endpoint_state_changed (self, KMS_URI_ENDPOINT_STATE_START);
}

static void
kms_recorder_endpoint_paused (KmsUriEndpoint * obj)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);

  kms_recorder_endpoint_change_state (self);

  kms_recorder_endpoint_remove_pads (self);

  /* Set internal pipeline to GST_STATE_PAUSED */
  kms_base_media_muxer_set_state (self->priv->mux, GST_STATE_PAUSED);

  KMS_ELEMENT_LOCK (self);

  self->priv->paused_start =
      gst_clock_get_time (kms_base_media_muxer_get_clock (self->priv->mux));

  KMS_ELEMENT_UNLOCK (self);

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
  GstAppSinkCallbacks callbacks;
  GstElement *appsink, *appsrc;
  KmsRecordingProfile profile;
  DataEvtProbe *data;
  KmsMediaType type;
  GstPad *target;
  gulong *probe;
  gchar *id;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target == self->priv->audio_target) {
    type = KMS_MEDIA_TYPE_AUDIO;
    probe = &self->priv->audio_probe;
  } else if (target == self->priv->video_target) {
    type = KMS_MEDIA_TYPE_VIDEO;
    probe = &self->priv->video_probe;
  } else {
    GST_ERROR_OBJECT (self, "Invalid pad %" GST_PTR_FORMAT " connected %"
        GST_PTR_FORMAT, pad, peer);
    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
    g_clear_object (&target);

    return;
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

  g_hash_table_insert (self->priv->srcs, id, g_object_ref (appsrc));

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  if (*probe != 0UL) {
    gst_pad_remove_probe (target, *probe);
  }

  callbacks.eos = recv_eos;
  callbacks.new_preroll = NULL;
  callbacks.new_sample = recv_sample;

  appsink = gst_pad_get_parent_element (target);
  gst_app_sink_set_callbacks (GST_APP_SINK (appsink), &callbacks, appsrc, NULL);
  g_object_unref (appsink);

  data = data_evt_probe_new (appsrc, profile);
  *probe = gst_pad_add_probe (target, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      configure_pipeline_capabilities, data,
      (GDestroyNotify) data_evt_probe_destroy);
  g_object_unref (target);
}

static void
unlink_sinkpad_cb (GstPad * pad, GstPad * peer, gpointer user_data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (user_data);
  gchar *id = NULL;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  id = gst_pad_get_name (pad);

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
    KmsElementPadType type)
{
  GstElement *appsink;
  GstPad *sinkpad;
  const gchar *appsink_name;
  GstPad **target_pad;

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      appsink_name = AUDIO_APPSINK;
      target_pad = &self->priv->audio_target;
      break;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      appsink_name = VIDEO_APPSINK;
      target_pad = &self->priv->video_target;
      break;
    default:
      return;
  }

  appsink = gst_element_factory_make ("appsink", appsink_name);

  g_object_set (appsink, "emit-signals", FALSE, "async", FALSE,
      "sync", FALSE, "qos", FALSE, NULL);

  gst_bin_add (GST_BIN (self), appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");

  gst_element_sync_state_with_parent (appsink);

  *target_pad = sinkpad;

  g_object_unref (sinkpad);
}

static gchar *
str_media_type (KmsMediaType type)
{
  switch (type) {
    case KMS_MEDIA_TYPE_VIDEO:
      return "video";
    case KMS_MEDIA_TYPE_AUDIO:
      return "audio";
    case KMS_MEDIA_TYPE_DATA:
      return "data";
    default:
      return "<unsupported>";
  }
}

static void
kms_recorder_endpoint_latency_cb (GstPad * pad, KmsMediaType type,
    GstClockTimeDiff t, gpointer user_data)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (user_data);
  gdouble *prev;

  switch (type) {
    case KMS_MEDIA_TYPE_AUDIO:
      prev = &self->priv->ai;
      break;
    case KMS_MEDIA_TYPE_VIDEO:
      prev = &self->priv->vi;
      break;
    default:
      GST_DEBUG_OBJECT (pad, "No stast calculated for media (%s)",
          str_media_type (type));
      return;
  }

  *prev = KMS_STATS_CALCULATE_LATENCY_AVG (t, *prev);
}

static void
kms_recorder_endpoint_enable_media_stats (KmsStatsProbe * sprobe,
    KmsRecorderEndpoint * self)
{
  kms_stats_probe_add_latency (sprobe, kms_recorder_endpoint_latency_cb, self,
      NULL);
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

  if (self->priv->stats_enabled) {
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

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  kms_base_media_muxer_set_state (recorder->priv->mux, GST_STATE_NULL);
  kms_recorder_endpoint_state_changed (recorder, KMS_URI_ENDPOINT_STATE_STOP);

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

  if (self->priv->stats_enabled) {
    kms_stats_probe_add_latency (sprobe, kms_recorder_endpoint_latency_cb, self,
        NULL);
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
    kms_recorder_endpoint_add_appsink (self, KMS_ELEMENT_PAD_TYPE_AUDIO);
  }

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_VIDEO)) {
    kms_recorder_endpoint_add_appsink (self, KMS_ELEMENT_PAD_TYPE_VIDEO);
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

static GstCaps *
kms_recorder_endpoint_allowed_caps (KmsElement * kmselement,
    KmsElementPadType type)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (kmselement);
  GstPad *target_pad;
  GstCaps *caps;

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      target_pad = self->priv->video_target;
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      target_pad = self->priv->audio_target;
      break;
    default:
      return NULL;
  }

  if (target_pad == NULL) {
    return NULL;
  }

  caps = gst_pad_get_allowed_caps (target_pad);

  return caps;
}

static gboolean
kms_recorder_endpoint_query_caps (KmsElement * element, GstPad * pad,
    GstQuery * query)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (element);
  GstCaps *allowed = NULL, *caps = NULL;
  GstCaps *filter, *result, *tcaps;

  gst_query_parse_caps (query, &filter);

  switch (kms_element_get_pad_type (element, pad)) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      allowed =
          kms_recorder_endpoint_allowed_caps (element,
          KMS_ELEMENT_PAD_TYPE_VIDEO);
      caps =
          kms_recorder_endpoint_get_caps_from_profile (self,
          KMS_ELEMENT_PAD_TYPE_VIDEO);
      result = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      allowed =
          kms_recorder_endpoint_allowed_caps (element,
          KMS_ELEMENT_PAD_TYPE_AUDIO);
      caps =
          kms_recorder_endpoint_get_caps_from_profile (self,
          KMS_ELEMENT_PAD_TYPE_AUDIO);
      result = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
      break;
    default:
      GST_ERROR_OBJECT (pad, "unknown pad");
      return FALSE;
  }

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
    gchar *id;

    id = gst_pad_get_name (pad);

    KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

    appsrc = g_hash_table_lookup (self->priv->srcs, id);
    g_free (id);

    if (appsrc == NULL) {
      GstCaps *aux;

      KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
      GST_ERROR_OBJECT (self, "No appsrc attached to pad %" GST_PTR_FORMAT,
          pad);

      /* Filter against profile */
      GST_WARNING_OBJECT (appsrc, "Using generic profile's caps");
      aux = gst_caps_intersect (caps, result);
      gst_caps_unref (result);
      result = aux;
      goto filter_caps;
    }
    srcpad = gst_element_get_static_pad (appsrc, "src");

    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

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
      GST_ERROR_OBJECT (self, "No appsrc attached to pad %" GST_PTR_FORMAT,
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

  self->priv->stats_enabled = enable;
  kms_recorder_endpoint_update_media_stats (self);

  KMS_ELEMENT_UNLOCK (self);

  KMS_ELEMENT_CLASS
      (kms_recorder_endpoint_parent_class)->collect_media_stats (obj, enable);
}

static GstStructure *
kms_recorder_endpoint_stats (KmsElement * obj, gchar * selector)
{
  KmsRecorderEndpoint *self = KMS_RECORDER_ENDPOINT (obj);
  GstStructure *stats, *e_stats;

  /* chain up */
  stats =
      KMS_ELEMENT_CLASS (kms_recorder_endpoint_parent_class)->stats (obj,
      selector);

  if (!self->priv->stats_enabled) {
    return stats;
  }

  e_stats = kms_stats_get_element_stats (stats);

  if (e_stats == NULL) {
    return stats;
  }

  /* Add end to end latency */
  gst_structure_set (e_stats, "video-e2e-latency", G_TYPE_UINT64,
      (guint64) self->priv->vi, "audio-e2e-latency", G_TYPE_UINT64,
      (guint64) self->priv->ai, NULL);

  GST_DEBUG_OBJECT (self, "Stats: %" GST_PTR_FORMAT, stats);

  return stats;
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

  self->priv->video_target = NULL;
  self->priv->audio_target = NULL;

  self->priv->ai = 0.0;
  self->priv->vi = 0.0;

  self->priv->audio_probe = 0UL;
  self->priv->video_probe = 0UL;

  g_cond_init (&self->priv->state_manager.cond);

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
