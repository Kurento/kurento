#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsplayerendpoint.h"

#define PLUGIN_NAME "playerendpoint"

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
  gchar *uri;
};

/* properties */
enum
{
  PROP_0,
  PROP_URI
};

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlayerEndPoint, kms_player_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_player_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for playerendpoint element"));

static void
kms_player_end_point_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (object);

  GST_DEBUG_OBJECT (self, "get_property");

  switch (prop_id) {
    case PROP_URI:
      g_value_set_string (value, self->priv->uri);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_player_end_point_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (object);

  GST_DEBUG_OBJECT (self, "set_property");

  switch (prop_id) {
    case PROP_URI:
      if (self->priv->uri != NULL)
        g_free (self->priv->uri);
      self->priv->uri = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

void
kms_player_end_point_dispose (GObject * object)
{
  KmsPlayerEndPoint *playerendpoint = KMS_PLAYER_END_POINT (object);

  GST_DEBUG_OBJECT (playerendpoint, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_player_end_point_parent_class)->dispose (object);
}

void
kms_player_end_point_finalize (GObject * object)
{
  KmsPlayerEndPoint *playerendpoint = KMS_PLAYER_END_POINT (object);

  GST_DEBUG_OBJECT (playerendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_player_end_point_parent_class)->finalize (object);
}

static void
kms_player_end_point_class_init (KmsPlayerEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlayerEndPoint", "Sink/Generic", "Kurento plugin player end point",
      "Joaquin Mengual García <kini.mengual@gmail.com>");

  gobject_class->dispose = kms_player_end_point_dispose;
  gobject_class->finalize = kms_player_end_point_finalize;

  /* define virtual function pointers */
  gobject_class->set_property = kms_player_end_point_set_property;
  gobject_class->get_property = kms_player_end_point_get_property;

  /* define properties */
  g_object_class_install_property (gobject_class, PROP_URI,
      g_param_spec_string ("uri", "uri", "URI of the resource to play", NULL,
          G_PARAM_READABLE | G_PARAM_WRITABLE));
  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlayerEndPointPrivate));

}

static void
kms_player_end_point_init (KmsPlayerEndPoint * self)
{
  self->priv = KMS_PLAYER_END_POINT_GET_PRIVATE (self);
}

gboolean
kms_player_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLAYER_END_POINT);
}
