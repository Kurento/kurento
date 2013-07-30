#ifndef _KMS_FILTER_ELEMENT_H_
#define _KMS_FILTER_ELEMENT_H_

G_BEGIN_DECLS
#define KMS_TYPE_FILTER_ELEMENT (kms_filter_element_get_type())
#define KMS_FILTER_ELEMENT(obj) (               \
  G_TYPE_CHECK_INSTANCE_CAST (                  \
    (obj),                                      \
    KMS_TYPE_FILTER_ELEMENT,                    \
    KmsFilterElement                            \
  )                                             \
)
#define KMS_FILTER_ELEMENT_CLASS(klass) (       \
  G_TYPE_CHECK_CLASS_CAST (                     \
    (klass),                                    \
    KMS_TYPE_FILTER_ELEMENT,                    \
    KmsFilterElementClass                       \
  )                                             \
)
#define KMS_IS_FILTER_ELEMENT(obj) (            \
  G_TYPE_CHECK_INSTANCE_TYPE (                  \
    (obj),                                      \
    KMS_TYPE_FILTER_ELEMENT                     \
    )                                           \
)
#define KMS_IS_FILTER_ELEMENT_CLASS(obj) (      \
  G_TYPE_CHECK_CLASS_TYPE (                     \
  (klass),                                      \
  KMS_TYPE_FILTER_ELEMENT                       \
  )                                             \
)
typedef struct _KmsFilterElement KmsFilterElement;
typedef struct _KmsFilterElementClass KmsFilterElementClass;

struct _KmsFilterElement
{
  KmsElement parent;

};

struct _KmsFilterElementClass
{
  KmsElementClass parent_class;
};

GType kms_filter_element_get_type (void);

gboolean kms_filter_element_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif
