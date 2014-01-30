/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include "kmsmainmixer.h"
#include "kmsagnosticcaps.h"
#include "kms-marshal.h"
#include "kmsmixerendpoint.h"

#define PLUGIN_NAME "mainmixer"

#define KMS_MAIN_MIXER_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_MAIN_MIXER_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_main_mixer_debug_category);
#define GST_CAT_DEFAULT kms_main_mixer_debug_category

#define KMS_MAIN_MIXER_GET_PRIVATE(obj) (       \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_MAIN_MIXER,                        \
    KmsMainMixerPrivate                         \
  )                                             \
)

struct _KmsMainMixerPrivate
{
  GRecMutex mutex;

  gint main_port;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsMainMixer, kms_main_mixer,
    KMS_TYPE_BASE_MIXER,
    GST_DEBUG_CATEGORY_INIT (kms_main_mixer_debug_category, PLUGIN_NAME,
        0, "debug category for mainmixer element"));

static void
kms_main_mixer_unhandle_port (KmsBaseMixer * mixer, gint id)
{
  KMS_BASE_MIXER_CLASS (G_OBJECT_CLASS
      (kms_main_mixer_parent_class))->unhandle_port (mixer, id);
}

static gint
kms_main_mixer_handle_port (KmsBaseMixer * mixer, GstElement * mixer_end_point)
{
  return
      KMS_BASE_MIXER_CLASS (G_OBJECT_CLASS
      (kms_main_mixer_parent_class))->handle_port (mixer, mixer_end_point);
}

static void
kms_main_mixer_dispose (GObject * object)
{
  G_OBJECT_CLASS (kms_main_mixer_parent_class)->dispose (object);
}

static void
kms_main_mixer_finalize (GObject * object)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (object);

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_main_mixer_parent_class)->finalize (object);
}

static void
kms_main_mixer_class_init (KmsMainMixerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseMixerClass *base_mixer_class = KMS_BASE_MIXER_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "MainMixer", "Generic", "Mixer element that makes dispatching of "
      "an input flow", "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_main_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_main_mixer_finalize);

  base_mixer_class->handle_port =
      GST_DEBUG_FUNCPTR (kms_main_mixer_handle_port);
  base_mixer_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_main_mixer_unhandle_port);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsMainMixerPrivate));
}

static void
kms_main_mixer_init (KmsMainMixer * self)
{
  self->priv = KMS_MAIN_MIXER_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->main_port = -1;
}

gboolean
kms_main_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MAIN_MIXER);
}
