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

#include "kmsaudiomixerbin.h"
#include "kmsloop.h"

#define PLUGIN_NAME "audiomixerbin"

#define KMS_AUDIO_MIXER_BIN_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_AUDIO_MIXER_BIN_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_audio_mixer_bin_debug_category);
#define GST_CAT_DEFAULT kms_audio_mixer_bin_debug_category

#define KMS_AUDIO_MIXER_BIN_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                \
    (obj),                                     \
    KMS_TYPE_AUDIO_MIXER_BIN,                  \
    KmsAudioMixerBinPrivate                    \
  )                                            \
)

struct _KmsAudioMixerBinPrivate
{
  GRecMutex mutex;
  KmsLoop *loop;
};

#define RAW_AUDIO_CAPS "audio/x-raw;"

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_MIXER_BIN_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_MIXER_BIN_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsAudioMixerBin, kms_audio_mixer_bin,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_audio_mixer_bin_debug_category,
        PLUGIN_NAME, 0, "debug category for " PLUGIN_NAME " element"));

static GstPad *
kms_audio_mixer_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  /* TODO: */
  return NULL;
}

static void
kms_audio_mixer_bin_release_pad (GstElement * element, GstPad * pad)
{
  /* TODO: */
}

static void
kms_audio_mixer_bin_dispose (GObject * object)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_AUDIO_MIXER_BIN_LOCK (self);

  g_clear_object (&self->priv->loop);

  KMS_AUDIO_MIXER_BIN_UNLOCK (self);

  G_OBJECT_CLASS (kms_audio_mixer_bin_parent_class)->dispose (object);
}

static void
kms_audio_mixer_bin_finalize (GObject * object)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_audio_mixer_bin_parent_class)->finalize (object);
}

static void
kms_audio_mixer_bin_class_init (KmsAudioMixerBinClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "AudioMixerBin", "Generic",
      "Audio mixer element",
      "Santiago Carot Nemesio <sancane at gmail dot com>");

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_finalize);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAudioMixerBinPrivate));
}

static void
kms_audio_mixer_bin_init (KmsAudioMixerBin * self)
{
  self->priv = KMS_AUDIO_MIXER_BIN_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->loop = kms_loop_new ();

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}

gboolean
kms_audio_mixer_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUDIO_MIXER_BIN);
}
