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

#ifndef __KMS_DEC_TREE_BIN_H__
#define __KMS_DEC_TREE_BIN_H__

#include "kmstreebin.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_DEC_TREE_BIN \
  (kms_dec_tree_bin_get_type())
#define KMS_DEC_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_DEC_TREE_BIN,KmsDecTreeBin))
#define KMS_DEC_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_DEC_TREE_BIN,KmsDecTreeBinClass))
#define KMS_IS_DEC_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_DEC_TREE_BIN))
#define KMS_IS_DEC_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_DEC_TREE_BIN))
#define KMS_DEC_TREE_BIN_CAST(obj) ((KmsDecTreeBin*)(obj))

typedef struct _KmsDecTreeBin KmsDecTreeBin;
typedef struct _KmsDecTreeBinClass KmsDecTreeBinClass;

struct _KmsDecTreeBin
{
  KmsTreeBin parent;
};

struct _KmsDecTreeBinClass
{
  KmsTreeBinClass parent_class;
};

GType kms_dec_tree_bin_get_type (void);

KmsDecTreeBin * kms_dec_tree_bin_new (const GstCaps * caps, const GstCaps * raw_aps);

G_END_DECLS
#endif /* __KMS_DEC_TREE_BIN_H__ */
