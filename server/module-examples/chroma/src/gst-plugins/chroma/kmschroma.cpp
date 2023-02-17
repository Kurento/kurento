/*
 * (C) Copyright 2023 Kurento (https://kurento.openvidu.io/)
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
 */
#define _XOPEN_SOURCE 500

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmschroma.hpp"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

#include <gst/video/gstvideofilter.h>
#include <libsoup/soup.h>

#include <opencv2/core.hpp> // Mat
#include <opencv2/imgcodecs.hpp> // imread
#include <opencv2/imgproc.hpp> // cvtColor, rectangle, resize

#include <cstdint>

#define TEMP_PATH "/tmp/XXXXXX"
#define LIMIT_FRAMES 60
#define HISTOGRAM_THRESHOLD (10 * LIMIT_FRAMES)

// Images of type `CV_8UC3` (8 bits depth) when converted to `cv::COLOR_BGR2HSV` (not full range) have these HSV ranges:
// * Hue: [0, 179]
// * Saturation: [0, 255]
// * Value: [0, 255]
// See: https://github.com/opencv/opencv/blob/4.2.0/modules/imgproc/src/color_hsv.simd.hpp#L1158
#define H_MAX 179
#define S_MAX 255
#define H_VALUES (H_MAX + 1)
#define S_VALUES (S_MAX + 1)

// Range of V values used to create masks.
#define V_MASK_MIN 30
#define V_MASK_MAX 255

#define PLUGIN_NAME "chroma"

GST_DEBUG_CATEGORY_STATIC (kms_chroma_debug_category);
#define GST_CAT_DEFAULT kms_chroma_debug_category

#define KMS_CHROMA_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_CHROMA, KmsChromaPrivate))

// Types used to traverse images.
typedef uint8_t Pixel1;
typedef cv::Point3_<uint8_t> Pixel3;

enum { PROP_0, PROP_IMAGE_BACKGROUND, PROP_CALIBRATION_AREA, N_PROPERTIES };

struct _KmsChromaPrivate {
  cv::Mat cvImage, background_image;
  gboolean dir_created, calibration_area;
  gchar *dir, *background_uri;
  gint configure_frames;
  gint x, y, width, height;
  gint h_min, s_min, h_max, s_max;
  gint h_values[H_VALUES];
  gint s_values[S_VALUES];
};

/* pad templates */

#define VIDEO_SRC_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

#define VIDEO_SINK_CAPS GST_VIDEO_CAPS_MAKE ("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsChroma,
    kms_chroma,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_chroma_debug_category,
        PLUGIN_NAME,
        0,
        "debug category for chroma element"));

static gboolean
kms_chroma_is_valid_uri (const gchar *url)
{
  gboolean ret;
  GRegex *regex;

  regex = g_regex_new (
      "^(?:((?:https?):)\\/\\/)([^:\\/\\s]+)(?::(\\d*))?(?:\\/([^\\s?#]+)?([?][^?#]*)?(#.*)?)?$",
      (GRegexCompileFlags)0, (GRegexMatchFlags)0, NULL);
  ret = g_regex_match (regex, url, G_REGEX_MATCH_ANCHORED, NULL);
  g_regex_unref (regex);

  return ret;
}

static void
load_from_url (gchar *file_name, gchar *url)
{
  SoupSession *session;
  SoupMessage *msg;
  FILE *dst;

  session = soup_session_new ();
  msg = soup_message_new ("GET", url);
  soup_session_send_message (session, msg);

  dst = fopen (file_name, "w+");

  if (dst == NULL) {
    GST_ERROR ("It is not possible to create the file");
    goto end;
  }
  fwrite (msg->response_body->data, 1, msg->response_body->length, dst);
  fclose (dst);

end:
  g_object_unref (msg);
  g_object_unref (session);
}

static void
kms_chroma_load_image_to_overlay (KmsChroma *chroma)
{
  cv::Mat image_aux;

  if (chroma->priv->background_uri == NULL) {
    GST_DEBUG ("Unset the background image");

    GST_OBJECT_LOCK (chroma);

    chroma->priv->background_image.release ();

    GST_OBJECT_UNLOCK (chroma);
    return;
  }

  if (!chroma->priv->dir_created) {
    gchar *d = g_strdup (TEMP_PATH);

    chroma->priv->dir = g_mkdtemp (d);
    chroma->priv->dir_created = TRUE;
  }

  GST_INFO ("Trying to load background image: %s",
      chroma->priv->background_uri);

  image_aux = cv::imread (chroma->priv->background_uri, cv::IMREAD_UNCHANGED);

  if (!image_aux.empty ()) {
    GST_INFO ("Background image loaded successfully");
    goto end;
  }

  if (kms_chroma_is_valid_uri (chroma->priv->background_uri)) {
    gchar *file_name = g_strconcat (chroma->priv->dir, "/image.png", NULL);

    GST_INFO ("Downloading background image from URL: %s",
        chroma->priv->background_uri);

    load_from_url (file_name, chroma->priv->background_uri);

    GST_INFO ("Trying to load background image: %s", file_name);

    image_aux = cv::imread (file_name, cv::IMREAD_UNCHANGED);

    g_remove (file_name);
    g_free (file_name);
  }

  if (image_aux.empty ()) {
    GST_ELEMENT_ERROR (chroma, RESOURCE, NOT_FOUND, ("Background not loaded"),
        (NULL));
  } else {
    GST_INFO ("Background image loaded successfully");
  }

end:

  GST_OBJECT_LOCK (chroma);

  if (!image_aux.empty ()) {
    image_aux.copyTo (chroma->priv->background_image);
  }

  GST_OBJECT_UNLOCK (chroma);
}

static void
kms_chroma_set_property (GObject *object,
    guint property_id,
    const GValue *value,
    GParamSpec *pspec)
{
  KmsChroma *chroma = KMS_CHROMA (object);

  switch (property_id) {
  case PROP_IMAGE_BACKGROUND:
    if (chroma->priv->background_uri != NULL)
      g_free (chroma->priv->background_uri);

    chroma->priv->background_uri = g_value_dup_string (value);
    kms_chroma_load_image_to_overlay (chroma);
    break;
  case PROP_CALIBRATION_AREA: {
    GstStructure *aux;

    aux = (GstStructure *)g_value_dup_boxed (value);
    gst_structure_get (aux, "x", G_TYPE_INT, &chroma->priv->x, NULL);
    gst_structure_get (aux, "y", G_TYPE_INT, &chroma->priv->y, NULL);
    gst_structure_get (aux, "width", G_TYPE_INT, &chroma->priv->width, NULL);
    gst_structure_get (aux, "height", G_TYPE_INT, &chroma->priv->height, NULL);

    if (chroma->priv->x < 0)
      chroma->priv->x = 0;

    if (chroma->priv->y < 0)
      chroma->priv->y = 0;

    chroma->priv->calibration_area = TRUE;
    gst_structure_free (aux);
    GST_DEBUG ("Defined calibration area in x %d, y %d,"
               "width %d, height %d",
        chroma->priv->x, chroma->priv->y, chroma->priv->width,
        chroma->priv->height);
    break;
  }
  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }
}

static void
kms_chroma_get_property (GObject *object,
    guint property_id,
    GValue *value,
    GParamSpec *pspec)
{
  KmsChroma *chroma = KMS_CHROMA (object);

  switch (property_id) {
  case PROP_IMAGE_BACKGROUND:
    if (chroma->priv->background_uri == NULL)
      g_value_set_string (value, "");
    else
      g_value_set_string (value, chroma->priv->background_uri);
    break;
  case PROP_CALIBRATION_AREA: {
    GstStructure *aux;

    aux = gst_structure_new ("calibration_area", "x", G_TYPE_INT,
        chroma->priv->x, "y", G_TYPE_INT, chroma->priv->y, "width", G_TYPE_INT,
        chroma->priv->width, "height", G_TYPE_INT, chroma->priv->height, NULL);
    g_value_set_boxed (value, aux);
    gst_structure_free (aux);
    break;
  }
  default:
    G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
    break;
  }
}

static void
kms_chroma_initialize_images (KmsChroma *chroma, GstVideoFrame *frame)
{
  const int width = GST_VIDEO_FRAME_WIDTH (frame);
  const int height = GST_VIDEO_FRAME_HEIGHT (frame);
  const void *data = GST_VIDEO_FRAME_PLANE_DATA (frame, 0);
  const size_t step = GST_VIDEO_FRAME_PLANE_STRIDE (frame, 0);

  chroma->priv->cvImage =
      cv::Mat (cv::Size (width, height), CV_8UC3, (void *)data, step);

  if (!chroma->priv->background_image.empty ()
      && chroma->priv->background_image.size () != cv::Size (width, height)) {
    // Resize the background image.
    cv::resize (chroma->priv->background_image, chroma->priv->background_image,
        cv::Size (width, height));
  }
}

static int
delete_file (const char *fpath,
    const struct stat *sb,
    int typeflag,
    struct FTW *ftwbuf)
{
  int rv = g_remove (fpath);

  if (rv) {
    GST_WARNING ("Error deleting file: %s. %s", fpath, strerror (errno));
  }

  return rv;
}

static void
remove_recursive (const gchar *path)
{
  nftw (path, delete_file, 64, FTW_DEPTH | FTW_PHYS);
}

static void
kms_chroma_add_values (KmsChroma *chroma, const cv::Mat &img_hsv)
{
  cv::Size size = img_hsv.size ();

  // Traverse as 1D vector if data is a continuous memory block.
  if (img_hsv.isContinuous ()) {
    size.width *= size.height;
    size.height = 1;
  }

  for (int i = 0; i < size.height; ++i) {
    const Pixel3 *ptr_img = img_hsv.ptr<Pixel3> (i);
    const Pixel3 *ptr_end = ptr_img + size.width;

    while (ptr_img != ptr_end) {
      const uint8_t hVal = ptr_img->x;
      const uint8_t sVal = ptr_img->y;

      // Check expected maximum values.
      if (hVal > H_MAX) {
        GST_WARNING_OBJECT (chroma,
            "BUG: Hue (%u) out of expected range [0, %u]", hVal, H_MAX);
      }
      if (sVal > S_MAX) {
        GST_WARNING_OBJECT (chroma,
            "BUG: Saturation (%u) out of expected range [0, %u]", sVal, S_MAX);
      }

      // Increment the count of values that have been seen.
      if (chroma->priv->h_values[hVal] < G_MAXINT) {
        chroma->priv->h_values[hVal]++;
      }
      if (chroma->priv->s_values[sVal] < G_MAXINT) {
        chroma->priv->s_values[sVal]++;
      }

      // Increment loop pointers.
      ++ptr_img;
    }
  }
}

static void
kms_chroma_get_histogram (KmsChroma *chroma, const cv::Mat &img_hsv)
{
  cv::Rect roi (chroma->priv->x, chroma->priv->y, chroma->priv->width,
      chroma->priv->height);
  kms_chroma_add_values (chroma, img_hsv (roi));
}

static void
get_mask (const cv::Mat &img_hsv,
    cv::Mat &mask,
    gint h_min,
    gint h_max,
    gint s_min,
    gint s_max)
{
  mask = cv::Mat::zeros (img_hsv.size (), CV_8UC1);

  cv::Size size = img_hsv.size ();

  // Traverse as 1D vector if data is a continuous memory block.
  if (img_hsv.isContinuous () && mask.isContinuous ()) {
    size.width *= size.height;
    size.height = 1;
  }

  for (int i = 0; i < size.height; ++i) {
    const Pixel3 *ptr_img = img_hsv.ptr<Pixel3> (i);
    const Pixel3 *ptr_end = ptr_img + size.width;

    Pixel1 *ptr_mask = mask.ptr<Pixel1> (i);

    while (ptr_img != ptr_end) {
      if ((ptr_img->x >= h_min && ptr_img->x <= h_max)
          && (ptr_img->y >= s_min && ptr_img->y <= s_max)
          && (ptr_img->z >= V_MASK_MIN && ptr_img->z <= V_MASK_MAX)) {
        *ptr_mask = 255;
      }

      // Increment loop pointers.
      ++ptr_img;
      ++ptr_mask;
    }
  }

  // Remove noise and small holes in the mask.
  cv::Mat kernel1 = cv::getStructuringElement (cv::MORPH_RECT, cv::Size (3, 3),
      cv::Point (1, 1));
  cv::Mat kernel2 = cv::getStructuringElement (cv::MORPH_RECT, cv::Size (3, 3),
      cv::Point (1, 1));
  cv::morphologyEx (mask, mask, cv::MORPH_CLOSE, kernel1);
  cv::morphologyEx (mask, mask, cv::MORPH_OPEN, kernel2);
}

static void
kms_chroma_display_background (KmsChroma *chroma, const cv::Mat &mask)
{
  GST_OBJECT_LOCK (chroma);

  cv::Mat img = chroma->priv->cvImage;
  const cv::Mat bg = chroma->priv->background_image;

  cv::Size size = img.size ();

  // Traverse as 1D vector if data is a continuous memory block.
  if (img.isContinuous () && mask.isContinuous ()
      && (bg.empty () || bg.isContinuous ())) {
    size.width *= size.height;
    size.height = 1;
  }

  for (int i = 0; i < size.height; ++i) {
    Pixel3 *ptr_img = img.ptr<Pixel3> (i);
    const Pixel3 *ptr_end = ptr_img + size.width;

    const Pixel1 *ptr_mask = mask.ptr<Pixel1> (i);

    const Pixel3 *ptr_bg = bg.empty () ? NULL : bg.ptr<Pixel3> (i);

    while (ptr_img != ptr_end) {
      if (*ptr_mask) {
        *ptr_img = ptr_bg ? *ptr_bg : Pixel3 (0, 0, 0);
      }

      // Increment loop pointers.
      ++ptr_img;
      ++ptr_mask;
      if (ptr_bg) {
        ++ptr_bg;
      }
    }
  }

  GST_OBJECT_UNLOCK (chroma);
}

static GstFlowReturn
kms_chroma_transform_frame_ip (GstVideoFilter *filter, GstVideoFrame *frame)
{
  KmsChroma *chroma = KMS_CHROMA (filter);
  gint i;
  cv::Mat img_hsv, mask;

  if (!chroma->priv->calibration_area) {
    GST_DEBUG ("Calibration area not defined");
    return GST_FLOW_OK;
  }

  if (chroma->priv->configure_frames > LIMIT_FRAMES
      && chroma->priv->background_image.empty ()) {
    GST_TRACE ("No background image, skipping");
    return GST_FLOW_OK;
  }

  kms_chroma_initialize_images (chroma, frame);

  cv::cvtColor (chroma->priv->cvImage, img_hsv, cv::COLOR_BGR2HSV);

  if (chroma->priv->configure_frames <= LIMIT_FRAMES) {
    //check if the calibration area fits into the image
    if ((chroma->priv->x + chroma->priv->width) > chroma->priv->cvImage.cols) {
      chroma->priv->x = chroma->priv->cvImage.cols - chroma->priv->x - 1;
    }
    if ((chroma->priv->y + chroma->priv->height) > chroma->priv->cvImage.rows) {
      chroma->priv->y = chroma->priv->cvImage.rows - chroma->priv->y - 1;
    }

    kms_chroma_get_histogram (chroma, img_hsv);
    chroma->priv->configure_frames++;

    {
      GST_OBJECT_LOCK (chroma);
      cv::rectangle (chroma->priv->cvImage,
          cv::Point (chroma->priv->x, chroma->priv->y),
          cv::Point (chroma->priv->x + chroma->priv->width,
              chroma->priv->y + chroma->priv->height),
          cv::Scalar (255, 0, 0, 0));
      GST_OBJECT_UNLOCK (chroma);
    }

    if (chroma->priv->configure_frames == LIMIT_FRAMES) {

      for (i = 0; i < H_MAX; i++) {
        if (chroma->priv->h_values[i] >= HISTOGRAM_THRESHOLD) {
          chroma->priv->h_min = i;
          break;
        }
      }
      for (i = H_MAX; i >= 0; i--) {
        if (chroma->priv->h_values[i] >= HISTOGRAM_THRESHOLD) {
          chroma->priv->h_max = i;
          break;
        }
      }
      for (i = 0; i < S_MAX; i++) {
        if (chroma->priv->s_values[i] >= HISTOGRAM_THRESHOLD) {
          chroma->priv->s_min = i;
          break;
        }
      }
      for (i = S_MAX; i >= 0; i--) {
        if (chroma->priv->s_values[i] >= HISTOGRAM_THRESHOLD) {
          chroma->priv->s_max = i;
          break;
        }
      }
      GST_DEBUG ("ARRAY h_min %d h_max %d s_min %d s_max %d",
          chroma->priv->h_min, chroma->priv->h_max, chroma->priv->s_min,
          chroma->priv->s_max);
    }

    goto end;
  }

  get_mask (img_hsv, mask, chroma->priv->h_min, chroma->priv->h_max,
      chroma->priv->s_min, chroma->priv->s_max);

  kms_chroma_display_background (chroma, mask);

end:

  return GST_FLOW_OK;
}

static void
kms_chroma_finalize (GObject *object)
{
  KmsChroma *chroma = KMS_CHROMA (object);

  chroma->priv->cvImage.release ();
  chroma->priv->background_image.release ();

  if (chroma->priv->dir_created) {
    remove_recursive (chroma->priv->dir);
  }

  if (chroma->priv->dir != NULL) {
    g_free (chroma->priv->dir);
  }

  if (chroma->priv->background_uri != NULL) {
    g_free (chroma->priv->background_uri);
  }

  G_OBJECT_CLASS (kms_chroma_parent_class)->finalize (object);
}

static void
kms_chroma_init (KmsChroma *chroma)
{
  chroma->priv = KMS_CHROMA_GET_PRIVATE (chroma);

  chroma->priv->dir_created = FALSE;
  chroma->priv->background_uri = NULL;
  chroma->priv->dir = NULL;
  chroma->priv->configure_frames = 0;

  chroma->priv->calibration_area = FALSE;
  chroma->priv->x = 0;
  chroma->priv->y = 0;
  chroma->priv->width = 0;
  chroma->priv->height = 0;

  chroma->priv->h_min = 0;
  chroma->priv->h_max = H_MAX;
  chroma->priv->s_min = 0;
  chroma->priv->s_max = S_MAX;

  memset (chroma->priv->h_values, 0, H_VALUES * sizeof (gint));
  memset (chroma->priv->s_values, 0, S_VALUES * sizeof (gint));
}

static void
kms_chroma_class_init (KmsChromaClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "chroma element", "Video/Filter",
      "Set a defined background over a chroma",
      "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_chroma_set_property;
  gobject_class->get_property = kms_chroma_get_property;
  gobject_class->finalize = kms_chroma_finalize;

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_chroma_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_IMAGE_BACKGROUND,
      g_param_spec_string ("image-background", "image background",
          "set the uri of the background image", NULL, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_CALIBRATION_AREA,
      g_param_spec_boxed ("calibration-area", "calibration area",
          "supply the position and dimensions of the color calibration area",
          GST_TYPE_STRUCTURE,
          (GParamFlags)(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS)));

  g_type_class_add_private (klass, sizeof (KmsChromaPrivate));
}

gboolean
kms_chroma_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_CHROMA);
}
