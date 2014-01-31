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

#define MAIN_PORT_NONE (-1)

struct _KmsMainMixerPrivate
{
  GRecMutex mutex;
  GHashTable *ports;

  gint main_port;
};

typedef struct _KmsMainMixerPortData KmsMainMixerPortData;

struct _KmsMainMixerPortData
{
  KmsMainMixer *mixer;
  gint id;
  GstElement *audio_agnostic;
  GstElement *video_agnostic;
};

enum
{
  PROP_0,
  PROP_MAIN_PORT
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsMainMixer, kms_main_mixer,
    KMS_TYPE_BASE_MIXER,
    GST_DEBUG_CATEGORY_INIT (kms_main_mixer_debug_category, PLUGIN_NAME,
        0, "debug category for mainmixer element"));

static KmsMainMixerPortData *
kms_main_mixer_port_data_create (KmsMainMixer * mixer, gint id)
{
  KmsMainMixerPortData *data = g_slice_new0 (KmsMainMixerPortData);

  data->mixer = mixer;
  data->audio_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->video_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->id = id;

  gst_bin_add_many (GST_BIN (mixer), g_object_ref (data->audio_agnostic),
      g_object_ref (data->video_agnostic), NULL);
  gst_element_sync_state_with_parent (data->audio_agnostic);
  gst_element_sync_state_with_parent (data->video_agnostic);

  kms_base_mixer_link_video_sink (KMS_BASE_MIXER (mixer), id,
      data->video_agnostic, "sink");
  kms_base_mixer_link_audio_sink (KMS_BASE_MIXER (mixer), id,
      data->audio_agnostic, "src");

  return data;
}

static void
kms_main_mixer_port_data_destroy (gpointer data)
{
  KmsMainMixerPortData *port_data = (KmsMainMixerPortData *) data;
  KmsMainMixer *self = port_data->mixer;

  KMS_MAIN_MIXER_LOCK (self);
  gst_bin_remove_many (GST_BIN (self), port_data->audio_agnostic,
      port_data->video_agnostic, NULL);
  KMS_MAIN_MIXER_UNLOCK (self);

  g_clear_object (&port_data->audio_agnostic);
  g_clear_object (&port_data->video_agnostic);

  g_slice_free (KmsMainMixerPortData, data);
}

static void
release_gint (gpointer data)
{
  g_slice_free (gint, data);
}

static gint *
create_gint (gint value)
{
  gint *p = g_slice_new (gint);

  *p = value;
  return p;
}

static void
kms_main_mixer_unhandle_port (KmsBaseMixer * mixer, gint id)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (mixer);

  KMS_MAIN_MIXER_LOCK (self);

  if (self->priv->main_port == id) {
    self->priv->main_port = MAIN_PORT_NONE;
  }

  g_hash_table_remove (self->priv->ports, &id);
  KMS_MAIN_MIXER_UNLOCK (self);

  KMS_BASE_MIXER_CLASS (G_OBJECT_CLASS
      (kms_main_mixer_parent_class))->unhandle_port (mixer, id);
}

static void
kms_main_mixer_link_port (KmsMainMixer * self, gint to)
{
  KmsMainMixerPortData *port_data;

  KMS_MAIN_MIXER_LOCK (self);
  if (self->priv->main_port >= 0)
    goto end;

  port_data = g_hash_table_lookup (self->priv->ports, &self->priv->main_port);
  kms_base_mixer_link_audio_src (KMS_BASE_MIXER (self), to,
      port_data->audio_agnostic, "src_%u");
  kms_base_mixer_link_video_src (KMS_BASE_MIXER (self), to,
      port_data->video_agnostic, "src_%u");

end:

  KMS_MAIN_MIXER_UNLOCK (self);
}

static gint
kms_main_mixer_handle_port (KmsBaseMixer * mixer, GstElement * mixer_end_point)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (mixer);
  KmsMainMixerPortData *port_data;
  gint port_id;

  port_id = KMS_BASE_MIXER_CLASS (G_OBJECT_CLASS
      (kms_main_mixer_parent_class))->handle_port (mixer, mixer_end_point);

  if (port_id < 0)
    return port_id;

  port_data = kms_main_mixer_port_data_create (self, port_id);

  KMS_MAIN_MIXER_LOCK (self);
  g_hash_table_insert (self->priv->ports, create_gint (port_id), port_data);

  kms_main_mixer_link_port (self, port_id);

  KMS_MAIN_MIXER_UNLOCK (self);

  return port_id;
}

static void
kms_main_mixer_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (object);

  KMS_MAIN_MIXER_LOCK (self);
  switch (property_id) {
    case PROP_MAIN_PORT:
      self->priv->main_port = g_value_get_int (value);
      // TODO: Change internal connections
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_MAIN_MIXER_UNLOCK (self);
}

static void
kms_main_mixer_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (object);

  KMS_MAIN_MIXER_LOCK (self);
  switch (property_id) {
    case PROP_MAIN_PORT:
      g_value_set_int (value, self->priv->main_port);
      // TODO: Change internal connections
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_MAIN_MIXER_UNLOCK (self);
}

static void
kms_main_mixer_dispose (GObject * object)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (object);

  KMS_MAIN_MIXER_LOCK (self);
  g_hash_table_remove_all (self->priv->ports);
  KMS_MAIN_MIXER_UNLOCK (self);

  G_OBJECT_CLASS (kms_main_mixer_parent_class)->dispose (object);
}

static void
kms_main_mixer_finalize (GObject * object)
{
  KmsMainMixer *self = KMS_MAIN_MIXER (object);

  g_rec_mutex_clear (&self->priv->mutex);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

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
  gobject_class->get_property = GST_DEBUG_FUNCPTR (kms_main_mixer_get_property);
  gobject_class->set_property = GST_DEBUG_FUNCPTR (kms_main_mixer_set_property);

  base_mixer_class->handle_port =
      GST_DEBUG_FUNCPTR (kms_main_mixer_handle_port);
  base_mixer_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_main_mixer_unhandle_port);

  g_object_class_install_property (gobject_class, PROP_MAIN_PORT,
      g_param_spec_int ("main",
          "Selected main port",
          "The selected main port, -1 indicates none.", -1, G_MAXINT,
          MAIN_PORT_NONE, G_PARAM_READWRITE));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsMainMixerPrivate));
}

static void
kms_main_mixer_init (KmsMainMixer * self)
{
  self->priv = KMS_MAIN_MIXER_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      release_gint, kms_main_mixer_port_data_destroy);

  self->priv->main_port = -1;
}

gboolean
kms_main_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MAIN_MIXER);
}
