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
#ifndef _KMS_MAIN_MIXER_H_
#define _KMS_MAIN_MIXER_H_

#include "kmsbasehub.h"

G_BEGIN_DECLS
#define KMS_TYPE_MAIN_MIXER kms_main_mixer_get_type()
#define KMS_MAIN_MIXER(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST(   \
    (obj),                      \
    KMS_TYPE_MAIN_MIXER,        \
    KmsMainMixer                \
  )                             \
)
#define KMS_MAIN_MIXER_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_MAIN_MIXER,                \
    KmsMainMixerClass                   \
  )                                     \
)
#define KMS_IS_MAIN_MIXER(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_MAIN_MIXER                 \
  )                                     \
)
#define KMS_IS_MAIN_MIXER_CLASS(klass) (        \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_MAIN_MIXER)                          \
)

typedef struct _KmsMainMixer KmsMainMixer;
typedef struct _KmsMainMixerClass KmsMainMixerClass;
typedef struct _KmsMainMixerPrivate KmsMainMixerPrivate;

struct _KmsMainMixer
{
  KmsBaseHub parent;

  /*< private > */
  KmsMainMixerPrivate *priv;
};

struct _KmsMainMixerClass
{
  KmsBaseHubClass parent_class;
};

GType kms_main_mixer_get_type (void);

gboolean kms_main_mixer_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_MAIN_MIXER_H_ */
