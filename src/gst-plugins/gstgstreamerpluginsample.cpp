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

/*
 * Example pipelines. Launch the command directly from the build directory.
 *
 * With a webcam:
 *
 *     gst-launch-1.0 --gst-plugin-path="$PWD/src/gst-plugins" \
 *         v4l2src ! videoconvert \
 *         ! gstreamerpluginsample type=0 edge-value=125 \
 *         ! videoconvert ! autovideosink
 *
 * With an input file:
 *
 *     gst-launch-1.0 --gst-plugin-path="$PWD/src/gst-plugins" \
 *         uridecodebin uri='file:///path/to/video.mp4' ! videoconvert \
 *         ! gstreamerpluginsample type=0 edge-value=125 \
 *         ! videoconvert ! autovideosink
 */

#include "gstgstreamerpluginsample.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <opencv2/core.hpp>    // cv::Mat
#include <opencv2/imgproc.hpp> // cv::cvtColor

//#include <kms-enumtypes.h>

GST_DEBUG_CATEGORY_STATIC (gst_gstreamer_plugin_sample_debug_category);
#define GST_CAT_DEFAULT gst_gstreamer_plugin_sample_debug_category
#define PLUGIN_NAME "gstreamerpluginsample"

#define GST_GSTREAMER_PLUGIN_SAMPLE_GET_PRIVATE(obj)                           \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_GSTREAMER_PLUGIN_SAMPLE,       \
      GstGStreamerPluginSamplePrivate))

/* pad templates */

#define VIDEO_SRC_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")
#define VIDEO_SINK_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (GstGStreamerPluginSample,
    gst_gstreamer_plugin_sample,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (gst_gstreamer_plugin_sample_debug_category,
        PLUGIN_NAME,
        0,
        "debug category for gstreamer_plugin_sample element"));

#define GSTREAMER_PLUGIN_SAMPLE_LOCK(self)                                     \
  (g_rec_mutex_lock (&((GstGStreamerPluginSample *)self)->priv->mutex))

#define GSTREAMER_PLUGIN_SAMPLE_UNLOCK(self)                                   \
  (g_rec_mutex_unlock (&((GstGStreamerPluginSample *)self)->priv->mutex))

#define DEFAULT_FILTER_TYPE GSTREAMER_PLUGIN_SAMPLE_TYPE_EDGES
#define DEFAULT_EDGE_VALUE 125

enum { PROP_0, PROP_FILTER_TYPE, PROP_EDGE_VALUE, N_PROPERTIES };

struct _GstGStreamerPluginSamplePrivate {
  cv::Mat *cv_image;
  int edge_value;
  GStreamerPluginSampleType filter_type;
  GRecMutex mutex;
};

static void
gstreamer_plugin_sample_set_property (GObject *object,
    guint property_id,
    const GValue *value,
    GParamSpec *pspec)
{
  GstGStreamerPluginSample *self = GST_GSTREAMER_PLUGIN_SAMPLE (object);

  // Changing values of the properties is a critical region because read/write
  // concurrently could produce race condition. For this reason, the following
  // code is protected with a mutex.
  GSTREAMER_PLUGIN_SAMPLE_LOCK (self);

  switch (property_id) {
  case PROP_FILTER_TYPE:
    self->priv->filter_type =
        (GStreamerPluginSampleType)g_value_get_int (value);
    break;

  case PROP_EDGE_VALUE:
    self->priv->edge_value = g_value_get_int (value);
    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  GSTREAMER_PLUGIN_SAMPLE_UNLOCK (self);
}

static void
gstreamer_plugin_sample_get_property (GObject *object,
    guint property_id,
    GValue *value,
    GParamSpec *pspec)
{
  GstGStreamerPluginSample *self = GST_GSTREAMER_PLUGIN_SAMPLE (object);

  // Reading values of the properties is a critical region because read/write
  // concurrently could produce race condition. For this reason, the following
  // code is protected with a mutex.
  GSTREAMER_PLUGIN_SAMPLE_LOCK (self);

  switch (property_id) {
  case PROP_FILTER_TYPE:
    g_value_set_int (value, (int)self->priv->filter_type);
    break;

  case PROP_EDGE_VALUE:
    g_value_set_int (value, self->priv->edge_value);
    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  GSTREAMER_PLUGIN_SAMPLE_UNLOCK (self);
}

static void
gstreamer_plugin_sample_display_background (GstGStreamerPluginSample *self,
    cv::Mat &mask)
{
  int i, j;
  uchar *img_ptr, *mask_ptr;
  int n_rows_img = self->priv->cv_image->rows;
  int n_cols_img = self->priv->cv_image->cols;

  for (i = 0; i < n_rows_img; ++i) {
    img_ptr = self->priv->cv_image->ptr<uchar> (i);
    mask_ptr = mask.ptr<uchar> (i);

    for (j = 0; j < n_cols_img; ++j) {
      img_ptr[j * self->priv->cv_image->channels ()] = mask_ptr[j];
      img_ptr[j * self->priv->cv_image->channels () + 1] = mask_ptr[j];
      img_ptr[j * self->priv->cv_image->channels () + 2] = mask_ptr[j];
    }
  }

  return;
}

static void
gstreamer_plugin_sample_initialize_images (GstGStreamerPluginSample *self,
    GstVideoFrame *frame,
    GstMapInfo &info)
{
  if (self->priv->cv_image == NULL) {
    self->priv->cv_image =
        new cv::Mat (frame->info.height, frame->info.width, CV_8UC3, info.data);
  } else if ((self->priv->cv_image->cols != frame->info.width)
      || (self->priv->cv_image->rows != frame->info.height)) {
    delete self->priv->cv_image;
    self->priv->cv_image =
        new cv::Mat (frame->info.height, frame->info.width, CV_8UC3, info.data);
  } else {
    self->priv->cv_image->data = info.data;
  }
}

/**
 * This function contains the image processing.
 */
static GstFlowReturn
gst_gstreamer_plugin_sample_transform_frame_ip (GstVideoFilter *filter,
    GstVideoFrame *frame)
{
  GstGStreamerPluginSample *self = GST_GSTREAMER_PLUGIN_SAMPLE (filter);
  GstMapInfo info;
  cv::Mat output_image;
  GStreamerPluginSampleType filter_type;
  int edge_threshold;

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  // The image size can change in runtime, for this reason it is neccessary
  // to check if the dimensions have changed
  gstreamer_plugin_sample_initialize_images (self, frame, info);

  //Accessing property values has to be protected by a mutex
  GSTREAMER_PLUGIN_SAMPLE_LOCK (self);
  filter_type = self->priv->filter_type;
  edge_threshold = self->priv->edge_value;
  GSTREAMER_PLUGIN_SAMPLE_UNLOCK (self);

  if (filter_type == GSTREAMER_PLUGIN_SAMPLE_TYPE_EDGES) {
    GST_DEBUG ("Calculating edges");
    cv::Mat gray_image;
    cvtColor ((*self->priv->cv_image), gray_image, cv::COLOR_BGR2GRAY);
    cv::Canny (gray_image, output_image, edge_threshold, 255);
  } else if (filter_type == GSTREAMER_PLUGIN_SAMPLE_TYPE_GREY) {
    GST_DEBUG ("Calculating black&white image");
    cv::cvtColor ((*self->priv->cv_image), output_image, cv::COLOR_BGR2GRAY);
  }

  if (output_image.data != NULL) {
    GST_DEBUG ("Updating output image");
    //This function copy the processed image to the output image
    gstreamer_plugin_sample_display_background (self, output_image);
  }

  gst_buffer_unmap (frame->buffer, &info);
  return GST_FLOW_OK;
}

/*
 * dispose() should free all resources referenced from this object,
 * which might themselves hold a reference to self. Generally, the simplest
 * solution is to unref all members.
 *
 * dispose() might be called multiple times, so you must guard against
 * calling g_object_unref() on invalid GObjects by setting the member to
 * NULL; g_clear_object() can be used to do this atomically.
 */
static void
gst_gstreamer_plugin_sample_dispose (GObject *object)
{
}

/*
 * finalize() is called when the object is destroyed.
 */
static void
gst_gstreamer_plugin_sample_finalize (GObject *object)
{
  GstGStreamerPluginSample *self = GST_GSTREAMER_PLUGIN_SAMPLE (object);

  if (self->priv->cv_image != NULL) {
    delete self->priv->cv_image;
  }

  g_rec_mutex_clear (&self->priv->mutex);
}

/*
 * init() should be used to initialize all variables to their default values.
 */
static void
gst_gstreamer_plugin_sample_init (GstGStreamerPluginSample *self)
{
  self->priv = GST_GSTREAMER_PLUGIN_SAMPLE_GET_PRIVATE (self);
  self->priv->edge_value = 125;
  g_rec_mutex_init (&self->priv->mutex);
}

static void
gst_gstreamer_plugin_sample_class_init (GstGStreamerPluginSampleClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "element definition", "Video/Filter", "Filter doc", "Developer");

  gobject_class->set_property = gstreamer_plugin_sample_set_property;
  gobject_class->get_property = gstreamer_plugin_sample_get_property;
  gobject_class->dispose = gst_gstreamer_plugin_sample_dispose;
  gobject_class->finalize = gst_gstreamer_plugin_sample_finalize;

  g_object_class_install_property (gobject_class, PROP_FILTER_TYPE,
      g_param_spec_int ("type", "Filter type",
          "Configures the type of filter. 0: Edges. 1: Black and White.", 0, 1,
          (int)DEFAULT_FILTER_TYPE,
          (GParamFlags)(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS)));

  g_object_class_install_property (gobject_class, PROP_EDGE_VALUE,
      g_param_spec_int ("edge-value", "Edge value",
          "Configures the edge threshold (only for Edges filter type).", 0, 255,
          DEFAULT_EDGE_VALUE,
          (GParamFlags)(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS)));

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (gst_gstreamer_plugin_sample_transform_frame_ip);

  g_type_class_add_private (klass, sizeof (GstGStreamerPluginSamplePrivate));
}

gboolean
gst_gstreamer_plugin_sample_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_GSTREAMER_PLUGIN_SAMPLE);
}
