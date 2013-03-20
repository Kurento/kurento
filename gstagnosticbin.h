
#ifndef __GST_AGNOSTIC_BIN_H__
#define __GST_AGNOSTIC_BIN_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_AGNOSTIC_BIN \
  (gst_agnostic_bin_get_type())
#define GST_AGNOSTIC_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_AGNOSTIC_BIN,GstAgnosticBin))
#define GST_AGNOSTIC_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_AGNOSTIC_BIN,GstAgnosticBinClass))
#define GST_IS_AGNOSTIC_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_AGNOSTIC_BIN))
#define GST_IS_AGNOSTIC_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_AGNOSTIC_BIN))
typedef struct _GstAgnosticBin GstAgnosticBin;
typedef struct _GstAgnosticBinClass GstAgnosticBinClass;

struct _GstAgnosticBin
{
  GstBin parent;

  GstPad *sinkpad;
  GstCaps *sink_caps;
};

struct _GstAgnosticBinClass
{
  GstBinClass parent_class;
};

GType gst_agnostic_bin_get_type (void);

gboolean gst_agnostic_bin_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __GST_AGNOSTIC_BIN_H__ */
