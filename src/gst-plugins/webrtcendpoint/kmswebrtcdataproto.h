/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
