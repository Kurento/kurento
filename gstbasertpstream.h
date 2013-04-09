#ifndef __GST_BASE_RTP_STREAM_H__
#define __GST_BASE_RTP_STREAM_H__

#include <gst/gst.h>
#include <gstbasestream.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define GST_TYPE_BASE_RTP_STREAM \
  (gst_base_rtp_stream_get_type())
#define GST_BASE_RTP_STREAM(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_BASE_RTP_STREAM,GstBaseRtpStream))
#define GST_BASE_RTP_STREAM_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_BASE_RTP_STREAM,GstBaseRtpStreamClass))
#define GST_IS_BASE_RTP_STREAM(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_BASE_RTP_STREAM))
#define GST_IS_BASE_RTP_STREAM_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_BASE_RTP_STREAM))
#define GST_BASE_RTP_STREAM_CAST(obj) ((GstBaseRtpStream*)(obj))
typedef struct _GstBaseRtpStream GstBaseRtpStream;
typedef struct _GstBaseRtpStreamClass GstBaseRtpStreamClass;

#define GST_BASE_RTP_STREAM_LOCK(elem) \
  (g_rec_mutex_lock (&GST_BASE_RTP_STREAM_CAST ((elem))->media_mutex))
#define GST_BASE_RTP_STREAM_UNLOCK(elem) \
  (g_rec_mutex_unlock (&GST_BASE_RTP_STREAM_CAST ((elem))->media_mutex))

struct _GstBaseRtpStream
{
  GstBaseStream parent;
};

struct _GstBaseRtpStreamClass
{
  GstBaseStreamClass parent_class;
};

GType gst_base_rtp_stream_get_type (void);

G_END_DECLS
#endif /* __GST_BASE_RTP_STREAM_H__ */
