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
#include "kmsselectablemixer.h"
#include <commons/kmshubport.h>

#define PLUGIN_NAME "selectablemixer"

#define KMS_SELECTABLE_MIXER_LOCK(e) \
  (g_rec_mutex_lock (&(e)->priv->mutex))

#define KMS_SELECTABLE_MIXER_UNLOCK(e) \
  (g_rec_mutex_unlock (&(e)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_selectable_mixer_debug_category);
#define GST_CAT_DEFAULT kms_selectable_mixer_debug_category

#define KMS_SELECTABLE_MIXER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_SELECTABLE_MIXER,                  \
    KmsSelectableMixerPrivate                   \
  )                                             \
)

struct _KmsSelectableMixerPrivate
{
  GRecMutex mutex;
  GHashTable *ports;
};

typedef struct _KmsSelectableMixerPortData KmsSelectableMixerPortData;

struct _KmsSelectableMixerPortData
{
  KmsSelectableMixer *mixer;
  GstElement *audiomixer;
  gint id;
  GstElement *audio_agnostic;
  GstElement *video_agnostic;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsSelectableMixer, kms_selectable_mixer,
    KMS_TYPE_BASE_HUB,
    GST_DEBUG_CATEGORY_INIT (kms_selectable_mixer_debug_category, PLUGIN_NAME,
        0, "debug category for selectable_mixer element"));

enum
{
  SIGNAL_CONNECT_VIDEO,
  SIGNAL_CONNECT_AUDIO,
  SIGNAL_DISCONNECT_AUDIO,
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
release_sink_pads (GstElement * audiomixer)
{
  GValue val = G_VALUE_INIT;
  GstIterator *it;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (audiomixer);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;
        GstElement *agnosticbin;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);
        agnosticbin = gst_pad_get_parent_element (srcpad);

        GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
            agnosticbin, audiomixer);

        if (!gst_pad_unlink (srcpad, sinkpad)) {
          GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
              srcpad, sinkpad);
        }

        gst_element_release_request_pad (audiomixer, sinkpad);
        gst_element_release_request_pad (agnosticbin, srcpad);

        gst_object_unref (srcpad);
        gst_object_unref (agnosticbin);
        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (audiomixer));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static gboolean
disconnect_elements (GstElement * agnosticbin, GstElement * audiomixer)
{
  gboolean done = FALSE, disconnected = FALSE;
  GValue val = G_VALUE_INIT;
  GstIterator *it;

  it = gst_element_iterate_src_pads (agnosticbin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *srcpad, *sinkpad;
        GstElement *mixer;

        srcpad = g_value_get_object (&val);
        sinkpad = gst_pad_get_peer (srcpad);
        mixer = gst_pad_get_parent_element (sinkpad);

        if (mixer == audiomixer) {
          GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
              agnosticbin, mixer);

          if (!gst_pad_unlink (srcpad, sinkpad)) {
            GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %"
                GST_PTR_FORMAT, srcpad, sinkpad);
          }

          gst_element_release_request_pad (mixer, sinkpad);
          gst_element_release_request_pad (agnosticbin, srcpad);
          disconnected |= TRUE;
        }

        gst_object_unref (sinkpad);
        gst_object_unref (mixer);
        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (agnosticbin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);

  return disconnected;
}

static void
kms_selectable_mixer_port_data_destroy (gpointer data)
{
  KmsSelectableMixerPortData *port_data = (KmsSelectableMixerPortData *) data;
  KmsSelectableMixer *self = port_data->mixer;

  KMS_SELECTABLE_MIXER_LOCK (self);

  release_sink_pads (port_data->audiomixer);

  gst_bin_remove_many (GST_BIN (self), port_data->audio_agnostic,
      port_data->video_agnostic, port_data->audiomixer, NULL);

  KMS_SELECTABLE_MIXER_LOCK (self);

  gst_element_set_state (port_data->audiomixer, GST_STATE_NULL);
  gst_element_set_state (port_data->audio_agnostic, GST_STATE_NULL);
  gst_element_set_state (port_data->video_agnostic, GST_STATE_NULL);

  g_clear_object (&port_data->audiomixer);
  g_clear_object (&port_data->video_agnostic);
  g_clear_object (&port_data->audio_agnostic);

  g_slice_free (KmsSelectableMixerPortData, data);
}

static KmsSelectableMixerPortData *
kms_selectable_mixer_port_data_create (KmsSelectableMixer * self, gint id)
{
  KmsSelectableMixerPortData *data = g_slice_new0 (KmsSelectableMixerPortData);

  data->mixer = self;
  data->audiomixer = gst_element_factory_make ("audiomixerbin", NULL);
  data->audio_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->video_agnostic = gst_element_factory_make ("agnosticbin", NULL);
  data->id = id;

  gst_bin_add_many (GST_BIN (self), g_object_ref (data->audio_agnostic),
      g_object_ref (data->video_agnostic), data->audiomixer, NULL);

  gst_element_sync_state_with_parent (data->audio_agnostic);
  gst_element_sync_state_with_parent (data->video_agnostic);
  gst_element_sync_state_with_parent (data->audiomixer);

  kms_base_hub_link_video_sink (KMS_BASE_HUB (self), id, data->video_agnostic,
      "sink", FALSE);
  kms_base_hub_link_audio_sink (KMS_BASE_HUB (self), id, data->audio_agnostic,
      "sink", FALSE);
  kms_base_hub_link_audio_src (KMS_BASE_HUB (self), id, data->audiomixer,
      "src", FALSE);

  return data;
}

static void
kms_selectable_mixer_dispose (GObject * object)
{
  KmsSelectableMixer *self = KMS_SELECTABLE_MIXER (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_SELECTABLE_MIXER_LOCK (self);

  if (self->priv->ports != NULL) {
    g_hash_table_remove_all (self->priv->ports);
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  KMS_SELECTABLE_MIXER_UNLOCK (self);

  G_OBJECT_CLASS (kms_selectable_mixer_parent_class)->dispose (object);
}

static void
kms_selectable_mixer_finalize (GObject * object)
{
  KmsSelectableMixer *self = KMS_SELECTABLE_MIXER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_selectable_mixer_parent_class)->finalize (object);
}

static void
kms_selectable_mixer_unhandle_port (KmsBaseHub * hub, gint id)
{
  KmsSelectableMixer *self = KMS_SELECTABLE_MIXER (hub);

  KMS_SELECTABLE_MIXER_LOCK (self);

  g_hash_table_remove (self->priv->ports, &id);

  KMS_SELECTABLE_MIXER_LOCK (self);

  KMS_BASE_HUB_CLASS (kms_selectable_mixer_parent_class)->unhandle_port (hub,
      id);
}

static gint
kms_selectable_mixer_handle_port (KmsBaseHub * hub, GstElement * mixer_port)
{
  KmsSelectableMixer *self = KMS_SELECTABLE_MIXER (hub);
  KmsSelectableMixerPortData *port_data;
  gint port_id;

  port_id =
      KMS_BASE_HUB_CLASS (kms_selectable_mixer_parent_class)->handle_port (hub,
      mixer_port);

  if (port_id < 0)
    return -1;

  port_data = kms_selectable_mixer_port_data_create (self, port_id);

  KMS_SELECTABLE_MIXER_LOCK (self);

  g_hash_table_insert (self->priv->ports, create_gint (port_id), port_data);

  KMS_SELECTABLE_MIXER_UNLOCK (self);

  return port_id;
}

static gboolean
kms_selectable_mixer_connect_video (KmsSelectableMixer * self, guint source,
    guint sink)
{
  KmsSelectableMixerPortData *source_port, *sink_port;
  gboolean connected = FALSE;

  KMS_SELECTABLE_MIXER_LOCK (self);

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

  if (!(connected =
          kms_base_hub_link_video_src (KMS_BASE_HUB (self), sink_port->id,
              source_port->video_agnostic, "src_%u", TRUE))) {
    GST_ERROR_OBJECT (self, "Can not connect video port");
  }

end:

  KMS_SELECTABLE_MIXER_UNLOCK (self);
  return connected;
}

static gboolean
kms_selectable_mixer_connect_audio (KmsSelectableMixer * self, guint source,
    guint sink)
{
  KmsSelectableMixerPortData *source_port, *sink_port;
  gboolean connected = FALSE;

  KMS_SELECTABLE_MIXER_LOCK (self);

  source_port = g_hash_table_lookup (self->priv->ports, &source);
  if (source_port == NULL) {
    GST_ERROR_OBJECT (self, "No source port %u found", source);
    goto end;
  }

  sink_port = g_hash_table_lookup (self->priv->ports, &sink);
  if (sink_port != NULL) {
    connected =
        gst_element_link (source_port->audio_agnostic, sink_port->audiomixer);
  } else {
    GST_ERROR_OBJECT (self, "No sink port %u found", source);
  }

end:

  KMS_SELECTABLE_MIXER_UNLOCK (self);

  return connected;
}

static gboolean
kms_selectable_mixer_disconnect_audio (KmsSelectableMixer * self, guint source,
    guint sink)
{
  KmsSelectableMixerPortData *source_port, *sink_port;
  gboolean disconnected = FALSE;

  KMS_SELECTABLE_MIXER_LOCK (self);

  source_port = g_hash_table_lookup (self->priv->ports, &source);
  if (source_port == NULL) {
    GST_ERROR_OBJECT (self, "No source port %u found", source);
    goto end;
  }

  sink_port = g_hash_table_lookup (self->priv->ports, &sink);
  if (sink_port != NULL) {
    disconnected = disconnect_elements (source_port->audio_agnostic,
        sink_port->audiomixer);
  } else {
    GST_ERROR_OBJECT (self, "No sink port %u found", source);
  }

end:

  KMS_SELECTABLE_MIXER_UNLOCK (self);

  return disconnected;
}

static void
kms_selectable_mixer_class_init (KmsSelectableMixerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseHubClass *base_hub_class = KMS_BASE_HUB_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "SelectableMixer", "Generic",
      "N to M selectable mixer that makes dispatching of "
      "media allowing to mix several audio streams",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  klass->connect_video = GST_DEBUG_FUNCPTR (kms_selectable_mixer_connect_video);
  klass->connect_audio = GST_DEBUG_FUNCPTR (kms_selectable_mixer_connect_audio);
  klass->disconnect_audio =
      GST_DEBUG_FUNCPTR (kms_selectable_mixer_disconnect_audio);

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_selectable_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_selectable_mixer_finalize);

  base_hub_class->handle_port =
      GST_DEBUG_FUNCPTR (kms_selectable_mixer_handle_port);
  base_hub_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_selectable_mixer_unhandle_port);

  /* Signals initialization */
  obj_signals[SIGNAL_CONNECT_VIDEO] =
      g_signal_new ("connect-video",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSelectableMixerClass, connect_video), NULL, NULL,
      __kms_core_marshal_BOOLEAN__UINT_UINT, G_TYPE_BOOLEAN, 2, G_TYPE_UINT,
      G_TYPE_UINT);

  obj_signals[SIGNAL_CONNECT_AUDIO] =
      g_signal_new ("connect-audio",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSelectableMixerClass, connect_audio), NULL, NULL,
      __kms_core_marshal_BOOLEAN__UINT_UINT, G_TYPE_BOOLEAN, 2, G_TYPE_UINT,
      G_TYPE_UINT);

  obj_signals[SIGNAL_DISCONNECT_AUDIO] =
      g_signal_new ("disconnect-audio",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSelectableMixerClass, disconnect_audio), NULL, NULL,
      __kms_core_marshal_BOOLEAN__UINT_UINT, G_TYPE_BOOLEAN, 2, G_TYPE_UINT,
      G_TYPE_UINT);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsSelectableMixerPrivate));
}

static void
kms_selectable_mixer_init (KmsSelectableMixer * self)
{
  self->priv = KMS_SELECTABLE_MIXER_GET_PRIVATE (self);
  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      destroy_gint, kms_selectable_mixer_port_data_destroy);

  g_rec_mutex_init (&self->priv->mutex);
}

gboolean
kms_selectable_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_SELECTABLE_MIXER);
}
