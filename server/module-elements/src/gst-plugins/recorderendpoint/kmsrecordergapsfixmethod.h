/*
 * (C) Copyright 2021 Kurento (http://kurento.org/)
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

#ifndef __KMS_RECORDER_GAPS_FIX_METHOD_H__
#define __KMS_RECORDER_GAPS_FIX_METHOD_H__

#include <glib.h>

G_BEGIN_DECLS

typedef enum
{
  KMS_RECORDER_GAPS_FIX_NONE,
  KMS_RECORDER_GAPS_FIX_GENPTS,
  KMS_RECORDER_GAPS_FIX_FILL_IF_TRANSCODING,
} KmsRecorderGapsFixMethod;

G_END_DECLS

#endif /* __KMS_RECORDER_GAPS_FIX_METHOD_H__ */
