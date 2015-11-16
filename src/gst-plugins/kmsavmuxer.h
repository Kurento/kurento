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
