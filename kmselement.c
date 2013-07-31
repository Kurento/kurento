#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "kmselement.h"
#include "kmsagnosticbin.h"

#define PLUGIN_NAME "element"

GST_DEBUG_CATEGORY_STATIC (kms_element_debug_category);
#define GST_CAT_DEFAULT kms_element_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsElement, kms_element,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_element_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define AUDIO_AGNOSTICBIN "audio_agnosticbin"
#define VIDEO_AGNOSTICBIN "video_agnosticbin"
#define AUDIO_VALVE "audio_valve"
#define VIDEO_VALVE "video_valve"

#define AUDIO_SINK_PAD "audio_sink"
#define VIDEO_SINK_PAD "video_sink"

/* Signals and args */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (AUDIO_CAPS)
    );

static GstStaticPadTemplate video_sink_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (VIDEO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE ("audio_src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (AUDIO_CAPS)
    );

static GstStaticPadTemplate video_src_factory =
GST_STATIC_PAD_TEMPLATE ("video_src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (VIDEO_CAPS)
    );

static GstPad *
kms_element_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *ret_pad, *agnostic_pad;
  GstElement *agnosticbin;
  gchar *pad_name, *pad_name_prefix;

  if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), "audio_src_%u")) {
    agnosticbin = KMS_ELEMENT (element)->audio_agnosticbin;
    pad_name_prefix = "audio_";
  } else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), "video_src_%u")) {
    agnosticbin = KMS_ELEMENT (element)->video_agnosticbin;
    pad_name_prefix = "video_";
  } else {
    return NULL;
  }

  agnostic_pad = gst_element_get_request_pad (agnosticbin, "src_%u");

  pad_name =
      g_strdup_printf ("%s%s", pad_name_prefix, GST_OBJECT_NAME (agnostic_pad));
  ret_pad = gst_ghost_pad_new_from_template (pad_name, agnostic_pad, templ);
  g_free (pad_name);

  if (ret_pad == NULL)
    return NULL;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (ret_pad, TRUE);

  if (gst_element_add_pad (element, ret_pad))
    return ret_pad;

  g_object_unref (ret_pad);
  gst_element_release_request_pad (agnosticbin, agnostic_pad);

  return NULL;
}

static void
kms_element_release_pad (GstElement * element, GstPad * pad)
{
  GstElement *agnosticbin;
  GstPad *target;

  if (g_str_has_prefix ("audio_src", GST_OBJECT_NAME (pad))) {
    agnosticbin = KMS_ELEMENT (element)->audio_agnosticbin;
  } else if (g_str_has_prefix ("audio_src", GST_OBJECT_NAME (pad))) {
    agnosticbin = KMS_ELEMENT (element)->audio_agnosticbin;
  } else {
    return;
  }

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target != NULL && agnosticbin != NULL)
    gst_element_release_request_pad (agnosticbin, target);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  gst_element_remove_pad (element, pad);
}

static void
kms_element_finalize (GObject * object)
{
  KmsElement *element = KMS_ELEMENT (object);

  /* free resources allocated by this object */
  g_rec_mutex_clear (&element->mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_element_parent_class)->finalize (object);
}

static void
kms_element_class_init (KmsElementClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_element_finalize;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "KmsElement",
      "Base/Bin/KmsElement",
      "Base class for elements",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");
  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_element_request_new_pad);
  gstelement_class->release_pad = GST_DEBUG_FUNCPTR (kms_element_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_sink_factory));
}

static void
kms_element_init (KmsElement * element)
{
  GstPad *audio_valve_sink, *video_valve_sink;
  GstPad *audio_sink, *video_sink;

  g_rec_mutex_init (&element->mutex);

  element->audio_agnosticbin =
      gst_element_factory_make ("agnosticbin", AUDIO_AGNOSTICBIN);
  element->video_agnosticbin =
      gst_element_factory_make ("agnosticbin", VIDEO_AGNOSTICBIN);

  element->audio_valve = gst_element_factory_make ("valve", AUDIO_VALVE);
  element->video_valve = gst_element_factory_make ("valve", VIDEO_VALVE);

  g_object_set (element->audio_valve, "drop", TRUE, NULL);
  g_object_set (element->video_valve, "drop", TRUE, NULL);

  gst_bin_add_many (GST_BIN (element), element->audio_agnosticbin,
      element->video_agnosticbin, element->audio_valve, element->video_valve,
      NULL);

  audio_valve_sink = gst_element_get_static_pad (element->audio_valve, "sink");
  video_valve_sink = gst_element_get_static_pad (element->video_valve, "sink");

  audio_sink =
      gst_ghost_pad_new_from_template (AUDIO_SINK_PAD, audio_valve_sink,
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), AUDIO_SINK_PAD));
  video_sink =
      gst_ghost_pad_new_from_template (VIDEO_SINK_PAD, video_valve_sink,
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), VIDEO_SINK_PAD));

  gst_element_add_pad (GST_ELEMENT (element), audio_sink);
  gst_element_add_pad (GST_ELEMENT (element), video_sink);

  g_object_set (G_OBJECT (element), "async-handling", TRUE, NULL);
}
