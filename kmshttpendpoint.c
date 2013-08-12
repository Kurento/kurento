#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
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
  GstElement *pipeline;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEndPoint, kms_http_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME,
        0, "debug category for httpendpoint element"));

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

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEndPointPrivate));
}

static void
kms_http_end_point_init (KmsHttpEndPoint * self)
{
  self->priv = KMS_HTTP_END_POINT_GET_PRIVATE (self);

  /* TODO: Create internal pipeline */
  self->priv->pipeline = NULL;
}

gboolean
kms_http_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_END_POINT);
}
