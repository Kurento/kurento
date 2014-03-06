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
#ifndef _KMS_MIXER_ENDPOINT_H_
#define _KMS_MIXER_ENDPOINT_H_

#include "kmselement.h"

#define MIXER_AUDIO_SINK_PAD "mixer_audio_sink"
#define MIXER_VIDEO_SINK_PAD "mixer_video_sink"
#define MIXER_AUDIO_SRC_PAD "mixer_audio_src"
#define MIXER_VIDEO_SRC_PAD "mixer_video_src"

G_BEGIN_DECLS
#define KMS_TYPE_MIXER_ENDPOINT kms_mixer_endpoint_get_type()
#define KMS_MIXER_ENDPOINT(obj) (               \
  G_TYPE_CHECK_INSTANCE_CAST(                   \
    (obj),                                      \
    KMS_TYPE_MIXER_ENDPOINT,                    \
    KmsMixerEndpoint                            \
  )                                             \
)
#define KMS_MIXER_ENDPOINT_CLASS(klass) (       \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_MIXER_ENDPOINT,                    \
    KmsMixerEndpointClass                       \
  )                                             \
)
#define KMS_IS_MIXER_ENDPOINT(obj) (            \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_MIXER_ENDPOINT                     \
  )                                             \
)
#define KMS_IS_MIXER_ENDPOINT_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_MIXER_ENDPOINT)                      \
)
typedef struct _KmsMixerEndpoint KmsMixerEndpoint;
typedef struct _KmsMixerEndpointClass KmsMixerEndpointClass;
typedef struct _KmsMixerEndpointPrivate KmsMixerEndpointPrivate;

struct _KmsMixerEndpoint
{
  KmsElement parent;

  /*< private > */
  KmsMixerEndpointPrivate *priv;
};

struct _KmsMixerEndpointClass
{
  KmsElementClass parent_class;
};

GType kms_mixer_endpoint_get_type (void);

gboolean kms_mixer_endpoint_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_MIXER_ENDPOINT_H_ */
