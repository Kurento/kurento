#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstbasestream.h"
#include "gstagnosticbin.h"

#define PLUGIN_NAME "base_stream"

GST_DEBUG_CATEGORY_STATIC (gst_base_stream_debug);
#define GST_CAT_DEFAULT gst_base_stream_debug

#define gst_base_stream_parent_class parent_class
G_DEFINE_TYPE (GstBaseStream, gst_base_stream, GST_TYPE_JOINABLE);

/* Signals and args */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0
};

static void
gst_base_stream_class_init (GstBaseStreamClass * klass)
{
  GstElementClass *gstelement_class;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseStream",
      "Base/Bin/BaseStream",
      "Base class for streams",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
gst_base_stream_init (GstBaseStream * base_stream)
{
}
