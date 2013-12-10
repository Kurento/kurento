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
#ifndef _KMS_LOOP_H_
#define _KMS_LOOP_H_

G_BEGIN_DECLS
#define KMS_TYPE_LOOP (kms_loop_get_type())
#define KMS_LOOP(obj) (                    \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_LOOP,                         \
    KmsLoop                                \
  )                                        \
)
#define KMS_LOOP_CLASS(klass) (            \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_LOOP,                         \
    KmsLoopClass                           \
  )                                        \
)
#define KMS_IS_LOOP(obj) (                 \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_LOOP                          \
  )                                        \
)
#define KMS_IS_LOOP_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), KMS_TYPE_LOOP))
#define KMS_LOOP_GET_CLASS(obj) (          \
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_LOOP,                         \
    KmsLoopClass                           \
  )                                        \
)
typedef struct _KmsLoop KmsLoop;
typedef struct _KmsLoopClass KmsLoopClass;
typedef struct _KmsLoopPrivate KmsLoopPrivate;

struct _KmsLoop
{
  GObject parent;

  /*< private > */
  KmsLoopPrivate *priv;
};

struct _KmsLoopClass
{
  GObjectClass parent_class;
};

GType kms_loop_get_type (void);

KmsLoop * kms_loop_new (void);

guint kms_loop_idle_add (KmsLoop *self, GSourceFunc function,
  gpointer data);

guint kms_loop_idle_add_full (KmsLoop *self, gint priority,
  GSourceFunc function, gpointer data, GDestroyNotify notify);

guint kms_loop_timeout_add (KmsLoop *self, guint interval, GSourceFunc function,
  gpointer data);

guint kms_loop_timeout_add_full (KmsLoop *self, gint priority, guint interval,
  GSourceFunc function, gpointer data, GDestroyNotify notify);

G_END_DECLS
#endif