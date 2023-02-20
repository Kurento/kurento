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

#include "kmsopencvfilter.hpp"
#include "OpenCVProcess.hpp"

#include <KurentoException.hpp>

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>

#include <opencv2/core.hpp> // Mat

#include <memory>

#define KMS_OPENCV_FILTER_LOCK(self) \
  (g_rec_mutex_lock (&((KmsOpenCVFilter *)(self))->priv->mutex))

#define KMS_OPENCV_FILTER_UNLOCK(self) \
  (g_rec_mutex_unlock (&((KmsOpenCVFilter *)(self))->priv->mutex))

#define PLUGIN_NAME "kmsopencvfilter"
#define GST_CAT_DEFAULT kms_opencv_filter_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_OPENCV_FILTER_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_OPENCV_FILTER, \
      KmsOpenCVFilterPrivate))

enum { PROP_0, PROP_TARGET_OBJECT, N_PROPERTIES };

struct _KmsOpenCVFilterPrivate {
  GRecMutex mutex;
  cv::Mat cv_image;
  kurento::OpenCVProcess *object;
};

/* pad templates */

#define VIDEO_SRC_CAPS GST_VIDEO_CAPS_MAKE ("{ BGRA }")

#define VIDEO_SINK_CAPS GST_VIDEO_CAPS_MAKE ("{ BGRA }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsOpenCVFilter,
    kms_opencv_filter,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT,
        PLUGIN_NAME,
        0,
        "GStreamer debug category for the '" PLUGIN_NAME "' element"));

static void
kms_opencv_filter_set_property (GObject *object,
    guint property_id,
    const GValue *value,
    GParamSpec *pspec)
{
  KmsOpenCVFilter *self = KMS_OPENCV_FILTER (object);

  KMS_OPENCV_FILTER_LOCK (self);

  switch (property_id) {
  case PROP_TARGET_OBJECT:
    try {
      self->priv->object = dynamic_cast<kurento::OpenCVProcess *> (
          (kurento::OpenCVProcess *)g_value_get_pointer (value));
    } catch (std::bad_cast &e) {
      self->priv->object = nullptr;
      GST_ERROR ("Object type not valid");
    }

    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  KMS_OPENCV_FILTER_UNLOCK (self);
}

static void
kms_opencv_filter_get_property (GObject *object,
    guint property_id,
    GValue *value,
    GParamSpec *pspec)
{
  KmsOpenCVFilter *self = KMS_OPENCV_FILTER (object);

  KMS_OPENCV_FILTER_LOCK (self);

  switch (property_id) {
  case PROP_TARGET_OBJECT:
    g_value_set_pointer (value, (gpointer)self->priv->object);
    break;

  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }

  KMS_OPENCV_FILTER_UNLOCK (self);
}

static void
kms_opencv_filter_initialize_images (KmsOpenCVFilter *self,
    GstVideoFrame *frame)
{
  const int width = GST_VIDEO_FRAME_WIDTH (frame);
  const int height = GST_VIDEO_FRAME_HEIGHT (frame);
  const void *data = GST_VIDEO_FRAME_PLANE_DATA (frame, 0);
  const size_t step = GST_VIDEO_FRAME_PLANE_STRIDE (frame, 0);

  self->priv->cv_image =
      cv::Mat (cv::Size (width, height), CV_8UC4, (void *)data, step);
}

static GstFlowReturn
kms_opencv_filter_transform_frame_ip (GstVideoFilter *filter,
    GstVideoFrame *frame)
{
  KmsOpenCVFilter *self = KMS_OPENCV_FILTER (filter);

  if (self->priv->object == nullptr) {
    return GST_FLOW_OK;
  }

  kms_opencv_filter_initialize_images (self, frame);

  try {
    self->priv->object->process (self->priv->cv_image);
  } catch (kurento::KurentoException &e) {
    GstMessage *message;
    GError *err = g_error_new (g_quark_from_string (e.getType ().c_str ()),
        e.getCode (), "%s", GST_ELEMENT_NAME (self));

    message = gst_message_new_error (GST_OBJECT (self), err,
        e.getMessage ().c_str ());

    gst_element_post_message (GST_ELEMENT (self), message);

    g_clear_error (&err);
  } catch (...) {
    GstMessage *message;
    GError *err = g_error_new (g_quark_from_string ("UNDEFINED_EXCEPTION"), 0,
        "%s", GST_ELEMENT_NAME (self));

    message = gst_message_new_error (GST_OBJECT (self), err,
        "Undefined filter error");

    gst_element_post_message (GST_ELEMENT (self), message);

    g_clear_error (&err);
  }

  return GST_FLOW_OK;
}

static void
kms_opencv_filter_dispose (GObject *object)
{
}

static void
kms_opencv_filter_finalize (GObject *object)
{
  KmsOpenCVFilter *self = KMS_OPENCV_FILTER (object);

  if (!self->priv->cv_image.empty ()) {
    self->priv->cv_image.release ();
  }

  g_rec_mutex_clear (&self->priv->mutex);
}

static void
kms_opencv_filter_init (KmsOpenCVFilter *self)
{
  self->priv = KMS_OPENCV_FILTER_GET_PRIVATE (self);
  g_rec_mutex_init (&self->priv->mutex);
}

static void
kms_opencv_filter_class_init (KmsOpenCVFilterClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

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
          "Reference to target object", (GParamFlags)G_PARAM_READWRITE));

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_opencv_filter_transform_frame_ip);

  /* Properties initialization */
  g_type_class_add_private (klass, sizeof (KmsOpenCVFilterPrivate));
}

gboolean
kms_opencv_filter_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_OPENCV_FILTER);
}
