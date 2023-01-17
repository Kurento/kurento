/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
#ifndef __GST_SDP_DIRECTION_H__
#define __GST_SDP_DIRECTION_H__

G_BEGIN_DECLS

typedef enum GstSDPDirection
{
  GST_SDP_DIRECTION_SENDONLY,
  GST_SDP_DIRECTION_RECVONLY,
  GST_SDP_DIRECTION_SENDRECV,
  GST_SDP_DIRECTION_INACTIVE
} GstSDPDirection;

G_END_DECLS

#endif /* __GST_SDP_DIRECTION_H__ */
