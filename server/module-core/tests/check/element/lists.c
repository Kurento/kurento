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
#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

#include "kmslist.h"

GST_START_TEST (list_create)
{
  KmsList *list;

  list = kms_list_new_full (g_str_equal, g_free, g_free);

  fail_if (list == NULL);
  fail_if (kms_list_length (list) != 0);

  kms_list_unref (list);
}

GST_END_TEST
GST_START_TEST (list_add)
{
  KmsList *list;
  gchar *val;

  list = kms_list_new_full (g_str_equal, g_free, g_free);

  fail_if (list == NULL);
  fail_if (kms_list_length (list) != 0);

  kms_list_append (list, g_strdup ("key1"), g_strdup ("val1"));
  fail_if (kms_list_length (list) != 1);
  kms_list_append (list, g_strdup ("key2"), g_strdup ("val2"));
  fail_if (kms_list_length (list) != 2);
  kms_list_append (list, g_strdup ("key3"), g_strdup ("val3"));
  fail_if (kms_list_length (list) != 3);

  fail_if (!kms_list_contains (list, "key1"));
  fail_if (!kms_list_contains (list, "key2"));
  fail_if (!kms_list_contains (list, "key3"));
  fail_if (kms_list_contains (list, "key5"));

  val = kms_list_lookup (list, "key1");
  fail_if (val == NULL);
  fail_if (!g_str_equal ("val1", val));

  val = kms_list_lookup (list, "key2");
  fail_if (val == NULL);
  fail_if (!g_str_equal ("val2", val));

  val = kms_list_lookup (list, "key2");
  fail_if (val == NULL);
  fail_if (!g_str_equal ("val2", val));

  val = kms_list_lookup (list, "key5");
  fail_if (val != NULL);

  kms_list_unref (list);
}

GST_END_TEST static gboolean
my_int_equal (gconstpointer v1, gconstpointer v2)
{
  return GPOINTER_TO_UINT (v1) == GPOINTER_TO_UINT (v2);
}

GST_START_TEST (list_remove)
{
  gpointer key, value;
  KmsListIter iter;
  KmsList *list;
  guint i;

  list = kms_list_new (my_int_equal);

  fail_if (list == NULL);
  fail_if (kms_list_length (list) != 0);

  for (i = 0; i < 10; i++) {
    kms_list_append (list, GUINT_TO_POINTER (i), GUINT_TO_POINTER (i));
  }

  for (i = 0; i < 10; i++) {
    fail_if (!kms_list_contains (list, GUINT_TO_POINTER (i)));
  }

  fail_if (kms_list_length (list) != 10);

  /* Remove first element */
  kms_list_remove (list, GUINT_TO_POINTER (0));
  fail_if (kms_list_length (list) != 9);
  fail_if (kms_list_contains (list, GUINT_TO_POINTER (0)));

  /* Remove last element */
  kms_list_remove (list, GUINT_TO_POINTER (9));
  fail_if (kms_list_length (list) != 8);
  fail_if (kms_list_contains (list, GUINT_TO_POINTER (9)));

  /* Remove intermediate element */
  kms_list_remove (list, GUINT_TO_POINTER (5));
  fail_if (kms_list_length (list) != 7);
  fail_if (kms_list_contains (list, GUINT_TO_POINTER (5)));

  kms_list_iter_init (&iter, list);
  while (kms_list_iter_next (&iter, &key, &value)) {
    fail_if (GPOINTER_TO_UINT (key) != GPOINTER_TO_UINT (value));
  }

  kms_list_unref (list);
}

GST_END_TEST static Suite *
lists_suite (void)
{
  Suite *s = suite_create ("lists");
  TCase *tc_chain = tcase_create ("utils");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, list_create);
  tcase_add_test (tc_chain, list_add);
  tcase_add_test (tc_chain, list_remove);

  return s;
}

GST_CHECK_MAIN (lists);
