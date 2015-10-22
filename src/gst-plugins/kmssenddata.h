#ifndef _KMS_SEND_DATA_H_
#define _KMS_SEND_DATA_H_

#include <gst/gst.h>
#include "commons/kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_SEND_DATA   (kms_send_data_get_type())
#define KMS_SEND_DATA(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_SEND_DATA,KmsSendData))
#define KMS_SEND_DATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_SEND_DATA,KmsSendDataClass))
#define KMS_IS_SEND_DATA(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_SEND_DATA))
#define KMS_IS_SEND_DATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SEND_DATA))
typedef struct _KmsSendData KmsSendData;
typedef struct _KmsSendDataClass KmsSendDataClass;
typedef struct _KmsSendDataPrivate KmsSendDataPrivate;

struct _KmsSendData
{
  KmsElement base;
  KmsSendDataPrivate *priv;
};

struct _KmsSendDataClass
{
  KmsElementClass parent_class;

  void (*configure_bus_watcher) (KmsSendData * self);
};

GType kms_send_data_get_type (void);

gboolean kms_send_data_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_SEND_DATA_H_ */
