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
#ifndef _KMS_COMPOSITE_MIXER_H_
#define _KMS_COMPOSITE_MIXER_H_

#include <commons/kmsbasehub.h>

#define AUDIO_SINK_PAD_PREFIX  "sink_"
#define AUDIO_SRC_PAD_PREFIX  "src_"

#define AUDIO_SINK_PAD AUDIO_SINK_PAD_PREFIX "%u"
#define AUDIO_SRC_PAD AUDIO_SRC_PAD_PREFIX "%u"

#define LENGTH_AUDIO_SINK_PAD_PREFIX 5  /* sizeof("sink_") */
#define LENGTH_AUDIO_SRC_PAD_PREFIX 4   /* sizeof("src_") */

G_BEGIN_DECLS
#define KMS_TYPE_COMPOSITE_MIXER kms_composite_mixer_get_type()
#define KMS_COMPOSITE_MIXER(obj) (      \
  G_TYPE_CHECK_INSTANCE_CAST(           \
    (obj),                              \
    KMS_TYPE_COMPOSITE_MIXER,           \
    KmsCompositeMixer                   \
  )                                     \
)
#define KMS_COMPOSITE_MIXER_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                  \
    (klass),                                 \
    KMS_TYPE_COMPOSITE_MIXER,                \
    KmsCompositeMixerClass                   \
  )                                          \
)
#define KMS_IS_COMPOSITE_MIXER(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (               \
    (obj),                                   \
    KMS_TYPE_COMPOSITE_MIXER                 \
  )                                          \
)
#define KMS_IS_COMPOSITE_MIXER_CLASS(klass) (\
  G_TYPE_CHECK_CLASS_TYPE((klass),           \
  KMS_TYPE_COMPOSITE_MIXER)                  \
)

typedef struct _KmsCompositeMixer KmsCompositeMixer;
typedef struct _KmsCompositeMixerClass KmsCompositeMixerClass;
typedef struct _KmsCompositeMixerPrivate KmsCompositeMixerPrivate;

struct _KmsCompositeMixer
{
  KmsBaseHub parent;

  /*< private > */
  KmsCompositeMixerPrivate *priv;
};

struct _KmsCompositeMixerClass
{
  KmsBaseHubClass parent_class;
};

GType kms_composite_mixer_get_type (void);

gboolean kms_composite_mixer_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_COMPOSITE_MIXER_H_ */
