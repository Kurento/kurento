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

static GstFlowReturn
kms_http_end_point_push_buffer_action (KmsHttpEndPoint * self,
    GstBuffer * buffer)
{
  g_return_val_if_fail (GST_IS_BUFFER (buffer), GST_FLOW_ERROR);

  /* TODO: Send buffer to internal appsrc */
  GST_DEBUG ("Received new buffer %P", buffer);
  gst_buffer_unref (buffer);
  return GST_FLOW_ERROR;
}

static void
kms_http_end_point_dispose (GObject * object)
{
  KmsHttpEndPoint *self = KMS_HTTP_END_POINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

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
