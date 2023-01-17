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

#ifndef __KMS_SDP_AGENT_COMMON_H__
#define __KMS_SDP_AGENT_COMMON_H__

#include <gst/gst.h>
#include "kmssdpmediahandler.h"
#include "../kmsrefstruct.h"

typedef struct _KmsSdpHandler
{
  KmsRefStruct ref;
  guint id;
  gchar *media;
  guint index;
  gboolean negotiated;
  KmsSdpMediaHandler *handler;
  GSList *groups;
} KmsSdpHandler;

KmsSdpHandler * kms_sdp_agent_common_new_sdp_handler (guint id, const gchar * media, KmsSdpMediaHandler * handler);
KmsSdpHandler * kms_sdp_agent_common_ref_sdp_handler (KmsSdpHandler *handler);
void kms_sdp_agent_common_unref_sdp_handler (KmsSdpHandler *handler);

#endif /* __KMS_SDP_AGENT_COMMON_H__ */
