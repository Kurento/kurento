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

#ifndef __KMS_WEBRTC_DATA_CHANNEL_PRIORITY_H__
#define __KMS_WEBRTC_DATA_CHANNEL_PRIORITY_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef enum
{
  KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_IGNORED,
  KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_BELOW_NORMAL,
  KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_NORMAL,
  KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_HIGH,
  KMS_WEB_RTC_DATA_CHANNEL_PRIORITY_EXTRA_HIGH
} KmsWebRtcDataChannelPriority;

G_END_DECLS

#endif /* __KMS_WEBRTC_DATA_CHANNEL_PRIORITY_H__ */
