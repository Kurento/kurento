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
#ifndef _KMS_CROWD_DETECTOR_H_
#define _KMS_CROWD_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>

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