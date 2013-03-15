
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstagnosticbin.h"

#define PLUGIN_NAME "agnosticbin"

GST_DEBUG_CATEGORY_STATIC (gst_agnostic_bin_debug);
#define GST_CAT_DEFAULT gst_agnostic_bin_debug

#define gst_agnostic_bin_parent_class parent_class
G_DEFINE_TYPE (GstAgnosticBin, gst_agnostic_bin, GST_TYPE_BIN);

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PROP_0
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("ANY")
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("ANY")
    );

static void
gst_agnostic_bin_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_agnostic_bin_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/* GstElement vmethod implementations */

/* this function handles sink events */
static gboolean
gst_agnostic_bin_sink_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  gboolean ret;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CAPS:
    {
      GstCaps *caps;

      gst_event_parse_caps (event, &caps);
      /* do something with the caps */

      /* and forward */
      ret = gst_pad_event_default (pad, parent, event);
      break;
    }
    default:
      ret = gst_pad_event_default (pad, parent, event);
      break;
  }
  return ret;
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn
gst_agnostic_bin_chain (GstPad * pad, GstObject * parent, GstBuffer * buf)
{
  GstAgnosticBin *filter;

  filter = GST_AGNOSTICBIN (parent);

  /* just push out the incoming buffer without touching it */
  return gst_pad_push (filter->srcpad, buf);
}

static void
gst_agnostic_bin_class_init (GstAgnosticBinClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstelement_class = GST_ELEMENT_CLASS (klass);

  gobject_class->set_property = gst_agnostic_bin_set_property;
  gobject_class->get_property = gst_agnostic_bin_get_property;

  gst_element_class_set_details_simple (gstelement_class,
      "Agnostic connector",
      "Generic/Bin/Connector",
      "Automatically encodes/decodes media to match sink and source pads caps",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>, "
      "Miguel París Díaz <mparisdiaz@gmail.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, "agnosticbin", 0, "agnosticbin");
}

static void
gst_agnostic_bin_init (GstAgnosticBin * filter)
{
  filter->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink");
  gst_pad_set_event_function (filter->sinkpad,
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_sink_event));
  gst_pad_set_chain_function (filter->sinkpad,
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_chain));
  GST_PAD_SET_PROXY_CAPS (filter->sinkpad);
  gst_element_add_pad (GST_ELEMENT (filter), filter->sinkpad);

  filter->srcpad = gst_pad_new_from_static_template (&src_factory, "src");
  GST_PAD_SET_PROXY_CAPS (filter->srcpad);
  gst_element_add_pad (GST_ELEMENT (filter), filter->srcpad);
}

gboolean
gst_agnostic_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_AGNOSTICBIN);
}
