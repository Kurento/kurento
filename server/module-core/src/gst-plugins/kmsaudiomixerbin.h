/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#define LENGTH_AUDIO_MIXER_BIN_SINK_PAD_PREFIX (sizeof(AUDIO_MIXER_BIN_SINK_PAD_PREFIX) - 1)
#define AUDIO_MIXER_BIN_SINK_PAD AUDIO_MIXER_BIN_SINK_PAD_PREFIX "%u"
#define AUDIO_MIXER_BIN_SRC_PAD "src"

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
