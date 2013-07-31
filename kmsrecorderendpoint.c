#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <gst/app/gstappsink.h>

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
recv_audio_sample (GstAppSink * gstappsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);

  GST_DEBUG ("TODO: Push audio buffer to %p", GST_ELEMENT_NAME (appsrc));
}

static void
recv_video_sample (GstAppSink * gstappsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);

  GST_DEBUG ("TODO: Push video buffer to %p", GST_ELEMENT_NAME (appsrc));
}

static void
pad_added (GstElement * element, GstPad * pad)
{
  GST_DEBUG ("TODO: Pad added");
}

static void
pad_removed (GstElement * element, GstPad * pad, gpointer data)
{
  GST_DEBUG ("TODO: Pad removed");
}

static void
kms_recorder_end_point_stopped (KmsUriEndPoint * self)
{
  GST_DEBUG ("TODO: Implement stopped");
}

static void
kms_recorder_end_point_started (KmsUriEndPoint * self)
{
  GST_DEBUG ("TODO: Implement started");
}

static void
kms_recorder_end_point_paused (KmsUriEndPoint * self)
{
  GST_DEBUG ("TODO: Implement paused");
}

static void
kms_recorder_end_point_class_init (KmsRecorderEndPointClass * klass)
{
  KmsUriEndPointClass *urienpoint_class = KMS_URI_END_POINT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "RecorderEndPoint", "Sink/Generic", "Kurento plugin recorder end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->dispose = kms_recorder_end_point_dispose;
  gobject_class->finalize = kms_recorder_end_point_finalize;

  urienpoint_class->stopped = kms_recorder_end_point_stopped;
  urienpoint_class->started = kms_recorder_end_point_started;
  urienpoint_class->paused = kms_recorder_end_point_paused;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsRecorderEndPointPrivate));
}

static void
kms_recorder_end_point_init (KmsRecorderEndPoint * self)
{
  GstElement *audiosink, *audiosrc, *videosink, *videosrc, *automuxer;

  self->priv = KMS_RECORDER_END_POINT_GET_PRIVATE (self);

  audiosink = gst_element_factory_make ("appsink", AUDIO_APPSINK);
  videosink = gst_element_factory_make ("appsink", VIDEO_APPSINK);

  gst_bin_add_many (GST_BIN (self), audiosink, videosink, NULL);

  /* Connect parent's valves to child's appsinks */
  gst_element_link (KMS_ELEMENT (self)->audio_valve, audiosink);
  gst_element_link (KMS_ELEMENT (self)->video_valve, videosink);

  /* Create internal pipeline */
  self->priv->pipeline = gst_pipeline_new ("automuxer-sink");
  audiosrc = gst_element_factory_make ("appsrc", AUDIO_APPSRC);
  videosrc = gst_element_factory_make ("appsrc", VIDEO_APPSRC);
  automuxer = gst_element_factory_make ("automuxerbin", AUTOMUXER);

  gst_bin_add_many (GST_BIN (self->priv->pipeline), audiosrc, videosrc,
      automuxer, NULL);

  /* Connect internal elements */
  gst_element_link_pads (audiosrc, "src", automuxer, "audio_%u");
  gst_element_link_pads (videosrc, "src", automuxer, "video_%u");

  /* Connect to signals */
  g_signal_connect (audiosink, "new-sample", G_CALLBACK (recv_audio_sample),
      audiosrc);
  g_signal_connect (videosink, "new-sample", G_CALLBACK (recv_video_sample),
      videosrc);
  g_signal_connect (automuxer, "pad-added", G_CALLBACK (pad_added), NULL);
  g_signal_connect (automuxer, "pad-removed", G_CALLBACK (pad_removed), NULL);
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
