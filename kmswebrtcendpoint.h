#ifndef __KMS_WEBRTC_END_POINT_H__
#define __KMS_WEBRTC_END_POINT_H__

#include <gst/gst.h>

G_BEGIN_DECLS
#define KMS_TYPE_WEBRTC_END_POINT \
  (kms_webrtc_end_point_get_type())
#define KMS_WEBRTC_END_POINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_WEBRTC_END_POINT,             \
    KmsWebrtcEndPoint                      \
  )                                        \
)
#define KMS_WEBRTC_END_POINT_CLASS(klass)( \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_WEBRTC_END_POINT,             \
    KmsWebrtcEndPoint                      \
  )                                        \
)
#define KMS_IS_WEBRTC_END_POINT(obj) (     \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_WEBRTC_END_POINT              \
  )                                        \
)
#define KMS_IS_WEBRTC_END_POINT_CLASS(klass)\
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_WEBRTC_END_POINT))

#define KMS_WEBRTC_END_POINT_CAST(obj) ((KmsWebrtcEndPoint*)(obj))
#define KMS_WEBRTC_END_POINT_GET_CLASS(obj)(\
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_WEBRTC_END_POINT,             \
    KmsWebrtcEndPointClass                 \
  )                                        \
)

typedef struct _KmsWebrtcEndPoint KmsWebrtcEndPoint;
typedef struct _KmsWebrtcEndPointClass KmsWebrtcEndPointClass;

struct _KmsWebrtcEndPoint
{
  GstElement parent;
};

struct _KmsWebrtcEndPointClass
{
  GstElementClass parent_class;
};

GType kms_webrtc_end_point_get_type (void);

gboolean kms_webrtc_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_WEBRTC_END_POINT_H__ */

