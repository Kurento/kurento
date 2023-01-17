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
#ifndef __KMS_AGNOSTIC_CAPS_H__
#define __KMS_AGNOSTIC_CAPS_H__

#define KMS_AGNOSTIC_RAW_AUDIO_CAPS \
  "audio/x-raw;"

#define KMS_AGNOSTIC_RAW_VIDEO_CAPS \
  "video/x-raw;"

#define KMS_AGNOSTIC_RAW_CAPS \
  KMS_AGNOSTIC_RAW_AUDIO_CAPS \
  KMS_AGNOSTIC_RAW_VIDEO_CAPS

#define KMS_AGNOSTIC_RTP_AUDIO_CAPS \
  "application/x-rtp,media=audio;"

#define KMS_AGNOSTIC_RTP_VIDEO_CAPS \
  "application/x-rtp,media=video;"

#define KMS_AGNOSTIC_RTP_CAPS \
  KMS_AGNOSTIC_RTP_AUDIO_CAPS \
  KMS_AGNOSTIC_RTP_VIDEO_CAPS

#define KMS_AGNOSTIC_FORMATS_AUDIO_CAPS \
  "audio/x-sbc;" \
  "audio/x-mulaw;" \
  "audio/x-flac;" \
  "audio/x-alaw;" \
  "audio/x-speex;" \
  "audio/x-ac3;" \
  "audio/x-alac;" \
  "audio/mpeg,mpegversion=1,layer=2;" \
  "audio/x-nellymoser;" \
  "audio/x-gst_ff-sonic;" \
  "audio/x-gst_ff-sonicls;" \
  "audio/x-wma,wmaversion=1;" \
  "audio/x-wma,wmaversion=2;" \
  "audio/x-dpcm,layout=roq;" \
  "audio/x-adpcm,layout=adx;" \
  "audio/x-adpcm,layout=g726;" \
  "audio/x-adpcm,layout=quicktime;" \
  "audio/x-adpcm,layout=dvi;" \
  "audio/x-adpcm,layout=microsoft;" \
  "audio/x-adpcm,layout=swf;" \
  "audio/x-adpcm,layout=yamaha;" \
  "audio/mpeg,mpegversion=4;" \
  "audio/mpeg,mpegversion=1,layer=3;" \
  "audio/x-celt;" \
  "audio/mpeg,mpegversion=[2, 4];" \
  "audio/x-vorbis;" \
  "audio/x-opus;" \
  "audio/AMR,rate=[8000, 16000],channels=1;" \
  "audio/x-gsm;"

#define KMS_AGNOSTIC_NO_RTP_AUDIO_CAPS \
  KMS_AGNOSTIC_RAW_AUDIO_CAPS \
  KMS_AGNOSTIC_FORMATS_AUDIO_CAPS

#define KMS_AGNOSTIC_AUDIO_CAPS \
  KMS_AGNOSTIC_NO_RTP_AUDIO_CAPS \
  KMS_AGNOSTIC_RTP_AUDIO_CAPS

#define KMS_AGNOSTIC_FORMATS_VIDEO_CAPS \
  "video/x-dirac;" \
  "image/png;" \
  "image/jpeg;" \
  "video/x-smoke;" \
  "video/x-asus,asusversion=1;" \
  "video/x-asus,asusversion=2;" \
  "image/bmp;" \
  "video/x-dnxhd;" \
  "video/x-dv;" \
  "video/x-ffv,ffvversion=1;" \
  "video/x-gst_ff-ffvhuff;" \
  "video/x-flash-screen;" \
  "video/x-flash-video,flvversion=1;" \
  "video/x-h261;" \
  "video/x-h263,variant=itu,h263version=h263;" \
  "video/x-h263,variant=itu,h263version=h263p;" \
  "video/x-huffyuv;" \
  "image/jpeg;" \
  "image/jpeg;" \
  "video/mpeg,mpegversion=1;" \
  "video/mpeg,mpegversion=2;" \
  "video/mpeg,mpegversion=4;" \
  "video/x-msmpeg,msmpegversion=41;" \
  "video/x-msmpeg,msmpegversion=42;" \
  "video/x-msmpeg,msmpegversion=43;" \
  "video/x-gst_ff-pam;" \
  "image/pbm;" \
  "video/x-gst_ff-pgm;" \
  "video/x-gst_ff-pgmyuv;" \
  "image/png;" \
  "image/ppm;" \
  "video/x-rle,layout=quicktime;" \
  "video/x-gst_ff-roqvideo;" \
  "video/x-pn-realvideo,rmversion=1;" \
  "video/x-pn-realvideo,rmversion=2;" \
  "video/x-gst_ff-snow;" \
  "video/x-svq,svqversion=1;" \
  "video/x-wmv,wmvversion=1;" \
  "video/x-wmv,wmvversion=2;" \
  "video/x-gst_ff-zmbv;" \
  "video/x-theora;" \
  "video/x-h264;" \
  "video/x-gst_ff-libxvid;" \
  "video/x-h264;" \
  "video/x-xvid;" \
  "video/mpeg,mpegversion=[1, 2];" \
  "video/x-theora;" \
  "video/x-vp8;" \
  "application/x-yuv4mpeg,y4mversion=2;"

#define KMS_AGNOSTIC_NO_RTP_VIDEO_CAPS \
  KMS_AGNOSTIC_RAW_VIDEO_CAPS \
  KMS_AGNOSTIC_FORMATS_VIDEO_CAPS

#define KMS_AGNOSTIC_VIDEO_CAPS \
  KMS_AGNOSTIC_NO_RTP_VIDEO_CAPS \
  KMS_AGNOSTIC_RTP_VIDEO_CAPS

#define KMS_AGNOSTIC_DATA_CAPS \
  "application/data;"

#define KMS_AGNOSTIC_CAPS \
  KMS_AGNOSTIC_AUDIO_CAPS \
  KMS_AGNOSTIC_VIDEO_CAPS

#define KMS_AGNOSTIC_NO_RTP_CAPS \
  KMS_AGNOSTIC_NO_RTP_AUDIO_CAPS \
  KMS_AGNOSTIC_NO_RTP_VIDEO_CAPS

#endif /* __KMS_AGNOSTIC_CAPS_H__ */
