#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsfilterelement.h"

#define PLUGIN_NAME "filterelement"

GST_DEBUG_CATEGORY_STATIC (kms_filter_element_debug_category);
#define GST_CAT_DEFAULT kms_filter_element_debug_category

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsFilterElement, kms_filter_element,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_filter_element_debug_category, PLUGIN_NAME,
        0, "debug category for filterelement element"));

void
kms_filter_element_dispose (GObject * object)
{
  KmsFilterElement *filter_element = KMS_FILTER_ELEMENT (object);

  GST_DEBUG_OBJECT (filter_element, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_filter_element_parent_class)->dispose (object);
}

void
kms_filter_element_finalize (GObject * object)
{
  KmsFilterElement *filter_element = KMS_FILTER_ELEMENT (object);

  GST_DEBUG_OBJECT (filter_element, "finalize");

  /* clean up object here */

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

}

static void
kms_filter_element_init (KmsFilterElement * filter_element)
{
}

gboolean
kms_filter_element_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_FILTER_ELEMENT);
}
