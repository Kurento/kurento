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

enum
{
  PROP_0,
  PROP_PROFILE,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

typedef enum
{
  RUNNING,
  RELEASING,
  RELEASED
} PilelineReleaseStatus;

struct _KmsRecorderEndPointPrivate
{
  PilelineReleaseStatus pipeline_released_status;
  GstElement *pipeline;
  GstElement *encodebin;
  KmsRecordingProfile profile;
  guint count;
};

enum
{
  SIGNAL_STOPPED,
  LAST_SIGNAL
};

static guint kms_recorder_end_point_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndPoint, kms_recorder_end_point,
    KMS_TYPE_URI_END_POINT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

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

static gboolean
set_to_null_state_on_EOS (GstBus * bus, GstMessage * message, gpointer data)
{
  KmsRecorderEndPoint *self = data;

  if (GST_MESSAGE_TYPE (message) == GST_MESSAGE_EOS &&
      GST_MESSAGE_SRC (message) == GST_OBJECT_CAST (self->priv->pipeline)) {
    GST_DEBUG ("Received EOS in pipeline, setting NULL state");

    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    g_signal_emit (G_OBJECT (self),
        kms_recorder_end_point_signals[SIGNAL_STOPPED], 0);

    if (self->priv->pipeline_released_status == RELEASING)
      self->priv->pipeline_released_status = RELEASED;
    return FALSE;
  }

  return TRUE;
}

static void
kms_recorder_end_point_wait_for_pipeline_eos (KmsRecorderEndPoint * self)
{
  GstBus *bus;

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_add_watch_full (bus, G_PRIORITY_DEFAULT, set_to_null_state_on_EOS,
      g_object_ref (self), g_object_unref);
  g_object_unref (bus);

  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);
  // Wait for EOS event to set pipeline to NULL
  kms_recorder_end_point_send_eos_to_appsrcs (self);
}

static void
kms_recorder_end_point_dispose (GObject * object)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  if (self->priv->pipeline_released_status != RELEASED) {
    self->priv->pipeline_released_status = RELEASING;
    if (GST_STATE (self->priv->pipeline) != GST_STATE_NULL) {
      kms_recorder_end_point_wait_for_pipeline_eos (self);
      return;
    } else {
      self->priv->pipeline_released_status = RELEASED;
      gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
      g_object_unref (self->priv->pipeline);
      self->priv->pipeline = NULL;
    }
  } else if (self->priv->pipeline != NULL) {
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    g_object_unref (self->priv->pipeline);
    self->priv->pipeline = NULL;
  }

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->dispose (object);
}

static void
kms_recorder_end_point_finalize (GObject * object)
{
  /* clean up object here */

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
    return GST_FLOW_ERROR;

  g_object_get (G_OBJECT (appsrc), "caps", &caps, NULL);
  if (caps == NULL) {
    /* Appsrc has not yet caps defined */
    GstPad *sink_pad = gst_element_get_static_pad (appsink, "sink");

    if (sink_pad != NULL) {
      caps = gst_pad_get_current_caps (sink_pad);
      g_object_unref (sink_pad);
    }

    if (caps != NULL) {
      g_object_set (appsrc, "caps", caps, NULL);
      gst_caps_unref (caps);
    } else {
      GST_ERROR ("No caps found for %s", GST_ELEMENT_NAME (appsrc));
    }
  } else {
    gst_caps_unref (caps);
  }

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
  gchar *uri = NULL;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  if (KMS_URI_END_POINT (self)->uri == NULL)
    goto no_uri;

  uri = g_strdup_printf (KMS_URI_END_POINT (self)->uri, self->priv->count);

  if (!gst_uri_is_valid (uri))
    goto invalid_uri;

  sink = gst_element_make_from_uri (GST_URI_SINK, uri, NULL, &err);
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
      gchar *location = gst_uri_get_location (uri);

      g_free (uri);
      uri = location;
    }
    GST_DEBUG_OBJECT (sink, "configuring location=%s", uri);
    g_object_set (sink, "location", uri, NULL);
  }

  /* Increment file counter */
  self->priv->count++;
  goto end;

no_uri:
  {
    /* we should use GST_ELEMENT_ERROR instead */
    GST_ERROR_OBJECT (self, "No URI specified to record to.");
    goto end;
  }
invalid_uri:
  {
    GST_ERROR_OBJECT (self, "Invalid URI \"%s\".",
        KMS_URI_END_POINT (self)->uri);

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

      GST_ERROR_OBJECT (self, "No URI handler implemented for \"%s\".", prot);

      g_free (prot);
    } else {
      GST_ERROR_OBJECT (self, "%s",
          (err) ? err->message : "URI was not accepted by any element");
    }

    g_clear_error (&err);
    goto end;
  }
end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  if (uri != NULL)
    g_free (uri);

  return sink;
}

static void
kms_recorder_end_point_add_sink (KmsRecorderEndPoint * self)
{
  GstElement *sink;

  sink = kms_recorder_end_point_get_sink (self);

  gst_bin_add (GST_BIN (self->priv->pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  gst_element_link (self->priv->encodebin, sink);

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
    // TODO Initialize sinks to a new url to avoid overwriting
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
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
}

static void
kms_recorder_end_point_paused (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  /* Close valves */
  kms_recorder_end_point_close_valves (self);

  /* Set internal pipeline to GST_STATE_PAUSED */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);
}

static void
kms_recorder_end_point_set_profile_to_encodebin (KmsRecorderEndPoint * self)
{
  gboolean has_audio, has_video;
  GstEncodingContainerProfile *cprof;

  has_video = kms_element_get_video_valve (KMS_ELEMENT (self)) != NULL;
  has_audio = kms_element_get_audio_valve (KMS_ELEMENT (self)) != NULL;

  cprof =
      kms_recording_profile_create_profile (self->priv->profile, has_audio,
      has_video);

  // HACK: this is the maximum time that the server can recor, I don't know
  // why but if synchronization is enabled, audio packets are droped
  g_object_set (G_OBJECT (self->priv->encodebin), "profile", cprof,
      "audio-jitter-tolerance", G_GUINT64_CONSTANT (0x0fffffffffffffff),
      "avoid-reencoding", TRUE, NULL);
  gst_encoding_profile_unref (cprof);
}

static void
kms_recorder_end_point_link_old_src_to_encodebin (KmsRecorderEndPoint * self,
    GstElement * old_encodebin)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  if (old_encodebin == NULL)
    return;

  it = gst_element_iterate_sink_pads (old_encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *peer;

        sinkpad = g_value_get_object (&val);
        peer = gst_pad_get_peer (sinkpad);

        if (peer != NULL) {
          GstElement *parent;

          parent = gst_pad_get_parent_element (peer);
          GST_PAD_STREAM_LOCK (peer);
          gst_element_release_request_pad (old_encodebin, sinkpad);
          gst_pad_unlink (peer, sinkpad);
          gst_element_link_pads (parent, GST_OBJECT_NAME (peer),
              self->priv->encodebin, GST_OBJECT_NAME (sinkpad));
          GST_PAD_STREAM_UNLOCK (peer);

          g_object_unref (parent);
          g_object_unref (peer);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (old_encodebin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static void
remove_encodebin (GstElement * encodebin)
{
  GstElement *peer_element;
  GstPad *src, *peer;
  GstBin *pipe;

  pipe = GST_BIN (gst_object_get_parent (GST_OBJECT (encodebin)));
  src = gst_element_get_static_pad (encodebin, "src");
  peer = gst_pad_get_peer (src);

  if (peer == NULL)
    goto end;

  peer_element = gst_pad_get_parent_element (peer);

  if (peer_element != NULL) {
    gst_bin_remove (pipe, peer_element);
    g_object_unref (peer_element);
  }

  g_object_unref (peer);

end:

  gst_bin_remove (pipe, encodebin);

  g_object_unref (pipe);
  g_object_unref (src);
}

static void
kms_recorder_end_point_add_appsrc (KmsRecorderEndPoint * self,
    GstElement * valve, const gchar * agnostic_caps, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  GstCaps *caps = gst_caps_from_string (agnostic_caps);
  GstElement *appsink, *appsrc;
  GstElement *old_encodebin = NULL;

  if (self->priv->encodebin != NULL)
    old_encodebin = self->priv->encodebin;

  self->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
  kms_recorder_end_point_set_profile_to_encodebin (self);
  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->encodebin);

  kms_recorder_end_point_add_sink (self);
  gst_element_sync_state_with_parent (self->priv->encodebin);

  appsink = gst_element_factory_make ("appsink", sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "caps", caps, NULL);
  g_object_set (appsink, "async", FALSE, NULL);
  g_object_set (appsink, "sync", FALSE, NULL);
  g_object_set (appsink, "qos", TRUE, NULL);

  gst_caps_unref (caps);

  kms_recorder_end_point_link_old_src_to_encodebin (self, old_encodebin);

  if (old_encodebin != NULL) {
    if (GST_STATE (old_encodebin) <= GST_STATE_PAUSED) {
      remove_encodebin (old_encodebin);
    } else {
      // TODO: Unlink encodebin and send EOS
      // TODO: Wait for EOS message in bus to destroy encodebin and sink
      gst_element_send_event (old_encodebin, gst_event_new_eos ());
    }
  }

  appsrc = gst_element_factory_make ("appsrc", srcname);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (KMS_RECORDER_END_POINT (self)->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  gst_element_link_pads (appsrc, "src", self->priv->encodebin, destpadname);

  g_signal_connect (appsink, "new-sample", G_CALLBACK (recv_sample), appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (recv_eos), appsrc);

  gst_bin_add (GST_BIN (self), appsink);
  gst_element_sync_state_with_parent (appsink);
  gst_element_link (valve, appsink);
}

static void
kms_recorder_end_point_audio_valve_added (KmsElement * self, GstElement * valve)
{
  // TODO: This caps should be set using the profile data
  kms_recorder_end_point_add_appsrc (KMS_RECORDER_END_POINT (self), valve,
      "audio/x-vorbis", AUDIO_APPSINK, AUDIO_APPSRC, "audio_%u");
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
      "video/x-vp8", VIDEO_APPSINK, VIDEO_APPSRC, "video_%u");
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

  kms_recorder_end_point_signals[SIGNAL_STOPPED] =
      g_signal_new ("stopped",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsRecorderEndPointClass, stopped_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

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
  self->priv->pipeline_released_status = RUNNING;
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
