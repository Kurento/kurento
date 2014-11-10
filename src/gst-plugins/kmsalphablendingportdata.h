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

#ifndef __KMS_ALPHA_BLENDING_PORT_DATA_H__
#define __KMS_ALPHA_BLENDING_PORT_DATA_H__

#include <gst/gst.h>
#include "kmsalphablending.h"

G_BEGIN_DECLS

typedef struct _KmsAlphaBlendingPortData KmsAlphaBlendingPortData;

struct _KmsAlphaBlendingPortData
{
  GstMiniObject obj;

  KmsAlphaBlending *mixer;
  gint id;
  GstElement *videoconvert;
  GstElement *capsfilter;
  GstElement *videoscale;
  GstElement *videorate;
  GstElement *queue;
  GstPad *video_mixer_pad, *videoconvert_sink_pad;
  gboolean input;
  gboolean configurated;
  gint probe_id, link_probe_id;
  gfloat relative_x, relative_y, relative_width, relative_height;
  gint z_order;
  gboolean removing;
  gboolean eos_managed;
};

#define KMS_TYPE_ALPHA_BLENDING_PORT_DATA	(kms_alpha_blending_port_data_get_type())
#define KMS_IS_ALPHA_BLENDING_PORT_DATA(obj)	(GST_IS_MINI_OBJECT_TYPE (obj, KMS_TYPE_ALPHA_BLENDING_PORT_DATA))
#define KMS_ALPHA_BLENDING_PORT_DATA_CAST(obj)	((KmsAlphaBlendingPortData*)(obj))
#define KMS_ALPHA_BLENDING_PORT_DATA(obj)	(KMS_ALPHA_BLENDING_PORT_DATA_CAST(obj))

GType kms_alpha_blending_port_data_get_type (void);

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsAlphaBlendingPortData * kms_alpha_blending_port_data_ref (KmsAlphaBlendingPortData * b);
#endif

static inline KmsAlphaBlendingPortData *
kms_alpha_blending_port_data_ref (KmsAlphaBlendingPortData * b)
{
  return KMS_ALPHA_BLENDING_PORT_DATA_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (b)));
}

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC void kms_alpha_blending_port_data_unref (KmsAlphaBlendingPortData * b);
#endif

static inline void
kms_alpha_blending_port_data_unref (KmsAlphaBlendingPortData * b)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (b));
}

KmsAlphaBlendingPortData * kms_alpha_blending_port_data_new (KmsAlphaBlending * mixer, gint id);

G_END_DECLS

#endif /* __KMS_ALPHA_BLENDING_PORT_DATA_H__ */
