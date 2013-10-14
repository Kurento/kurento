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

#include "kmsagnosticcaps.h"
#include "kmsrecorderendpoint.h"
#include "kmsuriendpointstate.h"
#include "kmsutils.h"

#include "kmsrecordingprofile.h"
#include "kms-enumtypes.h"

#define PLUGIN_NAME "recorderendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"

#define KEY_DESTINATION_PAD_NAME "kms-pad-key-destination-pad-name"
#define KEY_PAD_PROBE_ID "kms-pad-key-probe-id"

#define HTTP_PROTO "http"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

GST_DEBUG_CATEGORY_STATIC (kms_recorder_end_point_debug_category);
#define GST_CAT_DEFAULT kms_recorder_end_point_debug_category

#define KMS_RECORDER_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_RECORDER_END_POINT,                  \
    KmsRecorderEndPointPrivate                    \
  )                                               \
)

typedef void (*KmsActionFunc) (gpointer user_data);

typedef enum
{
  UNCONFIGURED,
  CONFIGURING,
  CONFIGURED
} RecorderState;

enum
{
  PROP_0,
  PROP_PROFILE,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

struct remove_data
{
  KmsRecorderEndPoint *recorder;
  GstElement *element;
};

struct config_data
{
  guint padblocked;
  GSList *blockedpads;
  GSList *pendingpads;
};

#define KMS_RECORDER_END_POINT_GET_LOCK(obj) (               \
  &KMS_RECORDER_END_POINT (obj)->priv->tdata.thread_mutex    \
)

#define KMS_RECORDER_END_POINT_GET_COND(obj) (               \
  &KMS_RECORDER_END_POINT (obj)->priv->tdata.thread_cond     \
)

#define KMS_RECORDER_END_POINT_LOCK(obj) (                   \
  g_mutex_lock (KMS_RECORDER_END_POINT_GET_LOCK (obj))       \
)

#define KMS_RECORDER_END_POINT_UNLOCK(obj) (                 \
  g_mutex_unlock (KMS_RECORDER_END_POINT_GET_LOCK (obj))     \
)

#define KMS_RECORDER_END_POINT_WAIT(obj) (                   \
  g_cond_wait (KMS_RECORDER_END_POINT_GET_COND (obj),        \
    KMS_RECORDER_END_POINT_GET_LOCK (obj))                   \
)

#define KMS_RECORDER_END_POINT_SIGNAL(obj) (                 \
  g_cond_signal (KMS_RECORDER_END_POINT_GET_COND (obj))      \
)

struct thread_data
{
  GQueue *actions;
  gboolean finish_thread;
  GThread *thread;
  GCond thread_cond;
  GMutex thread_mutex;
};

struct thread_cb_data
{
  KmsActionFunc function;
  gpointer data;
  GDestroyNotify notify;
};

struct _KmsRecorderEndPointPrivate
{
  GstElement *pipeline;
  GstElement *encodebin;
  KmsRecordingProfile profile;
  RecorderState state;
  struct config_data *confdata;
  struct thread_data tdata;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndPoint, kms_recorder_end_point,
    KMS_TYPE_URI_END_POINT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

static void
destroy_ulong (gpointer data)
{
  g_slice_free (gulong, data);
}

static void
send_eos (GstElement * appsrc)
{
  GstFlowReturn ret;

  GST_DEBUG ("Send EOS to %s", GST_ELEMENT_NAME (appsrc));

  g_signal_emit_by_name (appsrc, "end-of-stream", &ret);
  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send EOS to appsrc  %s. Ret code %d",
        GST_ELEMENT_NAME (appsrc), ret);
  }
}

static void
kms_recorder_end_point_add_action (KmsRecorderEndPoint * self,
    KmsActionFunc function, gpointer data, GDestroyNotify notify)
{
  struct thread_cb_data *th_data;

  th_data = g_slice_new0 (struct thread_cb_data);

  th_data->data = data;
  th_data->function = function;
  th_data->notify = notify;

  KMS_RECORDER_END_POINT_LOCK (self);

  g_queue_push_tail (self->priv->tdata.actions, th_data);

  KMS_RECORDER_END_POINT_SIGNAL (self);
  KMS_RECORDER_END_POINT_UNLOCK (self);
}

static void
kms_recorder_end_point_send_eos_to_appsrcs (KmsRecorderEndPoint * self)
{
  GstElement *audiosrc =
      gst_bin_get_by_name (GST_BIN (self->priv->pipeline), AUDIO_APPSRC);
  GstElement *videosrc =
      gst_bin_get_by_name (GST_BIN (self->priv->pipeline), VIDEO_APPSRC);

  if (audiosrc != NULL) {
    send_eos (audiosrc);
    g_object_unref (audiosrc);
  }

  if (videosrc != NULL) {
    send_eos (videosrc);
    g_object_unref (videosrc);
  }
}

static void
kms_recorder_end_point_free_config_data (KmsRecorderEndPoint * self)
{
  if (self->priv->confdata == NULL)
    return;

  g_slist_free (self->priv->confdata->blockedpads);
  g_slist_free (self->priv->confdata->pendingpads);
  g_slice_free (struct config_data, self->priv->confdata);

  self->priv->confdata = NULL;
}

static void
set_to_null_state_on_EOS (gpointer data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (data);

  GST_DEBUG ("Received EOS in pipeline, setting NULL state");

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  gst_element_set_state (recorder->priv->pipeline, GST_STATE_NULL);

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));

  KMS_URI_END_POINT_GET_CLASS (recorder)->change_state (KMS_URI_END_POINT
      (recorder), KMS_URI_END_POINT_STATE_STOP);
}

static void
kms_recorder_end_point_wait_for_pipeline_eos (KmsRecorderEndPoint * self)
{
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);
  // Wait for EOS event to set pipeline to NULL
  kms_recorder_end_point_send_eos_to_appsrcs (self);
}

static void
kms_recorder_end_point_dispose (GObject * object)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  if (self->priv->pipeline != NULL) {
    if (GST_STATE (self->priv->pipeline) != GST_STATE_NULL) {
      GST_ELEMENT_WARNING (self, RESOURCE, BUSY,
          ("Recorder may have buffers to save"),
          ("Disposing recorder when it isn't stopped."));
    }
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    g_object_unref (self->priv->pipeline);
    self->priv->pipeline = NULL;
  }

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->dispose (object);
}

static void
destroy_thread_cb_data (gpointer data)
{
  struct thread_cb_data *th_data = data;

  if (th_data->notify)
    th_data->notify (th_data->data);

  g_slice_free (struct thread_cb_data, th_data);
}

static void
kms_recorder_end_point_finalize (GObject * object)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  /* clean up object here */
  KMS_RECORDER_END_POINT_LOCK (self);
  self->priv->tdata.finish_thread = TRUE;
  KMS_RECORDER_END_POINT_SIGNAL (self);
  KMS_RECORDER_END_POINT_UNLOCK (self);

  g_thread_join (self->priv->tdata.thread);
  g_thread_unref (self->priv->tdata.thread);

  g_cond_clear (&self->priv->tdata.thread_cond);
  g_mutex_clear (&self->priv->tdata.thread_mutex);

  g_queue_free_full (self->priv->tdata.actions, destroy_thread_cb_data);

  kms_recorder_end_point_free_config_data (self);

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->finalize (object);
}

static GstFlowReturn
recv_sample (GstElement * appsink, gpointer user_data)
{
  GstElement *self = GST_ELEMENT (GST_OBJECT_PARENT (appsink));
  GstElement *appsrc = GST_ELEMENT (user_data);
  KmsUriEndPointState state;
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstCaps *caps;

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
      GST_ELEMENT_ERROR (self, CORE, CAPS, ("No caps found for %s",
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

  g_object_get (G_OBJECT (self), "state", &state, NULL);
  if (state != KMS_URI_END_POINT_STATE_START) {
    GST_DEBUG ("Dropping buffer %" GST_PTR_FORMAT, buffer);
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);
  buffer = gst_buffer_make_writable (buffer);

  buffer->pts = GST_CLOCK_TIME_NONE;
  buffer->dts = GST_CLOCK_TIME_NONE;

  buffer->offset = GST_CLOCK_TIME_NONE;
  buffer->offset_end = GST_CLOCK_TIME_NONE;
  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  gst_buffer_unref (buffer);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to appsrc %s. Ret code %d",
        GST_ELEMENT_NAME (appsrc), ret);
  }

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static void
recv_eos (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);

  send_eos (appsrc);
}

static GstElement *
kms_recorder_end_point_get_sink_fallback (KmsRecorderEndPoint * self)
{
  GstElement *sink = NULL;
  gchar *prot;

  prot = gst_uri_get_protocol (KMS_URI_END_POINT (self)->uri);

  if (g_strcmp0 (prot, HTTP_PROTO) == 0) {
    /* We use curlhttpsink */
    sink = gst_element_factory_make ("curlhttpsink", NULL);
  }
  /* Add more if required */
  return sink;
}

static GstElement *
kms_recorder_end_point_get_sink (KmsRecorderEndPoint * self)
{
  GObjectClass *sink_class;
  GstElement *sink = NULL;
  GParamSpec *pspec;
  GError *err = NULL;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  if (KMS_URI_END_POINT (self)->uri == NULL)
    goto no_uri;

  if (!gst_uri_is_valid (KMS_URI_END_POINT (self)->uri))
    goto invalid_uri;

  sink = gst_element_make_from_uri (GST_URI_SINK, KMS_URI_END_POINT (self)->uri,
      NULL, &err);
  if (sink == NULL) {
    /* Some elements have no URI handling capabilities though they can */
    /* handle them. We try to find such element before failing to attend */
    /* this request */
    sink = kms_recorder_end_point_get_sink_fallback (self);
    if (sink == NULL)
      goto no_sink;
  }

  /* Try to configure the sink element */
  sink_class = G_OBJECT_GET_CLASS (sink);

  pspec = g_object_class_find_property (sink_class, "location");
  if (pspec != NULL && G_PARAM_SPEC_VALUE_TYPE (pspec) == G_TYPE_STRING) {
    if (g_strcmp0 (GST_OBJECT_NAME (gst_element_get_factory (sink)),
            "filesink") == 0) {
      /* Work around for filesink elements */
      gchar *location = gst_uri_get_location (KMS_URI_END_POINT (self)->uri);

      GST_DEBUG_OBJECT (sink, "filesink location=%s", location);
      g_object_set (sink, "location", location, NULL);
      g_free (location);
    } else {
      GST_DEBUG_OBJECT (sink, "configuring location=%s",
          KMS_URI_END_POINT (self)->uri);
      g_object_set (sink, "location", KMS_URI_END_POINT (self)->uri, NULL);
    }
  }

  goto end;

no_uri:
  {
    GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS,
        ("No URI specified to record to."), GST_ERROR_SYSTEM);
    goto end;
  }
invalid_uri:
  {
    GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS, ("Invalid URI \"%s\".",
            KMS_URI_END_POINT (self)->uri), GST_ERROR_SYSTEM);
    g_clear_error (&err);
    goto end;
  }
no_sink:
  {
    /* whoops, could not create the sink element, dig a little deeper to
     * figure out what might be wrong. */
    if (err != NULL && err->code == GST_URI_ERROR_UNSUPPORTED_PROTOCOL) {
      gchar *prot;

      prot = gst_uri_get_protocol (KMS_URI_END_POINT (self)->uri);
      if (prot == NULL)
        goto invalid_uri;

      GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS,
          ("No URI handler implemented for \"%s\".", prot), GST_ERROR_SYSTEM);

      g_free (prot);
    } else {
      GST_ELEMENT_ERROR (self, RESOURCE, SETTINGS, ("%s",
              (err) ? err->message : "URI was not accepted by any element"),
          GST_ERROR_SYSTEM);
    }

    g_clear_error (&err);
    goto end;
  }
end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
  return sink;
}

static GstPadProbeReturn
stop_notification_cb (GstPad * srcpad, GstPadProbeInfo * info,
    gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  kms_recorder_end_point_add_action (recorder, set_to_null_state_on_EOS,
      recorder, NULL);

  return GST_PAD_PROBE_OK;
}

static void
kms_recorder_end_point_add_sink (KmsRecorderEndPoint * self)
{
  gulong *probe_id;
  GstElement *sink;
  GstPad *sinkpad;

  sink = kms_recorder_end_point_get_sink (self);

  gst_bin_add (GST_BIN (self->priv->pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  gst_element_link (self->priv->encodebin, sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  probe_id = g_slice_new0 (gulong);
  *probe_id = gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      stop_notification_cb, self, NULL);
  g_object_set_data_full (G_OBJECT (sinkpad), KEY_PAD_PROBE_ID, probe_id,
      destroy_ulong);
  g_object_unref (G_OBJECT (sinkpad));
}

static void
kms_recorder_end_point_send_force_key_unit_event (GstElement * valve)
{
  GstStructure *s;
  GstEvent *force_key_unit_event;

  GST_DEBUG ("Sending key ");
  s = gst_structure_new ("GstForceKeyUnit",
      "all-headers", G_TYPE_BOOLEAN, TRUE, NULL);
  force_key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);
  gst_element_send_event (valve, force_key_unit_event);
}

static void
kms_recorder_end_point_open_valves (KmsRecorderEndPoint * self)
{
  GstElement *valve;

  valve = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (valve != NULL) {
    kms_utils_set_valve_drop (valve, FALSE);
    kms_recorder_end_point_send_force_key_unit_event (valve);
  }

  valve = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (valve != NULL) {
    kms_utils_set_valve_drop (valve, FALSE);
    kms_recorder_end_point_send_force_key_unit_event (valve);
  }
}

static void
kms_recorder_end_point_close_valves (KmsRecorderEndPoint * self)
{
  GstElement *valve;

  valve = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (valve != NULL)
    kms_utils_set_valve_drop (valve, TRUE);

  valve = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (valve != NULL)
    kms_utils_set_valve_drop (valve, TRUE);
}

static void
kms_recorder_end_point_stopped (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  /* Close valves */
  kms_recorder_end_point_close_valves (self);

  if (GST_STATE (self->priv->pipeline) >= GST_STATE_PAUSED) {
    kms_recorder_end_point_wait_for_pipeline_eos (self);
  } else {
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
        KMS_URI_END_POINT_STATE_STOP);
  }
}

static void
kms_recorder_end_point_started (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  /* Set internal pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);

  /* Open valves */
  kms_recorder_end_point_open_valves (self);

  KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
      KMS_URI_END_POINT_STATE_START);
}

static void
kms_recorder_end_point_paused (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  /* Close valves */
  kms_recorder_end_point_close_valves (self);

  /* Set internal pipeline to GST_STATE_PAUSED */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);

  KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
      KMS_URI_END_POINT_STATE_PAUSE);
}

static void
kms_recorder_end_point_set_profile_to_encodebin (KmsRecorderEndPoint * self)
{
  gboolean has_audio, has_video;
  GstEncodingContainerProfile *cprof;
  const GList *profiles, *l;

  has_video = kms_element_get_video_valve (KMS_ELEMENT (self)) != NULL;
  has_audio = kms_element_get_audio_valve (KMS_ELEMENT (self)) != NULL;

  cprof =
      kms_recording_profile_create_profile (self->priv->profile, has_audio,
      has_video);

  profiles = gst_encoding_container_profile_get_profiles (cprof);

  for (l = profiles; l != NULL; l = l->next) {
    GstEncodingProfile *prof = l->data;
    GstCaps *caps;
    const gchar *appsink_name;
    GstElement *appsink;

    if (GST_IS_ENCODING_AUDIO_PROFILE (prof))
      appsink_name = AUDIO_APPSINK;
    else if (GST_IS_ENCODING_VIDEO_PROFILE (prof))
      appsink_name = VIDEO_APPSINK;
    else
      continue;

    appsink = gst_bin_get_by_name (GST_BIN (self), appsink_name);

    if (appsink == NULL)
      continue;

    caps = gst_encoding_profile_get_input_caps (prof);

    g_object_set (G_OBJECT (appsink), "caps", caps, NULL);

    g_object_unref (appsink);

    gst_caps_unref (caps);
  }

  g_object_set (G_OBJECT (self->priv->encodebin), "profile", cprof,
      "audio-jitter-tolerance", 100 * GST_MSECOND,
      "avoid-reencoding", TRUE, NULL);
  gst_encoding_profile_unref (cprof);
}

static void
kms_recorder_end_point_reconnect_pads (KmsRecorderEndPoint * self,
    GSList * pads)
{
  GSList *e;

  for (e = pads; e != NULL; e = e->next) {
    GstPad *srcpad = e->data;
    GstElement *appsrc = gst_pad_get_parent_element (srcpad);
    gchar *destpad = g_object_get_data (G_OBJECT (appsrc),
        KEY_DESTINATION_PAD_NAME);

    GST_DEBUG ("Relinking pad %" GST_PTR_FORMAT " %s", srcpad, destpad);
    if (!gst_element_link_pads (appsrc, "src", self->priv->encodebin, destpad)) {
      GST_ERROR ("Could not link srcpad %" GST_PTR_FORMAT " to %s", srcpad,
          GST_ELEMENT_NAME (self->priv->encodebin));
    }

    gst_object_unref (appsrc);
  }
}

static void
kms_recorder_end_point_unblock_pads (KmsRecorderEndPoint * self, GSList * pads)
{
  GSList *e;

  for (e = pads; e != NULL; e = e->next) {
    GstPad *srcpad = e->data;
    gulong *probe_id = g_object_get_data (G_OBJECT (srcpad), KEY_PAD_PROBE_ID);

    gst_pad_remove_probe (srcpad, *probe_id);
  }
}

static void
remove_element_from_pipeline (gpointer user_data)
{
  struct remove_data *data = user_data;

  GST_DEBUG ("Remove element %" GST_PTR_FORMAT, data->element);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (data->recorder));

  gst_element_set_locked_state (data->element, TRUE);
  gst_element_set_state (data->element, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (data->recorder->priv->pipeline), data->element);

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (data->recorder));
}

static void
destroy_remove_data (gpointer data)
{
  g_slice_free (struct remove_data, data);
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  struct remove_data *data;
  GstPad *srcpad, *sinkpad;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  /* Old encodebin has been flushed out. It's time to remove it */
  GST_DEBUG ("Event EOS received");

  /* remove the probe first */
  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  /* Unlink encodebin from sinkapp */
  srcpad = gst_element_get_static_pad (recorder->priv->encodebin, "src");
  sinkpad = gst_pad_get_peer (srcpad);

  if (!gst_pad_unlink (srcpad, sinkpad))
    GST_ERROR ("Encodebin %s could not be removed",
        GST_ELEMENT_NAME (recorder->priv->encodebin));

  g_object_unref (G_OBJECT (srcpad));
  g_object_unref (G_OBJECT (sinkpad));

  /* Remove old encodebin and sink elements */
  data = g_slice_new0 (struct remove_data);

  data->recorder = recorder;
  data->element = gst_pad_get_parent_element (pad);
  kms_recorder_end_point_add_action (recorder, remove_element_from_pipeline,
      data, destroy_remove_data);
  g_object_unref (G_OBJECT (data->element));

  data = g_slice_new0 (struct remove_data);

  data->recorder = recorder;
  data->element = recorder->priv->encodebin;
  kms_recorder_end_point_add_action (recorder, remove_element_from_pipeline,
      data, destroy_remove_data);

  /* Add the new encodebin to the pipeline */
  recorder->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
  kms_recorder_end_point_set_profile_to_encodebin (recorder);
  gst_bin_add (GST_BIN (recorder->priv->pipeline), recorder->priv->encodebin);

  /* Add new sink linked to the new encodebin */
  kms_recorder_end_point_add_sink (recorder);
  gst_element_sync_state_with_parent (recorder->priv->encodebin);

  /* Reconnect sources pads */
  kms_recorder_end_point_reconnect_pads (recorder,
      recorder->priv->confdata->blockedpads);

  /* Reconnect pending pads */
  kms_recorder_end_point_reconnect_pads (recorder,
      recorder->priv->confdata->pendingpads);

  /* Remove probes */
  kms_recorder_end_point_unblock_pads (recorder,
      recorder->priv->confdata->blockedpads);
  kms_recorder_end_point_unblock_pads (recorder,
      recorder->priv->confdata->pendingpads);

  recorder->priv->state = CONFIGURED;

  kms_recorder_end_point_free_config_data (recorder);

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));

  /* Do not pass the EOS event downstream */
  return GST_PAD_PROBE_DROP;
}

static void
send_eos_to_sink_pads (GstElement * element)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (element);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad;

        sinkpad = g_value_get_object (&val);
        GST_DEBUG ("Sending event to %" GST_PTR_FORMAT, sinkpad);

        if (!gst_pad_send_event (sinkpad, gst_event_new_eos ()))
          GST_WARNING ("EOS event could not be sent");

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (element));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static GstPadProbeReturn
pad_probe_cb (GstPad * srcpad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  GstPad *sinkpad;

  GST_DEBUG ("Pad blocked %" GST_PTR_FORMAT, srcpad);
  sinkpad = gst_pad_get_peer (srcpad);

  if (sinkpad == NULL) {
    GST_ERROR ("TODO: This situation should not happen");
    return GST_PAD_PROBE_DROP;
  }

  gst_pad_unlink (srcpad, sinkpad);
  g_object_unref (G_OBJECT (sinkpad));

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  recorder->priv->confdata->blockedpads =
      g_slist_prepend (recorder->priv->confdata->blockedpads, srcpad);
  if (g_slist_length (recorder->priv->confdata->blockedpads) ==
      recorder->priv->confdata->padblocked) {
    GstPad *pad, *peer;
    gulong *probe_id;

    GST_DEBUG ("Encodebin source pads blocked");
    /* install new probe for EOS */
    pad = gst_element_get_static_pad (recorder->priv->encodebin, "src");
    peer = gst_pad_get_peer (pad);

    probe_id = g_object_get_data (G_OBJECT (peer), KEY_PAD_PROBE_ID);
    if (probe_id != NULL) {
      gst_pad_remove_probe (peer, *probe_id);
      g_object_set_data_full (G_OBJECT (sinkpad), KEY_PAD_PROBE_ID, NULL, NULL);
    }

    gst_pad_add_probe (peer, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
        event_probe_cb, recorder, NULL);
    g_object_unref (G_OBJECT (pad));
    g_object_unref (G_OBJECT (peer));

    /* Flush out encodebin data by sending an EOS in all its sinkpads */
    send_eos_to_sink_pads (recorder->priv->encodebin);
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));

  return GST_PAD_PROBE_OK;
}

static void
kms_recorder_end_point_remove_encodebin (KmsRecorderEndPoint * self)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  GST_DEBUG ("Blocking encodebin %" GST_PTR_FORMAT, self->priv->encodebin);
  self->priv->confdata->padblocked = 0;

  it = gst_element_iterate_sink_pads (self->priv->encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);

        if (srcpad != NULL) {
          gulong *probe_id;

          probe_id = g_slice_new0 (gulong);
          *probe_id = gst_pad_add_probe (srcpad,
              GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM, pad_probe_cb, self, NULL);
          g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
              destroy_ulong);
          self->priv->confdata->padblocked++;
          g_object_unref (srcpad);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (self->priv->encodebin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static GstPadProbeReturn
pad_probe_blocked_cb (GstPad * srcpad, GstPadProbeInfo * info,
    gpointer user_data)
{
  GST_DEBUG ("Blocked pad %" GST_PTR_FORMAT, srcpad);
  return GST_PAD_PROBE_OK;
}

static void
kms_recorder_end_point_block_appsrc (KmsRecorderEndPoint * self,
    GstElement * appsrc, const gchar * destpadname)
{
  gulong *probe_id;
  GstPad *srcpad;

  srcpad = gst_element_get_static_pad (appsrc, "src");

  probe_id = g_slice_new0 (gulong);
  *probe_id = gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM,
      pad_probe_blocked_cb, NULL, NULL);
  g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
      destroy_ulong);

  self->priv->confdata->pendingpads =
      g_slist_prepend (self->priv->confdata->pendingpads, srcpad);

  g_object_unref (G_OBJECT (srcpad));
}

static void
kms_recorder_end_point_add_appsrc (KmsRecorderEndPoint * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  GstElement *appsink, *appsrc;

  GST_DEBUG ("Adding valve %s", GST_ELEMENT_NAME (valve));

  appsink = gst_element_factory_make ("appsink", sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "async", FALSE, NULL);
  g_object_set (appsink, "sync", FALSE, NULL);
  g_object_set (appsink, "qos", TRUE, NULL);

  gst_bin_add (GST_BIN (self), appsink);

  appsrc = gst_element_factory_make ("appsrc", srcname);
  g_object_set_data_full (G_OBJECT (appsrc), KEY_DESTINATION_PAD_NAME,
      g_strdup (destpadname), g_free);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (KMS_RECORDER_END_POINT (self)->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);

  g_signal_connect (appsink, "new-sample", G_CALLBACK (recv_sample), appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (recv_eos), appsrc);

  gst_element_sync_state_with_parent (appsink);
  gst_element_link (valve, appsink);

  switch (self->priv->state) {
    case UNCONFIGURED:
      self->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
      kms_recorder_end_point_set_profile_to_encodebin (self);
      gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->encodebin);

      kms_recorder_end_point_add_sink (self);
      gst_element_sync_state_with_parent (self->priv->encodebin);
      gst_element_link_pads (appsrc, "src", self->priv->encodebin, destpadname);
      self->priv->state = CONFIGURED;
      break;
    case CONFIGURED:
      self->priv->confdata = g_slice_new0 (struct config_data);

      kms_recorder_end_point_remove_encodebin (self);
      self->priv->state = CONFIGURING;
    case CONFIGURING:
      kms_recorder_end_point_block_appsrc (self, appsrc, destpadname);
      break;
  }
}

static void
kms_recorder_end_point_audio_valve_added (KmsElement * self, GstElement * valve)
{
  // TODO: This caps should be set using the profile data
  kms_recorder_end_point_add_appsrc (KMS_RECORDER_END_POINT (self), valve,
      AUDIO_APPSINK, AUDIO_APPSRC, "audio_%u");
}

static void
kms_recorder_end_point_audio_valve_removed (KmsElement * self,
    GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
}

static void
kms_recorder_end_point_video_valve_added (KmsElement * self, GstElement * valve)
{
  // TODO: This caps should be set using the profile data
  kms_recorder_end_point_add_appsrc (KMS_RECORDER_END_POINT (self), valve,
      VIDEO_APPSINK, VIDEO_APPSRC, "video_%u");
}

static void
kms_recorder_end_point_video_valve_removed (KmsElement * self,
    GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
}

static void
kms_recorder_end_point_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_PROFILE:
      self->priv->profile = g_value_get_enum (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_recorder_end_point_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_PROFILE:
      g_value_set_enum (value, self->priv->profile);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static gpointer
kms_recorder_end_point_thread (gpointer data)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (data);
  struct thread_cb_data *th_data;

  /* Main thread loop */
  while (TRUE) {
    KMS_RECORDER_END_POINT_LOCK (self);
    while (!self->priv->tdata.finish_thread &&
        g_queue_is_empty (self->priv->tdata.actions)) {
      GST_DEBUG_OBJECT (self, "Waiting for elements to remove");
      KMS_RECORDER_END_POINT_WAIT (self);
      GST_DEBUG_OBJECT (self, "Waked up");
    }

    if (self->priv->tdata.finish_thread) {
      KMS_RECORDER_END_POINT_UNLOCK (self);
      break;
    }

    th_data = g_queue_pop_head (self->priv->tdata.actions);

    KMS_RECORDER_END_POINT_UNLOCK (self);

    if (th_data->function)
      th_data->function (th_data->data);

    destroy_thread_cb_data (th_data);
  }

  GST_DEBUG_OBJECT (self, "Thread finished");
  return NULL;
}

static void
kms_recorder_end_point_class_init (KmsRecorderEndPointClass * klass)
{
  KmsUriEndPointClass *urienpoint_class = KMS_URI_END_POINT_CLASS (klass);
  KmsElementClass *kms_element_class;
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "RecorderEndPoint", "Sink/Generic", "Kurento plugin recorder end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->dispose = kms_recorder_end_point_dispose;
  gobject_class->finalize = kms_recorder_end_point_finalize;

  urienpoint_class->stopped = kms_recorder_end_point_stopped;
  urienpoint_class->started = kms_recorder_end_point_started;
  urienpoint_class->paused = kms_recorder_end_point_paused;

  kms_element_class = KMS_ELEMENT_CLASS (klass);

  kms_element_class->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_recorder_end_point_audio_valve_added);
  kms_element_class->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_recorder_end_point_video_valve_added);
  kms_element_class->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_recorder_end_point_audio_valve_removed);
  kms_element_class->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_recorder_end_point_video_valve_removed);

  gobject_class->set_property =
      GST_DEBUG_FUNCPTR (kms_recorder_end_point_set_property);
  gobject_class->get_property =
      GST_DEBUG_FUNCPTR (kms_recorder_end_point_get_property);

  obj_properties[PROP_PROFILE] = g_param_spec_enum ("profile",
      "Recording profile",
      "The profile used for encapsulating the media",
      GST_TYPE_RECORDING_PROFILE, DEFAULT_RECORDING_PROFILE, G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsRecorderEndPointPrivate));
}

static void
kms_recorder_end_point_init (KmsRecorderEndPoint * self)
{
  self->priv = KMS_RECORDER_END_POINT_GET_PRIVATE (self);

  /* Create internal pipeline */
  self->priv->pipeline = gst_pipeline_new ("automuxer-sink");
  g_object_set (self->priv->pipeline, "async-handling", TRUE, NULL);
  self->priv->encodebin = NULL;
  self->priv->state = UNCONFIGURED;

  self->priv->tdata.actions = g_queue_new ();
  g_cond_init (&self->priv->tdata.thread_cond);
  self->priv->tdata.finish_thread = FALSE;
  self->priv->tdata.thread =
      g_thread_new (GST_OBJECT_NAME (self), kms_recorder_end_point_thread,
      self);
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
