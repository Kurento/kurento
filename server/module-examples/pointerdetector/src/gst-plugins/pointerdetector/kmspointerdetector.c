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

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>
#include <libsoup/soup.h>
#include <math.h>

#include "kmspointerdetector.h"
#include <commons/kms-core-marshal.h>

// FIXME: Compatibility between OpenCV 2.x and 3.x
#include <opencv2/core/version.hpp>
#if CV_MAJOR_VERSION > 2
#include <opencv2/imgcodecs/imgcodecs_c.h>
int
cvRound (double value)
{
  return (ceil (value));
}
#endif

#define PLUGIN_NAME "pointerdetector"

GST_DEBUG_CATEGORY_STATIC (kms_pointer_detector_debug_category);
#define GST_CAT_DEFAULT kms_pointer_detector_debug_category

#define KMS_POINTER_DETECTOR_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                  \
    (obj),                                       \
    KMS_TYPE_POINTER_DETECTOR,                  \
    KmsPointerDetectorPrivate                   \
  )                                              \
)

#define FRAMES_TO_RESET  ((int) 250)
#define GREEN CV_RGB (0, 255, 0)
#define WHITE CV_RGB (255, 255, 255)
#define RED cvScalar (359, 89, 100, 0)

#define TEMP_PATH "/tmp/XXXXXX"
#define INACTIVE_IMAGE_VARIANT_NAME "i"
#define ACTIVE_IMAGE_VARIANT_NAME "a"
#define V_MIN 30
#define V_MAX 256
#define H_VALUES 181
#define S_VALUES 256
#define H_MAX 180
#define S_MAX 255
#define HIST_THRESHOLD 20

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  PROP_WINDOWS_LAYOUT,
  PROP_MESSAGE,
  PROP_SHOW_WINDOWS_LAYOUT,
  PROP_CALIBRATION_AREA
};

enum
{
  SIGNAL_CALIBRATE_COLOR,
  LAST_SIGNAL
};

static guint kms_pointer_detector_signals[LAST_SIGNAL] = { 0 };

struct _KmsPointerDetectorPrivate
{
  IplImage *cvImage;
  CvPoint finalPointerPosition;
  int iteration;
  CvSize frameSize;
  gboolean show_debug_info;
  GstStructure *buttonsLayout;
  GSList *buttonsLayoutList;
  gchar *previousButtonClickedId;
  gboolean putMessage;
  gboolean show_windows_layout;
  gchar *images_dir;
  gint x_calibration, y_calibration, width_calibration, height_calibration;
  gint h_min, h_max, s_min, s_max;
  IplConvKernel *kernel1;
  IplConvKernel *kernel2;
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPointerDetector, kms_pointer_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_pointer_detector_debug_category, PLUGIN_NAME,
        0, "debug category for pointerdetector element"));

static void
dispose_button_struct (gpointer data)
{
  ButtonStruct *aux = data;

  if (aux->id != NULL)
    g_free (aux->id);

  if (aux->inactive_icon != NULL)
    cvReleaseImage (&aux->inactive_icon);

  if (aux->active_icon != NULL)
    cvReleaseImage (&aux->active_icon);

  g_free (aux);
}

static void
kms_pointer_detector_dispose_buttons_layout_list (KmsPointerDetector *
    pointerdetector)
{
  g_slist_free_full (pointerdetector->priv->buttonsLayoutList,
      dispose_button_struct);
  pointerdetector->priv->buttonsLayoutList = NULL;
}

static gboolean
is_valid_uri (const gchar * url)
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

static IplImage *
load_image (gchar * uri, gchar * dir, gchar * image_name,
    const gchar * name_variant)
{
  IplImage *aux;

  aux = cvLoadImage (uri, CV_LOAD_IMAGE_UNCHANGED);
  if (aux == NULL) {
    if (is_valid_uri (uri)) {
      gchar *file_name;

      file_name =
          g_strconcat (dir, "/", image_name, name_variant, ".png", NULL);
      load_from_url (file_name, uri);
      aux = cvLoadImage (file_name, CV_LOAD_IMAGE_UNCHANGED);
      g_remove (file_name);
      g_free (file_name);
    }
  }

  return aux;
}

static void
kms_pointer_detector_load_buttonsLayout (KmsPointerDetector * pointerdetector)
{
  int aux, len;
  gboolean have_inactive_icon, have_active_icon, have_transparency;
  gchar *inactive_uri, *active_uri;

  if (pointerdetector->priv->buttonsLayoutList != NULL) {
    kms_pointer_detector_dispose_buttons_layout_list (pointerdetector);
  }

  len = gst_structure_n_fields (pointerdetector->priv->buttonsLayout);
  GST_DEBUG ("len: %d", len);

  for (aux = 0; aux < len; aux++) {
    const gchar *name =
        gst_structure_nth_field_name (pointerdetector->priv->buttonsLayout,
        aux);
    GstStructure *button;
    gboolean ret;

    ret =
        gst_structure_get (pointerdetector->priv->buttonsLayout, name,
        GST_TYPE_STRUCTURE, &button, NULL);
    if (ret) {
      ButtonStruct *structAux = g_malloc0 (sizeof (ButtonStruct));
      IplImage *aux = NULL;

      gst_structure_get (button, "upRightCornerX", G_TYPE_INT,
          &structAux->cvButtonLayout.x, NULL);
      gst_structure_get (button, "upRightCornerY", G_TYPE_INT,
          &structAux->cvButtonLayout.y, NULL);
      gst_structure_get (button, "width", G_TYPE_INT,
          &structAux->cvButtonLayout.width, NULL);
      gst_structure_get (button, "height", G_TYPE_INT,
          &structAux->cvButtonLayout.height, NULL);
      gst_structure_get (button, "id", G_TYPE_STRING, &structAux->id, NULL);
      have_inactive_icon =
          gst_structure_get (button, "inactive_uri", G_TYPE_STRING,
          &inactive_uri, NULL);
      have_transparency =
          gst_structure_get (button, "transparency", G_TYPE_DOUBLE,
          &structAux->transparency, NULL);
      have_active_icon =
          gst_structure_get (button, "active_uri", G_TYPE_STRING, &active_uri,
          NULL);

      if (have_inactive_icon) {
        aux =
            load_image (inactive_uri, pointerdetector->priv->images_dir,
            structAux->id, INACTIVE_IMAGE_VARIANT_NAME);

        if (aux != NULL) {
          structAux->inactive_icon =
              cvCreateImage (cvSize (structAux->cvButtonLayout.width,
                  structAux->cvButtonLayout.height), aux->depth,
              aux->nChannels);
          cvResize (aux, structAux->inactive_icon, CV_INTER_CUBIC);
          cvReleaseImage (&aux);
        } else {
          structAux->inactive_icon = NULL;
        }
      } else {
        structAux->inactive_icon = NULL;
      }

      if (have_active_icon) {
        aux =
            load_image (active_uri, pointerdetector->priv->images_dir,
            structAux->id, ACTIVE_IMAGE_VARIANT_NAME);

        if (aux != NULL) {
          structAux->active_icon =
              cvCreateImage (cvSize (structAux->cvButtonLayout.width,
                  structAux->cvButtonLayout.height), aux->depth,
              aux->nChannels);
          cvResize (aux, structAux->active_icon, CV_INTER_CUBIC);
          cvReleaseImage (&aux);
        } else {
          structAux->active_icon = NULL;
        }
      } else {
        structAux->active_icon = NULL;
      }

      if (have_transparency) {
        structAux->transparency = 1.0 - structAux->transparency;
      } else {
        structAux->transparency = 1.0;
      }

      GST_DEBUG ("check: %d %d %d %d", structAux->cvButtonLayout.x,
          structAux->cvButtonLayout.y, structAux->cvButtonLayout.width,
          structAux->cvButtonLayout.height);
      pointerdetector->priv->buttonsLayoutList =
          g_slist_append (pointerdetector->priv->buttonsLayoutList, structAux);
      gst_structure_free (button);

      if (have_inactive_icon) {
        g_free (inactive_uri);
      }
      if (have_active_icon) {
        g_free (active_uri);
      }
    }
  }
}

static void
kms_pointer_detector_init (KmsPointerDetector * pointerdetector)
{
  pointerdetector->priv = KMS_POINTER_DETECTOR_GET_PRIVATE (pointerdetector);

  pointerdetector->priv->cvImage = NULL;
  pointerdetector->priv->iteration = 0;
  pointerdetector->priv->show_debug_info = FALSE;
  pointerdetector->priv->buttonsLayout = NULL;
  pointerdetector->priv->buttonsLayoutList = NULL;
  pointerdetector->priv->putMessage = TRUE;
  pointerdetector->priv->show_windows_layout = TRUE;

  pointerdetector->priv->h_min = 0;
  pointerdetector->priv->h_max = 0;
  pointerdetector->priv->s_min = 0;
  pointerdetector->priv->s_max = 0;

  pointerdetector->priv->x_calibration = 0;
  pointerdetector->priv->y_calibration = 0;
  pointerdetector->priv->width_calibration = 0;
  pointerdetector->priv->height_calibration = 0;

  gchar d[] = TEMP_PATH;
  gchar *aux = g_mkdtemp (d);

  pointerdetector->priv->images_dir = g_strdup (aux);
  pointerdetector->priv->kernel1 =
      cvCreateStructuringElementEx (21, 21, 10, 10, CV_SHAPE_RECT, NULL);
  pointerdetector->priv->kernel2 =
      cvCreateStructuringElementEx (11, 11, 5, 5, CV_SHAPE_RECT, NULL);
}

static void
kms_pointer_detector_calibrate_color (KmsPointerDetector * pointerdetector)
{
  gint h_values[H_VALUES];
  gint s_values[S_VALUES];
  IplImage *h_channel, *s_channel;
  IplImage *calibration_area;
  gint i, j;

  if (pointerdetector->priv->cvImage == NULL) {
    return;
  }

  memset (h_values, 0, H_VALUES * sizeof (gint));
  memset (s_values, 0, S_VALUES * sizeof (gint));
  calibration_area =
      cvCreateImage (cvSize (pointerdetector->priv->width_calibration,
          pointerdetector->priv->height_calibration), IPL_DEPTH_8U, 3);
  h_channel = cvCreateImage (cvGetSize (calibration_area), 8, 1);
  s_channel = cvCreateImage (cvGetSize (calibration_area), 8, 1);

  GST_DEBUG ("REGION x %d y %d  width %d height %d\n",
      pointerdetector->priv->x_calibration,
      pointerdetector->priv->y_calibration,
      pointerdetector->priv->width_calibration,
      pointerdetector->priv->height_calibration);

  GST_OBJECT_LOCK (pointerdetector);
  cvSetImageROI (pointerdetector->priv->cvImage,
      cvRect (pointerdetector->priv->x_calibration,
          pointerdetector->priv->y_calibration,
          pointerdetector->priv->width_calibration,
          pointerdetector->priv->height_calibration));
  cvCopy (pointerdetector->priv->cvImage, calibration_area, 0);
  cvResetImageROI (pointerdetector->priv->cvImage);

  cvCvtColor (calibration_area, calibration_area, CV_BGR2HSV);
  cvSplit (calibration_area, h_channel, s_channel, NULL, NULL);

  for (i = 0; i < calibration_area->width; i++) {
    for (j = 0; j < calibration_area->height; j++) {
      h_values[(*(uchar *) (h_channel->imageData +
                  (j) * h_channel->widthStep + i))]++;
      s_values[(*(uchar *) (s_channel->imageData +
                  (j) * s_channel->widthStep + i))]++;
    }
  }

  for (i = 1; i < H_MAX; i++) {
    if (h_values[i] >= HIST_THRESHOLD) {
      pointerdetector->priv->h_min = i - 5;
      break;
    }
  }
  for (i = H_MAX; i >= 0; i--) {
    if (h_values[i] >= HIST_THRESHOLD) {
      pointerdetector->priv->h_max = i + 5;
      break;
    }
  }
  for (i = 1; i < S_MAX; i++) {
    if (s_values[i] >= HIST_THRESHOLD) {
      pointerdetector->priv->s_min = i - 5;
      break;
    }
  }
  for (i = S_MAX; i >= 0; i--) {
    if (s_values[i] >= HIST_THRESHOLD) {
      pointerdetector->priv->s_max = i + 5;
      break;
    }
  }
  GST_OBJECT_UNLOCK (pointerdetector);
  GST_DEBUG ("COLOR TO TRACK h_min %d h_max %d s_min %d s_max %d",
      pointerdetector->priv->h_min, pointerdetector->priv->h_max,
      pointerdetector->priv->s_min, pointerdetector->priv->s_max);

  cvReleaseImage (&h_channel);
  cvReleaseImage (&s_channel);
  cvReleaseImage (&calibration_area);
}

void
kms_pointer_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  GST_OBJECT_LOCK (pointerdetector);
  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      pointerdetector->priv->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_WINDOWS_LAYOUT:
      if (pointerdetector->priv->buttonsLayout != NULL)
        gst_structure_free (pointerdetector->priv->buttonsLayout);

      pointerdetector->priv->buttonsLayout = g_value_dup_boxed (value);
      kms_pointer_detector_load_buttonsLayout (pointerdetector);
      break;
    case PROP_MESSAGE:
      pointerdetector->priv->putMessage = g_value_get_boolean (value);
      break;
    case PROP_SHOW_WINDOWS_LAYOUT:
      pointerdetector->priv->show_windows_layout = g_value_get_boolean (value);
      break;
    case PROP_CALIBRATION_AREA:{
      GstStructure *aux;

      aux = g_value_dup_boxed (value);
      gst_structure_get (aux, "x", G_TYPE_INT,
          &pointerdetector->priv->x_calibration, NULL);
      gst_structure_get (aux, "y", G_TYPE_INT,
          &pointerdetector->priv->y_calibration, NULL);
      gst_structure_get (aux, "width", G_TYPE_INT,
          &pointerdetector->priv->width_calibration, NULL);
      gst_structure_get (aux, "height", G_TYPE_INT,
          &pointerdetector->priv->height_calibration, NULL);
      gst_structure_free (aux);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  GST_OBJECT_UNLOCK (pointerdetector);
}

void
kms_pointer_detector_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, pointerdetector->priv->show_debug_info);
      break;
    case PROP_WINDOWS_LAYOUT:
      if (pointerdetector->priv->buttonsLayout == NULL) {
        pointerdetector->priv->buttonsLayout =
            gst_structure_new_empty ("windows");
      }
      g_value_set_boxed (value, pointerdetector->priv->buttonsLayout);
      break;
    case PROP_MESSAGE:
      g_value_set_boolean (value, pointerdetector->priv->putMessage);
      break;
    case PROP_SHOW_WINDOWS_LAYOUT:
      g_value_set_boolean (value, pointerdetector->priv->show_windows_layout);
      break;
    case PROP_CALIBRATION_AREA:{
      GstStructure *aux;

      aux = gst_structure_new ("calibration_area",
          "x", G_TYPE_INT, pointerdetector->priv->x_calibration,
          "y", G_TYPE_INT, pointerdetector->priv->y_calibration,
          "width", G_TYPE_INT, pointerdetector->priv->width_calibration,
          "height", G_TYPE_INT, pointerdetector->priv->height_calibration,
          NULL);
      g_value_set_boxed (value, aux);
      gst_structure_free (aux);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
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

void
kms_pointer_detector_finalize (GObject * object)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (object);

  cvReleaseImageHeader (&pointerdetector->priv->cvImage);

  cvReleaseStructuringElement (&pointerdetector->priv->kernel1);
  cvReleaseStructuringElement (&pointerdetector->priv->kernel2);

  remove_recursive (pointerdetector->priv->images_dir);
  g_free (pointerdetector->priv->images_dir);

  if (pointerdetector->priv->previousButtonClickedId != NULL) {
    g_free (pointerdetector->priv->previousButtonClickedId);
  }

  if (pointerdetector->priv->buttonsLayoutList != NULL) {
    kms_pointer_detector_dispose_buttons_layout_list (pointerdetector);
  }

  if (pointerdetector->priv->buttonsLayout != NULL) {
    gst_structure_free (pointerdetector->priv->buttonsLayout);
  }

  G_OBJECT_CLASS (kms_pointer_detector_parent_class)->finalize (object);
}

static gboolean
kms_pointer_detector_start (GstBaseTransform * trans)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (trans);

  GST_DEBUG_OBJECT (pointerdetector, "start");

  return TRUE;
}

static gboolean
kms_pointer_detector_stop (GstBaseTransform * trans)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (trans);

  GST_DEBUG_OBJECT (pointerdetector, "stop");

  return TRUE;
}

static gboolean
kms_pointer_detector_set_info (GstVideoFilter * filter, GstCaps * incaps,
    GstVideoInfo * in_info, GstCaps * outcaps, GstVideoInfo * out_info)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (filter);

  GST_DEBUG_OBJECT (pointerdetector, "set_info");

  return TRUE;
}

static void
kms_pointer_detector_initialize_images (KmsPointerDetector * pointerdetector,
    GstVideoFrame * frame)
{
  if (pointerdetector->priv->cvImage == NULL) {
    pointerdetector->priv->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
  } else if ((pointerdetector->priv->cvImage->width != frame->info.width)
      || (pointerdetector->priv->cvImage->height != frame->info.height)) {
    cvReleaseImageHeader (&pointerdetector->priv->cvImage);
    pointerdetector->priv->cvImage =
        cvCreateImageHeader (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
  }
}

static gboolean
kms_pointer_detector_check_pointer_into_button (CvPoint * pointer_position,
    ButtonStruct * buttonStruct)
{
  int downLeftCornerX =
      buttonStruct->cvButtonLayout.x + buttonStruct->cvButtonLayout.width;
  int downLeftCornerY =
      buttonStruct->cvButtonLayout.y + buttonStruct->cvButtonLayout.height;
  if (((pointer_position->x - buttonStruct->cvButtonLayout.x) > 0)
      && ((pointer_position->y - buttonStruct->cvButtonLayout.y) > 0)
      && ((pointer_position->x - downLeftCornerX) < 0)
      && ((pointer_position->y - downLeftCornerY) < 0)) {
    return TRUE;
  } else {
    return FALSE;
  }
}

static void
kms_pointer_detector_overlay_icon (IplImage * icon,
    gint x, gint y,
    gdouble transparency,
    gboolean saturate, KmsPointerDetector * pointerdetector)
{
  int w, h;
  uchar *row, *image_row;

  row = (uchar *) icon->imageData;
  image_row = (uchar *) pointerdetector->priv->cvImage->imageData +
      (y * pointerdetector->priv->cvImage->widthStep);

  for (h = 0; h < icon->height; h++) {

    uchar *column = row;
    uchar *image_column = image_row + (x * 3);

    for (w = 0; w < icon->width; w++) {
      /* Check if point is inside overlay boundaries */
      if (((w + x) < pointerdetector->priv->cvImage->width)
          && ((w + x) >= 0)) {
        if (((h + y) < pointerdetector->priv->cvImage->height)
            && ((h + y) >= 0)) {

          if (icon->nChannels == 1) {
            *(image_column) = (uchar) (*(column));
            *(image_column + 1) = (uchar) (*(column));
            *(image_column + 2) = (uchar) (*(column));
          } else if (icon->nChannels == 3) {
            *(image_column) = (uchar) (*(column));
            *(image_column + 1) = (uchar) (*(column + 1));
            *(image_column + 2) = (uchar) (*(column + 2));
          } else if (icon->nChannels == 4) {
            double proportion =
                ((double) *(uchar *) (column + 3)) / (double) 255;
            double overlay = transparency * proportion;
            double original = 1 - overlay;

            *image_column =
                (uchar) ((*column * overlay) + (*image_column * original));

            if (saturate) {
              *(image_column + 1) =
                  (uchar) ((255 * overlay) + (*(image_column + 1) * original));
            } else {
              *(image_column + 1) =
                  (uchar) ((*(column + 1) * overlay) + (*(image_column +
                          1) * original));
            }

            *(image_column + 2) =
                (uchar) ((*(column + 2) * overlay) + (*(image_column +
                        2) * original));
          }
        }
      }

      column += icon->nChannels;
      image_column += pointerdetector->priv->cvImage->nChannels;
    }

    row += icon->widthStep;
    image_row += pointerdetector->priv->cvImage->widthStep;
  }
}

static void
kms_pointer_detector_check_pointer_position (KmsPointerDetector *
    pointerdetector)
{
  ButtonStruct *structAux;
  GSList *l;
  int buttonClickedCounter = 0;
  gchar *actualButtonClickedId;

  GST_OBJECT_LOCK (pointerdetector);
  for (l = pointerdetector->priv->buttonsLayoutList; l != NULL; l = l->next) {
    CvPoint upRightCorner;
    CvPoint downLeftCorner;
    CvScalar color;
    gboolean is_active_window;

    structAux = l->data;
    upRightCorner.x = structAux->cvButtonLayout.x;
    upRightCorner.y = structAux->cvButtonLayout.y;
    downLeftCorner.x =
        structAux->cvButtonLayout.x + structAux->cvButtonLayout.width;
    downLeftCorner.y =
        structAux->cvButtonLayout.y + structAux->cvButtonLayout.height;

    if (kms_pointer_detector_check_pointer_into_button
        (&pointerdetector->priv->finalPointerPosition, structAux)) {
      buttonClickedCounter++;

      color = GREEN;
      is_active_window = TRUE;
      actualButtonClickedId = structAux->id;
    } else {
      color = WHITE;
      is_active_window = FALSE;;
    }

    if (pointerdetector->priv->show_windows_layout) {
      if (!is_active_window) {
        if (structAux->inactive_icon != NULL) {
          kms_pointer_detector_overlay_icon (structAux->inactive_icon,
              structAux->cvButtonLayout.x,
              structAux->cvButtonLayout.y,
              structAux->transparency, FALSE, pointerdetector);
        } else {
          cvRectangle (pointerdetector->priv->cvImage, upRightCorner,
              downLeftCorner, color, 1, 8, 0);
        }
      } else {
        if (structAux->active_icon != NULL) {
          kms_pointer_detector_overlay_icon (structAux->active_icon,
              structAux->cvButtonLayout.x,
              structAux->cvButtonLayout.y,
              structAux->transparency, FALSE, pointerdetector);
        } else if (structAux->inactive_icon != NULL) {
          kms_pointer_detector_overlay_icon (structAux->inactive_icon,
              structAux->cvButtonLayout.x,
              structAux->cvButtonLayout.y,
              structAux->transparency, TRUE, pointerdetector);
        } else {
          cvRectangle (pointerdetector->priv->cvImage, upRightCorner,
              downLeftCorner, color, 1, 8, 0);
        }
      }
    }
  }
  GST_OBJECT_UNLOCK (pointerdetector);

  if (buttonClickedCounter == 0) {
    if (pointerdetector->priv->previousButtonClickedId != NULL) {
      GstStructure *s;
      GstMessage *m;

      /* post a message to bus */
      GST_DEBUG ("exit window: %s",
          pointerdetector->priv->previousButtonClickedId);
      if (pointerdetector->priv->putMessage) {
        s = gst_structure_new ("window-out",
            "window", G_TYPE_STRING,
            pointerdetector->priv->previousButtonClickedId, NULL);
        m = gst_message_new_element (GST_OBJECT (pointerdetector), s);
        gst_element_post_message (GST_ELEMENT (pointerdetector), m);
      }
      g_free (pointerdetector->priv->previousButtonClickedId);
      pointerdetector->priv->previousButtonClickedId = NULL;
    }
  } else {
    if (g_strcmp0 (pointerdetector->priv->previousButtonClickedId,
            actualButtonClickedId) != 0) {
      GstStructure *s;
      GstMessage *m;

      /* post a message to bus */
      GST_DEBUG ("into window: %s", actualButtonClickedId);
      if (pointerdetector->priv->putMessage) {
        s = gst_structure_new ("window-in",
            "window", G_TYPE_STRING, actualButtonClickedId, NULL);
        m = gst_message_new_element (GST_OBJECT (pointerdetector), s);
        gst_element_post_message (GST_ELEMENT (pointerdetector), m);
      }
      pointerdetector->priv->previousButtonClickedId =
          g_strdup (actualButtonClickedId);
    }
  }
}

static GstFlowReturn
kms_pointer_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsPointerDetector *pointerdetector = KMS_POINTER_DETECTOR (filter);
  GstMapInfo info;
  IplImage *color_filter;
  IplImage *hsv_image;
  IplImage *hough_image;
  CvMemStorage *storage = NULL;
  CvSeq *circles;
  int distance;
  int best_candidate;
  gint i;

  if ((pointerdetector->priv->x_calibration == 0)
      && (pointerdetector->priv->y_calibration == 0)
      && (pointerdetector->priv->width_calibration == 0)
      && (pointerdetector->priv->height_calibration == 0)) {
    GST_DEBUG ("Calibration area not defined");
    return GST_FLOW_OK;
  }

  pointerdetector->priv->frameSize =
      cvSize (frame->info.width, frame->info.height);
  kms_pointer_detector_initialize_images (pointerdetector, frame);
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  pointerdetector->priv->cvImage->imageData = (char *) info.data;

  cvRectangle (pointerdetector->priv->cvImage,
      cvPoint (pointerdetector->priv->x_calibration,
          pointerdetector->priv->y_calibration),
      cvPoint (pointerdetector->priv->x_calibration
          + pointerdetector->priv->width_calibration,
          pointerdetector->priv->y_calibration
          + pointerdetector->priv->height_calibration), WHITE, 1, 8, 0);

  if ((pointerdetector->priv->h_min == 0) && (pointerdetector->priv->h_max == 0)
      && (pointerdetector->priv->s_min == 0)
      && (pointerdetector->priv->s_max == 0)) {
    goto end;
  }
  //detect the coordenates of the pointer
  hsv_image = cvCreateImage (cvGetSize (pointerdetector->priv->cvImage),
      pointerdetector->priv->cvImage->depth, 3);
  cvCvtColor (pointerdetector->priv->cvImage, hsv_image, CV_BGR2HSV);
  color_filter = cvCreateImage (cvGetSize (pointerdetector->priv->cvImage),
      pointerdetector->priv->cvImage->depth, 1);
  GST_OBJECT_LOCK (pointerdetector);
  cvInRangeS (hsv_image,
      cvScalar (pointerdetector->priv->h_min, pointerdetector->priv->s_min,
          V_MIN, 0), cvScalar (pointerdetector->priv->h_max,
          pointerdetector->priv->s_max, V_MAX, 0), color_filter);
  GST_OBJECT_UNLOCK (pointerdetector);
  cvMorphologyEx (color_filter, color_filter, NULL,
      pointerdetector->priv->kernel1, CV_MOP_CLOSE, 1);
  cvMorphologyEx (color_filter, color_filter, NULL,
      pointerdetector->priv->kernel2, CV_MOP_OPEN, 1);

  hough_image = cvCloneImage (color_filter);
  cvSmooth (hough_image, hough_image, CV_GAUSSIAN, 15, 15, 0, 0);
  storage = cvCreateMemStorage (0);
  circles =
      cvHoughCircles (hough_image, storage, CV_HOUGH_GRADIENT, 2,
      color_filter->height / 10, 100, 40, 0, 0);

  if (circles->total == 0) {
    goto checkPoint;
  }

  if ((circles->total == 1)) {
    float *p = (float *) cvGetSeqElem (circles, 0);

    pointerdetector->priv->finalPointerPosition.x = cvRound (p[0]);
    pointerdetector->priv->finalPointerPosition.y = cvRound (p[1]);
    goto checkPoint;
  }

  distance = 0;
  best_candidate = 0;

  for (i = 0; i < circles->total; i++) {
    int current_distance;
    float *p = (float *) cvGetSeqElem (circles, i);

    if (distance == 0) {
      best_candidate = i;
      continue;
    }
    current_distance =
        sqrt (((abs (pointerdetector->priv->finalPointerPosition.x - p[0]) -
                abs (pointerdetector->priv->finalPointerPosition.x - p[0]))
            * (abs (pointerdetector->priv->finalPointerPosition.x - p[0]) -
                abs (pointerdetector->priv->finalPointerPosition.x - p[0])))
        + ((abs (pointerdetector->priv->finalPointerPosition.y - p[1]) -
                abs (pointerdetector->priv->finalPointerPosition.y - p[1]))
            * (abs (pointerdetector->priv->finalPointerPosition.y - p[1]) -
                abs (pointerdetector->priv->finalPointerPosition.y - p[1]))));

    if (current_distance < distance) {
      best_candidate = i;
    }
  }
  float *p = (float *) cvGetSeqElem (circles, best_candidate);

  pointerdetector->priv->finalPointerPosition.x = p[0];
  pointerdetector->priv->finalPointerPosition.y = p[1];

checkPoint:
  if (storage != NULL) {
    cvReleaseMemStorage (&storage);
  }

  kms_pointer_detector_check_pointer_position (pointerdetector);

  GST_OBJECT_LOCK (pointerdetector);
  cvCircle (pointerdetector->priv->cvImage,
      pointerdetector->priv->finalPointerPosition, 10.0, cvScalar (0, 0, 255,
          0), -1, 8, 0);
  GST_OBJECT_UNLOCK (pointerdetector);

  pointerdetector->priv->iteration++;
  if (hsv_image != NULL) {
    cvReleaseImage (&hsv_image);
  }
  if (color_filter != NULL) {
    cvReleaseImage (&color_filter);
  }
  if (hough_image != NULL) {
    cvReleaseImage (&hough_image);
  }

end:
  gst_buffer_unmap (frame->buffer, &info);
  return GST_FLOW_OK;
}

static void
kms_pointer_detector_class_init (KmsPointerDetectorClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstBaseTransformClass *base_transform_class =
      GST_BASE_TRANSFORM_CLASS (klass);
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
      "Pointer detector element", "Video/Filter",
      "Detects pointer an raises events with its position",
      "Francisco Rivero <fj.riverog@gmail.com>");

  klass->calibrate_color =
      GST_DEBUG_FUNCPTR (kms_pointer_detector_calibrate_color);

  gobject_class->set_property = kms_pointer_detector_set_property;
  gobject_class->get_property = kms_pointer_detector_get_property;
  gobject_class->finalize = kms_pointer_detector_finalize;
  base_transform_class->start = GST_DEBUG_FUNCPTR (kms_pointer_detector_start);
  base_transform_class->stop = GST_DEBUG_FUNCPTR (kms_pointer_detector_stop);
  video_filter_class->set_info =
      GST_DEBUG_FUNCPTR (kms_pointer_detector_set_info);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_pointer_detector_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_WINDOWS_LAYOUT,
      g_param_spec_boxed ("windows-layout", "windows layout",
          "supply the positions and dimensions of windows into the main window",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_MESSAGE,
      g_param_spec_boolean ("message", "message",
          "Put a window-in or window-out message in the bus if "
          "an object enters o leaves a window", TRUE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_SHOW_WINDOWS_LAYOUT,
      g_param_spec_boolean ("show-windows-layout", "show windows layout",
          "show windows layout over the image", TRUE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_CALIBRATION_AREA,
      g_param_spec_boxed ("calibration-area", "calibration area",
          "define the window used to calibrate the color to track",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  kms_pointer_detector_signals[SIGNAL_CALIBRATE_COLOR] =
      g_signal_new ("calibrate-color",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPointerDetectorClass, calibrate_color), NULL, NULL,
      __kms_core_marshal_VOID__VOID, G_TYPE_NONE, 0);

  g_type_class_add_private (klass, sizeof (KmsPointerDetectorPrivate));
}

gboolean
kms_pointer_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_POINTER_DETECTOR);
}
