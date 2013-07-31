#ifndef _KMS_URI_END_POINT_H_
#define _KMS_URI_END_POINT_H_

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_URI_END_POINT \
  (kms_uri_end_point_get_type())
#define KMS_URI_END_POINT(obj) (           \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_URI_END_POINT,                \
    KmsUriEndPoint                         \
  )                                        \
)
#define KMS_URI_END_POINT_CLASS(klass) (   \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_URI_END_POINT,                \
    KmsUriEndPointClass                    \
  )                                        \
)
#define KMS_IS_URI_END_POINT(obj) (        \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_URI_END_POINT                 \
  )                                        \
)
#define KMS_IS_URI_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_URI_END_POINT))
#define KMS_URI_END_POINT_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (              \
    (obj),                                 \
    KMS_TYPE_URI_END_POINT,                \
    KmsUriEndPointClass                    \
  )                                        \
)
typedef struct _KmsUriEndPoint KmsUriEndPoint;
typedef struct _KmsUriEndPointClass KmsUriEndPointClass;
typedef struct _KmsUriEndPointPrivate KmsUriEndPointPrivate;

struct _KmsUriEndPoint
{
  KmsElement parent;

  /*< private > */
  KmsUriEndPointPrivate *priv;

  /*< protected > */
  gchar *uri;
};

struct _KmsUriEndPointClass
{
  KmsElementClass parent_class;

  /*< protected abstract methods > */
  void (*stopped) (KmsUriEndPoint *self);
  void (*started) (KmsUriEndPoint *self);
  void (*paused) (KmsUriEndPoint *self);
};

GType kms_uri_end_point_get_type (void);

gboolean kms_uri_end_point_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
