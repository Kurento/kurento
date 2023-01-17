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

#ifndef __KMS_ENC_TREE_BIN_H__
#define __KMS_ENC_TREE_BIN_H__

#include "kmstreebin.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_ENC_TREE_BIN \
  (kms_enc_tree_bin_get_type())
#define KMS_ENC_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_ENC_TREE_BIN,KmsEncTreeBin))
#define KMS_ENC_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_ENC_TREE_BIN,KmsEncTreeBinClass))
#define KMS_IS_ENC_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_ENC_TREE_BIN))
#define KMS_IS_ENC_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_ENC_TREE_BIN))
#define KMS_ENC_TREE_BIN_CAST(obj) ((KmsEncTreeBin*)(obj))

typedef struct _KmsEncTreeBin KmsEncTreeBin;
typedef struct _KmsEncTreeBinClass KmsEncTreeBinClass;
typedef struct _KmsEncTreeBinPrivate KmsEncTreeBinPrivate;

struct _KmsEncTreeBin
{
  KmsTreeBin parent;

  KmsEncTreeBinPrivate *priv;
};

struct _KmsEncTreeBinClass
{
  KmsTreeBinClass parent_class;
};

GType kms_enc_tree_bin_get_type (void);

KmsEncTreeBin * kms_enc_tree_bin_new (const GstCaps * caps, gint target_bitrate, gint min_bitrate, gint max_bitrate, GstStructure *codec_configs);
void kms_enc_tree_bin_set_bitrate_limits (KmsEncTreeBin *self, gint min_bitrate, gint max_bitrate);
gint kms_enc_tree_bin_get_min_bitrate (KmsEncTreeBin *self);
gint kms_enc_tree_bin_get_max_bitrate (KmsEncTreeBin *self);

G_END_DECLS
#endif /* __KMS_ENC_TREE_BIN_H__ */
