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

#include "kmsrecordingprofile.h"

/* Caps accepted by mp4mux and matroskamux for H265 are hvc1|hev1; hvc1 is the
 * only tag played by Safari/QuickTime, so it is the one requested upstream. */
static GstCaps *
kms_recording_profile_get_video_caps (KmsRecordingVideoCodec codec)
{
  switch (codec) {
    case KMS_RECORDING_VIDEO_CODEC_H264:
      return gst_caps_from_string ("video/x-h264, "
          "stream-format=(string)avc, alignment=(string)au");
    case KMS_RECORDING_VIDEO_CODEC_H265:
      return gst_caps_from_string ("video/x-h265, "
          "stream-format=(string)hvc1, alignment=(string)au");
    case KMS_RECORDING_VIDEO_CODEC_VP8:
      return gst_caps_from_string ("video/x-vp8");
    case KMS_RECORDING_VIDEO_CODEC_VP9:
      return gst_caps_from_string ("video/x-vp9");
    case KMS_RECORDING_VIDEO_CODEC_AV1:
      return gst_caps_from_string ("video/x-av1, "
          "stream-format=(string)obu-stream, alignment=(string)tu");
    default:
      return NULL;
  }
}

static void
kms_recording_profile_add_video_profile (GstEncodingContainerProfile * cprof,
    KmsRecordingVideoCodec codec)
{
  GstCaps *vc = kms_recording_profile_get_video_caps (codec);

  g_return_if_fail (vc != NULL);

  gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
      gst_encoding_video_profile_new (vc, NULL, NULL, 0));

  gst_caps_unref (vc);
}

static KmsRecordingVideoCodec
kms_recording_profile_resolve_video_codec (KmsRecordingProfile profile,
    KmsRecordingVideoCodec codec)
{
  if (codec != KMS_RECORDING_VIDEO_CODEC_NONE)
    return codec;

  return kms_recording_profile_get_default_video_codec (profile);
}

KmsRecordingVideoCodec
kms_recording_profile_get_default_video_codec (KmsRecordingProfile profile)
{
  switch (profile) {
    case KMS_RECORDING_PROFILE_WEBM:
    case KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY:
      return KMS_RECORDING_VIDEO_CODEC_VP8;
    case KMS_RECORDING_PROFILE_MP4:
    case KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY:
    case KMS_RECORDING_PROFILE_MKV:
    case KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY:
    case KMS_RECORDING_PROFILE_FLV:
      return KMS_RECORDING_VIDEO_CODEC_H264;
    default:
      return KMS_RECORDING_VIDEO_CODEC_NONE;
  }
}

gboolean
kms_recording_profile_supports_video_codec (KmsRecordingProfile profile,
    KmsRecordingVideoCodec codec)
{
  /* NONE always means "container's default codec" */
  if (codec == KMS_RECORDING_VIDEO_CODEC_NONE)
    return TRUE;

  switch (profile) {
    case KMS_RECORDING_PROFILE_WEBM:
    case KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY:
      return codec == KMS_RECORDING_VIDEO_CODEC_VP8 ||
          codec == KMS_RECORDING_VIDEO_CODEC_VP9 ||
          codec == KMS_RECORDING_VIDEO_CODEC_AV1;
    case KMS_RECORDING_PROFILE_MKV:
    case KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY:
      return codec == KMS_RECORDING_VIDEO_CODEC_H264 ||
          codec == KMS_RECORDING_VIDEO_CODEC_H265 ||
          codec == KMS_RECORDING_VIDEO_CODEC_VP8 ||
          codec == KMS_RECORDING_VIDEO_CODEC_VP9 ||
          codec == KMS_RECORDING_VIDEO_CODEC_AV1;
    case KMS_RECORDING_PROFILE_MP4:
    case KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY:
      return codec == KMS_RECORDING_VIDEO_CODEC_H264 ||
          codec == KMS_RECORDING_VIDEO_CODEC_H265 ||
          codec == KMS_RECORDING_VIDEO_CODEC_VP9 ||
          codec == KMS_RECORDING_VIDEO_CODEC_AV1;
    case KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY:
    case KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY:
    case KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY:
      /* No video is recorded, so the codec is ignored */
      return TRUE;
    default:
      /* FLV, KSR, JPEG: the video codec is not configurable */
      return FALSE;
  }
}

static GstEncodingContainerProfile *
kms_recording_profile_create_webm_profile (KmsRecordingVideoCodec codec,
    gboolean has_audio, gboolean has_video)
{
  GstEncodingContainerProfile *cprof;
  GstCaps *pc;

  if (has_video)
    pc = gst_caps_from_string ("video/webm");
  else
    pc = gst_caps_from_string ("audio/webm");

  cprof = gst_encoding_container_profile_new ("Webm", NULL, pc, NULL);
  gst_caps_unref (pc);

  if (has_audio) {
    GstCaps *ac = gst_caps_from_string ("audio/x-opus");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video)
    kms_recording_profile_add_video_profile (cprof, codec);

  return cprof;
}

static GstEncodingContainerProfile *
kms_recording_profile_create_mkv_profile (KmsRecordingVideoCodec codec,
    gboolean has_audio, gboolean has_video)
{
  GstEncodingContainerProfile *cprof;
  GstCaps *pc;

  pc = gst_caps_from_string ("video/x-matroska");

  cprof = gst_encoding_container_profile_new ("Mkv", NULL, pc, NULL);
  gst_caps_unref (pc);

  if (has_audio) {
    GstCaps *ac = gst_caps_from_string ("audio/mpeg,mpegversion=4");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video)
    kms_recording_profile_add_video_profile (cprof, codec);

  return cprof;
}

static GstEncodingContainerProfile *
kms_recording_profile_create_mp4_profile (KmsRecordingVideoCodec codec,
    gboolean has_audio, gboolean has_video)
{
  GstEncodingContainerProfile *cprof;
  GstCaps *pc;

  pc = gst_caps_from_string ("video/quicktime, variant=(string)iso");

  cprof = gst_encoding_container_profile_new ("Mp4", NULL, pc, NULL);
  gst_caps_unref (pc);

  if (has_audio) {
    GstCaps *ac = gst_caps_from_string ("audio/mpeg,mpegversion=4");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video)
    kms_recording_profile_add_video_profile (cprof, codec);

  return cprof;
}

static GstEncodingContainerProfile *
kms_recording_profile_create_flv_profile (KmsRecordingVideoCodec codec,
    gboolean has_audio, gboolean has_video)
{
  GstEncodingContainerProfile *cprof;
  GstCaps *pc;

  pc = gst_caps_from_string ("video/flv");

  cprof = gst_encoding_container_profile_new ("Flv", NULL, pc, NULL);
  gst_caps_unref (pc);

  if (has_audio) {
    GstCaps *ac = gst_caps_from_string ("audio/mpeg,mpegversion=4");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video) {
    /* FLV only supports H264, requested without alignment as always */
    GstCaps *vc = gst_caps_from_string ("video/x-h264, "
        "stream-format=(string)avc");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_video_profile_new (vc, NULL, NULL, 0));

    gst_caps_unref (vc);
  }

  return cprof;
}

static GstEncodingContainerProfile *
kms_recording_profile_create_ksr_profile (gboolean has_audio,
    gboolean has_video)
{
  GstEncodingContainerProfile *cprof;
  GstPadTemplate *templ;
  GstElement *mux;
  GstCaps *pc;

  pc = gst_caps_from_string ("application/x-ksr");
  cprof = gst_encoding_container_profile_new ("Ksr", NULL, pc, NULL);
  gst_caps_unref (pc);

  /* Use matroska caps to define this profile */
  mux = gst_element_factory_make ("matroskamux", NULL);

  if (has_audio) {
    GstCaps *ac;

    templ =
        gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (mux),
        "audio_%u");
    ac = gst_pad_template_get_caps (templ);

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video) {
    GstCaps *vc;

    templ =
        gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (mux),
        "video_%u");
    vc = gst_pad_template_get_caps (templ);

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_video_profile_new (vc, NULL, NULL, 0));

    gst_caps_unref (vc);
  }

  g_object_unref (mux);

  return cprof;
}

static GstEncodingContainerProfile *
kms_recording_profile_create_jpeg_profile ()
{
  GstEncodingContainerProfile *cprof;
  GstCaps *vc;
  GstCaps *pc;

  pc = gst_caps_from_string ("image/jpeg");
  cprof = gst_encoding_container_profile_new ("jpeg", NULL, pc, NULL);
  gst_caps_unref (pc);

  vc = gst_caps_from_string ("image/jpeg");

  gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
      gst_encoding_video_profile_new (vc, NULL, NULL, 0));

  gst_caps_unref (vc);

  return cprof;
}

GstEncodingContainerProfile *
kms_recording_profile_create_profile (KmsRecordingProfile profile,
    KmsRecordingVideoCodec video_codec, gboolean has_audio,
    gboolean has_video)
{
  KmsRecordingVideoCodec codec;

  if (!kms_recording_profile_supports_video_codec (profile, video_codec)) {
    GST_ERROR ("Video codec %d is not supported by recording profile %d",
        video_codec, profile);
    return NULL;
  }

  codec = kms_recording_profile_resolve_video_codec (profile, video_codec);

  switch (profile) {
    case KMS_RECORDING_PROFILE_WEBM:
      return kms_recording_profile_create_webm_profile (codec, has_audio, has_video);
    case KMS_RECORDING_PROFILE_MP4:
      return kms_recording_profile_create_mp4_profile (codec, has_audio, has_video);
    case KMS_RECORDING_PROFILE_MKV:
      return kms_recording_profile_create_mkv_profile (codec, has_audio, has_video);
    case KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY:
      return kms_recording_profile_create_webm_profile (codec, FALSE, has_video);
    case KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY:
      return kms_recording_profile_create_webm_profile (codec, has_audio, FALSE);
    case KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY:
      return kms_recording_profile_create_mp4_profile (codec, FALSE, has_video);
    case KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY:
      return kms_recording_profile_create_mp4_profile (codec, has_audio, FALSE);
    case KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY:
      return kms_recording_profile_create_mkv_profile (codec, FALSE, has_video);
    case KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY:
      return kms_recording_profile_create_mkv_profile (codec, has_audio, FALSE);
    case KMS_RECORDING_PROFILE_JPEG_VIDEO_ONLY:
      return kms_recording_profile_create_jpeg_profile ();
    case KMS_RECORDING_PROFILE_KSR:
      return kms_recording_profile_create_ksr_profile (has_audio, has_video);
    case KMS_RECORDING_PROFILE_FLV:
      return kms_recording_profile_create_flv_profile (codec, has_audio, has_video);
    default:
      GST_WARNING ("Invalid recording profile");
      return NULL;
  }

}

gboolean
kms_recording_profile_supports_type (KmsRecordingProfile profile,
    KmsElementPadType type)
{
  if (type == KMS_ELEMENT_PAD_TYPE_DATA) {
    return FALSE;
  }

  switch (profile) {
    case KMS_RECORDING_PROFILE_WEBM:
    case KMS_RECORDING_PROFILE_MP4:
    case KMS_RECORDING_PROFILE_MKV:
      return TRUE;
    case KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_JPEG_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY:
      return type == KMS_ELEMENT_PAD_TYPE_VIDEO;
    case KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY:
    case KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY:
    case KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY:
      return type == KMS_ELEMENT_PAD_TYPE_AUDIO;
    case KMS_RECORDING_PROFILE_KSR:
      return TRUE;
    case KMS_RECORDING_PROFILE_FLV:
      return TRUE;
    default:
      return FALSE;
  }
}
