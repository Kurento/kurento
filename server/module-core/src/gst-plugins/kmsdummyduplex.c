/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#include <gst/gst.h>
#include "kmsdummyduplex.h"

#define PLUGIN_NAME "dummyduplex"

#define APPAUDIOSINK "audiosink"
#define APPVIDEOSINK "videosink"

GST_DEBUG_CATEGORY_STATIC (kms_dummy_duplex_debug_category);
#define GST_CAT_DEFAULT kms_dummy_duplex_debug_category

#define KMS_DUMMY_DUPLEX_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (             \
    (obj),                                  \
    KMS_TYPE_DUMMY_DUPLEX,                  \
    KmsDummyDuplexPrivate                   \
  )                                         \
)

struct _KmsDummyDuplexPrivate
{
  gboolean src_video;
  gboolean src_audio;
  gboolean sink_video;
  gboolean sink_audio;
  GstElement *videoappsrc;
  GstElement *audioappsrc;
  GstElement *videoappsink;
  GstElement *audioappsink;
};

G_DEFINE_TYPE_WITH_CODE (KmsDummyDuplex, kms_dummy_duplex,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_dummy_duplex_debug_category, PLUGIN_NAME,
        0, "debug category for kurento dummy plugin"));

/* Object properties */
enum
{
  PROP_0,
  PROP_SRC_AUDIO,
  PROP_SRC_VIDEO,
  PROP_SINK_AUDIO,
  PROP_SINK_VIDEO,
  N_PROPERTIES
};

#define DEFAULT_HTTP_ENDPOINT_START FALSE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

static void
kms_dummy_duplex_add_sinkpad (KmsDummyDuplex * self, KmsElementPadType type)
{
  GstElement **appsink;
  GstPad *sinkpad;
  gchar *name;

  if (type == KMS_ELEMENT_PAD_TYPE_AUDIO) {
    appsink = &self->priv->audioappsink;
    name = APPAUDIOSINK;
  } else if (type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
    appsink = &self->priv->videoappsink;
    name = APPVIDEOSINK;
  } else {
    GST_ERROR_OBJECT (self, "Invalid pad type provided");
    return;
  }

  if (*appsink == NULL) {
    /* First time that appsink is created */
    *appsink = gst_element_factory_make ("appsink", name);
    g_object_set (*appsink, "async", FALSE, "sync", FALSE, NULL);
    gst_bin_add (GST_BIN (self), *appsink);
    gst_element_sync_state_with_parent (*appsink);
  }

  sinkpad = gst_element_get_static_pad (*appsink, "sink");

  kms_element_connect_sink_target (KMS_ELEMENT (self), sinkpad, type);

  g_object_unref (sinkpad);
}

static void
kms_dummy_duplex_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsDummyDuplex *self = KMS_DUMMY_DUPLEX (object);
  gboolean val;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_SRC_AUDIO:
      self->priv->src_audio = g_value_get_boolean (value);

      if (self->priv->src_audio && self->priv->audioappsrc == NULL) {
        GstElement *agnosticbin;

        GST_DEBUG_OBJECT (self, "Creating audio stream");
        agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));

        self->priv->audioappsrc =
            gst_element_factory_make ("audiotestsrc", NULL);
        g_object_set (G_OBJECT (self->priv->audioappsrc), "is-live", TRUE,
            NULL);
        gst_bin_add (GST_BIN (self), self->priv->audioappsrc);
        gst_element_link_pads (self->priv->audioappsrc, "src", agnosticbin,
            "sink");
        gst_element_sync_state_with_parent (self->priv->audioappsrc);
      }
      break;
    case PROP_SRC_VIDEO:
      self->priv->src_video = g_value_get_boolean (value);

      if (self->priv->src_video && self->priv->videoappsrc == NULL) {
        GstElement *agnosticbin;

        GST_DEBUG_OBJECT (self, "Creating video stream");
        agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));

        self->priv->videoappsrc =
            gst_element_factory_make ("videotestsrc", NULL);
        g_object_set (G_OBJECT (self->priv->videoappsrc), "is-live", TRUE,
            NULL);
        gst_bin_add (GST_BIN (self), self->priv->videoappsrc);
        gst_element_link_pads (self->priv->videoappsrc, "src", agnosticbin,
            "sink");
        gst_element_sync_state_with_parent (self->priv->videoappsrc);
      }
      break;
    case PROP_SINK_AUDIO:
      val = g_value_get_boolean (value);
      if (val && !self->priv->sink_audio) {
        kms_dummy_duplex_add_sinkpad (self, KMS_ELEMENT_PAD_TYPE_AUDIO);
      } else if (!val && self->priv->sink_audio) {
        kms_element_remove_sink_by_type (KMS_ELEMENT (self),
            KMS_ELEMENT_PAD_TYPE_AUDIO);
      } else {
        GST_DEBUG_OBJECT (self, "Operation without effect");
      }

      self->priv->sink_audio = val;
      break;
    case PROP_SINK_VIDEO:
      val = g_value_get_boolean (value);
      if (val && !self->priv->sink_video) {
        kms_dummy_duplex_add_sinkpad (self, KMS_ELEMENT_PAD_TYPE_VIDEO);
      } else if (!val && self->priv->sink_video) {
        kms_element_remove_sink_by_type (KMS_ELEMENT (self),
            KMS_ELEMENT_PAD_TYPE_VIDEO);
      } else {
        GST_DEBUG_OBJECT (self, "Operation without effect");
      }

      self->priv->sink_video = val;
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_dummy_duplex_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsDummyDuplex *self = KMS_DUMMY_DUPLEX (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_SRC_AUDIO:
      g_value_set_boolean (value, self->priv->src_audio);
      break;
    case PROP_SRC_VIDEO:
      g_value_set_boolean (value, self->priv->src_video);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_dummy_duplex_class_init (KmsDummyDuplexClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_dummy_duplex_set_property;
  gobject_class->get_property = kms_dummy_duplex_get_property;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "KmsDummyDuplex",
      "Generic",
      "Dummy duplex element",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>, "
      "Jose Antonio Santos <santoscadenas@gmail.com>");

  obj_properties[PROP_SRC_AUDIO] = g_param_spec_boolean ("src-audio",
      "SrcAudio", "Provides audio source on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  obj_properties[PROP_SRC_VIDEO] = g_param_spec_boolean ("src-video",
      "SrcVideo", "Provides video source on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  obj_properties[PROP_SINK_AUDIO] = g_param_spec_boolean ("sink-audio",
      "SinkAudio", "Provides audio sink on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  obj_properties[PROP_SINK_VIDEO] = g_param_spec_boolean ("sink-video",
      "SinkVideo", "Provides video sink on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  g_type_class_add_private (klass, sizeof (KmsDummyDuplexPrivate));
}

static void
kms_dummy_duplex_init (KmsDummyDuplex * self)
{
  self->priv = KMS_DUMMY_DUPLEX_GET_PRIVATE (self);
}

gboolean
kms_dummy_duplex_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_DUMMY_DUPLEX);
}
