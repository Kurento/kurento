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
