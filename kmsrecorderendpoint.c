#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

#include "kmsagnosticcaps.h"
#include "kmsrecorderendpoint.h"

#define PLUGIN_NAME "recorderendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"
#define AUTOMUXER "automuxer"

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
kms_recorder_end_point_dispose (GObject * object)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  if (self->priv->pipeline != NULL) {
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
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstCaps *caps;

  g_object_get (G_OBJECT (appsink), "last-sample", &sample, NULL);
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

  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send audio buffer to appsrc. Ret code %d", ret);
  }
}

static GstElement *
kms_recorder_end_point_get_sink (KmsRecorderEndPoint * self)
{
  /* TODO: Get proper sink element based on the uri */
  GstElement *filesink = gst_element_factory_make ("filesink", NULL);
  gchar *filename;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  filename =
      g_strdup_printf ("%d_%s", self->priv->count++,
      KMS_URI_END_POINT (self)->uri);
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  g_object_set (G_OBJECT (filesink), "location", filename, NULL);
  g_free (filename);

  return filesink;
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
kms_recorder_end_point_open_valves (KmsRecorderEndPoint * self)
{
  GstElement *valve;

  valve = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (valve != NULL)
    g_object_set (valve, "drop", FALSE, NULL);

  valve = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (valve != NULL)
    g_object_set (valve, "drop", FALSE, NULL);
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
kms_recorder_end_point_audio_valve_added (KmsElement * self, GstElement * valve)
{
  GstCaps *audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  GstElement *audiosink, *audiosrc;

  audiosink = gst_element_factory_make ("appsink", AUDIO_APPSINK);

  g_object_set (audiosink, "emit-signals", TRUE, NULL);
  g_object_set (audiosink, "caps", audio_caps, NULL);

  gst_caps_unref (audio_caps);

  gst_bin_add (GST_BIN (self), audiosink);
  gst_element_sync_state_with_parent (audiosink);

  gst_element_link (valve, audiosink);

  audiosrc =
      gst_bin_get_by_name (GST_BIN (KMS_RECORDER_END_POINT (self)->priv->
          pipeline), AUDIO_APPSRC);
  g_signal_connect (audiosink, "new-sample", G_CALLBACK (recv_sample),
      audiosrc);
  g_object_unref (audiosrc);
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
  GstCaps *video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
  GstElement *videosink, *videosrc;

  videosink = gst_element_factory_make ("appsink", VIDEO_APPSINK);

  g_object_set (videosink, "emit-signals", TRUE, NULL);
  g_object_set (videosink, "caps", video_caps, NULL);

  gst_caps_unref (video_caps);

  gst_bin_add (GST_BIN (self), videosink);
  gst_element_sync_state_with_parent (videosink);

  gst_element_link (valve, videosink);

  videosrc =
      gst_bin_get_by_name (GST_BIN (KMS_RECORDER_END_POINT (self)->priv->
          pipeline), VIDEO_APPSRC);
  g_signal_connect (videosink, "new-sample", G_CALLBACK (recv_sample),
      videosrc);
  g_object_unref (videosrc);
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
  GstElement *audiosrc, *videosrc, *automuxer;

  self->priv = KMS_RECORDER_END_POINT_GET_PRIVATE (self);

  /* Create internal pipeline */
  self->priv->pipeline = gst_pipeline_new ("automuxer-sink");
  audiosrc = gst_element_factory_make ("appsrc", AUDIO_APPSRC);
  videosrc = gst_element_factory_make ("appsrc", VIDEO_APPSRC);
  automuxer = gst_element_factory_make ("automuxerbin", AUTOMUXER);

  // TODO: Create audiosrc and videosrc when they are needed

  /* setup appsrc */
  g_object_set (G_OBJECT (audiosrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", (gint64) 0, "format", GST_FORMAT_TIME, NULL);
  g_object_set (G_OBJECT (videosrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", (gint64) 0, "format", GST_FORMAT_TIME, NULL);

  gst_bin_add_many (GST_BIN (self->priv->pipeline), audiosrc, videosrc,
      automuxer, NULL);

  /* Connect internal elements */
  gst_element_link_pads (audiosrc, "src", automuxer, "audio_%u");
  gst_element_link_pads (videosrc, "src", automuxer, "video_%u");

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
