/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <commons/kms-core-enumtypes.h>
#include <commons/kmsrecordingprofile.h>
#include <commons/kmsutils.h>
#include <commons/kmsagnosticcaps.h>

#include "kmsksrmuxer.h"

#define OBJECT_NAME "ksrmuxer"
#define KMS_KSR_MUXER_NAME OBJECT_NAME

#define parent_class kms_ksr_muxer_parent_class

GST_DEBUG_CATEGORY_STATIC (kms_ksr_muxer_debug_category);
#define GST_CAT_DEFAULT kms_ksr_muxer_debug_category

#define KMS_KSR_MUXER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (          \
    (obj),                               \
    KMS_TYPE_KSR_MUXER,                  \
    KmsKSRMuxerPrivate                   \
  )                                      \
)

struct _KmsKSRMuxerPrivate
{
  GstElement *mux;
  GstTaskPool *tasks;

  GHashTable *tracks;
  guint video_id;
  guint audio_id;
};

G_DEFINE_TYPE_WITH_CODE (KmsKSRMuxer, kms_ksr_muxer,
    KMS_TYPE_BASE_MEDIA_MUXER,
    GST_DEBUG_CATEGORY_INIT (kms_ksr_muxer_debug_category, OBJECT_NAME,
        0, "debug category for muxing pipeline object"));

static void
kms_ksr_muxer_finalize (GObject * obj)
{
  KmsKSRMuxer *self = KMS_KSR_MUXER (obj);

  GST_DEBUG_OBJECT (self, "finalize");

  g_hash_table_unref (self->priv->tracks);

  G_OBJECT_CLASS (parent_class)->finalize (obj);
}

static GstElement *
kms_ksr_muxer_add_src (KmsBaseMediaMuxer * obj, KmsMediaType type,
    const gchar * id)
{
  KmsKSRMuxer *self = KMS_KSR_MUXER (obj);
  GstElement *appsrc = NULL;
  gchar *padname;

  KMS_BASE_MEDIA_MUXER_LOCK (self);

  if (g_hash_table_contains (self->priv->tracks, id)) {
    padname = g_hash_table_lookup (self->priv->tracks, id);
  } else {
    switch (type) {
      case KMS_MEDIA_TYPE_AUDIO:
        padname = g_strdup_printf ("audio_%u", self->priv->audio_id++);
        break;
      case KMS_MEDIA_TYPE_VIDEO:
        padname = g_strdup_printf ("video_%u", self->priv->video_id++);
        break;
      default:
        GST_WARNING_OBJECT (obj, "Unsupported media type %u", type);
        goto end;
    }

    g_hash_table_insert (self->priv->tracks, g_strdup (id), padname);
  }

  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (appsrc, "format", 3 /* GST_FORMAT_TIME */ , NULL);

  gst_bin_add (GST_BIN (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self)), appsrc);

  gst_element_link_pads (appsrc, "src", self->priv->mux, padname);
  gst_element_sync_state_with_parent (appsrc);

end:
  KMS_BASE_MEDIA_MUXER_UNLOCK (self);

  return appsrc;
}

static void
on_sink_added_cb (GstElement * mux, GstElement * sink, gpointer data)
{
  KmsKSRMuxer *self = KMS_KSR_MUXER (data);

  KMS_BASE_MEDIA_MUXER_GET_CLASS (self)->emit_on_sink_added
      (KMS_BASE_MEDIA_MUXER (self), sink);
}

static void
kms_ksr_muxer_class_init (KmsKSRMuxerClass * klass)
{
  KmsBaseMediaMuxerClass *basemediamuxerclass;
  GObjectClass *objclass;

  objclass = G_OBJECT_CLASS (klass);
  objclass->finalize = kms_ksr_muxer_finalize;

  basemediamuxerclass = KMS_BASE_MEDIA_MUXER_CLASS (klass);
  basemediamuxerclass->add_src = kms_ksr_muxer_add_src;

  g_type_class_add_private (klass, sizeof (KmsKSRMuxerPrivate));
}

static void
kms_ksr_muxer_init (KmsKSRMuxer * self)
{
  self->priv = KMS_KSR_MUXER_GET_PRIVATE (self);

  self->priv->tracks = g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      g_free);
  self->priv->mux = gst_element_factory_make ("ksrmux", NULL);

  if (self->priv->mux == NULL) {
    g_warning ("No ksrmux factory available");
    return;
  }

  g_signal_connect (self->priv->mux, "on-sink-added",
      G_CALLBACK (on_sink_added_cb), self);

  g_object_bind_property (self, "uri", self->priv->mux, "uri",
      G_BINDING_DEFAULT);

  gst_bin_add (GST_BIN (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self)),
      self->priv->mux);
}

KmsKSRMuxer *
kms_ksr_muxer_new (const char *optname1, ...)
{
  KmsKSRMuxer *obj;
  va_list ap;

  va_start (ap, optname1);
  obj = KMS_KSR_MUXER (g_object_new_valist (KMS_TYPE_KSR_MUXER, optname1, ap));
  va_end (ap);

  return obj;
}
