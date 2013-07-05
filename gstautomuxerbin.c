#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>
#include "gstagnosticbin.h"
#include "gstautomuxerbin.h"

#define PLUGIN_NAME "automuxerbin"

#define FUNNEL_NAME "funnel"

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

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory_audio =
GST_STATIC_PAD_TEMPLATE ("audio_sink_%u",
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (AUDIO_CAPS)
    );

static GstStaticPadTemplate sink_factory_video =
GST_STATIC_PAD_TEMPLATE ("video_sink_%u",
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (VIDEO_CAPS)
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src_%u",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS ("ANY")
    );

#define gst_automuxer_bin_parent_class parent_class
G_DEFINE_TYPE (GstAutoMuxerBin, gst_automuxer_bin, GST_TYPE_BIN);

static void gst_automuxer_bin_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_automuxer_bin_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static void
gst_automuxer_bin_typefind_have_type (GstElement * typefind,
    guint prob, GstCaps * caps, GstAutoMuxerBin * automuxerbin)
{
  GList *muxer_list, *filtered_list, *l;
  GstElementFactory *muxer_factory = NULL;

  muxer_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_MUXER,
      GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (muxer_list, caps, GST_PAD_SINK, FALSE);
  for (l = filtered_list; l != NULL && muxer_factory == NULL; l = l->next) {
    muxer_factory = GST_ELEMENT_FACTORY (l->data);
  }

  if (muxer_factory != NULL) {
    GstPad *target, *srcpad;
    GstPadTemplate *templ;
    GstElement *muxer = gst_element_factory_create (muxer_factory, NULL);

    gst_bin_add (GST_BIN (automuxerbin), muxer);
    gst_element_sync_state_with_parent (muxer);
    gst_element_link (typefind, muxer);

    target = gst_element_get_static_pad (muxer, "src");
    templ = gst_static_pad_template_get (&src_factory);
    srcpad = gst_ghost_pad_new_from_template ("src", target, templ);

    if (GST_STATE (typefind) >= GST_STATE_PAUSED
        || GST_STATE_PENDING (typefind) >= GST_STATE_PAUSED
        || GST_STATE_TARGET (typefind) >= GST_STATE_PAUSED)
      gst_pad_set_active (srcpad, TRUE);

    gst_element_add_pad (GST_ELEMENT (automuxerbin), srcpad);

    g_object_unref (templ);
    g_object_unref (target);
  }
  gst_plugin_feature_list_free (muxer_list);
  gst_plugin_feature_list_free (filtered_list);

}

static GstPad *
gst_automuxer_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstElementClass *klass = GST_ELEMENT_GET_CLASS (element);
  GstPad *pad, *target;
  gchar *pad_name = NULL;
  GstAutoMuxerBin *automuxerbin = GST_AUTOMUXER_BIN (element);
  GstElement *typefind;

  GST_AUTOMUXER_BIN_LOCK (element);

  if (templ == gst_element_class_get_pad_template (klass, "audio_sink_%u"))
    pad_name = g_strdup_printf ("audio_sink_%d", automuxerbin->pad_count++);
  else if (templ == gst_element_class_get_pad_template (klass, "video_sink_%u"))
    pad_name = g_strdup_printf ("video_sink_%d", automuxerbin->pad_count++);
  else {
    GST_AUTOMUXER_BIN_LOCK (element);
    return NULL;
  }
  GST_AUTOMUXER_BIN_LOCK (element);

  typefind = gst_element_factory_make ("typefind", NULL);
  gst_bin_add (GST_BIN (automuxerbin), typefind);
  gst_element_sync_state_with_parent (typefind);
  target = gst_element_get_static_pad (typefind, "sink");

  pad = gst_ghost_pad_new_from_template (pad_name, target, templ);

  g_object_unref (target);
  g_free (pad_name);

  g_signal_connect (typefind, "have-type",
      G_CALLBACK (gst_automuxer_bin_typefind_have_type), automuxerbin);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (gst_element_add_pad (element, pad))
    return pad;

  g_object_unref (pad);

  return NULL;
}

static void
gst_automuxer_bin_release_pad (GstElement * element, GstPad * pad)
{

  GstElement *typefind;
  GstPad *target;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target != NULL) {
    typefind = gst_pad_get_parent_element (target);
    g_object_unref (target);

    if (typefind != NULL) {
      gst_element_set_state (typefind, GST_STATE_NULL);
      gst_bin_remove (GST_BIN (element), typefind);
      g_object_unref (typefind);
    }
  }

  gst_element_remove_pad (element, pad);
}

static void
gst_automuxer_bin_dispose (GObject * object)
{
  GstAutoMuxerBin *automuxerbin = GST_AUTOMUXER_BIN (object);

  g_rec_mutex_clear (&automuxerbin->mutex);
  G_OBJECT_CLASS (gst_automuxer_bin_parent_class)->dispose (object);
}

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
  gobject_class->dispose = gst_automuxer_bin_dispose;

  gst_element_class_set_details_simple (gstelement_class,
      "Automuxer",
      "Basic/Bin",
      "Kurento plugin automuxer", "Joaquin Mengual <kini.mengual@gmail.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory_audio));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory_video));

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (gst_automuxer_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (gst_automuxer_bin_release_pad);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

}

static void
gst_automuxer_bin_init (GstAutoMuxerBin * automuxerbin)
{

  g_rec_mutex_init (&automuxerbin->mutex);

  automuxerbin->pad_count = 0;

  g_object_set (G_OBJECT (automuxerbin), "async-handling", TRUE, NULL);
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
