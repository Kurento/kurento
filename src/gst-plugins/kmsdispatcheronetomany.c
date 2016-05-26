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

#include "kmsdispatcheronetomany.h"
#include <commons/kmsagnosticcaps.h>
#include <commons/kmshubport.h>

#define PLUGIN_NAME "dispatcheronetomany"

#define KMS_DISPATCHER_ONE_TO_MANY_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_DISPATCHER_ONE_TO_MANY_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_dispatcher_one_to_many_debug_category);
#define GST_CAT_DEFAULT kms_dispatcher_one_to_many_debug_category

#define KMS_DISPATCHER_ONE_TO_MANY_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                         \
    (obj),                                              \
    KMS_TYPE_DISPATCHER_ONE_TO_MANY,                    \
    KmsDispatcherOneToManyPrivate                       \
  )                                                     \
)

#define MAIN_PORT_NONE (-1)

struct _KmsDispatcherOneToManyPrivate
{
  GRecMutex mutex;
  GHashTable *ports;

  gint main_port;
};

typedef struct _KmsDispatcherOneToManyPortData KmsDispatcherOneToManyPortData;

struct _KmsDispatcherOneToManyPortData
{
  KmsDispatcherOneToMany *mixer;
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

G_DEFINE_TYPE_WITH_CODE (KmsDispatcherOneToMany, kms_dispatcher_one_to_many,
    KMS_TYPE_BASE_HUB,
    GST_DEBUG_CATEGORY_INIT (kms_dispatcher_one_to_many_debug_category,
        PLUGIN_NAME, 0, "debug category for dispatcheronetomany element"));

static KmsDispatcherOneToManyPortData *
kms_dispatcher_one_to_many_port_data_create (KmsDispatcherOneToMany * mixer,
    gint id)
{
  KmsDispatcherOneToManyPortData *data =
      g_slice_new0 (KmsDispatcherOneToManyPortData);

  data->mixer = mixer;
  data->audio_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->video_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->id = id;

  gst_bin_add_many (GST_BIN (mixer), g_object_ref (data->audio_agnostic),
      g_object_ref (data->video_agnostic), NULL);
  gst_element_sync_state_with_parent (data->audio_agnostic);
  gst_element_sync_state_with_parent (data->video_agnostic);

  kms_base_hub_link_video_sink (KMS_BASE_HUB (mixer), id,
      data->video_agnostic, "sink", FALSE);
  kms_base_hub_link_audio_sink (KMS_BASE_HUB (mixer), id,
      data->audio_agnostic, "sink", FALSE);

  return data;
}

static void
kms_dispatcher_one_to_many_port_data_destroy (gpointer data)
{
  KmsDispatcherOneToManyPortData *port_data =
      (KmsDispatcherOneToManyPortData *) data;
  KmsDispatcherOneToMany *self = port_data->mixer;

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);
  gst_bin_remove_many (GST_BIN (self), port_data->audio_agnostic,
      port_data->video_agnostic, NULL);
  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);

  gst_element_set_state (port_data->audio_agnostic, GST_STATE_NULL);
  gst_element_set_state (port_data->video_agnostic, GST_STATE_NULL);

  g_clear_object (&port_data->audio_agnostic);
  g_clear_object (&port_data->video_agnostic);

  g_slice_free (KmsDispatcherOneToManyPortData, data);
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
kms_dispatcher_one_to_many_link_port (KmsDispatcherOneToMany * self, gint to)
{
  KmsDispatcherOneToManyPortData *port_data;

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);
  if (self->priv->main_port < 0) {
    kms_base_hub_unlink_audio_src (KMS_BASE_HUB (self), to);
    kms_base_hub_unlink_video_src (KMS_BASE_HUB (self), to);
  } else {
    port_data = g_hash_table_lookup (self->priv->ports, &self->priv->main_port);
    kms_base_hub_link_audio_src (KMS_BASE_HUB (self), to,
        port_data->audio_agnostic, "src_%u", TRUE);
    kms_base_hub_link_video_src (KMS_BASE_HUB (self), to,
        port_data->video_agnostic, "src_%u", TRUE);
  }

  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);
}

static void
kms_dispatcher_one_to_many_change_main_port_it (gpointer key, gpointer value,
    gpointer data)
{
  KmsDispatcherOneToManyPortData *port_data = value;

  kms_dispatcher_one_to_many_link_port (port_data->mixer, port_data->id);
}

static void
kms_dispatcher_one_to_many_change_main_port (KmsDispatcherOneToMany * self)
{
  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);

  g_hash_table_foreach (self->priv->ports,
      kms_dispatcher_one_to_many_change_main_port_it, NULL);

  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);
}

static void
kms_dispatcher_one_to_many_unhandle_port (KmsBaseHub * mixer, gint id)
{
  KmsDispatcherOneToMany *self = KMS_DISPATCHER_ONE_TO_MANY (mixer);

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);

  g_hash_table_remove (self->priv->ports, &id);

  if (self->priv->main_port == id) {
    self->priv->main_port = MAIN_PORT_NONE;
    kms_dispatcher_one_to_many_change_main_port (self);
  }

  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);

  KMS_BASE_HUB_CLASS (G_OBJECT_CLASS
      (kms_dispatcher_one_to_many_parent_class))->unhandle_port (mixer, id);
}

static gint
kms_dispatcher_one_to_many_handle_port (KmsBaseHub * mixer,
    GstElement * mixer_port)
{
  KmsDispatcherOneToMany *self = KMS_DISPATCHER_ONE_TO_MANY (mixer);
  KmsDispatcherOneToManyPortData *port_data;
  gint port_id;

  port_id = KMS_BASE_HUB_CLASS (G_OBJECT_CLASS
      (kms_dispatcher_one_to_many_parent_class))->handle_port (mixer,
      mixer_port);

  if (port_id < 0)
    return port_id;

  port_data = kms_dispatcher_one_to_many_port_data_create (self, port_id);

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);
  g_hash_table_insert (self->priv->ports, create_gint (port_id), port_data);

  kms_dispatcher_one_to_many_link_port (self, port_id);

  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);

  return port_id;
}

static void
kms_dispatcher_one_to_many_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsDispatcherOneToMany *self = KMS_DISPATCHER_ONE_TO_MANY (object);

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);
  switch (property_id) {
    case PROP_MAIN_PORT:
      self->priv->main_port = g_value_get_int (value);
      kms_dispatcher_one_to_many_change_main_port (self);

      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);
}

static void
kms_dispatcher_one_to_many_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsDispatcherOneToMany *self = KMS_DISPATCHER_ONE_TO_MANY (object);

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);
  switch (property_id) {
    case PROP_MAIN_PORT:
      g_value_set_int (value, self->priv->main_port);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);
}

static void
kms_dispatcher_one_to_many_dispose (GObject * object)
{
  KmsDispatcherOneToMany *self = KMS_DISPATCHER_ONE_TO_MANY (object);

  KMS_DISPATCHER_ONE_TO_MANY_LOCK (self);
  g_hash_table_remove_all (self->priv->ports);
  KMS_DISPATCHER_ONE_TO_MANY_UNLOCK (self);

  G_OBJECT_CLASS (kms_dispatcher_one_to_many_parent_class)->dispose (object);
}

static void
kms_dispatcher_one_to_many_finalize (GObject * object)
{
  KmsDispatcherOneToMany *self = KMS_DISPATCHER_ONE_TO_MANY (object);

  g_rec_mutex_clear (&self->priv->mutex);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  G_OBJECT_CLASS (kms_dispatcher_one_to_many_parent_class)->finalize (object);
}

static void
kms_dispatcher_one_to_many_class_init (KmsDispatcherOneToManyClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseHubClass *base_hub_class = KMS_BASE_HUB_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "DispatcherOneToMany", "Generic",
      "Mixer element that makes dispatching of " "an input flow",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  gobject_class->dispose =
      GST_DEBUG_FUNCPTR (kms_dispatcher_one_to_many_dispose);
  gobject_class->finalize =
      GST_DEBUG_FUNCPTR (kms_dispatcher_one_to_many_finalize);
  gobject_class->get_property =
      GST_DEBUG_FUNCPTR (kms_dispatcher_one_to_many_get_property);
  gobject_class->set_property =
      GST_DEBUG_FUNCPTR (kms_dispatcher_one_to_many_set_property);

  base_hub_class->handle_port =
      GST_DEBUG_FUNCPTR (kms_dispatcher_one_to_many_handle_port);
  base_hub_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_dispatcher_one_to_many_unhandle_port);

  g_object_class_install_property (gobject_class, PROP_MAIN_PORT,
      g_param_spec_int ("main",
          "Selected main port",
          "The selected main port, -1 indicates none.", -1, G_MAXINT,
          MAIN_PORT_NONE, G_PARAM_READWRITE));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsDispatcherOneToManyPrivate));
}

static void
kms_dispatcher_one_to_many_init (KmsDispatcherOneToMany * self)
{
  self->priv = KMS_DISPATCHER_ONE_TO_MANY_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      release_gint, kms_dispatcher_one_to_many_port_data_destroy);

  self->priv->main_port = MAIN_PORT_NONE;
}

gboolean
kms_dispatcher_one_to_many_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_DISPATCHER_ONE_TO_MANY);
}
