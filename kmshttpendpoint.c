#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kms-marshal.h"
#include "kmshttpendpoint.h"
#include "kmsagnosticcaps.h"

#define PLUGIN_NAME "httpendpoint"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"

#define GST_CAT_DEFAULT kms_http_end_point_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_HTTP_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_HTTP_END_POINT,                  \
    KmsHttpEndPointPrivate                    \
  )                                           \
)

struct _KmsHttpEndPointPrivate
{
  GstElement *post_pipeline;
  GstElement *postsrc;
};

enum
{
  /* actions */
  SIGNAL_PUSH_BUFFER,
  SIGNAL_END_OF_STREAM,
  LAST_SIGNAL
};

static guint http_ep_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEndPoint, kms_http_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME,
        0, "debug category for httpendpoint element"));

static void
new_sample_handler (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample == NULL)
    return;

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL)
    return;

  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  if (ret != GST_FLOW_OK) {
    // something wrong
    GST_ERROR ("Could not send buffer to appsrc  %s. Ret code %d", ret,
        GST_ELEMENT_NAME (appsrc));
  }
}

static void
post_decodebin_pad_added_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndPoint * self)
{
  GstElement *appsrc, *agnosticbin, *appsink;
  GstPad *sinkpad;
  GstCaps *audio_caps, *video_caps;
  GstCaps *src_caps;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_INFO ("pad %s added", gst_pad_get_name (pad));

  /* Create and link appsrc--agnosticbin with proper caps */
  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
  src_caps = gst_pad_query_caps (pad, NULL);
  GST_DEBUG ("caps are %" GST_PTR_FORMAT, src_caps);

  if (gst_caps_can_intersect (audio_caps, src_caps))
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  else if (gst_caps_can_intersect (video_caps, src_caps))
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  else {
    GST_ERROR_OBJECT (self, "No agnostic caps provided");
    gst_caps_unref (src_caps);
    goto end;
  }

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME,
      "caps", src_caps, NULL);

  gst_bin_add (GST_BIN (self), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  gst_element_link (appsrc, agnosticbin);

  /* Create appsink and link to pad */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (appsink, "sync", TRUE, "enable-last-sample",
      FALSE, "emit-signals", TRUE, NULL);
  gst_bin_add (GST_BIN (self->priv->post_pipeline), appsink);
  gst_element_sync_state_with_parent (appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  gst_pad_link (pad, sinkpad);
  GST_DEBUG_OBJECT (self, "Linked %s---%s", GST_ELEMENT_NAME (decodebin),
      GST_ELEMENT_NAME (appsink));
  g_object_unref (sinkpad);

  /* Connect new-sample signal to callback */
  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_handler),
      appsrc);
  g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
  g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);

end:
  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);
}

static void
post_decodebin_pad_removed_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndPoint * self)
{
  GstElement *appsink, *appsrc;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_DEBUG ("pad %s removed", gst_pad_get_name (pad));

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);
  appsrc = g_object_steal_data (G_OBJECT (pad), APPSRC_DATA);

  if (appsrc == NULL) {
    GST_ERROR ("No appsink was found associated with %P", pad);
    return;
  }

  if (GST_OBJECT_PARENT (appsrc) != NULL) {
    g_object_ref (appsrc);
    gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (appsrc)), appsrc);
    gst_element_set_state (appsrc, GST_STATE_NULL);
    g_object_unref (appsrc);
  }

  if (appsink == NULL) {
    GST_ERROR ("No appsink was found associated with %P", pad);
    return;
  }

  if (!gst_element_set_locked_state (appsink, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (appsink));

  GST_DEBUG ("Removing appsink %s from %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (self->priv->post_pipeline));

  gst_element_set_state (appsink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->post_pipeline), appsink);
}

static void
kms_http_end_point_init_post_pipeline (KmsHttpEndPoint * self)
{
  GstElement *decodebin;

  self->priv->post_pipeline = gst_pipeline_new (NULL);
  self->priv->postsrc = gst_element_factory_make ("appsrc", NULL);
  decodebin = gst_element_factory_make ("decodebin", NULL);

  /* configure appsrc */
  g_object_set (G_OBJECT (self->priv->postsrc), "is-live", TRUE,
      "do-timestamp", TRUE, "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add_many (GST_BIN (self->priv->post_pipeline), self->priv->postsrc,
      decodebin, NULL);

  gst_element_link (self->priv->postsrc, decodebin);

  /* Connect decodebin signals */
  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (post_decodebin_pad_added_handler), self);
  g_signal_connect (decodebin, "pad-removed",
      G_CALLBACK (post_decodebin_pad_removed_handler), self);

  /* Set pipeline to playing */
  gst_element_set_state (self->priv->post_pipeline, GST_STATE_PLAYING);
}

static GstFlowReturn
kms_http_end_point_push_buffer_action (KmsHttpEndPoint * self,
    GstBuffer * buffer)
{
  GstFlowReturn ret;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), GST_FLOW_ERROR);

  if (self->priv->post_pipeline == NULL)
    kms_http_end_point_init_post_pipeline (self);

  g_signal_emit_by_name (self->priv->postsrc, "push-buffer", buffer, &ret);

  return ret;
}

static GstFlowReturn
kms_http_end_point_end_of_stream_action (KmsHttpEndPoint * self)
{
  GstFlowReturn ret;

  if (self->priv->post_pipeline == NULL)
    return GST_FLOW_ERROR;

  g_signal_emit_by_name (self->priv->postsrc, "end-of-stream", &ret);
  return ret;
}

static void
kms_http_end_point_dispose (GObject * object)
{
  KmsHttpEndPoint *self = KMS_HTTP_END_POINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  if (self->priv->post_pipeline != NULL) {
    gst_element_set_state (self->priv->post_pipeline, GST_STATE_NULL);
    gst_object_unref (GST_OBJECT (self->priv->post_pipeline));
    self->priv->post_pipeline = NULL;
  }

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_http_end_point_parent_class)->dispose (object);
}

static void
kms_http_end_point_finalize (GObject * object)
{
  KmsHttpEndPoint *httpendpoint = KMS_HTTP_END_POINT (object);

  GST_DEBUG_OBJECT (httpendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_http_end_point_parent_class)->finalize (object);
}

static void
kms_http_end_point_class_init (KmsHttpEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "HttpEndPoint", "Generic", "Kurento http end point plugin",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->dispose = kms_http_end_point_dispose;
  gobject_class->finalize = kms_http_end_point_finalize;

  http_ep_signals[SIGNAL_PUSH_BUFFER] =
      g_signal_new ("push-buffer", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, push_buffer),
      NULL, NULL, __kms_marshal_ENUM__BOXED,
      GST_TYPE_FLOW_RETURN, 1, GST_TYPE_BUFFER);

  http_ep_signals[SIGNAL_END_OF_STREAM] =
      g_signal_new ("end-of-stream", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, end_of_stream),
      NULL, NULL, __kms_marshal_ENUM__VOID,
      GST_TYPE_FLOW_RETURN, 0, G_TYPE_NONE);

  klass->push_buffer = kms_http_end_point_push_buffer_action;
  klass->end_of_stream = kms_http_end_point_end_of_stream_action;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEndPointPrivate));
}

static void
kms_http_end_point_init (KmsHttpEndPoint * self)
{
  self->priv = KMS_HTTP_END_POINT_GET_PRIVATE (self);

  self->priv->post_pipeline = NULL;

}

gboolean
kms_http_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_END_POINT);
}
