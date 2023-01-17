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
#ifndef _KMS_HUB_PORT_H_
#define _KMS_HUB_PORT_H_

#include "kmselement.h"

#define HUB_AUDIO_SINK_PAD "hub_audio_sink"
#define HUB_VIDEO_SINK_PAD "hub_video_sink"
#define HUB_DATA_SINK_PAD "hub_data_sink"

#define HUB_AUDIO_SRC_PAD "hub_audio_src"
#define HUB_VIDEO_SRC_PAD "hub_video_src"
#define HUB_DATA_SRC_PAD "hub_data_src"

G_BEGIN_DECLS
#define KMS_TYPE_HUB_PORT kms_hub_port_get_type()
#define KMS_HUB_PORT(obj) (                     \
  G_TYPE_CHECK_INSTANCE_CAST(                   \
    (obj),                                      \
    KMS_TYPE_HUB_PORT,                          \
    KmsHubPort                                  \
  )                                             \
)
#define KMS_HUB_PORT_CLASS(klass) (             \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_HUB_PORT,                          \
    KmsHubPortClass                             \
  )                                             \
)
#define KMS_IS_HUB_PORT(obj) (                  \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_HUB_PORT                           \
  )                                             \
)
#define KMS_IS_HUB_PORT_CLASS(klass) (          \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_HUB_PORT)                            \
)
typedef struct _KmsHubPort KmsHubPort;
typedef struct _KmsHubPortClass KmsHubPortClass;
typedef struct _KmsHubPortPrivate KmsHubPortPrivate;

struct _KmsHubPort
{
  KmsElement parent;

  /*< private > */
  KmsHubPortPrivate *priv;
};

struct _KmsHubPortClass
{
  KmsElementClass parent_class;
};

GType kms_hub_port_get_type (void);

gboolean kms_hub_port_plugin_init (GstPlugin * plugin);

void kms_hub_port_unhandled (KmsHubPort * self);

G_END_DECLS
#endif /* _KMS_HUB_PORT_H_ */
