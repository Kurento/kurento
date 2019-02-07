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

#include "kmsimageoverlay.h"

#include <gst/gst.h>
#include <gst/video/video.h>
#include <gst/video/gstvideofilter.h>
#include <glib/gstdio.h>
#include <ftw.h>
#include <string.h>
#include <errno.h>

#include <opencv2/opencv_modules.hpp>
#include <opencv/cv.h>
#include <opencv/highgui.h>

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

  GQueue *events_queue;
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

/* Converts GTlsCertificateFlags to a translated string representation
 * of the first set error flag. */
static const gchar *
tls_certificate_flags_to_reason (GTlsCertificateFlags flags)
{
  if (flags & G_TLS_CERTIFICATE_UNKNOWN_CA) {
    return "The signing Certificate Authority is not known";
  } else if (flags & G_TLS_CERTIFICATE_BAD_IDENTITY) {
    return "The certificate was not issued to this domain";
  } else if (flags & G_TLS_CERTIFICATE_NOT_ACTIVATED) {
    return "The certificate is not valid yet; check that your"
        " computer's date and time are accurate";
  } else if (flags & G_TLS_CERTIFICATE_EXPIRED) {
    return "The certificate has expired";
  } else if (flags & G_TLS_CERTIFICATE_REVOKED) {
    return "The certificate has been revoked";
  } else if (flags & G_TLS_CERTIFICATE_INSECURE) {
    return "The certificate's algorithm is considered insecure";
  } else {
    /* Also catches G_TLS_CERTIFICATE_GENERIC_ERROR here */
    return "An unknown certificate error occurred";
  }
}

static gboolean
load_from_url (gchar * file_name, gchar * url)
{
  SoupSession *session;
  SoupMessage *msg;
  FILE *dst;
  gboolean ok = FALSE;

  session = soup_session_new_with_options (
      SOUP_SESSION_SSL_USE_SYSTEM_CA_FILE, TRUE,
      SOUP_SESSION_SSL_STRICT, FALSE,
      NULL);

  // Enable logging in 'libsoup' library
  if (g_strcmp0 (g_getenv ("SOUP_DEBUG"), "1") >= 0) {
    GST_INFO ("Enable debug logging in 'libsoup' library");
    SoupLogger *logger = soup_logger_new (SOUP_LOGGER_LOG_HEADERS, -1);
    soup_session_add_feature (session, SOUP_SESSION_FEATURE (logger));
  }

  msg = soup_message_new ("GET", url);
  if (!msg) {
    GST_ERROR ("Cannot parse URL: %s", url);
    goto end;
  }

  GST_INFO ("HTTP blocking request BEGIN, URL: %s", url);
  soup_session_send_message (session, msg);
  GST_INFO ("HTTP blocking request END");

  if (!SOUP_STATUS_IS_SUCCESSFUL (msg->status_code)) {
    GST_ERROR ("HTTP error code %u: %s", msg->status_code, msg->reason_phrase);

    if (msg->status_code == SOUP_STATUS_SSL_FAILED) {
      GTlsCertificate *certificate;
      GTlsCertificateFlags errors;

      soup_message_get_https_status (msg, &certificate, &errors);
      GST_ERROR ("SSL error code 0x%X: %s", errors,
          tls_certificate_flags_to_reason (errors));
    }

    goto end;
  } else {
    // "ssl-strict" is FALSE, so HTTP status will be OK even if HTTPS fails;
    // in that case, issue a warning.
    GTlsCertificate *certificate;
    GTlsCertificateFlags errors;

    if (soup_message_get_https_status (msg, &certificate, &errors)
        && errors != 0) {
      GST_WARNING ("HTTPS is NOT SECURE, error 0x%X: %s", errors,
          tls_certificate_flags_to_reason (errors));
    }
  }

  if (msg->response_body->length <= 0) {
    GST_ERROR ("Write 0 bytes: No data contained in HTTP response");
    goto end;
  }

  dst = fopen (file_name, "w+");
  if (!dst) {
    GST_ERROR ("Cannot create temp file: %s", file_name);
    goto end;
  }

  GST_DEBUG ("Write %ld bytes to temp file: %s",
      msg->response_body->length, file_name);
  fwrite (msg->response_body->data, 1, msg->response_body->length, dst);

  if (fclose (dst) != 0) {
    GST_ERROR ("Error writing temp file: %s", file_name);
    goto end;
  }

  ok = TRUE;

end:
  g_object_unref (msg);
  g_object_unref (session);
  return ok;
}

static void
kms_image_overlay_load_image_to_overlay (KmsImageOverlay * imageoverlay)
{
  gchar *url = NULL;
  gboolean fields_ok = TRUE;

  IplImage *imageAux = NULL;
  gchar *file_name = NULL;

  fields_ok = fields_ok
      && gst_structure_get (imageoverlay->priv->image_to_overlay,
      "offsetXPercent", G_TYPE_DOUBLE, &imageoverlay->priv->offsetXPercent,
      NULL);
  fields_ok = fields_ok
      && gst_structure_get (imageoverlay->priv->image_to_overlay,
      "offsetYPercent", G_TYPE_DOUBLE, &imageoverlay->priv->offsetYPercent,
      NULL);
  fields_ok = fields_ok
      && gst_structure_get (imageoverlay->priv->image_to_overlay,
      "widthPercent", G_TYPE_DOUBLE, &imageoverlay->priv->widthPercent, NULL);
  fields_ok = fields_ok
      && gst_structure_get (imageoverlay->priv->image_to_overlay,
      "heightPercent", G_TYPE_DOUBLE, &imageoverlay->priv->heightPercent, NULL);
  fields_ok = fields_ok
      && gst_structure_get (imageoverlay->priv->image_to_overlay, "url",
      G_TYPE_STRING, &url, NULL);

  if (!fields_ok) {
    GST_ERROR_OBJECT (imageoverlay, "Invalid image structure received");
    goto end;
  }

  if (url == NULL) {
    GST_INFO ("Unset the image overlay");
    goto end;
  } else {
    GST_INFO ("Try to load image: %s", url);
  }

  imageAux = cvLoadImage (url, CV_LOAD_IMAGE_UNCHANGED);
  if (imageAux) {
    GST_INFO ("Loaded successfully from local file");
    goto end;
  } else {
    GST_INFO ("Not a local file, try to download first");
  }

  if (!imageoverlay->priv->dir_created) {
    gchar *d = g_strdup (TEMP_PATH);

    imageoverlay->priv->dir = g_mkdtemp (d);
    if (!imageoverlay->priv->dir) {
      GST_ERROR ("Cannot create temp dir: %s", TEMP_PATH);
      goto end;
    }

    imageoverlay->priv->dir_created = TRUE;

    GST_DEBUG ("Created temp dir: %s", imageoverlay->priv->dir);
  }

  file_name = g_strconcat (imageoverlay->priv->dir, "/image.png", NULL);

  if (!load_from_url (file_name, url)) {
    GST_ERROR ("Failed downloading from URL");
    goto end;
  }

  imageAux = cvLoadImage (file_name, CV_LOAD_IMAGE_UNCHANGED);
  if (!imageAux) {
    GST_ERROR ("Failed loading from URL");
    goto end;
  }

  GST_INFO ("Loaded successfully from URL");

end:
  if (file_name) {
    g_remove (file_name);
    g_free (file_name);
  }

  if (imageoverlay->priv->costume != NULL) {
    cvReleaseImage (&imageoverlay->priv->costume);
    imageoverlay->priv->costume = NULL;
  }

  if (imageAux != NULL) {
    imageoverlay->priv->costume = imageAux;
  }

  g_free (url);
}

static void
kms_image_overlay_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (object);

  GST_OBJECT_LOCK (imageoverlay);

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
  GST_OBJECT_UNLOCK (imageoverlay);
}

static void
kms_image_overlay_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (object);

  GST_DEBUG_OBJECT (imageoverlay, "get_property");

  GST_OBJECT_LOCK (imageoverlay);

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
  GST_OBJECT_UNLOCK (imageoverlay);
}

static void
kms_image_overlay_display_detections_overlay_img (KmsImageOverlay *
    imageoverlay, const GSList * faces_list)
{
  const GSList *iterator = NULL;

  for (iterator = faces_list; iterator; iterator = iterator->next) {
    CvRect *r = iterator->data;
    IplImage *costumeAux;
    int w, h;
    uchar *row, *image_row;

    if ((imageoverlay->priv->heightPercent == 0) ||
        (imageoverlay->priv->widthPercent == 0)) {
      continue;
    }

    r->x = r->x + (r->width * (imageoverlay->priv->offsetXPercent));
    r->y = r->y + (r->height * (imageoverlay->priv->offsetYPercent));
    r->height = r->height * (imageoverlay->priv->heightPercent);
    r->width = r->width * (imageoverlay->priv->widthPercent);

    costumeAux = cvCreateImage (cvSize (r->width, r->height),
        imageoverlay->priv->costume->depth,
        imageoverlay->priv->costume->nChannels);
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

            if (imageoverlay->priv->costume->nChannels == 1) {
              *(image_column) = (uchar) (*(column));
              *(image_column + 1) = (uchar) (*(column));
              *(image_column + 2) = (uchar) (*(column));
            } else if (imageoverlay->priv->costume->nChannels == 3) {
              *(image_column) = (uchar) (*(column));
              *(image_column + 1) = (uchar) (*(column + 1));
              *(image_column + 2) = (uchar) (*(column + 2));
            } else if (imageoverlay->priv->costume->nChannels == 4) {
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

        column += imageoverlay->priv->costume->nChannels;
        image_column += imageoverlay->priv->cvImage->nChannels;
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

static GSList *
get_faces (GstStructure * faces)
{
  gint len, aux;
  GSList *list = NULL;

  len = gst_structure_n_fields (faces);

  for (aux = 0; aux < len; aux++) {
    GstStructure *face;
    gboolean ret;

    const gchar *name = gst_structure_nth_field_name (faces, aux);

    if (g_strcmp0 (name, "timestamp") == 0) {
      continue;
    }

    ret = gst_structure_get (faces, name, GST_TYPE_STRUCTURE, &face, NULL);

    if (ret) {
      CvRect *aux = g_slice_new0 (CvRect);

      gst_structure_get (face, "x", G_TYPE_UINT, &aux->x, NULL);
      gst_structure_get (face, "y", G_TYPE_UINT, &aux->y, NULL);
      gst_structure_get (face, "width", G_TYPE_UINT, &aux->width, NULL);
      gst_structure_get (face, "height", G_TYPE_UINT, &aux->height, NULL);
      gst_structure_free (face);
      list = g_slist_append (list, aux);
    }
  }
  return list;
}

static void
kms_image_overlay_get_timestamp (KmsImageOverlay * imageoverlay,
    GstStructure * faces)
{
  GstStructure *timestamp;
  gboolean ret;

  ret =
      gst_structure_get (faces, "timestamp", GST_TYPE_STRUCTURE, &timestamp,
      NULL);
  if (ret) {
    gst_structure_get (timestamp, "dts", G_TYPE_UINT64,
        &imageoverlay->priv->dts, NULL);
    gst_structure_get (timestamp, "pts", G_TYPE_UINT64,
        &imageoverlay->priv->pts, NULL);
    gst_structure_free (timestamp);
  }

}

static void
cvrect_free (gpointer data)
{
  g_slice_free (CvRect, data);
}

static GstFlowReturn
kms_image_overlay_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (filter);
  GstMapInfo info;
  GstStructure *faces;
  GSList *faces_list;

  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);

  kms_image_overlay_initialize_images (imageoverlay, frame);
  imageoverlay->priv->cvImage->imageData = (char *) info.data;

  GST_OBJECT_LOCK (imageoverlay);
  faces = g_queue_pop_head (imageoverlay->priv->events_queue);

  while (faces != NULL) {

    kms_image_overlay_get_timestamp (imageoverlay, faces);
    GST_DEBUG ("buffer pts %" G_GUINT64_FORMAT, frame->buffer->pts);
    GST_DEBUG ("event pts %" G_GUINT64_FORMAT, imageoverlay->priv->pts);
    GST_DEBUG ("queue length %d",
        g_queue_get_length (imageoverlay->priv->events_queue));

    if (imageoverlay->priv->pts == frame->buffer->pts) {
      faces_list = get_faces (faces);

      if (faces_list != NULL) {
        if (imageoverlay->priv->costume != NULL) {
          kms_image_overlay_display_detections_overlay_img (imageoverlay,
              faces_list);
        }
        g_slist_free_full (faces_list, cvrect_free);
      }
      gst_structure_free (faces);
      break;
    } else if (imageoverlay->priv->pts < frame->buffer->pts) {
      gst_structure_free (faces);
    } else {
      g_queue_push_head (imageoverlay->priv->events_queue, faces);
      break;
    }
    faces = g_queue_pop_head (imageoverlay->priv->events_queue);
  }

  GST_OBJECT_UNLOCK (imageoverlay);

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
dispose_queue_element (gpointer data)
{
  gst_structure_free (data);
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

  g_queue_free_full (imageoverlay->priv->events_queue, dispose_queue_element);
  imageoverlay->priv->events_queue = NULL;

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

  imageoverlay->priv->events_queue = g_queue_new ();
}

static gboolean
kms_image_overlay_sink_events (GstBaseTransform * trans, GstEvent * event)
{
  KmsImageOverlay *imageoverlay = KMS_IMAGE_OVERLAY (trans);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CUSTOM_DOWNSTREAM:
    {
      GstStructure *faces;

      GST_OBJECT_LOCK (imageoverlay);

      faces = gst_structure_copy (gst_event_get_structure (event));
      g_queue_push_tail (imageoverlay->priv->events_queue, faces);

      GST_OBJECT_UNLOCK (imageoverlay);
      break;
    }
    default:
      break;
  }

  return gst_pad_event_default (trans->sinkpad, GST_OBJECT (trans), event);
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
