/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifndef __KMS_REF_STRUCT_H__
#define __KMS_REF_STRUCT_H__

#include <gst/gst.h>

G_BEGIN_DECLS

typedef struct _KmsRefStruct KmsRefStruct;
#define KMS_REF_STRUCT_CAST(obj) ((KmsRefStruct *) obj)

void kms_ref_struct_init (KmsRefStruct * refstruct, GDestroyNotify notif);
KmsRefStruct * kms_ref_struct_ref (KmsRefStruct * refstruct);
void kms_ref_struct_unref (KmsRefStruct * refstruct);

struct _KmsRefStruct {
  gint _count;
  GDestroyNotify _notif;
};

G_END_DECLS

#endif /* __KMS_REF_STRUCT_H__ */
