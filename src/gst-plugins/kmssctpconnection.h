/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#ifndef __KMS_SCTP_CONNECTION_H__
#define __KMS_SCTP_CONNECTION_H__

#include <gst/gst.h>

G_BEGIN_DECLS

typedef struct _KmsSCTPConnection KmsSCTPConnection;

#define GST_TYPE_SCTP_CONNECTION	(kms_sctp_connection_get_type())
#define GST_IS_SCTP_CONNECTION(obj)	(GST_IS_MINI_OBJECT_TYPE (obj, GST_TYPE_SCTP_CONNECTION))
#define GST_SCTP_CONNECTION_CAST(obj)	((KmsSCTPConnection*)(obj))
#define GST_SCTP_CONNECTION(obj)	(GST_SCTP_CONNECTION_CAST(obj))

typedef enum _KmsSCTPResult {
  KMS_SCTP_OK,           /* no error */
  KMS_SCTP_EINVAL,       /* invalid arguments were provided to a function */
  GST_RTSP_ERESOLV,      /* a host resolve error occured */
  KMS_SCTP_EOF,          /* end-of-file was reached */
  KMS_SCTP_ERROR         /* some unspecified error occured */
} KmsSCTPResult;

typedef enum {
  KMS_SCTP_DATA_IO_EVENT =          (1 << 0),
  KMS_SCTP_ASSOCIATION_EVENT =      (1 << 1),
  KMS_SCTP_ADDRESS_EVENT =          (1 << 2),
  KMS_SCTP_SEND_FAILURE_EVENT =     (1 << 3),
  KMS_SCTP_PEER_ERROR_EVENT =       (1 << 4),
  KMS_SCTP_SHUTDOWN_EVENT =         (1 << 5),
  KMS_SCTP_PARTIAL_DELIVERY_EVENT = (1 << 6),
  KMS_SCTP_ADAPTATION_LAYER_EVENT = (1 << 7),
  KMS_SCTP_AUTHENTICATION_EVENT =   (1 << 8)
} KmsSCTPEventFlags;

typedef struct _KmsSCTPMessage {
  gchar *buf;
  gsize size;
  gssize used;
} KmsSCTPMessage;

#define INIT_SCTP_MESSAGE(msg, s) ({ \
  msg.buf = g_malloc (s);            \
  msg.size = s;                      \
  msg.used = 0;                      \
})

#define CLEAR_SCTP_MESSAGE(msg) ({ \
  if (msg.buf != NULL)             \
    g_free (msg.buf);              \
})

GType kms_sctp_connection_get_type (void);

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsSCTPConnection * kms_sctp_connection_ref (KmsSCTPConnection * b);
#endif

static inline KmsSCTPConnection *
kms_sctp_connection_ref (KmsSCTPConnection * b)
{
  return GST_SCTP_CONNECTION_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (b)));
}


#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC void kms_sctp_connection_unref (KmsSCTPConnection * b);
#endif

static inline void
kms_sctp_connection_unref (KmsSCTPConnection * b)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (b));
}

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsSCTPConnection * kms_sctp_connection_copy (const KmsSCTPConnection * b);
#endif

static inline KmsSCTPConnection *
kms_sctp_connection_copy (const KmsSCTPConnection * b)
{
  return GST_SCTP_CONNECTION_CAST (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST (b)));
}

#define kms_sctp_connection_is_writable(b) \
  gst_mini_object_is_writable (GST_MINI_OBJECT_CAST (b))

#define kms_sctp_connection_make_writable(b) \
  GST_SCTP_CONNECTION_CAST (gst_mini_object_make_writable (GST_MINI_OBJECT_CAST (b)))

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC gboolean kms_sctp_connection_replace (KmsSCTPConnection **o, KmsSCTPConnection *n);
#endif

static inline gboolean
kms_sctp_connection_replace (KmsSCTPConnection **o, KmsSCTPConnection *n)
{
  return gst_mini_object_replace ((GstMiniObject **) o, (GstMiniObject *) n);
}

KmsSCTPConnection * kms_sctp_connection_new (gchar *host, gint port,
  GCancellable *cancellable, GError **err);

KmsSCTPResult kms_sctp_connection_connect (KmsSCTPConnection *conn,
  GCancellable *cancellable, GError **err);
KmsSCTPResult kms_sctp_connection_bind (KmsSCTPConnection *conn,
  GCancellable *cancellable, GError **err);
KmsSCTPResult kms_sctp_connection_accept (KmsSCTPConnection *conn,
  GCancellable *cancellable, KmsSCTPConnection **client, GError **err);
KmsSCTPResult kms_sctp_connection_receive (KmsSCTPConnection *conn,
  KmsSCTPMessage *message, GCancellable *cancellable, GError **err);
KmsSCTPResult kms_sctp_connection_send (KmsSCTPConnection *conn,
  guint32 stream_id, guint32 timetolive, const KmsSCTPMessage *message,
  GCancellable *cancellable, GError **err);

void kms_sctp_connection_close (KmsSCTPConnection *conn);

gboolean kms_sctp_connection_set_event_subscribe (KmsSCTPConnection * conn,
    KmsSCTPEventFlags events, GError **err);

gboolean kms_sctp_connection_set_init_config (KmsSCTPConnection *conn,
  guint16 num_ostreams, guint16 max_instreams, guint16 max_attempts,
  guint16 max_init_timeo, GError **err);

int kms_sctp_connection_get_bound_port (KmsSCTPConnection *conn);
gchar * kms_sctp_connection_get_remote_address (KmsSCTPConnection *conn);

G_END_DECLS

#endif /* __KMS_SCTP_CONNECTION_H__ */