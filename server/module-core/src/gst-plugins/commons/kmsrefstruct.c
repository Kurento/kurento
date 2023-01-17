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

#include "kmsrefstruct.h"

void
kms_ref_struct_init (KmsRefStruct * refstruct, GDestroyNotify notif)
{
  g_return_if_fail (refstruct != NULL);

  refstruct->_count = 1;
  refstruct->_notif = notif;
}

KmsRefStruct *
kms_ref_struct_ref (KmsRefStruct * refstruct)
{
  g_return_val_if_fail (refstruct != NULL, NULL);

  g_atomic_int_inc (&refstruct->_count);

  return refstruct;
}

void
kms_ref_struct_unref (KmsRefStruct * refstruct)
{
  g_return_if_fail (refstruct != NULL);

  if (!g_atomic_int_dec_and_test (&refstruct->_count)) {
    return;
  }

  if (refstruct->_notif != NULL) {
    refstruct->_notif (refstruct);
  }
}
