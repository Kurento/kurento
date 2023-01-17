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

#include "kmssdpagent.h"
#include "kmssdppayloadmanager.h"
#include "kmsisdppayloadmanager.h"

#define OBJECT_NAME "sdppayloadmanager"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_payload_manager_debug_category);
#define GST_CAT_DEFAULT kms_sdp_payload_manager_debug_category

#define parent_class kms_sdp_payload_manager_parent_class

static void
kms_i_sdp_playload_manager_iface_init (KmsISdpPayloadManagerInterface * iface);

G_DEFINE_TYPE_WITH_CODE (KmsSdpPayloadManager,
    kms_sdp_payload_manager, G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_PAYLOAD_MANAGER,
        kms_i_sdp_playload_manager_iface_init);
    GST_DEBUG_CATEGORY_INIT (kms_sdp_payload_manager_debug_category,
        OBJECT_NAME, 0, "debug category for sdp rtp savpf media_handler"));

#define MIN_DYNAMIC_PAYLOAD 96
#define MAX_DYNAMIC_PAYLOAD 127

#define KMS_SDP_PAYLOAD_MANAGER_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                     \
    (obj),                                          \
    KMS_TYPE_SDP_PAYLOAD_MANAGER,                   \
    KmsSdpPayloadManagerPrivate                     \
  )                                                 \
)

struct _KmsSdpPayloadManagerPrivate
{
  GMutex mutex;
  guint counter;                /* atomic */
  gboolean share_pts;
  gchar *codecs[MAX_DYNAMIC_PAYLOAD + 1];
};

static void
kms_sdp_payload_manager_finalize (GObject * object)
{
  KmsSdpPayloadManager *self = KMS_SDP_PAYLOAD_MANAGER (object);
  gint i;

  for (i = 0; i <= MAX_DYNAMIC_PAYLOAD; i++) {
    g_free (self->priv->codecs[i]);
  }

  g_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_sdp_payload_manager_class_init (KmsSdpPayloadManagerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->finalize = kms_sdp_payload_manager_finalize;

  g_type_class_add_private (klass, sizeof (KmsSdpPayloadManagerPrivate));
}

static void
kms_sdp_payload_manager_init (KmsSdpPayloadManager * self)
{
  self->priv = KMS_SDP_PAYLOAD_MANAGER_GET_PRIVATE (self);
  self->priv->counter = MIN_DYNAMIC_PAYLOAD;
  self->priv->share_pts = FALSE;
  g_mutex_init (&self->priv->mutex);
}

KmsSdpPayloadManager *
kms_sdp_payload_manager_new ()
{
  KmsSdpPayloadManager *obj;

  obj =
      KMS_SDP_PAYLOAD_MANAGER (g_object_new (KMS_TYPE_SDP_PAYLOAD_MANAGER,
          NULL));

  return obj;
}

KmsSdpPayloadManager *
kms_sdp_payload_manager_new_same_codec_shares_pt ()
{
  KmsSdpPayloadManager *obj;

  obj =
      KMS_SDP_PAYLOAD_MANAGER (g_object_new (KMS_TYPE_SDP_PAYLOAD_MANAGER,
          NULL));
  obj->priv->share_pts = TRUE;

  return obj;
}

static gint
kms_sdp_payload_manager_get_next_int (KmsSdpPayloadManager * self,
    GError ** error)
{
  gint val;

  val = g_atomic_int_add (&self->priv->counter, 1);

  if (val <= MAX_DYNAMIC_PAYLOAD) {
    return val;
  }

  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
      "Not more dynamic payload types available");

  return -1;
}

static gint
kms_sdp_payload_manager_get_dynamic_pt_from_list (KmsSdpPayloadManager * self,
    GError ** error)
{
  gint i;

  do {
    i = kms_sdp_payload_manager_get_next_int (self, error);

    if (i < 0) {
      return i;
    }

  } while (self->priv->codecs[i] != NULL);

  return i;
}

static gint
kms_sdp_payload_get_pt_for_codec (KmsSdpPayloadManager * self,
    const gchar * codec_name)
{
  gint i;

  for (i = MIN_DYNAMIC_PAYLOAD; i <= MAX_DYNAMIC_PAYLOAD; i++) {
    if (g_strcmp0 (self->priv->codecs[i], codec_name) == 0) {
      GST_DEBUG_OBJECT (self, "Got codec for pt %s %d", codec_name, i);
      return i;
    }
  }

  return -1;
}

static void
kms_sdp_payload_manager_register_dynamic_payload_internal (KmsSdpPayloadManager
    * self, gint pt, const gchar * codec_name)
{
  gint i;

  if (self->priv->codecs[pt] != NULL) {
    g_free (self->priv->codecs[pt]);
    self->priv->codecs[pt] = NULL;
  }

  for (i = MIN_DYNAMIC_PAYLOAD; i <= MAX_DYNAMIC_PAYLOAD; i++) {
    if (g_strcmp0 (self->priv->codecs[i], codec_name) == 0) {
      g_free (self->priv->codecs[i]);
      self->priv->codecs[i] = NULL;
    }
  }

  self->priv->codecs[pt] = g_strdup (codec_name);

  GST_DEBUG_OBJECT (self, "Registering pt: %s -> %d", codec_name, pt);
}

static gint
kms_sdp_payload_manager_get_dynamic_pt (KmsISdpPayloadManager * obj,
    const gchar * codec_name, GError ** error)
{
  KmsSdpPayloadManager *self = KMS_SDP_PAYLOAD_MANAGER (obj);
  gint pt;

  if (!self->priv->share_pts) {
    return kms_sdp_payload_manager_get_next_int (self, error);
  }

  if (codec_name == NULL) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Codec name cannot be NULL");
    return -1;
  }

  g_mutex_lock (&self->priv->mutex);
  pt = kms_sdp_payload_get_pt_for_codec (self, codec_name);

  if (pt >= 0) {
    goto end;
  }

  pt = kms_sdp_payload_manager_get_dynamic_pt_from_list (self, error);

  if (pt == -1) {
    goto end;
  }

  kms_sdp_payload_manager_register_dynamic_payload_internal (self, pt,
      codec_name);

end:

  GST_DEBUG_OBJECT (self, "Pt for codec %s -> %d", codec_name, pt);
  g_mutex_unlock (&self->priv->mutex);

  return pt;
}

static gboolean
kms_sdp_payload_manager_register_dynamic_payload (KmsISdpPayloadManager * obj,
    gint pt, const gchar * codec_name, GError ** error)
{
  KmsSdpPayloadManager *self = KMS_SDP_PAYLOAD_MANAGER (obj);

  if (!self->priv->share_pts) {
    return TRUE;
  }

  if (codec_name == NULL) {
    GST_ERROR_OBJECT (self, "Codec name cannot be NULL");
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Codec name cannot be NULL");
    return FALSE;
  }

  g_mutex_lock (&self->priv->mutex);
  kms_sdp_payload_manager_register_dynamic_payload_internal (self, pt,
      codec_name);
  g_mutex_unlock (&self->priv->mutex);

  return TRUE;
}

static void
kms_i_sdp_playload_manager_iface_init (KmsISdpPayloadManagerInterface * iface)
{
  iface->get_dynamic_pt = kms_sdp_payload_manager_get_dynamic_pt;
  iface->register_dynamic_payload =
      kms_sdp_payload_manager_register_dynamic_payload;
}
