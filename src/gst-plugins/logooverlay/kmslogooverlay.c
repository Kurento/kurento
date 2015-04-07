/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#define _XOPEN_SOURCE 500

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmslogooverlay.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

#include <opencv/cv.h>

#include <opencv/highgui.h>
#include <gstreamer-1.0/gst/video/gstvideofilter.h>
#include <libsoup/soup.h>

#define TEMP_PATH "/tmp/XXXXXX"
#define SRC_OVERLAY ((double)1)

#define PLUGIN_NAME "logooverlay"

GST_DEBUG_CATEGORY_STATIC (kms_logo_overlay_debug_category);
#define GST_CAT_DEFAULT kms_logo_overlay_debug_category

#define KMS_LOGO_OVERLAY_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_LOGO_OVERLAY,                  \
    KmsLogoOverlayPrivate                   \
  )                                          \
)

enum
{
  PROP_0,
  PROP_IMAGES_TO_OVERLAY
};

struct _KmsLogoOverlayPrivate
{
  IplImage *cv_image;

  GstStructure *image_layout;
  GSList *image_layout_list;

  gchar *dir;
  gboolean configured;
};

typedef struct _ImageStruct
{
  gfloat offsetXPercent, offsetYPercent, widthPercent, heightPercent;
  gchar *id;
  IplImage *active_icon;
} ImageStruct;

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsLogoOverlay, kms_logo_overlay,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_logo_overlay_debug_category, PLUGIN_NAME,
        0, "debug category for logooverlay element"));

static void
dispose_image_struct (gpointer data)
{
  ImageStruct *aux = data;

  if (aux->id != NULL)
    g_free (aux->id);

  if (aux->active_icon != NULL)
    cvReleaseImage (&aux->active_icon);

  g_free (aux);
}

static void
kms_logo_overlay_dispose_image_layout_list (KmsLogoOverlay * logooverlay)
{
  g_slist_free_full (logooverlay->priv->image_layout_list,
      dispose_image_struct);
  logooverlay->priv->image_layout_list = NULL;
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
load_image (gchar * uri, gchar * dir, gchar * image_name)
{
  IplImage *aux;

  aux = cvLoadImage (uri, CV_LOAD_IMAGE_UNCHANGED);
  if (aux == NULL) {
    if (is_valid_uri (uri)) {
      gchar *file_name;

      file_name = g_strconcat (dir, "/", image_name, ".png", NULL);
      load_from_url (file_name, uri);
      aux = cvLoadImage (file_name, CV_LOAD_IMAGE_UNCHANGED);
      g_remove (file_name);
      g_free (file_name);
    }
  }

  return aux;
}

static void
kms_logo_overlay_load_image_layout (KmsLogoOverlay * logooverlay)
{
  int aux, len;
  gchar *uri;

  if (logooverlay->priv->image_layout_list != NULL) {
    kms_logo_overlay_dispose_image_layout_list (logooverlay);
  }

  len = gst_structure_n_fields (logooverlay->priv->image_layout);

  for (aux = 0; aux < len; aux++) {
    const gchar *name =
        gst_structure_nth_field_name (logooverlay->priv->image_layout,
        aux);
    GstStructure *image;
    gboolean ret;

    ret =
        gst_structure_get (logooverlay->priv->image_layout, name,
        GST_TYPE_STRUCTURE, &image, NULL);
    if (ret) {
      ImageStruct *structAux = g_malloc0 (sizeof (ImageStruct));
      IplImage *aux = NULL;

      gst_structure_get (image, "offsetXPercent", G_TYPE_FLOAT,
          &structAux->offsetXPercent, NULL);
      gst_structure_get (image, "offsetYPercent", G_TYPE_FLOAT,
          &structAux->offsetYPercent, NULL);
      gst_structure_get (image, "widthPercent", G_TYPE_FLOAT,
          &structAux->widthPercent, NULL);
      gst_structure_get (image, "heightPercent", G_TYPE_FLOAT,
          &structAux->heightPercent, NULL);
      gst_structure_get (image, "id", G_TYPE_STRING, &structAux->id, NULL);
      gst_structure_get (image, "uri", G_TYPE_STRING, &uri, NULL);

      if ((structAux->widthPercent == 0) || (structAux->heightPercent == 0)) {
        continue;
      }

      aux = load_image (uri, logooverlay->priv->dir, structAux->id);

      if (aux != NULL) {
        structAux->active_icon =
            cvCreateImage (cvSize (logooverlay->priv->cv_image->width *
                (structAux->widthPercent),
                logooverlay->priv->cv_image->height *
                (structAux->heightPercent)), aux->depth, aux->nChannels);
        cvResize (aux, structAux->active_icon, CV_INTER_CUBIC);
        cvReleaseImage (&aux);
      } else {
        structAux->active_icon = NULL;
        GST_WARNING ("Image %s not loaded", uri);
      }

      logooverlay->priv->image_layout_list =
          g_slist_append (logooverlay->priv->image_layout_list, structAux);
      gst_structure_free (image);

      g_free (uri);
    }
  }
}

static void
kms_logo_overlay_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsLogoOverlay *logooverlay = KMS_LOGO_OVERLAY (object);

  GST_OBJECT_LOCK (logooverlay);

  switch (property_id) {
    case PROP_IMAGES_TO_OVERLAY:
      if (logooverlay->priv->image_layout != NULL) {
        gst_structure_free (logooverlay->priv->image_layout);
      }

      logooverlay->priv->image_layout = g_value_dup_boxed (value);
      if (logooverlay->priv->cv_image != NULL) {
        kms_logo_overlay_load_image_layout (logooverlay);
        logooverlay->priv->configured = TRUE;
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  GST_OBJECT_UNLOCK (logooverlay);
}

static void
kms_logo_overlay_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsLogoOverlay *logooverlay = KMS_LOGO_OVERLAY (object);

  GST_OBJECT_LOCK (logooverlay);

  switch (property_id) {
    case PROP_IMAGES_TO_OVERLAY:
      if (logooverlay->priv->image_layout == NULL) {
        logooverlay->priv->image_layout = gst_structure_new_empty ("images");
      }
      g_value_set_boxed (value, logooverlay->priv->image_layout);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  GST_OBJECT_UNLOCK (logooverlay);
}

static void
kms_logo_overlay_display_overlay_img (KmsLogoOverlay * logooverlay,
    int x_position, int y_position, IplImage * image)
{
  int w, h;
  uchar *row, *image_row;

  row = (uchar *) image->imageData;
  image_row = (uchar *) logooverlay->priv->cv_image->imageData +
      (y_position * logooverlay->priv->cv_image->widthStep);

  for (h = 0; h < image->height; h++) {

    uchar *column = row;
    uchar *image_column = image_row + (x_position * 3);

    for (w = 0; w < image->width; w++) {
      /* Check if point is inside overlay boundaries */
      if (((w + x_position) < logooverlay->priv->cv_image->width)
          && ((w + x_position) >= 0)) {
        if (((h + y_position) < logooverlay->priv->cv_image->height)
            && ((h + y_position) >= 0)) {

          if (image->nChannels == 1) {
            *(image_column) = (uchar) (*(column));
            *(image_column + 1) = (uchar) (*(column));
            *(image_column + 2) = (uchar) (*(column));
          } else if (image->nChannels == 3) {
            *(image_column) = (uchar) (*(column));
            *(image_column + 1) = (uchar) (*(column + 1));
            *(image_column + 2) = (uchar) (*(column + 2));
          } else if (image->nChannels == 4) {
            double proportion =
                ((double) *(uchar *) (column + 3)) / (double) 255;
            double overlay = SRC_OVERLAY * proportion;
            double original = 1 - overlay;

            *image_column =
                (uchar) ((*column * overlay) + (*image_column * original));
            *(image_column + 1) =
                (uchar) ((*(column + 1) * overlay) + (*(image_column +
                        1) * original));
            *(image_column + 2) =
                (uchar) ((*(column + 2) * overlay) + (*(image_column +
                        2) * original));
          }
        }
      }

      column += image->nChannels;
      image_column += logooverlay->priv->cv_image->nChannels;
    }

    row += image->widthStep;
    image_row += logooverlay->priv->cv_image->widthStep;
  }
}

static void
kms_logo_overlay_initialize_images (KmsLogoOverlay * logooverlay,
    GstVideoFrame * frame)
{
  if (logooverlay->priv->cv_image == NULL) {
    logooverlay->priv->cv_image =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
    if ((!logooverlay->priv->configured)
        && (logooverlay->priv->image_layout != NULL)) {
      kms_logo_overlay_load_image_layout (logooverlay);
      logooverlay->priv->configured = TRUE;
    }

  } else if ((logooverlay->priv->cv_image->width != frame->info.width)
      || (logooverlay->priv->cv_image->height != frame->info.height)) {

    cvReleaseImage (&logooverlay->priv->cv_image);
    logooverlay->priv->cv_image =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);
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

static GstFlowReturn
kms_logo_overlay_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsLogoOverlay *logooverlay = KMS_LOGO_OVERLAY (filter);
  GstMapInfo info;
  GSList *l;
  ImageStruct *structAux;

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  GST_OBJECT_LOCK (logooverlay);
  kms_logo_overlay_initialize_images (logooverlay, frame);
  logooverlay->priv->cv_image->imageData = (char *) info.data;

  for (l = logooverlay->priv->image_layout_list; l != NULL; l = l->next) {
    structAux = l->data;

    if ((structAux->widthPercent == 0) || (structAux->heightPercent == 0)) {
      continue;
    }

    kms_logo_overlay_display_overlay_img (logooverlay,
        logooverlay->priv->cv_image->width *
        (structAux->offsetXPercent),
        logooverlay->priv->cv_image->height *
        (structAux->offsetYPercent), structAux->active_icon);
  }

  GST_OBJECT_UNLOCK (logooverlay);

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
kms_logo_overlay_dispose (GObject * object)
{
  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_logo_overlay_parent_class)->dispose (object);
}

static void
kms_logo_overlay_finalize (GObject * object)
{
  KmsLogoOverlay *logooverlay = KMS_LOGO_OVERLAY (object);

  remove_recursive (logooverlay->priv->dir);
  g_free (logooverlay->priv->dir);

  if (logooverlay->priv->cv_image != NULL) {
    cvReleaseImage (&logooverlay->priv->cv_image);
  }

  if (logooverlay->priv->image_layout_list != NULL) {
    kms_logo_overlay_dispose_image_layout_list (logooverlay);
  }

  if (logooverlay->priv->image_layout != NULL) {
    gst_structure_free (logooverlay->priv->image_layout);
  }

  G_OBJECT_CLASS (kms_logo_overlay_parent_class)->finalize (object);
}

static void
kms_logo_overlay_init (KmsLogoOverlay * logooverlay)
{
  gchar d[] = TEMP_PATH;
  gchar *aux = g_mkdtemp (d);

  logooverlay->priv = KMS_LOGO_OVERLAY_GET_PRIVATE (logooverlay);

  logooverlay->priv->dir = g_strdup (aux);

  logooverlay->priv->cv_image = NULL;
  logooverlay->priv->image_layout = NULL;
  logooverlay->priv->image_layout_list = NULL;
  logooverlay->priv->configured = FALSE;
}

static void
kms_logo_overlay_class_init (KmsLogoOverlayClass * klass)
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
      "logo overlay element", "Video/Filter",
      "Show a set of images in a set of defined positions",
      "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_logo_overlay_set_property;
  gobject_class->get_property = kms_logo_overlay_get_property;
  gobject_class->dispose = kms_logo_overlay_dispose;
  gobject_class->finalize = kms_logo_overlay_finalize;

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_logo_overlay_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_IMAGES_TO_OVERLAY,
      g_param_spec_boxed ("images-to-overlay", "images to overlay",
          "images to overlay in the video",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_type_class_add_private (klass, sizeof (KmsLogoOverlayPrivate));
}

gboolean
kms_logo_overlay_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_LOGO_OVERLAY);
}
