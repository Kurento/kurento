/*
 * Copyright 2022 Kurento
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _GST_GSTREAMER_PLUGIN_SAMPLE_H_
#define _GST_GSTREAMER_PLUGIN_SAMPLE_H_

#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS

#define GST_TYPE_GSTREAMER_PLUGIN_SAMPLE                                       \
  (gst_gstreamer_plugin_sample_get_type ())
#define GST_GSTREAMER_PLUGIN_SAMPLE(obj)                                       \
  (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_GSTREAMER_PLUGIN_SAMPLE,        \
      GstGStreamerPluginSample))
#define GST_GSTREAMER_PLUGIN_SAMPLE_CLASS(klass)                               \
  (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_GSTREAMER_PLUGIN_SAMPLE,         \
      GstGStreamerPluginSampleClass))
#define GST_IS_GSTREAMER_PLUGIN_SAMPLE(obj)                                    \
  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_GSTREAMER_PLUGIN_SAMPLE))
#define GST_IS_GSTREAMER_PLUGIN_SAMPLE_CLASS(klass)                            \
  (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_GSTREAMER_PLUGIN_SAMPLE))
typedef struct _GstGStreamerPluginSample GstGStreamerPluginSample;
typedef struct _GstGStreamerPluginSampleClass GstGStreamerPluginSampleClass;
typedef struct _GstGStreamerPluginSamplePrivate GstGStreamerPluginSamplePrivate;

typedef enum {
  GSTREAMER_PLUGIN_SAMPLE_TYPE_EDGES = 0,
  GSTREAMER_PLUGIN_SAMPLE_TYPE_GREY,
} GStreamerPluginSampleType;

struct _GstGStreamerPluginSample {
  GstVideoFilter base;
  GstGStreamerPluginSamplePrivate *priv;
};

struct _GstGStreamerPluginSampleClass {
  GstVideoFilterClass base_gstreamer_plugin_sample_class;
};

GType gst_gstreamer_plugin_sample_get_type (void);

gboolean gst_gstreamer_plugin_sample_plugin_init (GstPlugin *plugin);

G_END_DECLS

#endif /* _GST_GSTREAMER_PLUGIN_SAMPLE_H_ */
