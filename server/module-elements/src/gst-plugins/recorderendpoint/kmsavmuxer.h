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
#ifndef _KMS_AV_MUXER_H_
#define _KMS_AV_MUXER_H_

#include <gst/gst.h>
#include "kmsbasemediamuxer.h"

G_BEGIN_DECLS
#define KMS_TYPE_AV_MUXER               \
  (kms_av_muxer_get_type())
#define KMS_AV_MUXER_CAST(obj)          \
  ((KmsAVMuxer *)(obj))
#define KMS_AV_MUXER(obj)               \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),    \
  KMS_TYPE_AV_MUXER,KmsAVMuxer))
#define KMS_AV_MUXER_CLASS(klass)        \
  (G_TYPE_CHECK_CLASS_CAST((klass),      \
  KMS_TYPE_AV_MUXER,                     \
  KmsAVMuxerClass))
#define KMS_IS_AV_MUXER(obj)             \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),     \
  KMS_TYPE_AV_MUXER))
#define KMS_IS_AV_MUXER_CLASS(klass)     \
  (G_TYPE_CHECK_CLASS_TYPE((klass),      \
  KMS_TYPE_AV_MUXER))

#define KMS_AV_MUXER_PROFILE "profile"

typedef struct _KmsAVMuxer KmsAVMuxer;
typedef struct _KmsAVMuxerClass KmsAVMuxerClass;
typedef struct _KmsAVMuxerPrivate KmsAVMuxerPrivate;

struct _KmsAVMuxer
{
  KmsBaseMediaMuxer parent;

  /*< private > */
  KmsAVMuxerPrivate *priv;
};

struct _KmsAVMuxerClass
{
  KmsBaseMediaMuxerClass parent_class;
};

GType kms_av_muxer_get_type ();

KmsAVMuxer * kms_av_muxer_new (const char *optname1, ...);

G_END_DECLS
#endif
