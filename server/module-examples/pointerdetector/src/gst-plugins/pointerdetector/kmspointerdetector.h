/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifndef _KMS_POINTER_DETECTOR_H_
#define _KMS_POINTER_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <opencv2/core/core_c.h>
#include <opencv2/highgui/highgui_c.h>
#include <stdio.h>

G_BEGIN_DECLS
#define KMS_TYPE_POINTER_DETECTOR   (kms_pointer_detector_get_type())
#define KMS_POINTER_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_POINTER_DETECTOR,KmsPointerDetector))
#define KMS_POINTER_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_POINTER_DETECTOR,KmsPointerDetectorClass))
#define KMS_IS_POINTER_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_POINTER_DETECTOR))
#define KMS_IS_POINTER_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_POINTER_DETECTOR))
typedef struct _KmsPointerDetector KmsPointerDetector;
typedef struct _KmsPointerDetectorClass KmsPointerDetectorClass;
typedef struct _KmsPointerDetectorPrivate KmsPointerDetectorPrivate;

typedef struct _ButtonStruct {
    CvRect cvButtonLayout;
    gchar *id;
    cv::Mat inactive_icon;
    cv::Mat active_icon;
    gdouble transparency;
} ButtonStruct;

struct _KmsPointerDetector {
  GstVideoFilter base_pointerdetector;

  KmsPointerDetectorPrivate *priv;
};

struct _KmsPointerDetectorClass {
  GstVideoFilterClass base_pointerdetector_class;

  /* Actions */
  void (*calibrate_color) (KmsPointerDetector *pointerdetector);
};

GType kms_pointer_detector_get_type (void);

gboolean kms_pointer_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
