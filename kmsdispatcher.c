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

#include "kmsdispatcher.h"
#include "kmsmixerport.h"

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

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsDispatcher, kms_dispatcher,
    KMS_TYPE_BASE_HUB,
    GST_DEBUG_CATEGORY_INIT (kms_dispatcher_debug_category, PLUGIN_NAME,
        0, "debug category for dispatcher element"));

static void
destroy_gint (gpointer data)
{
  g_slice_free (gint, data);
}

static void
kms_dispatcher_port_data_destroy (gpointer data)
{
  /* TODO: */
}

static void
kms_dispatcher_dispose (GObject * object)
{
  KmsDispatcher *self = KMS_DISPATCHER (object);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  G_OBJECT_CLASS (kms_dispatcher_parent_class)->dispose (object);
}

static void
kms_dispatcher_finalize (GObject * object)
{
  KmsDispatcher *self = KMS_DISPATCHER (object);

  G_OBJECT_CLASS (kms_dispatcher_parent_class)->finalize (object);
  g_rec_mutex_clear (&self->priv->mutex);
}

static void
kms_dispatcher_unhandle_port (KmsBaseHub * hub, gint id)
{
  KmsDispatcher *self = KMS_DISPATCHER (hub);

  GST_DEBUG_OBJECT (self, "TODO:");
}

static gint
kms_dispatcher_handle_port (KmsBaseHub * hub, GstElement * mixer_port)
{
  KmsDispatcher *self = KMS_DISPATCHER (hub);

  GST_DEBUG_OBJECT (self, "TODO:");

  return -1;
}

static void
kms_dispatcher_class_init (KmsDispatcherClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseHubClass *base_hub_class = KMS_BASE_HUB_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Dispatcher", "Generic", "N to N dispatcher that makes dispatching of "
      "media flow", "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_dispatcher_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_dispatcher_finalize);

  base_hub_class->handle_port = GST_DEBUG_FUNCPTR (kms_dispatcher_handle_port);
  base_hub_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_dispatcher_unhandle_port);

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
