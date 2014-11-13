/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#include "kmsgenericstructure.h"

GST_DEBUG_CATEGORY_STATIC (kms_generic_structure_debug);
#define GST_CAT_DEFAULT kms_generic_structure_debug

typedef struct _KmsField
{
  gpointer data;
  GDestroyNotify destroy;
} KmsField;

struct _KmsGenericStructure
{
  GstMiniObject obj;

  GHashTable *fields;
};

GType _kms_generic_structure_type = 0;

GST_DEFINE_MINI_OBJECT_TYPE (KmsGenericStructure, kms_generic_structure);

static void
_priv_kms_generic_structure_initialize (void)
{
  _kms_generic_structure_type = kms_generic_structure_get_type ();

  GST_DEBUG_CATEGORY_INIT (kms_generic_structure_debug,
      "genericstructure", 0, "Generic Structure");
}

static void
_kms_generic_structure_free (KmsGenericStructure * data)
{
  g_return_if_fail (data != NULL);

  GST_DEBUG ("free");

  g_hash_table_unref (data->fields);

  g_slice_free (KmsGenericStructure, data);
}

static void
kms_generic_structure_destroy_field (KmsField * field)
{
  if (field->destroy != NULL) {
    field->destroy (field->data);
  }

  g_slice_free (KmsField, field);
}

KmsGenericStructure *
kms_generic_structure_new ()
{
  KmsGenericStructure *self;

  self = g_slice_new0 (KmsGenericStructure);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (self), 0,
      _kms_generic_structure_type, NULL, NULL,
      (GstMiniObjectFreeFunction) _kms_generic_structure_free);

  self->fields = g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      (GDestroyNotify) kms_generic_structure_destroy_field);

  return self;
}

void
kms_generic_structure_add_full (KmsGenericStructure * self, const gchar * name,
    gpointer value, GDestroyNotify notify)
{
  KmsField *field;

  g_return_if_fail (self != NULL);

  field = g_slice_new0 (KmsField);

  field->data = value;
  field->destroy = notify;

  g_hash_table_insert (self->fields, g_strdup (name), field);
}

gpointer
kms_generic_structure_get (KmsGenericStructure * self, const gchar * name)
{
  KmsField *field;

  g_return_val_if_fail (self != NULL, NULL);
  g_return_val_if_fail (name != NULL, NULL);

  field = g_hash_table_lookup (self->fields, name);

  if (field == NULL) {
    return NULL;
  }

  return field->data;
}

static void _priv_kms_generic_structure_initialize (void)
    __attribute__ ((constructor));
