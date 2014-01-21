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
#ifndef _KMS_RECORDER_END_POINT_H_
#define _KMS_RECORDER_END_POINT_H_

#include "kmsuriendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_RECORDER_END_POINT \
  (kms_recorder_end_point_get_type())
#define KMS_RECORDER_END_POINT(obj) (          \
  G_TYPE_CHECK_INSTANCE_CAST(                  \
    (obj),                                     \
    KMS_TYPE_RECORDER_END_POINT,               \
    KmsRecorderEndPoint                        \
  )                                            \
)
#define KMS_RECORDER_END_POINT_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                    \
    (klass),                                   \
    KMS_TYPE_RECORDER_END_POINT,               \
    KmsRecorderEndPointClass                   \
  )                                            \
)
#define KMS_IS_RECORDER_END_POINT(obj) (       \
  G_TYPE_CHECK_INSTANCE_TYPE (                 \
    (obj),                                     \
    KMS_TYPE_RECORDER_END_POINT                \
  )                                            \
)
#define KMS_IS_RECORDER_END_POINT_CLASS(klass) (        \
  G_TYPE_CHECK_CLASS_TYPE((klass),                      \
  KMS_TYPE_RECORDER_END_POINT)                          \
)
typedef struct _KmsRecorderEndPoint KmsRecorderEndPoint;
typedef struct _KmsRecorderEndPointClass KmsRecorderEndPointClass;
typedef struct _KmsRecorderEndPointPrivate KmsRecorderEndPointPrivate;

struct _KmsRecorderEndPoint
{
  KmsUriEndPoint parent;

  /*< private > */
  KmsRecorderEndPointPrivate *priv;
};

struct _KmsRecorderEndPointClass
{
  KmsUriEndPointClass parent_class;
};

GType kms_recorder_end_point_get_type (void);

gboolean kms_recorder_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
