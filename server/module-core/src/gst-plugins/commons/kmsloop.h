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

gboolean kms_loop_remove (KmsLoop *self, guint source_id);

#define KMS_LOOP_IS_CURRENT_THREAD(loop) \
  kms_loop_is_current_thread(loop)

gboolean kms_loop_is_current_thread (KmsLoop *self);

G_END_DECLS
#endif
