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

#include <gst/gst.h>

#include "kmslist.h"
#include "kmsrefstruct.h"

typedef struct _KmsListForeachData
{
  GHFunc func;
  gpointer user_data;
} KmsListForeachData;

struct _KmsList
{
  KmsRefStruct ref;
  GEqualFunc key_equal_func;
  GDestroyNotify key_destroy_func;
  GDestroyNotify value_destroy_func;
  GSList *l;
};

typedef struct _KmsListNode
{
  gpointer key;
  gpointer value;
} KmsListNode;

static void
kms_list_node_destroy (KmsListNode * n)
{
  g_slice_free (KmsListNode, n);
}

static KmsListNode *
kms_list_node_new (gpointer key, gpointer value)
{
  KmsListNode *node;

  node = g_slice_new0 (KmsListNode);
  node->key = key;
  node->value = value;

  return node;
}

static void
kms_list_node_free_data (KmsList * list, KmsListNode * node)
{
  if (list->key_destroy_func != NULL) {
    list->key_destroy_func (node->key);
  }

  if (node->value != NULL && list->value_destroy_func != NULL) {
    list->value_destroy_func (node->value);
  }

  kms_list_node_destroy (node);
}

static void
kms_list_destroy (KmsList * list)
{
  GSList *tmp, *l = list->l;

  while (l != NULL) {
    KmsListNode *node;

    node = l->data;
    tmp = l;
    l = g_slist_next (l);

    kms_list_node_free_data (list, node);
    g_slist_free_1 (tmp);
  }

  g_slice_free (KmsList, list);
}

KmsList *
kms_list_new_full (GEqualFunc key_equal_func, GDestroyNotify key_destroy_func,
    GDestroyNotify value_destroy_func)
{
  KmsList *list;

  list = g_slice_new0 (KmsList);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (list),
      (GDestroyNotify) kms_list_destroy);

  list->key_equal_func = key_equal_func;
  list->key_destroy_func = key_destroy_func;
  list->value_destroy_func = value_destroy_func;

  return list;
}

guint
kms_list_length (KmsList * list)
{
  return g_slist_length (list->l);
}

void
kms_list_append (KmsList * list, gpointer key, gpointer value)
{
  KmsListNode *n;

  n = kms_list_node_new (key, value);
  list->l = g_slist_append (list->l, n);
}

void
kms_list_prepend (KmsList * list, gpointer key, gpointer value)
{
  KmsListNode *n;

  n = kms_list_node_new (key, value);
  list->l = g_slist_prepend (list->l, n);
}

void
kms_list_remove (KmsList * list, gpointer key)
{
  GSList *prev = NULL, *tmp = list->l;

  if (list->key_equal_func == NULL) {
    return;
  }

  while (tmp != NULL) {
    KmsListNode *node;

    node = tmp->data;
    if (list->key_equal_func (node->key, key)) {
      if (prev) {
        prev->next = tmp->next;
      } else {
        list->l = tmp->next;
      }

      kms_list_node_free_data (list, node);
      g_slist_free_1 (tmp);
      break;
    }

    prev = tmp;
    tmp = prev->next;
  }
}

void
kms_list_iter_init (KmsListIter * iter, KmsList * list)
{
  iter->item = list->l;
}

gboolean
kms_list_iter_next (KmsListIter * iter, gpointer * key, gpointer * value)
{
  KmsListNode *n;

  if (iter->item == NULL) {
    return FALSE;
  }

  n = (KmsListNode *) iter->item->data;
  *key = n->key;
  *value = n->value;

  iter->item = g_slist_next (iter->item);

  return TRUE;
}

static void
foreach_cb (gpointer node, gpointer user_data)
{
  KmsListForeachData *data = (KmsListForeachData *) user_data;
  KmsListNode *n = (KmsListNode *) node;

  if (data->func != NULL) {
    data->func (n->key, n->value, data->user_data);
  }
}

void
kms_list_foreach (KmsList * list, GHFunc func, gpointer user_data)
{
  KmsListForeachData tmp;

  tmp.func = func;
  tmp.user_data = user_data;

  g_slist_foreach (list->l, foreach_cb, &tmp);
}

static KmsListNode *
kms_list_get_node (KmsList * list, gpointer key)
{
  GSList *l = NULL;

  if (list->key_equal_func == NULL) {
    return NULL;
  }

  for (l = list->l; l != NULL; l = g_slist_next (l)) {
    KmsListNode *n = l->data;

    if (list->key_equal_func (n->key, key)) {
      return n;
    }
  }

  return NULL;
}

gboolean
kms_list_contains (KmsList * list, gpointer key)
{
  return kms_list_get_node (list, key) != NULL;
}

gpointer
kms_list_lookup (KmsList * list, gpointer key)
{
  KmsListNode *n;

  n = kms_list_get_node (list, key);

  if (n == NULL) {
    return NULL;
  } else {
    return n->value;
  }
}

KmsList *
kms_list_ref (KmsList * list)
{
  return (KmsList *) kms_ref_struct_ref (KMS_REF_STRUCT_CAST (list));
}

void
kms_list_unref (KmsList * list)
{
  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (list));
}
