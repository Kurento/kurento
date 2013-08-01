#ifndef __KMS_ELEMENT_H__
#define __KMS_ELEMENT_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_ELEMENT \
  (kms_element_get_type())
#define KMS_ELEMENT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_ELEMENT,KmsElement))
#define KMS_ELEMENT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_ELEMENT,KmsElementClass))
#define KMS_IS_ELEMENT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_ELEMENT))
#define KMS_IS_ELEMENT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_ELEMENT))
#define KMS_ELEMENT_CAST(obj) ((KmsElement*)(obj))
typedef struct _KmsElement KmsElement;
typedef struct _KmsElementClass KmsElementClass;
typedef struct _KmsElementPrivate KmsElementPrivate;

#define KMS_ELEMENT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_ELEMENT_CAST ((elem))->mutex))
#define KMS_ELEMENT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_ELEMENT_CAST ((elem))->mutex))

struct _KmsElement
{
  GstBin parent;

  GRecMutex mutex;

  GstElement *audio_valve;
  GstElement *video_valve;

  /*< private > */
  KmsElementPrivate *priv;
};

struct _KmsElementClass
{
  GstBinClass parent_class;
};

GType kms_element_get_type (void);

/* Private methods */
GstElement * kms_element_get_audio_agnosticbin (KmsElement * self);
GstElement * kms_element_get_video_agnosticbin (KmsElement * self);

G_END_DECLS
#endif /* __KMS_ELEMENT_H__ */
