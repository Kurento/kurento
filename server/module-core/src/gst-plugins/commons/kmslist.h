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

#ifndef __KMS_LIST_H__
#define __KMS_LIST_H__

#include <glib.h>

G_BEGIN_DECLS

typedef struct _KmsList KmsList;
#define KMS_LIST_CAST(obj) ((KmsList *) obj)

#define kms_list_new(key_equal_func) \
  kms_list_new_full (key_equal_func, NULL, NULL);

KmsList * kms_list_new_full (GEqualFunc key_equal_func,
                      GDestroyNotify key_destroy_func,
                      GDestroyNotify value_destroy_func);

guint kms_list_length (KmsList *list);

void kms_list_append (KmsList *list, gpointer key, gpointer value);
void kms_list_prepend (KmsList *list, gpointer key, gpointer value);
void kms_list_remove (KmsList *list, gpointer key);

typedef struct _KmsListIter KmsListIter;
void kms_list_iter_init (KmsListIter *iter, KmsList *list);
gboolean kms_list_iter_next (KmsListIter *iter, gpointer *key, gpointer *value);

void kms_list_foreach (KmsList *list, GHFunc func, gpointer user_data);

gboolean kms_list_contains (KmsList *list, gpointer key);
gpointer kms_list_lookup (KmsList *list, gpointer key);

KmsList * kms_list_ref (KmsList *list);
void kms_list_unref (KmsList *list);

struct _KmsListIter
{
  /*< private >*/
  GSList *item;
};

G_END_DECLS

#endif /* __KMS_LIST_H__ */
