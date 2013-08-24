#ifndef __KMS_BASE_SDP_END_POINT_H__
#define __KMS_BASE_SDP_END_POINT_H__

#include <gst/gst.h>
#include <gst/sdp/gstsdpmessage.h>
#include <kmselement.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_BASE_SDP_END_POINT \
  (kms_base_sdp_end_point_get_type())
#define KMS_BASE_SDP_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BASE_SDP_END_POINT,KmsBaseSdpEndPoint))
#define KMS_BASE_SDP_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BASE_SDP_END_POINT,KmsBaseSdpEndPointClass))
#define KMS_IS_BASE_SDP_END_POINT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BASE_SDP_END_POINT))
#define KMS_IS_BASE_SDP_END_POINT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BASE_SDP_END_POINT))
#define KMS_BASE_SDP_END_POINT_CAST(obj) ((KmsBaseSdpEndPoint*)(obj))
typedef struct _KmsBaseSdpEndPoint KmsBaseSdpEndPoint;
typedef struct _KmsBaseSdpEndPointClass KmsBaseSdpEndPointClass;

#define KMS_BASE_SDP_END_POINT_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_SDP_END_POINT_CAST ((elem))->media_mutex))
#define KMS_BASE_SDP_END_POINT_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_SDP_END_POINT_CAST ((elem))->media_mutex))

struct _KmsBaseSdpEndPoint
{
  KmsElement parent;

  /* private */
  GstSDPMessage *pattern_sdp;

  GstSDPMessage *local_offer_sdp;
  GstSDPMessage *local_answer_sdp;

  GstSDPMessage *remote_offer_sdp;
  GstSDPMessage *remote_answer_sdp;

  gboolean use_ipv6;
};

struct _KmsBaseSdpEndPointClass
{
  KmsElementClass parent_class;

  /* private */
  /* actions */
  GstSDPMessage *(*generate_offer) (KmsBaseSdpEndPoint * base_stream);
  GstSDPMessage *(*process_offer) (KmsBaseSdpEndPoint * base_stream,
      GstSDPMessage * offer);
  void (*process_answer) (KmsBaseSdpEndPoint * base_stream,
      GstSDPMessage * answer);
  /* virtual methods */
    gboolean (*set_transport_to_sdp) (KmsBaseSdpEndPoint * base_stream,
      GstSDPMessage * msg);
  void (*start_transport_send) (KmsBaseSdpEndPoint * base_stream,
      const GstSDPMessage * offer, const GstSDPMessage * answer,
      gboolean local_offer);
  void (*connect_input_elements) (KmsBaseSdpEndPoint * base_stream,
      const GstSDPMessage * answer);
};

GType kms_base_sdp_end_point_get_type (void);

G_END_DECLS
#endif /* __KMS_BASE_SDP_END_POINT_H__ */
