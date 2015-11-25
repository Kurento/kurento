/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
