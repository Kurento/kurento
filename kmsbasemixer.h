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
  GstBin parent;

  /*< private > */
  KmsBaseMixerPrivate *priv;
};

struct _KmsBaseMixerClass
{
  GstBinClass parent_class;

  /* Actions */
  gint (*handle_port) (KmsBaseMixer * self, GstElement * mixer_end_point);
  void (*unhandle_port) (KmsBaseMixer * self, gint port_id);

  /* Virtual methods */
  gboolean (*link_video_src) (KmsBaseMixer * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name);
  gboolean (*link_audio_src) (KmsBaseMixer * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name);
};

gboolean kms_base_mixer_link_video_src (KmsBaseMixer * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name);
gboolean kms_base_mixer_link_audio_src (KmsBaseMixer * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name);

GType kms_base_mixer_get_type (void);

G_END_DECLS
#endif /* _KMS_BASE_MIXER_H_ */
