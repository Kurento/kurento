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

#include "kmsimageoverlay.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

#include <opencv/highgui.h>
#include <gstreamer-1.0/gst/video/gstvideofilter.h>
#include <libsoup/soup.h>

#define TEMP_PATH "/tmp/XXXXXX"
#define BLUE_COLOR (cvScalar (255, 0, 0, 0))
#define SRC_OVERLAY ((double)1)

#define PLUGIN_NAME "imageoverlay"

GST_DEBUG_CATEGORY_STATIC (kms_image_overlay_debug_category);
#define GST_CAT_DEFAULT kms_image_overlay_debug_category

#define KMS_IMAGE_OVERLAY_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_IMAGE_OVERLAY,                  \
    KmsImageOverlayPrivate                   \
  )                                          \
)

enum
{
  PROP_0,
  PROP_IMAGE_TO_OVERLAY,
  PROP_SHOW_DEBUG_INFO
};

struct _KmsImageOverlayPrivate
{
  IplImage *cvImage, *costume;
  GstStructure *image_to_overlay;

  gdouble offsetXPercent, offsetYPercent, widthPercent, heightPercent;
  gboolean show_debug_info, dir_created;
  gchar *dir;
  GstClockTime dts, pts;

  CvMemStorage *pStorageFace;
  CvSeq *pFaceRectSeq;
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsImageOverlay, kms_image_overlay,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_image_overlay_debug_category, PLUGIN_NAME,
        0, "debug category for imageoverlay element"));

static gboolean
kms_image_overlay_is_valid_uri (const gchar * url)
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
  }
  fwrite (msg->response_body->data, 1, msg->response_body->length, dst);
  fclose (dst);
}

static void
kms_image_overlay_load_image_to_overlay (KmsImageOverlay * imageoverlay)
{
  gchar *url;
  IplImage *costumeAux;

  if (!imageoverlay->priv->dir_created) {
    gchar d[] = TEMP_PATH;
    gchar *aux = g_mkdtemp (d);

    imageoverlay->priv->dir = g_strdup (aux);
    imageoverlay->priv->dir_created = TRUE;
  }

  gst_structure_get (imageoverlay->priv->image_to_overlay, "offsetXPercent",
      G_TYPE_INT, &imageoverlay->priv->offsetXPercent, NULL);
  gst_structure_get (imageoverlay->priv->image_to_overlay, "offsetYPercent",
      G_TYPE_INT, &imageoverlay->priv->offsetYPercent, NULL);
  gst_structure_get (imageoverlay->priv->image_to_overlay, "widthPercent",
      G_TYPE_INT, &imageoverlay->priv->widthPercent, NULL);
  gst_structure_get (imageoverlay->priv->image_to_overlay, "heightPercent",
      G_TYPE_INT, &imageoverlay->priv->heightPercent, NULL);
  gst_structure_get (imageoverlay->priv->image_to_overlay, "url", G_TYPE_STRING,
      &url, NULL);

  costumeAux = cvLoadImage (url, CV_LOAD_IMAGE_UNCHANGED);

  if (costumeAux != NULL) {
    GST_DEBUG ("Image loaded from file");
    goto end;
  }

  if (kms_image_overlay_is_valid_uri (url)) {
    gchar *file_name =
        g_strconcat (imageoverlay->priv->dir, "/image.png", NULL);
    load_from_url (file_name, url);
    costumeAux = cvLoadImage (file_name, CV_LOAD_IMAGE_UNCHANGED);
    g_free (file_name);
  }

  if (costumeAux == NULL) {
    GST_DEBUG ("Image not loaded");
  } else {
    GST_DEBUG ("Image loaded from URL");
  }

end:

  GST_OBJECT_LOCK (imageoverlay);

  if (imageoverlay->priv->costume != NULL)
    cvReleaseImage (&imageoverlay->priv->costume);

  if (costumeAux != NULL) {
    imageoverlay->priv->costume = costumeAux;
  }

  GST_OBJECT_UNLOCK (imageoverlay);

  g_free (url);
}

static void
kms_image_overlay_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (object);

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      imageoverlay->priv->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_IMAGE_TO_OVERLAY:
      if (imageoverlay->priv->image_to_overlay != NULL)
        gst_structure_free (imageoverlay->priv->image_to_overlay);

      imageoverlay->priv->image_to_overlay = g_value_dup_boxed (value);
      kms_image_overlay_load_image_to_overlay (imageoverlay);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_image_overlay_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (object);

  GST_DEBUG_OBJECT (imageoverlay, "get_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, imageoverlay->priv->show_debug_info);
      break;
    case PROP_IMAGE_TO_OVERLAY:
      if (imageoverlay->priv->image_to_overlay == NULL) {
        imageoverlay->priv->image_to_overlay =
            gst_structure_new_empty ("image_to_overlay");
      }
      g_value_set_boxed (value, imageoverlay->priv->image_to_overlay);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_image_overlay_display_detections_overlay_img (KmsImageOverlay *
    imageoverlay)
{
  int i;

  for (i = 0;
      i <
      (imageoverlay->priv->pFaceRectSeq ? imageoverlay->priv->
          pFaceRectSeq->total : 0); i++) {
    CvRect *r = (CvRect *) cvGetSeqElem (imageoverlay->priv->pFaceRectSeq, i);
    IplImage *costumeAux;
    int w, h;
    uchar *row, *image_row;

    if (imageoverlay->priv->costume == NULL) {
      gint radius = cvRound ((r->width + r->height) * 0.25);

      cvCircle (imageoverlay->priv->cvImage,
          cvPoint (r->x + (r->width / 2), r->y + (r->height / 2)),
          radius, BLUE_COLOR, 4, 8, 0);
      continue;
    }

    r->x = r->x - (r->width * (imageoverlay->priv->offsetXPercent / 100.0));
    r->y = r->y - (r->height * (imageoverlay->priv->offsetXPercent / 100));
    r->height = r->height * (imageoverlay->priv->heightPercent / 100);
    r->width = r->width * (imageoverlay->priv->widthPercent / 100);

    costumeAux = cvCreateImage (cvSize (r->width, r->height),
        imageoverlay->priv->cvImage->depth, 4);
    cvResize (imageoverlay->priv->costume, costumeAux, CV_INTER_LINEAR);

    row = (uchar *) costumeAux->imageData;
    image_row = (uchar *) imageoverlay->priv->cvImage->imageData +
        (r->y * imageoverlay->priv->cvImage->widthStep);

    for (h = 0; h < costumeAux->height; h++) {

      uchar *column = row;
      uchar *image_column = image_row + (r->x * 3);

      for (w = 0; w < costumeAux->width; w++) {
        /* Check if point is inside overlay boundaries */
        if (((w + r->x) < imageoverlay->priv->cvImage->width)
            && ((w + r->x) >= 0)) {
          if (((h + r->y) < imageoverlay->priv->cvImage->height)
              && ((h + r->y) >= 0)) {

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

        column += 4;
        image_column += 3;
      }

      row += costumeAux->widthStep;
      image_row += imageoverlay->priv->cvImage->widthStep;
    }

    cvReleaseImage (&costumeAux);
  }
}

static void
kms_image_overlay_initialize_images (KmsImageOverlay * imageoverlay,
    GstVideoFrame * frame)
{
  if (imageoverlay->priv->cvImage == NULL) {
    imageoverlay->priv->cvImage =
        cvCreateImage (cvSize (frame->info.width, frame->info.height),
        IPL_DEPTH_8U, 3);

  } else if ((imageoverlay->priv->cvImage->width != frame->info.width)
      || (imageoverlay->priv->cvImage->height != frame->info.height)) {

    cvReleaseImage (&imageoverlay->priv->cvImage);
    imageoverlay->priv->cvImage =
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
kms_image_overlay_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (filter);
  GstMapInfo info;

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  kms_image_overlay_initialize_images (imageoverlay, frame);
  imageoverlay->priv->cvImage->imageData = (char *) info.data;

  GST_OBJECT_LOCK (imageoverlay);
  if (imageoverlay->priv->pts == frame->buffer->pts) {
    if (imageoverlay->priv->costume != NULL) {
      kms_image_overlay_display_detections_overlay_img (imageoverlay);
    }
  }
  GST_OBJECT_UNLOCK (imageoverlay);

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
kms_image_overlay_dispose (GObject * object)
{
  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_image_overlay_parent_class)->dispose (object);
}

static void
kms_image_overlay_finalize (GObject * object)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (object);

  if (imageoverlay->priv->cvImage != NULL)
    cvReleaseImage (&imageoverlay->priv->cvImage);

  if (imageoverlay->priv->costume != NULL)
    cvReleaseImage (&imageoverlay->priv->costume);

  if (imageoverlay->priv->image_to_overlay != NULL)
    gst_structure_free (imageoverlay->priv->image_to_overlay);

  if (imageoverlay->priv->dir_created) {
    remove_recursive (imageoverlay->priv->dir);
    g_free (imageoverlay->priv->dir);
  }

  if (imageoverlay->priv->pStorageFace != NULL)
    cvClearMemStorage (imageoverlay->priv->pStorageFace);
  if (imageoverlay->priv->pFaceRectSeq != NULL)
    cvClearSeq (imageoverlay->priv->pFaceRectSeq);

  cvReleaseMemStorage (&imageoverlay->priv->pStorageFace);

  G_OBJECT_CLASS (kms_image_overlay_parent_class)->finalize (object);
}

static void
kms_image_overlay_init (KmsImageOverlay * imageoverlay)
{
  imageoverlay->priv = KMS_IMAGE_OVERLAY_GET_PRIVATE (imageoverlay);

  imageoverlay->priv->show_debug_info = FALSE;
  imageoverlay->priv->cvImage = NULL;
  imageoverlay->priv->costume = NULL;
  imageoverlay->priv->dir_created = FALSE;

  imageoverlay->priv->pStorageFace = cvCreateMemStorage (0);
  imageoverlay->priv->pFaceRectSeq =
      cvCreateSeq (0, sizeof (CvSeq), sizeof (CvRect),
      imageoverlay->priv->pStorageFace);
}

static gboolean
kms_image_overlay_sink_events (GstBaseTransform * trans, GstEvent * event)
{
  gboolean ret;
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (trans);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CUSTOM_DOWNSTREAM:
    {
      const GstStructure *faces;
      gint len;

      GST_OBJECT_LOCK (imageoverlay);

      faces = gst_event_get_structure (event);
      len = gst_structure_n_fields (faces);

      if (len != 0) {
        cvClearSeq (imageoverlay->priv->pFaceRectSeq);
      }

      for (gint aux = 0; aux < len; aux++) {
        GstStructure *face;
        gboolean ret;
        guint x, y, width, height;

        const gchar *name = gst_structure_nth_field_name (faces, aux);

        ret = gst_structure_get (faces, name, GST_TYPE_STRUCTURE, &face, NULL);

        if (ret) {
          CvRect aux;

          gst_structure_get (face, "x", G_TYPE_UINT, &x, NULL);
          gst_structure_get (face, "y", G_TYPE_UINT, &y, NULL);
          gst_structure_get (face, "width", G_TYPE_UINT, &width, NULL);
          gst_structure_get (face, "height", G_TYPE_UINT, &height, NULL);
          gst_structure_get (face, "dts", G_TYPE_UINT64,
              &imageoverlay->priv->dts, NULL);
          gst_structure_get (face, "pts", G_TYPE_UINT64,
              &imageoverlay->priv->pts, NULL);
          gst_structure_free (face);
          aux = cvRect (x, y, width, height);
          cvSeqPush (imageoverlay->priv->pFaceRectSeq, &aux);
        }
      }

      GST_OBJECT_UNLOCK (imageoverlay);
      break;
    }
    default:
      break;
  }
  ret = gst_pad_push_event (trans->srcpad, event);
  return ret;
}

static void
kms_image_overlay_class_init (KmsImageOverlayClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  GST_DEBUG ("class init");

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "image overlay element", "Video/Filter",
      "Set a defined image in a defined position",
      "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->set_property = kms_image_overlay_set_property;
  gobject_class->get_property = kms_image_overlay_get_property;
  gobject_class->dispose = kms_image_overlay_dispose;
  gobject_class->finalize = kms_image_overlay_finalize;

  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_image_overlay_transform_frame_ip);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-region", "show debug region",
          "show evaluation regions over the image", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_IMAGE_TO_OVERLAY,
      g_param_spec_boxed ("image-to-overlay", "image to overlay",
          "set the url of the image to overlay in the image",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  klass->base_facedetector_class.parent_class.sink_event =
      GST_DEBUG_FUNCPTR (kms_image_overlay_sink_events);

  g_type_class_add_private (klass, sizeof (KmsImageOverlayPrivate));
}

gboolean
kms_image_overlay_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_IMAGE_OVERLAY);
}
