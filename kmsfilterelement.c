#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsfilterelement.h"
#include "kmsagnosticcaps.h"

#define PLUGIN_NAME "filterelement"

GST_DEBUG_CATEGORY_STATIC (kms_filter_element_debug_category);
#define GST_CAT_DEFAULT kms_filter_element_debug_category

#define KMS_FILTER_ELEMENT_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_FILTER_ELEMENT,                    \
    KmsFilterElementPrivate                     \
  )                                             \
)

#define KMS_FILTER_ELEMENT_LOCK(obj) (                          \
  g_rec_mutex_lock(&KMS_FILTER_ELEMENT(obj)->priv->mutex)       \
)

#define KMS_FILTER_ELEMENT_UNLOCK(obj) (                        \
  g_rec_mutex_unlock(&KMS_FILTER_ELEMENT(obj)->priv->mutex)     \
)

struct _KmsFilterElementPrivate
{
  GRecMutex mutex;
  gchar *filter_factory;
  GstElement *filter;
};

/* properties */
enum
{
  PROP_0,
  PROP_FILTER_FACTORY,
  PROP_FILTER
};

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsFilterElement, kms_filter_element,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_filter_element_debug_category, PLUGIN_NAME,
        0, "debug category for filterelement element"));

static void
kms_filter_element_connect_filter (KmsFilterElement * self, GstElement * filter,
    GstElement * valve, GstElement * agnosticbin)
{
  gst_bin_add (GST_BIN (self), filter);
  gst_element_sync_state_with_parent (filter);

  self->priv->filter = filter;

  gst_element_link_many (valve, filter, agnosticbin, NULL);
  g_object_set (G_OBJECT (valve), "drop", FALSE, NULL);
}

static void
kms_filter_element_set_filter (KmsFilterElement * self)
{
  GstElement *filter;
  GstPad *sink = NULL, *src = NULL;
  GstCaps *audio_caps = NULL, *video_caps = NULL;
  GstCaps *sink_caps = NULL, *src_caps = NULL;

  if (self->priv->filter != NULL) {
    GST_WARNING_OBJECT (self, "Factory changes are not currently allowed");
    return;
  }

  filter = gst_element_factory_make (self->priv->filter_factory, NULL);

  if (filter == NULL) {
    GST_ERROR_OBJECT (self, "Invalid factory \"%s\", element cannot be created",
        self->priv->filter_factory);
    return;
  }

  sink = gst_element_get_static_pad (filter, "sink");
  src = gst_element_get_static_pad (filter, "src");

  if (sink == NULL || src == NULL) {
    GST_ERROR_OBJECT (self, "Invalid factory \"%s\", unexpected pad templates",
        self->priv->filter_factory);
    g_object_unref (filter);
    goto end;
  }

  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);

  sink_caps = gst_pad_query_caps (sink, NULL);
  src_caps = gst_pad_query_caps (src, NULL);

  if (gst_caps_can_intersect (audio_caps, sink_caps) &&
      gst_caps_can_intersect (audio_caps, src_caps)) {
    GST_DEBUG_OBJECT (self, "Connecting filter to audio");
    kms_filter_element_connect_filter (self, filter,
        KMS_ELEMENT (self)->audio_valve,
        kms_element_get_audio_agnosticbin (KMS_ELEMENT (self)));
  } else if (gst_caps_can_intersect (video_caps, sink_caps)
      && gst_caps_can_intersect (video_caps, src_caps)) {
    GST_DEBUG_OBJECT (self, "Connecting filter to video");
    kms_filter_element_connect_filter (self, filter,
        KMS_ELEMENT (self)->video_valve,
        kms_element_get_video_agnosticbin (KMS_ELEMENT (self)));
  } else {
    g_object_unref (filter);
    GST_ERROR_OBJECT (self, "Filter element cannot be connected");
  }

end:
  if (sink_caps != NULL)
    gst_caps_unref (sink_caps);

  if (src_caps != NULL)
    gst_caps_unref (src_caps);

  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);

  if (sink != NULL)
    g_object_unref (sink);

  if (src != NULL)
    g_object_unref (src);
}

static void
kms_filter_element_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsFilterElement *self = KMS_FILTER_ELEMENT (object);

  GST_DEBUG_OBJECT (self, "get_property");

  KMS_FILTER_ELEMENT_LOCK (object);
  switch (prop_id) {
    case PROP_FILTER:
      g_value_set_object (value, self->priv->filter);
      break;
    case PROP_FILTER_FACTORY:
      g_value_set_string (value, self->priv->filter_factory);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
  KMS_FILTER_ELEMENT_UNLOCK (object);
}

static void
kms_filter_element_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsFilterElement *self = KMS_FILTER_ELEMENT (object);

  GST_DEBUG_OBJECT (self, "set_property");

  KMS_FILTER_ELEMENT_LOCK (object);
  switch (prop_id) {
    case PROP_FILTER_FACTORY:
      if (self->priv->filter_factory != NULL) {
        GST_WARNING_OBJECT (object,
            "Factory changes are not currently allowed");
      } else {
        self->priv->filter_factory = g_value_dup_string (value);
        if (self->priv->filter_factory == NULL)
          GST_WARNING_OBJECT (object, "Invalid factory name NULL");
        else
          kms_filter_element_set_filter (self);
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
  KMS_FILTER_ELEMENT_UNLOCK (object);
}

void
kms_filter_element_dispose (GObject * object)
{
  KmsFilterElement *filter_element = KMS_FILTER_ELEMENT (object);

  GST_DEBUG_OBJECT (filter_element, "dispose");

  /* clean up as possible.  may be called multiple times */

  /* No need to release as bin is owning the reference */
  filter_element->priv->filter = NULL;

  G_OBJECT_CLASS (kms_filter_element_parent_class)->dispose (object);
}

void
kms_filter_element_finalize (GObject * object)
{
  KmsFilterElement *filter_element = KMS_FILTER_ELEMENT (object);

  GST_DEBUG_OBJECT (filter_element, "finalize");

  /* clean up object here */
  if (filter_element->priv->filter_factory != NULL) {
    g_free (filter_element->priv->filter_factory);
    filter_element->priv->filter_factory = NULL;
  }

  g_rec_mutex_clear (&filter_element->priv->mutex);

  G_OBJECT_CLASS (kms_filter_element_parent_class)->finalize (object);
}

static void
kms_filter_element_class_init (KmsFilterElementClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "FilterElement", "Generic/Filter", "Kurento filter_element",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  gobject_class->dispose = kms_filter_element_dispose;
  gobject_class->finalize = kms_filter_element_finalize;

  gobject_class->set_property = kms_filter_element_set_property;
  gobject_class->get_property = kms_filter_element_get_property;

  /* define properties */
  g_object_class_install_property (gobject_class, PROP_FILTER_FACTORY,
      g_param_spec_string ("filter-factory", "filter-factory",
          "Factory name of the filter", NULL,
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY));
  g_object_class_install_property (gobject_class, PROP_FILTER,
      g_param_spec_object ("filter", "filter", "Filter currently used",
          GST_TYPE_ELEMENT, G_PARAM_READABLE));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsFilterElementPrivate));
}

static void
kms_filter_element_init (KmsFilterElement * self)
{
  self->priv = KMS_FILTER_ELEMENT_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->filter = NULL;
  self->priv->filter_factory = NULL;
}

gboolean
kms_filter_element_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_FILTER_ELEMENT);
}
