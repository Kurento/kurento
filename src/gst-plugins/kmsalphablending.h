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
#ifndef _KMS_ALPHA_BLENDING_H_
#define _KMS_ALPHA_BLENDING_H_

#include <commons/kmsbasehub.h>

#define AUDIO_SINK_PAD_PREFIX  "sink_"
#define AUDIO_SRC_PAD_PREFIX  "src_"

#define AUDIO_SINK_PAD AUDIO_SINK_PAD_PREFIX "%u"
#define AUDIO_SRC_PAD AUDIO_SRC_PAD_PREFIX "%u"

#define LENGTH_AUDIO_SINK_PAD_PREFIX 5  /* sizeof("sink_") */
#define LENGTH_AUDIO_SRC_PAD_PREFIX 4   /* sizeof("src_") */

G_BEGIN_DECLS
#define KMS_TYPE_ALPHA_BLENDING kms_alpha_blending_get_type()
#define KMS_ALPHA_BLENDING(obj) (      \
  G_TYPE_CHECK_INSTANCE_CAST(           \
    (obj),                              \
    KMS_TYPE_ALPHA_BLENDING,           \
    KmsAlphaBlending                   \
  )                                     \
)
#define KMS_ALPHA_BLENDING_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                  \
    (klass),                                 \
    KMS_TYPE_ALPHA_BLENDING,                \
    KmsAlphaBlendingClass                   \
  )                                          \
)
#define KMS_IS_ALPHA_BLENDING(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (               \
    (obj),                                   \
    KMS_TYPE_ALPHA_BLENDING                 \
  )                                          \
)
#define KMS_IS_ALPHA_BLENDING_CLASS(klass) (\
  G_TYPE_CHECK_CLASS_TYPE((klass),           \
  KMS_TYPE_ALPHA_BLENDING)                  \
)

typedef struct _KmsAlphaBlending KmsAlphaBlending;
typedef struct _KmsAlphaBlendingClass KmsAlphaBlendingClass;
typedef struct _KmsAlphaBlendingPrivate KmsAlphaBlendingPrivate;

struct _KmsAlphaBlending
{
  KmsBaseHub parent;

  /*< private > */
  KmsAlphaBlendingPrivate *priv;
};

struct _KmsAlphaBlendingClass
{
  KmsBaseHubClass parent_class;

    /* Actions */
  void (*set_port_properties) (KmsAlphaBlending * self, GstStructure * properties);
};

GType kms_alpha_blending_get_type (void);

gboolean kms_alpha_blending_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_ALPHA_BLENDING_H_ */
