/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmshubport.h"
#include "kmsagnosticcaps.h"
#include "kmsutils.h"

#define PLUGIN_NAME "hubport"

#define KEY_ELEM_DATA "kms-hub-elem-data"
G_DEFINE_QUARK (KEY_ELEM_DATA, key_elem_data)

#define KEY_TYPE_DATA "kms-hub-type-data"
G_DEFINE_QUARK (KEY_TYPE_DATA, key_type_data)

#define KEY_PAD_DATA "kms-hub-pad-data"
G_DEFINE_QUARK (KEY_PAD_DATA, key_pad_data)

GST_DEBUG_CATEGORY_STATIC (kms_hub_port_debug_category);
#define GST_CAT_DEFAULT kms_hub_port_debug_category

#define KMS_HUB_PORT_GET_PRIVATE(obj) (         \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_HUB_PORT,                          \
    KmsHubPortPrivate                           \
  )                                             \
)

struct _KmsHubPortPrivate
{
  void *dummy;
};

/* Pad templates */
static GstStaticPadTemplate hub_audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (HUB_AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS));

static GstStaticPadTemplate hub_video_sink_factory =
GST_STATIC_PAD_TEMPLATE (HUB_VIDEO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS));

static GstStaticPadTemplate hub_data_sink_factory =
GST_STATIC_PAD_TEMPLATE (HUB_DATA_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_DATA_CAPS));

static GstStaticPadTemplate hub_audio_src_factory =
GST_STATIC_PAD_TEMPLATE (HUB_AUDIO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS));

static GstStaticPadTemplate hub_video_src_factory =
GST_STATIC_PAD_TEMPLATE (HUB_VIDEO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS));

static GstStaticPadTemplate hub_data_src_factory =
GST_STATIC_PAD_TEMPLATE (HUB_DATA_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_DATA_CAPS));

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHubPort, kms_hub_port,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_hub_port_debug_category, PLUGIN_NAME,
        0, "debug category for hubport element"));

static GstPad *
kms_hub_port_generate_sink_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps,
    GstElement * output)
{
  GstPad *output_pad, *pad;

  output_pad = gst_element_get_static_pad (output, "sink");
  pad = gst_ghost_pad_new_from_template (name, output_pad, templ);
  g_object_unref (output_pad);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED) {
    gst_pad_set_active (pad, TRUE);
  }

  if (gst_element_add_pad (element, pad))
    return pad;

  GST_ERROR_OBJECT (element, "Cannot add pad %" GST_PTR_FORMAT, pad);
  g_object_unref (pad);

  return NULL;
}

static GstPad *
kms_hub_port_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstElement *output = NULL;

  if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), HUB_AUDIO_SINK_PAD)) {

    if (g_strcmp0 (name, HUB_AUDIO_SINK_PAD) != 0) {
      GST_ERROR_OBJECT (element,
          "Invalid pad name %s for template %" GST_PTR_FORMAT, name, templ);
      return NULL;
    }

    output = kms_element_get_audio_agnosticbin (KMS_ELEMENT (element));
  }
  else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), HUB_VIDEO_SINK_PAD)) {
    if (g_strcmp0 (name, HUB_VIDEO_SINK_PAD) != 0) {
      GST_ERROR_OBJECT (element,
          "Invalid pad name %s for template %" GST_PTR_FORMAT, name, templ);
      return NULL;
    }

    output = kms_element_get_video_agnosticbin (KMS_ELEMENT (element));
  }
  else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), HUB_DATA_SINK_PAD)) {
    if (g_strcmp0 (name, HUB_DATA_SINK_PAD) != 0) {
      GST_ERROR_OBJECT (element,
          "Invalid pad name %s for template %" GST_PTR_FORMAT, name, templ);
      return NULL;
    }

    output = kms_element_get_data_tee (KMS_ELEMENT (element));
  }

  if (output == NULL) {
    GST_WARNING_OBJECT (element, "No agnosticbin got for template %"
        GST_PTR_FORMAT, templ);
    return NULL;
  } else {
    return kms_hub_port_generate_sink_pad (element, templ, name, caps, output);
  }
}

static void
kms_hub_port_internal_src_unhandled (KmsHubPort * self, GstPad * pad)
{
  GstPad *sink = g_object_get_qdata (G_OBJECT (pad), key_pad_data_quark ());

  if (sink == NULL) {
    // Classes that derive from BaseHub should link their internal elements
    // to all possible source pad types: audio, video, data.
    // Data pads are new and optional, so they might not be linked and the sink
    // here will be NULL. Audio and video pads are mandatory, so the sink here
    // should not be NULL.
    GstCaps *caps = gst_pad_query_caps (pad, NULL);
    if (!kms_utils_caps_is_data (caps)) {
      GST_ERROR_OBJECT (self, "Derived class is missing links for some SRCs");
      g_return_if_fail (sink);
    }
    return;
  }

  kms_element_remove_sink (KMS_ELEMENT (self), sink);

  g_object_set_qdata (G_OBJECT (pad), key_pad_data_quark (), NULL);
}

void
kms_hub_port_unhandled (KmsHubPort * self)
{
  GstPad *video_src, *audio_src, *data_src;

  g_return_if_fail (self);

  video_src =
      gst_element_get_static_pad (GST_ELEMENT (self), HUB_VIDEO_SRC_PAD);
  kms_hub_port_internal_src_unhandled (self, video_src);
  g_object_unref (video_src);

  audio_src =
      gst_element_get_static_pad (GST_ELEMENT (self), HUB_AUDIO_SRC_PAD);
  kms_hub_port_internal_src_unhandled (self, audio_src);
  g_object_unref (audio_src);

  data_src =
      gst_element_get_static_pad (GST_ELEMENT (self), HUB_DATA_SRC_PAD);
  kms_hub_port_internal_src_unhandled (self, data_src);
  g_object_unref (data_src);
}

static void
kms_hub_port_internal_src_pad_linked (GstPad * pad, GstPad * peer,
    gpointer data)
{
  GstPad *target, *new_pad;
  GstElement *capsfilter;
  KmsElement *self;
  KmsElementPadType type;

  capsfilter = g_object_get_qdata (G_OBJECT (pad), key_elem_data_quark ());
  g_return_if_fail (capsfilter);
  self = KMS_ELEMENT (gst_object_get_parent (GST_OBJECT (capsfilter)));
  g_return_if_fail (self);

  target = gst_element_get_static_pad (capsfilter, "sink");
  if (!target) {
    GST_WARNING_OBJECT (pad, "No sink in capsfilter");
    goto end;
  }

  type =
      GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pad),
          key_type_data_quark ()));
  new_pad = kms_element_connect_sink_target (self, target, type);
  g_object_unref (target);
  g_object_set_qdata_full (G_OBJECT (pad), key_pad_data_quark (),
      g_object_ref (new_pad), g_object_unref);

end:
  g_object_unref (self);
}

static void
kms_hub_port_start_media_type (KmsElement * self, KmsElementPadType type,
    GstPadTemplate * templ, const gchar * pad_name)
{
  GstElement *capsfilter = gst_element_factory_make ("capsfilter", NULL);
  GstPad *src = gst_element_get_static_pad (capsfilter, "src");
  GstPad *internal_src;

  gst_bin_add (GST_BIN (self), capsfilter);
  gst_element_sync_state_with_parent (capsfilter);

  internal_src = gst_ghost_pad_new_from_template (pad_name, src, templ);

  g_object_set_qdata_full (G_OBJECT (internal_src), key_elem_data_quark (),
      g_object_ref (capsfilter), g_object_unref);
  g_object_set_qdata (G_OBJECT (internal_src), key_type_data_quark (),
      GINT_TO_POINTER (type));

  g_signal_connect (internal_src, "linked",
      G_CALLBACK (kms_hub_port_internal_src_pad_linked), NULL);

  if (GST_STATE (self) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (self) >= GST_STATE_PAUSED) {
    gst_pad_set_active (internal_src, TRUE);
  }

  gst_element_add_pad (GST_ELEMENT (self), internal_src);
  g_object_unref (src);
}

static void
kms_hub_port_dispose (GObject * object)
{
  G_OBJECT_CLASS (kms_hub_port_parent_class)->dispose (object);
}

static void
kms_hub_port_finalize (GObject * object)
{
  G_OBJECT_CLASS (kms_hub_port_parent_class)->finalize (object);
}

static void
kms_hub_port_class_init (KmsHubPortClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "HubPort", "Generic", "Kurento plugin for mixer connection",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  gobject_class->dispose = kms_hub_port_dispose;
  gobject_class->finalize = kms_hub_port_finalize;

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_hub_port_request_new_pad);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&hub_audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&hub_video_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&hub_data_sink_factory));

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&hub_audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&hub_video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&hub_data_src_factory));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHubPortPrivate));
}

static void
kms_hub_port_init (KmsHubPort * self)
{
  KmsElement *kmselement;
  GstPadTemplate *templ;

  self->priv = KMS_HUB_PORT_GET_PRIVATE (self);

  kmselement = KMS_ELEMENT (self);

  templ = gst_static_pad_template_get (&hub_video_src_factory);
  kms_hub_port_start_media_type (kmselement, KMS_ELEMENT_PAD_TYPE_VIDEO, templ,
      HUB_VIDEO_SRC_PAD);
  g_object_unref (templ);

  templ = gst_static_pad_template_get (&hub_audio_src_factory);
  kms_hub_port_start_media_type (kmselement, KMS_ELEMENT_PAD_TYPE_AUDIO, templ,
      HUB_AUDIO_SRC_PAD);
  g_object_unref (templ);

  templ = gst_static_pad_template_get (&hub_data_src_factory);
  kms_hub_port_start_media_type (kmselement, KMS_ELEMENT_PAD_TYPE_DATA, templ,
      HUB_DATA_SRC_PAD);
  g_object_unref (templ);
}

gboolean
kms_hub_port_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HUB_PORT);
}
