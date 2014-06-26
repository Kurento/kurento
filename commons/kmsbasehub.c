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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsbasehub.h"
#include "kmsagnosticcaps.h"
#include "kms-marshal.h"
#include "kmsmixerport.h"

#define PLUGIN_NAME "basehub"

#define KMS_BASE_HUB_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_BASE_HUB_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_base_hub_debug_category);
#define GST_CAT_DEFAULT kms_base_hub_debug_category

#define KMS_BASE_HUB_GET_PRIVATE(obj) (         \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_BASE_HUB,                          \
    KmsBaseHubPrivate                           \
  )                                             \
)

#define AUDIO_SINK_PAD_PREFIX "audio_sink_"
#define VIDEO_SINK_PAD_PREFIX "video_sink_"
#define AUDIO_SRC_PAD_PREFIX "audio_src_"
#define VIDEO_SRC_PAD_PREFIX "video_src_"
#define AUDIO_SINK_PAD_NAME AUDIO_SINK_PAD_PREFIX "%u"
#define VIDEO_SINK_PAD_NAME VIDEO_SINK_PAD_PREFIX "%u"
#define AUDIO_SRC_PAD_NAME AUDIO_SRC_PAD_PREFIX "%u"
#define VIDEO_SRC_PAD_NAME VIDEO_SRC_PAD_PREFIX "%u"
#define LENGTH_VIDEO_SRC_PAD_PREFIX 10  //sizeof("video_src_")
#define LENGTH_AUDIO_SRC_PAD_PREFIX 10  //sizeof("audio_src_")

static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD_NAME,
    GST_PAD_SINK,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_sink_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SINK_PAD_NAME,
    GST_PAD_SINK,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD_NAME,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_src_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SRC_PAD_NAME,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

enum
{
  SIGNAL_HANDLE_PORT,
  SIGNAL_UNHANDLE_PORT,
  LAST_SIGNAL
};

static guint kms_base_hub_signals[LAST_SIGNAL] = { 0 };

struct _KmsBaseHubPrivate
{
  GHashTable *ports;
  GRecMutex mutex;
  gint port_count;
  gint pad_added_id;
};

typedef struct _KmsBaseHubPortData KmsBaseHubPortData;

struct _KmsBaseHubPortData
{
  KmsBaseHub *mixer;
  GstElement *port;
  gulong signal_id;
  gint id;
  GstPad *audio_sink_target;
  GstPad *video_sink_target;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsBaseHub, kms_base_hub,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_base_hub_debug_category, PLUGIN_NAME,
        0, "debug category for basehub element"));

static gboolean
set_target (GstPad * gp, GstPad * target)
{
  GstPad *old_target;
  GstPad *peer;

  old_target = gst_ghost_pad_get_target (GST_GHOST_PAD (gp));
  if (old_target == NULL) {
    goto end;
  }
  peer = gst_pad_get_peer (old_target);

  if (peer != NULL) {
    if (peer->direction == GST_PAD_SINK) {
      gst_pad_unlink (old_target, peer);
    } else {
      gst_pad_unlink (peer, old_target);
    }
    g_object_unref (peer);
  }
  g_object_unref (old_target);

end:
  return gst_ghost_pad_set_target (GST_GHOST_PAD (gp), target);
}

static KmsBaseHubPortData *
kms_base_hub_port_data_create (KmsBaseHub * mixer, GstElement * port, gint id)
{
  KmsBaseHubPortData *data = g_slice_new0 (KmsBaseHubPortData);

  data->mixer = mixer;
  data->port = g_object_ref (port);
  data->id = id;

  return data;
}

static void
kms_base_hub_port_data_destroy (gpointer data)
{
  KmsBaseHubPortData *port_data = (KmsBaseHubPortData *) data;

  if (port_data->signal_id != 0) {
    g_signal_handler_disconnect (port_data->port, port_data->signal_id);
    port_data->signal_id = 0;
  }

  g_clear_object (&port_data->audio_sink_target);
  g_clear_object (&port_data->video_sink_target);

  g_clear_object (&port_data->port);
  g_slice_free (KmsBaseHubPortData, data);
}

gboolean
kms_base_hub_link_video_src (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->link_video_src (mixer,
      id, internal_element, pad_name, remove_on_unlink);
}

gboolean
kms_base_hub_link_audio_src (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->link_audio_src (mixer,
      id, internal_element, pad_name, remove_on_unlink);
}

gboolean
kms_base_hub_link_video_sink (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->link_video_sink (mixer,
      id, internal_element, pad_name, remove_on_unlink);
}

gboolean
kms_base_hub_link_audio_sink (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->link_audio_sink (mixer,
      id, internal_element, pad_name, remove_on_unlink);
}

gboolean
kms_base_hub_unlink_video_src (KmsBaseHub * mixer, gint id)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->unlink_video_src
      (mixer, id);
}

gboolean
kms_base_hub_unlink_audio_src (KmsBaseHub * mixer, gint id)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->unlink_audio_src
      (mixer, id);
}

gboolean
kms_base_hub_unlink_video_sink (KmsBaseHub * mixer, gint id)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->unlink_video_sink
      (mixer, id);
}

gboolean
kms_base_hub_unlink_audio_sink (KmsBaseHub * mixer, gint id)
{
  g_return_val_if_fail (KMS_IS_BASE_HUB (mixer), FALSE);

  return
      KMS_BASE_HUB_CLASS (G_OBJECT_GET_CLASS (mixer))->unlink_audio_sink
      (mixer, id);
}

static void
release_gint (gpointer data)
{
  g_slice_free (gint, data);
}

static gboolean
kms_base_hub_unlink_pad (KmsBaseHub * mixer, const gchar * gp_name)
{
  GstPad *gp;
  gboolean ret;

  gp = gst_element_get_static_pad (GST_ELEMENT (mixer), gp_name);

  if (gp == NULL) {
    return TRUE;
  }

  ret = set_target (gp, NULL);
  g_object_unref (gp);
  return ret;
}

static gboolean
kms_base_hub_unlink_video_src_default (KmsBaseHub * mixer, gint id)
{
  gboolean ret;
  gchar *gp_name = g_strdup_printf (VIDEO_SRC_PAD_PREFIX "%d", id);

  ret = kms_base_hub_unlink_pad (mixer, gp_name);

  g_free (gp_name);

  return ret;
}

static gboolean
kms_base_hub_unlink_audio_src_default (KmsBaseHub * mixer, gint id)
{
  gboolean ret;
  gchar *gp_name = g_strdup_printf (AUDIO_SRC_PAD_PREFIX "%d", id);

  ret = kms_base_hub_unlink_pad (mixer, gp_name);

  g_free (gp_name);

  return ret;
}

static gboolean
kms_base_hub_unlink_video_sink_default (KmsBaseHub * mixer, gint id)
{
  gboolean ret;
  gchar *gp_name = g_strdup_printf (VIDEO_SINK_PAD_PREFIX "%d", id);

  ret = kms_base_hub_unlink_pad (mixer, gp_name);

  g_free (gp_name);

  return ret;
}

static gboolean
kms_base_hub_unlink_audio_sink_default (KmsBaseHub * mixer, gint id)
{
  gboolean ret;
  gchar *gp_name = g_strdup_printf (AUDIO_SINK_PAD_PREFIX "%d", id);

  ret = kms_base_hub_unlink_pad (mixer, gp_name);

  g_free (gp_name);

  return ret;
}

static void
remove_unlinked_pad (GstPad * pad, GstPad * peer, gpointer user_data)
{
  GstElement *parent = gst_pad_get_parent_element (pad);

  if (parent == NULL)
    return;

  GST_DEBUG_OBJECT (GST_OBJECT_PARENT (parent), "Removing pad %" GST_PTR_FORMAT,
      pad);

  gst_element_release_request_pad (parent, pad);

  g_object_unref (parent);
}

static gboolean
kms_base_hub_link_src_pad (KmsBaseHub * mixer, const gchar * gp_name,
    const gchar * template_name, GstElement * internal_element,
    const gchar * pad_name, gboolean remove_on_unlink)
{
  GstPad *gp, *target;
  gboolean ret;

  if (GST_OBJECT_PARENT (internal_element) != GST_OBJECT (mixer)) {
    GST_ERROR_OBJECT (mixer, "Cannot link %" GST_PTR_FORMAT " wrong hierarchy",
        internal_element);
    return FALSE;
  }

  target = gst_element_get_static_pad (internal_element, pad_name);
  if (target == NULL) {
    target = gst_element_get_request_pad (internal_element, pad_name);

    if (target != NULL && remove_on_unlink) {
      g_signal_connect (G_OBJECT (target), "unlinked",
          G_CALLBACK (remove_unlinked_pad), NULL);
    }
  }

  if (target == NULL) {
    GST_ERROR_OBJECT (mixer, "Cannot get target pad");
    return FALSE;
  }

  gp = gst_element_get_static_pad (GST_ELEMENT (mixer), gp_name);

  if (gp == NULL) {
    GstPadTemplate *templ;

    templ =
        gst_element_class_get_pad_template (GST_ELEMENT_CLASS
        (G_OBJECT_GET_CLASS (mixer)), template_name);
    gp = gst_ghost_pad_new_from_template (gp_name, target, templ);

    if (GST_STATE (mixer) >= GST_STATE_PAUSED
        || GST_STATE_PENDING (mixer) >= GST_STATE_PAUSED
        || GST_STATE_TARGET (mixer) >= GST_STATE_PAUSED) {
      gst_pad_set_active (gp, TRUE);
    }

    ret = gst_element_add_pad (GST_ELEMENT (mixer), gp);
    if (!ret) {
      g_object_unref (gp);
    }
  } else {
    ret = set_target (gp, target);
    g_object_unref (gp);
  }

  g_object_unref (target);

  return ret;
}

static gboolean
kms_base_hub_link_audio_src_default (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  gchar *gp_name = g_strdup_printf (AUDIO_SRC_PAD_PREFIX "%d", id);
  gboolean ret;

  ret =
      kms_base_hub_link_src_pad (mixer, gp_name, AUDIO_SRC_PAD_NAME,
      internal_element, pad_name, remove_on_unlink);
  g_free (gp_name);

  return ret;
}

static gboolean
kms_base_hub_link_video_src_default (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  gchar *gp_name = g_strdup_printf (VIDEO_SRC_PAD_PREFIX "%d", id);
  gboolean ret;

  ret =
      kms_base_hub_link_src_pad (mixer, gp_name, VIDEO_SRC_PAD_NAME,
      internal_element, pad_name, remove_on_unlink);
  g_free (gp_name);

  return ret;
}

static gboolean
kms_base_hub_create_and_link_ghost_pad (KmsBaseHub * mixer,
    GstPad * src_pad, const gchar * gp_name, const gchar * gp_template_name,
    GstPad * target)
{
  GstPadTemplate *templ;
  GstPad *gp;
  gboolean ret;

  templ =
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS
      (G_OBJECT_GET_CLASS (mixer)), gp_template_name);
  gp = gst_ghost_pad_new_from_template (gp_name, target, templ);

  if (GST_STATE (mixer) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (mixer) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (mixer) >= GST_STATE_PAUSED) {
    gst_pad_set_active (gp, TRUE);
  }

  ret = gst_element_add_pad (GST_ELEMENT (mixer), gp);

  if (ret) {
    gst_pad_link (src_pad, gp);
  } else {
    g_object_unref (gp);
  }

  return ret;
}

static gboolean
kms_base_hub_link_sink_pad (KmsBaseHub * mixer, gint id,
    const gchar * gp_name, const gchar * gp_template_name,
    GstElement * internal_element, const gchar * pad_name,
    const gchar * port_src_pad_name, gulong target_offset,
    gboolean remove_on_unlink)
{
  KmsBaseHubPortData *port_data;
  gboolean ret;
  GstPad *gp, *target;
  GstPad **port_data_target;

  if (GST_OBJECT_PARENT (internal_element) != GST_OBJECT (mixer)) {
    GST_ERROR_OBJECT (mixer, "Cannot link %" GST_PTR_FORMAT " wrong hierarchy",
        internal_element);
    return FALSE;
  }

  target = gst_element_get_static_pad (internal_element, pad_name);
  if (target == NULL) {
    target = gst_element_get_request_pad (internal_element, pad_name);

    if (target != NULL && remove_on_unlink) {
      g_signal_connect (G_OBJECT (target), "unlinked",
          G_CALLBACK (remove_unlinked_pad), NULL);
    }
  }

  if (target == NULL) {
    GST_ERROR_OBJECT (mixer, "Cannot get target pad");
    return FALSE;
  }

  KMS_BASE_HUB_LOCK (mixer);

  port_data = g_hash_table_lookup (mixer->priv->ports, &id);

  if (port_data == NULL) {
    ret = FALSE;
    goto end;
  }

  port_data_target = G_STRUCT_MEMBER_P (port_data, target_offset);
  if (*port_data_target != NULL) {
    g_clear_object (port_data_target);
  }
  *port_data_target = g_object_ref (target);

  gp = gst_element_get_static_pad (GST_ELEMENT (mixer), gp_name);
  if (gp != NULL) {
    ret = set_target (gp, target);
    g_object_unref (gp);
  } else {
    GstPad *src_pad = gst_element_get_static_pad (port_data->port,
        port_src_pad_name);

    if (src_pad != NULL) {
      ret = kms_base_hub_create_and_link_ghost_pad (mixer, src_pad,
          gp_name, gp_template_name, target);
      g_object_unref (src_pad);
    } else {
      ret = TRUE;
    }
  }

  GST_DEBUG_OBJECT (mixer, "Audio target pad for port %d: %" GST_PTR_FORMAT,
      port_data->id, port_data->audio_sink_target);
  GST_DEBUG_OBJECT (mixer, "Video target pad for port %d: %" GST_PTR_FORMAT,
      port_data->id, port_data->video_sink_target);

end:

  KMS_BASE_HUB_UNLOCK (mixer);

  g_object_unref (target);

  return ret;
}

static gboolean
kms_base_hub_link_video_sink_default (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  gboolean ret;
  gchar *gp_name = g_strdup_printf (VIDEO_SINK_PAD_PREFIX "%d", id);

  ret = kms_base_hub_link_sink_pad (mixer, id, gp_name, VIDEO_SINK_PAD_NAME,
      internal_element, pad_name, MIXER_VIDEO_SRC_PAD,
      G_STRUCT_OFFSET (KmsBaseHubPortData, video_sink_target),
      remove_on_unlink);

  g_free (gp_name);

  return ret;
}

static gboolean
kms_base_hub_link_audio_sink_default (KmsBaseHub * mixer, gint id,
    GstElement * internal_element, const gchar * pad_name,
    gboolean remove_on_unlink)
{
  gboolean ret;
  gchar *gp_name = g_strdup_printf (AUDIO_SINK_PAD_PREFIX "%d", id);

  ret = kms_base_hub_link_sink_pad (mixer, id, gp_name, AUDIO_SINK_PAD_NAME,
      internal_element, pad_name, MIXER_AUDIO_SRC_PAD,
      G_STRUCT_OFFSET (KmsBaseHubPortData, audio_sink_target),
      remove_on_unlink);

  g_free (gp_name);

  return ret;
}

static void
kms_base_hub_remove_port_pad (KmsBaseHub * mixer, gint id,
    const gchar * pad_prefix)
{
  gchar *pad_name = g_strdup_printf ("%s%d", pad_prefix, id);
  GstPad *pad = gst_element_get_static_pad (GST_ELEMENT (mixer), pad_name);

  GST_DEBUG_OBJECT (mixer, "Trying to remove pad: %s -> %" GST_PTR_FORMAT,
      pad_name, pad);

  if (pad != NULL) {
    set_target (pad, NULL);
    gst_element_remove_pad (GST_ELEMENT (mixer), pad);
    g_object_unref (pad);
  }
  g_free (pad_name);
}

static void
kms_base_hub_remove_port_pads (KmsBaseHub * mixer, gint id)
{
  kms_base_hub_remove_port_pad (mixer, id, AUDIO_SRC_PAD_PREFIX);
  kms_base_hub_remove_port_pad (mixer, id, AUDIO_SINK_PAD_PREFIX);
  kms_base_hub_remove_port_pad (mixer, id, VIDEO_SRC_PAD_PREFIX);
  kms_base_hub_remove_port_pad (mixer, id, VIDEO_SINK_PAD_PREFIX);
}

static void
kms_base_hub_unhandle_port (KmsBaseHub * mixer, gint id)
{
  KmsBaseHubPortData *port_data;

  GST_DEBUG_OBJECT (mixer, "Unhandle port %" G_GINT32_FORMAT, id);

  KMS_BASE_HUB_LOCK (mixer);

  port_data = (KmsBaseHubPortData *) g_hash_table_lookup (mixer->priv->ports,
      &id);

  if (port_data == NULL) {
    goto end;
  }

  GST_DEBUG ("Removing element: %" GST_PTR_FORMAT, port_data->port);

  kms_base_hub_remove_port_pads (mixer, id);

  g_hash_table_remove (mixer->priv->ports, &id);

end:
  KMS_BASE_HUB_UNLOCK (mixer);
}

static gint *
kms_base_hub_generate_port_id (KmsBaseHub * mixer)
{
  gint *id;

  id = g_slice_new (gint);
  *id = g_atomic_int_add (&mixer->priv->port_count, 1);

  return id;
}

static void
mixer_pad_added (KmsBaseHub * mixer, GstPad * pad, gpointer data)
{
  if (gst_pad_get_direction (pad) != GST_PAD_SRC) {
    return;
  }

  KMS_BASE_HUB_LOCK (mixer);

  if (g_str_has_prefix (GST_OBJECT_NAME (pad), VIDEO_SRC_PAD_PREFIX)) {
    KmsBaseHubPortData *port;
    gint64 id;
    const gchar *pad_name;

    pad_name = GST_OBJECT_NAME (pad);
    id = g_ascii_strtoll (pad_name + LENGTH_VIDEO_SRC_PAD_PREFIX, NULL, 10);
    port = g_hash_table_lookup (mixer->priv->ports, &id);

    gst_element_link_pads (GST_ELEMENT (mixer), GST_OBJECT_NAME (pad),
        port->port, "mixer_video_sink");
  } else if (g_str_has_prefix (GST_OBJECT_NAME (pad), AUDIO_SRC_PAD_PREFIX)) {
    KmsBaseHubPortData *port;
    gint64 id;
    const gchar *pad_name;

    pad_name = GST_OBJECT_NAME (pad);
    id = g_ascii_strtoll (pad_name + LENGTH_AUDIO_SRC_PAD_PREFIX, NULL, 10);
    port = g_hash_table_lookup (mixer->priv->ports, &id);

    gst_element_link_pads (GST_ELEMENT (mixer), GST_OBJECT_NAME (pad),
        port->port, "mixer_audio_sink");
  }

  KMS_BASE_HUB_UNLOCK (mixer);
}

static void
endpoint_pad_added (GstElement * endpoint, GstPad * pad,
    KmsBaseHubPortData * port_data)
{
  if (gst_pad_get_direction (pad) != GST_PAD_SRC ||
      !g_str_has_prefix (GST_OBJECT_NAME (pad), "mixer")) {
    return;
  }

  KMS_BASE_HUB_LOCK (port_data->mixer);

  if (port_data->video_sink_target != NULL
      && g_strstr_len (GST_OBJECT_NAME (pad), -1, "video")) {
    gchar *gp_name = g_strdup_printf (VIDEO_SINK_PAD_PREFIX "%d",
        port_data->id);

    GST_DEBUG_OBJECT (port_data->mixer,
        "Connect %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, pad,
        port_data->video_sink_target);

    kms_base_hub_create_and_link_ghost_pad (port_data->mixer, pad, gp_name,
        VIDEO_SINK_PAD_NAME, port_data->video_sink_target);
    g_free (gp_name);
  } else if (port_data->video_sink_target != NULL
      && g_strstr_len (GST_OBJECT_NAME (pad), -1, "audio")) {
    gchar *gp_name = g_strdup_printf (AUDIO_SINK_PAD_PREFIX "%d",
        port_data->id);

    GST_DEBUG_OBJECT (port_data->mixer,
        "Connect %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, pad,
        port_data->audio_sink_target);

    kms_base_hub_create_and_link_ghost_pad (port_data->mixer, pad, gp_name,
        AUDIO_SINK_PAD_NAME, port_data->audio_sink_target);
    g_free (gp_name);
  }

  KMS_BASE_HUB_UNLOCK (port_data->mixer);
}

static gint
kms_base_hub_handle_port (KmsBaseHub * mixer, GstElement * mixer_port)
{
  KmsBaseHubPortData *port_data;
  gint *id;

  if (!KMS_IS_MIXER_PORT (mixer_port)) {
    GST_INFO_OBJECT (mixer, "Invalid MixerPort: %" GST_PTR_FORMAT, mixer_port);

    return -1;
  }

  if (GST_OBJECT_PARENT (mixer) == NULL ||
      GST_OBJECT_PARENT (mixer) != GST_OBJECT_PARENT (mixer_port)) {
    GST_ERROR_OBJECT (mixer, "Mixer and MixerPort do not have the same parent");
    return -1;
  }

  GST_DEBUG_OBJECT (mixer, "Handle port: %" GST_PTR_FORMAT, mixer_port);

  id = kms_base_hub_generate_port_id (mixer);

  GST_DEBUG_OBJECT (mixer, "Adding new port %d", *id);
  port_data = kms_base_hub_port_data_create (mixer, mixer_port, *id);

  port_data->signal_id = g_signal_connect (G_OBJECT (mixer_port),
      "pad-added", G_CALLBACK (endpoint_pad_added), port_data);

  KMS_BASE_HUB_LOCK (mixer);
  g_hash_table_insert (mixer->priv->ports, id, port_data);
  KMS_BASE_HUB_UNLOCK (mixer);

  return *id;
}

static void
kms_base_hub_dispose (GObject * object)
{
  KmsBaseHub *self = KMS_BASE_HUB (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_BASE_HUB_LOCK (self);
  g_hash_table_remove_all (self->priv->ports);
  KMS_BASE_HUB_UNLOCK (self);

  G_OBJECT_CLASS (kms_base_hub_parent_class)->dispose (object);
}

static void
kms_base_hub_finalize (GObject * object)
{
  KmsBaseHub *self = KMS_BASE_HUB (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  G_OBJECT_CLASS (kms_base_hub_parent_class)->finalize (object);
}

static void
kms_base_hub_class_init (KmsBaseHubClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "BaseHub", "Generic", "Kurento plugin for mixer connection",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  klass->handle_port = GST_DEBUG_FUNCPTR (kms_base_hub_handle_port);
  klass->unhandle_port = GST_DEBUG_FUNCPTR (kms_base_hub_unhandle_port);

  klass->link_video_src =
      GST_DEBUG_FUNCPTR (kms_base_hub_link_video_src_default);
  klass->link_audio_src =
      GST_DEBUG_FUNCPTR (kms_base_hub_link_audio_src_default);
  klass->link_video_sink =
      GST_DEBUG_FUNCPTR (kms_base_hub_link_video_sink_default);
  klass->link_audio_sink =
      GST_DEBUG_FUNCPTR (kms_base_hub_link_audio_sink_default);

  klass->unlink_video_src =
      GST_DEBUG_FUNCPTR (kms_base_hub_unlink_video_src_default);
  klass->unlink_audio_src =
      GST_DEBUG_FUNCPTR (kms_base_hub_unlink_audio_src_default);
  klass->unlink_video_sink =
      GST_DEBUG_FUNCPTR (kms_base_hub_unlink_video_sink_default);
  klass->unlink_audio_sink =
      GST_DEBUG_FUNCPTR (kms_base_hub_unlink_audio_sink_default);

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_base_hub_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_base_hub_finalize);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_sink_factory));

  /* Signals initialization */
  kms_base_hub_signals[SIGNAL_HANDLE_PORT] =
      g_signal_new ("handle-port",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseHubClass, handle_port), NULL, NULL,
      __kms_marshal_INT__OBJECT, G_TYPE_INT, 1, GST_TYPE_ELEMENT);

  kms_base_hub_signals[SIGNAL_UNHANDLE_PORT] =
      g_signal_new ("unhandle-port",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseHubClass, unhandle_port), NULL, NULL,
      __kms_marshal_VOID__INT, G_TYPE_NONE, 1, G_TYPE_INT);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsBaseHubPrivate));
}

static void
kms_base_hub_init (KmsBaseHub * self)
{
  self->priv = KMS_BASE_HUB_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->port_count = 0;
  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      release_gint, kms_base_hub_port_data_destroy);

  self->priv->pad_added_id = g_signal_connect (G_OBJECT (self),
      "pad-added", G_CALLBACK (mixer_pad_added), NULL);
}
