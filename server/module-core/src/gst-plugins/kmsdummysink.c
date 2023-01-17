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
#include "kmsdummysink.h"

#define PLUGIN_NAME "dummysink"

#define APPDATASINK "datasink"
#define APPAUDIOSINK "audiosink"
#define APPVIDEOSINK "videosink"

GST_DEBUG_CATEGORY_STATIC (kms_dummy_sink_debug_category);
#define GST_CAT_DEFAULT kms_dummy_sink_debug_category

#define KMS_DUMMY_SINK_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (           \
    (obj),                                \
    KMS_TYPE_DUMMY_SINK,                  \
    KmsDummySinkPrivate                   \
  )                                       \
)

typedef struct _KmsDummySinkElement
{
  KmsElementPadType type;
  gchar *description;
  GstElement *sink;
} KmsDummySinkElement;

struct _KmsDummySinkPrivate
{
  gboolean video;
  gboolean audio;
  gboolean data;
  GstElement *videoappsink;
  GstElement *audioappsink;
  GstElement *dataappsink;

  GHashTable *sinks;            /* <name, KmsDummySinkElement> */
};

G_DEFINE_TYPE_WITH_CODE (KmsDummySink, kms_dummy_sink,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_dummy_sink_debug_category, PLUGIN_NAME,
        0, "debug category for kurento dummy plugin"));

/* Object properties */
enum
{
  PROP_0,
  PROP_DATA,
  PROP_AUDIO,
  PROP_VIDEO,
  N_PROPERTIES
};

#define DEFAULT_HTTP_ENDPOINT_START FALSE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

static KmsDummySinkElement *
create_dummy_sink_element (KmsElementPadType type, const gchar * description,
    GstElement * sink)
{
  KmsDummySinkElement *dummy_sink;

  dummy_sink = g_slice_new0 (KmsDummySinkElement);
  dummy_sink->type = type;
  dummy_sink->description = g_strdup (description);
  dummy_sink->sink = g_object_ref (sink);

  return dummy_sink;
}

static void
destroy_dummy_sink_element (KmsDummySinkElement * sink)
{
  g_free (sink->description);
  g_clear_object (&sink->sink);

  g_slice_free (KmsDummySinkElement, sink);
}

static void
kms_dummy_sink_add_sinkpad (KmsDummySink * self, KmsElementPadType type)
{
  GstElement **appsink;
  GstPad *sinkpad;
  gchar *name;

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_DATA:
      appsink = &self->priv->dataappsink;
      name = APPDATASINK;
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      appsink = &self->priv->audioappsink;
      name = APPAUDIOSINK;
      break;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      appsink = &self->priv->videoappsink;
      name = APPVIDEOSINK;
      break;
    default:
      GST_ERROR_OBJECT (self, "Invalid pad type provided");
      return;
  }

  if (*appsink == NULL) {
    /* First time that appsink is created */
    *appsink = gst_element_factory_make ("fakesink", name);
    g_object_set (*appsink, "async", FALSE, "sync", FALSE, NULL);
    gst_bin_add (GST_BIN (self), *appsink);
    gst_element_sync_state_with_parent (*appsink);
  }

  sinkpad = gst_element_get_static_pad (*appsink, "sink");

  kms_element_connect_sink_target (KMS_ELEMENT (self), sinkpad, type);

  g_object_unref (sinkpad);
}

static void
kms_dummy_sink_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsDummySink *self = KMS_DUMMY_SINK (object);
  gboolean val;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DATA:
      val = g_value_get_boolean (value);
      if (val && !self->priv->data) {
        kms_dummy_sink_add_sinkpad (self, KMS_ELEMENT_PAD_TYPE_DATA);
      } else if (!val && self->priv->data) {
        kms_element_remove_sink_by_type (KMS_ELEMENT (self),
            KMS_ELEMENT_PAD_TYPE_DATA);
      } else {
        GST_DEBUG_OBJECT (self, "Operation without effect");
      }

      self->priv->data = val;
      break;
    case PROP_AUDIO:
      val = g_value_get_boolean (value);
      if (val && !self->priv->audio) {
        kms_dummy_sink_add_sinkpad (self, KMS_ELEMENT_PAD_TYPE_AUDIO);
      } else if (!val && self->priv->audio) {
        kms_element_remove_sink_by_type (KMS_ELEMENT (self),
            KMS_ELEMENT_PAD_TYPE_AUDIO);
      } else {
        GST_DEBUG_OBJECT (self, "Operation without effect");
      }

      self->priv->audio = val;
      break;
    case PROP_VIDEO:
      val = g_value_get_boolean (value);
      if (val && !self->priv->video) {
        kms_dummy_sink_add_sinkpad (self, KMS_ELEMENT_PAD_TYPE_VIDEO);
      } else if (!val && self->priv->video) {
        kms_element_remove_sink_by_type (KMS_ELEMENT (self),
            KMS_ELEMENT_PAD_TYPE_VIDEO);
      } else {
        GST_DEBUG_OBJECT (self, "Operation without effect");
      }

      self->priv->video = val;
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_dummy_sink_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsDummySink *self = KMS_DUMMY_SINK (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DATA:
      g_value_set_boolean (value, self->priv->data);
      break;
    case PROP_AUDIO:
      g_value_set_boolean (value, self->priv->audio);
      break;
    case PROP_VIDEO:
      g_value_set_boolean (value, self->priv->video);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_dummy_sink_finalize (GObject * object)
{
  KmsDummySink *self = KMS_DUMMY_SINK (object);

  GST_DEBUG_OBJECT (self, "finalize");
  g_hash_table_unref (self->priv->sinks);

  /* chain up */
  G_OBJECT_CLASS (kms_dummy_sink_parent_class)->finalize (object);
}

static gboolean
kms_dummy_sink_request_new_sink_pad (KmsElement * obj, KmsElementPadType type,
    const gchar * description, const gchar * name)
{
  KmsDummySink *self = KMS_DUMMY_SINK (obj);
  KmsDummySinkElement *dummy;
  GstElement *sink;
  GstPad *sinkpad;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  if (g_hash_table_contains (self->priv->sinks, name)) {
    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

    return TRUE;
  }

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (sink, "async", FALSE, "sync", FALSE, NULL);

  dummy = create_dummy_sink_element (type, description, sink);

  if (!gst_bin_add (GST_BIN (self), sink)) {
    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
    destroy_dummy_sink_element (dummy);

    return FALSE;
  }

  g_hash_table_insert (self->priv->sinks, g_strdup (name), dummy);
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  gst_element_sync_state_with_parent (sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  kms_element_connect_sink_target_full (KMS_ELEMENT (self), sinkpad, type,
      description, NULL, NULL);
  g_object_unref (sinkpad);

  return TRUE;
}

static gboolean
kms_dummy_sink_release_requested_sink_pad (KmsElement * obj, GstPad * pad)
{
  KmsDummySink *self = KMS_DUMMY_SINK (obj);
  KmsDummySinkElement *dummy;
  gchar *padname;

  padname = gst_pad_get_name (pad);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));

  dummy = g_hash_table_lookup (self->priv->sinks, padname);

  if (dummy == NULL) {
    KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

    return FALSE;
  }

  kms_element_remove_sink_by_type_full (obj, dummy->type, dummy->description);
  g_hash_table_remove (self->priv->sinks, padname);
  g_free (padname);

  return TRUE;
}

static void
kms_dummy_sink_class_init (KmsDummySinkClass * klass)
{
  KmsElementClass *kmselement_class;
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_dummy_sink_set_property;
  gobject_class->get_property = kms_dummy_sink_get_property;
  gobject_class->finalize = kms_dummy_sink_finalize;

  kmselement_class = KMS_ELEMENT_CLASS (klass);
  kmselement_class->request_new_sink_pad =
      GST_DEBUG_FUNCPTR (kms_dummy_sink_request_new_sink_pad);
  kmselement_class->release_requested_sink_pad =
      GST_DEBUG_FUNCPTR (kms_dummy_sink_release_requested_sink_pad);

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "KmsDummySink",
      "Generic",
      "Dummy sink element",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  obj_properties[PROP_DATA] = g_param_spec_boolean ("data",
      "Data", "Provides data on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  obj_properties[PROP_AUDIO] = g_param_spec_boolean ("audio",
      "Audio", "Provides audio on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  obj_properties[PROP_VIDEO] = g_param_spec_boolean ("video",
      "Video", "Provides video on TRUE", FALSE,
      (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  g_type_class_add_private (klass, sizeof (KmsDummySinkPrivate));
}

static void
kms_dummy_sink_init (KmsDummySink * self)
{
  self->priv = KMS_DUMMY_SINK_GET_PRIVATE (self);

  self->priv->sinks = g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      (GDestroyNotify) destroy_dummy_sink_element);
}

gboolean
kms_dummy_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_DUMMY_SINK);
}
