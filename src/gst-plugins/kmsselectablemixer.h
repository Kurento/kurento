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
#ifndef _KMS_SELECTABLE_MIXER_H_
#define _KMS_SELECTABLE_MIXER_H_

#include "kmsbasehub.h"

G_BEGIN_DECLS
#define KMS_TYPE_SELECTABLE_MIXER kms_selectable_mixer_get_type()
#define KMS_SELECTABLE_MIXER(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST(       \
    (obj),                          \
    KMS_TYPE_SELECTABLE_MIXER,      \
    KmsSelectableMixer              \
  )                                 \
)
#define KMS_SELECTABLE_MIXER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                 \
    (klass),                                \
    KMS_TYPE_SELECTABLE_MIXER,              \
    KmsSelectableMixerClass                 \
  )                                         \
)
#define KMS_IS_SELECTABLE_MIXER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (         \
    (obj),                             \
    KMS_TYPE_SELECTABLE_MIXER          \
  )                                    \
)
#define KMS_IS_SELECTABLE_MIXER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE((klass),             \
  KMS_TYPE_SELECTABLE_MIXER)                   \
)

typedef struct _KmsSelectableMixer KmsSelectableMixer;
typedef struct _KmsSelectableMixerClass KmsSelectableMixerClass;
typedef struct _KmsSelectableMixerPrivate KmsSelectableMixerPrivate;

struct _KmsSelectableMixer
{
  KmsBaseHub parent;

  /*< private > */
  KmsSelectableMixerPrivate *priv;
};

struct _KmsSelectableMixerClass
{
  KmsBaseHubClass parent_class;

  /* Actions */
  gboolean (*connect_video) (KmsSelectableMixer * self, guint source, guint sink);
  gboolean (*connect_audio) (KmsSelectableMixer * self, guint source, guint sink);
  gboolean (*disconnect_audio) (KmsSelectableMixer * self, guint source, guint sink);
};

GType kms_selectable_mixer_get_type (void);

gboolean kms_selectable_mixer_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_SELECTABLE_MIXER_H_ */