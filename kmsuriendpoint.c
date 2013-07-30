#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsuriendpoint.h"

#define PLUGIN_NAME "uriendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_uri_end_point_debug_category);
#define GST_CAT_DEFAULT kms_uri_end_point_debug_category

enum
{
  PROP_0,
  PROP_URI,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsUriEndPoint, kms_uri_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_uri_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for uriendpoint element"));

static void
kms_uri_end_point_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsUriEndPoint *self = KMS_URI_END_POINT (object);

  GST_DEBUG_OBJECT (self, "set_property");

  switch (property_id) {
    case PROP_URI:
      if (self->uri != NULL)
        g_free (self->uri);

      self->uri = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_uri_end_point_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsUriEndPoint *self = KMS_URI_END_POINT (object);

  GST_DEBUG_OBJECT (self, "get_property");

  switch (property_id) {
    case PROP_URI:
      g_value_set_string (value, self->uri);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

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
  KmsUriEndPoint *self = KMS_URI_END_POINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  if (self->uri) {
    g_free (self->uri);
    self->uri = NULL;
  }

  G_OBJECT_CLASS (kms_uri_end_point_parent_class)->finalize (object);
}

static void
kms_uri_end_point_class_init (KmsUriEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "UriEndPoint", "Generic", "Kurento plugin uri end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_uri_end_point_set_property;
  gobject_class->get_property = kms_uri_end_point_get_property;
  gobject_class->dispose = kms_uri_end_point_dispose;
  gobject_class->finalize = kms_uri_end_point_finalize;

  obj_properties[PROP_URI] = g_param_spec_string ("uri",
      "uri where the file is located", "Set uri", NULL /* default value */ ,
      G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);
}

static void
kms_uri_end_point_init (KmsUriEndPoint * self)
{
  self->uri = NULL;
}

gboolean
kms_uri_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_URI_END_POINT);
}
