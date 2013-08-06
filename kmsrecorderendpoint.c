#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include "kmsagnosticcaps.h"
#include "kmsrecorderendpoint.h"
#include "kmsuriendpointstate.h"

#define PLUGIN_NAME "recorderendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"
#define AUTOMUXER "automuxer"

#define HTTP_PROTO "http"

GST_DEBUG_CATEGORY_STATIC (kms_recorder_end_point_debug_category);
#define GST_CAT_DEFAULT kms_recorder_end_point_debug_category

#define KMS_RECORDER_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_RECORDER_END_POINT,                  \
    KmsRecorderEndPointPrivate                    \
  )                                               \
)
struct _KmsRecorderEndPointPrivate
{
  GstElement *pipeline;
  guint count;
};

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
    GST_ERROR ("Could not send EOS to appsrc  %s. Ret code %d", ret,
        GST_ELEMENT_NAME (appsrc));
  }
}

static void
kms_recorder_end_point_send_eos_to_appsrcs (KmsRecorderEndPoint * self)
{
  GstElement *audiosrc =
      gst_bin_get_by_name (GST_BIN (self->priv->pipeline), AUDIO_APPSRC);
  GstElement *videosrc =
      gst_bin_get_by_name (GST_BIN (self->priv->pipeline), VIDEO_APPSRC);

  if (audiosrc != NULL)
    send_eos (audiosrc);

  if (videosrc != NULL)
    send_eos (videosrc);
}

static void
kms_recorder_end_point_dispose (GObject * object)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  if (self->priv->pipeline != NULL) {
    kms_recorder_end_point_send_eos_to_appsrcs (self);
    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    gst_object_unref (GST_OBJECT (self->priv->pipeline));
    self->priv->pipeline = NULL;
  }

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->dispose (object);
}

static void
kms_recorder_end_point_finalize (GObject * object)
{
  KmsRecorderEndPoint *recorderendpoint = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (recorderendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->finalize (object);
}

static void
recv_sample (GstAppSink * appsink, gpointer user_data)
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
    return;

  g_object_get (G_OBJECT (appsrc), "caps", &caps, NULL);
  if (caps == NULL) {
    /* Appsrc has not yet caps defined */
    caps = gst_sample_get_caps (sample);
    if (caps != NULL)
      g_object_set (appsrc, "caps", caps, NULL);
    else
      GST_ERROR ("No caps found for %s", GST_ELEMENT_NAME (appsrc));
  }

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL)
    return;

  g_object_get (G_OBJECT (self), "state", &state, NULL);
  if (state != KMS_URI_END_POINT_STATE_START) {
    GST_DEBUG ("Dropping buffer %P", buffer);
    return;
  }

  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to appsrc  %s. Ret code %d", ret,
        GST_ELEMENT_NAME (appsrc));
  }
}

static void
recv_eos (GstAppSink * appsink, gpointer user_data)
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
pad_added (GstElement * element, GstPad * pad, gpointer data)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (data);
  GstElement *sink;
  GstPad *sinkpad;

  GST_INFO ("Pad added");

  if (GST_PAD_IS_SINK (pad))
    return;

  sink = kms_recorder_end_point_get_sink (self);

  gst_bin_add (GST_BIN (self->priv->pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");

  if (gst_pad_link (pad, sinkpad) != GST_PAD_LINK_OK)
    GST_ERROR ("Could not link %s with %s", GST_ELEMENT_NAME (element),
        GST_ELEMENT_NAME (sink));

  gst_object_unref (sinkpad);
}

static void
pad_removed (GstElement * element, GstPad * pad, gpointer data)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (data);
  GstElement *sink;
  GstPad *peer;

  GST_INFO ("Pad removed");

  if (GST_PAD_IS_SINK (pad))
    return;

  peer = gst_pad_get_peer (pad);
  if (peer == NULL)
    return;

  sink = gst_pad_get_parent_element (peer);
  if (sink == NULL) {
    GST_ERROR ("No parent element for pad %s was faound",
        GST_ELEMENT_NAME (sink));
    return;
  }

  gst_pad_unlink (pad, peer);

  if (!gst_element_set_locked_state (sink, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (sink));

  GST_DEBUG ("Removing sink %s from %s", GST_ELEMENT_NAME (sink),
      GST_ELEMENT_NAME (self->priv->pipeline));

  gst_element_set_state (sink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), sink);

  gst_object_unref (peer);
  g_object_unref (sink);
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
    kms_recorder_end_point_send_force_key_unit_event (valve);
    g_object_set (valve, "drop", FALSE, NULL);
  }

  valve = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (valve != NULL) {
    kms_recorder_end_point_send_force_key_unit_event (valve);
    g_object_set (valve, "drop", FALSE, NULL);
  }
}

static void
kms_recorder_end_point_close_valves (KmsRecorderEndPoint * self)
{
  GstElement *valve;

  valve = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (valve != NULL)
    g_object_set (valve, "drop", TRUE, NULL);

  valve = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (valve != NULL)
    g_object_set (valve, "drop", TRUE, NULL);
}

static void
kms_recorder_end_point_stopped (KmsUriEndPoint * obj)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (obj);

  /* Close valves */
  kms_recorder_end_point_close_valves (self);

  /* Set internal pipeline to NULL */
  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
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
kms_recorder_end_point_add_appsrc (KmsElement * self, GstElement * valve,
    const gchar * agnostic_caps, const gchar * sinkname, const gchar * srcname,
    const gchar * destpadname)
{
  GstCaps *caps = gst_caps_from_string (agnostic_caps);
  GstElement *appsink, *appsrc;
  GstElement *automuxer =
      gst_bin_get_by_name (GST_BIN (KMS_RECORDER_END_POINT (self)->
          priv->pipeline), AUTOMUXER);

  appsink = gst_element_factory_make ("appsink", sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "caps", caps, NULL);

  gst_caps_unref (caps);

  gst_bin_add (GST_BIN (self), appsink);
  gst_element_sync_state_with_parent (appsink);

  gst_element_link (valve, appsink);

  appsrc = gst_element_factory_make ("appsrc", srcname);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (KMS_RECORDER_END_POINT (self)->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  gst_element_link_pads (appsrc, "src", automuxer, destpadname);

  g_signal_connect (appsink, "new-sample", G_CALLBACK (recv_sample), appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (recv_eos), appsrc);
}

static void
kms_recorder_end_point_audio_valve_added (KmsElement * self, GstElement * valve)
{
  kms_recorder_end_point_add_appsrc (self, valve, KMS_AGNOSTIC_AUDIO_CAPS,
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
  kms_recorder_end_point_add_appsrc (self, valve, KMS_AGNOSTIC_VIDEO_CAPS,
      VIDEO_APPSINK, VIDEO_APPSRC, "video_%u");
}

static void
kms_recorder_end_point_video_valve_removed (KmsElement * self,
    GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
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

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsRecorderEndPointPrivate));
}

static void
kms_recorder_end_point_init (KmsRecorderEndPoint * self)
{
  GstElement *automuxer;

  self->priv = KMS_RECORDER_END_POINT_GET_PRIVATE (self);

  /* Create internal pipeline */
  self->priv->pipeline = gst_pipeline_new ("automuxer-sink");
  automuxer = gst_element_factory_make ("automuxerbin", AUTOMUXER);

  gst_bin_add (GST_BIN (self->priv->pipeline), automuxer);

  /* Connect to signals */
  g_signal_connect (automuxer, "pad-added", G_CALLBACK (pad_added), self);
  g_signal_connect (automuxer, "pad-removed", G_CALLBACK (pad_removed), self);
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
