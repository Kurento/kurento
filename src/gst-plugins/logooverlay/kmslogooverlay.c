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

#include "kmslogooverlay.h"

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
  gboolean keepAspectRatio, center;
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

static IplImage *
load_image (gchar * url, gchar * dir, gchar * image_name)
{
  IplImage *imageAux = NULL;
  gchar *file_name = NULL;

  imageAux = cvLoadImage (url, CV_LOAD_IMAGE_UNCHANGED);
  if (imageAux) {
    GST_INFO ("Loaded successfully from local file");
    goto end;
  } else {
    GST_INFO ("Not a local file, try to download first");
  }

  file_name = g_strconcat (dir, "/", image_name, ".png", NULL);

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

  return imageAux;
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
      int new_width;
      int new_height;

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
      gst_structure_get (image, "keepAspectRatio", G_TYPE_BOOLEAN,
          &structAux->keepAspectRatio, NULL);
      gst_structure_get (image, "center", G_TYPE_BOOLEAN, &structAux->center,
          NULL);

      if ((structAux->widthPercent == 0) || (structAux->heightPercent == 0)) {
        continue;
      }

      aux = load_image (uri, logooverlay->priv->dir, structAux->id);

      if (aux != NULL) {

        new_width = logooverlay->priv->cv_image->width *
            (structAux->widthPercent);
        new_height = logooverlay->priv->cv_image->height *
            (structAux->heightPercent);

        if (structAux->keepAspectRatio) {
          float old_ratio = (float) aux->height / (float) aux->width;
          float new_ratio = (float) new_height / (float) new_width;

          if (old_ratio != new_ratio) {
            float widthRatio = (float) new_width / (float) aux->width;
            float heightRatio = (float) new_height / (float) aux->height;

            if (widthRatio < heightRatio) {
              //keep width and recalculate height
              new_height = (float) new_width / old_ratio;
            } else {
              //keep height and recalculate width
              new_width = old_ratio * (float) new_height;
            }
          }
        } else {
          structAux->center = FALSE;
        }
        structAux->active_icon =
            cvCreateImage (cvSize (new_width, new_height), aux->depth,
            aux->nChannels);
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

    if (structAux->active_icon != NULL) {
      int x_position = logooverlay->priv->cv_image->width *
          (structAux->offsetXPercent);
      int y_position = logooverlay->priv->cv_image->height *
          (structAux->offsetYPercent);

      if (structAux->center) {
        int real_width = logooverlay->priv->cv_image->width *
            (structAux->widthPercent);
        int real_height = logooverlay->priv->cv_image->height *
            (structAux->heightPercent);

        if (real_width > structAux->active_icon->width) {
          x_position = x_position +
              ((real_width - structAux->active_icon->width) / 2);
        }

        if (real_height > structAux->active_icon->height) {
          y_position = y_position +
              ((real_height - structAux->active_icon->height) / 2);
        }
      }

      kms_logo_overlay_display_overlay_img (logooverlay,
          x_position, y_position, structAux->active_icon);
    }
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
