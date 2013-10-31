/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifndef _KMS_HTTP_END_POINT_H_
#define _KMS_HTTP_END_POINT_H

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_HTTP_END_POINT \
  (kms_http_end_point_get_type())
#define KMS_HTTP_END_POINT(obj) (          \
  G_TYPE_CHECK_INSTANCE_CAST(              \
    (obj),                                 \
    KMS_TYPE_HTTP_END_POINT,               \
    KmsHttpEndPoint                        \
  )                                        \
)
#define KMS_HTTP_END_POINT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_HTTP_END_POINT,               \
    KmsHttpEndPointClass                   \
  )                                        \
)
#define KMS_IS_HTTP_END_POINT(obj) (       \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_HTTP_END_POINT                \
  )                                        \
)
#define KMS_IS_HTTP_END_POINT_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_TYPE(                   \
    (klass),                                 \
    KMS_TYPE_HTTP_END_POINT                  \
  )                                          \
)
typedef struct _KmsHttpEndPoint KmsHttpEndPoint;
typedef struct _KmsHttpEndPointClass KmsHttpEndPointClass;
typedef struct _KmsHttpEndPointPrivate KmsHttpEndPointPrivate;

struct _KmsHttpEndPoint
{
  KmsElement parent;

  /*< private > */
  KmsHttpEndPointPrivate *priv;
};

struct _KmsHttpEndPointClass
{
  KmsElementClass parent_class;

  /* signals */
  void (*eos_signal) (KmsHttpEndPoint * self);
  GstFlowReturn (*new_sample) (KmsHttpEndPoint *appsink);
  void (*eos_detected_signal) (KmsHttpEndPoint * self);

  /* actions */
  GstFlowReturn (*push_buffer) (KmsHttpEndPoint * self, GstBuffer * buffer);
  GstSample * (*pull_sample) (KmsHttpEndPoint * self);
  GstFlowReturn (*end_of_stream) (KmsHttpEndPoint * self);
};

GType kms_http_end_point_get_type (void);

gboolean kms_http_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_HTTP_END_POINT_H */
