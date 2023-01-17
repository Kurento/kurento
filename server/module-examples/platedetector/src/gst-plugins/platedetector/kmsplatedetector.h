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
#ifndef _KMS_PLATE_DETECTOR_H_
#define _KMS_PLATE_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS
#define KMS_TYPE_PLATE_DETECTOR   (kms_plate_detector_get_type())
#define KMS_PLATE_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_PLATE_DETECTOR,KmsPlateDetector))
#define KMS_PLATE_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_PLATE_DETECTOR,KmsPlateDetectorClass))
#define KMS_IS_PLATE_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_PLATE_DETECTOR))
#define KMS_IS_PLATE_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_PLATE_DETECTOR))
#define NUM_PLATE_CHARACTERS ((int)9)
#define NUM_PLATES_SAMPLES ((int) 11)
typedef struct _KmsPlateDetector KmsPlateDetector;
typedef struct _KmsPlateDetectorClass KmsPlateDetectorClass;
typedef struct _KmsPlateDetectorPrivate KmsPlateDetectorPrivate;

typedef enum
{
  PREPROCESSING_ONE,
  PREPROCESSING_TWO,
  PREPROCESSING_THREE
} KmsPlateDetectorPreprocessingType;

struct _KmsPlateDetector
{
  GstVideoFilter base_platedetector;

  /*< private > */
  KmsPlateDetectorPrivate *priv;
};

struct _KmsPlateDetectorClass
{
  GstVideoFilterClass base_platedetector_class;
};

GType kms_plate_detector_get_type (void);

gboolean kms_plate_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
