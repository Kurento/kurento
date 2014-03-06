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
#ifndef _KMS_MIXER_PORT_H_
#define _KMS_MIXER_PORT_H_

#include "kmselement.h"

#define MIXER_AUDIO_SINK_PAD "mixer_audio_sink"
#define MIXER_VIDEO_SINK_PAD "mixer_video_sink"
#define MIXER_AUDIO_SRC_PAD "mixer_audio_src"
#define MIXER_VIDEO_SRC_PAD "mixer_video_src"

G_BEGIN_DECLS
#define KMS_TYPE_MIXER_PORT kms_mixer_port_get_type()
#define KMS_MIXER_PORT(obj) (                   \
  G_TYPE_CHECK_INSTANCE_CAST(                   \
    (obj),                                      \
    KMS_TYPE_MIXER_PORT,                        \
    KmsMixerPort                                \
  )                                             \
)
#define KMS_MIXER_PORT_CLASS(klass) (           \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_MIXER_PORT,                        \
    KmsMixerPortClass                           \
  )                                             \
)
#define KMS_IS_MIXER_PORT(obj) (                \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_MIXER_PORT                         \
  )                                             \
)
#define KMS_IS_MIXER_PORT_CLASS(klass) (        \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_MIXER_PORT)                          \
)
typedef struct _KmsMixerPort KmsMixerPort;
typedef struct _KmsMixerPortClass KmsMixerPortClass;
typedef struct _KmsMixerPortPrivate KmsMixerPortPrivate;

struct _KmsMixerPort
{
  KmsElement parent;

  /*< private > */
  KmsMixerPortPrivate *priv;
};

struct _KmsMixerPortClass
{
  KmsElementClass parent_class;
};

GType kms_mixer_port_get_type (void);

gboolean kms_mixer_port_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_MIXER_PORT_H_ */
