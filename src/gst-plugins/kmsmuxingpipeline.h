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
#ifndef _KMS_MUXING_PIPELINE_H_
#define _KMS_MUXING_PIPELINE_H_

#include <gst/gst.h>

G_BEGIN_DECLS
#define KMS_TYPE_MUXING_PIPELINE               \
  (kms_muxing_pipeline_get_type())
#define KMS_MUXING_PIPELINE(obj)               \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),           \
  KMS_TYPE_MUXING_PIPELINE,KmsMuxingPipeline))
#define KMS_MUXING_PIPELINE_CLASS(klass)        \
  (G_TYPE_CHECK_CLASS_CAST((klass),             \
  KMS_TYPE_MUXING_PIPELINE,                     \
  KmsMuxingPipelineClass))
#define KMS_IS_MUXING_PIPELINE(obj)             \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),            \
  KMS_TYPE_MUXING_PIPELINE))
#define KMS_IS_MUXING_PIPELINE_CLASS(klass)     \
  (G_TYPE_CHECK_CLASS_TYPE((klass),             \
  KMS_TYPE_MUXING_PIPELINE))

#define KMS_MUXING_PIPELINE_VIDEO_APPSRC "video-appsrc"
#define KMS_MUXING_PIPELINE_AUDIO_APPSRC "audio-appsrc"
#define KMS_MUXING_PIPELINE_PROFILE "profile"
#define KMS_MUXING_PIPELINE_SINK "sink"

typedef struct _KmsMuxingPipeline KmsMuxingPipeline;
typedef struct _KmsMuxingPipelineClass KmsMuxingPipelineClass;
typedef struct _KmsMuxingPipelinePrivate KmsMuxingPipelinePrivate;

struct _KmsMuxingPipeline
{
  GObject parent;

  /*< private > */
  KmsMuxingPipelinePrivate *priv;
};

struct _KmsMuxingPipelineClass
{
  GObjectClass parent_class;
};

KmsMuxingPipeline * kms_muxing_pipeline_new (const char *optname1, ...);
GstStateChangeReturn kms_muxing_pipeline_set_state (KmsMuxingPipeline *obj,
  GstState state);
GstState kms_muxing_pipeline_get_state (KmsMuxingPipeline *obj);
GstClock * kms_muxing_pipeline_get_clock (KmsMuxingPipeline *obj);
GstBus * kms_muxing_pipeline_get_bus (KmsMuxingPipeline *obj);
void kms_muxing_pipeline_dot_file (KmsMuxingPipeline *obj);

G_END_DECLS
#endif
