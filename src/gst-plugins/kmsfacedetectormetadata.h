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

#ifndef _KMS_FACE_DETECTOR_METADATA_H_
#define _KMS_FACE_DETECTOR_METADATA_H_

#include <gst/video/gstvideofilter.h>
#include <opencv/cv.h>

G_BEGIN_DECLS

#define KMS_TYPE_FACE_DETECTOR_METADATA   (kms_face_detector_metadata_get_type())
#define KMS_FACE_DETECTOR_METADATA(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_FACE_DETECTOR_METADATA,KmsFaceDetectorMetadata))
#define KMS_FACE_DETECTOR_METADATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_FACE_DETECTOR_METADATA,KmsFaceDetectorMetadataClass))
#define KMS_IS_FACE_DETECTOR_METADATA(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_FACE_DETECTOR_METADATA))
#define KMS_IS_FACE_DETECTOR_METADATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_FACE_DETECTOR_METADATA))

typedef struct _KmsFaceDetectorMetadata KmsFaceDetectorMetadata;
typedef struct _KmsFaceDetectorMetadataClass KmsFaceDetectorMetadataClass;
typedef struct _KmsFaceDetectorMetadataPrivate KmsFaceDetectorMetadataPrivate;

struct _KmsFaceDetectorMetadata
{
  GstVideoFilter base;

  KmsFaceDetectorMetadataPrivate *priv;
};

struct _KmsFaceDetectorMetadataClass
{
  GstVideoFilterClass base_facedetector_class;
};

GType kms_face_detector_metadata_get_type (void);

gboolean kms_face_detector_metadata_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_FACE_DETECTOR_METADATA_H_ */
