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

#ifndef _KMS_MOVEMENT_DETECTOR_H_
#define _KMS_MOVEMENT_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>
#include <stdio.h>

G_BEGIN_DECLS
#define KMS_TYPE_MOVEMENT_DETECTOR   (kms_movement_detector_get_type())
#define KMS_MOVEMENT_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_MOVEMENT_DETECTOR,KmsMovementDetector))
#define KMS_MOVEMENT_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_MOVEMENT_DETECTOR,KmsMovementDetectorClass))
#define KMS_IS_MOVEMENT_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_MOVEMENT_DETECTOR))
#define KMS_IS_MOVEMENT_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_MOVEMENT_DETECTOR))
typedef struct _KmsMovementDetector KmsMovementDetector;
typedef struct _KmsMovementDetectorClass KmsMovementDetectorClass;

struct _KmsMovementDetector {
  GstVideoFilter parent;
  IplImage* img;
  IplImage* imgOldBW;
};

struct _KmsMovementDetectorClass {
  GstVideoFilterClass base_movementdetector_class;
};

GType kms_movement_detector_get_type (void);

gboolean kms_movement_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
