#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstbasestream.h"
#include "gstagnosticbin.h"
#include "gstkurento-marshal.h"

#define PLUGIN_NAME "base_stream"

GST_DEBUG_CATEGORY_STATIC (gst_base_stream_debug);
#define GST_CAT_DEFAULT gst_base_stream_debug

#define gst_base_stream_parent_class parent_class
G_DEFINE_TYPE (GstBaseStream, gst_base_stream, GST_TYPE_JOINABLE);

/* Signals and args */
enum
{
  SIGNAL_GENERATE_OFFER,
  SIGNAL_GENERATE_ANSWER,
  SIGNAL_PROCESS_ANSWER,
  LAST_SIGNAL
};

enum
{
  PROP_0
};

static guint gst_base_stream_signals[LAST_SIGNAL] = { 0 };

GstSDPMessage *
gst_base_stream_generate_offer (GstBaseStream * base_stream)
{
  GST_DEBUG ("generate_offer");
  return NULL;
}

GstSDPMessage *
gst_base_stream_generate_answer (GstBaseStream * base_stream,
    GstSDPMessage * offer)
{
  GST_DEBUG ("generate_answer");
  return NULL;
}

void
gst_base_stream_process_answer (GstBaseStream * base_stream,
    GstSDPMessage * answer)
{
  GST_DEBUG ("process_answer");
}

static void
gst_base_stream_class_init (GstBaseStreamClass * klass)
{
  GstElementClass *gstelement_class;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseStream",
      "Base/Bin/BaseStream",
      "Base class for streams",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  klass->generate_offer = gst_base_stream_generate_offer;
  klass->generate_answer = gst_base_stream_generate_answer;
  klass->process_answer = gst_base_stream_process_answer;

  /* Signals initialization */
  gst_base_stream_signals[SIGNAL_GENERATE_OFFER] =
      g_signal_new ("generate-offer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstBaseStreamClass,
          generate_offer), NULL, NULL,
      __gst_kurento_marshal_BOXED__VOID, GST_TYPE_SDP_MESSAGE, 0);

  gst_base_stream_signals[SIGNAL_GENERATE_ANSWER] =
      g_signal_new ("generate-answer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstBaseStreamClass,
          generate_answer), NULL, NULL,
      __gst_kurento_marshal_BOXED__BOXED, GST_TYPE_SDP_MESSAGE, 1,
      GST_TYPE_SDP_MESSAGE);

  gst_base_stream_signals[SIGNAL_PROCESS_ANSWER] =
      g_signal_new ("process-answer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstBaseStreamClass,
          process_answer), NULL, NULL,
      __gst_kurento_marshal_VOID__BOXED, G_TYPE_NONE, 1, GST_TYPE_SDP_MESSAGE);
}

static void
gst_base_stream_init (GstBaseStream * base_stream)
{
}
