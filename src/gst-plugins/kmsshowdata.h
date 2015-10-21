#ifndef _KMS_SHOW_DATA_H_
#define _KMS_SHOW_DATA_H_

#include <gst/gst.h>
#include "commons/kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_SHOW_DATA   (kms_show_data_get_type())
#define KMS_SHOW_DATA(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_SHOW_DATA,KmsShowData))
#define KMS_SHOW_DATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_SHOW_DATA,KmsShowDataClass))
#define KMS_IS_SHOW_DATA(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_SHOW_DATA))
#define KMS_IS_SHOW_DATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SHOW_DATA))
typedef struct _KmsShowData KmsShowData;
typedef struct _KmsShowDataClass KmsShowDataClass;
typedef struct _KmsShowDataPrivate KmsShowDataPrivate;

struct _KmsShowData
{
  KmsElement base;
  KmsShowDataPrivate *priv;
};

struct _KmsShowDataClass
{
  KmsElementClass parent_class;
};

GType kms_show_data_get_type (void);

gboolean kms_show_data_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_SHOW_DATA_H_ */
