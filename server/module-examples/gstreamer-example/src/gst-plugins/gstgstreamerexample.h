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

#ifndef _GST_GSTREAMER_EXAMPLE_H_
#define _GST_GSTREAMER_EXAMPLE_H_

#include <gst/video/gstvideofilter.h>

G_BEGIN_DECLS

#define GST_TYPE_GSTREAMER_EXAMPLE (gst_gstreamer_example_get_type ())
#define GST_GSTREAMER_EXAMPLE(obj)                                             \
  (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_GSTREAMER_EXAMPLE,              \
      GstGStreamerExample))
#define GST_GSTREAMER_EXAMPLE_CLASS(klass)                                     \
  (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_GSTREAMER_EXAMPLE,               \
      GstGStreamerExampleClass))
#define GST_IS_GSTREAMER_EXAMPLE(obj)                                          \
  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_GSTREAMER_EXAMPLE))
#define GST_IS_GSTREAMER_EXAMPLE_CLASS(klass)                                  \
  (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_GSTREAMER_EXAMPLE))
typedef struct _GstGStreamerExample GstGStreamerExample;
typedef struct _GstGStreamerExampleClass GstGStreamerExampleClass;
typedef struct _GstGStreamerExamplePrivate GstGStreamerExamplePrivate;

typedef enum {
  GSTREAMER_EXAMPLE_TYPE_EDGES = 0,
  GSTREAMER_EXAMPLE_TYPE_GREY,
} GStreamerExampleType;

struct _GstGStreamerExample {
  GstVideoFilter base;
  GstGStreamerExamplePrivate *priv;
};

struct _GstGStreamerExampleClass {
  GstVideoFilterClass base_gstreamer_example_class;
};

GType gst_gstreamer_example_get_type (void);

gboolean gst_gstreamer_example_plugin_init (GstPlugin *plugin);

G_END_DECLS

#endif /* _GST_GSTREAMER_EXAMPLE_H_ */
