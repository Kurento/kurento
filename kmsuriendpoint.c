#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsuriendpoint.h"

#define PLUGIN_NAME "uriendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_uri_end_point_debug_category);
#define GST_CAT_DEFAULT kms_uri_end_point_debug_category

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsUriEndPoint, kms_uri_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_uri_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for uriendpoint element"));

static void
kms_uri_end_point_dispose (GObject * object)
{
  KmsUriEndPoint *uriendpoint = KMS_URI_END_POINT (object);

  GST_DEBUG_OBJECT (uriendpoint, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_uri_end_point_parent_class)->dispose (object);
}

static void
kms_uri_end_point_finalize (GObject * object)
{
  KmsUriEndPoint *uriendpoint = KMS_URI_END_POINT (object);

  GST_DEBUG_OBJECT (uriendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_uri_end_point_parent_class)->finalize (object);
}

static void
kms_uri_end_point_class_init (KmsUriEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "UriEndPoint", "Generic", "Kurento plugin uri end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->dispose = kms_uri_end_point_dispose;
  gobject_class->finalize = kms_uri_end_point_finalize;

}

static void
kms_uri_end_point_init (KmsUriEndPoint * uriendpoint)
{
}

gboolean
kms_uri_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_URI_END_POINT);
}
