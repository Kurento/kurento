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

#ifndef __KMS_PARSE_TREE_BIN_H__
#define __KMS_PARSE_TREE_BIN_H__

#include "kmstreebin.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_PARSE_TREE_BIN \
  (kms_parse_tree_bin_get_type())
#define KMS_PARSE_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_PARSE_TREE_BIN,KmsParseTreeBin))
#define KMS_PARSE_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_PARSE_TREE_BIN,KmsParseTreeBinClass))
#define KMS_IS_PARSE_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_PARSE_TREE_BIN))
#define KMS_IS_PARSE_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_PARSE_TREE_BIN))
#define KMS_PARSE_TREE_BIN_CAST(obj) ((KmsParseTreeBin*)(obj))

typedef struct _KmsParseTreeBin KmsParseTreeBin;
typedef struct _KmsParseTreeBinClass KmsParseTreeBinClass;
typedef struct _KmsParseTreeBinPrivate KmsParseTreeBinPrivate;

struct _KmsParseTreeBin
{
  KmsTreeBin parent;

  KmsParseTreeBinPrivate *priv;
};

struct _KmsParseTreeBinClass
{
  KmsTreeBinClass parent_class;
};

GType kms_parse_tree_bin_get_type (void);

KmsParseTreeBin * kms_parse_tree_bin_new (const GstCaps * caps);
GstElement * kms_parse_tree_bin_get_parser (KmsParseTreeBin * self);

G_END_DECLS
#endif /* __KMS_PARSE_TREE_BIN_H__ */
