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

#ifndef __KMS_RTP_PAY_TREE_BIN_H__
#define __KMS_RTP_PAY_TREE_BIN_H__

#include "kmstreebin.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_RTP_PAY_TREE_BIN \
  (kms_rtp_pay_tree_bin_get_type())
#define KMS_RTP_PAY_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RTP_PAY_TREE_BIN,KmsRtpPayTreeBin))
#define KMS_RTP_PAY_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RTP_PAY_TREE_BIN,KmsRtpPayTreeBinClass))
#define KMS_IS_RTP_PAY_TREE_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RTP_PAY_TREE_BIN))
#define KMS_IS_RTP_PAY_TREE_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RTP_PAY_TREE_BIN))
#define KMS_RTP_PAY_TREE_BIN_CAST(obj) ((KmsRtpPayTreeBin*)(obj))

typedef struct _KmsRtpPayTreeBin KmsRtpPayTreeBin;
typedef struct _KmsRtpPayTreeBinClass KmsRtpPayTreeBinClass;

struct _KmsRtpPayTreeBin
{
  KmsTreeBin parent;
};

struct _KmsRtpPayTreeBinClass
{
  KmsTreeBinClass parent_class;
};

GType kms_rtp_pay_tree_bin_get_type (void);

KmsRtpPayTreeBin * kms_rtp_pay_tree_bin_new (const GstCaps * caps);

G_END_DECLS
#endif /* __KMS_RTP_PAY_TREE_BIN_H__ */
