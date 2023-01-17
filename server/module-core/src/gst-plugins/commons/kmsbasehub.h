/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  gboolean (*link_audio_sink) (KmsBaseHub * self, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_video_sink) (KmsBaseHub * self, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_data_sink) (KmsBaseHub * self, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);

  gboolean (*link_audio_src) (KmsBaseHub * self, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_video_src) (KmsBaseHub * self, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);
  gboolean (*link_data_src) (KmsBaseHub * self, gint id,
      GstElement * internal_element, const gchar * pad_name,
      gboolean remove_on_unlink);

  gboolean (*unlink_audio_sink) (KmsBaseHub * self, gint id);
  gboolean (*unlink_video_sink) (KmsBaseHub * self, gint id);
  gboolean (*unlink_data_sink) (KmsBaseHub * self, gint id);

  gboolean (*unlink_audio_src) (KmsBaseHub * self, gint id);
  gboolean (*unlink_video_src) (KmsBaseHub * self, gint id);
  gboolean (*unlink_data_src) (KmsBaseHub * self, gint id);
};

gboolean kms_base_hub_link_audio_sink (KmsBaseHub * self, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_video_sink (KmsBaseHub * self, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_data_sink (KmsBaseHub * self, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);

gboolean kms_base_hub_link_audio_src (KmsBaseHub * self, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_video_src (KmsBaseHub * self, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);
gboolean kms_base_hub_link_data_src (KmsBaseHub * self, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink);

gboolean kms_base_hub_unlink_audio_sink (KmsBaseHub * self, gint id);
gboolean kms_base_hub_unlink_video_sink (KmsBaseHub * self, gint id);
gboolean kms_base_hub_unlink_data_sink (KmsBaseHub * self, gint id);

gboolean kms_base_hub_unlink_audio_src (KmsBaseHub * self, gint id);
gboolean kms_base_hub_unlink_video_src (KmsBaseHub * self, gint id);
gboolean kms_base_hub_unlink_data_src (KmsBaseHub * self, gint id);

GType kms_base_hub_get_type (void);

G_END_DECLS
#endif /* _KMS_BASE_HUB_H_ */
