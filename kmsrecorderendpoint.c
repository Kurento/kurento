#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsrecorderendpoint.h"

#define PLUGIN_NAME "recorderendpoint"

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
  gchar *uri;
};

enum
{
  PROP_0,
  PROP_URI,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndPoint, kms_recorder_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

static void
kms_recorder_end_point_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (self, "set_property");

  switch (property_id) {
    case PROP_URI:
      if (self->priv->uri != NULL)
        g_free (self->priv->uri);

      self->priv->uri = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_recorder_end_point_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsRecorderEndPoint *self = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (self, "get_property");

  switch (property_id) {
    case PROP_URI:
      g_value_set_string (value, self->priv->uri);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_recorder_end_point_dispose (GObject * object)
{
  KmsRecorderEndPoint *recorderendpoint = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (recorderendpoint, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->dispose (object);
}

void
kms_recorder_end_point_finalize (GObject * object)
{
  KmsRecorderEndPoint *recorderendpoint = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (recorderendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->finalize (object);
}

static void
kms_recorder_end_point_class_init (KmsRecorderEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "RecorderEndPoint", "Sink/Generic", "Kurento plugin recorder end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_recorder_end_point_set_property;
  gobject_class->get_property = kms_recorder_end_point_get_property;
  gobject_class->dispose = kms_recorder_end_point_dispose;
  gobject_class->finalize = kms_recorder_end_point_finalize;

  obj_properties[PROP_URI] = g_param_spec_string ("uri",
      "uri where the file will be written",
      "Set uri", NULL /* default value */ ,
      G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsRecorderEndPointPrivate));
}

static void
kms_recorder_end_point_init (KmsRecorderEndPoint * self)
{
  self->priv = KMS_RECORDER_END_POINT_GET_PRIVATE (self);
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
