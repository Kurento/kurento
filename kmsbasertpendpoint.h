#ifndef __KMS_BASE_RTP_END_POINT_H__
#define __KMS_BASE_RTP_END_POINT_H__

#include <gst/gst.h>
#include <kmsbasesdpenpoint.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_RTP_END_POINT \
  (kms_base_rtp_end_point_get_type())
#define KMS_BASE_RTP_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_RTP_END_POINT,KmsBaseRtpEndPoint))
#define KMS_BASE_RTP_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_RTP_END_POINT,KmsBaseRtpEndPointClass))
#define KMS_IS_BASE_RTP_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_RTP_END_POINT))
#define KMS_IS_BASE_RTP_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_RTP_END_POINT))
#define KMS_BASE_RTP_END_POINT_CAST(obj) ((KmsBaseRtpEndPoint*)(obj))
typedef struct _KmsBaseRtpEndPoint KmsBaseRtpEndPoint;
typedef struct _KmsBaseRtpEndPointClass KmsBaseRtpEndPointClass;

#define KMS_BASE_RTP_END_POINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_RTP_END_POINT_CAST ((elem))->media_mutex))
#define KMS_BASE_RTP_END_POINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_RTP_END_POINT_CAST ((elem))->media_mutex))

struct _KmsBaseRtpEndPoint
{
  KmsBaseSdpEndPoint parent;

  // TODO: Move this properties to private structure
  GstElement *rtpbin;
  GstElement *audio_payloader;
  GstElement *video_payloader;
};

struct _KmsBaseRtpEndPointClass
{
  KmsBaseSdpEndPointClass parent_class;
};

GType kms_base_rtp_end_point_get_type (void);

G_END_DECLS
#endif /* __KMS_BASE_RTP_END_POINT_H__ */
