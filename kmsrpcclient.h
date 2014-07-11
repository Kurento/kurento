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

#ifndef __KMS_RPC_CLIENT_H__
#define __KMS_RPC_CLIENT_H__

#include <gst/gst.h>
#include <kurento/gstmarshal/marshal.h>

G_BEGIN_DECLS

typedef struct _KmsRPCClient KmsRPCClient;

#define GST_TYPE_RPC_CLIENT	(kms_rpc_client_get_type())
#define GST_IS_RPC_CLIENT(obj)	(GST_IS_MINI_OBJECT_TYPE (obj, GST_TYPE_RPC_CLIENT))
#define GST_RPC_CLIENT_CAST(obj)	((KmsRPCClient*)(obj))
#define GST_RPC_CLIENT(obj)	(GST_RPC_CLIENT_CAST(obj))

GType kms_rpc_client_get_type (void);

typedef void (*KmsEOFFunction) (gpointer);

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsRPCClient * kms_rpc_client_ref (KmsRPCClient * b);
#endif

static inline KmsRPCClient *
kms_rpc_client_ref (KmsRPCClient * b)
{
  return GST_RPC_CLIENT_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (b)));
}


#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC void kms_rpc_client_unref (KmsRPCClient * b);
#endif

static inline void
kms_rpc_client_unref (KmsRPCClient * b)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (b));
}

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsRPCClient * kms_rpc_client_copy (const KmsRPCClient * b);
#endif

static inline KmsRPCClient *
kms_rpc_client_copy (const KmsRPCClient * b)
{
  return GST_RPC_CLIENT_CAST (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST (b)));
}

#define kms_rpc_client_is_writable(b) \
  gst_mini_object_is_writable (GST_MINI_OBJECT_CAST (b))

#define kms_rpc_client_make_writable(b) \
  GST_RPC_CLIENT_CAST (gst_mini_object_make_writable (GST_MINI_OBJECT_CAST (b)))

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC gboolean kms_rpc_client_replace (KmsRPCClient **o, KmsRPCClient *n);
#endif

static inline gboolean
kms_rpc_client_replace (KmsRPCClient **o, KmsRPCClient *n)
{
  return gst_mini_object_replace ((GstMiniObject **) o, (GstMiniObject *) n);
}

KmsRPCClient * kms_rpc_client_new (KurentoMarshalRules rules, gsize max);

gboolean kms_rpc_client_start (KmsRPCClient *rpc, gchar *host, gint port,
  GCancellable *cancellable, GError **err);

void kms_rpc_client_stop (KmsRPCClient *rpc);

void
kms_rpc_client_set_eof_function_full (KmsRPCClient *rpc,
                                 KmsEOFFunction func,
                                 gpointer user_data,
                                 GDestroyNotify notify);

gboolean kms_rpc_client_query (KmsRPCClient *m, GstQuery *query, GstQuery **rsp,
  GError **err);
void kms_rpc_client_event (KmsRPCClient *m, GstEvent *event, GError **err);

G_END_DECLS

#endif /* __KMS_RPC_CLIENT_H__ */