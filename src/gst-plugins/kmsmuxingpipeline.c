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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <commons/kms-core-enumtypes.h>
#include <commons/kmsrecordingprofile.h>

#include "kmsmuxingpipeline.h"

#define OBJECT_NAME "muxingpipeline"

GST_DEBUG_CATEGORY_STATIC (kms_muxing_pipeline_debug_category);
#define GST_CAT_DEFAULT kms_muxing_pipeline_debug_category

#define KMS_MUXING_PIPELINE_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_MUXING_PIPELINE,                   \
    KmsMuxingPipelinePrivate                    \
  )                                             \
)

struct _KmsMuxingPipelinePrivate
{
  GstElement *videosrc;
  GstElement *audiosrc;
  GstElement *encodebin;
  GstElement *sink;
  GstElement *pipeline;
  KmsRecordingProfile profile;
};

G_DEFINE_TYPE_WITH_CODE (KmsMuxingPipeline, kms_muxing_pipeline, G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_muxing_pipeline_debug_category, OBJECT_NAME,
        0, "debug category for muxing pipeline object"));

/* Object properties */
enum
{
  PROP_0,
  PROP_VIDEO_APPSRC,
  PROP_AUDIO_APPSRC,
  PROP_SINK,
  PROP_PROFILE,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

#define KMS_MUXING_PIPELINE_DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

static void
kms_muxing_pipeline_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (object);

  switch (property_id) {
    case PROP_SINK:
      self->priv->sink = gst_object_ref (g_value_get_object (value));
      break;
    case PROP_PROFILE:
      self->priv->profile = g_value_get_enum (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_muxing_pipeline_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (object);

  switch (property_id) {
    case PROP_VIDEO_APPSRC:
      g_value_set_object (value, self->priv->videosrc);
      break;
    case PROP_AUDIO_APPSRC:
      g_value_set_object (value, self->priv->audiosrc);
      break;
    case PROP_SINK:
      g_value_set_object (value, self->priv->sink);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->priv->profile);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_muxing_pipeline_class_init (KmsMuxingPipelineClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->set_property = kms_muxing_pipeline_set_property;
  objclass->get_property = kms_muxing_pipeline_get_property;

  /* Install properties */
  obj_properties[PROP_VIDEO_APPSRC] =
      g_param_spec_object (KMS_MUXING_PIPELINE_VIDEO_APPSRC,
      "Video appsrc element", "Video media input for the pipeline",
      GST_TYPE_ELEMENT, (G_PARAM_READABLE));

  obj_properties[PROP_AUDIO_APPSRC] =
      g_param_spec_object (KMS_MUXING_PIPELINE_AUDIO_APPSRC,
      "Audio appsrc element", "Audio media input for the pipeline",
      GST_TYPE_ELEMENT, (G_PARAM_READABLE));

  obj_properties[PROP_SINK] =
      g_param_spec_object (KMS_MUXING_PIPELINE_SINK, "Sink element",
      "Sink element", GST_TYPE_ELEMENT,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE));

  obj_properties[PROP_PROFILE] = g_param_spec_enum (KMS_MUXING_PIPELINE_PROFILE,
      "Recording profile",
      "The profile used to encapsulate media",
      KMS_TYPE_RECORDING_PROFILE, KMS_MUXING_PIPELINE_DEFAULT_RECORDING_PROFILE,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE));

  g_object_class_install_properties (objclass, N_PROPERTIES, obj_properties);

  g_type_class_add_private (klass, sizeof (KmsMuxingPipelinePrivate));
}

static void
kms_muxing_pipeline_init (KmsMuxingPipeline * self)
{
  self->priv = KMS_MUXING_PIPELINE_GET_PRIVATE (self);
}

KmsMuxingPipeline *
kms_muxing_pipeline_new (const char *optname1, ...)
{
  KmsMuxingPipeline *obj;

  va_list ap;

  va_start (ap, optname1);
  obj = KMS_MUXING_PIPELINE (g_object_new_valist (KMS_TYPE_MUXING_PIPELINE,
          optname1, ap));
  va_end (ap);

  return obj;
}
