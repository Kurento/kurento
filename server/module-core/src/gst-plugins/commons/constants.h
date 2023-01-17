/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
#ifndef __KMS_CONSTANTS_H__
#define __KMS_CONSTANTS_H__

#define SDP_MEDIA_RTCP_FB "rtcp-fb"
#define SDP_MEDIA_RTCP_FB_NACK "nack"
#define SDP_MEDIA_RTCP_FB_CCM "ccm"
#define SDP_MEDIA_RTCP_FB_GOOG_REMB "goog-remb"
#define SDP_MEDIA_RTCP_FB_PLI "pli"
#define SDP_MEDIA_RTCP_FB_FIR "fir"

/* RTP Header Extensions */
#define RTP_HDR_EXT_ABS_SEND_TIME_URI "http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time"
#define RTP_HDR_EXT_ABS_SEND_TIME_SIZE 3
#define RTP_HDR_EXT_ABS_SEND_TIME_ID 3  /* TODO: do it dynamic when needed */

/* RTP/RTCP profiles */
#define SDP_MEDIA_RTP_AVP_PROTO "RTP/AVP"
#define SDP_MEDIA_RTP_SAVPF_PROTO "RTP/SAVPF"

#define RTCP_MIN_INTERVAL 500 /* ms */
#define REMB_MAX_INTERVAL 200 /* ms */
#define RTP_RTX_SIZE 512 /* packets */

/* rtpbin pad names */
#define RTPBIN_RECV_RTP_SINK "recv_rtp_sink_"
#define RTPBIN_RECV_RTCP_SINK "recv_rtcp_sink_"
#define RTPBIN_RECV_RTP_SRC "recv_rtp_src_"
#define RTPBIN_SEND_RTP_SRC "send_rtp_src_"
#define RTPBIN_SEND_RTCP_SRC "send_rtcp_src_"
#define RTPBIN_SEND_RTP_SINK "send_rtp_sink_"

#define AUDIO_STREAM_NAME "audio"
#define AUDIO_RTP_SESSION 0
#define AUDIO_RTP_SESSION_STR "0"
#define AUDIO_RTPBIN_RECV_RTP_SINK RTPBIN_RECV_RTP_SINK AUDIO_RTP_SESSION_STR
#define AUDIO_RTPBIN_RECV_RTCP_SINK RTPBIN_RECV_RTCP_SINK AUDIO_RTP_SESSION_STR
#define AUDIO_RTPBIN_RECV_RTP_SRC RTPBIN_RECV_RTP_SRC AUDIO_RTP_SESSION_STR
#define AUDIO_RTPBIN_SEND_RTP_SRC RTPBIN_SEND_RTP_SRC AUDIO_RTP_SESSION_STR
#define AUDIO_RTPBIN_SEND_RTCP_SRC RTPBIN_SEND_RTCP_SRC AUDIO_RTP_SESSION_STR
#define AUDIO_RTPBIN_SEND_RTP_SINK RTPBIN_SEND_RTP_SINK AUDIO_RTP_SESSION_STR

#define VIDEO_STREAM_NAME "video"
#define VIDEO_RTP_SESSION 1
#define VIDEO_RTP_SESSION_STR "1"
#define VIDEO_RTPBIN_RECV_RTP_SINK RTPBIN_RECV_RTP_SINK VIDEO_RTP_SESSION_STR
#define VIDEO_RTPBIN_RECV_RTCP_SINK RTPBIN_RECV_RTCP_SINK VIDEO_RTP_SESSION_STR
#define VIDEO_RTPBIN_RECV_RTP_SRC RTPBIN_RECV_RTP_SRC VIDEO_RTP_SESSION_STR
#define VIDEO_RTPBIN_SEND_RTP_SRC RTPBIN_SEND_RTP_SRC VIDEO_RTP_SESSION_STR
#define VIDEO_RTPBIN_SEND_RTCP_SRC RTPBIN_SEND_RTCP_SRC VIDEO_RTP_SESSION_STR
#define VIDEO_RTPBIN_SEND_RTP_SINK RTPBIN_SEND_RTP_SINK VIDEO_RTP_SESSION_STR

#define DATA_STREAM_NAME "data"

#define BUNDLE_STREAM_NAME "bundle"

/* RTP enconding names */
#define OPUS_ENCONDING_NAME "OPUS"
#define VP8_ENCONDING_NAME "VP8"

#endif /* __KMS_CONSTANTS_H__ */
