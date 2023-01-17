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
#include <string.h>

#include "kmsdummysrc.h"
#include "kmsagnosticcaps.h"

#define PLUGIN_NAME "dummysrc"

GST_DEBUG_CATEGORY_STATIC (kms_dummy_src_debug_category);
#define GST_CAT_DEFAULT kms_dummy_src_debug_category

#define KMS_DUMMY_SRC_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (           \
    (obj),                                \
    KMS_TYPE_DUMMY_SRC,                   \
    KmsDummySrcPrivate                    \
  )                                       \
)

#define DEFAULT_AUDIO_FREQ 440

struct _KmsDummySrcPrivate
{
  gboolean video;
  gboolean audio;
  gboolean data;
  gdouble audio_freq;
  GstElement *videoappsrc;
  GstElement *audioappsrc;
  GstElement *dataappsrc;
  guint data_index;
};

G_DEFINE_TYPE_WITH_CODE (KmsDummySrc, kms_dummy_src,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_dummy_src_debug_category, PLUGIN_NAME,
        0, "debug category for kurento dummy plugin"));

/* Object properties */
enum
{
  PROP_0,
  PROP_DATA,
  PROP_AUDIO,
  PROP_VIDEO,
  PROP_AUDIO_FREQ,
  N_PROPERTIES
};

#define DEFAULT_HTTP_ENDPOINT_START FALSE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

static void
kms_dummy_src_feed_data_channel (GstElement * appsrc, guint unused_size,
    gpointer data)
{
  KmsDummySrc *self = KMS_DUMMY_SRC (data);
  GstClockTime running_time, base_time, now;
  GstClock *clock;
  GstBuffer *buffer;
  gchar *buffer_data;
  GstFlowReturn ret;

  if ((clock = GST_ELEMENT_CLOCK (appsrc)) == NULL) {
    GST_ERROR_OBJECT (GST_ELEMENT (data), "no clock, we can't sync");
    return;
  }

  buffer_data = g_strdup_printf ("Test buffer %d",
      g_atomic_int_add (&self->priv->data_index, 1));

  buffer = gst_buffer_new_wrapped (buffer_data, strlen (buffer_data));

  base_time = GST_ELEMENT_CAST (appsrc)->base_time;

  now = gst_clock_get_time (clock);
  running_time = now - base_time;

  /* Live sources always timestamp their buffers with the running_time of the */
  /* pipeline. This is needed to be able to match the timestamps of different */
  /* live sources in order to synchronize them. */
  GST_BUFFER_PTS (buffer) = running_time;

  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_WARNING ("Could not send buffer");
  }

  gst_buffer_unref (buffer);
}

static void
kms_dummy_src_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsDummySrc *self = KMS_DUMMY_SRC (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_DATA:
      self->priv->data = g_value_get_boolean (value);

      if (self->priv->data && self->priv->dataappsrc == NULL) {
        GstElement *tee;
        GstCaps *caps;

        GST_DEBUG_OBJECT (self, "Creating data stream");
        tee = kms_element_get_data_tee (KMS_ELEMENT (self));
        caps = gst_caps_from_string (KMS_AGNOSTIC_DATA_CAPS);
        self->priv->dataappsrc = gst_element_factory_make ("appsrc", NULL);
        g_object_set (G_OBJECT (self->priv->dataappsrc), "is-live", TRUE,
            "caps", caps, "emit-signals", TRUE, "stream-type", 0,
            "format", GST_FORMAT_TIME, NULL);
        gst_caps_unref (caps);
        g_signal_connect (self->priv->dataappsrc, "need-data",
            G_CALLBACK (kms_dummy_src_feed_data_channel), self);
        gst_bin_add (GST_BIN (self), self->priv->dataappsrc);
        gst_element_link_pads (self->priv->dataappsrc, "src", tee, "sink");
        gst_element_sync_state_with_parent (self->priv->dataappsrc);
      }
      break;
    case PROP_AUDIO:
      self->priv->audio = g_value_get_boolean (value);

      if (self->priv->audio && self->priv->audioappsrc == NULL) {
        GstElement *agnosticbin;

        GST_DEBUG_OBJECT (self, "Creating audio stream");
        agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));

        self->priv->audioappsrc =
            gst_element_factory_make ("audiotestsrc", NULL);
        g_object_set (G_OBJECT (self->priv->audioappsrc), "is-live", TRUE,
            "freq", self->priv->audio_freq, NULL);
        gst_bin_add (GST_BIN (self), self->priv->audioappsrc);
        gst_element_link_pads (self->priv->audioappsrc, "src", agnosticbin,
            "sink");
        gst_element_sync_state_with_parent (self->priv->audioappsrc);
      }
      break;
    case PROP_VIDEO:
      self->priv->video = g_value_get_boolean (value);

      if (self->priv->video && self->priv->videoappsrc == NULL) {
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
    case PROP_AUDIO_FREQ:
      self->priv->audio_freq = g_value_get_double (value);

      if (self->priv->audioappsrc) {
        g_object_set (self->priv->audioappsrc, "freq", self->priv->audio_freq,
            NULL);
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_dummy_src_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsDummySrc *self = KMS_DUMMY_SRC (object);

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
    case PROP_AUDIO_FREQ:
      g_value_set_double (value, self->priv->audio_freq);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_dummy_src_class_init (KmsDummySrcClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_dummy_src_set_property;
  gobject_class->get_property = kms_dummy_src_get_property;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "KmsDummySrc",
      "Generic",
      "Dummy src element",
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

  obj_properties[PROP_AUDIO_FREQ] = g_param_spec_double ("audio-freq",
      "Audio frequesncy", "Sets audio frequency when audio is enabled", 0,
      20000, DEFAULT_AUDIO_FREQ, (G_PARAM_CONSTRUCT | G_PARAM_READWRITE));

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  g_type_class_add_private (klass, sizeof (KmsDummySrcPrivate));
}

static void
kms_dummy_src_init (KmsDummySrc * self)
{
  self->priv = KMS_DUMMY_SRC_GET_PRIVATE (self);

  self->priv->audio_freq = DEFAULT_AUDIO_FREQ;
}

gboolean
kms_dummy_src_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_DUMMY_SRC);
}
