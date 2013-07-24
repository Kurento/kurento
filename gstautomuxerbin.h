#ifndef __GST_AUTOMUXER_BIN_H__
#define __GST_AUTOMUXER_BIN_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_AUTOMUXER_BIN \
  (gst_automuxer_bin_get_type())
#define GST_AUTOMUXER_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_AUTOMUXER_BIN,GstAutoMuxerBin))
#define GST_AUTOMUXER_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_AUTOMUXER_BIN,GstAutoMuxerBinClass))
#define GST_IS_AUTOMUXER_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_AUTOMUXER_BIN))
#define GST_IS_AUTOMUXER_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_AUTOMUXER_BIN))
#define GST_AUTOMUXER_BIN_CAST(obj) ((GstAutoMuxerBin*)(obj))
typedef struct _GstAutoMuxerBin GstAutoMuxerBin;
typedef struct _GstAutoMuxerBinClass GstAutoMuxerBinClass;
typedef struct _GstAutoMuxerBinPrivate GstAutoMuxerBinPrivate;

struct _GstAutoMuxerBin
{
  GstBin parent;

  /*< private > */
  GstAutoMuxerBinPrivate *priv;
};

struct _GstAutoMuxerBinClass
{
  GstBinClass parent_class;
};

GType gst_automuxer_bin_get_type (void);

gboolean gst_automuxer_bin_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __GST_AUTOMUXER_BIN_H__ */
