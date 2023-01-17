/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
  GstTaskPool *pool;

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

  gst_task_pool_cleanup (self->priv->pool);
  gst_object_unref (self->priv->pool);

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
  g_object_set (appsrc, "block", TRUE, "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self)), appsrc);

  gst_element_link_pads (appsrc, "src", self->priv->mux, padname);
  gst_element_sync_state_with_parent (appsrc);

end:
  KMS_BASE_MEDIA_MUXER_UNLOCK (self);

  return appsrc;
}

static void
remove_appsrc_func (GstElement * appsrc)
{
  GstElement *parent = GST_ELEMENT_CAST (GST_OBJECT_PARENT (appsrc));

  if (parent == NULL) {
    GST_DEBUG_OBJECT (appsrc, "No parent got");
    goto end;
  }

  GST_DEBUG_OBJECT (parent, "Remove %" GST_PTR_FORMAT, appsrc);

  gst_element_set_locked_state (appsrc, TRUE);
  gst_element_set_state (appsrc, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (parent), appsrc);

end:
  g_object_unref (appsrc);
}

static void
kms_ksr_muxer_remove_appsrc (KmsKSRMuxer * self, GstElement * appsrc)
{
  GError *err = NULL;

  if (appsrc == NULL) {
    return;
  }

  gst_task_pool_push (self->priv->pool,
      (GstTaskPoolFunction) remove_appsrc_func, appsrc, &err);

  if (err != NULL) {
    GST_ERROR_OBJECT (self, "%s", err->message);
    g_error_free (err);
  }
}

static gboolean
kms_ksr_muxer_remove_src (KmsBaseMediaMuxer * obj, const gchar * id)
{
  KmsKSRMuxer *self = KMS_KSR_MUXER (obj);
  GstPad *srcpad = NULL, *sinkpad = NULL;
  gchar *padname;
  gboolean ret = FALSE;

  KMS_BASE_MEDIA_MUXER_LOCK (self);

  padname = g_hash_table_lookup (self->priv->tracks, id);

  if (padname == NULL) {
    goto end;
  }

  sinkpad = gst_element_get_static_pad (self->priv->mux, padname);

  if (sinkpad == NULL) {
    GST_WARNING_OBJECT (self, "Element %" GST_PTR_FORMAT
        " does not have pad %s", self->priv->mux, padname);
    goto end;
  }

  srcpad = gst_pad_get_peer (sinkpad);

  if (srcpad == NULL) {
    GST_WARNING_OBJECT (self, "Pad %" GST_PTR_FORMAT " has not got any peer.",
        sinkpad);
  } else {
    GstElement *appsrc;

    gst_pad_unlink (srcpad, sinkpad);
    appsrc = gst_pad_get_parent_element (srcpad);
    kms_ksr_muxer_remove_appsrc (self, appsrc);
    g_object_unref (srcpad);
  }

  GST_DEBUG_OBJECT (self, "Releasing pad %" GST_PTR_FORMAT, sinkpad);

  gst_element_release_request_pad (self->priv->mux, sinkpad);
  ret = TRUE;

end:
  KMS_BASE_MEDIA_MUXER_UNLOCK (self);

  g_clear_object (&sinkpad);

  return ret;
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
  basemediamuxerclass->remove_src = kms_ksr_muxer_remove_src;

  g_type_class_add_private (klass, sizeof (KmsKSRMuxerPrivate));
}

static void
kms_ksr_muxer_init (KmsKSRMuxer * self)
{
  GError *err = NULL;

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

  self->priv->pool = gst_task_pool_new ();
  gst_task_pool_prepare (self->priv->pool, &err);

  if (G_UNLIKELY (err != NULL)) {
    g_warning ("%s", err->message);
    g_error_free (err);
  }
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
