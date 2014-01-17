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
#ifndef _KMS_CROWD_DETECTOR_H_
#define _KMS_CROWD_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>
#include <opencv/highgui.h>

G_BEGIN_DECLS
#define KMS_TYPE_CROWD_DETECTOR   (kms_crowd_detector_get_type())
#define KMS_CROWD_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_CROWD_DETECTOR,KmsCrowdDetector))
#define KMS_CROWD_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_CROWD_DETECTOR,KmsCrowdDetectorClass))
#define KMS_IS_CROWD_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_CROWD_DETECTOR))
#define KMS_IS_CROWD_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_CROWD_DETECTOR))

typedef struct _KmsCrowdDetector KmsCrowdDetector;
typedef struct _KmsCrowdDetectorClass KmsCrowdDetectorClass;
typedef struct _KmsCrowdDetectorPrivate KmsCrowdDetectorPrivate;

struct _KmsCrowdDetector
{
  GstVideoFilter base_crowddetector;

  /*< private > */
  KmsCrowdDetectorPrivate *priv;
};

struct _KmsCrowdDetectorClass
{
  GstVideoFilterClass base_crowddetector_class;
};

GType kms_crowd_detector_get_type (void);

gboolean kms_crowd_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif