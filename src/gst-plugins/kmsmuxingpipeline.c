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

static void
kms_muxing_pipeline_class_init (KmsMuxingPipelineClass * klass)
{
  g_type_class_add_private (klass, sizeof (KmsMuxingPipelinePrivate));
}

static void
kms_muxing_pipeline_init (KmsMuxingPipeline * self)
{
  self->priv = KMS_MUXING_PIPELINE_GET_PRIVATE (self);
}
