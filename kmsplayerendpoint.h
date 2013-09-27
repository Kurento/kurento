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
#ifndef _KMS_PLAYER_END_POINT_H_
#define _KMS_PLAYER_END_POINT_H_

#include "kmsuriendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_PLAYER_END_POINT               \
  (kms_player_end_point_get_type())
#define KMS_PLAYER_END_POINT(obj)               \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),           \
  KMS_TYPE_PLAYER_END_POINT,KmsPlayerEndPoint))
#define KMS_PLAYER_END_POINT_CLASS(klass)       \
  (G_TYPE_CHECK_CLASS_CAST((klass),             \
  KMS_TYPE_PLAYER_END_POINT,                    \
  KmsPlayerEndPointClass))
#define KMS_IS_PLAYER_END_POINT(obj)            \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),            \
  KMS_TYPE_PLAYER_END_POINT))
#define KMS_IS_PLAYER_END_POINT_CLASS(klass)    \
  (G_TYPE_CHECK_CLASS_TYPE((klass),             \
  KMS_TYPE_PLAYER_END_POINT))

typedef struct _KmsPlayerEndPoint KmsPlayerEndPoint;
typedef struct _KmsPlayerEndPointClass KmsPlayerEndPointClass;
typedef struct _KmsPlayerEndPointPrivate KmsPlayerEndPointPrivate;

struct _KmsPlayerEndPoint
{
  KmsUriEndPoint parent;

  /*< private > */
  KmsPlayerEndPointPrivate *priv;
};

struct _KmsPlayerEndPointClass
{
  KmsUriEndPointClass parent_class;

  void (*eos_signal) (KmsPlayerEndPoint * self);
};

GType kms_player_end_point_get_type (void);

gboolean kms_player_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
