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
#ifndef _KMS_AUDIO_MIXER_BIN_H_
#define _KMS_AUDIO_MIXER_BIN_H_

G_BEGIN_DECLS
#define KMS_TYPE_AUDIO_MIXER_BIN kms_audio_mixer_bin_get_type()

#define KMS_AUDIO_MIXER_BIN(obj) ( \
  G_TYPE_CHECK_INSTANCE_CAST(      \
    (obj),                         \
    KMS_TYPE_AUDIO_MIXER_BIN,      \
    KmsAudioMixerBin               \
  )                                \
)

#define KMS_AUDIO_MIXER_BIN_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_AUDIO_MIXER_BIN,              \
    KmsAudioMixerBinClass                  \
  )                                        \
)
#define KMS_IS_AUDIO_MIXER_BIN(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (        \
    (obj),                            \
    KMS_TYPE_AUDIO_MIXER_BIN          \
  )                                   \
)
#define KMS_IS_AUDIO_MIXER_BIN_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE((klass),            \
  KMS_TYPE_AUDIO_MIXER_BIN)                   \
)

#define AUDIO_MIXER_BIN_SINK_PAD_PREFIX  "sink_"

#define AUDIO_MIXER_BIN_SINK_PAD AUDIO_MIXER_BIN_SINK_PAD_PREFIX "%u"
#define AUDIO_MIXER_BIN_SRC_PAD "src"

#define LENGTH_AUDIO_MIXER_BIN_SINK_PAD_PREFIX 5  /* sizeof("sink_") */

typedef struct _KmsAudioMixerBin KmsAudioMixerBin;
typedef struct _KmsAudioMixerBinClass KmsAudioMixerBinClass;
typedef struct _KmsAudioMixerBinPrivate KmsAudioMixerBinPrivate;

struct _KmsAudioMixerBin
{
  GstBin parent;

  /*< private > */
  KmsAudioMixerBinPrivate *priv;
};

struct _KmsAudioMixerBinClass
{
  GstBinClass parent_class;
};

GType kms_audio_mixer_bin_get_type (void);

gboolean kms_audio_mixer_bin_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_AUDIO_MIXER_BIN_H_ */