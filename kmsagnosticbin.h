
#ifndef __KMS_AGNOSTIC_BIN_H__
#define __KMS_AGNOSTIC_BIN_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_AGNOSTIC_BIN \
  (kms_agnostic_bin_get_type())
#define KMS_AGNOSTIC_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_AGNOSTIC_BIN,KmsAgnosticBin))
#define KMS_AGNOSTIC_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_AGNOSTIC_BIN,KmsAgnosticBinClass))
#define KMS_IS_AGNOSTIC_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_AGNOSTIC_BIN))
#define KMS_IS_AGNOSTIC_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_AGNOSTIC_BIN))
#define KMS_AGNOSTIC_BIN_CAST(obj) ((KmsAgnosticBin*)(obj))
typedef struct _KmsAgnosticBin KmsAgnosticBin;
typedef struct _KmsAgnosticBinClass KmsAgnosticBinClass;

#define KMS_AGNOSTIC_BIN_LOCK(elem) \
  (GST_PAD_STREAM_LOCK (KMS_AGNOSTIC_BIN_CAST (elem)->sinkpad))
#define KMS_AGNOSTIC_BIN_UNLOCK(elem) \
  (GST_PAD_STREAM_UNLOCK (KMS_AGNOSTIC_BIN_CAST (elem)->sinkpad))

struct _KmsAgnosticBin
{
  GstBin parent;

  GstPad *sinkpad;

  guint pad_count;

  GRecMutex mutex;

  GHashTable *encoded_tees;

  GstCaps *current_caps;
};

struct _KmsAgnosticBinClass
{
  GstBinClass parent_class;
};

GType kms_agnostic_bin_get_type (void);

gboolean kms_agnostic_bin_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_AGNOSTIC_BIN_H__ */
