#ifndef _KMS_RECORDER_END_POINT_H_
#define _KMS_RECORDER_END_POINT_H_

G_BEGIN_DECLS
#define KMS_TYPE_RECORDER_END_POINT   (kms_recorder_end_point_get_type())
#define KMS_RECORDER_END_POINT(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_RECORDER_END_POINT,KmsRecorderEndPoint))
#define KMS_RECORDER_END_POINT_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_RECORDER_END_POINT,KmsRecorderEndPointClass))
#define KMS_IS_RECORDER_END_POINT(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_RECORDER_END_POINT))
#define KMS_IS_RECORDER_END_POINT_CLASS(obj)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_RECORDER_END_POINT))
typedef struct _KmsRecorderEndPoint KmsRecorderEndPoint;
typedef struct _KmsRecorderEndPointClass KmsRecorderEndPointClass;

struct _KmsRecorderEndPoint
{
  KmsElement parent;

};

struct _KmsRecorderEndPointClass
{
  KmsElementClass parent_class;;
};

GType kms_recorder_end_point_get_type (void);

gboolean kms_recorder_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
