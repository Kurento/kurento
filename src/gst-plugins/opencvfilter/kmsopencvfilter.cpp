/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsopencvfilter.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <opencv2/opencv.hpp>
#include <memory>
#include "OpenCVProcess.hpp"
#include <KurentoException.hpp>

#define PLUGIN_NAME "opencvfilter"

using namespace cv;

#define KMS_OPENCV_FILTER_LOCK(opencv_filter) \
  (g_rec_mutex_lock (&( (KmsOpenCVFilter *) (opencv_filter))->priv->mutex))

#define KMS_OPENCV_FILTER_UNLOCK(opencv_filter) \
  (g_rec_mutex_unlock (&( (KmsOpenCVFilter *) (opencv_filter))->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_opencv_filter_debug_category);
#define GST_CAT_DEFAULT kms_opencv_filter_debug_category

#define KMS_OPENCV_FILTER_GET_PRIVATE(obj) (    \
    G_TYPE_INSTANCE_GET_PRIVATE (               \
        (obj),                                  \
        KMS_TYPE_OPENCV_FILTER,                 \
        KmsOpenCVFilterPrivate                  \
                                )               \
                                           )

enum {
  PROP_0,
  PROP_TARGET_OBJECT,
  N_PROPERTIES
};

struct _KmsOpenCVFilterPrivate {
  GRecMutex mutex;
  Mat *cv_image;
  kurento::OpenCVProcess *object;
};

/* pad templates */

#define VIDEO_SRC_CAPS \
  GST_VIDEO_CAPS_MAKE("{ BGRA }")

#define VIDEO_SINK_CAPS \
  GST_VIDEO_CAPS_MAKE("{ BGRA }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsOpenCVFilter, kms_opencv_filter,
                         GST_TYPE_VIDEO_FILTER,
                         GST_DEBUG_CATEGORY_INIT (kms_opencv_filter_debug_category,
                             PLUGIN_NAME, 0,
                             "debug category for opencv_filter element") );

static void
kms_opencv_filter_set_property (GObject *object, guint property_id,
                                const GValue *value, GParamSpec *pspec)
{
  KmsOpenCVFilter *opencv_filter = KMS_OPENCV_FILTER (object);

  KMS_OPENCV_FILTER_LOCK (opencv_filter);

  switch (property_id) {
  case PROP_TARGET_OBJECT:
    try {
      opencv_filter->priv->object = dynamic_cast<kurento::OpenCVProcess *> ( (
                                      kurento::OpenCVProcess *) g_value_get_pointer (value) );
    } catch (std::bad_cast &e) {
      opencv_filter->priv->object = nullptr;
      GST_ERROR ( "Object type not valid");
    }

    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  KMS_OPENCV_FILTER_UNLOCK (opencv_filter);
}

static void
kms_opencv_filter_get_property (GObject *object, guint property_id,
                                GValue *value, GParamSpec *pspec)
{
  KmsOpenCVFilter *opencv_filter = KMS_OPENCV_FILTER (object);

  KMS_OPENCV_FILTER_LOCK (opencv_filter);

  switch (property_id) {
  case PROP_TARGET_OBJECT:
    g_value_set_pointer (value, (gpointer) opencv_filter->priv->object);
    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  KMS_OPENCV_FILTER_UNLOCK (opencv_filter);
}

static void
kms_opencv_filter_initialize_images (KmsOpenCVFilter *opencv_filter,
                                     GstVideoFrame *frame, GstMapInfo &info)
{
  if (opencv_filter->priv->cv_image == nullptr) {

    opencv_filter->priv->cv_image = new Mat (frame->info.height,
        frame->info.width, CV_8UC4, info.data);

  } else if ( (opencv_filter->priv->cv_image->cols != frame->info.width)
              || (opencv_filter->priv->cv_image->rows != frame->info.height) ) {

    delete opencv_filter->priv->cv_image;

    opencv_filter->priv->cv_image = new Mat (frame->info.height,
        frame->info.width, CV_8UC4, info.data);

  } else {
    opencv_filter->priv->cv_image->data = info.data;
  }
}

static GstFlowReturn
kms_opencv_filter_transform_frame_ip (GstVideoFilter *filter,
                                      GstVideoFrame *frame)
{
  KmsOpenCVFilter *opencv_filter = KMS_OPENCV_FILTER (filter);
  GstMapInfo info{};

  if (opencv_filter->priv->object == nullptr) {
    return GST_FLOW_OK;
  }

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  kms_opencv_filter_initialize_images (opencv_filter, frame, info);

  try {
    opencv_filter->priv->object->process (* (opencv_filter->priv->cv_image) );
  } catch (kurento::KurentoException &e) {
    GstMessage *message;
    GError *err = g_error_new (g_quark_from_string (e.getType ().c_str () ),
                               e.getCode (), "%s", GST_ELEMENT_NAME (opencv_filter) );

    message = gst_message_new_error (GST_OBJECT (opencv_filter),
                                     err, e.getMessage ().c_str () );

    gst_element_post_message (GST_ELEMENT (opencv_filter),
                              message);

    g_clear_error (&err);
  } catch (...) {
    GstMessage *message;
    GError *err = g_error_new (g_quark_from_string ("UNDEFINED_EXCEPTION"),
                               0, "%s", GST_ELEMENT_NAME (opencv_filter) );

    message = gst_message_new_error (GST_OBJECT (opencv_filter),
                                     err, "Undefined filter error");

    gst_element_post_message (GST_ELEMENT (opencv_filter),
                              message);

    g_clear_error (&err);
  }

  gst_buffer_unmap (frame->buffer, &info);
  return GST_FLOW_OK;
}

static void
kms_opencv_filter_dispose (GObject *object)
{
}

static void
kms_opencv_filter_finalize (GObject *object)
{
  KmsOpenCVFilter *opencv_filter = KMS_OPENCV_FILTER (object);

  if (opencv_filter->priv->cv_image != nullptr) {
    delete opencv_filter->priv->cv_image;
  }

  g_rec_mutex_clear (&opencv_filter->priv->mutex);
}

static void
kms_opencv_filter_init (KmsOpenCVFilter *
                        opencv_filter)
{
  opencv_filter->priv = KMS_OPENCV_FILTER_GET_PRIVATE (opencv_filter);
  g_rec_mutex_init (&opencv_filter->priv->mutex);
}

static void
kms_opencv_filter_class_init (KmsOpenCVFilterClass *klass)
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
                                         "generic opencv element", "Video/Filter",
                                         "Create a generic opencv filter to process images",
                                         "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_opencv_filter_set_property;
  gobject_class->get_property = kms_opencv_filter_get_property;
  gobject_class->dispose = kms_opencv_filter_dispose;
  gobject_class->finalize = kms_opencv_filter_finalize;

  g_object_class_install_property (gobject_class, PROP_TARGET_OBJECT,
                                   g_param_spec_pointer ("target-object", "target object",
                                       "Reference to target object",
                                       (GParamFlags) G_PARAM_READWRITE) );

  video_filter_class->transform_frame_ip =
    GST_DEBUG_FUNCPTR (kms_opencv_filter_transform_frame_ip);

  /* Properties initialization */
  g_type_class_add_private (klass, sizeof (KmsOpenCVFilterPrivate) );
}

gboolean
kms_opencv_filter_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
                               KMS_TYPE_OPENCV_FILTER);
}
