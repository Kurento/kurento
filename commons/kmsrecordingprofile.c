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

#include <kmsrecordingprofile.h>

static GstEncodingContainerProfile *
kms_recording_profile_create_webm_profile (gboolean has_audio,
    gboolean has_video)
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
    GstCaps *ac = gst_caps_from_string ("audio/x-vorbis");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video) {
    GstCaps *vc = gst_caps_from_string ("video/x-vp8");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_video_profile_new (vc, NULL, NULL, 0));

    gst_caps_unref (vc);
  }

  return cprof;
}

static GstEncodingContainerProfile *
kms_recording_profile_create_mp4_profile (gboolean has_audio,
    gboolean has_video)
{
  GstEncodingContainerProfile *cprof;
  GstCaps *pc;

  pc = gst_caps_from_string ("video/quicktime, variant=(string)iso");

  cprof = gst_encoding_container_profile_new ("Mp4", NULL, pc, NULL);
  gst_caps_unref (pc);

  if (has_audio) {
    GstCaps *ac = gst_caps_from_string ("audio/mpeg,mpegversion=1,layer=3");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video) {
    GstCaps *vc = gst_caps_from_string ("video/x-h264, "
        "stream-format=(string)avc, alignment=(string)au");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_video_profile_new (vc, NULL, NULL, 0));

    gst_caps_unref (vc);
  }

  return cprof;
}

GstEncodingContainerProfile *
kms_recording_profile_create_profile (KmsRecordingProfile profile,
    gboolean has_audio, gboolean has_video)
{
  switch (profile) {
    case KMS_RECORDING_PROFILE_WEBM:
      return kms_recording_profile_create_webm_profile (has_audio, has_video);
    case KMS_RECORDING_PROFILE_MP4:
      return kms_recording_profile_create_mp4_profile (has_audio, has_video);
    default:
      GST_WARNING ("Invalid recording profile");
      return NULL;
  }

}
