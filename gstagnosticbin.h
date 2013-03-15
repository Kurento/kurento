
#ifndef __GST_AGNOSTICBIN_H__
#define __GST_AGNOSTICBIN_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_AGNOSTICBIN \
  (gst_agnostic_bin_get_type())
#define GST_AGNOSTICBIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_AGNOSTICBIN,GstAgnosticBin))
#define GST_AGNOSTICBIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_AGNOSTICBIN,GstAgnosticBinClass))
#define GST_IS_AGNOSTICBIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_AGNOSTICBIN))
#define GST_IS_AGNOSTICBIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_AGNOSTICBIN))
typedef struct _GstAgnosticBin GstAgnosticBin;
typedef struct _GstAgnosticBinClass GstAgnosticBinClass;

struct _GstAgnosticBin
{
  GstBin parent;

  GstPad *sinkpad, *srcpad;
};

struct _GstAgnosticBinClass
{
  GstBinClass parent_class;
};

GType gst_agnostic_bin_get_type (void);

gboolean gst_agnostic_bin_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __GST_AGNOSTICBIN_H__ */
