#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kms-marshal.h"
#include "kmshttpendpoint.h"

#define PLUGIN_NAME "httpendpoint"

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
  LAST_SIGNAL
};

static guint http_ep_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEndPoint, kms_http_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME,
        0, "debug category for httpendpoint element"));

static void
post_decodebin_pad_added_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndPoint * self)
{
  GST_INFO ("Pad added %s", gst_pad_get_name (pad));
}

static void
post_decodebin_pad_removed_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndPoint * self)
{
  GST_INFO ("Pad removed %s", gst_pad_get_name (pad));
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
      G_CALLBACK (post_decodebin_pad_added_handler), NULL);
  g_signal_connect (decodebin, "pad-removed",
      G_CALLBACK (post_decodebin_pad_removed_handler), NULL);

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

  klass->push_buffer = kms_http_end_point_push_buffer_action;

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
