/*
 * GStreamer
 *
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */


#ifndef __GST_DTLS_SRTP_ENC_H__
#define __GST_DTLS_SRTP_ENC_H__

#include <gst/gst.h>
#include <gio/gio.h>

#include <ext/gio/kmsgioenums.h>

#include "gstdtlssrtp.h"

G_BEGIN_DECLS
#define GST_TYPE_DTLS_SRTP_ENC            (gst_dtls_srtp_enc_get_type())
#define GST_DTLS_SRTP_ENC(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_DTLS_SRTP_ENC,GstDtlsSrtpEnc))
#define GST_IS_DTLS_SRTP_ENC(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_DTLS_SRTP_ENC))
#define GST_DTLS_SRTP_ENC_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass) ,GST_TYPE_DTLS_SRTP_ENC,GstDtlsSrtpEncClass))
#define GST_IS_DTLS_SRTP_ENC_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass) ,GST_TYPE_DTLS_SRTP_ENC))
#define GST_DTLS_SRTP_ENC_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj) ,GST_TYPE_DTLS_SRTP_ENC,GstDtlsSrtpEncClass))

typedef struct _GstDtlsSrtpEnc GstDtlsSrtpEnc;
typedef struct _GstDtlsSrtpEncClass GstDtlsSrtpEncClass;

/**
 * GstDtlsSrtpEnc:
 *
 * The adder object structure.
 */
struct _GstDtlsSrtpEnc
{
  /*< private >*/
  GstBin parent;

  GstPad *srcpad;

  GstPad *rtp_sinkpad;
  GstPad *rtcp_sinkpad;

  GstElement *in_funnel;
  GstElement *dtls_enc;
  GstElement *srtp_enc;
  GstElement *out_funnel;

  GstPad *srtpenc_rtpsink;
  GstPad *srtpenc_rtcpsink;

  GTlsConnection *conn;
  gulong status_changed_id;

  gulong rtp_probe_id;
  gulong rtcp_probe_id;

  GstBuffer *key_and_salt;

  GTlsSrtpProfile srtp_profile;

  GstDtlsSrtpProfile profiles;
};

struct _GstDtlsSrtpEncClass
{
  GstBinClass parent_class;
};

GType gst_dtls_srtp_enc_get_type (void);

G_END_DECLS
#endif /* __GST_DTLS_SRTP_ENC_H__ */
