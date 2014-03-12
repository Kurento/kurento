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
#ifndef _KMS_AUDIO_MIXER_H_
#define _KMS_AUDIO_MIXER_H_

G_BEGIN_DECLS
#define KMS_TYPE_AUDIO_MIXER kms_audio_mixer_get_type()

#define KMS_AUDIO_MIXER(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST(  \
    (obj),                     \
    KMS_TYPE_AUDIO_MIXER,      \
    KmsAudioMixer              \
  )                            \
)

#define KMS_AUDIO_MIXER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (            \
    (klass),                           \
    KMS_TYPE_AUDIO_MIXER,              \
    KmsAudioMixerClass                 \
  )                                    \
)
#define KMS_IS_AUDIO_MIXER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (    \
    (obj),                        \
    KMS_TYPE_AUDIO_MIXER          \
  )                               \
)
#define KMS_IS_AUDIO_MIXER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE((klass),        \
  KMS_TYPE_AUDIO_MIXER)                   \
)

typedef struct _KmsAudioMixer KmsAudioMixer;
typedef struct _KmsAudioMixerClass KmsAudioMixerClass;
typedef struct _KmsAudioMixerPrivate KmsAudioMixerPrivate;

struct _KmsAudioMixer
{
  GstBin parent;

  /*< private > */
  KmsAudioMixerPrivate *priv;
};

struct _KmsAudioMixerClass
{
  GstBinClass parent_class;
};

GType kms_audio_mixer_get_type (void);

gboolean kms_audio_mixer_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_AUDIO_MIXER_H_ */