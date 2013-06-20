#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstautomuxerbin.h"

#define PLUGIN_NAME "automuxerbin"

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PROP_0,
};

#define gst_automuxer_bin_parent_class parent_class
G_DEFINE_TYPE (GstAutoMuxerBin, gst_automuxer_bin, GST_TYPE_BIN);

static void gst_automuxer_bin_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_automuxer_bin_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

/* initialize the automuxerbin's class */
static void
gst_automuxer_bin_class_init (GstAutoMuxerBinClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;

  gobject_class->set_property = gst_automuxer_bin_set_property;
  gobject_class->get_property = gst_automuxer_bin_get_property;

  gst_element_class_set_details_simple (gstelement_class,
      "Automuxer",
      "Basic/Bin",
      "Kurento plugin automuxer", "Joaquin Mengual <kini.mengual@gmail.com>");

}

static void
gst_automuxer_bin_init (GstAutoMuxerBin * filter)
{
}

static void
gst_automuxer_bin_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_automuxer_bin_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

gboolean
gst_automuxer_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_AUTOMUXER_BIN);
}
