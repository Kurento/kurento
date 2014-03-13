/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>

#include "kmsaudiomixer.h"

#define PLUGIN_NAME "audiomixer"
#define KEY_SINK_PAD_NAME "kms-key-sink-pad-name"
#define KMS_AUDIO_MIXER_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_AUDIO_MIXER_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_audio_mixer_debug_category);
#define GST_CAT_DEFAULT kms_audio_mixer_debug_category

#define KMS_AUDIO_MIXER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (            \
    (obj),                                 \
    KMS_TYPE_AUDIO_MIXER,                  \
    KmsAudioMixerPrivate                   \
  )                                        \
)

struct _KmsAudioMixerPrivate
{
  GRecMutex mutex;
  GHashTable *adders;
  GHashTable *agnostics;
  guint count;
};

#define AUDIO_SINK_PAD_PREFIX  "sink_"
#define AUDIO_SRC_PAD_PREFIX  "src_"

#define AUDIO_SINK_PAD AUDIO_SINK_PAD_PREFIX "%u"
#define AUDIO_SRC_PAD AUDIO_SRC_PAD_PREFIX "%u"

#define LENGTH_AUDIO_SINK_PAD_PREFIX 5  /* sizeof("sink_") */

#define RAW_AUDIO_CAPS "audio/x-raw;"

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsAudioMixer, kms_audio_mixer,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_audio_mixer_debug_category,
        PLUGIN_NAME, 0, "debug category for " PLUGIN_NAME " element"));

static void
link_new_agnosticbin (gchar * padname, GstElement * adder,
    GstElement * agnosticbin)
{
  GST_DEBUG ("Linking %s to %s", GST_ELEMENT_NAME (agnosticbin),
      GST_ELEMENT_NAME (adder));

  if (!gst_element_link_pads (agnosticbin, "src_%u", adder, "sink_%u"))
    GST_ERROR ("Could not link %s to %s", GST_ELEMENT_NAME (agnosticbin),
        GST_ELEMENT_NAME (adder));
}

static void
link_new_adder (gchar * padname, GstElement * agnosticbin, GstElement * adder)
{
  GST_DEBUG ("Linking %s to %s", GST_ELEMENT_NAME (agnosticbin),
      GST_ELEMENT_NAME (adder));

  if (!gst_element_link_pads (agnosticbin, "src_%u", adder, "sink_%u"))
    GST_ERROR ("Could not link %s to %s", GST_ELEMENT_NAME (agnosticbin),
        GST_ELEMENT_NAME (adder));
}

static gint
get_stream_id_from_padname (const gchar * name)
{
  gint64 id;

  if (name == NULL)
    return -1;

  if (!g_str_has_prefix (name, AUDIO_SINK_PAD_PREFIX))
    return -1;

  id = g_ascii_strtoll (name + LENGTH_AUDIO_SINK_PAD_PREFIX, NULL, 10);
  if (id > G_MAXINT)
    return -1;

  return id;
}

static void
kms_audio_mixer_dispose (GObject * object)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (object);

  KMS_AUDIO_MIXER_LOCK (self);

  if (self->priv->agnostics != NULL) {
    g_hash_table_remove_all (self->priv->agnostics);
    g_hash_table_unref (self->priv->agnostics);
    self->priv->agnostics = NULL;
  }

  if (self->priv->adders != NULL) {
    g_hash_table_remove_all (self->priv->adders);
    g_hash_table_unref (self->priv->adders);
    self->priv->adders = NULL;
  }

  KMS_AUDIO_MIXER_UNLOCK (self);

  G_OBJECT_CLASS (kms_audio_mixer_parent_class)->dispose (object);
}

static void
kms_audio_mixer_finalize (GObject * object)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (object);

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_audio_mixer_parent_class)->finalize (object);
}

static void
kms_audio_mixer_have_type (GstElement * typefind, guint arg0, GstCaps * caps,
    gpointer data)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (data);
  GstElement *audiorate, *agnosticbin, *adder;
  GstPad *srcpad, *pad;
  gchar *padname;
  gint id;

  padname = g_object_get_data (G_OBJECT (typefind), KEY_SINK_PAD_NAME);
  if ((id = get_stream_id_from_padname (padname)) < 0) {
    GST_ERROR_OBJECT (self, "Can not get pad id from element %" GST_PTR_FORMAT,
        typefind);
    return;
  }

  audiorate = gst_element_factory_make ("audiorate", NULL);
  agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  adder = gst_element_factory_make ("adder", NULL);

  gst_bin_add_many (GST_BIN (self), audiorate, agnosticbin, adder, NULL);

  gst_element_sync_state_with_parent (audiorate);
  gst_element_sync_state_with_parent (agnosticbin);
  gst_element_sync_state_with_parent (adder);

  gst_element_link_many (typefind, audiorate, agnosticbin, NULL);

  KMS_AUDIO_MIXER_LOCK (self);

  g_hash_table_foreach (self->priv->adders, (GHFunc) link_new_agnosticbin,
      agnosticbin);
  g_hash_table_foreach (self->priv->agnostics, (GHFunc) link_new_adder, adder);

  g_hash_table_insert (self->priv->agnostics, padname, agnosticbin);
  g_hash_table_insert (self->priv->adders, padname, adder);

  padname = g_strdup_printf ("src_%u", id);
  srcpad = gst_element_get_static_pad (adder, "src");
  pad = gst_ghost_pad_new (padname, srcpad);
  g_free (padname);
  gst_object_unref (srcpad);

  if (GST_STATE (self) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (self) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (gst_element_add_pad (GST_ELEMENT (self), pad)) {
    KMS_AUDIO_MIXER_UNLOCK (self);
    return;
  }

  GST_ERROR_OBJECT (self, "Can not add pad %" GST_PTR_FORMAT, pad);

  padname = g_object_get_data (G_OBJECT (typefind), KEY_SINK_PAD_NAME);
  g_hash_table_remove (self->priv->agnostics, padname);
  g_hash_table_remove (self->priv->adders, padname);
  /*TODO: Unlink agnostic and adder connections */
  KMS_AUDIO_MIXER_UNLOCK (self);

  gst_object_unref (pad);

  gst_element_set_locked_state (audiorate, TRUE);
  gst_element_set_locked_state (agnosticbin, TRUE);
  gst_element_set_locked_state (adder, TRUE);

  gst_element_set_state (audiorate, GST_STATE_NULL);
  gst_element_set_state (agnosticbin, GST_STATE_NULL);
  gst_element_set_state (adder, GST_STATE_NULL);

  gst_element_unlink_many (audiorate, agnosticbin, adder, NULL);

  gst_bin_remove_many (GST_BIN (self), audiorate, agnosticbin, adder, NULL);
}

static GstPad *
kms_audio_mixer_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (element);
  GstPad *sinkpad, *pad = NULL;
  GstElement *typefind;
  gchar *padname;

  if (templ !=
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), AUDIO_SINK_PAD))
    return NULL;

  typefind = gst_element_factory_make ("typefind", NULL);
  sinkpad = gst_element_get_static_pad (typefind, "sink");
  if (sinkpad == NULL) {
    gst_object_unref (typefind);
    return NULL;
  }

  gst_bin_add (GST_BIN (self), typefind);
  gst_element_sync_state_with_parent (typefind);

  KMS_AUDIO_MIXER_LOCK (self);

  padname = g_strdup_printf (AUDIO_SINK_PAD, self->priv->count++);

  pad = gst_ghost_pad_new (padname, sinkpad);
  g_object_unref (sinkpad);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (!gst_element_add_pad (element, pad)) {
    GST_ERROR_OBJECT (self, "Could not create pad");
    g_object_unref (pad);
    gst_element_set_locked_state (typefind, TRUE);
    gst_element_set_state (typefind, GST_STATE_NULL);
    gst_bin_remove (GST_BIN (self), typefind);
    self->priv->count--;
    pad = NULL;
    g_free (padname);
  } else {
    g_object_set_data_full (G_OBJECT (typefind), KEY_SINK_PAD_NAME, padname,
        g_free);
    g_signal_connect (G_OBJECT (typefind), "have-type",
        G_CALLBACK (kms_audio_mixer_have_type), self);
  }

  KMS_AUDIO_MIXER_UNLOCK (self);

  return pad;
}

static void
kms_audio_mixer_release_pad (GstElement * element, GstPad * pad)
{
  GST_DEBUG ("Remove pad %" GST_PTR_FORMAT, pad);
}

static void
kms_audio_mixer_class_init (KmsAudioMixerClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "AudioMixer", "Generic",
      "Audio mixer element",
      "Santiago Carot Nemesio <sancane at gmail dot com>");

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_audio_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_audio_mixer_finalize);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAudioMixerPrivate));
}

static void
kms_audio_mixer_init (KmsAudioMixer * self)
{
  self->priv = KMS_AUDIO_MIXER_GET_PRIVATE (self);

  self->priv->adders = g_hash_table_new_full (g_str_hash, g_str_equal, NULL,
      NULL);
  self->priv->agnostics = g_hash_table_new_full (g_str_hash, g_str_equal, NULL,
      NULL);

  g_rec_mutex_init (&self->priv->mutex);

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}

gboolean
kms_audio_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUDIO_MIXER);
}
