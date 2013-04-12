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
  PROP_0,
  PROP_PATTERN_SDP,
  PROP_LOCAL_OFFER_SDP,
  PROP_LOCAL_ANSWER_SDP,
  PROP_REMOTE_OFFER_SDP,
  PROP_REMOTE_ANSWER_SDP
};

static guint gst_base_stream_signals[LAST_SIGNAL] = { 0 };

static void
gst_base_stream_release_pattern_sdp (GstBaseStream * base_stream)
{
  if (base_stream->pattern_sdp == NULL)
    return;

  gst_sdp_message_free (base_stream->pattern_sdp);
  base_stream->pattern_sdp = NULL;
}

static void
gst_base_stream_release_local_offer_sdp (GstBaseStream * base_stream)
{
  if (base_stream->local_offer_sdp == NULL)
    return;

  gst_sdp_message_free (base_stream->local_offer_sdp);
  base_stream->local_offer_sdp = NULL;
}

static void
gst_base_stream_release_local_answer_sdp (GstBaseStream * base_stream)
{
  if (base_stream->local_answer_sdp == NULL)
    return;

  gst_sdp_message_free (base_stream->local_answer_sdp);
  base_stream->local_answer_sdp = NULL;
}

static void
gst_base_stream_release_remote_offer_sdp (GstBaseStream * base_stream)
{
  if (base_stream->remote_offer_sdp == NULL)
    return;

  gst_sdp_message_free (base_stream->remote_offer_sdp);
  base_stream->remote_offer_sdp = NULL;
}

static void
gst_base_stream_release_remote_answer_sdp (GstBaseStream * base_stream)
{
  if (base_stream->remote_answer_sdp == NULL)
    return;

  gst_sdp_message_free (base_stream->remote_answer_sdp);
  base_stream->remote_answer_sdp = NULL;
}

static gboolean
gst_base_stream_set_transport_to_sdp (GstBaseStream * base_stream,
    GstSDPMessage * msg)
{
  /* Defalut function, do nothing */
  return TRUE;
}

static GstSDPMessage *
gst_base_stream_generate_offer (GstBaseStream * base_stream)
{
  GstSDPMessage *offer = NULL;
  GstBaseStreamClass *base_stream_class =
      GST_BASE_STREAM_CLASS (G_OBJECT_GET_CLASS (base_stream));

  GST_DEBUG ("generate_offer");

  GST_OBJECT_LOCK (base_stream);
  if (base_stream->pattern_sdp != NULL) {
    gst_sdp_message_copy (base_stream->pattern_sdp, &offer);
  }
  GST_OBJECT_UNLOCK (base_stream);

  if (offer == NULL)
    return NULL;

  if (base_stream_class->set_transport_to_sdp ==
      gst_base_stream_set_transport_to_sdp) {
    g_warning ("%s does not reimplement \"set_transport_to_sdp\"",
        G_OBJECT_CLASS_NAME (base_stream_class));
  }

  if (!base_stream_class->set_transport_to_sdp (base_stream, offer)) {
    gst_sdp_message_free (offer);
    return NULL;
  }

  return offer;
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
gst_base_stream_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstBaseStream *base_stream = GST_BASE_STREAM (object);

  switch (prop_id) {
    case PROP_PATTERN_SDP:
      GST_OBJECT_LOCK (base_stream);
      gst_base_stream_release_pattern_sdp (base_stream);
      base_stream->pattern_sdp = g_value_dup_boxed (value);
      GST_OBJECT_UNLOCK (base_stream);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_base_stream_get_property (GObject * object, guint prop_id, GValue * value,
    GParamSpec * pspec)
{
  GstBaseStream *base_stream = GST_BASE_STREAM (object);

  switch (prop_id) {
    case PROP_PATTERN_SDP:
      g_value_set_boxed (value, base_stream->pattern_sdp);
      break;
    case PROP_LOCAL_OFFER_SDP:
      g_value_set_boxed (value, base_stream->local_offer_sdp);
      break;
    case PROP_LOCAL_ANSWER_SDP:
      g_value_set_boxed (value, base_stream->local_answer_sdp);
      break;
    case PROP_REMOTE_OFFER_SDP:
      g_value_set_boxed (value, base_stream->remote_offer_sdp);
      break;
    case PROP_REMOTE_ANSWER_SDP:
      g_value_set_boxed (value, base_stream->remote_answer_sdp);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_base_stream_dispose (GObject * object)
{
  GstBaseStream *base_stream = GST_BASE_STREAM (object);

  gst_base_stream_release_pattern_sdp (base_stream);
  gst_base_stream_release_local_offer_sdp (base_stream);
  gst_base_stream_release_local_answer_sdp (base_stream);
  gst_base_stream_release_remote_offer_sdp (base_stream);
  gst_base_stream_release_remote_answer_sdp (base_stream);
}

static void
gst_base_stream_class_init (GstBaseStreamClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->get_property = gst_base_stream_get_property;
  gobject_class->set_property = gst_base_stream_set_property;
  gobject_class->dispose = gst_base_stream_dispose;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseStream",
      "Base/Bin/BaseStream",
      "Base class for streams",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  klass->set_transport_to_sdp = gst_base_stream_set_transport_to_sdp;

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

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_PATTERN_SDP,
      g_param_spec_boxed ("pattern-sdp", "Pattern to create local sdps",
          "Pattern to create \"local-offer-sdp\" and \"local-answer-sdp\"",
          GST_TYPE_SDP_MESSAGE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_READY));

  g_object_class_install_property (gobject_class, PROP_LOCAL_OFFER_SDP,
      g_param_spec_boxed ("local-offer-sdp", "Local offer sdp",
          "The local generated offer, negotiated with \"remote-answer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_REMOTE_OFFER_SDP,
      g_param_spec_boxed ("remote-offer-sdp", "Remote offer sdp",
          "The remote offer, negotiated with \"local-answer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_LOCAL_ANSWER_SDP,
      g_param_spec_boxed ("local-answer-sdp", "Local answer sdp",
          "The local negotiated answer, negotiated with \"remote-offer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_REMOTE_ANSWER_SDP,
      g_param_spec_boxed ("remote-answer-sdp", "Remote answer sdp",
          "The remote answer, negotiated with \"local-offer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
}

static void
gst_base_stream_init (GstBaseStream * base_stream)
{
  base_stream->pattern_sdp = NULL;
  base_stream->local_offer_sdp = NULL;
  base_stream->local_answer_sdp = NULL;
  base_stream->remote_offer_sdp = NULL;
  base_stream->remote_answer_sdp = NULL;
}
