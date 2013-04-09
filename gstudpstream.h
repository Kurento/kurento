#ifndef __GST_UDP_STREAM_H__
#define __GST_UDP_STREAM_H__

#include <gst/gst.h>
#include <gstbasertpstream.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_UDP_STREAM \
  (gst_udp_stream_get_type())
#define GST_UDP_STREAM(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_UDP_STREAM,GstUdpStream))
#define GST_UDP_STREAM_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_UDP_STREAM,GstUdpStreamClass))
#define GST_IS_UDP_STREAM(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_UDP_STREAM))
#define GST_IS_UDP_STREAM_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_UDP_STREAM))
#define GST_UDP_STREAM_CAST(obj) ((GstUdpStream*)(obj))
typedef struct _GstUdpStream GstUdpStream;
typedef struct _GstUdpStreamClass GstUdpStreamClass;

#define GST_UDP_STREAM_LOCK(elem) \
  (g_rec_mutex_lock (&GST_UDP_STREAM_CAST ((elem))->media_mutex))
#define GST_UDP_STREAM_UNLOCK(elem) \
  (g_rec_mutex_unlock (&GST_UDP_STREAM_CAST ((elem))->media_mutex))

struct _GstUdpStream
{
  GstBaseRtpStream parent;
};

struct _GstUdpStreamClass
{
  GstBaseRtpStreamClass parent_class;
};

GType gst_udp_stream_get_type (void);

gboolean gst_udp_stream_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __GST_UDP_STREAM_H__ */
