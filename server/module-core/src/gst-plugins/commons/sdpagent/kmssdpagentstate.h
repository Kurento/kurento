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

#ifndef __KMS_SDP_AGENT_STATE_STATE_H__
#define __KMS_SDP_AGENT_STATE_STATE_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef enum
{
  KMS_SDP_AGENT_STATE_UNNEGOTIATED,
  KMS_SDP_AGENT_STATE_LOCAL_OFFER,
  KMS_SDP_AGENT_STATE_REMOTE_OFFER,
  KMS_SDP_AGENT_STATE_WAIT_NEGO,
  KMS_SDP_AGENT_STATE_NEGOTIATED
} KmsSDPAgentState;

G_END_DECLS

#endif /* __KMS_SDP_AGENT_STATE_STATE_H__ */
