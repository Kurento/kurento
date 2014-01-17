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
#ifndef _KMS_BASE_MIXER_H_
#define _KMS_BASE_MIXER_H_

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_BASE_MIXER kms_base_mixer_get_type()
#define KMS_BASE_MIXER(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST(   \
    (obj),                      \
    KMS_TYPE_BASE_MIXER,        \
    KmsBaseMixer                \
  )                             \
)
#define KMS_BASE_MIXER_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_BASE_MIXER,                \
    KmsBaseMixerClass                   \
  )                                     \
)
#define KMS_IS_BASE_MIXER(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_BASE_MIXER                 \
  )                                     \
)
#define KMS_IS_BASE_MIXER_CLASS(klass) (        \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_BASE_MIXER)                          \
)
typedef struct _KmsBaseMixer KmsBaseMixer;
typedef struct _KmsBaseMixerClass KmsBaseMixerClass;
typedef struct _KmsBaseMixerPrivate KmsBaseMixerPrivate;

struct _KmsBaseMixer
{
  KmsElement parent;

  /*< private > */
  KmsBaseMixerPrivate *priv;
};

struct _KmsBaseMixerClass
{
  KmsElementClass parent_class;

  /* Actions */
  gboolean (*handle_port) (KmsBaseMixer *self, GstElement *mixer_end_point);
};

GType kms_base_mixer_get_type (void);

G_END_DECLS
#endif /* _KMS_BASE_MIXER_H_ */
