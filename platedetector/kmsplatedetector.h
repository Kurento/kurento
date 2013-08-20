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
typedef struct _KmsPlateDetector KmsPlateDetector;
typedef struct _KmsPlateDetectorClass KmsPlateDetectorClass;

struct _KmsPlateDetector
{
  GstVideoFilter base_platedetector;

};

struct _KmsPlateDetectorClass
{
  GstVideoFilterClass base_platedetector_class;
};

GType kms_plate_detector_get_type (void);

gboolean kms_plate_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
