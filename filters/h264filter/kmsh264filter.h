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

#ifndef _KMS_H264_FILTER_H_
#define _KMS_H264_FILTER_H_

#include <gst/base/gstbasetransform.h>

G_BEGIN_DECLS

#define KMS_TYPE_H264_FILTER   (kms_h264_filter_get_type())
#define KMS_H264_FILTER(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_H264_FILTER,KmsH264Filter))
#define KMS_H264_FILTER_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_H264_FILTER,KmsH264FilterClass))
#define KMS_IS_H264_FILTER(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_H264_FILTER))
#define KMS_IS_H264_FILTER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_H264_FILTER))

typedef struct _KmsH264Filter KmsH264Filter;
typedef struct _KmsH264FilterClass KmsH264FilterClass;

struct _KmsH264Filter
{
  GstBaseTransform base;
};

struct _KmsH264FilterClass
{
  GstBaseTransformClass base_class;
};

GType kms_h264_filter_get_type (void);

gboolean kms_h264_filter_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_H264_FILTER_H_ */
