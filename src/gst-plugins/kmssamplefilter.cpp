/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Kurento (http://kurento.org/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * cop ies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/*
 * Example pipelines. The command has to be launched from the source directory.
 * (with a webcam)
 * gst-launch-1.0 --gst-plugin-path=. v4l2src ! videoconvert !
 *                              samplefilter type=1 ! videoconvert ! autovideosink
 * (with a video)
 * gst-launch-1.0 --gst-plugin-path=. uridecodebin uri=<video_uri> ! videoconvert !
 *                              samplefilter type=0 edge-value=125 ! videoconvert !
 *                              autovideosink
 */
#include "kmssamplefilter.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <opencv2/opencv.hpp>

#include <kms-enumtypes.h>

#define PLUGIN_NAME "samplefilter"
#define DEFAULT_FILTER_TYPE (KmsSampleFilterType)0
#define DEFAULT_EDGE_VALUE 125

using namespace cv;

#define KMS_SAMPLE_FILTER_LOCK(sample_filter) \
  (g_rec_mutex_lock (&( (KmsSampleFilter *) sample_filter)->priv->mutex))

#define KMS_SAMPLE_FILTER_UNLOCK(sample_filter) \
  (g_rec_mutex_unlock (&( (KmsSampleFilter *) sample_filter)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_sample_filter_debug_category);
#define GST_CAT_DEFAULT kms_sample_filter_debug_category

#define KMS_SAMPLE_FILTER_GET_PRIVATE(obj) (    \
    G_TYPE_INSTANCE_GET_PRIVATE (               \
        (obj),                                  \
        KMS_TYPE_SAMPLE_FILTER,                 \
        KmsSampleFilterPrivate                  \
                                )               \
                                           )

enum {
  PROP_0,
  PROP_FILTER_TYPE,
  PROP_EDGE_VALUE,
  N_PROPERTIES
};

struct _KmsSampleFilterPrivate {
  Mat *cv_image;
  int edge_value;
  KmsSampleFilterType filter_type;
  GRecMutex mutex;
};

/* pad templates */

#define VIDEO_SRC_CAPS \
  GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
  GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsSampleFilter, kms_sample_filter,
                         GST_TYPE_VIDEO_FILTER,
                         GST_DEBUG_CATEGORY_INIT (kms_sample_filter_debug_category,
                             PLUGIN_NAME, 0,
                             "debug category for sample_filter element") );

static void
kms_sample_filter_set_property (GObject *object, guint property_id,
                                const GValue *value, GParamSpec *pspec)
{
  KmsSampleFilter *sample_filter = KMS_SAMPLE_FILTER (object);

  //Changing values of the properties is a critical region because read/write
  //concurrently could produce race condition. For this reason, the following
  //code is protected with a mutex
  KMS_SAMPLE_FILTER_LOCK (sample_filter);

  switch (property_id) {
  case PROP_FILTER_TYPE:
    sample_filter->priv->filter_type = (KmsSampleFilterType) g_value_get_enum (
                                         value);
    break;

  case PROP_EDGE_VALUE:
    sample_filter->priv->edge_value = g_value_get_int (value);
    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  KMS_SAMPLE_FILTER_UNLOCK (sample_filter);
}

static void
kms_sample_filter_get_property (GObject *object, guint property_id,
                                GValue *value, GParamSpec *pspec)
{
  KmsSampleFilter *sample_filter = KMS_SAMPLE_FILTER (object);

  //Reading values of the properties is a critical region because read/write
  //concurrently could produce race condition. For this reason, the following
  //code is protected with a mutex
  KMS_SAMPLE_FILTER_LOCK (sample_filter);

  switch (property_id) {
  case PROP_FILTER_TYPE:
    g_value_set_enum (value, sample_filter->priv->filter_type);
    break;

  case PROP_EDGE_VALUE:
    g_value_set_int (value, sample_filter->priv->edge_value);
    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  KMS_SAMPLE_FILTER_UNLOCK (sample_filter);
}

static void
kms_sample_filter_display_background (KmsSampleFilter *sample_filter, Mat &mask)
{
  int i, j;
  uchar *img_ptr, *mask_ptr;
  int n_rows_img = sample_filter->priv->cv_image->rows;
  int n_cols_img = sample_filter->priv->cv_image->cols;

  for ( i = 0; i < n_rows_img; ++i) {
    img_ptr = sample_filter->priv->cv_image->ptr<uchar> (i);
    mask_ptr = mask.ptr<uchar> (i);

    for ( j = 0; j < n_cols_img; ++j) {
      img_ptr[j * sample_filter->priv->cv_image->channels()] = mask_ptr[j] ;
      img_ptr[j * sample_filter->priv->cv_image->channels() + 1] = mask_ptr[j];
      img_ptr[j * sample_filter->priv->cv_image->channels() + 2] = mask_ptr[j];
    }
  }

  return;
}

static void
kms_sample_filter_initialize_images (KmsSampleFilter *sample_filter,
                                     GstVideoFrame *frame, GstMapInfo &info)
{
  if (sample_filter->priv->cv_image == NULL) {
     sample_filter->priv->cv_image = new Mat(frame->info.height,
                                             frame->info.width, CV_8UC3, info.data);
  } else if ( (sample_filter->priv->cv_image->cols != frame->info.width)
              || (sample_filter->priv->cv_image->rows != frame->info.height) ) {
    delete sample_filter->priv->cv_image;
    sample_filter->priv->cv_image = new Mat(frame->info.height, frame->info.width,
                                            CV_8UC3, info.data);
  } else {
    sample_filter->priv->cv_image->data = info.data;
  }
}

/**
 * This function contains the image processing.
 */
static GstFlowReturn
kms_sample_filter_transform_frame_ip (GstVideoFilter *filter,
                                      GstVideoFrame *frame)
{
  KmsSampleFilter *sample_filter = KMS_SAMPLE_FILTER (filter);
  GstMapInfo info;
  Mat output_image;
  KmsSampleFilterType filter_type;
  int edge_threshold;

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  // The image size can change in runtime, for this reason it is neccessary
  // to check if the dimensions have changed
  kms_sample_filter_initialize_images (sample_filter, frame, info);

  //Accessing property values has to be protected by a mutex
  KMS_SAMPLE_FILTER_LOCK (sample_filter);
  filter_type = sample_filter->priv->filter_type;
  edge_threshold = sample_filter->priv->edge_value;
  KMS_SAMPLE_FILTER_UNLOCK (sample_filter);

  if (filter_type == KMS_SAMPLE_FILTER_TYPE_EDGES) {
    GST_DEBUG ( "Calculating edges");
    Mat gray_image;
    cvtColor ( (*sample_filter->priv->cv_image), gray_image, COLOR_BGR2GRAY);
    Canny (gray_image, output_image, edge_threshold, 255);
  } else if (filter_type == KMS_SAMPLE_FILTER_TYPE_GREY) {
    GST_DEBUG ( "Calculating black&white image");
    cvtColor ( (*sample_filter->priv->cv_image), output_image, COLOR_BGR2GRAY );
  }

  if (output_image.data != NULL) {
    GST_DEBUG ( "Updating output image");
    //This function copy the processed image to the output image
    kms_sample_filter_display_background (sample_filter, output_image);
  }

  gst_buffer_unmap (frame->buffer, &info);
  return GST_FLOW_OK;
}

/*
 * In dispose(), you are supposed to free all types referenced from this
 * object which might themselves hold a reference to self. Generally,
 * the most simple solution is to unref all members on which you own a
 * reference.
 * dispose() might be called multiple times, so we must guard against
 * calling g_object_unref() on an invalid GObject by setting the member
 * NULL; g_clear_object() does this for us, atomically.
*/
static void
kms_sample_filter_dispose (GObject *object)
{
}

/*
 * The finalize function is called when the object is destroyed.
 */
static void
kms_sample_filter_finalize (GObject *object)
{
  KmsSampleFilter *sample_filter = KMS_SAMPLE_FILTER (object);

  if (sample_filter->priv->cv_image != NULL) {
    delete sample_filter->priv->cv_image;
  }

  g_rec_mutex_clear (&sample_filter->priv->mutex);
}

/*
 * In this function it is possible to initialize the variables.
 * For example, we set edge_value to 125 and the filter type to
 * edge filter. This values can be changed via set_properties
 */
static void
kms_sample_filter_init (KmsSampleFilter *
                        sample_filter)
{
  sample_filter->priv = KMS_SAMPLE_FILTER_GET_PRIVATE (sample_filter);
  sample_filter->priv->edge_value = 125;
  g_rec_mutex_init (&sample_filter->priv->mutex);
}

static void
kms_sample_filter_class_init (KmsSampleFilterClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
                                      gst_pad_template_new ("src", GST_PAD_SRC,
                                          GST_PAD_ALWAYS,
                                          gst_caps_from_string (VIDEO_SRC_CAPS) ) );
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
                                      gst_pad_template_new ("sink", GST_PAD_SINK,
                                          GST_PAD_ALWAYS,
                                          gst_caps_from_string (VIDEO_SINK_CAPS) ) );

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
                                         "sample filter element", "Video/Filter",
                                         "Example of filter with OpenCV operations",
                                         "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_sample_filter_set_property;
  gobject_class->get_property = kms_sample_filter_get_property;
  gobject_class->dispose = kms_sample_filter_dispose;
  gobject_class->finalize = kms_sample_filter_finalize;

  //properties definition
  g_object_class_install_property (gobject_class, PROP_FILTER_TYPE,
                                   g_param_spec_enum ("type", "Type",
                                       "Filter type",
                                       GST_TYPE_SAMPLE_FILTER_TYPE,
                                       DEFAULT_FILTER_TYPE,
                                       (GParamFlags) (G_PARAM_READWRITE |  G_PARAM_STATIC_STRINGS) ) );

  g_object_class_install_property (gobject_class, PROP_EDGE_VALUE,
                                   g_param_spec_int ("edge-value", "edge value",
                                       "Threshold value for edge image", 0, 255,
                                       DEFAULT_EDGE_VALUE, (GParamFlags) G_PARAM_READWRITE) );

  video_filter_class->transform_frame_ip =
    GST_DEBUG_FUNCPTR (kms_sample_filter_transform_frame_ip);

  /* Properties initialization */
  g_type_class_add_private (klass, sizeof (KmsSampleFilterPrivate) );
}

gboolean
kms_sample_filter_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
                               KMS_TYPE_SAMPLE_FILTER);
}
