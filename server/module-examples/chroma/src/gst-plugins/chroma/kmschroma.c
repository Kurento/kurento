/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#define _XOPEN_SOURCE 500

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmschroma.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

#include <opencv/cv.h>
#include <opencv/cxcore.h>
#include <opencv/highgui.h>
#include <gst/video/gstvideofilter.h>
#include <libsoup/soup.h>

// FIXME: Compatibility between OpenCV 2.x and 3.x
#include <opencv2/core/version.hpp>
#if CV_MAJOR_VERSION > 2
#include <opencv2/imgcodecs/imgcodecs_c.h>
#include <math.h>
int
cvRound (double value)
{
  return (ceil (value));
}
#endif

#define TEMP_PATH "/tmp/XXXXXX"
#define LIMIT_FRAMES 60
#define HISTOGRAM_THRESHOLD (10*LIMIT_FRAMES)
#define H_VALUES 181
#define S_VALUES 256
#define H_MAX 180
#define S_MAX 255
#define V_MIN 30
#define V_MAX 256

#define PLUGIN_NAME "chroma"

GST_DEBUG_CATEGORY_STATIC (kms_chroma_debug_category);
#define GST_CAT_DEFAULT kms_chroma_debug_category

#define KMS_CHROMA_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (       \
    (obj),                            \
    KMS_TYPE_CHROMA,                  \
    KmsChromaPrivate                  \
  )                                   \
)

enum
{
  PROP_0,
  PROP_IMAGE_BACKGROUND,
  PROP_CALIBRATION_AREA,
  N_PROPERTIES
};

struct _KmsChromaPrivate
{
  IplImage *cvImage, *background_image;
  gboolean dir_created, calibration_area;
  gchar *dir, *background_uri;
  gint configure_frames;
  gint x, y, width, height;
  gint h_min, s_min, h_max, s_max;
  gint h_values[H_VALUES];
  gint s_values[S_VALUES];
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsChroma, kms_chroma,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_chroma_debug_category, PLUGIN_NAME,
        0, "debug category for chroma element"));

static gboolean
kms_chroma_is_valid_uri (const gchar * url)
{
  gboolean ret;
  GRegex *regex;

  regex = g_regex_new ("^(?:((?:https?):)\\/\\/)([^:\\/\\s]+)(?::(\\d*))?(?:\\/"
      "([^\\s?#]+)?([?][^?#]*)?(#.*)?)?$", 0, 0, NULL);
  ret = g_regex_match (regex, url, G_REGEX_MATCH_ANCHORED, NULL);
  g_regex_unref (regex);

  return ret;
}

static void
load_from_url (gchar * file_name, gchar * url)
{
  SoupSession *session;
  SoupMessage *msg;
  FILE *dst;

  session = soup_session_sync_new ();
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
kms_chroma_load_image_to_overlay (KmsChroma * chroma)
{
  IplImage *image_aux = NULL;

  if (chroma->priv->background_uri == NULL) {
    GST_DEBUG ("Unset the background image");

    GST_OBJECT_LOCK (chroma);

    if (chroma->priv->background_image != NULL) {
      cvReleaseImage (&chroma->priv->background_image);
      chroma->priv->background_image = NULL;
    }

    GST_OBJECT_UNLOCK (chroma);
    return;
  }

  if (!chroma->priv->dir_created) {
    gchar *d = g_strdup (TEMP_PATH);

    chroma->priv->dir = g_mkdtemp (d);
    chroma->priv->dir_created = TRUE;
  }

  image_aux =
      cvLoadImage (chroma->priv->background_uri, CV_LOAD_IMAGE_UNCHANGED);

  if (image_aux != NULL) {
    GST_DEBUG ("Background loaded from file");
    goto end;
  }

  if (kms_chroma_is_valid_uri (chroma->priv->background_uri)) {
    gchar *file_name = g_strconcat (chroma->priv->dir, "/image.png", NULL);

    load_from_url (file_name, chroma->priv->background_uri);
    image_aux = cvLoadImage (file_name, CV_LOAD_IMAGE_UNCHANGED);
    g_remove (file_name);
    g_free (file_name);
  }

  if (image_aux == NULL) {
    GST_ELEMENT_ERROR (chroma, RESOURCE, NOT_FOUND, ("Background not loaded"),
        (NULL));
  } else {
    GST_DEBUG ("Background loaded from URL");
  }

end:

  GST_OBJECT_LOCK (chroma);

  if (chroma->priv->background_image != NULL) {
    cvReleaseImage (&chroma->priv->background_image);
    chroma->priv->background_image = NULL;
  }

  if (image_aux != NULL)
    chroma->priv->background_image = image_aux;

  GST_OBJECT_UNLOCK (chroma);
}

static void
kms_chroma_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsChroma *chroma = KMS_CHROMA (object);

  switch (property_id) {
    case PROP_IMAGE_BACKGROUND:
      if (chroma->priv->background_uri != NULL)
        g_free (chroma->priv->background_uri);

      chroma->priv->background_uri = g_value_dup_string (value);
      kms_chroma_load_image_to_overlay (chroma);
      break;
    case PROP_CALIBRATION_AREA:{
      GstStructure *aux;

      aux = g_value_dup_boxed (value);
      gst_structure_get (aux, "x", G_TYPE_INT, &chroma->priv->x, NULL);
      gst_structure_get (aux, "y", G_TYPE_INT, &chroma->priv->y, NULL);
      gst_structure_get (aux, "width", G_TYPE_INT, &chroma->priv->width, NULL);
      gst_structure_get (aux, "height", G_TYPE_INT, &chroma->priv->height,
          NULL);

      if (chroma->priv->x < 0)
        chroma->priv->x = 0;

      if (chroma->priv->y < 0)
        chroma->priv->y = 0;

      chroma->priv->calibration_area = TRUE;
      gst_structure_free (aux);
      GST_DEBUG ("Defined calibration area in x %d, y %d,"
          "width %d, height %d", chroma->priv->x, chroma->priv->y,
          chroma->priv->width, chroma->priv->height);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_chroma_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsChroma *chroma = KMS_CHROMA (object);

  switch (property_id) {
    case PROP_IMAGE_BACKGROUND:
      if (chroma->priv->background_uri == NULL)
        g_value_set_string (value, "");
      else
        g_value_set_string (value, chroma->priv->background_uri);
      break;
    case PROP_CALIBRATION_AREA:{
      GstStructure *aux;

      aux = gst_structure_new ("calibration_area",
          "x", G_TYPE_INT, chroma->priv->x,
          "y", G_TYPE_INT, chroma->priv->y,
          "width", G_TYPE_INT, chroma->priv->width,
          "height", G_TYPE_INT, chroma->priv->height, NULL);
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
kms_chroma_initialize_images (KmsChroma * chroma, GstVideoFrame * frame)
{
  if (chroma->priv->cvImage == NULL) {
    chroma->priv->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);

  } else if ((chroma->priv->cvImage->width != frame->info.width)
      || (chroma->priv->cvImage->height != frame->info.height)) {
    cvReleaseImageHeader (&chroma->priv->cvImage);
    chroma->priv->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
  }

  if ((chroma->priv->background_image != NULL) &&
      ((chroma->priv->background_image->width != frame->info.width)
          || (chroma->priv->background_image->height != frame->info.height))) {
    //resize the background image
    IplImage *aux;

    aux = cvCloneImage (chroma->priv->background_image);
    cvReleaseImage (&chroma->priv->background_image);
    chroma->priv->background_image = cvCreateImage (cvSize (frame->info.width,
            frame->info.height), IPL_DEPTH_8U, aux->nChannels);
    cvResize (aux, chroma->priv->background_image, CV_INTER_LINEAR);
    cvReleaseImage (&aux);
  }
}

static int
delete_file (const char *fpath, const struct stat *sb, int typeflag,
    struct FTW *ftwbuf)
{
  int rv = g_remove (fpath);

  if (rv) {
    GST_WARNING ("Error deleting file: %s. %s", fpath, strerror (errno));
  }

  return rv;
}

static void
remove_recursive (const gchar * path)
{
  nftw (path, delete_file, 64, FTW_DEPTH | FTW_PHYS);
}

static void
kms_chroma_add_values (KmsChroma * chroma, IplImage * img)
{
  IplImage *h_plane = cvCreateImage (cvGetSize (img), 8, 1);
  IplImage *s_plane = cvCreateImage (cvGetSize (img), 8, 1);
  gint i, j;

  cvSplit (img, h_plane, s_plane, NULL, NULL);

  for (i = 0; i < img->width; i++) {
    for (j = 0; j < img->height; j++) {
      if (chroma->priv->h_values[(*(uchar *) (h_plane->imageData +
                      (j) * h_plane->widthStep + i))] < G_MAXINT) {
        chroma->priv->h_values[(*(uchar *) (h_plane->imageData +
                    (j) * h_plane->widthStep + i))]++;
      }
      if (chroma->priv->s_values[(*(uchar *) (s_plane->imageData +
                      (j) * s_plane->widthStep + i))] < G_MAXINT) {
        chroma->priv->s_values[(*(uchar *) (s_plane->imageData +
                    (j) * s_plane->widthStep + i))]++;
      }
    }
  }

  cvReleaseImage (&h_plane);
  cvReleaseImage (&s_plane);
}

static void
kms_chroma_get_histogram (KmsChroma * chroma, IplImage * hsv)
{
  IplImage *srcAux =
      cvCreateImage (cvSize (chroma->priv->width, chroma->priv->height),
      IPL_DEPTH_8U, 3);

  cvSetImageROI (hsv, cvRect (chroma->priv->x, chroma->priv->y,
          chroma->priv->width, chroma->priv->height));
  cvCopy (hsv, srcAux, 0);
  cvResetImageROI (hsv);
  kms_chroma_add_values (chroma, srcAux);

  cvReleaseImage (&srcAux);
}

static IplImage *
get_mask (IplImage * img, gint h_min, gint h_max, gint s_min, gint s_max)
{
  int w, h;
  uchar *image_row, *image_row_mask;
  IplConvKernel *kernel1;
  IplConvKernel *kernel2;

  IplImage *mask = cvCreateImage (cvGetSize (img), img->depth, 1);

  image_row = (uchar *) img->imageData;
  image_row_mask = (uchar *) mask->imageData;

  for (h = 0; h < img->height; h++) {
    uchar *image_column = image_row;
    uchar *image_column_mask = image_row_mask;

    for (w = 0; w < img->width; w++) {
      if ((((uchar) * (image_column) >= h_min)
              && ((uchar) * (image_column) <= h_max))
          && (((uchar) * (image_column + 1) >= s_min)
              && ((uchar) * (image_column + 1) <= s_max))
          && (((uchar) * (image_column + 2) >= V_MIN)
              && ((uchar) * (image_column + 2) <= V_MAX))) {

        *(image_column_mask) = 255;
      } else {
        *(image_column_mask) = 0;
      }

      image_column += img->nChannels;
      image_column_mask += mask->nChannels;
    }
    image_row += img->widthStep;
    image_row_mask += mask->widthStep;
  }

  kernel1 = cvCreateStructuringElementEx (3, 3, 1, 1, CV_SHAPE_RECT, NULL);
  kernel2 = cvCreateStructuringElementEx (3, 3, 1, 1, CV_SHAPE_RECT, NULL);
  cvMorphologyEx (mask, mask, NULL, kernel1, CV_MOP_CLOSE, 1);
  cvMorphologyEx (mask, mask, NULL, kernel2, CV_MOP_OPEN, 1);
  cvReleaseStructuringElement (&kernel1);
  cvReleaseStructuringElement (&kernel2);

  return mask;
}

static void
kms_chroma_display_background (KmsChroma * chroma, IplImage * mask)
{
  int w, h;
  uchar *image_row, *image_row_mask, *image_row_background;

  GST_OBJECT_LOCK (chroma);
  if (chroma->priv->background_image != NULL) {
    image_row = (uchar *) chroma->priv->cvImage->imageData;
    image_row_mask = (uchar *) mask->imageData;
    image_row_background = (uchar *) chroma->priv->background_image->imageData;

    for (h = 0; h < chroma->priv->cvImage->height; h++) {
      uchar *image_column = image_row;
      uchar *image_column_mask = image_row_mask;
      uchar *image_column_background = image_row_background;

      for (w = 0; w < chroma->priv->cvImage->width; w++) {
        if (((uchar) * (image_column_mask) == 255)) {

          *(image_column) = (uchar) (*(image_column_background));
          *(image_column + 1) = (uchar) (*(image_column_background + 1));
          *(image_column + 2) = (uchar) (*(image_column_background + 2));
        }
        image_column += chroma->priv->cvImage->nChannels;
        image_column_mask += mask->nChannels;
        image_column_background += chroma->priv->background_image->nChannels;
      }
      image_row += chroma->priv->cvImage->widthStep;
      image_row_mask += mask->widthStep;
      image_row_background += chroma->priv->background_image->widthStep;
    }
  } else {
    image_row = (uchar *) chroma->priv->cvImage->imageData;
    image_row_mask = (uchar *) mask->imageData;

    for (h = 0; h < chroma->priv->cvImage->height; h++) {
      uchar *image_column = image_row;
      uchar *image_column_mask = image_row_mask;

      for (w = 0; w < chroma->priv->cvImage->width; w++) {
        if (((uchar) * (image_column_mask) == 255)) {

          *(image_column) = 0;
          *(image_column + 1) = 0;
          *(image_column + 2) = 0;
        }
        image_column += chroma->priv->cvImage->nChannels;
        image_column_mask += mask->nChannels;
      }
      image_row += chroma->priv->cvImage->widthStep;
      image_row_mask += mask->widthStep;
    }
  }
  GST_OBJECT_UNLOCK (chroma);
}

static GstFlowReturn
kms_chroma_transform_frame_ip (GstVideoFilter * filter, GstVideoFrame * frame)
{
  KmsChroma *chroma = KMS_CHROMA (filter);
  GstMapInfo info;
  IplImage *hsv;
  IplImage *mask;
  gint i;

  if (!chroma->priv->calibration_area) {
    GST_DEBUG ("Calibration area not defined");
    return GST_FLOW_OK;
  }

  if (chroma->priv->configure_frames > LIMIT_FRAMES &&
      chroma->priv->background_image == NULL) {
    GST_TRACE ("No background image, skipping");
    return GST_FLOW_OK;
  }

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  kms_chroma_initialize_images (chroma, frame);
  chroma->priv->cvImage->imageData = (char *) info.data;

  hsv = cvCreateImage (cvGetSize (chroma->priv->cvImage),
      chroma->priv->cvImage->depth, chroma->priv->cvImage->nChannels);
  cvCvtColor (chroma->priv->cvImage, hsv, CV_BGR2HSV);

  if (chroma->priv->configure_frames <= LIMIT_FRAMES) {
    //check if the calibration area fits into the image
    if ((chroma->priv->x + chroma->priv->width) > chroma->priv->cvImage->width) {
      chroma->priv->x = chroma->priv->cvImage->width - chroma->priv->x - 1;
    }
    if ((chroma->priv->y + chroma->priv->height) >
        chroma->priv->cvImage->height) {
      chroma->priv->y = chroma->priv->cvImage->height - chroma->priv->y - 1;
    }

    kms_chroma_get_histogram (chroma, hsv);
    chroma->priv->configure_frames++;
    cvRectangle (chroma->priv->cvImage, cvPoint (chroma->priv->x,
            chroma->priv->y), cvPoint (chroma->priv->x + chroma->priv->width,
            chroma->priv->y + chroma->priv->height), cvScalar (255, 0, 0, 0), 1,
        8, 0);

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
          chroma->priv->h_min, chroma->priv->h_max,
          chroma->priv->s_min, chroma->priv->s_max);
    }

    goto end;
  }

  mask = get_mask (hsv, chroma->priv->h_min, chroma->priv->h_max,
      chroma->priv->s_min, chroma->priv->s_max);

  kms_chroma_display_background (chroma, mask);

  cvReleaseImage (&mask);

end:
  cvReleaseImage (&hsv);
  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
kms_chroma_finalize (GObject * object)
{
  KmsChroma *chroma = KMS_CHROMA (object);

  if (chroma->priv->cvImage != NULL)
    cvReleaseImage (&chroma->priv->cvImage);

  if (chroma->priv->background_image != NULL)
    cvReleaseImage (&chroma->priv->background_image);

  if (chroma->priv->dir_created)
    remove_recursive (chroma->priv->dir);

  if (chroma->priv->dir != NULL)
    g_free (chroma->priv->dir);

  if (chroma->priv->background_uri != NULL)
    g_free (chroma->priv->background_uri);

  G_OBJECT_CLASS (kms_chroma_parent_class)->finalize (object);
}

static void
kms_chroma_init (KmsChroma * chroma)
{
  chroma->priv = KMS_CHROMA_GET_PRIVATE (chroma);

  chroma->priv->cvImage = NULL;
  chroma->priv->background_image = NULL;
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
kms_chroma_class_init (KmsChromaClass * klass)
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
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsChromaPrivate));
}

gboolean
kms_chroma_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_CHROMA);
}
