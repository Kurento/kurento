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

#include <gst/gst.h>
#include "kmsloop.h"

#define NAME "loop"

GST_DEBUG_CATEGORY_STATIC (kms_loop_debug_category);
#define GST_CAT_DEFAULT kms_loop_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsLoop, kms_loop,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_loop_debug_category, NAME,
        0, "debug category for kurento loop"));

#define KMS_LOOP_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (     \
    (obj),                          \
    KMS_TYPE_LOOP,                  \
    KmsLoopPrivate                  \
  )                                 \
)
struct _KmsLoopPrivate
{
  GThread *thread;
  GMainLoop *loop;
  GMainContext *context;
};

static gboolean
quit_main_loop (GMainLoop * loop)
{
  GST_INFO ("Exiting main loop");

  g_main_loop_quit (loop);

  return G_SOURCE_REMOVE;
}

static gpointer
loop_thread_init (gpointer data)
{
  KmsLoop *self = KMS_LOOP (data);

  if (!g_main_context_acquire (self->priv->context)) {
    GST_ERROR ("Can not acquire context");
    return NULL;
  }

  GST_DEBUG ("Running main loop");
  g_main_loop_run (self->priv->loop);
  GST_DEBUG ("Thread finished");

  g_main_context_release (self->priv->context);
  return NULL;
}

static void
kms_loop_dispose (GObject * obj)
{
  KmsLoop *self = KMS_LOOP (obj);

  GST_DEBUG_OBJECT (obj, "Dispose");

  if (self->priv->loop != NULL) {
    kms_loop_idle_add_full (self, G_PRIORITY_DEFAULT_IDLE,
        (GSourceFunc) quit_main_loop, self->priv->loop,
        (GDestroyNotify) g_main_loop_unref);
    self->priv->loop = NULL;
  }

  if (self->priv->thread != NULL) {
    if (g_thread_self () != self->priv->thread)
      g_thread_join (self->priv->thread);

    g_thread_unref (self->priv->thread);
    self->priv->thread = NULL;
  }

  if (self->priv->context != NULL) {
    g_main_context_unref (self->priv->context);
    self->priv->context = NULL;
  }

  G_OBJECT_CLASS (kms_loop_parent_class)->dispose (obj);
}

static void
kms_loop_finalize (GObject * obj)
{
  GST_DEBUG_OBJECT (obj, "Finalize");

  G_OBJECT_CLASS (kms_loop_parent_class)->finalize (obj);
}

static void
kms_loop_class_init (KmsLoopClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->dispose = kms_loop_dispose;
  objclass->finalize = kms_loop_finalize;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsLoopPrivate));
}

static void
kms_loop_init (KmsLoop * self)
{
  self->priv = KMS_LOOP_GET_PRIVATE (self);
  self->priv->context = g_main_context_new ();
  self->priv->loop = g_main_loop_new (self->priv->context, FALSE);

  self->priv->thread = g_thread_new (GST_OBJECT_NAME (self),
      loop_thread_init, self);
}

KmsLoop *
kms_loop_new (void)
{
  GObject *loop;

  loop = g_object_new (KMS_TYPE_LOOP, NULL);

  return KMS_LOOP (loop);
}

static guint
kms_loop_attach (KmsLoop * self, GSource * source, gint priority,
    GSourceFunc function, gpointer data, GDestroyNotify notify)
{
  guint id;

  g_source_set_priority (source, priority);
  g_source_set_callback (source, function, data, notify);
  id = g_source_attach (source, self->priv->context);

  return id;
}

guint
kms_loop_idle_add_full (KmsLoop * self, gint priority, GSourceFunc function,
    gpointer data, GDestroyNotify notify)
{
  GSource *source;
  guint id;

  source = g_idle_source_new ();
  id = kms_loop_attach (self, source, priority, function, data, notify);
  g_source_unref (source);

  return id;
}

guint
kms_loop_idle_add (KmsLoop * self, GSourceFunc function, gpointer data)
{
  return kms_loop_idle_add_full (self, G_PRIORITY_DEFAULT_IDLE, function, data,
      NULL);
}

guint
kms_loop_timeout_add_full (KmsLoop * self, gint priority, guint interval,
    GSourceFunc function, gpointer data, GDestroyNotify notify)
{
  GSource *source;
  guint id;

  source = g_timeout_source_new (interval);
  id = kms_loop_attach (self, source, priority, function, data, notify);
  g_source_unref (source);

  return id;
}

guint
kms_loop_timeout_add (KmsLoop * self, guint interval, GSourceFunc function,
    gpointer data)
{
  return kms_loop_timeout_add_full (self, G_PRIORITY_DEFAULT, interval,
      function, data, NULL);
}
