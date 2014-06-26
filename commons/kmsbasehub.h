/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifndef _KMS_BASE_HUB_H_
#define _KMS_BASE_HUB_H_

#include "kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_BASE_HUB kms_base_hub_get_type()
#define KMS_BASE_HUB(obj) (     \
  G_TYPE_CHECK_INSTANCE_CAST(   \
    (obj),                      \
    KMS_TYPE_BASE_HUB,          \
    KmsBaseHub                  \
  )                             \
)
#define KMS_BASE_HUB_CLASS(klass) (     \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_BASE_HUB,                  \
    KmsBaseHubClass                     \
  )                                     \
)
#define KMS_IS_BASE_HUB(obj) (          \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_BASE_HUB                   \
  )                                     \
)
#define KMS_IS_BASE_HUB_CLASS(klass) (          \
  G_TYPE_CHECK_CLASS_TYPE((klass),              \
  KMS_TYPE_BASE_HUB)                            \
)
typedef struct _KmsBaseHub KmsBaseHub;
typedef struct _KmsBaseHubClass KmsBaseHubClass;
typedef struct _KmsBaseHubPrivate KmsBaseHubPrivate;

struct _KmsBaseHub
{
  GstBin parent;

  /*< private > */
  KmsBaseHubPrivate *priv;
};

struct _KmsBaseHubClass
{
  GstBinClass parent_class;

  /* Actions */
  gint (*handle_port) (KmsBaseHub * self, GstElement * mixer_port);
  void (*unhandle_port) (KmsBaseHub * self, gint port_id);

  /* Virtual methods */
  gboolean (*link_video_src) (KmsBaseHub * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_audio_src) (KmsBaseHub * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_video_sink) (KmsBaseHub * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_audio_sink) (KmsBaseHub * mixer, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);

  gboolean (*unlink_video_src) (KmsBaseHub * mixer, gint id);
  gboolean (*unlink_audio_src) (KmsBaseHub * mixer, gint id);
  gboolean (*unlink_video_sink) (KmsBaseHub * mixer, gint id);
  gboolean (*unlink_audio_sink) (KmsBaseHub * mixer, gint id);
};

gboolean kms_base_hub_link_video_src (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_audio_src (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_video_sink (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_audio_sink (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);

gboolean kms_base_hub_unlink_video_src (KmsBaseHub * mixer, gint id);
gboolean kms_base_hub_unlink_audio_src (KmsBaseHub * mixer, gint id);
gboolean kms_base_hub_unlink_video_sink (KmsBaseHub * mixer, gint id);
gboolean kms_base_hub_unlink_audio_sink (KmsBaseHub * mixer, gint id);

GType kms_base_hub_get_type (void);

G_END_DECLS
#endif /* _KMS_BASE_HUB_H_ */
