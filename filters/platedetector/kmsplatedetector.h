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
#ifndef _KMS_PLATE_DETECTOR_H_
#define _KMS_PLATE_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>
#include <opencv/highgui.h>

G_BEGIN_DECLS
#define KMS_TYPE_PLATE_DETECTOR   (kms_plate_detector_get_type())
#define KMS_PLATE_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_PLATE_DETECTOR,KmsPlateDetector))
#define KMS_PLATE_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_PLATE_DETECTOR,KmsPlateDetectorClass))
#define KMS_IS_PLATE_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_PLATE_DETECTOR))
#define KMS_IS_PLATE_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_PLATE_DETECTOR))
typedef struct _KmsPlateDetector KmsPlateDetector;
typedef struct _KmsPlateDetectorClass KmsPlateDetectorClass;

typedef enum
{
  PREPROCESSING_ONE,
  PREPROCESSING_TWO,
  PREPROCESSING_THREE
} KmsPlateDetectorPreprocessingType;

struct _KmsPlateDetector
{
  GstVideoFilter base_platedetector;
  IplImage *cvImage, *edges, *edgesDilatedMask, *characterContoursMask;
  KmsPlateDetectorPreprocessingType preprocessingType;
};

struct _KmsPlateDetectorClass
{
  GstVideoFilterClass base_platedetector_class;
};

GType kms_plate_detector_get_type (void);

gboolean kms_plate_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
