
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstagnosticbin.h"

#define PLUGIN_NAME "agnosticbin"

#define AUDIO_CAPS "audio/x-raw;"\
  "audio/x-sbc;" \
  "audio/x-mulaw;" \
  "audio/x-flac;" \
  "audio/x-alaw;" \
  "audio/x-speex;" \
  "audio/x-ac3;" \
  "audio/x-alac;" \
  "audio/mpeg,mpegversion=1,layer=2;" \
  "audio/x-nellymoser;" \
  "audio/x-gst_ff-sonic;" \
  "audio/x-gst_ff-sonicls;" \
  "audio/x-wma,wmaversion=1;" \
  "audio/x-wma,wmaversion=2;" \
  "audio/x-dpcm,layout=roq;" \
  "audio/x-adpcm,layout=adx;" \
  "audio/x-adpcm,layout=g726;" \
  "audio/x-adpcm,layout=quicktime;" \
  "audio/x-adpcm,layout=dvi;" \
  "audio/x-adpcm,layout=microsoft;" \
  "audio/x-adpcm,layout=swf;" \
  "audio/x-adpcm,layout=yamaha;" \
  "audio/mpeg,mpegversion=4;" \
  "audio/mpeg,mpegversion=1,layer=3;" \
  "audio/x-celt;" \
  "audio/mpeg,mpegversion=[2, 4];" \
  "audio/x-vorbis;" \
  "audio/x-opus;" \
  "audio/AMR,rate=[8000, 16000],channels=1;" \
  "audio/x-gsm;"

#define VIDEO_CAPS "video/x-raw;" \
  "video/x-dirac;" \
  "image/png;" \
  "image/jpeg;" \
  "video/x-smoke;" \
  "video/x-asus,asusversion=1;" \
  "video/x-asus,asusversion=2;" \
  "image/bmp;" \
  "video/x-dnxhd;" \
  "video/x-dv;" \
  "video/x-ffv,ffvversion=1;" \
  "video/x-gst_ff-ffvhuff;" \
  "video/x-flash-screen;" \
  "video/x-flash-video,flvversion=1;" \
  "video/x-h261;" \
  "video/x-h263,variant=itu,h263version=h263;" \
  "video/x-h263,variant=itu,h263version=h263p;" \
  "video/x-huffyuv;" \
  "image/jpeg;" \
  "image/jpeg;" \
  "video/mpeg,mpegversion=1;" \
  "video/mpeg,mpegversion=2;" \
  "video/mpeg,mpegversion=4;" \
  "video/x-msmpeg,msmpegversion=41;" \
  "video/x-msmpeg,msmpegversion=42;" \
  "video/x-msmpeg,msmpegversion=43;" \
  "video/x-gst_ff-pam;" \
  "image/pbm;" \
  "video/x-gst_ff-pgm;" \
  "video/x-gst_ff-pgmyuv;" \
  "image/png;" \
  "image/ppm;" \
  "video/x-rle,layout=quicktime;" \
  "video/x-gst_ff-roqvideo;" \
  "video/x-pn-realvideo,rmversion=1;" \
  "video/x-pn-realvideo,rmversion=2;" \
  "video/x-gst_ff-snow;" \
  "video/x-svq,svqversion=1;" \
  "video/x-wmv,wmvversion=1;" \
  "video/x-wmv,wmvversion=2;" \
  "video/x-gst_ff-zmbv;" \
  "video/x-theora;" \
  "video/x-h264;" \
  "video/x-gst_ff-libxvid;" \
  "video/x-h264;" \
  "video/x-xvid;" \
  "video/mpeg,mpegversion=[1, 2];" \
  "video/x-theora;" \
  "video/x-vp8;" \
  "application/x-yuv4mpeg,y4mversion=2;" \

#define AGNOSTIC_CAPS AUDIO_CAPS VIDEO_CAPS

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
    GST_STATIC_CAPS (AGNOSTIC_CAPS)
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (AGNOSTIC_CAPS)
    );

static void
gst_agnostic_bin_dispose_sink_caps (GstAgnosticBin * agnosticbin)
{
  if (agnosticbin->sink_caps != NULL) {
    gst_caps_unref (agnosticbin->sink_caps);
    agnosticbin->sink_caps = NULL;
  }
}

static void
connect_srcpad (GstAgnosticBin * agnosticbin, GstPad * srcpad)
{
  GstCaps *caps;

  GST_PAD_STREAM_LOCK (agnosticbin->sinkpad);

  caps = gst_pad_get_allowed_caps (srcpad);
  GST_DEBUG ("allowed caps pad: %P", caps);
  gst_caps_unref (caps);

  if (!agnosticbin->sink_caps) {
    GST_DEBUG ("agnosticbin->sink_caps not assigned yet.");
    goto end;
  }

  GST_DEBUG ("agnosticbin->sink_caps already assigned.");

end:
  GST_PAD_STREAM_UNLOCK (agnosticbin->sinkpad);
}

static void
connect_previous_srcpads (GstAgnosticBin * agnosticbin,
    const GstCaps * sinkpad_caps)
{
  GValue item = { 0, };
  GstIterator *it;
  gboolean done;
  GstPad *srcpad;

  GST_PAD_STREAM_LOCK (agnosticbin->sinkpad);
  gst_agnostic_bin_dispose_sink_caps (agnosticbin);
  agnosticbin->sink_caps = gst_caps_copy (sinkpad_caps);
  GST_PAD_STREAM_UNLOCK (agnosticbin->sinkpad);
  GST_DEBUG ("sinkpad_caps: %P", sinkpad_caps);

  it = gst_element_iterate_src_pads (GST_ELEMENT (agnosticbin));
  done = FALSE;
  while (!done) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:
        srcpad = g_value_get_object (&item);
        connect_srcpad (agnosticbin, srcpad);
        g_value_reset (&item);
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        done = TRUE;
        break;
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  }
  g_value_unset (&item);
  gst_iterator_free (it);
}

/* GstElement vmethod implementations */
static gboolean
gst_agnostic_bin_src_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  GST_INFO ("gst_agnostic_bin_src_event, pad: %P, event: %P", pad, event);
  return TRUE;
}

/* this function handles sink events */
static gboolean
gst_agnostic_bin_sink_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  gboolean ret;
  GstCaps *caps;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CAPS:
      gst_event_parse_caps (event, &caps);
      connect_previous_srcpads (GST_AGNOSTIC_BIN (parent), caps);
    default:
      ret = gst_pad_event_default (pad, parent, event);
      break;
  }
  return ret;
}

static GstPadLinkReturn
gst_agnostic_bin_link_src (GstPad * pad, GstObject * parent, GstPad * peer)
{
  connect_srcpad (GST_AGNOSTIC_BIN (parent), pad);
  return GST_PAD_LINK_OK;
}

static GstPad *
gst_agnostic_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *srcpad;

  srcpad = gst_pad_new_from_template (templ, name);
  //TODO: Set pad active if element is in PAUSE or PLAYING states
  gst_pad_set_link_function (srcpad, gst_agnostic_bin_link_src);
  gst_pad_set_event_function (srcpad,
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_src_event));

  gst_element_add_pad (element, srcpad);

  return srcpad;
}

static void
gst_agnostic_bin_release_pad (GstElement * element, GstPad * pad)
{
  GST_INFO ("gst_agnostic_bin_release_pad. pad: %P", pad);
  //TODO: Set pad inactive if element is in PAUSE of PLAYING states
  gst_element_remove_pad (element, pad);
}

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

static void
gst_agnostic_bin_dispose (GObject * object)
{
  GstAgnosticBin *agnostic = GST_AGNOSTIC_BIN (object);

  gst_agnostic_bin_dispose_sink_caps (agnostic);
  G_OBJECT_CLASS (gst_agnostic_bin_parent_class)->dispose (object);
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
  gobject_class->dispose = gst_agnostic_bin_dispose;

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

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_release_pad);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, "agnosticbin", 0, "agnosticbin");
}

static void
gst_agnostic_bin_init (GstAgnosticBin * agnosticbin)
{
  GstPad *target_sink;
  GstElement *tee, *queue, *fakesink;
  GstPadTemplate *templ;

  tee = gst_element_factory_make ("tee", NULL);
  queue = gst_element_factory_make ("queue", NULL);
  fakesink = gst_element_factory_make ("fakesink", NULL);

  gst_bin_add_many (GST_BIN (agnosticbin), tee, queue, fakesink, NULL);
  gst_element_link_many (tee, queue, fakesink, NULL);

  g_object_set (G_OBJECT (queue), "leaky", 2, "max-size-time",
      GST_MSECOND * 100, NULL);

  target_sink = gst_element_get_static_pad (tee, "sink");
  templ = gst_static_pad_template_get (&sink_factory);

  agnosticbin->sink_caps = NULL;
  agnosticbin->sinkpad =
      gst_ghost_pad_new_from_template ("sink", target_sink, templ);

  g_object_unref (templ);

  g_object_unref (target_sink);

  gst_pad_set_event_function (agnosticbin->sinkpad,
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_sink_event));
  GST_PAD_SET_PROXY_CAPS (agnosticbin->sinkpad);
  gst_element_add_pad (GST_ELEMENT (agnosticbin), agnosticbin->sinkpad);
}

gboolean
gst_agnostic_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_AGNOSTIC_BIN);
}
