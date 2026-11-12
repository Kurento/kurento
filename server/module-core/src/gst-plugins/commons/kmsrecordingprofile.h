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

#ifndef __KMS_RECORDING_PROFILE_H__
#define __KMS_RECORDING_PROFILE_H__

#include <glib.h>

#include <gst/pbutils/encoding-profile.h>
#include "kmselement.h"

G_BEGIN_DECLS

typedef enum
{
  KMS_RECORDING_PROFILE_NONE = -1,
  KMS_RECORDING_PROFILE_WEBM,
  KMS_RECORDING_PROFILE_MKV,
  KMS_RECORDING_PROFILE_MP4,
  KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY,
  KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY,
  KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY,
  KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY,
  KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
  KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY,
  KMS_RECORDING_PROFILE_JPEG_VIDEO_ONLY,
  KMS_RECORDING_PROFILE_KSR,
  KMS_RECORDING_PROFILE_FLV
} KmsRecordingProfile;

/**
 * Video codec stored in the recording container. NONE selects the default
 * codec of the container (H264 for MP4/MKV/FLV, VP8 for WebM).
 */
typedef enum
{
  KMS_RECORDING_VIDEO_CODEC_NONE = 0,
  KMS_RECORDING_VIDEO_CODEC_H264,
  KMS_RECORDING_VIDEO_CODEC_H265,
  KMS_RECORDING_VIDEO_CODEC_VP8,
  KMS_RECORDING_VIDEO_CODEC_VP9,
  KMS_RECORDING_VIDEO_CODEC_AV1
} KmsRecordingVideoCodec;

GstEncodingContainerProfile * kms_recording_profile_create_profile (
    KmsRecordingProfile profile, gboolean has_audio, gboolean has_video);

GstEncodingContainerProfile * kms_recording_profile_create_profile_with_codec (
    KmsRecordingProfile profile, KmsRecordingVideoCodec video_codec,
    gboolean has_audio, gboolean has_video);

/* Container x codec validation (NONE is always valid) */
gboolean kms_recording_profile_supports_video_codec (
    KmsRecordingProfile profile, KmsRecordingVideoCodec codec);

KmsRecordingVideoCodec kms_recording_profile_get_default_video_codec (
    KmsRecordingProfile profile);

gboolean kms_recording_profile_supports_type (KmsRecordingProfile profile,
    KmsElementPadType type);

G_END_DECLS
#endif /* __KMS_RECORDING_PROFILE_H__ */
