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

#ifndef _KMS_OPENCV_FILTER_H_
#define _KMS_OPENCV_FILTER_H_

#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS
#define KMS_TYPE_OPENCV_FILTER   (kms_opencv_filter_get_type())
#define KMS_OPENCV_FILTER(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_OPENCV_FILTER,KmsOpenCVFilter))
#define KMS_OPENCV_FILTER_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_OPENCV_FILTER,KmsOpenCVFilterClass))
#define KMS_IS_OPENCV_FILTER(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_OPENCV_FILTER))
#define KMS_IS_OPENCV_FILTER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_OPENCV_FILTER))
typedef struct _KmsOpenCVFilter KmsOpenCVFilter;
typedef struct _KmsOpenCVFilterClass KmsOpenCVFilterClass;
typedef struct _KmsOpenCVFilterPrivate KmsOpenCVFilterPrivate;

struct _KmsOpenCVFilter
{
  GstVideoFilter base;
  KmsOpenCVFilterPrivate *priv;
};

struct _KmsOpenCVFilterClass
{
  GstVideoFilterClass base_opencv_filter_class;
};

GType kms_opencv_filter_get_type (void);

gboolean kms_opencv_filter_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_OPENCV_FILTER_H_ */
