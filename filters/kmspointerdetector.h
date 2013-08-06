#ifndef _KMS_POINTER_DETECTOR_H_
#define _KMS_POINTER_DETECTOR_H_

#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>
#include <opencv/highgui.h>
#include <stdio.h>

G_BEGIN_DECLS
#define KMS_TYPE_POINTER_DETECTOR   (kms_pointer_detector_get_type())
#define KMS_POINTER_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_POINTER_DETECTOR,KmsPointerDetector))
#define KMS_POINTER_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_POINTER_DETECTOR,KmsPointerDetectorClass))
#define KMS_IS_POINTER_DETECTOR(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_POINTER_DETECTOR))
#define KMS_IS_POINTER_DETECTOR_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_POINTER_DETECTOR))
typedef struct _KmsPointerDetector KmsPointerDetector;
typedef struct _KmsPointerDetectorClass KmsPointerDetectorClass;

typedef enum
{
  START,
  CAPTURING_REF_HIST,
  CAPTURING_SECOND_HIST,
  BOTH_HIST_SIMILAR
} KmsPointerDetectorState;

struct _KmsPointerDetector
{
  GstVideoFilter base_pointerdetector;
  IplImage *cvImage, *cvImageAux1;
  CvHistogram *histModel, *histCompare, *histSetUp1, *histSetUp2, *histSetUpRef;
  CvPoint upCornerFinalRect, downCornerFinalRect, upCornerRect1,
      downCornerRect1, upCornerRect2, downCornerRect2, trackingPoint,
      trackingPoint1, trackingPoint2, trackingPoint1Aux, trackingPoint2Aux;
  int iteration;
  CvSize imageSize, trackinRectSize, frameSize;
  int numOfRegions;
  float windowScale, windowScaleRef;
  int histRefCapturesCounter, secondHistCapturesCounter;
  CvScalar colorRect1, colorRect2;
  KmsPointerDetectorState state;
  gboolean show_debug_info;
};

struct _KmsPointerDetectorClass
{
  GstVideoFilterClass base_pointerdetector_class;
};

GType kms_pointer_detector_get_type (void);

gboolean kms_pointer_detector_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
