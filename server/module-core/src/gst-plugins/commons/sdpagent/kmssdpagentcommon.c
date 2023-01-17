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

#include "kmssdpagentcommon.h"

static void
kms_sdp_agent_common_destroy_sdp_handler (KmsSdpHandler * handler)
{
  g_free (handler->media);
  g_slist_free (handler->groups);
  g_clear_object (&handler->handler);

  g_slice_free (KmsSdpHandler, handler);
}

KmsSdpHandler *
kms_sdp_agent_common_new_sdp_handler (guint id, const gchar * media,
    KmsSdpMediaHandler * handler)
{
  KmsSdpHandler *sdp_handler;

  sdp_handler = g_slice_new0 (KmsSdpHandler);

  kms_ref_struct_init (KMS_REF_STRUCT_CAST (sdp_handler),
      (GDestroyNotify) kms_sdp_agent_common_destroy_sdp_handler);

  sdp_handler->id = id;
  sdp_handler->media = g_strdup (media);
  sdp_handler->handler = handler;

  return sdp_handler;
}

KmsSdpHandler *
kms_sdp_agent_common_ref_sdp_handler (KmsSdpHandler * handler)
{
  return (KmsSdpHandler *) kms_ref_struct_ref (KMS_REF_STRUCT_CAST (handler));
}

void
kms_sdp_agent_common_unref_sdp_handler (KmsSdpHandler * handler)
{
  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (handler));
}
