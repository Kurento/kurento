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
};

static void
kms_loop_dispose (GObject * obj)
{
  GST_DEBUG_OBJECT (obj, "Dispose");
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
}
