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

/* Based on:
 * WebRTC Data Channels: draft-ietf-rtcweb-data-channel-13
 * WebRTC Data Channel Establishment Protocol: draft-ietf-rtcweb-data-protocol-09
 */

#ifndef __KMS_WEBRTC_DATA_PROTO_H__
#define __KMS_WEBRTC_DATA_PROTO_H__

typedef enum {
  KMS_DATA_CHANNEL_MESSAGE_TYPE_ACK = 0x02,
  KMS_DATA_CHANNEL_MESSAGE_TYPE_OPEN_REQUEST = 0x03
} KMSDataChannelMessageType;

typedef enum  {
  KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE = 0x00,
  KMS_DATA_CHANNEL_CHANNEL_TYPE_RELIABLE_UNORDERED = 0x80,
  KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX = 0x01,
  KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_REMIX_UNORDERED = 0x81,
  KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED = 0x02,
  KMS_DATA_CHANNEL_CHANNEL_TYPE_PARTIAL_RELIABLE_TIMED_UNORDERED = 0x82
} KmsDataChannelChannelType;

typedef enum {
  KMS_DATA_CHANNEL_PPID_CONTROL = 50,
  KMS_DATA_CHANNEL_PPID_STRING = 51,
  KMS_DATA_CHANNEL_PPID_BINARY_PARTIAL = 52, /* Deprecated */
  KMS_DATA_CHANNEL_PPID_BINARY = 53,
  KMS_DATA_CHANNEL_PPID_STRING_PARTIAL = 54, /* Deprecated */
  KMS_DATA_CHANNEL_PPID_STRING_EMPTY = 56,
  KMS_DATA_CHANNEL_PPID_BINARY_EMPTY = 57
} KmsDataChannelPPID;

typedef enum {
  KMS_DATA_CHANNEL_PRIORITY_IGNORED = 0,
  KMS_DATA_CHANNEL_PRIORITY_BELOW_NORMAL = 128,
  KMS_DATA_CHANNEL_PRIORITY_NORMAL = 256,
  KMS_DATA_CHANNEL_PRIORITY_HIGH = 512,
  KMS_DATA_CHANNEL_PRIORITY_EXTRA_HIGH = 1024
} KmsDataChannelPriority;

#endif /* __KMS_WEBRTC_DATA_PROTO_H__ */
