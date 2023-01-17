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

#include <gst/check/gstcheck.h>
#include <glib.h>

#include "kmsrefstruct.h"

static gboolean is_freed = FALSE;

typedef struct _MyRefStruct
{
  KmsRefStruct ref;
  gint val;
} MyRefStruct;

static void
my_ref_struct_destroy (MyRefStruct * data)
{
  GST_DEBUG ("Free data");

  g_slice_free (MyRefStruct, data);

  /* Destroy function has been executed */
  is_freed = TRUE;
}

GST_START_TEST (check_refs)
{
  MyRefStruct *refs;

  refs = g_slice_new0 (MyRefStruct);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (refs),
      (GDestroyNotify) my_ref_struct_destroy);

  kms_ref_struct_ref (KMS_REF_STRUCT_CAST (refs));
  fail_if (is_freed);
  kms_ref_struct_ref (KMS_REF_STRUCT_CAST (refs));
  fail_if (is_freed);

  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (refs));
  fail_if (is_freed);
  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (refs));
  fail_if (is_freed);

  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (refs));

  /* Data should have been released */
  fail_unless (is_freed);
}

GST_END_TEST
/* Suite initialization */
static Suite *
refcount_suite (void)
{
  Suite *s = suite_create ("common");
  TCase *tc_chain = tcase_create ("refstruct");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, check_refs);

  return s;
}

GST_CHECK_MAIN (refcount);
