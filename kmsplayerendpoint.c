#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsagnosticcaps.h"
#include "kmsplayerendpoint.h"

#define PLUGIN_NAME "playerendpoint"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSRC "video_appsrc"
#define URIDECODEBIN "uridecodebin"

GST_DEBUG_CATEGORY_STATIC (kms_player_end_point_debug_category);
#define GST_CAT_DEFAULT kms_player_end_point_debug_category

#define KMS_PLAYER_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PLAYER_END_POINT,                  \
    KmsPlayerEndPointPrivate                    \
  )                                             \
)

struct _KmsPlayerEndPointPrivate
{
  GstElement *pipeline;
  GstElement *uridecodebin;
  GstElement *appsrc_audio;
  GstElement *appsrc_video;
};

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlayerEndPoint, kms_player_end_point,
    KMS_TYPE_URI_END_POINT,
    GST_DEBUG_CATEGORY_INIT (kms_player_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for playerendpoint element"));

static void
kms_player_end_point_dispose (GObject * object)
{
  KmsPlayerEndPoint *playerendpoint = KMS_PLAYER_END_POINT (object);

  GST_DEBUG_OBJECT (playerendpoint, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_player_end_point_parent_class)->dispose (object);
}

static void
kms_player_end_point_finalize (GObject * object)
{
  KmsPlayerEndPoint *playerendpoint = KMS_PLAYER_END_POINT (object);

  GST_DEBUG_OBJECT (playerendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_player_end_point_parent_class)->finalize (object);
}

void
read_buffer (GstElement * object, gpointer user_data)
{

  GST_DEBUG ("TODO: Implement read_buffer");
}

static void
pad_added (GstElement * element, GstPad * pad, KmsPlayerEndPoint * self)
{
  GST_DEBUG ("Pad added");
  GstElement *appsink, *agnosticbin;
  GstPad *sinkpad;
  GstCaps *audio_caps, *video_caps;
  GstCaps *src_caps;

  /* Create and link appsink */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (appsink, "sync", TRUE, "enable-last-sample", FALSE,
      "emit-signals", TRUE, NULL);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  gst_pad_link (pad, sinkpad);
  GST_DEBUG_OBJECT (self, "Linked uridecodebin---appsink");
  gst_object_unref (G_OBJECT (sinkpad));

  /* Create and link appsrc--agnosticbin with proper caps */
  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
  src_caps = gst_pad_get_current_caps (pad);
  GST_DEBUG ("caps are %" GST_PTR_FORMAT, src_caps);

  if (gst_caps_can_intersect (audio_caps, src_caps)) {
    GST_DEBUG_OBJECT (self, "Creating audio appsrc");
    self->priv->appsrc_audio =
        gst_element_factory_make ("appsrc", AUDIO_APPSRC);

    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
    gst_bin_add (GST_BIN (self), self->priv->appsrc_audio);
    gst_element_sync_state_with_parent (self->priv->appsrc_audio);

    gst_element_link (self->priv->appsrc_audio, agnosticbin);
    GST_DEBUG_OBJECT (self, "Linked appsrc_audio--audio_agnosticbin");

  } else if (gst_caps_can_intersect (video_caps, src_caps)) {
    GST_DEBUG_OBJECT (self, "Creating video appsrc");
    self->priv->appsrc_video =
        gst_element_factory_make ("appsrc", VIDEO_APPSRC);
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
    gst_bin_add (GST_BIN (self), self->priv->appsrc_video);
    gst_element_sync_state_with_parent (self->priv->appsrc_video);

    gst_element_link (self->priv->appsrc_video, agnosticbin);
    GST_DEBUG_OBJECT (self, "Linked appsrc_video--video_agnosticbin");

  } else {
    GST_DEBUG_OBJECT (self, "NOT agnostic caps");
    goto end;
  }

  g_signal_connect (appsink, "emit-signals", G_CALLBACK (read_buffer), self);

end:
  if (sinkpad != NULL)
    g_object_unref (sinkpad);

  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);

  if (src_caps != NULL)
    gst_caps_unref (src_caps);
}

static void
pad_removed (GstElement * element, GstPad * pad, gpointer data)
{
  GST_DEBUG ("Pad removed");
}

static void
kms_player_end_point_stopped (KmsUriEndPoint * self)
{
  GST_DEBUG ("TODO: Implement stopped");
}

static void
kms_player_end_point_started (KmsUriEndPoint * self)
{
  GST_DEBUG ("TODO: Implement started");
}

static void
kms_player_end_point_paused (KmsUriEndPoint * self)
{
  GST_DEBUG ("TODO: Implement paused");
}

static void
kms_player_end_point_class_init (KmsPlayerEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsUriEndPointClass *urienpoint_class = KMS_URI_END_POINT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlayerEndPoint", "Sink/Generic", "Kurento plugin player end point",
      "Joaquin Mengual García <kini.mengual@gmail.com>");

  gobject_class->dispose = kms_player_end_point_dispose;
  gobject_class->finalize = kms_player_end_point_finalize;

  urienpoint_class->stopped = kms_player_end_point_stopped;
  urienpoint_class->started = kms_player_end_point_started;
  urienpoint_class->paused = kms_player_end_point_paused;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlayerEndPointPrivate));
}

static void
kms_player_end_point_init (KmsPlayerEndPoint * self)
{
  self->priv = KMS_PLAYER_END_POINT_GET_PRIVATE (self);

  self->priv->pipeline = gst_pipeline_new ("pipeline");
  self->priv->appsrc_audio = NULL;
  self->priv->appsrc_video = NULL;
  self->priv->uridecodebin =
      gst_element_factory_make ("uridecodebin", URIDECODEBIN);

  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->uridecodebin);
  /* Connect to signals */
  g_signal_connect (self->priv->uridecodebin, "pad-added",
      G_CALLBACK (pad_added), NULL);
  g_signal_connect (self->priv->uridecodebin, "pad-removed",
      G_CALLBACK (pad_removed), NULL);

  /* Connect to signals */
  g_signal_connect (self->priv->uridecodebin, "pad-added",
      G_CALLBACK (pad_added), self);
  g_signal_connect (self->priv->uridecodebin, "pad-removed",
      G_CALLBACK (pad_removed), self);

  g_object_set (G_OBJECT (self->priv->uridecodebin), "uri",
      KMS_URI_END_POINT (self)->uri, NULL);

}

gboolean
kms_player_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLAYER_END_POINT);
}
