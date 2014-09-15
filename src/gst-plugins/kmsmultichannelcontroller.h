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

#ifndef __KMS_MULTI_CHANNEL_CONTROLLER_H__
#define __KMS_MULTI_CHANNEL_CONTROLLER_H__

#include <gst/gst.h>
#include "kmsmccp.h"

G_BEGIN_DECLS

typedef struct _KmsMultiChannelController KmsMultiChannelController;

#define KMS_TYPE_MULTI_CHANNEL_CONTROLLER	(kms_multi_channel_controller_get_type())
#define KMS_IS_MULTI_CHANNEL_CONTROLLER(obj)	(GST_IS_MINI_OBJECT_TYPE (obj, KMS_TYPE_MULTI_CHANNEL_CONTROLLER))
#define KMS_MULTI_CHANNEL_CONTROLLER_CAST(obj)	((KmsMultiChannelController*)(obj))
#define KMS_MULTI_CHANNEL_CONTROLLER(obj)	(KMS_MULTI_CHANNEL_CONTROLLER_CAST(obj))

GType kms_multi_channel_controller_get_type (void);

typedef int (*KmsCreateStreamFunction) (StreamType type, guint16 chanid, gpointer user_data);

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsMultiChannelController * kms_multi_channel_controller_ref (KmsMultiChannelController * m);
#endif

static inline KmsMultiChannelController *
kms_multi_channel_controller_ref (KmsMultiChannelController * m)
{
  return KMS_MULTI_CHANNEL_CONTROLLER_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (m)));
}

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC void kms_multi_channel_controller_unref (KmsMultiChannelController * m);
#endif

static inline void
kms_multi_channel_controller_unref (KmsMultiChannelController * m)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (m));
}

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsMultiChannelController * kms_multi_channel_controller_copy (const KmsMultiChannelController * m);
#endif

static inline KmsMultiChannelController *
kms_multi_channel_controller_copy (const KmsMultiChannelController * m)
{
  return KMS_MULTI_CHANNEL_CONTROLLER_CAST (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST (m)));
}

#define kms_multi_channel_controller_is_writable(m) \
  gst_mini_object_is_writable (GST_MINI_OBJECT_CAST (m))

#define kms_multi_channel_controller_make_writable(m) \
  KMS_MULTI_CHANNEL_CONTROLLER_CAST (gst_mini_object_make_writable (GST_MINI_OBJECT_CAST (m)))

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC gboolean kms_multi_channel_controller_replace (KmsMultiChannelController **old_multi_channel_controller, KmsMultiChannelController *new_multi_channel_controller);
#endif

static inline gboolean
kms_multi_channel_controller_replace (KmsMultiChannelController **old_multi_channel_controller,
  KmsMultiChannelController *new_multi_channel_controller)
{
  return gst_mini_object_replace ((GstMiniObject **) old_multi_channel_controller,
    (GstMiniObject *) new_multi_channel_controller);
}

/* application specific */

/**
 * kms_multi_channel_controller_new:
 *
 * Creates a newly allocated multi_channel_controller without any data.
 *
 * MT safe.
 *
 * Returns: (transfer full): the new #KmsMultiChannelController.
 */

KmsMultiChannelController * kms_multi_channel_controller_new (const gchar *host,
  guint16 port);

void kms_multi_channel_controller_set_create_stream_callback (KmsMultiChannelController *
    mcc, KmsCreateStreamFunction func, gpointer user_data, GDestroyNotify notify);

gboolean kms_multi_channel_controller_connect (KmsMultiChannelController *mcc,
  gchar *host, guint16 port, GError **err);

gboolean kms_multi_channel_controller_start (KmsMultiChannelController *mccp);
void kms_multi_channel_controller_stop (KmsMultiChannelController *mcc);

int kms_multi_channel_controller_get_bound_port(KmsMultiChannelController *mcc);

int kms_multi_channel_controller_create_media_stream (KmsMultiChannelController *
    mcc, StreamType type, guint16 chanid, GError ** err);

G_END_DECLS

#endif /* __KMS_MULTI_CHANNEL_CONTROLLER_H__ */