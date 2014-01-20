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
#ifndef _KMS_POINTER_DETECTOR2_H_
#define _KMS_POINTER_DETECTOR2_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>
#include <opencv/highgui.h>
#include <stdio.h>

G_BEGIN_DECLS
#define KMS_TYPE_POINTER_DETECTOR2   (kms_pointer_detector2_get_type())
#define KMS_POINTER_DETECTOR2(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_POINTER_DETECTOR2,KmsPointerDetector2))
#define KMS_POINTER_DETECTOR2_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_POINTER_DETECTOR2,KmsPointerDetector2Class))
#define KMS_IS_POINTER_DETECTOR2(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_POINTER_DETECTOR2))
#define KMS_IS_POINTER_DETECTOR2_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_POINTER_DETECTOR2))
typedef struct _KmsPointerDetector2 KmsPointerDetector2;
typedef struct _KmsPointerDetector2Class KmsPointerDetector2Class;
typedef struct _KmsPointerDetector2Private KmsPointerDetector2Private;

typedef struct _ButtonStruct {
    CvRect cvButtonLayout;
    gchar *id;
    IplImage* inactive_icon;
    IplImage* active_icon;
    gdouble transparency;
} ButtonStruct;

struct _KmsPointerDetector2 {
  GstVideoFilter base_pointerdetector2;
  KmsPointerDetector2Private *priv;
};

struct _KmsPointerDetector2Class {
  GstVideoFilterClass base_pointerdetector2_class;
};

GType kms_pointer_detector2_get_type (void);

gboolean kms_pointer_detector2_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
