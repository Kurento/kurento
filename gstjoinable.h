#ifndef __GST_JOINABLE_H__
#define __GST_JOINABLE_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_JOINABLE \
  (gst_joinable_get_type())
#define GST_JOINABLE(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_JOINABLE,GstJoinable))
#define GST_JOINABLE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_JOINABLE,GstJoinableClass))
#define GST_IS_JOINABLE(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_JOINABLE))
#define GST_IS_JOINABLE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_JOINABLE))
#define GST_JOINABLE_CAST(obj) ((GstJoinable*)(obj))
typedef struct _GstJoinable GstJoinable;
typedef struct _GstJoinableClass GstJoinableClass;

#define GST_JOINABLE_LOCK(elem) \
  (g_rec_mutex_lock (&GST_JOINABLE_CAST ((elem))->mutex))
#define GST_JOINABLE_UNLOCK(elem) \
  (g_rec_mutex_unlock (&GST_JOINABLE_CAST ((elem))->mutex))

struct _GstJoinable
{
  GstBin parent;

  GRecMutex mutex;

  GstElement *audio_agnosticbin;
  GstElement *video_agnosticbin;

  GstElement *audio_valve;
  GstElement *video_valve;
};

struct _GstJoinableClass
{
  GstBinClass parent_class;
};

GType gst_joinable_get_type (void);

G_END_DECLS
#endif /* __GST_JOINABLE_H__ */
