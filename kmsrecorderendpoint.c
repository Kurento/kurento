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
#include <libsoup/soup.h>

#include "kmsagnosticcaps.h"
#include "kmsrecorderendpoint.h"
#include "kmsuriendpointstate.h"
#include "kmsutils.h"
#include "kmsloop.h"
#include "kmsrecordingprofile.h"
#include "kms-enumtypes.h"

#define PLUGIN_NAME "recorderendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"

#define KEY_DESTINATION_PAD_NAME "kms-pad-key-destination-pad-name"
#define KEY_PAD_PROBE_ID "kms-pad-key-probe-id"
#define KEY_APP_SINK "kms-key_app_sink"

#define BASE_TIME_DATA "base_time_data"

#define HTTP_PROTO "http"
#define HTTPS_PROTO "https"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

#define HTTP_TIMEOUT 10

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
  WAIT_PENDING,
  CONFIGURED
} RecorderState;

enum
{
  PROP_0,
  PROP_PROFILE,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

struct config_valve
{
  GstElement *valve;
  const gchar *sinkname;
  const gchar *srcname;
  const gchar *destpadname;
};

struct config_data
{
  guint padblocked;
  guint pendingpadsblocked;
  GSList *blockedpads;
  GSList *pendingvalves;
};

struct state_controller
{
  GCond cond;
  GMutex mutex;
  guint locked;
  gboolean changing;
};

struct _KmsRecorderEndPointPrivate
{
  GstElement *pipeline;
  GstElement *encodebin;
  KmsRecordingProfile profile;
  RecorderState state;
  GstClockTime paused_time;
  GstClockTime paused_start;
  struct config_data *confdata;
  struct state_controller state_manager;
  gboolean has_data;
  KmsLoop *loop;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndPoint, kms_recorder_end_point,
    KMS_TYPE_URI_END_POINT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

static void
destroy_valve_configuration (gpointer data)
{
  g_slice_free (struct config_valve, data);
}

static struct config_valve *
generate_valve_configuration (GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  struct config_valve *conf;

  conf = g_slice_new0 (struct config_valve);

  conf->valve = valve;
  conf->sinkname = sinkname;
  conf->srcname = srcname;
  conf->destpadname = destpadname;

  return conf;
}

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
recv_sample (GstElement * appsink, gpointer user_data)
{
  KmsRecorderEndPoint *self =
      KMS_RECORDER_END_POINT (GST_OBJECT_PARENT (appsink));
  GstElement *appsrc = GST_ELEMENT (user_data);
  KmsUriEndPointState state;
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstCaps *caps;
  BaseTimeType *base_time;

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
    buffer->pts -= base_time->pts + self->priv->paused_time;
    buffer->dts = buffer->pts;
  } else if (GST_CLOCK_TIME_IS_VALID (base_time->dts)
      && GST_BUFFER_DTS_IS_VALID (buffer)) {
    buffer->dts -= base_time->dts + self->priv->paused_time;
    buffer->pts = buffer->dts;
  }

  self->priv->has_data = TRUE;

  KMS_ELEMENT_UNLOCK (GST_OBJECT_PARENT (appsink));

  GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_LIVE);

  if (GST_BUFFER_FLAG_IS_SET (buffer, GST_BUFFER_FLAG_HEADER))
    GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);

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
recv_eos (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);

  send_eos (appsrc);
}

static void
kms_recorder_end_point_change_state (KmsRecorderEndPoint * self)
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
kms_recorder_end_point_state_changed (KmsRecorderEndPoint * self,
    KmsUriEndPointState state)
{
  KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
      state);
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_mutex_lock (&self->priv->state_manager.mutex);
  self->priv->state_manager.changing = FALSE;
  if (self->priv->state_manager.locked > 0)
    g_cond_signal (&self->priv->state_manager.cond);
  g_mutex_unlock (&self->priv->state_manager.mutex);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
}

static void
kms_recorder_end_point_add_appsink (KmsRecorderEndPoint * self,
    struct config_valve *conf)
{
  GstElement *appsink;

  GST_DEBUG ("Adding appsink %s", conf->sinkname);

  appsink = gst_element_factory_make ("appsink", conf->sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "async", FALSE, NULL);
  g_object_set (appsink, "sync", FALSE, NULL);
  g_object_set (appsink, "qos", TRUE, NULL);

  gst_bin_add (GST_BIN (self), appsink);
  gst_element_sync_state_with_parent (appsink);
}

static void
kms_recorder_end_point_connect_valve_to_appsink (KmsRecorderEndPoint * self,
    struct config_valve *conf)
{
  GstElement *appsink;

  appsink = gst_bin_get_by_name (GST_BIN (self), conf->sinkname);
  if (appsink == NULL) {
    GST_ERROR ("No appsink %s found", conf->sinkname);
    return;
  }

  GST_DEBUG ("Connecting %s to %s", GST_ELEMENT_NAME (conf->valve),
      GST_ELEMENT_NAME (appsink));

  if (!gst_element_link (conf->valve, appsink)) {
    GST_ERROR ("Could not link %s to %s", GST_ELEMENT_NAME (conf->valve),
        GST_ELEMENT_NAME (appsink));
  }

  g_object_unref (appsink);
}

static void
kms_recorder_end_point_connect_appsink_to_appsrc (KmsRecorderEndPoint * self,
    struct config_valve *conf)
{
  GstElement *appsink, *appsrc;

  appsink = gst_bin_get_by_name (GST_BIN (self), conf->sinkname);
  if (appsink == NULL) {
    GST_ERROR ("No appsink %s found", conf->sinkname);
    return;
  }

  appsrc = gst_element_factory_make ("appsrc", conf->srcname);
  g_object_set_data (G_OBJECT (appsrc), KEY_DESTINATION_PAD_NAME,
      (gpointer) conf->destpadname);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", FALSE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (KMS_RECORDER_END_POINT (self)->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);

  g_signal_connect (appsink, "new-sample", G_CALLBACK (recv_sample), appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (recv_eos), appsrc);

  GST_DEBUG ("Connected %s to %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (appsrc));

  g_object_set_data_full (G_OBJECT (appsrc), KEY_APP_SINK,
      g_object_ref (appsink), g_object_unref);

  g_object_unref (appsink);
}

static void
kms_recorder_end_point_connect_appsrc_to_encodebin (KmsRecorderEndPoint * self,
    struct config_valve *conf)
{
  GstElement *appsrc;

  appsrc = gst_bin_get_by_name (GST_BIN (self->priv->pipeline), conf->srcname);
  if (appsrc == NULL) {
    GST_ERROR ("No appsrc %s found", conf->srcname);
    return;
  }

  GST_DEBUG ("Connecting %s to %s (%s)", GST_ELEMENT_NAME (appsrc),
      GST_ELEMENT_NAME (self->priv->encodebin), conf->destpadname);

  if (!gst_element_link_pads (appsrc, "src", self->priv->encodebin,
          conf->destpadname)) {
    GST_DEBUG ("Connecting %s to %s (%s)", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (self->priv->encodebin), conf->destpadname);
  }

  g_object_unref (appsrc);
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
  g_slist_free_full (self->priv->confdata->pendingvalves,
      destroy_valve_configuration);

  g_slice_free (struct config_data, self->priv->confdata);

  self->priv->confdata = NULL;
}

static void
kms_recorder_end_point_init_config_data (KmsRecorderEndPoint * self)
{
  if (self->priv->confdata != NULL) {
    GST_WARNING ("Configuration data is not empty.");
    kms_recorder_end_point_free_config_data (self);
  }

  self->priv->confdata = g_slice_new0 (struct config_data);
}

static gboolean
set_to_null_state_on_EOS (gpointer data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (data);

  GST_DEBUG ("Received EOS in pipeline, setting NULL state");

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  gst_element_set_state (recorder->priv->pipeline, GST_STATE_NULL);

  kms_recorder_end_point_state_changed (recorder, KMS_URI_END_POINT_STATE_STOP);

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));

  return G_SOURCE_REMOVE;
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

  g_clear_object (&self->priv->loop);

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
kms_recorder_end_point_release_pending_requests (KmsRecorderEndPoint * self)
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

  g_cond_clear (&self->priv->state_manager.cond);
  g_mutex_clear (&self->priv->state_manager.mutex);
}

static void
kms_recorder_end_point_finalize (GObject * object)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  kms_recorder_end_point_release_pending_requests (self);

  kms_recorder_end_point_free_config_data (self);

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->finalize (object);
}

static GstElement *
kms_recorder_end_point_get_sink_fallback (KmsRecorderEndPoint * self)
{
  GstElement *sink = NULL;
  gchar *prot;

  prot = gst_uri_get_protocol (KMS_URI_END_POINT (self)->uri);

  if ((g_strcmp0 (prot, HTTP_PROTO) == 0)
      || (g_strcmp0 (prot, HTTPS_PROTO) == 0)) {
    SoupSession *ss;

    if (kms_is_valid_uri (KMS_URI_END_POINT (self)->uri)) {
      //valid url
      /* We use souphttpclientsink */
      sink = gst_element_factory_make ("souphttpclientsink", NULL);
      ss = soup_session_new_with_options ("timeout", HTTP_TIMEOUT,
          "ssl-strict", FALSE, NULL);
      g_object_set (G_OBJECT (sink), "session", ss, NULL);
    } else {
      GST_ERROR ("URL not valid");
    }

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

  kms_loop_idle_add_full (recorder->priv->loop, G_PRIORITY_HIGH_IDLE,
      set_to_null_state_on_EOS, g_object_ref (recorder), g_object_unref);

  return GST_PAD_PROBE_OK;
}

static void
kms_recorder_end_point_add_sink (KmsRecorderEndPoint * self)
{
  gulong *probe_id;
  GstElement *sink;
  GstPad *sinkpad;

  sink = kms_recorder_end_point_get_sink (self);

  if (sink == NULL) {
    sink = gst_element_factory_make ("fakesink", NULL);
    GST_ELEMENT_ERROR (self, STREAM, WRONG_TYPE, ("No available sink"), (NULL));
  }

  gst_bin_add (GST_BIN (self->priv->pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  GST_DEBUG ("Added sink %s", GST_ELEMENT_NAME (sink));

  if (!gst_element_link (self->priv->encodebin, sink)) {
    GST_ERROR ("Could not link %s to %s",
        GST_ELEMENT_NAME (self->priv->encodebin), GST_ELEMENT_NAME (sink));
  }

  sinkpad = gst_element_get_static_pad (sink, "sink");
  probe_id = g_slice_new0 (gulong);
  *probe_id = gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      stop_notification_cb, self, NULL);
  g_object_set_data_full (G_OBJECT (sinkpad), KEY_PAD_PROBE_ID, probe_id,
      destroy_ulong);
  g_object_unref (sinkpad);
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
  GstElement *audio_src, *video_src;

  kms_recorder_end_point_change_state (self);

  /* Close valves */
  kms_recorder_end_point_close_valves (self);

  // Reset base time data
  audio_src =
      gst_bin_get_by_name (GST_BIN (self->priv->pipeline), AUDIO_APPSRC);
  video_src =
      gst_bin_get_by_name (GST_BIN (self->priv->pipeline), VIDEO_APPSRC);

  KMS_ELEMENT_LOCK (self);

  if (audio_src != NULL) {
    g_object_set_data_full (G_OBJECT (audio_src), BASE_TIME_DATA, NULL, NULL);
    g_object_unref (audio_src);
  }

  if (video_src != NULL) {
    g_object_set_data_full (G_OBJECT (video_src), BASE_TIME_DATA, NULL, NULL);
    g_object_unref (video_src);
  }

  self->priv->paused_time = G_GUINT64_CONSTANT (0);
  self->priv->paused_start = GST_CLOCK_TIME_NONE;

  KMS_ELEMENT_UNLOCK (self);

  if (GST_STATE (self->priv->pipeline) >= GST_STATE_PAUSED) {
    kms_recorder_end_point_wait_for_pipeline_eos (self);
  } else {
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    kms_recorder_end_point_state_changed (self, KMS_URI_END_POINT_STATE_STOP);
  }
}

static void
kms_recorder_end_point_started (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  kms_recorder_end_point_change_state (self);

  /* Set internal pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);

  KMS_ELEMENT_LOCK (self);

  if (GST_CLOCK_TIME_IS_VALID (self->priv->paused_start)) {
    self->priv->paused_time +=
        gst_clock_get_time (GST_ELEMENT (self->priv->pipeline)->clock) -
        self->priv->paused_start;
    self->priv->paused_start = GST_CLOCK_TIME_NONE;
  }

  KMS_ELEMENT_UNLOCK (self);

  /* Open valves */
  kms_recorder_end_point_open_valves (self);

  kms_recorder_end_point_state_changed (self, KMS_URI_END_POINT_STATE_START);
}

static void
kms_recorder_end_point_paused (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  kms_recorder_end_point_change_state (self);

  /* Close valves */
  kms_recorder_end_point_close_valves (self);

  /* Set internal pipeline to GST_STATE_PAUSED */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);

  KMS_ELEMENT_LOCK (self);

  self->priv->paused_start =
      gst_clock_get_time (GST_ELEMENT (self->priv->pipeline)->clock);

  KMS_ELEMENT_UNLOCK (self);

  kms_recorder_end_point_state_changed (self, KMS_URI_END_POINT_STATE_PAUSE);
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

  if (self->priv->profile == KMS_RECORDING_PROFILE_MP4) {
    GstElement *mux =
        gst_bin_get_by_name (GST_BIN (self->priv->encodebin), "muxer");

    g_object_set (G_OBJECT (mux), "fragment-duration", 2000, "streamable", TRUE,
        NULL);

    g_object_unref (mux);
  } else if (self->priv->profile == KMS_RECORDING_PROFILE_WEBM) {
    GstElement *mux =
        gst_bin_get_by_name (GST_BIN (self->priv->encodebin), "muxer");

    g_object_set (G_OBJECT (mux), "streamable", TRUE, NULL);

    g_object_unref (mux);
  }
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

    GST_DEBUG ("Relinking pad %" GST_PTR_FORMAT " to %s", srcpad,
        GST_ELEMENT_NAME (self->priv->encodebin));

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
    GstStructure *s;
    GstEvent *force_key_unit_event;
    GstPad *srcpad = e->data;
    GstElement *appsrc = GST_ELEMENT (GST_OBJECT_PARENT (srcpad));
    GstElement *appsink = g_object_get_data (G_OBJECT (appsrc), KEY_APP_SINK);
    GstPad *sinkpad = gst_element_get_static_pad (appsink, "sink");
    gulong *probe_id = g_object_get_data (G_OBJECT (srcpad), KEY_PAD_PROBE_ID);

    if (probe_id != NULL) {
      GST_DEBUG ("Remove probe in pad %" GST_PTR_FORMAT, srcpad);
      gst_pad_remove_probe (srcpad, *probe_id);
      g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, NULL, NULL);
    }
    // Request key frame
    s = gst_structure_new ("GstForceKeyUnit", "all-headers", G_TYPE_BOOLEAN,
        TRUE, NULL);
    force_key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);
    GST_DEBUG_OBJECT (sinkpad, "Request key frame.");
    gst_pad_push_event (sinkpad, force_key_unit_event);
    g_object_unref (sinkpad);
  }
}

static void
unlock_pending_valves (gpointer data, gpointer user_data)
{
  struct config_valve *config = data;
  gulong *probe_id;
  GstPad *srcpad;

  srcpad = gst_element_get_static_pad (config->valve, "src");
  probe_id = g_object_get_data (G_OBJECT (srcpad), KEY_PAD_PROBE_ID);

  if (probe_id != NULL) {
    GST_DEBUG ("Remove probe in pad %" GST_PTR_FORMAT, srcpad);
    gst_pad_remove_probe (srcpad, *probe_id);
    g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, NULL, NULL);
  }

  g_object_unref (srcpad);
}

static void
add_pending_appsinks (gpointer data, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  struct config_valve *config = data;

  kms_recorder_end_point_add_appsink (recorder, config);
}

static void
connect_pending_valves_to_appsinks (gpointer data, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  struct config_valve *config = data;

  kms_recorder_end_point_connect_valve_to_appsink (recorder, config);
}

static void
connect_pending_appsinks_to_appsrcs (gpointer data, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  struct config_valve *config = data;

  kms_recorder_end_point_connect_appsink_to_appsrc (recorder, config);
}

static void
connect_pending_appsrcs_to_encodebin (gpointer data, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  struct config_valve *config = data;

  kms_recorder_end_point_connect_appsrc_to_encodebin (recorder, config);
}

static void
kms_recorder_end_point_reconfigure_pipeline (KmsRecorderEndPoint * recorder)
{
  GstPad *srcpad, *sinkpad;
  GstElement *sink;

  /* Unlink encodebin from sinkapp */
  srcpad = gst_element_get_static_pad (recorder->priv->encodebin, "src");
  sinkpad = gst_pad_get_peer (srcpad);

  if (!gst_pad_unlink (srcpad, sinkpad))
    GST_ERROR ("Encodebin %s could not be removed",
        GST_ELEMENT_NAME (recorder->priv->encodebin));

  g_object_unref (srcpad);

  /* Remove old encodebin and sink elements */
  sink = gst_pad_get_parent_element (sinkpad);
  g_object_unref (sinkpad);
  gst_element_set_locked_state (sink, TRUE);
  gst_element_set_state (sink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (recorder->priv->pipeline), sink);
  g_object_unref (sink);

  gst_element_set_locked_state (recorder->priv->encodebin, TRUE);
  gst_element_set_state (recorder->priv->encodebin, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (recorder->priv->pipeline),
      recorder->priv->encodebin);

  /* Add the new encodebin to the pipeline */
  recorder->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
  g_slist_foreach (recorder->priv->confdata->pendingvalves,
      add_pending_appsinks, recorder);
  kms_recorder_end_point_set_profile_to_encodebin (recorder);
  g_slist_foreach (recorder->priv->confdata->pendingvalves,
      connect_pending_valves_to_appsinks, recorder);
  g_slist_foreach (recorder->priv->confdata->pendingvalves,
      connect_pending_appsinks_to_appsrcs, recorder);
  gst_bin_add (GST_BIN (recorder->priv->pipeline), recorder->priv->encodebin);

  /* Add new sink linked to the new encodebin */
  kms_recorder_end_point_add_sink (recorder);
  gst_element_sync_state_with_parent (recorder->priv->encodebin);

  /* Reconnect sources pads */
  kms_recorder_end_point_reconnect_pads (recorder,
      recorder->priv->confdata->blockedpads);
  /* Reconnect pending pads */
  g_slist_foreach (recorder->priv->confdata->pendingvalves,
      connect_pending_appsrcs_to_encodebin, recorder);

  /* remove probes to unlock pads */
  kms_recorder_end_point_unblock_pads (recorder,
      recorder->priv->confdata->blockedpads);
  g_slist_foreach (recorder->priv->confdata->pendingvalves,
      unlock_pending_valves, recorder);

  kms_recorder_end_point_free_config_data (recorder);
}

static gboolean
kms_recorder_end_point_do_reconfiguration (gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  kms_recorder_end_point_reconfigure_pipeline (recorder);
  recorder->priv->state = CONFIGURED;
  recorder->priv->has_data = FALSE;

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));

  return G_SOURCE_REMOVE;
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  /* remove the probe first */
  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  /* Old encodebin has been flushed out. It's time to remove it */
  GST_DEBUG ("Element %s flushed out",
      GST_ELEMENT_NAME (recorder->priv->encodebin));

  if (recorder->priv->confdata->pendingpadsblocked ==
      g_slist_length (recorder->priv->confdata->pendingvalves)) {
    GST_DEBUG ("No pad in blocking state");
    /* No more pending valves in blocking state */
    /* so we can remove probes to unlock pads */
    kms_loop_idle_add_full (recorder->priv->loop, G_PRIORITY_HIGH_IDLE,
        kms_recorder_end_point_do_reconfiguration, g_object_ref (recorder),
        g_object_unref);
  } else {
    GST_DEBUG ("Waiting for pads to block");
    recorder->priv->state = WAIT_PENDING;
  }

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
  g_object_unref (sinkpad);

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
    g_object_unref (pad);
    g_object_unref (peer);

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

static gint
compare_configuration_data (gconstpointer a, gconstpointer b)
{
  const GstElement *valve = GST_ELEMENT (b);
  const struct config_valve *conf = a;

  return (conf->valve == valve) ? 0 : -1;
}

static struct config_valve *
kms_recorder_end_point_get_configuration_from_valve (KmsRecorderEndPoint * self,
    GstElement * valve)
{
  GSList *l;

  l = g_slist_find_custom (self->priv->confdata->pendingvalves, valve,
      compare_configuration_data);
  return l->data;
}

static GstPadProbeReturn
pad_probe_blocked_cb (GstPad * srcpad, GstPadProbeInfo * info,
    gpointer user_data)
{
  KmsRecorderEndPoint *recorder = KMS_RECORDER_END_POINT (user_data);
  struct config_valve *conf;
  GstElement *valve;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (recorder));

  GST_DEBUG ("Blocked pending pad %" GST_PTR_FORMAT, srcpad);

  recorder->priv->confdata->pendingpadsblocked++;

  if (recorder->priv->state != WAIT_PENDING ||
      recorder->priv->confdata->pendingpadsblocked !=
      g_slist_length (recorder->priv->confdata->pendingvalves))
    goto end;

  GST_DEBUG ("Reconfiguring internal pipeline");

  valve = gst_pad_get_parent_element (srcpad);
  conf = kms_recorder_end_point_get_configuration_from_valve (recorder, valve);

  if (conf == NULL) {
    GST_ERROR ("No configuration found for valve %s",
        GST_ELEMENT_NAME (conf->valve));
    goto end;
  }

  kms_loop_idle_add_full (recorder->priv->loop, G_PRIORITY_HIGH_IDLE,
      kms_recorder_end_point_do_reconfiguration, g_object_ref (recorder),
      g_object_unref);

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (recorder));
  return GST_PAD_PROBE_OK;
}

static void
kms_recorder_end_point_block_valve (KmsRecorderEndPoint * self,
    struct config_valve *conf)
{
  gulong *probe_id;
  GstPad *srcpad;

  GST_DEBUG ("Blocking valve %s", GST_ELEMENT_NAME (conf->valve));
  srcpad = gst_element_get_static_pad (conf->valve, "src");

  probe_id = g_slice_new0 (gulong);
  *probe_id = gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM,
      pad_probe_blocked_cb, self, NULL);
  g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
      destroy_ulong);

  self->priv->confdata->pendingvalves =
      g_slist_prepend (self->priv->confdata->pendingvalves, conf);
  g_object_unref (srcpad);
}

static void
kms_recorder_end_point_add_appsrc_pads (KmsRecorderEndPoint * self)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (self->priv->encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);

        if (srcpad != NULL) {
          self->priv->confdata->blockedpads =
              g_slist_prepend (self->priv->confdata->blockedpads, srcpad);
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

static void
kms_recorder_end_point_add_appsrc (KmsRecorderEndPoint * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  struct config_valve *config;
  KmsUriEndPointState state;

  config = generate_valve_configuration (valve, sinkname, srcname, destpadname);

  GST_DEBUG ("Connecting %s", GST_ELEMENT_NAME (valve));

  switch (self->priv->state) {
    case UNCONFIGURED:
      self->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
      kms_recorder_end_point_add_appsink (self, config);
      kms_recorder_end_point_set_profile_to_encodebin (self);
      kms_recorder_end_point_connect_valve_to_appsink (self, config);
      kms_recorder_end_point_connect_appsink_to_appsrc (self, config);

      gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->encodebin);

      kms_recorder_end_point_add_sink (self);
      gst_element_sync_state_with_parent (self->priv->encodebin);
      kms_recorder_end_point_connect_appsrc_to_encodebin (self, config);
      destroy_valve_configuration (config);
      self->priv->state = CONFIGURED;
      self->priv->has_data = FALSE;
      break;
    case CONFIGURED:
      kms_recorder_end_point_init_config_data (self);

      if (self->priv->has_data
          && (GST_STATE (self->priv->encodebin) >= GST_STATE_PAUSED
              || GST_STATE_PENDING (self->priv->encodebin) >= GST_STATE_PAUSED
              || GST_STATE_TARGET (self->priv->encodebin) >=
              GST_STATE_PAUSED)) {
        kms_recorder_end_point_remove_encodebin (self);
        self->priv->state = CONFIGURING;
      } else {
        kms_recorder_end_point_add_appsrc_pads (self);
        self->priv->confdata->pendingvalves =
            g_slist_prepend (self->priv->confdata->pendingvalves, config);
        kms_recorder_end_point_reconfigure_pipeline (self);
        break;
      }
    case CONFIGURING:
    case WAIT_PENDING:
      kms_recorder_end_point_block_valve (self, config);
      break;
  }

  g_object_get (self, "state", &state, NULL);
  if (state == KMS_URI_END_POINT_STATE_START) {
    GST_DEBUG ("Setting %" GST_PTR_FORMAT " drop to FALSE", valve);
    kms_utils_set_valve_drop (valve, FALSE);
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

static gboolean
kms_recorder_end_point_post_error (gpointer data)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (data);

  gchar *message = (gchar *) g_object_steal_data (G_OBJECT (self), "message");

  GST_ELEMENT_ERROR (self, STREAM, FAILED, ("%s", message), (NULL));
  g_free (message);

  return G_SOURCE_REMOVE;
}

static GstBusSyncReply
bus_sync_signal_handler (GstBus * bus, GstMessage * msg, gpointer data)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (data);

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {
    GError *err = NULL;

    gst_message_parse_error (msg, &err, NULL);
    GST_ERROR ("ERROR %s", err->message);
    g_object_set_data_full (G_OBJECT (self), "message",
        g_strdup (err->message), (GDestroyNotify) g_free);

    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
        kms_recorder_end_point_post_error, g_object_ref (self), g_object_unref);

    g_error_free (err);
  }
  return GST_BUS_PASS;
}

static void
kms_recorder_end_point_init (KmsRecorderEndPoint * self)
{
  GstBus *bus;

  self->priv = KMS_RECORDER_END_POINT_GET_PRIVATE (self);

  self->priv->loop = kms_loop_new ();

  self->priv->paused_time = G_GUINT64_CONSTANT (0);
  self->priv->paused_start = GST_CLOCK_TIME_NONE;

  /* Create internal pipeline */
  self->priv->pipeline = gst_pipeline_new ("recorder-pipeline");
  g_object_set (self->priv->pipeline, "async-handling", TRUE, NULL);
  self->priv->encodebin = NULL;
  self->priv->state = UNCONFIGURED;
  self->priv->has_data = FALSE;
  g_cond_init (&self->priv->state_manager.cond);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_set_sync_handler (bus, bus_sync_signal_handler, self, NULL);
  g_object_unref (bus);
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
