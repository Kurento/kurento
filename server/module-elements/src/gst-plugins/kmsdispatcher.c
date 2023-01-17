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

#include <commons/kms-core-marshal.h>
#include "kmsdispatcher.h"
#include <commons/kmshubport.h>

#define PLUGIN_NAME "dispatcher"

#define KMS_DISPATCHER_LOCK(e) \
  (g_rec_mutex_lock (&(e)->priv->mutex))

#define KMS_DISPATCHER_UNLOCK(e) \
  (g_rec_mutex_unlock (&(e)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_dispatcher_debug_category);
#define GST_CAT_DEFAULT kms_dispatcher_debug_category

#define KMS_DISPATCHER_GET_PRIVATE(obj) (       \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_DISPATCHER,                        \
    KmsDispatcherPrivate                        \
  )                                             \
)

struct _KmsDispatcherPrivate
{
  GRecMutex mutex;
  GHashTable *ports;
};

typedef struct _KmsDispatcherPortData KmsDispatcherPortData;

struct _KmsDispatcherPortData
{
  KmsDispatcher *dispatcher;
  gint id;
  GstElement *audio_agnostic;
  GstElement *video_agnostic;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsDispatcher, kms_dispatcher,
    KMS_TYPE_BASE_HUB,
    GST_DEBUG_CATEGORY_INIT (kms_dispatcher_debug_category, PLUGIN_NAME,
        0, "debug category for dispatcher element"));

enum
{
  SIGNAL_CONNECT,
  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static void
destroy_gint (gpointer data)
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
kms_dispatcher_port_data_destroy (gpointer data)
{
  KmsDispatcherPortData *port_data = (KmsDispatcherPortData *) data;
  KmsDispatcher *self = port_data->dispatcher;

  KMS_DISPATCHER_LOCK (self);
  gst_bin_remove_many (GST_BIN (self), port_data->audio_agnostic,
      port_data->video_agnostic, NULL);
  KMS_DISPATCHER_UNLOCK (self);

  gst_element_set_state (port_data->audio_agnostic, GST_STATE_NULL);
  gst_element_set_state (port_data->video_agnostic, GST_STATE_NULL);

  g_clear_object (&port_data->audio_agnostic);
  g_clear_object (&port_data->video_agnostic);

  g_slice_free (KmsDispatcherPortData, data);
}

static KmsDispatcherPortData *
kms_dispatcher_port_data_create (KmsDispatcher * self, gint id)
{
  KmsDispatcherPortData *data = g_slice_new0 (KmsDispatcherPortData);

  data->dispatcher = self;
  data->audio_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->video_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->id = id;

  gst_bin_add_many (GST_BIN (self), g_object_ref (data->audio_agnostic),
      g_object_ref (data->video_agnostic), NULL);
  gst_element_sync_state_with_parent (data->audio_agnostic);
  gst_element_sync_state_with_parent (data->video_agnostic);

  kms_base_hub_link_video_sink (KMS_BASE_HUB (self), id,
      data->video_agnostic, "sink", FALSE);
  kms_base_hub_link_audio_sink (KMS_BASE_HUB (self), id,
      data->audio_agnostic, "sink", FALSE);

  return data;
}

static void
kms_dispatcher_dispose (GObject * object)
{
  KmsDispatcher *self = KMS_DISPATCHER (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_DISPATCHER_LOCK (self);
  if (self->priv->ports != NULL) {
    g_hash_table_remove_all (self->priv->ports);
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }
  KMS_DISPATCHER_UNLOCK (self);

  G_OBJECT_CLASS (kms_dispatcher_parent_class)->dispose (object);
}

static void
kms_dispatcher_finalize (GObject * object)
{
  KmsDispatcher *self = KMS_DISPATCHER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_dispatcher_parent_class)->finalize (object);
}

static void
kms_dispatcher_unhandle_port (KmsBaseHub * hub, gint id)
{
  KmsDispatcher *self = KMS_DISPATCHER (hub);

  KMS_DISPATCHER_LOCK (self);

  g_hash_table_remove (self->priv->ports, &id);

  KMS_DISPATCHER_UNLOCK (self);

  KMS_BASE_HUB_CLASS (kms_dispatcher_parent_class)->unhandle_port (hub, id);
}

static gint
kms_dispatcher_handle_port (KmsBaseHub * hub, GstElement * mixer_port)
{
  KmsDispatcher *self = KMS_DISPATCHER (hub);
  KmsDispatcherPortData *port_data;
  gint port_id;

  port_id = KMS_BASE_HUB_CLASS (kms_dispatcher_parent_class)->handle_port (hub,
      mixer_port);

  if (port_id < 0)
    return -1;

  port_data = kms_dispatcher_port_data_create (self, port_id);

  KMS_DISPATCHER_LOCK (self);

  g_hash_table_insert (self->priv->ports, create_gint (port_id), port_data);

  KMS_DISPATCHER_UNLOCK (self);

  return port_id;
}

static gboolean
kms_dispatcher_connect (KmsDispatcher * self, guint source, guint sink)
{
  KmsDispatcherPortData *source_port, *sink_port;
  gboolean connected = FALSE;

  KMS_DISPATCHER_LOCK (self);

  source_port = g_hash_table_lookup (self->priv->ports, &source);
  if (source_port == NULL) {
    GST_ERROR_OBJECT (self, "No source port %u found", source);
    goto end;
  }

  sink_port = g_hash_table_lookup (self->priv->ports, &sink);
  if (sink_port == NULL) {
    GST_ERROR_OBJECT (self, "No sink port %u found", source);
    goto end;
  }

  if (!kms_base_hub_link_audio_src (KMS_BASE_HUB (self), sink_port->id,
          source_port->audio_agnostic, "src_%u", TRUE)) {
    GST_ERROR_OBJECT (self, "Can not connect audio port");
    goto end;
  }

  if (!kms_base_hub_link_video_src (KMS_BASE_HUB (self), sink_port->id,
          source_port->video_agnostic, "src_%u", TRUE)) {
    GST_ERROR_OBJECT (self, "Can not connect video port");
    kms_base_hub_unlink_audio_src (KMS_BASE_HUB (self), sink_port->id);
    goto end;
  }

  connected = TRUE;

end:

  KMS_DISPATCHER_UNLOCK (self);
  return connected;
}

static void
kms_dispatcher_class_init (KmsDispatcherClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseHubClass *base_hub_class = KMS_BASE_HUB_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Dispatcher", "Generic", "N to M dispatcher that makes dispatching of "
      "media flow", "Santiago Carot-Nemesio <sancane at gmail dot com>");

  klass->connect = GST_DEBUG_FUNCPTR (kms_dispatcher_connect);

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_dispatcher_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_dispatcher_finalize);

  base_hub_class->handle_port = GST_DEBUG_FUNCPTR (kms_dispatcher_handle_port);
  base_hub_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_dispatcher_unhandle_port);

  /* Signals initialization */
  obj_signals[SIGNAL_CONNECT] =
      g_signal_new ("connect",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsDispatcherClass, connect), NULL, NULL,
      __kms_core_marshal_BOOLEAN__UINT_UINT, G_TYPE_BOOLEAN, 2, G_TYPE_UINT,
      G_TYPE_UINT);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsDispatcherPrivate));
}

static void
kms_dispatcher_init (KmsDispatcher * self)
{
  self->priv = KMS_DISPATCHER_GET_PRIVATE (self);
  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      destroy_gint, kms_dispatcher_port_data_destroy);

  g_rec_mutex_init (&self->priv->mutex);
}

gboolean
kms_dispatcher_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_DISPATCHER);
}
