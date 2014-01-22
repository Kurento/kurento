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
#ifndef _KMS_MIXER_END_POINT_H_
#define _KMS_MIXER_END_POINT_H_

#include "kmselement.h"

#define MIXER_AUDIO_SINK_PAD "mixer_audio_sink"
#define MIXER_VIDEO_SINK_PAD "mixer_video_sink"
#define MIXER_AUDIO_SRC_PAD "mixer_audio_src"
#define MIXER_VIDEO_SRC_PAD "mixer_video_src"

G_BEGIN_DECLS
#define KMS_TYPE_MIXER_END_POINT kms_mixer_end_point_get_type()
#define KMS_MIXER_END_POINT(obj) (              \
  G_TYPE_CHECK_INSTANCE_CAST(                   \
    (obj),                                      \
    KMS_TYPE_MIXER_END_POINT,                   \
    KmsMixerEndPoint                            \
  )                                             \
)
#define KMS_MIXER_END_POINT_CLASS(klass) (      \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_MIXER_END_POINT,                   \
    KmsMixerEndPointClass                       \
  )                                             \
)
#define KMS_IS_MIXER_END_POINT(obj) (           \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_MIXER_END_POINT                    \
  )                                             \
)
#define KMS_IS_MIXER_END_POINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_MIXER_END_POINT)                     \
)
typedef struct _KmsMixerEndPoint KmsMixerEndPoint;
typedef struct _KmsMixerEndPointClass KmsMixerEndPointClass;
typedef struct _KmsMixerEndPointPrivate KmsMixerEndPointPrivate;

struct _KmsMixerEndPoint
{
  KmsElement parent;

  /*< private > */
  KmsMixerEndPointPrivate *priv;
};

struct _KmsMixerEndPointClass
{
  KmsElementClass parent_class;
};

GType kms_mixer_end_point_get_type (void);

gboolean kms_mixer_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_MIXER_END_POINT_H_ */
