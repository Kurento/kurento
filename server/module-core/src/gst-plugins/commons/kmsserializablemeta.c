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

#include "kmsserializablemeta.h"

GType
kms_serializable_meta_api_get_type (void)
{
  static GType initialization_value = 0;
  if (g_once_init_enter (&initialization_value)) {
    static const gchar *tags[] = {NULL};
    GType type = gst_meta_api_type_register ("KmsSerializableMetaAPI", tags);
    g_once_init_leave (&initialization_value, type);
  }
  return initialization_value;
}

static gboolean
kms_serializable_meta_init (GstMeta * meta, gpointer params, GstBuffer * buffer)
{
  KmsSerializableMeta *smeta = (KmsSerializableMeta *) meta;

  smeta->data = NULL;

  return TRUE;
}

static gboolean
kms_serializable_meta_transform (GstBuffer * transbuf, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  KmsSerializableMeta *smeta;

  if (GST_META_TRANSFORM_IS_COPY (type)) {
    GstStructure *copy = NULL;

    smeta = (KmsSerializableMeta *) meta;

    if (smeta->data != NULL) {
      copy = gst_structure_copy (smeta->data);
    }

    GST_DEBUG ("copy serializable metadata");
    kms_buffer_add_serializable_meta (transbuf, copy);
  }

  return TRUE;
}

static void
kms_serializable_meta_free (GstMeta * meta, GstBuffer * buffer)
{
  KmsSerializableMeta *smeta = (KmsSerializableMeta *) meta;

  if (smeta->data != NULL) {
    gst_structure_free (smeta->data);
  }
}

const GstMetaInfo *
kms_serializable_meta_get_info (void)
{
  static GstMetaInfo *initialization_value = 0;
  if (g_once_init_enter (&initialization_value)) {
    GstMetaInfo *meta_info = (GstMetaInfo *)gst_meta_register (
        KMS_SERIALIZABLE_META_API_TYPE, "KmsSerializableMeta",
        sizeof (KmsSerializableMeta), kms_serializable_meta_init,
        kms_serializable_meta_free, kms_serializable_meta_transform);
    g_once_init_leave (&initialization_value, meta_info);
  }
  return initialization_value;
}

gboolean
add_fields_to_structure (GQuark field_id, const GValue * value, gpointer st)
{
  GstStructure *data = GST_STRUCTURE (st);

  gst_structure_id_set_value (data, field_id, value);

  return TRUE;
}

KmsSerializableMeta *
kms_buffer_add_serializable_meta (GstBuffer * buffer, GstStructure * data)
{
  KmsSerializableMeta *meta;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);

  meta = (KmsSerializableMeta *) gst_buffer_get_meta (buffer,
      KMS_SERIALIZABLE_META_API_TYPE);

  if (meta != NULL) {
    gst_structure_foreach (data, add_fields_to_structure, meta->data);
    gst_structure_free (data);

  } else {
    meta = (KmsSerializableMeta *) gst_buffer_add_meta (buffer,
        KMS_SERIALIZABLE_META_INFO, NULL);

    meta->data = data;
  }

  return meta;
}

GstStructure *
kms_serializable_meta_get_metadata (GstBuffer * buffer)
{
  KmsSerializableMeta *meta;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);

  meta = (KmsSerializableMeta *) gst_buffer_get_meta (buffer,
      KMS_SERIALIZABLE_META_API_TYPE);

  if (meta == NULL) {
    return NULL;
  }

  return meta->data;
}
