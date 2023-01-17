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
#ifndef _KMS_KSR_MUXER_H_
#define _KMS_KSR_MUXER_H_

#include <gst/gst.h>
#include "kmsbasemediamuxer.h"

G_BEGIN_DECLS
#define KMS_TYPE_KSR_MUXER               \
  (kms_ksr_muxer_get_type())
#define KMS_KSR_MUXER_CAST(obj)          \
  ((KmsKSRMuxer *)(obj))
#define KMS_KSR_MUXER(obj)               \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),     \
  KMS_TYPE_KSR_MUXER,KmsKSRMuxer))
#define KMS_KSR_MUXER_CLASS(klass)       \
  (G_TYPE_CHECK_CLASS_CAST((klass),      \
  KMS_TYPE_KSR_MUXER,                    \
  KmsKSRMuxerClass))
#define KMS_IS_KSR_MUXER(obj)            \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),     \
  KMS_TYPE_KSR_MUXER))
#define KMS_IS_KSR_MUXER_CLASS(klass)    \
  (G_TYPE_CHECK_CLASS_TYPE((klass),      \
  KMS_TYPE_KSR_MUXER))

#define KMS_KSR_MUXER_PROFILE "profile"

typedef struct _KmsKSRMuxer KmsKSRMuxer;
typedef struct _KmsKSRMuxerClass KmsKSRMuxerClass;
typedef struct _KmsKSRMuxerPrivate KmsKSRMuxerPrivate;

struct _KmsKSRMuxer
{
  KmsBaseMediaMuxer parent;

  /*< private > */
  KmsKSRMuxerPrivate *priv;
};

struct _KmsKSRMuxerClass
{
  KmsBaseMediaMuxerClass parent_class;
};

GType kms_ksr_muxer_get_type ();

KmsKSRMuxer * kms_ksr_muxer_new (const char *optname1, ...);

G_END_DECLS
#endif
